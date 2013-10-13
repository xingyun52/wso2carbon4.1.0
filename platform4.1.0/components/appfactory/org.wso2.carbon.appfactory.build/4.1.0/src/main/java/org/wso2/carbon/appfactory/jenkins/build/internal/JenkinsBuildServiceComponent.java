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

package org.wso2.carbon.appfactory.jenkins.build.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.core.ApplicationEventsListener;
import org.wso2.carbon.appfactory.core.ContinuousIntegrationSystemDriver;
import org.wso2.carbon.appfactory.jenkins.build.JenkinsApplicationEventsListener;
import org.wso2.carbon.appfactory.jenkins.build.JenkinsCIConstants;
import org.wso2.carbon.appfactory.jenkins.build.JenkinsCISystemDriver;
import org.wso2.carbon.appfactory.jenkins.build.RestBasedJenkinsCIConnector;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryManager;

/**
 * @scr.component name="org.wso2.carbon.appfactory.jenkins.build"
 *                immediate="true"
 * @scr.reference name="appfactory.configuration" interface=
 *                "org.wso2.carbon.appfactory.common.AppFactoryConfiguration"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setAppFactoryConfiguration"
 *                unbind="unsetAppFactoryConfiguration"
 * @scr.reference name="repository.manager"
 *                interface=
 *                "org.wso2.carbon.appfactory.repository.mgt.RepositoryManager"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setRepositoryManager" unbind="unsetRepositoryManager"
 */
public class JenkinsBuildServiceComponent {

    private static final Log log = LogFactory.getLog(JenkinsBuildServiceComponent.class);

    protected void setAppFactoryConfiguration(AppFactoryConfiguration appFactoryConfiguration) {
        ServiceContainer.setAppFactoryConfiguration(appFactoryConfiguration);
    }

    protected void unsetAppFactoryConfiguration(AppFactoryConfiguration appFactoryConfiguration) {
        ServiceContainer.setAppFactoryConfiguration(null);
    }

    protected void setRepositoryManager(RepositoryManager repoManager) {
        ServiceContainer.setRepositoryManager(repoManager);
    }

    protected void unsetRepositoryManager(RepositoryManager repoManager) {
        ServiceContainer.setRepositoryManager(null);
    }

    protected void activate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Jenkins build service bundle is activated");
        }
        try {

            if (isJenkinsEnabled()) {

                String authenticate =
                                      ServiceContainer.getAppFactoryConfiguration()
                                                      .getFirstProperty(JenkinsCIConstants.AUTHENTICATE_CONFIG_SELECTOR);
                String userName =
                                  ServiceContainer.getAppFactoryConfiguration()
                                                  .getFirstProperty(JenkinsCIConstants.ADMIN_USER_NAME_CONFIG_SELECTOR);
                String apiKey =
                                ServiceContainer.getAppFactoryConfiguration()
                                                .getFirstProperty(JenkinsCIConstants.ADMIN_API_KEY_CONFIG_SELECTOR);
                String jenkinsUrl =
                                    ServiceContainer.getAppFactoryConfiguration()
                                                    .getFirstProperty(JenkinsCIConstants.BASE_URL_CONFIG_SELECTOR);
                String jenkinsDefaultGlobalRoles =
                                                   ServiceContainer.getAppFactoryConfiguration()
                                                                   .getFirstProperty(JenkinsCIConstants.DEFAULT_GLOBAL_ROLES_CONFIG_SELECTOR);
                String listenerPriority =
                                          ServiceContainer.getAppFactoryConfiguration()
                                                          .getFirstProperty(JenkinsCIConstants.LISTENER_PRIORITY_CONFIG_SELECTOR);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Authenticate : %b", authenticate));
                    log.debug(String.format("Jenkins user name : %s", userName));
                    log.debug(String.format("Jenkins api key : %s", apiKey));
                    log.debug(String.format("Jenkins url : %s", jenkinsUrl));
                    log.debug(String.format("Default Global Roles : %s", jenkinsDefaultGlobalRoles));
                    log.debug(String.format("Listener Priority : %s", listenerPriority));
                }

                RestBasedJenkinsCIConnector connector =
                                                        new RestBasedJenkinsCIConnector(
                                                                                        jenkinsUrl,
                                                                                        Boolean.parseBoolean(authenticate),
                                                                                        userName,
                                                                                        apiKey);
                String[] globalRoles = jenkinsDefaultGlobalRoles.split(",");
                if (globalRoles == null) {
                    globalRoles = new String[] {};
                }

                int jenkinsListnerPriority = -1;
                try {
                    jenkinsListnerPriority = Integer.parseInt(listenerPriority);
                } catch (NumberFormatException nef) {
                    throw new IllegalArgumentException(
                                                       "Invalid priority specified for jenkins application event listener. Please provide a number",
                                                       nef);
                }

                JenkinsCISystemDriver jenkinsCISystemDriver =
                                                              new JenkinsCISystemDriver(connector,
                                                                                        globalRoles);
                ServiceContainer.setJenkinsCISystemDriver(jenkinsCISystemDriver);
                BundleContext bundleContext = context.getBundleContext();
                // Note: register the service only if its enabled in the
                // appfactory
                // configuration file.
                bundleContext.registerService(ContinuousIntegrationSystemDriver.class.getName(),
                                              jenkinsCISystemDriver, null);

                // Registering the Jenkins application event listener.
                bundleContext.registerService(ApplicationEventsListener.class.getName(),
                                              new JenkinsApplicationEventsListener(jenkinsListnerPriority), null);
            } else {
                log.info("Jenkins is not enabled");
            }

        } catch (Throwable e) {
            log.error("Error in registering Jenkins build service ", e);
        }
    }

    private boolean isJenkinsEnabled() {

        String[] definedCIDriverNames =

                                        ServiceContainer.getAppFactoryConfiguration()
                                                        .getProperties(JenkinsCIConstants.CONTINUOUS_INTEGRATION_PROVIDER_CONFIG_SELECTOR);
        boolean defined = false;
        for (String driverName : definedCIDriverNames) {
            if ("jenkins".equalsIgnoreCase(driverName)) {
                defined = true;
                break;
            }
        }

        return defined;
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Jenkins build service bundle is deactivated");
        }
    }
}
