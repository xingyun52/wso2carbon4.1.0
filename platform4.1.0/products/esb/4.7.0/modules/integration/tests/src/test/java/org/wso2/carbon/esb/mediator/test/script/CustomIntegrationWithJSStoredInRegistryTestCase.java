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
package org.wso2.carbon.esb.mediator.test.script;

import org.apache.axiom.om.OMElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.api.clients.logging.LoggingAdminClient;
import org.wso2.carbon.automation.api.clients.registry.ResourceAdminServiceClient;
import org.wso2.carbon.esb.ESBIntegrationTest;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import java.net.URL;
import java.rmi.RemoteException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


public class CustomIntegrationWithJSStoredInRegistryTestCase extends ESBIntegrationTest {


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        enableDebugLogging();
        uploadResourcesToConfigRegistry();
        loadESBConfigurationFromClasspath("/artifacts/ESB/synapseconfig/config46/synapse.xml");
    }


    @Test(groups = "wso2.esb", description = "custom mediator with JS and store it in registry and invoke it with the given 'key'")
    public void testJSMediatorWithTheGivenKey() throws Exception {

        OMElement response = axis2Client.sendCustomQuoteRequest(getMainSequenceURL(), null, "WSO2");

        assertNotNull(response, "Fault response message null");

        assertNotNull(response.getQName().getLocalPart(), "Fault response null localpart");
        assertEquals(response.getQName().getLocalPart(), "CheckPriceResponse", "Fault localpart mismatched");

        assertNotNull(response.getFirstElement().getQName().getLocalPart(), " Fault response null localpart");
        assertEquals(response.getFirstElement().getQName().getLocalPart(), "Code", "Fault localpart mismatched");
        assertEquals(response.getFirstElement().getText(), "WSO2", "Fault value mismatched");


        assertNotNull(response.getFirstChildWithName(new QName("http://services.samples/xsd", "Price")), "Fault response null localpart");


    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        clearUploadedResource();
        super.cleanup();
    }


    private void uploadResourcesToConfigRegistry() throws Exception {

        ResourceAdminServiceClient resourceAdminServiceStub =
                new ResourceAdminServiceClient(esbServer.getBackEndUrl(), userInfo.getUserName(), userInfo.getPassword());

        resourceAdminServiceStub.deleteResource("/_system/config/script_js");
        resourceAdminServiceStub.addCollection("/_system/config/", "script_js", "",
                                               "Contains test js files");

        resourceAdminServiceStub.addResource(
                "/_system/config/script_js/stockquoteTransform.js", "application/x-javascript", "js files",
                new DataHandler(new URL("file:///" + getClass().getResource(
                        "/artifacts/ESB/mediatorconfig/script_js/stockquoteTransform.js").getPath())));


    }

    private void enableDebugLogging() throws Exception {
        LoggingAdminClient logAdminClient = new LoggingAdminClient(esbServer.getBackEndUrl(), esbServer.getSessionCookie());
        logAdminClient.updateLoggerData("org.apache.synapse", "DEBUG", true, false);
    }

    private void clearUploadedResource()
            throws InterruptedException, ResourceAdminServiceExceptionException, RemoteException {

        ResourceAdminServiceClient resourceAdminServiceStub =
                new ResourceAdminServiceClient(esbServer.getBackEndUrl(), userInfo.getUserName(), userInfo.getPassword());

        resourceAdminServiceStub.deleteResource("/_system/config/script_js");
        Thread.sleep(1000);
    }
}
