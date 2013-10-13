/*
 *  Copyright (c) WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.identity.mgt.cache;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.caching.core.CacheInvalidator;
import org.wso2.carbon.caching.core.identity.IdentityCacheEntry;
import org.wso2.carbon.caching.core.identity.IdentityCacheKey;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.mgt.internal.IdentityMgtServiceComponent;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Login attempt Cache that caches no of failed login attempts of the user
 */
public class LoginAttemptCache {

    private Cache cache = null;

    private static LoginAttemptCache loginAttemptCache = new LoginAttemptCache();
    
    private final static String LOGIN_ATTEMPT_CACHE = "LOGIN_ATTEMPT_CACHE";

    /**
     * the logger we'll use for all messages
     */
	private static Log log = LogFactory.getLog(LoginAttemptCache.class);
    
    private LoginAttemptCache() {
        this.cache =  CarbonUtils.getLocalCache(LOGIN_ATTEMPT_CACHE); 
    }

	public static LoginAttemptCache getInstance() {
		return loginAttemptCache;
	}

    public void addToCache(String userName) {

        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        IdentityCacheKey cacheKey = new IdentityCacheKey(tenantId, userName);
        IdentityCacheEntry cacheEntry = (IdentityCacheEntry) this.cache.get(cacheKey);

        int i;        
        if(cacheEntry != null){
            i = cacheEntry.getHashEntry() + 1;
        } else {
            i = 1;
        }

        IdentityCacheEntry newCacheEntry = new IdentityCacheEntry(i);
        this.cache.put(cacheKey, newCacheEntry);
        if (log.isDebugEnabled()) {
            log.debug("Cache entry is added");
        }        
    }

    public int getValueFromCache(String userName){
        
        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        IdentityCacheKey cacheKey = new IdentityCacheKey(tenantId, userName);
        IdentityCacheEntry cacheEntry = (IdentityCacheEntry) this.cache.get(cacheKey);
        if(cacheEntry != null){
            if (log.isDebugEnabled()) {
                log.debug("Cache entry is found");
            }
            return cacheEntry.getHashEntry();
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Cache entry is not found");
        }

        return 0;
    }

    public void clearCacheEntry(String userName){

        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        IdentityCacheKey cacheKey = new IdentityCacheKey(tenantId, userName);

        if(this.cache.containsKey(cacheKey)){

            this.cache.remove(cacheKey);

            if (log.isDebugEnabled()) {
                log.debug("Local cache is invalidated");
            }
            //sending cluster message
            CacheInvalidator invalidator = IdentityMgtServiceComponent.getCacheInvalidator();
            try {
                if (invalidator != null) {
                    invalidator.invalidateCache(LOGIN_ATTEMPT_CACHE, cacheKey);
                    if (log.isDebugEnabled()) {
                        log.debug("Calling invalidation cache");
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Not calling invalidation cache");
                    }
                }
            } catch (CacheException e) {
                log.error("Error while invalidating cache", e);
            }
        }

    }

    public Cache getCache() {
        return cache;
    }
}
