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

package org.wso2.carbon.databridge.core.internal;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.DifferentStreamDefinitionAlreadyDefinedException;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;
import org.wso2.carbon.databridge.commons.utils.EventDefinitionConverterUtils;
import org.wso2.carbon.databridge.core.*;
import org.wso2.carbon.databridge.core.Utils.AgentSession;
import org.wso2.carbon.databridge.core.Utils.EventComposite;
import org.wso2.carbon.databridge.core.conf.DataBridgeConfiguration;
import org.wso2.carbon.databridge.core.definitionstore.AbstractStreamDefinitionStore;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.carbon.databridge.core.internal.queue.EventQueue;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispactches events  and their definitions subscribers
 */
public class EventDispatcher {

    private List<AgentCallback> subscribers = new ArrayList<AgentCallback>();
    private List<RawDataAgentCallback> rawDataSubscribers = new ArrayList<RawDataAgentCallback>();
    private AbstractStreamDefinitionStore streamDefinitionStore;
    private Map<String, StreamTypeHolder> domainNameStreamTypeHolderCache = new ConcurrentHashMap<String, StreamTypeHolder>();
    private EventQueue eventQueue;

    private static final Log log = LogFactory.getLog(EventDispatcher.class);


    public EventDispatcher(AbstractStreamDefinitionStore streamDefinitionStore,
                           DataBridgeConfiguration dataBridgeConfiguration) {
        this.eventQueue = new EventQueue(subscribers, rawDataSubscribers, dataBridgeConfiguration);
        this.streamDefinitionStore = streamDefinitionStore;
    }

    public void addCallback(AgentCallback agentCallback) {
        subscribers.add(agentCallback);
    }

    /**
     * Add thrift subscribers
     *
     * @param agentCallback
     */
    public void addCallback(RawDataAgentCallback agentCallback) {
        rawDataSubscribers.add(agentCallback);
    }

    public synchronized String defineStream(String streamDefinition, AgentSession agentSession)
            throws MalformedStreamDefinitionException,
                   DifferentStreamDefinitionAlreadyDefinedException,
                   StreamDefinitionStoreException {
        StreamDefinition newStreamDefinition = EventDefinitionConverterUtils.convertFromJson(streamDefinition);

        StreamTypeHolder streamTypeHolder = getStreamDefinitionHolder(agentSession.getCredentials());
        StreamAttributeComposite attributeComposite = streamTypeHolder.getAttributeComposite(newStreamDefinition.getStreamId());
        if (attributeComposite != null) {

            StreamDefinition existingStreamDefinition = attributeComposite.getStreamDefinition();
            if (!existingStreamDefinition.equals(newStreamDefinition)) {
                throw new DifferentStreamDefinitionAlreadyDefinedException("Similar event stream for " + newStreamDefinition + " with the same name and version already exist: " + streamDefinitionStore.getStreamDefinition(agentSession.getCredentials(), newStreamDefinition.getName(), newStreamDefinition.getVersion()));
            }
            newStreamDefinition = existingStreamDefinition;

        } else {
            for (StreamAttributeComposite aAttributeComposite : streamTypeHolder.getAttributeCompositeMap().values()) {
                validateStreamDefinition(newStreamDefinition, aAttributeComposite.getStreamDefinition());
            }

            updateDomainNameStreamTypeHolderCache(newStreamDefinition, agentSession.getCredentials());
            streamDefinitionStore.saveStreamDefinition(agentSession.getCredentials(), newStreamDefinition);

        }

        for (AgentCallback agentCallback : subscribers) {
            agentCallback.definedStream(newStreamDefinition, agentSession.getCredentials());
        }
        for (RawDataAgentCallback agentCallback : rawDataSubscribers) {
            agentCallback.definedStream(newStreamDefinition, agentSession.getCredentials());
        }
        return newStreamDefinition.getStreamId();
    }

    private void validateStreamDefinition(StreamDefinition newStreamDefinition,
                                          StreamDefinition existingStreamDefinition)
            throws DifferentStreamDefinitionAlreadyDefinedException {
        if (newStreamDefinition.getName().equals(existingStreamDefinition.getName())) {
            validateAttributes(newStreamDefinition.getMetaData(), existingStreamDefinition.getMetaData(), "meta", newStreamDefinition, existingStreamDefinition);
            validateAttributes(newStreamDefinition.getCorrelationData(), existingStreamDefinition.getCorrelationData(), "correlation", newStreamDefinition, existingStreamDefinition);
            validateAttributes(newStreamDefinition.getPayloadData(), existingStreamDefinition.getPayloadData(), "payload", newStreamDefinition, existingStreamDefinition);
        }
    }

    private void validateAttributes(List<Attribute> newAttributes,
                                    List<Attribute> existingAttributes, String type,
                                    StreamDefinition newStreamDefinition,
                                    StreamDefinition existingStreamDefinition)
            throws DifferentStreamDefinitionAlreadyDefinedException {
        if (newAttributes != null && existingAttributes != null) {
            for (Attribute attribute : newAttributes) {
                for (Attribute existingAttribute : existingAttributes) {
                    if (attribute.getName().equals(existingAttribute.getName())) {
                        if (attribute.getType() != existingAttribute.getType()) {
                            throw new DifferentStreamDefinitionAlreadyDefinedException("Attribute type mismatch " + type + " " +
                                                                                       attribute.getName() + " type:" + attribute.getType() +
                                                                                       " was already defined with type:" + existingAttribute.getType() +
                                                                                       " in " + existingStreamDefinition + ", hence " + newStreamDefinition +
                                                                                       " cannot be defined");
                        }
                    }
                }
            }
        }
    }


    public void publish(Object eventBundle, AgentSession agentSession,
                        EventConverter eventConverter) {
        eventQueue.publish(new EventComposite(eventBundle, getStreamDefinitionHolder(agentSession.getCredentials()), agentSession, eventConverter));
    }

    private StreamTypeHolder getStreamDefinitionHolder(Credentials credentials) {
        // this will occur only outside of carbon (ex: Siddhi)

        StreamTypeHolder streamTypeHolder = domainNameStreamTypeHolderCache.get(credentials.getDomainName());

        if (streamTypeHolder != null) {
            if (log.isDebugEnabled()) {
                String logMsg = "Event stream holder for domain name : " + credentials.getDomainName() + " : \n ";
                logMsg += "Meta, Correlation & Payload Data Type Map : ";
                for (Map.Entry entry : streamTypeHolder.getAttributeCompositeMap().entrySet()) {
                    logMsg += "StreamID=" + entry.getKey() + " :  ";
                    logMsg += "Meta= " + Arrays.deepToString(((StreamAttributeComposite) entry.getValue()).getAttributeTypes()[0]) + " :  ";
                    logMsg += "Correlation= " + Arrays.deepToString(((StreamAttributeComposite) entry.getValue()).getAttributeTypes()[1]) + " :  ";
                    logMsg += "Payload= " + Arrays.deepToString(((StreamAttributeComposite) entry.getValue()).getAttributeTypes()[2]) + "\n";
                }
                log.debug(logMsg);
            }
            return streamTypeHolder;
        } else {
            return initDomainNameStreamTypeHolderCache(credentials);
        }
    }

    private synchronized void updateDomainNameStreamTypeHolderCache(
            StreamDefinition streamDefinition, Credentials credentials) {
        StreamTypeHolder streamTypeHolder = getStreamDefinitionHolder(credentials);
        streamTypeHolder.putStreamDefinition(streamDefinition);
    }

    private synchronized StreamTypeHolder initDomainNameStreamTypeHolderCache(
            Credentials credentials) {
        StreamTypeHolder streamTypeHolder = domainNameStreamTypeHolderCache.get(credentials.getDomainName());
        if (null == streamTypeHolder) {
            streamTypeHolder = new StreamTypeHolder(credentials.getDomainName());
            Collection<StreamDefinition> allStreamDefinitions =
                    streamDefinitionStore.getAllStreamDefinitions(credentials);
            if (null != allStreamDefinitions) {
                for (StreamDefinition aStreamDefinition : allStreamDefinitions) {
                    streamTypeHolder.putStreamDefinition(aStreamDefinition);
                    for (AgentCallback agentCallback : subscribers) {
                        agentCallback.definedStream(aStreamDefinition, credentials);
                    }
                    for (RawDataAgentCallback agentCallback : rawDataSubscribers) {
                        agentCallback.definedStream(aStreamDefinition, credentials);
                    }
                }
            }
            domainNameStreamTypeHolderCache.put(credentials.getDomainName(), streamTypeHolder);
        }
        return streamTypeHolder;
    }


    public List<AgentCallback> getSubscribers() {
        return subscribers;
    }

    public List<RawDataAgentCallback> getRawDataSubscribers() {
        return rawDataSubscribers;
    }

    public String findStreamId(Credentials credentials, String streamName, String streamVersion)
            throws StreamDefinitionStoreException {
        StreamTypeHolder streamTypeHolder = getStreamDefinitionHolder(credentials);
        StreamAttributeComposite attributeComposite = streamTypeHolder.getAttributeComposite(DataBridgeCommonsUtils.generateStreamId(streamName, streamVersion));
        if (attributeComposite != null) {
            return attributeComposite.getStreamDefinition().getStreamId();
        }
        return null;
    }

    public boolean deleteStream(Credentials credentials, String streamName, String streamVersion) {
        String streamId = DataBridgeCommonsUtils.generateStreamId(streamName, streamVersion);
        StreamDefinition streamDefinition = removeStreamDefinitionFromStreamTypeHolder(credentials, streamId);
        if (streamDefinition != null) {
            for (AgentCallback agentCallback : subscribers) {
                agentCallback.removeStream(streamDefinition, credentials);
            }
            for (RawDataAgentCallback agentCallback : rawDataSubscribers) {
                agentCallback.removeStream(streamDefinition, credentials);
            }
        }
        return streamDefinitionStore.deleteStreamDefinition(credentials, streamName, streamVersion);
    }

    private synchronized StreamDefinition removeStreamDefinitionFromStreamTypeHolder(
            Credentials credentials,
            String streamId) {
        StreamTypeHolder streamTypeHolder = domainNameStreamTypeHolderCache.get(credentials.getDomainName());
        if (streamTypeHolder != null) {
            StreamAttributeComposite attributeComposite = streamTypeHolder.getAttributeCompositeMap().remove(streamId);
            return attributeComposite.getStreamDefinition();
        }
        return null;
    }
}
