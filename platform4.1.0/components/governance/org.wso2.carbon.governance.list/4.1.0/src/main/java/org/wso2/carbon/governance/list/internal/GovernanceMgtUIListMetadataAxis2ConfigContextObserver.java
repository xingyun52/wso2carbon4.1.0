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
package org.wso2.carbon.governance.list.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.cache.ArtifactCache;
import org.wso2.carbon.governance.api.cache.ArtifactCacheFactory;
import org.wso2.carbon.governance.api.cache.ArtifactCacheManager;
import org.wso2.carbon.governance.list.util.CommonUtil;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;

import java.util.List;

public class GovernanceMgtUIListMetadataAxis2ConfigContextObserver implements
        Axis2ConfigurationContextObserver {

    private static final Log log =
            LogFactory.getLog(GovernanceMgtUIListMetadataAxis2ConfigContextObserver.class);

    @Override
    public void creatingConfigurationContext(int i) {

    }

    @Override
    public void createdConfigurationContext(ConfigurationContext configurationContext) {
        final RegistryService registryService = CommonUtil.getRegistryService();
        try {
            int tenantId =
                    PrivilegedCarbonContext.getCurrentContext(configurationContext).getTenantId();
            CommonUtil.configureGovernanceArtifacts(registryService.getRegistry(
                    CarbonConstants.REGISTRY_SYSTEM_USERNAME, tenantId),
                    configurationContext.getAxisConfiguration());
            ArtifactCache cache = ArtifactCacheManager.getCacheManager().getTenantArtifactCache(tenantId);
            if (cache == null) {
                cache = ArtifactCacheFactory.createArtifactCache();
                ArtifactCacheManager.getCacheManager().addTenantArtifactCache(cache,tenantId);
            }
            CommonUtil.schedulePreFetchTasks();
        } catch (RegistryException e) {
            log.error("Unable to load governance artifacts.", e);
        }

    }

    @Override
    public void terminatingConfigurationContext(ConfigurationContext configurationContext) {
        try {
            List<TaskInfo> tasks = CommonUtil.getTaskManager().getAllTasks();
            for (TaskInfo task : tasks) {
                CommonUtil.getTaskManager().deleteTask(task.getName());
            }
            int tenantId =
                    PrivilegedCarbonContext.getCurrentContext(configurationContext).getTenantId();
            ArtifactCacheManager.getCacheManager().removeTenantArtifactCache(tenantId);
        } catch (TaskException e) {
            log.error("Error while stopping the tasks", e);
        }
    }

    @Override
    public void terminatedConfigurationContext(ConfigurationContext configurationContext) {

    }

}
