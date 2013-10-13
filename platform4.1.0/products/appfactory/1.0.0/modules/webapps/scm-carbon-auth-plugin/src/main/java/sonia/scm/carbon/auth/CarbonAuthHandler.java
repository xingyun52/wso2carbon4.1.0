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
package sonia.scm.carbon.auth;

import com.google.inject.Singleton;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.ServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.CarbonUtils;
import sonia.scm.SCMContextProvider;
import sonia.scm.plugin.ext.Extension;
import sonia.scm.user.User;
import sonia.scm.util.AssertUtil;
import sonia.scm.web.security.AuthenticationHandler;
import sonia.scm.web.security.AuthenticationResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 */
@Singleton
@Extension
public class CarbonAuthHandler implements AuthenticationHandler {
    private static final Logger logger =
            LoggerFactory.getLogger(CarbonAuthHandler.class);


    /**
     * Field description
     */
    public static final String TYPE = "carbon";



    //~--- methods --------------------------------------------------------------

    /**
     * Method description
     *
     * @param request
     * @param response
     * @param username
     * @param password
     * @return
     */
    @Override
    public AuthenticationResult authenticate(HttpServletRequest request,
                                             HttpServletResponse response, String username,
                                             String password) {
        String applicationName = request.getRequestURI().split("/")[3];
        AssertUtil.assertIsNotEmpty(username);
        AssertUtil.assertIsNotEmpty(password);
        AssertUtil.assertIsNotEmpty(applicationName);
        AppFactoryConfiguration configuration = (AppFactoryConfiguration) PrivilegedCarbonContext.
                getCurrentContext().getOSGiService(AppFactoryConfiguration.class);



                if (authenticateRemotely(username, password,applicationName,configuration)) {
                    return new AuthenticationResult(getUser(username),
                                                    getGroups(applicationName));
                }


        return AuthenticationResult.FAILED;
    }

    private boolean authenticateRemotely(String username, String password,String applicationName,AppFactoryConfiguration configuration) {
        try {

            String EPR=configuration.getFirstProperty("ServerUrls.AppFactory") + "RepositoryAuthenticationService";
            //Create a service client
            ServiceClient client = new ServiceClient();

            //Set the endpoint address
            client.getOptions().setTo(new EndpointReference(EPR));
            client.getOptions().setAction("hasAccess");

            CarbonUtils.setBasicAccessSecurityHeaders(username, password, client);

            //Make the request and get the response
            String payload = "   <p:hasAccess xmlns:p=\"http://service.mgt.repository.appfactory.carbon.wso2.org\">\n" +
                             "      <!--0 to 1 occurrence-->\n" +
                             "      <xs:username xmlns:xs=\"http://service.mgt.repository.appfactory.carbon.wso2.org\">"+username+"</xs:username>\n" +
                             "      <!--0 to 1 occurrence-->\n" +
                             "      <xs:applicationId xmlns:xs=\"http://service.mgt.repository.appfactory.carbon.wso2.org\">"+applicationName+"</xs:applicationId>\n" +
                             "   </p:hasAccess>";
           OMElement result= client.sendReceive(new StAXOMBuilder(new ByteArrayInputStream(payload.getBytes())).getDocumentElement());
           Iterator iterator= result.getChildElements();
           if( iterator.hasNext() ){
                 OMElement object= (OMElement) iterator.next();
              if( object.getText().equals("true")){
                  return  true;
              }
           }
        } catch (AxisFault e) {

            e.printStackTrace();
        } catch (XMLStreamException e) {

        }
        return false;
    }

    /**
     * Method description
     *
     * @throws java.io.IOException
     */
    @Override
    public void close() throws IOException {

        // nothing todo
    }


    /**
     * Method description
     *
     * @return
     */
    @Override
    public String getType() {
        return TYPE;
    }


    //~--- methods --------------------------------------------------------------

    private User getUser(String userName) {
        User user = new User();
        user.setName(userName);
        user.setType(CarbonAuthHandler.TYPE);
        user.setDisplayName(userName);
        user.setMail("dummy@example.com"); //we have to just pass an email to get this passed.
        return user;
    }

    private Set<String> getGroups(String projectKey) {
        Set<String> groups = new HashSet<String>();
        groups.add(projectKey);
        return groups;
    }


    @Override
    public void init(SCMContextProvider scmContextProvider) {
        logger.info("initializing  Carbon Auth Handler");
    }
}
