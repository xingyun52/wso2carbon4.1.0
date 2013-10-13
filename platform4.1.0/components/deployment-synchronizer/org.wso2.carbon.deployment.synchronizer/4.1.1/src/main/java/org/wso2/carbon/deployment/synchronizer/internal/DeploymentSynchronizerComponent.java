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

package org.wso2.carbon.deployment.synchronizer.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.deployment.synchronizer.ArtifactRepository;
import org.wso2.carbon.deployment.synchronizer.internal.util.RepositoryConfigParameter;
import org.wso2.carbon.deployment.synchronizer.internal.util.RepositoryReferenceHolder;
import org.wso2.carbon.deployment.synchronizer.registry.RegistryBasedArtifactRepository;
import org.wso2.carbon.deployment.synchronizer.DeploymentSynchronizerException;
import org.wso2.carbon.deployment.synchronizer.internal.repository.CarbonRepositoryUtils;
import org.wso2.carbon.deployment.synchronizer.services.DeploymentSynchronizerService;
import org.wso2.carbon.deployment.synchronizer.internal.util.ServiceReferenceHolder;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @scr.component name="org.wso2.carbon.deployment.synchronizer.XXX" immediate="true"
 * @scr.reference name="configuration.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="registry.service" immediate="true"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="repository.reference.service"
 * interface="org.wso2.carbon.deployment.synchronizer.ArtifactRepository" cardinality="0..n"
 * policy="dynamic" bind="addArtifactRepository" unbind="removeArtifactRepository"
 */
public class DeploymentSynchronizerComponent {

    private static final Log log = LogFactory.getLog(DeploymentSynchronizerComponent.class);

    private ServiceRegistration observerRegistration;
    private ServiceRegistration registryDepSynServiceRegistration;


    protected void activate(ComponentContext context) {

        // Initialize the repository manager so that it can be later used to
        // start a synchronizer (eg: via the UI)
        ServerConfiguration serverConfig = ServerConfiguration.getInstance();
        DeploymentSynchronizationManager.getInstance().init(serverConfig);

        //Register the Registry Based Artifact Repository
        ArtifactRepository registryBasedArtifactRepository =  new RegistryBasedArtifactRepository();
        registryDepSynServiceRegistration =
                context.getBundleContext().registerService(ArtifactRepository.class.getName(),
                                                           registryBasedArtifactRepository, null);

        try {
            initDeploymentSynchronizerForSuperTenant();
        } catch (DeploymentSynchronizerException e) {
            log.error("Error while initializing a deployment synchronizer for the super tenant " +
                      "Carbon repository", e);
        }

        // Register an observer so we can track tenant ConfigurationContext creation and
        // do the synchronization operations as necessary
        BundleContext bundleContext = context.getBundleContext();
        observerRegistration = bundleContext.registerService(Axis2ConfigurationContextObserver.class.getName(),
                                                             new DeploymentSyncAxis2ConfigurationContextObserver(), null);

        // register the OSGi service
        bundleContext.registerService(new String[]{DeploymentSynchronizerService.class.getName(),
                                                  org.wso2.carbon.core.deployment.DeploymentSynchronizer.class.getName()},
                                      new DeploymentSynchronizerServiceImpl(), null);



        log.debug("Deployment synchronizer component activated");
    }

    private void initDeploymentSynchronizerForSuperTenant() throws DeploymentSynchronizerException {
        if (!CarbonRepositoryUtils.isSynchronizerEnabled(MultitenantConstants.SUPER_TENANT_ID)) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Initializing the deployment synchronizer for super tenant");
        }

        CarbonRepositoryUtils.newCarbonRepositorySynchronizer(MultitenantConstants.SUPER_TENANT_ID);
    }

    protected void deactivate(ComponentContext context) {
        DeploymentSynchronizationManager.getInstance().shutdown();

        if (observerRegistration != null) {
            observerRegistration.unregister();
            observerRegistration = null;
        }

        if(registryDepSynServiceRegistration != null){
            registryDepSynServiceRegistration.unregister();
            registryDepSynServiceRegistration = null;
        }

        log.debug("Deployment synchronizer component deactivated");
    }

    protected void setConfigurationContextService(ConfigurationContextService service) {
        if (log.isDebugEnabled()) {
            log.debug("Deployment synchronizer component bound to the " +
                    "configuration context service");
        }
        ServiceReferenceHolder.setConfigurationContextService(service);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService service) {
        if (log.isDebugEnabled()) {
            log.debug("Deployment synchronizer component unbound from the " +
                    "configuration context service");
        }
        ServiceReferenceHolder.setConfigurationContextService(null);
    }

    protected void setRegistryService(RegistryService service) {
        if (log.isDebugEnabled()) {
            log.debug("Deployment synchronizer component bound to the registry service");
        }
        ServiceReferenceHolder.setRegistryService(service);
    }

    protected void unsetRegistryService(RegistryService service) {
        if (log.isDebugEnabled()) {
            log.debug("Deployment synchronizer component unbound from the registry service");
        }
        ServiceReferenceHolder.setRegistryService(null);
    }

    protected void addArtifactRepository(ArtifactRepository artifactRepository){
        RepositoryReferenceHolder repositoryReferenceHolder = RepositoryReferenceHolder.getInstance();
        repositoryReferenceHolder.addRepository(artifactRepository, artifactRepository.getParameters());
    }

    protected void removeArtifactRepository(ArtifactRepository artifactRepository){
        RepositoryReferenceHolder repositoryReferenceHolder = RepositoryReferenceHolder.getInstance();
        repositoryReferenceHolder.removeRepository(artifactRepository);
    }

    /**
     *
 * @scr.reference name="registry.eventing.service" immediate="true"
 * interface="org.wso2.carbon.registry.eventing.services.EventingService" cardinality="0..1"
 * policy="dynamic" bind="setEventingService" unbind="unsetEventingService"
     */
    /*protected void setEventingService(EventingService service) {
        if (log.isDebugEnabled()) {
            log.debug("Deployment synchronizer component bound to the registry eventing service");
        }
        ServiceReferenceHolder.setEventingService(service);

        // Call delayed auto checkout initialization
        // Because we are not waiting on the EventingService, the auto checkout feature
        // of some synchronizers may not get initialized at the first time
        if (observerRegistration != null) {
            DeploymentSynchronizationManager.getInstance().initDelayedAutoCheckout();
        }
    }

    protected void unsetEventingService(EventingService service) {
        if (log.isDebugEnabled()) {
            log.debug("Deployment synchronizer component unbound from the registry eventing service");
        }
        ServiceReferenceHolder.setEventingService(null);
    }*/
}
