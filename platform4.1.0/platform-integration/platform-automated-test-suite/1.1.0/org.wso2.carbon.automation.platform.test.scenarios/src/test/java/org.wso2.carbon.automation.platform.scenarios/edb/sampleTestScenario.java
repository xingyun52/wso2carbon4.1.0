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

package org.wso2.carbon.automation.platform.scenarios.edb;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.utils.esb.StockQuoteClient;

import java.rmi.RemoteException;

public class sampleTestScenario {
    private static final Log log = LogFactory.getLog(sampleTestScenario.class);
    @BeforeClass(alwaysRun = true)
    public void serviceDeployment() throws Exception {
        System.out.println("-------------------Before Test-------------------");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws RemoteException {
        System.out.println("-------------------After Test-------------------");
    }

    @Test(groups = {"wso2.dss"})
    public void selectOperation() throws AxisFault {
        System.out.println("-------------------test Test-------------------");
        StockQuoteClient axis2Client= new StockQuoteClient();
        OMElement response = axis2Client.sendSimpleStockQuoteRequest("http://192.168.122.1:8282/services/testproxy",
                                                                     null, "WSO2");
        Assert.assertNotNull(response);
        Assert.assertTrue(response.toString().contains("WSO2 Company"));
    }

}
