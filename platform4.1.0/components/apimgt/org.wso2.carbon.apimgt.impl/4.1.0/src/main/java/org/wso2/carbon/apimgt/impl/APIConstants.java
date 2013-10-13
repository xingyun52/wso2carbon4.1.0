/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.apimgt.impl;

import javax.xml.namespace.QName;

/**
 * This class represents the constants that are used for APIManager implementation
 */
public final class APIConstants {

    //key value of the provider rxt
    public static final String PROVIDER_KEY = "provider";

    //key value of the APIImpl rxt
    public static final String API_KEY = "api";
    
    public static final String API_CONTEXT_ID = "api.context.id";
    //This is the resource name of API
    public static final String API_RESOURCE_NAME ="/api";

    //Association between documentation and its content
    public static final String DOCUMENTATION_CONTENT_ASSOCIATION = "hasContent";

    public static final String DOCUMENTATION_KEY = "document";

    //association type between provider and APIImpl
    public static final String PROVIDER_ASSOCIATION = "provides";

    //association type between API and Documentation
    public static final String DOCUMENTATION_ASSOCIATION = "document";

    //registry location of providers
    public static final String PROVIDERS_PATH = "/providers";
    
    public static final String API_APPLICATION_DATA_LOCATION = "/apimgt/applicationdata";

    //registry location of API
    public static final String API_LOCATION = API_APPLICATION_DATA_LOCATION + "/provider";
    
    public static final String API_TIER_LOCATION = API_APPLICATION_DATA_LOCATION + "/tiers.xml";

    public static final String API_IMAGE_LOCATION = API_APPLICATION_DATA_LOCATION + "/icons";

    //registry location for consumer
    public static final String API_ROOT_LOCATION = API_APPLICATION_DATA_LOCATION + "/provider";

    public static final String API_ICON_IMAGE = "icon";
    public static final String API_RESTRICTED_VISIBILITY = "restricted";

    public static final String ACCESS_TOKEN_STORE_TABLE = "IDN_OAUTH2_ACCESS_TOKEN";

    public static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";
    // Those constance are used in API artifact.
    public static final String API_OVERVIEW_NAME = "overview_name";
    public static final String API_OVERVIEW_VERSION = "overview_version";
    public static final String API_OVERVIEW_CONTEXT = "overview_context";
    public static final String API_OVERVIEW_DESCRIPTION = "overview_description";
    public static final String API_OVERVIEW_ENDPOINT_URL = "overview_endpointURL";
    public static final String API_OVERVIEW_SANDBOX_URL = "overview_sandboxURL";
    public static final String API_OVERVIEW_WSDL = "overview_wsdl";
    public static final String API_OVERVIEW_WADL = "overview_wadl";
    public static final String API_OVERVIEW_PROVIDER = "overview_provider";
    public static final String API_OVERVIEW_THUMBNAIL_URL="overview_thumbnail";
    public static final String API_OVERVIEW_STATUS="overview_status";
    public static final String API_OVERVIEW_TIER="overview_tier";
    public static final String API_OVERVIEW_IS_LATEST ="overview_isLatest";
    public static final String API_URI_TEMPLATES ="uriTemplates_entry";
    public static final String API_OVERVIEW_TEC_OWNER ="overview_technicalOwner";
    public static final String API_OVERVIEW_TEC_OWNER_EMAIL ="overview_technicalOwnerEmail";
    public static final String API_OVERVIEW_BUSS_OWNER ="overview_businessOwner";
    public static final String API_OVERVIEW_BUSS_OWNER_EMAIL ="overview_businessOwnerEmail";
    public static final String API_OVERVIEW_VISIBILITY ="overview_visibility";
    public static final String API_OVERVIEW_VISIBLE_ROLES ="overview_visibleRoles";
    public static final String API_STATUS = "STATUS";
    public static final String API_URI_PATTERN ="URITemplate_urlPattern";
    public static final String API_URI_HTTP_METHOD ="URITemplate_httpVerb";
    public static final String API_URI_AUTH_TYPE ="URITemplate_authType";
    public static final String API_OVERVIEW_ENDPOINT_SECURED = "overview_endpointSecured";
    public static final String API_OVERVIEW_ENDPOINT_USERNAME = "overview_endpointUsername";
    public static final String API_OVERVIEW_ENDPOINT_PASSWORD = "overview_endpointPpassword";

    //Those constance are used in Provider artifact.
    public static final String PROVIDER_OVERVIEW_NAME= "overview_name";
    public static final String PROVIDER_OVERVIEW_EMAIL = "overview_email";
    public static final String PROVIDER_OVERVIEW_DESCRIPTION = "overview_description";

    //database columns for Subscriber
    public static final String SUBSCRIBER_FIELD_EMAIL_ADDRESS = "EMAIL_ADDRESS";
    public static final String SUBSCRIBER_FIELD_USER_ID = "USER_ID";
    public static final String SUBSCRIBER_FIELD_DATE_SUBSCRIBED = "DATE_SUBSCRIBED";

    //tables columns for subscription
    public static final String SUBSCRIPTION_FIELD_SUBSCRIPTION_ID = "SUBSCRIPTION_ID";
    public static final String SUBSCRIPTION_FIELD_TIER_ID = "TIER_ID";
    public static final String SUBSCRIPTION_FIELD_API_ID = "API_ID";
    public static final String SUBSCRIPTION_FIELD_ACCESS_TOKEN = "ACCESS_TOKEN";
    public static final String SUBSCRIPTION_FIELD_LAST_ACCESS = "LAST_ACCESSED";

    public static final String SUBSCRIPTION_KEY_TYPE = "KEY_TYPE";
    public static final String SUBSCRIPTION_USER_TYPE = "USER_TYPE";
    public static final String ACCESS_TOKEN_USER_TYPE_APPLICATION = "APPLICATION";
    public static final String USER_TYPE_END_USER = "END_USER";

    //table columns for AM_APPLICATION
    public static final String APPLICATION_ID = "APPLICATION_ID";
    public static final String APPLICATION_NAME = "NAME";
    public static final String APPLICATION_SUBSCRIBER_ID = "SUBSCRIBER_ID";
    public static final String APPLICATION_TIER = "APPLICATION_TIER";

    //IDENTITY OAUTH2 table
    public static final String IDENTITY_OAUTH2_FIELD_TOKEN_STATE="TOKEN_STATE";
    public static final String IDENTITY_OAUTH2_FIELD_AUTHORIZED_USER = "AUTHZ_USER";
    public static final String IDENTITY_OAUTH2_FIELD_TIME_CREATED = "TIME_CREATED";
    public static final String IDENTITY_OAUTH2_FIELD_VALIDITY_PERIOD = "VALIDITY_PERIOD";

    //documentation rxt

    public static final String DOC_NAME= "overview_name";
    public static final String DOC_SUMMARY = "overview_summary";
    public static final String DOC_TYPE = "overview_type";
    public static final String DOC_DIR = "documentation";
    public static final String INLINE_DOCUMENT_CONTENT_DIR = "contents";
    public static final String DOCUMENT_FILE_DIR = "files";
    public static final String DOC_API_BASE_PATH="overview_apiBasePath";
    public static final String DOC_SOURCE_URL = "overview_sourceURL";
    public static final String DOC_FILE_PATH = "overview_filePath";
    public static final String DOC_SOURCE_TYPE = "overview_sourceType";
    public static final String DOC_OTHER_TYPE_NAME = "overview_otherTypeName";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String CREATED = "CREATED";
    public static final String DEPRECATED = "DEPRECATED";


    public static class TokenStatus {
        public static final String ACTIVE = "ACTIVE";
        public static final String BLOCKED = "BLOCKED";
        public static final String REVOKED = "REVOKED";
    }

    public static final String RXT_MEDIA_TYPE = "application/vnd.wso2.registry-ext-type+xml";
    public static final int TOP_TATE_MARGIN = 4;
    
    public static final class Permissions {
        public static final String API_CREATE = "/permission/admin/manage/api/create";
        public static final String API_PUBLISH = "/permission/admin/manage/api/publish";
        public static final String API_SUBSCRIBE = "/permission/admin/manage/api/subscribe";
    }
    
    public static final String API_GATEWAY = "APIGateway.";
    public static final String API_GATEWAY_SERVER_URL = API_GATEWAY + "ServerURL";
    public static final String API_GATEWAY_USERNAME = API_GATEWAY + "Username";
    public static final String API_GATEWAY_PASSWORD = API_GATEWAY + "Password";
    public static final String API_GATEWAY_KEY_CACHE_ENABLED = API_GATEWAY + "EnableGatewayKeyCache";
    public static final String API_GATEWAY_API_ENDPOINT = API_GATEWAY + "APIEndpointURL";
    
    public static final String API_KEY_MANAGER = "APIKeyManager.";
    public static final String API_KEY_MANAGER_URL = API_KEY_MANAGER + "ServerURL";
    public static final String API_KEY_MANAGER_USERNAME = API_KEY_MANAGER + "Username";
    public static final String API_KEY_MANAGER_PASSWORD = API_KEY_MANAGER + "Password";
    public static final String API_KEY_MANGER_THRIFT_CLIENT_PORT = API_KEY_MANAGER + "ThriftClientPort";
    public static final String API_KEY_MANGER_THRIFT_SERVER_PORT = API_KEY_MANAGER + "ThriftServerPort";
    public static final String API_KEY_MANGER_CONNECTION_TIMEOUT = API_KEY_MANAGER + "ThriftClientConnectionTimeOut";
    public static final String API_KEY_MANAGER_THRIFT_SERVER_HOST = API_KEY_MANAGER + "ThriftServerHost";
    public static final String API_KEY_VALIDATOR_CLIENT_TYPE = API_KEY_MANAGER + "KeyValidatorClientType";
    public static final String API_KEY_VALIDATOR_WS_CLIENT = "WSClient";
    public static final String API_KEY_MANAGER_ENABLE_THRIFT_SERVER = API_KEY_MANAGER + "EnableThriftServer";
    public static final String API_KEY_VALIDATOR_THRIFT_CLIENT = "ThriftClient";
    public static final String API_KEY_SECURITY_CONTEXT_TTL = API_KEY_MANAGER + "SecurityContextTTL";
    public static final String API_KEY_MANAGER_ENABLE_JWT_CACHE = API_KEY_MANAGER + "EnableJWTCache";
    public static final String API_KEY_MANAGER_ENABLE_VALIDATION_INFO_CACHE = API_KEY_MANAGER + "EnableKeyMgtValidationInfoCache";
    public static final String API_KEY_MANAGER_REMOVE_USERNAME_TO_JWT_FOR_APP_TOKEN = API_KEY_MANAGER + "RemoveUserNameToJWTForApplicationToken";
    public static final String API_KEY_MANAGER_ENABLE_ASSERTIONS = API_KEY_MANAGER + "EnableAssertions.";
    public static final String API_KEY_MANAGER_ENABLE_ASSERTIONS_USERNAME = API_KEY_MANAGER_ENABLE_ASSERTIONS + "UserName";
    public static final String API_KEY_MANAGER_ENABLE_ACCESS_TOKEN_PARTITIONING = API_KEY_MANAGER + "AccessTokenPartitioning." + "EnableAccessTokenPartitioning";
    public static final String API_KEY_MANAGER_ACCESS_TOKEN_PARTITIONING_DOMAINS = API_KEY_MANAGER + "AccessTokenPartitioning." + "AccessTokenPartitioningDomains";

    public static final String API_STORE = "APIStore.";
    public static final String API_STORE_DISPLAY_ALL_APIS = API_STORE + "DisplayAllAPIs";
    public static final String API_STORE_DISPLAY_MULTIPLE_VERSIONS = API_STORE + "DisplayMultipleVersions";
	public static final String API_STORE_DISPLAY_COMMENTS = API_STORE + "DisplayComments";
	public static final String API_STORE_DISPLAY_RATINGS = API_STORE + "DisplayRatings";
 
    public static final String AUTH_MANAGER = "AuthManager.";
    public static final String AUTH_MANAGER_URL = AUTH_MANAGER + "ServerURL";
    public static final String AUTH_MANAGER_USERNAME = AUTH_MANAGER + "Username";
    public static final String AUTH_MANAGER_PASSWORD = AUTH_MANAGER + "Password";
    
    public static final String SELF_SIGN_UP = "SelfSignUp.";
    public static final String SELF_SIGN_UP_ENABLED = SELF_SIGN_UP + "Enabled";
    public static final String SELF_SIGN_UP_ROLE = SELF_SIGN_UP + "SubscriberRoleName";
    public static final String SELF_SIGN_UP_CREATE_ROLE = SELF_SIGN_UP + "CreateSubscriberRole";

    public static final String GLOBAL_API_PUBLISHER_ROLE = "globalAPIPublisher";
    public static final String STATUS_OBSERVERS = "StatusObservers.";
    public static final String OBSERVER = STATUS_OBSERVERS + "Observer";
    
    public static final String API_KEY_TYPE = "AM_KEY_TYPE";
    public static final String API_KEY_TYPE_PRODUCTION = "PRODUCTION";
    public static final String API_KEY_TYPE_SANDBOX = "SANDBOX";

    public static final String BILLING_AND_USAGE_CONFIGURATION = "EnableBillingAndUsage";
    
    public static final String DEFAULT_APPLICATION_NAME = "DefaultApplication";

    public static final QName POLICY_ELEMENT = new QName("http://schemas.xmlsoap.org/ws/2004/09/policy",
                      "Policy");
    public static final QName ASSERTION_ELEMENT = new QName("http://www.wso2.org/products/wso2commons/throttle",
            "MediatorThrottleAssertion");
    public static final QName THROTTLE_ID_ELEMENT = new QName("http://www.wso2.org/products/wso2commons/throttle",
            "ID");
    public static final String TIER_DESCRIPTION_PREFIX = "tier.desc.";
    
    public static final String TIER_MANAGEMENT = "TierManagement.";
    public static final String ENABLE_UNLIMITED_TIER = TIER_MANAGEMENT + "EnableUnlimitedTier";
    
    public static final String UNLIMITED_TIER = "Unlimited";
    public static final String UNLIMITED_TIER_DESC = "Allows unlimited requests";

    public static final String UNAUTHENTICATED_TIER = "Unauthenticated";
    
    public static final int AM_CREATOR_APIMGT_EXECUTION_ID = 200;
    public static final int AM_CREATOR_GOVERNANCE_EXECUTION_ID = 201;
    public static final int AM_PUBLISHER_APIMGT_EXECUTION_ID = 202;
    public static final QName THROTTLE_CONTROL_ELEMENT = new QName("http://www.wso2.org/products/wso2commons/throttle",
                        "Control");
    public static final QName THROTTLE_MAXIMUM_COUNT_ELEMENT = new QName("http://www.wso2"
            +".org/products/wso2commons/throttle", "MaximumCount");
    public static final QName THROTTLE_UNIT_TIME_ELEMENT = new QName("http://www.wso2"
            +".org/products/wso2commons/throttle", "UnitTime");

    public static final String TIER_DESC_NOT_AVAILABLE = "Tire Description is not available";
    
    public static final String AUTH_TYPE_DEFAULT = "DEFAULT";
    public static final String AUTH_TYPE_NONE = "NONE";
    public static final String AUTH_TYPE_USER = "USER";
    public static final String AUTH_TYPE_APP = "APP";

    public static final String REMOTE_ADDR = "REMOTE_ADDR";

    //TODO: move this to a common place (& Enum) to be accessible by all components
    public static class KeyValidationStatus {
        public static final int API_AUTH_GENERAL_ERROR       = 900900;
        public static final int API_AUTH_INVALID_CREDENTIALS = 900901;
        public static final int API_AUTH_MISSING_CREDENTIALS = 900902;
        public static final int API_AUTH_ACCESS_TOKEN_EXPIRED = 900903;
        public static final int API_AUTH_ACCESS_TOKEN_INACTIVE = 900904;
        public static final int API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE = 900905;
        public static final int API_AUTH_INCORRECT_API_RESOURCE = 900906;
    }

    public static final String EMAIL_DOMAIN_SEPARATOR = "@";

    public static final String EMAIL_DOMAIN_SEPARATOR_REPLACEMENT = "-AT-";


    //URI Authentication Schemes
    public static final String AUTH_NO_AUTHENTICATION = "None";
    public static final String AUTH_APPLICATION_LEVEL_TOKEN = "Application";
    public static final String AUTH_APPLICATION_USER_LEVEL_TOKEN = "Application_User";
    public static final String AUTH_APPLICATION_OR_USER_LEVEL_TOKEN = "Any";
    public static final String NO_MATCHING_AUTH_SCHEME = "noMatchedAuthScheme";

    public static final String EVERYONE_ROLE = "everyone";
    public static final String ANONYMOUS_ROLE = "wso2.anonymous.role";

    public static final String READ_ACTION = "2";
    public static final String WRITE_ACTION = "3";
    public static final String DELETE_ACTION = "4";
    public static final String PERMISSION_ENABLED = "1";
    public static final String PERMISSION_DISABLED = "0";
}
