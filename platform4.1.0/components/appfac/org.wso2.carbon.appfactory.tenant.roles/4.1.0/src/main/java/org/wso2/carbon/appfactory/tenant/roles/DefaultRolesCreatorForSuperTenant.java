/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.appfactory.tenant.roles;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.tenant.roles.util.Util;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.Permission;

import java.util.ArrayList;
import java.util.List;

/**
 * Platform level roles defined in appfactory.xml are created through this
 * class.
 * All the permissions defined are assigned to the roles and if the role is
 * existing, permissions
 * are updated.
 */
public class DefaultRolesCreatorForSuperTenant {
    private static Log log = LogFactory.getLog(DefaultRolesCreatorForSuperTenant.class);
    private List<RoleBean> roleBeanList = null;

    public DefaultRolesCreatorForSuperTenant() throws Exception {
        roleBeanList = new ArrayList<RoleBean>();
        AppFactoryConfiguration configuration = Util.getConfiguration();
        try {
            String adminUser =Util.getRealmService().getBootstrapRealm().
            getRealmConfiguration().
            getAdminUserName();
            loadPlatformDefaultRoleConfigurations(configuration, adminUser);
            loadPlatformRoleConfigurations(configuration, adminUser);
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            String message = "Failed to read default roles from appfactory configuration.";
            log.error(message);
            throw new Exception(message, e);
        }
    }

    private void loadPlatformDefaultRoleConfigurations(AppFactoryConfiguration configuration,
                                                       String adminUser) {
        String[] roles = configuration.getProperties("PlatformRoles.DefaultUserRole");
        for (String role : roles) {
            String permissionIdString =configuration.
            getFirstProperty("PlatformRoles.DefaultUserRole." +
                             role + ".Permission");
            String[] permissionIds = permissionIdString.split(",");
            RoleBean roleBean = new RoleBean(role);
            roleBean.addUser(adminUser);
            for (String permissionId : permissionIds) {
                String[] resourceAndActionParts = permissionId.split(":");
                if (resourceAndActionParts.length == 2) {
                    Permission permission =new Permission(resourceAndActionParts[0],
                                                           resourceAndActionParts[1]);
                    roleBean.addPermission(permission);

                } else if (resourceAndActionParts.length == 1) {
                    Permission permission =new Permission(resourceAndActionParts[0],
                                                          CarbonConstants.UI_PERMISSION_ACTION);
                    roleBean.addPermission(permission);
                }
            }
            roleBeanList.add(roleBean);
        }
    }

    private void loadPlatformRoleConfigurations(AppFactoryConfiguration configuration,
                                                String adminUser) {
        String[] roles = configuration.getProperties("PlatformRoles.Role");
        for (String role : roles) {
            String permissionIdString =configuration.getFirstProperty("PlatformRoles.Role." +
                                                                       role + ".Permission");
            String[] permissionIds = permissionIdString.split(",");
            RoleBean roleBean = new RoleBean(role);
            roleBean.addUser(adminUser);
            for (String permissionId : permissionIds) {
                String[] resourceAndActionParts = permissionId.split(":");
                if (resourceAndActionParts.length == 2) {
                    Permission permission =new Permission(resourceAndActionParts[0],
                                                           resourceAndActionParts[1]);
                    roleBean.addPermission(permission);

                } else if (resourceAndActionParts.length == 1) {
                    Permission permission =new Permission(resourceAndActionParts[0],
                                                           CarbonConstants.UI_PERMISSION_ACTION);
                    roleBean.addPermission(permission);
                }
            }
            roleBeanList.add(roleBean);
        }
    }

    public void createDefaultRoles() throws UserStoreException {
        UserStoreManager userStoreManager =Util.getRealmService().getBootstrapRealm().
        getUserStoreManager();
        AuthorizationManager authorizationManager =Util.getRealmService().getBootstrapRealm().
        getAuthorizationManager();
        for (RoleBean roleBean : roleBeanList) {
            if (!userStoreManager.isExistingRole(roleBean.getRoleName())) {
                userStoreManager.addRole(roleBean.getRoleName(),
                                         roleBean.getUsers().toArray(new String[roleBean.getUsers()
                                                                                        .size()]),
                                         roleBean.getPermissions()
                                                 .toArray(new Permission[roleBean.getPermissions()
                                                                                 .size()]));
            } else {
                for (Permission permission : roleBean.getPermissions()) {
                    if (!authorizationManager.isRoleAuthorized(roleBean.getRoleName(),
                                                               permission.getResourceId(),
                                                               permission.getAction())) {
                        authorizationManager.authorizeRole(roleBean.getRoleName(),
                                                           permission.getResourceId(),
                                                           permission.getAction());
                    }
                }
            }
        }

    }
}