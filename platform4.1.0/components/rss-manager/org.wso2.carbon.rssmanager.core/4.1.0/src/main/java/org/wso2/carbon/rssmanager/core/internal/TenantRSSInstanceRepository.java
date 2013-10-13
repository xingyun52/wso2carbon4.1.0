/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.rssmanager.core.internal;

import org.wso2.carbon.rssmanager.core.entity.RSSInstance;

import java.util.HashMap;
import java.util.Map;

public class TenantRSSInstanceRepository {

    private Map<String, RSSInstance> rssInstances;

    public TenantRSSInstanceRepository() {
        this.rssInstances = new HashMap<String, RSSInstance>();
    }

    public RSSInstance getRSSInstance(String rssInstanceName) {
        return this.getRSSInstances().get(rssInstanceName);
    }

    public void addRSSInstance(RSSInstance rssInstance) {
        this.getRSSInstances().put(rssInstance.getName(), rssInstance);
    }

    public Map<String, RSSInstance> getRSSInstances() {
        return rssInstances;
    }

    public void removeRSSInstance(String rssInstanceName) {
        this.getRSSInstances().remove(rssInstanceName);
    }

}
