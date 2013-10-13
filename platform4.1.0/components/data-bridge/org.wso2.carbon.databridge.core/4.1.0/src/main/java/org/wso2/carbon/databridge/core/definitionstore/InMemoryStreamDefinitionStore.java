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
package org.wso2.carbon.databridge.core.definitionstore;

import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The in memory implementation of the Event Stream definition Store
 */
public class InMemoryStreamDefinitionStore extends
        AbstractStreamDefinitionStore {

    private ConcurrentHashMap<String, ConcurrentHashMap<String, StreamDefinition>> streamDefinitionStore = new ConcurrentHashMap<String, ConcurrentHashMap<String, StreamDefinition>>();

    @Override
    protected boolean removeStreamDefinition(Credentials credentials, String name, String version) {
        if (streamDefinitionStore.containsKey(credentials.getDomainName())) {
            if (null != streamDefinitionStore.get(credentials.getDomainName()).remove(DataBridgeCommonsUtils.generateStreamId(name,version))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveStreamDefinitionToStore(Credentials credentials, StreamDefinition streamDefinition) throws StreamDefinitionStoreException {
        if (!streamDefinitionStore.containsKey(credentials.getDomainName())) {
            streamDefinitionStore.put(credentials.getDomainName(), new ConcurrentHashMap<String, StreamDefinition>());
        }
        streamDefinitionStore.get(credentials.getDomainName()).put(streamDefinition.getStreamId(), streamDefinition);
    }


    public StreamDefinition getStreamDefinitionFromStore(Credentials credentials,
                                                         String name, String version) throws StreamDefinitionStoreException {
        return getStreamDefinition(credentials, DataBridgeCommonsUtils.generateStreamId(name, version));
    }

    @Override
    protected StreamDefinition getStreamDefinitionFromStore(Credentials credentials, String streamId) throws StreamDefinitionStoreException {
        if (streamDefinitionStore.get(credentials.getDomainName()) != null) {
            return streamDefinitionStore.get(credentials.getDomainName()).get(streamId);
        }
        return null;
    }

    public Collection<StreamDefinition> getAllStreamDefinitionsFromStore(
            Credentials credentials) {
        ConcurrentHashMap<String, StreamDefinition> map = streamDefinitionStore.get(credentials.getDomainName());
        if (map != null) {
            return map.values();
        }
        return null;

    }

}
