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
package org.wso2.andes.server.information.management;

import org.wso2.andes.management.common.mbeans.QueueManagementInformation;
import org.wso2.andes.management.common.mbeans.annotations.MBeanOperationParameter;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.cassandra.DefaultClusteringEnabledSubscriptionManager;
import org.wso2.andes.server.cluster.ClusterManager;
import org.wso2.andes.server.cluster.GlobalQueueManager;
import org.wso2.andes.server.management.AMQManagedObject;
import org.wso2.andes.server.store.CassandraMessageStore;

import javax.management.NotCompliantMBeanException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QueueManagementInformationMBean extends AMQManagedObject implements QueueManagementInformation {

    GlobalQueueManager globalQueueManager;
    CassandraMessageStore messageStore;

    public QueueManagementInformationMBean() throws NotCompliantMBeanException {
        super(QueueManagementInformation.class, QueueManagementInformation.TYPE);
        this.messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
        this.globalQueueManager = new GlobalQueueManager(messageStore);
    }

    public String getObjectInstanceName() {
        return QueueManagementInformation.TYPE;
    }

    public synchronized String[] getAllQueueNames() {

        try {
            ArrayList<String> queuesList = (ArrayList<String>) messageStore.getDestinationQueueNames();
            Iterator itr = queuesList.iterator();
            //remove topic specific queues
            while (itr.hasNext()) {
                String globalQueueName = (String) itr.next();
                if(globalQueueName.startsWith("tmp_")) {
                    itr.remove();
                }
            }
            String[] queues= new String[queuesList.size()];
            queuesList.toArray(queues);
            return queues;
        } catch (Exception e) {
          throw new RuntimeException("Error in accessing destination queues",e);
        }

    }

    public boolean isQueueExists(String queueName) {
        try {
            List<String> queuesList = messageStore.getDestinationQueueNames();
            return queuesList.contains(queueName);
        } catch (Exception e) {
          throw new RuntimeException("Error in accessing destination queues",e);
        }
    }

    public void deleteQueue(@MBeanOperationParameter(name = "queueName",
            description = "Name of the queue to be deleted") String queueName) {
        ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();
        CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
        DefaultClusteringEnabledSubscriptionManager subscriptionManager =
                (DefaultClusteringEnabledSubscriptionManager) ClusterResourceHolder.getInstance().getSubscriptionManager();
        try {
            if(subscriptionManager.getNumberOfSubscriptionsForQueue(queueName)>0) {
                throw new Exception("Queue" +queueName +" Has Active Subscribers. Please Stop Them First.");
            }
            messageStore.removeMessageCounterForQueue(queueName);
            clusterManager.handleQueueRemoval(queueName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getMessageCount(String queueName) {
       return (int) messageStore.getCassandraMessageCountForQueue(queueName);
    }

    public int getSubscriptionCount( String queueName){
        try {
            return globalQueueManager.getSubscriberCount(queueName);
        } catch (Exception e) {
            throw new RuntimeException("Error in getting subscriber count",e);
        }
    }
}
