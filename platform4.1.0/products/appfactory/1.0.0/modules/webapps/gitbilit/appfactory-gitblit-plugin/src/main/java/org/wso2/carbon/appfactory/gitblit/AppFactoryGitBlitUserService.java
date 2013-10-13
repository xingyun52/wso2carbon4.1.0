/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.appfactory.gitblit;

import com.gitblit.IStoredSettings;
import com.gitblit.IUserService;
import com.gitblit.models.TeamModel;
import com.gitblit.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.appfactory.git.AppFactoryAuthenticationClient;
import org.wso2.carbon.appfactory.git.AppFactoryGitBlitUserModel;
import org.wso2.carbon.appfactory.git.AppFactoryRepositoryAuthorizationClient;
import org.wso2.carbon.appfactory.git.ApplicationManagementServiceClient;
import org.wso2.carbon.appfactory.git.GitBlitConfiguration;
import org.wso2.carbon.appfactory.git.GitBlitConstants;
import org.wso2.carbon.appfactory.git.UserAdminServiceClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A GitBlit UserService for Wso2 App Factory.
 * Data Model Mappings
 * App Factory       GitBlit
 * applications ---> teams
 * Note:
 * 1.An application can have only one repository in appfactory,same way a team  can have access
 * to a repository
 * 2.Application names and repository names are same in appfactory ,same way repository names and
 * team names are same
 * 3.This pluigin is not allow any modification to user related data from gitblit side
 * <p/>
 * <p/>
 * Following properties should be added to gitblit.properties
 * appfactory.truststore= /media/Entetainment/base_appfactory/appfactory_deployment/setup/appfactory/wso2appfactory-1.0.0/repository/resources/security/wso2carbon.jks
 * appfactory.truststore.password= wso2carbon
 * appfactory.admin.username= admin@admin.com
 * appfactory.admin.password= admin
 * appfactory.url= https://localhost:9443
 */
public class AppFactoryGitBlitUserService implements IUserService {
    private IStoredSettings settings;
    private static final Logger logger = LoggerFactory.getLogger(AppFactoryGitBlitUserService.class);
    private  GitBlitConfiguration configuration;

    public GitBlitConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(GitBlitConfiguration configuration) {
        this.configuration = configuration;
    }

    public AppFactoryAuthenticationClient getAppFactoryAuthenticationClient() {
        AppFactoryAuthenticationClient appFactoryAuthenticationClient;
        return appFactoryAuthenticationClient =new AppFactoryAuthenticationClient(this.getConfiguration());
    }

    public ApplicationManagementServiceClient getApplicationProvider() {
        ApplicationManagementServiceClient applicationProvider;
        return applicationProvider =new ApplicationManagementServiceClient(getConfiguration());
    }

    public UserAdminServiceClient getUserAdminServiceClient() {
        UserAdminServiceClient userAdminServiceClient;
        return userAdminServiceClient =new UserAdminServiceClient(getConfiguration());
    }

    public AppFactoryRepositoryAuthorizationClient getRepositoryAuthorizationClient() {
        AppFactoryRepositoryAuthorizationClient repositoryAuthorizationClient;
        return repositoryAuthorizationClient =new AppFactoryRepositoryAuthorizationClient(getConfiguration());
    }

    /**
     * @param settings
     */
    @Override
    public void setup(IStoredSettings settings) {
        this.settings = settings;
        configuration = new GitBlitConfiguration(settings);
        try {
            System.setProperty("javax.net.ssl.trustStore", configuration.getProperty
                    (GitBlitConstants.APPFACTORY_TRUST_STORE_LOCATION,
                     new File(".").getCanonicalPath() + "/wso2carbon.jks"));
        } catch (IOException e) {
            logger.error("Could not find any trust store for communicate with app factory");
        }
        //set system property truststore password
        System.setProperty("javax.net.ssl.trustStorePassword", configuration.getProperty
                (GitBlitConstants.APPFACTORY_TRUST_STORE_PASSWORD, "wso2carbon"));
        logger.info("***********App Factory User Service is  initialized ************");
    }

    /**
     * Restrict credential change from GitBlit
     *
     * @return
     */
    @Override
    public boolean supportsCredentialChanges() {
        return false;
    }

    /**
     * Restrict user display name change from GitBlit
     *
     * @return
     */
    @Override
    public boolean supportsDisplayNameChanges() {
        return false;
    }

    /**
     * Restrict user email address change from GitBlit
     *
     * @return
     */
    @Override
    public boolean supportsEmailAddressChanges() {
        return false;
    }

    /**
     * Restrict team membership changes from GitBlit
     *
     * @return
     */
    @Override
    public boolean supportsTeamMembershipChanges() {
        return false;
    }

    /**
     * Cookies are not supported
     *
     * @return
     */
    @Override
    public boolean supportsCookies() {
        return false;
    }

    /**
     * Cookies are not supported.thus returning null
     *
     * @param userModel
     * @return
     */
    @Override
    public String getCookie(UserModel userModel) {
        return null;
    }

    /**
     * Cookie based authentication is not supported
     *
     * @param chars
     * @return
     */
    @Override
    public UserModel authenticate(char[] chars) {
        return null;
    }

    /**
     * Authenticate user using AuthenticationAdmin service from app factory.
     * If the user name matches with admin name from gitblit.properties file
     * the admin permission will be granted.This implies only admin can create repositories.Use
     * this admin user to create repository from RPC API.
     *
     * @param username
     * @param password
     * @return If authentication is successful the user model will be filled with teams
     *         (applications) and repository of user otherwise null
     */
    public UserModel authenticate(String username, char[] password) {
        if (getAppFactoryAuthenticationClient().authenticate(username, new String(password))) {
            UserModel userModel = getUserModel(username);
            if (username.equals(settings.getString(GitBlitConstants
                                                           .APPFACTORY_ADMIN_USERNAME,
                                                   "admin@admin.com"))) {
                userModel.canAdmin = true;
            } else {
                userModel.canAdmin = false;
            }
            //userModel.cookie = StringUtils.getSHA1(userModel.username + new String(password));
            return userModel;
        }
        return null;
    }

    /**
     * Hence the user service is loaded for each session we can  logout the same user who logged in
     *
     * @param userModel
     */
    @Override
    public void logout(UserModel userModel) {
        getAppFactoryAuthenticationClient().logout();
    }

    /**
     * Return user model for a username with filled with teams of the user and the repository he
     * has access.
     * Note:repository name and team name is same as in app factory application name
     * and repository name is same
     *
     * @param username
     * @return
     */
    @Override
    public UserModel getUserModel(String username) {
        TeamModel teamModel;
        UserModel userModel = new AppFactoryGitBlitUserModel(username, getConfiguration());
        for (String app : getApplicationProvider().getAllApplicationsOfUser(username)) {
            teamModel = new TeamModel(getGitBlitRepositoryName(app));
            userModel.teams.add(teamModel);
            userModel.addRepository(getGitBlitRepositoryName(app));
        }
        return userModel;
    }

    /**
     * Convert application name to gitblit repository name by appending ".git"
     *
     * @param applicationName
     * @return
     */
    private String getGitBlitRepositoryName(String applicationName) {
        return applicationName.concat(".git");
    }

    /**
     * Editing user data in gitblit is restricted
     *
     * @param userModel
     * @return
     */
    @Override
    public boolean updateUserModel(UserModel userModel) {
        return false;
    }

    /**
     * Editing user data in gitblit is restricted
     *
     * @param userName
     * @param userModel
     * @return
     */
    @Override
    public boolean updateUserModel(String userName, UserModel userModel) {
        return false;
    }

    /**
     * Removing user data in gitblit is restricted
     *
     * @param userModel
     * @return
     */
    @Override
    public boolean deleteUserModel(UserModel userModel) {
        return false;
    }

    /**
     * Deleting user is not supported by this plugin
     *
     * @param username
     * @return
     */
    @Override
    public boolean deleteUser(String username) {
        return false;
    }

    /**
     * Get all the user of system(app factory) using UserAdmin service
     *
     * @return
     */
    @Override
    public List<String> getAllUsernames() {
        return getUserAdminServiceClient().getAllUsers();
    }

    /**
     * Editing user model is not supported by this plugin
     *
     * @return
     */
    @Override
    public List<UserModel> getAllUsers() {
        return Collections.emptyList();
    }

    /**
     * Get all available teams.
     * Hence admin user is member of all applications here we are retrieving the applications of
     * admin user
     *
     * @return list application keys
     */
    @Override
    public List<String> getAllTeamNames() {
        return getApplicationProvider().getAllApplicationsOfUser(settings.getString(GitBlitConstants
                .APPFACTORY_ADMIN_USERNAME, "admin@admin.com"));

    }

    /**
     * Editing team model is not supported by this plugin
     *
     * @return
     */
    @Override
    public List<TeamModel> getAllTeams() {
        return Collections.emptyList();
    }

    /**
     * This method will return the teams which have access to a repository.
     * Hence in app factory cross repository access from applications is restricted the list
     * contains only team name with same name as repository name.
     *
     * @param repositoryName
     * @return
     */
    @Override
    public List<String> getTeamnamesForRepositoryRole(String repositoryName) {
        List<String> appList = new ArrayList<String>();
        appList.add(repositoryName);
        return appList;
    }

    /**
     * Editing repository access is not supported
     *
     * @param repositoryName
     * @param strings
     * @return
     */
    @Override
    public boolean setTeamnamesForRepositoryRole(String repositoryName, List<String> strings) {
        return false;
    }

    /**
     * Returning team model filled with all the users of mapped application from app factory and
     * a repository with same name
     *
     * @param teamName
     * @return
     */
    @Override
    public TeamModel getTeamModel(String teamName) {
        TeamModel teamModel = new TeamModel(teamName);
        for (String user : getApplicationProvider().getUsersOfApplication(getAppFactoryApplicationName(teamName)
        )) {
            teamModel.addUser(user);
        }
        teamModel.addRepository(teamName);
        return teamModel;
    }

    /**
     * Editing team data is not supported
     *
     * @param teamModel
     * @return
     */
    @Override
    public boolean updateTeamModel(TeamModel teamModel) {
        return false;
    }

    /**
     * Editing team data is not supported
     *
     * @param teamName
     * @param teamModel
     * @return
     */
    @Override
    public boolean updateTeamModel(String teamName, TeamModel teamModel) {
        return false;
    }

    /**
     * Deleting team model is not supported
     *
     * @param teamModel
     * @return
     */
    @Override
    public boolean deleteTeamModel(TeamModel teamModel) {
        return false;
    }

    /**
     * Deleting team  is not supported
     *
     * @param teamName
     * @return
     */
    @Override
    public boolean deleteTeam(String teamName) {
        return false;
    }

    /**
     * This return list of users belongs to application in app factory,
     * because all the user in an application has access to repository for now.
     *
     * @param repositoryName
     * @return list of user names
     */
    @Override
    public List<String> getUsernamesForRepositoryRole(String repositoryName) {
        return getApplicationProvider().getUsersOfApplication(getAppFactoryApplicationName(repositoryName));
    }

    /**
     * Method to get app factory application name from gitblit repository name
     *
     * @param repositoryName
     * @return
     */
    private String getAppFactoryApplicationName(String repositoryName) {
        String applicationName = repositoryName.substring(0, repositoryName.lastIndexOf(".git"));
        return applicationName;
    }

    /**
     * Not supported
     *
     * @param s
     * @param strings
     * @return
     */
    @Override
    public boolean setUsernamesForRepositoryRole(String s, List<String> strings) {
        return false;
    }

    /**
     * Not supported
     *
     * @param s
     * @param s1
     * @return
     */
    @Override
    public boolean renameRepositoryRole(String s, String s1) {
        return false;
    }

    /**
     * Not supported
     *
     * @param s
     * @return
     */
    @Override
    public boolean deleteRepositoryRole(String s) {
        return false;
    }

}
