/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.andes.core.internal.util;

import org.wso2.carbon.andes.core.QueueManagerException;
import org.wso2.carbon.andes.core.internal.ds.QueueManagerServiceValueHolder;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserStoreException;
import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.andes.core.types.Queue;


public class Utils {

    public static String getTenantAwareCurrentUserName() {
        String username = CarbonContext.getCurrentContext().getUsername();
        if (CarbonContext.getCurrentContext().getTenantId() > 0) {
            return username + "@" + CarbonContext.getCurrentContext().getTenantDomain();
        }
        return username;
    }

    public static UserRegistry getUserRegistry() throws RegistryException {
        RegistryService registryService =
                QueueManagerServiceValueHolder.getInstance().getRegistryService();

        return registryService.getGovernanceSystemRegistry(CarbonContext.getCurrentContext().getTenantId());

    }

    public static org.wso2.carbon.user.api.UserRealm getUserRelam() throws UserStoreException {
        return QueueManagerServiceValueHolder.getInstance().getRealmService().
                getTenantUserRealm(CarbonContext.getCurrentContext().getTenantId());
    }

    public static String getTenantBasedQueueName(String queueName) {
        String tenantDomain = CarbonContext.getCurrentContext().getTenantDomain();
        if (tenantDomain != null && (!tenantDomain.equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME))) {
            queueName = tenantDomain + "/" + queueName;
        }
        return queueName;
    }

    /**
     * Checks if a given user has admin privileges
     *
     * @param username Name of the user
     * @return true if the user has admin rights or false otherwise
     * @throws org.wso2.carbon.andes.core.QueueManagerException
     *          if getting roles for the user fails
     */
    public static boolean isAdmin(String username) throws QueueManagerException {
        boolean isAdmin = false;

        try {
            String[] userRoles = QueueManagerServiceValueHolder.getInstance().getRealmService().
                    getTenantUserRealm(CarbonContext.getCurrentContext().getTenantId()).
                    getUserStoreManager().getRoleListOfUser(username);
            String adminRole = QueueManagerServiceValueHolder.getInstance().getRealmService().
                    getBootstrapRealmConfiguration().getAdminUserName();
            for (String userRole : userRoles) {
                if (userRole.equals(adminRole)) {
                    isAdmin = true;
                    break;
                }
            }
        } catch (UserStoreException e) {
            throw new QueueManagerException("Failed to get list of user roles", e);
        }

        return isAdmin;
    }

    /**
     * filter queues to suit the tenant domain
     * @param fullList
     * @return List<Queue>
     */
    public static List<Queue> filterDomainSpecificQueues(List<Queue> fullList) {
        String domainName = CarbonContext.getCurrentContext().getTenantDomain();
        ArrayList<Queue> tenantFilteredQueues = new ArrayList<Queue>();
        if(domainName != null && !CarbonContext.getCurrentContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Queue aQueue : fullList) {
                if(aQueue.getQueueName().startsWith(domainName)) {
                    tenantFilteredQueues.add(aQueue);
                }
            }
        }
        //for super tenant load all queues not specific to a domain. That means queues created by external
        //JMS clients are visible, and those names should not have "/" in their queue names
        else if(domainName != null && CarbonContext.getCurrentContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)){
            for (Queue aQueue : fullList) {
                if(!aQueue.getQueueName().contains("/")) {
                    tenantFilteredQueues.add(aQueue);
                }
            }
        }

        return tenantFilteredQueues;
    }

}
