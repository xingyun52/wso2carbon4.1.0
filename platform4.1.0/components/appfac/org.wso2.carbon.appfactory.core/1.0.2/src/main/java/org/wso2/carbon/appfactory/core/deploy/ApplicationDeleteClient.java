/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package org.wso2.carbon.appfactory.core.deploy;


import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.application.mgt.stub.ApplicationAdminStub;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.webapp.mgt.stub.WebappAdminStub;

public class ApplicationDeleteClient {

    private static final Log log = LogFactory.getLog(ApplicationDeployer.class);

    private String authCookie;
    private String backendServerURL;

    public ApplicationDeleteClient(String backendServerURL) {
        if (!backendServerURL.endsWith("/")) {
            backendServerURL += "/";
        }
        this.backendServerURL = backendServerURL;
    }

    /**
     * Authenticates the session using specified credentials
     *
     * @param userName The user name
     * @param password The password
     * @param remoteIp the Staging server's hostname/ip
     * @return
     * @throws Exception
     */
    public boolean authenticate(String userName, String password, String remoteIp)
            throws Exception {
        String serviceURL = backendServerURL + "AuthenticationAdmin";

        AuthenticationAdminStub authStub = new AuthenticationAdminStub(serviceURL);
        boolean authenticate;

        authStub._getServiceClient().getOptions().setManageSession(true);
        authenticate = authStub.login(userName, password, remoteIp);
        authCookie =
                (String) authStub._getServiceClient().getServiceContext()
                        .getProperty(HTTPConstants.COOKIE_STRING);
        return authenticate;
    }

    /**
     * Delete the specified artifact
     * @param applicationName
     *              Artifacts to delete
     * @throws Exception
     *              An error
     */
    public void deleteCarbonApp(String applicationName) throws Exception {
        String serviceURL;
        ServiceClient client;
        Options option;
        ApplicationAdminStub applicationAdminStub;

        serviceURL = backendServerURL + "ApplicationAdmin";
        applicationAdminStub = new ApplicationAdminStub(serviceURL);
        client = applicationAdminStub._getServiceClient();
        option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                           authCookie);
        applicationAdminStub.deleteApplication(applicationName + ".CApp");
    }


    /**
     * Delete the given web-app
     * @param applicationName
     * @throws Exception
     */
    public void deleteWebApp(String applicationName,String type) throws Exception {
        String serviceURL;
        ServiceClient client;
        Options option;
        WebappAdminStub webappAdminStub;

        serviceURL = backendServerURL + "WebappAdmin";
        webappAdminStub = new WebappAdminStub(serviceURL);
        client = webappAdminStub._getServiceClient();
        option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                authCookie);

        // Assumption: the web app file name is equal to application Name
        // If it is not available, search for SNAPSHOT file and delete it
        // else, log error
        String appFileName = applicationName +type;
        String snapshotAppFileName = applicationName + "-SNAPSHOT"+type ;
        if(webappAdminStub.getStartedWebapp(appFileName) != null ){
            log.debug("Undeploying the application " + appFileName);
            webappAdminStub.deleteWebapp(appFileName);
        } else if(webappAdminStub.getStartedWebapp(snapshotAppFileName) != null){
            log.debug("Undeploying the application " + snapshotAppFileName);
            webappAdminStub.deleteWebapp(snapshotAppFileName);
        }
        else{
            log.error("Can not find web app with name " + appFileName +
                    " or " + snapshotAppFileName + " to un deploy");
        }

    }


}
