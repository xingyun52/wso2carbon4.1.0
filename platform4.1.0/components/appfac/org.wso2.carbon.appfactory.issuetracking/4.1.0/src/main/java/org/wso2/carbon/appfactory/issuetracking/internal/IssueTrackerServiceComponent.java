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
package org.wso2.carbon.appfactory.issuetracking.internal;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.appfactory.application.mgt.service.ApplicationManagementService;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.core.ApplicationEventsListener;
import org.wso2.carbon.appfactory.issuetracking.AppFactoryApplicationEventListener;

/**
 * @scr.component name="org.wso2.carbon.issutracker"
 * immediate="true"
 * @scr.reference name="appfactory.configuration" interface=
 * "org.wso2.carbon.appfactory.common.AppFactoryConfiguration"
 * cardinality="1..1" policy="dynamic"
 * bind="setAppFactoryConfiguration"
 * unbind="unsetAppFactoryConfiguration"
 * @scr.reference name="appfactory.application.mgt.service" interface=
 * "org.wso2.carbon.appfactory.application.mgt.service.ApplicationManagementService"
 * cardinality="1..1" policy="dynamic"
 * bind="setApplicationManagementService"
 * unbind="unsetApplicationManagementService"
 */
public class IssueTrackerServiceComponent {
    private static final Log log = LogFactory.getLog(IssueTrackerServiceComponent.class);

    protected void activate(ComponentContext context) {

        if (log.isDebugEnabled()) {
            log.debug("Issue tracking  service bundle is activated");
        }
        if(ServiceContainer.getAppFactoryConfiguration().getProperties("IssueTrackerConnector").length>0){
        
        String priorityConfigValue =  ServiceContainer.getAppFactoryConfiguration().getFirstProperty("IssueTrackerConnector.redmine.Property.ListenerPriority");
        
        int redmineListnerPriority = -1;
        try {
            redmineListnerPriority = Integer.parseInt(priorityConfigValue);
        } catch (NumberFormatException nef) {
            throw new IllegalArgumentException(
                                               "Invalid priority specified for redmin application event listener. Please provide a number",
                                               nef);
        }
        
        BundleContext bundleContext = context.getBundleContext();
        // Registering the issueTracker application event listener.
        bundleContext.registerService(ApplicationEventsListener.class.getName(),
                                      new AppFactoryApplicationEventListener(redmineListnerPriority), null);
        log.info("Issue tracking is enabled");
        }else {
        log.info("Issue tracking is disabled");
        }
    }

    protected void setAppFactoryConfiguration(AppFactoryConfiguration appFactoryConfiguration) {
        ServiceContainer.setAppFactoryConfiguration(appFactoryConfiguration);
    }

    protected void unsetAppFactoryConfiguration(AppFactoryConfiguration appFactoryConfiguration) {
        ServiceContainer.setAppFactoryConfiguration(null);
    }

    protected void setApplicationManagementService(
            ApplicationManagementService applicationManagementService) {
        ServiceContainer.setApplicationManagementService(applicationManagementService);
    }

    protected void unsetApplicationManagementService(
            ApplicationManagementService applicationManagementService) {
        ServiceContainer.setApplicationManagementService(null);
    }
}
