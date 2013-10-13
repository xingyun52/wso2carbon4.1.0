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

package org.wso2.carbon.apimgt.keymgt.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.keymgt.APIKeyMgtException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserStoreException;

import java.sql.Connection;

public class APIKeyMgtUtil {

    private static final Log log = LogFactory.getLog(APIKeyMgtUtil.class);

    public static String getTenantDomainFromTenantId(int tenantId) throws APIKeyMgtException {
        try {
            TenantManager tenantManager = APIKeyMgtDataHolder.getRealmService().getTenantManager();
            return tenantManager.getDomain(tenantId);
        } catch (UserStoreException e) {
            String errorMsg = "Error when getting the Tenant domain name for the given Tenant Id";
            log.error(errorMsg, e);
            throw new APIKeyMgtException(errorMsg, e);
        }
    }

    /**
     * Get a database connection instance from the Identity Persistence Manager
     * @return Database Connection
     * @throws APIKeyMgtException Error when getting an instance of the identity Persistence Manager
     */
    public static Connection getDBConnection() throws APIKeyMgtException {
        try {
            return IdentityDatabaseUtil.getDBConnection();
        } catch (IdentityException e) {
            String errMsg = "Error when getting a database connection from the Identity Persistence Manager";
            log.error(errMsg, e);
            throw new APIKeyMgtException(errMsg, e);
        }
    }

    /**
     * This returns API object for given APIIdentifier. Reads from registry entry for given APIIdentifier
     * creates API object
     *
     * @param identifier APIIdentifier object for the API
     * @return API object for given identifier
     * @throws APIManagementException on error in getting API artifact
     */
    public static API getAPI(APIIdentifier identifier) throws APIManagementException {
        String apiPath = APIUtil.getAPIPath(identifier);

        try {
            Registry registry = APIKeyMgtDataHolder.getRegistryService().getGovernanceSystemRegistry();
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                    APIConstants.API_KEY);
            Resource apiResource = registry.get(apiPath);
            String artifactId = apiResource.getUUID();
            if (artifactId == null) {
                throw new APIManagementException("artifact id is null for : " + apiPath);
            }
            GenericArtifact apiArtifact = artifactManager.getGenericArtifact(artifactId);
            return APIUtil.getAPI(apiArtifact, registry);

        } catch (RegistryException e) {
            return null;
        }
    }

    /**
     * validates if an accessToken has expired or not
     * @param accessTokenDO
     * @return
     */
    public static boolean hasAccessTokenExpired(APIKeyValidationInfoDTO accessTokenDO) {
        long timestampSkew;
        long currentTime;
        long validityPeriod = accessTokenDO.getValidityPeriod() * 1000;
        long issuedTime = accessTokenDO.getIssuedTime();
        //long issuedTime = accessTokenDO.getIssuedTime().getTime();

        timestampSkew = OAuthServerConfiguration.getInstance().
                getDefaultTimeStampSkewInSeconds() * 1000;
        currentTime = System.currentTimeMillis();

        //If the validity period is not an never expiring value
        if (validityPeriod != Long.MAX_VALUE) {
            //check the validity of cached OAuth2AccessToken Response
            if ((currentTime - timestampSkew) > (issuedTime + validityPeriod)) {
                accessTokenDO.setValidationStatus(
                        APIConstants.KeyValidationStatus.API_AUTH_ACCESS_TOKEN_EXPIRED);
                log.info("Token " + accessTokenDO.getEndUserToken() + " expired.");
                return true;
            }
        }


        return false;
    }
}
