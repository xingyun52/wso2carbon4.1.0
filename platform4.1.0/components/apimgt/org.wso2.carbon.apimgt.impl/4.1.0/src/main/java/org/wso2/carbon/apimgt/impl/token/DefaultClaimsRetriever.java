/*
*Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.apimgt.impl.token;

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.ClaimCache;
import org.wso2.carbon.apimgt.impl.utils.ClaimCacheKey;
import org.wso2.carbon.apimgt.impl.utils.UserClaims;
import org.wso2.carbon.caching.core.CacheKey;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class is the default implementation of ClaimsRetriever.
 * It reads user claim values from the default carbon user store.
 * The user claims are encoded to the JWT in the natural order of the claimURIs.
 * To engage this class its fully qualified class name should be mentioned under
 * api-manager.xml -> APIConsumerAuthentication -> ClaimsRetrieverImplClass
 */
public class DefaultClaimsRetriever implements ClaimsRetriever {

    private String dialectURI = ClaimsRetriever.DEFAULT_DIALECT_URI;
    private ClaimCache claimsLocalCache;

    /**
     * Reads the DialectURI of the ClaimURIs to be retrieved from api-manager.xml ->
     * APIConsumerAuthentication -> ConsumerDialectURI.
     * If not configured it uses http://wso2.org/claims as default
     */
    @Override
    public void init() {
        dialectURI = ServiceReferenceHolder.getInstance().getAPIManagerConfigurationService().
                getAPIManagerConfiguration().getFirstProperty(CONSUMER_DIALECT_URI);
        claimsLocalCache = ClaimCache.getInstance();
        if (dialectURI == null) {
            dialectURI = ClaimsRetriever.DEFAULT_DIALECT_URI;
        }
    }

    @Override
    public SortedMap<String, String> getClaims(String endUserName) throws APIManagementException {
        SortedMap<String, String> claimValues;
        try {
            int tenantId = JWTGenerator.getTenantId(endUserName);
            //check in local cache
            String key = endUserName + ":" + tenantId;
            CacheKey cacheKey = new ClaimCacheKey(key);
            Object result = claimsLocalCache.getValueFromCache(cacheKey);
            if (result != null) {
                claimValues = ((UserClaims) result).getClaimValues();
            } else {
                ClaimManager claimManager = ServiceReferenceHolder.getInstance().getRealmService().
                        getTenantUserRealm(tenantId).getClaimManager();
                Claim[] claims = claimManager.getAllClaims(dialectURI);
                String[] claimURIs = claim_to_string(claims);
                UserStoreManager userStoreManager = ServiceReferenceHolder.getInstance().getRealmService().
                        getTenantUserRealm(tenantId).getUserStoreManager();
                claimValues = new TreeMap(userStoreManager.getUserClaimValues(endUserName, claimURIs, null));
                UserClaims userClaims = new UserClaims(claimValues);
                //add to cache
                claimsLocalCache.addToCache(cacheKey, userClaims);
            }
        } catch (UserStoreException e) {
            throw new APIManagementException("Error while retrieving user claim values from "
                    + "user store");
        }
        return claimValues;
    }

    /**
     * Always returns the ConsumerDialectURI configured in api-manager.xml
     */
    @Override
    public String getDialectURI(String endUserName) {
        return dialectURI;
    }

    /**
     * Helper method to convert array of <code>Claim</code> object to
     * array of <code>String</code> objects corresponding to the ClaimURI values.
     */
    private String[] claim_to_string(Claim[] claims) {
        String[] temp = new String[claims.length];
        for (int i = 0; i < claims.length; i++) {
            temp[i] = claims[i].getClaimUri();
        }
        return temp;
    }
}
