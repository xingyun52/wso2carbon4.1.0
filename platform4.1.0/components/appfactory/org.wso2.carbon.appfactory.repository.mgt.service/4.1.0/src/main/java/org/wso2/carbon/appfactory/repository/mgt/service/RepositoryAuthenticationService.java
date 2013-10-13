/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.wso2.carbon.appfactory.repository.mgt.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.repository.mgt.internal.Util;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;

/**
 *
 */
public class RepositoryAuthenticationService extends AbstractAdmin {
    private static final Log log = LogFactory.getLog(RepositoryAuthenticationService.class);

    public boolean hasAccess(String username, String applicationId) {
        Integer tID = null;
        try {
            tID = Util.getRealmService().getTenantManager().getTenantId(applicationId);

            UserRealm realm = Util.getRealmService().getTenantUserRealm(tID);
            if (realm!=null && realm.getAuthorizationManager().isUserAuthorized(username,
                                                                 Util.getConfiguration().getFirstProperty(
                                                                         AppFactoryConstants.SCM_READ_WRITE_PERMISSION), "ui.execute")) {
                return true;
            }

        } catch (UserStoreException e) {
            String msg = "Error while checking permission for accessing svn repository of " + applicationId + " by " + username;
            log.error(msg, e);
        }


        return false;
    }
}
