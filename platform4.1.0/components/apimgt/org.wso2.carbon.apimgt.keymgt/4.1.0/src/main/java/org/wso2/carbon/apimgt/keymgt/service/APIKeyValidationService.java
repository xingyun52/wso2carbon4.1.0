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

import net.sf.jsr107cache.Cache;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.dao.ApiMgtDAO;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.apimgt.keymgt.APIKeyMgtException;
import org.wso2.carbon.apimgt.keymgt.util.APIKeyMgtDataHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.keymgt.util.APIKeyMgtUtil;
import org.wso2.carbon.apimgt.impl.APIConstants;

/**
 *
 */
public class APIKeyValidationService extends AbstractAdmin {
    private static final Log log = LogFactory.getLog(APIKeyValidationService.class);

    /**
     * Validates the access tokens issued for a particular user to access an API.
     *
     * @param context     Requested context
     * @param accessToken Provided access token
     * @return APIKeyValidationInfoDTO with authorization info and tier info if authorized. If it is not
     *         authorized, tier information will be <pre>null</pre>
     * @throws APIKeyMgtException Error occurred when accessing the underlying database or registry.
     */
    public APIKeyValidationInfoDTO validateKey(String context, String version, String accessToken,String requiredAuthenticationLevel)
            throws APIKeyMgtException, APIManagementException {
        Cache cache = PrivilegedCarbonContext.getCurrentContext(getAxisConfig()).getCache("keyCache");
        String cacheKey = accessToken + ":" + context + ":" + version;
        APIKeyValidationInfoDTO info;
        ApiMgtDAO ApiMgtDAO = new ApiMgtDAO();
        Boolean keyCacheEnabledGateway = APIKeyMgtDataHolder.getKeyCacheEnabledKeyMgt();
        //If gateway key cache enabled only we retrieve key validation info or JWT token form cache
        if (keyCacheEnabledGateway) {
            info = (APIKeyValidationInfoDTO) cache.get(cacheKey);
            //If key validation information is not null then only we proceed with cached object
            if (info != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Found cached access token for : " + cacheKey + " .Checking for expiration time.");
                }
                //check if token has expired
                boolean tokenExpired = APIKeyMgtUtil.hasAccessTokenExpired(info);
                if (!tokenExpired) {
                    //If key validation information is authorized then only we have to check for JWT token
                    //If key validation information is authorized and JWT cache disabled then only we use
                    //cached api key validation information and generate new JWT token
                    if (!APIKeyMgtDataHolder.getJWTCacheEnabledKeyMgt() && info.isAuthorized()) {
                        String JWTString;
                        if (info.getUserType().equalsIgnoreCase(APIConstants.ACCESS_TOKEN_USER_TYPE_APPLICATION)) {
                            JWTString = ApiMgtDAO.createJWTTokenString(context, version, info.getSubscriber(),
                                    info.getApplicationName(), info.getTier(), "null");
                        } else {
                            JWTString = ApiMgtDAO.createJWTTokenString(context, version, info.getSubscriber(),
                                    info.getApplicationName(), info.getTier(), info.getEndUserName());
                        }
                        info.setEndUserToken(JWTString);
                    }
                    return info;
                } else {
                    log.info("Token " + cacheKey + " expired.");
                }
            }
        }
        //If validation info is not cached creates fresh api key validation information object and returns it
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = ApiMgtDAO.validateKey(context, version, accessToken,requiredAuthenticationLevel);
        //If key validation information is not null and key validation enabled at keyMgt we put validation
        //information into cache
        if (apiKeyValidationInfoDTO != null && keyCacheEnabledGateway) {
            cache.put(cacheKey, apiKeyValidationInfoDTO);
        }
        return apiKeyValidationInfoDTO;
    }


}
