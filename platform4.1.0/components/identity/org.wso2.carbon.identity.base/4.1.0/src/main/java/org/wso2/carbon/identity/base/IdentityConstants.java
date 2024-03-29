/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.identity.base;

/**
 * Common constants of the identity solution.
 */
public class IdentityConstants {

	private IdentityConstants() {
	}

	public static final String DEFULT_RESOURCES = "org.wso2.carbon.identity.core.resources";
	public static final String SELF_ISSUED_ISSUER = "http://schemas.xmlsoap.org/ws/2005/05/identity/issuer/self";
	public static final String PREFIX = "ic";
	public static final String NS = "http://schemas.xmlsoap.org/ws/2005/05/identity";
	public static final String OPENID_NS = "http://schema.openid.net/2007/05";
	public final static String NS_MSFT_ADDR = "http://schemas.microsoft.com/ws/2005/05/addressing/none";
	public static final String IDENTITY_ADDRESSING_NS = "http://schemas.xmlsoap.org/ws/2006/02/addressingidentity";

    public final static String CLAIM_TENANT_DOMAIN = "http://wso2.org/claims/tenant";
	public final static String CLAIM_PPID = NS
			+ "/claims/privatepersonalidentifier";
	
	public final static String CLAIM_OPENID = OPENID_NS + "/claims/identifier";

	public final static String PARAM_SUPPORTED_TOKEN_TYPES = "SupportedTokenTypes";
	public final static String PARAM_NOT_SUPPORTED_TOKEN_TYPES = "NotSupportedTokenTypes";

	public final static String PARAM_CARD_NAME = "CardName";
	public final static String PARAM_VALUE_CARD_NAME = "WSO2 Managed Card";
	public final static String PARAM_VALID_PERIOD = "ValidPeriod";
	public final static String PARAM_VALUE_VALID_PERIOD = "365";

	public final static String SAML10_URL = "urn:oasis:names:tc:SAML:1.0:assertion";
	public final static String SAML11_URL = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
	public final static String SAML20_URL = "urn:oasis:names:tc:SAML:2.0:assertion";
	public static String PPID_DISPLAY_VALUE = "Private personal identifier";
	public final static String CARD_IMAGE_PATH = "/card.jpg";
	public final static String PARAM_USE_SYMM_BINDING = "useSymmBinding";
	public final static String USER_VERIFICATION_PAGE = "/UserVerification.action";
	public final static String USER_VERIFICATION_PARAM = "confString";

	public final static String XML_TOKEN = "xmlToken";
	public final static String PROFILE_NAME = "profileName";
	public final static String PASSWORD = "oppassword";
	public final static String INFOCARD_LOGIN = "opinfocardlogin";
	public static final String USER_APPROVED = "userApproved";

	public final static String WSO2_IS_NS = "http://www.wso2.org/solutions/identity";
	public final static String RESOURCES = "org.wso2.solutions.identity.resources";
	public final static String INITIAL_CLAIMS_FILE_PATH = "conf/initial-claims.xml";
	public static final String PROPERTY_USER = "IdentityProvier.User";
	
	public static final String HTTPS = "https://";
	public static final String HTTPS_PORT = "Ports.HTTPS";
	public static final String HOST_NAME = "HostName";
	public static final String TRUE = "true";
	public static final String PHISHING_RESISTANCE = "phishingResistanceAuthentication";
	public static final String MULTI_FACTOR_AUTH = "multifactorlogin";
	public static final String PARAM_MAP = "parameterMap";
	public static final String DESTINATION_URL = "destinationUrl";
	public static final String FORM_REDIRECTION = "jsp/redirect.jsp";

	public final static String ISSUER_SELF = "Self";
	public final static String CARD_ISSUSER_LOG = "org.wso2.solutions.identity.card";
	public final static String TOKEN_ISSUSER_LOG = "org.wso2.solutions.identity.token";

	public static final String SERVICE_NAME_STS_UT = "sts-ut";
	public static final String SERVICE_NAME_STS_UT_SYMM = "sts-ut-symm";
	public static final String SERVICE_NAME_STS_IC = "sts-ic";
	public static final String SERVICE_NAME_STS_IC_SYMM = "sts-ic-symm";
	public static final String SERVICE_NAME_MEX_UT = "mex-ut";
	public static final String SERVICE_NAME_MEX_UT_SYMM = "mex-ut-symm";
	public static final String SERVICE_NAME_MEX_IC = "mex-ic";
	public static final String SERVICE_NAME_MEX_IC_SYMM = "mex-ic-symm";

	public static final String INFOCARD_DIALECT = "http://schemas.xmlsoap.org/ws/2005/05/identity";
	public static final String OPENID_SREG_DIALECT = "http://schema.openid.net/2007/05/claims";
	public static final String OPENID_AX_DIALECT = "http://axschema.org";

	// Authentication mechanism
	public static final int AUTH_TYPE_USERNAME_TOKEN = 1;
	public static final int AUTH_TYPE_KEBEROS_TICKET = 2;
	public static final int AUTH_TYPE_X509_CERTIFICATE = 3;
	public static final int AUTH_TYPE_SELF_ISSUED = 4;
	public static final String RP_USER_ROLE = "Rp_User_Role";
	public final static String PARAM_NAME_ALLOW_USER_REGISTRATION = "allowUserReg";
	public final static String PARAM_NAME_ENABLE_OPENID_LOGIN = "enableOpenIDLogin";

	public final static String IDENTITY_DEFAULT_ROLE = "identity";
    public final static String DEFAULT_SUPER_TENAT = "identity.cloud.wso2.com";

	/**
	 * Server Configuration data retrieval Strings.
	 */
	public static class ServerConfig {
		
		public final static String USER_TRUSTED_RP_STORE_LOCATION = "Security.UserTrustedRPStore.Location";
		public final static String USER_TRUSTED_RP_STORE_PASSWORD = "Security.UserTrustedRPStore.Password";
		public final static String USER_TRUSTED_RP_STORE_TYPE = "Security.UserTrustedRPStore.Type";
		public final static String USER_TRUSTED_RP_KEY_PASSWORD = "Security.UserTrustedRPStore.KeyPassword";

		public final static String USER_SSO_STORE_LOCATION = "Security.UserSSOStore.Location";
		public final static String USER_SSO_STORE_PASSWORD = "Security.UserSSOStore.Password";
		public final static String USER_SSO_STORE_TYPE = "Security.UserSSOStore.Type";
		public final static String USER_SSO_KEY_PASSWORD = "Security.UserSSOStore.KeyPassword";
        
        public final static String OPENID_SERVER_URL = "OpenID.OpenIDServerUrl";
		public final static String OPENID_USER_PATTERN = "OpenID.OpenIDUserPattern";
		public final static String OPENID_SKIP_USER_CONSENT = "OpenID.OpenIDSkipUserConsent";
		public final static String OPENID_REMEMBER_ME_EXPIRY = "OpenID.OpenIDRememberMeExpiry";
		public final static String OPENID_USE_MULTIFACTOR_AUTHENTICATION = "OpenID.UseMultifactorAuthentication";
		public final static String OPENID_DISABLE_DUMB_MODE = "OpenID.DisableOpenIDDumbMode";

		public static final String ISSUER_POLICY = "Identity.IssuerPolicy";
		public static final String TOKEN_VALIDATE_POLICY = "Identity.TokenValidationPolicy";
		public static final String BLACK_LIST = "Identity.BlackList";
		public static final String WHITE_LIST = "Identity.WhiteList";
		public static final String SYSTEM_KEY_STORE_PASS = "Identity.System.StorePass";
		public static final String SYSTEM_KEY_STORE = "Identity.System.KeyStore";

		// Location of the identity provider main key store
		public final static String IDP_STORE_LOCATION = "Security.KeyStore.Location";

		// Password of the identity provider main key store
		public final static String IDP_STORE_PASSWORD = "Security.KeyStore.Password";

		// Store type of the identity provider main key store
		public final static String IDP_STORE_TYPE = "Security.KeyStore.Type";

		// Location of the key store used to store users' personal certificates
		public final static String USER_PERSONAL_STORE_LOCATION = "Security.UserPersonalCeritificateStore.Location";

		// Password of the key store used to store users' personal certificates
		public final static String USER_PERSONAL_STORE_PASSWORD = "Security.UserPersonalCeritificateStore.Password";

		// Type of the key store used to store users' personal certificates
		public final static String USER_PERSONAL_STORE_TYPE = "Security.UserPersonalCeritificateStore.Type";

		public final static String USER_PERSONAL_KEY_PASSWORD = "Security.UserPersonalCeritificateStore.KeyPassword";

        //XMPP Settings for multifactor authentication

        public final static String XMPP_SETTINGS_PROVIDER = "MultifactorAuthentication.XMPPSettings.XMPPConfig.XMPPProvider";

        public final static String XMPP_SETTINGS_SERVER = "MultifactorAuthentication.XMPPSettings.XMPPConfig.XMPPServer";

        public final static String XMPP_SETTINGS_PORT = "MultifactorAuthentication.XMPPSettings.XMPPConfig.XMPPPort";

        public final static String XMPP_SETTINGS_EXT = "MultifactorAuthentication.XMPPSettings.XMPPConfig.XMPPExt";

        public final static String XMPP_SETTINGS_USERNAME = "MultifactorAuthentication.XMPPSettings.XMPPConfig.XMPPUserName";

        public final static String XMPP_SETTINGS_PASSWORD = "MultifactorAuthentication.XMPPSettings.XMPPConfig.XMPPPassword";

        //SAML SSO Service config
        public final static String SSO_IDP_URL = "SSOService.IdentityProviderURL";
        public final static String SSO_ATTRIB_CLAIM_DIALECT = "SSOService.AttributesClaimDialect";
        public static final String SINGLE_LOGOUT_RETRY_COUNT = "SSOService.SingleLogoutRetryCount";
        public static final String SINGLE_LOGOUT_RETRY_INTERVAL = "SSOService.SingleLogoutRetryInterval";
        public static final String SSO_TENANT_PARTITIONING_ENABLED = "SSOService.TenantPartitioningEnabled";

        //Identity Persistence Manager
        public static final String SKIP_DB_SCHEMA_CREATION = "JDBCPersistenceManager.SkipDBSchemaCreation";
    }

	/**
	 * Local names of the identity provider constants
	 */
	public static class LocalNames {
		public static final String REQUESTED_DISPLAY_TOKEN = "RequestedDisplayToken";
		public static final String REQUEST_DISPLAY_TOKEN = "RequestDisplayToken";
		public static final String DISPLAY_TOKEN = "DisplayToken";
		public static final String DISPLAY_CLAIM = "DisplayClaim";
		public static final String DISPLAY_TAG = "DisplayTag";
		public static final String DISPLAY_VALUE = "DisplayValue";
		public static final String IDENTITY_CLAIM = "Claim";
		public static final String IDENTITY_CLAIM_TYPE = "ClaimType";
		public static final String INFO_CARD_REFERENCE = "InformationCardReference";
		public static final String CARD_ID = "CardId";
		public final static String SELFISSUED_AUTHENTICATE = "SelfIssuedAuthenticate";
		public final static String USERNAME_PASSWORD_AUTHENTICATE = "UserNamePasswordAuthenticate";
		public final static String KEBEROSV5_AUTHENTICATE = "KerberosV5Authenticate";
		public final static String X509V3_AUTNENTICATE = "X509V3Authenticate";
		public final static String IDENTITY = "Identity";
		public final static String OPEN_ID_TOKEN = "OpenIDToken";
	}

	/**
	 * Common constants related to OpenID.
	 */
	public static class OpenId {

		public final static String NS = "http://schema.openid.net";
		public final static String OPENID_URL = "http://specs.openid.net/auth/2.0";
		public final static String ATTR_MODE = "openid.mode";
		public final static String ATTR_IDENTITY = "openid.identity";
		public final static String ATTR_RESPONSE_NONCE = "openid.response_nonce";
		public final static String ATTR_OP_ENDPOINT = "openid.op_endpoint";
		public final static String ATTR_NS = "openid.ns";
		public final static String ATTR_CLAIM_ID = "openid.claimed_id";
		public final static String ATTR_RETURN_TO = "openid.return_to";
		public final static String ATTR_ASSOC_HANDLE = "openid.assoc_handle";
		public final static String ATTR_SIGNED = "openid.signed";
		public final static String ATTR_SIG = "openid.sig";
		public final static String OPENID_IDENTIFIER = "openid_identifier";
		public final static String ASSOCIATE = "associate";
		public final static String CHECKID_SETUP = "checkid_setup";
		public final static String CHECKID_IMMEDIATE = "checkid_immediate";
		public final static String CHECK_AUTHENTICATION = "check_authentication";
		public final static String DISC = "openid-disc";
		public static final String PREFIX = "openid";
		public final static String ASSERTION = "openidAssertion";
		public final static String COMPLETE = "complete";
		public final static String ONLY_ONCE = "Only Once";
		public final static String ONCE = "once";
		public final static String ALWAYS = "always";
		public final static String DENY = "Deny";
		public final static String ACTION = "_action";
		public final static String OPENID_RESPONSE = "id_res";
		public static final String AUTHENTICATED_AND_APPROVED = "authenticatedAndApproved";
		public final static String CANCEL = "cancel";
		public final static String FALSE = "false";
		public final static String PARAM_LIST = "parameterlist";
		public final static String PASSWORD = "password";
		public static final String SERVICE_NAME_STS_OPENID = "sts-openid-ut";
		public static final String SERVICE_NAME_MEX_OPENID = "mex-openid-ut";
		public static final String SERVICE_NAME_MEX_IC_OPENID = "mex-openid-ic";
		public static final String SERVICE_NAME_STS_IC_OPENID = "sts-openid-ic";

		public static final String SIMPLE_REGISTRATION = "sreg";
		public static final String ATTRIBUTE_EXCHANGE = "ax";
		public static final String PAPE = "pape";

		public static class PapeAttributes {

			public final static String AUTH_POLICIES = "auth_policies";
			public final static String NIST_AUTH_LEVEL = "nist_auth_level";
			public final static String AUTH_AGE = "auth_age";
			public final static String PHISHING_RESISTANCE = "http://schemas.openid.net/pape/policies/2007/06/phishing-resistant";
			public final static String MULTI_FACTOR = "http://schemas.openid.net/pape/policies/2007/06/multi-factor";
			public final static String MULTI_FACTOR_PHYSICAL = "http://schemas.openid.net/pape/policies/2007/06/multi-factor-physical";
            public final static String XMPP_BASED_MULTIFACTOR_AUTH ="xmpp_based_multifactor_auth";
            public final static String INFOCARD_BASED_MULTIFACTOR_AUTH = "infocard_based_multifactor_auth";
		}

		public static class SimpleRegAttributes {

			// As per the OpenID Simple Registration Extension 1.0 specification
			// fields below should
			// be included in the Identity Provider's response when
			// "openid.mode" is "id_res"

			public final static String NS_SREG = "http://openid.net/sreg/1.0";
			public final static String NS_SREG_1 = "http://openid.net/extensions/sreg/1.1";
			public final static String SREG = "openid.sreg.";
			public final static String OP_SREG = "openid.ns.sreg";
		}

		public static class ExchangeAttributes extends SimpleRegAttributes {

			public final static String NS = "http://axschema.org";
			public final static String NS_AX = "http://openid.net/srv/ax/1.0";
			public final static String EXT = "openid.ns.ext1";
			public final static String MODE = "openid.ext1.mode";
			public final static String TYPE = "openid.ext1.type.";
			public final static String VALUE = "openid.ext1.value.";
			public final static String FETCH_RESPONSE = "fetch_response";
		}
	}
}
