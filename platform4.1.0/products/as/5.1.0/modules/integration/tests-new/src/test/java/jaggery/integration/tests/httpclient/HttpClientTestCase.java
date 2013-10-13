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
package jaggery.integration.tests.httpclient;

import jaggery.integration.tests.util.Utility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.integration.test.ASIntegrationTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * This class sends requests to get.jag and validates the response
 */
public class HttpClientTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(HttpClientTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
    }

    @Test(groups = {"wso2.as"}, description = "Test Http Client GET object")
    public void testHttpClientGet() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/get.jag");
        URLConnection jaggeryServerConnection = Utility.openConnection(jaggeryURL);
        assertNotNull(jaggeryServerConnection, "Connection establishment failure");

        BufferedReader in = Utility.inputReader(jaggeryServerConnection);
        assertNotNull(in, "Input stream failure");

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response = inputLine;
        }

        in.close();
        log.info("Response: " + response);
        assertNotNull(response, "Result cannot be null");
        assertEquals(response, "{\"data\" : [{\"type\" : \"GET\", \"name\" : " +
                "\"Test\"}], \"xhr\" : {}}");
    }

    @Test(groups = {"wso2.as"}, description = "Test Http Client GET operation ",
            dependsOnMethods = "testHttpClientGet")
    public void testHttpClientGetParameters() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/get.jag?action=parameters");
        URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                jaggeryServerConnection.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response = inputLine;
        }

        in.close();
        log.info("Response: " + response);
        assertEquals(response, "{\"data\" : {\"type\" : \"GET\", \"name\" :" +
                " \"Test parameters\"}, \"xhr\" : {}}");
    }

    @Test(groups = {"wso2.as"}, description = "Test Http Client POST object",
            dependsOnMethods = "testHttpClientGetParameters")
    public void testHttpClientPost() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/post.jag");
        URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                jaggeryServerConnection.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response = inputLine;
        }

        in.close();
        log.info("Response: " + response);
        assertNotNull(response, "Result cannot be null");
        assertEquals(response, "{\"data\" : [{\"type\" : \"POST\", \"name\" : \"Test\"}]," +
                " \"xhr\" : {}}");
    }

    @Test(groups = {"wso2.as"}, description = "Test Http Client POST operation with params",
            dependsOnMethods = "testHttpClientPost")
    public void testHttpClientPostParameters() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/post.jag?action=parameters");
        URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                jaggeryServerConnection.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response = inputLine;
        }

        in.close();
        log.info("Response: " + response);
        assertEquals(response, "{\"data\" : {\"type\" : \"POST\", \"name\" : " +
                "\"Test parameters\"}, \"xhr\" : {}}");
    }

    @Test(groups = {"wso2.as"}, description = "Test Http Client PUT object",
            dependsOnMethods = "testHttpClientPostParameters")
    public void testHttpClientPut() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/put.jag");
        URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                jaggeryServerConnection.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response = inputLine;
        }

        in.close();
        log.info("Response: " + response);
        assertNotNull(response, "Result cannot be null");
        assertEquals(response, "{\"data\" : \"[{\\\"type\\\" : \\\"PUT\\\""
                + ", \\\"name\\\" : \\\"Test\\\"}]\", \"xhr\" : {}}");
    }

    @Test(groups = {"wso2.as"}, description = "Test Http Client PUT operation with params",
            dependsOnMethods = "testHttpClientPut")
    public void testHttpClientPutParameters() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/put.jag?action=parameters");
        URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                jaggeryServerConnection.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response = inputLine;
        }

        in.close();
        log.info("Response: " + response);
        assertEquals(response, "{\"data\" : {\"type\" : \"PUT\", \"name\" : " +
                "\"Test parameters\"}, \"xhr\" : {}}");
    }

    @Test(groups = {"wso2.as"}, description = "Test Http Client DEL object",
            dependsOnMethods = "testHttpClientPutParameters")
    public void testHttpClientDel() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/delet.jag");
        URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                jaggeryServerConnection.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response = inputLine;
        }

        in.close();
        log.info("Response: " + response);
        assertNotNull(response, "Result cannot be null");
        assertEquals(response, "{\"data\" : \"[{\\\"type\\\" : \\\"DELETE\\\""
                + ", \\\"name\\\" : \\\"Test\\\"}]\", \"xhr\" : {}}");
    }

    @Test(groups = {"wso2.as"}, description = "Test Http Client DEL operation with params",
            dependsOnMethods = "testHttpClientDel")
    public void testHttpClientDelParameters() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/delet.jag?action=parameters");
        URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                jaggeryServerConnection.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response = inputLine;
        }

        in.close();
        log.info("Response: " + response);
        assertEquals(response, "{\"data\" : {\"type\" : \"DELETE\", \"name\" : " +
                "\"Test parameters\"}, \"xhr\" : {}}");
    }
}
