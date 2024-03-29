/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.scim.common.group;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.persistence.JDBCPersistenceManager;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.carbon.identity.scim.common.utils.SQLQueries;
import org.wso2.charon.core.schema.SCIMConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * JDBC based Data Access layer for managing SCIM specific attributes that are not stored in
 * user store.
 */
public class GroupDAO {
    private static Log log = LogFactory.getLog(GroupDAO.class);

    public boolean isExistingGroup(String groupName, int tenantId) throws IdentitySCIMException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;

        boolean isExistingGroup = false;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.CHECK_EXISTING_GROUP_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, groupName);

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isExistingGroup = true;
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            //log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.CHECK_EXISTING_GROUP_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error when reading the group information from the persistence store.");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return isExistingGroup;
    }

    private boolean isExistingAttribute(String attributeName, String groupName, int tenantId)
            throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;

        boolean isExistingAttribute = false;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.CHECK_EXISTING_ATTRIBUTE_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, groupName);
            prepStmt.setString(3, attributeName);

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isExistingAttribute = true;
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            //log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.CHECK_EXISTING_ATTRIBUTE_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error when reading the group attribute information from " +
                                            "the persistence store.");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return isExistingAttribute;
    }

    public void addSCIMGroupAttributes(int tenantId, String roleName,
                                       Map<String, String> attributes)
            throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        if (!isExistingGroup(roleName, tenantId)) {
            try {
                connection = JDBCPersistenceManager.getInstance().getDBConnection();
                prepStmt = connection.prepareStatement(SQLQueries.ADD_ATTRIBUTES_SQL);
                prepStmt.setInt(1, tenantId);
                prepStmt.setString(2, roleName);
                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    if (!isExistingAttribute(entry.getKey(), roleName, tenantId)) {
                        prepStmt.setString(3, entry.getKey());
                        prepStmt.setString(4, entry.getValue());
                        prepStmt.execute();
                        connection.commit();
                    } else {
                        throw new IdentitySCIMException("Error when adding SCIM Attribute: " + entry.getKey() +
                                                        " An attribute with the same name already exists.");
                    }
                }

            } catch (IdentityException e) {
                String errorMsg = "Error when getting an Identity Persistence Store instance.";
                log.error(errorMsg, e);
                throw new IdentitySCIMException(errorMsg, e);
            } catch (SQLException e) {
                log.error("Error when executing the SQL : " + SQLQueries.ADD_ATTRIBUTES_SQL);
                log.error(e.getMessage(), e);
                throw new IdentitySCIMException("Error when adding SCIM attributes for the group: "
                                                + roleName);
            } finally {
                IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
            }
        } else {
            throw new IdentitySCIMException("Error when adding SCIM Attributes for the group: " + roleName +
                                            " A Group with the same name already exists.");
        }
    }

    public void updateSCIMGroupAttributes(int tenantId, String roleName,
                                          Map<String, String> attributes)
            throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        if (isExistingGroup(roleName, tenantId)) {
            try {
                connection = JDBCPersistenceManager.getInstance().getDBConnection();
                prepStmt = connection.prepareStatement(SQLQueries.UPDATE_ATTRIBUTES_SQL);

                prepStmt.setInt(2, tenantId);
                prepStmt.setString(3, roleName);

                for (Map.Entry<String, String> entry : attributes.entrySet()) {
                    if (isExistingAttribute(entry.getKey(), roleName, tenantId)) {
                        prepStmt.setString(4, entry.getKey());
                        prepStmt.setString(1, entry.getValue());
                        int count = prepStmt.executeUpdate();
                        if (log.isDebugEnabled()) {
                            log.debug("No. of records updated for updating SCIM Group : " + count);
                        }
                        connection.commit();
                    } else {
                        throw new IdentitySCIMException("Error when adding SCIM Attribute: " + entry.getKey() +
                                                        " An attribute with the same name doesn't exists.");
                    }
                }
            } catch (IdentityException e) {
                String errorMsg = "Error when getting an Identity Persistence Store instance.";
                log.error(errorMsg, e);
                throw new IdentitySCIMException(errorMsg, e);
            } catch (SQLException e) {
                log.error("Error when executing the SQL : " + SQLQueries.UPDATE_ATTRIBUTES_SQL);
                log.error(e.getMessage(), e);
                throw new IdentitySCIMException("Error updating the SCIM Group Attributes.");
            } finally {
                IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
            }
        } else {
            throw new IdentitySCIMException("Error when updating SCIM Attributes for the group: " + roleName +
                                            " A Group with the same name doesn't exists.");
        }
    }

    private void updateSCIMGroupAttribute(Connection dbConnection, String statement, int tenantId,
                                          String roleName, String attributeName,
                                          String attributeValue) throws IdentityException {
        //just for testing
        //dbConnection = JDBCPersistenceManager.getInstance().getDBConnection();

    }

    public void removeSCIMGroup(int tenantId, String roleName) throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.DELETE_GROUP_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, roleName);

            prepStmt.execute();
            connection.commit();

        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.DELETE_GROUP_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error deleting the SCIM Group.");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
    }

    public Map<String, String> getSCIMGroupAttributes(int tenantId, String roleName)
            throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        Map<String, String> attributes = new HashMap<String, String>();

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.GET_ATTRIBUTES_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, roleName);

            rSet = prepStmt.executeQuery();
            while (rSet.next()) {
                if (rSet.getString(1) != null && rSet.getString(1).length() > 0) {
                    attributes.put(rSet.getString(1), rSet.getString(2));
                }
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.GET_ATTRIBUTES_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error when reading the SCIM Group information from the " +
                                            "persistence store.");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return attributes;
    }

    public String getGroupNameById(int tenantId, String id) throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        String roleName = null;

        try {
            connection = JDBCPersistenceManager.getInstance().getDBConnection();
            prepStmt = connection.prepareStatement(SQLQueries.GET_GROUP_NAME_BY_ID_SQL);
            prepStmt.setInt(1, tenantId);
            prepStmt.setString(2, id);
            prepStmt.setString(3, SCIMConstants.ID_URI);
            rSet = prepStmt.executeQuery();
            while (rSet.next()) {
                //we assume only one result since group id and tenant id is unique.
                roleName = rSet.getString(1);
            }
        } catch (IdentityException e) {
            String errorMsg = "Error when getting an Identity Persistence Store instance.";
            log.error(errorMsg, e);
            throw new IdentitySCIMException(errorMsg, e);
        } catch (SQLException e) {
            log.error("Error when executing the SQL : " + SQLQueries.GET_GROUP_NAME_BY_ID_SQL);
            log.error(e.getMessage(), e);
            throw new IdentitySCIMException("Error when reading the SCIM Group information from the persistence store.");
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rSet, prepStmt);
        }
        return roleName;
    }

    public void updateRoleName(int tenantId, String oldRoleName, String newRoleName)
            throws IdentitySCIMException {
        Connection connection = null;
        PreparedStatement prepStmt = null;

        if (isExistingGroup(oldRoleName, tenantId)) {
            try {
                connection = JDBCPersistenceManager.getInstance().getDBConnection();
                prepStmt = connection.prepareStatement(SQLQueries.UPDATE_GROUP_NAME_SQL);

                prepStmt.setString(1, newRoleName);
                prepStmt.setInt(2, tenantId);
                prepStmt.setString(3, oldRoleName);
                
                int count = prepStmt.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("No. of records updated for updating SCIM Group : " + count);
                }
                connection.commit();
            } catch (IdentityException e) {
                String errorMsg = "Error when getting an Identity Persistence Store instance.";
                log.error(errorMsg, e);
                throw new IdentitySCIMException(errorMsg, e);
            } catch (SQLException e) {
                log.error("Error when executing the SQL : " + SQLQueries.UPDATE_GROUP_NAME_SQL);
                log.error(e.getMessage(), e);
                throw new IdentitySCIMException("Error updating the SCIM Group Attributes.");
            } finally {
                IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
            }
        } else {
            throw new IdentitySCIMException("Error when updating role name of the role: " + oldRoleName);
        }
    }
}
