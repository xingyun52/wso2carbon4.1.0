/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
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
import org.wso2.carbon.ndatasource.ui.stub.NDataSourceAdminDataSourceException;
import org.wso2.carbon.ndatasource.ui.stub.NDataSourceAdminStub;
import org.wso2.carbon.ndatasource.ui.stub.core.services.xsd.WSDataSourceInfo;
import org.wso2.carbon.rssmanager.ui.stub.RSSAdminRSSManagerExceptionException;
import org.wso2.carbon.rssmanager.ui.stub.RSSAdminStub;
import org.wso2.carbon.rssmanager.ui.stub.types.Database;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabaseMetaData;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabasePrivilegeSet;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabasePrivilegeTemplate;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabaseUser;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabaseUserMetaData;
import org.wso2.carbon.rssmanager.ui.stub.types.RSSInstance;
import org.wso2.carbon.rssmanager.ui.stub.types.RSSInstanceMetaData;
import org.wso2.carbon.rssmanager.ui.stub.types.UserDatabaseEntry;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RSSTenantAdminTest
{
    private static final String RSS_CONFIG = "repository" + File.separator + "conf" + File.separator +
                                             "etc" + File.separator + "rss-config.xml";

    private RSSAdminStub rssAdminStub;
    private final String databaseName="test_db2";
    private final String databaseUserName ="db_usr1";
    private final String rssInstanceName="WSO2_RSS";
    private final String databaseUserPassword="password";
    private final String privilegeTemplateName="testTemplate";
    private NDataSourceAdminStub nDataSourceAdminStub;
    private final String databaseType="mysql";
    private final String serverCategoryLocal ="LOCAL";
    private final String serverCategoryRDS ="RDS";
    private final String testRssInstance="testRssInstance";
    private RSSTestHelper rssTestHelper;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception, RemoteException, MalformedURLException {

        rssTestHelper=new RSSTestHelper();
        rssTestHelper.initialize(1);
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String rssConigDst = carbonHome + File.separator + RSS_CONFIG;
        String rssConigSrc = ProductConstant.getResourceLocations("SS") + File.separator + RSS_CONFIG;
        File calrityRssConfig = new File(rssConigSrc);
        File clarityRssDist = new File(rssConigDst);
        //FileUtil.copyFile(calrityRssConfig, clarityRssDist);
        // ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(backendUrl);
        //serverConfigurationManager.restartGracefully();
        String endPoint = rssTestHelper.getBackendUrl() + "RSSAdmin";
        rssAdminStub = new RSSAdminStub(endPoint);
        nDataSourceAdminStub=new NDataSourceAdminStub(endPoint);
        AuthenticateStub.authenticateStub(rssTestHelper.getSessionCookie(), rssAdminStub);
        AuthenticateStub.authenticateStub(rssTestHelper.getSessionCookie(),nDataSourceAdminStub);

    }
    @Test(description = "Test connection")
    public void testDbConnection() throws RSSAdminRSSManagerExceptionException, RemoteException {
        rssAdminStub.testConnection("com.mysql.jdbc.Driver", rssTestHelper.getDataSource().getRssDbPassword(), rssTestHelper.getDataSource().getDbUser(), rssTestHelper.getDataSource().getRssDbPassword());
        assertTrue(true);

    }

    @Test(description = "Create database")
    public void createDatabase() throws RSSAdminRSSManagerExceptionException, RemoteException {
        assertFalse(isDatabaseExists(databaseName),"Database is already exists");
        Database database = new Database();
        database.setName(databaseName);
        database.setRssInstanceName(rssInstanceName);
        rssAdminStub.createDatabase(database);
        assertTrue(isDatabaseExists(databaseName),"Database is not in the database list");
    }

    private boolean isDatabaseExists(String databaseName) throws RSSAdminRSSManagerExceptionException, RemoteException
    {
        DatabaseMetaData[] databaseMetaDatas=rssAdminStub.getDatabases();
        for(DatabaseMetaData databaseMetaData:databaseMetaDatas)
        {
            if(databaseName.equals(databaseMetaData.getName()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isDatabaseUserExists(String databaseUserName) throws RSSAdminRSSManagerExceptionException, RemoteException
    {
        DatabaseUserMetaData[] databaseUserMetaDatas=rssAdminStub.getDatabaseUsers();
        for(DatabaseUserMetaData databaseUserMetaData:databaseUserMetaDatas)
        {
            if(databaseUserName.equals(databaseUserMetaData.getUsername()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isDatabasePrivilegeTemplateExists(String databasePrivilegeTemplateName)
            throws RSSAdminRSSManagerExceptionException, RemoteException {
        DatabasePrivilegeTemplate[] databasePrivilegeTemplates=rssAdminStub.getDatabasePrivilegesTemplates();
        for(DatabasePrivilegeTemplate databasePrivilegeTemplate:databasePrivilegeTemplates)
        {
            if(databasePrivilegeTemplateName.equals(databasePrivilegeTemplate.getName()))
            {
                return true;
            }
        }
        return false;
    }

    @Test(dependsOnMethods = "createDatabase,createDatabaseUser,dropDatabaseUser",description = "Drop database")
    public void dropDatabase() throws RSSAdminRSSManagerExceptionException, RemoteException {
        rssAdminStub.dropDatabase(rssInstanceName,databaseName);
    }

    @Test(description = "Get rss instance list")
    public void getRSSInstanceList() throws RSSAdminRSSManagerExceptionException, RemoteException {
        RSSInstanceMetaData[] rssInstanceMetaDatas=rssAdminStub.getRSSInstances();
        assertTrue(true);
    }
    @Test(description = "Get database list")
    public void getDatabasesList() throws RSSAdminRSSManagerExceptionException, RemoteException {
        DatabaseMetaData[]databaseMetaDatas=rssAdminStub.getDatabases();
        assertTrue(true);
    }

    @Test(description = "Create database user")
    public void createDatabaseUser() throws RSSAdminRSSManagerExceptionException, RemoteException {
        assertFalse(isDatabaseUserExists(databaseUserName),"Database user is already exists");
        DatabaseUser databaseUser = new DatabaseUser();
        databaseUser.setUsername(databaseUserName);
        databaseUser.setPassword(databaseUserPassword);
        databaseUser.setRssInstanceName(rssInstanceName);
        rssAdminStub.createDatabaseUser(databaseUser);
        assertTrue(isDatabaseUserExists(databaseUserName), "Created database user is not in the list");
    }

    @Test(dependsOnMethods = "createDatabase,createDatabaseUser,attachDatabaseUser",description = "Edit database user")
    public void editDatabaseUser() throws RSSAdminRSSManagerExceptionException, RemoteException {
        DatabaseUser user = new DatabaseUser();
        user.setUsername(databaseUserName);
        user.setPassword(databaseUserPassword);
        user.setRssInstanceName(rssInstanceName);

        DatabasePrivilegeSet privileges = new DatabasePrivilegeSet();
        privileges.setSelectPriv("Y");
        privileges.setInsertPriv("Y");
        privileges.setUpdatePriv("Y");
        privileges.setDeletePriv("Y");
        privileges.setCreatePriv("Y");
        privileges.setDropPriv("Y");
        privileges.setGrantPriv("Y");
        privileges.setReferencesPriv("Y");
        privileges.setIndexPriv("Y");
        privileges.setAlterPriv("Y");
        privileges.setCreateTmpTablePriv("N");
        privileges.setLockTablesPriv("N");
        privileges.setCreateViewPriv("N");
        privileges.setShowViewPriv("N");
        privileges.setCreateRoutinePriv("N");
        privileges.setAlterRoutinePriv("N");
        privileges.setExecutePriv("N");
        privileges.setEventPriv("N");
        privileges.setTriggerPriv("N");
        rssAdminStub.editDatabaseUserPrivileges(privileges,user,databaseName);
        privileges =rssAdminStub.getUserDatabasePermissions(rssInstanceName, databaseName, databaseUserName);
        DatabaseUserMetaData databaseUserMetaData = rssAdminStub.getDatabaseUser(rssInstanceName, databaseUserName);
        assertEquals(privileges.getSelectPriv(),"Y");
        assertEquals(privileges.getInsertPriv(),"Y");
        assertEquals(privileges.getIndexPriv(),"N");
        assertEquals(privileges.getAlterPriv(),"N");

    }

    @Test(dependsOnMethods = "createDatabase,createDatabaseUser",description = "Create privilege template")
    public void createPrivilegeTemplate()
            throws RSSAdminRSSManagerExceptionException, RemoteException {
        assertFalse(isDatabasePrivilegeTemplateExists(privilegeTemplateName),"Database privilege template is already exists");
        DatabasePrivilegeSet privileges = new DatabasePrivilegeSet();
        privileges.setSelectPriv("Y");
        privileges.setInsertPriv("Y");
        privileges.setUpdatePriv("Y");
        privileges.setDeletePriv("Y");
        privileges.setCreatePriv("Y");
        privileges.setDropPriv("Y");
        privileges.setGrantPriv("Y");
        privileges.setReferencesPriv("Y");
        privileges.setIndexPriv("N");
        privileges.setAlterPriv("N");
        privileges.setCreateTmpTablePriv("N");
        privileges.setLockTablesPriv("N");
        privileges.setCreateViewPriv("N");
        privileges.setShowViewPriv("N");
        privileges.setCreateRoutinePriv("N");
        privileges.setAlterRoutinePriv("N");
        privileges.setExecutePriv("N");
        privileges.setEventPriv("N");
        privileges.setTriggerPriv("N");
        DatabasePrivilegeTemplate template = new DatabasePrivilegeTemplate();
        template.setName(privilegeTemplateName);
        template.setPrivileges(privileges);
        rssAdminStub.createDatabasePrivilegesTemplate(template);
        assertTrue(isDatabasePrivilegeTemplateExists(privilegeTemplateName), "Created database privilege template is not in the list");
        DatabasePrivilegeTemplate databasePrivilegeTemplate=rssAdminStub.getDatabasePrivilegesTemplate(privilegeTemplateName);
        privileges=databasePrivilegeTemplate.getPrivileges();
        assertEquals(privileges.getSelectPriv(),"Y");
        assertEquals(privileges.getInsertPriv(),"Y");
        assertEquals(privileges.getUpdatePriv(),"Y");
        assertEquals(privileges.getDeletePriv(),"Y");
        assertEquals(privileges.getCreatePriv(),"Y");
        assertEquals(privileges.getDropPriv(),"Y");
        assertEquals(privileges.getGrantPriv(),"Y");
        assertEquals(privileges.getReferencesPriv(),"Y");
        assertEquals(privileges.getIndexPriv(),"N");
        assertEquals(privileges.getAlterPriv(),"N");
        assertEquals(privileges.getCreateTmpTablePriv(),"N");
        assertEquals(privileges.getLockTablesPriv(),"N");
        assertEquals(privileges.getCreateViewPriv(),"N");
        assertEquals(privileges.getShowViewPriv(),"N");
        assertEquals(privileges.getCreateRoutinePriv(),"N");
        assertEquals(privileges.getExecutePriv(),"N");
        assertEquals(privileges.getEventPriv(),"N");
        assertEquals(privileges.getTriggerPriv(),"N");

    }

    @Test(dependsOnMethods = "createDatabase,createDatabaseUser,attachDatabaseUser",description = "Edit privilege template")
    public void editPrivilegeTemplate()
            throws RSSAdminRSSManagerExceptionException, RemoteException {
        assertTrue(isDatabasePrivilegeTemplateExists(privilegeTemplateName),"Database privilege template is not exists");
        DatabasePrivilegeSet privileges = new DatabasePrivilegeSet();
        privileges.setSelectPriv("Y");
        privileges.setInsertPriv("Y");
        privileges.setUpdatePriv("N");
        privileges.setDeletePriv("Y");
        privileges.setCreatePriv("Y");
        privileges.setDropPriv("Y");
        privileges.setGrantPriv("Y");
        privileges.setReferencesPriv("Y");
        privileges.setIndexPriv("Y");
        privileges.setAlterPriv("Y");
        privileges.setCreateTmpTablePriv("N");
        privileges.setLockTablesPriv("N");
        privileges.setCreateViewPriv("N");
        privileges.setShowViewPriv("N");
        privileges.setCreateRoutinePriv("N");
        privileges.setAlterRoutinePriv("N");
        privileges.setExecutePriv("N");
        privileges.setEventPriv("N");
        privileges.setTriggerPriv("N");
        DatabasePrivilegeTemplate template = new DatabasePrivilegeTemplate();
        template.setName(privilegeTemplateName);
        template.setPrivileges(privileges);
        rssAdminStub.editDatabasePrivilegesTemplate(template);
        assertTrue(isDatabasePrivilegeTemplateExists(privilegeTemplateName), "Edited database privilege template is not in the list");
        DatabasePrivilegeTemplate databasePrivilegeTemplate=rssAdminStub.getDatabasePrivilegesTemplate(privilegeTemplateName);
        privileges=databasePrivilegeTemplate.getPrivileges();
        assertEquals(privileges.getSelectPriv(),"Y");
        assertEquals(privileges.getInsertPriv(),"Y");
        assertEquals(privileges.getUpdatePriv(),"N");
        assertEquals(privileges.getDeletePriv(),"Y");
        assertEquals(privileges.getCreatePriv(),"Y");
        assertEquals(privileges.getDropPriv(),"Y");
        assertEquals(privileges.getGrantPriv(),"Y");
        assertEquals(privileges.getReferencesPriv(),"Y");
        assertEquals(privileges.getIndexPriv(),"Y");
        assertEquals(privileges.getAlterPriv(),"Y");
        assertEquals(privileges.getCreateTmpTablePriv(),"N");
        assertEquals(privileges.getLockTablesPriv(),"N");
        assertEquals(privileges.getCreateViewPriv(),"N");
        assertEquals(privileges.getShowViewPriv(),"N");
        assertEquals(privileges.getCreateRoutinePriv(),"N");
        assertEquals(privileges.getExecutePriv(),"N");
        assertEquals(privileges.getEventPriv(),"N");
        assertEquals(privileges.getTriggerPriv(),"N");
    }

    @Test(dependsOnMethods = "createDatabase,createDatabaseUser,editPrivilegeTemplate,attachDatabaseUser",description = "Drop privilege template")
    public void dropPrivilegeTemplate()
            throws RSSAdminRSSManagerExceptionException, RemoteException {
        rssAdminStub.dropDatabasePrivilegesTemplate(privilegeTemplateName);
    }

    @Test(dependsOnMethods = "createDatabase,createDatabaseUser,attachDatabaseUser,dropAttachedDatabaseUser",description = "Drop database user")
    public void dropDatabaseUser() throws RSSAdminRSSManagerExceptionException, RemoteException {
        rssAdminStub.dropDatabaseUser(rssInstanceName,databaseUserName);
    }

    @Test(dependsOnMethods = "createDatabase,createDatabaseUser,createPrivilegeTemplate",description = "Attach database user")
    public void attachDatabaseUser() throws RSSAdminRSSManagerExceptionException, RemoteException {
        String[] availableUsersToAttachToDatabase=rssAdminStub.getAvailableUsersToAttachToDatabase(rssInstanceName,databaseName);
        int availableUserCount=availableUsersToAttachToDatabase.length;
        boolean contains=false;
        for(String temp:availableUsersToAttachToDatabase)
        {
            if(databaseUserName.equals(temp))
            {
                contains=true;
            }
        }
        assertTrue(contains,"Database user not available to attach to database");
        rssAdminStub.attachUserToDatabase(rssInstanceName,databaseName,databaseUserName,privilegeTemplateName);
        assertTrue((availableUserCount-1)==rssAdminStub.getAvailableUsersToAttachToDatabase(rssInstanceName,databaseName).length);
        contains=false;
        for(String temp:rssAdminStub.getAvailableUsersToAttachToDatabase(rssInstanceName,databaseName))
        {
            if(databaseUserName.equals(temp))
            {
                contains=true;
            }
        }
        contains=false;
        assertFalse(contains,"Database user still available for attach after attached to the database");
        for(String temp:rssAdminStub.getUsersAttachedToDatabase(rssInstanceName,databaseName))
        {
            if(databaseUserName.equals(temp))
            {
                contains=true;
            }
        }
        assertTrue(contains,"Attached user not appear in the list");
    }

    @Test(dependsOnMethods = "createDatabase,createDatabaseUser,createDataSource,attachDatabaseUser,createDataSource",description = "Drop attach database user")
    public void dropAttachedDatabaseUser()
            throws RSSAdminRSSManagerExceptionException, RemoteException {
        String[] attachedDatabaseUsers=rssAdminStub.getUsersAttachedToDatabase(rssInstanceName, databaseName);
        int availableUserCount=rssAdminStub.getAvailableUsersToAttachToDatabase(rssInstanceName,databaseName).length;
        boolean contains=false;
        for(String temp:attachedDatabaseUsers)
        {
            if(databaseUserName.equals(temp))
            {
                contains=true;
            }
        }
        assertTrue(contains,"Database user not attached to the database");
        rssAdminStub.detachUserFromDatabase(rssInstanceName,databaseName,databaseUserName);
        assertTrue((availableUserCount+1)==rssAdminStub.getAvailableUsersToAttachToDatabase(rssInstanceName,databaseName).length);
        contains=false;
        for(String temp:rssAdminStub.getUsersAttachedToDatabase(rssInstanceName,databaseName))
        {
            if(databaseUserName.equals(temp))
            {
                contains=true;
            }
        }
        assertFalse(contains, "Database user still attached to the database");
        contains=false;
        for(String temp:rssAdminStub.getAvailableUsersToAttachToDatabase(rssInstanceName, databaseName))
        {
            if(databaseUserName.equals(temp))
            {
                contains=true;
            }
        }
        assertTrue(contains, "Database user not available to attach after de-attached");
    }

    @Test(dependsOnMethods = "createDatabase,createDatabaseUser,attachDatabaseUser",description = "Create data source")
    public void createDataSource() throws RSSAdminRSSManagerExceptionException, RemoteException,
                                          NDataSourceAdminDataSourceException {
        UserDatabaseEntry entry = new UserDatabaseEntry();
        entry.setRssInstanceName(rssInstanceName);
        entry.setDatabaseName(databaseName);
        entry.setUsername(databaseUserName);
        rssAdminStub.createCarbonDataSource(entry);
        WSDataSourceInfo[] allDataSources = nDataSourceAdminStub.getAllDataSources();
        boolean isCreated=false;
        for(WSDataSourceInfo wsDataSourceInfo:allDataSources)
        {
            if(databaseName.equals(wsDataSourceInfo.getDsMetaInfo().getName()))
            {
                isCreated=true;
            }
        }
        assertTrue(isCreated,"Data source has not been created");
    }

    @Test(description = "Create rss instance")
    public void createRSSInstance() throws RSSAdminRSSManagerExceptionException, RemoteException {
        RSSInstance rssIns = new RSSInstance();
        rssIns.setName(testRssInstance);
        rssIns.setServerURL(rssTestHelper.getDataSource().getDbUrl());
        rssIns.setAdminUsername(databaseUserName);
        rssIns.setAdminPassword(databaseUserPassword);
        rssIns.setDbmsType(databaseType);
        rssIns.setInstanceType(rssInstanceName);
        rssIns.setServerCategory(serverCategoryLocal.toUpperCase());
        rssAdminStub.createRSSInstance(rssIns);
        boolean isContains=false;
        for(RSSInstanceMetaData rssInstanceMetaData:rssAdminStub.getRSSInstances())
        {
            if(testRssInstance.equals(rssInstanceMetaData.getName()))
            {
                isContains=true;
            }
        }
        assertTrue(isContains,"Rss instance not created");
    }

    @Test(dependsOnMethods ="createRSSInstance" ,description = "Edit rss instance")
    public void editRSSInstance() throws RSSAdminRSSManagerExceptionException, RemoteException {
        RSSInstance rssIns = new RSSInstance();
        rssIns.setName(testRssInstance);
        rssIns.setServerURL(rssTestHelper.getDataSource().getDbUrl());
        rssIns.setAdminUsername(databaseUserName);
        rssIns.setAdminPassword(databaseUserPassword);
        rssIns.setDbmsType(databaseType);
        rssIns.setInstanceType(rssInstanceName);
        rssIns.setServerCategory(serverCategoryRDS.toUpperCase());
        rssAdminStub.editRSSInstance(rssIns);
        boolean isChanged=false;
        for(RSSInstanceMetaData rssInstanceMetaData:rssAdminStub.getRSSInstances())
        {
            if(serverCategoryRDS.toUpperCase().equals(rssInstanceMetaData.getServerCategory()))
            {
                isChanged=true;
            }
        }
        assertTrue(isChanged,"Rss instance not edited");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws Exception {
        DatabaseMetaData[] databaseMetaDatas = rssAdminStub.getDatabases();
        if (databaseMetaDatas.length > 0) {
            for (DatabaseMetaData databaseMetaData : databaseMetaDatas) {
                if (databaseMetaData.getName().equals(databaseName)) {
                    rssAdminStub.dropDatabase(rssInstanceName, databaseName);
                }
            }
        }

        DatabasePrivilegeTemplate[] databasePrivilegeTemplates=rssAdminStub.getDatabasePrivilegesTemplates();
        for(DatabasePrivilegeTemplate databasePrivilegeTemplate:databasePrivilegeTemplates)
        {
            if(privilegeTemplateName.equals(databasePrivilegeTemplate.getName()))
            {
                rssAdminStub.dropDatabasePrivilegesTemplate(privilegeTemplateName);
            }
        }

        DatabaseUserMetaData[] databaseUsers = rssAdminStub.getDatabaseUsers();
        if (databaseUsers.length > 0) {
            for (DatabaseUserMetaData databaseUserMetaData : databaseUsers) {
                if (databaseUserMetaData.getUsername().equals(databaseUserName)) {
                    rssAdminStub.dropDatabaseUser(rssInstanceName, databaseUserName);
                }
            }
        }

        WSDataSourceInfo[] wsDataSourceInfos=nDataSourceAdminStub.getAllDataSources();
        for(WSDataSourceInfo wsDataSourceInfo:wsDataSourceInfos)
        {
            if(databaseName.equals(wsDataSourceInfo.getDsMetaInfo().getName()))
            {
                nDataSourceAdminStub.deleteDataSource(databaseName);
            }
        }

        RSSInstanceMetaData[] rssInstanceMetaDatas=rssAdminStub.getRSSInstances();
        for(RSSInstanceMetaData rssInstanceMetaData:rssInstanceMetaDatas)
        {
            if(testRssInstance.equals(rssInstanceMetaData.getName()))
            {
                rssAdminStub.dropRSSInstance(testRssInstance);
            }
        }
    }
    }
