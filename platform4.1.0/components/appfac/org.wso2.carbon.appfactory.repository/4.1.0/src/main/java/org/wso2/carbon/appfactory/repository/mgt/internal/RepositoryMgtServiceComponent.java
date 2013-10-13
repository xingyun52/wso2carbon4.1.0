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

package org.wso2.carbon.appfactory.repository.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.core.RevisionControlDriver;
import org.wso2.carbon.appfactory.repository.mgt.AppfactoryRevisionControlDriver;
import org.wso2.carbon.appfactory.repository.mgt.BranchingStrategy;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryManager;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryProvider;
import org.wso2.carbon.appfactory.repository.mgt.git.GITBranchingStrategy;
import org.wso2.carbon.appfactory.repository.mgt.service.RepositoryAuthenticationService;
import org.wso2.carbon.appfactory.repository.mgt.svn.SVNBranchingStrategy;
import org.wso2.carbon.appfactory.utilities.version.AppVersionStrategyExecutor;
import org.wso2.carbon.user.core.service.RealmService;

import java.lang.reflect.Constructor;

/**
 * @scr.component name="org.wso2.carbon.appfactory.repository.mgt" immediate="true"
 * @scr.reference name="appfactory.configuration" interface=
 * "org.wso2.carbon.appfactory.common.AppFactoryConfiguration"
 * cardinality="1..1" policy="dynamic"
 * bind="setAppFactoryConfiguration"
 * unbind="unsetAppFactoryConfiguration"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="appversion.executor"
 * interface="org.wso2.carbon.appfactory.utilities.version.AppVersionStrategyExecutor"
 * cardinality="1..1" policy="dynamic" bind="setAppVersionStrategyExecutor"
 * unbind="unsetAppVersionStrategyExecutor"
 */
public class RepositoryMgtServiceComponent {
    Log log = LogFactory.getLog(RepositoryMgtServiceComponent.class);
    public String repositoryTypes[] = {"svn", "git"};

    protected void unsetAppFactoryConfiguration(AppFactoryConfiguration appFactoryConfiguration) {
        Util.setConfiguration(null);
    }

    protected void setAppFactoryConfiguration(AppFactoryConfiguration appFactoryConfiguration) {
        Util.setConfiguration(appFactoryConfiguration);
    }

    protected void setRealmService(RealmService realmService) {

        Util.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        Util.setRealmService(null);
    }


    protected void setAppVersionStrategyExecutor(AppVersionStrategyExecutor versionExecutor) {
        Util.setVersionStrategyExecutor(versionExecutor);
    }

    protected void unsetAppVersionStrategyExecutor(AppVersionStrategyExecutor versionExecutor) {
        Util.setVersionStrategyExecutor(null);
    }

    protected void activate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.info("**************SVN repository mgt bundle is activated*************");
        }
        try {
            BundleContext bundleContext = context.getBundleContext();
            AppFactoryConfiguration configuration = Util.getConfiguration();
            for (String repoType : this.repositoryTypes) {
                //
                StringBuilder classNameKey = new StringBuilder(AppFactoryConstants.REPOSITORY_PROVIDER_CONFIG);
                classNameKey.append(".").append(repoType).append(".").append("Property").append(".").append("Class");
                String className = configuration.getFirstProperty(classNameKey.toString());
                if (className != null) {
                    Class<RepositoryProvider> provider = (Class<RepositoryProvider>) this.getClass().getClassLoader().loadClass(className);
                    Constructor constructor = provider.getConstructor();
                    RepositoryProvider providerObject = (RepositoryProvider) constructor.newInstance();
                    providerObject.setConfiguration(configuration);
                    BranchingStrategy branchingStrategy = null;
                    if ("svn".equals(repoType)) {
                        branchingStrategy = new SVNBranchingStrategy();
                    } else if ("git".equals(repoType)) {
                        branchingStrategy = new GITBranchingStrategy();
                    }
                    if (branchingStrategy != null) {
                        branchingStrategy.setRepositoryProvider(providerObject);
                    } else {
                        log.error("No Branching Strategy found for" + repoType);
                    }
                    providerObject.setBranchingStrategy(branchingStrategy);
                    log.info("Adding Provider for " + repoType);
                    Util.setRepositoryProvider(repoType, providerObject);
                } else {
                    log.error("repository provider is not found for " + repoType);
                }
            }
            AppfactoryRevisionControlDriver revisionControlDriver = new AppfactoryRevisionControlDriver();
            RepositoryAuthenticationService authenticationService = new RepositoryAuthenticationService();
            RepositoryManager repositoryManager = new RepositoryManager();
            bundleContext.registerService(RevisionControlDriver.class.getName(), revisionControlDriver, null);
            bundleContext.registerService(RepositoryAuthenticationService.class.getName(), authenticationService, null);
            bundleContext.registerService(RepositoryManager.class.getName(), repositoryManager, null);


        } catch (Throwable e) {
            log.error("Error in registering Repository Management Service  ", e);
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("*************SVN repository mgt bundle is deactivated*************");
        }
    }

}
