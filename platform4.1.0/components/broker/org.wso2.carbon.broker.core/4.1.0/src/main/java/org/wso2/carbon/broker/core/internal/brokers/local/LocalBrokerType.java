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

package org.wso2.carbon.broker.core.internal.brokers.local;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.wso2.carbon.broker.core.BrokerConfiguration;
import org.wso2.carbon.broker.core.BrokerListener;
import org.wso2.carbon.broker.core.BrokerTypeDto;
import org.wso2.carbon.broker.core.exception.BrokerEventProcessingException;
import org.wso2.carbon.broker.core.BrokerType;
import org.wso2.carbon.broker.core.internal.ds.BrokerServiceValueHolder;
import org.wso2.carbon.broker.core.internal.util.Axis2Util;
import org.wso2.carbon.broker.core.internal.util.BrokerConstants;
import org.wso2.carbon.event.core.EventBroker;
import org.wso2.carbon.event.core.Message;
import org.wso2.carbon.event.core.exception.EventBrokerException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.UUID;

public final class LocalBrokerType implements BrokerType {

    private BrokerTypeDto brokerTypeDto = null;

    private static LocalBrokerType localBrokerType = new LocalBrokerType();

    private LocalBrokerType() {
        this.brokerTypeDto = new BrokerTypeDto();
        this.brokerTypeDto.setName(BrokerConstants.BROKER_TYPE_LOCAL);
    }

    public static LocalBrokerType getInstance() {
        return localBrokerType;
    }

    public String subscribe(String topicName,
                            BrokerListener brokerListener,
                            BrokerConfiguration brokerConfiguration,
                            AxisConfiguration axisConfiguration)
            throws BrokerEventProcessingException {
        // When publishing we only need to register the axis2 service 
        // no subscribing
        String subscriptionId = UUID.randomUUID().toString();
        try {
            Axis2Util.registerAxis2Service(topicName, brokerListener,
                                           brokerConfiguration, axisConfiguration, subscriptionId);
        } catch (AxisFault axisFault) {
            throw new BrokerEventProcessingException("Can not create " +
                                                     "the axis2 service to receive events", axisFault);
        }
        return subscriptionId;

    }

    public void publish(String topicName,
                        Object message,
                        BrokerConfiguration brokerConfiguration)
            throws BrokerEventProcessingException {

        EventBroker eventBroker = BrokerServiceValueHolder.getEventBroker();
        Message eventMessage = new Message();
        eventMessage.setMessage(((OMElement) message));
        try {
            eventBroker.publishRobust(eventMessage, topicName);
        } catch (EventBrokerException e) {
            throw new BrokerEventProcessingException("Can not publish the to local broker ", e);
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

    public BrokerTypeDto getBrokerTypeDto() {
        return brokerTypeDto;
    }

    public void unsubscribe(String topicName,
                            BrokerConfiguration brokerConfiguration,
                            AxisConfiguration axisConfiguration, String subscriptionId)
            throws BrokerEventProcessingException {
        try {
            Axis2Util.removeOperation(topicName, brokerConfiguration, axisConfiguration, subscriptionId);
        } catch (AxisFault axisFault) {
            throw new BrokerEventProcessingException("Can not remove operation ", axisFault);
        }

    }
}
