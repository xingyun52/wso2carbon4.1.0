/*
 * Copyright 2005-2012 WSO2, Inc. (http://wso2.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.wso2.carbon.ss.rss.test;

import org.apache.derby.iapi.services.io.FileUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.authenticator.stub.AuthenticationAdmin;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentVariables;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.rssmanager.ui.stub.RSSAdminRSSManagerExceptionException;
import org.wso2.carbon.rssmanager.ui.stub.RSSAdminStub;
import org.wso2.carbon.rssmanager.ui.stub.types.Database;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabaseMetaData;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabaseUser;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabaseUserMetaData;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import static org.testng.Assert.assertTrue;

public class RSSMgtTestCase {

    private static final String RSS_CONFIG = "repository" + File.separator + "conf" + File.separator +
            "etc" + File.separator + "rss-config.xml";
    private EnvironmentVariables ssServer;
    private UserInfo userInfo;
    private String backendUrl;
    private String sessionCookie;
    private String serviceeUrl;
    EnvironmentBuilder builder;
    RSSAdminStub rssAdminStub;
    AuthenticationAdmin authenticationAdmin;


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception, RemoteException, MalformedURLException {
        userInfo = UserListCsvReader.getUserInfo(0);
        builder = new EnvironmentBuilder().ss(0);
        ssServer = builder.build().getSs();
        backendUrl = ssServer.getBackEndUrl();

        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String rssConigDst = carbonHome + File.separator + RSS_CONFIG;
        String rssConigSrc = ProductConstant.getResourceLocations("SS") + File.separator + RSS_CONFIG;
        File calrityRssConfig = new File(rssConigSrc);
        File clarityRssDist = new File(rssConigDst);
        FileUtil.copyFile(calrityRssConfig, clarityRssDist);
        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(backendUrl);
        serverConfigurationManager.restartGracefully();

        builder = new EnvironmentBuilder().ss(0);
        ssServer = builder.build().getSs();
        sessionCookie = ssServer.getSessionCookie();
        serviceeUrl = ssServer.getServiceUrl();
        String endPoint = backendUrl + "RSSAdmin";
        rssAdminStub = new RSSAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, rssAdminStub);
        //SqlDataSourceUtil sqlDataSourceUtil = new SqlDataSourceUtil(sessionCookie,backendUrl,)

    }

    @Test
    public void stubAuth() {
        AuthenticateStub.authenticateStub(sessionCookie, rssAdminStub);
        assertTrue(true);
    }

    @Test
    public void testDbConnection() throws RSSAdminRSSManagerExceptionException, RemoteException {
        rssAdminStub.testConnection("com.mysql.jdbc.Driver", "jdbc:mysql://127.0.0.1:3306/", "root", "root");
        assertTrue(true);

    }

    @Test
    public void createDb() throws RSSAdminRSSManagerExceptionException, RemoteException {
        Database database = new Database();
        database.setName("testdb");
        database.setRssInstanceName("WSO2 RSS Cluster");
        database.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        rssAdminStub.createDatabase(database);
        assertTrue(true);
    }

    @Test
    public void getDatabasesList() throws RSSAdminRSSManagerExceptionException, RemoteException {
        assertTrue(rssAdminStub.getDatabases().length > 0);
    }

    @Test
    public void createDbUser() throws RSSAdminRSSManagerExceptionException, RemoteException {
        DatabaseUser databaseUser = new DatabaseUser();
        databaseUser.setUsername("dbuser01");
        databaseUser.setPassword("dbuser01passwd");
        databaseUser.setRssInstanceName("WSO2 RSS Cluster");

        databaseUser.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        rssAdminStub.createDatabaseUser(databaseUser);
        assertTrue(true);
    }

    @Test
    public void tenantCreateDb() throws RemoteException, TenantMgtAdminServiceExceptionException, RSSAdminRSSManagerExceptionException {
        Database database = new Database();
        database.setName("testdbtid1");
        database.setRssInstanceName("WSO2 RSS Cluster");
        database.setTenantId(1);

        rssAdminStub.createDatabase(database);
        assertTrue(true);

    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws Exception {
        DatabaseMetaData[] databaseMetaDatas = rssAdminStub.getDatabases();
        if (databaseMetaDatas.length > 0) {
            for (DatabaseMetaData databaseMetaData : databaseMetaDatas) {
                if (databaseMetaData.getName().equals("testdb")) {
                    rssAdminStub.dropDatabase("WSO2 RSS Cluster", "testdb");
                }
            }
        }
        DatabaseUserMetaData[] databaseUsers = rssAdminStub.getDatabaseUsers();
        if (databaseUsers.length > 0) {
            for (DatabaseUserMetaData databaseUserMetaData : databaseUsers) {
                if (databaseUserMetaData.getUsername().equals("dbuser01")) {
                    rssAdminStub.dropDatabaseUser("WSO2 RSS Cluster", "dbuser01");
                }
            }
        }

    }


}
