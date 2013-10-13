package org.wso2.carbon.appfactory.tenant.roles.S2Integration;


import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.tenant.roles.util.Util;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.hosting.mgt.stub.ApplicationManagementServiceStub;

import java.lang.String;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.appfactory.tenant.roles.util.CommonUtil.getAdminUsername;
import static org.wso2.carbon.appfactory.tenant.roles.util.CommonUtil.getRemoteHost;
import static org.wso2.carbon.appfactory.tenant.roles.util.CommonUtil.getServerAdminPassword;

/**
 * This client is used to subscribe to cartridges for production application deployment.
 */
public class SubscriptionManagerClient {
    private static final Log log = LogFactory.getLog(SubscriptionManagerClient.class);

    private static Map<String, CartridgeInfo> deployerMap = new HashMap<String, CartridgeInfo>();

    public SubscriptionManagerClient() {
        init();
    }

    private void init() {
        if (deployerMap.isEmpty()) {
            AppFactoryConfiguration configuration = Util.getConfiguration();

            Map<String, List<String>> properties = configuration.getAllProperties();
            for (Map.Entry<String, List<String>> property : properties.entrySet()) {
                String key = property.getKey();
                if (key.startsWith("ApplicationDeployment.DeploymentStage.") &&
                        key.contains(".Class.Properties")) {
                    String stage = key.substring(0,key.indexOf(".Class.Properties")).
                            replace("ApplicationDeployment.DeploymentStage.", "");
                    if (deployerMap.containsKey(stage)) {
                        continue;
                    }

                    CartridgeInfo cartridgeInfo = new CartridgeInfo();
                    String minInstances = configuration.getFirstProperty(
                            "ApplicationDeployment.DeploymentStage." + stage + ".Class.Properties.Property.minInstances");
                    if (minInstances != null && !minInstances.equals("")) {
                        cartridgeInfo.setMinInstances(Integer.parseInt(minInstances));
                    }

                    String maxInstances = configuration.getFirstProperty(
                            "ApplicationDeployment.DeploymentStage." + stage + ".Class.Properties.Property.maxInstances");
                    if (maxInstances != null && !maxInstances.equals("")) {
                        cartridgeInfo.setMaxInstances(Integer.parseInt(maxInstances));
                    }

                    String shouldActive = configuration.getFirstProperty(
                            "ApplicationDeployment.DeploymentStage." + stage + ".Class.Properties.Property.shouldActivate");
                    if (shouldActive != null && !shouldActive.equals("")) {
                        cartridgeInfo.setShouldActivate(Boolean.parseBoolean(shouldActive));
                    }

                    String alias = configuration.getFirstProperty(
                            "ApplicationDeployment.DeploymentStage." + stage + ".Class.Properties.Property.alias");
                    if (alias != null) {
                        cartridgeInfo.setAlias(alias);
                    }

                    String cartridgeType = configuration.getFirstProperty(
                            "ApplicationDeployment.DeploymentStage." + stage + ".Class.Properties.Property.cartridgeType");
                    if (cartridgeType != null) {
                        cartridgeInfo.setCartridgeType(cartridgeType);
                    }

                    String repoURL = configuration.getFirstProperty(
                            "ApplicationDeployment.DeploymentStage." + stage + ".Class.Properties.Property.repoURL");
                    if (repoURL != null) {
                        cartridgeInfo.setRepoURL(repoURL);
                    }

                    String dataCartridgeType = configuration.getFirstProperty(
                            "ApplicationDeployment.DeploymentStage." + stage + ".Class.Properties.Property.dataCartridgeType");
                    if (dataCartridgeType != null) {
                        cartridgeInfo.setDataCartridgeType(dataCartridgeType);
                    }

                    String dataCartridgeAlias = configuration.getFirstProperty(
                            "ApplicationDeployment.DeploymentStage." + stage + ".Class.Properties.Property.dataCartridgeAlias");
                    if (dataCartridgeAlias != null) {
                        cartridgeInfo.setDataCartridgeAlias(dataCartridgeAlias);
                    }

                    String endpoint = configuration.getFirstProperty(
                            "ApplicationDeployment.DeploymentStage." + stage + ".Class.Endpoint");
                    cartridgeInfo.setEndpoint(endpoint);

                    deployerMap.put(stage, cartridgeInfo);
                }
            }
        }
    }

    /**
     * This method does 2 things.
     * 1. Subscribe to the given cartridge
     * 2. Persist the git-repo URL in the registry resource
     *
     * @param applicationId The application ID of the newly created application
     * @throws AppFactoryException
     */
    public void subscribe(String applicationId) throws AppFactoryException {
        if (deployerMap.isEmpty()) {
            init();
        }

        for (Map.Entry<String, CartridgeInfo> cartridgeInfoEntry : deployerMap.entrySet()) {
            subscribeToStage(applicationId, cartridgeInfoEntry.getValue());

            if (log.isDebugEnabled()) {
                log.debug("Successfully subscribed in to stage : " + cartridgeInfoEntry.getKey());
            }
        }

    }

    private void subscribeToStage(String applicationId, CartridgeInfo cartridgeInfo) throws AppFactoryException {

        ApplicationManagementServiceStub serviceStub = initializeApplicationManagementServiceStub(applicationId,
                cartridgeInfo.getEndpoint());

        try {
            String gitRepoUrl = serviceStub.subscribe(cartridgeInfo.getMinInstances(), cartridgeInfo.getMaxInstances(),
                    cartridgeInfo.isShouldActivate(), cartridgeInfo.getAlias(), cartridgeInfo.getCartridgeType(),
                    cartridgeInfo.getRepoURL(), cartridgeInfo.getDataCartridgeType(), cartridgeInfo.getDataCartridgeAlias());

            if (gitRepoUrl == null) {
                String msg = "No repository was created";
                log.error(msg);
                throw new AppFactoryException(msg);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Git repo URL : " + gitRepoUrl);
                }
            }
        } catch (RemoteException e) {
            String msg = "Unable to subscribe to the production cartridge";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
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

        try {
            AuthenticationAdminStub authStub = new AuthenticationAdminStub(serviceURL);
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
        }
    }
}
