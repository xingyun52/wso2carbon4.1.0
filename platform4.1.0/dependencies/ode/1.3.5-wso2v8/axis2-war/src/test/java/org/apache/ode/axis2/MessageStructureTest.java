/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package org.apache.ode.axis2;

import org.apache.ode.utils.DOMUtils;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

public class MessageStructureTest extends Axis2TestBase {
    @Test(dataProvider="configs")
    public void testAttrWithNsValue() throws Exception {
        String bundleName = "TestAttributeNamespaces";
        // deploy the required service
        server.deployService(DummyService.class.getCanonicalName());
        if (server.isDeployed(bundleName)) server.undeployProcess(bundleName);
        server.deployProcess(bundleName);
        try {
            String response = server.sendRequestFile("http://localhost:8888/processes/attrNSWorld",
                    bundleName, "testRequest.soap");
            Element domResponse = DOMUtils.stringToDOM(response);
            Element out = DOMUtils.getFirstChildElement(DOMUtils.getFirstChildElement(DOMUtils.getFirstChildElement(domResponse)));
            String nsAttr = out.getAttribute("xmlns:myns");
            System.out.println("=> " + response);
            assertTrue(nsAttr != null);
        } finally {
            server.undeployProcess(bundleName);
        }
    }
}
