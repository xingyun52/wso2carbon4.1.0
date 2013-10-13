/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.gateway.handlers.security.service;

import net.sf.jsr107cache.Cache;

import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.mediation.initializer.AbstractServiceBusAdmin;

import java.util.Set;

public class APIAuthenticationService extends AbstractServiceBusAdmin {

    public void invalidateKeys(APIKeyMapping[] mappings) {
        Cache cache = PrivilegedCarbonContext.getCurrentContext(getAxisConfig()).getCache("keyCache");
        for (APIKeyMapping mapping : mappings) {
            String cacheKey = mapping.getKey() + ":" + mapping.getContext() + ":" + mapping.getApiVersion();
            Set keys = cache.keySet();
            for (Object cKey : keys) {
                String key = cKey.toString();
                if (key.contains(cacheKey)) {
                    cache.remove(key);

                }
            }
        }
    }

    public void invalidateOAuthKeys(String consumerKey, String authorizedUser) {
        Cache cache = PrivilegedCarbonContext.getCurrentContext(getAxisConfig()).getCache("keyCache");
        String cacheKey = consumerKey + ":" + authorizedUser;
        cache.remove(cacheKey);

    }

    public void invalidateResourceCache(String apiContext, String apiVersion,
                                        String resourceURLContext, String httpVerb) {
        String resourceVerbCacheKey = apiContext + "/" + apiVersion +
                                      resourceURLContext + ":" + httpVerb;
        String resourceCacheKey = apiContext + ":" + apiVersion;
        Cache cache = PrivilegedCarbonContext.getCurrentContext(getAxisConfig()).getCache("resourceCache");
        Cache keyCache = PrivilegedCarbonContext.getCurrentContext(getAxisConfig()).getCache("keyCache");
        if (keyCache.size() != 0) {
            Set keys = keyCache.keySet();
            for (Object cacheKey : keys) {
                String key = cacheKey.toString();
                if (key.contains(apiContext + ":" + apiVersion)) {
                    keyCache.remove(key);
                }
            }
        }
        if (cache.size() != 0) {
            if (resourceURLContext.equals("/")) {
                Set keys = cache.keySet();
                for (Object cacheKey : keys) {
                    String key = cacheKey.toString();
                    if (key.contains(apiContext + "/" + apiVersion + resourceURLContext)) {
                        cache.remove(key);
                    }
                }
            } else if (cache.get(resourceCacheKey) != null) {
                cache.remove(resourceVerbCacheKey);
            }

            cache.remove(resourceCacheKey);
        }

    }

    /**
     * This method is to invalidate an access token which is already in gateway cache.
     * @param accessToken The access token to be remove from the cache
     */
    public void invalidateKey(String accessToken) {
        Cache cache = PrivilegedCarbonContext.getCurrentContext(getAxisConfig()).getCache("keyCache");
        for (int i = 0; i < cache.keySet().size(); i++) {
            String cacheAccessKey = cache.keySet().toArray()[i].toString().split(":")[0];
            if (cacheAccessKey.equals(accessToken)) {
                cache.remove(cache.keySet().toArray()[i]);
            }

        }
    }
}
