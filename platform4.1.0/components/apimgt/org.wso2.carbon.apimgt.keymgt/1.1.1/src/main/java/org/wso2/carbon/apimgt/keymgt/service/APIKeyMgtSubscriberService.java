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

package org.wso2.carbon.apimgt.keymgt.service;

import org.apache.axis2.AxisFault;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.api.model.SubscribedAPI;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.handlers.security.stub.types.APIKeyMapping;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.APIInfoDTO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIAuthenticationAdminClient;
import org.wso2.carbon.apimgt.keymgt.APIKeyMgtException;
import org.wso2.carbon.apimgt.keymgt.ApplicationKeysDTO;
import org.wso2.carbon.apimgt.keymgt.util.APIKeyMgtUtil;
import org.wso2.carbon.caching.core.CacheKey;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.cache.OAuthCacheKey;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This service class exposes the functionality required by the application developers who will be
 * consuming the APIs published in the API Store.
 */
public class APIKeyMgtSubscriberService extends AbstractAdmin {

    /**
     * Get the access token for a user per given API. Users/developers can use this access token
     * to consume the API by directly passing it as a bearer token as per the OAuth 2.0 specification.
     * @param userId User/Developer name
     * @param apiInfoDTO Information about the API to which the Access token will be issued.
     *                   Provider name, API name and the version should be provided to uniquely identify
     *                   an API.
     * @param tokenType Type (scope) of the required access token
     * @return  Access Token
     * @throws APIKeyMgtException Error when getting the AccessToken from the underlying token store.
     */
    public String getAccessToken(String userId, APIInfoDTO apiInfoDTO,
                                 String applicationName, String tokenType, String callbackUrl) throws APIKeyMgtException,
            APIManagementException, IdentityException {
        ApiMgtDAO apiMgtDAO = new ApiMgtDAO();
        String accessToken = apiMgtDAO.getAccessKeyForAPI(userId, applicationName, apiInfoDTO, tokenType);
        if (accessToken == null){
            //get the tenant id for the corresponding domain
            String tenantAwareUserId = userId;
            int tenantId = IdentityUtil.getTenantIdOFUser(userId);

            String[] credentials = apiMgtDAO.addOAuthConsumer(tenantAwareUserId, tenantId, applicationName, callbackUrl);

            accessToken = apiMgtDAO.registerAccessToken(credentials[0],applicationName,
                    tenantAwareUserId, tenantId, apiInfoDTO, tokenType);
        }
        return accessToken;
    }

    /**
     * Get the access token for the specified application. This token can be used as an OAuth
     * 2.0 bearer token to access any API in the given application.
     *
     * @param userId User/Developer name
     * @param applicationName Name of the application
     * @param tokenType Type (scope) of the required access token
     * @return Access token
     * @throws APIKeyMgtException on error
     */
    public ApplicationKeysDTO getApplicationAccessToken(String userId, String applicationName, String tokenType,
    		String callbackUrl, String[] allowedDomains)
            throws APIKeyMgtException, APIManagementException, IdentityException {

        ApiMgtDAO apiMgtDAO = new ApiMgtDAO();
        String[] credentials = null;
        String accessToken = apiMgtDAO.getAccessKeyForApplication(userId, applicationName, tokenType);
        if (accessToken == null) {
            //get the tenant id for the corresponding domain
            String tenantAwareUserId = userId;
            int tenantId = IdentityUtil.getTenantIdOFUser(userId);
            credentials = apiMgtDAO.addOAuthConsumer(tenantAwareUserId, tenantId, applicationName, callbackUrl);
            accessToken = apiMgtDAO.registerApplicationAccessToken(credentials[0], applicationName,
                    tenantAwareUserId, tenantId, tokenType, allowedDomains);

        } else if (credentials == null) {
            credentials = apiMgtDAO.getOAuthCredentials(accessToken, tokenType);
            if (credentials == null || credentials[0] == null || credentials[1] == null) {
                throw new APIKeyMgtException("Unable to locate OAuth credentials");
            }
        }

        ApplicationKeysDTO keys = new ApplicationKeysDTO();
        keys.setApplicationAccessToken(accessToken);
        keys.setConsumerKey(credentials[0]);
        keys.setConsumerSecret(credentials[1]);
        return keys;
    }

    /**
     * Get the list of subscribed APIs of a user
     * @param userId User/Developer name
     * @return An array of APIInfoDTO instances, each instance containing information of provider name,
     * api name and version.
     * @throws APIKeyMgtException Error when getting the list of APIs from the persistence store.
     */
    public APIInfoDTO[] getSubscribedAPIsOfUser(String userId) throws APIKeyMgtException,
            APIManagementException, IdentityException {
        ApiMgtDAO ApiMgtDAO = new ApiMgtDAO();
        return ApiMgtDAO.getSubscribedAPIsOfUser(userId);
    }

    public String renewAccessToken(String tokenType, String oldAccessToken, String[] allowedDomains)
            throws Exception {
        ApiMgtDAO apiMgtDAO = new ApiMgtDAO();
        return apiMgtDAO.refreshAccessToken(tokenType, oldAccessToken, allowedDomains);

    }

    public void unsubscribeFromAPI(String userId, APIInfoDTO apiInfoDTO) {

    }

    /**
     * Revoke Access tokens by Access token string.This will change access token status to revoked and
     * remove cached access tokens from memory
     *
     * @param key Access Token String to be revoked
     * @throws APIManagementException on error in revoking
     * @throws AxisFault              on error in clearing cached key
     */
    public void revokeAccessToken(String key,String consumerKey,String authorizedUser) throws APIManagementException, AxisFault {
        ApiMgtDAO dao=new ApiMgtDAO();
        dao.revokeAccessToken(key);
        clearOAuthCache(consumerKey,authorizedUser);
    }

    /**
     * Revoke All access tokens associated with an application.This will change access tokens status to revoked and
     * remove cached access tokens from memory
     *
     * @param application Application object associated with keys to be removed
     * @throws APIManagementException on error in revoking
     * @throws AxisFault              on error in revoking cached keys
     */
    public void revokeAccessTokenForApplication(Application application) throws APIManagementException, AxisFault {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        boolean gatewayExists = config.getFirstProperty(APIConstants.API_GATEWAY_SERVER_URL) != null;
        Set<SubscribedAPI> apiSet = null;
        Set<String> keys = null;
        ApiMgtDAO dao;
        dao = new ApiMgtDAO();
        if (gatewayExists) {
            keys = dao.getApplicationKeys(application.getId());
            apiSet = dao.getSubscribedAPIs(application.getSubscriber());
        }
        List<APIKeyMapping> mappings = new ArrayList<APIKeyMapping>();
        for (String key : keys) {
            dao.revokeAccessToken(key);
            for (SubscribedAPI api : apiSet) {
                APIKeyMapping mapping = new APIKeyMapping();
                API apiDefinition = APIKeyMgtUtil.getAPI(api.getApiId());
                mapping.setApiVersion(api.getApiId().getVersion());
                mapping.setContext(apiDefinition.getContext());
                mapping.setKey(key);
                mappings.add(mapping);
            }
        }
        if (mappings.size() > 0) {
            APIAuthenticationAdminClient client = new APIAuthenticationAdminClient();
            client.invalidateKeys(mappings);

        }
    }


    /**
     * Revoke all access tokens associated by subscriber user.This will change access token status to revoked and
     * remove cached access tokens from memory
     *
     * @param subscriber Subscriber associated with the keys to be removed
     * @throws APIManagementException on error in revoking keys
     * @throws AxisFault              on error in clearing cached keys
     */
    public void revokeAccessTokenBySubscriber(Subscriber subscriber) throws
            APIManagementException, AxisFault {
        ApiMgtDAO dao;
        dao = new ApiMgtDAO();
        Application[] applications = dao.getApplications(subscriber);
        for (Application app : applications) {
            revokeAccessTokenForApplication(app);
        }
    }

    /**
     * Revoke all access tokens associated with the given tier.This will change access token status to revoked and
     * remove cached access tokens from memory
     *
     * @param tierName Tier associated with keys to be removed
     * @throws APIManagementException on error in revoking keys
     * @throws AxisFault              on error in clearing cached keys
     */
    public void revokeKeysByTier(String tierName) throws APIManagementException, AxisFault {
        ApiMgtDAO dao;
        dao = new ApiMgtDAO();
        Application[] applications = dao.getApplicationsByTier(tierName);
        for (Application application : applications) {
            revokeAccessTokenForApplication(application);
        }
    }

    public void clearOAuthCache(String consumerKey, String authorizedUser) {
        OAuthCache oauthCache;
        CacheKey cacheKey = new OAuthCacheKey(consumerKey + ":" + authorizedUser);
        if (OAuthServerConfiguration.getInstance().isCacheEnabled()) {
            oauthCache = OAuthCache.getInstance();
            oauthCache.clearCacheEntry(cacheKey);
        }
    }
}
