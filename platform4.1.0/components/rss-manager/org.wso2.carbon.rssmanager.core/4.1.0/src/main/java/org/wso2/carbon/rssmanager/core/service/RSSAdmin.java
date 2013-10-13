/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.rssmanager.core.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.ndatasource.common.DataSourceException;
import org.wso2.carbon.ndatasource.core.DataSourceMetaInfo;
import org.wso2.carbon.rssmanager.core.RSSManagerException;
import org.wso2.carbon.rssmanager.core.entity.*;
import org.wso2.carbon.rssmanager.core.internal.RSSManagerServiceComponent;
import org.wso2.carbon.rssmanager.core.internal.manager.RSSManager;
import org.wso2.carbon.rssmanager.core.internal.util.RSSConfig;
import org.wso2.carbon.rssmanager.core.internal.util.RSSManagerUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class RSSAdmin extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(RSSAdmin.class);

    public void createRSSInstance(RSSInstance rssInstance) throws RSSManagerException {
        this.getRSSManager().createRSSInstance(rssInstance);
    }

    public void dropRSSInstance(String rssInstanceName) throws RSSManagerException {
        this.getRSSManager().dropRSSInstance(rssInstanceName);
    }

    public void editRSSInstance(RSSInstance rssInstance) throws RSSManagerException {
        this.getRSSManager().editRSSInstanceConfiguration(rssInstance);
    }

    public RSSInstanceMetaData getRSSInstance(String rssInstanceName) throws RSSManagerException {
        RSSInstance rssInstance = this.getRSSManager().getRSSInstance(rssInstanceName);
        if (rssInstance == null) {
            throw new RSSManagerException("Given name '" + rssInstance + "' does not " +
                    "correspond to a valid RSS instance");
        }
        return RSSManagerUtil.convertRSSInstanceToMetadata(rssInstance);
    }

    public RSSInstanceMetaData[] getRSSInstances() throws RSSManagerException {
        int tid = this.getCurrentTenantId();
        RSSInstanceMetaData[] rssInstances = new RSSInstanceMetaData[0];
        try {
            List<RSSInstanceMetaData> tmpList = this.getRSSManager().getRSSInstances(tid);
            rssInstances = tmpList.toArray(new RSSInstanceMetaData[tmpList.size()]);
        } catch (RSSManagerException e) {
            String tenantDomain = null;
            try {
                tenantDomain = RSSManagerUtil.getTenantDomainFromTenantId(tid);
            } catch (RSSManagerException e1) {
                log.error(e1);
            }
            String msg = "Error occurred in retrieving the RSS instance list of the tenant '" +
                    tenantDomain + "'";
            handleException(msg, e);
        }
        return rssInstances;
    }

    public void createDatabase(Database database) throws RSSManagerException {
        this.getRSSManager().createDatabase(database);
    }

    public void dropDatabase(String rssInstanceName, String databaseName) throws
            RSSManagerException {
        this.getRSSManager().dropDatabase(rssInstanceName, databaseName);
    }

    public DatabaseMetaData[] getDatabases() throws RSSManagerException {
        int tid = this.getCurrentTenantId();
        DatabaseMetaData[] databases = new DatabaseMetaData[0];
        try {
            List<DatabaseMetaData> tmpList = this.getRSSManager().getDatabases(tid);
            databases = tmpList.toArray(new DatabaseMetaData[tmpList.size()]);
        } catch (RSSManagerException e) {
            String tenantDomain = null;
            try {
                tenantDomain = RSSManagerUtil.getTenantDomainFromTenantId(tid);
            } catch (RSSManagerException e1) {
                log.error(e1);
            }
            String msg = "Error occurred while retrieving the database list of the tenant '" +
                    tenantDomain + "'";
            handleException(msg, e);
        }
        return databases;
    }

    public DatabaseMetaData getDatabase(String rssInstanceName, String databaseName) throws
            RSSManagerException {
        Database database =
                this.getRSSManager().getDatabase(rssInstanceName, databaseName);
        if (database == null) {
            throw new RSSManagerException("Database '" + databaseName + "' does not exist");
        }
        return RSSManagerUtil.convertDatabaseToMetadata(database);
    }

    public boolean isDatabaseExist(String rssInstanceName,
                                   String databaseName) throws RSSManagerException {
        return this.getRSSManager().isDatabaseExist(rssInstanceName, databaseName);
    }

    public boolean isDatabaseUserExist(String rssInstanceName,
                                   String databaseUsername) throws RSSManagerException {
        return this.getRSSManager().isDatabaseUserExist(rssInstanceName, databaseUsername);
    }

    public boolean isDatabasePrivilegesTemplateExist(String templateName) throws
            RSSManagerException {
        return this.getRSSManager().isDatabasePrivilegeTemplateExist(templateName);
    }

    public void createDatabaseUser(DatabaseUser user) throws
            RSSManagerException {
        this.getRSSManager().createDatabaseUser(user);
    }

    public void dropDatabaseUser(String rssInstanceName,
                                 String username) throws RSSManagerException {
        this.getRSSManager().dropDatabaseUser(rssInstanceName, username);
    }

    public void editDatabaseUserPrivileges(DatabasePrivilegeSet privileges,
                                           DatabaseUser user,
                                           String databaseName) throws RSSManagerException {
        this.getRSSManager().editDatabaseUserPrivileges(privileges, user, databaseName);
    }

    public DatabaseUserMetaData getDatabaseUser(String rssInstanceName, String username) throws
            RSSManagerException {
        DatabaseUser user = this.getRSSManager().getDatabaseUser(rssInstanceName, username);
        if (user == null) {
            throw new RSSManagerException("Given username '" + username + "' does not " +
                    "correspond to a valid database user");
        }
        return RSSManagerUtil.convertToDatabaseUserMetadata(user);
    }

    public DatabaseUserMetaData[] getDatabaseUsers() throws RSSManagerException {
        List<DatabaseUserMetaData> tmpList =
                this.getRSSManager().getDatabaseUsers(this.getCurrentTenantId());
        return tmpList.toArray(new DatabaseUserMetaData[tmpList.size()]);
    }

    public void createDatabasePrivilegesTemplate(
            DatabasePrivilegeTemplate template) throws RSSManagerException {
        this.getRSSManager().createDatabasePrivilegesTemplate(template);
    }

    public void dropDatabasePrivilegesTemplate(String templateName) throws RSSManagerException {
        this.getRSSManager().dropDatabasePrivilegesTemplate(templateName);
    }

    public void editDatabasePrivilegesTemplate(DatabasePrivilegeTemplate template) throws
            RSSManagerException {
        this.getRSSManager().editDatabasePrivilegesTemplate(template);
    }

    public DatabasePrivilegeTemplate[] getDatabasePrivilegesTemplates() throws RSSManagerException {
        List<DatabasePrivilegeTemplate> templates =
                this.getRSSManager().getDatabasePrivilegeTemplates();
        return templates.toArray(new DatabasePrivilegeTemplate[templates.size()]);
    }

    public DatabasePrivilegeTemplate getDatabasePrivilegesTemplate(String templateName) throws
            RSSManagerException {
        return this.getRSSManager().getDatabasePrivilegeTemplate(
                templateName);
    }

    /**
     * Test the RSS instance connection using a mock database connection test.
     *
     * @param driverClass JDBC Driver class.
     * @param jdbcURL     JDBC url.
     * @param username    username.
     * @param password    password.
     * @return success or failure message.
     * @throws org.wso2.carbon.rssmanager.core.RSSManagerException
     *          rssDaoException
     */
    public void testConnection(String driverClass, String jdbcURL, String username,
                               String password) throws RSSManagerException {
        Connection conn = null;
        int tenantId =
                PrivilegedCarbonContext.getCurrentContext(this.getConfigContext()).getTenantId();

        if (driverClass == null || driverClass.length() == 0) {
            String msg = "Driver class is missing";
            throw new RSSManagerException(msg);
        }
        if (jdbcURL == null || jdbcURL.length() == 0) {
            String msg = "Driver connection URL is missing";
            throw new RSSManagerException(msg);
        }
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);

            Class<?> clz = (Class<?>) Class.forName(driverClass).newInstance();
            

            conn = DriverManager.getConnection(jdbcURL, username, password);
            if (conn == null) {
                String msg = "Unable to establish a JDBC connection with the database server";
                throw new RSSManagerException(msg);
            }
        } catch (SQLException e) {
            String msg = "Error occurred while testing the JDBC connection";
            handleException(msg, e);
        } catch (ClassNotFoundException e) {
            throw new RSSManagerException("Error occurred while testing database connectivity : " +
                    e.getMessage());
        } catch (InstantiationException e) {
            throw new RSSManagerException("Error occurred while testing database connectivity : " +
                    e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RSSManagerException("Error occurred while testing database connectivity : " +
                    e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    private void handleException(String msg, Exception e) throws RSSManagerException {
        log.error(msg, e);
        throw new RSSManagerException(msg, e);
    }

    public int getSystemRSSInstanceCount() throws RSSManagerException {
        return this.getRSSManager().getSystemRSSInstanceCount();
    }

    public void attachUserToDatabase(String rssInstanceName, String databaseName, String username,
                                     String templateName) throws RSSManagerException {
        this.getRSSManager().attachUserToDatabase(rssInstanceName, databaseName, username,
                templateName);
    }

    public void detachUserFromDatabase(String rssInstanceName, String databaseName,
                                       String username) throws RSSManagerException {
        this.getRSSManager().detachUserFromDatabase(rssInstanceName, databaseName, username);
    }

    public String[] getUsersAttachedToDatabase(String rssInstanceName,
                                               String databaseName) throws
            RSSManagerException {
        List<String> tmpList =
                this.getRSSManager().getUsersAttachedToDatabase(rssInstanceName, databaseName);
        return tmpList.toArray(new String[tmpList.size()]);
    }

    public String[] getAvailableUsersToAttachToDatabase(String rssInstanceName,
                                                        String databaseName) throws
            RSSManagerException {
        List<String> tmpList =
                this.getRSSManager().getAvailableUsersToAttachToDatabase(rssInstanceName,
                        databaseName);
        return tmpList.toArray(new String[tmpList.size()]);
    }
    
    private RSSManager getRSSManager() throws RSSManagerException {
        RSSConfig config = RSSConfig.getInstance();
        if (config == null) {
            throw new RSSManagerException("RSSConfig is not properly initialized and is null");
        }
        return config.getRssManager();
    }

    public void createCarbonDataSource(UserDatabaseEntry entry) throws RSSManagerException {
        Database database = this.getRSSManager().getDatabase(entry.getRssInstanceName(),
                entry.getDatabaseName());
        DataSourceMetaInfo metaInfo =
                RSSManagerUtil.createDSMetaInfo(database, entry.getUsername());
        try {
            RSSManagerServiceComponent.getDataSourceService().addDataSource(metaInfo);
        } catch (DataSourceException e) {
            String msg = "Error occurred while creating carbon datasource for the database '" +
                    entry.getDatabaseName() + "'";
            handleException(msg, e);
        }
    }

    public DatabasePrivilegeSet getUserDatabasePermissions(
            String rssInstanceName, String databaseName, String username) throws RSSManagerException {
        return this.getRSSManager().getUserDatabasePrivileges(rssInstanceName, databaseName,
                username);
    }

    public boolean isInitializedTenant(String tenantDomainName) throws RSSManagerException {
        if (!isSuperTenantUser()) {
            String msg = "Unauthorized operation, only super tenant is authorized to perform " +
                    "this operation permission denied";
            throw new RSSManagerException(msg);
        }
        int tenantId = getTenantId(tenantDomainName);
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
        boolean initialized = false;
        
        return initialized;
    }

    public void initializeTenant(String tenantDomain) throws RSSManagerException {
        if (!isSuperTenantUser()) {
            String msg = "Unauthorized operation, only super tenant is authorized. " +
                    "Tenant domain :" + CarbonContext.getThreadLocalCarbonContext().getTenantDomain() +
                    " permission denied";
            throw new RSSManagerException(msg);
        }
        int tenantId = getTenantId(tenantDomain);
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
    }

    public DatabaseMetaData[] getDatabasesForTenant(String tenantDomain) throws RSSManagerException {
        if (!isSuperTenantUser()) {
            String msg = "Unauthorized operation, only super tenant is authorized. " +
                    "Tenant domain :" + CarbonContext.getThreadLocalCarbonContext().getTenantDomain() +
                    " permission denied";
            throw new RSSManagerException(msg);
        }
        int tenantId = getTenantId(tenantDomain);
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
        DatabaseMetaData[] databases = null;
        try {
            databases = getDatabases();
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return databases;
    }

    public void createDatabaseForTenant(Database database,
                                        String tenantDomain) throws RSSManagerException {
        if (!isSuperTenantUser()) {
            String msg = "Unauthorized operation, only super tenant is authorized to perform " +
                    "this operation permission denied";
            log.error(msg);
            throw new RSSManagerException(msg);
        }
        try {
            int tenantId = getTenantId(tenantDomain);
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
            try {
                database.setTenantId(tenantId);
                createDatabase(database);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        } catch (RSSManagerException e) {
            log.error("Error occurred while creating database for tenant : " + e.getMessage(), e);
            throw e;
        }
    }

    public DatabaseMetaData getDatabaseForTenant(String rssInstanceName,
                                                 String databaseName, String tenantDomain) throws
            RSSManagerException {
        if (!isSuperTenantUser()) {
            String msg = "Unauthorized operation, only super tenant is authorized to perform " +
                    "this operation permission denied";
            log.error(msg);
            throw new RSSManagerException(msg);
        }
        int tenantId = getTenantId(tenantDomain);
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
        DatabaseMetaData metaData = null;
        try {
            metaData = getDatabase(rssInstanceName, databaseName);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return metaData;
    }

    private boolean isSuperTenantUser() {
        return (CarbonContext.getThreadLocalCarbonContext().getTenantId() ==
                MultitenantConstants.SUPER_TENANT_ID);
    }

    private int getTenantId(String tenantDomainName) {
        int tenantId = MultitenantConstants.INVALID_TENANT_ID;
        if (null != tenantDomainName) {
            TenantManager tenantManager = RSSManagerServiceComponent.getTenantManager();
            try {
                tenantId = tenantManager.getTenantId(tenantDomainName);
            } catch (UserStoreException e) {
                log.error("Error while retrieving the tenant Id for tenant domain:" +
                        tenantDomainName, e);
            }
        }
        return tenantId;
    }

    private int getCurrentTenantId() {
        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }

}
