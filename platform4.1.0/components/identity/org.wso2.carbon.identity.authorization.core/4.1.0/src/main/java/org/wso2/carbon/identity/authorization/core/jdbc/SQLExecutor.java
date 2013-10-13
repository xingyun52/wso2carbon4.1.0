/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.authorization.core.jdbc;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.authorization.core.internal.AuthorizationServiceComponent;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.user.core.util.DatabaseUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * .
 */
public class SQLExecutor {

    private static Log log = LogFactory.getLog(SQLExecutor.class);

    DataSource dataSource;

    public SQLExecutor() {

        dataSource = DatabaseUtil.getRealmDataSource(AuthorizationServiceComponent.
                getRealmService().getBootstrapRealmConfiguration());
    }

    public Map<String, String> populateResourceId(int permissionId){

        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        Map<String, String> stringMap = new HashMap<String, String>();		
        PreparedStatement statement = null;
        ResultSet result = null;
        Connection connection = null;
        try{
            connection = dataSource.getConnection();
            statement = connection.prepareStatement(JDBCConstants.GET_RESOURCE_ID);
            statement.setInt(1, permissionId);
            statement.setInt(2, tenantId);
            result = statement.executeQuery();
            while(result.next()){
                String resourceId = result.getString(1);
                String action = result.getString(2);
                stringMap.put(resourceId, action);
            }
        } catch (SQLException e) {
            log.error("Error while retrieving authorization data ", e);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if(connection != null){
                    connection.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }

        return stringMap;

    }

    public Map<Integer, Integer> populateUserPermissionId(String subject, boolean isUser) {

        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        Map<Integer, Integer> integerMap = new HashMap<Integer, Integer>();
        
        PreparedStatement statement = null;
        ResultSet result = null;
        Connection connection = null;
        try{
            connection = dataSource.getConnection();
            if(isUser){
                statement = connection.prepareStatement(JDBCConstants.GET_PERMISSIONS_OF_USER);
            } else {
                statement = connection.prepareStatement(JDBCConstants.GET_PERMISSIONS_OF_ROLE);                
            }
            statement.setString(1, subject);
            statement.setInt(2, tenantId);
            result = statement.executeQuery();
            while(result.next()){
                int permissionId = result.getInt(1);
                int allow = result.getInt(2);
                integerMap.put(permissionId, allow);
            }
        } catch (SQLException e) {
            log.error("Error while retrieving authorization data ", e);
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if(connection != null){
                    connection.close();
                }
            } catch (SQLException ex) {
                String msg = RegistryConstants.RESULT_SET_PREPARED_STATEMENT_CLOSE_ERROR;
                log.error(msg, ex);
            }
        }

        return integerMap;
    }
}
