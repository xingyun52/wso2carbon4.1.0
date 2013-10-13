/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.cep.statistics.internal;

import org.wso2.carbon.cep.statistics.CEPStatisticsManagerInterface;
import org.wso2.carbon.cep.statistics.CEPStatisticsMonitor;
import org.wso2.carbon.cep.statistics.internal.counter.BrokerCounter;
import org.wso2.carbon.cep.statistics.internal.counter.BucketCounter;
import org.wso2.carbon.cep.statistics.internal.counter.TenantCounter;
import org.wso2.carbon.cep.statistics.internal.counter.TopicCounter;

import java.util.concurrent.ConcurrentHashMap;

public class CEPStatisticsManager implements CEPStatisticsManagerInterface {

    ConcurrentHashMap<Integer, TenantCounter> tenantDataMap;

    public CEPStatisticsManager() {
        this.tenantDataMap =  new ConcurrentHashMap<Integer, TenantCounter>();
    }

    public synchronized CEPStatisticsMonitor createNewCEPStatisticMonitor(String topicName, String brokerName,
                                                            String bucketName, int tenantId) {
        TenantCounter tenantData = tenantDataMap.get(tenantId);
        if (tenantData == null) {
            tenantData = new TenantCounter(tenantId);
            tenantDataMap.put(tenantId, tenantData);
        }

        BucketCounter bucketData = tenantData.getBuckets().get(bucketName);
        if (bucketData == null) {
            bucketData = new BucketCounter(bucketName);
            tenantData.getBuckets().put(bucketName, bucketData);
        }

        BrokerCounter brokerData = tenantData.getBrokers().get(brokerName);
        if (brokerData == null) {
            brokerData = new BrokerCounter(brokerName);
            tenantData.getBrokers().put(brokerName, brokerData);
        }

        TopicCounter topicDataFromBroker = brokerData.getTopics().get(topicName);
        TopicCounter topicDataFromBucket = bucketData.getTopics().get(topicName);
        if (topicDataFromBroker == null && topicDataFromBucket == null) {
            topicDataFromBroker = new TopicCounter(topicName);
            topicDataFromBucket = topicDataFromBroker;
            brokerData.getTopics().put(topicName, topicDataFromBroker);
            bucketData.getTopics().put(topicName, topicDataFromBucket);
        } else if (topicDataFromBroker == null) {
            topicDataFromBroker = topicDataFromBucket;
            brokerData.getTopics().put(topicName, topicDataFromBroker);
        } else if (topicDataFromBucket == null) {
            topicDataFromBucket = topicDataFromBroker;
            bucketData.getTopics().put(topicName, topicDataFromBucket);
        }


        return new CEPStatisticsMonitor(tenantData, bucketData, brokerData, topicDataFromBroker);

    }

    public synchronized void reset() {
        for (TenantCounter tenantData : tenantDataMap.values()) {
            tenantData.reset();
            for (BrokerCounter brokerData : tenantData.getBrokers().values()) {
                brokerData.reset();
            }
            for (BucketCounter bucketData : tenantData.getBuckets().values()) {
                bucketData.reset();
                for (TopicCounter topicData : bucketData.getTopics().values()) {
                    topicData.reset();
                }
            }
        }
    }

    public ConcurrentHashMap<Integer, TenantCounter> getTenantDataMap() {
        return tenantDataMap;
    }
}
