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

import org.wso2.carbon.rssmanager.core.RSSManagerException;
import org.wso2.carbon.rssmanager.core.entity.*;

import java.util.List;

/**
 * Data Access Object interface for WSO2 RSS based database operations.
 */
public interface RSSDAO {

	public void createRSSInstance(RSSInstance rssInstance) throws RSSManagerException;

	public List<RSSInstance> getAllSystemRSSInstances() throws RSSManagerException;
	
	public void dropRSSInstance(String rssInstanceName, int tenantId) throws RSSManagerException;
	
	public void updateRSSInstance(RSSInstance rssInstance) throws RSSManagerException;

	public void createDatabase(Database database) throws RSSManagerException;

	public List<Database> getAllDatabases(int tenantId) throws RSSManagerException;

	public void dropDatabase(RSSInstance rssInstance, String databaseName, int tenantId) throws
            RSSManagerException;

	public void createDatabaseUser(RSSInstance rssInstance, DatabaseUser user)
            throws RSSManagerException;

	public void dropDatabaseUser(RSSInstance rssInstance, String username,
                                 int tenantId) throws RSSManagerException;

	public UserDatabaseEntry createUserDatabaseEntry(RSSInstance rssInstance, 
                                                     Database database,
                                                     String user) throws RSSManagerException;

	public void deleteUserDatabaseEntry(RSSInstance rssInstance,
                                        String username) throws RSSManagerException;

	public int getSystemRSSDatabaseCount() throws RSSManagerException;

    public List<RSSInstance> getAllRSSInstances(int tid) throws RSSManagerException;

    public RSSInstance getRSSInstance(String rssInstanceName) throws
            RSSManagerException;

    public Database getDatabase(RSSInstance rssInstance, 
                                String databaseName) throws RSSManagerException;

    public DatabaseUser getDatabaseUser(RSSInstance rssInstance,
                                        String username) throws RSSManagerException;
    
    public DatabasePrivilegeSet getUserDatabasePrivileges(RSSInstance rssInstance,
                                                          String databaseName,
                                                          String username) throws RSSManagerException;

    public void updateDatabaseUser(DatabasePrivilegeSet privileges,
                                   RSSInstance rssInstance,
                                   DatabaseUser user,
                                   String databaseName) throws RSSManagerException;

    public DatabasePrivilegeTemplate createDatabasePrivilegesTemplate(
            DatabasePrivilegeTemplate template) throws RSSManagerException;

    public void dropDatabasePrivilegesTemplate(String templateName) throws RSSManagerException;

    public void editDatabasePrivilegesTemplate(
            DatabasePrivilegeTemplate template) throws RSSManagerException;

    public List<DatabasePrivilegeTemplate> getAllDatabasePrivilegesTemplates(int tid) throws
            RSSManagerException;

    public DatabasePrivilegeTemplate getDatabasePrivilegesTemplate(
            String templateName) throws RSSManagerException;
    
    public List<DatabaseUser> getAllDatabaseUsers(int tid) throws RSSManagerException;

    public List<DatabaseUser> getUsersByRSSInstance(RSSInstance rssInstance) throws
            RSSManagerException;

    public List<String> getUsersAssignedToDatabase(
            RSSInstance rssInstance, String databaseName) throws RSSManagerException;

    public List<DatabaseUser> getSystemCreatedDatabaseUsers() throws RSSManagerException;

    public List<String> getSystemUsersAssignedToDatabase(
            RSSInstance rssInstance, String databaseName) throws RSSManagerException;

    public DatabasePrivilegeSet getSystemUserDatabasePrivileges(RSSInstance rssInstance,
            String databaseName, String username) throws RSSManagerException;

    public RSSInstance findRSSInstanceDatabaseBelongsTo(
            String rssInstanceName, String databaseName) throws RSSManagerException;

    public RSSInstance findRSSInstanceDatabaseUserBelongsTo(
            String rssInstanceName, String databaseName) throws RSSManagerException;

    public void setUserDatabasePrivileges (
            UserDatabaseEntry userDBEntry, DatabasePrivilegeTemplate template) throws
            RSSManagerException;

    public void deleteUserDatabasePrivilegeEntriesByDatabaseUser(
            RSSInstance rssInstance, String username, int tenantId) throws RSSManagerException;

    public void removeUserDatabaseEntriesByDatabaseUser(
            RSSInstance rssInstance, String username, int tenantId) throws RSSManagerException;

    public void removeUserDatabaseEntriesByDatabase(
            RSSInstance rssInstance, String databaseName, int tenantId) throws RSSManagerException;

    public void removeDatabasePrivilegesTemplateEntries(String templateName, int tenantId) throws
            RSSManagerException;

    public void incrementSystemRSSDatabaseCount() throws RSSManagerException;

    public void deleteUserDatabasePrivileges(RSSInstance rssInstance, String username) throws
            RSSManagerException;

    public void setDatabasePrivilegeTemplateProperties(DatabasePrivilegeTemplate template) throws
            RSSManagerException;

    public boolean isDatabaseExist(String rssInstanceName, String databaseName) throws
            RSSManagerException;

    public boolean isDatabaseUserExist(String rssInstanceName, String databaseUsername) throws
            RSSManagerException;

    public boolean isDatabasePrivilegeTemplateExist(String templateName) throws
            RSSManagerException;

}
