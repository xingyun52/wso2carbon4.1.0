/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.bpel.core.ode.integration.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.iapi.ProcessConf;
import org.apache.ode.bpel.iapi.Scheduler;
import org.apache.ode.il.config.OdeConfigProperties;
import org.apache.ode.utils.CronExpression;
import org.apache.xmlbeans.XmlException;
import org.wso2.carbon.bpel.config.*;
import org.wso2.carbon.bpel.core.BPELConstants;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * The class which represents the BPEL configuration file, bps.xml
 */
public class BPELServerConfiguration {
    private static final Log log = LogFactory.getLog(BPELServerConfiguration.class);

    private WSO2BPSDocument bpsConfigDocument;

    public static enum DataSourceType {
        EMBEDDED,
        EXTERNAL
    }

    private DataSourceType dsType = DataSourceType.EMBEDDED;

    // If data source type is external following three fields must be not null
    // Name of the data source created in the JNDI repo
    private String dataSourceName;

    private String dataSourceJNDIRepoInitialContextFactory;

    private String dataSourceJNDIRepoProviderURL;

    private int processDehydrationMaxAge;

    private boolean isProcessDehydrationEnabled = false;

    private int processDehydraionMaxCount;

    private String transactionFactoryClass = "org.apache.ode.il.EmbeddedGeronimoFactory";

    private List<String> eventListeners = new ArrayList<String>();

    private List<String> mexInterceptors = new ArrayList<String>();

    private List<String> extensionBundleRuntimes = new ArrayList<String>();

    private List<String> extensionCorrelationFilters = new ArrayList<String>();

    private List<String> extensionBundleValidators = new ArrayList<String>();

    private Map<String, String> openJpaProperties = new HashMap<String, String>();

    // Message exchange timeout in milliseconds
    private int mexTimeOut = BPELConstants.DEFAULT_TIMEOUT;

    // External Service timeout in milliseconds
    private int externalServiceTimeOut = BPELConstants.DEFAULT_TIMEOUT;

    private int maxConnectionsPerHost = 10;

    private int maxTotalConnections = 100;

//    // Life time in days
//    private int completedInstanceLifeTime = 3;
//
//    // Life time in days
//    private int failedInstanceLifeTime = 5;

    // Use Debug on transaction manager or not
    private boolean debugOnTransactionManager = false;

    private boolean syncWithRegistry = false;

    private long inMemoryInstanceTTL = 600000;

    private int odeSchedulerThreadPoolSize = 0;

    public BPELServerConfiguration() {
        if (log.isDebugEnabled()) {
            log.debug("Loading bps configuration....");
        }

        populateDefaultOpenJPAProps();
        loadBPELServerConfigurationFile();
    }

    public DataSourceType getDsType() {
        return dsType;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getDataSourceJNDIRepoInitialContextFactory() {
        return dataSourceJNDIRepoInitialContextFactory;
    }

    public String getDataSourceJNDIRepoProviderURL() {
        return dataSourceJNDIRepoProviderURL;
    }

    public int getProcessDehydrationMaxAge() {
        return processDehydrationMaxAge;
    }

    public boolean isProcessDehydrationEnabled() {
        return isProcessDehydrationEnabled;
    }

    public int getProcessDehydraionMaxCount() {
        return processDehydraionMaxCount;
    }

    public String getTransactionFactoryClass() {
        return transactionFactoryClass;
    }

    public List<String> getEventListeners() {
        return eventListeners;
    }

    public List<String> getMexInterceptors() {
        return mexInterceptors;
    }

    public List<String> getExtensionBundleRuntimes() {
        return extensionBundleRuntimes;
    }

    public List<String> getExtensionCorrelationFilters() {
        return extensionCorrelationFilters;
    }

//    public List<String> getExtensionBundleValidators() {
//        return extensionBundleValidators;
//    }

    public Map<String, String> getOpenJpaProperties() {
        return openJpaProperties;
    }

    public int getMexTimeOut() {
        return mexTimeOut;
    }

    public int getExternalServiceTimeOut() {
        return externalServiceTimeOut;
    }

    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

//    public int getCompletedInstanceLifeTime() {
//        return completedInstanceLifeTime;
//    }
//
//    public int getFailedInstanceLifeTime() {
//        return failedInstanceLifeTime;
//    }

    public boolean isDebugOnTransactionManager() {
        return debugOnTransactionManager;
    }

    public List<ProcessConf.CronJob> getSystemCleanupCronJobs() {
        List<ProcessConf.CronJob> jobs = new ArrayList<ProcessConf.CronJob>();
        TSchedules schedules = bpsConfigDocument.getWSO2BPS().getSchedules();

        if (schedules != null &&
                schedules.getScheduleArray() != null &&
                schedules.getScheduleArray().length > 0) {
            for (org.wso2.carbon.bpel.config.TSchedule schedule : schedules.getScheduleArray()) {
                ProcessConf.CronJob job = new ProcessConf.CronJob();

                try {
                    job.setCronExpression(new CronExpression(schedule.getWhen()));
                    for (final TCleanup aCleanup : schedule.getCleanupArray()) {
                        ProcessConf.CleanupInfo cleanupInfo = new ProcessConf.CleanupInfo();
                        assert !(aCleanup.getFilterArray().length == 0);
                        cleanupInfo.setFilters(Arrays.asList(aCleanup.getFilterArray()));
                        processACleanup(cleanupInfo.getCategories(),
                                Arrays.asList(aCleanup.getCategoryArray()));

                        Scheduler.JobDetails runnableDetails = new Scheduler.JobDetails();

                        runnableDetails.getDetailsExt().put(BPELConstants.ODE_DETAILS_EXT_CLEAN_UP_INFO,
                                cleanupInfo);
                        runnableDetails.getDetailsExt().put(BPELConstants.ODE_DETAILS_EXT_TRANSACTION_SIZE, 10);
                        job.getRunnableDetailList().add(runnableDetails);

                        log.info("SYSTEM CRON configuration added a runtime data cleanup: " +
                                runnableDetails);
                    }
                    jobs.add(job);
                } catch (ParseException e) {
                    log.error("Exception during parsing the schedule cron expression: " +
                            schedule.getWhen() + ", skipped the scheduled job.");
                }
            }
        }

        return jobs;
    }

    public static void processACleanup(Set<ProcessConf.CLEANUP_CATEGORY> categories,
                                       List<TCleanup.Category.Enum> categoryList) {
        if (categoryList.isEmpty()) {
            // add all categories
            categories.addAll(EnumSet.allOf(ProcessConf.CLEANUP_CATEGORY.class));
        } else {
            for (TCleanup.Category.Enum aCategory : categoryList) {
                if (aCategory == TCleanup.Category.ALL) {
                    // add all categories
                    categories.addAll(EnumSet.allOf(ProcessConf.CLEANUP_CATEGORY.class));
                } else {
                    categories.add(ProcessConf.CLEANUP_CATEGORY.fromString(aCategory.toString()));
                }
            }
        }
    }


    /**
     * Make the BPEL Configuration file ODE readable
     *
     * @return Properties object which is expected from ODE environment as configuration
     */
    public Properties toODEConfigProperties() {
        Properties odeConfig = new Properties();

        odeConfig.setProperty(addPrefix(OdeConfigProperties.PROP_DB_MODE), dsType.toString());
        if (dsType == DataSourceType.EXTERNAL) {
            odeConfig.setProperty(addPrefix(OdeConfigProperties.PROP_DB_EXTERNAL_DS),
                    dataSourceName);
            if (dataSourceJNDIRepoInitialContextFactory != null) {
                odeConfig.setProperty(addPrefix(BPELConstants.PROP_DB_EXTERNAL_JNDI_CTX_FAC),
                        dataSourceJNDIRepoInitialContextFactory);
            }
            if (dataSourceJNDIRepoProviderURL != null) {
                odeConfig.setProperty(addPrefix(BPELConstants.PROP_DB_EXTERNAL_JNDI_PROVIDER_URL),
                        dataSourceJNDIRepoProviderURL);
            }
        }

        if (transactionFactoryClass != null) {
            odeConfig.setProperty(addPrefix(OdeConfigProperties.PROP_TX_FACTORY_CLASS),
                    transactionFactoryClass);
        }

        if (openJpaProperties.size() > 0) {
            for (String key : openJpaProperties.keySet()) {
                odeConfig.setProperty(key, openJpaProperties.get(key));
            }
        }

        boolean acquireTransactionLocks = false;
        odeConfig.setProperty(addPrefix(BPELConstants.ODE_ACQUIRE_TRANSACTION_LOCKS),
                Boolean.toString(acquireTransactionLocks));

        odeConfig.setProperty(addPrefix(OdeConfigProperties.PROP_MEX_INMEM_TTL),
                Long.toString(inMemoryInstanceTTL));

        odeConfig.setProperty(addPrefix(OdeConfigProperties.PROP_THREAD_POOL_SIZE),
                Integer.toString(odeSchedulerThreadPoolSize));

        return odeConfig;
    }

    private void populateDefaultOpenJPAProps() {
        openJpaProperties.put(BPELConstants.OPENJPA_FLUSH_BEFORE_QUERIES, "true");
    }

    private void loadBPELServerConfigurationFile() {
        String carbonConfigDirPath = CarbonUtils.getCarbonConfigDirPath();
        String bpsConfigPath = carbonConfigDirPath + File.separator +
                BPELConstants.BPEL_CONFIGURATION_FILE_NAME;
        File bpsConfigFile = new File(bpsConfigPath);

        if (!bpsConfigFile.exists()) {
            log.warn("Cannot find BPEL configuration file: " + bpsConfigPath +
                    " Default values are used.");
            return;
        }

        try {
            this.bpsConfigDocument = WSO2BPSDocument.Factory.parse(bpsConfigFile);
            populateFields();
        } catch (XmlException e) {
            String errMsg = "BPS configuration parsing exception. " +
                    BPELConstants.BPEL_CONFIGURATION_FILE_NAME + " can be found at " +
                    bpsConfigPath;
            log.error(errMsg);
            throw new RuntimeException(errMsg, e);
        } catch (IOException e) {
            String errMsg = "Error reading bps configuration file." +
                    BPELConstants.BPEL_CONFIGURATION_FILE_NAME + " can be found at " +
                    bpsConfigPath;
            log.error(errMsg);
            throw new RuntimeException(errMsg, e);
        }
    }

    private void populateFields() {
        populateDataSourceConfigFields();
        populateProcessDehydrationField();
        populateTransactionFactoryField();
        populateEventListeners();
        populateMexInterceptors();
        populateExtensionBundleFields();
        populateOpenJPAProps();
        populateMexTimeoutField();
        populateExternalServiceTimeOut();
        populateHttpConnectionManagerProperties();
        populateDebugOnTransactionManagerProp();
        populateSyncWithRegistry();

        populateInMemoryInstanceTTL();
        populateOdeSchedulerThreadPoolSize();
    }

    private void populateSyncWithRegistry() {
        if (bpsConfigDocument.getWSO2BPS().isSetSyncWithRegistry()) {
            syncWithRegistry = bpsConfigDocument.getWSO2BPS().getSyncWithRegistry();
        }
    }

    private void populateDataSourceConfigFields() {
        TDataBaseConfig databaseConfig = bpsConfigDocument.getWSO2BPS().getDataBaseConfig();
        if (databaseConfig != null) {
            // Now we do not have concept called EMBEDDED. All the DBs are configured as EXTERNAL.
            // This way users can modify the default db config as well. And also support the
            // -Dsetup
            dsType = DataSourceType.EXTERNAL;
            if (databaseConfig.getDataSource().getName() != null &&
                    databaseConfig.getDataSource().getName().length() > 0) {
                dataSourceName = databaseConfig.getDataSource().getName();
            } else {
                throw new RuntimeException("Data Source name cannot be null, " +
                        "when data source mode is external.");
            }

            if (databaseConfig.getDataSource().isSetJNDI()) {
                TDataBaseConfig.DataSource.JNDI jndiConfig = databaseConfig.getDataSource().getJNDI();
                if (jndiConfig.getContextFactory() != null &&
                        jndiConfig.getContextFactory().length() > 0 &&
                        jndiConfig.getProviderURL() != null &&
                        jndiConfig.getProviderURL().length() > 0) {
                    dataSourceJNDIRepoInitialContextFactory = jndiConfig.getContextFactory().trim();
                    dataSourceJNDIRepoProviderURL = jndiConfig.getProviderURL().trim();

                    // Read Port Offset
                    int portOffset = readPortOffset();
                    //applying port offset operation
                    String urlWithoutPort = dataSourceJNDIRepoProviderURL.substring(0,
                            dataSourceJNDIRepoProviderURL.lastIndexOf(':') + 1);
                    int dataSourceJNDIRepoProviderPort = Integer.parseInt(
                            dataSourceJNDIRepoProviderURL.substring(urlWithoutPort.length())) + portOffset;
                    dataSourceJNDIRepoProviderURL = urlWithoutPort + dataSourceJNDIRepoProviderPort;
                }
            }
        }
    }

    private int readPortOffset() {
        String offSet =
                CarbonUtils.getServerConfiguration().getFirstProperty(BPELConstants.PORTS_OFFSET);
        try {
            return ((offSet != null) ? Integer.parseInt(offSet.trim()) : 0);
        } catch (NumberFormatException e) {
            log.warn("Incorrect port offset: " + offSet + " resetting offset to 0");
            return 0;
        }
    }

    private void populateProcessDehydrationField() {
        TProcessDehydration processDehydrationConfig = bpsConfigDocument.getWSO2BPS().
                getProcessDehydration();
        if (processDehydrationConfig != null) {
            isProcessDehydrationEnabled = processDehydrationConfig.getValue();

            if (processDehydrationConfig.getMaxAge() != null) {
                processDehydrationMaxAge = processDehydrationConfig.getMaxAge().getValue();
            }

            processDehydraionMaxCount = processDehydrationConfig.getMaxCount();
        }

    }

    private void populateTransactionFactoryField() {
        TBPS.TransactionFactory tfClass = bpsConfigDocument.getWSO2BPS().getTransactionFactory();
        if (tfClass != null && tfClass.getClass1() != null && tfClass.getClass1().length() > 0) {
            transactionFactoryClass = tfClass.getClass1();
        }
    }

    private void populateEventListeners() {
        TEventListeners eventListenerList = bpsConfigDocument.getWSO2BPS().getEventListeners();
        if (eventListenerList != null) {
            for (TEventListeners.Listener listener : eventListenerList.getListenerArray()) {
                this.eventListeners.add(listener.getClass1());
            }
        }
    }

    private void populateMexInterceptors() {
        TMexInterceptors mexInterceptorList = bpsConfigDocument.getWSO2BPS().getMexInterceptors();
        if (mexInterceptorList != null) {
            for (TMexInterceptors.Interceptor interceptor : mexInterceptorList.getInterceptorArray()) {
                this.mexInterceptors.add(interceptor.getClass1());
            }
        }
    }

    private void populateExtensionBundleFields() {
        TExtensionBundles extensionBundles = bpsConfigDocument.getWSO2BPS().getExtensionBundles();
        if (extensionBundles != null) {
            if (extensionBundles.getRuntimes() != null) {
                for (TExtensionBundles.Runtimes.Runtime runtime : extensionBundles.getRuntimes().
                        getRuntimeArray()) {
                    this.extensionBundleRuntimes.add(runtime.getClass1());
                }
            }

            if (extensionBundles.getFilters() != null) {
                for (TExtensionBundles.Filters.Filter filter :
                        extensionBundles.getFilters().getFilterArray()) {
                    this.extensionCorrelationFilters.add(filter.getClass1());
                }
            }

            if (extensionBundles.getValidators() != null) {
                for (TExtensionBundles.Validators.Validator validator :
                        extensionBundles.getValidators().getValidatorArray()) {
                    this.extensionBundleValidators.add(validator.getClass1());
                }
            }
        }
    }

    private void populateOpenJPAProps() {
        TOpenJPAConfig openJpaConfig = bpsConfigDocument.getWSO2BPS().getOpenJPAConfig();
        if (openJpaConfig != null) {
            for (TOpenJPAConfig.Property prop : openJpaConfig.getPropertyArray()) {
                openJpaProperties.put(prop.getName(), prop.getValue());
            }
        }
    }

    private void populateMexTimeoutField() {
        TBPS.MexTimeOut timeOut = bpsConfigDocument.getWSO2BPS().getMexTimeOut();
        if (timeOut != null) {
            this.mexTimeOut = timeOut.getValue();
        }
    }

    private void populateExternalServiceTimeOut() {
        TBPS.ExternalServiceTimeOut extSvcTimeOut = bpsConfigDocument.getWSO2BPS().
                getExternalServiceTimeOut();
        if (extSvcTimeOut != null) {
            this.externalServiceTimeOut = extSvcTimeOut.getValue();
        }
    }

    private void populateHttpConnectionManagerProperties() {
        TMultithreadedHttpConnectionManagerConfig multiThreadedConManagerConfig =
                bpsConfigDocument.getWSO2BPS().getMultithreadedHttpConnectionManagerConfig();
        if (multiThreadedConManagerConfig != null) {
            this.maxConnectionsPerHost = multiThreadedConManagerConfig.getMaxConnectionsPerHost().
                    getValue();
            this.maxTotalConnections = multiThreadedConManagerConfig.getMaxTotalConnections().
                    getValue();
        }
    }


    private void populateDebugOnTransactionManagerProp() {
        this.debugOnTransactionManager = bpsConfigDocument.getWSO2BPS().getDebugTransactions();
    }

//    private void populateAcquireTransactionLocksProp() {
//        this.acquireTransactionLocks = bpsConfigDocument.getWSO2BPS().getAquireTransactionLocks();
//    }

    private void populateInMemoryInstanceTTL() {
        if (bpsConfigDocument.getWSO2BPS().isSetInMemoryInstanceTimeToLive()) {
            this.inMemoryInstanceTTL = bpsConfigDocument.getWSO2BPS().getInMemoryInstanceTimeToLive();
        }
    }

    private void populateOdeSchedulerThreadPoolSize() {
        if (bpsConfigDocument.getWSO2BPS().isSetODESchedulerThreadPoolSize()) {
            this.odeSchedulerThreadPoolSize = bpsConfigDocument.getWSO2BPS().
                    getODESchedulerThreadPoolSize();
        }
    }

    private static String addPrefix(String prop) {
        return BPELConstants.BPS_PROPERTY_PREFIX + prop;
    }

    public boolean isSyncWithRegistry() {
        return syncWithRegistry;
    }
}
