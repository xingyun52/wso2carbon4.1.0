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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.AMQStoreException;
import org.wso2.andes.server.AMQChannel;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.queue.AMQQueue;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.subscription.SubscriptionImpl;
import org.wso2.andes.server.util.AndesUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class DefaultClusteringEnabledSubscriptionManager implements ClusteringEnabledSubscriptionManager{

    private static Log log = LogFactory.getLog(DefaultClusteringEnabledSubscriptionManager.class);

    private Map<String,QueueDeliveryWorker> workMap =
            new ConcurrentHashMap<String,QueueDeliveryWorker>();

    /**
     * Keeps Subscription that have for this given queue
     */
    private Map<String,Map<String,CassandraSubscription>> subscriptionMap =
            new ConcurrentHashMap<String,Map<String,CassandraSubscription>>();



    private ExecutorService messageFlusherExecutor =  null;
    private SequentialThreadPoolExecutor messagePublishingExecutor = null;



    /**
     * Hash map that keeps the unacked messages.
     */
    private Map<AMQChannel, Map<Long, Semaphore>> unAckedMessagelocks =
            new ConcurrentHashMap<AMQChannel, Map<Long, Semaphore>>();


    private Map<AMQChannel,QueueSubscriptionAcknowledgementHandler> acknowledgementHandlerMap =
            new ConcurrentHashMap<AMQChannel,QueueSubscriptionAcknowledgementHandler>();

     private int queueWorkerWaitInterval; 


    public void init()  {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("QueueDeliveryWorker-%d").build();
        messageFlusherExecutor =  Executors.newFixedThreadPool(ClusterResourceHolder.getInstance().getClusterConfiguration().
                                      getSubscriptionPoolSize(),namedThreadFactory);
        messagePublishingExecutor = new SequentialThreadPoolExecutor((ClusterResourceHolder.getInstance().getClusterConfiguration().
                getPublisherPoolSize()),"messagePublishingExecutor");
        queueWorkerWaitInterval = ClusterResourceHolder.getInstance().getClusterConfiguration().
        getQueueWorkerInterval();



    }

    /**
     * Register a subscription for a Given Queue
     * This will handle the subscription addition task.
     * @param queue
     * @param subscription
     */
    public void addSubscription(AMQQueue queue, CassandraSubscription subscription) {
        try {
            if (subscription.getSubscription() instanceof SubscriptionImpl.BrowserSubscription) {
                ClusterResourceHolder.getInstance().getCassandraMessageStore()
                        .addUserQueueToGlobalQueue(AndesUtils.getGlobalQueueNameForDestinationQueue(queue.getResourceName()));
                QueueBrowserDeliveryWorker deliveryWorker = new QueueBrowserDeliveryWorker(subscription.getSubscription(),queue,subscription.getSession());
                deliveryWorker.send();
            } else {

                Map<String, CassandraSubscription> subscriptions = subscriptionMap.get(queue.getResourceName());

                if (subscriptions == null || subscriptions.size() == 0) {
                    synchronized (subscriptionMap) {
                        subscriptions = subscriptionMap.get(queue.getResourceName());
                        if (subscriptions == null || subscriptions.size() == 0) {
                            subscriptions = subscriptionMap.get(queue.getResourceName());
                            if (subscriptions == null) {
                                subscriptions = new ConcurrentHashMap<String, CassandraSubscription>();
                                subscriptions.put(subscription.getSubscription().getSubscriptionID() + "",
                                        subscription);
                                subscriptionMap.put(queue.getResourceName(), subscriptions);
                                //for topic subscriptions no need to handleSubscription
                                if(!queue.checkIfBoundToTopicExchange()) {
                                    handleSubscription(queue);
                                }
                            } else if (subscriptions.size() == 0) {
                                subscriptions.put(subscription.getSubscription().getSubscriptionID() + "",
                                        subscription);
                                //for topic subscriptions no need to handleSubscription
                                if(!queue.checkIfBoundToTopicExchange()) {
                                    handleSubscription(queue);
                                }
                            }
                        } else {

                            subscriptions.put(subscription.getSubscription().getSubscriptionID() + "", subscription);
                        }
                    }
                } else {
                    subscriptions.put(subscription.getSubscription().getSubscriptionID() + "", subscription);
                }

                log.info("Binding Subscription "+subscription.getSubscription().getSubscriptionID()+" to queue "+queue.getName());

            }
            ClusterResourceHolder.getInstance().getSubscriptionCoordinationManager().handleSubscriptionChange();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Handle Subscription removal for a queue.
     * @param queue  queue for this Subscription
     * @param subId  SubscriptionId
     */
    public void removeSubscription(String queue, String subId, boolean isBoundToTopics ) {
        try {
            Map<String,CassandraSubscription> subs = subscriptionMap.get(queue);
            if (subs != null && subs.containsKey(subId)) {
                subs.remove(subId);
                log.info("Removing Subscription " + subId + " from queue " + queue);
                if (subs.size() == 0) {
                    log.debug("Executing subscription removal handler to minimize message losses");
                    handleMessageRemoval(queue, AndesUtils.getGlobalQueueNameForDestinationQueue(queue));
                    //remove message counters for queues having 0 messages with 0 subscriptions
                    if(!isBoundToTopics) {
                        if(ClusterResourceHolder.getInstance().getCassandraMessageStore().
                                getCassandraMessageCountForQueue(queue) == 0)  {
                            ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessageCounterForQueue(queue);
                        }
                    }

                }
            }
        } catch (Exception e) {
            log.error("Error while removing subscription for queue: " + queue,e);
        }

        try {
            ClusterResourceHolder.getInstance().getSubscriptionCoordinationManager().handleSubscriptionChange();
        } catch (Exception e) {
            log.error("Error while notifying Subscription change");
        }

    }


    private void handleMessageRemoval(String userQueue , String globalQueue) throws AMQStoreException {

        /**
         * 1) Remove User userQueue from Global userQueue user userQueue mapping
         * 2) Collect messages from User userQueue
         * 3)
         * 4) Put the messages back to Global Queue
         */

        CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
        messageStore.removeUserQueueFromQpidQueue(globalQueue);
        String nodeQueue = AndesUtils.getNodeQueueNameForDestinationQueue(userQueue);

        long lastProcessedMessageID = 0;
        List<CassandraQueueMessage> messages = messageStore.getMessagesFromUserQueue(nodeQueue,40,lastProcessedMessageID);
        while (messages.size() != 0) {
            for (CassandraQueueMessage msg : messages) {
                lastProcessedMessageID = msg.getMessageId();
                if (msg.getNodeQueue().equals(nodeQueue)) {
                    messageStore.removeMessageFromUserQueue(userQueue, msg.getMessageId());

                    try {
                        //when adding back to global queue we mark it as an message that was already came in (as un-acked)
                        //we do not evaluate if message addressed queue is bound to topics as it is not used. Just pass false for that.
                        messageStore.addMessageToGlobalQueue(globalQueue, msg.getNodeQueue(), msg.getMessageId(), msg.getMessage(),false, false);
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
            messages = messageStore.getMessagesFromUserQueue(nodeQueue,40,lastProcessedMessageID);
        }


    }

    private void handleSubscription(AMQQueue queue) {
        try {
            ClusterResourceHolder.getInstance().getCassandraMessageStore().
                    addUserQueueToGlobalQueue(AndesUtils.getGlobalQueueNameForDestinationQueue(queue.getResourceName()));
            String nodeQueueName =AndesUtils.getNodeQueueNameForDestinationQueue(queue.getResourceName());
            ClusterResourceHolder.getInstance().getCassandraMessageStore().addMessageCounterForQueue(queue.getName());
            if (workMap.get(nodeQueueName) == null) {
                QueueDeliveryWorker work = new QueueDeliveryWorker(nodeQueueName,queue,subscriptionMap,messagePublishingExecutor, queueWorkerWaitInterval);
                workMap.put(nodeQueueName,work);
                messageFlusherExecutor.execute(work);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while adding subscription to queue :" + queue ,e);
        }

    }


    public void markSubscriptionForRemovel(String queue) {
        QueueDeliveryWorker work = workMap.get(queue);

        if (work != null) {
            work.stopFlusher();
        }

    }

    public int getNumberOfSubscriptionsForQueue(String queueName) {
        int numberOfSubscriptions = 0;
        Map<String,CassandraSubscription> subs = subscriptionMap.get(queueName);
        if(subs != null){
            numberOfSubscriptions = subs.size();
        }
        return numberOfSubscriptions;
    }

    public void stopAllMessageFlushers() {
        Collection<QueueDeliveryWorker> workers = workMap.values();
        for (QueueDeliveryWorker flusher : workers) {
            flusher.stopFlusher();
        }
    }

    public void startAllMessageFlushers() {
        Collection<QueueDeliveryWorker> workers = workMap.values();
        for (QueueDeliveryWorker flusher : workers) {
            flusher.startFlusher();
        }
    }

    public Map<String, QueueDeliveryWorker> getWorkMap() {
        return workMap;
    }

    public Map<AMQChannel, Map<Long, Semaphore>> getUnAcknowledgedMessageLocks() {
        return unAckedMessagelocks;
    }

    @Override
    public Map<AMQChannel, QueueSubscriptionAcknowledgementHandler> getAcknowledgementHandlerMap() {
        return acknowledgementHandlerMap;
    }

}
