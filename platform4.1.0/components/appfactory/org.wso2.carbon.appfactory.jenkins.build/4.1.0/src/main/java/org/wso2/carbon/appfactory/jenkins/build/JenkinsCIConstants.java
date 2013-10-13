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

package org.wso2.carbon.appfactory.jenkins.build;

import org.wso2.carbon.appfactory.common.AppFactoryConstants;

public class JenkinsCIConstants {

    public static final String CONTINUOUS_INTEGRATION_PROVIDER_CONFIG_SELECTOR =
                                                                                 "ContinuousIntegrationProvider";
    public static final String BASE_URL_CONFIG_SELECTOR =
                                                          "ContinuousIntegrationProvider.jenkins.Property.BaseURL";
    public static final String ADMIN_USER_NAME_CONFIG_SELECTOR =
                                                                 "ContinuousIntegrationProvider.jenkins.Property.AdminUserName";
    public static final String ADMIN_API_KEY_CONFIG_SELECTOR =
                                                               "ContinuousIntegrationProvider.jenkins.Property.AdminApiKey";
    public static final String MAVEN3_CONFIG_NAME_CONFIG_SELECTOR =
                                                                    "ContinuousIntegrationProvider.jenkins.Property.Maven3ConfigName";
    public static final String AUTHENTICATE_CONFIG_SELECTOR =
                                                              "ContinuousIntegrationProvider.jenkins.Property.Authenticate";
    public static final String DEFAULT_GLOBAL_ROLES_CONFIG_SELECTOR = "ContinuousIntegrationProvider.jenkins.Property.DefaultGlobalRoles";
       
    public static final String SVN_REPOSITORY = "svn.repository";
    public static final String SVN_REPOSITORY_XPATH_SELECTOR =
                                                               "/*/scm/locations/hudson.scm.SubversionSCM_-ModuleLocation/remote";
    public static final String SVN_CREDENTIALS_USERNAME = "svn.credentials.username";
    public static final String SVN_CREDENTIALS_PASSWORD = "svn.credentials.password";

    public static final String MAVEN3_CONFIG_NAME = "maven3.config.name";
    public static final String MAVEN3_CONFIG_NAME_XAPTH_SELECTOR = "mavenName";
    public static final String PREBUILDERS_MAVEN3_CONFIG_NAME_XPATH_SELECTOR =
                                                                               "/*/prebuilders/hudson.tasks.Maven/mavenName";

    public static final String APPLICATION_ID = AppFactoryConstants.APPLICATION_ID;
    public static final String APPLICATION_VERSION = AppFactoryConstants.APPLICATION_VERSION;
    public static final String APPLICATION_EXTENSION = "application.extension";

    private static final String PUBLISHERS_APPFACTORY_POST_BUILD_XPATH_BASE =
                              "/*/publishers/org.jenkins.wso2.appfactory.deploy.notify.AppfactoryPostBuildNotifier/";
    public static final String PUBLISHERS_APPFACTORY_POST_BUILD_APP_ID_XPATH_SELECTOR =
                                                     PUBLISHERS_APPFACTORY_POST_BUILD_XPATH_BASE + "applicationId";
    public static final String PUBLISHERS_APPFACTORY_POST_BUILD_APP_VERSION_XPATH_SELECTOR =
                                                     PUBLISHERS_APPFACTORY_POST_BUILD_XPATH_BASE + "/applicationVersion";
    public static final String PUBLISHERS_APPFACTORY_POST_BUILD_APP_EXTENSION_XPATH_SELECTOR =
                                                     PUBLISHERS_APPFACTORY_POST_BUILD_XPATH_BASE + "/applicationArtifactExtention";

}
