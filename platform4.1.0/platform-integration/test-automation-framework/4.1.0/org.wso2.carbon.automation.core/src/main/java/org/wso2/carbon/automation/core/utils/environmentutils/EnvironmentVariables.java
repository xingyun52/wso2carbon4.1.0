package org.wso2.carbon.automation.core.utils.environmentutils;

import org.wso2.carbon.automation.api.clients.authenticators.AuthenticatorClient;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.frameworkutils.productvariables.ProductVariables;
import org.wso2.carbon.automation.core.utils.frameworkutils.productvariables.WorkerVariables;

public class EnvironmentVariables {


    private String sessionCookie;
    private String workerSessionCookie;
    private String backEndUrl;
    private String serviceUrl;
    private String secureServiceUrl;
    private UserInfo userDetails;
    private AuthenticatorClient adminServiceAuthentication;
    private ProductVariables productVariables;
    private WorkerVariables workerVariables;
    private String webAppURL;

    public String getSessionCookie() {
        return sessionCookie;
    }

    public String getBackEndUrl() {
        return backEndUrl;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public String getSecureServiceUrl() {
        return secureServiceUrl;
    }

    public String getWebAppURL() {
        return webAppURL;
    }

    public AuthenticatorClient getAdminServiceAuthentication() {
        return adminServiceAuthentication;
    }

    public ProductVariables getProductVariables() {
        return productVariables;
    }

    public WorkerVariables getWorkerVariables() {
        return workerVariables;
    }

    public void setEnvironment
            (String _cookie, String _backendUrl, String _serviceUrl, String _secureServiceUrl,
             UserInfo user,
             AuthenticatorClient authentication) {
        this.sessionCookie = _cookie;
        this.backEndUrl = _backendUrl;
        this.serviceUrl = _serviceUrl;
        this.secureServiceUrl = _secureServiceUrl;
        this.userDetails = user;
        this.adminServiceAuthentication = authentication;
    }

    public void setEnvironment(String _cookie, String _backendUrl, String _serviceUrl,String _secureServiceUrl, UserInfo user,
                               AuthenticatorClient authentication,
                               ProductVariables productVariables) {
        this.sessionCookie = _cookie;
        this.backEndUrl = _backendUrl;
        this.serviceUrl = _serviceUrl;
        this.secureServiceUrl = _secureServiceUrl;
        this.userDetails = user;
        this.adminServiceAuthentication = authentication;
        this.productVariables = productVariables;
    }

    public void setEnvironment
            (String _cookie, String _workerCookie, String _backendUrl, String _serviceUrl,
             String _secureServiceUrl, UserInfo user,
             AuthenticatorClient authentication, ProductVariables productVariables,
             WorkerVariables workerVariables) {
        this.sessionCookie = _cookie;
        this.workerSessionCookie = _workerCookie;
        this.backEndUrl = _backendUrl;
        this.serviceUrl = _serviceUrl;
        this.secureServiceUrl = _secureServiceUrl;
        this.userDetails = user;
        this.adminServiceAuthentication = authentication;
        this.productVariables = productVariables;
        this.workerVariables = workerVariables;
    }

    public void setEnvironment
            (String _cookie, String _backendUrl, String _serviceUrl,String _secureServiceUrl, String webAppURL,
             UserInfo user,
             AuthenticatorClient authentication, ProductVariables productVariables) {
        this.sessionCookie = _cookie;
        this.backEndUrl = _backendUrl;
        this.serviceUrl = _serviceUrl;
        this.secureServiceUrl = _secureServiceUrl;
        this.userDetails = user;
        this.webAppURL = webAppURL;
        this.adminServiceAuthentication = authentication;
        this.productVariables = productVariables;
    }

    public void setEnvironment
            (String _cookie, String _workerSessionCookie, String _backendUrl, String _serviceUrl,
             String _secureServiceUrl,String webAppURL,
             UserInfo user,
             AuthenticatorClient authentication, ProductVariables productVariables,
             WorkerVariables workerVariables) {
        this.sessionCookie = _cookie;
        this.workerSessionCookie = _workerSessionCookie;
        this.backEndUrl = _backendUrl;
        this.serviceUrl = _serviceUrl;
        this.secureServiceUrl = _secureServiceUrl;
        this.userDetails = user;
        this.webAppURL = webAppURL;
        this.adminServiceAuthentication = authentication;
        this.productVariables = productVariables;
        this.workerVariables = workerVariables;
    }

    public void setEnvironment
            (String _backendUrl, String _serviceUrl,String _secureServiceUrl, UserInfo user,
             ProductVariables productVariables) {
        this.backEndUrl = _backendUrl;
        this.serviceUrl = _serviceUrl;
        this.secureServiceUrl = _secureServiceUrl;
        this.userDetails = user;
        this.productVariables = productVariables;
    }
}
