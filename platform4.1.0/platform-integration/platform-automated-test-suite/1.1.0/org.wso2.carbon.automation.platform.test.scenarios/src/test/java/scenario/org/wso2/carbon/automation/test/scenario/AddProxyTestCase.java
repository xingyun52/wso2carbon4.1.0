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
package scenario.org.wso2.carbon.automation.test.scenario;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.api.clients.proxy.admin.ProxyServiceAdminClient;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.ManageEnvironment;
import org.wso2.carbon.proxyadmin.stub.ProxyServiceAdminProxyAdminException;
import org.wso2.carbon.proxyadmin.stub.types.carbon.ProxyData;

import java.io.IOException;
import java.rmi.RemoteException;

public class AddProxyTestCase {
    private ManageEnvironment environment;
    private UserInfo userInfo;

    @BeforeClass
    public void init() throws LoginAuthenticationExceptionException, RemoteException {
        int userId = 1;
        userInfo = UserListCsvReader.getUserInfo(userId);
        EnvironmentBuilder builder = new EnvironmentBuilder().esb(userId);
        environment = builder.build();
    }


    @Test(alwaysRun = true)
    public void testAddProxyService() throws IOException, ProxyServiceAdminProxyAdminException,
                                             LoginAuthenticationExceptionException {
        ProxyServiceAdminClient proxyServiceAdminClient = new ProxyServiceAdminClient(environment.getEsb().getBackEndUrl(), environment.getEsb().getSessionCookie());

        ProxyData proxyData = new ProxyData();
        String PROXY_NAME = "chamara_Service";
        proxyData.setName(PROXY_NAME);
        proxyData.setOutSeqXML("<outSequence xmlns=\"http://ws.apache.org/ns/synapse\"><send /></outSequence>");
        proxyData.setEndpointXML("<endpoint xmlns=\"http://ws.apache.org/ns/synapse\">" +
                                 "<address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/></endpoint>");
        proxyData.setWsdlURI("file:repository/samples/resources/proxy/sample_proxy_1.wsdl");
        proxyServiceAdminClient.addProxyService(PROXY_NAME, "file:repository/samples/resources/proxy/sample_proxy_1.wsdl", "http://localhost:9000/services/SimpleStockQuoteService");

    }


}
