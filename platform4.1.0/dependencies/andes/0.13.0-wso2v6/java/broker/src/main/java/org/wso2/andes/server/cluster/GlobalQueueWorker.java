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
package org.wso2.andes.server.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.cassandra.CassandraQueueMessage;
import org.wso2.andes.server.stats.PerformanceCounter;
import org.wso2.andes.server.store.CassandraMessageStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * <code>GlobalQueueWorker</code> is responsible for polling global queues and
 * distribute messages to the subscriber userQueues.
 */
public class GlobalQueueWorker implements Runnable {

    private static Log log = LogFactory.getLog(GlobalQueueWorker.class);

    private String globalQueueName;
    private boolean running;
    private int messageCountToReadFromCasssandra;
    private CassandraMessageStore cassandraMessageStore;
    private long totMsgMoved = 0;

    public GlobalQueueWorker(String queueName, CassandraMessageStore cassandraMessageStore,
            int messageCountToReadFromCasssandra) {
        this.cassandraMessageStore = cassandraMessageStore;
        this.globalQueueName = queueName;
        this.messageCountToReadFromCasssandra = messageCountToReadFromCasssandra;
    }

    public void run() {
        long pollingLoopCount = 0;
        int queueWorkerWaitTime = ClusterResourceHolder.getInstance().getClusterConfiguration()
                .getQueueWorkerInterval();

        List<String> nodeQueues = null;
        try {
            nodeQueues = cassandraMessageStore.getUserQueues(globalQueueName);
        } catch (Exception e1) {
            log.error(e1.getMessage(), e1);
        }
        while (running) {
            try {
                /**
                 * Steps
                 * 
                 * 1)Poll Global queue and get chunk of messages 2) Put messages
                 * one by one to user queues and delete them
                 */
                Queue<CassandraQueueMessage> cassandraMessages = cassandraMessageStore.getMessagesFromGlobalQueue(
                        globalQueueName, messageCountToReadFromCasssandra);
                int size = cassandraMessages.size();
                PerformanceCounter.recordGlobalQueueMsgMove(size);

                //We only poll cassandra onces for eveny 1/10th of runs. TODO can we get this broker broker
                if (pollingLoopCount % 10 == 0) {
                   nodeQueues = cassandraMessageStore.getUserQueues(globalQueueName);
                }

                if (nodeQueues != null && nodeQueues.size() > 0) {
                    List<Long> addedMsgs = new ArrayList<Long>();

                    for (int i = 0; i < size; i++) {
                        CassandraQueueMessage msg = cassandraMessages.poll();

                        int index = i % nodeQueues.size();
                        String s = nodeQueues.get(index);

                        //messages are stamped to which userQueue they should be transferred to.
                        //to be fair we divide messages across all user queues registered for the qlobal queue.
                        msg.setNodeQueue(s);

                        addedMsgs.add(msg.getMessageId());
                        cassandraMessages.add(msg);
                        if (log.isDebugEnabled()) {
                            log.debug("global worker moved " + msg.getMessageId() + " to subscription " + s);
                        }
                    }

                    cassandraMessageStore.transferMessageBatchFromGlobalQueueToUserQueue(
                            cassandraMessages.toArray(new CassandraQueueMessage[cassandraMessages.size()]),
                            globalQueueName);

                    if (size == 0 || pollingLoopCount % 10 == 0) {
                        try {
                            pollingLoopCount = 0;
                            Thread.sleep(queueWorkerWaitTime);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    } else {
                        totMsgMoved = totMsgMoved + cassandraMessages.size();
                        if(log.isDebugEnabled()){
                            log.debug("[Global, " + globalQueueName + "] moved " + cassandraMessages.size()
                                    + " to user queues, tot = " + totMsgMoved);
                        }
                    }
                } else {
                    try {
                        Thread.sleep(queueWorkerWaitTime);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            pollingLoopCount++;
        }

    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
