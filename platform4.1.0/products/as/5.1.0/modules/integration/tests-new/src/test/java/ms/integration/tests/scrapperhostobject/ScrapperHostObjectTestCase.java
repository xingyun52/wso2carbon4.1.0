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
package ms.integration.tests.scrapperhostobject;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.utils.axis2client.AxisServiceClient;
import org.wso2.carbon.integration.test.ASIntegrationTest;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;

import static org.testng.Assert.*;

/**
 * This class invokes the scrapperTest service and evaluates the response
 */
public class ScrapperHostObjectTestCase extends ASIntegrationTest {
    private static final Log log = LogFactory.getLog(ScrapperHostObjectTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
    }

    @AfterClass(alwaysRun = true)
    public void serviceDelete() throws Exception {
        deleteService("admin/scrapperTest");   // deleting scrapperTest from the services list
        log.info("scrapperTest service deleted");
    }

    @Test(groups = {"wso2.as"}, description = "Test Scrapper Host Object")
    public void testScrap() throws RemoteException, XMLStreamException {
        boolean serDeployedStatus = isServiceDeployed("admin/scrapperTest");
        assertTrue(serDeployedStatus, "scrapperTest Service deployment failure ");
        AxisServiceClient axisServiceClient = new AxisServiceClient();
        OMElement response = axisServiceClient.sendReceive(createPayload(),
                asServer.getServiceUrl() + "/admin/scrapperTest", "testScrap");
        log.info("Response :" + response);
        assertNotNull(response, "Result cannot be null");
        assertEquals(response.toString().trim(),
                "<ws:testScrapResponse xmlns:ws=\"http://services.mashup.wso2.org/scrapperTest"
                        + "?xsd\"><return xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:js=" +
                        "\"htt" + "p://www.wso2.org/ns/jstype\" xmlns:xsi=\"" +
                        "http://www.w3.org/2001/XMLSchema-i" + "nstance\" js:type=\"string\" " +
                        "xsi:type=\"xs:string\">Response is not null or" + " empty</return>" +
                        "</ws:testScrapResponse>"
        );
    }

    private OMElement createPayload() throws XMLStreamException {  // creation of request
        String request = "<p:testScrap xmlns:p=\"http://www.wso2.org/types\">" +
                "<name>maninda</name></p:testScrap>";
        return new StAXOMBuilder(new ByteArrayInputStream(request.getBytes())).getDocumentElement();
    }
}
