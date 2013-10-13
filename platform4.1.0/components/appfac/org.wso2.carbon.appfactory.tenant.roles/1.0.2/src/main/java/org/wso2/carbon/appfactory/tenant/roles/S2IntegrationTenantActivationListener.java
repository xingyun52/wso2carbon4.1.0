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

package org.wso2.carbon.appfactory.tenant.roles;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.tenant.roles.S2Integration.SubscriptionManagerClient;
import org.wso2.carbon.appfactory.tenant.roles.util.Util;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;

public class S2IntegrationTenantActivationListener implements TenantMgtListener {
    private static final Log log = LogFactory.getLog(S2IntegrationTenantActivationListener.class);

    private static final int ORDER = 50;
    private static SubscriptionManagerClient subscriptionManagerClient;
    
//    We use the on tenant create method to subscribe to the production S2 ADC
    @Override
    public void onTenantCreate(TenantInfoBean tenantInfoBean) throws StratosException {

    }

    @Override
    public void onTenantUpdate(TenantInfoBean tenantInfoBean) throws StratosException {
        //Do nothing.
    }

    @Override
    public void onTenantRename(int i, String s, String s2) throws StratosException {
        //Do nothing.
    }

    @Override
    public void onTenantInitialActivation(int i) throws StratosException {
        if(subscriptionManagerClient == null){
            subscriptionManagerClient = new SubscriptionManagerClient();
        }

        RealmService realmService = Util.getRealmService();
        TenantManager tenantManager = realmService.getTenantManager();

        try {
            String domain = tenantManager.getDomain(i);

            if(log.isDebugEnabled()){
                log.debug("Tenant domain : " + domain);
            }
            subscriptionManagerClient.subscribe(domain);
        } catch (AppFactoryException e) {
            String msg = "Unable to subscribe to the S2 production instance";
            log.error(msg,e);
            throw new StratosException(msg,e);
        } catch (UserStoreException e) {
            String msg = "Unable to get tenant domain for id : " + i;
            log.error(msg,e);
            throw new StratosException(msg,e);
        }
    }

    @Override
    public void onTenantActivation(int i) throws StratosException {
        //Do nothing.
    }

    @Override
    public void onTenantDeactivation(int i) throws StratosException {
        //Do nothing.
    }

    @Override
    public void onSubscriptionPlanChange(int i, String s, String s2) throws StratosException {
        //Do nothing.
    }

    @Override
    public int getListenerOrder() {
        return ORDER;
    }
}
