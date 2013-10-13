package org.wso2.andes.server.cassandra;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.server.AMQChannel;
import org.wso2.andes.server.ClusterResourceHolder;


public class OnflightMessageTracker {
    private static Log log = LogFactory.getLog(OnflightMessageTracker.class);

    private int acktimeout = 10000;
    private LinkedHashMap<Long,MsgData> msgId2MsgData = new LinkedHashMap<Long,MsgData>(); 
    private Map<String,Long> deliveryTag2MsgID = new HashMap<String,Long>();
    
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    public class MsgData{
        final long msgID; 
        boolean ackreceived = false;
        final String queue; 
        final long timestamp; 
        final String deliveryID; 
        final AMQChannel channel; 
        public MsgData(long msgID, boolean ackreceived, String queue, long timestamp, String deliveryID, AMQChannel channel) {
            this.msgID = msgID;
            this.ackreceived = ackreceived;
            this.queue = queue; 
            this.timestamp = timestamp;
            this.deliveryID = deliveryID;
            this.channel = channel;
        }
    }
    
    private static OnflightMessageTracker instance = new OnflightMessageTracker();
    public static OnflightMessageTracker getInstance(){
        return instance; 
    }

    
    private OnflightMessageTracker(){
        /*
         * for all add and remove, following is executed, and it will remove the oldest entry if needed
         */
        msgId2MsgData = new LinkedHashMap<Long, MsgData>() {
            private static final long serialVersionUID = -8681132571102532817L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, MsgData> eldest) {
                MsgData msgData = eldest.getValue(); 
                boolean todelete = (System.currentTimeMillis() - msgData.timestamp) > (acktimeout*10);
                if(todelete){
                    if(!msgData.ackreceived){
                        //reduce messages on flight on this channel
                        msgData.channel.decrementNonAckedMessageCount();
                        log.debug("No ack received for delivery tag " + msgData.deliveryID + " and message id "+ msgData.msgID); 
                        //TODO notify the QueueDeliveryWorker to resend (it work now as well as flusher loops around, but this will be faster)
                    }
                    if(deliveryTag2MsgID.remove(msgData.deliveryID) == null){
                        log.error("Cannot find delivery tag " + msgData.deliveryID + " and message id "+ msgData.msgID);
                    }
                }
                return todelete;
            }
        };
        
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                //TODO replace this with Gvava Cache if possible
                synchronized (this) {
                    long currentTime = System.currentTimeMillis(); 
                    Iterator<MsgData> iterator = msgId2MsgData.values().iterator();
                    while(iterator.hasNext()){
                        MsgData mdata = iterator.next();
                        if((currentTime - mdata.timestamp) > acktimeout){
                            iterator.remove();
                            deliveryTag2MsgID.remove(mdata.deliveryID);
                            mdata.channel.decrementNonAckedMessageCount();
                        }
                    }
                }
            }
        },  5, 10, TimeUnit.SECONDS);
    }
    
    
    
    public synchronized boolean testMessage(long messageId){
        long currentTime = System.currentTimeMillis();
        MsgData mdata = msgId2MsgData.get(messageId); 
                
        if (mdata == null || (!mdata.ackreceived && (currentTime - mdata.timestamp) > acktimeout)) {
            if(mdata != null){
                msgId2MsgData.remove(messageId);
                deliveryTag2MsgID.remove(mdata.deliveryID);
                mdata.channel.decrementNonAckedMessageCount();
            }
            return true; 
        }else{
            return false;
        }
    }
    
    /** 
     * This cleanup the current message ID form tracking. Useful for undo changes in case of a failure
     * @param deliveryTag
     * @param messageId
     * @param channel
     */
    public void removeMessage(AMQChannel channel, long deliveryTag, long messageId){
        String deliveryID = new StringBuffer(channel.getId().toString()).append("/").append(deliveryTag).toString(); 
        Long messageIDStored = deliveryTag2MsgID.remove(deliveryID);
        
        if(messageIDStored != null && messageIDStored.longValue() != messageId){
            throw new RuntimeException("Delivery Tag "+deliveryID+ " reused for " +messageId + " and " + messageIDStored +" , this should not happen");
        }
        msgId2MsgData.remove(messageId);
    }
    
    public synchronized boolean testAndAddMessage(AMQChannel channel, long deliveryTag, long messageId, String queue){
        String deliveryID = new StringBuffer(channel.getId().toString()).append("/").append(deliveryTag).toString(); 
        long currentTime = System.currentTimeMillis();
        MsgData mdata = msgId2MsgData.get(messageId); 
        
//        if(deliveryTag2MsgID.size() != msgId2MsgData.size()){
//            log.error("Two maps are out of sync "+ deliveryTag2MsgID.size() + "!=" + msgId2MsgData.size());
//        }
                
        if (mdata == null || (!mdata.ackreceived && (currentTime - mdata.timestamp) > acktimeout)) {
            if (deliveryTag2MsgID.containsKey(deliveryID)) {
                throw new RuntimeException("Delivery Tag "+deliveryID+" reused, this should not happen");
            }
            if (mdata != null) {
                // message has sent once, we will clean that up
                deliveryTag2MsgID.remove(mdata.deliveryID); 
                msgId2MsgData.remove(messageId); 
                mdata.channel.decrementNonAckedMessageCount();
            }
            deliveryTag2MsgID.put(deliveryID, messageId);
            msgId2MsgData.put(new Long(messageId), new MsgData(messageId, false, queue, currentTime, deliveryID, channel));
            return true;
        } else {
            return false;
        }
    }

    private void decrementCassandraMsgCount(MsgData msgData) {
        String queueNameFmMsgData = msgData.queue;
        String subscriptionQueueName = queueNameFmMsgData.substring(0,queueNameFmMsgData.lastIndexOf("_"));
        ClusterResourceHolder.getInstance().getCassandraMessageStore().decrementQueueCount(subscriptionQueueName,1L);
    }

    public synchronized MsgData ackReceived(AMQChannel channel, long deliveryTag){
        String deliveryID = new StringBuffer(channel.getId().toString()).append("/").append(deliveryTag).toString(); 
        Long messageid = deliveryTag2MsgID.get(deliveryID); 
        if(messageid != null){
            MsgData msgData = msgId2MsgData.get(messageid);
            if(msgData != null){
                msgData.ackreceived = true;
                //TODO we have to revisit the topics case
                channel.decrementNonAckedMessageCount();
                //decrement message count from relevant queue at Message Store
                decrementCassandraMsgCount(msgData);
                return msgData;
            }else{
                throw new RuntimeException("No message data found for messageid "+ messageid); 
            }
        }else{
            //TODO We ignore as this happens only with publish case. May be there is a better way to handle this 
            return null; 
        }
    }
    
    public MsgData getMsgData(long deliveryTag){
        return msgId2MsgData.get(deliveryTag);
    }
}
