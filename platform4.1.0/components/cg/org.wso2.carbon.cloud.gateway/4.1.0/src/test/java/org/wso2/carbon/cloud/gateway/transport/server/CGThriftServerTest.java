/*
 * Copyright WSO2, Inc. (http://wso2.com)
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
package org.wso2.carbon.cloud.gateway.transport.server;

import junit.framework.TestCase;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;
import org.wso2.carbon.cloud.gateway.transport.server.CGThriftServer;
import org.wso2.carbon.cloud.gateway.transport.server.CGThriftServerHandler;

import java.net.URL;

public class CGThriftServerTest extends TestCase {

    private CGThriftServer server;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        WorkerPool workerPool = WorkerPoolFactory.getWorkerPool(5, 100, 5, -1, "TestThread",
                "TestThreadID");
        CGThriftServerHandler handler = new CGThriftServerHandler(workerPool);
        server = new CGThriftServer(handler);

    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (server.isServerAlive()) {
            server.stop();
        }
    }

    public void testStart() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url1 = cl.getResource("wso2carbon.jks");
        URL url2 = cl.getResource("client-truststore.jks");
        assertNotNull("KeyStore URL can not be null", url1);
        server.start("localhost", 23003, 80, url1.getPath(), "wso2carbon", url2.getPath(), "wso2carbon",
                "CSG-ThriftServer-test-thread");
    }

    public void testStop() throws Exception {
        server.stop();
    }
}
