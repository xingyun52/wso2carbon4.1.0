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
package org.wso2.carbon.esb.mediator.test.enrich;

import org.apache.axiom.om.OMElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.api.clients.registry.ResourceAdminServiceClient;
import org.wso2.carbon.esb.ESBIntegrationTest;
import org.wso2.carbon.esb.util.ESBTestConstant;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class EnrichMediatorFollowedByEnrichIntegrationTestCase extends ESBIntegrationTest {
    private ResourceAdminServiceClient resourceAdminServiceStub;

    @BeforeClass(alwaysRun = true)
    public void uploadSynapseConfig() throws Exception {
        super.init();
        resourceAdminServiceStub = new ResourceAdminServiceClient(esbServer.getBackEndUrl(),
                                                                  userInfo.getUserName(),
                                                                  userInfo.getPassword());
        uploadResourcesToGovernanceRegistry();
        loadESBConfigurationFromClasspath("/artifacts/ESB/mediatorconfig/enrich/enrich_by_enrich.xml");
    }

    @Test(groups = {"wso2.esb"}, description = "Enrich mediator followed by enrich mediator")
    public void enrichMediatorFollowedByEnrichMediator() throws IOException,
                                                                XMLStreamException {
        OMElement response = axis2Client.sendCustomQuoteRequest(getProxyServiceURL(
                "enrichSample1"), getBackEndServiceUrl(ESBTestConstant.SIMPLE_STOCK_QUOTE_SERVICE), "IBM");
        assertNotNull(response, "Response message is null");
        assertEquals(response.getLocalName(), "CheckPriceResponse", "CheckPriceResponse not match");
        assertTrue(response.toString().contains("Price"), "No price tag in response");
        assertTrue(response.toString().contains("Code"), "No code tag in response");
        assertEquals(response.getFirstChildWithName
                (new QName("http://services.samples/xsd", "Code")).getText(), "MSFT", "Symbol not changed");


    }

    private void uploadResourcesToGovernanceRegistry() throws Exception {


        resourceAdminServiceStub.deleteResource("/_system/governance/xslt");
        resourceAdminServiceStub.addCollection("/_system/governance/", "xslt", "",
                                               "Contains test XSLT files");
        resourceAdminServiceStub.addResource(
                "/_system/governance/xslt/transform_back.xslt", "application/xml", "xslt files",
                new DataHandler(new URL("file:///" + getClass().getResource(
                        "/artifacts/ESB/mediatorconfig/xslt/transform_back.xslt").getPath())));

    }

    @AfterClass(alwaysRun = true)
    private void destroy() throws Exception {

        resourceAdminServiceStub.deleteResource("/_system/governance/xslt");
        cleanup();
    }
}
