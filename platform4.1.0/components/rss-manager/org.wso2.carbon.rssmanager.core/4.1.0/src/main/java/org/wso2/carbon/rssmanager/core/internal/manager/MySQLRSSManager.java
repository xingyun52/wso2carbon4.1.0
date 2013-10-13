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
package org.wso2.carbon.rssmanager.core.internal.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.rssmanager.common.RSSManagerConstants;
import org.wso2.carbon.rssmanager.core.RSSManagerException;
import org.wso2.carbon.rssmanager.core.entity.*;
import org.wso2.carbon.rssmanager.core.internal.util.RSSConfig;
import org.wso2.carbon.rssmanager.core.internal.util.RSSManagerUtil;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MySQLRSSManager extends RSSManager {

    private static RSSManager rssManager = new MySQLRSSManager();
    private static final Log log = LogFactory.getLog(MySQLRSSManager.class);

    private MySQLRSSManager() {
    }

    public static synchronized RSSManager getMySQLRSSManager() {
        return rssManager;
    }

    @Override
    public void createDatabase(Database database) throws RSSManagerException {
        Connection conn = null;
        RSSInstance rssIns = this.lookupRSSInstance(database.getRssInstanceName());
        if (this.isInTransaction()) {
            this.endTransaction();
        }
        if (rssIns == null) {
            throw new RSSManagerException("RSS instance " + database.getRssInstanceName() +
                    " does not exist");
        }
        String qualifiedDatabaseName =
                    RSSManagerUtil.getFullyQualifiedDatabaseName(database.getName());
        try {
            conn = rssIns.getDataSource().getConnection();
            conn.setAutoCommit(false);
            String sql = "CREATE DATABASE " + qualifiedDatabaseName;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.execute();

            this.beginTransaction();
            database.setName(qualifiedDatabaseName);
            database.setRssInstanceName(rssIns.getName());
            String databaseUrl = RSSManagerUtil.composeDatabaseUrl(rssIns, qualifiedDatabaseName);
            database.setUrl(databaseUrl);
            database.setType(this.inferEntityType(rssIns.getName()));
            /* Sets the tenant id under which the database is created */
            database.setTenantId(this.getCurrentTenantId());

            /* creates a reference to the database inside the metadata repository */
            this.getDAO().createDatabase(database);
            this.getDAO().incrementSystemRSSDatabaseCount();
            this.endTransaction();

            /* committing the changes to RSS instance */
            conn.commit();
            stmt.close();
        } catch (SQLException e) {
            if (this.isInTransaction()) {
                this.rollbackTransaction();
            }
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error(e1);
                }
            }
            throw new RSSManagerException("Error while creating the database '" +
                     qualifiedDatabaseName + "' on RSS instance '" + rssIns.getName() + "' : " +
                    e.getMessage(), e);
        } catch (RSSManagerException e) {
            if (this.isInTransaction()) {
                this.rollbackTransaction();
            }
            try {
                conn.rollback();
            } catch (SQLException e1) {
                log.error(e1);
            }
            throw e;
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
    public void dropDatabase(String rssInstanceName, String databaseName) throws
            RSSManagerException {
        Connection conn = null;
        /* Initiating distributed transaction */
        this.beginTransaction();
        RSSInstance rssInstance =
                this.getDAO().findRSSInstanceDatabaseBelongsTo(rssInstanceName, databaseName);
        this.endTransaction();
        if (rssInstance == null) {
            throw new RSSManagerException("RSS instance " + rssInstanceName + " does not exist");
        }
        if (this.isInTransaction()) {
            this.endTransaction();
        }
        try {
            conn = rssInstance.getDataSource().getConnection();
            conn.setAutoCommit(false);
            String sql = "DROP DATABASE " + databaseName;
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.execute();

            this.beginTransaction();
            this.getDAO().removeUserDatabaseEntriesByDatabase(
                    rssInstance, databaseName, this.getCurrentTenantId());
            this.getDAO().dropDatabase(rssInstance, databaseName, this.getCurrentTenantId());
            this.endTransaction();

            conn.commit();
            stmt.close();
        } catch (SQLException e) {
            if (this.isInTransaction()) {
                this.rollbackTransaction();
            }
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error(e1);
                }
            }
            throw new RSSManagerException("Error while dropping the database '" + databaseName +
                    "' on RSS " + "instance '" + rssInstance.getName() + "' : " +
                    e.getMessage(), e);
        } catch (RSSManagerException e) {
            if (this.isInTransaction()) {
                this.rollbackTransaction();
            }
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                log.error(e1);
            }
            throw e;
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
    public void createDatabaseUser(DatabaseUser user) throws
            RSSManagerException {
        Connection conn = null;
        try {
            String qualifiedUsername = RSSManagerUtil.getFullyQualifiedUsername(user.getUsername());

            /* Sets the fully qualified username */
            user.setUsername(qualifiedUsername);
            /* Sets the tenant id under which the database is created */
            user.setTenantId(this.getCurrentTenantId());
            user.setRssInstanceName(user.getRssInstanceName());
            user.setType(this.inferUserType(user.getRssInstanceName()));

            for (RSSInstance rssInstance : RSSConfig.getInstance().getRssManager().
                    getRSSInstancePool().getAllSystemRSSInstances()) {
                try {
                    if (this.isInTransaction()) {
                        this.endTransaction();
                    }
                    conn = rssInstance.getDataSource().getConnection();
                    conn.setAutoCommit(false);

                    String sql = "INSERT INTO mysql.user (Host, User, Password, ssl_cipher, x509_issuer, x509_subject, authentication_string) VALUES (?, ?, PASSWORD(?), ?, ?, ?, ?)";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, "%");
                    stmt.setString(2, qualifiedUsername);
                    stmt.setString(3, user.getPassword());
                    stmt.setBlob(4, new ByteArrayInputStream(new byte[0]));
                    stmt.setBlob(5, new ByteArrayInputStream(new byte[0]));
                    stmt.setBlob(6, new ByteArrayInputStream(new byte[0]));
                    stmt.setString(7, "");
                    stmt.execute();

                    /* Initiating the distributed transaction */
                    this.beginTransaction();
                    user.setRssInstanceName(rssInstance.getName());
                    this.getDAO().createDatabaseUser(rssInstance, user);
                    /* Committing distributed transaction */
                    this.endTransaction();

                    stmt.close();
                    conn.commit();
                } catch (SQLException e) {
                    if (this.isInTransaction()) {
                        this.rollbackTransaction();
                    }
                    if (conn != null) {
                        conn.rollback();
                    }
                    throw new RSSManagerException("Error occurred while creating the database " +
                            "user '" + qualifiedUsername + "' on RSS instance '" +
                            rssInstance.getName() + "'", e);
                } finally {
                    if (conn != null) {
                        conn.close();
                    }
                }
            }
            for (RSSInstance rssInstance : RSSConfig.getInstance().getRssManager().
                    getRSSInstancePool().getAllSystemRSSInstances()) {
                this.flushPrivileges(rssInstance);
            }
        } catch (SQLException e) {
            this.rollbackTransaction();
            String msg = "Error while creating the database user '" +
                    user.getUsername() + "' on RSS instance '" + user.getRssInstanceName() +
                    "' : " + e.getMessage();
            throw new RSSManagerException(msg, e);
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
    public void dropDatabaseUser(String rssInstanceName, String username) throws
            RSSManagerException {
        Connection conn = null;
        try {
            for (RSSInstance rssInstance : RSSConfig.getInstance().getRssManager().
                    getRSSInstancePool().getAllSystemRSSInstances()) {
                try {
                    if (this.isInTransaction()) {
                        this.endTransaction();
                    }
                    conn = rssInstance.getDataSource().getConnection();
                    conn.setAutoCommit(false);

                    String sql = "DELETE FROM mysql.user WHERE User = ? AND Host = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, username);
                    stmt.setString(2, "%");
                    stmt.execute();
                    stmt.close();

                    /* Initiating the transaction */
                    this.beginTransaction();
                    this.getDAO().deleteUserDatabasePrivilegeEntriesByDatabaseUser(rssInstance,
                            username, this.getCurrentTenantId());
                    this.getDAO().removeUserDatabaseEntriesByDatabaseUser(rssInstance, username,
                            this.getCurrentTenantId());
                    this.getDAO().dropDatabaseUser(rssInstance, username,
                            this.getCurrentTenantId());
                    /* committing the distributed transaction */
                    this.endTransaction();

                    conn.commit();
                } finally {
                    if (conn != null) {
                        conn.close();
                    }
                }
            }
            for (RSSInstance rssInstance : RSSConfig.getInstance().getRssManager().
                    getRSSInstancePool().getAllSystemRSSInstances()) {
                this.flushPrivileges(rssInstance);
            }
        } catch (SQLException e) {
            if (this.isInTransaction()) {
                this.rollbackTransaction();
            }
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error(e1);
                }
            }
            String msg = "Error while dropping the database user '" + username +
                    "' on RSS instances : " + e.getMessage();
            throw new RSSManagerException(msg, e);
        } catch (RSSManagerException e) {
            if (isInTransaction()) {
                this.rollbackTransaction();
            }
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error(e1);
                }
            }
            throw e;
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
    public void editDatabaseUserPrivileges(DatabasePrivilegeSet privileges,
                                           DatabaseUser user,
                                           String databaseName) throws RSSManagerException {
        try {
            this.beginTransaction();
            RSSInstance rssInstance =
                    this.getDAO().findRSSInstanceDatabaseBelongsTo(user.getRssInstanceName(),
                            databaseName);
            if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(user.getRssInstanceName())) {
                user.setRssInstanceName(rssInstance.getName());
            }
            this.getDAO().updateDatabaseUser(privileges, rssInstance, user, databaseName);
            this.endTransaction();
        } catch (RSSManagerException e) {
            this.rollbackTransaction();
            throw e;
        }
    }

    @Override
    public void attachUserToDatabase(String rssInstanceName,
                                     String databaseName,
                                     String username,
                                     String templateName) throws RSSManagerException {
        Connection conn = null;
        /* Initiating distributed transaction */
        this.beginTransaction();
        RSSInstance rssInstance =
                this.getDAO().findRSSInstanceDatabaseBelongsTo(rssInstanceName, databaseName);
        if (rssInstance == null) {
            throw new RSSManagerException("RSS instance " + rssInstanceName + " does not exist");
        }
        Database database = this.getDAO().getDatabase(rssInstance, databaseName);
        if (database == null) {
            throw new RSSManagerException("Database '" + databaseName + "' does not exist");
        }
        DatabasePrivilegeTemplate template =
                this.getDAO().getDatabasePrivilegesTemplate(templateName);
        if (template == null) {
            throw new RSSManagerException("Database privilege template '" + templateName +
                    "' does not exist");
        }
        this.endTransaction();
        try {
            if (this.isInTransaction()) {
                this.endTransaction();
            }
            conn = rssInstance.getDataSource().getConnection();
            conn.setAutoCommit(false);
            PreparedStatement stmt =
                    this.composePreparedStatement(conn, databaseName, username, template);
            stmt.execute();
            stmt.close();

            this.beginTransaction();
            UserDatabaseEntry ude =
                    this.getDAO().createUserDatabaseEntry(rssInstance, database, username);
            this.getDAO().setUserDatabasePrivileges(ude, template);
            /* ending distributed transaction */
            this.endTransaction();

            conn.commit();

            this.flushPrivileges(rssInstance);
        } catch (SQLException e) {
            if (this.isInTransaction()) {
                this.rollbackTransaction();
            }
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error(e1);
                }
            }
            String msg = "Error occurred while attaching the database user '" + username + "' to " +
                    "the database '" + databaseName + "' : " + e.getMessage();
            throw new RSSManagerException(msg, e);
        } catch (RSSManagerException e) {
            if (this.isInTransaction()) {
                this.rollbackTransaction();
            }
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error(e1);
                }
            }
            throw e;
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
    public void detachUserFromDatabase(String rssInstanceName,
                                       String databaseName,
                                       String username) throws RSSManagerException {
        Connection conn = null;
        Database database = this.getDatabase(rssInstanceName, databaseName);
        if (database == null) {
            throw new RSSManagerException("Database '" + databaseName + "' does not exist");
        }
        /* Initiating the distributed transaction */
        this.beginTransaction();
        RSSInstance rssInstance = this.getDAO().findRSSInstanceDatabaseBelongsTo(rssInstanceName,
                databaseName);
        this.endTransaction();

        try {
            if (this.isInTransaction()) {
                this.endTransaction();
            }
            conn = rssInstance.getDataSource().getConnection();
            conn.setAutoCommit(false);
            String sql = "DELETE FROM mysql.db WHERE host = ? AND user = ? AND db = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "%");
            stmt.setString(2, username);
            stmt.setString(3, databaseName);
            stmt.execute();

            /* Initiating the distributed transaction */
            this.beginTransaction();
            this.getDAO().deleteUserDatabasePrivileges(rssInstance, username);
            this.getDAO().deleteUserDatabaseEntry(rssInstance, username);
            /* Committing the transaction */
            this.endTransaction();

            stmt.close();
            conn.commit();

            this.flushPrivileges(rssInstance);
        } catch (SQLException e) {
            if (isInTransaction()) {
                this.rollbackTransaction();
            }
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error(e1);
                }
            }
            String msg = "Error occurred while attaching the database user '" + username + "' to " +
                    "the database '" + databaseName + "' : " + e.getMessage();
            throw new RSSManagerException(msg, e);
        } catch (RSSManagerException e) {
            if (this.isInTransaction()) {
                this.rollbackTransaction();
            }
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error(e1);
                }
            }
            throw e;
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

    private PreparedStatement composePreparedStatement(Connection con,
                                                       String databaseName,
                                                       String username,
                                                       DatabasePrivilegeTemplate template) throws
            SQLException, RSSManagerException {
        DatabasePrivilegeSet privileges = template.getPrivileges();
        String sql = "INSERT INTO mysql.db VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setString(1, "%");
        stmt.setString(2, databaseName);
        stmt.setString(3, username);
        stmt.setString(4, privileges.getSelectPriv());
        stmt.setString(5, privileges.getInsertPriv());
        stmt.setString(6, privileges.getUpdatePriv());
        stmt.setString(7, privileges.getDeletePriv());
        stmt.setString(8, privileges.getCreatePriv());
        stmt.setString(9, privileges.getDropPriv());
        stmt.setString(10, privileges.getGrantPriv());
        stmt.setString(11, privileges.getReferencesPriv());
        stmt.setString(12, privileges.getIndexPriv());
        stmt.setString(13, privileges.getAlterPriv());
        stmt.setString(14, privileges.getCreateTmpTablePriv());
        stmt.setString(15, privileges.getLockTablesPriv());
        stmt.setString(16, privileges.getCreateViewPriv());
        stmt.setString(17, privileges.getShowViewPriv());
        stmt.setString(18, privileges.getCreateRoutinePriv());
        stmt.setString(19, privileges.getAlterRoutinePriv());
        stmt.setString(20, privileges.getExecutePriv());
        stmt.setString(21, privileges.getEventPriv());
        stmt.setString(22, privileges.getTriggerPriv());

        return stmt;
    }

    private void flushPrivileges(RSSInstance rssInstance) throws RSSManagerException {
        Connection conn = null;
        try {
            conn = rssInstance.getDataSource().getConnection();
            String sql = "FLUSH PRIVILEGES";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.execute();
            stmt.close();
        } catch (SQLException e) {
            throw new RSSManagerException("Error occurred while flushing privileges on RSS " +
                    "instance '" + rssInstance.getName() + "' : " + e.getMessage(), e);
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

    private RSSInstance lookupRSSInstance(String rssInstanceName) throws RSSManagerException {
        return (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) ?
                this.getRoundRobinAssignedDatabaseServer() : this.getRSSInstance(rssInstanceName);
    }

    private String inferEntityType(String rssInstanceName) throws RSSManagerException {
        return (RSSConfig.getInstance().getRssManager().getRSSInstancePool().
                isSystemRSSInstance(rssInstanceName)) ?
                RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE :
                RSSManagerConstants.USER_DEFINED_INSTANCE_TYPE;
    }

    private String inferUserType(String rssInstanceName) throws RSSManagerException {
        return (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(rssInstanceName)) ?
                RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE :
                RSSManagerConstants.USER_DEFINED_INSTANCE_TYPE;
    }

}
