/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.hdfs.namenode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * Keep runtime objects for HDFS namenode and have the reference to userRealm and authAdmin
 */
public class HDFSNameNodeComponentManager {
    private static Log log = LogFactory.getLog(HDFSNameNodeComponentManager.class);

    private static HDFSNameNodeComponentManager componentManager = null;
    private boolean initialized = false;
    private RealmService realmService;
    //private AuthenticationService authenticationService;

    public static HDFSNameNodeComponentManager getInstance() {
        if (componentManager == null) {
            synchronized (HDFSNameNodeComponentManager.class) {
                componentManager = new HDFSNameNodeComponentManager();
            }
        }
        return componentManager;
    }

    private HDFSNameNodeComponentManager() {
    }

    //public void init(RealmService realmService, AuthenticationService authenticationService) {
    public void init(RealmService realmService) {

        this.realmService = realmService;
        //this.authenticationService = authenticationService;
        //if (realmService != null && authenticationService != null) {
            this.initialized = true;
       // }
    }


    public boolean isInitialized() {
        return initialized;
    }

    private void assertInitialized() throws Exception {
        if (!initialized) {
            throw new Exception("HDFS Admin Component has not been initialized");
        }
    }

    public UserRealm getRealmForCurrentTenant() throws Exception {
        assertInitialized();
        try {
            int tenantId = realmService.getTenantManager().getTenantId(MultitenantUtils.getTenantDomain("admin"));

            //return realmService.getTenantUserRealm(CarbonContext.getCurrentContext().getTenantId());
            return realmService.getTenantUserRealm(tenantId);


        } catch (UserStoreException e) {
            throw new Exception("Error accessing the UserRealm for super tenant : " + e);
        }
    }

    /**
     * Cleanup resources
     */
    public void destroy() {
        realmService = null;
        initialized = false;
    }


}
