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

import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Tests that timeouts set in the *.endpoint files are applied.
 * The test is designed so a fault must be received.
 *
 * Actually, the process invokes a 3-sec long operation (see the process request).
 * The specified timeouts are lesser than 3-sec, so if properly applied, a fault should be trown.
 * If not applied, the default 120-sec timeouts will be used. 5sec < 120sec, so the request will succeed.
 *
 */
public class SelectorsTest extends Axis2TestBase implements ODEConfigDirAware {
    @Test(dataProvider="configs")
    public void testNoP2P() throws Exception {
        String bundleName = "TestSelectors";
        if (server.isDeployed(bundleName)) server.undeployProcess(bundleName);
        server.deployProcess(bundleName);
        String response = server.sendRequestFile("http://localhost:8888/ode/processes/Project-Reproduce-Isolation-Problem/ReproduceIsolationProblem/Pool2/Pool",
                bundleName, "testRequest.soap");
        System.out.println(response);
        assertTrue(response.contains("_21Response"));
        if (server.isDeployed(bundleName)) server.undeployProcess(bundleName);
    }

    public String getODEConfigDir() {
        return HIB_DERBY_CONF_DIR;
    }
}