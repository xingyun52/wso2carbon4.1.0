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
package org.wso2.carbon.esb.proxyservice.test.secureProxy;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.carbon.aarservices.stub.ExceptionException;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.utils.axis2serverutils.SampleAxis2Server;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentVariables;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkFactory;
import org.wso2.carbon.esb.util.ServiceDeploymentUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

public class Axis2ServerStartupWithSecuredServices {
    private SampleAxis2Server axis2Server1 = null;
    EnvironmentBuilder builder = null;

    @BeforeTest(alwaysRun = true)
    public void deployServices()
            throws IOException, LoginAuthenticationExceptionException, ExceptionException {

        if (FrameworkFactory.getFrameworkProperties(ProductConstant.ESB_SERVER_NAME).getEnvironmentSettings().is_builderEnabled()) {
            axis2Server1 = new SampleAxis2Server("test_axis2_server_9007.xml");
            axis2Server1.deployService("SecureStockQuoteServiceScenario1");
            axis2Server1.start();

            axis2Server1.deployService("SecureStockQuoteServiceScenario2");
            axis2Server1.deployService("SecureStockQuoteServiceScenario3");
            axis2Server1.deployService("SecureStockQuoteServiceScenario4");
            axis2Server1.deployService("SecureStockQuoteServiceScenario5");
            axis2Server1.deployService("SecureStockQuoteServiceScenario6");
            axis2Server1.deployService("SecureStockQuoteServiceScenario7");
            axis2Server1.deployService("SecureStockQuoteServiceScenario8");
            //        axis2Server1.deployService("SecureStockQuoteServiceScenario9");
            //        axis2Server1.deployService("SecureStockQuoteServiceScenario10");

        } else {
            builder = new EnvironmentBuilder().as(ProductConstant.ADMIN_USER_ID);
            EnvironmentVariables appServer = builder.build().getAs();
            int deploymentDelay = builder.getFrameworkSettings().getEnvironmentVariables().getDeploymentDelay();
            String serviceName = "SecureStockQuoteServiceScenario";
            String serviceFilePath = ProductConstant.getResourceLocations(ProductConstant.AXIS2_SERVER_NAME)
                                     + File.separator + "aar" + File.separator + serviceName;
            ServiceDeploymentUtil deployer = new ServiceDeploymentUtil();
            for (int i = 1; i < 9; i++) {
                deployer.deployArrService(appServer.getBackEndUrl(), appServer.getSessionCookie()
                        , serviceName + i, serviceFilePath + i + ".aar", deploymentDelay);
            }
        }
    }

    @AfterTest(alwaysRun = true)
    public void unDeployServices()
            throws MalformedURLException, LoginAuthenticationExceptionException, ExceptionException,
                   RemoteException {
        if (axis2Server1 != null && axis2Server1.isStarted()) {
            axis2Server1.stop();
        } else {
            if (builder != null) {
                EnvironmentVariables appServer = builder.build().getAs();
                int deploymentDelay = builder.getFrameworkSettings().getEnvironmentVariables().getDeploymentDelay();
                String serviceName = "SecureStockQuoteServiceScenario";
                ServiceDeploymentUtil deployer = new ServiceDeploymentUtil();
                for (int i = 1; i < 9; i++) {
                    deployer.unDeployArrService(appServer.getBackEndUrl(), appServer.getSessionCookie()
                            , serviceName + i, deploymentDelay);
                }
            }
        }
    }
}
