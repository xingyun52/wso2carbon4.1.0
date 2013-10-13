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

package org.wso2.carbon.automation.core.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.api.clients.logging.LogViewerClient;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkFactory;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkProperties;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;

import java.rmi.RemoteException;

import static org.testng.Assert.assertFalse;

public abstract class ServerStartupBaseTest {
    private LogViewerClient logViewerClient;
    private static final Log log = LogFactory.getLog(ServerStartupBaseTest.class);
    private static final String SERVER_START_LINE = "Starting WSO2 Carbon";
    private static final String MANAGEMENT_CONSOLE_URL = "Mgt Console URL";
    public String productName;

    abstract public String getProductName();

    @BeforeSuite(alwaysRun = true)
    public void initialize() throws LoginAuthenticationExceptionException, RemoteException {
        int userId = ProductConstant.SUPER_ADMIN_USER_ID;
        FrameworkProperties properties = FrameworkFactory.getFrameworkProperties(getProductName());
        UserInfo userInfo = UserListCsvReader.getUserInfo(userId);
        logViewerClient = new LogViewerClient(properties.getProductVariables().getBackendUrl(),
                                              userInfo.getUserName(), userInfo.getPassword());
    }



    @Test(groups = "wso2.all", description = "verify server startup errors")
    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_user})
    public void testVerifyLogs() throws RemoteException {
        boolean status = false;
        int startLine = 0;
        int stopLine = 0;

        LogEvent[] logEvents = logViewerClient.getAllSystemLogs();
        if (logEvents.length > 0) {
            for (int i = 0; i < logEvents.length; i++) {
                if (logEvents[i] != null) {
                    if (logEvents[i].getMessage().contains(SERVER_START_LINE)) {
                        stopLine = i;
                        log.info("Server started message found - " + logEvents[i].getMessage());

                    }
                    if (logEvents[i].getMessage().contains(MANAGEMENT_CONSOLE_URL)) {
                        startLine = i;
                        log.info("Server stopped message found - " + logEvents[i].getMessage());
                    }
                }

                if (startLine != 0 && stopLine != 0) {
                    break;
                }
            }

            while (startLine <= stopLine) {
                if (logEvents[startLine].getPriority().contains("ERROR")) {
                    log.error("Startup contain errors - " + logEvents[startLine].getMessage());
                    status = true;
                    break;
                }
                startLine++;
            }
        }
        assertFalse(status, "Server started with errors");
    }
}
