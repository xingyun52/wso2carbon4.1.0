/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.andes.server.cassandra;

/**
 * CassandraTopicPublisherManager
 *
 * Thread pool worker for TopicDeliveryWorker
 * */

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.server.ClusterResourceHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

public class CassandraTopicPublisherManager {

    private Map<String, TopicDeliveryWorker> workMap =
            new ConcurrentHashMap<String, TopicDeliveryWorker>();
    private Queue<String> subscriptionQueue = new ConcurrentLinkedQueue<String>();
    private HashMap<String, List<String>> userQueuesMap = new HashMap<String, List<String>>();
    private static Log log =  LogFactory.getLog(CassandraTopicPublisherManager.class);

    private ExecutorService topicPublisherExecutor = null;
    private SequentialThreadPoolExecutor messagePublishingExecutor = null;


    private boolean active = true;
    private TopicDeliveryWorker currentWork;

    public static final int poolSize = 20;



    public CassandraTopicPublisherManager(){

    }

    public void init() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("TopicDeliveryWorker-%d").build();
        topicPublisherExecutor = Executors.newFixedThreadPool(poolSize,namedThreadFactory);
        messagePublishingExecutor = new SequentialThreadPoolExecutor((ClusterResourceHolder.getInstance().getClusterConfiguration().
                getPublisherPoolSize()),"TopicPublishingExecutor");
    }

    public void start() {
        active = true;
        topicPublisherExecutor.submit(new CassandraTopicPublisherManagerTask());
    }


    public void stop() {

        active = false;
    }

    public void addWork(String id, TopicDeliveryWorker work) {

        workMap.put(id, work);
        subscriptionQueue.offer(id);
    }

    public TopicDeliveryWorker getCurrentWork() {
        return currentWork;
    }

    public void markSubscriptionForRemoval(String id) {
        TopicDeliveryWorker work = workMap.get(id);
        if (work != null) {
            work.setMarkedForRemoval(true);
        }
    }

    public int getSubscriptionCount() {
        return subscriptionQueue.size();
    }


    public Map<String, TopicDeliveryWorker> getWorkMap() {
        return workMap;
    }

    public Queue<String> getSubscriptionQueue() {
        return subscriptionQueue;
    }

    public List<String> getUserQueues(String amqpQueueName) {
        return userQueuesMap.get(amqpQueueName);
    }

    public boolean isActive() {
        return active;
    }

    public SequentialThreadPoolExecutor getMessagePublishingExecutor() {
        return messagePublishingExecutor;
    }

    private class CassandraTopicPublisherManagerTask implements Runnable {

        @Override
        public void run() {
            while (active) {
                if (subscriptionQueue.size() > 0) {

                    String id = subscriptionQueue.peek();
                    if (workMap.containsKey(id)) {
                        TopicDeliveryWorker work = workMap.get(id);
                        if (work.isMarkedForRemoval()) {
                            workMap.remove(id);
                            subscriptionQueue.remove();
                            if(log.isDebugEnabled()){
                                log.debug("Removing subscription queue "+id+" from work map");
                            }
                        } else {
                            if (!work.isWorking()) {
                                topicPublisherExecutor.execute(work);
                            }
                            subscriptionQueue.remove();
                            subscriptionQueue.offer(id);
                        }
                    } else {
                        subscriptionQueue.remove();
                    }

                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Error in thread sleep" ,e);
                }


            }
        }
    }
}
