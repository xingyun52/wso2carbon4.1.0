package org.wso2.carbon.appfactory.application.mgt.internal;

import org.wso2.carbon.utils.ConfigurationContextService;

public class ServiceReferenceHolder {

    private ConfigurationContextService configContextService;

    private static final ServiceReferenceHolder instance = new ServiceReferenceHolder();

    private ServiceReferenceHolder() {
    }

    public static ServiceReferenceHolder getInstance(){
        return instance;
    }

    public ConfigurationContextService getConfigContextService() {
        return configContextService;
    }

    public void setConfigContextService(ConfigurationContextService configContextService) {
        this.configContextService = configContextService;
    }
}
