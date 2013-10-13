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

package org.wso2.carbon.broker.core.internal;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.broker.core.*;
import org.wso2.carbon.broker.core.exception.BrokerEventProcessingException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * broker service implementation.
 */
public class CarbonBrokerService implements BrokerService {

    private static Log log = LogFactory.getLog(CarbonBrokerService.class);
    private Map<String, BrokerType> brokerTypeMap;

    public CarbonBrokerService() {
        this.brokerTypeMap = new ConcurrentHashMap();
    }

    public void registerBrokerType(BrokerType brokerType) {
        BrokerTypeDto brokerTypeTypeDto = brokerType.getBrokerTypeDto();
        this.brokerTypeMap.put(brokerTypeTypeDto.getName(), brokerType);
    }

    public List<BrokerTypeDto> getBrokerTypes() {
        List<BrokerTypeDto> brokerTypeDtos = new ArrayList<BrokerTypeDto>();
        for (BrokerType brokerType : this.brokerTypeMap.values()) {
            brokerTypeDtos.add(brokerType.getBrokerTypeDto());
        }
        return brokerTypeDtos;
    }

    public List<String> getBrokerTypeNames() {
        List<String> brokerTypeNames = new ArrayList<String>();
        for (BrokerType brokerType : this.brokerTypeMap.values()) {
            brokerTypeNames.add(brokerType.getBrokerTypeDto().getName());
        }
        return brokerTypeNames;
    }

    public List<Property> getBrokerProperties(String brokerType) {
        return brokerTypeMap.get(brokerType).getBrokerTypeDto().getPropertyList();
    }

    public String subscribe(BrokerConfiguration brokerConfiguration,
                            String topicName,
                            BrokerListener brokerListener,
                            AxisConfiguration axisConfiguration) throws BrokerEventProcessingException {
        BrokerType brokerType = this.brokerTypeMap.get(brokerConfiguration.getType());
        try {
            return brokerType.subscribe(topicName, brokerListener, brokerConfiguration, axisConfiguration);
        } catch (BrokerEventProcessingException e) {
            log.error(e.getMessage(),e);
            throw new BrokerEventProcessingException(e.getMessage(),e);
        }
    }

    public void publish(BrokerConfiguration brokerConfiguration,
                        String topicName, Object object) throws BrokerEventProcessingException {

        BrokerType brokerType = this.brokerTypeMap.get(brokerConfiguration.getType());
        try {
            brokerType.publish(topicName, object, brokerConfiguration);
        } catch (BrokerEventProcessingException e) {
            log.error(e.getMessage(),e);
            throw new BrokerEventProcessingException(e.getMessage(),e);
        }
    }

    @Override
    public void testConnection(BrokerConfiguration brokerConfiguration) throws BrokerEventProcessingException {
        BrokerType brokerType = this.brokerTypeMap.get(brokerConfiguration.getType());
        try {
            brokerType.testConnection(brokerConfiguration);
        } catch (BrokerEventProcessingException e) {
            log.error(e.getMessage(),e);
            throw new BrokerEventProcessingException(e.getMessage(),e);
        }
    }

    public void unsubscribe(String topicName,
                            BrokerConfiguration brokerConfiguration,
                            AxisConfiguration axisConfiguration,String subscriptionId) throws BrokerEventProcessingException {
        BrokerType brokerType = this.brokerTypeMap.get(brokerConfiguration.getType());
        try {
            brokerType.unsubscribe(topicName, brokerConfiguration, axisConfiguration,subscriptionId);
        } catch (BrokerEventProcessingException e) {
            log.error(e.getMessage(), e);
            throw new BrokerEventProcessingException(e.getMessage(),e);
        }
    }
}
