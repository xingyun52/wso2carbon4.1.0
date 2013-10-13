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
import org.wso2.carbon.rssmanager.ui.stub.RSSAdminStub;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;
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

public class HiveAxis2ConfigObserver extends AbstractAxis2ConfigurationContextObserver {

    private static final Log log = LogFactory.getLog(HiveAxis2ConfigObserver.class);
    private RSSAdminStub rssAdminStub;

    public void createdConfigurationContext(ConfigurationContext configurationContext) {
//        int tenantId = PrivilegedCarbonContext.getCurrentContext(
//                configurationContext).getTenantId();
//        initializeStub(configurationContext);
//        initializeTenant(tenantId);
    }

    private void initializeStub(ConfigurationContext configurationContext) {
        if (null == rssAdminStub) {
            try {
                configurationContext = ServiceHolder.getConfigurationContextService().getClientConfigContext();
                String serverUrl = rssServerURL();
                rssAdminStub = new RSSAdminStub(configurationContext, serverUrl);
                String sessionCookie = login(configurationContext);
                Options options = rssAdminStub._getServiceClient().getOptions();
                options.setManageSession(true);
                options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                        sessionCookie);
            } catch (AxisFault axisFault) {
                log.error("Error while creating the RSS client." , axisFault);
            } catch (RemoteException e) {
               log.error("Error while creating RSS client.\n" +e.getMessage(), e);
            } catch (LoginAuthenticationExceptionException e) {
              log.error("Error while creating RSS client.\n" +e.getMessage(), e);
            } catch (SocketException e) {
               log.error("Error while creating RSS client.\n" +e.getMessage(), e);
            }
        }
    }

    protected String getBackendServerURLHTTPS() throws SocketException {
        String contextRoot = ServiceHolder.getConfigurationContextService().getServerConfigContext().getContextRoot();
        return "https://" + NetworkUtils.getLocalHostname() + ":" +
                CarbonUtils.getTransportPort(ServiceHolder.getConfigurationContextService(), "https") + contextRoot;

    }

    private String login(ConfigurationContext configurationContext) throws RemoteException, LoginAuthenticationExceptionException, SocketException {
        String authURL = rssServerURL()+"services/AuthenticationAdmin";
//        authURL= "local://services/AuthenticationAdmin";
        AuthenticationAdminStub authenticationAdminStub =
                new AuthenticationAdminStub(configurationContext, authURL);
        ServiceClient client = authenticationAdminStub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        try{
        authenticationAdminStub.login("admin", "admin", rssServerURL());
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }
        ServiceContext serviceContext = authenticationAdminStub.
                _getServiceClient().getLastOperationContext().getServiceContext();
        return (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
    }

    private String rssServerURL() throws SocketException {
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
                            rssConfigElement.getFirstChildWithName(new QName(HiveConstants.HIVE_RSS_CONFIG_SERVER_URL));
                    if (null != rssServerElement) {
                        return rssServerElement.getText().trim();
                    }
                }
            } catch (FileNotFoundException e) {
                log.warn("No hive-rss-config.xml " + path);
            } catch (XMLStreamException e) {
                log.error("Incorrect format " + path, e);
            }
        }
        return getBackendServerURLHTTPS()+HiveConstants.HIVE_RSS_CONFIG_DEFAULT_SERVER_URL;
    }

//    public void initializeTenant(int tenantId) {
//        if (isMultiTenantMode() && tenantId != MultitenantConstants.SUPER_TENANT_ID) {
//            try {
//                PrivilegedCarbonContext.startTenantFlow();
//                PrivilegedCarbonContext.getCurrentContext().setTenantId(tenantId);
//
////                RSSManagerService rssManagerService = ServiceHolder.getRSSManagerService();
///*            RSSInstanceMetaData[] rssEntries = rssManagerService.getRSSInstances();
//
//            String rssInstanceName = null;
//            if (rssEntries != null) {
//                for (RSSInstanceMetaData rssEntry : rssEntries) {
//                    if (rssEntry.getName().equals(HiveConstants.HIVE_METASTORE_RSS_INSTANCE)) {
//                        rssInstanceName = rssEntry.getName();
//                    }
//                }
//            }*/
//
//                HiveConf conf = new HiveConf();
//
//                String url = conf.getVar(HiveConf.ConfVars.METASTORECONNECTURLKEY);
//                String userName = conf.getVar(HiveConf.ConfVars.METASTORE_CONNECTION_USER_NAME);
//                String password = conf.getVar(HiveConf.ConfVars.METASTOREPWD);
//
//
///*            if (rssInstanceName == null) {
//                RSSInstance rssInstance = new RSSInstance();
//                rssInstance.setName(HiveConstants.HIVE_METASTORE_RSS_INSTANCE);
//                rssInstance.setAdminUsername(userName);
//                rssInstance.setAdminPassword(password);
//                rssInstance.setServerCategory("LOCAL");
//                rssInstance.setServerURL(url);
//                rssInstance.setDbmsType(url.split(":")[1]);
//                rssInstance.setInstanceType(url.split(":")[1]);
//
//                rssManagerService.createRSSInstance(rssInstance);
//
//            }*/
//
//                boolean dbPresent = false;
//                if(!rssAdminStub.isInitializedTenant(tenantId)){
//                    rssAdminStub.initializeTenant(tenantId);
//                }
//                DatabaseMetaData[] databaseEntries = rssAdminStub.getDatabasesForTenant(tenantId);
//                if(null != databaseEntries){
//                    for (DatabaseMetaData databaseEntry : databaseEntries) {
//                        if (databaseEntry.getName().contains(HiveConstants.HIVE_METASTORE_DB)) {
//                            dbPresent = true;
//                            break;
//                        }
//                    }
//                }
//
//                if (!dbPresent) {
//                    Database db = new Database();
//                    db.setName(HiveConstants.HIVE_METASTORE_DB);
//                    db.setRssInstanceName(RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
//                    db.setTenantId(tenantId);
//
//                    rssAdminStub.createDatabaseForTenant(db, tenantId);
//                }
//
//            } catch (Exception e) {
//                log.error("Error initializing tenant Hive meta store.. ", e);
//            }finally {
//                PrivilegedCarbonContext.endTenantFlow();
//            }
//        }
//    }

    private static boolean isMultiTenantMode() {
        RealmService realmService = ServiceHolder.getRealmService();

        Tenant[] tenants;
        try {
            tenants = realmService.getTenantManager().getAllTenants();
        } catch (UserStoreException e) {
            return false;
        }

        if (tenants != null && tenants.length > 0) {
            return true;
        }

        return false;
    }

}
