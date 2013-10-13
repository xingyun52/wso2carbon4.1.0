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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.esb.mediator.test.aggregate;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.esb.ESBIntegrationTest;

import java.io.IOException;

public class NestedAggregatesTestCase extends ESBIntegrationTest {

    private AggregatedRequestClient aggregatedRequestClient;
    private final int no_of_requests = 4;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        loadESBConfigurationFromClasspath("/artifacts/ESB/synapseconfig/config13/synapse.xml");
        aggregatedRequestClient = new AggregatedRequestClient();
        aggregatedRequestClient.setProxyServiceUrl(getMainSequenceURL());
        aggregatedRequestClient.setSymbol("IBM");
        aggregatedRequestClient.setNo_of_iterations(no_of_requests);


    }

    @Test(groups = {"wso2.esb"}, description = "replacing a property by using an enrich mediator")
    public void test() throws IOException {
        int companyCount = 0, responseCount = 0, SoapEnvCount = 0;

        String Response = aggregatedRequestClient.getResponse();
        String[] response = getTagArray(Response);

        for (int i = 0; i < response.length; i++) {
            if (response[i].contains("soapenv")) {
                SoapEnvCount++;
            } else if (response[i].contains("IBM Company")) {
                companyCount++;
            } else if (response[i].contains("getQuoteResponse")) {
                responseCount++;
            }
        }


        Assert.assertEquals(no_of_requests, SoapEnvCount);
        Assert.assertEquals(2 * no_of_requests, responseCount);
        Assert.assertEquals(no_of_requests, companyCount);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        aggregatedRequestClient = null;
        super.cleanup();
    }

    public String[] getTagArray(String xml) {
        return xml.split("<");
    }


}
