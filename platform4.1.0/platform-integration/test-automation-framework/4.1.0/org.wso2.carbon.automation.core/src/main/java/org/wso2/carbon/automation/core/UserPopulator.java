/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.automation.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.api.clients.authenticators.AuthenticatorClient;
import org.wso2.carbon.automation.api.clients.registry.ResourceAdminServiceClient;
import org.wso2.carbon.automation.api.clients.security.KeyStoreAdminServiceClient;
import org.wso2.carbon.automation.api.clients.stratos.tenant.mgt.TenantMgtAdminServiceClient;
import org.wso2.carbon.automation.api.clients.user.mgt.UserManagementClient;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.environmentutils.ClusterReader;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkFactory;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkProperties;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkSettings;
import org.wso2.carbon.security.mgt.stub.keystore.KeyStoreAdminServiceSecurityConfigExceptionException;
import org.wso2.carbon.user.mgt.common.UserAdminException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

public class UserPopulator {
    private static final Log log = LogFactory.getLog(UserPopulator.class);
    private UserManagementClient userMgtAdmin;

    EnvironmentBuilder environmentBuilder;
    FrameworkSettings framework;

    UserPopulator() {
        environmentBuilder = new EnvironmentBuilder();
        framework = environmentBuilder.getFrameworkSettings();
    }

    public void populateUsers(List<String> productList)
            throws Exception {
        String executionEnv = environmentBuilder.getFrameworkSettings().getEnvironmentSettings().executionEnvironment();

        boolean isRunningOnStratos = environmentBuilder.getFrameworkSettings().getEnvironmentSettings().is_runningOnStratos();
        log.info("Populating Users....");
        if (isRunningOnStratos) {
            UserInfo superTenantDetails = UserListCsvReader.getUserInfo(ProductConstant.SUPER_ADMIN_USER_ID);
            int userCount = UserListCsvReader.getUserCount();

            if (ExecutionEnvironment.stratos.name().equalsIgnoreCase(executionEnv)) {
                createStratosUsers(superTenantDetails, userCount, productList);
            } else {
                createStratosUsersForIntegration(superTenantDetails, userCount, productList);

            }
            log.info("Users Populated");
        } else {
            int adminUserId = 0;
            UserInfo adminDetails = UserListCsvReader.getUserInfo(adminUserId);
            ClusterReader clusterReader = new ClusterReader();
            if (framework.getEnvironmentSettings().isClusterEnable()) {
                clusterReader.getClusterList();
                for (String id : clusterReader.getClusterList()) {
                    if (productList.contains(clusterReader.getProductName(id).toUpperCase())) {
                        FrameworkProperties properties = FrameworkFactory.getClusterProperties(id);
                        String backendURL = properties.getProductVariables().getBackendUrl();
                        String hostName = properties.getProductVariables().getHostName();
                        String sessionCookieUser = login(adminDetails.getUserName(), adminDetails.getPassword(), backendURL, hostName);
                        userMgtAdmin = new UserManagementClient(backendURL, sessionCookieUser);
                        log.info("Populate users to " + id + " server");
                        createProductUsers(backendURL);
                    }
                }
            } else {
                for (String product : productList) {
                    FrameworkProperties properties = FrameworkFactory.getFrameworkProperties(product);
                    String backendURL = properties.getProductVariables().getBackendUrl();
                    String hostName = properties.getProductVariables().getHostName();
                    String sessionCookieUser = login(adminDetails.getUserName(), adminDetails.getPassword(), backendURL, hostName);
                    userMgtAdmin = new UserManagementClient(backendURL, sessionCookieUser);
                    log.info("Populate user to " + product + " server");
                    createProductUsers(backendURL);
                }
            }
        }
    }


    private void createProductUsers(String backendUrl) throws Exception {
        UserInfo adminUserInfo = UserListCsvReader.getUserInfo(0);
        createRoleWithAllPermissions(ProductConstant.DEFAULT_PRODUCT_ROLE, backendUrl, adminUserInfo);
        for (int userId = 0; userId < UserListCsvReader.getUserCount(); userId++) {
            if (userId != 0) {
                String userId_str = Integer.toString(userId);
                int userIdValue = UserListCsvReader.getUserId(userId_str);
                UserInfo userDetails = UserListCsvReader.getUserInfo(userIdValue);

                try {
                    if (!userMgtAdmin.userNameExists(ProductConstant.DEFAULT_PRODUCT_ROLE,
                                                     userDetails.getUserName())) {
                        if (userId == 1) {
                            userMgtAdmin.addUser(userDetails.getUserName(), userDetails.getUserName(),
                                                 new String[]{"admin"}, null);
                        } else {
                            userMgtAdmin.addUser(userDetails.getUserName(), userDetails.getUserName(),
                                                 new String[]{ProductConstant.DEFAULT_PRODUCT_ROLE}, null);
                        }
                        log.info("User " + userDetails.getUserName() + " was created successfully");
                    }
                } catch (UserAdminException e) {
                    log.error("Unable to add users :", e);
                    throw new UserAdminException("Unable to add role :", e);
                }
            }
        }
    }

    private void createStratosUsers(UserInfo superTenantDetails, int userCount,
                                    List<String> productList) throws Exception {
        //get the first product name
        FrameworkProperties manProperties =
                FrameworkFactory.getFrameworkProperties(productList.get(0));
        populateTenants(superTenantDetails, userCount, manProperties);
    }


    private void createStratosUsersForIntegration(UserInfo superTenantDetails, int userCount,
                                                  List<String> productList) throws Exception {
        for (String product : productList) {
            //get the first product name
            FrameworkProperties manProperties = FrameworkFactory.getFrameworkProperties(product);
            populateTenants(superTenantDetails, userCount, manProperties);
        }
    }

    private void populateTenants(UserInfo superTenantDetails, int userCount,
                                 FrameworkProperties manProperties) throws Exception {

        String sessionCookie = login(superTenantDetails.getUserName(), superTenantDetails.getPassword(),
                                     manProperties.getProductVariables().getBackendUrl(),
                                     manProperties.getProductVariables().getHostName());

        TenantMgtAdminServiceClient tenantStub =
                new TenantMgtAdminServiceClient(manProperties.getProductVariables().getBackendUrl(),
                                                sessionCookie);
        UserInfo tenantDetails = null;
        String tenantAdminSession = null;
        for (int userId = 0; userId < userCount; userId++) {
            if (userId == ProductConstant.ADMIN_USER_ID) {
                String userId_str = Integer.toString(userId);
                int tenantId = UserListCsvReader.getUserId(userId_str);
                tenantDetails = UserListCsvReader.getUserInfo(tenantId);
                tenantStub.addTenant(tenantDetails.getDomain(), tenantDetails.getPassword(),
                                     ProductConstant.TENANT_ADMIN_PASSWORD,
                                     "demo");
                UserInfo tenantAdminUserInfo = UserListCsvReader.getUserInfo(ProductConstant.ADMIN_USER_ID);//get tenant admin
                tenantAdminSession = login(tenantDetails.getUserName(), tenantDetails.getPassword()
                        , manProperties.getProductVariables().getBackendUrl(), manProperties.getProductVariables().getHostName());
                createRoleWithAllPermissions(ProductConstant.DEFAULT_PRODUCT_ROLE,
                                             manProperties.getProductVariables().getBackendUrl(),
                                             tenantAdminUserInfo);

                addKeyStoreIfAvailable(manProperties.getProductVariables().getBackendUrl(), tenantDetails);

            } else if (userId > ProductConstant.ADMIN_USER_ID) { // populate all tenant users
                assert tenantDetails != null;

                userMgtAdmin =
                        new UserManagementClient(manProperties.getProductVariables().getBackendUrl(),
                                                 tenantAdminSession);
                UserInfo userDetails = UserListCsvReader.getUserInfo(userId);
                if (!userMgtAdmin.userNameExists(ProductConstant.DEFAULT_PRODUCT_ROLE,
                                                 userDetails.getUserNameWithoutDomain())) {
                    userMgtAdmin.addUser(userDetails.getUserName().substring(0, userDetails.getUserName().lastIndexOf('@')),
                                         userDetails.getPassword(),
                                         new String[]{ProductConstant.DEFAULT_PRODUCT_ROLE}, null);
                    log.info("Tenant User " + userDetails.getUserName() + " was created successfully");
                }
            }
        }
    }


    protected static String login(String userName, String password, String backendUrl,
                                  String hostName)
            throws RemoteException, LoginAuthenticationExceptionException {
        AuthenticatorClient loginClient = new AuthenticatorClient(backendUrl);
        return loginClient.login(userName, password, hostName);
    }

    public static void createRoleWithAllPermissions(String roleName, String backendUrl,
                                                    UserInfo adminUserInfo) throws Exception {
        ResourceAdminServiceClient resourceAdmin;
        UserManagementClient userManagementClient;

        //done this change due to a bug in UM - please refer to carbon dev mail
        // "G-Reg integration test failures due to user mgt issue."
        String[] permissions = {"/permission/admin/configure/",
                                "/permission/admin/login",
                                "/permission/admin/manage/",
                                "/permission/admin/monitor",
                                "/permission/protected"};

        String session = login(adminUserInfo.getUserName(), adminUserInfo.getPassword(), backendUrl,
                               new URL(backendUrl).getHost());
        resourceAdmin = new ResourceAdminServiceClient(backendUrl, session);
        userManagementClient = new UserManagementClient(backendUrl, session);
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

    private void addKeyStoreIfAvailable(String backEndUrl, UserInfo user)
            throws IOException, KeyStoreAdminServiceSecurityConfigExceptionException,
                   LoginAuthenticationExceptionException {
        String keyStoreFilePath = ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator + "security"
                                  + File.separator + "keystore" + File.separator + "service.jks";
        File file = new File(keyStoreFilePath);
        if (!file.exists()) {
            return;
        }
        String hostname = new URL(backEndUrl).getHost();
        String sessionCookie = login(user.getUserName(), user.getPassword(), backEndUrl, hostname);
        KeyStoreAdminServiceClient keyStoreAdminServiceClient = new KeyStoreAdminServiceClient(backEndUrl, sessionCookie);

        String fileName = "service.jks";
        String keyStorePassword = "automation";
        String privateKeyPass = "automation";
        if (!keyStoreAdminServiceClient.getKeyStoresList().contains(fileName)) {
            keyStoreAdminServiceClient.addKeyStore(keyStoreFilePath, fileName, keyStorePassword, privateKeyPass);
            log.info("service.jks uploaded");
        }
    }
}