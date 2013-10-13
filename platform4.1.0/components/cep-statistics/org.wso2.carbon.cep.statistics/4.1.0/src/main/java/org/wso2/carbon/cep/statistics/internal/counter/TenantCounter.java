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
package org.wso2.carbon.cep.statistics.internal.counter;

import java.util.concurrent.ConcurrentHashMap;

public class TenantCounter extends AbstractCounter {
    int tenantId;
    ConcurrentHashMap<String,BucketCounter> buckets=new ConcurrentHashMap<String, BucketCounter>();
    ConcurrentHashMap<String,BrokerCounter> brokers=new ConcurrentHashMap<String, BrokerCounter>();

    public TenantCounter(int tenantId) {
        this.tenantId = tenantId;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public ConcurrentHashMap<String, BucketCounter> getBuckets() {
        return buckets;
    }

    public void setBuckets(ConcurrentHashMap<String, BucketCounter> buckets) {
        this.buckets = buckets;
    }

    public ConcurrentHashMap<String, BrokerCounter> getBrokers() {
        return brokers;
    }

    public void setBrokers(ConcurrentHashMap<String, BrokerCounter> brokers) {
        this.brokers = brokers;
    }
}
