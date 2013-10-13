package org.wso2.andes.server.cassandra;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.server.AMQChannel;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.configuration.ClusterConfiguration;
import org.wso2.andes.server.message.AMQMessage;
import org.wso2.andes.server.protocol.AMQProtocolSession;
import org.wso2.andes.server.queue.AMQQueue;
import org.wso2.andes.server.queue.QueueEntry;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.subscription.Subscription;
import org.wso2.andes.server.subscription.SubscriptionImpl;
import org.wso2.andes.server.util.AndesUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <code>QueueDeliveryWorker</code> Handles the task of polling the user queues and flushing
 * the messages to subscribers
 * There will be one Flusher per Queue Per Node
 */
public class QueueDeliveryWorker extends Thread{
    private AMQQueue queue;
    private String nodeQueue;
    private boolean running = true;
    private static Log log = LogFactory.getLog(QueueDeliveryWorker.class);

    private int messageCountToRead = 50;
    private int maxMessageCountToRead = 300;
    private int minMessageCountToRead = 20;
    

    private int maxNumberOfUnAckedMessages = 20000;
    
    private long lastProcessedId = 0;

    private int resetCounter;

    private int maxRestCounter = 50;
    
    private long totMsgSent = 0;
    private long totMsgRead = 0;
    
    private long lastRestTime = 0;

    private SequentialThreadPoolExecutor executor;
    
    private int queueWorkerWaitInterval;

    private int queueMsgDeliveryCurserResetTimeInterval;
    
    private OnflightMessageTracker onflightMessageTracker; 
    
    
    private long iterations = 0; 
    private int workqueueSize = 0; 

    private long failureCount = 0;  
    
    private Map<String,Map<String,CassandraSubscription>> subscriptionMap =
        new ConcurrentHashMap<String,Map<String,CassandraSubscription>>();
    
    private int totalReadButUndeliveredMessages = 0;
    
    public class QueueDeliveryInfo{
        String queueName; 
        Iterator<CassandraSubscription> iterator; 
        List<QueueEntry> readButUndeliveredMessages = new ArrayList<QueueEntry>(); 
        boolean messageIgnored = false; 
    }
    
    private Map<String, QueueDeliveryInfo> subscriptionCursar4QueueMap = new HashMap<String, QueueDeliveryInfo>();
    
    /**
     * Get the next subscription for the given queue. If at end of the subscriptions, it circles around to the first one
     * @param queueName
     * @return
     */
    public CassandraSubscription findNextSubscriptionToSent(String queueName){
        Map<String, CassandraSubscription> subscriptions = subscriptionMap.get(queueName);
        if(subscriptions == null || subscriptions.size() == 0){
            subscriptionCursar4QueueMap.remove(queueName);
            return null; 
        }
        
        QueueDeliveryInfo queueDeliveryInfo = getQueueDeliveryInfo(queueName);
        Iterator<CassandraSubscription> it = queueDeliveryInfo.iterator;
        if(it.hasNext()){
            return it.next();
        }else{
            it = subscriptions.values().iterator();
            queueDeliveryInfo.iterator = it;
            if(it.hasNext()){
                return it.next();
            }else{
                return null;
            }
        }
    }



    private QueueDeliveryInfo getQueueDeliveryInfo(String queueName) {
        QueueDeliveryInfo queueDeliveryInfo = subscriptionCursar4QueueMap.get(queueName);
        if(queueDeliveryInfo == null){
            queueDeliveryInfo = new QueueDeliveryInfo();
            queueDeliveryInfo.queueName = queueName;
            Map<String, CassandraSubscription> subscriptions = subscriptionMap.get(queueName);
            queueDeliveryInfo.iterator = subscriptions.values().iterator(); 
            subscriptionCursar4QueueMap.put(queueName,queueDeliveryInfo);
        }
        return queueDeliveryInfo;
    }

    
    
    public QueueDeliveryWorker(String nodeQueue, AMQQueue queue, Map<String, Map<String, CassandraSubscription>> subscriptionMap,
                               SequentialThreadPoolExecutor executorService, int queueWorkerWaitInterval) {

        //this.cassandraSubscriptions = cassandraSubscriptions;
        this.queue = queue;
        this.nodeQueue = nodeQueue;
        this.executor = executorService;

        ClusterConfiguration clusterConfiguration = ClusterResourceHolder.getInstance().getClusterConfiguration();
        this.messageCountToRead = clusterConfiguration.getMessageBatchSizeForSubscribers();
        this.maxMessageCountToRead = clusterConfiguration.getMaxMessageBatchSizeForSubscribers();
        this.minMessageCountToRead = clusterConfiguration.getMinMessageBatchSizeForSubscribers();
        this.maxNumberOfUnAckedMessages = clusterConfiguration.getMaxNumberOfUnackedMessages();
        this.queueMsgDeliveryCurserResetTimeInterval = clusterConfiguration.getQueueMsgDeliveryCurserResetTimeInterval();
        this.queueWorkerWaitInterval = queueWorkerWaitInterval;
        this.subscriptionMap =  subscriptionMap; 
        onflightMessageTracker = OnflightMessageTracker.getInstance(); 

        log.info("Queue worker started for queue: "+ queue.getResourceName() + " with on flight message checks");
        
    }

    @Override
    public void run() {
        iterations = 0; 
        workqueueSize = 0; 
        lastRestTime = System.currentTimeMillis();
        failureCount = 0;  
        
        while (running) {
            try {
                /**
                 *    Following check is to avoid the worker queue been full with too many pending tasks.
                 *    those pending tasks are best left in Cassandra until we have some breathing room 
                 */
                workqueueSize = executor.getSize();

                if(workqueueSize > 1000){
                    if(workqueueSize > 5000){
                        log.error("Flusher queue is growing, and this should not happen. Please check cassandra Flusher"); 
                    }
                    log.info("skipping content cassandra reading thread as flusher queue has "+ workqueueSize + " tasks");
                    sleep4waitInterval();
                    continue; 
                }
                
                resetOffsetAtCassadraQueueIfNeeded(false);
                
                CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
                /**
                 * Following reads from cassandara, it reads only if there are not enough messages loaded in memory
                 */
                int msgReadThisTime = 0; 
                if(totalReadButUndeliveredMessages < 10000){
                    List<QueueEntry> messagesFromCassansdra = messageStore.getMessagesFromUserQueue(nodeQueue,queue, messageCountToRead,lastProcessedId);
                    for(QueueEntry message: messagesFromCassansdra){
                        String queueName = ((AMQMessage)message.getMessage()).getMessageMetaData().getMessagePublishInfo().getRoutingKey().toString();
                        QueueDeliveryInfo queueDeliveryInfo = getQueueDeliveryInfo(queueName); 
                        if(!queueDeliveryInfo.messageIgnored){ 
                            if(queueDeliveryInfo.readButUndeliveredMessages.size() < 5000){
                                queueDeliveryInfo.readButUndeliveredMessages.add(message);
                                totalReadButUndeliveredMessages++;
                                lastProcessedId = message.getMessage().getMessageNumber();
                            }else{
                                queueDeliveryInfo.messageIgnored = true; 
                            }
                        }
                    }
                    
                    if(messagesFromCassansdra.size() == 0) {
                        sleep4waitInterval();
                    }
                    
                    //If we have read all messages we asked for, we increase the reading count. Else we reduce it. 
                    //TODO we might want to take into account the size of the message while we change the batch size
                    if(messagesFromCassansdra.size() == messageCountToRead) {
                        messageCountToRead += 100;
                        if(messageCountToRead > maxMessageCountToRead){
                            messageCountToRead = maxMessageCountToRead;
                        }
                    } else {
                        messageCountToRead -= 50;
                        if(messageCountToRead < minMessageCountToRead) {
                            messageCountToRead = minMessageCountToRead;
                        }
                    }
                    totMsgRead = totMsgRead + messagesFromCassansdra.size(); 
                    msgReadThisTime = messagesFromCassansdra.size(); 
                 }
                
                //Then we schedule them to be sent to subcribers
                int sentMessageCount = 0; 
                for(QueueDeliveryInfo queueDeliveryInfo:subscriptionCursar4QueueMap.values()) {
                    sentMessageCount = sendMessagesToSubscriptions(queueDeliveryInfo.queueName, queueDeliveryInfo.readButUndeliveredMessages);
                }
                
                if(iterations%20 == 0){
                    log.info("[Flusher"+this+"]readNow="+ msgReadThisTime + " totRead="+ totMsgRead+ " totprocessed= "+ totMsgSent + ", totalReadButNotSent="+ 
                            totalReadButUndeliveredMessages+". workQueue= "+ workqueueSize  + " lastID="+ lastProcessedId);
                }
                iterations++;
                //on every 10th, we sleep a bit to give cassandra a break, we do the same if we have not sent any messages
                if(sentMessageCount == 0 || iterations%10 == 0){
                    sleep4waitInterval();
                }
                failureCount = 0;
            } catch (Throwable e) {
                /**
                 * When there is a error, we will wait to avoid looping.  
                 */
                long waitTime = queueWorkerWaitInterval;
                failureCount++;
                long faultWaitTime = Math.max(waitTime*5, failureCount*waitTime);
                try {
                    Thread.sleep(faultWaitTime);
                } catch (InterruptedException e1) {}
                log.error("Error running Cassandra Message Flusher"+ e.getMessage(), e); 
            }
        }
    }



    private void sleep4waitInterval() {
        try {
            Thread.sleep(queueWorkerWaitInterval);
        } catch (InterruptedException ignored) {}
    }

    private boolean isThisSubscriptionHasRoom(CassandraSubscription cassandraSubscription){
        AMQChannel channel = null;  
        if(cassandraSubscription.getSubscription() instanceof SubscriptionImpl.AckSubscription){
            channel = ((SubscriptionImpl.AckSubscription)cassandraSubscription.getSubscription()).getChannel();
        }
        //is that queue has too many messages pending
        int notAckedMsgCount = channel.getNotAckedMessageCount(); 
        
        //Here we ignore messages that has been scheduled but not execuated, so it might send few messages than maxNumberOfUnAckedMessages
        if(notAckedMsgCount < maxNumberOfUnAckedMessages){
            return true;
        }else{
            log.info("Not selected, channel=" +queue.getName() + "/"+ channel + " pending count ="  + (notAckedMsgCount + workqueueSize));

            if(log.isDebugEnabled()){
                log.debug("Not selected, channel=" +queue.getName() + "/"+ channel + " pending count ="  + (notAckedMsgCount + workqueueSize));
            }
            return false; 
        }
    }
    
    
    public int sendMessagesToSubscriptions(String targetQueue, List<QueueEntry> messages){
        int sentMessageCount = 0;
        Iterator<QueueEntry> iterator = messages.iterator();
        while (iterator.hasNext()) {
            QueueEntry message = iterator.next();
            boolean messageSent = false;
            Map<String, CassandraSubscription> subscriptions4Queue = subscriptionMap.get(targetQueue);
            if(subscriptions4Queue != null){
                /*
                 * we do this in a for loop to avoid iterating for a subscriptions for ever. We only iterate as 
                 * once for each subscription 
                 */
                for(int j =0;j< subscriptions4Queue.size();j++){
                    CassandraSubscription cassandraSubscription = findNextSubscriptionToSent(targetQueue);
                    if(isThisSubscriptionHasRoom(cassandraSubscription)){
                        AMQProtocolSession session = cassandraSubscription.getSession();

                        ((AMQMessage) message.getMessage()).setClientIdentifier(session);
                        
                        if(log.isDebugEnabled()){
                            log.debug("readFromCassandra"+ AndesUtils.printAMQMessage(message)); 
                        }
                        deliverAsynchronously(cassandraSubscription.getSubscription(), message);
                        totMsgSent++;
                        sentMessageCount++;
                        totalReadButUndeliveredMessages--;
                        messageSent = true; 
                        iterator.remove();
                        break; 
                    }
                }
                if(!messageSent){
                    log.debug("All subscriptions for queue "+ targetQueue + " have max Unacked messages "+ queue.getName());
                }
            }else{
                //All subscriptions deleted for the queue, should we move messages back to global queue?
            }
        }
        return sentMessageCount; 
    }

    public AMQQueue getQueue() {
        return queue;
    }



    private void deliverAsynchronously(final Subscription subscription , final QueueEntry message) {
        if(onflightMessageTracker.testMessage(message.getMessage().getMessageNumber())){
            AMQChannel channel = null;  
            if(subscription instanceof SubscriptionImpl.AckSubscription){
                channel = ((SubscriptionImpl.AckSubscription)subscription).getChannel();
            }
            channel.incrementNonAckedMessageCount();
            if(log.isDebugEnabled()){
                log.debug("sent out message for channel id="+ channel +  " "+ queue.getName()); 
            }
            
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (subscription instanceof SubscriptionImpl.AckSubscription) {
                            subscription.send(message);
                        } else {
                            log.error("Unexpected Subscription Implementation : " +
                                    subscription !=null?subscription.getClass().getName():null);
                        }
                    } catch (Throwable e) {
                         log.error("Error while delivering message " ,e);
                    }
                }
            };
            executor.submit(r,subscription.getSubscriptionID());
        }
    }


    public void stopFlusher() {
        running = false;
        log.debug("Shutting down the message flusher for the queue "+ queue.getName());
    }

    public void startFlusher() {
        log.debug("staring flusher for "+ queue.getName());
        running = true;
    }


    private  boolean resetOffsetAtCassadraQueueIfNeeded(boolean force) {
        resetCounter++; 
        if((resetCounter > maxRestCounter && (System.currentTimeMillis() - lastRestTime) > queueMsgDeliveryCurserResetTimeInterval)) {
            resetCounter = 0;
            lastRestTime = System.currentTimeMillis();
            
            lastProcessedId = 0;
            for(QueueDeliveryInfo deliveryInfo:subscriptionCursar4QueueMap.values()){
                deliveryInfo.messageIgnored = false;
                deliveryInfo.readButUndeliveredMessages.clear();
            }
            totalReadButUndeliveredMessages = 0;
            if(log.isDebugEnabled()){
                log.debug("Reset the next message ID to read for cassandra flusher");
            }
            return true;
        }
        return false;
    }
}
