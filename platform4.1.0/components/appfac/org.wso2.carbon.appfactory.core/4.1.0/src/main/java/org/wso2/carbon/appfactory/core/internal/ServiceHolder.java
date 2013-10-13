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

package org.wso2.carbon.appfactory.core.internal;

import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.core.ArtifactStorage;
import org.wso2.carbon.appfactory.core.BuildDriver;
import org.wso2.carbon.appfactory.core.ContinuousIntegrationSystemDriver;
import org.wso2.carbon.appfactory.core.Storage;
import org.wso2.carbon.appfactory.core.RevisionControlDriver;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

public class ServiceHolder {
    public static RevisionControlDriver revisionControlDriver;
    public static BuildDriver buildDriver;
    public static ArtifactStorage artifactStorage;
    public static Storage storage;
    public static ContinuousIntegrationSystemDriver continuousIntegrationSystemDriver;
    public static AppFactoryConfiguration appFactoryConfiguration;
    private static RegistryService registryService;
    private static RealmService realmService;
    private static TenantRegistryLoader tenantRegistryLoader;
    private ConfigurationContextService configContextService;

    private static final ServiceHolder instance = new ServiceHolder();

    private ServiceHolder() {
    }

    public static ServiceHolder getInstance(){
        return instance;
    }

    public static BuildDriver getBuildDriver() {
        return buildDriver;
    }

    public static void setBuildDriver(BuildDriver buildDriver) {
        ServiceHolder.buildDriver = buildDriver;
    }


    public static RevisionControlDriver getRevisionControlDriver() {
        return revisionControlDriver;
    }

    public static void setRevisionControlDriver(RevisionControlDriver revisionControlDriver) {
        ServiceHolder.revisionControlDriver = revisionControlDriver;
    }


    public static ContinuousIntegrationSystemDriver getContinuousIntegrationSystemDriver() {
        return continuousIntegrationSystemDriver;
    }

    public static void setContinuousIntegrationSystemDriver(ContinuousIntegrationSystemDriver continuousIntegrationSystemDriver) {
        ServiceHolder.continuousIntegrationSystemDriver = continuousIntegrationSystemDriver;
    }


    public static ArtifactStorage getArtifactStorage() {
        return artifactStorage;
    }

    public static void setArtifactStorage(ArtifactStorage artifactStorage) {
        ServiceHolder.artifactStorage = artifactStorage;
    }

    public static Storage getStorage() {
        return storage;
    }

    public static void setStorage(Storage storage) {
        ServiceHolder.storage = storage;
    }

    public static AppFactoryConfiguration getAppFactoryConfiguration() {
        return appFactoryConfiguration;
    }

    public static void setAppFactoryConfiguration(AppFactoryConfiguration appFactoryConfiguration) {
        ServiceHolder.appFactoryConfiguration = appFactoryConfiguration;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static void setRegistryService(RegistryService registryService) {
        ServiceHolder.registryService = registryService;
    }
    public static RealmService getRealmService() {
        return realmService;
    }


    public static synchronized void setRealmService(RealmService realmSer) {
        realmService = realmSer;
    }

    public static TenantRegistryLoader getTenantRegistryLoader() {
        return tenantRegistryLoader;
    }

    public static void setTenantRegistryLoader(TenantRegistryLoader tenantRegistryLoader) {
        ServiceHolder.tenantRegistryLoader = tenantRegistryLoader;
    }

    public ConfigurationContextService getConfigContextService() {
        return configContextService;
    }

    public void setConfigContextService(ConfigurationContextService configContextService) {
        this.configContextService = configContextService;
    }
}
