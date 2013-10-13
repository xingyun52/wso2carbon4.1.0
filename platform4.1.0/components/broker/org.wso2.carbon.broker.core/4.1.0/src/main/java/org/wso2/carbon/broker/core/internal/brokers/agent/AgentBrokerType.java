/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.wso2.carbon.broker.core.internal.brokers.agent;

import com.google.gson.Gson;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.broker.core.BrokerConfiguration;
import org.wso2.carbon.broker.core.BrokerListener;
import org.wso2.carbon.broker.core.BrokerTypeDto;
import org.wso2.carbon.broker.core.Property;
import org.wso2.carbon.broker.core.exception.BrokerEventProcessingException;
import org.wso2.carbon.broker.core.BrokerType;
import org.wso2.carbon.broker.core.internal.ds.BrokerServiceValueHolder;
import org.wso2.carbon.broker.core.internal.util.BrokerConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Credentials;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.core.AgentCallback;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionNotFoundException;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AgentBrokerType implements BrokerType {

    private static final Log log = LogFactory.getLog(AgentBrokerType.class);
    private Gson gson = new Gson();
    private BrokerTypeDto brokerTypeDto = null;
    private static AgentBrokerType agentBrokerType = new AgentBrokerType();

    private Map<String, Map<String, BrokerListener>> topicBrokerListenerMap =
            new ConcurrentHashMap<String, Map<String, BrokerListener>>();
    private Map<String, StreamDefinition> topicStreamDefinitionMap =
            new ConcurrentHashMap<String, StreamDefinition>();
    private Map<String, Map<String, BrokerListener>> streamIdBrokerListenerMap =
            new ConcurrentHashMap<String, Map<String, BrokerListener>>();
    //    private Map<String, String> outputStreamIdMap = new ConcurrentHashMap<String, String>();
    private ConcurrentHashMap<Integer, ConcurrentHashMap<BrokerConfiguration, AsyncDataPublisher>> dataPublisherMap = new ConcurrentHashMap<Integer, ConcurrentHashMap<BrokerConfiguration, AsyncDataPublisher>>();
    private Agent agent;

    private AgentBrokerType() {
        this.brokerTypeDto = new BrokerTypeDto();
        this.brokerTypeDto.setName(BrokerConstants.BROKER_TYPE_AGENT);

        ResourceBundle resourceBundle = ResourceBundle.getBundle(
                "org.wso2.carbon.broker.core.i18n.Resources", Locale.getDefault());

        // set receiver url broker
        Property ipProperty = new Property(BrokerConstants.BROKER_CONF_AGENT_PROP_RECEIVER_URL);
        ipProperty.setDisplayName(
                resourceBundle.getString(BrokerConstants.BROKER_CONF_AGENT_PROP_RECEIVER_URL));
        ipProperty.setRequired(true);
        this.brokerTypeDto.addProperty(ipProperty);

        // set authenticator url of broker
        Property authenticatorIpProperty = new Property(BrokerConstants.
                                                                BROKER_CONF_AGENT_PROP_AUTHENTICATOR_URL);
        authenticatorIpProperty.setDisplayName(
                resourceBundle.getString(BrokerConstants.BROKER_CONF_AGENT_PROP_AUTHENTICATOR_URL));
        authenticatorIpProperty.setRequired(false);
        this.brokerTypeDto.addProperty(authenticatorIpProperty);

        // set connection user name as property
        Property userNameProperty = new Property(BrokerConstants.BROKER_CONF_AGENT_PROP_USER_NAME);
        userNameProperty.setRequired(true);
        userNameProperty.setDisplayName(
                resourceBundle.getString(BrokerConstants.BROKER_CONF_AGENT_PROP_USER_NAME));
        this.brokerTypeDto.addProperty(userNameProperty);

        // set connection password as property
        Property passwordProperty = new Property(BrokerConstants.BROKER_CONF_AGENT_PROP_PASSWORD);
        passwordProperty.setRequired(true);
        passwordProperty.setSecured(true);
        passwordProperty.setDisplayName(
                resourceBundle.getString(BrokerConstants.BROKER_CONF_AGENT_PROP_PASSWORD));
        this.brokerTypeDto.addProperty(passwordProperty);

        BrokerServiceValueHolder.getDataBridgeSubscriberService().subscribe(new AgentBrokerCallback());
    }

    public static AgentBrokerType getInstance() {
        return agentBrokerType;
    }

    private class AgentBrokerCallback implements AgentCallback {


        @Override
        public void removeStream(StreamDefinition streamDefinition, Credentials credentials) {
            topicStreamDefinitionMap.remove(createTopic(streamDefinition));
            Map<String, BrokerListener> brokerListeners = topicBrokerListenerMap.get(createTopic(streamDefinition));
            if (brokerListeners != null) {
                for (BrokerListener brokerListener : brokerListeners.values()) {
                    try {
                        brokerListener.removeEventDefinition(streamDefinition);
                    } catch (BrokerEventProcessingException e) {
                        log.error("Cannot remove Stream Definition from a brokerListener subscribed to " +
                                  streamDefinition.getStreamId(), e);
                    }

                }
            }
            streamIdBrokerListenerMap.remove(streamDefinition.getStreamId());
        }

        @Override
        public void definedStream(StreamDefinition streamDefinition, Credentials credentials) {
            topicStreamDefinitionMap.put(createTopic(streamDefinition), streamDefinition);
            Map<String, BrokerListener> brokerListeners = topicBrokerListenerMap.get(createTopic(streamDefinition));
            if (brokerListeners == null) {
                brokerListeners = new HashMap<String, BrokerListener>();
                topicBrokerListenerMap.put(createTopic(streamDefinition), brokerListeners);
            }
//            inputTypeDefMap.put(streamDefinition.getName(), streamDefinition);
            for (BrokerListener brokerListener : brokerListeners.values()) {
                try {
                    brokerListener.addEventDefinition(streamDefinition);
                } catch (BrokerEventProcessingException e) {
                    log.error("Cannot send Stream Definition to a brokerListener subscribed to " +
                              streamDefinition.getStreamId(), e);
                }

            }
            streamIdBrokerListenerMap.put(streamDefinition.getStreamId(), topicBrokerListenerMap.get(createTopic(streamDefinition)));
        }

        private String createTopic(StreamDefinition streamDefinition) {
            return streamDefinition.getName() + "/" + streamDefinition.getVersion();
        }

        @Override
        public void receive(List<Event> events, Credentials credentials) {
            for (Event event : events) {
                Map<String, BrokerListener> brokerListeners = streamIdBrokerListenerMap.get(event.getStreamId());
                if (brokerListeners == null) {
                    try {
                        definedStream(BrokerServiceValueHolder.getDataBridgeSubscriberService().getStreamDefinition(credentials, event.getStreamId()), credentials);
                    } catch (StreamDefinitionNotFoundException e) {
                        log.error("No Stream definition store found for the event " +
                                  event.getStreamId(), e);
                        return;
                    } catch (StreamDefinitionStoreException e) {
                        log.error("No Stream definition store found when checking stream definition for " +
                                  event.getStreamId(), e);
                        return;
                    }
                    brokerListeners = streamIdBrokerListenerMap.get(event.getStreamId());
                    if (brokerListeners == null) {
                        log.error("No broker listeners for  " + event.getStreamId());
                        return;
                    }
                }
                for (BrokerListener brokerListener : brokerListeners.values()) {
                    try {
                        brokerListener.onEvent(event);
                    } catch (BrokerEventProcessingException e) {
                        log.error("Cannot send event to a brokerListener subscribed to " +
                                  event.getStreamId(), e);
                    }

                }

            }
        }

    }

    public String subscribe(String topicName, BrokerListener brokerListener,
                            BrokerConfiguration brokerConfiguration,
                            AxisConfiguration axisConfiguration)
            throws BrokerEventProcessingException {
        String subscriptionId = UUID.randomUUID().toString();
        if (!topicBrokerListenerMap.containsKey(topicName)) {
            Map<String, BrokerListener> map = new HashMap<String, BrokerListener>();
            map.put(subscriptionId, brokerListener);
            topicBrokerListenerMap.put(topicName, map);
        } else {
            topicBrokerListenerMap.get(topicName).put(subscriptionId, brokerListener);
            StreamDefinition streamDefinition = topicStreamDefinitionMap.get(topicName);
            if (streamDefinition != null) {
                brokerListener.addEventDefinition(streamDefinition);
            }

        }
        return subscriptionId;
    }

    /**
     * @param topicName           - topic name to publish messages
     * @param message             - is and Object[]{Event, EventDefinition}
     * @param brokerConfiguration - broker configuration to be used
     * @throws BrokerEventProcessingException
     */
    public void publish(String topicName, Object message,
                        BrokerConfiguration brokerConfiguration)
            throws BrokerEventProcessingException {
        Integer tenantId = CarbonContext.getCurrentContext().getTenantId();
        ConcurrentHashMap<BrokerConfiguration, AsyncDataPublisher> dataPublishers = dataPublisherMap.get(tenantId);
        if (dataPublishers == null) {
            dataPublishers = new ConcurrentHashMap<BrokerConfiguration, AsyncDataPublisher>();
            dataPublisherMap.putIfAbsent(tenantId, dataPublishers);
            dataPublishers = dataPublisherMap.get(tenantId);
        }
        AsyncDataPublisher dataPublisher = dataPublishers.get(brokerConfiguration);
        if (dataPublisher == null) {
            synchronized (this) {
                dataPublisher = dataPublishers.get(brokerConfiguration);
                if (dataPublisher == null) {
                    dataPublisher = createDataPublisher(brokerConfiguration);
                    dataPublishers.putIfAbsent(brokerConfiguration, dataPublisher);
                }
            }
        }

        try {
            Event event = (Event) ((Object[]) message)[0];
            StreamDefinition streamDefinition = (StreamDefinition) ((Object[]) message)[1];

//            String streamId = outputStreamIdMap.get(topicName + tenantId);
            if (!dataPublisher.isStreamDefinitionAdded(streamDefinition)) {
                dataPublisher.addStreamDefinition(streamDefinition);

                //Sending the first Event

                publishEvent(brokerConfiguration, dataPublisher, event, streamDefinition);
            } else {
                //Sending Events
                publishEvent(brokerConfiguration, dataPublisher, event, streamDefinition);
            }
        } catch (Exception ex) {
            throw new BrokerEventProcessingException(
                    ex.getMessage() + " Error Occurred When Publishing Events", ex);
        }

    }

    private AsyncDataPublisher createDataPublisher(BrokerConfiguration brokerConfiguration) {
        if (agent == null) {
            agent = BrokerServiceValueHolder.getAgent();
        }
        AsyncDataPublisher dataPublisher;
        Map<String, String> properties = brokerConfiguration.getProperties();
        if (null != properties.get(BrokerConstants.BROKER_CONF_AGENT_PROP_AUTHENTICATOR_URL) && properties.get(BrokerConstants.BROKER_CONF_AGENT_PROP_AUTHENTICATOR_URL).length() > 0) {
            dataPublisher = new AsyncDataPublisher(properties.get(BrokerConstants.BROKER_CONF_AGENT_PROP_AUTHENTICATOR_URL),
                                                   properties.get(BrokerConstants.BROKER_CONF_AGENT_PROP_RECEIVER_URL),
                                                   properties.get(BrokerConstants.BROKER_CONF_AGENT_PROP_USER_NAME),
                                                   properties.get(BrokerConstants.BROKER_CONF_AGENT_PROP_PASSWORD),
                                                   agent);
        } else {
            dataPublisher = new AsyncDataPublisher(properties.get(BrokerConstants.BROKER_CONF_AGENT_PROP_RECEIVER_URL),
                                                   properties.get(BrokerConstants.BROKER_CONF_AGENT_PROP_USER_NAME),
                                                   properties.get(BrokerConstants.BROKER_CONF_AGENT_PROP_PASSWORD),
                                                   agent);
        }
        return dataPublisher;
    }

    private void throwBrokerEventProcessingException(BrokerConfiguration brokerConfiguration,
                                                     Exception e)
            throws BrokerEventProcessingException {
        throw new BrokerEventProcessingException(
                "Cannot create DataPublisher for the broker configuration:" + brokerConfiguration.getName(), e);
    }

    private void publishEvent(BrokerConfiguration brokerConfiguration,
                              AsyncDataPublisher dataPublisher,
                              Event event, StreamDefinition streamDefinition)
            throws BrokerEventProcessingException {
        try {
            dataPublisher.publish(streamDefinition.getName(), streamDefinition.getVersion(), event);
        } catch (AgentException ex) {
            throw new BrokerEventProcessingException(
                    "Cannot publish data via DataPublisher for the broker configuration:" +
                    brokerConfiguration.getName() + " for the  event " + event, ex);
        }

    }

    @Override
    public void testConnection(BrokerConfiguration brokerConfiguration)
            throws BrokerEventProcessingException {
        // no test
    }

    public BrokerTypeDto getBrokerTypeDto() {
        return brokerTypeDto;
    }

    public void unsubscribe(String topicName,
                            BrokerConfiguration brokerConfiguration,
                            AxisConfiguration axisConfiguration, String subscriptionId)
            throws BrokerEventProcessingException {
        Map<String, BrokerListener> map = topicBrokerListenerMap.get(topicName);
        if (map != null) {
            map.remove(subscriptionId);
        }

    }
}
