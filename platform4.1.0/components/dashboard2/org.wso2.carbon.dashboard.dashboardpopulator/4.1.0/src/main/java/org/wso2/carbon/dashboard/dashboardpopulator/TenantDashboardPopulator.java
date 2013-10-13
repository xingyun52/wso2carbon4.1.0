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
package org.wso2.carbon.dashboard.dashboardpopulator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.services.callback.*;
import org.wso2.carbon.registry.core.Registry;

import java.util.LinkedList;
import java.util.List;

public class TenantDashboardPopulator implements LoginListener {

	private static Log log = LogFactory.getLog(TenantDashboardPopulator.class);

    private List<Integer> initializedTenants = new LinkedList<Integer>();

    public void onLogin(Registry configRegistry, LoginEvent loginEvent) {
        if (initializedTenants.contains(loginEvent.getTenantId())) {
            return;
        }

        // Populate the Dashboard Layout(dashboard.xml) for the current tenant, if not done so already.
        // Other tab related resources will be copied in the tab access time. Not inside this operation.
        try {
            GadgetPopulator.PopulateDashboardLayout(loginEvent.getTenantId());
            initializedTenants.add(loginEvent.getTenantId());
        } catch (Exception e) {
            log.error("Failed to execute tenant dashboard populator for tenant : " + loginEvent.getTenantId());
        }
    }
}
