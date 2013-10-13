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
package org.wso2.carbon.cep.statistics;

import org.wso2.carbon.cep.statistics.internal.counter.BrokerCounter;
import org.wso2.carbon.cep.statistics.internal.counter.BucketCounter;
import org.wso2.carbon.cep.statistics.internal.counter.TenantCounter;
import org.wso2.carbon.cep.statistics.internal.counter.TopicCounter;

public class CEPStatisticsMonitor {

    private TenantCounter tenantData;
    private BrokerCounter brokerData;
    private BucketCounter bucketData;
    private TopicCounter topicData;

    public CEPStatisticsMonitor(TenantCounter tenantData,
                                BucketCounter bucketData,
                                BrokerCounter brokerData,
                                TopicCounter topicData) {
        this.tenantData = tenantData;
        this.brokerData = brokerData;
        this.bucketData = bucketData;
        this.topicData = topicData;
    }

    public void incrementRequest() {
        tenantData.incrementRequest();
        brokerData.incrementRequest();
        bucketData.incrementRequest();
        topicData.incrementRequest();
    }

    public void incrementResponse() {
        tenantData.incrementResponse();
        brokerData.incrementResponse();
        bucketData.incrementResponse();
        topicData.incrementResponse();
    }



}
