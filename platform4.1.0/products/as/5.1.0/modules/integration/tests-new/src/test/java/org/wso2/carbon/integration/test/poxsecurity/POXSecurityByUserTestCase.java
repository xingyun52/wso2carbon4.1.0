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

package org.wso2.carbon.integration.test.poxsecurity;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.api.clients.security.SecurityAdminServiceClient;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.utils.httpclient.HttpsResponse;
import org.wso2.carbon.automation.utils.httpclient.HttpsURLConnectionClient;
import org.wso2.carbon.endpoint.stub.types.EndpointAdminEndpointAdminException;
import org.wso2.carbon.integration.test.ASIntegrationTest;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

import static org.testng.Assert.assertTrue;

/*
check all GET, PUT, DELETE and POST HTTPS methods with pox security by admin user.
 */
public class POXSecurityByUserTestCase extends ASIntegrationTest {
    private static String USER_GROUP = "everyone";
    private static final String SERVICE_NAME = "StudentService";
    private static final String studentName = "automationStudent";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        userInfo = UserListCsvReader.getUserInfo(2);
        super.init();
        applySecurity("1", SERVICE_NAME, ProductConstant.DEFAULT_PRODUCT_ROLE);
    }


    @Test(groups = {"wso2.as"}, description = "POST request by valid user")
    public void testAddNewStudent() throws IOException, EndpointAdminEndpointAdminException,
                                           LoginAuthenticationExceptionException,
                                           XMLStreamException {

        String addStudentData = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                                "   <p:addStudent xmlns:p=\"http://axis2.apache.org\">\n" +
                                "      <!--0 to 1 occurrence-->\n" +
                                "      <ns:student xmlns:ns=\"http://axis2.apache.org\">\n" +
                                "         <!--0 to 1 occurrence-->\n" +
                                "         <xs:age xmlns:xs=\"http://axis2.apache.org\">100</xs:age>\n" +
                                "         <!--0 to 1 occurrence-->\n" +
                                "         <xs:name xmlns:xs=\"http://axis2.apache.org\">" + studentName + "</xs:name>\n" +
                                "         <!--0 or more occurrences-->\n" +
                                "         <xs:subjects xmlns:xs=\"http://axis2.apache.org\">testAutomation</xs:subjects>\n" +
                                "      </ns:student>\n" +
                                "   </p:addStudent>";


        String securedRestURL = getSecuredServiceEndpoint(SERVICE_NAME) + "/students";
        HttpsResponse response = HttpsURLConnectionClient.postWithBasicAuth(securedRestURL, addStudentData,
                                                                            "application/xml", userInfo.getUserName(),
                                                                            userInfo.getPassword());
        assertTrue(response.getData().contains(studentName)
                , "response doesn't contain the expected output");

        //check whether the student is added.
        String studentGetUri = getSecuredServiceEndpoint(SERVICE_NAME) + "/student/" + studentName;
        HttpsResponse getResponse =
                HttpsURLConnectionClient.getWithBasicAuth(studentGetUri, null, userInfo.getPassword(),
                                                          userInfo.getPassword());
        assertTrue(getResponse.getData().contains("<ns:getStudentResponse xmlns:ns=\"http://axis2.apache.org\"><ns:return>" +
                                                  "<ns:age>100</ns:age>" +
                                                  "<ns:name>" + studentName + "</ns:name>" +
                                                  "<ns:subjects>testAutomation</ns:subjects>" +
                                                  "</ns:return></ns:getStudentResponse>"));

    }

    @Test(groups = {"wso2.as"}, description = "PUT request by valid user", dependsOnMethods = "testAddNewStudent")
    public void testUpdateStudent() throws IOException, EndpointAdminEndpointAdminException,
                                           LoginAuthenticationExceptionException,
                                           XMLStreamException {

        String updateStudentData = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n" +
                                   "<p:updateStudent xmlns:p=\"http://axis2.apache.org\">\n" +
                                   "      <!--0 to 1 occurrence-->\n" +
                                   "      <ns:student xmlns:ns=\"http://axis2.apache.org\">\n" +
                                   "         <!--0 to 1 occurrence-->\n" +
                                   "         <xs:age xmlns:xs=\"http://axis2.apache.org\">999</xs:age>\n" +
                                   "         <!--0 to 1 occurrence-->\n" +
                                   "         <xs:name xmlns:xs=\"http://axis2.apache.org\">" + studentName + "</xs:name>\n" +
                                   "         <!--0 or more occurrences-->\n" +
                                   "         <xs:subjects xmlns:xs=\"http://axis2.apache.org\">testAutomationUpdated</xs:subjects>\n" +
                                   "      </ns:student>\n" +
                                   "</p:updateStudent>";

        String securedRestURL = getSecuredServiceEndpoint(SERVICE_NAME) + "/student/" + studentName;
        HttpsResponse response = HttpsURLConnectionClient.putWithBasicAuth(securedRestURL, updateStudentData,
                                                                           "application/xml", userInfo.getUserName(),
                                                                           userInfo.getPassword());
        assertTrue(response.getData().contains(studentName)
                , "response doesn't contain the expected output");

        //check whether the student is added.
        String studentGetUri = getSecuredServiceEndpoint(SERVICE_NAME) + "/student/" + studentName;
        HttpsResponse getResponse =
                HttpsURLConnectionClient.getWithBasicAuth(studentGetUri, null, userInfo.getPassword(),
                                                          userInfo.getPassword());
        assertTrue(getResponse.getData().contains("<ns:getStudentResponse xmlns:ns=\"http://axis2.apache.org\"><ns:return>" +
                                                  "<ns:age>999</ns:age>" +
                                                  "<ns:name>" + studentName + "</ns:name>" +
                                                  "<ns:subjects>testAutomationUpdated</ns:subjects>" +
                                                  "</ns:return></ns:getStudentResponse>"));
    }

    @Test(groups = {"wso2.as"}, description = "DELETE request by valid user", dependsOnMethods = "testUpdateStudent")
    public void testDeleteStudent() throws IOException, EndpointAdminEndpointAdminException,
                                           LoginAuthenticationExceptionException,
                                           XMLStreamException {


        String deleteStudentData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                   "<p:deleteStudent xmlns:p=\"http://axis2.apache.org\">\n" +
                                   "      <!--0 to 1 occurrence-->\n" +
                                   "      <xs:name xmlns:xs=\"http://axis2.apache.org\">" + studentName + "</xs:name>\n" +
                                   "</p:deleteStudent>";

        String securedRestURL = getSecuredServiceEndpoint(SERVICE_NAME) + "/student/" + studentName;
        HttpsResponse response =
                HttpsURLConnectionClient.deleteWithBasicAuth(securedRestURL, null, userInfo.getUserName(),
                                                             userInfo.getPassword());
        assertTrue(!response.getData().contains(studentName)
                , "response doesn't contain the expected output");
    }

    @Test(groups = {"wso2.as"}, description = "GET resource after delete by valid user",
          dependsOnMethods = "testDeleteStudent", expectedExceptions = IOException.class)
    public void testGetResourceAfterDelete()
            throws IOException, EndpointAdminEndpointAdminException,
                   LoginAuthenticationExceptionException,
                   XMLStreamException {

        //check whether the student is deleted
        String studentGetUri = getSecuredServiceEndpoint(SERVICE_NAME) + "/student/" + studentName;
        HttpsResponse getResponse =
                HttpsURLConnectionClient.getWithBasicAuth(studentGetUri, null, userInfo.getPassword(),
                                                          userInfo.getPassword());
        assertTrue(getResponse.getData().equals(""), "student was not deleted");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        securityAdminServiceClient.disableSecurity(SERVICE_NAME);
        super.cleanup();
    }
}
