package org.wso2.carbon.databridge.agent.thrift.lb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AgentHolder;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.util.ArrayList;
import java.util.Map;

/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class LoadBalancingDataPublisher {
    private static Log log = LogFactory.getLog(AsyncDataPublisher.class);

    private ArrayList<ReceiverGroup> receiverGroups;

    public LoadBalancingDataPublisher(ArrayList<ReceiverGroup> receiverGroups) {
        this.receiverGroups = receiverGroups;
        for (ReceiverGroup group : receiverGroups) {
            group.createDataPublishers(AgentHolder.getOrCreateAgent());
        }
    }

    public LoadBalancingDataPublisher(ArrayList<ReceiverGroup> receiverGroups, Agent agent) {
        this.receiverGroups = receiverGroups;
        AgentHolder.setAgent(agent);
        for (ReceiverGroup group : receiverGroups) {
            group.createDataPublishers(agent);
        }
    }

    public void addReceiverGroup(ReceiverGroup aReceiverGroup) {
        aReceiverGroup.createDataPublishers(AgentHolder.getOrCreateAgent());
        this.receiverGroups.add(aReceiverGroup);
    }

    /**
     * Publish the event asynchronously.
     * <p/>
     * If the data publisher hasn't been initialized properly, the events will be pushed in the queue
     * If the stream id is not in the cache the event will be pushed in the queue
     * else the stream id from the cache will be used to publish the event
     *
     * @param streamName           Name of the stream which the events is for
     * @param streamVersion        Version of the stream which the events is for
     * @param streamName           Name of the stream which the events is for
     * @param streamVersion        Version of the stream which the events is for
     * @param timeStamp            Timestamp of the event
     * @param metaDataArray        metadata array of the event
     * @param correlationDataArray correlation array of the event
     * @param payloadDataArray     payload array of the event
     * @param arbitraryDataMap     arbitrary mata (as meta.<key_name>),correlation (as correlation.<key_name>) & payload (as <key_name>) data as key-value pairs
     * @throws AgentException
     */
    public void publish(String streamName, String streamVersion,
                        long timeStamp,
                        Object[] metaDataArray, Object[] correlationDataArray,
                        Object[] payloadDataArray, Map<String, String> arbitraryDataMap) throws AgentException {
        for (ReceiverGroup aGroup : receiverGroups) {
            aGroup.publish(streamName, streamVersion, timeStamp,
                    metaDataArray, correlationDataArray, payloadDataArray, arbitraryDataMap);
        }
    }

    /**
     * Publish the event asynchronously.
     * <p/>
     * If the data publisher hasn't been initialized properly, the events will be pushed in the queue
     * If the stream id is not in the cache the event will be pushed in the queue
     * else the stream id from the cache will be used to publish the event
     *
     * @param streamName           Name of the stream which the events is for
     * @param streamVersion        Version of the stream which the events is for
     * @param streamName           Name of the stream which the events is for
     * @param streamVersion        Version of the stream which the events is for
     * @param timeStamp            Timestamp of the event
     * @param metaDataArray        metadata array of the event
     * @param correlationDataArray correlation array of the event
     * @param payloadDataArray     payload array of the event
     * @throws AgentException
     */
    public void publish(String streamName, String streamVersion,
                        long timeStamp,
                        Object[] metaDataArray, Object[] correlationDataArray,
                        Object[] payloadDataArray) throws AgentException {
        for (ReceiverGroup aGroup : receiverGroups) {
            aGroup.publish(streamName, streamVersion, timeStamp,
                    metaDataArray, correlationDataArray, payloadDataArray, null);
        }
    }

    /**
     * Publish the event asynchronously.
     * <p/>
     * If the data publisher hasn't been initialized properly, the events will be pushed in the queue
     * If the stream id is not in the cache the event will be pushed in the queue
     * else the stream id from the cache will be used to publish the event
     *
     * @param streamName           Name of the stream which the events is for
     * @param streamVersion        Version of the stream which the events is for
     * @param streamName           Name of the stream which the events is for
     * @param streamVersion        Version of the stream which the events is for
     * @param metaDataArray        metadata array of the event
     * @param correlationDataArray correlation array of the event
     * @param payloadDataArray     payload array of the event
     * @param arbitraryDataMap     arbitrary mata (as meta.<key_name>),correlation (as correlation.<key_name>) & payload (as <key_name>) data as key-value pairs
     * @throws AgentException
     */
    public void publish(String streamName, String streamVersion,
                        Object[] metaDataArray, Object[] correlationDataArray,
                        Object[] payloadDataArray, Map<String, String> arbitraryDataMap) throws AgentException {
        for (ReceiverGroup aGroup : receiverGroups) {
            aGroup.publish(streamName, streamVersion,
                    metaDataArray, correlationDataArray, payloadDataArray, arbitraryDataMap);
        }
    }

    /**
     * Publish the event asynchronously.
     * <p/>
     * If the data publisher hasn't been initialized properly, the events will be pushed in the queue
     * If the stream id is not in the cache the event will be pushed in the queue
     * else the stream id from the cache will be used to publish the event
     *
     * @param streamName           Name of the stream which the events is for
     * @param streamVersion        Version of the stream which the events is for
     * @param streamName           Name of the stream which the events is for
     * @param streamVersion        Version of the stream which the events is for
     * @param metaDataArray        metadata array of the event
     * @param correlationDataArray correlation array of the event
     * @param payloadDataArray     payload array of the event
     * @throws AgentException
     */
    public void publish(String streamName, String streamVersion,
                        Object[] metaDataArray, Object[] correlationDataArray,
                        Object[] payloadDataArray) throws AgentException {
        for (ReceiverGroup aGroup : receiverGroups) {
            aGroup.publish(streamName, streamVersion,
                    metaDataArray, correlationDataArray, payloadDataArray, null);
        }
    }

    /**
     * Publish the event asynchronously.
     * <p/>
     * If the data publisher hasn't been initialized properly, the events will be pushed in the queue
     * If the stream id is not in the cache the event will be pushed in the queue
     * else the stream id from the cache will be used to publish the event
     *
     * @param streamName    Name of the stream which the events is for
     * @param streamVersion Version of the stream which the events is for
     * @param streamName    Name of the stream which the events is for
     * @param streamVersion Version of the stream which the events is for
     * @param event         Event which should be published
     * @throws AgentException
     */
    public void publish(String streamName, String streamVersion, Event event)
            throws AgentException {
        for (ReceiverGroup aGroup : receiverGroups) {
            aGroup.publish(streamName, streamVersion,
                    event);
        }
    }

    /**
     * Publish the event asynchronously.
     * <p/>
     * If the data publisher hasn't been initialized properly, the events will be pushed in the queue
     * If the stream id is not in the cache the event will be pushed in the queue
     * else the stream id from the cache will be used to publish the event
     *
     * @param event Event which should be published
     * @throws AgentException
     */
    public void publish(Event event)
            throws AgentException {
        for (ReceiverGroup aGroup : receiverGroups) {
            aGroup.publish(event);
        }
    }

    public void addStreamDefinition(String streamDefn, String streamName, String version) {
        for (ReceiverGroup group : receiverGroups) {
            group.addStreamDefinition(streamDefn, streamName, version);
        }
    }

    public void addStreamDefinition(StreamDefinition definition) {
        for (ReceiverGroup group : receiverGroups) {
            group.addStreamDefinition(definition);
        }
    }

    public void stop() {
        for (ReceiverGroup group : receiverGroups) {
            group.stop();
        }
    }

}
