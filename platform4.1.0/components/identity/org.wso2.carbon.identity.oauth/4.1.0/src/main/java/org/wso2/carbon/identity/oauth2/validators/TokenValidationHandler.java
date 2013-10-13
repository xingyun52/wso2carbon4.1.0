/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth2.validators;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.AuthorizationContextToken;
import org.wso2.carbon.identity.oauth2.authcontext.AuthorizationContextTokenGenerator;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;

import java.util.Hashtable;
import java.util.Map;

/**
 * Handles the token validation by invoking the proper validation handler by looking at the token
 * type.
 */
public class TokenValidationHandler {

    private Log log = LogFactory.getLog(TokenValidationHandler.class);

    private Map<String, OAuth2TokenValidator> tokenValidators =
            new Hashtable<String, OAuth2TokenValidator>();

    private static TokenValidationHandler instance = new TokenValidationHandler();

    AuthorizationContextTokenGenerator tokenGenerator = null;

    private TokenValidationHandler() {
        if(instance == null){
            synchronized (this) {
                if(instance == null){
                    tokenValidators.put(BearerTokenValidator.TOKEN_TYPE, new BearerTokenValidator());
                    // setting up the JWT if required
                    if (OAuthServerConfiguration.getInstance().isAuthContextTokGenEnabled()) {
                        try {
                            Class clazz = this.getClass().getClassLoader().loadClass(OAuthServerConfiguration.getInstance().
                                    getTokenGeneratorImplClass());
                            tokenGenerator = (AuthorizationContextTokenGenerator)clazz.newInstance();
                            tokenGenerator.init();
                            if (log.isDebugEnabled()) {
                                log.debug("An instance of " + OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass() +
                                    " is created for OAuthServerConfiguration.");
                            }
                        } catch (ClassNotFoundException e){
                            String errorMsg = "Class not found: " +
                                    OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                            log.error(errorMsg,e);
                        } catch (InstantiationException e) {
                            String errorMsg = "Error while instantiating: " +
                                    OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                            log.error(errorMsg, e);
                        } catch (IllegalAccessException e) {
                            String errorMsg = "Illegal access to: " +
                                    OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                            log.error(errorMsg, e);
                        } catch (IdentityOAuth2Exception e){
                            String errorMsg = "Error while initializing: " +
                                    OAuthServerConfiguration.getInstance().getTokenGeneratorImplClass();
                            log.error(errorMsg, e);
                        }

                    }
                }
            }
        }
    }

    public static TokenValidationHandler getInstance() {
        return instance;
    }
    
    public void addTokenValidator(String type, OAuth2TokenValidator handler) {
        tokenValidators.put(type, handler);
    }

    public OAuth2TokenValidationResponseDTO validate(OAuth2TokenValidationRequestDTO requestDTO)
            throws IdentityOAuth2Exception {

        OAuth2TokenValidator tokenValidator = tokenValidators.get(requestDTO.getTokenType());

        // There is no token validator for the provided token type.
        if (tokenValidator == null) {
            log.warn("Unsupported token type.");
            OAuth2TokenValidationResponseDTO tokenValidationRespDTO =
                    new OAuth2TokenValidationResponseDTO();
            tokenValidationRespDTO.setValid(false);
            tokenValidationRespDTO.setErrorMsg("Unsupported token type.");
            return tokenValidationRespDTO;
        }

        OAuth2TokenValidationResponseDTO tokenRespDTO = tokenValidator.validate(requestDTO);

        if(tokenRespDTO.isValid()){
            if(tokenGenerator != null){
                AuthorizationContextToken token = tokenGenerator.generateToken(requestDTO, tokenRespDTO);
                tokenRespDTO.setAuthorizationContextToken(token);
                if (log.isDebugEnabled()) {
                    log.debug(tokenGenerator.getClass().getName() + "generated token set to response : " + token.getTokenString());
                }
            }
        }

        return tokenRespDTO;
    }
}
