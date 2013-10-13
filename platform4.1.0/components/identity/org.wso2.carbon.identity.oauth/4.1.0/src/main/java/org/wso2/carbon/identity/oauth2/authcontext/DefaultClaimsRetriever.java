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

package org.wso2.carbon.identity.oauth2.authcontext;

import org.wso2.carbon.caching.core.CacheKey;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.internal.OAuthComponentServiceHolder;
import org.wso2.carbon.identity.oauth.util.ClaimCache;
import org.wso2.carbon.identity.oauth.util.ClaimCacheKey;
import org.wso2.carbon.identity.oauth.util.UserClaims;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class is the default implementation of ClaimsRetriever.
 * It reads user claim values from the default carbon user store.
 * The user claims are encoded to the token in the natural order of the claimURIs.
 * To engage this class its fully qualified class name should be mentioned under
 * identity.xml -> OAuth -> TokenGeneration -> ClaimsRetrieverImplClass
 */
public class DefaultClaimsRetriever implements ClaimsRetriever {

    public static final String DEFAULT_DIALECT_URI = "http://wso2.org/claims";

    private ClaimCache claimsLocalCache;
    private String dialectURI;

    /**
     * Reads the DialectURI of the ClaimURIs to be retrieved from identity.xml ->
     * OAuth -> TokenGeneration -> ConsumerDialectURI.
     * If not configured it uses http://wso2.org/claims as default
     */
    @Override
    public void init() {
        dialectURI = OAuthServerConfiguration.getInstance().getConsumerDialectURI();
        if (dialectURI == null) {
            dialectURI = DEFAULT_DIALECT_URI;
        }
        claimsLocalCache = ClaimCache.getInstance();
    }

    @Override
    public SortedMap<String, String> getClaims(String endUserName) throws IdentityOAuth2Exception {
        SortedMap<String, String> claimValues;
        try {
            int tenantId = JWTTokenGenerator.getTenantId(endUserName);
            //check in local cache
            String key = endUserName + ":" + tenantId;
            CacheKey cacheKey = new ClaimCacheKey(key);
            Object result = claimsLocalCache.getValueFromCache(cacheKey);
            if (result != null) {
                claimValues = ((UserClaims) result).getClaimValues();
            } else {
                ClaimManager claimManager = OAuthComponentServiceHolder.getRealmService().
                        getTenantUserRealm(tenantId).getClaimManager();
                Claim[] claims = claimManager.getAllClaims(dialectURI);
                String[] claimURIs = claim_to_string(claims);
                UserStoreManager userStoreManager = OAuthComponentServiceHolder.getRealmService().
                        getTenantUserRealm(tenantId).getUserStoreManager();
                claimValues = new TreeMap(userStoreManager.getUserClaimValues(endUserName, claimURIs, null));
                UserClaims userClaims = new UserClaims(claimValues);
                //add to cache
                claimsLocalCache.addToCache(cacheKey, userClaims);
            }
        } catch (UserStoreException e) {
            throw new IdentityOAuth2Exception("Error while retrieving user claim values from "
                    + "user store");
        }
        return claimValues;
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
