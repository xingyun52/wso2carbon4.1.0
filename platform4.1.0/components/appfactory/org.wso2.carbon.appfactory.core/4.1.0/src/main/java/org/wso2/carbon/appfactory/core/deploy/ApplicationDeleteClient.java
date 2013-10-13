package org.wso2.carbon.appfactory.core.deploy;


import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.wso2.carbon.application.mgt.stub.ApplicationAdminStub;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;

public class ApplicationDeleteClient {

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


}
