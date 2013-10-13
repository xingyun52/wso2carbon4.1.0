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

package org.wso2.carbon.appfactory.application.mgt.util;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.core.ApplicationEventsListener;
import org.wso2.carbon.appfactory.core.ContinuousIntegrationSystemDriver;
import org.wso2.carbon.registry.api.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 *
 *
 */
public class Util {
    private static RegistryService registryService;
    private static RealmService realmService;
    private static AppFactoryConfiguration configuration;
    private static ContinuousIntegrationSystemDriver continuousIntegrationSystemDriver;
    
    /**
     * This set needs be a {@link SortedSet} ( e.g.{@link TreeSet} ) to preserve natural
     * ordering among {@link ApplicationEventsListener}s.
     * Refer
     * {@link ApplicationEventsListener#compareTo(ApplicationEventsListener)} to find out
     * how natural ordering occurs
     */
    private static Set<ApplicationEventsListener> applicationEventsListeners =
                                                                               Collections.synchronizedSet(new TreeSet<ApplicationEventsListener>());

    public static AppFactoryConfiguration getConfiguration() {
        return configuration;
    }

    public static void setConfiguration(AppFactoryConfiguration configuration) {
        Util.configuration = configuration;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static RealmService getRealmService() {
        return realmService;
    }



    public static synchronized void setRegistryService(RegistryService reg) {

            registryService=reg;

    }

    public static synchronized void setRealmService(RealmService realmSer) {

           realmService=realmSer;

    }

    public static ContinuousIntegrationSystemDriver getContinuousIntegrationSystemDriver() {
        return continuousIntegrationSystemDriver;
    }

    public static void setContinuousIntegrationSystemDriver(ContinuousIntegrationSystemDriver continuousIntegrationSystemDriver) {
        Util.continuousIntegrationSystemDriver = continuousIntegrationSystemDriver;
    }


    public static void addApplicationEventsListener(ApplicationEventsListener applicationEventsListener){
    	applicationEventsListeners.add(applicationEventsListener);
    }
    
    public static void removeApplicationEventsListener(ApplicationEventsListener applicationEventsListener) {
        applicationEventsListeners.remove(applicationEventsListener);
    }

    public static Set<ApplicationEventsListener> getApplicationEventsListeners() {
        return applicationEventsListeners;
    }

}
