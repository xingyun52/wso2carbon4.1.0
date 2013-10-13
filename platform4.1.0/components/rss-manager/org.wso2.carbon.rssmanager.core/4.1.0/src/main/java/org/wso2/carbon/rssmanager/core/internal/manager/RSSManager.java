package org.wso2.carbon.rssmanager.core.internal.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.rssmanager.common.RSSManagerConstants;
import org.wso2.carbon.rssmanager.core.RSSManagerException;
import org.wso2.carbon.rssmanager.core.RSSTransactionManager;
import org.wso2.carbon.rssmanager.core.entity.*;
import org.wso2.carbon.rssmanager.core.internal.RSSInstancePool;
import org.wso2.carbon.rssmanager.core.internal.dao.RSSDAO;
import org.wso2.carbon.rssmanager.core.internal.dao.RSSDAOFactory;
import org.wso2.carbon.rssmanager.core.internal.util.RSSManagerUtil;

import javax.sql.XAConnection;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public abstract class RSSManager {

    private RSSInstancePool rssInstancePool;
    private RSSTransactionManager txManager;
    private RSSDAO dao = RSSDAOFactory.getRSSDAO();
    private static final Log log = LogFactory.getLog(RSSManager.class);

    /**
     * Thread local variable to track the status of active nested transactions
     */
    private static ThreadLocal<Integer> activeNestedTransactions = new ThreadLocal<Integer>() {
        protected synchronized Integer initialValue() {
            return 0;
        }
    };

    /**
     * This is used to keep the enlisted XADatasource objects
     */
    private static ThreadLocal<Set<XAResource>> enlistedXADataSources = new ThreadLocal<Set<XAResource>>() {
        protected Set<XAResource> initialValue() {
            return new HashSet<XAResource>();
        }
    };

    public RSSManager() {
        this.init();
        this.rssInstancePool = new RSSInstancePool();
    }

    public RSSInstancePool getRSSInstancePool() {
        return rssInstancePool;
    }

    public abstract void createDatabase(Database database) throws RSSManagerException;

    public abstract void dropDatabase(String rssInstanceName, String databaseName) throws
            RSSManagerException;

    public abstract void createDatabaseUser(DatabaseUser databaseUser) throws RSSManagerException;

    public abstract void dropDatabaseUser(String rssInstanceName, String username) throws
            RSSManagerException;

    public abstract void editDatabaseUserPrivileges(DatabasePrivilegeSet privileges,
                                                    DatabaseUser databaseUser,
                                                    String databaseName) throws RSSManagerException;

    public abstract void attachUserToDatabase(String rssInstanceName, String databaseName,
                                              String username, String templateName) throws
            RSSManagerException;

    public abstract void detachUserFromDatabase(String rssInstanceName, String databaseName,
                                                String username) throws RSSManagerException;

    public void createRSSInstance(RSSInstance rssInstance) throws RSSManagerException {
        try {
            this.beginTransaction();
            rssInstance.setTenantId(this.getCurrentTenantId());
            this.getDAO().createRSSInstance(rssInstance);
            this.endTransaction();
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public void dropRSSInstance(String rssInstanceName) throws RSSManagerException {
        try {
            this.beginTransaction();

            RSSInstance rssInstance = this.getDAO().getRSSInstance(rssInstanceName);
            DataSource dataSource = (DataSource) rssInstance.getDataSource();
            if (dataSource != null) {
                dataSource.close();
            }
            this.getDAO().dropRSSInstance(rssInstanceName, this.getCurrentTenantId());
            //TODO : Drop dependent databases etc.
            this.endTransaction();
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public void editRSSInstanceConfiguration(RSSInstance rssInstance) throws RSSManagerException {
        try {
            this.beginTransaction();
            rssInstance.setTenantId(this.getCurrentTenantId());
            this.getDAO().updateRSSInstance(rssInstance);
            this.endTransaction();
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public List<RSSInstanceMetaData> getRSSInstances(int tid) throws RSSManagerException {
        try {
            this.beginTransaction();

            List<RSSInstance> tmpList = this.getDAO().getAllRSSInstances(tid);
            List<RSSInstanceMetaData> rssInstances = new ArrayList<RSSInstanceMetaData>();
            for (RSSInstance tmpIns : tmpList) {
                RSSInstanceMetaData rssIns = RSSManagerUtil.convertRSSInstanceToMetadata(tmpIns);
                rssInstances.add(rssIns);
            }
            this.endTransaction();
            return rssInstances;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public List<DatabaseMetaData> getDatabases(int tid) throws RSSManagerException {
        List<DatabaseMetaData> databases = new ArrayList<DatabaseMetaData>();
        try {
            this.beginTransaction();

            List<Database> tmpList = this.getDAO().getAllDatabases(tid);
            for (Database database : tmpList) {
                DatabaseMetaData metadata = RSSManagerUtil.convertDatabaseToMetadata(database);
                databases.add(metadata);
            }
            this.endTransaction();
            return databases;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public List<DatabaseUserMetaData> getDatabaseUsers(int tid) throws RSSManagerException {
        List<DatabaseUserMetaData> users = new ArrayList<DatabaseUserMetaData>();
        try {
            this.beginTransaction();

            List<DatabaseUser> tmpList = this.getDAO().getAllDatabaseUsers(tid);
            for (DatabaseUser tmpUser : tmpList) {
                DatabaseUserMetaData user = RSSManagerUtil.convertToDatabaseUserMetadata(tmpUser);
                users.add(user);
            }
            this.endTransaction();
            return users;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public RSSInstance getRoundRobinAssignedDatabaseServer() throws
            RSSManagerException {
        RSSInstance rssIns = null;
        try {
            this.beginTransaction();

            List<RSSInstance> rdsInstances = this.getDAO().getAllSystemRSSInstances();
            int count = this.getDAO().getSystemRSSDatabaseCount();

            for (int i = 0; i < rdsInstances.size(); i++) {
                if (i == count % rdsInstances.size()) {
                    rssIns = rdsInstances.get(i);
                    if (rssIns != null) {
                        return rssIns;
                    }
                }
            }
            this.endTransaction();
            return rssIns;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public void createDatabasePrivilegesTemplate(
            DatabasePrivilegeTemplate template) throws RSSManagerException {
        try {
            this.beginTransaction();

            if (template == null) {
                this.rollbackTransaction();
                throw new RSSManagerException("Database privilege template information " +
                        "cannot be null");
            }

            boolean isExist = this.getDAO().isDatabasePrivilegeTemplateExist(template.getName());
            if (isExist) {
                this.rollbackTransaction();
                throw new RSSManagerException("A database privilege template named '" +
                        template.getName() + "' already exists");
            }

            template = this.getDAO().createDatabasePrivilegesTemplate(template);
            this.getDAO().setDatabasePrivilegeTemplateProperties(template);
            this.endTransaction();
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public void editDatabasePrivilegesTemplate(DatabasePrivilegeTemplate template) throws
            RSSManagerException {
        try {
            this.beginTransaction();

            if (template == null) {
                this.rollbackTransaction();
                throw new RSSManagerException("Database privilege template information " +
                        "cannot be null");
            }

            template.setTenantId(this.getCurrentTenantId());
            this.getDAO().editDatabasePrivilegesTemplate(template);

            this.endTransaction();
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public RSSInstance getRSSInstance(String rssInstanceName) throws RSSManagerException {
        try {
            this.beginTransaction();
            RSSInstance rssInstance = this.getDAO().getRSSInstance(rssInstanceName);
            this.endTransaction();
            return rssInstance;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public Database getDatabase(String rssInstanceName,
                                String databaseName) throws RSSManagerException {
        Database database;
        try {
            this.beginTransaction();

            boolean isExist = this.getDAO().isDatabaseExist(rssInstanceName, databaseName);
            if (isExist) {
                this.rollbackTransaction();
                throw new RSSManagerException("A database named '" + databaseName +
                        "' already exists in RSS instance ' " + rssInstanceName + "'");
            }
            RSSInstance rssInstance =
                    this.getDAO().findRSSInstanceDatabaseBelongsTo(rssInstanceName, databaseName);
            if (rssInstance == null) {
                this.rollbackTransaction();
                throw new RSSManagerException("Database '" + databaseName + "' does not exist " +
                        "in RSS instance '" + rssInstanceName + "'");
            }
            database = this.getDAO().getDatabase(rssInstance, databaseName);

            this.endTransaction();
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
        return database;
    }

    public boolean isDatabaseExist(String rssInstanceName, String databaseName) throws
            RSSManagerException {
        boolean isExists;
        try {
            this.beginTransaction();
            isExists = this.getDAO().isDatabaseExist(rssInstanceName, databaseName);
            this.endTransaction();
            
            return isExists;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public boolean isDatabaseUserExist(String rssInstanceName, String databaseUsername) throws
            RSSManagerException {
        boolean isExists;
        try {
            this.beginTransaction();
            isExists = this.getDAO().isDatabaseUserExist(rssInstanceName, databaseUsername);
            this.endTransaction();

            return isExists;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public boolean isDatabasePrivilegeTemplateExist(String templateName) throws
            RSSManagerException {
        boolean isExist;
        try {
            this.beginTransaction();
            isExist = this.getDAO().isDatabasePrivilegeTemplateExist(templateName);
            this.endTransaction();
            return isExist;
        } catch(RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public DatabaseUser getDatabaseUser(String rssInstanceName,
                                        String username) throws RSSManagerException {
        DatabaseUser user;
        try {
            this.beginTransaction();

            boolean isExist = this.getDAO().isDatabaseUserExist(rssInstanceName, username);
            if (isExist) {
                this.rollbackTransaction();
                throw new RSSManagerException("Database user '" + username + "' already exists " +
                        "in the RSS instance '" + rssInstanceName + "'");
            }

            RSSInstance rssInstance =
                    this.getDAO().findRSSInstanceDatabaseUserBelongsTo(rssInstanceName, username);
            if (rssInstance == null) {
                this.rollbackTransaction();
                throw new RSSManagerException("Database user '" + username + "' does not exist " +
                        "in RSS instance '" + rssInstanceName + "'");
            }
            user = this.getDAO().getDatabaseUser(rssInstance, username);

            this.endTransaction();
            return user;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public void dropDatabasePrivilegesTemplate(String templateName) throws RSSManagerException {
        try {
            this.beginTransaction();

            this.getDAO().removeDatabasePrivilegesTemplateEntries(templateName,
                    this.getCurrentTenantId());
            this.getDAO().dropDatabasePrivilegesTemplate(templateName);

            this.endTransaction();
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public List<DatabasePrivilegeTemplate> getDatabasePrivilegeTemplates() throws
            RSSManagerException {
        try {
            this.beginTransaction();

            List<DatabasePrivilegeTemplate> templates =
                    this.getDAO().getAllDatabasePrivilegesTemplates(this.getCurrentTenantId());

            this.endTransaction();
            return templates;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public DatabasePrivilegeTemplate getDatabasePrivilegeTemplate(
            String templateName) throws RSSManagerException {
        try {
            this.beginTransaction();
            DatabasePrivilegeTemplate template =
                    this.getDAO().getDatabasePrivilegesTemplate(templateName);
            this.endTransaction();
            return template;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public int getSystemRSSInstanceCount() throws RSSManagerException {
        try {
            this.beginTransaction();
            int count = this.getDAO().getAllSystemRSSInstances().size();
            this.endTransaction();
            return count;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public List<String> getUsersAttachedToDatabase(
            String rssInstanceName, String databaseName) throws RSSManagerException {
        try {
            this.beginTransaction();

            RSSInstance rssInstance =
                    this.getDAO().findRSSInstanceDatabaseBelongsTo(rssInstanceName, databaseName);
            if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) {
                return this.getDAO().getSystemUsersAssignedToDatabase(rssInstance, databaseName);
            }
            List<String> existingUsers =
                    this.getDAO().getUsersAssignedToDatabase(rssInstance, databaseName);

            this.endTransaction();
            return existingUsers;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public List<String> getAvailableUsersToAttachToDatabase(
            String rssInstanceName, String databaseName) throws RSSManagerException {
        try {
            this.beginTransaction();

            RSSInstance rssInstance =
                    this.getDAO().findRSSInstanceDatabaseBelongsTo(rssInstanceName, databaseName);
            List<String> availableUsers = new ArrayList<String>();

            List<String> existingUsers;
            if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) {
                existingUsers = this.getDAO().getSystemUsersAssignedToDatabase(rssInstance, databaseName);
            } else {
                existingUsers = this.getDAO().getUsersAssignedToDatabase(rssInstance, databaseName);
            }

//            List<String> existingUsers =
//                    this.getUsersAttachedToDatabase(rssInstanceName, databaseName);
            if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) {
                for (DatabaseUser user : this.getDAO().getSystemCreatedDatabaseUsers()) {
                    String username = user.getUsername();
                    if (!existingUsers.contains(username)) {
                        availableUsers.add(username);
                    }
                }
            } else {
                for (DatabaseUser user : this.getDAO().getUsersByRSSInstance(rssInstance)) {
                    String username = user.getUsername();
                    if (!existingUsers.contains(username)) {
                        availableUsers.add(username);
                    }
                }
            }

            this.endTransaction();
            return availableUsers;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    public DatabasePrivilegeSet getUserDatabasePrivileges(String rssInstanceName,
                                                          String databaseName,
                                                          String username) throws RSSManagerException {
        try {
            this.beginTransaction();

            RSSInstance rssInstance =
                    this.getDAO().findRSSInstanceDatabaseBelongsTo(rssInstanceName, databaseName);
            if (rssInstance == null) {
                this.rollbackTransaction();
                throw new RSSManagerException("Database '" + databaseName + "' does not exist " +
                        "in RSS instance '" + rssInstanceName + "'");
            }
            if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) {
                return this.getDAO().getSystemUserDatabasePrivileges(rssInstance,
                        databaseName, username);
            }
            DatabasePrivilegeSet privileges =
                    this.getDAO().getUserDatabasePrivileges(rssInstance, databaseName, username);

            this.endTransaction();
            return privileges;
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    private void init() {
        TransactionManager txMgr = RSSManagerUtil.getTransactionManager();
        this.txManager = new RSSTransactionManager(txMgr);
    }

    public RSSTransactionManager getRSSTransactionManager() {
        return txManager;
    }

    public boolean isInTransaction() {
        return activeNestedTransactions.get() > 0;
    }

    public void beginTransaction() throws RSSManagerException {
        if (log.isDebugEnabled()) {
            log.debug("beginTransaction()");
        }
        if (activeNestedTransactions.get() == 0) {
            this.getRSSTransactionManager().begin();
        }
        activeNestedTransactions.set(activeNestedTransactions.get() + 1);
    }

    public void endTransaction() throws RSSManagerException {
        if (log.isDebugEnabled()) {
            log.debug("endTransaction()");
        }
        activeNestedTransactions.set(activeNestedTransactions.get() - 1);
        /* commit all only if we are at the outer most transaction */
        if (activeNestedTransactions.get() == 0) {
            this.getRSSTransactionManager().commit();
        } else if (activeNestedTransactions.get() < 0) {
            activeNestedTransactions.set(0);
        }
    }

    public void rollbackTransaction() throws RSSManagerException {
        if (log.isDebugEnabled()) {
            log.debug("rollbackTransaction()");
        }
        if (log.isDebugEnabled()) {
            log.debug("this.getRSSTxManager().rollback()");
        }
        this.getRSSTransactionManager().rollback();
        activeNestedTransactions.set(0);
    }

    public Connection createConnection(javax.sql.DataSource dataSource) throws RSSManagerException {
        Connection conn;
        try {
            conn = dataSource.getConnection();
            if (conn instanceof XAConnection && isInTransaction()) {
                Transaction tx =
                        this.getRSSTransactionManager().getTransactionManager().getTransaction();
                XAResource xaRes = ((XAConnection) conn).getXAResource();
                if (!isXAResourceEnlisted(xaRes)) {
                    tx.enlistResource(xaRes);
                    addToEnlistedXADataSources(xaRes);
                }
            }
            return conn;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while creating datasource connection", e);
        } catch (SystemException e) {
            throw new RSSManagerException("Error occurred while creating datasource connection", e);
        } catch (RollbackException e) {
            throw new RSSManagerException("Error occurred while creating datasource connection", e);
        }
    }

    /**
     * This method adds XAResource object to enlistedXADataSources Threadlocal set
     *
     * @param resource XA resource associated with the connection
     */
    private void addToEnlistedXADataSources(XAResource resource) {
        enlistedXADataSources.get().add(resource);
    }

    private boolean isXAResourceEnlisted(XAResource resource) {
        return enlistedXADataSources.get().contains(resource);
    }

    protected int getCurrentTenantId() {
        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }

    protected RSSDAO getDAO() {
        return dao;
    }

}
