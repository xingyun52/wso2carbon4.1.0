/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.gateway.handlers.security;

import net.sf.jsr107cache.Cache;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.gateway.handlers.security.keys.APIKeyDataStore;
import org.wso2.carbon.apimgt.gateway.handlers.security.keys.WSAPIKeyDataStore;
import org.wso2.carbon.apimgt.gateway.handlers.security.thrift.ThriftAPIDataStore;
import org.wso2.carbon.apimgt.gateway.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.apache.synapse.rest.RESTUtils;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.URITemplate;
import org.wso2.carbon.apimgt.impl.dto.ResourceInfoDTO;
import org.wso2.carbon.apimgt.impl.dto.VerbInfoDTO;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.APIInfoDTO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to validate a given API key against a given API context and a version.
 * Actual validation operations are carried out by invoking back-end authentication and
 * key validation services. In order to minimize the network overhead, this implementation
 * caches some API key authentication information in memory. This implementation and the
 * underlying caching implementation are thread-safe. An instance of this class must not be
 * shared among multiple APIs, API handlers or authenticators.
 */
public class APIKeyValidator {

    private Cache infoCache;
    private Cache resourceCache;
    private APIKeyDataStore dataStore;
    private AxisConfiguration axisConfig;
    private boolean isGatewayAPIKeyValidationEnabled = true;
    protected Log log = LogFactory.getLog(getClass());

    public APIKeyValidator(AxisConfiguration axisConfig) {
        this.axisConfig = axisConfig;
        //check the client type from config
        String keyValidatorClientType = APISecurityUtils.getKeyValidatorClientType();
        if(APIConstants.API_KEY_VALIDATOR_WS_CLIENT.equals(keyValidatorClientType)){
            this.dataStore = new WSAPIKeyDataStore();
        } else if (APIConstants.API_KEY_VALIDATOR_THRIFT_CLIENT.equals(keyValidatorClientType)){
            this.dataStore = new ThriftAPIDataStore();
        }
        this.infoCache = initCache();
        this.resourceCache = initResourceCache();
        this.isGatewayAPIKeyValidationEnabled=isAPIKeyValidationEnabled();
    }

    protected Cache initCache() {
        return PrivilegedCarbonContext.getCurrentContext(axisConfig).getCache("keyCache");
    }

    protected Cache initResourceCache(){
        return PrivilegedCarbonContext.getCurrentContext(axisConfig).getCache("resourceCache");
    }

    /**
     * Get the API key validated against the specified API
     *
     * @param context    API context
     * @param apiKey     API key to be validated
     * @param apiVersion API version number
     * @return An APIKeyValidationInfoDTO object
     * @throws APISecurityException If an error occurs while accessing backend services
     */
    public APIKeyValidationInfoDTO getKeyValidationInfo(String context, String apiKey,
                                                        String apiVersion,String authenticationScheme) throws APISecurityException {
        String cacheKey = apiKey + ":" + context + ":" + apiVersion;
        if (isGatewayAPIKeyValidationEnabled) {
            APIKeyValidationInfoDTO info = (APIKeyValidationInfoDTO) infoCache.get(cacheKey);
            if (info != null) {
                return info;
            }
        }


        //synchronized (apiKey.intern()) {
            // We synchronize on the API key here to allow concurrent processing
            // of different API keys - However when a burst of requests with the
            // same key is encountered, only one will be allowed to execute the logic,
            // and the rest will pick the value from the cache.
            //   info = (APIKeyValidationInfoDTO) infoCache.get(cacheKey);
            // if (info != null) {
            //   return info;
            //}
            APIKeyValidationInfoDTO info = doGetKeyValidationInfo(context, apiVersion, apiKey,authenticationScheme);
            if (info != null) {
                if (isGatewayAPIKeyValidationEnabled) {
                    infoCache.put(cacheKey, info);
                }
                return info;
            } else {
                throw new APISecurityException(APISecurityConstants.API_AUTH_GENERAL_ERROR,
                        "API key validator returned null");
            }
        //}
    }

    protected APIKeyValidationInfoDTO doGetKeyValidationInfo(String context, String apiVersion,
                                                             String apiKey,String authenticationScheme) throws APISecurityException {

        return dataStore.getAPIKeyData(context, apiVersion, apiKey, authenticationScheme);
    }

    public void cleanup() {
        dataStore.cleanup();
    }

    public boolean isAPIKeyValidationEnabled() {
        try {
            APIManagerConfiguration config = ServiceReferenceHolder.getInstance().getAPIManagerConfiguration();
            String serviceURL = config.getFirstProperty(APIConstants.API_GATEWAY_KEY_CACHE_ENABLED);
            return Boolean.parseBoolean(serviceURL);
        } catch (Exception e) {
            log.error("Did not found valid API Validation Information cache configuration. Use default configuration" + e);
        }
        return true;
    }

    public String getResourceAuthenticationScheme(String context, String apiVersion,
                                                  String requestPath, String httpMethod){

        String cacheKey = context + ":" + apiVersion;
        APIInfoDTO apiInfoDTO = (APIInfoDTO)resourceCache.get(cacheKey);

        if(apiInfoDTO == null){
            apiInfoDTO = doGetAPIInfo(context, apiVersion);
            resourceCache.put(cacheKey, apiInfoDTO);
        }

        //Match the case where the direct api context is matched
        if("/".equals(requestPath)){
            String requestCacheKey = context + "/" + apiVersion + requestPath + ":" + httpMethod;

            //Get decision from cache.
            VerbInfoDTO matchingVerb = (VerbInfoDTO)resourceCache.get(requestCacheKey);
            //On a cache hit
            if(matchingVerb != null){
                return matchingVerb.getAuthType();
            }
            else{
                for(ResourceInfoDTO resourceInfoDTO : apiInfoDTO.getResources()){
                    String urlPattern = resourceInfoDTO.getUrlPattern();

                    //If the request patch is '/', it can only be matched with a resource whose url-context is '/*'
                    if("/*".equals(urlPattern)){
                        for(VerbInfoDTO verbDTO : resourceInfoDTO.getHttpVerbs()){
                            if(verbDTO.getHttpVerb().equals(httpMethod)){
                                //Store verb in cache
                                resourceCache.put(requestCacheKey, verbDTO);
                                return verbDTO.getAuthType();
                            }
                        }
                    }
                }
            }
        }

        //Remove the ending '/' from request
        requestPath = RESTUtils.trimTrailingSlashes(requestPath);

            while(requestPath.length() > 1){

            String requestCacheKey = context + "/" + apiVersion + requestPath + ":" + httpMethod;

            //Get decision from cache.
            VerbInfoDTO matchingVerb = (VerbInfoDTO)resourceCache.get(requestCacheKey);

            //On a cache hit
            if(matchingVerb != null){
                return matchingVerb.getAuthType();
            }
            //On a cache miss
            else{
                for(ResourceInfoDTO resourceInfoDTO : apiInfoDTO.getResources()){
                    String urlPattern = resourceInfoDTO.getUrlPattern();
                    if(urlPattern.endsWith("/*")){
                        //Remove the ending '/*'
                        urlPattern = urlPattern.substring(0, urlPattern.length() - 2);
                    }
                    //If the urlPattern ends with a '/', remove that as well.
                    urlPattern = RESTUtils.trimTrailingSlashes(urlPattern);

                    if(requestPath.endsWith(urlPattern)){

                        for(VerbInfoDTO verbDTO : resourceInfoDTO.getHttpVerbs()){
                            if(verbDTO.getHttpVerb().equals(httpMethod)){
                                //Store verb in cache
                                resourceCache.put(requestCacheKey, verbDTO);
                                return verbDTO.getAuthType();
                            }
                        }
                    }
                }
            }


            //Remove the section after the last occurrence of the '/' character
            int index = requestPath.lastIndexOf("/");
            requestPath = requestPath.substring(0, index <= 0 ? 0 : index);
        }
        //nothing found. return the highest level of security
        return APIConstants.NO_MATCHING_AUTH_SCHEME;
    }

    private APIInfoDTO doGetAPIInfo(String context, String apiVersion) {
        APIInfoDTO apiInfoDTO = new APIInfoDTO();
        try {
            ArrayList<URITemplate> uriTemplates = ApiMgtDAO.getAllURITemplates(context, apiVersion);

            apiInfoDTO.setApiName(context);
            apiInfoDTO.setContext(context);
            apiInfoDTO.setVersion(apiVersion);
            apiInfoDTO.setResources(new HashSet<ResourceInfoDTO>());

            ResourceInfoDTO resourceInfoDTO = null;
            VerbInfoDTO verbInfoDTO = null;
            int i = 0;
            for (URITemplate uriTemplate : uriTemplates) {
                if (resourceInfoDTO != null && resourceInfoDTO.getUrlPattern().equalsIgnoreCase(uriTemplate.getUriTemplate())) {
                    HashSet<VerbInfoDTO> verbs = (HashSet<VerbInfoDTO>) resourceInfoDTO.getHttpVerbs();
                    verbInfoDTO = new VerbInfoDTO();
                    verbInfoDTO.setHttpVerb(uriTemplate.getHTTPVerb());
                    verbInfoDTO.setAuthType(uriTemplate.getAuthType());
                    verbs.add(verbInfoDTO);
                    resourceInfoDTO.setHttpVerbs(verbs);
                    apiInfoDTO.getResources().add(resourceInfoDTO);
                } else {
                    resourceInfoDTO = new ResourceInfoDTO();
                    resourceInfoDTO.setUrlPattern(uriTemplate.getUriTemplate());
                    verbInfoDTO = new VerbInfoDTO();
                    verbInfoDTO.setHttpVerb(uriTemplate.getHTTPVerb());
                    verbInfoDTO.setAuthType(uriTemplate.getAuthType());
                    HashSet<VerbInfoDTO> httpVerbs2 = new HashSet();
                    httpVerbs2.add(verbInfoDTO);
                    resourceInfoDTO.setHttpVerbs(httpVerbs2);
                    apiInfoDTO.getResources().add(resourceInfoDTO);
                }
            }
        } catch (APIManagementException e) {
            log.error("Loading URI templates for " + context + ":" + apiVersion + " failed", e);
        }
        return apiInfoDTO;
    }
}
