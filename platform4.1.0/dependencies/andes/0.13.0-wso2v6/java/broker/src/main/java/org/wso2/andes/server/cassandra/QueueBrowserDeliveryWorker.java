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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.AMQStoreException;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.protocol.AMQProtocolSession;
import org.wso2.andes.server.queue.AMQQueue;
import org.wso2.andes.server.queue.QueueEntry;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.subscription.Subscription;
import org.wso2.andes.server.subscription.SubscriptionImpl;
import org.wso2.andes.server.util.AndesUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * From JMS Spec
 * -----------------
 *
 * A client uses a QueueBrowser to look at messages on a queue without removing
 * them.
 * The browse methods return a java.util.Enumeration that is used to scan the
 * queue’s messages. It may be an enumeration of the entire content of a queue or
 * it may only contain the messages matching a message selector.
 * Messages may be arriving and expiring while the scan is done. JMS does not
 * require the content of an enumeration to be a static snapshot of queue content.
 * Whether these changes are visible or not depends on the JMS provider.
 * 
 * When someone made a QueueBroswer Subscription, we read messages for that queue and 
 * send them to that subscription. 
 */

public class QueueBrowserDeliveryWorker {

    private Subscription subscription;
    private AMQQueue queue;
    private AMQProtocolSession session;
    private String id;
    private int defaultMessageCount = Integer.MAX_VALUE;
    private int messageCount;
    private int messageBatchSize;

    private static Log log = LogFactory.getLog(QueueBrowserDeliveryWorker.class);
    private long lastReadMessageId = 0;

    public QueueBrowserDeliveryWorker(Subscription subscription, AMQQueue queue, AMQProtocolSession session) {
        this.subscription = subscription;
        this.queue = queue;
        this.session = session;
        this.id = "" + subscription.getSubscriptionID();
        this.messageCount = defaultMessageCount;
        this.messageBatchSize = ClusterResourceHolder.getInstance().getClusterConfiguration().
                getMessageBatchSizeForBrowserSubscriptions();

    }


    public void send(){
           List<QueueEntry> messages = null;
        try {
            messages = getSortedMessages();
            if (messages.size() > 0) {
                int count = messageBatchSize;
                if (messages.size() < messageBatchSize) {
                    count = messages.size();
                }
                for (int i =0 ; i < count ; i ++) {
                    QueueEntry message = messages.get(i);
                    try {
                        if (subscription instanceof SubscriptionImpl.BrowserSubscription) {
                            subscription.send(message);
                        }

                    } catch (Exception e) {
                        log.error("Unexpected Error in Message Flusher Task " +
                                "while delivering the message : ", e);
                    }
                }

            }
        } catch (AMQStoreException e) {
            log.error("Error while sending message for Browser subscription",e);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
             // It is essential to confirm auto close , since in the client side it waits to know the end of the messages
                subscription.confirmAutoClose();
        }
    }

    private List<QueueEntry> getSortedMessages() throws Exception {
        CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().
                getCassandraMessageStore();
        List<CassandraQueueMessage> queueMessages = new ArrayList<CassandraQueueMessage>();
        queueMessages = readMessages(queueMessages, messageBatchSize);
        int retryCount = 2;
        while (queueMessages.size() < messageBatchSize && retryCount < 5) {
            queueMessages = readMessages(queueMessages, messageBatchSize * retryCount);
            retryCount++;
        }
        CustomComparator orderComparator = new CustomComparator();
        Collections.sort(queueMessages, orderComparator);
        return messageStore.getPreparedBrowserMessages(queue, session, queueMessages);
    }

    private List<CassandraQueueMessage> readMessages(List<CassandraQueueMessage> messages,int messageBatchSize) {
        CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().
                getCassandraMessageStore();
        String nodeQueue = AndesUtils.getNodeQueueNameForDestinationQueue(queue.getResourceName());
        List<CassandraQueueMessage> allMessages = messageStore.getMessagesFromUserQueue(nodeQueue,messageBatchSize,lastReadMessageId);
        for (CassandraQueueMessage message : allMessages) {
            if(message.getNodeQueue().equals(queue.getResourceName())){
                messages.add(message);
            }
            lastReadMessageId = message.getMessageId();
        }
        return messages;
    }

    public class CustomComparator implements Comparator<CassandraQueueMessage> {

        public int compare(CassandraQueueMessage message1, CassandraQueueMessage message2) {
            return (int) (message1.getMessageId()-message2.getMessageId());
        }
    }

}
