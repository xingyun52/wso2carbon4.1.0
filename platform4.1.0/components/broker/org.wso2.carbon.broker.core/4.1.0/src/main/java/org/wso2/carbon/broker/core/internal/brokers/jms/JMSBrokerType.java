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
package org.wso2.carbon.broker.core.internal.brokers.jms;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.broker.core.BrokerConfiguration;
import org.wso2.carbon.broker.core.BrokerListener;
import org.wso2.carbon.broker.core.BrokerTypeDto;
import org.wso2.carbon.broker.core.exception.BrokerEventProcessingException;
import org.wso2.carbon.broker.core.BrokerType;

import javax.jms.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JMS implementation of BrokerType
 */
public abstract class JMSBrokerType implements BrokerType {

    private static final Log log = LogFactory.getLog(JMSBrokerType.class);
    private BrokerTypeDto brokerTypeDto = null;
    private Map<String, Map<String, SubscriptionDetails>> brokerSubscriptionsMap;
    private ConcurrentHashMap<BrokerConfiguration, ConcurrentHashMap<String, JMSConnection>> jmsConnectionMap = new ConcurrentHashMap<BrokerConfiguration, ConcurrentHashMap<String, JMSConnection>>();


    public BrokerTypeDto getBrokerTypeDto() {
        return brokerTypeDto;
    }

    /**
     * Subscribe to given topic
     *
     * @param topicName           - topic name to subscribe
     * @param brokerListener      - broker type will invoke this when it receive events
     * @param brokerConfiguration - broker configuration details
     * @throws BrokerEventProcessingException
     */
    public String subscribe(String topicName, BrokerListener brokerListener,
                            BrokerConfiguration brokerConfiguration,
                            AxisConfiguration axisConfiguration)
            throws BrokerEventProcessingException {
        String subscriptionId = UUID.randomUUID().toString();
        // create connection
        TopicConnection topicConnection = getTopicConnection(brokerConfiguration);
        // create session, subscriber, message listener and listen on that topic
        try {
            TopicSession session = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicName);
            TopicSubscriber subscriber = session.createSubscriber(topic);
            MessageListener messageListener = new JMSMessageListener(brokerListener);
            subscriber.setMessageListener(messageListener);
            topicConnection.start();

            Map<String, SubscriptionDetails> subscriptionIdSubscriptionsMap =
                    this.brokerSubscriptionsMap.get(brokerConfiguration.getName());
            if (subscriptionIdSubscriptionsMap == null) {
                subscriptionIdSubscriptionsMap = new ConcurrentHashMap<String, SubscriptionDetails>();
                this.brokerSubscriptionsMap.put(brokerConfiguration.getName(), subscriptionIdSubscriptionsMap);
            }

            SubscriptionDetails subscriptionDetails =
                    new SubscriptionDetails(topicConnection, session, subscriber);
            subscriptionIdSubscriptionsMap.put(subscriptionId, subscriptionDetails);

            return subscriptionId;
        } catch (JMSException e) {
            String error = "Failed to subscribe to topic:" + topicName;
            log.error(error, e);
            throw new BrokerEventProcessingException(error, e);
        }
    }

    /**
     * Create Connection factory with initial context
     *
     * @param brokerConfiguration broker - configuration details to create a broker
     * @return Topic connection
     * @throws BrokerEventProcessingException - jndi look up failed
     */
    protected abstract TopicConnection getTopicConnection(BrokerConfiguration brokerConfiguration)
            throws BrokerEventProcessingException;


    /**
     * Publish message to given topic
     *
     * @param topicName           - topic name to publish messages
     * @param message             - message to send
     * @param brokerConfiguration - broker configuration to be used
     * @throws BrokerEventProcessingException
     */
    public void publish(String topicName, Object message,
                        BrokerConfiguration brokerConfiguration)
            throws BrokerEventProcessingException {


        ConcurrentHashMap<String, JMSConnection> producerMap = jmsConnectionMap.get(brokerConfiguration);
        if (null == producerMap) {
            producerMap = new ConcurrentHashMap<String, JMSConnection>();
            jmsConnectionMap.putIfAbsent(brokerConfiguration, producerMap);
        }
        JMSConnection jmsConnection = producerMap.get(topicName);
        if (null == jmsConnection) {
            try {
                // create topic connection
                TopicConnection topicConnection = getTopicConnection(brokerConfiguration);
                // create session, producer, message and send message to given destination(topic)
                // OMElement message text is published here.
                Session session  = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

                Topic topic = session.createTopic(topicName);
                MessageProducer messageProducer = session.createProducer(topic);
                jmsConnection =new JMSConnection(topicConnection,session, messageProducer);
                producerMap.putIfAbsent(topicName, jmsConnection);
            } catch (JMSException e) {
                String error = "Failed to publish to topic:" + topicName;
                log.error(error, e);
                throw new BrokerEventProcessingException(error, e);
            }

        }


        try {
            Session session = jmsConnection.getSession();
            Message jmsMessage = null;
            if (message instanceof OMElement) {
                jmsMessage = session.createTextMessage(message.toString());
            } else if (message instanceof String) {
                jmsMessage = session.createTextMessage((String) message);
            } else if (message instanceof Map) {
                MapMessage mapMessage = session.createMapMessage();
                Map sourceMessage = (Map) message;
                for (Object key : sourceMessage.keySet()) {
                    mapMessage.setObject((String) key, sourceMessage.get(key));
                }
                jmsMessage = mapMessage;
            }
            jmsConnection.getMessageProducer().send(jmsMessage);
        } catch (JMSException e) {
            producerMap.remove(topicName);
            String error = "Failed to publish to topic:" + topicName;
            log.error(error, e);
            try {
                if (jmsConnection.getSession() != null) {
                    jmsConnection.getSession().close();
                }
                if (jmsConnection.getTopicConnection() != null) {
                    jmsConnection.getTopicConnection().close();
                }
            } catch (JMSException e1) {
                log.warn("Failed to reallocate resources.", e1);
            }
            throw new BrokerEventProcessingException(error, e);

        }
    }

    @Override
    public void testConnection(BrokerConfiguration brokerConfiguration) throws BrokerEventProcessingException {
        String testMessage = " <brokerConfigurationTest>\n" +
                             "   <message>This is a test message.</message>\n" +
                             "   </brokerConfigurationTest>";
        publish("test", testMessage, brokerConfiguration);
    }


    public void unsubscribe(String topicName,
                            BrokerConfiguration brokerConfiguration,
                            AxisConfiguration axisConfiguration, String subscriptionId) throws BrokerEventProcessingException {
        Map<String, SubscriptionDetails> subscriptionIdSubscriptionsMap =
                this.brokerSubscriptionsMap.get(brokerConfiguration.getName());
        if (subscriptionIdSubscriptionsMap == null) {
            throw new BrokerEventProcessingException("There is no subscription for broker "
                                                     + brokerConfiguration.getName());
        }

        SubscriptionDetails subscriptionDetails = subscriptionIdSubscriptionsMap.remove(subscriptionId);
        if (subscriptionDetails == null) {
            throw new BrokerEventProcessingException("There is no subscriptions for topic" + topicName + " with subscriptionId " + subscriptionId);
        }

        try {
            subscriptionDetails.close();
        } catch (JMSException e) {
            throw new BrokerEventProcessingException("Can not unsubscribe from the broker with" +
                                                     "configuration " + brokerConfiguration.getName(), e);
        }

    }

    protected Map<String, Map<String, SubscriptionDetails>> getBrokerSubscriptionsMap() {
        return brokerSubscriptionsMap;
    }

    protected void setBrokerSubscriptionsMap(
            Map<String, Map<String, SubscriptionDetails>> brokerSubscriptionsMap) {
        this.brokerSubscriptionsMap = brokerSubscriptionsMap;
    }

    protected void setBrokerTypeDto(BrokerTypeDto brokerTypeDto) {
        this.brokerTypeDto = brokerTypeDto;
    }

    class JMSConnection {
        private TopicConnection topicConnection;
        private  Session session;
        private  MessageProducer messageProducer;

        JMSConnection(TopicConnection topicConnection, Session session, MessageProducer messageProducer) {
            this.topicConnection = topicConnection;
            this.session = session;
            this.messageProducer = messageProducer;
        }

        public Session getSession() {
            return session;
        }

        public MessageProducer getMessageProducer() {
            return messageProducer;
        }

        public TopicConnection getTopicConnection() {
            return topicConnection;
        }
    }
}
