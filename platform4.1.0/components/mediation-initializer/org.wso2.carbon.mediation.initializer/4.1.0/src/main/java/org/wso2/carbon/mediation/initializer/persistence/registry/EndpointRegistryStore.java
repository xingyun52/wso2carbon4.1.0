/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.mediation.initializer.persistence.registry;

import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.mediation.initializer.ServiceBusConstants;
import org.wso2.carbon.mediation.initializer.ServiceBusInitializer;
import org.wso2.carbon.application.deployer.AppDeployerUtils;
import org.wso2.carbon.application.deployer.synapse.SynapseAppDeployerConstants;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.config.xml.XMLConfigConstants;

import java.util.Collection;
import java.util.ArrayList;

/**
 * 
 */
public class EndpointRegistryStore extends AbstractRegistryStore {

    public EndpointRegistryStore(UserRegistry registry, String configName) {
        super(registry, configName);
        // if endpoints collection does not exists, create a one
        createCollection(getConfigurationPath());
    }

    public Collection<OMElement> getElements() {
        Collection<OMElement> endpointElements = new ArrayList<OMElement>();
        try {
            endpointElements = getChildElementsInPath(getConfigurationPath());
        } catch (RegistryException e) {
            handleException("Couldn't get the list of endpoints from the registry in path : "
                    + getConfigurationPath(), e);
        }
        return endpointElements;
    }

    public OMElement getElement(String name) {
        // todo
        return null;
    }

    public void persistElement(String name, OMElement element, String fileName) {
        if (element.getLocalName().equals(
                XMLConfigConstants.ENDPOINT_ELT.getLocalPart())) {
            if (log.isDebugEnabled()) {
                log.debug("Persisting endpoint : " + name + " to the registry");
            }

            String endpointPath = getConfigurationPath() + RegistryConstants.PATH_SEPARATOR + name;

            AppDeployerUtils.attachArtifactToOwnerApp(fileName, SynapseAppDeployerConstants
                    .ENDPOINT_TYPE, name, registry.getTenantId());

            try {
                persistElement(element, endpointPath, fileName);
            } catch (RegistryException e) {
                handleException("Unable to persist the sequence in the path : " + endpointPath, e);
            }
        } else {
            handleException("The element provided to persist is not a sequence");
        }
    }

    public void deleteElement(String name) {
        String endpointPath = getConfigurationPath() + RegistryConstants.PATH_SEPARATOR + name;
        try {
            if (registry.resourceExists(endpointPath)) {
                registry.delete(endpointPath);
            }
        } catch (RegistryException e) {
            handleException("Error in deleting the endpoint at path : " + endpointPath, e);
        }
    }

    protected String getConfigurationPath() {
        return getConfigurationRoot() + RegistryConstants.PATH_SEPARATOR +
                ServiceBusConstants.RegistryStore.ENDPOINT_REGISTRY;
    }

}
