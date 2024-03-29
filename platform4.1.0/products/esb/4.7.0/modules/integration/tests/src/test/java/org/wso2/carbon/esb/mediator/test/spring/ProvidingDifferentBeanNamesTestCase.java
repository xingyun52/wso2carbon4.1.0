/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.esb.mediator.test.spring;

import org.apache.axis2.AxisFault;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.api.clients.registry.ResourceAdminServiceClient;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.esb.ESBIntegrationTest;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import javax.activation.DataHandler;
import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ProvidingDifferentBeanNamesTestCase extends ESBIntegrationTest {

    private static final String SIMPLE_BEAN_JAR = "org.wso2.carbon.test.simplebean.jar";
    private static final String JAR_LOCATION = "/artifacts/ESB/jar";

    private ServerConfigurationManager serverConfigurationManager;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        init(ProductConstant.ADMIN_USER_ID);
        serverConfigurationManager = new ServerConfigurationManager(esbServer.getBackEndUrl());
        serverConfigurationManager.copyToComponentLib
                (new File(getClass().getResource(JAR_LOCATION + File.separator + SIMPLE_BEAN_JAR).toURI()));
        serverConfigurationManager.restartGracefully();

        init(ProductConstant.ADMIN_USER_ID);
        uploadResourcesToConfigRegistry();
        loadESBConfigurationFromClasspath("/artifacts/ESB/mediatorconfig/spring/spring_mediation.xml");
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @Test(groups = {"wso2.esb", "localOnly"}, description = "Spring Mediator " +
                                                            "-Added Simple bean into lib " +
                                                            "-Different bean ids in spring xml")
    public void testUsingAddedBeanSpringMediation() throws AxisFault {

        try {
            axis2Client.sendSimpleStockQuoteRequest
                    (getMainSequenceURL(), null, "IBM");
            fail();
        } catch (AxisFault axisFault) {
            assertEquals(axisFault.getMessage(), "No bean named 'springtest' is defined", "Fault: Error message mismatched");
        }

    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        deleteSequence("main");
        Thread.sleep(30000);//wait till the main sequence get deleted before restarting.
        clearUploadedResource();
        super.cleanup();
        serverConfigurationManager.removeFromComponentLib(SIMPLE_BEAN_JAR);
        serverConfigurationManager.restartGracefully();

        serverConfigurationManager = null;
    }


    private void uploadResourcesToConfigRegistry() throws Exception {

        ResourceAdminServiceClient resourceAdminServiceStub =
                new ResourceAdminServiceClient(esbServer.getBackEndUrl(), userInfo.getUserName(), userInfo.getPassword());

        resourceAdminServiceStub.deleteResource("/_system/config/spring");
        resourceAdminServiceStub.addCollection("/_system/config/", "spring", "",
                                               "Contains spring bean config files");

        resourceAdminServiceStub.addResource(
                "/_system/config/spring/springbean.xml", "application/xml", "spring bean config files",
                new DataHandler(new URL("file:///" + getClass().getResource(
                        "/artifacts/ESB/mediatorconfig/spring/utils/different_bean_names.xml").getPath())));


    }


    private void clearUploadedResource()
            throws InterruptedException, ResourceAdminServiceExceptionException, RemoteException {

        ResourceAdminServiceClient resourceAdminServiceStub =
                new ResourceAdminServiceClient(esbServer.getBackEndUrl(), userInfo.getUserName(), userInfo.getPassword());

        resourceAdminServiceStub.deleteResource("/_system/config/spring");

    }
}
