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
package org.wso2.carbon.databridge.streamdefn.registry.datastore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.carbon.databridge.commons.utils.EventDefinitionConverterUtils;
import org.wso2.carbon.databridge.core.definitionstore.AbstractStreamDefinitionStore;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.carbon.databridge.streamdefn.registry.internal.ServiceHolder;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The in memory implementation of the Event Stream definition Store
 */
public class RegistryStreamDefinitionStore extends
                                           AbstractStreamDefinitionStore {
    private Log log = LogFactory.getLog(RegistryStreamDefinitionStore.class);

    private static final String STREAM_DEFINITION_STORE = "/StreamDefinitions";


    public StreamDefinition getStreamDefinitionFromStore(Credentials credentials,
                                                         String name, String version)
            throws StreamDefinitionStoreException {
        try {
            UserRegistry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(credentials.getUsername(), credentials.getPassword());
            if (registry.resourceExists(STREAM_DEFINITION_STORE + RegistryConstants.PATH_SEPARATOR + name + RegistryConstants.PATH_SEPARATOR + version)) {
                Resource resource = registry.get(STREAM_DEFINITION_STORE + RegistryConstants.PATH_SEPARATOR + name + RegistryConstants.PATH_SEPARATOR + version);
                Object content = resource.getContent();
                if (content != null) {
                    return EventDefinitionConverterUtils.convertFromJson(RegistryUtils.decodeBytes((byte[]) resource.getContent()));
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error in getting Stream Definition " + name + ":" + version);
            throw new StreamDefinitionStoreException("Error in getting Stream Definition " + name + ":" + version, e);
        }
    }

    @Override
    protected StreamDefinition getStreamDefinitionFromStore(Credentials credentials,
                                                            String streamId)
            throws StreamDefinitionStoreException {
        return getStreamDefinitionFromStore(credentials, DataBridgeCommonsUtils.getStreamNameFromStreamId(streamId),
                                            DataBridgeCommonsUtils.getStreamVersionFromStreamId(streamId));
    }

    @Override
    protected boolean removeStreamDefinition(Credentials credentials, String name, String version) {
        try {
            UserRegistry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(credentials.getUsername(), credentials.getPassword());
            registry.delete(STREAM_DEFINITION_STORE + RegistryConstants.PATH_SEPARATOR + name + RegistryConstants.PATH_SEPARATOR + version);
            return !registry.resourceExists(STREAM_DEFINITION_STORE + RegistryConstants.PATH_SEPARATOR + name + RegistryConstants.PATH_SEPARATOR + version);
        } catch (RegistryException e) {
            log.error("Error in deleting Stream Definition " + name + ":" + version);
            return false;
        }
    }

    @Override
    protected void saveStreamDefinitionToStore(Credentials credentials,
                                               StreamDefinition streamDefinition)
            throws StreamDefinitionStoreException {
        try {
            UserRegistry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(credentials.getUsername(), credentials.getPassword());
            Resource resource = registry.newResource();
            resource.setContent(EventDefinitionConverterUtils.convertToJson(streamDefinition));
            resource.setMediaType("application/json");
            registry.put(STREAM_DEFINITION_STORE + RegistryConstants.PATH_SEPARATOR + streamDefinition.getName() + RegistryConstants.PATH_SEPARATOR + streamDefinition.getVersion(), resource);
        } catch (RegistryException e) {
            log.error("Error in saving Stream Definition " + streamDefinition);
        }
    }

    public Collection<StreamDefinition> getAllStreamDefinitionsFromStore(
            Credentials credentials) {
        ConcurrentHashMap<String, StreamDefinition> map = new ConcurrentHashMap<String, StreamDefinition>();

        try {
            UserRegistry registry = ServiceHolder.getRegistryService().getGovernanceUserRegistry(credentials.getUsername(), credentials.getPassword());

            if (!registry.resourceExists(STREAM_DEFINITION_STORE)) {
                registry.put(STREAM_DEFINITION_STORE, registry.newCollection());
            } else {
                org.wso2.carbon.registry.core.Collection collection = (org.wso2.carbon.registry.core.Collection) registry.get(STREAM_DEFINITION_STORE);
                for (String streamNameCollection : collection.getChildren()) {

                    org.wso2.carbon.registry.core.Collection innerCollection = (org.wso2.carbon.registry.core.Collection) registry.get(streamNameCollection);
                    for (String streamVersionCollection : innerCollection.getChildren()) {

                        Resource resource = (Resource) registry.get(streamVersionCollection);
                        try {
                            StreamDefinition streamDefinition = EventDefinitionConverterUtils.convertFromJson(RegistryUtils.decodeBytes((byte[]) resource.getContent()));
                            map.put(streamDefinition.getStreamId(), streamDefinition);
                        } catch (Throwable e) {
                            log.error("Error in retrieving streamDefinition from the resource at " + resource.getPath(), e);
                        }
                    }
                }
            }

        } catch (RegistryException e) {
            log.error("Error in retrieving streamDefinitions from the registry", e);
        }
        return map.values();

    }

}
