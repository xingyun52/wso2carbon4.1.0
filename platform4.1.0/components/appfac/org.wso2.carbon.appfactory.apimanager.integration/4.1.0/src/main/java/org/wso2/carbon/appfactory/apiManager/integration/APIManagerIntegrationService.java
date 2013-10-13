package org.wso2.carbon.appfactory.apiManager.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.appfactory.apiManager.integration.internal.ServiceHolder;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.APIIntegration;
import org.wso2.carbon.appfactory.core.dto.API;
import org.wso2.carbon.appfactory.core.dto.APIMetadata;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.carbon.appfactory.apiManager.integration.utils.Constants.*;
import static org.wso2.carbon.appfactory.apiManager.integration.utils.Utils.*;

public class APIManagerIntegrationService extends AbstractAdmin implements APIIntegration {

    private static final Log log = LogFactory.getLog(APIManagerIntegrationService.class);

    private HttpClient httpClient = new DefaultHttpClient();

    public void loginToStore() throws AppFactoryException {
        login(STORE_LOGIN_ENDPOINT);
    }

    public void loginToPublisher() throws AppFactoryException {
        login(PUBLISHER_LOGIN_ENDPOINT);
    }

    private void login(String endpoint) throws AppFactoryException {
        HttpServletRequest request = (HttpServletRequest) MessageContext.getCurrentMessageContext().
                getProperty(HTTPConstants.MC_HTTP_SERVLETREQUEST);
        String samlToken = request.getHeader(SAML_TOKEN);

//        We expect an encoded saml token.
        if (samlToken == null || samlToken.equals("")) {
            String msg = "Unable to get the SAML token";
            log.error(msg);
            throw new AppFactoryException(msg);
        }

//        Now we decode the token
        samlToken = decode(samlToken);

        URL apiManagerUrl = getApiManagerURL();

        List<NameValuePair> parameters = new ArrayList<NameValuePair>();

        parameters.add(new BasicNameValuePair(ACTION, "loginWithSAMLToken"));
        parameters.add(new BasicNameValuePair("samlToken", samlToken));

        HttpPost postMethod = createHttpPostRequest(apiManagerUrl, parameters, endpoint);
        HttpResponse response = executeHttpMethod(httpClient, postMethod);

        try {
            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            String msg = "Failed to consume http response";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }
    }


    public boolean createApplication(String applicationId) throws AppFactoryException {

        loginToStore();

        if (!isApplicationNameInUse(applicationId)) {
            URL apiManagerUrl = getApiManagerURL();

            List<NameValuePair> parameters = new ArrayList<NameValuePair>();

            parameters.add(new BasicNameValuePair(ACTION, "addApplication"));
            parameters.add(new BasicNameValuePair("application", applicationId));
            parameters.add(new BasicNameValuePair("tier", getDefaultTier()));

            HttpPost postMethod = createHttpPostRequest(apiManagerUrl, parameters, CREATE_APPLICATION_ENDPOINT);
            HttpResponse response = executeHttpMethod(httpClient, postMethod);

            if (response != null) {

            }
        }
        return true;
    }


    public boolean isApplicationNameInUse(String applicationId) throws AppFactoryException {
        loginToStore();

        URL apiManagerUrl = getApiManagerURL();

        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair(ACTION, "getApplications"));
        parameters.add(new BasicNameValuePair(USERNAME, CarbonContext.getCurrentContext().getUsername()));

        HttpPost postMethod = createHttpPostRequest(apiManagerUrl, parameters, LIST_APPLICATION_ENDPOINT);

        HttpResponse httpResponse = executeHttpMethod(httpClient, postMethod);
        if (httpResponse != null) {
            try {

                HttpEntity responseEntity = httpResponse.getEntity();
                String responseBody = EntityUtils.toString(responseEntity);

                JsonObject response = getJsonObject(responseBody);
                JsonArray applications = response.getAsJsonArray("applications");

                for (JsonElement application : applications) {
                    String applicationName = ((JsonObject) application).get(NAME).getAsString();
                    if (applicationName.equals(applicationId)) {
                        return true;
                    }
                }
            } catch (IOException e) {
                String msg = "Error reading the json response";
                log.error(msg, e);
                throw new AppFactoryException(msg, e);
            }
        }
        return false;
    }

    public boolean removeApplication(String applicationID) throws AppFactoryException {
//        returning false since we do not support this for the moment.
        return false;
    }

    public boolean addAPIsToApplication(String s, String s1, String s2, String s3) throws AppFactoryException {
//        returning false since we do not support this for the moment.
        return false;
    }

    public API[] getAPIsOfApplication(String applicationId) throws AppFactoryException {
        loginToStore();

        URL apiManagerUrl = getApiManagerURL();

        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair(ACTION, "getAllSubscriptions"));
        parameters.add(new BasicNameValuePair(USERNAME, CarbonContext.getCurrentContext().getUsername()));

        HttpPost postMethod = createHttpPostRequest(apiManagerUrl, parameters, LIST_SUBSCRIPTIONS_ENDPOINT);
        HttpResponse httpResponse = executeHttpMethod(httpClient, postMethod);
        if (httpResponse != null) {
//            Reading the response json
            List<API> apiNames = new ArrayList<API>();
            try {
                HttpEntity responseEntity = httpResponse.getEntity();
                String responseBody = EntityUtils.toString(responseEntity);

                JsonObject response = getJsonObject(responseBody);
                JsonArray subscriptions = response.getAsJsonArray(SUBSCRIPTIONS);

                for (JsonElement subscription : subscriptions) {
                    String applicationName = ((JsonObject) subscription).get(NAME).getAsString();
                    if (applicationName.equals(applicationId)) {
                        JsonArray applicationSubscriptions = ((JsonObject) subscription).getAsJsonArray(SUBSCRIPTIONS);
                        for (JsonElement applicationSubscription : applicationSubscriptions) {
                            API apiInfo = populateAPIInfo((JsonObject) applicationSubscription);

                            apiNames.add(apiInfo);
                        }
                        break;
                    }
                }

            } catch (IOException e) {
                String msg = "Error reading the json response";
                log.error(msg, e);
                throw new AppFactoryException(msg, e);
            }
            return apiNames.toArray(new API[apiNames.size()]);
        }
        return new API[0];
    }


    public API getAPIInformation(String apiName, String apiVersion, String apiProvider) throws AppFactoryException {
        loginToPublisher();

        URL apiManagerUrl = getApiManagerURL();

        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair(ACTION, "getAPI"));
        parameters.add(new BasicNameValuePair(NAME, apiName));
        parameters.add(new BasicNameValuePair(VERSION, apiVersion));
        parameters.add(new BasicNameValuePair(PROVIDER, apiProvider));

        HttpPost postMethod = createHttpPostRequest(apiManagerUrl, parameters, PUBLISHER_API_INFO_ENDPOINT);

        HttpResponse httpResponse = executeHttpMethod(httpClient, postMethod);
        if (httpResponse != null) {
            try {

                HttpEntity responseEntity = httpResponse.getEntity();
                String responseBody = EntityUtils.toString(responseEntity);

                JsonObject response = getJsonObject(responseBody);
                JsonObject apiElement = response.getAsJsonObject("api");

                return populateAPIInfo(apiElement);
            } catch (IOException e) {
                String msg = "Error reading the json response";
                log.error(msg, e);
                throw new AppFactoryException(msg, e);
            }
        }
        return new API();
    }

    public void generateKeys(String appId, String apiName, String apiVersion, String apiProvider)
            throws AppFactoryException {
        loginToStore();

        URL apiManagerUrl = getApiManagerURL();

        generateKey(appId, apiManagerUrl, "SANDBOX", httpClient);
        generateKey(appId, apiManagerUrl, "PRODUCTION", httpClient);
    }


    public boolean removeAPIFromApplication(String s, String s1, String s2, String s3) throws AppFactoryException {
//        returning false since we do not support this for the moment.
        return false;
    }

    public APIMetadata[] createDependencies(String applicationId) throws AppFactoryException{
        Registry tenantRegistry;

        RegistryService registryService = ServiceHolder.getInstance().getRegistryService();
        RealmService realmService = ServiceHolder.getInstance().getRealmService();
        TenantRegistryLoader tenantRegistryLoader = ServiceHolder.getInstance().getTenantRegistryLoader();

        if(registryService == null){
            String msg = "Unable to find the registry service";
            log.error(msg);
            throw new AppFactoryException(msg);
        }
        if(realmService == null){
            String msg = "Unable to find the realm service";
            log.error(msg);
            throw new AppFactoryException(msg);
        }
        if(tenantRegistryLoader == null){
            String msg = "Unable to find the tenant registry loader service";
            log.error(msg);
            throw new AppFactoryException(msg);
        }

        try {
            int tenantId = realmService.getTenantManager().getTenantId(applicationId);
            if(tenantId == MultitenantConstants.INVALID_TENANT_ID){
                String msg = "Invalid tenant Id returned";
                log.error(msg);
                throw new AppFactoryException(msg);
            }

            tenantRegistryLoader.loadTenantRegistry(tenantId);
            tenantRegistry = registryService.getGovernanceSystemRegistry(tenantId);
        } catch (UserStoreException e) {
            String msg = "Unable to get the tenant id ";
            log.error(msg,e);
            throw new AppFactoryException(msg,e);
        } catch (RegistryException e) {
            String msg = "Unable to get the registry";
            log.error(msg,e);
            throw new AppFactoryException(msg,e);
        }

        API[] api = getAPIsOfApplication(applicationId);
        List<APIMetadata> dependencyList = new ArrayList<APIMetadata>();

        if(api != null){
//            Because API Manager has keys per application, not for api
            API singleApi = api[0];
            APIMetadata[] keys = singleApi.getKeys();

            if(keys != null){
                for (APIMetadata key : keys) {
                    if(key.getType().startsWith(SANDBOX)){
                        String name = key.getType().replace(SANDBOX, "");
                        String environment = DEVELOPMENT + "/" + TESTING;

                        registerSandboxKeys(tenantRegistry,name, key.getValue());
                        dependencyList.add(new APIMetadata(key.getType(),key.getValue(),name, environment));
                    }else{
                        String name = key.getType().replace(PROD, "");
                        registerProdKeys(tenantRegistry,name, key.getValue());
                        dependencyList.add(new APIMetadata(key.getType(), key.getValue(), name,PRODUCTION));
                    }
                }
            }
        }
        return dependencyList.toArray(new APIMetadata[dependencyList.size()]);
    }
}
