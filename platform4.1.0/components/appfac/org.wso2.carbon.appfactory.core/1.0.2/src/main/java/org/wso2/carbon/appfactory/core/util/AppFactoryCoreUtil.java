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

package org.wso2.carbon.appfactory.core.util;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.util.NotificationSender;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

public class AppFactoryCoreUtil {

    private static final Log log = LogFactory.getLog(AppFactoryCoreUtil.class);

    public static String getStage (String applicationId, String version) throws AppFactoryException {
	    	String stage = null;
	        try {
	            RegistryService registryService = ServiceHolder.getRegistryService();
	            UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
	            // child nodes of this will contains folders for all life cycles (
	            // e.g. QA, Dev, Prod)
	            Resource application =
	                    userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
                                RegistryConstants.PATH_SEPARATOR + applicationId);

	            if (application != null && application instanceof Collection) {

	                // Contains paths to life cycles (.e.g .../<appid>/dev,
	                // .../<appid>/qa , .../<appid>/prod )
	                String[] definedLifeCyclePaths = ((Collection) application).getChildren();

	                for (String lcPath : definedLifeCyclePaths) {

	                    Resource versionsInLCResource = userRegistry.get(lcPath);
	                    if (versionsInLCResource != null && versionsInLCResource instanceof Collection) {

	                        // contains paths to a versions (e.g.
	                        // .../<appid>/<lifecycle>/trunk,
	                        // .../<appid>/<lifecycle>/1.0.1 )
	                        for(String currentVersion :((Collection) versionsInLCResource).getChildren()) {
	                            stage = lcPath.substring(lcPath.lastIndexOf("/") + 1);
                                String versionOnly = currentVersion.substring(currentVersion.lastIndexOf("/") + 1);

	                            if (versionOnly.equals(version)) {
	                            	return stage;
	                            }
	                        }
	                    }
	                }
	            }
	        } catch (RegistryException e) {
	            String errorMsg = String.format("Unable to load the application information for application id: %s",
	                                  applicationId);
	            log.error(errorMsg, e);
	            throw new AppFactoryException(errorMsg, e);
	        }
	        return stage;
	    }

    public static void sendEventNotification(final String applicationId, final String event, final String result) {
        try {
            AppFactoryConfiguration configuration = ServiceHolder.getAppFactoryConfiguration();
            String serverUrl = configuration.getFirstProperty(AppFactoryConstants.APPFACTORY_SERVER_URL);
            NotificationSender notificationSender = new NotificationSender(serverUrl);
            notificationSender.publishEvents(applicationId, event, result);
        } catch (RemoteException e) {
            log.error("Notification sending failed "+e.getMessage(), e);
        }
    }

}
