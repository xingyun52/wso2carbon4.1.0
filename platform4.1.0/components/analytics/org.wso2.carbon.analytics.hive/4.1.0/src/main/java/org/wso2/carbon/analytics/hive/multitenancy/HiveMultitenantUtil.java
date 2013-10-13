package org.wso2.carbon.analytics.hive.multitenancy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.hive.ServiceHolder;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class HiveMultitenantUtil {
    private static final Log log = LogFactory.getLog(HiveMultitenantUtil.class);



    public static boolean isMultiTenantMode() {
        RealmService realmService = ServiceHolder.getRealmService();
        Tenant[] tenants;
        try {
            tenants = realmService.getTenantManager().getAllTenants();
        } catch (UserStoreException e) {
            return false;
        }
        return tenants != null && tenants.length > 0;
    }


}
