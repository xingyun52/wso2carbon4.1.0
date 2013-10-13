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
package org.wso2.carbon.apimgt.keymgt.service.thrift;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TException;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.thrift.authentication.ThriftAuthenticatorService;
import org.wso2.carbon.utils.ThriftSession;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;

public class APIKeyValidationServiceImpl extends AbstractAdmin
        implements APIKeyValidationService.Iface {
    private static Log log = LogFactory.getLog(APIKeyValidationServiceImpl.class);
    /*Handler to ThriftAuthenticatorService which handles authentication to admin services.*/
    private static ThriftAuthenticatorService thriftAuthenticatorService;
    /*Handler to actual entitlement service which is going to be wrapped by thrift interface*/
    private static org.wso2.carbon.apimgt.keymgt.service.APIKeyValidationService apiKeyValidationService;

    /**
     * Init the AuthenticationService handler to be used for authentication.
     */
    public static void init(ThriftAuthenticatorService authenticatorService) {
        thriftAuthenticatorService = authenticatorService;
        apiKeyValidationService = new org.wso2.carbon.apimgt.keymgt.service.APIKeyValidationService();

    }

    /**
     * CarbonContextHolderBase is thread local. So we need to populate it with the one created
     * at user authentication.
     *
     * @param authSession
     */
    private void populateCurrentCarbonContextFromAuthSession(
            PrivilegedCarbonContext carbonContextHolder, ThriftSession authSession) {

        //read parameters from it and set it in current carbon context for this thread
        PrivilegedCarbonContext storedCarbonCtxHolder = (PrivilegedCarbonContext)
                authSession.getSessionCarbonContextHolder();

        carbonContextHolder.setUsername(storedCarbonCtxHolder.getUsername());
        carbonContextHolder.setTenantDomain(storedCarbonCtxHolder.getTenantDomain());
        carbonContextHolder.setTenantId(storedCarbonCtxHolder.getTenantId());
        carbonContextHolder.setRegistry(RegistryType.LOCAL_REPOSITORY,
                storedCarbonCtxHolder.getRegistry(RegistryType.LOCAL_REPOSITORY));
        carbonContextHolder.setRegistry(RegistryType.SYSTEM_CONFIGURATION,
                storedCarbonCtxHolder.getRegistry(RegistryType.SYSTEM_CONFIGURATION));
        carbonContextHolder.setRegistry(RegistryType.SYSTEM_GOVERNANCE,
                storedCarbonCtxHolder.getRegistry(RegistryType.SYSTEM_GOVERNANCE));
        carbonContextHolder.setRegistry(RegistryType.USER_CONFIGURATION,
                storedCarbonCtxHolder.getRegistry(RegistryType.USER_CONFIGURATION));
        carbonContextHolder.setRegistry(RegistryType.USER_GOVERNANCE,
                storedCarbonCtxHolder.getRegistry(RegistryType.USER_GOVERNANCE));
        carbonContextHolder.setUserRealm(storedCarbonCtxHolder.getUserRealm());
    }

    public APIKeyValidationInfoDTO validateKey(String context, String version, String accessToken,
                                               String sessionId,String requiredAuthenticationLevel )
            throws APIKeyMgtException, APIManagementException, TException {
        APIKeyValidationInfoDTO thriftKeyValidationInfoDTO = null;
        try {
            if (thriftAuthenticatorService != null && apiKeyValidationService != null) {

                if (thriftAuthenticatorService.isAuthenticated(sessionId)) {

                    //obtain the thrift session for this session id
                    ThriftSession currentSession = thriftAuthenticatorService.getSessionInfo(sessionId);

                    //obtain a dummy carbon context holder
                    PrivilegedCarbonContext carbonContextHolder = PrivilegedCarbonContext.getCurrentContext();


                    /*start tenant flow to stack up any existing carbon context holder base,
                    and initialize a raw one*/
                     PrivilegedCarbonContext.startTenantFlow();

                    try {

                        // need to populate current carbon context from the one created at
                        // authentication
                        populateCurrentCarbonContextFromAuthSession(carbonContextHolder,
                                                                    currentSession);

                        org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO keyValidationInfoDTO =
                                apiKeyValidationService.validateKey(context, version, accessToken,requiredAuthenticationLevel);

                        thriftKeyValidationInfoDTO = new APIKeyValidationInfoDTO();
                        thriftKeyValidationInfoDTO.setAuthorized(keyValidationInfoDTO.isAuthorized());
                        thriftKeyValidationInfoDTO.setSubscriber(keyValidationInfoDTO.getSubscriber());
                        thriftKeyValidationInfoDTO.setTier(keyValidationInfoDTO.getTier());
                        thriftKeyValidationInfoDTO.setType(keyValidationInfoDTO.getType());
                        thriftKeyValidationInfoDTO.setEndUserToken(keyValidationInfoDTO.getEndUserToken());
                        thriftKeyValidationInfoDTO.setEndUserName(keyValidationInfoDTO.getEndUserName());
                        thriftKeyValidationInfoDTO.setApplicationName(keyValidationInfoDTO.getApplicationName());
                        thriftKeyValidationInfoDTO.setValidationStatus(keyValidationInfoDTO.getValidationStatus());
                        thriftKeyValidationInfoDTO.setApplicationId(keyValidationInfoDTO.getApplicationId());
                        thriftKeyValidationInfoDTO.setApplicationTier(keyValidationInfoDTO.getApplicationTier());
                    } finally {
                         PrivilegedCarbonContext.endTenantFlow();
                    }

                } else {
                    String authErrorMsg = "Invalid session id for thrift authenticator.";
                    log.warn(authErrorMsg);
                    throw new APIKeyMgtException(authErrorMsg);
                }

            } else {
                String initErrorMsg = "Thrift Authenticator or APIKeyValidationService is not initialized.";
                log.error(initErrorMsg);
                throw new APIKeyMgtException(initErrorMsg);
            }

        } catch (org.wso2.carbon.apimgt.keymgt.APIKeyMgtException e) {
            log.error("Error in invoking validate key via thrift..");
            throw new org.wso2.carbon.apimgt.keymgt.service.thrift.APIKeyMgtException(e.getMessage());
        } catch (org.wso2.carbon.apimgt.api.APIManagementException e) {
            log.error("Error in invoking validate key via thrift..");
            throw new APIManagementException(e.getMessage());
        }
        return thriftKeyValidationInfoDTO;
    }
}
