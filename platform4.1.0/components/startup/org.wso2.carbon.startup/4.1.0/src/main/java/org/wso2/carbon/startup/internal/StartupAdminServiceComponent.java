/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.startup.internal;

import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Startup;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.config.xml.MultiXMLConfigurationBuilder;
import org.apache.synapse.deployers.SynapseArtifactDeploymentStore;
import org.apache.synapse.task.service.TaskManagementService;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.mediation.initializer.ServiceBusConstants;
import org.wso2.carbon.mediation.initializer.ServiceBusUtils;
import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;
import org.wso2.carbon.mediation.initializer.services.SynapseRegistrationsService;
import org.wso2.carbon.startup.StartupAdminService;
import org.wso2.carbon.startup.StartupJobMetaDataProviderService;
import org.wso2.carbon.startup.StartupTaskDeployer;
import org.wso2.carbon.startup.util.ConfigHolder;
import org.wso2.carbon.task.services.JobMetaDataProviderService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 * @scr.component name="org.wso2.carbon.startup" immediate="true"
 * @scr.reference name="synapse.env.service"
 * interface="org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService"
 * cardinality="1..n" policy="dynamic"
 * bind="setSynapseEnvironmentService" unbind="unsetSynapseEnvironmentService"
 * @scr.reference name="synapse.registrations.service"
 * interface="org.wso2.carbon.mediation.initializer.services.SynapseRegistrationsService"
 * cardinality="1..n" policy="dynamic" bind="setSynapseRegistrationsService"
 * unbind="unsetSynapseRegistrationsService"
 */
@SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
public class StartupAdminServiceComponent {

    private static final Log log = LogFactory.getLog(StartupAdminServiceComponent.class);

    private Map<Integer, SynapseEnvironmentService> synapseEnvironmentServices =
            new HashMap<Integer, SynapseEnvironmentService>();

    /*private TaskDescriptionRepositoryService repositoryService;*/

    private boolean initialized = false;

    protected void activate(ComponentContext context) throws Exception {
         try {
            initialized = true;
            SynapseEnvironmentService synEnvService = synapseEnvironmentServices.get(
                            MultitenantConstants.SUPER_TENANT_ID);
             if (synEnvService != null) {
                 context.getBundleContext().registerService(TaskManagementService.class.getName(),
                         new StartupAdminService(), null);
                 context.getBundleContext().registerService(
                         JobMetaDataProviderService.class.getName(),
                         new StartupJobMetaDataProviderService(), null);
                 registerDeployer(synEnvService.getConfigurationContext().getAxisConfiguration(),
                         synEnvService.getSynapseEnvironment());
             } else {
                log.error("Couldn't initialize the StartupManager, " +
                    "SynapseEnvironment service and/or TaskDescriptionRepositoryService not found");
             }
        } catch (Throwable t) {
            log.error("Couldn't initialize the StartupManager, " +
                    "SynapseEnvironment service and/or TaskDescriptionRepositoryService not found");
        }
    }

    protected void deactivate(ComponentContext context) throws Exception {
        Set<Map.Entry<Integer, SynapseEnvironmentService>> entrySet =
                synapseEnvironmentServices.entrySet();
        for (Map.Entry<Integer, SynapseEnvironmentService> entry : entrySet) {
            unregistryDeployer(
                    entry.getValue().getConfigurationContext().getAxisConfiguration(),
                    entry.getValue().getSynapseEnvironment());
        }
    }

    private void registerDeployer(AxisConfiguration axisConfig, SynapseEnvironment synEnv) {
        DeploymentEngine deploymentEngine = (DeploymentEngine) axisConfig.getConfigurator();

        SynapseArtifactDeploymentStore deploymentStore = synEnv.getSynapseConfiguration().
                getArtifactDeploymentStore();

        String synapseConfigPath = ServiceBusUtils.getSynapseConfigAbsPath(synEnv.
                                getServerContextInformation());
        String taskDirDirPath = synapseConfigPath
                + File.separator + MultiXMLConfigurationBuilder.TASKS_DIR;

        for (Startup stp : synEnv.getSynapseConfiguration().getStartups()) {
            if (stp.getFileName() != null) {
                deploymentStore.addRestoredArtifact(
                        taskDirDirPath + File.separator + stp.getFileName());
            }
        }
        deploymentEngine.addDeployer(new StartupTaskDeployer(),
                taskDirDirPath, ServiceBusConstants.ARTIFACT_EXTENSION);
    }

    protected void setSynapseEnvironmentService(
            SynapseEnvironmentService synEnvSvc) {
        boolean alreadyCreated = synapseEnvironmentServices.containsKey(synEnvSvc.getTenantId());

        synapseEnvironmentServices.put(synEnvSvc.getTenantId(), synEnvSvc);
        if (initialized) {
            int tenantId = synEnvSvc.getTenantId();
            AxisConfiguration axisConfiguration = synEnvSvc.
                    getConfigurationContext().getAxisConfiguration();

            if (!alreadyCreated) {
                registerDeployer(
                        synEnvSvc.getConfigurationContext().getAxisConfiguration(),
                        synEnvSvc.getSynapseEnvironment());
            }
        }
    }

    protected void unsetSynapseEnvironmentService(
            SynapseEnvironmentService synapseEnvironmentService) {
        synapseEnvironmentServices.remove(synapseEnvironmentService.getTenantId());        
    }

    protected void setSynapseRegistrationsService(
            SynapseRegistrationsService synapseRegistrationsService) {

    }

    protected void unsetSynapseRegistrationsService(
            SynapseRegistrationsService synapseRegistrationsService) {
        int tenantId = synapseRegistrationsService.getTenantId();
        if (synapseEnvironmentServices.containsKey(tenantId)) {
            SynapseEnvironment env = synapseEnvironmentServices.get(tenantId).
                    getSynapseEnvironment();

            synapseEnvironmentServices.remove(synapseRegistrationsService.getTenantId());

            AxisConfiguration axisConfig = synapseRegistrationsService.getConfigurationContext().
                    getAxisConfiguration();
            if (axisConfig != null) {
                unregistryDeployer(axisConfig, env);
            }
        }
    }

    /**
     * Un-registers the Task Deployer.
     *
     * @param axisConfig AxisConfiguration to which this deployer belongs
     * @param synapseEnvironment SynapseEnvironment to which this deployer belongs
     */
    private void unregistryDeployer(AxisConfiguration axisConfig,
                                    SynapseEnvironment synapseEnvironment) {
        DeploymentEngine deploymentEngine = (DeploymentEngine) axisConfig.getConfigurator();
        String synapseConfigPath = ServiceBusUtils.getSynapseConfigAbsPath(
                synapseEnvironment.getServerContextInformation());
        String proxyDirPath = synapseConfigPath
                + File.separator + MultiXMLConfigurationBuilder.TASKS_DIR;
        deploymentEngine.removeDeployer(
                proxyDirPath, ServiceBusConstants.ARTIFACT_EXTENSION);
    }
}
