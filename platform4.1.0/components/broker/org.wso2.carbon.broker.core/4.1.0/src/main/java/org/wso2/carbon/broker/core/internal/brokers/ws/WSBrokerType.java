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

package org.wso2.carbon.broker.core.internal.brokers.ws;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.wso2.carbon.broker.core.BrokerConfiguration;
import org.wso2.carbon.broker.core.BrokerListener;
import org.wso2.carbon.broker.core.BrokerTypeDto;
import org.wso2.carbon.broker.core.Property;
import org.wso2.carbon.broker.core.exception.BrokerEventProcessingException;
import org.wso2.carbon.broker.core.BrokerType;
import org.wso2.carbon.broker.core.internal.ds.BrokerServiceValueHolder;
import org.wso2.carbon.broker.core.internal.util.Axis2Util;
import org.wso2.carbon.broker.core.internal.util.BrokerConstants;
import org.wso2.carbon.event.client.broker.BrokerClient;
import org.wso2.carbon.event.client.broker.BrokerClientException;
import org.wso2.carbon.event.client.stub.generated.authentication.AuthenticationExceptionException;
import org.wso2.carbon.utils.ConfigurationContextService;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WSBrokerType implements BrokerType {

//    private static final Log log = LogFactory.getLog(WSBrokerType.class);

    private static WSBrokerType instance = new WSBrokerType();

    private BrokerTypeDto brokerTypeDto = null;

    private Map<String, Map<String, String>> brokerSubscriptionsMap;

    private WSBrokerType() {

        this.brokerTypeDto = new BrokerTypeDto();
        this.brokerTypeDto.setName(BrokerConstants.BROKER_TYPE_WS_EVENT);
        ResourceBundle resourceBundle = ResourceBundle.getBundle("org.wso2.carbon.broker.core.i18n.Resources", Locale.getDefault());

        Property uriProperty = new Property(BrokerConstants.BROKER_CONF_WS_PROP_URI);
        uriProperty.setRequired(true);
        uriProperty.setDisplayName(resourceBundle.getString(BrokerConstants.BROKER_CONF_WS_PROP_URI));
        this.brokerTypeDto.addProperty(uriProperty);

        Property userNameProperty = new Property(BrokerConstants.BROKER_CONF_WS_PROP_USERNAME);
        userNameProperty.setRequired(true);
        userNameProperty.setDisplayName(resourceBundle.getString(BrokerConstants.BROKER_CONF_WS_PROP_USERNAME));
        this.brokerTypeDto.addProperty(userNameProperty);

        Property passwordProperty = new Property(BrokerConstants.BROKER_CONF_WS_PROP_PASSWORD);
        passwordProperty.setRequired(true);
        passwordProperty.setSecured(true);
        passwordProperty.setDisplayName(resourceBundle.getString(BrokerConstants.BROKER_CONF_WS_PROP_PASSWORD));
        this.brokerTypeDto.addProperty(passwordProperty);

        this.brokerSubscriptionsMap = new ConcurrentHashMap<String, Map<String, String>>();

    }

    public static WSBrokerType getInstance() {
        return instance;
    }

    public BrokerTypeDto getBrokerTypeDto() {
        return this.brokerTypeDto;
    }

    public String subscribe(String topicName,
                          BrokerListener brokerListener,
                          BrokerConfiguration brokerConfiguration,
                          AxisConfiguration axisConfiguration)
            throws BrokerEventProcessingException {

                 String subscriptionId= UUID.randomUUID().toString();
        try {

            AxisService axisService =
                    Axis2Util.registerAxis2Service(topicName, brokerListener, brokerConfiguration, axisConfiguration, subscriptionId);

            String httpEpr = null;
            for (String epr : axisService.getEPRs()) {
                if (epr.startsWith("http")) {
                    httpEpr = epr;
                    break;
                }
            }

            if (httpEpr != null && !httpEpr.endsWith("/")) {
                httpEpr += "/";
            }

            httpEpr += topicName.replaceAll("/", "");

            Map<String, String> properties = brokerConfiguration.getProperties();
            BrokerClient brokerClient =
                    new BrokerClient(properties.get(BrokerConstants.BROKER_CONF_WS_PROP_URI),
                                     properties.get(BrokerConstants.BROKER_CONF_WS_PROP_USERNAME),
                                     properties.get(BrokerConstants.BROKER_CONF_WS_PROP_PASSWORD));
            brokerClient.subscribe(topicName, httpEpr);

            String subscriptionID = brokerClient.subscribe(topicName, httpEpr);

            // keep the subscription id to unsubscribe
            Map<String, String> localSubscriptionIdSubscriptionsMap =
                    this.brokerSubscriptionsMap.get(brokerConfiguration.getName());
            if (localSubscriptionIdSubscriptionsMap == null) {
                localSubscriptionIdSubscriptionsMap = new ConcurrentHashMap<String, String>();
                this.brokerSubscriptionsMap.put(brokerConfiguration.getName(), localSubscriptionIdSubscriptionsMap);
            }

            localSubscriptionIdSubscriptionsMap.put(subscriptionId, subscriptionID);
            return subscriptionId;

        } catch (BrokerClientException e) {
            throw new BrokerEventProcessingException("Can not create the broker client", e);
        } catch (AuthenticationExceptionException e) {
            throw new BrokerEventProcessingException("Can not authenticate the broker client", e);
        } catch (AxisFault axisFault) {
            throw new BrokerEventProcessingException("Can not subscribe", axisFault);
        }
    }

    public void publish(String topicName,
                        Object message,
                        BrokerConfiguration brokerConfiguration)
            throws BrokerEventProcessingException {

        try {
            Map<String, String> properties = brokerConfiguration.getProperties();
            ConfigurationContextService configurationContextService =
                    BrokerServiceValueHolder.getConfigurationContextService();
            BrokerClient brokerClient =
                    new BrokerClient(configurationContextService.getClientConfigContext(),
                                     properties.get(BrokerConstants.BROKER_CONF_WS_PROP_URI),
                                     properties.get(BrokerConstants.BROKER_CONF_WS_PROP_USERNAME),
                                     properties.get(BrokerConstants.BROKER_CONF_WS_PROP_PASSWORD));
            brokerClient.publish(topicName, ((OMElement) message));
        } catch (AuthenticationExceptionException e) {
            throw new BrokerEventProcessingException("Can not authenticate the broker client", e);
        } catch (AxisFault axisFault) {
            throw new BrokerEventProcessingException("Can not subscribe", axisFault);
        }

    }

    @Override
    public void testConnection(BrokerConfiguration brokerConfiguration) throws BrokerEventProcessingException {
        String testMessage = " <brokerConfigurationTest>\n" +
                "   <message>This is a test message.</message>\n" +
                "   </brokerConfigurationTest>";
        try {
            XMLStreamReader reader1 = StAXUtils.createXMLStreamReader(new ByteArrayInputStream(testMessage.getBytes()));
            StAXOMBuilder builder1 = new StAXOMBuilder(reader1);
            publish("test", builder1.getDocumentElement(), brokerConfiguration);
        } catch (XMLStreamException e) {
            //ignored as this will not happen
        }
    }

    public void unsubscribe(String topicName,
                            BrokerConfiguration brokerConfiguration,
                            AxisConfiguration axisConfiguration,String subscriptionId)
            throws BrokerEventProcessingException {
        try {
            Axis2Util.removeOperation(topicName, brokerConfiguration, axisConfiguration, subscriptionId);
        } catch (AxisFault axisFault) {
            throw new BrokerEventProcessingException("Can not unsubscribe from the broker", axisFault);
        }

        Map<String, String> localSubscriptionIdSubscriptionsMap =
                this.brokerSubscriptionsMap.get(brokerConfiguration.getName());
        if (localSubscriptionIdSubscriptionsMap == null) {
            throw new BrokerEventProcessingException("There is no subscription for broker "
                                                     + brokerConfiguration.getName());
        }

        String subscriptionID = localSubscriptionIdSubscriptionsMap.remove(subscriptionId);
        if (subscriptionID == null) {
            throw new BrokerEventProcessingException("There is no subscriptions for this topic" + topicName);
        }

        try {
            Map<String, String> properties = brokerConfiguration.getProperties();
            ConfigurationContextService configurationContextService =
                    BrokerServiceValueHolder.getConfigurationContextService();
            BrokerClient brokerClient =
                    new BrokerClient(configurationContextService.getClientConfigContext(),
                                     properties.get(BrokerConstants.BROKER_CONF_WS_PROP_URI),
                                     properties.get(BrokerConstants.BROKER_CONF_WS_PROP_USERNAME),
                                     properties.get(BrokerConstants.BROKER_CONF_WS_PROP_PASSWORD));
            brokerClient.unsubscribe(subscriptionID);
        } catch (AuthenticationExceptionException e) {
            throw new BrokerEventProcessingException("Can not authenticate the broker client", e);
        } catch (RemoteException e) {
            throw new BrokerEventProcessingException("Can not connect to the server", e);
        }

    }
}
