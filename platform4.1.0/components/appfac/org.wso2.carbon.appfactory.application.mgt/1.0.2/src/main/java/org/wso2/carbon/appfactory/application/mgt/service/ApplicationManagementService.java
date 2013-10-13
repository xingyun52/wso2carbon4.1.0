/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.appfactory.application.mgt.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.appfactory.application.mgt.service.applicationqueue.ApplicationCreator;
import org.wso2.carbon.appfactory.application.mgt.util.UserApplicationCache;
import org.wso2.carbon.appfactory.application.mgt.util.Util;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.common.util.AppFactoryUtil;
import org.wso2.carbon.appfactory.core.ApplicationEventsListener;
import org.wso2.carbon.appfactory.core.dto.Application;
import org.wso2.carbon.appfactory.core.dto.UserInfo;
import org.wso2.carbon.appfactory.core.dto.Version;
import org.wso2.carbon.appfactory.core.governance.RxtManager;
import org.wso2.carbon.appfactory.core.queue.AppFactoryQueueException;
import org.wso2.carbon.appfactory.jenkins.build.JenkinsCISystemDriver;
import org.wso2.carbon.appfactory.utilities.project.ProjectUtils;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.email.sender.api.EmailSender;
import org.wso2.carbon.email.sender.api.EmailSenderConfiguration;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApplicationManagementService extends AbstractAdmin {

    private static Log log = LogFactory.getLog(ApplicationManagementService.class);

    public static String EMAIL_CLAIM_URI = "http://wso2.org/claims/emailaddress";
    public static String FIRST_NAME_CLAIM_URI = "http://wso2.org/claims/givenname";
    public static String LAST_NAME_CLAIM_URI = "http://wso2.org/claims/lastname";


    public static UserApplicationCache userApplicationCache = UserApplicationCache.getUserApplicationCache();

    /**
     * This createApplication method is used for the create an application. When
     * call this method, it put to the queue.
     *
     * @param applicationName        Application name.
     * @param applicationKey         Key for the Application. This should be unique.
     * @param applicationDescription Description of the application.
     * @param applicationType        Type of the application. ex: war, jaxrs, jaxws ...
     * @param repositoryType         Type of the repository that should use. ex: svn, git
     * @param userName               Logged-in user name.
     */
    public void createApplication(String applicationName, String applicationKey,
                                  String applicationDescription, String applicationType,
                                  String repositoryType, String userName)
            throws ApplicationManagementException {

        ApplicationInfoBean applicationInfoBean = new ApplicationInfoBean();
        applicationInfoBean.setName(applicationName);
        applicationInfoBean.setApplicationKey(applicationKey);
        applicationInfoBean.setDescription(applicationDescription);
        applicationInfoBean.setApplicationType(applicationType);
        applicationInfoBean.setRepositoryType(repositoryType);
        applicationInfoBean.setOwnerUserName(userName);


        try {
            ApplicationCreator applicationCreator = ApplicationCreator.getInstance();
            applicationCreator.getExecutionEngine().getSynchQueue().put(applicationInfoBean);
        } catch (AppFactoryQueueException e) {
            String errorMsg = "Error occured when adding an application in to queue, " + e.getMessage();
            log.error(errorMsg, e);
            throw new ApplicationManagementException(errorMsg, e);
        }

    }

    public boolean addUserToApplication(String applicationId, String userName, String[] roles)
            throws ApplicationManagementException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        try {
            UserRealm realm =
                    Util.getRealmService()
                            .getTenantUserRealm(tenantManager.getTenantId(applicationId));
            String[] newRolesForUser = removeRolesUserAlreadyIn(userName, roles, realm);
            realm.getUserStoreManager().updateRoleListOfUser(userName, null, newRolesForUser);

            AppFactoryConfiguration config = AppFactoryUtil.getAppfactoryConfiguration();
            String emailSend = config.getFirstProperty("EmailSend");

            if (emailSend.equals("true") && !roles[0].equals("appOwner")) {
                sendMail(applicationId, userName, roles, "invite-user-email-config.xml");
            }
            userApplicationCache.clearFromCache(userName);
            clearRealmCache(applicationId);


        } catch (UserStoreException e) {
            String msg = "Error while adding user " + userName + " to application " + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        } catch (AppFactoryException e) {
            String errorMsg = "Unable to notify user " + e.getMessage();
            log.error(errorMsg, e);
            throw new ApplicationManagementException(errorMsg, e);
        }

        // Notify the event listener about the user addition
        Iterator<ApplicationEventsListener> appEventListeners =
                Util.getApplicationEventsListeners()
                        .iterator();
        try {
            Application app = ProjectUtils.getApplicationInfo(applicationId);
            UserInfo user = new UserInfo(userName, roles);
            while (appEventListeners.hasNext()) {
                ApplicationEventsListener listener = appEventListeners.next();
                listener.onUserAddition(app, user);
            }

        } catch (Exception ex) {

            if (ex.getCause() != null && ex.getCause().getCause() != null && ex.getCause().getCause().getMessage().trim().equals("User has already been taken")) {
                log.warn(ex.getCause().getCause().getMessage());
            } else {
                String errorMsg = "Unable to publish user addition event due to " + ex.getMessage();
                log.error(errorMsg, ex);
                if (ex.getMessage().equals("0")) {
                    return false;
                } else {
                    throw new ApplicationManagementException(errorMsg, ex);
                }
            }
        }

        return true;
    }

    // If user is going to be added to a role that he is already having, remove
    // that role from 'newRoles'
    private String[] removeRolesUserAlreadyIn(String userName, String[] newRoles, UserRealm realm)
            throws UserStoreException {

        ArrayList<String> newRolesArray = new ArrayList<String>();
        for (String newRole : newRoles) {
            newRolesArray.add(newRole);
        }

        String[] existingRoles = realm.getUserStoreManager().getRoleListOfUser(userName);
        if (existingRoles != null) {
            for (String role : existingRoles) {
                if (newRolesArray.contains(role)) {
                    newRolesArray.remove(role);
                }
            }
        }


        return newRolesArray.toArray(new String[newRolesArray.size()]);
    }

    public boolean updateRolesOfUserForApplication(String applicationId, String userName,
                                                   String[] rolesToDelete, String[] newRoles)
            throws ApplicationManagementException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        try {
            UserRealm realm =
                    Util.getRealmService()
                            .getTenantUserRealm(tenantManager.getTenantId(applicationId));
            realm.getUserStoreManager().updateRoleListOfUser(userName, rolesToDelete, newRoles);
            userApplicationCache.clearFromCache(userName);
            return true;
        } catch (UserStoreException e) {
            String msg =
                    "Error while updating roles for user: " + userName + " of application " +
                            applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    public String[] getUsersOfApplication(String applicationId)
            throws ApplicationManagementException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        ArrayList<String> userList = new ArrayList<String>();
        try {
            UserRealm realm =
                    Util.getRealmService()
                            .getTenantUserRealm(tenantManager.getTenantId(applicationId));
            String[] roles = realm.getUserStoreManager().getRoleNames();
            if (roles.length > 0) {
                for (String roleName : roles) {
                    if (!Util.getRealmService().getBootstrapRealmConfiguration()
                            .getEveryOneRoleName().equals(roleName)) {
                        String[] usersOfRole =
                                realm.getUserStoreManager()
                                        .getUserListOfRole(roleName);
                        if (usersOfRole != null && usersOfRole.length > 0) {
                            for (String userName : usersOfRole) {
                                if (!userList.contains(userName) &&
                                        !Util.getRealmService().getBootstrapRealmConfiguration()
                                                .getAdminUserName().equals(userName) &&
                                        !CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(userName)) {
                                    userList.add(userName);
                                }
                            }
                        }
                    }

                }
            }
            return userList.toArray(new String[userList.size()]);
        } catch (UserStoreException e) {
            String msg = "Error while getting users of application " + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    public boolean removeUserFromApplication(String applicationId, String userName)
            throws ApplicationManagementException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        try {
            UserRealm realm =
                    Util.getRealmService()
                            .getTenantUserRealm(tenantManager.getTenantId(applicationId));
            realm.getUserStoreManager().deleteUser(userName);
            userApplicationCache.clearFromCache(userName);

        } catch (UserStoreException e) {
            String msg =
                    "Error while removing user " + userName + " from application " +
                            applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }

        //Notify listeners about removing the user from application.
        Iterator<ApplicationEventsListener> appEventListeners =
                Util.getApplicationEventsListeners()
                        .iterator();
        try {
            Application app = ProjectUtils.getApplicationInfo(applicationId);

            /** Update the roles in this dto */
            UserInfo user = new UserInfo(userName);
            while (appEventListeners.hasNext()) {
                ApplicationEventsListener listener = appEventListeners.next();
                listener.onUserDeletion(app, user);
            }

        } catch (Exception ex) {
            String errorMsg = "Unable to publish user deletion event due to " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new ApplicationManagementException(errorMsg, ex);
        }

        return true;

    }

    public boolean revokeApplication(String applicationId) throws ApplicationManagementException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        try {
            tenantManager.deleteTenant(tenantManager.getTenantId(applicationId));

        } catch (UserStoreException e) {
            String msg = "Error while revoking application " + applicationId;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }

        // Notify listeners about the revoke
        Iterator<ApplicationEventsListener> appEventListeners =
                Util.getApplicationEventsListeners()
                        .iterator();

        try {
            Application application = ProjectUtils.getApplicationInfo(applicationId);
            while (appEventListeners.hasNext()) {
                ApplicationEventsListener listener = appEventListeners.next();
                listener.onRevoke(application);
            }
        } catch (AppFactoryException ex) {
            String errorMsg = "Unable to notify revoke application event due to " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new ApplicationManagementException(errorMsg, ex);
        }


        return true;
    }

    public boolean isApplicationIdAvailable(String applicationKey)
            throws ApplicationManagementException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        int tenantID;
        try {
            tenantID = tenantManager.getTenantId(applicationKey);
        } catch (UserStoreException e) {
            String msg = "Error while getting applicationKey " + applicationKey;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        return tenantID < 0;
    }

    public UserInfoBean getUserInfoBean(String userName) throws ApplicationManagementException {

        try {
            UserRealm realm =
                    Util.getRealmService()
                            .getTenantUserRealm(MultitenantConstants.SUPER_TENANT_ID);
            String email =
                    realm.getUserStoreManager().getUserClaimValue(userName, EMAIL_CLAIM_URI,
                            null);
            String firstName =
                    realm.getUserStoreManager().getUserClaimValue(userName,
                            FIRST_NAME_CLAIM_URI,
                            null);
            String lastName =
                    realm.getUserStoreManager().getUserClaimValue(userName,
                            LAST_NAME_CLAIM_URI,
                            null);
            return new UserInfoBean(userName, firstName, lastName, email);
        } catch (UserStoreException e) {
            String msg = "Error while getting info for user " + userName;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    public UserInfoBean[] getUserInfo(String applicationId) throws ApplicationManagementException {
        String[] users = getUsersOfApplication(applicationId);
        ArrayList<UserInfoBean> userInfoList = new ArrayList<UserInfoBean>();
        if (users != null && users.length > 0) {
            for (int i = 0; i < users.length; i++) {
                try {
                    userInfoList.add(getUserInfoBean(users[i]));
                } catch (ApplicationManagementException e) {
                    String msg =
                            "Error while getting info for user " + users[i] +
                                    "\n Continue getting other users information";
                    log.error(msg, e);
                }
            }
        }
        return userInfoList.toArray(new UserInfoBean[userInfoList.size()]);
    }

    public String[] getAllApplications(String userName) throws ApplicationManagementException {

        String apps[] = userApplicationCache.getValueFromCache(userName);
        if (apps != null) {
            return apps;
        } else {
            apps = new String[0];
        }
        List<String> list;
        TenantManager manager = Util.getRealmService().getTenantManager();
        try {
            String[] tenantDomainStrs = manager.getAllTenantDomainStrOfUser(userName);
            list = Arrays.asList(tenantDomainStrs);
        } catch (UserStoreException e) {
            String msg = "Error while getting all applications";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        if (!list.isEmpty()) {
            apps = list.toArray(new String[list.size()]);
        }
        userApplicationCache.addToCache(userName, apps);
        return apps;
    }

    public String[] getAllCreatedApplications() throws ApplicationManagementException {
        String apps[] = new String[0];
        List<String> list = new ArrayList<String>();
        TenantManager manager = Util.getRealmService().getTenantManager();
        try {
            Tenant[] tenants = manager.getAllTenants();

            for (Tenant tenant : tenants) {
                list.add(tenant.getDomain());
            }

        } catch (UserStoreException e) {
            String msg = "Error while getting all applications";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        if (!list.isEmpty()) {
            apps = list.toArray(new String[list.size()]);
        }
        return apps;
    }

    public ApplicationInfoBean[] getAllVersionsOfApplications() throws
            ApplicationManagementException,
            AppFactoryException {
        ApplicationInfoBean[] arrApplicationInfo;
        TenantManager manager = Util.getRealmService().getTenantManager();
        try {
            Tenant[] tenants = manager.getAllTenants();

            if (tenants == null || tenants.length == 0) {
                arrApplicationInfo = new ApplicationInfoBean[0];
            } else {
                arrApplicationInfo = new ApplicationInfoBean[tenants.length];
            }

            int index = 0;
            for (Tenant tenant : tenants) {
                Application application = ProjectUtils.getApplicationInfo(tenant.getDomain());
                ApplicationInfoBean applicationInfo = new ApplicationInfoBean();
                applicationInfo.setApplicationKey(tenant.getDomain());
                applicationInfo.setName(application.getName());
                Version[] versions = ProjectUtils.getVersions(tenant.getDomain());
                if (versions == null || versions.length == 0) {
                    applicationInfo.setVersions(new String[0]);
                } else {
                    String strVersions[] = new String[versions.length];
                    for (int i = 0; i < versions.length; i++) {
                        strVersions[i] = versions[i].getId();
                    }
                    applicationInfo.setVersions(strVersions);
                }

                arrApplicationInfo[index++] = applicationInfo;
            }

        } catch (UserStoreException e) {
            String msg = "Error while getting all applications";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }

        return arrApplicationInfo;
    }

    public UserApplications[] getApplicationsOfUser(String roleName)
            throws ApplicationManagementException {
        Map<String, ArrayList<String>> tempUserMap = new HashMap<String, ArrayList<String>>();
        TenantManager manager = Util.getRealmService().getTenantManager();
        try {
            Tenant[] tenants = manager.getAllTenants();

            for (Tenant tenant : tenants) {
                UserRealm realm = Util.getRealmService().getTenantUserRealm(tenant.getId());
                String[] userList = realm.getUserStoreManager().getUserListOfRole(roleName);

                if (userList != null && userList.length > 0) {
                    for (String userIdentifier : userList) {
                        ArrayList<String> elementList = tempUserMap.get(userIdentifier);
                        if (elementList == null) {
                            elementList = new ArrayList<String>();
                        }
                        elementList.add(tenant.getDomain());
                        tempUserMap.put(userIdentifier, elementList);
                    }
                }
            }

        } catch (UserStoreException e) {
            String msg = "Error while getting all users of applications";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }

        UserApplications arrUserApplications[];
        if (!tempUserMap.isEmpty()) {
            arrUserApplications = new UserApplications[tempUserMap.keySet().size()];
            int index = 0;
            for (String mapKey : tempUserMap.keySet()) {
                UserApplications userApplication = new UserApplications();
                userApplication.setUserNam(mapKey);
                userApplication.setApplications(tempUserMap.get(mapKey).toArray
                        (new String[tempUserMap.get(mapKey).size()]));
                arrUserApplications[index++] = userApplication;
            }
        } else {
            arrUserApplications = new UserApplications[0];
        }

        return arrUserApplications;
    }

    public UserRoleCount[] getUserCountByRoles() throws ApplicationManagementException {

        String[] createdApplications = getAllCreatedApplications();
        Map<String, Integer> userRoleCountMap = new HashMap<String, Integer>();

        for (String applicationId : createdApplications) {
            TenantManager tenantManager = Util.getRealmService().getTenantManager();

            try {
                UserRealm realm = Util.getRealmService().getTenantUserRealm(tenantManager.
                        getTenantId(applicationId));
                String[] roles = realm.getUserStoreManager().getRoleNames();

                for (String roleName : roles) {
                    if (!Util.getRealmService().getBootstrapRealmConfiguration()
                            .getEveryOneRoleName().equals(roleName)) {
                        String[] usersOfRole = realm.getUserStoreManager()
                                .getUserListOfRole(roleName);

                        userRoleCountMap.put(roleName, new Integer(usersOfRole.length));
                    }
                }

            } catch (UserStoreException e) {
                String msg = "Error while getting users of application "
                        + applicationId;
                log.error(msg, e);
                throw new ApplicationManagementException(msg, e);
            }
        }

        Set<String> userRoleKeySet = userRoleCountMap.keySet();
        UserRoleCount arrUserRoleCount[] = new UserRoleCount[userRoleKeySet.size()];

        int counter = 0;
        for (String roleName : userRoleKeySet) {
            UserRoleCount userRoleCount = new UserRoleCount();
            userRoleCount.setRoleName(roleName);
            userRoleCount.setUserCount(userRoleCountMap.get(roleName));
            arrUserRoleCount[counter++] = userRoleCount;
        }

        return arrUserRoleCount;
    }

    public String[] getRolesOfUserPerApplication(String appId, String userName)
            throws ApplicationManagementException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        org.wso2.carbon.user.api.UserStoreManager userStoreManager;
        ArrayList<String> roleList = new ArrayList<String>();
        String roles[];
        try {
            UserRealm realm =
                    Util.getRealmService()
                            .getTenantUserRealm(tenantManager.getTenantId(appId));
            userStoreManager = realm.getUserStoreManager();
            roles = userStoreManager.getRoleListOfUser(userName);
            for (String role : roles) {
                if (!Util.getRealmService().getBootstrapRealmConfiguration().getEveryOneRoleName()
                        .equals(role)) {
                    roleList.add(role);
                }
            }
        } catch (UserStoreException e) {
            String msg = "Error while getting role of the user " + userName;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
        return roleList.toArray(new String[roleList.size()]);
    }

    public Version[] getVersionsInStages()
            throws ApplicationManagementException, AppFactoryException {

        TenantManager manager = Util.getRealmService().getTenantManager();
        try {
            Tenant[] tenants = manager.getAllTenants();
            ArrayList<Version> allVersions = new ArrayList<Version>();

            for (Tenant tenant : tenants) {
                Version[] versions = ProjectUtils.getVersions(tenant.getDomain());
                if (versions != null && versions.length != 0) {
                    for (Version version : versions) {
                        allVersions.add(version);
                    }
                }
            }

            Version[] arrVersions = new Version[allVersions.size()];
            arrVersions = allVersions.toArray(arrVersions);

            return arrVersions;
        } catch (UserStoreException e) {
            String msg = "Error while getting versions";
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);
        }
    }

    public void publishApplicationCreation(String applicationId)
            throws ApplicationManagementException {
        //New application is created successfully so now time to clear realm in cache to reload
        // the new realm with updated permissions
        clearRealmCache(applicationId);

        Iterator<ApplicationEventsListener> appEventListeners =
                Util.getApplicationEventsListeners()
                        .iterator();
        try {
            Application application = ProjectUtils.getApplicationInfo(applicationId);
            if (application == null) {
                String errorMsg =
                        String.format("Unable to load application information for id ",
                                applicationId);
                throw new ApplicationManagementException(errorMsg);
            }

            while (appEventListeners.hasNext()) {
                ApplicationEventsListener listener = appEventListeners.next();
                listener.onCreation(application);
            }
        } catch (AppFactoryException ex) {
            String errorMsg = "Unable to publish application creation due to : " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new ApplicationManagementException(errorMsg, ex);
        }
    }

    public void publishApplicationVersionCreation(String applicationId, String sourceVersion,
                                                  String targetVersion)
            throws ApplicationManagementException {
        try {

            Iterator<ApplicationEventsListener> appEventListeners =
                    Util.getApplicationEventsListeners()
                            .iterator();

            Application application = ProjectUtils.getApplicationInfo(applicationId);

            Version[] versions = ProjectUtils.getVersions(applicationId);

            //find the versions.
            Version source = null;
            Version target = null;
            for (Version v : versions) {
                if (v.getId().equals(sourceVersion)) {
                    source = v;
                }

                if (v.getId().equals(targetVersion)) {
                    target = v;
                }

                if (source != null && target != null) {
                    // both version are found. no need to traverse more
                    break;
                }

            }

            while (appEventListeners.hasNext()) {
                ApplicationEventsListener listener = appEventListeners.next();
                listener.onVersionCreation(application, source, target);
            }

        } catch (AppFactoryException ex) {
            String errorMsg = "Unable to publish version creation due to " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new ApplicationManagementException(errorMsg, ex);
        }
    }

    /**
     * Service method to make the application related to given {@code applicationId} auto build.
     *
     * @param applicationId
     * @param stage
     * @param version
     * @param isAutoBuildable
     * @throws ApplicationManagementException
     */
    public void publishSetApplicationAutoBuild(String applicationId, String stage, String version,
                                               boolean isAutoBuildable)
            throws ApplicationManagementException {
        log.info("Auto build change event recieved for : " + applicationId + " " + " Version : " +
                version + " stage :" + stage + " isAutoBuildable :" + isAutoBuildable);
        updateRxtWithBuildStatus(applicationId, stage, version, isAutoBuildable);

        try {
            JenkinsCISystemDriver jenkinsCISystemDriver =
                    (JenkinsCISystemDriver) Util.getContinuousIntegrationSystemDriver();
            // TODO this from configuration
            int pollingPeriod = 6;

            jenkinsCISystemDriver.setJobAutoBuildable(applicationId, version, isAutoBuildable,
                    pollingPeriod);
            log.info("Application : " + applicationId + " sccessfully configured for auto building " + isAutoBuildable);
        } catch (AppFactoryException e) {
            String msg = "Error occured while updating jenkins configuration";
            log.error(msg, e);
            throw new ApplicationManagementException(msg);
        }

    }

    /**
     * Service method to make the application related to given {@code applicationId} auto deploy.
     *
     * @param applicationId
     * @param stage
     * @param version
     * @param isAutoDeployable
     * @throws ApplicationManagementException
     */
    public void publishSetApplicationAutoDeploy(String applicationId, String stage, String version,
                                                boolean isAutoDeployable)
            throws ApplicationManagementException {
        log.info("Auto deploy change event recieved for : " + applicationId + " " + " Version : " +
                version + " stage :" + stage + " isAutoBuildable :" + isAutoDeployable);
        updateRxtWithDeplymentStatus(applicationId, stage, version, isAutoDeployable);
        try {
            JenkinsCISystemDriver jenkinsCISystemDriver =
                    (JenkinsCISystemDriver) Util.getContinuousIntegrationSystemDriver();

            jenkinsCISystemDriver.setJobAutoDeployable(applicationId, version, isAutoDeployable);

            log.info("Application : " + applicationId + " sccessfully configured for auto deploy " +
                    isAutoDeployable);
        } catch (AppFactoryException e) {
            String msg = "Error occured while updating jenkins configuration";
            log.error(msg, e);
            throw new ApplicationManagementException(msg);
        }
    }

    /**
     * Updates the rxt registry with given auto build information.
     *
     * @param applicationId
     * @param stage
     * @param version
     * @param isAutoBuildable
     * @throws ApplicationManagementException
     */
    private void updateRxtWithBuildStatus(String applicationId, String stage, String version,
                                          boolean isAutoBuildable)
            throws ApplicationManagementException {
        RxtManager rxtManager = new RxtManager();
        try {
            rxtManager.updateAppVersionRxt(applicationId, stage, version, "appversion_isAutoBuild",
                    String.valueOf(isAutoBuildable));
            log.debug(" Rtx updated successfully for : " + applicationId + " " + " Version : " +
                    version + " stage :" + stage + " isAutoBuildable :" + isAutoBuildable);

        } catch (AppFactoryException e) {
            String msg = "Error occured while updating the rxt with auto-build status";
            log.error(msg, e);
            throw new ApplicationManagementException(msg);
        }
    }

    /**
     * Updates the rxt registry with given auto deploy information.
     *
     * @param applicationId
     * @param stage
     * @param version
     * @param isAutoDeployable
     * @throws ApplicationManagementException
     */
    private void updateRxtWithDeplymentStatus(String applicationId, String stage, String version,
                                              boolean isAutoDeployable)
            throws ApplicationManagementException {
        RxtManager rxtManager = new RxtManager();
        try {
            rxtManager.updateAppVersionRxt(applicationId, stage, version, "appversion_isAutoDeploy",
                    String.valueOf(isAutoDeployable));
            log.debug(" Rtx updated successfully for : " + applicationId + " " + " Version : " +
                    version + " stage :" + stage + " isAutoDeployable :" + isAutoDeployable);

        } catch (AppFactoryException e) {
            String msg = "Error occured while updating the rxt with auto-build status";
            log.error(msg, e);
            throw new ApplicationManagementException(msg);
        }
    }

    public void publishApplicationAutoDeploymentChange(String applicationId, String previousVersion,
                                                       String nextVersion, String versionStage)
            throws ApplicationManagementException {

        JenkinsCISystemDriver jenkinsCISystemDriver = (JenkinsCISystemDriver) Util.getContinuousIntegrationSystemDriver();

        int pollingPeriod = 0;

        try {

            Application application = ProjectUtils.getApplicationInfo(applicationId);

            Version[] versions = ProjectUtils.getVersions(applicationId);

            //find the versions.
            /*  Version previous = null;
            Version next = null;
          if (!previousVersion.trim().equals("") && !nextVersion.trim().equals("")) {
                for (Version v : versions) {
                    if (v.getId().equals(previousVersion)) {
                        previous = v;
                    }

                    if (v.getId().equals(nextVersion)) {
                        next = v;
                    }

                    if (previous != null && next != null) {
                        // both version are found. no need to traverse more
                        break;
                    }

                }
            }  else {
                for (Version v : versions) {
                                    if (v.getId().equals(previousVersion)) {
                                        previous = v;
                                    }
                }

            }
*/

            log.info("AutoDeployment Version Change event recieved for : " + application.getId() + " " +
                    application.getName() + " From Version : " + previousVersion + " To Version : " + nextVersion);

            if (previousVersion != null && previousVersion != "") {
                jenkinsCISystemDriver.editADJobConfiguration(applicationId, previousVersion, "removeAD", pollingPeriod);

            }
            if (nextVersion != null && nextVersion != "") {
                AppFactoryConfiguration configuration = Util.getConfiguration();
                pollingPeriod = Integer.parseInt(configuration.getFirstProperty("ApplicationDeployment.DeploymentStage."
                        + versionStage + ".AutomaticDeployment.PollingPeriod"));
                jenkinsCISystemDriver.editADJobConfiguration(applicationId, nextVersion, "addAD", pollingPeriod);

            }


        } catch (AppFactoryException ex) {
            String errorMsg = "Unable to publish Auto Deoployment State Change due to  " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new ApplicationManagementException(errorMsg, ex);
        }

    }

    private void clearRealmCache(String applicationKey) throws ApplicationManagementException {
        RealmService realmService = Util.getRealmService();
        int tenantID;
        try {
            tenantID = Util.getRealmService().getTenantManager().getTenantId(applicationKey);
            realmService.clearCachedUserRealm(tenantID);
        } catch (UserStoreException e) {
            String errorMsg = "Unable to clear user realm cache for tenant id  " + applicationKey +
                    " due to : " + e.getMessage();
            log.error(errorMsg, e);
            throw new ApplicationManagementException(errorMsg, e);
        }
    }

    public boolean sendMail(String applicationId, String userName, String roles[], String config)
            throws ApplicationManagementException {
        EmailSender sender = new EmailSender(loadEmailSenderConfiguration(config));
        try {
            Application currentApp = ProjectUtils.getApplicationInfo(applicationId);
            String applicationName = currentApp.getName();
            String applicationDescription = currentApp.getDescription();
            Map<String, String> userParams = new HashMap<String, String>();
            userParams.put("applicationId", applicationId);
            userParams.put("userName", userName);
            userParams.put("roles", roles[0]);
            userParams.put("applicationName", applicationName);
            if (applicationDescription != null && applicationDescription != "") {
                userParams.put("applicationDescription", applicationDescription);
            } else {
                userParams.put("applicationDescription", "<No description available on the project>");
            }

            sender.sendEmail(userName, userParams);


        } catch (Exception e) {
            String msg = "Email sending is failed for  " + userName;
            log.error(msg, e);
            throw new ApplicationManagementException(msg, e);

        }
        return true;

    }

    private EmailSenderConfiguration loadEmailSenderConfiguration(String configFile) {
        String configFilePath = CarbonUtils.getCarbonConfigDirPath() + File.separator + "email" +
                File.separator + configFile;
        return EmailSenderConfiguration.loadEmailSenderConfiguration(configFilePath);
    }
}