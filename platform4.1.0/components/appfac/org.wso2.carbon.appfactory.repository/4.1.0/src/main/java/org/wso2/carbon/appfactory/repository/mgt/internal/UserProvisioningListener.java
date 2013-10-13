/*
* Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.appfactory.repository.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.ApplicationEventsListener;
import org.wso2.carbon.appfactory.core.dto.Application;
import org.wso2.carbon.appfactory.core.dto.UserInfo;
import org.wso2.carbon.appfactory.core.dto.Version;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryManager;
import org.wso2.carbon.appfactory.userstore.service.UserClaimStoreService;
import org.wso2.carbon.user.api.Claim;

public class UserProvisioningListener extends ApplicationEventsListener {
    Log log = LogFactory.getLog(UserProvisioningListener.class);
    public static final String GITHUB_ACCOUNT_CLAIM = "http://wso2.org/github_account_name";

    @Override
    public void onCreation(Application application) throws AppFactoryException {
        //Do nothing
    }

    @Override
    public void onUserAddition(Application application, UserInfo userInfo)
            throws AppFactoryException {
        log.info("User addition event received for github user provisioning listener.");
        if (application.getRepositoryType().equals("git")) {
            UserClaimStoreService UserClaimStoreService = new UserClaimStoreService();
            try {
                Claim[] claims = UserClaimStoreService.getUserClaims(userInfo.getUserName());
                if (claims != null && claims.length > 0) {
                    for (Claim claim : claims) {
                        if (GITHUB_ACCOUNT_CLAIM.equals(claim.getClaimUri())) {
                            RepositoryManager repositoryManager = new RepositoryManager();
                            repositoryManager.provisionUser(application.getId(), application.getRepositoryType(), claim.getValue());
                            log.info("User:"+userInfo.getUserName()+" having github account:"+claim.getValue()+ " is provisioned to repository:"+application.getId());
                            return;
                        }
                    }
                }
                String msg = "User claim:" + GITHUB_ACCOUNT_CLAIM + " is not found for user:" + userInfo.getUserName();
                log.error(msg);
                throw new AppFactoryException(msg);
            } catch (Exception e) {
                String msg = "Failed to provision user:" + userInfo.getUserName() + " to access repository.";
                log.error(msg, e);
                throw new AppFactoryException(msg, e);
            }
        }
    }

    @Override
    public void onUserDeletion(Application application, UserInfo userInfo)
            throws AppFactoryException {
        //To do: remove user from repository
    }

    @Override
    public void onRevoke(Application application) throws AppFactoryException {
        //Do nothing
    }

    @Override
    public void onVersionCreation(Application application, Version version, Version version1)
            throws AppFactoryException {
        //Do nothing
    }

    @Override
    public void onLifeCycleStageChange(Application application, Version version, String s,
                                       String s1) throws AppFactoryException {
        //Do nothing
    }

    @Override
    public int getPriority() {
        //todo make this configurable
        return 9;
    }
}