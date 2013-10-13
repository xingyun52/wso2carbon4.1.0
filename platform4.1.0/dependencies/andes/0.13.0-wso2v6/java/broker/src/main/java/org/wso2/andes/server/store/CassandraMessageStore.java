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
package org.wso2.andes.server.store;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import me.prettyprint.cassandra.serializers.*;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.AMQException;
import org.wso2.andes.AMQStoreException;
import org.wso2.andes.framing.AMQShortString;
import org.wso2.andes.framing.FieldTable;
import org.wso2.andes.pool.AndesExecuter;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.cassandra.*;
import org.wso2.andes.server.cluster.ClusterManagementInformationMBean;
import org.wso2.andes.server.cluster.ClusterManager;
import org.wso2.andes.server.cluster.GlobalQueueManager;
import org.wso2.andes.server.cluster.coordination.*;
import org.wso2.andes.server.configuration.ClusterConfiguration;
import org.wso2.andes.server.exchange.Exchange;
import org.wso2.andes.server.information.management.QueueManagementInformationMBean;
import org.wso2.andes.server.logging.LogSubject;
import org.wso2.andes.server.message.AMQMessage;
import org.wso2.andes.server.protocol.AMQProtocolSession;
import org.wso2.andes.server.queue.AMQQueue;
import org.wso2.andes.server.queue.BaseQueue;
import org.wso2.andes.server.queue.IncomingMessage;
import org.wso2.andes.server.queue.QueueEntry;
import org.wso2.andes.server.queue.SimpleQueueEntryList;
import org.wso2.andes.server.stats.PerformanceCounter;
import org.wso2.andes.server.store.util.CassandraDataAccessException;
import org.wso2.andes.server.store.util.CassandraDataAccessHelper;
import org.wso2.andes.server.util.AndesUtils;
import org.wso2.andes.server.virtualhost.VirtualHostConfigSynchronizer;
import org.wso2.andes.tools.utils.DataCollector;

import com.google.common.base.Splitter;

/**
 * Class <code>CassandraMessageStore</code> is the Message Store implemented for cassandra
 * Working with andes as an alternative to Derby Message Store
 */
public class CassandraMessageStore implements MessageStore {

    private Cluster cluster;
    private final String USERNAME_KEY = "username";
    private final String PASSWORD_KEY = "password";
    private final String CONNECTION_STRING = "connectionString";
    private final String CLUSTER_KEY = "cluster";
    private final String ID_GENENRATOR = "idGenerator";


    private Keyspace keyspace;
    public final static String KEYSPACE = "QpidKeySpace";
    private final static String LONG_TYPE = "LongType";
    private final static String UTF8_TYPE = "UTF8Type";
    private final static String INTEGER_TYPE = "IntegerType";
    private final static String QUEUE_COLUMN_FAMILY = "Queue";
    private final static String QUEUE_DETAILS_COLUMN_FAMILY = "QueueDetails";
    private final static String QUEUE_DETAILS_ROW = "QUEUE_DETAILS";
    private final static String QUEUE_ENTRY_COLUMN_FAMILY = "QueueEntries";
    private final static String QUEUE_ENTRY_ROW = "QueueEntriesRow";
    private final static String EXCHANGE_COLUMN_FAMILY = "ExchangeColumnFamily";
    private final static String EXCHANGE_ROW = "ExchangesRow";
    private final static String BINDING_COLUMN_FAMILY = "Binding";
    private final static String MESSAGE_CONTENT_COLUMN_FAMILY = "MessageContent";
    private final static String MESSAGE_CONTENT_ID_COLUMN_FAMILY = "MessageContentIDs";
    private final static String MESSAGE_QUEUE_MAPPING_COLUMN_FAMILY =
            "MessageQueueMappingColumnFamily";
    private final static String MESSAGE_QUEUE_MAPPING_ROW =
            "MessageQueueMappingRow";
    private final static String SQ_COLUMN_FAMILY = "SubscriptionQueues";
    private final static String GLOBAL_QUEUE_TO_USER_QUEUE_COLUMN_FAMILY = "QpidQueues";
    private final static String USER_QUEUES_COLUMN_FAMILY = "UserQueues";
    private final static String GLOBAL_QUEUES_COLUMN_FAMILY =
            "GlobalQueue";
    private final static String GLOBAL_QUEUE_LIST_COLUMN_FAMILY = "GlobalQueueList";
    private final static String GLOBAL_QUEUE_LIST_ROW = "GlobalQueueListRow";

    private final static String QMD_COLUMN_FAMILY = "MetaData";
    private final static String QMD_ROW_NAME = "qpidMetaData";

    private final static String MSG_CONTENT_IDS_ROW = "messageContentIds";
    private final static String TOPIC_EXCHANGE_MESSAGE_IDS = "TopicExchangeMessageIds";
    private final static String PUB_SUB_MESSAGE_IDS = "pubSubMessages";
    private final static String TOPIC_SUBSCRIBERS = "topicSubscribers";
    private final static String TOPIC_SUBSCRIBER_QUEUES = "topicSubscriberQueues";
    private final static String TOPICS_COLUMN_FAMILY = "topics";
    private final static String TOPICS_ROW = "TOPICS";
    private final static String ACKED_MESSAGE_IDS_COLUMN_FAMILY = "acknowledgedMessageIds";
    private final static String ACKED_MESSAGE_IDS_ROW = "acknowledgedMessageIdsRow";


    private final static String NODE_DETAIL_COLUMN_FAMILY = "CusterNodeDetails";
    private final static String NODE_DETAIL_ROW = "NodeDetailsRow";
    private final static String MESSAGE_COUNTERS_COLUMN_FAMILY = "MessageCountDetails";
    private final static String MESSAGE_COUNTERS_RAW_NAME = "QueueMessageCountRow";

    private final AtomicLong _messageId = new AtomicLong(0);

    private MessageIdGenerator messageIdGenerator = null;

    private SortedMap<Long, Long> contentDeletionTasks = new ConcurrentSkipListMap<Long, Long>();

    private ConcurrentHashMap<Long, Long> pubSubMessageContentDeletionTasks;

    private ConcurrentHashMap<String, ArrayList<String>> topicSubscribersMap = new ConcurrentHashMap<String, ArrayList<String>>();
    private ConcurrentHashMap<String, ArrayList<String>> topicNodeQueuesMap = new ConcurrentHashMap<String, ArrayList<String>>();

    private CassandraMessageContentCache messageCacheForCassandra = null;

    private ContentRemoverTask messageContentRemovalTask = null;
    private PubSubMessageContentRemoverTask pubSubMessageContentRemoverTask = null;
    private ClusterManagementInformationMBean clusterManagementMBean;
    private QueueManagementInformationMBean queueManagementMBean;
    private InMemoryMessageRemoverTask inMemoryMessageRemoverTask = null;

    private boolean configured = false;

    private boolean isCassandraConnectionLive = false;

    private static StringSerializer stringSerializer = StringSerializer.get();
    private static LongSerializer longSerializer = LongSerializer.get();
    private static BytesArraySerializer bytesArraySerializer = BytesArraySerializer.get();
    private static IntegerSerializer integerSerializer = IntegerSerializer.get();
    private static ByteBufferSerializer byteBufferSerializer = ByteBufferSerializer.get();

    private Hashtable<Long,Long> removalPendingMessageIds = new Hashtable<Long,Long>();
    private Hashtable<Long,IncomingMessage> incomingMessageHashtable = new Hashtable<Long,IncomingMessage>();
    private HashSet<Long> alreadyAddedMessages = new HashSet<Long>();
    private HashMap<String,LinkedBlockingQueue<Long>> subscriberQueueMap = new HashMap<String,LinkedBlockingQueue<Long>>();
    private HashMap<String,HashSet<Long>> sentButNotAckedMessageMap = new HashMap<String,HashSet<Long>>();

    private PublishMessageWriter publishMessageWriter;
    private PublishMessageContentWriter publishMessageContentWriter;

    private boolean isInMemoryMode = false;

    private static Log log =
            LogFactory.getLog(CassandraMessageStore.class);

    public CassandraMessageStore() {
        ClusterResourceHolder.getInstance().setCassandraMessageStore(this);
    }

    public void addMessage(IncomingMessage message) {
        long messageId = message.getMessageNumber();
        StorableMessageMetaData metaData = message.headersReceived();

        if (isInMemoryMode && message.getExchange().toString().equalsIgnoreCase("amq.topic")) {
            try {
                if (alreadyAddedMessages.contains(messageId)) {
                    return;
                }
                addIncomingMessagesToMemory(messageId, message);
                addCompletedTopicMessageIds(message.getBinding(), messageId);
                alreadyAddedMessages.add(messageId);
            } catch (Exception e) {
                throw new RuntimeException("Error while adding messages to queues  ", e);
            }
        } else {
            for (BaseQueue destinationQueue : message.getDestinationQueues()) {
                try {
                    if (isCassandraConnectionLive) {
                        final int bodySize = 1 + metaData.getStorableSize();
                        byte[] underlying = new byte[bodySize];
                        underlying[0] = (byte) metaData.getType().ordinal();
                        ByteBuffer buf = ByteBuffer.wrap(underlying);
                        buf.position(1);
                        buf = buf.slice();

                        metaData.writeToBuffer(0, buf);

                        //see if message is addressed to a queue bound to topic exchange
                        boolean isDestinationQueueBoundToTopicExchange;
                        if (message.getExchange().equals("amq.topic")) {
                            isDestinationQueueBoundToTopicExchange = true;
                        } else {
                            isDestinationQueueBoundToTopicExchange = false;
                        }
                        addMessageToGlobalQueue(AndesUtils.getGlobalQueueNameForDestinationQueue(destinationQueue.getResourceName()),
                                message.getRoutingKey(), messageId, underlying, true, isDestinationQueueBoundToTopicExchange);
                    } else {
                        log.error("Error while adding messages to queues. Message Store is Inaccessible");
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error while adding messages to queues  ", e);
                }
            }
        }
    }

    private void addIncomingMessagesToMemory(long messageId, IncomingMessage incomingMessage) {
        incomingMessageHashtable.put(messageId, incomingMessage);
    }

    private void removeIncomingMessage(long messageId) {
        removalPendingMessageIds.put(messageId, System.currentTimeMillis());
    }

    private IncomingMessage getIncomingMessage(long messageId) {
        return incomingMessageHashtable.get(messageId);
    }

    public void addCompletedTopicMessageIds(String topic, long messageId){
        try {
            List<String> registeredSubscribers = getRegisteredSubscribersForTopic(topic);
            if (registeredSubscribers != null) {
                for (String subscriber : registeredSubscribers) {

                    try {
                        addCompletedMessageToSubscriberQueue(subscriber, messageId);
                    } catch (InterruptedException e){
                        log.error("Error adding message id " + messageId + "To subscriber " + subscriber + " using in memory mode");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while adding Message Id to Subscriber queue", e);
        }

    }
    private void addCompletedMessageToSubscriberQueue(String queueName,long messageID) throws InterruptedException {
        if (null != subscriberQueueMap.get(queueName)) {
            subscriberQueueMap.get(queueName).put(messageID);
        } else {
            LinkedBlockingQueue<Long> subscriberQueue = new LinkedBlockingQueue<Long>();
            subscriberQueue.put(messageID);
            subscriberQueueMap.put(queueName.trim(), subscriberQueue);
        }
    }

    public AMQMessage getSubscriberMessage(AMQQueue queue) throws InterruptedException {
         long nextMessageId = getPendingMessageId(queue.getName());
         AMQMessage message  =  null;
         if (nextMessageId != -1) {
             IncomingMessage incomingMessage =  getIncomingMessage(nextMessageId);
             message = new AMQMessage(incomingMessage.getStoredMessage());
         }
         return message;
     }

    private Long getPendingMessageId(String queueName) throws InterruptedException {
        long pendingMessageID = -1;
        LinkedBlockingQueue<Long> pendingMessageIds = subscriberQueueMap.get(queueName);
        HashSet<Long> sentButNotAckedMids = sentButNotAckedMessageMap.get(queueName);
        if (null == sentButNotAckedMids) {
            sentButNotAckedMids = new HashSet<Long>();
            sentButNotAckedMessageMap.put(queueName, sentButNotAckedMids);
        }
        if (null != pendingMessageIds) {
            pendingMessageID = pendingMessageIds.take();
            sentButNotAckedMids.add(pendingMessageID);
        }else {
             LinkedBlockingQueue<Long> subscriberQueue = new LinkedBlockingQueue<Long>();
             subscriberQueueMap.put(queueName.trim(),subscriberQueue);
        }
        return pendingMessageID;
    }



    /**
     * Get a given Number of Messages from User queue using the given offset
     *
     * @param queue         Queue name
     * @param messageCount  messagecount
     * @param lastMessageId last processed message id. we will try  to get messages from
     *                      lasProcessedMessageId+1 .. lasProcessedMessageId+1 + count
     * @return List of messages
     * @throws AMQStoreException in case of an Data Access Error
     */
    public List<QueueEntry> getMessagesFromUserQueue(String nodeQueue,AMQQueue queue,
                                                     int messageCount, long lastMessageId) throws AMQStoreException {

        List<QueueEntry> messages = null;
        SimpleQueueEntryList list = new SimpleQueueEntryList(queue);
        messages = new ArrayList<QueueEntry>();

        if(!isCassandraConnectionLive) {
            log.error("Cassandra Message Store is Inaccessible. Cannot Receive Messages from User Queues");
            return messages;
        }
        try {
            ColumnSlice<Long, byte[]> columnSlice = CassandraDataAccessHelper.getMessagesFromQueue(nodeQueue,
                    USER_QUEUES_COLUMN_FAMILY, keyspace, lastMessageId, messageCount);
            for (Object column : columnSlice.getColumns()) {
                if (column instanceof HColumn) {
                    long messageId = ((HColumn<Long, byte[]>) column).getName();
                    byte[] value = ((HColumn<Long, byte[]>) column).getValue();
                    byte[] dataAsBytes = value;
                    ByteBuffer buf = ByteBuffer.wrap(dataAsBytes);
                    buf.position(1);
                    buf = buf.slice();
                    MessageMetaDataType type = MessageMetaDataType.values()[dataAsBytes[0]];
                    StorableMessageMetaData metaData = type.getFactory().createMetaData(buf);
                    StoredCassandraMessage message = new StoredCassandraMessage(messageId, metaData);
                    message.setExchange("amq.direct");
                    AMQMessage amqMessage = new AMQMessage(message);
                    messages.add(list.add(amqMessage));
                }
            }
        } catch (NumberFormatException e) {
            throw new AMQStoreException("Error while accessing user queue" + nodeQueue, e);
        } catch (Exception e) {
            throw new AMQStoreException("Error while accessing user queue" + nodeQueue, e);
        }

        return messages;
    }

    /**
     * Get given number of messages from User Queue. If number of messages in the queue (qn) is less than the requested
     * Number of messages(rn) (qn <= rn) this will return all the messages in the given user queue
     *
     * @param userQueue    User Queue name
     * @param messageCount max message count
     * @param lastReadMessageId id of the last processed message
     * @return List of Messages
     */
    public List<CassandraQueueMessage> getMessagesFromUserQueue(String userQueue, int messageCount, long lastReadMessageId) {

        List<CassandraQueueMessage> messages = new ArrayList<CassandraQueueMessage>();
        ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();

        if (!isCassandraConnectionLive) {
            log.error("Cassandra Message Store is Inaccessible. Cannot Receive Messages from User Queues");
            return messages;
        }
        try {
            ColumnSlice<Long, byte[]> columnSlice = CassandraDataAccessHelper.getMessagesFromQueue(userQueue.trim(),
                    USER_QUEUES_COLUMN_FAMILY, keyspace,lastReadMessageId, messageCount);
            for (Object column : columnSlice.getColumns()) {
                if (column instanceof HColumn) {
                    long messageId = ((HColumn<Long, byte[]>) column).getName();
                    byte[] value = ((HColumn<Long, byte[]>) column).getValue();

                    byte[] dataAsBytes = value;
                    ByteBuffer buf = ByteBuffer.wrap(dataAsBytes);
                    buf.position(1);
                    buf = buf.slice();
                    MessageMetaDataType type = MessageMetaDataType.values()[dataAsBytes[0]];
                    StorableMessageMetaData metaData = type.getFactory().createMetaData(buf);
                    StoredCassandraMessage message = new StoredCassandraMessage(messageId, metaData);
                    message.setExchange("amq.direct");
                    AMQMessage amqMessage = new AMQMessage(message);
                    String queueName = amqMessage.getMessageMetaData().getMessagePublishInfo().getRoutingKey().toString();
                    CassandraQueueMessage cqm = new CassandraQueueMessage(messageId, queueName, dataAsBytes);
                    messages.add(cqm);
                }
            }
        } catch (NumberFormatException e) {
            log.error(e);
        } catch (Exception e) {
            log.error(e);
        }
        return messages;
    }

    public int getMessageCountOfUserQueues(String queueName) {
        int messageCount = 0;
        if (!isCassandraConnectionLive) {
            log.error("Error in Getting Messages from Global Queue: " + queueName + ". Message Store is Inaccessible.");
            return messageCount;
        }
        try {

            List<String> userQueues = getUserQueues(queueName);
            for (String userQueue : userQueues) {

                ColumnSlice<Long, byte[]> columnSlice = CassandraDataAccessHelper.getMessagesFromQueue(userQueue.trim(),
                        USER_QUEUES_COLUMN_FAMILY, keyspace, Integer.MAX_VALUE);

                messageCount = +columnSlice.getColumns().size();

            }
        } catch (NumberFormatException e) {
            log.error("Number format error in getting messages from global queue : " + queueName, e);
        } catch (Exception e) {
            log.error("Error in getting messages from global queue: " + queueName, e);
        }
        return messageCount;
    }


    public int getMessageCountOfGlobalQueue(String queueName) {
        int messageCount = 0;
        if (!isCassandraConnectionLive) {
            log.error("Error in getting messages from global queue: " + queueName + ". Message Store is Inaccessible.");
            return messageCount;
        }
        try {
            ColumnSlice<Long, byte[]> columnSlice = CassandraDataAccessHelper.getMessagesFromQueue(queueName.trim(),
                    GLOBAL_QUEUES_COLUMN_FAMILY, keyspace, Integer.MAX_VALUE);

            messageCount = columnSlice.getColumns().size();
        } catch (NumberFormatException e) {
            log.error("Number format error in getting messages from global queue : " + queueName, e);
        } catch (Exception e) {
            log.error("Error in getting messages from global queue: " + queueName, e);
        }
        return messageCount;
    }

    /**
     * Get List of messages from a given Global queue
     *
     * @param queueName    Global queue Name
     * @param messageCount Number of messages that must be fetched.
     * @return List of Messages.
     */
    public Queue<CassandraQueueMessage> getMessagesFromGlobalQueue(String queueName,
                                                                   int messageCount) throws AMQStoreException {
        Queue<CassandraQueueMessage> messages = new LinkedList<CassandraQueueMessage>();

        if (!isCassandraConnectionLive) {
            log.error("Error in getting messages from global queue: " + queueName + ". Message Store is Inaccessible.");
            return messages;
        }

        try {
            ColumnSlice<Long, byte[]> columnSlice = CassandraDataAccessHelper.getMessagesFromQueue(queueName.trim(),
                    GLOBAL_QUEUES_COLUMN_FAMILY, keyspace, messageCount);
            for (Object column : columnSlice.getColumns()) {
                if (column instanceof HColumn) {
                    long messageId = ((HColumn<Long, byte[]>) column).getName();
                    byte[] value = ((HColumn<Long, byte[]>) column).getValue();
                    CassandraQueueMessage msg
                            = new CassandraQueueMessage(messageId, queueName, value);
                    messages.add(msg);
                }
            }
        } catch (NumberFormatException e) {
            throw new AMQStoreException("Number format error in getting messages from global queue : " + queueName, e);
        } catch (Exception e) {
            throw new AMQStoreException("Error in getting messages from global queue: " + queueName, e);
        }

        return messages;
    }


    public List<QueueEntry> getMessagesFromGlobalQueue(AMQQueue queue,
                                                       AMQProtocolSession session, int messageCount) throws AMQStoreException {

        List<QueueEntry> messages = new ArrayList<QueueEntry>();
        SimpleQueueEntryList list = new SimpleQueueEntryList(queue);
        if (!isCassandraConnectionLive) {
            log.error("Error while getting messages from queue : " + queue + ". Message Store is Inaccessible.");
            return messages;
        }
        try {
            ColumnSlice<Long, byte[]> columnSlice = CassandraDataAccessHelper.
                    getMessagesFromQueue(queue.getName().trim(), GLOBAL_QUEUES_COLUMN_FAMILY, keyspace, messageCount);

            for (Object column : columnSlice.getColumns()) {
                if (column instanceof HColumn) {
                    long messageId = ((HColumn<Long, byte[]>) column).getName();
                    byte[] value = ((HColumn<Long, byte[]>) column).getValue();
                    byte[] dataAsBytes = value;
                    ByteBuffer buf = ByteBuffer.wrap(dataAsBytes);
                    buf.position(1);
                    buf = buf.slice();
                    MessageMetaDataType type = MessageMetaDataType.values()[dataAsBytes[0]];
                    StorableMessageMetaData metaData = type.getFactory().createMetaData(buf);
                    StoredCassandraMessage message = new StoredCassandraMessage(messageId, metaData);
                    message.setExchange("amq.direct");
                    AMQMessage amqMessage = new AMQMessage(message);
                    amqMessage.setClientIdentifier(session);
                    messages.add(list.add(amqMessage));
                }
            }
        } catch (Exception e) {
            throw new AMQStoreException("Error while getting messages from queue : " + queue, e);
        }

        return messages;
    }


    public void dequeueMessages(AMQQueue queue, List<QueueEntry> messagesToDelete) {

        try {
            List<QueueEntry> messages = messagesToDelete;
            ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();
            String key = queue.getResourceName() + "_" + clusterManager.getNodeId();

            for (QueueEntry queueEntry : messages) {
                removeMessageFromUserQueue(key, queueEntry.getMessage().getMessageNumber());
            }
        } catch (Exception e) {
            log.error("Error in dequeuing messages from " + queue.getName(), e);
        }
    }


    /**
     * Remove a message from User Queue
     *
     * @param queueName User queue name
     * @param messageId message id
     */
    public void removeMessageFromUserQueue(String queueName, long messageId) throws AMQStoreException {
        if (!isCassandraConnectionLive) {
            log.error("Error while removing message from User queue. Message Store is Inaccessible.");
            return;
        }
        try {
            String nodeQueueName = AndesUtils.getNodeQueueNameForDestinationQueue(queueName);
            CassandraDataAccessHelper.deleteLongColumnFromRaw(USER_QUEUES_COLUMN_FAMILY, nodeQueueName, messageId, keyspace);
        } catch (CassandraDataAccessException e) {
            throw new AMQStoreException("Error while removing message from User queue", e);
        }
    }

    /**
     * Remove List of Message From Cassandra Message Store. Use this to Delete set of messages in  CassandraMessageStore
     * In one DB Call
     *
     * @param queueName User Queue name
     * @param msgList   Message List
     * @throws AMQStoreException If Error occurs while removing data.
     */
    public void removeMessageBatchFromUserQueue(String queueName, List<CassandraQueueMessage> msgList)
            throws AMQStoreException {

        if (!isCassandraConnectionLive) {
            log.error("Error while removing messages from User queue. Message Store is Inaccessible.");
            return;
        }
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
        try {
            String nodeQueueName= AndesUtils.getNodeQueueNameForDestinationQueue(queueName);

            for (CassandraQueueMessage msg : msgList) {
                CassandraDataAccessHelper.deleteLongColumnFromRaw(USER_QUEUES_COLUMN_FAMILY, nodeQueueName,
                        msg.getMessageId(), mutator, false);
            }
        } catch (CassandraDataAccessException e) {
            throw new AMQStoreException("Error while removing messages from User queue", e);
        } finally {
            mutator.execute();
        }

    }


    /**
     * Remove a message from Global queue
     *
     * @param queueName
     * @param messageId
     */
    public void removeMessageFromGlobalQueue(String queueName, long messageId) {
        if (!isCassandraConnectionLive) {
            log.error("Error while removing messages from global queue " + queueName + ". Message Store is Inaccessible.");
            return;
        }
        try {
            CassandraDataAccessHelper.deleteLongColumnFromRaw(GLOBAL_QUEUES_COLUMN_FAMILY,
                    queueName, messageId, keyspace);
        } catch (CassandraDataAccessException e) {
            log.error("Error while removing messages from global queue " + queueName, e);
        }
    }

    public void removeMessageFromGlobalQueue(String queueName, long messageId, Mutator<String> mutator) {
        if (!isCassandraConnectionLive) {
            log.error("Error while removing messages from global queue " + queueName + ". Message Store is Inaccessible.");
            return;
        }
        try {
            CassandraDataAccessHelper.deleteLongColumnFromRaw(GLOBAL_QUEUES_COLUMN_FAMILY,
                    queueName, messageId, mutator, false);
        } catch (CassandraDataAccessException e) {
            log.error("Error while removing messages from global queue " + queueName, e);
        }
    }

    public void transferMessageBatchFromGlobalQueueToUserQueue(CassandraQueueMessage[] list, String globalQueueName) {

        if (!isCassandraConnectionLive) {
            log.error("Error while transferring messages from Global Queue to User Queues. Message Store is Inaccessible.");
            return;
        }
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
        try {
            for (CassandraQueueMessage msg : list) {
                addMessageToUserQueue(msg.getNodeQueue(), msg.getMessageId(), msg.getMessage(), mutator);
                removeMessageFromGlobalQueue(globalQueueName, msg.getMessageId(), mutator);
            }
        } catch (CassandraDataAccessException e) {
            e.printStackTrace();
            log.error("Error while transferring messages from Global Queue to User Queues");
        } finally {
            mutator.execute();
        }
    }

    public void removeMessageBatchFromGlobalQueue(List<CassandraQueueMessage> list, String globalQUeueName) {

        if (!isCassandraConnectionLive) {
            log.error("Error while removing messages from global queue " + globalQUeueName + ". " +
                    "Message Store is Inaccessible.");
            return;
        }
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
        try {
            for (CassandraQueueMessage msg : list) {
                removeMessageFromGlobalQueue(globalQUeueName, msg.getMessageId(), mutator);
            }
        } finally {
            mutator.execute();
        }
    }


    public void recover(ConfigurationRecoveryHandler recoveryHandler) throws AMQException {


        boolean readyOrTimeOut = false;
        boolean error = false;

        int initTimeOut = 10;
        int count = 0;
        int maxTries = 10;

        while (!readyOrTimeOut) {
            try {
                ConfigurationRecoveryHandler.QueueRecoveryHandler qrh = recoveryHandler.begin(this);
                loadQueues(qrh);

                ConfigurationRecoveryHandler.ExchangeRecoveryHandler erh = qrh.completeQueueRecovery();
                List<String> exchanges = loadExchanges(erh);
                ConfigurationRecoveryHandler.BindingRecoveryHandler brh = erh.completeExchangeRecovery();
                recoverBindings(brh, exchanges);
                brh.completeBindingRecovery();
            } catch (Exception e) {
                error = true;
                log.error("Error recovering persistent state: " + e.getMessage(), e);
            } finally {
                if (!error) {
                    readyOrTimeOut = true;
                    continue;
                } else {
                    long waitTime = initTimeOut * 1000 * (long) Math.pow(2, count);
                    log.warn("Waiting for Cluster data to be synced Please ,start the other nodes soon, wait time: "
                            + waitTime + "ms");
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {

                    }
                    if (count > maxTries) {
                        readyOrTimeOut = true;
                        throw new AMQStoreException("Max Backoff attempts expired for data recovery");
                    }
                    count++;
                }
            }

        }


    }


    private Keyspace createKeySpace() throws CassandraDataAccessException {

        this.keyspace = CassandraDataAccessHelper.createKeySpace(cluster, KEYSPACE);


        CassandraDataAccessHelper.createColumnFamily(QUEUE_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE);
        CassandraDataAccessHelper.createColumnFamily(BINDING_COLUMN_FAMILY, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(MESSAGE_CONTENT_COLUMN_FAMILY, KEYSPACE, this.cluster, INTEGER_TYPE);
        CassandraDataAccessHelper.createColumnFamily(MESSAGE_CONTENT_ID_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE);
        CassandraDataAccessHelper.createColumnFamily(SQ_COLUMN_FAMILY, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(GLOBAL_QUEUE_TO_USER_QUEUE_COLUMN_FAMILY, KEYSPACE, this.cluster,
                UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(QMD_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE);
        CassandraDataAccessHelper.createColumnFamily(QUEUE_DETAILS_COLUMN_FAMILY, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(QUEUE_ENTRY_COLUMN_FAMILY, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(EXCHANGE_COLUMN_FAMILY, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(USER_QUEUES_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE);
        CassandraDataAccessHelper.createColumnFamily(MESSAGE_QUEUE_MAPPING_COLUMN_FAMILY, KEYSPACE, this.cluster,
                UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(GLOBAL_QUEUES_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE);
        CassandraDataAccessHelper.createColumnFamily(GLOBAL_QUEUE_LIST_COLUMN_FAMILY, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(TOPIC_EXCHANGE_MESSAGE_IDS, KEYSPACE, this.cluster, LONG_TYPE);
        CassandraDataAccessHelper.createColumnFamily(PUB_SUB_MESSAGE_IDS, KEYSPACE, this.cluster, LONG_TYPE);
        CassandraDataAccessHelper.createColumnFamily(TOPIC_SUBSCRIBERS, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(TOPIC_SUBSCRIBER_QUEUES, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(TOPICS_COLUMN_FAMILY, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createColumnFamily(ACKED_MESSAGE_IDS_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE);
        CassandraDataAccessHelper.createColumnFamily(NODE_DETAIL_COLUMN_FAMILY, KEYSPACE, this.cluster, UTF8_TYPE);
        CassandraDataAccessHelper.createCounterColumnFamily(MESSAGE_COUNTERS_COLUMN_FAMILY,KEYSPACE,this.cluster);

        return keyspace;
    }

    public void addMessageCounterForQueue(String queueName) throws Exception {
        if (!isCassandraConnectionLive) {
            log.error("Error in adding message counters");
            return;
        }
        try {
            if(!getDestinationQueueNames().contains(queueName))
            CassandraDataAccessHelper.insertCounterColumn(MESSAGE_COUNTERS_COLUMN_FAMILY, MESSAGE_COUNTERS_RAW_NAME,queueName,keyspace);
        } catch (Exception e) {
            log.error("Error in accessing message counters", e);
            throw e;
        }
    }

    public void removeMessageCounterForQueue(String queueName) {
        if (!isCassandraConnectionLive) {
            log.error("Error removing the counter. Message Store is Inaccessible.");
            return;
        }
        try {
            CassandraDataAccessHelper.removeCounterColumn(MESSAGE_COUNTERS_COLUMN_FAMILY, MESSAGE_COUNTERS_RAW_NAME, queueName, keyspace);
        } catch (CassandraDataAccessException e) {
            log.error("Error in accessing message counters", e);
        }
    }

    public void incrementQueueCount(String queueName, long incrementBy) {
        if (!isCassandraConnectionLive) {
            log.error("Error while incrementing message counters. Message Store is Inaccessible.");
            return;
        }
        try {
        CassandraDataAccessHelper.incrementCounter(queueName,MESSAGE_COUNTERS_COLUMN_FAMILY, MESSAGE_COUNTERS_RAW_NAME,keyspace,incrementBy);
        } catch (CassandraDataAccessException e) {
            log.error("Error in accessing message counters", e);
        }
    }

    public void decrementQueueCount(String queueName, long decrementBy) {
        if (!isCassandraConnectionLive) {
            log.error("Error while decrementing message counters. Message Store is Inaccessible.");
            return;
        }
        try {
        CassandraDataAccessHelper.decrementCounter(queueName,MESSAGE_COUNTERS_COLUMN_FAMILY, MESSAGE_COUNTERS_RAW_NAME,
                keyspace,decrementBy);
        } catch (CassandraDataAccessException e) {
            log.error("Error in accessing message counters", e);
        }
    }

    public long getCassandraMessageCountForQueue(String queueName) {
        long msgCount = 0;
        if (!isCassandraConnectionLive) {
            log.error("Error while getting message count for queue. Message Store is Inaccessible.");
        }
        try {
            msgCount = CassandraDataAccessHelper.getCountValue(keyspace,MESSAGE_COUNTERS_COLUMN_FAMILY,queueName,
                    MESSAGE_COUNTERS_RAW_NAME);
        } catch (CassandraDataAccessException e) {
            log.error("Error in accessing message counters", e);
        }
        return msgCount;
    }

    private int getUserQueueCount(String qpidQueueName) throws AMQStoreException {
        int queueCount = 0;
        if (!isCassandraConnectionLive) {
            log.error("Error in getting user queue count for " + qpidQueueName + ". " +
                    "Message Store is Inaccessible.");
            return queueCount;
        }
        try {
            ColumnSlice<String, String> columnSlice = CassandraDataAccessHelper.
                    getStringTypeColumnsInARow(qpidQueueName, GLOBAL_QUEUE_TO_USER_QUEUE_COLUMN_FAMILY, keyspace,
                            Integer.MAX_VALUE);
            queueCount = columnSlice.getColumns().size();
        } catch (Exception e) {
            throw new AMQStoreException("Error in getting user queue count", e);
        }
        return queueCount;
    }

    /**
     * Add a Message to Internal User level Queue
     *
     * @param userQueue User Queue Name
     * @param messageId message id
     * @param message   message content.
     */
    public void addMessageToUserQueue(String userQueue, long messageId, byte[] message, Mutator<String> mutator)
            throws CassandraDataAccessException {

        if (!isCassandraConnectionLive) {
            log.error("Error in adding message :" + messageId + " to user queue :" +
                    userQueue + ". Message Store is Inaccessible");
            return;
        }
        try {

            CassandraDataAccessHelper.addMessageToQueue(USER_QUEUES_COLUMN_FAMILY, userQueue,
                    messageId, message, mutator, false);
            CassandraDataAccessHelper.addMappingToRaw(MESSAGE_QUEUE_MAPPING_COLUMN_FAMILY, MESSAGE_QUEUE_MAPPING_ROW,
                    "" + messageId, userQueue, mutator, false);
        } catch (Exception e) {
            throw new CassandraDataAccessException("Error in adding message :" + messageId + " to user queue :" +
                    userQueue, e);
        }
    }


    public void addMessageBatchToUserQueues(CassandraQueueMessage[] messages) throws CassandraDataAccessException {

        if (!isCassandraConnectionLive) {
            log.error("Error in adding message batch to Queues. Message Store is Inaccessible.");
            return;
        }
        try {
            Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
            try {
                for (CassandraQueueMessage message : messages) {
                    addMessageToUserQueue(message.getNodeQueue(), message.getMessageId(), message.getMessage(), mutator);
                }
            } finally {
                mutator.execute();
            }


        } catch (CassandraDataAccessException e) {
            throw new CassandraDataAccessException("Error in adding message batch to Queues ", e);
        }
    }



    public List<QueueEntry> getPreparedBrowserMessages(AMQQueue queue,
                                                       AMQProtocolSession session,
                                                       List<CassandraQueueMessage> queueMessages) throws AMQStoreException {
        List<QueueEntry> messages = new ArrayList<QueueEntry>();
        SimpleQueueEntryList list = new SimpleQueueEntryList(queue);
        if (!isCassandraConnectionLive) {
            log.error("Error while getting messages from queue : " + queue + "Message Store is Inaccessible.");
            return messages;
        }
        try {

            for (CassandraQueueMessage message : queueMessages) {
                long messageId = message.getMessageId();
                byte[] value = message.getMessage();
                byte[] dataAsBytes = value;
                ByteBuffer buf = ByteBuffer.wrap(dataAsBytes);
                buf.position(1);
                buf = buf.slice();
                MessageMetaDataType type = MessageMetaDataType.values()[dataAsBytes[0]];
                StorableMessageMetaData metaData = type.getFactory().createMetaData(buf);
                StoredCassandraMessage storedMessage = new StoredCassandraMessage(messageId, metaData);
                storedMessage.setExchange("amq.direct");
                AMQMessage amqMessage = new AMQMessage(storedMessage);
                amqMessage.setClientIdentifier(session);
                messages.add(list.add(amqMessage));
            }
        } catch (Exception e) {
            throw new AMQStoreException("Error while getting messages from queue : " + queue, e);
        }

        return messages;
    }



    /**
     * Add message to global queue
     *
     * @param globalQueueName
     * @param messageId
     * @param message
     */
    public void addMessageToGlobalQueue(String globalQueueName, String routingKey, long messageId, byte[] message,
                                        boolean isNewMessage, boolean isDestinationQueueBoundToTopicExchange) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Adding Message with id " + messageId + " to Queue " + globalQueueName);
        }
        publishMessageWriter.addMessage(globalQueueName, routingKey, messageId, message, isNewMessage, isDestinationQueueBoundToTopicExchange);
    }


    public void addMessageContent(String messageId, final int offset, ByteBuffer src) throws AMQStoreException {

        if (!isCassandraConnectionLive) {
            log.error("Error in adding message content. Message Store is Inaccessible.");
            return;
        }
        try {
            final String rowKey = "mid" + messageId;
            src = src.slice();
            final byte[] chunkData = new byte[src.limit()];

            src.duplicate().get(chunkData);


            long start = System.currentTimeMillis();
            Mutator<String> messageMutator = HFactory.createMutator(keyspace, stringSerializer);
            CassandraDataAccessHelper.addIntegerByteArrayContentToRaw(MESSAGE_CONTENT_COLUMN_FAMILY, rowKey,
                    offset, chunkData, messageMutator, false);
            messageMutator.execute();
            if (log.isDebugEnabled()) {
                log.debug("Content Write for " + rowKey + " took " + (System.currentTimeMillis() - start) + "ms");
            }
            //above inner class is instead of following
            //publishMessageContentWriter.addMessage(rowKey.trim(), offset, chunkData);
        } catch (Exception e) {
            throw new AMQStoreException("Error in adding message content", e);
        }
    }

//    public void removeMessageContent(String messageId) throws AMQStoreException {
//        try {
//            String rowKey = "mid" + messageId;
//            CassandraDataAccessHelper.deleteIntegerColumnFromRow(MESSAGE_CONTENT_COLUMN_FAMILY,rowKey.trim(),null,keyspace);
//        } catch (Exception e) {
//            throw new AMQStoreException("Error in removing message content", e);
//        }
//    }


    public int getContent(String messageId, int offsetValue, ByteBuffer dst) {

        int written = 0;
        int chunkSize = 65534;
        byte[] content = null;
        //read from cache.
        //written = messageCacheForCassandra.getContent(messageId,offsetValue,dst);
        //If entry is not there written value won't change
        if (!isCassandraConnectionLive) {
            log.error("Error in reading content. Message Store is Inaccessible.");
            return written;
        }
        if (written == 0) {
            //load from DB and add entry to the cache
            try {

                String rowKey = "mid" + messageId;
                if (offsetValue == 0) {

                    ColumnQuery columnQuery = HFactory.createColumnQuery(keyspace, stringSerializer,
                            integerSerializer, byteBufferSerializer);
                    columnQuery.setColumnFamily(MESSAGE_CONTENT_COLUMN_FAMILY);
                    columnQuery.setKey(rowKey.trim());
                    columnQuery.setName(offsetValue);

                    QueryResult<HColumn<Integer, ByteBuffer>> result = columnQuery.execute();
                    HColumn<Integer, ByteBuffer> column = result.get();
                    if (column != null) {
                        int offset = column.getName();
                        content = bytesArraySerializer.fromByteBuffer(column.getValue());

                        final int size = (int) content.length;
                        int posInArray = offset + written - offset;
                        int count = size - posInArray;
                        if (count > dst.remaining()) {
                            count = dst.remaining();
                        }
                        dst.put(content, 0, count);
                        written = count;
                    } else {
                        throw new RuntimeException("Unexpected Error , content already deleted");
                    }
                } else {
                    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
                    int k = offsetValue / chunkSize;
                    SliceQuery query = HFactory.createSliceQuery(keyspace, stringSerializer,
                            integerSerializer, byteBufferSerializer);
                    query.setColumnFamily(MESSAGE_CONTENT_COLUMN_FAMILY);
                    query.setKey(rowKey.trim());
                    query.setRange(k * chunkSize, (k + 1) * chunkSize + 1, false, 10);

                    QueryResult<ColumnSlice<Integer, ByteBuffer>> result = query.execute();
                    ColumnSlice<Integer, ByteBuffer> columnSlice = result.get();
                    boolean added = false;
                    for (HColumn<Integer, ByteBuffer> column : columnSlice.getColumns()) {
                        added = true;
                        byteOutputStream.write(bytesArraySerializer.fromByteBuffer(column.getValue()));
                    }
                    content = byteOutputStream.toByteArray();
                    final int size = (int) content.length;
                    int posInArray = offsetValue - (k * chunkSize);
                    int count = size - posInArray;
                    if (count > dst.remaining()) {
                        count = dst.remaining();
                    }

                    dst.put(content, posInArray, count);

                    written += count;
                }

                // add a new entry to the cache. If cache is full eldest entry will be removed.
                /*byte[] cacheValue = new byte[content.length];
                System.arraycopy(content, 0, cacheValue, 0, content.length);
                messageCacheForCassandra.addEntryToCache(messageId,offsetValue, cacheValue);*/

            } catch (Exception e) {
                e.printStackTrace();
                log.error("Error in reading content", e);
            }
        }
        return written;
    }

    public void storeMetaData(long messageId, StorableMessageMetaData metaData) {

        if (!isCassandraConnectionLive) {
            log.error("Error in storing meta data. Message Store is Inaccessible.");
            return;
        }
        try {
            final int bodySize = 1 + metaData.getStorableSize();
            byte[] underlying = new byte[bodySize];
            underlying[0] = (byte) metaData.getType().ordinal();
            java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(underlying);
            buf.position(1);
            buf = buf.slice();
            metaData.writeToBuffer(0, buf);

            Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);


            mutator.addInsertion(QMD_ROW_NAME, QMD_COLUMN_FAMILY, HFactory.createColumn(messageId,
                    underlying, longSerializer, bytesArraySerializer));
            mutator.execute();

        } catch (Exception e) {
            log.error("Error in storing meta data", e);
        }
    }

    private StorableMessageMetaData getMetaData(long messageId) {

        StorableMessageMetaData metaData = null;
        if (!isCassandraConnectionLive) {
            log.error("Error in getting meta data of provided message id. Message Store is Inaccessible.");
            return metaData;
        }
        try {
            HColumn<Long, byte[]> column = CassandraDataAccessHelper.
                    getLongByteArrayColumnInARow(QMD_ROW_NAME, QMD_COLUMN_FAMILY, messageId, keyspace);
            byte[] dataAsBytes = column.getValue();
            ByteBuffer buf = ByteBuffer.wrap(dataAsBytes);
            buf.position(1);
            buf = buf.slice();
            MessageMetaDataType type = MessageMetaDataType.values()[dataAsBytes[0]];
            metaData = type.getFactory().createMetaData(buf);
        } catch (Exception e) {
            log.error("Error in getting meta data of provided message id", e);
        }
        return metaData;
    }

    private void removeMetaData(long messageId) throws AMQStoreException {

        if (isCassandraConnectionLive) {
            log.error("Error in removing metadata. Message Store is Inaccessible.");
            return;
        }
        try {
            Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
            CassandraDataAccessHelper.deleteLongColumnFromRaw(QMD_COLUMN_FAMILY, QMD_ROW_NAME, messageId, mutator, true);
        } catch (Exception e) {
            throw new AMQStoreException("Error in removing metadata", e);
        }
    }

    /**
     * Acknowledged messages are added to this column family with the current system
     * time as the acknowledged time
     */
    public void addAckedMessage(long messageId) {

        if (!isCassandraConnectionLive) {
            log.error("Error in storing meta data. Message Store is Inaccessible.");
            return;
        }
        try {
            pubSubMessageContentDeletionTasks.put(messageId, messageId);
            Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
            long ackTime = System.currentTimeMillis();

            mutator.addInsertion(ACKED_MESSAGE_IDS_ROW, ACKED_MESSAGE_IDS_COLUMN_FAMILY, HFactory.createColumn(messageId,
                    ackTime, longSerializer, longSerializer));
            mutator.execute();
        } catch (Exception e) {
            log.error("Error in storing meta data", e);
        }
    }

    /**
     * When message contents are ready to remove , removing the reference to that from the acknowledged message
     * column family
     */
    private void removeAckedMessage(long messageId) throws AMQStoreException {
        if (!isCassandraConnectionLive) {
            log.error("Error in storing meta data. Message Store is Inaccessible.");
            return;
        }
        try {
            Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
            CassandraDataAccessHelper.deleteLongColumnFromRaw(ACKED_MESSAGE_IDS_COLUMN_FAMILY, ACKED_MESSAGE_IDS_ROW,
                    messageId, mutator, true);
        } catch (Exception e) {
            throw new AMQStoreException("Error in storing meta data", e);
        }
    }

    /**
     * Checking whether the message is ready to remove and remove the message if conditions satisfied
     */
    public boolean isReadyAndRemovedMessageContent(long messageId) {

        long currentSystemTime = System.currentTimeMillis();
        try {
            ColumnQuery<String, Long, Long> columnQuery =
                    HFactory.createColumnQuery(keyspace, stringSerializer, longSerializer, longSerializer);
            columnQuery.setKey(ACKED_MESSAGE_IDS_ROW);
            columnQuery.setColumnFamily(ACKED_MESSAGE_IDS_COLUMN_FAMILY);
            columnQuery.setName(messageId);
            QueryResult<HColumn<Long, Long>> result = null;
            if (isCassandraConnectionLive) {
                result = columnQuery.execute();
            } else {
                log.error("Error while removing meta data. Message Store is Inaccessible.");
            }

            if (result != null) {
                HColumn<Long, Long> column = result.get();
                //Checking whether the message is ready to remove

                if (column != null && column.getValue() != null) {
                    ClusterConfiguration clusterConfiguration = ClusterResourceHolder.getInstance().
                            getClusterConfiguration();
                    if ((currentSystemTime - column.getValue()) >= clusterConfiguration.getContentRemovalTimeDifference()) {
                        removeMetaData(messageId);
                        removeAckedMessage(messageId);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return true;
            }

        } catch (Exception e) {
            log.error("Error while removing Message data", e);
            return false;
        }
    }

    public void addBinding(Exchange exchange, AMQQueue amqQueue, String routingKey) throws CassandraDataAccessException {
        if (keyspace == null) {
            return;
        }
        if (!isCassandraConnectionLive) {
            log.error("Cannot add bindings. Message Store is Inaccessible.");
            return;
        }
        String columnName = routingKey;
        String columnValue = amqQueue.getName();
        CassandraDataAccessHelper.addMappingToRaw(BINDING_COLUMN_FAMILY, exchange.getName(), columnName,
                columnValue, keyspace);

    }

    public void addBinding(String exchangeName, String amqQueueName, String routingKey) throws CassandraDataAccessException {
        if (keyspace == null) {
            return;
        }
        if (!isCassandraConnectionLive) {
            log.error("Cannot add bindings. Message Store is Inaccessible.");
            return;
        }
        String columnName = routingKey;
        String columnValue = amqQueueName;
        CassandraDataAccessHelper.addMappingToRaw(BINDING_COLUMN_FAMILY, exchangeName, columnName,
                columnValue, keyspace);
    }


    public void removeBinding(Exchange exchange, AMQQueue amqQueue, String routingKey)
            throws CassandraDataAccessException {

        if (keyspace == null) {
            return;
        }
        if (!isCassandraConnectionLive) {
            log.error("Cannot add bindings. Message Store is Inaccessible.");
            return;
        }
        CassandraDataAccessHelper.deleteStringColumnFromRaw(BINDING_COLUMN_FAMILY, exchange.getName(), routingKey, keyspace);

    }


    /**
     * When a new message arrived for a topic, in implementations
     * before (0.13.0-wso2v5) it searched for the registered subscribers for that topic
     * once it got the list of registered subscribers for that topic
     * it adds the received message for all of those subscription queues
     *
     * But from 0,13.0-wso2v5 it does not do any search for subscriber queues ,
     * It get the TopicNodeQueue names which has subscriptions for that topic
     * and add the message id to those queues. This is done
     * to resolve the issue https://wso2.org/jira/browse/MB-89
     *
     *
     * @param topic     - Topic
     * @param messageId - Id of the new message
     */
    public void addTopicExchangeMessageIds(String topic, long messageId) {
        try {
            List<String> nodeQueuesForTopic = getRegisteredTopicNodeQueuesForTopic(topic);
            if (nodeQueuesForTopic != null) {
                for (String nodeQueue : nodeQueuesForTopic) {

                    try {
                        addMessageIdToSubscriberQueue(nodeQueue, messageId);
                    } catch (AMQStoreException e) {
                        log.error("Error adding message id " + messageId + "To subscriber " + nodeQueue);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while adding Message Id to Subscriber queue", e);
        }
    }

    /**
     * Getting messages from the provided queue
     * <p/>
     * This method retrives message from the queue. It search for the message ids
     * from the provided id to above
     *
     * @param queue            - AMQQueue
     * @param lastDeliveredMid - Id of the last delivered message
     * @return List of messages to be delivered
     */
    public List<AMQMessage> getSubscriberMessages(String queue, long lastDeliveredMid) {
        List<AMQMessage> messages = null;
        List<Long> messageIds = getPendingMessageIds(queue, lastDeliveredMid);
        if (messageIds.size() > 0) {
            messages = new ArrayList<AMQMessage>();
            for (long messageId : messageIds) {
                StorableMessageMetaData messageMetaData = getMetaData(messageId);
                if (messageMetaData != null) {
                    StoredCassandraMessage storedCassandraMessage = new StoredCassandraMessage(messageId, messageMetaData, true);
                    AMQMessage message = new AMQMessage(storedCassandraMessage, null);
                    messages.add(message);
                }
            }
        }
        return messages;
    }

    /**
     * Registers topic
     * Add an entry to the Topics column family to indicate that there is a subscriber for this topic
     *
     * @param topic - Topic name
     */
    private void registerTopic(String topic) {

        if (!isCassandraConnectionLive) {
            log.error("Error in registering queue for the topic. Message Store is Inaccessible.");
            return;
        }
        try {
            if (topic != null && (topicSubscribersMap.get(topic) == null)) {
                topicSubscribersMap.put(topic, new ArrayList<String>());
            }
            if (topic != null && (topicNodeQueuesMap.get(topic) == null)) {
                topicNodeQueuesMap.put(topic, new ArrayList<String>());
            }
            CassandraDataAccessHelper.addMappingToRaw(TOPICS_COLUMN_FAMILY, TOPICS_ROW, topic, topic, keyspace);
            log.info("Created Topic : "+topic);
        } catch (Exception e) {
            log.error("Error in registering queue for the topic", e);
        }
    }

    /**
     * Getting all the topics where subscribers exists
     */
    public List<String> getTopics() throws Exception {
        List<String> topicList = null;
        if (!isCassandraConnectionLive) {
            log.error("Error in getting the topic list. Message Store is Inaccessible.");
            return topicList;
        }
        try {

            topicList = CassandraDataAccessHelper.getRowList(TOPICS_COLUMN_FAMILY, TOPICS_ROW, keyspace);

        } catch (Exception e) {
            log.error("Error in getting the topic list", e);
            throw e;
        }

        return topicList;
    }


    public List<String> getUserQueues(String globalQueueName) throws Exception {
        if (keyspace == null) {
            return new ArrayList<String>();
        }
        if (!isCassandraConnectionLive) {
            log.error("Error in getting user queues for qpid queue :" + globalQueueName + ". Message Store is Inaccessible.");
            return new ArrayList<String>();
        }
        try {
            List<String> userQueues = CassandraDataAccessHelper.getRowList(GLOBAL_QUEUE_TO_USER_QUEUE_COLUMN_FAMILY,
                    globalQueueName, keyspace);
            return userQueues;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error in getting user queues for qpid queue :" + globalQueueName, e);
            throw e;
        }
    }


    public List<String> getDestinationQueueNames() throws Exception {

        List<String> destinationQueueNamesList;
        if (keyspace == null) {
            return new ArrayList<String>();
        }
        if (!isCassandraConnectionLive) {
            log.error("Error in getting global queues. Message Store is Inaccessible.");
        }
        try {
            destinationQueueNamesList = CassandraDataAccessHelper.getDestinationQueueNamesFromCounterColumns
                    (MESSAGE_COUNTERS_COLUMN_FAMILY, MESSAGE_COUNTERS_RAW_NAME, keyspace);
            return destinationQueueNamesList;
        } catch (Exception e) {
            log.error("Error in getting global queues", e);
            throw e;
        }
    }

    /**
     * Remove the topic from the topics column family when there are no subscribers for that topic
     *
     * @param topic
     */
    private void unRegisterTopic(String topic) throws AMQStoreException {

        if (!isCassandraConnectionLive) {
            log.error("Error in un registering topic. Cassandra Message Store is Inaccessible.");
        }
        try {
            CassandraDataAccessHelper.deleteStringColumnFromRaw(TOPICS_COLUMN_FAMILY, TOPICS_ROW, topic, keyspace);
            log.info("Removing Topic : "+topic);
        } catch (Exception e) {
            throw new AMQStoreException("Error in un registering topic", e);
        }
    }

    /**
     * Registers subscriber for topic
     * Simply adding the queue name as a subscriber for the provided topic
     *
     * @param topic     - Topic to be subscribed
     * @param workerQueue - Name of the TopicDeliveryWorker queue
     * @param  subscriptionQueue - Name of the amq queue
     */
    public void registerSubscriberForTopic(String topic, String workerQueue , String subscriptionQueue ) {
        if (keyspace == null) {
            return;
        }
        if (!isCassandraConnectionLive) {
            log.error("Error in registering queue for the topic. Message store is inaccessible.");
            return;
        }
        try {
            registerTopic(topic);
            CassandraDataAccessHelper.addMappingToRaw(TOPIC_SUBSCRIBERS, topic, workerQueue, workerQueue, keyspace);
            CassandraDataAccessHelper.addMappingToRaw(TOPIC_SUBSCRIBER_QUEUES, topic, subscriptionQueue, subscriptionQueue, keyspace);
            ClusterResourceHolder.getInstance().getTopicSubscriptionCoordinationManager().handleSubscriptionChange(topic);
            log.info("Registered Subscription "+workerQueue+" for Topic "+topic);
        } catch (Exception e) {
            log.error("Error in registering queue for the topic", e);
        }
    }

    /**
     * Retrieving the names of the subscriptions (Queue Names) which are subscribed for the
     * provided topic
     *
     * @param topic - Name of the topic
     * @return List of names
     */
    public List<String> getRegisteredSubscribersForTopic(String topic) throws Exception {
        try {
            List<String> queueList = topicSubscribersMap.get(topic);
            return queueList;
        } catch (Exception e) {
            log.error("Error in getting registered subscribers for the topic", e);
            throw e;
        }
    }

    /**
     * Retrieving the names of the subscriptions (Queue Names) which are subscribed for the
     * provided topic
     *
     * @param topic - Name of the topic
     * @return List of names
     */
    public List<String> getRegisteredTopicNodeQueuesForTopic(String topic) throws Exception {
        try {
            List<String> queueList = topicNodeQueuesMap.get(topic);
            return queueList;
        } catch (Exception e) {
            log.error("Error in getting registered subscribers for the topic", e);
            throw e;
        }
    }

    /**
     * Removing the subscription entry from the subscribers list for the topic
     *
     * @param topic     - Name of the topic
     * @param queueName - Queue name to be removed
     */
    public void unRegisterQueueFromTopic(String topic, String queueName) {

        try {
            if (log.isDebugEnabled()) {
                log.debug(" removing queue = " + queueName + " from topic =" + topic);
            }
            if (!isCassandraConnectionLive) {
                log.error("Error in un registering queue from the topic. Message store in inaccessible.");
                return;
            }
            CassandraDataAccessHelper.deleteStringColumnFromRaw(TOPIC_SUBSCRIBER_QUEUES, topic, queueName, keyspace);
            //no need to log removing subscription from direct exchange (internal change)
            if(!topic.startsWith("tmp_")) {
                log.info("Removing Subscription "+queueName+ " from Topic "+topic);
            }
            if ((getRegisteredSubscribersForTopic(topic) != null) && (getRegisteredSubscribersForTopic(topic).size() == 0)) {
                unRegisterTopic(topic);
                topicSubscribersMap.remove(topic);
            }
             if ((getRegisteredTopicNodeQueuesForTopic(topic) != null) && (getRegisteredTopicNodeQueuesForTopic(topic).size() == 0)) {
                topicNodeQueuesMap.remove(topic);
            }
            ClusterResourceHolder.getInstance().getTopicSubscriptionCoordinationManager().handleSubscriptionChange(topic);
        } catch (Exception e) {
            log.error("Error in un registering queue from the topic", e);
        }
    }

    /**
     * Adding message id to the subscriber queue
     *
     * @param queueName - Name of the queue
     * @param messageId - Message ID
     */
    private void addMessageIdToSubscriberQueue(String queueName, long messageId) throws AMQStoreException {

        if (!isCassandraConnectionLive) {
            log.error("Error in adding message Id to subscriber queue. Message store is Inaccessible.");
            return;
        }
        try {
            long columnName = messageId;
            long columnValue = messageId;
            CassandraDataAccessHelper.addLongContentToRow(PUB_SUB_MESSAGE_IDS, queueName, columnName, columnValue, keyspace);

        } catch (Exception e) {
            throw new AMQStoreException("Error in adding message Id to subscriber queue", e);
        }
    }

    /**
     * Search and return message ids of the provided queue begining from the
     * provided message id to above
     *
     * @param queueName        - Name of the queue
     * @param lastDeliveredMid - Last delivered message Id
     * @return list of message IDs
     */
    private List<Long> getPendingMessageIds(String queueName, long lastDeliveredMid) {
        List<Long> queueList = new ArrayList<Long>();
        if (!isCassandraConnectionLive) {
            log.error("Error in retriving message ids of the queue:" + queueName + ". Message store is inaccessible.");
            return queueList;
        }
        try {
            SliceQuery<String, Long, Long> sliceQuery =
                    HFactory.createSliceQuery(keyspace, stringSerializer, longSerializer, longSerializer);
            sliceQuery.setKey(queueName);
            sliceQuery.setColumnFamily(PUB_SUB_MESSAGE_IDS);
            sliceQuery.setRange(lastDeliveredMid, Long.MAX_VALUE, false, 1000);

            QueryResult<ColumnSlice<Long, Long>> result = sliceQuery.execute();
            ColumnSlice<Long, Long> columnSlice = result.get();
            for (HColumn<Long, Long> column : columnSlice.getColumns()) {
                queueList.add(column.getValue());
            }

        } catch (Exception e) {
            log.error("Error in retriving message ids of the queue", e);
        }

        return queueList;
    }

    /**
     * Remove delivered messages from the provided queue
     *
     * @param messageIdsToBeRemoved - List of delivered message ids to be removed
     * @param queueName             - name of the queue
     */
    public void removeDeliveredMessageIds(List<Long> messageIdsToBeRemoved, String queueName)
            throws AMQStoreException {

        if (isInMemoryMode) {
            HashSet<Long> unackedMessageIDsSet = sentButNotAckedMessageMap.get(queueName);
            for (Long mid : messageIdsToBeRemoved) {
                unackedMessageIDsSet.remove(mid);
                removeIncomingMessage(mid);
            }
        } else {
            if (!isCassandraConnectionLive) {
                log.error("Error in removing message ids from subscriber queue. Message Store is inaccessible");
                return;
            }
            try {
                Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
                for (Long mid : messageIdsToBeRemoved) {
                    CassandraDataAccessHelper.
                            deleteLongColumnFromRaw(PUB_SUB_MESSAGE_IDS, queueName, mid, mutator, false);
                    if (log.isDebugEnabled()) {
                        log.debug(" removing mid = " + mid + " from =" + queueName);
                    }
                }
                mutator.execute();
            } catch (Exception e) {
                throw new AMQStoreException("Error in removing message ids from subscriber queue", e);
            }
        }
    }


    public void synchBindings(VirtualHostConfigSynchronizer vhcs) {
        try {

            if (!isCassandraConnectionLive) {
                log.error("Error in synchronizing bindings. Message store is unreachable.");
                return;
            }

            Mutator<String> mutator =
                    HFactory.createMutator(keyspace, stringSerializer);


            RangeSlicesQuery<String, String, String> rangeSliceQuery =
                    HFactory.createRangeSlicesQuery(keyspace, stringSerializer, stringSerializer,
                            stringSerializer);
            rangeSliceQuery.setKeys("", "");
            rangeSliceQuery.setColumnFamily(BINDING_COLUMN_FAMILY);
            rangeSliceQuery.setRange("", "", false, 100);

            QueryResult<OrderedRows<String, String, String>> result = rangeSliceQuery.execute();
            OrderedRows<String, String, String> orderedRows = result.get();
            List<Row<String, String, String>> rowArrayList = orderedRows.getList();
            for (Row<String, String, String> row : rowArrayList) {
                String exchange = row.getKey();
                ColumnSlice<String, String> columnSlice = row.getColumnSlice();
                for (Object column : columnSlice.getColumns()) {
                    if (column instanceof HColumn) {
                        String columnName = ((HColumn<String, String>) column).getName();
                        String value = ((HColumn<String, String>) column).getValue();
                        vhcs.binding(exchange, value, columnName, null);
                    }
                }
            }
        } catch (NumberFormatException e) {
            log.error("Error in synchronizing bindings", e);
        }

    }

    public void recoverBindings(ConfigurationRecoveryHandler.BindingRecoveryHandler brh,
                                List<String> exchanges)
            throws Exception {

        if (!isCassandraConnectionLive) {
            log.error("Error occurred when recovering bindings. Message store is inaccessible.");
        }
        try {


            Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

            RangeSlicesQuery<String, String, String> rangeSliceQuery =
                    HFactory.createRangeSlicesQuery(keyspace, stringSerializer, stringSerializer,
                            stringSerializer);
            rangeSliceQuery.setKeys("", "");
            rangeSliceQuery.setColumnFamily(BINDING_COLUMN_FAMILY);
            rangeSliceQuery.setRange("", "", false, 100);

            QueryResult<OrderedRows<String, String, String>> result = rangeSliceQuery.execute();
            OrderedRows<String, String, String> orderedRows = result.get();
            List<Row<String, String, String>> rowArrayList = orderedRows.getList();
            for (Row<String, String, String> row : rowArrayList) {
                String exchange = row.getKey();
                ColumnSlice<String, String> columnSlice = row.getColumnSlice();
                for (Object column : columnSlice.getColumns()) {
                    if (column instanceof HColumn) {
                        String columnName = ((HColumn<String, String>) column).getName();
                        String value = ((HColumn<String, String>) column).getValue();
                        brh.binding(exchange, value, columnName, null);


                    }
                }
            }
        } catch (NumberFormatException e) {
            log.error("Number formatting error occurred when recovering bindings", e);
        }


    }

    private List<String> getBindings(String routingKey) {

        if (!isCassandraConnectionLive) {
            log.error("Error in getting bindings. Message store is inaccessible.");
            return new ArrayList<String>();
        }
        List<String> bindings = new ArrayList<String>();
        try {

            Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);


            // Retrieving multiple rows with Range Slice Query
            RangeSlicesQuery<String, String, String> rangeSlicesQuery =
                    HFactory.createRangeSlicesQuery(keyspace, stringSerializer, stringSerializer,
                            stringSerializer);
            rangeSlicesQuery.setKeys("DirectExchange", "DirectExchange");
            rangeSlicesQuery.setColumnFamily(BINDING_COLUMN_FAMILY);
            rangeSlicesQuery.setRange(routingKey, "", false, 10);


            QueryResult<OrderedRows<String, String, String>> result = rangeSlicesQuery.execute();
            OrderedRows<String, String, String> columnSlice = result.get();
            List<Row<String, String, String>> rows = columnSlice.getList();


            for (Object column : columnSlice.getList().get(0).getColumnSlice().getColumns()) {
                if (column instanceof HColumn) {
                    String columnName = ((HColumn<String, String>) column).getName();
                    String value = ((HColumn<String, String>) column).getValue();
                    String stringValue = new String(value);
                    bindings.add(stringValue);

                }
            }
        } catch (Exception e) {
            log.error("Error in getting bindings", e);
        }
        return bindings;
    }

    private void recoverMessages(MessageStoreRecoveryHandler recoveryHandler) {

        StorableMessageMetaData metaData = null;
        long maxId = 0;
        if (!isCassandraConnectionLive) {
            log.error("Error in recovering bindings. Message store is inaccessible.");
            return;
        }
        try {
            LongSerializer ls = LongSerializer.get();
            BytesArraySerializer bs = BytesArraySerializer.get();

            SliceQuery sliceQuery = HFactory.createSliceQuery(keyspace, stringSerializer, ls, bs);
            sliceQuery.setColumnFamily(QMD_COLUMN_FAMILY);
            sliceQuery.setKey(QMD_ROW_NAME);
            sliceQuery.setRange(Long.parseLong("0"), Long.MAX_VALUE, false, 10000);

            QueryResult<ColumnSlice<Long, byte[]>> result = sliceQuery.execute();

            ColumnSlice<Long, byte[]> columnSlice = result.get();

            List<HColumn<Long, byte[]>> columnList = columnSlice.getColumns();

            for (HColumn<Long, byte[]> column : columnList) {

                long key = column.getName();
                if (key > maxId) {
                    maxId = key;
                }
                byte[] dataAsBytes = column.getValue();

                ByteBuffer buf = ByteBuffer.wrap(dataAsBytes);
                buf.position(1);
                buf = buf.slice();
                MessageMetaDataType type = MessageMetaDataType.values()[dataAsBytes[0]];
                metaData = type.getFactory().createMetaData(buf);
            }
            _messageId.set(maxId);
        } catch (Exception e) {
            log.error("Error in recovering bindings", e);
        }
    }

    public AtomicLong currentMessageId() {
        return _messageId;
    }


    final static Splitter pipeSplitter = Splitter.on('|');

    public void synchQueues(VirtualHostConfigSynchronizer vhcs) throws Exception {

        if (!isCassandraConnectionLive) {
            log.error("Error in queue synchronization. Message store is inaccessble.");
        }
        try {
            // Retrieving multiple rows with Range Slice Query
            ColumnSlice<String, String> columnSlice = CassandraDataAccessHelper.
                    getStringTypeColumnsInARow(QUEUE_DETAILS_ROW, QUEUE_DETAILS_COLUMN_FAMILY, keyspace, Integer.MAX_VALUE);
            for (Object column : columnSlice.getColumns()) {
                if (column instanceof HColumn) {
                    String columnName = ((HColumn<String, String>) column).getName();
                    String value = ((HColumn<String, String>) column).getValue();
                    Iterable<String> results = pipeSplitter.split(value);
                    Iterator<String> it = results.iterator();
                    String owner = it.next();
                    boolean isExclusive = Boolean.parseBoolean(it.next());
                    vhcs.queue(columnName, owner, isExclusive, null);
                }
            }
        } catch (Exception e) {
            throw new AMQStoreException("Error in queue synchronization", e);
        }
    }


    public void loadQueues(ConfigurationRecoveryHandler.QueueRecoveryHandler qrh) throws Exception {

        if (!isCassandraConnectionLive) {
            log.error("Error in loading queues. Message store is inaccessible.");
            return;
        }
        try {


            // Retriving multiple rows with Range Slice Query
            ColumnSlice<String, String> columnSlice = CassandraDataAccessHelper.
                    getStringTypeColumnsInARow(QUEUE_DETAILS_ROW, QUEUE_DETAILS_COLUMN_FAMILY, keyspace,
                            Integer.MAX_VALUE);
            for (Object column : columnSlice.getColumns()) {
                if (column instanceof HColumn) {
                    String columnName = ((HColumn<String, String>) column).getName();
                    String value = ((HColumn<String, String>) column).getValue();
                    String[] valuesFields = value.split("\\|");
                    String owner = valuesFields[1];
                    boolean isExclusive = Boolean.parseBoolean(valuesFields[2]);
                    qrh.queue(columnName, owner, isExclusive, null);
                }
            }
        } catch (Exception e) {
            throw new AMQStoreException("Error in loading queues", e);
        }


    }


    /**
     * Add Global Queue to User Queue Mapping
     *
     * @param globalQueueName
     */
    public void addUserQueueToGlobalQueue(String globalQueueName) throws AMQStoreException {

        if (!isCassandraConnectionLive) {
            log.error("Error in adding user queue to global queue. Message store is inaccessible.");
            return;
        }
        try {
            String nodeQueueName = AndesUtils.getNodeQueueNameForGlobalQueue(globalQueueName);
            Mutator<String> qqMutator = HFactory.createMutator(keyspace, stringSerializer);
            CassandraDataAccessHelper.addMappingToRaw(GLOBAL_QUEUE_TO_USER_QUEUE_COLUMN_FAMILY, globalQueueName,
                    nodeQueueName, nodeQueueName, qqMutator, false);
            CassandraDataAccessHelper.addMappingToRaw(GLOBAL_QUEUE_LIST_COLUMN_FAMILY, GLOBAL_QUEUE_LIST_ROW, globalQueueName,
                    globalQueueName, qqMutator, true);
        } catch (Exception e) {
            throw new AMQStoreException("Error in adding user queue to global queue", e);
        }
    }

    public void removeUserQueueFromQpidQueue(String globalQueueName) {

        if (!isCassandraConnectionLive) {
            log.error("Error in removing user queue from qpid queue. Message store is inaccessible.");
            return;
        }
        try {
            ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();
            String userQueueName = globalQueueName + "_" +
                    clusterManager.getNodeId();
            CassandraDataAccessHelper.deleteStringColumnFromRaw(GLOBAL_QUEUE_TO_USER_QUEUE_COLUMN_FAMILY,
                    globalQueueName.trim(), userQueueName, keyspace);
        } catch (Exception e) {
            log.error("Error in removing user queue from qpid queue", e);
        }
    }

    @Override
    public void configureMessageStore(String name, MessageStoreRecoveryHandler recoveryHandler,
                                      Configuration config, LogSubject logSubject) throws Exception {
        if (!configured) {
            performCommonConfiguration(config);
            ClusterResourceHolder resourceHolder = ClusterResourceHolder.getInstance();

            CassandraTopicPublisherManager cassandraTopicPublisherManager =
                    resourceHolder.getCassandraTopicPublisherManager();
            if (cassandraTopicPublisherManager == null) {
                cassandraTopicPublisherManager = new CassandraTopicPublisherManager();
                resourceHolder.setCassandraTopicPublisherManager(cassandraTopicPublisherManager);
            }
            cassandraTopicPublisherManager.init();
            cassandraTopicPublisherManager.start();

        }

        recoverMessages(recoveryHandler);
    }


    private void performCommonConfiguration(Configuration configuration) throws Exception {
        String userName = (String) configuration.getProperty(USERNAME_KEY);
        String password = (String) configuration.getProperty(PASSWORD_KEY);

        Object connections = configuration.getProperty(CONNECTION_STRING);
        String connectionString = "";
        if (connections instanceof ArrayList) {
            ArrayList<String> cons = (ArrayList<String>) connections;

            for (String c : cons) {
                connectionString += c + ",";
            }

            connectionString = connectionString.substring(0, connectionString.length() - 1);
        } else if (connectionString instanceof String) {
            connectionString = (String) connections;
            if(connectionString.indexOf(":") > 0){
               String host = connectionString.substring(0,connectionString.indexOf(":"));
               int port = AndesUtils.getInstance().getCassandraPort();
               connectionString  = host +":"+ port;
            }
        }
        String clusterName = (String) configuration.getProperty(CLUSTER_KEY);
        String idGeneratorImpl = (String) configuration.getProperty(ID_GENENRATOR);

        cluster = CassandraDataAccessHelper.createCluster(userName, password, clusterName, connectionString);
        checkCassandraConnection();
        keyspace = createKeySpace();


        if (idGeneratorImpl != null && !"".equals(idGeneratorImpl)) {
            try {
                Class clz = Class.forName(idGeneratorImpl);

                Object o = clz.newInstance();
                messageIdGenerator = (MessageIdGenerator) o;
            } catch (Exception e) {
                log.error("Error while loading Message id generator implementation : " + idGeneratorImpl +
                        " adding TimeStamp based implementation as the default", e);
                messageIdGenerator = new TimeStampBasedMessageIdGenerator();
            }
        } else {
            messageIdGenerator = new TimeStampBasedMessageIdGenerator();
        }

        messageContentRemovalTask = new ContentRemoverTask(ClusterResourceHolder.getInstance().getClusterConfiguration().
                getContentRemovalTaskInterval());
        messageContentRemovalTask.setRunning(true);
        Thread t = new Thread(messageContentRemovalTask);
        t.setName(messageContentRemovalTask.getClass().getSimpleName() + "-Thread");
        t.start();

        pubSubMessageContentDeletionTasks = new ConcurrentHashMap<Long, Long>();

        ClusterConfiguration clusterConfiguration = ClusterResourceHolder.getInstance().getClusterConfiguration();
        pubSubMessageContentRemoverTask = new PubSubMessageContentRemoverTask(clusterConfiguration.
                getPubSubMessageRemovalTaskInterval());
        pubSubMessageContentRemoverTask.setRunning(true);
        Thread th = new Thread(pubSubMessageContentRemoverTask);
        th.start();


        publishMessageWriter = new PublishMessageWriter();
        publishMessageWriter.start();
        Thread messageWriter = new Thread(publishMessageWriter);
        messageWriter.setName(PublishMessageWriter.class.getName());
        messageWriter.start();

        //we do not use this anymore
        publishMessageContentWriter = new PublishMessageContentWriter();
//        publishMessageContentWriter.start();
//        Thread contentWriter = new Thread(publishMessageContentWriter);
//        contentWriter.setName(PublishMessageContentWriter.class.getName());
//        contentWriter.start();

        messageCacheForCassandra = new CassandraMessageContentCache();

        AndesConsistantLevelPolicy consistencyLevel = new AndesConsistantLevelPolicy();

        keyspace.setConsistencyLevelPolicy(consistencyLevel);


        if (ClusterResourceHolder.getInstance().getSubscriptionCoordinationManager() == null) {

            SubscriptionCoordinationManager subscriptionCoordinationManager =
                    new SubscriptionCoordinationManagerImpl();
            subscriptionCoordinationManager.init();
            ClusterResourceHolder.getInstance().setSubscriptionCoordinationManager(subscriptionCoordinationManager);
        }

        if (ClusterResourceHolder.getInstance().getTopicSubscriptionCoordinationManager() == null) {

            TopicSubscriptionCoordinationManager topicSubscriptionCoordinationManager =
                    new TopicSubscriptionCoordinationManager();
            topicSubscriptionCoordinationManager.init();
            ClusterResourceHolder.getInstance().setTopicSubscriptionCoordinationManager(topicSubscriptionCoordinationManager);
        }
        ClusterManager clusterManager = null;

        if (clusterConfiguration.isClusteringEnabled()) {
            clusterManager = new ClusterManager(ClusterResourceHolder.getInstance().
                    getCassandraMessageStore(), clusterConfiguration.getZookeeperConnection());
        } else {
            clusterManager = new ClusterManager(ClusterResourceHolder.getInstance().getCassandraMessageStore());
        }

        isInMemoryMode = clusterConfiguration.isInMemoryMode();
        if (isInMemoryMode) {
            inMemoryMessageRemoverTask = new InMemoryMessageRemoverTask(ClusterResourceHolder.getInstance().getClusterConfiguration().
                    getContentRemovalTaskInterval());
            inMemoryMessageRemoverTask.setRunning(true);
            Thread inMemoryMessageRemover = new Thread(inMemoryMessageRemoverTask);
            inMemoryMessageRemover.setName(inMemoryMessageRemoverTask.getClass().getSimpleName() + "-Thread");
            inMemoryMessageRemover.start();
        }

        ClusterResourceHolder.getInstance().setClusterManager(clusterManager);
        clusterManager.init();
        clusterManager.startAllGlobalQueueWorkers();


        clusterManagementMBean = new ClusterManagementInformationMBean(clusterManager);
        clusterManagementMBean.register();

        queueManagementMBean = new QueueManagementInformationMBean();
        queueManagementMBean.register();


        if (ClusterResourceHolder.getInstance().getClusterConfiguration().isOnceInOrderSupportEnabled()) {
            ClusteringEnabledSubscriptionManager subscriptionManager =
                    new OnceInOrderEnabledSubscriptionManager();
            ClusterResourceHolder.getInstance().setSubscriptionManager(subscriptionManager);
            subscriptionManager.init();

        } else {
            ClusteringEnabledSubscriptionManager subscriptionManager =
                    new DefaultClusteringEnabledSubscriptionManager();
            ClusterResourceHolder.getInstance().setSubscriptionManager(subscriptionManager);
            subscriptionManager.init();

        }
        configured = true;
    }

    public void syncTopicSubscriptionsWithDatabase(String topic) throws Exception {

        if (!isCassandraConnectionLive) {
            log.error("Error Synchronizing subscribers for topic. Message store is inaccessible.");
            return;
        }
        if (topic != null) {
            ArrayList<String> subscriberQueues = new ArrayList<String>();
            List<String> subscribers = CassandraDataAccessHelper.getRowList(TOPIC_SUBSCRIBER_QUEUES, topic, keyspace);
            for (String subscriber : subscribers) {
                subscriberQueues.add(subscriber);
            }
            topicSubscribersMap.remove(topic);
            topicSubscribersMap.put(topic, subscriberQueues);
        }
        if (log.isDebugEnabled()) {
            log.debug("Synchronizing subscribers for topic" + topic);
        }
    }

    public void syncTopicNodeQueuesWithDatabase(String topic) throws Exception {

          if (!isCassandraConnectionLive) {
              log.error("Error Synchronizing subscribers for topic. Message store is inaccessible.");
              return;
          }
          if (topic != null) {
              ArrayList<String> topicNodeQueuesList = new ArrayList<String>();
              List<String> topicNodeQueues = CassandraDataAccessHelper.getRowList(TOPIC_SUBSCRIBERS, topic, keyspace);
              for (String topicNodeQueue : topicNodeQueues) {
                  topicNodeQueuesList.add(topicNodeQueue);
              }
              topicNodeQueuesMap.remove(topic);
              topicNodeQueuesMap.put(topic, topicNodeQueuesList);
          }
          if (log.isDebugEnabled()) {
              log.debug("Synchronizing subscribers for topic" + topic);
          }
      }


    @Override
    public void close() throws Exception {
        if (!ClusterResourceHolder.getInstance().getClusterManager().isClusteringEnabled()) {
            ClusterResourceHolder.getInstance().getClusterManager().shutDownMyNode();
        }
        if (ClusterResourceHolder.getInstance().getClusterManager().isClusteringEnabled()) {
            deleteNodeData("" + ClusterResourceHolder.getInstance().getClusterManager().getNodeId());
        }
        if (messageContentRemovalTask != null && messageContentRemovalTask.isRunning()) {
            messageContentRemovalTask.setRunning(false);
        }

        if (pubSubMessageContentRemoverTask != null && pubSubMessageContentRemoverTask.isRunning()) {
            pubSubMessageContentRemoverTask.setRunning(false);
        }
        log.info("Stopping all current queue message publishers");
        ClusteringEnabledSubscriptionManager csm =
                ClusterResourceHolder.getInstance().getSubscriptionManager();
        if (csm != null) {
            csm.stopAllMessageFlushers();
        }

        log.info("Stopping all current topic message publishers");
        CassandraTopicPublisherManager stpm =
                ClusterResourceHolder.getInstance().getCassandraTopicPublisherManager();
        if (stpm != null && stpm.isActive()) {
            stpm.stop();
        }

        log.info("Stopping all global queue workers locally");
        ClusterManager cm = ClusterResourceHolder.getInstance().getClusterManager();
        if (cm != null) {
            GlobalQueueManager gqm = cm.getGlobalQueueManager();
            if (gqm != null) {
                gqm.stopAllQueueWorkersLocally();
            }
        }
    }

    @Override
    public <T extends StorableMessageMetaData> StoredMessage<T> addMessage(T metaData) {
        long mid = messageIdGenerator.getNextId();
        if (log.isDebugEnabled()) {
            log.debug("MessageID generated:" + mid);
        }
        return new StoredCassandraMessage(mid, metaData);
    }


    @Override
    public boolean isPersistent() {
        return false;
    }

    @Override
    public void configureConfigStore(String name, ConfigurationRecoveryHandler recoveryHandler,

                                     Configuration config, LogSubject logSubject) throws Exception {
        if (!configured) {
            performCommonConfiguration(config);
            recover(recoveryHandler);

            ClusterResourceHolder resourceHolder = ClusterResourceHolder.getInstance();
            CassandraTopicPublisherManager cassandraTopicPublisherManager =
                    resourceHolder.getCassandraTopicPublisherManager();
            if (cassandraTopicPublisherManager == null) {
                cassandraTopicPublisherManager = new CassandraTopicPublisherManager();
                resourceHolder.setCassandraTopicPublisherManager(cassandraTopicPublisherManager);
            }
            cassandraTopicPublisherManager.init();
            cassandraTopicPublisherManager.start();
        }

    }


    @Override
    public void createExchange(Exchange exchange) throws AMQStoreException {

        if (!isCassandraConnectionLive) {
            log.error("Error in creating exchange " + exchange.getName() + ". Message store is inaccessible.");
            return;
        }
        try {
            String name = exchange.getName();
            String type = exchange.getTypeShortString().asString();
            Short autoDelete = exchange.isAutoDelete() ? (short) 1 : (short) 0;
            String value = name + "|" + type + "|" + autoDelete;
            CassandraDataAccessHelper.addMappingToRaw(EXCHANGE_COLUMN_FAMILY, EXCHANGE_ROW, name, value, keyspace);
        } catch (Exception e) {
            throw new AMQStoreException("Error in creating exchange " + exchange.getName(), e);
        }
    }


    public List<String> loadExchanges(ConfigurationRecoveryHandler.ExchangeRecoveryHandler erh)
            throws Exception {

        List<String> exchangeNames = new ArrayList<String>();
        if (!isCassandraConnectionLive) {
            log.error("Error in loading exchanges. Message store is inaccessible.");
            return exchangeNames;
        }
        try {
            ColumnSlice<String, String> columnSlice = CassandraDataAccessHelper.
                    getStringTypeColumnsInARow(EXCHANGE_ROW, EXCHANGE_COLUMN_FAMILY, keyspace, Integer.MAX_VALUE);
            for (Object column : columnSlice.getColumns()) {
                if (column instanceof HColumn) {
                    String columnName = ((HColumn<String, String>) column).getName();
                    String value = ((HColumn<String, String>) column).getValue();
                    String[] valuesFields = value.split("|");
                    String type = valuesFields[1];
                    short autoDelete = Short.parseShort(valuesFields[2]);
                    exchangeNames.add(columnName);
                    erh.exchange(columnName, type, autoDelete != 0);

                }
            }
        } catch (Exception e) {
            throw new AMQStoreException("Error in loading exchanges", e);
        }


        return exchangeNames;
    }

    public List<String> synchExchanges(VirtualHostConfigSynchronizer vhcs) throws Exception {

        List<String> exchangeNames = new ArrayList<String>();
        if (!isCassandraConnectionLive) {
            log.error("Error in synchronizing exchanges. Message store is inaccessible.");
            return exchangeNames;
        }
        try {
            // Retriving multiple rows with Range Slice Query
            ColumnSlice<String, String> columnSlice = CassandraDataAccessHelper.
                    getStringTypeColumnsInARow(EXCHANGE_ROW, EXCHANGE_COLUMN_FAMILY, keyspace, Integer.MAX_VALUE);
            for (Object column : columnSlice.getColumns()) {
                if (column instanceof HColumn) {
                    String columnName = ((HColumn<String, String>) column).getName();
                    String value = ((HColumn<String, String>) column).getValue();
                    String[] valuesFields = value.split("|");
                    String type = valuesFields[1];
                    short autoDelete = Short.parseShort(valuesFields[2]);
                    exchangeNames.add(columnName);
                    vhcs.exchange(columnName, type, autoDelete != 0);

                }
            }
        } catch (Exception e) {
            throw new AMQStoreException("Error in synchronizing exchanges", e);
        }


        return exchangeNames;
    }


    @Override
    public void removeExchange(Exchange exchange) throws AMQStoreException {
        throw new UnsupportedOperationException("removeExchange function is unsupported");
    }

    @Override
    public void bindQueue(Exchange exchange, AMQShortString routingKey,
                          AMQQueue queue, FieldTable args) throws AMQStoreException {

        try {
            addBinding(exchange, queue, routingKey.asString());
        } catch (CassandraDataAccessException e) {
            throw new AMQStoreException("Error adding Binding details to cassandra store", e);
        }

    }

    @Override
    public void unbindQueue(Exchange exchange, AMQShortString routingKey, AMQQueue queue, FieldTable args) throws AMQStoreException {
        try {
            removeBinding(exchange, queue, routingKey.asString());
        } catch (CassandraDataAccessException e) {
            throw new AMQStoreException("Error removing binding details from cassandra store", e);
        }
    }

    @Override
    public void createQueue(AMQQueue queue, FieldTable arguments) throws AMQStoreException {
        createQueue(queue);
    }


    public void createQueue(AMQQueue queue) {

        if (!isCassandraConnectionLive) {
            log.error("Error While creating queue" + queue.getName() + "Message store is inaccessible.");
            return;
        }
        try {
            String owner = queue.getOwner() == null ? null : queue.getOwner().toString();
            String value = queue.getNameShortString().toString() + "|" + owner + "|" + (queue.isExclusive() ? "true" : "false");
            CassandraDataAccessHelper.addMappingToRaw(QUEUE_DETAILS_COLUMN_FAMILY, QUEUE_DETAILS_ROW,
                    queue.getNameShortString().toString(), value, keyspace);
        } catch (Exception e) {
            throw new RuntimeException("Error While creating queue" + queue.getName(), e);
        }
    }

    /**
     * Add Node details to cassandra
     *
     * @param nodeId node id
     * @param data   node data
     */
    public void addNodeDetails(String nodeId, String data) {
        if (!isCassandraConnectionLive) {
            log.error("Error writing Node details to cassandra database. Message store is inaccessible.");
            return;
        }
        try {
            CassandraDataAccessHelper.addMappingToRaw(NODE_DETAIL_COLUMN_FAMILY, NODE_DETAIL_ROW, nodeId, data, keyspace);
        } catch (CassandraDataAccessException e) {
            throw new RuntimeException("Error writing Node details to cassandra database", e);
        }
    }

    /**
     * Get Node data from a given node
     *
     * @param nodeId node id assigned by the cluster manager
     * @return Node data
     */
    public String getNodeData(String nodeId) {
        if (!isCassandraConnectionLive) {
            log.error("Error accessing Node details to cassandra database. Message store is inaccessible.");
            return null;
        }
        try {

            ColumnSlice<String, String> values = CassandraDataAccessHelper.getStringTypeColumnsInARow(NODE_DETAIL_ROW, NODE_DETAIL_COLUMN_FAMILY,
                    keyspace, Integer.MAX_VALUE);

            Object column = values.getColumnByName(nodeId);

            String columnName = ((HColumn<String, String>) column).getName();
            String value = ((HColumn<String, String>) column).getValue();
            return value;

        } catch (CassandraDataAccessException e) {
            throw new RuntimeException("Error accessing Node details to cassandra database");
        }
    }

    /**
     * Returns list of all Node ids stored as Cluster nodes in the cassandra database
     *
     * @return
     */
    public List<String> storedNodeDetails() {

        if (!isCassandraConnectionLive) {
            log.error("Error accessing Node details to cassandra database. Message store is inaccessible.");
            return new ArrayList<String>();
        }
        try {
            ColumnSlice<String, String> values = CassandraDataAccessHelper.getStringTypeColumnsInARow(NODE_DETAIL_ROW, NODE_DETAIL_COLUMN_FAMILY,
                    keyspace, Integer.MAX_VALUE);


            List<HColumn<String, String>> columns = values.getColumns();
            List<String> nodes = new ArrayList<String>();
            for (HColumn<String, String> column : columns) {
                nodes.add(column.getName());
            }

            return nodes;

        } catch (CassandraDataAccessException e) {
            throw new RuntimeException("Error accessing Node details to cassandra database");
        }
    }


    public void deleteNodeData(String nodeId) {

        if (!isCassandraConnectionLive) {
            log.error("Error accessing Node details to cassandra database. Message store is inaccessible.");
            return;
        }
        try {
            CassandraDataAccessHelper.deleteStringColumnFromRaw(NODE_DETAIL_COLUMN_FAMILY, NODE_DETAIL_ROW, nodeId, keyspace);
        } catch (CassandraDataAccessException e) {
            throw new RuntimeException("Error accessing Node details to cassandra database");
        }
    }

    /**
     * Create a Global Queue in Cassandra MessageStore
     *
     * @param queueName
     */
    public void createGlobalQueue(String queueName) throws AMQStoreException {

        if (!isCassandraConnectionLive) {
            log.error("Error while adding Global Queue to Cassandra message store. Message store is inaccessible.");
            return;
        }
        try {
            CassandraDataAccessHelper.addMappingToRaw(GLOBAL_QUEUE_LIST_COLUMN_FAMILY, GLOBAL_QUEUE_LIST_ROW, queueName,
                    queueName, keyspace);
            log.info("Created Queue : "+queueName);
        } catch (CassandraDataAccessException e) {
            throw new AMQStoreException("Error while adding Global Queue to Cassandra message store", e);
        }

    }

    @Override
    public void removeQueue(AMQQueue queue) throws AMQStoreException {

        //avoiding cassandra alive check, as error should be shown in UI.
        try {
            String queueName = queue.getNameShortString().toString();
            CassandraDataAccessHelper.deleteStringColumnFromRaw(QUEUE_DETAILS_COLUMN_FAMILY, QUEUE_DETAILS_ROW,
                    queueName, keyspace);
        } catch (CassandraDataAccessException e) {
            throw new AMQStoreException("Error while deleting queue : " + queue, e);
        }

    }


    /**
     * Removes a global queue from Cassandra Message Store
     * This will remove the Global queue and associated User queues from the Stores
     * With all the message entries and message content with it.
     *
     * @param queueName Global QueueName
     * @throws AMQStoreException If Error occurs while deleting the queues
     */
    public void removeGlobalQueue(String queueName) throws AMQStoreException {

        if (!isCassandraConnectionLive) {
            log.error("Error while removing Global Queue" + queueName + ". Message store is inaccessible.");
            return;
        }
        try {

            List<String> userQueues = getUserQueues(queueName);

            for (String userQ : userQueues) {
                CassandraDataAccessHelper.deleteStringColumnFromRaw(GLOBAL_QUEUE_TO_USER_QUEUE_COLUMN_FAMILY,
                        queueName, userQ, keyspace);
            }

            CassandraDataAccessHelper.deleteStringColumnFromRaw(GLOBAL_QUEUE_LIST_COLUMN_FAMILY, GLOBAL_QUEUE_LIST_ROW,
                    queueName, keyspace);
            removeMessageCounterForQueue(queueName);

            if(!queueName.startsWith("tmp_")) {
                log.info("Removed Global Queue Assigned for Queue: "+queueName);
            } else {
                log.info("Removed Global Queue Assigned for Topic Subscription: "+queueName);
            }

        } catch (Exception e) {
            throw new AMQStoreException("Error while removing Global Queue  : " + queueName, e);
        }
    }

    /**
     * This will check if cassandra connection is live in an exponential back-off way
     */
    public void checkCassandraConnection() {
        Thread cassandraConnectionCheckerThread = new Thread(new Runnable() {
            public void run() {
                int retriedCount = 0;
                while (true) {
                    try {
                        if (cluster.describeClusterName() != null) {
                            boolean previousState = isCassandraConnectionLive;
                            isCassandraConnectionLive = true;
                            retriedCount = 0;
                            if (previousState == false) {
                                //start back all tasks accessing cassandra
                                log.info("Cassandra Message Store is alive....");

                                log.info("Starting all current queue message publishers");
                                ClusteringEnabledSubscriptionManager csm =
                                        ClusterResourceHolder.getInstance().getSubscriptionManager();
                                if (csm != null) {
                                    csm.startAllMessageFlushers();
                                }
                                log.info("Starting all current topic message publishers");
                                CassandraTopicPublisherManager stpm =
                                        ClusterResourceHolder.getInstance().getCassandraTopicPublisherManager();
                                if (stpm != null && !stpm.isActive()) {
                                    stpm.start();
                                }

                                log.info("Starting all available Global Queue Workers");
                                ClusterManager cm =  ClusterResourceHolder.getInstance().getClusterManager();
                                if(cm != null) {
                                    GlobalQueueManager gqm = cm.getGlobalQueueManager();
                                    if (gqm != null) {
                                        gqm.startAllQueueWorkersLocally();
                                    }
                                }

                                log.info("Starting all message content writers");
                                if(publishMessageContentWriter != null) {
                                    publishMessageWriter.start();
                                }

                                log.info("Starting message content deletion");
                                if (messageContentRemovalTask != null && !messageContentRemovalTask.isRunning()) {
                                    messageContentRemovalTask.setRunning(true);
                                }

                                log.info("Starting pub-sub message removal task");
                                if (pubSubMessageContentRemoverTask != null && !pubSubMessageContentRemoverTask.isRunning()) {
                                    pubSubMessageContentRemoverTask.setRunning(true);
                                }
                            }
                            Thread.sleep(10000);
                        }
                    } catch (HectorException e) {

                        try {

                            if (e.getMessage().contains("All host pools marked down. Retry burden pushed out to client")) {

                                isCassandraConnectionLive = false;
                                //print the error log several times
                                if (retriedCount < 5) {
                                    log.error(e);
                                }
                                retriedCount += 1;
                                if (retriedCount == 4) {
                                    //stop all tasks accessing  Cassandra
                                    log.error("Cassandra Message Store is Inaccessible....");

                                    log.info("Stopping all current queue message publishers");
                                    ClusteringEnabledSubscriptionManager csm =
                                            ClusterResourceHolder.getInstance().getSubscriptionManager();
                                    if (csm != null) {
                                        csm.stopAllMessageFlushers();
                                    }

                                    log.info("Stopping all current topic message publishers");
                                    CassandraTopicPublisherManager stpm =
                                            ClusterResourceHolder.getInstance().getCassandraTopicPublisherManager();
                                    if (stpm != null && stpm.isActive()) {
                                        stpm.stop();
                                    }

                                    log.info("Stopping all global queue workers locally");
                                    ClusterManager cm =  ClusterResourceHolder.getInstance().getClusterManager();
                                    if (cm != null) {
                                        GlobalQueueManager gqm = cm.getGlobalQueueManager();
                                        if (gqm != null) {
                                            gqm.stopAllQueueWorkersLocally();
                                        }
                                    }

                                    log.info("Stopping all message content writers");
                                    if(publishMessageContentWriter != null) {
                                        publishMessageWriter.stop();
                                    }

                                    log.info("Stopping message content deletion");
                                    if (messageContentRemovalTask != null && messageContentRemovalTask.isRunning()) {
                                        messageContentRemovalTask.setRunning(false);
                                    }

                                    log.info("Stopping pub-sub message removal task");
                                    if (pubSubMessageContentRemoverTask != null && pubSubMessageContentRemoverTask.isRunning()) {
                                        pubSubMessageContentRemoverTask.setRunning(false);
                                    }
                                }
                                log.info("Waiting for Cassandra connection configured to become live...");

                                if(retriedCount <= 10) {
                                    Thread.sleep(6000);
                                }   else {
                                    if(retriedCount == 120) {
                                        retriedCount = 10;
                                    }
                                    Thread.sleep(500*retriedCount);
                                }


                            }
                        } catch (InterruptedException ex) {
                            //silently ignore
                        } catch (Exception ex) {
                            log.error("Error while checking if Cassandra Connection is alive.", ex);
                        }
                    } catch (InterruptedException e) {
                        //silently ignore
                    } catch (Exception e) {
                        log.error("Error while checking if Cassandra Connection is alive.", e);
                    }
                }
            }
        });
        cassandraConnectionCheckerThread.start();
    }

    @Override
    public void updateQueue(AMQQueue queue) throws AMQStoreException {

        if (!isCassandraConnectionLive) {
            log.error("Error in updating the queue. Message store is inaccessible.");
            return;
        }
        try {
            String owner = queue.getOwner() == null ? null : queue.getOwner().toString();
            String value = queue.getNameShortString().toString() + "|" + owner + "|" + (queue.isExclusive() ? "true" : "false");
            CassandraDataAccessHelper.addMappingToRaw(QUEUE_DETAILS_COLUMN_FAMILY, QUEUE_DETAILS_ROW,
                    queue.getNameShortString().toString(), value, keyspace);
        } catch (CassandraDataAccessException e) {
            throw new AMQStoreException("Error in updating the queue", e);
        }
    }

    @Override
    public void configureTransactionLog(String name, TransactionLogRecoveryHandler recoveryHandler,
                                        Configuration storeConfiguration, LogSubject logSubject) throws Exception {
    }

    @Override
    public Transaction newTransaction() {
        return new CassandraTransaction();
    }

    public boolean isConfigured() {
        return configured;
    }

    public class StoredCassandraMessage implements StoredMessage {

        private final long _messageId;
        private StorableMessageMetaData metaData;
        private String channelID;
        private String exchange;
        private ByteBuffer _content;

        private StoredCassandraMessage(long messageId, StorableMessageMetaData metaData) {
            this._messageId = messageId;
            this.metaData = metaData;
            this._content = ByteBuffer.allocate(metaData.getContentSize());
            //storeMetaData(_messageId, metaData);
        }

        private StoredCassandraMessage(long messageId, StorableMessageMetaData metaData, boolean isTopics) {
            this._messageId = messageId;
            this.metaData = metaData;
            this._content = ByteBuffer.allocate(metaData.getContentSize());
            if (isTopics) {
                this.exchange = "amq.topic";
            }
        }


        @Override
        public StorableMessageMetaData getMetaData() {
            if (metaData == null) {
                metaData = CassandraMessageStore.this.getMetaData(_messageId);
            }
            return metaData;
        }

        @Override
        public long getMessageNumber() {
            return _messageId;
        }

        @Override
        public void addContent(int offsetInMessage, ByteBuffer src) {
            if (isInMemoryMode && exchange.equalsIgnoreCase("amq.topic")) {
                src = src.duplicate();
                ByteBuffer dst = _content.duplicate();
                dst.position(offsetInMessage);
                dst.put(src);
            } else {
                addContentInPersistentMode(offsetInMessage, src);
            }
        }
        private void addContentInPersistentMode(final int offsetInMessage, final ByteBuffer src) {
            AndesExecuter.submit(new Runnable() {
                public void run() {
                    try {
                        CassandraMessageStore.this.addMessageContent(_messageId + "", offsetInMessage, src);
                    } catch (Throwable e) {
                        log.error("Error processing completed messages", e);

                        /**
                         * TODO close the session, have a find a way to get access to protocol session.
                         *  if (_session instanceof AMQProtocolEngine) {
                         ((AMQProtocolEngine) _session).closeProtocolSession();
                         }
                         */
                    }
                }
            }, channelID);
        }


        @Override
        public int getContent(int offsetInMessage, ByteBuffer dst) {
            int c;
            if (isInMemoryMode && exchange.equalsIgnoreCase("amq.topic")){
                  ByteBuffer src = _content.duplicate();
                src.position(offsetInMessage);
                src = src.slice();
                if (dst.remaining() < src.limit()) {
                    src.limit(dst.remaining());
                }
                dst.put(src);
                c = src.limit();
            }else {
               c =  CassandraMessageStore.this.getContent(_messageId + "", offsetInMessage, dst);
            }
            return c;
        }

        @Override
        public TransactionLog.StoreFuture flushToStore() {
            storeMetaData(_messageId, metaData);
            return IMMEDIATE_FUTURE;
        }

        public String getChannelID() {
            return channelID;
        }

        public void setChannelID(String channelID) {
            this.channelID = channelID;
        }

        @Override
        public void remove() {

//            if(ClusterResourceHolder.getInstance().getClusterConfiguration().isOnceInOrderSupportEnabled()){
//                return;
//            }
//            ColumnQuery<String, String, String> columnQuery =
//                    HFactory.createColumnQuery(keyspace, stringSerializer, stringSerializer ,
//                            stringSerializer);
//            columnQuery.setColumnFamily(MESSAGE_QUEUE_MAPPING_COLUMN_FAMILY).
//                    setKey(MESSAGE_QUEUE_MAPPING_ROW).setName("" + _messageId);
//            QueryResult<HColumn<String, String>> result = columnQuery.execute();
//
//            HColumn<String, String> rc = result.get();
//            if (rc != null) {
//                String qname = result.get().getValue();
//                try {
//                    CassandraMessageStore.this.removeMessageFromUserQueue(qname,_messageId);
//                } catch (AMQStoreException e) {
//                    log.error("Error remove message",e);
//                }
//                contentDeletionTasks.add(_messageId);
//            } else {
//                throw new RuntimeException("Can't remove message : message does not exist");
//            }


        }

        public void setExchange(String exchange) {
            this.exchange =exchange;
        }
    }

    private class CassandraTransaction implements Transaction {

        public void enqueueMessage(final TransactionLogResource queue, final Long messageId)
                throws AMQStoreException {

            try {
                AndesExecuter.submit(new Runnable() {
                    public void run() {

                        try {
                            Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
                            String name = queue.getResourceName();
                            LongSerializer ls = LongSerializer.get();
                            mutator.addInsertion(QUEUE_ENTRY_ROW, QUEUE_ENTRY_COLUMN_FAMILY,
                                    HFactory.createColumn(name, messageId, stringSerializer, ls));
                            mutator.execute();
                        } catch (Throwable e) {
                            log.error("Error adding Queue Entry ", e);
                        }

                    }
                }, null);
            } catch (Throwable e) {

                log.error("Error adding Queue Entry ", e);
                throw new AMQStoreException("Error adding Queue Entry "
                        + queue.getResourceName(), e);
            }
        }

        public void dequeueMessage(final TransactionLogResource queue, Long messageId) throws AMQStoreException {
            try {
                AndesExecuter.submit(new Runnable() {
                    public void run() {
                        String name = queue.getResourceName();
                        try {
                            CassandraDataAccessHelper.deleteStringColumnFromRaw(QUEUE_ENTRY_COLUMN_FAMILY, QUEUE_DETAILS_ROW, name,
                                    keyspace);
                        } catch (Throwable e) {
                            log.error("Error deleting Queue Entry", e);
                        }
                    }
                }, null);
            } catch (Throwable e) {
                log.error("Error deleting Queue Entry", e);
                throw new AMQStoreException("Error deleting Queue Entry :"
                        + queue.getResourceName(), e);
            }

        }

        public void commitTran() throws AMQStoreException {

        }

        public StoreFuture commitTranAsync() throws AMQStoreException {
            return new StoreFuture() {
                public boolean isComplete() {
                    return true;
                }

                public void waitForCompletion() {

                }
            };
        }

        public void abortTran() throws AMQStoreException {

        }
    }

    private class ContentRemoverTask implements Runnable {
        private int waitInterval = 5000;
        private long timeOutPerMessage = 60000; //10s
        private boolean running = true;

        public ContentRemoverTask(int waitInterval) {
            this.waitInterval = waitInterval;
        }

        public void run() {

            while (running) {
                try {

                    if (!contentDeletionTasks.isEmpty()) {
                        long currentTime = System.currentTimeMillis();

                        SortedMap<Long, Long> timedOutList = contentDeletionTasks.headMap(currentTime - timeOutPerMessage);

                        List<String> rows2Remove = new ArrayList<String>();
                        for (Long key : timedOutList.keySet()) {
                            rows2Remove.add(new StringBuffer("mid").append(key).toString());
                        }
                        CassandraDataAccessHelper.deleteIntegerColumnsFromRow(MESSAGE_CONTENT_COLUMN_FAMILY, rows2Remove, keyspace);

                        for (Long key : timedOutList.keySet()) {
                            contentDeletionTasks.remove(key);
                        }
                    }
                    try {
                        Thread.sleep(waitInterval);
                    } catch (InterruptedException e) {
                        log.error("Error while Executing content removal Task", e);
                    }
                } catch (Throwable e) {
                    log.error("Error while Executing content removal Task", e);
                }
            }
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }
    }

    private class InMemoryMessageRemoverTask implements Runnable {
        private int waitInterval = 5000;
        private long timeOutPerMessage = 5000; //10s
        private boolean running = true;

        public InMemoryMessageRemoverTask(int waitInterval) {
            this.waitInterval = waitInterval;
        }

        public void run() {

            while (running) {
                try {

                    if (!removalPendingMessageIds.isEmpty()) {
                        long currentTime = System.currentTimeMillis();
                        List<Long> readyToRemove = new ArrayList<Long>();

                        Enumeration<Long> messageIds = removalPendingMessageIds.keys();
                        while (messageIds.hasMoreElements()){
                           long mid = messageIds.nextElement();
                           if((currentTime - removalPendingMessageIds.get(mid)) > timeOutPerMessage){
                              readyToRemove.add(mid);
                           }
                        }

                        for(Long mid :readyToRemove){
                            removalPendingMessageIds.remove(mid);
                            incomingMessageHashtable.remove(mid);
                            alreadyAddedMessages.remove(mid);
                        }

                    }
                    try {
                        Thread.sleep(waitInterval);
                    } catch (InterruptedException e) {
                        log.error("Error while Executing content removal Task", e);
                    }
                } catch (Throwable e) {
                    log.error("Error while Executing content removal Task", e);
                }
            }
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }
    }



    /**
     * <code>PubSubMessageContentRemoverTask</code>
     * This task is used to remove message content from database when the message
     * published and acknowledged from client.
     * It checks the acknowledged message was delivered before a time difference of
     * CONTENT_REMOVAL_TIME_DEFFERENCE and it condition satisfies, it removes messages from
     * data store
     */
    private class PubSubMessageContentRemoverTask implements Runnable {


        private int waitInterval = 5000;

        private boolean running = true;

        public PubSubMessageContentRemoverTask(int waitInterval) {
            this.waitInterval = waitInterval;
        }

        public void run() {
            while (running) {
                try {
                    while (!pubSubMessageContentDeletionTasks.isEmpty()) {
                        Set<Long> messageIds = pubSubMessageContentDeletionTasks.keySet();
                        for (long messageID : messageIds) {
                            // If ready to remove , remove it from content table
                            if (CassandraMessageStore.this.isReadyAndRemovedMessageContent(messageID)) {
                                pubSubMessageContentDeletionTasks.remove(messageID);
                            }
                        }
                    }
                    try {
                        Thread.sleep(waitInterval);
                    } catch (InterruptedException e) {
                        log.error(e);
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean isRunning() {
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }
    }

    public void addContentDeletionTask(long messageId) {
        contentDeletionTasks.put(System.currentTimeMillis(), messageId);
    }


    public class PublishMessageWriter implements Runnable {


        private boolean start = false;

        private int writeCount = 20;

        private BlockingQueue<PublishMessageWriterMessage> messageQueue =
                new LinkedBlockingQueue<PublishMessageWriterMessage>();

        private List<PublishMessageWriterMessage> writtenMessages =
                new ArrayList<PublishMessageWriterMessage>();

        private HashMap<String,Long> messageCountForQueues = new HashMap<String, Long>();

        public PublishMessageWriter() {
            writeCount = ClusterResourceHolder.getInstance().getClusterConfiguration().
                    getMetadataPublisherMessageBatchSize();
        }

        @Override
        public void run() {
            Mutator<String> messageMutator = HFactory.createMutator(keyspace, stringSerializer);
            Mutator<String> mappingMutator = HFactory.createMutator(keyspace, stringSerializer);
            while (start) {


                int count = 0;

                PublishMessageWriterMessage msg = null;
                try {

                    msg = messageQueue.peek();

                    if (msg == null) {
                        /**
                         * If Queue is empty we flush all the current messages
                         * Notify all the waiting threads
                         * reset counters
                         */
                        long start = System.currentTimeMillis();
                        messageMutator.execute();
                        mappingMutator.execute();
                        updateCounters();
                        if (log.isDebugEnabled()) {
                            log.info("message Write, batch= " + count + " took " + (System.currentTimeMillis() - start) + "ms");
                        }
                        count = 0;
                        for (PublishMessageWriterMessage m : writtenMessages) {
                            m.release();
                        }
                        writtenMessages.clear();

                        msg = messageQueue.take();

                        // We need to add this message too
                        bufferMessageToCassandra(msg, messageMutator, mappingMutator);
                        count++;

                    } else {
                        //add to mutators
                        msg = messageQueue.take();
                        bufferMessageToCassandra(msg, messageMutator, mappingMutator);

                        count++;

                        if (count >= writeCount) {
                            messageMutator.execute();
                            mappingMutator.execute();
                            updateCounters();
                            count = 0;
                            for (PublishMessageWriterMessage m : writtenMessages) {
                                m.release();
                            }
                            writtenMessages.clear();
                        }
                    }

                } catch (InterruptedException e) {
                    log.error("Error while writing incoming messages", e);
                    continue;
                }


                if (log.isDebugEnabled()) {
                    log.debug("Adding Message with id " + msg.messageId + " to Queue " + msg.routingKey);
                }


            }

        }


        private void bufferMessageToCassandra(PublishMessageWriterMessage msg, Mutator<String> messageMutator,
                                              Mutator<String> mappingMutator) {
            ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();
            if (!isCassandraConnectionLive) {
                log.error("Error writing messages to global queue. Message Store is Inaccessible.");
                return;
            }
            try {
                long sTime = System.nanoTime();
                CassandraDataAccessHelper.addMessageToQueue(CassandraMessageStore.GLOBAL_QUEUES_COLUMN_FAMILY,
                        msg.globalQueueName, msg.messageId, msg.message, messageMutator, false);

                CassandraDataAccessHelper.addMappingToRaw(CassandraMessageStore.GLOBAL_QUEUE_LIST_COLUMN_FAMILY,
                        CassandraMessageStore.GLOBAL_QUEUE_LIST_ROW, msg.globalQueueName,
                        msg.globalQueueName, mappingMutator, false);
                writtenMessages.add(msg);

                long eTime = System.nanoTime();
                DataCollector.write(DataCollector.PUBLISHER_WRITE_LATENCY, (eTime - sTime));
                DataCollector.flush();

                //we need to do this only for messages addressed for queues
                if(!msg.isDestinationQueueBoundToTopicExchange) {
                    if(!messageCountForQueues.containsKey(msg.routingKey)) {
                        String queueName = msg.routingKey;
                        if(msg.isNewMessage) {
                            addMessageCounterForQueue(queueName);
                            messageCountForQueues.put(msg.routingKey,1L);
                        }
                    } else {
                        if(msg.isNewMessage) {
                            messageCountForQueues.put(msg.routingKey,messageCountForQueues.get(msg.routingKey) + 1);
                        }
                    }
                }
                // clusterManager.handleQueueAddition(msg.queue);

            } catch (Exception e) {
                log.error("Error in adding message to global queue", e);
            }
        }

        public void addMessage(String globalQueueName, String routingKey, long messageId, byte[] message,
                               boolean isNewMessage, boolean isDestinationQueueBoundToTopicExchange) {
            try {
                PublishMessageWriterMessage msg = new PublishMessageWriterMessage(globalQueueName, routingKey,
                        messageId, message, isNewMessage, isDestinationQueueBoundToTopicExchange);
                messageQueue.add(msg);
//                msg.waitForToBeWritten();

            } catch (InterruptedException e) {
                throw new RuntimeException("Error while adding Incomming message", e);
            }
        }

        private void updateCounters() {
            for(String queue : messageCountForQueues.keySet()) {
                incrementQueueCount(queue,messageCountForQueues.get(queue));
            }
            messageCountForQueues.clear();
        }

        public void start() {
            start = true;
        }

        public void stop() {
            start = false;
        }


        private class PublishMessageWriterMessage {
            private Semaphore messageCallBack;

            private String globalQueueName;
            private boolean isNewMessage;
            private boolean isDestinationQueueBoundToTopicExchange;
            private String routingKey;
            private long messageId;
            private byte[] message;

            public PublishMessageWriterMessage(String globalQueueName, String routingKey, long messageId, byte[] message,
                                               boolean isNewMessage, boolean isDestinationQueueBoundToTopicExchange) throws InterruptedException {
                this.globalQueueName = globalQueueName;
                this.messageId = messageId;
                this.message = message;
                this.routingKey = routingKey;
                this.isNewMessage = isNewMessage;
                this.isDestinationQueueBoundToTopicExchange = isDestinationQueueBoundToTopicExchange;
                this.messageCallBack = new Semaphore(1);
                messageCallBack.acquire();
            }

            public void release() {
                messageCallBack.release();
            }

            public void waitForToBeWritten() throws InterruptedException {
//                messageCallBack.acquire();
            }

        }
    }

    public class PublishMessageContentWriter implements Runnable {


        private boolean start = false;

        private int writeCount = 1;

        private BlockingQueue<PublishMessageContentWriterMessage> messageQueue =
                new LinkedBlockingQueue<PublishMessageContentWriterMessage>();

        private List<PublishMessageContentWriterMessage> writtenMessages =
                new ArrayList<PublishMessageContentWriterMessage>();

        public PublishMessageContentWriter() {
            writeCount = ClusterResourceHolder.getInstance().getClusterConfiguration().
                    getContentPublisherMessageBatchSize();
        }

        @Override
        public void run() {
            Mutator<String> messageMutator = HFactory.createMutator(keyspace, stringSerializer);
            while (start) {

                int count = 0;

                PublishMessageContentWriterMessage msg = null;
                try {

                    msg = messageQueue.peek();

                    if (msg == null) {
                        /**
                         * If Queue is empty we flush all the current messages
                         * Notify all the waiting threads
                         * reset counters
                         */
                        messageMutator.execute();
                        count = 0;
                        for (PublishMessageContentWriterMessage m : writtenMessages) {
                            m.release();
                        }
                        writtenMessages.clear();

                        msg = messageQueue.take();

                        // We need to add this message too
                        bufferMessageToCassandra(msg, messageMutator);
                        count++;

                    } else {
                        //add to mutators
                        msg = messageQueue.take();

                        bufferMessageToCassandra(msg, messageMutator);

                        count++;

                        if (count >= writeCount) {
                            messageMutator.execute();
                            count = 0;
                            for (PublishMessageContentWriterMessage m : writtenMessages) {
                                m.release();
                            }
                            writtenMessages.clear();
                        }
                    }

                } catch (InterruptedException e) {
                    log.error("Error while writing incoming messages content", e);
                    continue;
                }


            }

        }


        private void bufferMessageToCassandra(PublishMessageContentWriterMessage msg, Mutator<String> messageMutator
        ) {


            try {
                long sTime = System.currentTimeMillis();
                CassandraDataAccessHelper.addIntegerByteArrayContentToRaw(MESSAGE_CONTENT_COLUMN_FAMILY, msg.rowKey,
                        msg.offset, msg.message, messageMutator, false);

                writtenMessages.add(msg);
                long eTime = System.currentTimeMillis();
                PerformanceCounter.recordCassandraWrite(eTime - sTime);
            } catch (Exception e) {
                log.error("Error in adding message to global queue", e);
            }
        }

        public void addMessage(final String rowKey, int offset, byte[] message) {
            try {
                final PublishMessageContentWriterMessage msg =
                        new PublishMessageContentWriterMessage(rowKey, offset, message);
                //messageQueue.add(msg);
//                msg.waitForToBeWritten();

                //submit to to the same work queue, so this will happen before. TODO fix buffering
                if (isCassandraConnectionLive) {

                    AndesExecuter.submit(new Runnable() {
                        public void run() {
                            try {
                                long start = System.currentTimeMillis();
                                Mutator<String> messageMutator = HFactory.createMutator(keyspace, stringSerializer);
                                CassandraDataAccessHelper.addIntegerByteArrayContentToRaw(MESSAGE_CONTENT_COLUMN_FAMILY, msg.rowKey,
                                        msg.offset, msg.message, messageMutator, false);
                                messageMutator.execute();
                                PerformanceCounter.recordCassandraWrite(System.currentTimeMillis() - start);
                            } catch (Throwable e) {
                                log.error(e);
                            }
                        }
                    }, null);
                } else {
                    log.error("Error while adding incoming message. Message Store is Inaccessible.");
                }

            } catch (InterruptedException e) {
                throw new RuntimeException("Error while adding Incoming message", e);
            }
        }

        public void start() {
            start = true;
        }

        public void stop() {
            start = false;
        }


        private class PublishMessageContentWriterMessage {
//            private Semaphore messageCallBack;

            private String rowKey;
            private int offset;
            private byte[] message;

            public PublishMessageContentWriterMessage(String rowKey, int offset, byte[] message)
                    throws InterruptedException {
                this.rowKey = rowKey;
                this.offset = offset;
                this.message = message;
//                this.messageCallBack = new Semaphore(1);
//                messageCallBack.acquire();
            }

            public void release() {
//                messageCallBack.release();
            }

            public void waitForToBeWritten() throws InterruptedException {
//                messageCallBack.acquire();
            }

        }
    }

}

