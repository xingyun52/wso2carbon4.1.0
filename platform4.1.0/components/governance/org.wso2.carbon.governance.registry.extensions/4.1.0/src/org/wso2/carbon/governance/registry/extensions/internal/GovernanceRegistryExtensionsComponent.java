/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.governance.registry.extensions.internal;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.core.services.callback.LoginSubscriptionManagerService;
import org.wso2.carbon.governance.registry.extensions.listeners.RxtLoader;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;

/**
 * @scr.component name="org.wso2.governance.registry.extensions.services" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic"  bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="login.subscription.service"
 * interface="org.wso2.carbon.core.services.callback.LoginSubscriptionManagerService" cardinality="0..1"
 * policy="dynamic" bind="setLoginSubscriptionManagerService" unbind="unsetLoginSubscriptionManagerService"
 */

public class GovernanceRegistryExtensionsComponent {

    private static final Log log = LogFactory.getLog(GovernanceRegistryExtensionsComponent.class);
    private static RegistryService registryService = null;

    protected void activate(ComponentContext componentContext) {
       if(log.isDebugEnabled()){
           log.debug("GovernanceRegistryExtensionsComponent activated");
       }
    }

    protected void setRegistryService(RegistryService registryService) {
        if(registryService!=null && log.isDebugEnabled()){
          log.debug("Registry service initialized");
        }
        this.registryService = registryService;
    }

    protected void unsetRegistryService(RegistryService registryService) {
        this.registryService = null;
    }

    public static RegistryService getRegistryService() throws RegistryException {
        return registryService;
    }

    protected void setLoginSubscriptionManagerService(LoginSubscriptionManagerService loginManager) {
        log.debug("******* LoginSubscriptionManagerServic is set ******* ");
        loginManager.subscribe(new RxtLoader());
    }

    protected void unsetLoginSubscriptionManagerService(LoginSubscriptionManagerService loginManager) {
        log.debug("******* LoginSubscriptionManagerServic is unset ******* ");
    }
}
