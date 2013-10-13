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
package org.wso2.carbon.esb.nhttp.transport.test;


import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.PostMethod;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.esb.ESBIntegrationTest;
import org.wso2.carbon.esb.util.WireMonitorServer;

import java.io.IOException;

/**
 * To ensure that the body of the message is not get dropped when,
 * Content-Type of the message is not mentioned
 */

public class MessageWithoutContentTypeTestCase extends ESBIntegrationTest {

    private WireMonitorServer wireMonitorServer;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.init();

    }

    /**
     * Sending a message without mentioning Content Type and check the body part at the listening port
     * <p/>
     * Public JIRA:    WSO2 Carbon/CARBON-6029
     * Responses With No Content-Type Header not handled properly
     * <p/>
     * Test Artifacts: ESB Sample 0
     *
     * @throws Exception
     */
    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @Test(groups = "wso2.esb")
    public void testMessageWithoutContentType() throws Exception {

        loadSampleESBConfiguration(0);

        wireMonitorServer = new WireMonitorServer(9090);

        wireMonitorServer.start();

        Thread.sleep(1000);


        /**
         * Creating a new HttpClient to send SOAP message without Content-Type header
         */
        HttpClient httpclient = new HttpClient();

        httpclient.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_1);
        httpclient.getParams().setParameter("http.socket.timeout", new Integer(1000));

        PostMethod httpPost = new PostMethod(getMainSequenceURL());
        httpPost.getParams().setParameter("http.socket.timeout", new Integer(5000));

        String soapRequest = "<?xml version='1.0' encoding='UTF-8'?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                             + "<soapenv:Header xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">"
                             + "<wsa:To>http://localhost:9090/</wsa:To>"
                             + "<wsa:MessageID>urn:uuid:7d8a6eab-b490-450f-ab84-2783a04a6f80</wsa:MessageID>"
                             + "<wsa:Action>urn:getQuote</wsa:Action>"
                             + "</soapenv:Header>"
                             + "<soapenv:Body>"
                             + "<m0:getQuote xmlns:m0=\"http://services.samples\">"
                             + "<m0:request><m0:symbol>WSO2</m0:symbol></m0:request>"
                             + "</m0:getQuote>"
                             + "</soapenv:Body>"
                             + "</soapenv:Envelope>";


        httpPost.setRequestBody(soapRequest);

        try {

            httpclient.executeMethod(httpPost);


        } catch (IOException ioe) {

        } finally {
            httpPost.releaseConnection();
            httpclient = null;
            httpPost = null;
        }


        // Waits until the wire message is read
        String reply = wireMonitorServer.getCapturedMessage();


        /**
         * Assert for the Body element and for its contents
         */
        Assert.assertTrue(reply.contains("Body"), "Body element is missing in the message at back end !!!");
        Assert.assertTrue(reply.contains("WSO2"), "WSO2 symbol is missing in the message at back end !!!");


    }


    @AfterClass(alwaysRun = true)
    public void close() throws Exception {
        super.cleanup();
        wireMonitorServer = null;

    }


}
