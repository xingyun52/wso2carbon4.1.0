package org.wso2.andes.management.common.mbeans;

import org.wso2.andes.management.common.mbeans.annotations.MBeanAttribute;
import org.wso2.andes.management.common.mbeans.annotations.MBeanOperationParameter;

import java.util.*;


/**
 * <code>ClusterManagementInformation</code>
 * Exposes the Cluster Management related information
 */
public interface ClusterManagementInformation {

    static final String TYPE = "ClusterManagementInformation";

     //Individual attribute name constants
    String ATTR_NODE_ID = "nodeId" ;
    String ATTR_ADDRESS = "Address";
    String ATTR_PORT = "Port";

     //All attribute names constant
    static final List<String> CLUSTER_ATTRIBUTES
            = Collections.unmodifiableList(
             new ArrayList<String>(
                     new HashSet<String>(
                             Arrays.asList(
                                     ATTR_NODE_ID,
                                     ATTR_ADDRESS,
                                     ATTR_PORT))));


    @MBeanAttribute(name = "Address", description = "zookeeper Server")
    String getZkServer();

    @MBeanAttribute(name = "isClusteringEnabled", description = "is in clustering mode")
    boolean isClusteringEnabled();

    @MBeanAttribute(name = "getMyNodeID", description = "Zookeeper Node Id assigned for the node")
    String getMyNodeID();

    @MBeanAttribute(name = "Queues", description = "Existing queues in the node")
    String[] getQueues(int nodeId);

    @MBeanAttribute(name = "updateWorkerForQueue", description = "Move the given global queue Worker Handler to a new node")
    boolean updateWorkerForQueue(@MBeanOperationParameter(name="queueToMove",description = "name of queue whose queue worker to move") String queueToBeMoved,
                                 @MBeanOperationParameter(name="newNode",description = "name of new node to assign queue worker") String newNodeToAssign);

    @MBeanAttribute(name = "zooKeeperNodes" , description = "Existing zookeeper nodes")
    List<Integer> getZkNodes();

    @MBeanAttribute(name = "MessageCount" , description = "Message Count in the queue")
    int getMessageCount(@MBeanOperationParameter(name = "queueName",description = "Name of the queue which message count is required")String queueName);

    @MBeanAttribute(name = "Topics" ,description = "Topics where subscribers are available")
    List<String> getTopics();

    @MBeanAttribute(name = "Subscribers",description = "Subscribers for a given topic")
    List<String> getSubscribers(@MBeanOperationParameter(name="Topic",description = "Topic name") String topic);

    @MBeanAttribute(name = "Subscriber Count",description = "Number of subscribers for a given topic")
    int getSubscriberCount(@MBeanOperationParameter(name="Topic",description = "Topic name") String topic);
}
