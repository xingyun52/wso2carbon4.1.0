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

package org.wso2.carbon.appfactory.tenant.roles.S2Integration;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.hosting.mgt.stub.*;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import static org.wso2.carbon.appfactory.tenant.roles.util.CommonUtil.*;

public class SubscribeExecutor implements Runnable {
    private static final Log log = LogFactory.getLog(SubscribeExecutor.class);

    private String applicationId;
    private DeployerInfo deployerInfo;
    private String stage;


    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public void setDeployerInfo(DeployerInfo deployerInfo) {
        this.deployerInfo = deployerInfo;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("Thread started for application id : " + applicationId + " for cartridge type : "
                    + deployerInfo.getCartridgeType());
        }
        String repoUrl = null;
//        This is where we create a git repo
        try {
            RepositoryProvider repoProvider = (RepositoryProvider) deployerInfo.getRepoProvider().newInstance();
            repoProvider.setBaseUrl(deployerInfo.getBaseURL());
            repoProvider.setAdminUsername(deployerInfo.getAdminUserName());
            repoProvider.setAdminPassword(deployerInfo.getAdminPassword());
            repoProvider.setRepoName(generateRepoUrlFromTemplate(deployerInfo.getRepoPattern(), applicationId, stage));

            repoUrl = repoProvider.createRepository();
        } catch (InstantiationException e) {
            String msg = "Unable to create repository";
            log.error(msg, e);
        } catch (IllegalAccessException e) {
            String msg = "Unable to create repository";
            log.error(msg, e);
        } catch (AppFactoryException e) {
            String msg = "Unable to create repository";
            log.error(msg, e);
        }


//        This is where we add the subscription. For that we take the above created repo
        ApplicationManagementServiceStub serviceStub = null;
        try {

            if (deployerInfo.getEndpoint() != null) {
                serviceStub = initializeApplicationManagementServiceStub(applicationId,
                        deployerInfo.getEndpoint());
//            serviceStub._getServiceClient().fireAndForget(getSubscribeOMPayload(deployerInfo));
                serviceStub.subscribe(deployerInfo.getMinInstances(), deployerInfo.getMaxInstances(),
                        deployerInfo.isShouldActivate(), deployerInfo.getAlias() + applicationId,
                        deployerInfo.getCartridgeType(), repoUrl,deployerInfo.getAdminUserName(),
                        deployerInfo.getAdminPassword(), deployerInfo.getDataCartridgeType(),
                        deployerInfo.getDataCartridgeAlias());
            }

        } catch (ApplicationManagementServiceADCExceptionException e){
            String msg = "Unable to subscribe to the cartridge : " + deployerInfo.getCartridgeType() +
                    " for application : " + applicationId;
            log.error(msg, e);
        } catch (RemoteException e) {
            String msg = "Unable to subscribe to the cartridge : " + deployerInfo.getCartridgeType() +
                    " for application : " + applicationId;
            log.error(msg, e);
        } catch (AppFactoryException e) {
            String msg = "Unable to get service stub : " + deployerInfo.getCartridgeType() +
                    " for application : " + applicationId;
            log.error(msg, e);
        } finally {
            try {
                if (serviceStub != null) {
                    serviceStub.cleanup();
                }
            } catch (AxisFault axisFault) {
                log.debug("Unable to cleanup service stub : " + deployerInfo.getCartridgeType() +
                        " for application : " + applicationId, axisFault);
            }
        }

    }

    /**
     * We are initializing this every time because for different applications we need to login as different tenants
     * This is because the ADC service creates a git-repo for the logged in tenant.
     *
     * @param applicationId The application ID of the newly created application
     * @return an instance of the applicationManagementServiceStub which is authenticated as the tenant of application ID
     * @throws AppFactoryException
     */
    private ApplicationManagementServiceStub initializeApplicationManagementServiceStub(String applicationId,
                                                                                        String backEndEpr)
            throws AppFactoryException {
        ApplicationManagementServiceStub serviceStub;
        String endpoint = backEndEpr + "ApplicationManagementService";
        try {
            String authCookie = authenticate(applicationId, backEndEpr);

            serviceStub = new ApplicationManagementServiceStub(endpoint);
            ServiceClient client = serviceStub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setTimeOutInMilliSeconds(1000000);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                    authCookie);

        } catch (AxisFault axisFault) {
            String msg = "Unable to initialize application management service stub ";
            log.error(msg, axisFault);
            throw new AppFactoryException(msg, axisFault);
        }

        return serviceStub;
    }

    /**
     * The authenticate method which will login to the ADC as a tenant.
     *
     * @param applicationId The application ID of the newly created application.
     *                      this is used to create the tenant name.
     *                      the name is created as 'admin@admin.com@applicationId'
     * @return the cookie string
     * @throws AppFactoryException
     */
    private String authenticate(String applicationId, String backEndEpr) throws AppFactoryException {
        String serviceURL = backEndEpr + "AuthenticationAdmin";
        String userName = getAdminUsername(applicationId);
        String password = getServerAdminPassword();
        boolean authenticate;

        AuthenticationAdminStub authStub = null;
        try {
            authStub = new AuthenticationAdminStub(serviceURL);
            authStub._getServiceClient().getOptions().setManageSession(true);
            authenticate = authStub.login(userName, password, getRemoteHost(serviceURL));
            if (authenticate) {
                return (String) authStub._getServiceClient().getServiceContext()
                        .getProperty(HTTPConstants.COOKIE_STRING);
            }
            return null;
        } catch (RemoteException e) {
            String msg = "Invalid remote address given";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        } catch (LoginAuthenticationExceptionException e) {
            String msg = "Unable to login";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        } catch (MalformedURLException e) {
            String msg = "The given URL is incorrect";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        } finally {
            if (authStub != null) {
                try {
                    authStub.cleanup();
                } catch (AxisFault axisFault) {
                    log.debug("Unable to clean up service stub", axisFault);
                }
            }
        }
    }

    private String generateRepoUrlFromTemplate(String pattern, String applicationId, String stage) {
        return pattern.replace("{@application_key}", applicationId).replace("{@stage}", stage);

    }
}
