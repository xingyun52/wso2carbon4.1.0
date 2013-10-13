/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.esb.nhttp.transport.test;

import org.codehaus.plexus.util.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.esb.ESBIntegrationTest;
import org.wso2.carbon.esb.util.ESBTestConstant;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static java.io.File.separator;

public class ConfiguringNhttpAccessLogLocationTestCase extends ESBIntegrationTest {

    private ServerConfigurationManager serverConfigurationManager;
    private String nhttpLogDir;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.init();
        serverConfigurationManager = new ServerConfigurationManager(esbServer.getBackEndUrl());
        String nhttpFile = ProductConstant.getResourceLocations(ProductConstant.ESB_SERVER_NAME) +
                           separator + "synapseconfig" + separator + "nhttp_transport" + separator
                           + "nhttp.properties";

        File srcFile = new File(nhttpFile);
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        nhttpLogDir = carbonHome + File.separator + "repository" + File.separator + "logs"
                      + File.separator + "nhttpLogs";
        String propertyName = "nhttp.log.directory";

        createNewDir(nhttpLogDir);
        applyProperty(srcFile, propertyName, nhttpLogDir);
        super.init();
        loadESBConfigurationFromClasspath("/artifacts/ESB/synapseconfig/nhttp_transport/nhttp_test_synapse.xml");
    }

    /**
     * To check the functionality of  ESB after enabling Nhttp transport
     * <p/>
     * Test Artifacts: /artifacts/ESB/synapseconfig/nhttp_transport/nhttp.properties
     *
     * @throws Exception
     */
    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @Test(groups = "wso2.esb")
    public void testNhttpAccessLogLocation() throws Exception {

        axis2Client.sendSimpleStockQuoteRequest(getProxyServiceURL("NhttpLogsTestProxy"),
                                                getBackEndServiceUrl(ESBTestConstant.SIMPLE_STOCK_QUOTE_SERVICE), "WSO2");
        Assert.assertTrue(new File(nhttpLogDir).listFiles().length > 0,
                          "nhttp access logs were not written to the configured directory " + nhttpLogDir);
    }

    /*
    create a new dir with the given path, deleting if already exists
     */
    private void createNewDir(String path) throws IOException {
        File dir = new File(path);
        if (dir.exists()) {
            FileUtils.deleteDirectory(path);
        } else {
            dir.mkdir();
        }
    }

    /**
     * Apply the given property and restart the server to
     *
     * @param srcFile
     * @param key
     * @param value
     * @throws Exception
     */
    private void applyProperty(File srcFile, String key, String value) throws Exception {
        File destinationFile = new File("nhttp.properties");
        Properties properties = new Properties();
        properties.load(new FileInputStream(srcFile));
        properties.setProperty(key, value);
        properties.store(new FileOutputStream(destinationFile), null);
        serverConfigurationManager.applyConfiguration(destinationFile);

    }

    /**
     * At the wind-up replace nhttp.properties file with previous
     *
     * @throws Exception
     */
    @AfterClass(alwaysRun = true)
    public void atEnd() throws Exception {
        try {
            super.cleanup();
        } finally {
            Thread.sleep(3000);
            serverConfigurationManager.restoreToLastConfiguration();
            serverConfigurationManager = null;
            nhttpLogDir = null;
        }

    }
}
