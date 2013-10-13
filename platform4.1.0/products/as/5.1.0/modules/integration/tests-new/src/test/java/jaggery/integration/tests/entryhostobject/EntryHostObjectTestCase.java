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

package jaggery.integration.tests.entryhostobject;

import jaggery.integration.tests.util.Utility;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.integration.test.ASIntegrationTest;

import java.io.BufferedReader;
import java.net.URL;
import java.net.URLConnection;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * This class sends requests to entry.jag and validates the response
 */
public class EntryHostObjectTestCase extends ASIntegrationTest {

    private static final Log log = LogFactory.getLog(EntryHostObjectTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
    }

    @Test(groups = {"wso2.as"}, description = "Test entry host object")
    public void testFeed() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/entry.jag");
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
        assertEquals(response, "String : <feed xmlns=\"http://www.w3.org/2005/Atom\"><entry>" +
                "<id>1</id><title type=\"text\">Jaggery Sample Entry</title><content type=" +
                "\"text\">This is content for a sample atom entry" + "</content>" +
                "<author><name>madhuka</name></author><author><name>nuwan</name></author>" +
                "<category term=\"js\"/><category term=\"jaggery\"/>" +
                "<link href=\"http://jaggeryjs.org/\"/>"
                + "<link href=\"madhukaudantha.blogspot.com\"/>" +
                "<summary type=\"text\">summary test" +
                "</summary><rights type=\"text\">rights list test</rights><contributor><name>" +
                "madhuka</name></contributor><contributor>"
                + "<name>nuwan</name></contributor><contributor><name>ruchira</name></contributor>" +
                "</entry></feed>");
    }

    @Test(groups = {"wso2.as"}, description = "Test Entry host object toXML",
            dependsOnMethods = "testFeed")
    public void testFeedXML() throws Exception {

        String response = null;
        URL jaggeryURL = new URL(asServer.getWebAppURL() + "/testapp/entry.jag?action=xml");
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
        assertEquals(response, "XML : <feed xmlns=\"http://www.w3.org/2005/Atom\">" +
                "<entry><id>1</id><title type=\"text\">Jaggery Sample Entry</title>" +
                "<content type=\"text\">This is content for a sample atom entry"
                + "</content><author><name>madhuka</name></author><author><name>nuwan</name>" +
                "</author><category term=\"js\"/><category term=\"jaggery\"/>" +
                "<link href=\"http://jaggeryjs.org/\"/>"
                + "<link href=\"madhukaudantha.blogspot.com\"/><summary type=\"text\">" +
                "summary test</summary><rights type=\"text\">rights list test</rights>" +
                "<contributor><name>madhuka</name></contributor><contributor>"
                + "<name>nuwan</name></contributor><contributor><name>ruchira</name>" +
                "</contributor></entry></feed>");
    }
}
