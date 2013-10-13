/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.identity.sso.saml.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensaml.saml2.core.Response;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.model.SAMLSSOServiceProviderDO;
import org.wso2.carbon.identity.core.persistence.IdentityPersistenceManager;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.builders.ErrorResponseBuilder;
import org.wso2.carbon.identity.sso.saml.builders.ResponseBuilder;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.dto.SAMLSSORespDTO;
import org.wso2.carbon.identity.sso.saml.session.SSOSessionPersistenceManager;
import org.wso2.carbon.identity.sso.saml.session.SessionInfoData;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class AuthnRequestProcessor {

    private static Log log = LogFactory.getLog(AuthnRequestProcessor.class);

	public SAMLSSORespDTO process(SAMLSSOAuthnReqDTO authnReqDTO, String sessionId,
	                              boolean isAuthencated, String authMode) throws Exception {
		try {
			SAMLSSOServiceProviderDO serviceProviderConfigs = getServiceProviderConfig(authnReqDTO);
			if (serviceProviderConfigs == null) {
				String msg =
				             "A Service Provider with the Issuer '" + authnReqDTO.getIssuer() +
				                     "' is not registered." +
				                     " Service Provider should be registered in advance.";
				log.warn(msg);
				return buildErrorResponse(authnReqDTO.getId(),
				                          SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, msg);
			}
			// reading the service provider configs
			populateServiceProviderConfigs(serviceProviderConfigs, authnReqDTO);

			// validate the signature
			if (authnReqDTO.getCertAlias() != null) {
				boolean isSignatureValid = SAMLSSOUtil.validateAuthnRequestSignature(authnReqDTO);
				                          
				if (!isSignatureValid) {
					String msg = "Signature Validation Failed for the SAML Assertion.";
					log.warn(msg);
					return buildErrorResponse(authnReqDTO.getId(),
					                          SAMLSSOConstants.StatusCodes.REQUESTOR_ERROR, msg);
				}
			}

			// if subject is specified in AuthnRequest only that user should be
			// allowed to logged-in
			if (authnReqDTO.getSubject() != null && authnReqDTO.getUsername() != null) {
				if (!authnReqDTO.getUsername().equals(authnReqDTO.getSubject())) {
					String msg = "Provided username does not match with the requested subject";
					log.warn(msg);
					return buildErrorResponse(authnReqDTO.getId(),
					                          SAMLSSOConstants.StatusCodes.AUTHN_FAILURE, msg);
				}
			}

			// persist the session
			SSOSessionPersistenceManager sessionPersistenceManager =
			                                                         SSOSessionPersistenceManager.getPersistenceManager();

			// authenticate the user, if required
			if (!isAuthencated && authMode.equals(SAMLSSOConstants.AuthnModes.USERNAME_PASSWORD)) {
				RealmService realmService = SAMLSSOUtil.getRealmService();
				TenantManager tenantManager = realmService.getTenantManager();
				String tenantDomain = MultitenantUtils.getTenantDomain(authnReqDTO.getUsername());
				int tenantId = tenantManager.getTenantId(tenantDomain);
				if (tenantId > 0) {
					boolean isTenantActive = tenantManager.isTenantActive(tenantId);
					if (!isTenantActive) {
						log.warn("Unsuccessful login attempt from the tenant : " + tenantDomain);
						String errorMsg = "login.fail.inactive.tenant";
						SAMLSSORespDTO errorResp =
						                           buildErrorResponse(authnReqDTO.getId(),
						                                              SAMLSSOConstants.StatusCodes.AUTHN_FAILURE,
						                                              errorMsg);
						errorResp.setErrorMsg(errorMsg);
						errorResp.setLoginPageURL(authnReqDTO.getLoginPageURL());
						return errorResp;
					}
				}

				if (!authenticate(authnReqDTO.getUsername(), authnReqDTO.getPassword())) {
					log.warn("Authentication Failure, invalid username or password.");
					String errorMsg = "login.fail.message";
					SAMLSSORespDTO errorResp =
					                           buildErrorResponse(authnReqDTO.getId(),
					                                              SAMLSSOConstants.StatusCodes.AUTHN_FAILURE,
					                                              errorMsg);
					errorResp.setErrorMsg(errorMsg);
					errorResp.setLoginPageURL(authnReqDTO.getLoginPageURL());
					return errorResp;
				}

				SAMLSSOServiceProviderDO spDO = new SAMLSSOServiceProviderDO();
				spDO.setIssuer(authnReqDTO.getIssuer());
				spDO.setAssertionConsumerUrl(authnReqDTO.getAssertionConsumerURL());
				spDO.setCertAlias(authnReqDTO.getCertAlias());
				spDO.setLogoutURL(authnReqDTO.getLogoutURL());
				sessionPersistenceManager.persistSession(sessionId, authnReqDTO.getUsername(),
				                                         spDO, authnReqDTO.getRpSessionId());
			}

			if (isAuthencated && authMode.equals(SAMLSSOConstants.AuthnModes.USERNAME_PASSWORD)) {
				SessionInfoData sessionInfo = sessionPersistenceManager.getSessionInfo(sessionId);
				authnReqDTO.setUsername(sessionInfo.getSubject());
				sessionPersistenceManager.persistSession(sessionId, authnReqDTO.getIssuer(),
				                                         authnReqDTO.getAssertionConsumerURL(),
				                                         authnReqDTO.getRpSessionId());
			}

			if (isAuthencated && authMode.equals(SAMLSSOConstants.AuthnModes.OPENID)) {
				SAMLSSOServiceProviderDO spDO = new SAMLSSOServiceProviderDO();
				spDO.setIssuer(authnReqDTO.getIssuer());
				spDO.setAssertionConsumerUrl(authnReqDTO.getAssertionConsumerURL());
				spDO.setCertAlias(authnReqDTO.getCertAlias());
				spDO.setLogoutURL(authnReqDTO.getLogoutURL());
				sessionPersistenceManager.persistSession(sessionId, authnReqDTO.getUsername(),
				                                         spDO, authnReqDTO.getRpSessionId());
			}

			// Build the response for the successful scenario
			ResponseBuilder respBuilder = new ResponseBuilder();
			Response response = respBuilder.buildResponse(authnReqDTO, sessionId);
			SAMLSSORespDTO samlssoRespDTO = new SAMLSSORespDTO();
			samlssoRespDTO.setRespString(SAMLSSOUtil.encode(SAMLSSOUtil.marshall(response)));
			samlssoRespDTO.setSessionEstablished(true);
			samlssoRespDTO.setAssertionConsumerURL(authnReqDTO.getAssertionConsumerURL());
			samlssoRespDTO.setLoginPageURL(authnReqDTO.getLoginPageURL());
			samlssoRespDTO.setSubject(authnReqDTO.getUsername());

			return samlssoRespDTO;
		} catch (Exception e) {
			log.error("Error processing the authentication request", e);
			SAMLSSORespDTO errorResp =
			                           buildErrorResponse(authnReqDTO.getId(),
			                                              SAMLSSOConstants.StatusCodes.AUTHN_FAILURE,
			                                              "Authentication Failure, invalid username or password.");
			errorResp.setLoginPageURL(authnReqDTO.getLoginPageURL());
			return errorResp;
		}
	}


	/**
	 * Returns the configured service provider configurations. The
	 * configurations are taken from the user registry or from the
	 * sso-idp-config.xml configuration file. In Stratos deployment the
	 * configurations are read from the sso-idp-config.xml file.
	 * 
	 * @param authnReqDTO
	 * @return
	 * @throws IdentityException
	 */
	private SAMLSSOServiceProviderDO getServiceProviderConfig(SAMLSSOAuthnReqDTO authnReqDTO)
	                                                                                         throws IdentityException {
		try {
			SSOServiceProviderConfigManager stratosIdpConfigManager =
			                                                          SSOServiceProviderConfigManager.getInstance();
			SAMLSSOServiceProviderDO ssoIdpConfigs =
			                                         stratosIdpConfigManager.getServiceProvider(authnReqDTO.getIssuer());
			if (ssoIdpConfigs == null) {
				IdentityPersistenceManager persistenceManager =
				                                                IdentityPersistenceManager.getPersistanceManager();
				UserRegistry registry =
				                        AnonymousSessionUtil.getSystemRegistryByUserName(SAMLSSOUtil.getRegistryService(),
				                                                                         SAMLSSOUtil.getRealmService(),
				                                                                         authnReqDTO.getUsername());
				ssoIdpConfigs =
				                persistenceManager.getServiceProvider(registry,
				                                                      authnReqDTO.getIssuer());
				authnReqDTO.setStratosDeployment(false); // not stratos
			} else {
				authnReqDTO.setStratosDeployment(true); // stratos deployment
			}
			return ssoIdpConfigs;
		} catch (Exception e) {
			log.error("Error while reading Service Provider configurations", e);
			throw new IdentityException("Error while reading Service Provider configurations");
		}
	}

	/**
	 * Populate the configurations of the service provider
	 * 
	 * @param ssoIdpConfigs
	 * @param authnReqDTO
	 * @throws IdentityException
	 */
	private void populateServiceProviderConfigs(SAMLSSOServiceProviderDO ssoIdpConfigs,
	                                            SAMLSSOAuthnReqDTO authnReqDTO)
	                                                                           throws IdentityException {
		String acsUrl = authnReqDTO.getAssertionConsumerURL();
		if (acsUrl != null && !ssoIdpConfigs.getAssertionConsumerUrl().equals(acsUrl)) {
			log.error("ALERT: Invalid Assertion Consumer URL value '" + acsUrl + "' in the " +
			          "AuthnRequest message from  the issuer '" + ssoIdpConfigs.getIssuer() +
			          "'. Possibly " + "an attempt for a spoofing attack");
			throw new IdentityException("Invalid Assertion Consumer URL value '" + acsUrl +
			                            "' in the " + "AuthnRequest");
		}

		authnReqDTO.setAssertionConsumerURL(ssoIdpConfigs.getAssertionConsumerUrl());
		authnReqDTO.setLoginPageURL(ssoIdpConfigs.getLoginPageURL());
		authnReqDTO.setCertAlias(ssoIdpConfigs.getCertAlias());
		authnReqDTO.setUseFullyQualifiedUsernameAsSubject(ssoIdpConfigs.isUseFullyQualifiedUsername());
		authnReqDTO.setDoSingleLogout(ssoIdpConfigs.isDoSingleLogout());
		authnReqDTO.setLogoutURL(ssoIdpConfigs.getLogoutURL());
		authnReqDTO.setDoSignAssertions(ssoIdpConfigs.isDoSignAssertions());
		authnReqDTO.setRequestedClaims((ssoIdpConfigs.getRequestedClaims()));
	}
	
	/**
	 * Creates a <code>SAMLSSOAuthnReqDTO</code> object using the
	 * <code>SAMLSSOReqValidationResponseDTO</code> object parameters. Then the
	 * <code>SAMLSSOAuthnReqDTO</code> is fed into the process method for
	 * further processing of the request message.
	 * 
	 * @param valiationDTO
	 * @param sessionId
	 * @param rpSessionId
	 * @param authMode
	 * @return
	 * @throws Exception
	 */
	public SAMLSSOReqValidationResponseDTO process(SAMLSSOReqValidationResponseDTO valiationDTO,
	                                               String sessionId, String rpSessionId,
	                                               String authMode) throws Exception {
		SAMLSSOAuthnReqDTO authReqDTO = new SAMLSSOAuthnReqDTO();
		authReqDTO.setIssuer(valiationDTO.getIssuer());
		authReqDTO.setAssertionConsumerURL(valiationDTO.getAssertionConsumerURL());
		authReqDTO.setSubject(valiationDTO.getSubject());
		authReqDTO.setId(valiationDTO.getId());
		authReqDTO.setRpSessionId(rpSessionId);
		authReqDTO.setRequestMessageString(valiationDTO.getRequestMessageString());
		authReqDTO.setQueryString(valiationDTO.getQueryString());

		if (authMode.equals(SAMLSSOConstants.AuthnModes.USERNAME_PASSWORD)) {
			SSOSessionPersistenceManager sessionPersistenceManager =
			                                                         SSOSessionPersistenceManager.getPersistenceManager();
			SessionInfoData sessionInfo = sessionPersistenceManager.getSessionInfo(sessionId);
			authReqDTO.setUsername(sessionInfo.getSubject());
		} else {
			authReqDTO.setUsername(valiationDTO.getSubject());
		}

		SAMLSSOReqValidationResponseDTO responseDTO = new SAMLSSOReqValidationResponseDTO();
		SAMLSSORespDTO respDTO = process(authReqDTO, sessionId, true, authMode);
		
		responseDTO.setValid(true);
		responseDTO.setResponse(respDTO.getRespString());
		responseDTO.setAssertionConsumerURL(respDTO.getAssertionConsumerURL());
		responseDTO.setLoginPageURL(respDTO.getLoginPageURL());
		responseDTO.setSubject(respDTO.getSubject());

		return responseDTO;
	}

	/**
	 * 
	 * @param id
	 * @param status
	 * @param statMsg
	 * @return
	 * @throws Exception
	 */
    private SAMLSSORespDTO buildErrorResponse(String id, String status,
                                              String statMsg) throws Exception {
        SAMLSSORespDTO samlSSORespDTO = new SAMLSSORespDTO();
        ErrorResponseBuilder errRespBuilder = new ErrorResponseBuilder();
        Response resp = errRespBuilder.buildResponse(id, status, statMsg);
        samlSSORespDTO.setRespString(SAMLSSOUtil.encode(SAMLSSOUtil.marshall(resp)));
        samlSSORespDTO.setSessionEstablished(false);
        return samlSSORespDTO;
    }

    private boolean authenticate(String username, String password) throws IdentityException {
        boolean isAuthenticated = false;
        try {
            UserRealm realm = AnonymousSessionUtil.getRealmByUserName(
                    SAMLSSOUtil.getRegistryService(), SAMLSSOUtil.getRealmService(), username);

            if (realm == null) {
                log.warn("Realm creation failed. Tenant may be inactive or invalid.");
                return false;
            }

            UserStoreManager userStoreManager = realm.getUserStoreManager();

//            //update the permission tree before authentication
//            String tenantDomain = MultitenantUtils.getTenantDomain(username);
//            int tenantId = SAMLSSOUtil.getRealmService().getTenantManager().getTenantId(tenantDomain);
//            PermissionUpdateUtil.updatePermissionTree(tenantId);

            // Check the authentication
            isAuthenticated = userStoreManager.authenticate(
                    MultitenantUtils.getTenantAwareUsername(username), password);
            if (!isAuthenticated) {
                if (log.isDebugEnabled()) {
                    log.debug("user authentication failed due to invalid credentials.");
                }
                return false;
            }

            // Check the authorization
            boolean isAuthorized = realm.getAuthorizationManager().
                    isUserAuthorized(MultitenantUtils.getTenantAwareUsername(username),
                            "/permission/admin/login",
                            CarbonConstants.UI_PERMISSION_ACTION);
            if (!isAuthorized) {
                if (log.isDebugEnabled()) {
                    log.debug("Authorization Failure when performing log-in action");
                }
                return false;
            }
            if (log.isDebugEnabled()) {
                log.debug("User is successfully authenticated.");
            }
            return true;
        } catch (Exception e) {
            String msg = "Error obtaining user realm for authenticating the user";
            log.error(msg, e);
            throw new IdentityException(msg, e);
        }
    }
}
