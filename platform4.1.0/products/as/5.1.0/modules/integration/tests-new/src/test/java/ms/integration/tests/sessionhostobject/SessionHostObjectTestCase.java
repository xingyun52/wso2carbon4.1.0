/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package ms.integration.tests.sessionhostobject;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
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
 * This class invokes the sessionTest service and evaluates the response
 */
public class SessionHostObjectTestCase extends ASIntegrationTest {
    private static final Log log = LogFactory.getLog(SessionHostObjectTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
    }

    @AfterClass(alwaysRun = true)
    public void serviceDelete() throws Exception {
        deleteService("admin/sessionTest");   // deleting sessionTest from the services list
        log.info("sessionTest service deleted");
    }

    @Test(groups = {"wso2.as"}, description = "Test putting a sample value into the Session Host" +
            " Object")
    public void testPutValue() throws RemoteException, XMLStreamException {
        boolean serDeployedStatus = isServiceDeployed("admin/sessionTest");
        assertTrue(serDeployedStatus, "sessionTest Service deployment failure ");
        AxisServiceClient axisServiceClient = new AxisServiceClient();
        OMElement response = axisServiceClient.sendReceive(createPayloadOne(),
                asServer.getServiceUrl() + "/admin/sessionTest", "putValue");
        log.info("Response :" + response);
        assertNotNull(response, "Result cannot be null");
        assertEquals(response.toString().trim(),
                "<ws:putValueResponse xmlns:ws=\"http://services.mashup.wso2.org/sessionTest"
                        + "?xsd\"><return xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:js"
                        + "=\"http://www.wso2.org/ns/jstype\" xmlns:xsi=\"http://www.w3.org/2001/XMLS"
                        + "chema-instance\" js:type=\"string\" xsi:type=\"xs:string\">number</return"
                        + "></ws:putValueResponse>");
    }

    @Test(groups = {"wso2.as"}, dependsOnMethods = "testPutValue",
            description = "Test getting a sample value from the Session Host Object")
    public void testGetValue() throws AxisFault, XMLStreamException {
        AxisServiceClient axisServiceClient = new AxisServiceClient();
        OMElement response = axisServiceClient.sendReceive(createPayloadTwo(),
                asServer.getServiceUrl() + "/admin/sessionTest", "getValue");
        log.info("Response :" + response);
        assertNotNull(response, "Result cannot be null");
        assertEquals(response.toString().trim(),
                "<ws:getValueResponse xmlns:ws=\"http://services.mashup.wso2.org/sessionTest"
                        + "?xsd\"><return xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:js=\"ht"
                        + "tp://www.wso2.org/ns/jstype\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchem"
                        + "a-instance\" js:type=\"number\" xsi:type=\"xs:double\">2</return></ws:getV"
                        + "alueResponse>");
    }

    @Test(groups = {"wso2.as"}, dependsOnMethods = "testGetValue",
            description = "Test removing a sample value from the Session Host Object")
    public void testRemoveValue() throws AxisFault, XMLStreamException {
        AxisServiceClient axisServiceClient = new AxisServiceClient();
        OMElement response = axisServiceClient.sendReceive(createPayloadThree(),
                asServer.getServiceUrl() + "/admin/sessionTest", "removeValue");
        log.info("Response :" + response);
        assertNotNull(response, "Result cannot be null");
        assertEquals(response.toString().trim(),
                "<ws:removeValueResponse xmlns:ws=\"http://services.mashup.wso2.org/session"
                        + "Test?xsd\"><return xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:js"
                        + "=\"http://www.wso2.org/ns/jstype\" xmlns:xsi=\"http://www.w3.org/2001/XML"
                        + "Schema-instance\" js:type=\"xml\" xsi:type=\"xs:anyType\"><success /></ret"
                        + "urn></ws:removeValueResponse>");
    }

    @Test(groups = {"wso2.as"}, dependsOnMethods = "testRemoveValue",
            description = "Test clearing the Session Host Object")
    public void testClearSession() throws AxisFault, XMLStreamException {
        AxisServiceClient axisServiceClient = new AxisServiceClient();
        OMElement response = axisServiceClient.sendReceive(createPayloadFour(),
                asServer.getServiceUrl() + "/admin/sessionTest", "clearSession");
        log.info("Response :" + response);
        assertNotNull(response, "Result cannot be null");
        assertEquals(response.toString().trim(),
                "<ws:clearSessionResponse xmlns:ws=\"http://services.mashup.wso2.org/session"
                        + "Test?xsd\"><return xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:js"
                        + "=\"http://www.wso2.org/ns/jstype\" xmlns:xsi=\"http://www.w3.org/2001/XML"
                        + "Schema-instance\" js:type=\"xml\" xsi:type=\"xs:anyType\"><success /></re"
                        + "turn></ws:clearSessionResponse>");
    }

    // // creation of requests
    private OMElement createPayloadOne() throws XMLStreamException {
        String request = "<p:putValue xmlns:p=\"http://www.wso2.org/types\">" +
                "<name>wso2</name></p:putValue>";
        return new StAXOMBuilder(new ByteArrayInputStream(request.getBytes())).getDocumentElement();
    }

    private OMElement createPayloadTwo() throws XMLStreamException {
        String request = "<p:getValue xmlns:p=\"http://www.wso2.org/types\">" +
                "<name>wso2</name></p:getValue>";
        return new StAXOMBuilder(new ByteArrayInputStream(request.getBytes())).getDocumentElement();
    }

    private OMElement createPayloadThree() throws XMLStreamException {
        String request = "<p:removeValue xmlns:p=\"http://www.wso2.org/types\">" +
                "<name>wso2</name></p:removeValue>";
        return new StAXOMBuilder(new ByteArrayInputStream(request.getBytes())).getDocumentElement();
    }

    private OMElement createPayloadFour() throws XMLStreamException {
        String request = "<p:clearSession xmlns:p=\"http://www.wso2.org/types\">" +
                "<name>wso2</name></p:clearSession>";
        return new StAXOMBuilder(new ByteArrayInputStream(request.getBytes())).getDocumentElement();
    }

}
