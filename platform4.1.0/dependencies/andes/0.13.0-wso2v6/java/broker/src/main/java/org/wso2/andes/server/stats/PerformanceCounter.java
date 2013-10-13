package org.wso2.andes.server.stats;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

public class PerformanceCounter {
    private static final Logger log = Logger.getLogger(PerformanceCounter.class);
    private static final int MSG_BUFFER_SIZE = 1000; 
    
    private  static AtomicLong totMessagesReceived = new AtomicLong();
    private  static AtomicLong totmessagesDelivered = new AtomicLong();
    
    private static AtomicLong queueDeliveryCount =  new AtomicLong();
    private static AtomicLong queueReceiveCount =  new AtomicLong();
    private static AtomicLong queueGlobalQueueMoveCount =  new AtomicLong();
    
    
    private static long lastEmitTs = System.currentTimeMillis();

    
    static{
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Thread.sleep(30000);
                        
                    } catch (InterruptedException e) {
                    }
                    
                    long timeTookSinceEmit =  System.currentTimeMillis() - lastEmitTs; 
                    float deliverythroughput = queueDeliveryCount.get()*1000/timeTookSinceEmit; 
                    queueDeliveryCount.set(0);
                    
                    float receivethroughput = queueReceiveCount.get()*1000/timeTookSinceEmit; 
                    queueReceiveCount.set(0);
                    
                    float globalQueueMovethroughput = queueGlobalQueueMoveCount.get()*1000/timeTookSinceEmit; 
                    queueGlobalQueueMoveCount.set(0);


                    if(queueReceiveCount.get() > 0){
                        log.info("PerfCount: summary ["+new Date()+"] deliveryThoughput = "+deliverythroughput +", receiveThoughput="+ receivethroughput 
                                +", qquaueMoveT="+globalQueueMovethroughput+" delivered=" + totmessagesDelivered + ", received " + totMessagesReceived );                    
                    }
                    lastEmitTs = System.currentTimeMillis(); 
                }
            }
        }).start();
    }


    public static class QueuePerfData{
        AtomicInteger messagesReceived = new AtomicInteger();
        AtomicInteger messagesDelivered = new AtomicInteger(); 
        
        AtomicInteger[] msgSizes = new AtomicInteger[10];
        
        public QueuePerfData(){
            for(int i = 0;i< msgSizes.length;i++){
                msgSizes[i] = new AtomicInteger();
            }
        }
    }
    
    
    private static ConcurrentHashMap< String, QueuePerfData> perfMap = new ConcurrentHashMap<String, PerformanceCounter.QueuePerfData>(); 
    
    public static void recordMessageReceived(String qName, long sizeInChuncks){
        totMessagesReceived.incrementAndGet(); 
        QueuePerfData perfData = perfMap.get(qName); 
        
        synchronized (perfMap) {
            if(perfData == null){
                perfData = new QueuePerfData(); 
                perfMap.put(qName, perfData); 
            }
        }
        int count = perfData.messagesReceived.incrementAndGet();
        if(count%MSG_BUFFER_SIZE == 0){
            log.info("PerfCount:" + qName + ":" + perfData.messagesDelivered + "/" + perfData.messagesReceived + " "+ Arrays.toString(perfData.msgSizes) + ", tot=" + totmessagesDelivered + "/" + totMessagesReceived );
        }
        
        int index = (int)Math.min(Math.ceil(Math.log10(sizeInChuncks)),10); 
        perfData.msgSizes[index].incrementAndGet(); 
        
        queueReceiveCount.incrementAndGet();
    }
    
    public static void recordMessageDelivered(String qName){
        totmessagesDelivered.incrementAndGet();
        QueuePerfData perfData = perfMap.get(qName); 
        
        synchronized (perfMap) {
            if(perfData == null){
                perfData = new QueuePerfData(); 
                perfMap.put(qName, perfData); 
            }
        }
        int count = perfData.messagesDelivered.incrementAndGet();
        if(count%MSG_BUFFER_SIZE == 0){
            log.info("PerfCount" + qName + ":" + perfData.messagesDelivered + "/" + perfData.messagesReceived + " "+ Arrays.toString(perfData.msgSizes) + ", tot=" + totmessagesDelivered + "/" + totMessagesReceived );
        }
        
        queueDeliveryCount.incrementAndGet();

    }
    
    public static void recordGlobalQueueMsgMove(int messagesMoved){
        queueGlobalQueueMoveCount.addAndGet(messagesMoved);
    }

    public static void recordCassandraWrite(long timeTookInMillis){
        
    }
    
    public static void recordCassandraRead(long timeTookInMillis){
    }
    
}
