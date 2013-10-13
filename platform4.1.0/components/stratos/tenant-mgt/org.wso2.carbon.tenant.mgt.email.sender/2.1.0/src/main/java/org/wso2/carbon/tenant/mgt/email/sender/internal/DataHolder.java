/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.tenant.mgt.email.sender.internal;

import org.wso2.carbon.email.verification.util.EmailVerifcationSubscriber;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.ConfigurationContextService;

import org.apache.axis2.context.ConfigurationContext;

import org.osgi.framework.BundleContext;

/**
 * Utility methods for the email sender component
 */
public class DataHolder {

    private static RegistryService registryService;
    private static RealmService realmService;
    private static ConfigurationContextService configurationContextService;
    private static EmailVerifcationSubscriber emailVerificationService;
    private static BundleContext bundleContext;

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    public static void setBundleContext(BundleContext bundleContext) {
        DataHolder.bundleContext = bundleContext;
    }

    public static ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }

    public static void setConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        DataHolder.configurationContextService = configurationContextService;
    }

    public static ConfigurationContext getConfigurationContext() {
        return configurationContextService.getServerConfigContext();
    }

    public static synchronized void setRegistryService(RegistryService service) {
        if ((registryService == null) || (service == null)) {
            registryService = service;
        }
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static synchronized void setRealmService(RealmService service) {
        if ((realmService == null) || (service == null)){
            realmService = service;
        }
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static TenantManager getTenantManager() {
        return realmService.getTenantManager();
    }

    public static RealmConfiguration getBootstrapRealmConfiguration() {
        return realmService.getBootstrapRealmConfiguration();
    }

    public static UserRegistry getGovernanceSystemRegistry(int tenantId) throws RegistryException {
        return registryService.getGovernanceSystemRegistry(tenantId);
    }
    
    public static void setEmailVerificationService(EmailVerifcationSubscriber emailService) {
        if ((emailVerificationService == null) || (emailService == null)){
            emailVerificationService = emailService;
        }
    }
    
    public static EmailVerifcationSubscriber getEmailVerificationService() {
        return emailVerificationService;
    }
}
