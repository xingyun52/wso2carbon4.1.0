/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.governance.list.internal;

import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.governance.api.cache.ArtifactCache;
import org.wso2.carbon.governance.api.cache.ArtifactCacheFactory;
import org.wso2.carbon.governance.api.cache.ArtifactCacheManager;
import org.wso2.carbon.governance.api.util.GovernanceArtifactConfiguration;
import org.wso2.carbon.governance.api.util.GovernanceConstants;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.governance.list.util.CommonUtil;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.handlers.Handler;
import org.wso2.carbon.registry.core.jdbc.handlers.HandlerManager;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;
import org.wso2.carbon.registry.core.jdbc.handlers.filters.MediaTypeMatcher;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.component.xml.config.ManagementPermission;

import java.util.List;

/**
 * @scr.component name="org.wso2.carbon.governance.list"
 * immediate="true"
 * @scr.reference name="configuration.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 * cardinality="1..1" policy="dynamic" bind="setTaskService" unbind="unsetTaskService"
 */
public class GovernanceMgtUIListMetadataServiceComponent {

    private static Log log = LogFactory.getLog(GovernanceMgtUIListMetadataServiceComponent.class);
    private ServiceRegistration serviceRegistration;

    protected void activate(ComponentContext context) {
        final RegistryService registryService = CommonUtil.getRegistryService();
        try {
            UserRegistry registry =
                    registryService.getRegistry(CarbonConstants.REGISTRY_SYSTEM_USERNAME);
            CommonUtil.configureGovernanceArtifacts(registry,
                    CommonUtil.getConfigurationContext().getAxisConfiguration());

            ArtifactCache cache = ArtifactCacheManager.getCacheManager().getTenantArtifactCache(registry.getTenantId());
            if (cache == null) {
                cache = ArtifactCacheFactory.createArtifactCache();
                ArtifactCacheManager.getCacheManager().addTenantArtifactCache(cache,registry.getTenantId());
            }

            TaskService taskService = CommonUtil.getTaskService();
            if (taskService != null) {
                try {
                    TaskManager taskManager = taskService.getTaskManager(GovernanceConstants.PRE_FETCH_TASK);
                    CommonUtil.setTaskManager(taskManager);
                } catch (TaskException e) {
                    log.error("Error occurred while registering the task manager for prefetching tasks", e);
                }
            }

            serviceRegistration = context.getBundleContext().registerService(
                    Axis2ConfigurationContextObserver.class.getName(),
                    new GovernanceMgtUIListMetadataAxis2ConfigContextObserver(), null);

            HandlerManager handlerManager = registry.getRegistryContext().getHandlerManager();
            if (handlerManager != null) {
                handlerManager.addHandler(null,
                        new MediaTypeMatcher(
                                GovernanceConstants.GOVERNANCE_ARTIFACT_CONFIGURATION_MEDIA_TYPE),
                        new Handler() {
                            public void put(RequestContext requestContext)
                                    throws RegistryException {
                                if (!org.wso2.carbon.registry.extensions.utils.CommonUtil
                                        .isUpdateLockAvailable()) {
                                    return;
                                }
                                org.wso2.carbon.registry.extensions.utils.CommonUtil
                                        .acquireUpdateLock();
                                try {
                                    if (!CommonUtil.validateXMLConfigOnSchema(
                                            RegistryUtils.decodeBytes((byte[])
                                                    requestContext.getResource().getContent()),
                                            "rxt-ui-config")) {
                                        throw new RegistryException("Violation of RXT definition in" +
                                                " configuration file, follow the schema correctly..!!");
                                    }

                                    Registry userRegistry = requestContext.getRegistry();
                                    userRegistry.put(
                                            requestContext.getResourcePath().getPath(),
                                            requestContext.getResource());
                                    Registry systemRegistry = requestContext.getSystemRegistry();
                                    CommonUtil.configureGovernanceArtifacts(systemRegistry,
                                            CommonUtil.getConfigurationContext().getAxisConfiguration());
                                    requestContext.setProcessingComplete(true);
                                } finally {
                                    org.wso2.carbon.registry.extensions.utils.CommonUtil
                                            .releaseUpdateLock();
                                }
                            }

                            public void delete(RequestContext requestContext) throws RegistryException {
                                Resource resource = requestContext.getResource();
                                Object content = resource.getContent();
                                String elementString;
                                if (content instanceof String) {
                                    elementString = (String) content;
                                } else {
                                    elementString = RegistryUtils.decodeBytes((byte[]) content);
                                }
                                GovernanceArtifactConfiguration artifactConfiguration =
                                        GovernanceUtils.getGovernanceArtifactConfiguration(elementString);
                                String needToDelete = artifactConfiguration.getKey();

                                UserRegistry systemRegistry =
                                        registryService.getRegistry(CarbonConstants.REGISTRY_SYSTEM_USERNAME);
                                if (systemRegistry.resourceExists(GovernanceConstants.ARTIFACT_CONTENT_PATH + needToDelete)) {
                                    systemRegistry.delete(GovernanceConstants.ARTIFACT_CONTENT_PATH + needToDelete);
                                }
                                GovernanceUtils.loadGovernanceArtifacts((UserRegistry) systemRegistry);
                                List<GovernanceArtifactConfiguration> configurations =
                                        GovernanceUtils.findGovernanceArtifactConfigurations(systemRegistry);
                                for (GovernanceArtifactConfiguration configuration : configurations) {
                                    for (ManagementPermission uiPermission : configuration.getUIPermissions()) {
                                        String resourceId = RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                                uiPermission.getResourceId();
                                        if (systemRegistry.resourceExists(resourceId) && needToDelete.equals(configuration.getKey())) {
                                            systemRegistry.delete(resourceId);
                                        }
                                    }
                                }

                                unDeployCRUDService(artifactConfiguration,
                                        CommonUtil.getConfigurationContext().getAxisConfiguration());
                            }
                        });
                handlerManager.addHandler(null,
                        new MediaTypeMatcher() {
                            public boolean handlePut(RequestContext requestContext)
                                    throws RegistryException {
                                Resource resource = requestContext.getResource();
                                if (resource == null) {
                                    return false;
                                }
                                String mType = resource.getMediaType();
                                return mType != null && (invert != (mType.matches(
                                        "application/vnd\\.[a-zA-Z0-9.-]+\\+xml") & !mType.matches(
                                        "application/vnd.wso2-service\\+xml")));
                            }

                            @Override
                            public boolean handleCreateLink(RequestContext requestContext) throws RegistryException {
                                String targetPath = requestContext.getTargetPath();
                                if (!requestContext.getRegistry().resourceExists(targetPath)) {
                                    return false;
                                }
                                Resource targetResource = requestContext.getRegistry().get(targetPath);
                                String mType = targetResource.getMediaType();

                                return mType != null && (invert != (mType.matches(
                                        "application/vnd\\.[a-zA-Z0-9.-]+\\+xml") & !mType.matches(
                                        "application/vnd.wso2-service\\+xml")));
                            }
                        },
                        new Handler() {
/*
                            public void put(RequestContext requestContext)
                                    throws RegistryException {
                                if (!org.wso2.carbon.registry.extensions.utils.CommonUtil
                                        .isUpdateLockAvailable()) {
                                    return;
                                }
                                org.wso2.carbon.registry.extensions.utils.CommonUtil
                                        .acquireUpdateLock();
                                try {
                                    String id = requestContext.getResource().getUUID();
                                    if (id != null) {
                                        String path = requestContext.getResourcePath().getPath();
                                        Registry unchrootedSystemRegistry =
                                                org.wso2.carbon.registry.extensions.utils.CommonUtil
                                                        .getUnchrootedSystemRegistry(
                                                                requestContext);
                                    }
                                } finally {
                                    org.wso2.carbon.registry.extensions.utils.CommonUtil
                                            .releaseUpdateLock();
                                }
                            }
*/

                            @Override
                            public void createLink(RequestContext requestContext) throws RegistryException {
                                String symlinkPath = requestContext.getResourcePath().getPath();

                                if (!symlinkPath.startsWith(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH)) {
                                    throw new RegistryException("symlink creation is not allowed for artifact "
                                            + requestContext.getTargetPath());
                                }
                            }
                        });
            }
           CommonUtil.schedulePreFetchTasks();
        } catch (RegistryException e) {
            log.error("Unable to load governance artifacts.", e);
        }
        log.debug("******* Governance List Metadata bundle is activated ******* ");
    }

    private void unDeployCRUDService(GovernanceArtifactConfiguration configuration, AxisConfiguration axisConfig) {
        String singularLabel = configuration.getSingularLabel();

        try {
            if (axisConfig.getService(singularLabel) != null) {
                axisConfig.removeService(singularLabel);
            }
        } catch (AxisFault axisFault) {
            log.error(axisFault);
        }
    }
    protected void deactivate(ComponentContext context) {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
        try {
            List<TaskInfo> tasks = CommonUtil.getTaskManager().getAllTasks();
            for (TaskInfo task : tasks) {
                CommonUtil.getTaskManager().deleteTask(task.getName());
            }
//            ArtifactCacheManager.getCacheManager().removeTenantArtifactCache();
        } catch (TaskException e) {
            log.error("Error while stopping the tasks", e);
        }
        log.debug("Governance List Metadata bundle is deactivated ");
    }

    protected void setRegistryService(RegistryService registryService) {
        CommonUtil.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        CommonUtil.setRegistryService(null);
    }

    protected void setConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        log.debug("The Configuration Context Service was set");
        if (configurationContextService != null) {
            CommonUtil.setConfigurationContext(configurationContextService.getServerConfigContext());
        }
    }

    protected void unsetConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        CommonUtil.setConfigurationContext(null);
    }

    protected void setTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Task Service");
        }
        CommonUtil.setTaskService(taskService);
    }

    protected void unsetTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Unsetting the Task Service");
        }
        CommonUtil.setTaskService(null);
    }

}
