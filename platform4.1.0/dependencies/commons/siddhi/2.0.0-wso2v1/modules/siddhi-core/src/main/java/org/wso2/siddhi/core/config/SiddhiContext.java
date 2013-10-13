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
package org.wso2.siddhi.core.config;

import com.hazelcast.core.HazelcastInstance;
import org.wso2.siddhi.core.persistence.PersistenceService;
import org.wso2.siddhi.core.persistence.ThreadBarrier;
import org.wso2.siddhi.core.util.ElementIdGenerator;
import org.wso2.siddhi.core.util.GlobalIndexGenerator;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class SiddhiContext {

    private int threads;
    private boolean asyncProcessing;
    private int eventBatchSize;
    private PersistenceService persistenceService;
    private String siddhiInstanceIdentifier;
    private ThreadBarrier threadBarrier;
    private ThreadPoolExecutor threadPoolExecutor;
    private ScheduledExecutorService scheduledExecutorService;
    private boolean distributedProcessing;
    private ElementIdGenerator elementIdGenerator;
    private GlobalIndexGenerator globalIndexGenerator;
    private HazelcastInstance hazelcastInstance;
    private String queryPlanIdentifier;
    private List<Class> siddhiExtensions;


    public SiddhiContext(String queryPlanIdentifier, boolean distributedProcessing) {
        this.queryPlanIdentifier = queryPlanIdentifier;
        this.distributedProcessing=distributedProcessing;
        elementIdGenerator =new ElementIdGenerator(queryPlanIdentifier);
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public boolean isAsyncProcessing() {
        return asyncProcessing;
    }

    public void setAsyncProcessing(boolean asyncProcessing) {
        this.asyncProcessing = asyncProcessing;
    }

    public int getEventBatchSize() {
        return eventBatchSize;
    }

    public void setEventBatchSize(int eventBatchSize) {
        this.eventBatchSize = eventBatchSize;
    }

    public void setPersistenceService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public PersistenceService getPersistenceService() {
        return persistenceService;
    }

    public String getSiddhiInstanceIdentifier() {
        return siddhiInstanceIdentifier;
    }

    public void setThreadBarrier(ThreadBarrier threadBarrier) {
        this.threadBarrier = threadBarrier;
    }

    public ThreadBarrier getThreadBarrier() {
        return threadBarrier;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public boolean isDistributedProcessing() {
        return distributedProcessing;
    }


    public ElementIdGenerator getElementIdGenerator() {
        return elementIdGenerator;
    }

    public GlobalIndexGenerator getGlobalIndexGenerator() {
        return globalIndexGenerator;
    }


    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    public void setGlobalIndexGenerator(GlobalIndexGenerator globalIndexGenerator) {
        this.globalIndexGenerator = globalIndexGenerator;
    }

    public void setSiddhiInstanceIdentifier(String siddhiInstanceIdentifier) {
        this.siddhiInstanceIdentifier = siddhiInstanceIdentifier;
    }

    public String getQueryPlanIdentifier() {
        return queryPlanIdentifier;
    }

    public void setSiddhiExtensions(List<Class> siddhiExtensions) {
        this.siddhiExtensions = siddhiExtensions;
    }

    public List<Class> getSiddhiExtensions() {
        return siddhiExtensions;
    }
}
