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

package org.wso2.carbon.apimgt.gateway.handlers.security.oauth;

import org.apache.axis2.Constants;
import org.apache.http.HttpHeaders;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.RESTConstants;
import org.wso2.carbon.apimgt.gateway.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.gateway.handlers.security.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;

import java.util.Map;

/**
 * An API consumer authenticator which authenticates user requests using
 * the OAuth protocol. This implementation uses some default token/delimiter
 * values to parse OAuth headers, but if needed these settings can be overridden
 * through the APIManagerConfiguration.
 */
public class OAuthAuthenticator implements Authenticator {

    protected APIKeyValidator keyValidator;

    private String securityHeader = HttpHeaders.AUTHORIZATION;
    private String consumerKeyHeaderSegment = "Bearer";
    private String oauthHeaderSplitter = ",";
    private String consumerKeySegmentDelimiter = " ";
    private String securityContextHeader;
    private boolean removeOAuthHeadersFromOutMessage=false;

    public void init(SynapseEnvironment env) {
        this.keyValidator = new APIKeyValidator(env.getSynapseConfiguration().getAxisConfiguration());
        initOAuthParams();
    }

    public void destroy() {
        this.keyValidator.cleanup();
    }

    public boolean authenticate(MessageContext synCtx) throws APISecurityException {
        Map headers = (Map) ((Axis2MessageContext) synCtx).getAxis2MessageContext().
                getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        String apiKey = null;
        if (headers != null) {
            apiKey = extractCustomerKeyFromAuthHeader(headers);
        }
        if(removeOAuthHeadersFromOutMessage){
            headers.remove(securityHeader);
        }
        String apiContext = (String) synCtx.getProperty(RESTConstants.REST_API_CONTEXT);
        String apiVersion = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API_VERSION);
        String fullRequestPath = (String)synCtx.getProperty(RESTConstants.REST_FULL_REQUEST_PATH);

        String requestPath = fullRequestPath.substring((apiContext + apiVersion).length() + 1, fullRequestPath.length());
        String httpMethod = (String)((Axis2MessageContext) synCtx).getAxis2MessageContext().
                getProperty(Constants.Configuration.HTTP_METHOD);

        //If the matching resource does not require authentication
        String authenticationScheme = keyValidator.getResourceAuthenticationScheme(apiContext, apiVersion, requestPath, httpMethod);
        APIKeyValidationInfoDTO info;
        if(APIConstants.AUTH_NO_AUTHENTICATION.equals(authenticationScheme)){

            String clientIP = (String)((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty(APIConstants.REMOTE_ADDR);

            //Create a dummy AuthenticationContext object with hard coded values for
            // Tier and KeyType. This is because we cannot determine the Tier nor Key
            // Type without subscription information..
            AuthenticationContext authContext = new AuthenticationContext();
            authContext.setAuthenticated(true);
            authContext.setTier(APIConstants.UNAUTHENTICATED_TIER);
            //Requests are throttled by the ApiKey that is set here. In an unauthenticated scenario,
            //we will use the client's IP address for throttling.
            authContext.setApiKey(clientIP);
            authContext.setKeyType(APIConstants.API_KEY_TYPE_PRODUCTION);
            //This name is hardcoded as anonymous because there is no associated user token
            authContext.setUsername("anonymous");
            authContext.setCallerToken(null);
            authContext.setApplicationName(null);
            APISecurityUtils.setAuthenticationContext(synCtx, authContext, securityContextHeader);
            return true;
        } else if (APIConstants.NO_MATCHING_AUTH_SCHEME.equals(authenticationScheme)) {
            info = new APIKeyValidationInfoDTO();
            info.setAuthorized(false);
            info.setValidationStatus(900906);
        } else {
            if (apiKey == null || apiContext == null || apiVersion == null) {
                throw new APISecurityException(APISecurityConstants.API_AUTH_MISSING_CREDENTIALS,
                                               "Required OAuth credentials not provided");
            }
            info = keyValidator.getKeyValidationInfo(apiContext, apiKey, apiVersion, authenticationScheme);
            synCtx.setProperty("APPLICATION_NAME", info.getApplicationName());
            synCtx.setProperty("END_USER_NAME", info.getEndUserName());
        }

        if (info.isAuthorized()) {
            AuthenticationContext authContext = new AuthenticationContext();
            authContext.setAuthenticated(true);
            authContext.setTier(info.getTier());
            authContext.setApiKey(apiKey);
            authContext.setKeyType(info.getType());
            authContext.setUsername(info.getSubscriber());
            authContext.setCallerToken(info.getEndUserToken());
            authContext.setApplicationId(info.getApplicationId());
            authContext.setApplicationName(info.getApplicationName());
            authContext.setApplicationTier(info.getApplicationTier());
            APISecurityUtils.setAuthenticationContext(synCtx, authContext, securityContextHeader);
            return true;
        } else {
            throw new APISecurityException(info.getValidationStatus(),
                    "Access failure for API: " + apiContext + ", version: " + apiVersion +
                            " with key: " + apiKey);
        }
    }

    /**
     * Extracts the customer API key from the OAuth Authentication header. If the required
     * security header is present in the provided map, it will be removed from the map
     * after processing.
     *
     * @param headersMap Map of HTTP headers
     * @return extracted customer key value or null if the required header is not present
     */
    public String extractCustomerKeyFromAuthHeader(Map headersMap) {

        //From 1.0.7 version of this component onwards remove the OAuth authorization header from
        // the message is configurable. So we dont need to remove headers at this point.
        String authHeader = (String) headersMap.get(securityHeader);
        if (authHeader == null) {
            return null;
        }

        if (authHeader.startsWith("OAuth ") || authHeader.startsWith("oauth ")) {
            authHeader = authHeader.substring(authHeader.indexOf("o"));
        }

        String[] headers = authHeader.split(oauthHeaderSplitter);
        if (headers != null) {
            for (int i = 0; i < headers.length; i++) {
                String[] elements = headers[i].split(consumerKeySegmentDelimiter);
                if (elements != null && elements.length > 1) {
                    int j = 0;
                    boolean isConsumerKeyHeaderAvailable = false;
                    for (String element : elements) {
                        if (!"".equals(element.trim())) {
                            if (consumerKeyHeaderSegment.equals(elements[j].trim())) {
                                isConsumerKeyHeaderAvailable = true;
                            } else if (isConsumerKeyHeaderAvailable) {
                                return removeLeadingAndTrailing(elements[j].trim());
                            }
                        }
                        j++;
                    }
                }
            }
        }
        return null;
    }

    private String removeLeadingAndTrailing(String base) {
        String result = base;

        if (base.startsWith("\"") || base.endsWith("\"")) {
            result = base.replace("\"", "");
        }
        return result.trim();
    }

    protected void initOAuthParams() {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().getAPIManagerConfiguration();
        String value = config.getFirstProperty(
                APISecurityConstants.API_SECURITY_OAUTH_HEADER);
        if (value != null) {
            securityHeader = value;
        }

        value = config.getFirstProperty(
                APISecurityConstants.API_SECURITY_CONSUMER_KEY_HEADER_SEGMENT);
        if (value != null) {
            consumerKeyHeaderSegment = value;
        }

        value = config.getFirstProperty(
                APISecurityConstants.API_SECURITY_OAUTH_HEADER_SPLITTER);
        if (value != null) {
            oauthHeaderSplitter = value;
        }

        value = config.getFirstProperty(
                APISecurityConstants.API_SECURITY_CONSUMER_KEY_SEGMENT_DELIMITER);
        if (value != null) {
            consumerKeySegmentDelimiter = value;
        }

        value = config.getFirstProperty(
                APISecurityConstants.API_SECURITY_CONTEXT_HEADER);
        if (value != null) {
            securityContextHeader = value;
        }
    }

    public String getChallengeString() {
        return "OAuth2 realm=\"WSO2 API Manager\"";
    }
}
