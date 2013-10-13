package org.wso2.carbon.analytics.hive.multitenancy;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.hive.HiveConstants;
import org.wso2.carbon.analytics.hive.ServiceHolder;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.rssmanager.common.RSSManagerConstants;
import org.wso2.carbon.rssmanager.ui.stub.RSSAdminRSSManagerExceptionException;
import org.wso2.carbon.rssmanager.ui.stub.RSSAdminStub;
import org.wso2.carbon.rssmanager.ui.stub.types.Database;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabaseMetaData;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.NetworkUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class HiveRSSMetastoreManager {
    private ConcurrentHashMap<Integer, DatabaseMetaData> hiveMetaStoreCache =
            new ConcurrentHashMap<Integer, DatabaseMetaData>();

    private RSSAdminStub rssAdminStub;
    private static RSSConfig rssConfig;

    private static HiveRSSMetastoreManager instance;

    private static final Log log = LogFactory.getLog(HiveRSSMetastoreManager.class);

    private HiveRSSMetastoreManager() {

    }

    private void initializeStub() throws RemoteException, LoginAuthenticationExceptionException, SocketException {
        if (null == rssAdminStub) {
            try {
                ConfigurationContext configurationContext =
                        ServiceHolder.getConfigurationContextService().getClientConfigContext();
                rssAdminStub = new RSSAdminStub(configurationContext, rssConfig.getRssServerUrl() +
                        HiveConstants.HIVE_RSS_CONFIG_DEFAULT_SERVER_URL);
                login(configurationContext);
            } catch (AxisFault axisFault) {
                log.error("Error while creating the RSS client", axisFault);
                throw axisFault;
            } catch (RemoteException e) {
                log.error("Error while creating RSS client." + e.getMessage(), e);
                throw e;
            } catch (LoginAuthenticationExceptionException e) {
                log.error("Error while creating RSS client." + e.getMessage(), e);
                throw e;
            } catch (SocketException e) {
                log.error("Error while creating RSS client." + e.getMessage(), e);
                throw e;
            }
        }
    }

    private String getBackendServerURLHTTPS() {
        String contextRoot = ServiceHolder.getConfigurationContextService().
                getServerConfigContext().getContextRoot();
        try {
            return "https://" + NetworkUtils.getLocalHostname() + ":" +
                    CarbonUtils.getTransportPort(ServiceHolder.getConfigurationContextService(),
                            "https") + contextRoot;
        } catch (SocketException e) {
            log.error(e);
            return HiveConstants.DEFAULT_SERVER_URL;
        }
    }

    private void login(ConfigurationContext configurationContext)
            throws RemoteException,
            LoginAuthenticationExceptionException, SocketException {
        String authURL = rssConfig.getRssServerUrl() + "services/AuthenticationAdmin";
        AuthenticationAdminStub authenticationAdminStub =
                new AuthenticationAdminStub(configurationContext, authURL);
        ServiceClient client = authenticationAdminStub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        try {
            authenticationAdminStub.login(rssConfig.getUserName()
                    , rssConfig.getPassword(), NetworkUtils.getLocalHostname());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        ServiceContext serviceContext = authenticationAdminStub.
                _getServiceClient().getLastOperationContext().getServiceContext();
        String sessionCookie =  (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);

        Options rssOptions = rssAdminStub._getServiceClient().getOptions();
          rssOptions.setManageSession(true);
                rssOptions.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                        sessionCookie);
    }



    private RSSConfig getRSSConfig() {
        RSSConfig rssConfig = new RSSConfig();
        String path = CarbonUtils.getCarbonConfigDirPath() + File.separator + HiveConstants.HIVE_RSS_CONFIG_FILE_PATH;
        File rssConfigFile = new File(path);
        if (rssConfigFile.exists()) {
            try {
                XMLInputFactory xif = XMLInputFactory.newInstance();
                InputStream inputStream = new FileInputStream(rssConfigFile);
                XMLStreamReader reader = xif.createXMLStreamReader(inputStream);
                xif.setProperty("javax.xml.stream.isCoalescing", false);

                StAXOMBuilder builder = new StAXOMBuilder(reader);
                OMElement rssConfigElement = builder.getDocument().getOMDocumentElement();

                if (null != rssConfigElement) {
                    OMElement rssServerElement =
                            rssConfigElement.
                                    getFirstChildWithName(new QName(HiveConstants.
                                            HIVE_RSS_CONFIG_SERVER_URL));
                    if (null != rssServerElement) {
                        rssConfig.setRssServerUrl(rssServerElement.getText().trim());
                    }

                    OMElement rssUserNameElement =
                            rssConfigElement.
                                    getFirstChildWithName(new QName(HiveConstants.
                                            HIVE_RSS_CONFIG_USERNAME));
                    if (null != rssUserNameElement) {
                        rssConfig.setUserName(rssUserNameElement.getText().trim());
                    }

                    OMElement rssPassWordElement =
                            rssConfigElement.
                                    getFirstChildWithName(new QName(HiveConstants.
                                            HIVE_RSS_CONFIG_PASSWORD));
                    if (null != rssUserNameElement) {
                        rssConfig.setPassword(rssPassWordElement.getText().trim());
                    }
                }
            } catch (FileNotFoundException e) {
                log.warn("No hive-rss-config.xml " + path);
            } catch (XMLStreamException e) {
                log.error("Incorrect format " + path, e);
            }
        }
        if (null == rssConfig.getRssServerUrl())
            rssConfig.setRssServerUrl(getBackendServerURLHTTPS());
        if (null == rssConfig.getUserName())
            rssConfig.setUserName(HiveConstants.HIVE_RSS_CONFIG_DEFAULT_USERNAME);
        if (null == rssConfig.getPassword())
            rssConfig.setPassword(HiveConstants.HIVE_RSS_CONFIG_DEFAULT_PASSWORD);
        return rssConfig;
    }

    public static HiveRSSMetastoreManager getInstance() {
        if (null == instance) {
            instance = new HiveRSSMetastoreManager();
            rssConfig = instance.getRSSConfig();
            try{
                instance.initializeStub();
            }catch (Exception e){
                instance = null;
            }
        }
        return instance;
    }

    public void prepareRSSMetaStore(String tenantDomain, int tenantId) {
        if (HiveMultitenantUtil.isMultiTenantMode()) {
            if (null == hiveMetaStoreCache.get(tenantId)) {
                synchronized (HiveRSSMetastoreManager.class) {
                    if (null == hiveMetaStoreCache.get(tenantId)) {
                        try {
                            login(ServiceHolder.getConfigurationContextService().getClientConfigContext());
                        } catch (RemoteException e) {
                            log.error("Error while logging in", e);
                        } catch (LoginAuthenticationExceptionException e) {
                             log.error("Error while logging in", e);
                        } catch (SocketException e) {
                             log.error("Error while logging in", e);
                        }
                        DatabaseMetaData metaData = getRSSMetaStore(tenantDomain);
                        if (null != metaData) {
                            hiveMetaStoreCache.put(tenantId, metaData);
                            if(log.isDebugEnabled())log.debug("************************Successfully updating the cache for tenant id:"+tenantId);
                        } else {
                            log.error("Error while retrieving setting the hive meta " +
                                    "store for tenant domain:" + tenantDomain);
                        }
                    }
                }
            }
        }
    }


    private DatabaseMetaData getRSSMetaStore(String tenantDomain) {
        try {
            if (!rssAdminStub.isInitializedTenant(tenantDomain)) {
                if(log.isDebugEnabled())log.debug("&&&&&& Tenant is not initialized &&&&&&&&");
                rssAdminStub.initializeTenant(tenantDomain);
                if(log.isDebugEnabled())log.debug("&&&&&& Tenant is initialized &&&&&&&&");
            }
            if(log.isDebugEnabled())log.debug("********* Getting Database before creating data base entries **************");
            DatabaseMetaData metaData = getHiveMetaDatabase(tenantDomain);
            if (null != metaData) {
             if(log.isDebugEnabled())log.debug("*********** BAM Metadata found so returning without creating BAM DB**************");
                return metaData;
            }
              if(log.isDebugEnabled())log.debug("*********** No BAM Metadata found so going to create BAM DB**************");
            //If not already created database, create another data store.
            Database db = new Database();
            db.setName(HiveConstants.HIVE_METASTORE_DB);
            db.setRssInstanceName(RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);

            rssAdminStub.createDatabaseForTenant(db, tenantDomain);
             if(log.isDebugEnabled())log.debug("*********** Created BAM DB succesfully **************");
             if(log.isDebugEnabled())log.debug("*********** Fetching DB list after creating the BAM DB **************");
            return getHiveMetaDatabase(tenantDomain);
        } catch (Exception e) {
            log.error("Error initializing tenant Hive meta store.. ", e);
            return null;
        }
    }

    private DatabaseMetaData getHiveMetaDatabase(String tenantDomain)
            throws RSSAdminRSSManagerExceptionException, RemoteException {
        DatabaseMetaData[] databaseEntries = rssAdminStub.getDatabasesForTenant(tenantDomain);
        if (null != databaseEntries) {
            for (DatabaseMetaData databaseEntry : databaseEntries) {
                if (databaseEntry.getName().contains(HiveConstants.HIVE_METASTORE_DB)) {
                    if(log.isDebugEnabled())log.debug("Found MetaStoreDB -->" + databaseEntry.getName());
                    return databaseEntry;
                }else {
                   if(log.isDebugEnabled())log.debug("It's not BAM MetaStoreDB -->" + databaseEntry.getName());
                }
            }
        }
        return null;
    }

    public String getMetaDataStoreConnectionURL(int tenantId){
       DatabaseMetaData metaData =  hiveMetaStoreCache.get(tenantId);
       return metaData.getUrl();
    }


}
