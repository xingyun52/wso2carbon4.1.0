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

package org.wso2.carbon.appfactory.userstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.userstore.internal.OTLDAPUtil;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.ldap.LDAPConstants;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;

import java.util.Map;

public class OTAppFactoryUserStore extends AppFactoryUserStore {
    private static Log log = LogFactory.getLog(OTAppFactoryUserStore.class);

    public OTAppFactoryUserStore(RealmConfiguration realmConfig,
                                 Map<String, Object> properties, ClaimManager claimManager,
                                 ProfileConfigurationManager profileManager, UserRealm realm,
                                 Integer tenantId) throws UserStoreException {
        super(realmConfig, properties, claimManager, profileManager, realm, tenantId);
    }

    public OTAppFactoryUserStore(RealmConfiguration realmConfig, ClaimManager claimManager,
                                 ProfileConfigurationManager profileManager)
            throws UserStoreException {
        super(realmConfig, claimManager, profileManager);
    }

    @Override
    public boolean authenticate(String userName, Object credential) throws UserStoreException {
        return super.doAuthenticate(doConvert(userName), credential);
    }

    @Override
    public void updateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {
        super.updateRoleListOfUser(doConvert(userName), deletedRoles, newRoles);
    }

    @Override
    public void doUpdateUserListOfRole(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {
        super.doUpdateUserListOfRole(doConvert(userName), deletedRoles, newRoles);
    }

    @Override
    public boolean doAuthenticate(String userName, Object password) throws UserStoreException {
        return super.doAuthenticate(doConvert(userName), password);
    }

    @Override
    public String[] getRoleListOfUser(String userName) throws UserStoreException {
        return super.getRoleListOfUser(doConvert(userName));
    }

    @Override
    public int getTenantId() throws UserStoreException {
        return super.getTenantId();
    }

    @Override
    public int getTenantId(String tenantId) throws UserStoreException {
        return super.getTenantId(tenantId);
    }

    @Override
    public int getUserId(String userName) throws UserStoreException {
        return super.getUserId(doConvert(userName));
    }

    @Override
    public boolean isExistingUser(String userName) throws UserStoreException {
        return super.isExistingUser(doConvert(userName));
    }

    @Override
    public boolean isUserHasTheRole(String userName, String roleName) throws UserStoreException {
        return super.isUserHasTheRole(doConvert(userName), roleName);
    }

    private String doConvert(String email) throws UserStoreException {
        if (email == null) {
            throw new UserStoreException("User name can not be null.");
        }
        String searchBase = realmConfig.getUserStoreProperty(LDAPConstants.USER_SEARCH_BASE);
        return OTLDAPUtil.getUserIdFromEmail(email, this.connectionSource, searchBase);
    }


}
