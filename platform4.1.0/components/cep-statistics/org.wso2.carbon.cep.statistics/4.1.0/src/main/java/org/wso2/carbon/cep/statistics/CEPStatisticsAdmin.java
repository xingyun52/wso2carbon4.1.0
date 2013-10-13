/*
* Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* 	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.cep.statistics;


import org.wso2.carbon.cep.statistics.internal.data.CollectionDTO;
import org.wso2.carbon.cep.statistics.internal.data.CountDTO;
import org.wso2.carbon.cep.statistics.internal.CEPStatisticsManager;
import org.wso2.carbon.cep.statistics.internal.CEPStatisticsServiceHolder;
import org.wso2.carbon.cep.statistics.internal.counter.BrokerCounter;
import org.wso2.carbon.cep.statistics.internal.counter.BucketCounter;
import org.wso2.carbon.cep.statistics.internal.counter.TenantCounter;
import org.wso2.carbon.cep.statistics.internal.counter.TopicCounter;
import org.wso2.carbon.context.CarbonContext;

import java.util.Random;

public class CEPStatisticsAdmin {
    Random random;
    public CEPStatisticsAdmin() {
       random=new Random();
    }

    public CountDTO getGlobalCount() {
        CEPStatisticsManager cepStatisticsManager = CEPStatisticsServiceHolder.getInstance().getCepStatisticsManager();
        int tenantId = CarbonContext.getCurrentContext().getTenantId();

        TenantCounter tenantCounter = cepStatisticsManager.getTenantDataMap().get(tenantId);

        if(tenantCounter==null){
            tenantCounter=new TenantCounter(tenantId);
            cepStatisticsManager.getTenantDataMap().put(tenantId,tenantCounter);
        }
        CountDTO countDTO = new CountDTO();
        countDTO.setRequestCount(tenantCounter.getCountRequest());
        countDTO.setResponseCount(tenantCounter.getCountResponse());

//        cepStatisticsManager.reset();
//
        return countDTO;
    }

    public CollectionDTO getBucketCount(String bucketName) {
        CEPStatisticsManager cepStatisticsManager = CEPStatisticsServiceHolder.getInstance().getCepStatisticsManager();
        int tenantId = CarbonContext.getCurrentContext().getTenantId();

        TenantCounter tenantCounter = cepStatisticsManager.getTenantDataMap().get(tenantId);
        if(tenantCounter==null){
            tenantCounter=new TenantCounter(tenantId);
            cepStatisticsManager.getTenantDataMap().put(tenantId,tenantCounter);
        }

        BucketCounter bucketCounter = tenantCounter.getBuckets().get(bucketName);
        if(bucketCounter==null){
            bucketCounter=new BucketCounter(bucketName);
            tenantCounter.getBuckets().put(bucketName,bucketCounter);
        }

        CountDTO countDTO = new CountDTO();
        countDTO.setRequestCount(bucketCounter.getCountRequest());
        countDTO.setResponseCount(bucketCounter.getCountResponse());

        CollectionDTO collectionDTO = new CollectionDTO();
        collectionDTO.setBucket(true);
        collectionDTO.setCount(countDTO);

        CountDTO[] topicCountDTOs = new CountDTO[bucketCounter.getTopics().size()];
        String[] topicNames = new String[bucketCounter.getTopics().size()];
        int i = 0;
        for (TopicCounter topicData : bucketCounter.getTopics().values()) {
            CountDTO topicCountDTO = new CountDTO();
            topicCountDTO.setRequestCount(topicData.getCountRequest());
            topicCountDTO.setResponseCount(topicData.getCountResponse());
            topicCountDTOs[i] = topicCountDTO;
            topicNames[i] = topicData.getName();
            i++;
        }

        collectionDTO.setTopicCounts(topicCountDTOs);
        collectionDTO.setTopicNames(topicNames);

       // cepStatisticsManager.reset();

        return collectionDTO;
    }

    public CollectionDTO getBrokerCount(String brokerName) {
        CEPStatisticsManager cepStatisticsManager = CEPStatisticsServiceHolder.getInstance().getCepStatisticsManager();
        int tenantId = CarbonContext.getCurrentContext().getTenantId();

        TenantCounter tenantCounter = cepStatisticsManager.getTenantDataMap().get(tenantId);
        if(tenantCounter==null){
            tenantCounter=new TenantCounter(tenantId);
            cepStatisticsManager.getTenantDataMap().put(tenantId,tenantCounter);
        }

        BrokerCounter brokerCounter = tenantCounter.getBrokers().get(brokerName);
        if(brokerCounter==null){
            brokerCounter=new BrokerCounter(brokerName);
            tenantCounter.getBrokers().put(brokerName,brokerCounter);
        }

        CountDTO countDTO = new CountDTO();
        countDTO.setRequestCount(brokerCounter.getCountRequest());
        countDTO.setResponseCount(brokerCounter.getCountResponse());

        CollectionDTO collectionDTO = new CollectionDTO();
        collectionDTO.setBucket(false);
        collectionDTO.setCount(countDTO);

        CountDTO[] topicCountDTOs = new CountDTO[brokerCounter.getTopics().size()];
        String[] topicNames = new String[brokerCounter.getTopics().size()];
        int i = 0;
        for (TopicCounter topicData : brokerCounter.getTopics().values()) {
            CountDTO topicCountDTO = new CountDTO();
            topicCountDTO.setRequestCount(topicData.getCountRequest());
            topicCountDTO.setResponseCount(topicData.getCountResponse());
            topicCountDTOs[i] = topicCountDTO;
            topicNames[i] = topicData.getName();
            i++;
        }

        collectionDTO.setTopicCounts(topicCountDTOs);
        collectionDTO.setTopicNames(topicNames);

       // cepStatisticsManager.reset();

        return collectionDTO;
    }
}