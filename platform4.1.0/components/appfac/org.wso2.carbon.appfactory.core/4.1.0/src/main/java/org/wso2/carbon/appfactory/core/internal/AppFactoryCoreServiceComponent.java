/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.appfactory.core.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
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

/**
 * @scr.component name=
 *                "org.wso2.carbon.appfactory.core.internal.AppFactoryCoreServiceComponent"
 *                immediate="true"
 * @scr.reference name="appfactory.maven"
 *                interface="org.wso2.carbon.appfactory.core.BuildDriver"
 *                cardinality="1..1" policy="dynamic" bind="setBuildDriver"
 *                unbind="unsetBuildDriver"
 * @scr.reference name="appfactory.svn"
 *                interface="org.wso2.carbon.appfactory.core.RevisionControlDriver"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setRevisionControlDriver"
 *                unbind="unsetRevisionControlDriver"
 * @scr.reference name="appfactory.configuration"
 *                interface="org.wso2.carbon.appfactory.common.AppFactoryConfiguration"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setAppFactoryConfiguration"
 *                unbind="unsetAppFactoryConfiguration"
 * @scr.reference name="appfactory.artifact"
 *                interface="org.wso2.carbon.appfactory.core.ArtifactStorage"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setArtifactStorage"
 *                unbind="unsetArtifactStorage"
 * @scr.reference name="appfactory.cidriver"
 *                interface="org.wso2.carbon.appfactory.core.ContinuousIntegrationSystemDriver"
 *                cardinality="0..1" policy="dynamic"
 *                bind="setContinuousIntegrationSystemDriver"
 *                unbind="unsetContinuousIntegrationSystemDriver"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="appfactory.storage"
 *                interface="org.wso2.carbon.appfactory.core.Storage"
 *                cardinality="0..1" policy="dynamic"
 *                bind="setStorage"
 *                unbind="unsetStorage"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setRealmService"
 *                unbind="unsetRealmService"
 * @scr.reference name="registry.loader.default"
 *                interface="org.wso2.carbon.registry.core.service.TenantRegistryLoader"
 *                cardinality="1..1"
 *                policy="dynamic"
 *                bind="setRegistryLoader"
 *                unbind="unsetRegistryLoader"
 * @scr.reference name="config.context.service"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setConfigurationContextService"
 *                unbind="unsetConfigurationContextService"
 */

public class AppFactoryCoreServiceComponent {

	private static final Log log = LogFactory
			.getLog(AppFactoryCoreServiceComponent.class);
    private static BundleContext bundleContext;
    protected void activate(ComponentContext context) {
        AppFactoryCoreServiceComponent. bundleContext = context.getBundleContext();

        try {
            if (log.isDebugEnabled()) {
                log.debug("Appfactory core bundle is activated");
            }
        } catch (Throwable e) {
            log.error("Error in creating appfactory configuration", e);
        }

    }

	protected void deactivate(ComponentContext context) {
		if (log.isDebugEnabled()) {
			log.debug("Appfactory common bundle is deactivated");
		}
	}

	protected void unsetBuildDriver(BuildDriver buildDriver) {
		ServiceHolder.setBuildDriver(null);
	}

	protected void setBuildDriver(BuildDriver buildDriver) {
		ServiceHolder.setBuildDriver(buildDriver);
	}

	protected void unsetRevisionControlDriver(
			RevisionControlDriver revisionControlDriver) {
		ServiceHolder.setRevisionControlDriver(null);
	}

	protected void setRevisionControlDriver(
			RevisionControlDriver revisionControlDriver) {
		ServiceHolder.setRevisionControlDriver(revisionControlDriver);
	}

    protected void unsetArtifactStorage(ArtifactStorage artifactStorage) {
        ServiceHolder.setArtifactStorage(null);
    }

    protected void setArtifactStorage(ArtifactStorage artifactStorage) {
        ServiceHolder.setArtifactStorage(artifactStorage);
    }

    protected void unsetStorage(Storage storage) {
        ServiceHolder.setArtifactStorage(null);
    }

    protected void setStorage(Storage storage) {
        ServiceHolder.setStorage(storage);
    }

    protected void setAppFactoryConfiguration(AppFactoryConfiguration appFactoryConfiguration) {
        ServiceHolder.setAppFactoryConfiguration(appFactoryConfiguration);
    }

    protected void unsetAppFactoryConfiguration(AppFactoryConfiguration appFactoryConfiguration) {
        ServiceHolder.setAppFactoryConfiguration(null);
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    public static void setBundleContext(BundleContext bundleContext) {
        AppFactoryCoreServiceComponent.bundleContext = bundleContext;
    }
    protected void unsetContinuousIntegrationSystemDriver(ContinuousIntegrationSystemDriver driver) {
        ServiceHolder.setContinuousIntegrationSystemDriver(null);
    }

    protected void setContinuousIntegrationSystemDriver(ContinuousIntegrationSystemDriver driver) {
        ServiceHolder.setContinuousIntegrationSystemDriver(driver);
    }
    protected void setRegistryService(RegistryService registryService) {
        if (registryService != null && log.isDebugEnabled()) {
            log.debug("Registry service initialized");
        }
        ServiceHolder.getInstance().setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        ServiceHolder.getInstance().setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("RealmService acquired");
        }
        ServiceHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        ServiceHolder.getInstance().setRealmService(null);
    }

    protected void setRegistryLoader(TenantRegistryLoader tenantRegistryLoader) {
        ServiceHolder.getInstance().setTenantRegistryLoader(tenantRegistryLoader);
    }

    protected void unsetRegistryLoader(TenantRegistryLoader tenantRegistryLoader) {
        ServiceHolder.getInstance().setTenantRegistryLoader(null);
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        ServiceHolder.getInstance().setConfigContextService(contextService);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        ServiceHolder.getInstance().setConfigContextService(null);
    }
}
