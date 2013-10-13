/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.metastore;

import com.facebook.fb303.FacebookBase;
import com.facebook.fb303.fb_status;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.common.classification.InterfaceAudience;
import org.apache.hadoop.hive.common.classification.InterfaceStability;
import org.apache.hadoop.hive.common.metrics.Metrics;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.AlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.ConfigValSecurityException;
import org.apache.hadoop.hive.metastore.api.Constants;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.HiveObjectPrivilege;
import org.apache.hadoop.hive.metastore.api.HiveObjectRef;
import org.apache.hadoop.hive.metastore.api.HiveObjectType;
import org.apache.hadoop.hive.metastore.api.Index;
import org.apache.hadoop.hive.metastore.api.IndexAlreadyExistsException;
import org.apache.hadoop.hive.metastore.api.InvalidObjectException;
import org.apache.hadoop.hive.metastore.api.InvalidOperationException;
import org.apache.hadoop.hive.metastore.api.InvalidPartitionException;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.metastore.api.PartitionEventType;
import org.apache.hadoop.hive.metastore.api.PrincipalPrivilegeSet;
import org.apache.hadoop.hive.metastore.api.PrincipalType;
import org.apache.hadoop.hive.metastore.api.PrivilegeBag;
import org.apache.hadoop.hive.metastore.api.PrivilegeGrantInfo;
import org.apache.hadoop.hive.metastore.api.Role;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.metastore.api.ThriftHiveMetastore;
import org.apache.hadoop.hive.metastore.api.Type;
import org.apache.hadoop.hive.metastore.api.UnknownDBException;
import org.apache.hadoop.hive.metastore.api.UnknownPartitionException;
import org.apache.hadoop.hive.metastore.api.UnknownTableException;
import org.apache.hadoop.hive.metastore.events.multitenancy.AddPartitionEvent;
import org.apache.hadoop.hive.metastore.events.multitenancy.AlterPartitionEvent;
import org.apache.hadoop.hive.metastore.events.multitenancy.AlterTableEvent;
import org.apache.hadoop.hive.metastore.events.multitenancy.CreateDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.multitenancy.CreateTableEvent;
import org.apache.hadoop.hive.metastore.events.multitenancy.DropDatabaseEvent;
import org.apache.hadoop.hive.metastore.events.multitenancy.DropPartitionEvent;
import org.apache.hadoop.hive.metastore.events.multitenancy.DropTableEvent;
import org.apache.hadoop.hive.metastore.events.multitenancy.EventCleanerTask;
import org.apache.hadoop.hive.metastore.events.multitenancy.LoadPartitionDoneEvent;
import org.apache.hadoop.hive.metastore.hooks.JDOConnectionURLHook;
import org.apache.hadoop.hive.metastore.model.MDBPrivilege;
import org.apache.hadoop.hive.metastore.model.MGlobalPrivilege;
import org.apache.hadoop.hive.metastore.model.MPartitionColumnPrivilege;
import org.apache.hadoop.hive.metastore.model.MPartitionPrivilege;
import org.apache.hadoop.hive.metastore.model.MRole;
import org.apache.hadoop.hive.metastore.model.MRoleMap;
import org.apache.hadoop.hive.metastore.model.MTableColumnPrivilege;
import org.apache.hadoop.hive.metastore.model.MTablePrivilege;
import org.apache.hadoop.hive.serde2.Deserializer;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.SerDeUtils;
import org.apache.hadoop.hive.shims.ShimLoader;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.util.ReflectionUtils;
import org.apache.hadoop.util.StringUtils;
import org.apache.thrift.TException;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.join;
import static org.apache.hadoop.hive.metastore.MetaStoreUtils.DEFAULT_DATABASE_NAME;
import static org.apache.hadoop.hive.metastore.MetaStoreUtils.validateName;

public class MultitenantMetaStoreHandler extends FacebookBase implements
                                                              ThriftHiveMetastore.Iface {
    public static final Log LOG = HiveMetaStore.LOG;
    //private static boolean createDefaultDB = false;
    //private String rawStoreClassName;
    private final HiveConf hiveConf; // stores datastore (jpox) properties,
    // right now they come from jpox.properties

    //private Warehouse wh; // hdfs warehouse
    private final ThreadLocal<RawStore> threadLocalMS =
            new ThreadLocal<RawStore>() {
                @Override
                protected synchronized RawStore initialValue() {
                    return null;
                }
            };

    // Thread local configuration is needed as many threads could make changes
    // to the conf using the connection hook
    private final ThreadLocal<Configuration> threadLocalConf =
            new ThreadLocal<Configuration>() {
                @Override
                protected synchronized Configuration initialValue() {
                    return null;
                }
            };

    public static final String AUDIT_FORMAT =
            "ugi=%s\t" +  // ugi
            "ip=%s\t" +   // remote IP
            "cmd=%s\t";   // command
    public static final Log auditLog = LogFactory.getLog(
            HiveMetaStore.class.getName() + ".audit");
    private static final ThreadLocal<Formatter> auditFormatter =
            new ThreadLocal<Formatter>() {
                @Override
                protected Formatter initialValue() {
                    return new Formatter(new StringBuilder(AUDIT_FORMAT.length() * 4));
                }
            };

    private final void logAuditEvent(String cmd) {
        if (!HiveMetaStore.isUseSASL() || cmd == null) {
            return;
        }

        UserGroupInformation ugi;
        try {
            ugi = ShimLoader.getHadoopShims().getUGIForConf(getConf());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        final Formatter fmt = auditFormatter.get();
        ((StringBuilder) fmt.out()).setLength(0);
        auditLog.info(fmt.format(AUDIT_FORMAT, ugi.getUserName(),
                                 HiveMetaStore.getSASLServer().getRemoteAddress().toString(), cmd).toString());
    }

    // The next serial number to be assigned
    private boolean checkForDefaultDb;
    private static int nextSerialNum = 0;
    private static ThreadLocal<Integer> threadLocalId = new ThreadLocal() {
        @Override
        protected synchronized Object initialValue() {
            return new Integer(nextSerialNum++);
        }
    };

    // Used for retrying JDO calls on datastore failures
    private int retryInterval = 0;
    private int retryLimit = 0;
    private JDOConnectionURLHook urlHook = null;
    private String urlHookClassName = "";

    public static Integer get() {
        return threadLocalId.get();
    }

    public MultitenantMetaStoreHandler(String name) throws MetaException {
        super(name);
        hiveConf = new HiveConf(this.getClass());
        init();
    }

    public MultitenantMetaStoreHandler(String name, HiveConf conf) throws MetaException {
        super(name);
        hiveConf = conf;
        init();
    }

    public HiveConf getHiveConf() {
        return hiveConf;
    }

    private ClassLoader classLoader;
    private AlterHandler alterHandler;
    private List<MultitenantMetaStoreEventListener> listeners;
    private List<MetaStoreEndFunctionListener> endFunctionListeners;

    {
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Configuration.class.getClassLoader();
        }
    }

    private boolean init() throws MetaException {
        //rawStoreClassName = hiveConf.getVar(HiveConf.ConfVars.METASTORE_RAW_STORE_IMPL);
/*        checkForDefaultDb = hiveConf.getBoolean(
                "hive.metastore.checkForDefaultDb", true);*/
        String alterHandlerName = hiveConf.get("hive.metastore.alter.impl",
                                               HiveAlterHandler.class.getName());
        alterHandler = (AlterHandler) ReflectionUtils.newInstance(getClass(
                alterHandlerName, AlterHandler.class), hiveConf);
        //wh = new Warehouse(hiveConf);

        retryInterval = HiveConf.getIntVar(hiveConf,
                                           HiveConf.ConfVars.METASTOREINTERVAL);
        retryLimit = HiveConf.getIntVar(hiveConf,
                                        HiveConf.ConfVars.METASTOREATTEMPTS);
        // Using the hook on startup ensures that the hook always has priority
        // over settings in *.xml. We can use hiveConf as only a single thread
        // will be calling the constructor.
/*        updateConnectionURL(hiveConf, null);*/

        //createDefaultDB();

        if (hiveConf.getBoolean("hive.metastore.metrics.enabled", false)) {
            try {
                Metrics.init();
            } catch (Exception e) {
                // log exception, but ignore inability to start
                LOG.error("error in Metrics init: " + e.getClass().getName() + " "
                          + e.getMessage());
                MetaStoreUtils.printStackTrace(e);

            }
        }

        listeners = MetaStoreUtils.getMetaStoreListeners(MultitenantMetaStoreEventListener.class, hiveConf,
                                                         hiveConf.getVar(HiveConf.ConfVars.METASTORE_EVENT_LISTENERS));
        endFunctionListeners = MetaStoreUtils.getMetaStoreListeners(
                MetaStoreEndFunctionListener.class, hiveConf,
                hiveConf.getVar(HiveConf.ConfVars.METASTORE_END_FUNCTION_LISTENERS));

        long cleanFreq = hiveConf.getLongVar(HiveConf.ConfVars.METASTORE_EVENT_CLEAN_FREQ) * 1000L;
        if (cleanFreq > 0) {
            // In default config, there is no timer.
            Timer cleaner = new Timer("Metastore Events Cleaner Thread", true);
            cleaner.schedule(new EventCleanerTask(this), cleanFreq, cleanFreq);
        }
        return true;
    }

    private String addPrefix(String s) {
        return threadLocalId.get() + ": " + s;
    }

    /**
     * A Command is a closure used to pass a block of code from individual
     * functions to executeWithRetry, which centralizes connection error
     * handling. Command is parameterized on the return type of the function.
     * <p/>
     * The general transformation is:
     * <p/>
     * From:
     * String foo(int a) throws ExceptionB {
     * <block of code>
     * }
     * <p/>
     * To:
     * String foo(final int a) throws ExceptionB {
     * String ret =  null;
     * try {
     * ret = executeWithRetry(new Command<Boolean>() {
     * String run(RawStore ms) {
     * <block of code>
     * }
     * }
     * } catch (ExceptionB e) {
     * throw e;
     * } catch (Exception e) {
     * // Since run is only supposed to throw ExceptionB it could only
     * // be a runtime exception
     * throw (RuntimeException)e;
     * }
     * }
     * <p/>
     * The catch blocks are used to ensure that the exceptions thrown by the
     * <block of code> follow the function definition.
     */
    @InterfaceAudience.LimitedPrivate({"HCATALOG"})
    @InterfaceStability.Evolving
    public static class Command<T> {

        @InterfaceAudience.LimitedPrivate({"HCATALOG"})
        @InterfaceStability.Evolving
        public T run(RawStore ms) throws Exception {
            return null;
        }
    }

    @InterfaceAudience.LimitedPrivate({"HCATALOG"})
    @InterfaceStability.Evolving
    public <T> T executeWithRetry(Command<T> cmd) throws Exception {
        T ret = null;

/*        boolean gotNewConnectUrl = false;
        boolean reloadConf = HiveConf.getBoolVar(HiveContext.getCurrentContext().getConf(),
                                                 HiveConf.ConfVars.METASTOREFORCERELOADCONF);

        if (reloadConf) {
            updateConnectionURL(getConf(), null);
        }*/

        int retryCount = 0;
        Exception caughtException = null;
        while (true) {
            try {
                RawStore ms = HiveContext.getCurrentContext().getMetaStore();
                ret = cmd.run(ms);
                break;
            } catch (javax.jdo.JDOException e) {
                caughtException = e;
            }

            if (retryCount >= retryLimit) {
                throw caughtException;
            }

            assert (retryInterval >= 0);
            retryCount++;
            LOG.error(
                    String.format(
                            "JDO datastore error. Retrying metastore command " +
                            "after %d ms (attempt %d of %d)", retryInterval, retryCount, retryLimit));
            Thread.sleep(retryInterval);
            // If we have a connection error, the JDO connection URL hook might
            // provide us with a new URL to access the datastore.
/*            String lastUrl = getConnectionURL(getConf());
            gotNewConnectUrl = updateConnectionURL(getConf(), lastUrl);*/
        }
        return ret;
    }

    private Configuration getConf() {
/*        Configuration conf = threadLocalConf.get();
        if (conf == null) {
            conf = new Configuration(hiveConf);
            threadLocalConf.set(conf);
        }*/
        return HiveContext.getCurrentContext().getConf();
    }

    /**
     * Get a cached RawStore.
     *
     * @return
     * @throws MetaException
     */
    @InterfaceAudience.LimitedPrivate({"HCATALOG"})
    @InterfaceStability.Evolving
/*    public RawStore getMS(boolean reloadConf) throws MetaException {
        RawStore ms = threadLocalMS.get();
        if (ms == null) {
            LOG.info(addPrefix("Opening raw store with implemenation class:"
                               + rawStoreClassName));
            ms = (RawStore) ReflectionUtils.newInstance(getClass(rawStoreClassName,
                                                                 RawStore.class), getConf());
            threadLocalMS.set(ms);
            ms = threadLocalMS.get();
        }

        if (reloadConf) {
            ms.setConf(getConf());
        }

        return ms;
    }*/

/**
 * Updates the connection URL in hiveConf using the hook
 *
 * @return true if a new connection URL was loaded into the thread local
 *         configuration
 */
/*    private boolean updateConnectionURL(Configuration conf, String badUrl)
            throws MetaException {
        String connectUrl = null;
        String currentUrl = getConnectionURL(conf);
        try {
            // We always call init because the hook name in the configuration could
            // have changed.
            initConnectionUrlHook();
            if (urlHook != null) {
                if (badUrl != null) {
                    urlHook.notifyBadConnectionUrl(badUrl);
                }
                connectUrl = urlHook.getJdoConnectionUrl(hiveConf);
            }
        } catch (Exception e) {
            LOG.error("Exception while getting connection URL from the hook: " +
                      e);
        }

        if (connectUrl != null && !connectUrl.equals(currentUrl)) {
            LOG.error(addPrefix(
                    String.format("Overriding %s with %s",
                                  HiveConf.ConfVars.METASTORECONNECTURLKEY.toString(),
                                  connectUrl)));
            conf.set(HiveConf.ConfVars.METASTORECONNECTURLKEY.toString(),
                     connectUrl);
            return true;
        }
        return false;
    }*/

/*    private static String getConnectionURL(Configuration conf) {
        return conf.get(
                HiveConf.ConfVars.METASTORECONNECTURLKEY.toString(), "");
    }*/

// Multiple threads could try to initialize at the same time.

/*    synchronized private void initConnectionUrlHook()
            throws ClassNotFoundException {

        String className =
                hiveConf.get(HiveConf.ConfVars.METASTORECONNECTURLHOOK.toString(), "").trim();
        if (className.equals("")) {
            urlHookClassName = "";
            urlHook = null;
            return;
        }
        boolean urlHookChanged = !urlHookClassName.equals(className);
        if (urlHook == null || urlHookChanged) {
            urlHookClassName = className.trim();

            Class<?> urlHookClass = Class.forName(urlHookClassName, true,
                                                  JavaUtils.getClassLoader());
            urlHook = (JDOConnectionURLHook) ReflectionUtils.newInstance(urlHookClass, null);
        }
        return;
    }*/

/*private void createDefaultDB_core(RawStore ms, String db, String locationURI)
            throws MetaException, InvalidObjectException {
        try {
            ms.getDatabase(db);
        } catch (NoSuchObjectException e) {
            ms.createDatabase(
                    new Database(db, DEFAULT_DATABASE_COMMENT,
                                 getDefaultDatabasePath(db).toString(), null));
        }
    }

    *//**
     * create default database if it doesn't exist
     *
     * @throws MetaException
     *//*
    private void createDefaultDB(final String db, final String locationURI) throws MetaException {

        try {
            executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    createDefaultDB_core(ms, db, locationURI);
                    return Boolean.TRUE;
                }
            });
        } catch (InvalidObjectException e) {
            throw new MetaException(e.getMessage());
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        }

    }*/

    private Class<?> getClass(String rawStoreClassName, Class<?> class1)
            throws MetaException {
        try {
            return Class.forName(rawStoreClassName, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new MetaException(rawStoreClassName + " class not found");
        }
    }

    private void logInfo(String m) {
        LOG.info(threadLocalId.get().toString() + ": " + m);
        logAuditEvent(m);
    }

    public String startFunction(String function, String extraLogInfo) {
        incrementCounter(function);
        logInfo(function + extraLogInfo);
        try {
            Metrics.startScope(function);
        } catch (IOException e) {
            LOG.debug("Exception when starting metrics scope"
                      + e.getClass().getName() + " " + e.getMessage());
            MetaStoreUtils.printStackTrace(e);
        }
        return function;
    }

    public String startFunction(String function) {
        return startFunction(function, "");
    }

    public String startTableFunction(String function, String db, String tbl) {
        return startFunction(function, " : db=" + db + " tbl=" + tbl);
    }

    public String startMultiTableFunction(String function, String db, List<String> tbls) {
        String tableNames = join(tbls, ",");
        return startFunction(function, " : db=" + db + " tbls=" + tableNames);
    }

    public String startPartitionFunction(String function, String db, String tbl,
                                         List<String> partVals) {
        return startFunction(function, " : db=" + db + " tbl=" + tbl
                                       + "[" + join(partVals, ",") + "]");
    }

    public String startPartitionFunction(String function, String db, String tbl,
                                         Map<String, String> partName) {
        return startFunction(function, " : db=" + db + " tbl=" + tbl + "partition=" + partName);
    }

    public void endFunction(String function, boolean successful) {
        endFunction(function, new MetaStoreEndFunctionContext(successful));
    }

    public void endFunction(String function, MetaStoreEndFunctionContext context) {
        try {
            Metrics.endScope(function);
        } catch (IOException e) {
            LOG.debug("Exception when closing metrics scope" + e);
        }

        for (MetaStoreEndFunctionListener listener : endFunctionListeners) {
            listener.onEndFunction(function, context);
        }
    }

    @Override
    public fb_status getStatus() {
        return fb_status.ALIVE;
    }

    @Override
    public void shutdown() {
        logInfo("Shutting down the object store...");
        RawStore ms = threadLocalMS.get();
        if (ms != null) {
            ms.shutdown();
            ms = null;
        }
        logInfo("Metastore shutdown complete.");
    }

    @Override
    public AbstractMap<String, Long> getCounters() {
        AbstractMap<String, Long> counters = super.getCounters();

        // Allow endFunctionListeners to add any counters they have collected
        if (endFunctionListeners != null) {
            for (MetaStoreEndFunctionListener listener : endFunctionListeners) {
                listener.exportCounters(counters);
            }
        }

        return counters;
    }

    private static final String DATABASE_WAREHOUSE_SUFFIX = ".db";

    private Path getDefaultDatabasePath(String dbName) throws MetaException {

        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        if (dbName.equalsIgnoreCase(DEFAULT_DATABASE_NAME)) {
            return wh.getWhRoot();
        }
        return new Path(wh.getWhRoot(), dbName.toLowerCase() + DATABASE_WAREHOUSE_SUFFIX);
    }

    private void create_database_core(RawStore ms, final Database db)
            throws AlreadyExistsException, InvalidObjectException, MetaException,
                   IOException {
        if (!validateName(db.getName())) {
            throw new InvalidObjectException(db.getName() + " is not a valid database name");
        }

        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        if (null == db.getLocationUri()) {
            db.setLocationUri(getDefaultDatabasePath(db.getName()).toString());
        } else {
            db.setLocationUri(wh.getDnsPath(new Path(db.getLocationUri())).toString());
        }
        Path dbPath = new Path(db.getLocationUri());
        boolean success = false;
        boolean madeDir = false;
        try {
            if (!wh.isDir(dbPath)) {
                if (!wh.mkdirs(dbPath)) {
                    throw new MetaException("Unable to create database path " + dbPath +
                                            ", failed to create database " + db.getName());
                }
                madeDir = true;
            }

            ms.openTransaction();
            ms.createDatabase(db);
            success = ms.commitTransaction();
        } finally {
            if (!success) {
                ms.rollbackTransaction();
                if (madeDir) {
                    wh.deleteDir(dbPath, true);
                }
            }
            for (MultitenantMetaStoreEventListener listener : listeners) {
                listener.onCreateDatabase(new CreateDatabaseEvent(db, success, this));
            }
        }
    }

    public void create_database(final Database db)
            throws AlreadyExistsException, InvalidObjectException, MetaException {
        startFunction("create_database", ": "
                                         + db.getName() + " "
                                         + db.getLocationUri() + " "
                                         + db.getDescription());
        boolean success = false;
        try {
            try {
                if (null != get_database(db.getName())) {
                    throw new AlreadyExistsException("Database " + db.getName() + " already exists");
                }
            } catch (NoSuchObjectException e) {
                // expected
            }
            success = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    create_database_core(ms, db);
                    return Boolean.TRUE;
                }
            });
        } catch (AlreadyExistsException e) {
            throw e;
        } catch (InvalidObjectException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("create_database", success);
        }
    }

    public Database get_database(final String name) throws NoSuchObjectException,
                                                           MetaException {
        startFunction("get_database", ": " + name);
        Database db = null;
        try {
            db = executeWithRetry(new Command<Database>() {
                @Override
                public Database run(RawStore ms) throws Exception {
                    return ms.getDatabase(name);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_database", db != null);
        }
        return db;
    }

    public void alter_database(final String dbName, final Database db)
            throws NoSuchObjectException, TException, MetaException {
        startFunction("alter_database" + dbName);
        boolean success = false;
        try {
            success = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    return ms.alterDatabase(dbName, db);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("alter_database", success);
        }
    }

    private void drop_database_core(RawStore ms,
                                    final String name, final boolean deleteData,
                                    final boolean cascade)
            throws NoSuchObjectException, InvalidOperationException, MetaException,
                   IOException {
        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        boolean success = false;
        Database db = null;
        try {
            ms.openTransaction();
            db = ms.getDatabase(name);
            List<String> allTables = get_all_tables(db.getName());
            if (!cascade && !allTables.isEmpty()) {
                throw new InvalidOperationException("Database " + db.getName() + " is not empty");
            }
            Path path = new Path(db.getLocationUri()).getParent();
            if (!wh.isWritable(path)) {
                throw new MetaException("Database not dropped since " +
                                        path + " is not writable by " +
                                        hiveConf.getUser());
            }
            if (ms.dropDatabase(name)) {
                success = ms.commitTransaction();
            }
        } finally {
            if (!success) {
                ms.rollbackTransaction();
            } else if (deleteData) {
                wh.deleteDir(new Path(db.getLocationUri()), true);
                // it is not a terrible thing even if the data is not deleted
            }
            for (MultitenantMetaStoreEventListener listener : listeners) {
                listener.onDropDatabase(new DropDatabaseEvent(db, success, this));
            }
        }
    }

    public void drop_database(final String dbName, final boolean deleteData, final boolean cascade)
            throws NoSuchObjectException, InvalidOperationException, MetaException {

        startFunction("drop_database", ": " + dbName);
        if (DEFAULT_DATABASE_NAME.equalsIgnoreCase(dbName)) {
            endFunction("drop_database", false);
            throw new MetaException("Can not drop default database");
        }

        boolean success = false;
        try {
            success = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    drop_database_core(ms, dbName, deleteData, cascade);
                    return Boolean.TRUE;
                }
            });
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (InvalidOperationException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("drop_database", success);
        }
    }

    public List<String> get_databases(final String pattern) throws MetaException {
        startFunction("get_databases", ": " + pattern);

        List<String> ret = null;
        try {
            ret = executeWithRetry(new Command<List<String>>() {
                @Override
                public List<String> run(RawStore ms) throws Exception {
                    return ms.getDatabases(pattern);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_databases", ret != null);
        }
        return ret;
    }

    public List<String> get_all_databases() throws MetaException {
        startFunction("get_all_databases");

        List<String> ret = null;
        try {
            ret = executeWithRetry(new Command<List<String>>() {
                @Override
                public List<String> run(RawStore ms) throws Exception {
                    return ms.getAllDatabases();
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_all_databases", ret != null);
        }
        return ret;
    }

    private void create_type_core(final RawStore ms, final Type type)
            throws AlreadyExistsException, MetaException, InvalidObjectException {
        if (!MetaStoreUtils.validateName(type.getName())) {
            throw new InvalidObjectException("Invalid type name");
        }

        boolean success = false;
        try {
            ms.openTransaction();
            if (is_type_exists(ms, type.getName())) {
                throw new AlreadyExistsException("Type " + type.getName() + " already exists");
            }
            ms.createType(type);
            success = ms.commitTransaction();
        } finally {
            if (!success) {
                ms.rollbackTransaction();
            }
        }
    }

    public boolean create_type(final Type type) throws AlreadyExistsException,
                                                       MetaException, InvalidObjectException {
        startFunction("create_type", ": " + type.getName());
        boolean ret = false;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    create_type_core(ms, type);
                    return Boolean.TRUE;
                }
            });
        } catch (AlreadyExistsException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (InvalidObjectException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("create_type", ret);
        }

        return ret;
    }

    public Type get_type(final String name) throws MetaException, NoSuchObjectException {
        startFunction("get_type", ": " + name);

        Type ret = null;
        try {
            ret = executeWithRetry(new Command<Type>() {
                @Override
                public Type run(RawStore ms) throws Exception {
                    Type type = ms.getType(name);
                    if (null == type) {
                        throw new NoSuchObjectException("Type \"" + name + "\" not found.");
                    }
                    return type;
                }
            });
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_type", ret != null);
        }
        return ret;
    }

    private boolean is_type_exists(RawStore ms, String typeName)
            throws MetaException {
        return (ms.getType(typeName) != null);
    }

    private void drop_type_core(final RawStore ms, String typeName)
            throws NoSuchObjectException, MetaException {
        boolean success = false;
        try {
            ms.openTransaction();
            // drop any partitions
            if (!is_type_exists(ms, typeName)) {
                throw new NoSuchObjectException(typeName + " doesn't exist");
            }
            if (!ms.dropType(typeName)) {
                throw new MetaException("Unable to drop type " + typeName);
            }
            success = ms.commitTransaction();
        } finally {
            if (!success) {
                ms.rollbackTransaction();
            }
        }
    }


    public boolean drop_type(final String name) throws MetaException {
        startFunction("drop_type", ": " + name);

        boolean ret = false;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    // TODO:pc validate that there are no types that refer to this
                    return ms.dropType(name);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("drop_type", ret);
        }
        return ret;
    }

    public Map<String, Type> get_type_all(String name) throws MetaException {
        // TODO Auto-generated method stub
        startFunction("get_type_all", ": " + name);
        endFunction("get_type_all", false);
        throw new MetaException("Not yet implemented");
    }

    private void create_table_core(final RawStore ms, final Table tbl)
            throws AlreadyExistsException, MetaException, InvalidObjectException,
                   NoSuchObjectException {

        if (!MetaStoreUtils.validateName(tbl.getTableName())
            || !MetaStoreUtils.validateColNames(tbl.getSd().getCols())
            || (tbl.getPartitionKeys() != null && !MetaStoreUtils
                .validateColNames(tbl.getPartitionKeys()))) {
            throw new InvalidObjectException(tbl.getTableName()
                                             + " is not a valid object name");
        }

        Path tblPath = null;
        boolean success = false, madeDir = false;
        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        try {
            ms.openTransaction();

            if (ms.getDatabase(tbl.getDbName()) == null) {
                throw new NoSuchObjectException("The database " + tbl.getDbName() + " does not exist");
            }

            // get_table checks whether database exists, it should be moved here
            if (is_table_exists(ms, tbl.getDbName(), tbl.getTableName())) {
                throw new AlreadyExistsException("Table " + tbl.getTableName()
                                                 + " already exists");
            }

            if (!TableType.VIRTUAL_VIEW.toString().equals(tbl.getTableType())) {
                if (tbl.getSd().getLocation() == null
                    || tbl.getSd().getLocation().isEmpty()) {
                    tblPath = wh.getTablePath(
                            ms.getDatabase(tbl.getDbName()), tbl.getTableName());
                } else {
                    if (!isExternal(tbl) && !MetaStoreUtils.isNonNativeTable(tbl)) {
                        LOG.warn("Location: " + tbl.getSd().getLocation()
                                 + " specified for non-external table:" + tbl.getTableName());
                    }
                    tblPath = wh.getDnsPath(new Path(tbl.getSd().getLocation()));
                }
                tbl.getSd().setLocation(tblPath.toString());
            }

            if (tblPath != null) {
                if (!wh.isDir(tblPath)) {
                    if (!wh.mkdirs(tblPath)) {
                        throw new MetaException(tblPath
                                                + " is not a directory or unable to create one");
                    }
                    madeDir = true;
                }
            }

            // set create time
            long time = System.currentTimeMillis() / 1000;
            tbl.setCreateTime((int) time);
            if (tbl.getParameters() == null ||
                tbl.getParameters().get(Constants.DDL_TIME) == null) {
                tbl.putToParameters(Constants.DDL_TIME, Long.toString(time));
            }
            ms.createTable(tbl);
            success = ms.commitTransaction();

        } finally {
            if (!success) {
                ms.rollbackTransaction();
                if (madeDir) {
                    wh.deleteDir(tblPath, true);
                }
            }
            for (MultitenantMetaStoreEventListener listener : listeners) {
                listener.onCreateTable(new CreateTableEvent(tbl, success, this));
            }
        }
    }

    public void create_table(final Table tbl) throws AlreadyExistsException,
                                                     MetaException, InvalidObjectException {
        startFunction("create_table", ": db=" + tbl.getDbName() + " tbl="
                                      + tbl.getTableName());
        boolean success = false;
        try {
            success = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    create_table_core(ms, tbl);
                    return Boolean.TRUE;
                }
            });
        } catch (AlreadyExistsException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (InvalidObjectException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw new InvalidObjectException(e.getMessage());
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("create_table", success);
        }
    }

    private boolean is_table_exists(RawStore ms, String dbname, String name)
            throws MetaException {
        return (ms.getTable(dbname, name) != null);
    }

    private void drop_table_core(final RawStore ms, final String dbname,
                                 final String name, final boolean deleteData)
            throws NoSuchObjectException, MetaException, IOException {

        boolean success = false;
        boolean isExternal = false;
        Path tblPath = null;
        Table tbl = null;
        isExternal = false;
        boolean isIndexTable = false;
        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        try {
            ms.openTransaction();
            // drop any partitions
            tbl = get_table(dbname, name);
            if (tbl == null) {
                throw new NoSuchObjectException(name + " doesn't exist");
            }
            if (tbl.getSd() == null) {
                throw new MetaException("Table metadata is corrupted");
            }

            isIndexTable = isIndexTable(tbl);
            if (isIndexTable) {
                throw new RuntimeException(
                        "The table " + name + " is an index table. Please do drop index instead.");
            }

            if (!isIndexTable) {
                try {
                    List<Index> indexes = ms.getIndexes(dbname, name, Short.MAX_VALUE);
                    while (indexes != null && indexes.size() > 0) {
                        for (Index idx : indexes) {
                            this.drop_index_by_name(dbname, name, idx.getIndexName(), true);
                        }
                        indexes = ms.getIndexes(dbname, name, Short.MAX_VALUE);
                    }
                } catch (TException e) {
                    throw new MetaException(e.getMessage());
                }
            }
            isExternal = isExternal(tbl);
            if (tbl.getSd().getLocation() != null) {
                tblPath = new Path(tbl.getSd().getLocation());
                if (!wh.isWritable(tblPath.getParent())) {
                    throw new MetaException("Table metadata not deleted since " +
                                            tblPath.getParent() + " is not writable by " +
                                            hiveConf.getUser());
                }
            }

            if (!ms.dropTable(dbname, name)) {
                throw new MetaException("Unable to drop table");
            }
            success = ms.commitTransaction();
        } finally {
            if (!success) {
                ms.rollbackTransaction();
            } else if (deleteData && (tblPath != null) && !isExternal) {
                wh.deleteDir(tblPath, true);
                // ok even if the data is not deleted
            }
            for (MultitenantMetaStoreEventListener listener : listeners) {
                listener.onDropTable(new DropTableEvent(tbl, success, this));
            }
        }
    }

    public void drop_table(final String dbname, final String name, final boolean deleteData)
            throws NoSuchObjectException, MetaException {
        startTableFunction("drop_table", dbname, name);

        boolean success = false;
        try {
            success = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    drop_table_core(ms, dbname, name, deleteData);
                    return Boolean.TRUE;
                }
            });
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("drop_table", success);
        }

    }

    /**
     * Is this an external table?
     *
     * @param table Check if this table is external.
     * @return True if the table is external, otherwise false.
     */
    private boolean isExternal(Table table) {
        return MetaStoreUtils.isExternalTable(table);
    }

    private boolean isIndexTable(Table table) {
        return MetaStoreUtils.isIndexTable(table);
    }

    public Table get_table(final String dbname, final String name) throws MetaException,
                                                                          NoSuchObjectException {
        Table t = null;
        startTableFunction("get_table", dbname, name);
        try {
            t = executeWithRetry(new Command<Table>() {
                @Override
                public Table run(RawStore ms) throws Exception {
                    Table t = ms.getTable(dbname, name);
                    if (t == null) {
                        throw new NoSuchObjectException(dbname + "." + name
                                                        + " table not found");
                    }
                    return t;
                }
            });
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_table", t != null);
        }
        return t;
    }

    /**
     * Gets multiple tables from the hive metastore.
     *
     * @param dbname The name of the database in which the tables reside
     * @param names  The names of the tables to get.
     * @return A list of tables whose names are in the the list "names" and
     *         are retrievable from the database specified by "dbnames."
     *         There is no guarantee of the order of the returned tables.
     *         If there are duplicate names, only one instance of the table will be returned.
     * @throws MetaException
     * @throws InvalidOperationException
     * @throws org.apache.hadoop.hive.metastore.api.UnknownDBException
     *
     */
    public List<Table> get_table_objects_by_name(final String dbname, final List<String> names)
            throws MetaException, InvalidOperationException, UnknownDBException {
        List<Table> tables = null;
        startMultiTableFunction("get_multi_table", dbname, names);
        try {
            tables = executeWithRetry(new Command<List<Table>>() {
                @Override
                public List<Table> run(RawStore ms) throws Exception {
                    if (dbname == null || dbname.isEmpty()) {
                        throw new UnknownDBException("DB name is null or empty");
                    }
                    if (names == null) {
                        throw new InvalidOperationException(dbname + " cannot find null tables");
                    }
                    List<Table> foundTables = ms.getTableObjectsByName(dbname, names);
                    return foundTables;
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (InvalidOperationException e) {
            throw e;
        } catch (UnknownDBException e) {
            throw e;
        } catch (Exception e) {
            throw new MetaException(e.toString());
        } finally {
            endFunction("get_multi_table", tables != null);
        }
        return tables;
    }

    @Override
    public List<String> get_table_names_by_filter(
            final String dbName, final String filter, final short maxTables)
            throws MetaException, InvalidOperationException, UnknownDBException {
        List<String> tables = null;
        startFunction("get_table_names_by_filter", ": db = " + dbName + ", filter = " + filter);
        try {
            tables = executeWithRetry(new Command<List<String>>() {
                @Override
                public List<String> run(RawStore ms) throws Exception {
                    if (dbName == null || dbName.isEmpty()) {
                        throw new UnknownDBException("DB name is null or empty");
                    }
                    if (filter == null) {
                        throw new InvalidOperationException(filter + " cannot apply null filter");
                    }
                    List<String> tables = ms.listTableNamesByFilter(dbName, filter, maxTables);
                    return tables;
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (InvalidOperationException e) {
            throw e;
        } catch (UnknownDBException e) {
            throw e;
        } catch (Exception e) {
            throw new MetaException(e.toString());
        } finally {
            endFunction("get_table_names_by_filter", tables != null);
        }
        return tables;
    }

    public boolean set_table_parameters(String dbname, String name,
                                        Map<String, String> params)
            throws NoSuchObjectException, MetaException {
        endFunction(startTableFunction("set_table_parameters", dbname, name), false);
        // TODO Auto-generated method stub
        return false;
    }

    private Partition append_partition_common(RawStore ms, String dbName, String tableName,
                                              List<String> part_vals) throws InvalidObjectException,
                                                                             AlreadyExistsException,
                                                                             MetaException {

        Partition part = new Partition();
        boolean success = false, madeDir = false;
        Path partLocation = null;
        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        try {
            ms.openTransaction();
            part.setDbName(dbName);
            part.setTableName(tableName);
            part.setValues(part_vals);

            Table tbl = ms.getTable(part.getDbName(), part.getTableName());
            if (tbl == null) {
                throw new InvalidObjectException(
                        "Unable to add partition because table or database do not exist");
            }
            if (tbl.getSd().getLocation() == null) {
                throw new MetaException(
                        "Cannot append a partition to a view");
            }

            part.setSd(tbl.getSd());
            partLocation = new Path(tbl.getSd().getLocation(), Warehouse
                    .makePartName(tbl.getPartitionKeys(), part_vals));
            part.getSd().setLocation(partLocation.toString());

            Partition old_part = null;
            try {
                old_part = ms.getPartition(part.getDbName(), part
                        .getTableName(), part.getValues());
            } catch (NoSuchObjectException e) {
                // this means there is no existing partition
                old_part = null;
            }
            if (old_part != null) {
                throw new AlreadyExistsException("Partition already exists:" + part);
            }

            if (!wh.isDir(partLocation)) {
                if (!wh.mkdirs(partLocation)) {
                    throw new MetaException(partLocation
                                            + " is not a directory or unable to create one");
                }
                madeDir = true;
            }

            // set create time
            long time = System.currentTimeMillis() / 1000;
            part.setCreateTime((int) time);
            part.putToParameters(Constants.DDL_TIME, Long.toString(time));

            success = ms.addPartition(part);
            if (success) {
                success = ms.commitTransaction();
            }
        } finally {
            if (!success) {
                ms.rollbackTransaction();
                if (madeDir) {
                    wh.deleteDir(partLocation, true);
                }
            }
        }
        return part;
    }

    public Partition append_partition(final String dbName, final String tableName,
                                      final List<String> part_vals) throws InvalidObjectException,
                                                                           AlreadyExistsException,
                                                                           MetaException {
        startPartitionFunction("append_partition", dbName, tableName, part_vals);
        if (LOG.isDebugEnabled()) {
            for (String part : part_vals) {
                LOG.debug(part);
            }
        }

        Partition ret = null;
        try {
            ret = executeWithRetry(new Command<Partition>() {
                @Override
                public Partition run(RawStore ms) throws Exception {
                    return append_partition_common(ms, dbName, tableName, part_vals);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (InvalidObjectException e) {
            throw e;
        } catch (AlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("append_partition", ret != null);
        }
        return ret;
    }

    private int add_partitions_core(final RawStore ms, final List<Partition> parts)
            throws MetaException, InvalidObjectException, AlreadyExistsException {
        String db = parts.get(0).getDbName();
        String tbl = parts.get(0).getTableName();
        logInfo("add_partitions : db=" + db + " tbl=" + tbl);

        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        boolean success = false;
        Map<Partition, Boolean> addedPartitions = new HashMap<Partition, Boolean>();
        try {
            ms.openTransaction();
            for (Partition part : parts) {
                Map.Entry<Partition, Boolean> e = add_partition_core_notxn(ms, part);
                addedPartitions.put(e.getKey(), e.getValue());
            }
            success = true;
            ms.commitTransaction();
        } finally {
            if (!success) {
                ms.rollbackTransaction();
                for (Map.Entry<Partition, Boolean> e : addedPartitions.entrySet()) {
                    if (e.getValue()) {
                        wh.deleteDir(new Path(e.getKey().getSd().getLocation()), true);
                        // we just created this directory - it's not a case of pre-creation, so we nuke
                    }
                }
            }
        }
        return parts.size();
    }

    public int add_partitions(final List<Partition> parts) throws MetaException,
                                                                  InvalidObjectException,
                                                                  AlreadyExistsException {
        startFunction("add_partition");
        if (parts.size() == 0) {
            return 0;
        }

        Integer ret = null;
        try {
            ret = executeWithRetry(new Command<Integer>() {
                @Override
                public Integer run(RawStore ms) throws Exception {
                    int ret = add_partitions_core(ms, parts);
                    return Integer.valueOf(ret);
                }
            });
        } catch (InvalidObjectException e) {
            throw e;
        } catch (AlreadyExistsException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("add_partition", ret != null);
        }
        return ret;
    }

    /**
     * An implementation of add_partition_core that does not commit
     * transaction or rollback transaction as part of its operation
     * - it is assumed that will be tended to from outside this call
     *
     * @param ms
     * @param part
     * @return
     * @throws InvalidObjectException
     * @throws AlreadyExistsException
     * @throws MetaException
     */
    private Map.Entry<Partition, Boolean> add_partition_core_notxn(
            final RawStore ms, final Partition part)
            throws InvalidObjectException, AlreadyExistsException, MetaException {
        boolean success = false, madeDir = false;

        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        Path partLocation = null;
        try {
            Partition old_part = null;
            try {
                old_part = ms.getPartition(part.getDbName(), part
                        .getTableName(), part.getValues());
            } catch (NoSuchObjectException e) {
                // this means there is no existing partition
                old_part = null;
            }
            if (old_part != null) {
                throw new AlreadyExistsException("Partition already exists:" + part);
            }
            Table tbl = ms.getTable(part.getDbName(), part.getTableName());
            if (tbl == null) {
                throw new InvalidObjectException(
                        "Unable to add partition because table or database do not exist");
            }

            String partLocationStr = null;
            if (part.getSd() != null) {
                partLocationStr = part.getSd().getLocation();
            }
            if (partLocationStr == null || partLocationStr.isEmpty()) {
                // set default location if not specified and this is
                // a physical table partition (not a view)
                if (tbl.getSd().getLocation() != null) {
                    partLocation = new Path(tbl.getSd().getLocation(), Warehouse
                            .makePartName(tbl.getPartitionKeys(), part.getValues()));
                }

            } else {
                if (tbl.getSd().getLocation() == null) {
                    throw new MetaException(
                            "Cannot specify location for a view partition");
                }
                partLocation = wh.getDnsPath(new Path(partLocationStr));
            }

            if (partLocation != null) {
                part.getSd().setLocation(partLocation.toString());


                // Check to see if the directory already exists before calling
                // mkdirs() because if the file system is read-only, mkdirs will
                // throw an exception even if the directory already exists.
                if (!wh.isDir(partLocation)) {
                    if (!wh.mkdirs(partLocation)) {
                        throw new MetaException(partLocation
                                                + " is not a directory or unable to create one");
                    }
                    madeDir = true;
                }
            }

            // set create time
            long time = System.currentTimeMillis() / 1000;
            part.setCreateTime((int) time);
            if (part.getParameters() == null ||
                part.getParameters().get(Constants.DDL_TIME) == null) {
                part.putToParameters(Constants.DDL_TIME, Long.toString(time));
            }

            // Inherit table properties into partition properties.
            Map<String, String> tblParams = tbl.getParameters();
            String inheritProps = hiveConf.getVar(HiveConf.ConfVars.METASTORE_PART_INHERIT_TBL_PROPS).trim();
            // Default value is empty string in which case no properties will be inherited.
            // * implies all properties needs to be inherited
            Set<String> inheritKeys = new HashSet<String>(Arrays.asList(inheritProps.split(",")));
            if (inheritKeys.contains("*")) {
                inheritKeys = tblParams.keySet();
            }

            for (String key : inheritKeys) {
                String paramVal = tblParams.get(key);
                if (null != paramVal) { // add the property only if it exists in table properties
                    part.putToParameters(key, paramVal);
                }
            }

            success = ms.addPartition(part);

        } finally {
            if (!success) {
                if (madeDir) {
                    wh.deleteDir(partLocation, true);
                }
            }
            for (MultitenantMetaStoreEventListener listener : listeners) {
                listener.onAddPartition(new AddPartitionEvent(part, success, this));
            }
        }
        Map<Partition, Boolean> returnVal = new HashMap<Partition, Boolean>();
        returnVal.put(part, madeDir);
        return returnVal.entrySet().iterator().next();
    }

    private Partition add_partition_core(final RawStore ms, final Partition part)
            throws InvalidObjectException, AlreadyExistsException, MetaException {
        boolean success = false;
        Partition retPtn = null;
        try {
            ms.openTransaction();
            retPtn = add_partition_core_notxn(ms, part).getKey();
            // we proceed only if we'd actually succeeded anyway, otherwise,
            // we'd have thrown an exception
            success = ms.commitTransaction();
        } finally {
            if (!success) {
                ms.rollbackTransaction();
            }
        }
        return retPtn;
    }

    public Partition add_partition(final Partition part)
            throws InvalidObjectException, AlreadyExistsException, MetaException {
        startTableFunction("add_partition", part.getDbName(), part.getTableName());

        Partition ret = null;
        try {
            ret = executeWithRetry(new Command<Partition>() {
                @Override
                public Partition run(RawStore ms) throws Exception {
                    return add_partition_core(ms, part);
                }
            });
        } catch (InvalidObjectException e) {
            throw e;
        } catch (AlreadyExistsException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("add_partition", ret != null);
        }
        return ret;

    }

    private boolean drop_partition_common(RawStore ms, String db_name, String tbl_name,
                                          List<String> part_vals, final boolean deleteData)
            throws MetaException, NoSuchObjectException, IOException {

        boolean success = false;
        Path partPath = null;
        Table tbl = null;
        Partition part = null;
        boolean isArchived = false;
        Path archiveParentDir = null;
        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();

        try {
            ms.openTransaction();
            part = ms.getPartition(db_name, tbl_name, part_vals);

            if (part == null) {
                throw new NoSuchObjectException("Partition doesn't exist. "
                                                + part_vals);
            }

            isArchived = MetaStoreUtils.isArchived(part);
            if (isArchived) {
                archiveParentDir = MetaStoreUtils.getOriginalLocation(part);
                if (!wh.isWritable(archiveParentDir.getParent())) {
                    throw new MetaException("Table partition not deleted since " +
                                            archiveParentDir.getParent() + " is not writable by " +
                                            hiveConf.getUser());
                }
            }
            if (!ms.dropPartition(db_name, tbl_name, part_vals)) {
                throw new MetaException("Unable to drop partition");
            }
            success = ms.commitTransaction();
            if ((part.getSd() != null) && (part.getSd().getLocation() != null)) {
                partPath = new Path(part.getSd().getLocation());
                if (!wh.isWritable(partPath.getParent())) {
                    throw new MetaException("Table partition not deleted since " +
                                            partPath.getParent() + " is not writable by " +
                                            hiveConf.getUser());
                }
            }
            tbl = get_table(db_name, tbl_name);
        } finally {
            if (!success) {
                ms.rollbackTransaction();
            } else if (deleteData && ((partPath != null) || (archiveParentDir != null))) {
                if (tbl != null && !isExternal(tbl)) {
                    // Archived partitions have har:/to_har_file as their location.
                    // The original directory was saved in params
                    if (isArchived) {
                        assert (archiveParentDir != null);
                        wh.deleteDir(archiveParentDir, true);
                    } else {
                        assert (partPath != null);
                        wh.deleteDir(partPath, true);
                    }
                    // ok even if the data is not deleted
                }
            }
            for (MultitenantMetaStoreEventListener listener : listeners) {
                listener.onDropPartition(new DropPartitionEvent(part, success, this));
            }
        }
        return true;
    }

    public boolean drop_partition(final String db_name, final String tbl_name,
                                  final List<String> part_vals, final boolean deleteData)
            throws NoSuchObjectException, MetaException, TException {
        startPartitionFunction("drop_partition", db_name, tbl_name, part_vals);
        LOG.info("Partition values:" + part_vals);

        boolean ret = false;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    return Boolean.valueOf(
                            drop_partition_common(ms, db_name, tbl_name, part_vals, deleteData));
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("drop_partition", ret);
        }
        return ret;

    }

    public Partition get_partition(final String db_name, final String tbl_name,
                                   final List<String> part_vals)
            throws MetaException, NoSuchObjectException {
        startPartitionFunction("get_partition", db_name, tbl_name, part_vals);

        Partition ret = null;
        try {
            ret = executeWithRetry(new Command<Partition>() {
                @Override
                public Partition run(RawStore ms) throws Exception {
                    return ms.getPartition(db_name, tbl_name, part_vals);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partition", ret != null);
        }
        return ret;
    }

    @Override
    public Partition get_partition_with_auth(final String db_name,
                                             final String tbl_name, final List<String> part_vals,
                                             final String user_name, final List<String> group_names)
            throws MetaException, NoSuchObjectException, TException {
        startPartitionFunction("get_partition_with_auth", db_name, tbl_name,
                               part_vals);

        Partition ret = null;
        try {
            ret = executeWithRetry(new Command<Partition>() {
                @Override
                public Partition run(RawStore ms) throws Exception {
                    return ms.getPartitionWithAuth(db_name, tbl_name, part_vals,
                                                   user_name, group_names);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partition_with_auth", ret != null);
        }
        return ret;
    }

    public List<Partition> get_partitions(final String db_name, final String tbl_name,
                                          final short max_parts)
            throws NoSuchObjectException, MetaException {
        startTableFunction("get_partitions", db_name, tbl_name);

        List<Partition> ret = null;
        try {
            ret = executeWithRetry(new Command<List<Partition>>() {
                @Override
                public List<Partition> run(RawStore ms) throws Exception {
                    return ms.getPartitions(db_name, tbl_name, max_parts);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partitions", ret != null);
        }
        return ret;

    }

    @Override
    public List<Partition> get_partitions_with_auth(final String dbName,
                                                    final String tblName, final short maxParts,
                                                    final String userName,
                                                    final List<String> groupNames)
            throws NoSuchObjectException,
                   MetaException, TException {
        startTableFunction("get_partitions_with_auth", dbName, tblName);

        List<Partition> ret = null;
        try {
            ret = executeWithRetry(new Command<List<Partition>>() {
                @Override
                public List<Partition> run(RawStore ms) throws Exception {
                    return ms.getPartitionsWithAuth(dbName, tblName, maxParts,
                                                    userName, groupNames);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partitions_with_auth", ret != null);
        }
        return ret;

    }

    public List<String> get_partition_names(final String db_name, final String tbl_name,
                                            final short max_parts) throws MetaException {
        startTableFunction("get_partition_names", db_name, tbl_name);

        List<String> ret = null;
        try {
            ret = executeWithRetry(new Command<List<String>>() {
                @Override
                public List<String> run(RawStore ms) throws Exception {
                    return ms.listPartitionNames(db_name, tbl_name, max_parts);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partition_names", ret != null);
        }
        return ret;
    }

    public void alter_partition(final String db_name, final String tbl_name,
                                final Partition new_part)
            throws InvalidOperationException, MetaException,
                   TException {
        rename_partition(db_name, tbl_name, null, new_part);
    }

    public void rename_partition(final String db_name, final String tbl_name,
                                 final List<String> part_vals, final Partition new_part)
            throws InvalidOperationException, MetaException,
                   TException {
        startTableFunction("alter_partition", db_name, tbl_name);
        LOG.info("New partition values:" + new_part.getValues());
        if (part_vals != null && part_vals.size() > 0) {
            LOG.info("Old Partition values:" + part_vals);
        }

        boolean success = false;
        try {
            success = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    alter_partition_core(ms, db_name, tbl_name, part_vals, new_part);
                    return Boolean.TRUE;
                }
            });
        } catch (InvalidObjectException e) {
            throw new InvalidOperationException(e.getMessage());
        } catch (AlreadyExistsException e) {
            throw new InvalidOperationException(e.getMessage());
        } catch (MetaException e) {
            throw e;
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("alter_partition", success);
        }
        return;
    }

    private void alter_partition_core(final RawStore ms, final String dbname, final String name,
                                      final List<String> part_vals, final Partition new_part)
            throws InvalidOperationException, InvalidObjectException, AlreadyExistsException,
                   MetaException {
        boolean success = false;

        Path srcPath = null;
        Path destPath = null;
        FileSystem srcFs = null;
        FileSystem destFs = null;
        Table tbl = null;
        Partition oldPart = null;
        String oldPartLoc = null;
        String newPartLoc = null;
        // Set DDL time to now if not specified
        if (new_part.getParameters() == null ||
            new_part.getParameters().get(Constants.DDL_TIME) == null ||
            Integer.parseInt(new_part.getParameters().get(Constants.DDL_TIME)) == 0) {
            new_part.putToParameters(Constants.DDL_TIME, Long.toString(System
                    .currentTimeMillis() / 1000));
        }
        //alter partition
        if (part_vals == null || part_vals.size() == 0) {
            try {
                oldPart = ms.getPartition(dbname, name, new_part.getValues());
                ms.alterPartition(dbname, name, new_part.getValues(), new_part);
                for (MultitenantMetaStoreEventListener listener : listeners) {
                    listener.onAlterPartition(new AlterPartitionEvent(oldPart, new_part, true, this));
                }
            } catch (InvalidObjectException e) {
                throw new InvalidOperationException("alter is not possible");
            } catch (NoSuchObjectException e) {
                //old partition does not exist
                throw new InvalidOperationException("alter is not possible");
            }
            return;
        }

        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        //rename partition
        try {
            ms.openTransaction();
            try {
                oldPart = ms.getPartition(dbname, name, part_vals);
            } catch (NoSuchObjectException e) {
                // this means there is no existing partition
                throw new InvalidObjectException(
                        "Unable to rename partition because old partition does not exist");
            }
            Partition check_part = null;
            try {
                check_part = ms.getPartition(dbname, name, new_part.getValues());
            } catch (NoSuchObjectException e) {
                // this means there is no existing partition
                check_part = null;
            }
            if (check_part != null) {
                throw new AlreadyExistsException("Partition already exists:" + dbname + "." + name + "." + new_part.getValues());
            }
            tbl = ms.getTable(dbname, name);
            if (tbl == null) {
                throw new InvalidObjectException(
                        "Unable to rename partition because table or database do not exist");
            }
            try {
                destPath = new Path(wh.getTablePath(ms.getDatabase(dbname), name), Warehouse.makePartName(tbl.getPartitionKeys(),
                                                                                                          new_part.getValues()));
            } catch (NoSuchObjectException e) {
                LOG.debug(e);
                throw new InvalidOperationException(
                        "Unable to change partition or table. Database " + dbname + " does not exist"
                        + " Check metastore logs for detailed stack." + e.getMessage());
            }
            if (destPath != null) {
                newPartLoc = destPath.toString();
                oldPartLoc = oldPart.getSd().getLocation();

                srcPath = new Path(oldPartLoc);

                LOG.info("srcPath:" + oldPartLoc);
                LOG.info("descPath:" + newPartLoc);
                srcFs = wh.getFs(srcPath);
                destFs = wh.getFs(destPath);
                // check that src and dest are on the same file system
                if (srcFs != destFs) {
                    throw new InvalidOperationException("table new location " + destPath
                                                        + " is on a different file system than the old location "
                                                        + srcPath + ". This operation is not supported");
                }
                try {
                    srcFs.exists(srcPath); // check that src exists and also checks
                    if (newPartLoc.compareTo(oldPartLoc) != 0 && destFs.exists(destPath)) {
                        throw new InvalidOperationException("New location for this table "
                                                            + tbl.getDbName() + "." + tbl.getTableName()
                                                            + " already exists : " + destPath);
                    }
                } catch (IOException e) {
                    Warehouse.closeFs(srcFs);
                    Warehouse.closeFs(destFs);
                    throw new InvalidOperationException("Unable to access new location "
                                                        + destPath + " for partition " + tbl.getDbName() + "."
                                                        + tbl.getTableName() + " " + new_part.getValues());
                }
                new_part.getSd().setLocation(newPartLoc);
                ms.alterPartition(dbname, name, part_vals, new_part);
            }

            success = ms.commitTransaction();
        } finally {
            if (!success) {
                ms.rollbackTransaction();
            }
            if (success && newPartLoc.compareTo(oldPartLoc) != 0) {
                //rename the data directory
                try {
                    if (srcFs.exists(srcPath)) {
                        //if destPath's parent path doesn't exist, we should mkdir it
                        Path destParentPath = destPath.getParent();
                        if (!wh.mkdirs(destParentPath)) {
                            throw new IOException("Unable to create path " + destParentPath);
                        }
                        srcFs.rename(srcPath, destPath);
                        LOG.info("rename done!");
                    }
                } catch (IOException e) {
                    boolean revertMetaDataTransaction = false;
                    try {
                        ms.openTransaction();
                        ms.alterPartition(dbname, name, new_part.getValues(), oldPart);
                        revertMetaDataTransaction = ms.commitTransaction();
                    } catch (Exception e1) {
                        LOG.error("Reverting metadata opeation failed During HDFS operation failed", e1);
                        if (!revertMetaDataTransaction) {
                            ms.rollbackTransaction();
                        }
                    }
                    throw new InvalidOperationException("Unable to access old location "
                                                        + srcPath + " for partition " + tbl.getDbName() + "."
                                                        + tbl.getTableName() + " " + part_vals);
                }
            }
            for (MultitenantMetaStoreEventListener listener : listeners) {
                listener.onAlterPartition(new AlterPartitionEvent(oldPart, new_part, true, this));
            }
        }
    }

    public boolean create_index(Index index_def)
            throws IndexAlreadyExistsException, MetaException {
        endFunction(startFunction("create_index"), false);
        // TODO Auto-generated method stub
        throw new MetaException("Not yet implemented");
    }

    public void alter_index(final String dbname, final String base_table_name,
                            final String index_name, final Index newIndex)
            throws InvalidOperationException, MetaException {
        startFunction("alter_index", ": db=" + dbname + " base_tbl=" + base_table_name
                                     + " idx=" + index_name + " newidx=" + newIndex.getIndexName());
        newIndex.putToParameters(Constants.DDL_TIME, Long.toString(System
                .currentTimeMillis() / 1000));

        boolean success = false;
        try {
            success = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    ms.alterIndex(dbname, base_table_name, index_name, newIndex);
                    return Boolean.TRUE;
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (InvalidOperationException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("alter_index", false);
        }
        return;
    }

    public String getVersion() throws TException {
        endFunction(startFunction("getVersion"), true);
        return "3.0";
    }

    public void alter_table(final String dbname, final String name, final Table newTable)
            throws InvalidOperationException, MetaException {
        startFunction("alter_table", ": db=" + dbname + " tbl=" + name
                                     + " newtbl=" + newTable.getTableName());

        // Update the time if it hasn't been specified.
        if (newTable.getParameters() == null ||
            newTable.getParameters().get(Constants.DDL_TIME) == null) {
            newTable.putToParameters(Constants.DDL_TIME, Long.toString(System
                    .currentTimeMillis() / 1000));
        }

        final Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        boolean success = false;
        try {
            Table oldt = get_table(dbname, name);
            success = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    alterHandler.alterTable(ms, wh, dbname, name, newTable);
                    return Boolean.TRUE;
                }
            });
            for (MultitenantMetaStoreEventListener listener : listeners) {
                listener.onAlterTable(new AlterTableEvent(oldt, newTable, success, this));
            }
        } catch (MetaException e) {
            throw e;
        } catch (InvalidOperationException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            //thrown when the table to be altered does not exist
            throw new InvalidOperationException(e.getMessage());
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("alter_table", success);
        }
    }

    public List<String> get_tables(final String dbname, final String pattern)
            throws MetaException {
        startFunction("get_tables", ": db=" + dbname + " pat=" + pattern);

        List<String> ret = null;
        try {
            ret = executeWithRetry(new Command<List<String>>() {
                @Override
                public List<String> run(RawStore ms) throws Exception {
                    return ms.getTables(dbname, pattern);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_tables", ret != null);
        }
        return ret;
    }

    public List<String> get_all_tables(final String dbname) throws MetaException {
        startFunction("get_all_tables", ": db=" + dbname);

        List<String> ret = null;
        try {
            ret = executeWithRetry(new Command<List<String>>() {
                @Override
                public List<String> run(RawStore ms) throws Exception {
                    return ms.getAllTables(dbname);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_all_tables", ret != null);
        }
        return ret;
    }

    public List<FieldSchema> get_fields(String db, String tableName)
            throws MetaException, UnknownTableException, UnknownDBException {
        startFunction("get_fields", ": db=" + db + "tbl=" + tableName);
        String[] names = tableName.split("\\.");
        String base_table_name = names[0];

        Table tbl;
        List<FieldSchema> ret = null;
        try {
            try {
                tbl = get_table(db, base_table_name);
            } catch (NoSuchObjectException e) {
                throw new UnknownTableException(e.getMessage());
            }
            boolean getColsFromSerDe = SerDeUtils.shouldGetColsFromSerDe(
                    tbl.getSd().getSerdeInfo().getSerializationLib());
            if (!getColsFromSerDe) {
                ret = tbl.getSd().getCols();
            } else {
                try {
                    Deserializer s = MetaStoreUtils.getDeserializer(hiveConf, tbl);
                    ret = MetaStoreUtils.getFieldsFromDeserializer(tableName, s);
                } catch (SerDeException e) {
                    StringUtils.stringifyException(e);
                    throw new MetaException(e.getMessage());
                }
            }
        } finally {
            endFunction("get_fields", ret != null);
        }

        return ret;
    }

    /**
     * Return the schema of the table. This function includes partition columns
     * in addition to the regular columns.
     *
     * @param db        Name of the database
     * @param tableName Name of the table
     * @return List of columns, each column is a FieldSchema structure
     * @throws MetaException
     * @throws UnknownTableException
     * @throws UnknownDBException
     */
    public List<FieldSchema> get_schema(String db, String tableName)
            throws MetaException, UnknownTableException, UnknownDBException {
        startFunction("get_schema", ": db=" + db + "tbl=" + tableName);
        boolean success = false;
        try {
            String[] names = tableName.split("\\.");
            String base_table_name = names[0];

            Table tbl;
            try {
                tbl = get_table(db, base_table_name);
            } catch (NoSuchObjectException e) {
                throw new UnknownTableException(e.getMessage());
            }
            List<FieldSchema> fieldSchemas = get_fields(db, base_table_name);

            if (tbl == null || fieldSchemas == null) {
                throw new UnknownTableException(tableName + " doesn't exist");
            }

            if (tbl.getPartitionKeys() != null) {
                // Combine the column field schemas and the partition keys to create the
                // whole schema
                fieldSchemas.addAll(tbl.getPartitionKeys());
            }
            success = true;
            return fieldSchemas;
        } finally {
            endFunction("get_schema", success);
        }
    }

    public String getCpuProfile(int profileDurationInSec) throws TException {
        return "";
    }

    /**
     * Returns the value of the given configuration variable name. If the
     * configuration variable with the given name doesn't exist, or if there
     * were an exception thrown while retrieving the variable, or if name is
     * null, defaultValue is returned.
     */
    public String get_config_value(String name, String defaultValue)
            throws TException, ConfigValSecurityException {
        startFunction("get_config_value", ": name=" + name + " defaultValue="
                                          + defaultValue);
        boolean success = false;
        try {
            if (name == null) {
                success = true;
                return defaultValue;
            }
            // Allow only keys that start with hive.*, hdfs.*, mapred.* for security
            // i.e. don't allow access to db password
            if (!Pattern.matches("(hive|hdfs|mapred).*", name)) {
                throw new ConfigValSecurityException("For security reasons, the "
                                                     + "config key " + name + " cannot be accessed");
            }

            String toReturn = defaultValue;
            try {
                toReturn = hiveConf.get(name, defaultValue);
            } catch (RuntimeException e) {
                LOG.error(threadLocalId.get().toString() + ": "
                          + "RuntimeException thrown in get_config_value - msg: "
                          + e.getMessage() + " cause: " + e.getCause());
            }
            success = true;
            return toReturn;
        } finally {
            endFunction("get_config_value", success);
        }
    }

    private List<String> getPartValsFromName(RawStore ms, String dbName, String tblName,
                                             String partName)
            throws MetaException, InvalidObjectException {
        // Unescape the partition name
        LinkedHashMap<String, String> hm = Warehouse.makeSpecFromName(partName);

        // getPartition expects partition values in a list. use info from the
        // table to put the partition column values in order
        Table t = ms.getTable(dbName, tblName);
        if (t == null) {
            throw new InvalidObjectException(dbName + "." + tblName
                                             + " table not found");
        }

        List<String> partVals = new ArrayList<String>();
        for (FieldSchema field : t.getPartitionKeys()) {
            String key = field.getName();
            String val = hm.get(key);
            if (val == null) {
                throw new InvalidObjectException("incomplete partition name - missing " + key);
            }
            partVals.add(val);
        }
        return partVals;
    }

    private Partition get_partition_by_name_core(final RawStore ms, final String db_name,
                                                 final String tbl_name, final String part_name)
            throws MetaException, NoSuchObjectException, TException {
        List<String> partVals = null;
        try {
            partVals = getPartValsFromName(ms, db_name, tbl_name, part_name);
        } catch (InvalidObjectException e) {
            throw new NoSuchObjectException(e.getMessage());
        }
        Partition p = ms.getPartition(db_name, tbl_name, partVals);

        if (p == null) {
            throw new NoSuchObjectException(db_name + "." + tbl_name
                                            + " partition (" + part_name + ") not found");
        }
        return p;
    }

    public Partition get_partition_by_name(final String db_name, final String tbl_name,
                                           final String part_name)
            throws MetaException, NoSuchObjectException, TException {

        startFunction("get_partition_by_name", ": db=" + db_name + " tbl="
                                               + tbl_name + " part=" + part_name);

        Partition ret = null;

        try {
            ret = executeWithRetry(new Command<Partition>() {
                @Override
                public Partition run(RawStore ms) throws Exception {
                    return get_partition_by_name_core(ms, db_name, tbl_name, part_name);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partition_by_name", ret != null);
        }
        return ret;
    }

    public Partition append_partition_by_name(final String db_name, final String tbl_name,
                                              final String part_name) throws InvalidObjectException,
                                                                             AlreadyExistsException,
                                                                             MetaException,
                                                                             TException {
        startFunction("append_partition_by_name", ": db=" + db_name + " tbl="
                                                  + tbl_name + " part=" + part_name);

        Partition ret = null;
        try {
            ret = executeWithRetry(new Command<Partition>() {
                @Override
                public Partition run(RawStore ms) throws Exception {
                    List<String> partVals = getPartValsFromName(ms, db_name, tbl_name, part_name);
                    return append_partition_common(ms, db_name, tbl_name, partVals);
                }
            });
        } catch (InvalidObjectException e) {
            throw e;
        } catch (AlreadyExistsException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("append_partition_by_name", ret != null);
        }
        return ret;
    }

    private boolean drop_partition_by_name_core(final RawStore ms,
                                                final String db_name, final String tbl_name,
                                                final String part_name,
                                                final boolean deleteData)
            throws NoSuchObjectException,
                   MetaException, TException, IOException {

        List<String> partVals = null;
        try {
            partVals = getPartValsFromName(ms, db_name, tbl_name, part_name);
        } catch (InvalidObjectException e) {
            throw new NoSuchObjectException(e.getMessage());
        }

        return drop_partition_common(ms, db_name, tbl_name, partVals, deleteData);
    }

    @Override
    public boolean drop_partition_by_name(final String db_name, final String tbl_name,
                                          final String part_name, final boolean deleteData)
            throws NoSuchObjectException,
                   MetaException, TException {
        startFunction("drop_partition_by_name", ": db=" + db_name + " tbl="
                                                + tbl_name + " part=" + part_name);

        boolean ret = false;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    return drop_partition_by_name_core(ms, db_name, tbl_name,
                                                       part_name, deleteData);
                }
            });
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("drop_partition_by_name", ret);
        }

        return ret;
    }

    @Override
    public List<Partition> get_partitions_ps(final String db_name,
                                             final String tbl_name, final List<String> part_vals,
                                             final short max_parts)
            throws MetaException, TException {
        startPartitionFunction("get_partitions_ps", db_name, tbl_name, part_vals);

        List<Partition> ret = null;
        try {
            ret = get_partitions_ps_with_auth(db_name, tbl_name, part_vals,
                                              max_parts, null, null);
        }
        finally {
            endFunction("get_partitions_ps", ret != null);
        }

        return ret;
    }

    @Override
    public List<Partition> get_partitions_ps_with_auth(final String db_name,
                                                       final String tbl_name,
                                                       final List<String> part_vals,
                                                       final short max_parts, final String userName,
                                                       final List<String> groupNames)
            throws MetaException, TException {
        startPartitionFunction("get_partitions_ps_with_auth", db_name, tbl_name,
                               part_vals);
        List<Partition> ret = null;
        try {
            ret = executeWithRetry(new Command<List<Partition>>() {
                @Override
                public List<Partition> run(RawStore ms) throws Exception {
                    return ms.listPartitionsPsWithAuth(db_name, tbl_name, part_vals, max_parts,
                                                       userName, groupNames);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (InvalidObjectException e) {
            throw new MetaException(e.getMessage());
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partitions_ps_with_auth", ret != null);
        }
        return ret;
    }

    @Override
    public List<String> get_partition_names_ps(final String db_name,
                                               final String tbl_name, final List<String> part_vals,
                                               final short max_parts)
            throws MetaException, TException {
        startPartitionFunction("get_partitions_names_ps", db_name, tbl_name, part_vals);
        List<String> ret = null;
        try {
            ret = executeWithRetry(new Command<List<String>>() {
                @Override
                public List<String> run(RawStore ms) throws Exception {
                    return ms.listPartitionNamesPs(db_name, tbl_name, part_vals, max_parts);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partitions_names_ps", ret != null);
        }
        return ret;
    }

    @Override
    public List<String> partition_name_to_vals(String part_name)
            throws MetaException, TException {
        if (part_name.length() == 0) {
            return new ArrayList<String>();
        }
        LinkedHashMap<String, String> map = Warehouse.makeSpecFromName(part_name);
        List<String> part_vals = new ArrayList<String>();
        part_vals.addAll(map.values());
        return part_vals;
    }

    @Override
    public Map<String, String> partition_name_to_spec(String part_name) throws MetaException,
                                                                               TException {
        if (part_name.length() == 0) {
            return new HashMap<String, String>();
        }
        return Warehouse.makeSpecFromName(part_name);
    }

    @Override
    public Index add_index(final Index newIndex, final Table indexTable)
            throws InvalidObjectException, AlreadyExistsException, MetaException, TException {
        startFunction("add_index", ": db=" + newIndex.getDbName() + " tbl="
                                   + newIndex.getOrigTableName() + " index=" + newIndex.getIndexName());
        Index ret = null;
        try {
            ret = executeWithRetry(new Command<Index>() {
                @Override
                public Index run(RawStore ms) throws Exception {
                    return add_index_core(ms, newIndex, indexTable);
                }
            });
        } catch (InvalidObjectException e) {
            throw e;
        } catch (AlreadyExistsException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("add_index", ret != null);
        }
        return ret;
    }

    private Index add_index_core(final RawStore ms, final Index index, final Table indexTable)
            throws InvalidObjectException, AlreadyExistsException, MetaException {

        boolean success = false, indexTableCreated = false;

        try {
            ms.openTransaction();
            Index old_index = null;
            try {
                old_index = get_index_by_name(index.getDbName(), index
                        .getOrigTableName(), index.getIndexName());
            } catch (Exception e) {
            }
            if (old_index != null) {
                throw new AlreadyExistsException("Index already exists:" + index);
            }
            Table origTbl = ms.getTable(index.getDbName(), index.getOrigTableName());
            if (origTbl == null) {
                throw new InvalidObjectException(
                        "Unable to add index because database or the orginal table do not exist");
            }

            // set create time
            long time = System.currentTimeMillis() / 1000;
            Table indexTbl = indexTable;
            if (indexTbl != null) {
                try {
                    indexTbl = ms.getTable(index.getDbName(), index.getIndexTableName());
                } catch (Exception e) {
                }
                if (indexTbl != null) {
                    throw new InvalidObjectException(
                            "Unable to add index because index table already exists");
                }
                this.create_table(indexTable);
                indexTableCreated = true;
            }

            index.setCreateTime((int) time);
            index.putToParameters(Constants.DDL_TIME, Long.toString(time));

            ms.addIndex(index);
            success = ms.commitTransaction();
            return index;
        } finally {
            if (!success) {
                if (indexTableCreated) {
                    try {
                        this.drop_table(index.getDbName(), index.getIndexTableName(), false);
                    } catch (Exception e) {
                    }
                }
                ms.rollbackTransaction();
            }
        }
    }

    @Override
    public boolean drop_index_by_name(final String dbName, final String tblName,
                                      final String indexName, final boolean deleteData)
            throws NoSuchObjectException,
                   MetaException, TException {
        startFunction("drop_index_by_name", ": db=" + dbName + " tbl="
                                            + tblName + " index=" + indexName);

        boolean ret = false;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    return drop_index_by_name_core(ms, dbName, tblName,
                                                   indexName, deleteData);
                }
            });
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (MetaException e) {
            throw e;
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("drop_index_by_name", ret);
        }

        return ret;
    }

    private boolean drop_index_by_name_core(final RawStore ms,
                                            final String dbName, final String tblName,
                                            final String indexName, final boolean deleteData)
            throws NoSuchObjectException,
                   MetaException, TException, IOException {

        Warehouse wh = HiveContext.getCurrentContext().getWarehouse();
        boolean success = false;
        Path tblPath = null;
        try {
            ms.openTransaction();

            //drop the underlying index table
            Index index = get_index_by_name(dbName, tblName, indexName);
            if (index == null) {
                throw new NoSuchObjectException(indexName + " doesn't exist");
            }
            ms.dropIndex(dbName, tblName, indexName);

            String idxTblName = index.getIndexTableName();
            if (idxTblName != null) {
                Table tbl = null;
                tbl = this.get_table(dbName, idxTblName);
                if (tbl.getSd() == null) {
                    throw new MetaException("Table metadata is corrupted");
                }

                if (tbl.getSd().getLocation() != null) {
                    tblPath = new Path(tbl.getSd().getLocation());
                    if (!wh.isWritable(tblPath.getParent())) {
                        throw new MetaException("Index table metadata not deleted since " +
                                                tblPath.getParent() + " is not writable by " +
                                                hiveConf.getUser());
                    }
                }
                if (!ms.dropTable(dbName, idxTblName)) {
                    throw new MetaException("Unable to drop underlying data table "
                                            + idxTblName + " for index " + idxTblName);
                }
            }
            success = ms.commitTransaction();
        } finally {
            if (!success) {
                ms.rollbackTransaction();
                return false;
            } else if (deleteData && tblPath != null) {
                wh.deleteDir(tblPath, true);
                // ok even if the data is not deleted
            }
        }
        return true;
    }

    @Override
    public Index get_index_by_name(final String dbName, final String tblName,
                                   final String indexName)
            throws MetaException, NoSuchObjectException,
                   TException {

        startFunction("get_index_by_name", ": db=" + dbName + " tbl="
                                           + tblName + " index=" + indexName);

        Index ret = null;

        try {
            ret = executeWithRetry(new Command<Index>() {
                @Override
                public Index run(RawStore ms) throws Exception {
                    return get_index_by_name_core(ms, dbName, tblName, indexName);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("drop_index_by_name", ret != null);
        }
        return ret;
    }

    private Index get_index_by_name_core(final RawStore ms, final String db_name,
                                         final String tbl_name, final String index_name)
            throws MetaException, NoSuchObjectException, TException {
        Index index = ms.getIndex(db_name, tbl_name, index_name);

        if (index == null) {
            throw new NoSuchObjectException(db_name + "." + tbl_name
                                            + " index=" + index_name + " not found");
        }
        return index;
    }

    @Override
    public List<String> get_index_names(final String dbName, final String tblName,
                                        final short maxIndexes) throws MetaException, TException {
        startTableFunction("get_index_names", dbName, tblName);

        List<String> ret = null;
        try {
            ret = executeWithRetry(new Command<List<String>>() {
                @Override
                public List<String> run(RawStore ms) throws Exception {
                    return ms.listIndexNames(dbName, tblName, maxIndexes);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_index_names", ret != null);
        }
        return ret;
    }

    @Override
    public List<Index> get_indexes(final String dbName, final String tblName,
                                   final short maxIndexes)
            throws NoSuchObjectException, MetaException,
                   TException {
        startTableFunction("get_indexes", dbName, tblName);

        List<Index> ret = null;
        try {
            ret = executeWithRetry(new Command<List<Index>>() {
                @Override
                public List<Index> run(RawStore ms) throws Exception {
                    return ms.getIndexes(dbName, tblName, maxIndexes);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_indexes", ret != null);
        }
        return ret;
    }

    @Override
    public List<Partition> get_partitions_by_filter(final String dbName,
                                                    final String tblName, final String filter,
                                                    final short maxParts)
            throws MetaException, NoSuchObjectException, TException {
        startTableFunction("get_partitions_by_filter", dbName, tblName);

        List<Partition> ret = null;
        try {
            ret = executeWithRetry(new Command<List<Partition>>() {
                @Override
                public List<Partition> run(RawStore ms) throws Exception {
                    return ms.getPartitionsByFilter(dbName, tblName, filter, maxParts);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partitions_by_filter", ret != null);
        }
        return ret;
    }

    @Override
    public List<Partition> get_partitions_by_names(final String dbName,
                                                   final String tblName,
                                                   final List<String> partNames)
            throws MetaException, NoSuchObjectException, TException {

        startTableFunction("get_partitions_by_names", dbName, tblName);

        List<Partition> ret = null;
        try {
            ret = executeWithRetry(new Command<List<Partition>>() {
                @Override
                public List<Partition> run(RawStore ms) throws Exception {
                    return ms.getPartitionsByNames(dbName, tblName, partNames);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (NoSuchObjectException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        } finally {
            endFunction("get_partitions_by_names", ret != null);
        }
        return ret;
    }

    @Override
    public PrincipalPrivilegeSet get_privilege_set(HiveObjectRef hiveObject,
                                                   String userName, List<String> groupNames)
            throws MetaException,
                   TException {
        if (hiveObject.getObjectType() == HiveObjectType.COLUMN) {
            String partName = getPartName(hiveObject);
            return this.get_column_privilege_set(hiveObject.getDbName(), hiveObject
                    .getObjectName(), partName, hiveObject.getColumnName(), userName,
                                                 groupNames);
        } else if (hiveObject.getObjectType() == HiveObjectType.PARTITION) {
            String partName = getPartName(hiveObject);
            return this.get_partition_privilege_set(hiveObject.getDbName(),
                                                    hiveObject.getObjectName(), partName, userName, groupNames);
        } else if (hiveObject.getObjectType() == HiveObjectType.DATABASE) {
            return this.get_db_privilege_set(hiveObject.getDbName(), userName,
                                             groupNames);
        } else if (hiveObject.getObjectType() == HiveObjectType.TABLE) {
            return this.get_table_privilege_set(hiveObject.getDbName(), hiveObject
                    .getObjectName(), userName, groupNames);
        } else if (hiveObject.getObjectType() == HiveObjectType.GLOBAL) {
            return this.get_user_privilege_set(userName, groupNames);
        }
        return null;
    }

    private String getPartName(HiveObjectRef hiveObject) throws MetaException {
        String partName = null;
        List<String> partValue = hiveObject.getPartValues();
        if (partValue != null && partValue.size() > 0) {
            try {
                Table table = get_table(hiveObject.getDbName(), hiveObject
                        .getObjectName());
                partName = Warehouse
                        .makePartName(table.getPartitionKeys(), partValue);
            } catch (NoSuchObjectException e) {
                throw new MetaException(e.getMessage());
            }
        }
        return partName;
    }

    public PrincipalPrivilegeSet get_column_privilege_set(final String dbName,
                                                          final String tableName,
                                                          final String partName,
                                                          final String columnName,
                                                          final String userName,
                                                          final List<String> groupNames)
            throws MetaException,
                   TException {
        incrementCounter("get_column_privilege_set");

        PrincipalPrivilegeSet ret = null;
        try {
            ret = executeWithRetry(new Command<PrincipalPrivilegeSet>() {
                @Override
                public PrincipalPrivilegeSet run(RawStore ms) throws Exception {
                    return ms.getColumnPrivilegeSet(
                            dbName, tableName, partName, columnName, userName, groupNames);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public PrincipalPrivilegeSet get_db_privilege_set(final String dbName,
                                                      final String userName,
                                                      final List<String> groupNames)
            throws MetaException,
                   TException {
        incrementCounter("get_db_privilege_set");

        PrincipalPrivilegeSet ret = null;
        try {
            ret = executeWithRetry(new Command<PrincipalPrivilegeSet>() {
                @Override
                public PrincipalPrivilegeSet run(RawStore ms) throws Exception {
                    return ms.getDBPrivilegeSet(dbName, userName, groupNames);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public PrincipalPrivilegeSet get_partition_privilege_set(
            final String dbName, final String tableName, final String partName,
            final String userName, final List<String> groupNames)
            throws MetaException, TException {
        incrementCounter("get_partition_privilege_set");

        PrincipalPrivilegeSet ret = null;
        try {
            ret = executeWithRetry(new Command<PrincipalPrivilegeSet>() {
                @Override
                public PrincipalPrivilegeSet run(RawStore ms) throws Exception {
                    return ms.getPartitionPrivilegeSet(dbName, tableName, partName,
                                                       userName, groupNames);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public PrincipalPrivilegeSet get_table_privilege_set(final String dbName,
                                                         final String tableName,
                                                         final String userName,
                                                         final List<String> groupNames)
            throws MetaException, TException {
        incrementCounter("get_table_privilege_set");

        PrincipalPrivilegeSet ret = null;
        try {
            ret = executeWithRetry(new Command<PrincipalPrivilegeSet>() {
                @Override
                public PrincipalPrivilegeSet run(RawStore ms) throws Exception {
                    return ms.getTablePrivilegeSet(dbName, tableName, userName,
                                                   groupNames);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public boolean grant_role(final String roleName,
                              final String userName, final PrincipalType principalType,
                              final String grantor, final PrincipalType grantorType,
                              final boolean grantOption)
            throws MetaException, TException {
        incrementCounter("add_role_member");

        Boolean ret = null;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    Role role = ms.getRole(roleName);
                    return ms.grantRole(role, userName, principalType, grantor, grantorType, grantOption);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public List<Role> list_roles(final String principalName,
                                 final PrincipalType principalType)
            throws MetaException, TException {
        incrementCounter("list_roles");

        List<Role> ret = null;
        try {
            ret = executeWithRetry(new Command<List<Role>>() {
                @Override
                public List<Role> run(RawStore ms) throws Exception {
                    List<Role> result = new ArrayList<Role>();
                    List<MRoleMap> roleMap = ms.listRoles(principalName, principalType);
                    if (roleMap != null) {
                        for (MRoleMap role : roleMap) {
                            MRole r = role.getRole();
                            result.add(new Role(r.getRoleName(), r
                                    .getCreateTime(), r.getOwnerName()));
                        }
                    }
                    return result;
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public boolean create_role(final Role role)
            throws MetaException, TException {
        incrementCounter("create_role");

        Boolean ret = null;
        try {

            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    return ms.addRole(role.getRoleName(), role.getOwnerName());
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public boolean drop_role(final String roleName)
            throws MetaException, TException {
        incrementCounter("drop_role");

        Boolean ret = null;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    return ms.removeRole(roleName);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public List<String> get_role_names() throws MetaException, TException {
        incrementCounter("get_role_names");

        List<String> ret = null;
        try {
            ret = executeWithRetry(new Command<List<String>>() {
                @Override
                public List<String> run(RawStore ms) throws Exception {
                    return ms.listRoleNames();
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            assert (e instanceof RuntimeException);
            throw (RuntimeException) e;
        }
        return ret;
    }

    @Override
    public boolean grant_privileges(final PrivilegeBag privileges) throws MetaException,
                                                                          TException {
        incrementCounter("grant_privileges");

        Boolean ret = null;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    return ms.grantPrivileges(privileges);
                }
            });
        } catch (MetaException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public boolean revoke_role(final String roleName, final String userName,
                               final PrincipalType principalType) throws MetaException, TException {
        incrementCounter("remove_role_member");

        Boolean ret = null;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    Role mRole = ms.getRole(roleName);
                    return ms.revokeRole(mRole, userName, principalType);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public boolean revoke_privileges(final PrivilegeBag privileges)
            throws MetaException, TException {
        incrementCounter("revoke_privileges");

        Boolean ret = null;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    return ms.revokePrivileges(privileges);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public PrincipalPrivilegeSet get_user_privilege_set(final String userName,
                                                        final List<String> groupNames)
            throws MetaException, TException {
        incrementCounter("get_user_privilege_set");

        PrincipalPrivilegeSet ret = null;
        try {
            ret = executeWithRetry(new Command<PrincipalPrivilegeSet>() {
                @Override
                public PrincipalPrivilegeSet run(RawStore ms) throws Exception {
                    return ms.getUserPrivilegeSet(userName, groupNames);
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public PrincipalType getPrincipalType(String principalType) {
        return PrincipalType.valueOf(principalType);
    }

    @Override
    public List<HiveObjectPrivilege> list_privileges(String principalName,
                                                     PrincipalType principalType,
                                                     HiveObjectRef hiveObject)
            throws MetaException, TException {
        if (hiveObject.getObjectType() == HiveObjectType.GLOBAL) {
            return this.list_global_privileges(principalName, principalType);
        } else if (hiveObject.getObjectType() == HiveObjectType.DATABASE) {
            return this.list_db_privileges(principalName, principalType, hiveObject
                    .getDbName());
        } else if (hiveObject.getObjectType() == HiveObjectType.TABLE) {
            return this.list_table_privileges(principalName, principalType,
                                              hiveObject.getDbName(), hiveObject.getObjectName());
        } else if (hiveObject.getObjectType() == HiveObjectType.PARTITION) {
            return this.list_partition_privileges(principalName, principalType,
                                                  hiveObject.getDbName(), hiveObject.getObjectName(), hiveObject
                            .getPartValues());
        } else if (hiveObject.getObjectType() == HiveObjectType.COLUMN) {
            return this.list_column_privileges(principalName, principalType,
                                               hiveObject.getDbName(), hiveObject.getObjectName(), hiveObject
                            .getPartValues(), hiveObject.getColumnName());
        }
        return null;
    }

    public List<HiveObjectPrivilege> list_column_privileges(
            final String principalName, final PrincipalType principalType,
            final String dbName, final String tableName, final List<String> partValues,
            final String columnName) throws MetaException, TException {
        incrementCounter("list_security_column_grant");

        List<HiveObjectPrivilege> ret = null;
        try {
            ret = executeWithRetry(new Command<List<HiveObjectPrivilege>>() {
                @Override
                public List<HiveObjectPrivilege> run(RawStore ms) throws Exception {
                    String partName = null;
                    if (partValues != null && partValues.size() > 0) {
                        Table tbl = get_table(dbName, tableName);
                        partName = Warehouse.makePartName(tbl.getPartitionKeys(), partValues);
                    }

                    List<HiveObjectPrivilege> result = Collections.<HiveObjectPrivilege>emptyList();

                    if (partName != null) {
                        Partition part = null;
                        part = get_partition_by_name(dbName, tableName, partName);
                        List<MPartitionColumnPrivilege> mPartitionCols
                                = ms.listPrincipalPartitionColumnGrants(principalName,
                                                                        principalType, dbName, tableName, partName, columnName);
                        if (mPartitionCols.size() > 0) {
                            result = new ArrayList<HiveObjectPrivilege>();
                            for (int i = 0; i < mPartitionCols.size(); i++) {
                                MPartitionColumnPrivilege sCol = mPartitionCols.get(i);
                                HiveObjectRef objectRef = new HiveObjectRef(
                                        HiveObjectType.COLUMN, dbName, tableName,
                                        part == null ? null : part.getValues(), sCol
                                                .getColumnName());
                                HiveObjectPrivilege secObj = new HiveObjectPrivilege(objectRef,
                                                                                     sCol.getPrincipalName(), principalType,
                                                                                     new PrivilegeGrantInfo(sCol.getPrivilege(), sCol
                                                                                             .getCreateTime(), sCol.getGrantor(), PrincipalType
                                                                                             .valueOf(sCol.getGrantorType()), sCol.getGrantOption()));
                                result.add(secObj);
                            }
                        }
                    } else {
                        List<MTableColumnPrivilege> mTableCols = ms
                                .listPrincipalTableColumnGrants(principalName, principalType,
                                                                dbName, tableName, columnName);
                        if (mTableCols.size() > 0) {
                            result = new ArrayList<HiveObjectPrivilege>();
                            for (int i = 0; i < mTableCols.size(); i++) {
                                MTableColumnPrivilege sCol = mTableCols.get(i);
                                HiveObjectRef objectRef = new HiveObjectRef(
                                        HiveObjectType.COLUMN, dbName, tableName, null, sCol
                                                .getColumnName());
                                HiveObjectPrivilege secObj = new HiveObjectPrivilege(
                                        objectRef, sCol.getPrincipalName(), principalType,
                                        new PrivilegeGrantInfo(sCol.getPrivilege(), sCol
                                                .getCreateTime(), sCol.getGrantor(), PrincipalType
                                                .valueOf(sCol.getGrantorType()), sCol
                                                .getGrantOption()));
                                result.add(secObj);
                            }
                        }
                    }

                    return result;
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public List<HiveObjectPrivilege> list_db_privileges(final String principalName,
                                                        final PrincipalType principalType,
                                                        final String dbName)
            throws MetaException, TException {
        incrementCounter("list_security_db_grant");

        List<HiveObjectPrivilege> ret = null;
        try {
            ret = executeWithRetry(new Command<List<HiveObjectPrivilege>>() {
                @Override
                public List<HiveObjectPrivilege> run(RawStore ms) throws Exception {
                    List<MDBPrivilege> mDbs = ms.listPrincipalDBGrants(
                            principalName, principalType, dbName);
                    if (mDbs.size() > 0) {
                        List<HiveObjectPrivilege> result = new ArrayList<HiveObjectPrivilege>();
                        for (int i = 0; i < mDbs.size(); i++) {
                            MDBPrivilege sDB = mDbs.get(i);
                            HiveObjectRef objectRef = new HiveObjectRef(
                                    HiveObjectType.DATABASE, dbName, null, null, null);
                            HiveObjectPrivilege secObj = new HiveObjectPrivilege(objectRef,
                                                                                 sDB.getPrincipalName(), principalType,
                                                                                 new PrivilegeGrantInfo(sDB.getPrivilege(), sDB
                                                                                         .getCreateTime(), sDB.getGrantor(), PrincipalType
                                                                                         .valueOf(sDB.getGrantorType()), sDB.getGrantOption()));
                            result.add(secObj);
                        }
                        return result;
                    }
                    return Collections.<HiveObjectPrivilege>emptyList();
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public List<HiveObjectPrivilege> list_partition_privileges(
            final String principalName, final PrincipalType principalType,
            final String dbName, final String tableName, final List<String> partValues)
            throws MetaException, TException {
        incrementCounter("list_security_partition_grant");

        List<HiveObjectPrivilege> ret = null;
        try {
            ret = executeWithRetry(new Command<List<HiveObjectPrivilege>>() {
                @Override
                public List<HiveObjectPrivilege> run(RawStore ms) throws Exception {
                    Table tbl = get_table(dbName, tableName);
                    String partName = Warehouse.makePartName(tbl.getPartitionKeys(), partValues);
                    List<MPartitionPrivilege> mParts = ms.listPrincipalPartitionGrants(
                            principalName, principalType, dbName, tableName, partName);
                    if (mParts.size() > 0) {
                        List<HiveObjectPrivilege> result = new ArrayList<HiveObjectPrivilege>();
                        for (int i = 0; i < mParts.size(); i++) {
                            MPartitionPrivilege sPart = mParts.get(i);
                            HiveObjectRef objectRef = new HiveObjectRef(
                                    HiveObjectType.PARTITION, dbName, tableName, partValues,
                                    null);
                            HiveObjectPrivilege secObj = new HiveObjectPrivilege(objectRef,
                                                                                 sPart.getPrincipalName(), principalType,
                                                                                 new PrivilegeGrantInfo(sPart.getPrivilege(), sPart
                                                                                         .getCreateTime(), sPart.getGrantor(), PrincipalType
                                                                                         .valueOf(sPart.getGrantorType()), sPart
                                                                                         .getGrantOption()));

                            result.add(secObj);
                        }
                        return result;
                    }
                    return Collections.<HiveObjectPrivilege>emptyList();
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public List<HiveObjectPrivilege> list_table_privileges(
            final String principalName, final PrincipalType principalType,
            final String dbName, final String tableName) throws MetaException,
                                                                TException {
        incrementCounter("list_security_table_grant");

        List<HiveObjectPrivilege> ret = null;
        try {
            ret = executeWithRetry(new Command<List<HiveObjectPrivilege>>() {
                @Override
                public List<HiveObjectPrivilege> run(RawStore ms) throws Exception {
                    List<MTablePrivilege> mTbls = ms
                            .listAllTableGrants(principalName, principalType, dbName, tableName);
                    if (mTbls.size() > 0) {
                        List<HiveObjectPrivilege> result = new ArrayList<HiveObjectPrivilege>();
                        for (int i = 0; i < mTbls.size(); i++) {
                            MTablePrivilege sTbl = mTbls.get(i);
                            HiveObjectRef objectRef = new HiveObjectRef(
                                    HiveObjectType.TABLE, dbName, tableName, null, null);
                            HiveObjectPrivilege secObj = new HiveObjectPrivilege(objectRef,
                                                                                 sTbl.getPrincipalName(), principalType,
                                                                                 new PrivilegeGrantInfo(sTbl.getPrivilege(), sTbl.getCreateTime(), sTbl
                                                                                         .getGrantor(), PrincipalType.valueOf(sTbl
                                                                                         .getGrantorType()), sTbl.getGrantOption()));
                            result.add(secObj);
                        }
                        return result;
                    }
                    return Collections.<HiveObjectPrivilege>emptyList();
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public List<HiveObjectPrivilege> list_global_privileges(
            final String principalName, final PrincipalType principalType)
            throws MetaException, TException {
        incrementCounter("list_security_user_grant");

        List<HiveObjectPrivilege> ret = null;
        try {
            ret = executeWithRetry(new Command<List<HiveObjectPrivilege>>() {
                @Override
                public List<HiveObjectPrivilege> run(RawStore ms) throws Exception {
                    List<MGlobalPrivilege> mUsers = ms.listPrincipalGlobalGrants(
                            principalName, principalType);
                    if (mUsers.size() > 0) {
                        List<HiveObjectPrivilege> result = new ArrayList<HiveObjectPrivilege>();
                        for (int i = 0; i < mUsers.size(); i++) {
                            MGlobalPrivilege sUsr = mUsers.get(i);
                            HiveObjectRef objectRef = new HiveObjectRef(
                                    HiveObjectType.GLOBAL, null, null, null, null);
                            HiveObjectPrivilege secUser = new HiveObjectPrivilege(
                                    objectRef, sUsr.getPrincipalName(), principalType,
                                    new PrivilegeGrantInfo(sUsr.getPrivilege(), sUsr
                                            .getCreateTime(), sUsr.getGrantor(), PrincipalType
                                            .valueOf(sUsr.getGrantorType()), sUsr.getGrantOption()));
                            result.add(secUser);
                        }
                        return result;
                    }
                    return Collections.<HiveObjectPrivilege>emptyList();
                }
            });
        } catch (MetaException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    @Override
    public void cancel_delegation_token(String token_str_form)
            throws MetaException, TException {
        startFunction("cancel_delegation_token");
        boolean success = false;
        try {
            HiveMetaStore.cancelDelegationToken(token_str_form);
            success = true;
        } catch (IOException e) {
            throw new MetaException(e.getMessage());
        } finally {
            endFunction("cancel_delegation_token", success);
        }
    }

    @Override
    public long renew_delegation_token(String token_str_form)
            throws MetaException, TException {
        startFunction("renew_delegation_token");
        Long ret = null;
        try {
            ret = HiveMetaStore.renewDelegationToken(token_str_form);
        } catch (IOException e) {
            throw new MetaException(e.getMessage());
        } finally {
            endFunction("renew_delegation_token", ret != null);
        }
        return ret;
    }

    @Override
    public String get_delegation_token(String token_owner,
                                       String renewer_kerberos_principal_name)
            throws MetaException, TException {
        startFunction("get_delegation_token");
        String ret = null;
        try {
            ret =
                    HiveMetaStore.getDelegationToken(token_owner,
                                                     renewer_kerberos_principal_name);
        } catch (IOException e) {
            throw new MetaException(e.getMessage());
        } catch (InterruptedException e) {
            throw new MetaException(e.getMessage());
        } finally {
            endFunction("get_delegation_token", ret != null);
        }
        return ret;
    }

    @Override
    public void markPartitionForEvent(final String db_name, final String tbl_name,
                                      final Map<String, String> partName,
                                      final PartitionEventType evtType) throws
                                                                        MetaException, TException,
                                                                        NoSuchObjectException,
                                                                        UnknownDBException,
                                                                        UnknownTableException,
                                                                        InvalidPartitionException,
                                                                        UnknownPartitionException {

        Table tbl = null;
        try {
            startPartitionFunction("markPartitionForEvent", db_name, tbl_name, partName);
            try {
                tbl = executeWithRetry(new Command<Table>() {
                    @Override
                    public Table run(RawStore ms) throws Exception {
                        return ms.markPartitionForEvent(db_name, tbl_name, partName, evtType);
                    }
                });
            } catch (Exception original) {
                LOG.error(original);
                if (original instanceof NoSuchObjectException) {
                    throw (NoSuchObjectException) original;
                } else if (original instanceof UnknownTableException) {
                    throw (UnknownTableException) original;
                } else if (original instanceof UnknownDBException) {
                    throw (UnknownDBException) original;
                } else if (original instanceof UnknownPartitionException) {
                    throw (UnknownPartitionException) original;
                } else if (original instanceof InvalidPartitionException) {
                    throw (InvalidPartitionException) original;
                } else if (original instanceof MetaException) {
                    throw (MetaException) original;
                } else {
                    MetaException me = new MetaException(original.toString());
                    me.initCause(original);
                    throw me;
                }
            }
            if (null == tbl) {
                throw new UnknownTableException("Table: " + tbl_name + " not found.");
            } else {
                for (MultitenantMetaStoreEventListener listener : listeners) {
                    listener.onLoadPartitionDone(new LoadPartitionDoneEvent(true, this, tbl, partName));
                }
            }
        }
        finally {
            endFunction("markPartitionForEvent", tbl != null);
        }
    }

    @Override
    public boolean isPartitionMarkedForEvent(final String db_name, final String tbl_name,
                                             final Map<String, String> partName,
                                             final PartitionEventType evtType) throws
                                                                               MetaException,
                                                                               NoSuchObjectException,
                                                                               UnknownDBException,
                                                                               UnknownTableException,
                                                                               TException,
                                                                               UnknownPartitionException,
                                                                               InvalidPartitionException {

        startPartitionFunction("isPartitionMarkedForEvent", db_name, tbl_name, partName);
        Boolean ret = null;
        try {
            ret = executeWithRetry(new Command<Boolean>() {
                @Override
                public Boolean run(RawStore ms) throws Exception {
                    return ms.isPartitionMarkedForEvent(db_name, tbl_name, partName, evtType);
                }

            });
        } catch (Exception original) {
            LOG.error(original);
            if (original instanceof NoSuchObjectException) {
                throw (NoSuchObjectException) original;
            } else if (original instanceof UnknownTableException) {
                throw (UnknownTableException) original;
            } else if (original instanceof UnknownDBException) {
                throw (UnknownDBException) original;
            } else if (original instanceof UnknownPartitionException) {
                throw (UnknownPartitionException) original;
            } else if (original instanceof InvalidPartitionException) {
                throw (InvalidPartitionException) original;
            } else if (original instanceof MetaException) {
                throw (MetaException) original;
            } else {
                MetaException me = new MetaException(original.toString());
                me.initCause(original);
                throw me;
            }
        }
        finally {
            endFunction("isPartitionMarkedForEvent", ret != null);
        }

        return ret;
    }

    @Override
    public List<String> set_ugi(String username, List<String> groupNames) throws MetaException,
                                                                                 TException {
        Collections.addAll(groupNames, username);
        return groupNames;
    }
}
