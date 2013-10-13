/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.dataservices.core.test.sql;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.dataservices.core.test.DataServiceBaseTestCase;
import org.wso2.carbon.dataservices.core.test.util.TestUtils;

public abstract class AbstractUDTTest extends DataServiceBaseTestCase {

    private String epr;

    public AbstractUDTTest(String testName, String serviceName) {
        super(testName);
        this.epr = baseEpr + serviceName;
    }

    protected void selectUDTsFromTable () {
        TestUtils.showMessage(this.epr + " - selectUDTsFromTable");
        try {
			OMElement result = TestUtils.callOperation(this.epr,
					"select_udt_from_table", null);
			assertTrue(TestUtils.validateResultStructure(result, TestUtils.CUSTOMER_XSD_PATH));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
    }

    protected void selectUDTAsOutParameterOfStoredProc() {
        TestUtils.showMessage(this.epr + " - selectUDTAsOutParameterOfStoredProc");
        try {
			OMElement result = TestUtils.callOperation(this.epr,
					"select_udt_as_out_param", null);
			assertTrue(TestUtils.validateResultStructure(result, TestUtils.CUSTOMER_XSD_PATH));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
    }

}
