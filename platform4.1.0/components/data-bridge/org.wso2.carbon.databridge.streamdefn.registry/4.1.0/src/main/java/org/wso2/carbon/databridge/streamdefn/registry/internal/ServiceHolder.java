/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.databridge.streamdefn.registry.internal;

import org.wso2.carbon.identity.authentication.AuthenticationService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

public class ServiceHolder {
    private static RealmService realmService;
    private static AuthenticationService authenticationService;
    private static RegistryService registryService;
    private static ConfigurationContextService configurationContextService;

    public static void setRealmService(RealmService realmService) {
        ServiceHolder.realmService = realmService;
    }

    public static void setAuthenticationService(AuthenticationService authenticationService) {
        ServiceHolder.authenticationService = authenticationService;
    }

    public static void setRegistryService(RegistryService registryService) {
        ServiceHolder.registryService = registryService;
    }

    public static void setConfigurationContextService(ConfigurationContextService configurationContextService) {
        ServiceHolder.configurationContextService = configurationContextService;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }
}
