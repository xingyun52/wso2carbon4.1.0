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

package org.wso2.carbon.identity.oauth.config;

import org.apache.amber.oauth2.common.message.types.GrantType;
import org.apache.amber.oauth2.common.message.types.ResponseType;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfigurationException;
import org.wso2.carbon.identity.core.util.IdentityConfigParser;
import org.wso2.carbon.identity.oauth.OAuthConstants;
import org.wso2.carbon.identity.oauth.preprocessor.TokenPersistencePreprocessor;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.validators.OAuth2TokenValidator;
import org.wso2.carbon.identity.oauth2.validators.TokenValidationHandler;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * Runtime representation of the OAuth Configuration as configured through identity.xml
 */
public class OAuthServerConfiguration {

    private static Log log = LogFactory.getLog(OAuthServerConfiguration.class);

    private static final String CONFIG_ELEM_OAUTH = "OAuth";

    /**
     * Localpart names for the OAuth configuration in identity.xml.
     */
    private class ConfigElements {
        // Callback handler related configuration elements
        private static final String OAUTH_CALLBACK_HANDLERS = "OAuthCallbackHandlers";
        private static final String OAUTH_CALLBACK_HANDLER = "OAuthCallbackHandler";
        private static final String CLAIM_URI = "ClaimUri";
        private static final String REQUIRED_CLAIM_URIS = "RequiredRespHeaderClaimUris";
        private static final String CALLBACK_CLASS = "Class";
        private static final String CALLBACK_PRIORITY = "Priority";
        private static final String CALLBACK_PROPERTIES = "Properties";
        private static final String CALLBACK_PROPERTY = "Property";
        private static final String CALLBACK_ATTR_NAME = "Name";
        private static final String TOKEN_VALIDATORS = "TokenValidators";
        private static final String TOKEN_VALIDATOR = "TokenValidator";
        private static final String TOKEN_TYPE_ATTR = "type";
        private static final String TOKEN_CLASS_ATTR = "class";
        

        // Default timestamp skew
        public static final String TIMESTAMP_SKEW = "TimestampSkew";

        // Default validity periods
        private static final String AUTHORIZATION_CODE_DEFAULT_VALIDITY_PERIOD =
                "AuthorizationCodeDefaultValidityPeriod";
        private static final String ACCESS_TOKEN_DEFAULT_VALIDITY_PERIOD =
                "AccessTokenDefaultValidityPeriod";
        // Enable/Disable cache
        public static final String ENABLE_CACHE = "EnableOAuthCache";

        //TokenStoragePreprocessor
        public static final String TOKEN_PERSISTENCE_PREPROCESSOR = "TokenPersistencePreprocessor";

        //Supported Grant Types
        public static final String SUPPORTED_GRANT_TYPES = "SupportedGrantTypes";

        //Supported Response Types
        public static final String SUPPORTED_RESP_TYPES = "SupportedResponseTypes";
        
        //JWT Generator 
        public static final String AUTHORIZATION_CONTEXT_TOKEN_GENERATION = "AuthorizationContextTokenGeneration";
        public static final String ENABLED = "Enabled";
        public static final String TOKEN_GENERATOR_IMPL_CLASS = "TokenGeneratorImplClass";
        public static final String CLAIMS_RETRIEVER_IMPL_CLASS = "ClaimsRetrieverImplClass";
        public static final String CONSUMER_DIALECT_URI = "ConsumerDialectURI";
        public static final String SIGNATURE_ALGORITHM = "SignatureAlgorithm";
        public static final String SECURITY_CONTEXT_TTL = "AuthorizationContextTTL";

        public static final String ENABLE_ASSERTIONS = "EnableAssertions";
        public static final String ENABLE_ASSERTIONS_USERNAME = "UserName";
        public static final String ENABLE_ACCESS_TOKEN_PARTITIONING = "EnableAccessTokenPartitioning";
        public static final String ACCESS_TOKEN_PARTITIONING_DOMAINS = "AccessTokenPartitioningDomains";
    }


    private static OAuthServerConfiguration instance;

    private long defaultAuthorizationCodeValidityPeriodInSeconds = 300;

    private long defaultAccessTokenValidityPeriodInSeconds = 3600;

    private long defaultTimeStampSkewInSeconds = 300;

    private boolean cacheEnabled = true;

    private boolean assertionsUserNameEnabled = false;

    private boolean accessTokenPartitioningEnabled = false;

    private String accessTokenPartitioningDomains = null;

    private String tokenPersistencePreProcessorClassName =
            "org.wso2.carbon.identity.oauth.preprocessor.PlainTokenPersistencePreprocessor";

    private TokenPersistencePreprocessor tokenPersistencePreprocessor = null;

    private Set<OAuthCallbackHandlerMetaData> callbackHandlerMetaData =
            new HashSet<OAuthCallbackHandlerMetaData>();

    private List<String> supportedGrantTypes = new ArrayList<String>();

    private List<String> supportedResponseTypes = new ArrayList<String>();
    
    private List<String> requiredHeaderClaimUris = new ArrayList<String>();
    
    private boolean isAuthContextTokGenEnabled = false;

    private String tokenGeneratorImplClass = "org.wso2.carbon.identity.oauth2.token.JWTTokenGenerator";

    private String claimsRetrieverImplClass = "org.wso2.carbon.identity.oauth2.token.DefaultClaimsRetriever";

    private String consumerDialectURI = "http://wso2.org/claims";

    private String signatureAlgorithm = "SHA256withRSA";

    private String authContextTTL = "15L";

    private OAuthServerConfiguration() {
        buildOAuthServerConfiguration();
    }

    public static OAuthServerConfiguration getInstance() {
        CarbonUtils.checkSecurity();
        if (instance == null) {
            synchronized (OAuthServerConfiguration.class) {
                if (instance == null) {
                    instance = new OAuthServerConfiguration();
                }
            }
        }
        return instance;
    }

    private void buildOAuthServerConfiguration() {
        try {
        	
            IdentityConfigParser configParser = IdentityConfigParser.getInstance();
            OMElement oauthElem = configParser.getConfigElement(CONFIG_ELEM_OAUTH);

            if (oauthElem == null) {
                warnOnFaultyConfiguration("OAuth element is not available.");
                return;
            }

            // read callback handler configurations
            parseOAuthCallbackHandlers(oauthElem.getFirstChildWithName(
                    getQNameWithIdentityNS(ConfigElements.OAUTH_CALLBACK_HANDLERS)));
            
            // get the required claim uris - that needs to be included in the response.
            parseRequiredHeaderClaimUris(oauthElem.getFirstChildWithName(
                    getQNameWithIdentityNS(ConfigElements.REQUIRED_CLAIM_URIS)));
            
            // get the token validators by type
            parseTokenValidators(oauthElem.getFirstChildWithName(
                    getQNameWithIdentityNS(ConfigElements.TOKEN_VALIDATORS)));

            // read default timeout periods
            parseDefaultValidityPeriods(oauthElem);

            // read caching configurations
            parseCachingConfiguration(oauthElem);

            // read token preprocessor config
            parseTokenPersistencePreProcessorConfig(oauthElem);

            // read supported grant types
            parseSupportedGrantTypesConfig(oauthElem);

            // read supported response types
            parseSupportedResponseTypesConfig(oauthElem);
            
            // read JWT generator config
            parseAuthorizationContextTokenGeneratorConfig(oauthElem);

            // read the assertions user name config
            parseEnableAssertionsUserNameConfig(oauthElem);

            // read access token partitioning config
            parseAccessTokenPartitioningConfig(oauthElem);

            // read access token partitioning domains config
            parseAccessTokenPartitioningDomainsConfig(oauthElem);

        } catch (ServerConfigurationException e) {
            log.error("Error when reading the OAuth Configurations. " +
                    "OAuth related functionality might be affected.", e);
        }
    }

    public Set<OAuthCallbackHandlerMetaData> getCallbackHandlerMetaData() {
        return callbackHandlerMetaData;
    }

    public long getDefaultAuthorizationCodeValidityPeriodInSeconds() {
        return defaultAuthorizationCodeValidityPeriodInSeconds;
    }

    public long getDefaultAccessTokenValidityPeriodInSeconds() {
        return defaultAccessTokenValidityPeriodInSeconds;
    }

    public long getDefaultTimeStampSkewInSeconds() {
        return defaultTimeStampSkewInSeconds;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public List<String> getSupportedGrantTypes() {
        return supportedGrantTypes;
    }

    public List<String> getSupportedResponseTypes() {
        return supportedResponseTypes;
    }
    
    public List<String> getRequiredHeaderClaimUris() {
        return requiredHeaderClaimUris;
   
    }

    public boolean isAccessTokenPartitioningEnabled() {
        return accessTokenPartitioningEnabled;
    }

    public boolean isUserNameAssertionEnabled() {
        return assertionsUserNameEnabled;
    }

    public String getAccessTokenPartitioningDomains() {
        return accessTokenPartitioningDomains;
    }

    private QName getQNameWithIdentityNS(String localPart) {
        return new QName(IdentityConfigParser.IDENTITY_DEFAULT_NAMESPACE, localPart);
    }

    public boolean isAuthContextTokGenEnabled() {
        return isAuthContextTokGenEnabled;
    }

    public String getTokenGeneratorImplClass() {
        return tokenGeneratorImplClass;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public String getConsumerDialectURI() {
        return consumerDialectURI;
    }

    public String getClaimsRetrieverImplClass() {
        return claimsRetrieverImplClass;
    }

    public String getAuthorizationContextTTL() {
        return authContextTTL;
    }

    public TokenPersistencePreprocessor getTokenPersistencePreprocessor()
            throws IdentityOAuth2Exception {
        // create an instance of a TokenPersistencePreprocessor. This is a one time operation
        // because there can be only on OAuthServerConfiguration in a given runtime.
        if (tokenPersistencePreprocessor == null) {
            synchronized (this) {
                try {
                    Class clazz = this.getClass().getClassLoader().loadClass(tokenPersistencePreProcessorClassName);
                    tokenPersistencePreprocessor = (TokenPersistencePreprocessor) clazz.newInstance();

                    if (log.isDebugEnabled()) {
                        log.debug("An instance of " + tokenPersistencePreProcessorClassName +
                                " is created for OAuthServerConfiguration.");
                    }

                } catch (Exception e) {
                    String errorMsg = "Error when instantiating the TokenPersistencePreprocessor : "
                            + tokenPersistencePreProcessorClassName;
                    log.error(errorMsg, e);
                    throw new IdentityOAuth2Exception(errorMsg, e);
                }
            }
        }
        return tokenPersistencePreprocessor;
    }

    private void parseOAuthCallbackHandlers(OMElement callbackHandlersElem) {
        if (callbackHandlersElem == null) {
            warnOnFaultyConfiguration("AuthorizationCallbackHandlers element is not available.");
            return;
        }

        Iterator callbackHandlers = callbackHandlersElem.getChildrenWithLocalName(
                ConfigElements.OAUTH_CALLBACK_HANDLER);
        int callbackHandlerCount = 0;
        if (callbackHandlers != null) {
            for (; callbackHandlers.hasNext(); ) {
                OAuthCallbackHandlerMetaData cbHandlerMetadata =
                        buildAuthzCallbackHandlerMetadata((OMElement) callbackHandlers.next());
                if (cbHandlerMetadata != null) {
                    callbackHandlerMetaData.add(cbHandlerMetadata);
                    if (log.isDebugEnabled()) {
                        log.debug("OAuthAuthorizationCallbackHandleMetadata was added. Class : "
                                + cbHandlerMetadata.getClassName());
                    }
                    callbackHandlerCount++;
                }
            }
        }
        // if no callback handlers are registered, print a WARN
        if (!(callbackHandlerCount > 0)) {
            warnOnFaultyConfiguration("No AuthorizationCallbackHandler elements were found.");
        }
    }
    
    private void parseRequiredHeaderClaimUris(OMElement requiredClaimUrisElem) {
        if (requiredClaimUrisElem == null) {
            return;
        }

        Iterator claimUris = requiredClaimUrisElem.getChildrenWithLocalName(
                ConfigElements.CLAIM_URI);
        if (claimUris != null) {
            for (; claimUris.hasNext(); ) {
                OMElement claimUri =  ((OMElement) claimUris.next());
                if (claimUri != null) {
                   requiredHeaderClaimUris.add(claimUri.getText());
                }
            }
        }

    }
    
    private void parseTokenValidators(OMElement tokenValidators) {
        if (tokenValidators == null) {
            return;
        }

        Iterator validators = tokenValidators.getChildrenWithLocalName(
                ConfigElements.TOKEN_VALIDATOR);
        if (validators != null) {
            for (; validators.hasNext(); ) {
                OMElement validator =  ((OMElement) validators.next());
                if (validator != null) {
                   OAuth2TokenValidator tokenValidator = null;
                   String clazzName = null;
                try {
                    clazzName = validator.getAttributeValue(getQNameWithIdentityNS(ConfigElements.TOKEN_CLASS_ATTR));
                    Class clazz = Thread.currentThread().getContextClassLoader().loadClass(clazzName);
                    tokenValidator = (OAuth2TokenValidator) clazz.newInstance();
                } catch (ClassNotFoundException e) {
                    log.error("Class not in build path "+clazzName,e);
                } catch (InstantiationException e) {
                    log.error("Class initialization error "+clazzName,e);
                } catch (IllegalAccessException e) {
                    log.error("Class access error "+clazzName,e);

                }
                   String type = validator.getAttributeValue(getQNameWithIdentityNS(ConfigElements.TOKEN_TYPE_ATTR));
                   TokenValidationHandler.getInstance().addTokenValidator(type, tokenValidator);
                }
            }
        }
    }

    private void warnOnFaultyConfiguration(String logMsg) {
        log.warn("Error in OAuth Configuration. " + logMsg);
    }

    private OAuthCallbackHandlerMetaData buildAuthzCallbackHandlerMetadata(
            OMElement omElement) {
        // read the class attribute which is mandatory
        String className = omElement.getAttributeValue(new QName(ConfigElements.CALLBACK_CLASS));

        if (className == null) {
            log.error("Mandatory attribute \"Class\" is not present in the " +
                    "AuthorizationCallbackHandler element. " +
                    "AuthorizationCallbackHandler will not be registered.");
            return null;
        }

        // read the priority element, if it is not there, use the default priority of 1
        int priority = OAuthConstants.OAUTH_AUTHZ_CB_HANDLER_DEFAULT_PRIORITY;
        OMElement priorityElem = omElement.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.CALLBACK_PRIORITY));
        if (priorityElem != null) {
            priority = Integer.parseInt(priorityElem.getText());
        }

        if (log.isDebugEnabled()) {
            log.debug("Priority level of : " + priority + " is set for the " +
                    "AuthorizationCallbackHandler with the class : " + className);
        }

        // read the additional properties.
        OMElement paramsElem = omElement.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.CALLBACK_PROPERTIES));
        Properties properties = null;
        if (paramsElem != null) {
            Iterator paramItr = paramsElem.getChildrenWithLocalName(
                    ConfigElements.CALLBACK_PROPERTY);
            properties = new Properties();
            if (log.isDebugEnabled()) {
                log.debug("Registering Properties for AuthorizationCallbackHandler class : "
                        + className);
            }
            for (; paramItr.hasNext(); ) {
                OMElement paramElem = (OMElement) paramItr.next();
                String paramName = paramElem.getAttributeValue(
                        new QName(ConfigElements.CALLBACK_ATTR_NAME));
                String paramValue = paramElem.getText();
                properties.put(paramName, paramValue);
                if (log.isDebugEnabled()) {
                    log.debug("Property name : " + paramName + ", Property Value : " + paramValue);
                }
            }
        }
        return new OAuthCallbackHandlerMetaData(className, properties, priority);
    }

    private void parseDefaultValidityPeriods(OMElement oauthConfigElem) {
        // set the authorization code default timeout
        OMElement authzCodeTimeoutElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.AUTHORIZATION_CODE_DEFAULT_VALIDITY_PERIOD));

        if (authzCodeTimeoutElem != null) {
            defaultAuthorizationCodeValidityPeriodInSeconds = Long.parseLong(authzCodeTimeoutElem.getText());
        }

        // set the access token default timeout
        OMElement accessTokTimeoutElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.ACCESS_TOKEN_DEFAULT_VALIDITY_PERIOD));
        if (accessTokTimeoutElem != null) {
            defaultAccessTokenValidityPeriodInSeconds = Long.parseLong(accessTokTimeoutElem.getText());
        }

        OMElement timeStampSkewElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.TIMESTAMP_SKEW));
        if (timeStampSkewElem != null) {
            defaultTimeStampSkewInSeconds = Long.parseLong(timeStampSkewElem.getText());
        }

        if (log.isDebugEnabled()) {
            if (authzCodeTimeoutElem == null) {
                log.debug("\"Authorization Code Default Timeout\" element was not available " +
                        "in identity.xml. Continuing with the default value.");
            }
            if (accessTokTimeoutElem == null) {
                log.debug("\"Access Token Default Timeout\" element was not available " +
                        "in from identity.xml. Continuing with the default value.");
            }
            if (timeStampSkewElem == null) {
                log.debug("\"Default Timestamp Skew\" element was not available " +
                        "in from identity.xml. Continuing with the default value.");
            }
            log.debug("Authorization Code Default Timeout is set to : " +
                    defaultAuthorizationCodeValidityPeriodInSeconds + "ms.");
            log.debug("Access Token Default Timeout is set to " +
                    defaultAccessTokenValidityPeriodInSeconds + "ms.");
            log.debug("Default TimestampSkew is set to " +
                    defaultTimeStampSkewInSeconds + "ms.");
        }
    }

    private void parseCachingConfiguration(OMElement oauthConfigElem) {
        OMElement enableCacheElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.ENABLE_CACHE));
        if (enableCacheElem != null) {
            cacheEnabled = Boolean.parseBoolean(enableCacheElem.getText());
        }

        if (log.isDebugEnabled()) {
            log.debug("Enable OAuth Cache was set to : " + cacheEnabled);
        }
    }

    private void parseAccessTokenPartitioningConfig(OMElement oauthConfigElem) {
        OMElement enableAccessTokenPartitioningElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.ENABLE_ACCESS_TOKEN_PARTITIONING));
        if (enableAccessTokenPartitioningElem != null) {
            accessTokenPartitioningEnabled =
                    Boolean.parseBoolean(enableAccessTokenPartitioningElem.getText());
        }

        if (log.isDebugEnabled()) {
            log.debug("Enable OAuth Access Token Partitioning was set to : " +
                    accessTokenPartitioningEnabled);
        }
    }

    private void parseAccessTokenPartitioningDomainsConfig(OMElement oauthConfigElem) {
        OMElement enableAccessTokenPartitioningElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.ACCESS_TOKEN_PARTITIONING_DOMAINS));
        if (enableAccessTokenPartitioningElem != null) {
            accessTokenPartitioningDomains = enableAccessTokenPartitioningElem.getText();
        }

        if (log.isDebugEnabled()) {
            log.debug("Enable OAuth Access Token Partitioning Domains was set to : " +
                    accessTokenPartitioningDomains);
        }
    }

    private void parseEnableAssertionsUserNameConfig(OMElement oauthConfigElem) {
        OMElement enableAssertionsElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.ENABLE_ASSERTIONS));
        if (enableAssertionsElem != null) {
            OMElement enableAssertionsUserNameElem = enableAssertionsElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.ENABLE_ASSERTIONS_USERNAME));
            if (enableAssertionsUserNameElem != null) {
                assertionsUserNameEnabled =
                        Boolean.parseBoolean(enableAssertionsUserNameElem.getText());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Enable Assertions-UserName was set to : " +
                    assertionsUserNameEnabled);
        }
    }

    private void parseTokenPersistencePreProcessorConfig(OMElement oauthConfigElem) {
        OMElement preprocessorConfigElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.TOKEN_PERSISTENCE_PREPROCESSOR));
        if (preprocessorConfigElem != null) {
            tokenPersistencePreProcessorClassName = preprocessorConfigElem.getText().trim();
        }

        if (log.isDebugEnabled()) {
            log.debug("Token Persistence Preprocessor was set to : "
                    + tokenPersistencePreProcessorClassName);
        }
    }

    private void parseSupportedGrantTypesConfig(OMElement oauthConfigElem) {
        OMElement supportedGrantTypesElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.SUPPORTED_GRANT_TYPES));

        // These are the default grant types supported.
        List<String> validGrantTypes = new ArrayList<String>(4);
        validGrantTypes.add(GrantType.AUTHORIZATION_CODE.toString());
        validGrantTypes.add(GrantType.CLIENT_CREDENTIALS.toString());
        validGrantTypes.add(GrantType.PASSWORD.toString());
        validGrantTypes.add(GrantType.REFRESH_TOKEN.toString());
        validGrantTypes.add(GrantType.SAML20_BEARER_ASSERTION.toString());

        if (supportedGrantTypesElem != null) {
            String grantTypeStr = supportedGrantTypesElem.getText();
            if (grantTypeStr != null) {
                String[] grantTypes = grantTypeStr.split(",");
                // Check whether user provided grant types are valid
                if (grantTypes != null) {
                    for (String providedGrantType : grantTypes) {
                        providedGrantType = providedGrantType.trim();
                        if (validGrantTypes.contains(providedGrantType)) {
                            supportedGrantTypes.add(providedGrantType);
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Invalid Grant Type provided : " + providedGrantType +
                                        ". This will be ignored.");
                            }
                        }
                    }
                }
            }
        } else {
            // if this element is not present, assume the default case.
            supportedGrantTypes.addAll(validGrantTypes);
        }



        if (log.isDebugEnabled()) {
            log.debug("Supported Grant Types : " + supportedGrantTypes);
        }
    }

    private void parseSupportedResponseTypesConfig(OMElement oauthConfigElem) {
        OMElement supportedRespTypesElem = oauthConfigElem.getFirstChildWithName(
                getQNameWithIdentityNS(ConfigElements.SUPPORTED_RESP_TYPES));

        // These are the default grant types supported.
        List<String> validRespTypes = new ArrayList<String>(4);
        validRespTypes.add(ResponseType.CODE.toString());
        validRespTypes.add(ResponseType.TOKEN.toString());

        if (supportedRespTypesElem != null) {
            String respTypesStr = supportedRespTypesElem.getText();
            if (respTypesStr != null) {
                String[] respTypes = respTypesStr.split(",");
                // Check whether user provided grant types are valid
                if (respTypes != null) {
                    for (String providedRespType : respTypes) {
                        providedRespType = providedRespType.trim();
                        if (validRespTypes.contains(providedRespType)) {
                            supportedResponseTypes.add(providedRespType);
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Invalid Response Type provided : " + providedRespType +
                                        ". This will be ignored.");
                            }
                        }
                    }
                }
            }
        } else {
            // if this element is not present, assume the default case.
            supportedResponseTypes.addAll(validRespTypes);
        }

        if (log.isDebugEnabled()) {
            log.debug("Supported Response Types : " + supportedResponseTypes);
        }
    }
    
	private void parseAuthorizationContextTokenGeneratorConfig(OMElement oauthConfigElem) {
		OMElement authContextTokGenConfigElem =
                oauthConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.AUTHORIZATION_CONTEXT_TOKEN_GENERATION));
		if (authContextTokGenConfigElem != null) {
            OMElement enableJWTGenerationConfigElem =
                    authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.ENABLED));
			if(enableJWTGenerationConfigElem != null){
                String enableJWTGeneration = enableJWTGenerationConfigElem.getText().trim();
                if (enableJWTGeneration != null && JavaUtils.isTrueExplicitly(enableJWTGeneration)) {
                    isAuthContextTokGenEnabled = true;
                    if(authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.TOKEN_GENERATOR_IMPL_CLASS)) != null){
                        tokenGeneratorImplClass =  authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.TOKEN_GENERATOR_IMPL_CLASS)).getText().trim();
                    }
                    if(authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.CLAIMS_RETRIEVER_IMPL_CLASS)) != null){
                        claimsRetrieverImplClass = authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.CLAIMS_RETRIEVER_IMPL_CLASS)).getText().trim();
                    }
                    if(authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.CONSUMER_DIALECT_URI)) != null){
                        consumerDialectURI = authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.CONSUMER_DIALECT_URI)).getText().trim();
                    }
                    if(authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.SIGNATURE_ALGORITHM)) != null){
                        signatureAlgorithm = authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.SIGNATURE_ALGORITHM)).getText().trim();
                    }
                    if(authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.SECURITY_CONTEXT_TTL)) != null){
                        authContextTTL = authContextTokGenConfigElem.getFirstChildWithName(getQNameWithIdentityNS(ConfigElements.SECURITY_CONTEXT_TTL)).getText().trim();
                    }
                }
            }
		}
		if (log.isDebugEnabled()) {
            if(isAuthContextTokGenEnabled){
                log.debug("JWT Generation is enabled");
            }else{
			    log.debug("JWT Generation is disabled");
            }
		}
	}
}
