/*
* Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.wso2.carbon.appfactory.userstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.config.RealmConfiguration;
import org.wso2.carbon.user.core.ldap.ReadWriteLDAPUserStoreManager;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;

import javax.naming.directory.SearchResult;
import java.util.Map;

public class OTReadWriteLDAPUserStoreManager extends ReadWriteLDAPUserStoreManager{
    private static Log log = LogFactory.getLog(OTReadWriteLDAPUserStoreManager.class);

    public OTReadWriteLDAPUserStoreManager(RealmConfiguration realmConfig,
                                           Map<String, Object> properties,
                                           ClaimManager claimManager,
                                           ProfileConfigurationManager profileManager,
                                           UserRealm realm, Integer tenantId)
            throws UserStoreException {
        super(realmConfig, properties, claimManager, profileManager, realm, tenantId);
        log.info("OT Userstore is configured.");
    }

    @Override
    public boolean doAuthenticate(String userName, Object credential) throws UserStoreException {
        log.info("OT userstore : DoAuthenticate");
        if (userName == null || credential == null) {
            return false;
        }
        userName = userName.replaceFirst("@", ".");
        return super.doAuthenticate(userName, credential);

    }

    @Override
    public void doUpdateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {
        log.info("OT userstore : doUpdateRoleListOfUser");

        super.doUpdateRoleListOfUser(doConvert(userName), deletedRoles, newRoles);
    }

    @Override
    protected void updateUserRoles(String userName, String[] roleList) throws UserStoreException {
        log.info("OT userstore : updateUserRoles");
        super.updateUserRoles(doConvert(userName), roleList);
    }

    @Override
    public void doUpdateUserListOfRole(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {
        log.info("OT userstore : doUpdateUserListOfRole");
        super.doUpdateUserListOfRole(doConvert(userName), deletedRoles, newRoles);
    }

    @Override
    public String[] getRoleListOfUser(String userName) throws UserStoreException {
        log.info("OT userstore : getRoleListOfUser");
        return super.getRoleListOfUser(doConvert(userName));
    }

    @Override
    public int getUserId(String userName) throws UserStoreException {
        log.info("OT userstore : getUserId");
        return super.getUserId(doConvert(userName));
    }

    @Override
    public boolean isExistingUser(String userName) throws UserStoreException {
        log.info("OT userstore : isExistingUser");
        return super.isExistingUser(doConvert(userName));
    }

    @Override
    public boolean isUserHasTheRole(String userName, String roleName) throws UserStoreException {
        log.info("OT userstore : isUserHasTheRole");
        return super.isUserHasTheRole(doConvert(userName), roleName);
    }

    @Override
    public boolean isUserInRole(String userDN, SearchResult groupEntry)
            throws UserStoreException {
        log.info("OT userstore : isUserInRole");
        return super.isUserInRole(doConvert(userDN), groupEntry);
    }

    private String doConvert(String userName) throws UserStoreException {
        if (userName == null) {
            throw new UserStoreException("User name can not be null.");
        }
        return userName.replaceFirst("@", ".");
    }


}