package org.wso2.carbon.appfactory.tenant.roles;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.tenant.roles.S2Integration.SubscriptionManagerClient;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;

public class S2IntegrationTenantActivationListener implements TenantMgtListener {
    private static final Log log = LogFactory.getLog(S2IntegrationTenantActivationListener.class);

    private static final int ORDER = 50;
    private static SubscriptionManagerClient subscriptionManagerClient;
    
//    We use the on tenant create method to subscribe to the production S2 ADC
    @Override
    public void onTenantCreate(TenantInfoBean tenantInfoBean) throws StratosException {
        if(subscriptionManagerClient == null){
            subscriptionManagerClient = new SubscriptionManagerClient();
        }

        try {
            subscriptionManagerClient.subscribe(tenantInfoBean.getTenantDomain());
        } catch (AppFactoryException e) {
            String msg = "Unable to subscribe to the S2 production instance";
            log.error(msg,e);
            throw new StratosException(msg,e);
        }

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
        //Do nothing.
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
