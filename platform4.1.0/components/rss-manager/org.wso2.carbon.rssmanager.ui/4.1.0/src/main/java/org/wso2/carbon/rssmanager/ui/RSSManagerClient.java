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
package org.wso2.carbon.rssmanager.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.rssmanager.ui.stub.RSSAdminStub;
import org.wso2.carbon.rssmanager.ui.stub.types.*;

import java.util.*;

public class RSSManagerClient {

    private RSSAdminStub stub;

    private ResourceBundle bundle;

    private static final String BUNDLE = "org.wso2.carbon.rssmanager.ui.i18n.Resources";

    private static final Log log = LogFactory.getLog(RSSManagerClient.class);

    public RSSManagerClient(String cookie, String backendServerUrl,
                            ConfigurationContext configurationContext, Locale locale) {
        String serviceEndpoint = backendServerUrl + "RSSAdmin";
        bundle = java.util.ResourceBundle.getBundle(BUNDLE, locale);
        try {
            stub = new RSSAdminStub(configurationContext, serviceEndpoint);
            ServiceClient serviceClient = stub._getServiceClient();
            Options options = serviceClient.getOptions();
            options.setManageSession(true);
            options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
        } catch (AxisFault axisFault) {
            log.error(axisFault);
        }
    }

    public void dropDatabasePrivilegesTemplate(String templateName) throws AxisFault {
        try {
            stub.dropDatabasePrivilegesTemplate(templateName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public void editDatabasePrivilegesTemplate(DatabasePrivilegeTemplate template) throws
            AxisFault {
        try {
            stub.editDatabasePrivilegesTemplate(template);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public void createDatabasePrivilegesTemplate(DatabasePrivilegeTemplate template) throws
            AxisFault {
        try {
            stub.createDatabasePrivilegesTemplate(template);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public List<DatabasePrivilegeTemplate> getDatabasePrivilegesTemplates() throws AxisFault {
        List<DatabasePrivilegeTemplate> templates = new ArrayList<DatabasePrivilegeTemplate>();
        try {
            DatabasePrivilegeTemplate[] tmp = stub.getDatabasePrivilegesTemplates();
            if (tmp != null && tmp.length > 0) {
                templates = Arrays.asList(stub.getDatabasePrivilegesTemplates());
            }
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return templates;
    }

    public void editUserPrivileges(DatabasePrivilegeSet privileges,
                                   DatabaseUser user, String databaseName) throws AxisFault {
        try {
            stub.editDatabaseUserPrivileges(privileges, user, databaseName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public void createDatabase(Database database) throws AxisFault {
        try {
            stub.createDatabase(database);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public List<DatabaseMetaData> getDatabaseList() throws AxisFault {
        List<DatabaseMetaData> databases = new ArrayList<DatabaseMetaData>();
        try {
            DatabaseMetaData[] tmp = stub.getDatabases();
            if (tmp != null && tmp.length > 0) {
                databases = Arrays.asList(tmp);
            }
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return databases;
    }

    public DatabaseMetaData getDatabase(String rssInstanceName, String databaseName) throws
            AxisFault {
        try {
            return stub.getDatabase(rssInstanceName, databaseName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return null;
    }

    public void dropDatabase(String rssInstanceName, String databaseName) throws AxisFault {
        try {
            stub.dropDatabase(rssInstanceName, databaseName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public List<RSSInstanceMetaData> getRSSInstanceList() throws AxisFault {
        List<RSSInstanceMetaData> rssInstances = new ArrayList<RSSInstanceMetaData>();
        try {
            RSSInstanceMetaData[] tmp = stub.getRSSInstances();
            if (tmp != null && tmp.length > 0) {
                rssInstances = Arrays.asList(tmp);
            }
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return rssInstances;
    }

    public void createRSSInstance(RSSInstance rssIns) throws AxisFault {
        try {
            stub.createRSSInstance(rssIns);
        } catch (Exception e) {
            handleException(bundle.getString("rss.manager.failed.to.add.database.server.instance") +
                    " : " + rssIns.getName(), e);
        }
    }

    public void testConnection(String driverClass, String jdbcUrl,
                               String username, String password) throws AxisFault {
        try {
            stub.testConnection(driverClass, jdbcUrl, username, password);
        } catch (Exception e) {
            handleException("Error occurred while connecting to '" + jdbcUrl +
                    "' with the username '" + username + "' and the driver class '" +
                    driverClass + "'", e);
        }
    }

    public void editRSSInstance(RSSInstance rssIns) throws AxisFault {
        try {
            stub.editRSSInstance(rssIns);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public DatabaseUserMetaData getDatabaseUser(String rssInstanceName, String username) throws
            AxisFault {
        DatabaseUserMetaData user = null;
        try {
            user = stub.getDatabaseUser(rssInstanceName, username);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return user;
    }

    public void dropDatabaseUser(String rssInstanceName, String username) throws AxisFault {
        try {
            stub.dropDatabaseUser(rssInstanceName, username);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public void createCarbonDataSource(UserDatabaseEntry entry) throws AxisFault {
        try {
            stub.createCarbonDataSource(entry);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public void createDatabaseUser(DatabaseUser user) throws AxisFault {
        try {
            stub.createDatabaseUser(user);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public DatabasePrivilegeTemplate getDatabasePrivilegesTemplate(String templateName) throws
            AxisFault {
        DatabasePrivilegeTemplate tempalte = null;
        try {
            tempalte = stub.getDatabasePrivilegesTemplate(templateName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return tempalte;
    }

    private void handleException(String msg, Exception e) throws AxisFault {
        //log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    public RSSInstanceMetaData getRSSInstance(String rssInstanceName) throws AxisFault {
        RSSInstanceMetaData rssIns = null;
        try {
            rssIns = stub.getRSSInstance(rssInstanceName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return rssIns;
    }

    public DatabaseUserMetaData[] getDatabaseUsers() throws AxisFault {
        DatabaseUserMetaData[] users = new DatabaseUserMetaData[0];
        try {
            users = stub.getDatabaseUsers();
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return users;
    }

    public void dropRSSInstance(String rssInstanceName) throws AxisFault {
        try {
            stub.dropRSSInstance(rssInstanceName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public int getSystemRSSInstanceCount() throws AxisFault {
        int count = 0;
        try {
            count = stub.getSystemRSSInstanceCount();
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return count;
    }

    public void attachUserToDatabase(String rssInstanceName, String databaseName, String username,
                                     String templateName) throws AxisFault {
        try {
            stub.attachUserToDatabase(rssInstanceName, databaseName, username, templateName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public void detachUserFromDatabase(String rssInstanceName, String databaseName,
                                       String username) throws AxisFault {
        try {
            stub.detachUserFromDatabase(rssInstanceName, databaseName, username);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
    }

    public String[] getUsersAttachedToDatabase(String rssInstanceName,
                                                             String databaseName) throws AxisFault {
        String[] users = new String[0];
        try {
            users = stub.getUsersAttachedToDatabase(rssInstanceName, databaseName);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return users;
    }

    public String[] getAvailableUsersToAttachToDatabase(
            String rssInstanceName, String databaseName) throws AxisFault {
        String[] users = new String[0];
        try {
            users = stub.getAvailableUsersToAttachToDatabase(rssInstanceName, databaseName);
        } catch (Exception e) {
             handleException(e.getMessage(), e);
        }
        return users;
    }


    public DatabasePrivilegeSet getUserDatabasePermissions(
            String rssInstanceName, String databaseName, String username) throws AxisFault {
        DatabasePrivilegeSet privileges = null;
        try {
            privileges = stub.getUserDatabasePermissions(rssInstanceName, databaseName, username);
        } catch (Exception e) {
            handleException(e.getMessage(), e);
        }
        return privileges;
    }

}
