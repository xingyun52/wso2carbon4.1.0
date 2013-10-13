/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.automation.utils.usermgt;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.api.clients.registry.ResourceAdminServiceClient;
import org.wso2.carbon.automation.api.clients.user.mgt.UserManagementClient;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceResourceServiceExceptionException;
import org.wso2.carbon.user.mgt.stub.GetAllRolesNamesUserAdminExceptionException;

import java.rmi.RemoteException;

public class UserManagementUtil {
    private static final Log log = LogFactory.getLog(UserManagementUtil.class);

    /**
     * Add new user to given role
     * @param backendUrl - backend url of products
     * @param newUserName - user name of the new user to be added
     * @param newUserPassword  - password of the new user to be added.
     * @param roleName - role of the user
     * @param adminUserInfo - UserBean of admin user
     * @throws Exception - throws if user addition fails.
     */
    public static void createUser(String backendUrl, String newUserName,
                                  String newUserPassword, String roleName,
                                  UserInfo adminUserInfo)
            throws Exception {

        UserManagementClient userManagementClient = null;
        userManagementClient = new UserManagementClient(backendUrl, adminUserInfo.getUserName(),
                                                        adminUserInfo.getPassword());

        if (userManagementClient.roleNameExists(roleName)) {
            userManagementClient.addUser(newUserName, newUserPassword, new String[]{roleName}, null);
            log.info("User " + newUserName + " was created successfully");
        }
    }

    /**
     * The role will be created with all permissions and read, write, delete and authorize permission fro registry browser
     *
     * @param roleName - name of the role to be added
     * @param backendUrl - backendURL of the product
     * @param adminUserInfo - UserBean of admin user
     * @throws Exception - throws if role addition fails
     */
    public static void createRoleWithAllPermissions(String roleName, String backendUrl,
                                                    UserInfo adminUserInfo)
            throws Exception {
        ResourceAdminServiceClient resourceAdmin = null;
        UserManagementClient userManagementClient = null;
        String[] permissions = {"/permission/"};
        resourceAdmin = new ResourceAdminServiceClient(backendUrl, adminUserInfo.getUserName(),
                                                       adminUserInfo.getPassword());
        userManagementClient = new UserManagementClient(backendUrl, adminUserInfo.getUserName(),
                                                        adminUserInfo.getPassword());
        String[] userList = null;
        if (!userManagementClient.roleNameExists(roleName)) {
            userManagementClient.addRole(roleName, userList, permissions);
            resourceAdmin.addResourcePermission("/", ProductConstant.DEFAULT_PRODUCT_ROLE, "3", "1");
            resourceAdmin.addResourcePermission("/", ProductConstant.DEFAULT_PRODUCT_ROLE, "2", "1");
            resourceAdmin.addResourcePermission("/", ProductConstant.DEFAULT_PRODUCT_ROLE, "4", "1");
            resourceAdmin.addResourcePermission("/", ProductConstant.DEFAULT_PRODUCT_ROLE, "5", "1");
            log.info("Role " + roleName + " was created successfully");
        }
    }
}



