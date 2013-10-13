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
package org.wso2.carbon.esb.mediator.test.rule;

import org.apache.axis2.AxisFault;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.esb.ESBIntegrationTest;
import org.wso2.carbon.esb.util.ESBTestConstant;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class WithOutRuleSetPropertyTestCase extends ESBIntegrationTest {


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();


    }

    @Test(groups = "wso2.esb",
          description = "scenario without rules")
    public void testSequenceWithOutRuleSet() throws Exception {
        try {
            loadESBConfigurationFromClasspath("/artifacts/ESB/synapseconfig/config_without_rule/synapse.xml");
            Assert.fail("This Configuration can not be saved successfully due to empty rule set");
        } catch (AxisFault expected) {
            assertTrue((expected.getMessage().contains(ESBTestConstant.ERROR_ADDING_SEQUENCE) || expected.getMessage().contains(ESBTestConstant.UNABLE_TO_SAVE_SEQUENCE))
                    , "Error Message Mismatched. actual:" + expected.getMessage() + " but expected: Error adding sequence or Unable to save the Sequence");
        }

    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
    }
}
