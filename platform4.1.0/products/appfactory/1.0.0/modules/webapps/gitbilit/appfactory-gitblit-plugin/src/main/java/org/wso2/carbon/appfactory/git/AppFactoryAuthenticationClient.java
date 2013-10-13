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

package org.wso2.carbon.appfactory.git;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.appfactory.git.util.Util;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.authenticator.stub.LogoutAuthenticationExceptionException;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * The client for authenticate user using AuthenticationAdmin web service
 */
public class AppFactoryAuthenticationClient {
    private static final Logger log = LoggerFactory.getLogger(ApplicationManagementServiceClient.class);
    private AuthenticationAdminStub serviceStub;

    /**
     * Constructor taking Gitblit configuration
     *
     * @param configuration
     */
    public AppFactoryAuthenticationClient(GitBlitConfiguration configuration) {
        try {
            ConfigurationContext context=Axis2ConfigurationContextHolder.getHolder().getConfigurationContext();
            serviceStub = new AuthenticationAdminStub(context,configuration.getProperty(GitBlitConstants
                                                                                        .APPFACTORY_URL, "https://localhost:9443") + "/services/AuthenticationAdmin");
            Util.setMaxTotalConnection(serviceStub._getServiceClient());
        } catch (AxisFault fault) {
            log.error("Error occurred while initializing client ", fault);
        } catch (RemoteException e) {
            log.error("Error occurred in remote end while initializing client ", e);
        }
    }

    /**
     * @param userName
     * @param password
     * @return
     */
    public boolean authenticate(String userName, String password) {
        // Create service stub
        boolean isAuth = false;
        try {
            if (serviceStub.login(userName, password, null)) {
                isAuth = true;
            }
        } catch (AxisFault e) {
            log.error("Error while calling ApplicationManagementService:Error is " + e.getLocalizedMessage(), e);
        } catch (RemoteException e) {
            log.error("Error while calling ApplicationManagementService:Error is " + e.getLocalizedMessage(), e);
        } catch (IOException e) {
            log.error("Error while calling ApplicationManagementService:Error is " + e.getLocalizedMessage(), e);
        } catch (LoginAuthenticationExceptionException e) {
            log.error("Error while calling ApplicationManagementService:Error is " + e.getLocalizedMessage(), e);
        }finally {
            try {
                serviceStub._getServiceClient().cleanupTransport();
                serviceStub._getServiceClient().cleanup();
                serviceStub.cleanup();
            } catch (AxisFault fault) {
                //ignore
            }
        }
        return isAuth;
    }

    public void logout() {
        try {
            serviceStub.logout();
        } catch (RemoteException e) {
            log.error("Error while calling ApplicationManagementService:Error is " + e.getLocalizedMessage(), e);
        } catch (LogoutAuthenticationExceptionException e) {
            log.error("Error while calling ApplicationManagementService:Error is " + e.getLocalizedMessage(), e);
        }finally {
            try {
                serviceStub._getServiceClient().cleanupTransport();
                serviceStub._getServiceClient().cleanup();
                serviceStub.cleanup();
            } catch (AxisFault fault) {
                //ignore
            }
        }
    }
}
