/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package org.wso2.carbon.appfactory.userstore.internal;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OTUserIdCache {
    private static Log log = LogFactory.getLog(OTUserIdCache.class);

    public static final String OT_USER_ID_CACHE_NAME = "OT_USER_ID_CACHE";

    protected Cache cache = null;

    private static OTUserIdCache userIdCache = null;

    private OTUserIdCache() {
        this.cache = CacheManager.getInstance().getCache(OT_USER_ID_CACHE_NAME);
        if (log.isDebugEnabled()) {
            if (cache != null) {
                log.debug(OT_USER_ID_CACHE_NAME + " is successfully initiated.");
            } else {
                log.error(OT_USER_ID_CACHE_NAME + " is not initiated.");
            }
        }
    }

    public static OTUserIdCache getOTUserIdCache() {
        if (userIdCache == null) {
            userIdCache = new OTUserIdCache();
        }
        return userIdCache;
    }

    public void addToCache(String email, String uid) {
        if (isCacheNull()) {
            return;
        }
        this.cache.put(email, uid);
        if (log.isDebugEnabled()) {
            log.debug(OT_USER_ID_CACHE_NAME + " was updated for email:" + email);
        }
    }

    public String getValueFromCache(String username) {
        String userId = null;
        if (isCacheNull()) {
            return userId;
        }
        Object cacheValue = this.cache.get(username);
        if (cacheValue instanceof String) {
            userId = (String) cacheValue;
        }
        return userId;
    }

    private boolean isCacheNull() {
        if (this.cache == null) {
            if (log.isDebugEnabled()) {
                StackTraceElement[] elemets = Thread.currentThread().getStackTrace();
                String traceString = "";
                for (int i = 1; i < elemets.length; ++i) {
                    traceString += elemets[i] + System.getProperty("line.separator");
                }
                log.debug(OT_USER_ID_CACHE_NAME + " doesn't exist in CacheManager:\n" + traceString);
            }
            return true;
        }
        return false;
    }

    public void clearFromCache(String email) {
        if (getValueFromCache(email) != null) {
            this.cache.remove(email);
            if (log.isDebugEnabled()) {
                log.debug(OT_USER_ID_CACHE_NAME + " was cleaned up for email:" + email);
            }
        }
    }

}