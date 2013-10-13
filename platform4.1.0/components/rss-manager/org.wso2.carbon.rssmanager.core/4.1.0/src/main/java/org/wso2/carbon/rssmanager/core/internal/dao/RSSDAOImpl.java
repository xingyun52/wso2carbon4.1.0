/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.rssmanager.core.internal.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.rssmanager.common.RSSManagerConstants;
import org.wso2.carbon.rssmanager.core.RSSManagerException;
import org.wso2.carbon.rssmanager.core.entity.*;
import org.wso2.carbon.rssmanager.core.internal.util.RSSConfig;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO implementation for DSSDAO interface.
 */
public class RSSDAOImpl implements RSSDAO {

    private static Log log = LogFactory.getLog(RSSDAOImpl.class);

    public void createRSSInstance(RSSInstance rssInstance) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "INSERT INTO RM_SERVER_INSTANCE (NAME, SERVER_URL, DBMS_TYPE, INSTANCE_TYPE, SERVER_CATEGORY, ADMIN_USERNAME, ADMIN_PASSWORD, TENANT_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rssInstance.getName());
            stmt.setString(2, rssInstance.getServerURL());
            stmt.setString(3, rssInstance.getDbmsType());
            stmt.setString(4, rssInstance.getInstanceType());
            stmt.setString(5, rssInstance.getServerCategory());
            stmt.setString(6, rssInstance.getAdminUsername());
            stmt.setString(7, rssInstance.getAdminPassword());
            stmt.setInt(8, this.getCurrentTenantId());
            stmt.execute();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while creating the RSS instance '" +
                    rssInstance.getName() + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void updateRSSInstance(RSSInstance rssInstance) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "UPDATE RM_SERVER_INSTANCE SET SERVER_URL = ?, DBMS_TYPE = ?, INSTANCE_TYPE = ?, SERVER_CATEGORY = ?, ADMIN_USERNAME = ?, ADMIN_PASSWORD = ? WHERE NAME = ? AND TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rssInstance.getServerURL());
            stmt.setString(2, rssInstance.getDbmsType());
            stmt.setString(3, rssInstance.getInstanceType());
            stmt.setString(4, rssInstance.getServerCategory());
            stmt.setString(5, rssInstance.getAdminUsername());
            stmt.setString(6, rssInstance.getAdminPassword());
            stmt.setString(7, rssInstance.getName());
            stmt.setInt(8, this.getCurrentTenantId());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while editing the RSS instance '" +
                    rssInstance.getName() + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }


    public RSSInstance getRSSInstance(String rssInstanceName) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        RSSInstance rssInstance = null;
        try {
            String sql = "SELECT * FROM RM_SERVER_INSTANCE WHERE NAME = ? AND TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rssInstanceName);
            stmt.setInt(2, this.getCurrentTenantId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                rssInstance = this.createRSSInstanceFromRS(rs);
            }
            rs.close();
            stmt.close();
            return rssInstance;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving the configuration of " +
                    "RSS instance '" + rssInstanceName + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public DatabaseUser getDatabaseUser(RSSInstance
            rssInstance, String username) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        DatabaseUser user = new DatabaseUser();
        try {
            String sql = "SELECT u.USERNAME, s.NAME AS RSS_INSTANCE_NAME, u.TENANT_ID, u.TYPE FROM RM_SERVER_INSTANCE s, RM_DATABASE_USER u WHERE s.ID = u.RSS_INSTANCE_ID AND s.NAME = ? AND s.TENANT_ID = ? AND u.USERNAME = ? AND u.TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rssInstance.getName());
            stmt.setInt(2, rssInstance.getTenantId());
            stmt.setString(3, username);
            stmt.setInt(4, this.getCurrentTenantId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                user = this.createDatabaseUserFromRS(rs);
            }
            rs.close();
            stmt.close();

            return user;
        } catch (SQLException e) {
            throw new RSSManagerException("Error while occurred while retrieving information of " +
                    "the database user '" + user.getUsername() + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void incrementSystemRSSDatabaseCount() throws RSSManagerException {
        Connection conn = null;
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            String sql = "SELECT * FROM RM_SYSTEM_DATABASE_COUNT";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                sql = "INSERT INTO RM_SYSTEM_DATABASE_COUNT (COUNT) VALUES (0)";
                stmt = conn.prepareStatement(sql);
                stmt.executeUpdate();
            }
            sql = "UPDATE RM_SYSTEM_DATABASE_COUNT SET COUNT = COUNT + 1";
            stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while incrementing system RSS " +
                    "database count : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    public List<RSSInstance> getAllSystemRSSInstances() throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM RM_SERVER_INSTANCE WHERE INSTANCE_TYPE = ? AND TENANT_ID = ?");
            stmt.setString(1, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
            stmt.setInt(2, MultitenantConstants.SUPER_TENANT_ID);
            ResultSet rs = stmt.executeQuery();
            List<RSSInstance> result = new ArrayList<RSSInstance>();
            while (rs.next()) {
                result.add(this.createRSSInstanceFromRS(rs));
            }
            rs.close();
            stmt.close();

            return result;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving system RSS " +
                    "instances : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void dropRSSInstance(String rssInstanceName, int tenantId) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        RSSInstance rssInstance = this.getRSSInstance(rssInstanceName);
        try {
            List<DatabaseUser> users =
                    this.getDatabaseUsersByRSSInstance(conn, rssInstance);
            if (users.size() > 0) {
                for (DatabaseUser user : users) {
                    this.dropDatabaseUser(rssInstance, user.getUsername(), tenantId);
                }
            }
            String sql = "DELETE FROM RM_SERVER_INSTANCE WHERE NAME = ? AND TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rssInstanceName);
            stmt.setInt(2, tenantId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while dropping the RSS instance '" +
                    rssInstanceName + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    private List<DatabaseUser> getDatabaseUsersByRSSInstance(
            Connection conn, RSSInstance rssInstance) throws SQLException {
        String sql = "SELECT u.USERNAME AS USERNAME, s.NAME AS RSS_INSTANCE_NAME, u.TENANT_ID, u.TYPE AS TYPE AS TENANT_ID FROM RM_SERVER_INSTANCE s, RM_DATABASE_USER u WHERE s.ID = u.RSS_SERVER_INSTANCE AND u.TENANT_ID = ? AND s.NAME = ? AND s.TENANT_ID = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, this.getCurrentTenantId());
        stmt.setString(2, rssInstance.getName());
        stmt.setInt(2, this.getCurrentTenantId());
        ResultSet rs = stmt.executeQuery();
        List<DatabaseUser> users = new ArrayList<DatabaseUser>();
        while (rs.next()) {
            users.add(this.createDatabaseUserFromRS(rs));
        }
        rs.close();
        stmt.close();
        return users;
    }


    public List<Database> getAllDatabases(int tid) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "SELECT d.ID AS DATABASE_ID, d.NAME, d.TENANT_ID, s.NAME AS RSS_INSTANCE_NAME, s.SERVER_URL, s.TENANT_ID AS RSS_INSTANCE_TENANT_ID, d.TYPE  FROM RM_SERVER_INSTANCE s, RM_DATABASE d WHERE s.ID = d.RSS_INSTANCE_ID AND d.TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tid);
            ResultSet rs = stmt.executeQuery();
            List<Database> result = new ArrayList<Database>();
            while (rs.next()) {
                Database entry = this.createDatabaseFromRS(rs);
                if (entry != null) {
                    result.add(entry);
                }
            }
            rs.close();
            stmt.close();
            return result;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving all databases : " +
                    e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public Database getDatabase(RSSInstance rssInstance, String databaseName) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        Database database = null;
        try {
            int tenantID = this.getCurrentTenantId();
            String sql = "SELECT d.ID AS DATABASE_ID, d.NAME, d.TENANT_ID, s.NAME AS RSS_INSTANCE_NAME, s.SERVER_URL, s.TENANT_ID AS RSS_INSTANCE_TENANT_ID, d.TYPE FROM RM_SERVER_INSTANCE s, RM_DATABASE d WHERE s.ID = d.RSS_INSTANCE_ID AND d.NAME = ? AND d.TENANT_ID = ? AND s.NAME = ? AND s.TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, databaseName);
            stmt.setInt(2, tenantID);
            stmt.setString(3, rssInstance.getName());
            stmt.setInt(4, rssInstance.getTenantId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                database = this.createDatabaseFromRS(rs);
            }
            rs.close();
            stmt.close();

            return database;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving the configuration of " +
                    "database '" + databaseName + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void createDatabase(Database database) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            int rssInstanceTenantId =
                    (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(database.getType())) ?
                            MultitenantConstants.SUPER_TENANT_ID : this.getCurrentTenantId();

            String sql = "INSERT INTO RM_DATABASE SET NAME = ?, RSS_INSTANCE_ID = (SELECT ID FROM RM_SERVER_INSTANCE WHERE NAME = ? AND TENANT_ID = ?), TENANT_ID = ?, TYPE = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, database.getName());
            stmt.setString(2, database.getRssInstanceName());
            stmt.setInt(3, rssInstanceTenantId);
            stmt.setInt(4, this.getCurrentTenantId());
            stmt.setString(5, database.getType());
            stmt.executeUpdate();
            stmt.close();

            //this.setDatabaseInstanceProperties(conn, database);
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while creating the database " +
                    database.getName() + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void dropDatabase(RSSInstance rssInstance, String databaseName, int tenantId)
            throws RSSManagerException {
        Connection conn = null;
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "DELETE FROM RM_DATABASE WHERE NAME = ? AND TENANT_ID = ? AND RSS_INSTANCE_ID = (SELECT ID FROM RM_SERVER_INSTANCE WHERE NAME = ? AND TENANT_ID = ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, databaseName);
            stmt.setInt(2, tenantId);
            if (RSSConfig.getInstance().getRssManager().getRSSInstancePool().
                    isSystemRSSInstance(rssInstance.getName())) {
                stmt.setString(3, rssInstance.getName());
            } else {
                stmt.setString(3, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
            }
            stmt.setInt(4, rssInstance.getTenantId());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while dropping the database '" +
                    databaseName + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void createDatabaseUser(RSSInstance rssInstance, DatabaseUser user) throws RSSManagerException {
        Connection conn = null;
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "INSERT INTO RM_DATABASE_USER SET USERNAME = ?, RSS_INSTANCE_ID = (SELECT ID FROM RM_SERVER_INSTANCE WHERE NAME = ? AND TENANT_ID = ?), TYPE = ?, TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.getUsername());
            stmt.setString(2, rssInstance.getName());
            stmt.setInt(3, rssInstance.getTenantId());
            stmt.setString(4, user.getType());
            stmt.setInt(5, this.getCurrentTenantId());
            stmt.execute();
            stmt.close();

        } catch (Throwable e) {
            throw new RSSManagerException("Error occurred while creating the database user '" +
                    user.getUsername() + "' : " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }


    public void dropDatabaseUser(RSSInstance rssInstance, String username, int tenantId) throws RSSManagerException {
        Connection conn = null;
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "DELETE FROM RM_DATABASE_USER WHERE USERNAME = ? AND RSS_INSTANCE_ID = ? AND TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setInt(2, rssInstance.getId());
            stmt.setInt(3, tenantId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while dropping the database user '" +
                    username + "' : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    public List<DatabaseUser> getAllDatabaseUsers(int tid) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        List<DatabaseUser> users = new ArrayList<DatabaseUser>();
        try {
            String sql = "SELECT u.USERNAME, s.NAME AS RSS_INSTANCE_NAME, u.TENANT_ID, u.TYPE FROM RM_SERVER_INSTANCE s, RM_DATABASE_USER u WHERE s.ID = u.RSS_INSTANCE_ID AND u.TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tid);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(this.createDatabaseUserFromRS(rs));
            }
            rs.close();
            stmt.close();
            return users;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving the database users : " +
                    e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public List<DatabaseUser> getSystemCreatedDatabaseUsers() throws RSSManagerException {
        List<DatabaseUser> users = new ArrayList<DatabaseUser>();
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "SELECT u.USERNAME, u.TYPE, u.TENANT_ID, s.NAME AS RSS_INSTANCE_NAME FROM RM_SERVER_INSTANCE s, RM_DATABASE_USER u WHERE s.ID = u.RSS_INSTANCE_ID AND u.TYPE = ? AND u.TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
            stmt.setInt(2, this.getCurrentTenantId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                DatabaseUser user = this.createDatabaseUserFromRS(rs);
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving the system created " +
                    "database user list : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    @Override
    public List<String> getSystemUsersAssignedToDatabase(
            RSSInstance rssInstance, String databaseName) throws RSSManagerException {
        List<String> users = new ArrayList<String>();
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "SELECT DISTINCT u.USERNAME FROM RM_DATABASE_USER u, RM_USER_DATABASE_ENTRY e WHERE u.ID = e.DATABASE_USER_ID AND u.TYPE = ? AND u.TENANT_ID = ? AND e.DATABASE_ID = (SELECT ID FROM RM_DATABASE WHERE RSS_INSTANCE_ID = ? AND NAME = ? AND TENANT_ID = ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
            stmt.setInt(2, this.getCurrentTenantId());
            stmt.setInt(3, rssInstance.getId());
            stmt.setString(4, databaseName);
            stmt.setInt(5, this.getCurrentTenantId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String user = rs.getString("USERNAME");
                users.add(user);
            }
            return users;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving the system created " +
                    "database user list : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    @Override
    public DatabasePrivilegeSet getSystemUserDatabasePrivileges(RSSInstance rssInstance,
                                                                String databaseName, String username) throws RSSManagerException {
        DatabasePrivilegeSet privileges = null;
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "SELECT * FROM RM_USER_DATABASE_PRIVILEGE WHERE USER_DATABASE_ENTRY_ID = (SELECT ID FROM RM_USER_DATABASE_ENTRY WHERE DATABASE_ID = (SELECT ID FROM RM_DATABASE WHERE NAME = ? AND RSS_INSTANCE_ID = ? AND TENANT_ID = ?) AND DATABASE_USER_ID = (SELECT ID FROM RM_DATABASE_USER WHERE USERNAME = ? AND RSS_INSTANCE_ID = ? AND TENANT_ID = ?))";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, databaseName);
            stmt.setInt(2, rssInstance.getId());
            stmt.setInt(3, this.getCurrentTenantId());
            stmt.setString(4, username);
            stmt.setInt(5, rssInstance.getId());
            stmt.setInt(6, this.getCurrentTenantId());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                privileges = this.createUserDatabasePrivilegeSetFromRS(rs);
            }

            return privileges;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving the database " +
                    "privileges assigned to the user '" + username + "' upon the database '" +
                    databaseName + "' : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    public RSSInstance findRSSInstanceDatabaseBelongsTo(String rssInstanceName,
                                                        String databaseName) throws RSSManagerException {
        RSSInstance rssInstance = null;
        if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) {
            Connection conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "SELECT s.ID, s.NAME, s.SERVER_URL, s.DBMS_TYPE, s.INSTANCE_TYPE, s.SERVER_CATEGORY, s.TENANT_ID, s.ADMIN_USERNAME, s.ADMIN_PASSWORD FROM RM_SERVER_INSTANCE s, RM_DATABASE d WHERE s.ID = d.RSS_INSTANCE_ID AND d.TYPE = ? AND d.TENANT_ID = ? AND d.NAME = ?";
            try {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
                stmt.setInt(2, this.getCurrentTenantId());
                stmt.setString(3, databaseName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    rssInstance = this.createRSSInstanceFromRS(rs);
                }
                rs.close();
                stmt.close();

            } catch (SQLException e) {
                throw new RSSManagerException("Error occurred while retrieving the RSS instance " +
                        "to which the database '" + databaseName + "' belongs to : " +
                        e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        log.error(e);
                    }
                }
            }
        } else {
            rssInstance = this.getRSSInstance(rssInstanceName);
        }
        return rssInstance;
    }

    public boolean isDatabaseExist(String rssInstanceName, String databaseName) throws
            RSSManagerException {
        boolean isExist = false;
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) {
            String sql = "SELECT d.ID AS DATABASE_ID FROM RM_SERVER_INSTANCE s, RM_DATABASE d WHERE s.ID = d.RSS_INSTANCE_ID AND d.TYPE = ? AND d.TENANT_ID = ? AND d.NAME = ?";
            try {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
                stmt.setInt(2, this.getCurrentTenantId());
                stmt.setString(3, databaseName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int databaseId = rs.getInt("DATABASE_ID");
                    if (databaseId > 0) {
                        isExist = true;
                    }
                }
                rs.close();
                stmt.close();
                return isExist;
            } catch (SQLException e) {
                throw new RSSManagerException("Error occurred while retrieving the RSS instance " +
                        "to which the database '" + databaseName + "' belongs to : " +
                        e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        log.error(e);
                    }
                }
            }
        } else {
            String sql = "SELECT d.ID AS DATABASE_ID FROM RM_SERVER_INSTANCE s, RM_DATABASE d WHERE s.ID = d.RSS_INSTANCE_ID AND s.NAME = ? AND d.TYPE = ? AND d.TENANT_ID = ? AND d.NAME = ?";
            try {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, rssInstanceName);
                stmt.setString(2, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
                stmt.setInt(3, this.getCurrentTenantId());
                stmt.setString(4, databaseName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int databaseId = rs.getInt("DATABASE_ID");
                    if (databaseId > 0) {
                        isExist = true;
                    }
                }
                rs.close();
                stmt.close();
                return isExist;
            } catch (SQLException e) {
                throw new RSSManagerException("Error occurred while retrieving the RSS instance " +
                        "to which the database '" + databaseName + "' belongs to : " +
                        e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        log.error(e);
                    }
                }
            }
        }
    }

    public boolean isDatabaseUserExist(String rssInstanceName, String databaseUsername) throws
            RSSManagerException {
        boolean isExist = false;
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) {
            String sql = "SELECT u.ID AS DATABASE_USER_ID FROM RM_SERVER_INSTANCE s, RM_DATABASE_USER u WHERE s.ID = u.RSS_INSTANCE_ID AND u.TYPE = ? AND u.TENANT_ID = ? AND u.USERNAME = ?";
            try {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
                stmt.setInt(2, this.getCurrentTenantId());
                stmt.setString(3, databaseUsername);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int databaseId = rs.getInt("DATABASE_USER_ID");
                    if (databaseId > 0) {
                        isExist = true;
                    }
                }
                rs.close();
                stmt.close();
                return isExist;
            } catch (SQLException e) {
                throw new RSSManagerException("Error occurred while checking the existence of " +
                        "the database user '" + databaseUsername + "' : " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        log.error(e);
                    }
                }
            }
        } else {
            String sql = "SELECT u.ID AS DATABASE_USER_ID FROM RM_SERVER_INSTANCE s, RM_DATABASE_USER u WHERE s.ID = u.RSS_INSTANCE_ID AND s.NAME = ? AND u.TYPE = ? AND u.TENANT_ID = ? AND u.USERNAME = ?";
            try {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, rssInstanceName);
                stmt.setString(2, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
                stmt.setInt(3, this.getCurrentTenantId());
                stmt.setString(4, databaseUsername);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int databaseId = rs.getInt("DATABASE_USER_ID");
                    if (databaseId > 0) {
                        isExist = true;
                    }
                }
                rs.close();
                stmt.close();
                return isExist;
            } catch (SQLException e) {
                throw new RSSManagerException("Error occurred while checking the existence of " +
                        "the database user '" + databaseUsername + "' : " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        log.error(e);
                    }
                }
            }
        }
    }

    public RSSInstance findRSSInstanceDatabaseUserBelongsTo(String rssInstanceName,
                                                            String username) throws RSSManagerException {
        RSSInstance rssInstance = null;
        if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) {
            Connection conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "SELECT s.ID, s.NAME, s.SERVER_URL, s.DBMS_TYPE, s.INSTANCE_TYPE, s.SERVER_CATEGORY, s.ADMIN_USERNAME, s.ADMIN_PASSWORD, s.TENANT_ID FROM RM_SERVER_INSTANCE s, RM_DATABASE_USER u WHERE s.ID = u.RSS_INSTANCE_ID AND u.TYPE = ? AND u.TENANT_ID = ? AND u.USERNAME = ?";
            try {
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
                stmt.setInt(2, this.getCurrentTenantId());
                stmt.setString(3, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    rssInstance = this.createRSSInstanceFromRS(rs);
                }
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                throw new RSSManagerException("Error occurred while retrieving the RSS instance " +
                        "to which the database user '" + username + "' belongs to : " +
                        e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        log.error(e);
                    }
                }
            }
        } else {
            rssInstance = this.getRSSInstance(rssInstanceName);
        }
        return rssInstance;
    }

    public List<DatabaseUser> getUsersByRSSInstance(RSSInstance rssInstance) throws
            RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        List<DatabaseUser> users = new ArrayList<DatabaseUser>();
        try {
            String sql = "SELECT u.USERNAME, s.NAME AS RSS_INSTANCE_NAME, u.TENANT_ID, u.TYPE FROM RM_SERVER_INSTANCE s, RM_DATABASE_USER u WHERE s.ID = u.RSS_INSTANCE_ID AND s.NAME = ? AND s.TENANT_ID = ? AND u.TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rssInstance.getName());
            stmt.setInt(2, rssInstance.getTenantId());
            stmt.setInt(3, this.getCurrentTenantId());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(this.createDatabaseUserFromRS(rs));
            }
            rs.close();
            stmt.close();

            return users;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving the database users : " +
                    e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public List<String> getUsersAssignedToDatabase(RSSInstance rssInstance,
                                                   String databaseName) throws RSSManagerException {
        List<String> attachedUsers = new ArrayList<String>();
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "SELECT p.USERNAME FROM RM_USER_DATABASE_ENTRY e, (SELECT t.ID AS DATABASE_ID, u.ID AS DATABASE_USER_ID, u.USERNAME FROM RM_DATABASE_USER u,(SELECT d.RSS_INSTANCE_ID, d.NAME, d.ID FROM RM_SERVER_INSTANCE s, RM_DATABASE d WHERE s.ID = d.RSS_INSTANCE_ID AND s.NAME = ? AND s.TENANT_ID = ? AND d.NAME = ? AND d.TENANT_ID = ?) t WHERE u.RSS_INSTANCE_ID = t.RSS_INSTANCE_ID AND TENANT_ID = ?) p WHERE e.DATABASE_USER_ID = p.DATABASE_USER_ID AND e.DATABASE_ID = p.DATABASE_ID";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rssInstance.getName());
            stmt.setInt(2, rssInstance.getTenantId());
            stmt.setString(3, databaseName);
            stmt.setInt(4, this.getCurrentTenantId());
            stmt.setInt(5, this.getCurrentTenantId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                attachedUsers.add(rs.getString("USERNAME"));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving the users assigned " +
                    "to the database '" + databaseName + "' : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
        return attachedUsers;
    }

    public UserDatabaseEntry createUserDatabaseEntry(RSSInstance rssInstance, Database database,
                                                     String username) throws RSSManagerException {
        Connection conn = null;
        UserDatabaseEntry ude = new UserDatabaseEntry(-1, -1, username, database.getId(), database.getName(),
                rssInstance.getId(), rssInstance.getName());
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "INSERT INTO RM_USER_DATABASE_ENTRY SET DATABASE_USER_ID = (SELECT ID FROM RM_DATABASE_USER WHERE RSS_INSTANCE_ID = ? AND TENANT_ID = ? AND USERNAME = ?), DATABASE_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, rssInstance.getId());
            stmt.setInt(2, this.getCurrentTenantId());
            stmt.setString(3, username);
            stmt.setInt(4, database.getId());
            int rowsCreated = stmt.executeUpdate();

            if (rowsCreated == 0) {
                throw new RSSManagerException("Failed to attach database user '" +
                        username + "' was not attached to the database '" +
                        database.getName() + "'");
            }
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                ude.setId(rs.getInt(1));
            }
            stmt.close();
            return ude;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while adding new user-database-entry : " +
                    e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    public void deleteUserDatabaseEntry(RSSInstance rssInstance, String username)
            throws RSSManagerException {
        Connection conn = null;
        try {
            /* now delete the user-database-entry */
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "DELETE FROM RM_USER_DATABASE_ENTRY WHERE DATABASE_USER_ID = (SELECT ID FROM RM_DATABASE_USER WHERE RSS_INSTANCE_ID = ? AND USERNAME = ? AND TENANT_ID = ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, rssInstance.getId());
            stmt.setString(2, username);
            stmt.setInt(3, this.getCurrentTenantId());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while deleting user-database-entry : " +
                    e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    public void deleteUserDatabasePrivileges(RSSInstance rssInstance,
                                             String username) throws RSSManagerException {
        Connection conn = null;
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            /* delete permissions first */
            String sql = "DELETE FROM RM_USER_DATABASE_PRIVILEGE WHERE USER_DATABASE_ENTRY_ID IN (SELECT ID FROM RM_USER_DATABASE_ENTRY WHERE DATABASE_USER_ID = (SELECT ID FROM RM_DATABASE_USER WHERE RSS_INSTANCE_ID = ? AND USERNAME = ? AND TENANT_ID = ?))";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, rssInstance.getId());
            stmt.setString(2, username);
            stmt.setInt(3, this.getCurrentTenantId());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while deleting user database " +
                    "privileges of the database user '" + username + "' : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    public int getSystemRSSDatabaseCount() throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        int count = 0;
        try {
            String sql = "SELECT COUNT FROM RM_SYSTEM_DATABASE_COUNT";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
            rs.close();
            stmt.close();

            return count;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving system RSS database " +
                    "count : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public DatabasePrivilegeSet getUserDatabasePrivileges(RSSInstance rssInstance,
                                                          String databaseName, String username)
            throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        DatabasePrivilegeSet privileges = new DatabasePrivilegeSet();
        PreparedStatement stmt;
        try {
            String sql = "SELECT * FROM RM_USER_DATABASE_PRIVILEGE WHERE USER_DATABASE_ENTRY_ID = (SELECT ID FROM RM_USER_DATABASE_ENTRY WHERE DATABASE_ID = (SELECT ID FROM RM_DATABASE WHERE NAME = ? AND RSS_INSTANCE_ID = ? AND TENANT_ID = ?) AND DATABASE_USER_ID = (SELECT ID FROM RM_DATABASE_USER WHERE USERNAME = ? AND RSS_INSTANCE_ID = ? AND TENANT_ID = ?))";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, databaseName);
            stmt.setInt(2, rssInstance.getId());
            stmt.setInt(3, this.getCurrentTenantId());
            stmt.setString(4, username);
            stmt.setInt(5, rssInstance.getTenantId());
            stmt.setInt(6, this.getCurrentTenantId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                privileges = this.createUserDatabasePrivilegeSetFromRS(rs);
            }
            rs.close();
            stmt.close();

            return privileges;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving user permissions " +
                    "granted for the database user '" + username + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }

    }

    public List<RSSInstance> getAllRSSInstances(int tid) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "SELECT * FROM RM_SERVER_INSTANCE WHERE TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tid);
            ResultSet rs = stmt.executeQuery();
            List<RSSInstance> result = new ArrayList<RSSInstance>();
            while (rs.next()) {
                result.add(this.createRSSInstanceFromRS(rs));
            }
            rs.close();
            stmt.close();
            return result;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving all RSS instances : " +
                    e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void removeUserDatabaseEntriesByDatabase(
            RSSInstance rssInstance, String databaseName, int tenantId) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "SELECT DISTINCT d.ID FROM RM_USER_DATABASE_ENTRY e, RM_DATABASE d WHERE d.ID = e.DATABASE_ID AND d.NAME = ? AND d.RSS_INSTANCE_ID = ? AND d.TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, databaseName);
            stmt.setInt(2, rssInstance.getId());
            stmt.setInt(3, tenantId);
            ResultSet rs = stmt.executeQuery();
            int databaseId = -1;
            if (rs.next()) {
                databaseId = rs.getInt("ID");
            }
            rs.close();
            stmt.close();

            sql = "DELETE FROM RM_USER_DATABASE_ENTRY WHERE DATABASE_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, databaseId);
            stmt.execute();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while removing the user database " +
                    "entries : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void removeUserDatabaseEntriesByDatabaseUser(
            RSSInstance rssInstance, String username, int tenantId) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "SELECT DISTINCT u.ID FROM RM_USER_DATABASE_ENTRY e, RM_DATABASE_USER u WHERE u.ID = e.DATABASE_USER_ID AND u.USERNAME = ? AND u.RSS_INSTANCE_ID = ? AND u.TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setInt(2, rssInstance.getId());
            stmt.setInt(3, tenantId);
            ResultSet rs = stmt.executeQuery();
            int databaseId = -1;
            if (rs.next()) {
                databaseId = rs.getInt("ID");
            }
            rs.close();
            stmt.close();

            sql = "DELETE FROM RM_USER_DATABASE_ENTRY WHERE DATABASE_USER_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, databaseId);
            stmt.execute();
            stmt.close();

        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while removing the user database " +
                    "entries : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void deleteUserDatabasePrivilegeEntriesByDatabaseUser(RSSInstance rssInstance, String username,
                                                                 int tenantId) throws RSSManagerException {
        Connection conn = null;
        try {
            /* delete permissions first */
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "DELETE FROM RM_USER_DATABASE_PRIVILEGE WHERE USER_DATABASE_ENTRY_ID IN (SELECT ID FROM RM_USER_DATABASE_ENTRY WHERE DATABASE_USER_ID = (SELECT ID FROM RM_DATABASE_USER WHERE RSS_INSTANCE_ID = ? AND USERNAME = ? AND TENANT_ID = ?))";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, rssInstance.getId());
            stmt.setString(2, username);
            stmt.setInt(3, tenantId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while deleting user database " +
                    "privileges assigned to the database user '" + username + "' : " +
                    e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    public void updateDatabaseUser(DatabasePrivilegeSet privileges, RSSInstance rssInstance,
                                   DatabaseUser user, String databaseName) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "UPDATE RM_USER_DATABASE_PRIVILEGE SET SELECT_PRIV = ?, INSERT_PRIV = ?, UPDATE_PRIV = ?, DELETE_PRIV = ?, CREATE_PRIV = ?, DROP_PRIV = ?, GRANT_PRIV = ?, REFERENCES_PRIV = ?, INDEX_PRIV = ?, ALTER_PRIV = ?, CREATE_TMP_TABLE_PRIV = ?, LOCK_TABLES_PRIV = ?, CREATE_VIEW_PRIV = ?, SHOW_VIEW_PRIV = ?, CREATE_ROUTINE_PRIV = ?, ALTER_ROUTINE_PRIV = ?, EXECUTE_PRIV = ?, EVENT_PRIV = ?, TRIGGER_PRIV = ? WHERE USER_DATABASE_ENTRY_ID = (SELECT ID FROM RM_USER_DATABASE_ENTRY WHERE DATABASE_ID = (SELECT ID FROM RM_DATABASE WHERE NAME = ? AND TENANT_ID = ? AND RSS_INSTANCE_ID = ?) AND DATABASE_USER_ID = (SELECT ID FROM RM_DATABASE_USER WHERE USERNAME = ? AND TENANT_ID = ? AND RSS_INSTANCE_ID = ?))";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, privileges.getSelectPriv());
            stmt.setString(2, privileges.getInsertPriv());
            stmt.setString(3, privileges.getUpdatePriv());
            stmt.setString(4, privileges.getDeletePriv());
            stmt.setString(5, privileges.getCreatePriv());
            stmt.setString(6, privileges.getDropPriv());
            stmt.setString(7, privileges.getGrantPriv());
            stmt.setString(8, privileges.getReferencesPriv());
            stmt.setString(9, privileges.getIndexPriv());
            stmt.setString(10, privileges.getAlterPriv());
            stmt.setString(11, privileges.getCreateTmpTablePriv());
            stmt.setString(12, privileges.getLockTablesPriv());
            stmt.setString(13, privileges.getCreateViewPriv());
            stmt.setString(14, privileges.getShowViewPriv());
            stmt.setString(15, privileges.getCreateRoutinePriv());
            stmt.setString(16, privileges.getAlterRoutinePriv());
            stmt.setString(17, privileges.getExecutePriv());
            stmt.setString(18, privileges.getEventPriv());
            stmt.setString(19, privileges.getTriggerPriv());
            stmt.setString(20, databaseName);
            stmt.setInt(21, this.getCurrentTenantId());
            stmt.setInt(22, rssInstance.getId());
            stmt.setString(23, user.getUsername());
            stmt.setInt(24, this.getCurrentTenantId());
            stmt.setInt(25, rssInstance.getId());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while updating database privileges " +
                    "of the user '" + user.getUsername() + "' : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    public DatabasePrivilegeTemplate createDatabasePrivilegesTemplate(
            DatabasePrivilegeTemplate template) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            String sql = "INSERT INTO RM_DB_PRIVILEGE_TEMPLATE(NAME, TENANT_ID) VALUES(?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, template.getName());
            stmt.setInt(2, this.getCurrentTenantId());
            int rowsCreated = stmt.executeUpdate();

            if (rowsCreated == 0) {
                throw new RSSManagerException("Database privilege was not created");
            }
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                template.setId(rs.getInt(1));
            }
            stmt.close();
            return template;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while creating database privilege " +
                    "template '" + template.getName() + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public boolean isDatabasePrivilegeTemplateExist(String templateName) throws RSSManagerException {
        Connection conn = null;
        boolean isExist = false;
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "SELECT ID FROM RM_DB_PRIVILEGE_TEMPLATE WHERE NAME = ? AND TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, templateName);
            stmt.setInt(2, this.getCurrentTenantId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int templateId = rs.getInt("ID");
                if (templateId > 0) {
                   isExist = true;
                }
            }
            return isExist;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while checking the existence " +
                    "of database privilege template '" + templateName + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    public void setDatabasePrivilegeTemplateProperties(
            DatabasePrivilegeTemplate template) throws RSSManagerException {
        DatabasePrivilegeSet privileges = template.getPrivileges();
        Connection conn = null;
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "INSERT INTO RM_DB_PRIVILEGE_TEMPLATE_ENTRY(TEMPLATE_ID, SELECT_PRIV, INSERT_PRIV, UPDATE_PRIV, DELETE_PRIV, CREATE_PRIV, DROP_PRIV, GRANT_PRIV, REFERENCES_PRIV, INDEX_PRIV, ALTER_PRIV, CREATE_TMP_TABLE_PRIV, LOCK_TABLES_PRIV, CREATE_VIEW_PRIV, SHOW_VIEW_PRIV, CREATE_ROUTINE_PRIV, ALTER_ROUTINE_PRIV, EXECUTE_PRIV, EVENT_PRIV, TRIGGER_PRIV) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, template.getId());
            stmt.setString(2, privileges.getSelectPriv());
            stmt.setString(3, privileges.getInsertPriv());
            stmt.setString(4, privileges.getUpdatePriv());
            stmt.setString(5, privileges.getDeletePriv());
            stmt.setString(6, privileges.getCreatePriv());
            stmt.setString(7, privileges.getDropPriv());
            stmt.setString(8, privileges.getGrantPriv());
            stmt.setString(9, privileges.getReferencesPriv());
            stmt.setString(10, privileges.getIndexPriv());
            stmt.setString(11, privileges.getAlterPriv());
            stmt.setString(12, privileges.getCreateTmpTablePriv());
            stmt.setString(13, privileges.getLockTablesPriv());
            stmt.setString(14, privileges.getCreateViewPriv());
            stmt.setString(15, privileges.getShowViewPriv());
            stmt.setString(16, privileges.getCreateRoutinePriv());
            stmt.setString(17, privileges.getAlterRoutinePriv());
            stmt.setString(18, privileges.getExecutePriv());
            stmt.setString(19, privileges.getEventPriv());
            stmt.setString(20, privileges.getTriggerPriv());
            stmt.executeUpdate();

            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred setting database privilege template " +
                    "properties : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    public void setUserDatabasePrivileges(
            UserDatabaseEntry entry, DatabasePrivilegeTemplate template) throws RSSManagerException {
        DatabasePrivilegeSet privileges = template.getPrivileges();
        Connection conn = null;
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "INSERT INTO RM_USER_DATABASE_PRIVILEGE(USER_DATABASE_ENTRY_ID, SELECT_PRIV, INSERT_PRIV, UPDATE_PRIV, DELETE_PRIV, CREATE_PRIV, DROP_PRIV, GRANT_PRIV, REFERENCES_PRIV, INDEX_PRIV, ALTER_PRIV, CREATE_TMP_TABLE_PRIV, LOCK_TABLES_PRIV, CREATE_VIEW_PRIV, SHOW_VIEW_PRIV, CREATE_ROUTINE_PRIV, ALTER_ROUTINE_PRIV, EXECUTE_PRIV, EVENT_PRIV, TRIGGER_PRIV) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, entry.getId());
            stmt.setString(2, privileges.getSelectPriv());
            stmt.setString(3, privileges.getInsertPriv());
            stmt.setString(4, privileges.getUpdatePriv());
            stmt.setString(5, privileges.getDeletePriv());
            stmt.setString(6, privileges.getCreatePriv());
            stmt.setString(7, privileges.getDropPriv());
            stmt.setString(8, privileges.getGrantPriv());
            stmt.setString(9, privileges.getReferencesPriv());
            stmt.setString(10, privileges.getIndexPriv());
            stmt.setString(11, privileges.getAlterPriv());
            stmt.setString(12, privileges.getCreateTmpTablePriv());
            stmt.setString(13, privileges.getLockTablesPriv());
            stmt.setString(14, privileges.getCreateViewPriv());
            stmt.setString(15, privileges.getShowViewPriv());
            stmt.setString(16, privileges.getCreateRoutinePriv());
            stmt.setString(17, privileges.getAlterRoutinePriv());
            stmt.setString(18, privileges.getExecutePriv());
            stmt.setString(19, privileges.getEventPriv());
            stmt.setString(20, privileges.getTriggerPriv());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while setting user database " +
                    "privileges for the database user '" + entry.getUsername() + "' on database '" +
                    entry.getDatabaseName() + "' : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * Drops a database privilege template carriying the given name which belongs to the currently
     * logged in tenant.
     *
     * @param templateName Name of the database privilege template to be deleted
     * @throws RSSManagerException Is thrown in case of an unexpected error such as database access
     *                             failure, etc.
     */
    public void dropDatabasePrivilegesTemplate(String templateName) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        try {
            int tenantId = this.getCurrentTenantId();
            String sql = "DELETE FROM RM_DB_PRIVILEGE_TEMPLATE WHERE NAME = ? AND TENANT_ID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, templateName);
            stmt.setInt(2, tenantId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while dropping the database privilege " +
                    "template '" + templateName + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    /**
     * Remotes the permission entries assigned to a particular database privilege template.
     *
     * @param templateName Name of the database template associated with the permissions
     * @param tenantId     Id of the currently logged in tenant
     * @throws RSSManagerException Is thrown if any unexpected error occurs
     */
    public void removeDatabasePrivilegesTemplateEntries(String templateName, int tenantId) throws RSSManagerException {
        Connection conn = null;
        try {
            conn = RSSConfig.getInstance().getRSSDBConnection();
            String sql = "DELETE FROM RM_DB_PRIVILEGE_TEMPLATE_ENTRY WHERE TEMPLATE_ID = (SELECT ID FROM RM_DB_PRIVILEGE_TEMPLATE WHERE NAME = ? AND TENANT_ID = ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, templateName);
            stmt.setInt(2, tenantId);
            stmt.executeUpdate();

            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while removing database privilege " +
                    "template entries : " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * Updates the database permissions enabled in a particular database privilege template.
     *
     * @param template Name of the associated database privilege template
     * @throws RSSManagerException Is thrown if any unexpected error occurs
     */
    public void editDatabasePrivilegesTemplate(
            DatabasePrivilegeTemplate template) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        DatabasePrivilegeSet privileges = template.getPrivileges();
        try {
            String sql = "UPDATE RM_DB_PRIVILEGE_TEMPLATE_ENTRY SET SELECT_PRIV = ?, INSERT_PRIV = ?, UPDATE_PRIV = ?, DELETE_PRIV = ?, CREATE_PRIV = ?, DROP_PRIV = ?, GRANT_PRIV = ?, REFERENCES_PRIV = ?, INDEX_PRIV = ?, ALTER_PRIV = ?, CREATE_TMP_TABLE_PRIV = ?, LOCK_TABLES_PRIV = ?, CREATE_VIEW_PRIV = ?, SHOW_VIEW_PRIV = ?, CREATE_ROUTINE_PRIV = ?, ALTER_ROUTINE_PRIV = ?, EXECUTE_PRIV = ?, EVENT_PRIV = ?, TRIGGER_PRIV = ? WHERE TEMPLATE_ID = (SELECT ID FROM RM_DB_PRIVILEGE_TEMPLATE WHERE NAME = ? AND TENANT_ID = ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, privileges.getSelectPriv());
            stmt.setString(2, privileges.getInsertPriv());
            stmt.setString(3, privileges.getUpdatePriv());
            stmt.setString(4, privileges.getDeletePriv());
            stmt.setString(5, privileges.getCreatePriv());
            stmt.setString(6, privileges.getDropPriv());
            stmt.setString(7, privileges.getGrantPriv());
            stmt.setString(8, privileges.getReferencesPriv());
            stmt.setString(9, privileges.getIndexPriv());
            stmt.setString(10, privileges.getAlterPriv());
            stmt.setString(11, privileges.getCreateTmpTablePriv());
            stmt.setString(12, privileges.getLockTablesPriv());
            stmt.setString(13, privileges.getCreateViewPriv());
            stmt.setString(14, privileges.getShowViewPriv());
            stmt.setString(15, privileges.getCreateRoutinePriv());
            stmt.setString(16, privileges.getAlterRoutinePriv());
            stmt.setString(17, privileges.getExecutePriv());
            stmt.setString(18, privileges.getEventPriv());
            stmt.setString(19, privileges.getTriggerPriv());
            stmt.setString(20, template.getName());
            stmt.setInt(21, this.getCurrentTenantId());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while editing the database privilege " +
                    "template '" + template.getName() + "' : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    /**
     * Retrieves all the database privilege template entries created by the tenant which carries
     * the given id.
     *
     * @param tid Id of the logged in tenant
     * @return The list of database privilege templates belong to the given
     *         tenant
     * @throws RSSManagerException Is thrown if any unexpected error occurs
     */
    public List<DatabasePrivilegeTemplate> getAllDatabasePrivilegesTemplates(int tid) throws
            RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        PreparedStatement stmt;
        try {
            String sql = "SELECT p.ID, p.NAME, p.TENANT_ID, e.SELECT_PRIV, e.INSERT_PRIV, e.UPDATE_PRIV, e.DELETE_PRIV, e.CREATE_PRIV, e.DROP_PRIV, e.GRANT_PRIV, e.REFERENCES_PRIV, e.INDEX_PRIV, e.ALTER_PRIV, e.CREATE_TMP_TABLE_PRIV, e.LOCK_TABLES_PRIV, e.CREATE_VIEW_PRIV, e.SHOW_VIEW_PRIV, e.CREATE_ROUTINE_PRIV, e.ALTER_ROUTINE_PRIV, e.EXECUTE_PRIV, e.EVENT_PRIV, e.TRIGGER_PRIV FROM RM_DB_PRIVILEGE_TEMPLATE p, RM_DB_PRIVILEGE_TEMPLATE_ENTRY e WHERE p.ID = e.TEMPLATE_ID AND p.TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, tid);
            ResultSet rs = stmt.executeQuery();
            List<DatabasePrivilegeTemplate> result = new ArrayList<DatabasePrivilegeTemplate>();
            while (rs.next()) {
                result.add(this.createDatabasePrivilegeTemplateFromRS(rs));
            }
            rs.close();
            stmt.close();

            return result;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving database privilege " +
                    "templates : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    /**
     * Retrieves the database privilege information associated with the template which carries the
     * given template name.
     *
     * @param templateName Name of the database privilege template to be retrieved
     * @return Database privilege template information
     * @throws RSSManagerException Is thrown if any unexpected error occurs
     */
    public DatabasePrivilegeTemplate getDatabasePrivilegesTemplate(
            String templateName) throws RSSManagerException {
        Connection conn = RSSConfig.getInstance().getRSSDBConnection();
        PreparedStatement stmt;
        DatabasePrivilegeTemplate template = null;
        try {
            String sql = "SELECT p.ID, p.NAME, p.TENANT_ID, e.SELECT_PRIV, e.INSERT_PRIV, e.UPDATE_PRIV, e.DELETE_PRIV, e.CREATE_PRIV, e.DROP_PRIV, e.GRANT_PRIV, e.REFERENCES_PRIV, e.INDEX_PRIV, e.ALTER_PRIV, e.CREATE_TMP_TABLE_PRIV, e.LOCK_TABLES_PRIV, e.CREATE_VIEW_PRIV, e.SHOW_VIEW_PRIV, e.CREATE_ROUTINE_PRIV, e.ALTER_ROUTINE_PRIV, e.EXECUTE_PRIV, e.EVENT_PRIV, e.TRIGGER_PRIV FROM RM_DB_PRIVILEGE_TEMPLATE p, RM_DB_PRIVILEGE_TEMPLATE_ENTRY e WHERE p.ID = e.TEMPLATE_ID AND p.NAME = ? AND p.TENANT_ID = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, templateName);
            stmt.setInt(2, this.getCurrentTenantId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                template = this.createDatabasePrivilegeTemplateFromRS(rs);
            }
            rs.close();
            stmt.close();

            return template;
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while retrieving database privilege " +
                    "template information : " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                log.error(e);
            }
        }
    }

    /**
     * Extracts the assigned values for the database permissions from a result set.
     *
     * @param rs Result set carrying the database permission information
     * @return Database privilege set wrapping the returned result
     * @throws SQLException Is thrown if any unexpected error occurs while retrieving the values
     *                      from the result set
     */
    private DatabasePrivilegeSet createUserDatabasePrivilegeSetFromRS(ResultSet rs) throws
            SQLException {
        DatabasePrivilegeSet privileges = new DatabasePrivilegeSet();
        privileges.setSelectPriv(rs.getString("SELECT_PRIV"));
        privileges.setInsertPriv(rs.getString("INSERT_PRIV"));
        privileges.setUpdatePriv(rs.getString("UPDATE_PRIV"));
        privileges.setDeletePriv(rs.getString("DELETE_PRIV"));
        privileges.setCreatePriv(rs.getString("CREATE_PRIV"));
        privileges.setDropPriv(rs.getString("DROP_PRIV"));
        privileges.setGrantPriv(rs.getString("GRANT_PRIV"));
        privileges.setReferencesPriv(rs.getString("REFERENCES_PRIV"));
        privileges.setIndexPriv(rs.getString("INDEX_PRIV"));
        privileges.setAlterPriv(rs.getString("ALTER_PRIV"));
        privileges.setCreateTmpTablePriv(rs.getString("CREATE_TMP_TABLE_PRIV"));
        privileges.setLockTablesPriv(rs.getString("LOCK_TABLES_PRIV"));
        privileges.setCreateViewPriv(rs.getString("CREATE_VIEW_PRIV"));
        privileges.setShowViewPriv(rs.getString("SHOW_VIEW_PRIV"));
        privileges.setCreateRoutinePriv(rs.getString("CREATE_ROUTINE_PRIV"));
        privileges.setAlterRoutinePriv(rs.getString("ALTER_ROUTINE_PRIV"));
        privileges.setExecutePriv(rs.getString("EXECUTE_PRIV"));
        privileges.setEventPriv(rs.getString("EVENT_PRIV"));
        privileges.setTriggerPriv(rs.getString("TRIGGER_PRIV"));

        return privileges;
    }

    /**
     * Extracts the database privilege template information from a result set.
     *
     * @param rs Result set carrying the database privilege template information
     * @return Database privilege template object wrapping the returned result
     * @throws SQLException        Is thrown if any unexpected error occurs while retrieving the
     *                             values from the result set
     * @throws RSSManagerException Is thrown if any unexpected error occurs while retrieving the
     *                             values from the result set
     */
    private DatabasePrivilegeTemplate createDatabasePrivilegeTemplateFromRS(ResultSet rs) throws
            SQLException, RSSManagerException {
        int id = rs.getInt("ID");
        String templateName = rs.getString("NAME");
        DatabasePrivilegeSet privileges = new DatabasePrivilegeSet();
        privileges.setSelectPriv(rs.getString("SELECT_PRIV"));
        privileges.setInsertPriv(rs.getString("INSERT_PRIV"));
        privileges.setUpdatePriv(rs.getString("UPDATE_PRIV"));
        privileges.setDeletePriv(rs.getString("DELETE_PRIV"));
        privileges.setCreatePriv(rs.getString("CREATE_PRIV"));
        privileges.setDropPriv(rs.getString("DROP_PRIV"));
        privileges.setGrantPriv(rs.getString("GRANT_PRIV"));
        privileges.setReferencesPriv(rs.getString("REFERENCES_PRIV"));
        privileges.setIndexPriv(rs.getString("INDEX_PRIV"));
        privileges.setAlterPriv(rs.getString("ALTER_PRIV"));
        privileges.setCreateTmpTablePriv(rs.getString("CREATE_TMP_TABLE_PRIV"));
        privileges.setLockTablesPriv(rs.getString("LOCK_TABLES_PRIV"));
        privileges.setCreateViewPriv(rs.getString("CREATE_VIEW_PRIV"));
        privileges.setShowViewPriv(rs.getString("SHOW_VIEW_PRIV"));
        privileges.setCreateRoutinePriv(rs.getString("CREATE_ROUTINE_PRIV"));
        privileges.setAlterRoutinePriv(rs.getString("ALTER_ROUTINE_PRIV"));
        privileges.setExecutePriv(rs.getString("EXECUTE_PRIV"));
        privileges.setEventPriv(rs.getString("EVENT_PRIV"));
        privileges.setTriggerPriv(rs.getString("TRIGGER_PRIV"));

        return new DatabasePrivilegeTemplate(id, templateName, privileges);
    }

    private Database createDatabaseFromRS(ResultSet rs) throws SQLException,
            RSSManagerException {
        int id = rs.getInt("DATABASE_ID");
        String dbName = rs.getString("NAME");
        int dbTenantId = rs.getInt("TENANT_ID");
        String rssName = rs.getString("RSS_INSTANCE_NAME");
        String rssServerUrl = rs.getString("SERVER_URL");
        int rssTenantId = rs.getInt("RSS_INSTANCE_TENANT_ID");
        String type = rs.getString("TYPE");

        if (rssTenantId == MultitenantConstants.SUPER_TENANT_ID &&
                dbTenantId != MultitenantConstants.SUPER_TENANT_ID) {
            rssName = RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE;
        }
        String url = rssServerUrl + "/" + dbName;
        return new Database(id, dbName, rssName, url, type, rssTenantId);
    }

    private RSSInstance createRSSInstanceFromRS(ResultSet rs) throws SQLException {
        int id = rs.getInt("ID");
        String name = rs.getString("NAME");
        String serverURL = rs.getString("SERVER_URL");
        String instanceType = rs.getString("INSTANCE_TYPE");
        String serverCategory = rs.getString("SERVER_CATEGORY");
        String adminUsername = rs.getString("ADMIN_USERNAME");
        String adminPassword = rs.getString("ADMIN_PASSWORD");
        String dbmsType = rs.getString("DBMS_TYPE");
        int tenantId = rs.getInt("TENANT_ID");
        return new RSSInstance(id, name, serverURL, dbmsType, instanceType, serverCategory,
                adminUsername, adminPassword, tenantId);
    }

    private DatabaseUser createDatabaseUserFromRS(ResultSet rs) throws SQLException {
        String username = rs.getString("USERNAME");
        String rssInstName = rs.getString("RSS_INSTANCE_NAME");
        int tenantId = rs.getInt("TENANT_ID");
        String type = rs.getString("TYPE");
        return new DatabaseUser(username, null, rssInstName, type, tenantId);
    }

    private int getCurrentTenantId() {
        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }

}
