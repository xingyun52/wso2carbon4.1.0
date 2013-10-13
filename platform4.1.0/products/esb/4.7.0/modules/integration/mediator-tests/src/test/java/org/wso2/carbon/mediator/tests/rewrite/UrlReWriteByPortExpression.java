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
package org.wso2.carbon.mediator.tests.rewrite;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.esb.integration.ESBIntegrationTestCase;
import org.wso2.esb.integration.axis2.SampleAxis2Server;
import org.wso2.esb.integration.axis2.StockQuoteClient;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class UrlReWriteByPortExpression extends ESBIntegrationTestCase {
    private StockQuoteClient axis2Client;

    public void init() throws Exception {
        axis2Client = new StockQuoteClient();
        String filePath = "/mediators/rewrite/url_reWrite_by_port_expression_synapse.xml";
        loadESBConfigurationFromClasspath(filePath);
        launchBackendAxis2Service(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE);

    }

    @Test(priority = 1, groups = {"wso2.esb"}, description = "Conditional URL Rewriting",
          dataProvider = "addressingUrl")
    public void reWritePort(String addUrl) throws AxisFault {
        OMElement response;

        response = axis2Client.sendSimpleStockQuoteRequest(
                getProxyServiceURL("urlRewriteProxy", false),
                addUrl,
                "IBM");
        assertTrue(response.toString().contains("IBM"));

    }


    @Override
    protected void cleanup() {
        super.cleanup();
        axis2Client.destroy();
    }

    @DataProvider(name = "addressingUrl")
    public Object[][] addressingUrl() {
        return new Object[][]{
                {"http://localhost:8000/services/SimpleStockQuoteService"},
                {"https://localhost:8000/services/SimpleStockQuoteService"},
        };

    }

}
