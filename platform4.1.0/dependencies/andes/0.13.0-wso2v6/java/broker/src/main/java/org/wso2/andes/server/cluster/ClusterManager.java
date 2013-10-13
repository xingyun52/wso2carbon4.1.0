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
import org.wso2.andes.server.cluster.coordination.CoordinationConstants;
import org.wso2.andes.server.cluster.coordination.CoordinationException;
import org.wso2.andes.server.cluster.coordination.ZooKeeperAgent;
import org.wso2.andes.server.configuration.ClusterConfiguration;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.apache.zookeeper.*;
import org.wso2.andes.server.util.AndesUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**                                                                                                    F
 * Cluster Manager is responsible for Handling the Broker Cluster Management Tasks like
 * Queue Worker distribution. Fail over handling for cluster nodes. etc.
 */
public class ClusterManager {
    private Log log = LogFactory.getLog(ClusterManager.class);

    private int nodeId;
    private String zkNode;

    private GlobalQueueManager globalQueueManager;
    /**
     * Map that Keeps the node id
     */
    private Map<Integer, ClusterNode> nodeMap =
            new ConcurrentHashMap<Integer, ClusterNode>();

    /**
     * This list contains the leaders what this will node will backup in case of failures
     */
    private Map<String, String> leaderBackUpList = new ConcurrentHashMap<String, String>();

    private ZooKeeperAgent zkAgent;

    private HashMap<Integer,String[]> queueNodeMap = new HashMap<Integer,String[]>();

    /**
     *
     */
    private List<String> workerAssignedQueues = new ArrayList<String>();


    /**
     * This is the group size of the nodes that will run the leader election in case of a leader
     * failure
     *
     * TODO make this configurable after adding the cluster configuration
     */
    private int leaderBackNodeGroupSize = 3;



    private String connectionString;

    /**
     * Create a ClusterManager instance
     * @param messageStore Underlying CassandraMessageStore
     * @param zkConnectionString zookeeper port
     */
    public ClusterManager(CassandraMessageStore messageStore , String zkConnectionString) {
        this.globalQueueManager = new GlobalQueueManager(messageStore);
        this.connectionString =zkConnectionString;
    }

    public ClusterManager(CassandraMessageStore messageStore) {
        this.globalQueueManager = new GlobalQueueManager(messageStore);
        this.nodeId =1;
    }

    public void startAllGlobalQueueWorkers() throws CoordinationException {

        /* With new queue worker implmentation with regarding to issue : https://wso2.org/jira/browse/MB-81
          We need to have a thread sleep before starting all global queue workers.
          It will throw a null pointer exception,When we trigger this just after the init() of this class,
          It is because in zoo keeper data change listener need to be fired to assign a node id , before this.
        */
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            String msg = "Error while making the thread sleep for 5000ms";
            log.error(msg, e);
        }
        List<String> globalQueueNames = AndesUtils.getAllGlobalQueueNames();
         for(String globalQueueName : globalQueueNames){
             handleQueueAddition(globalQueueName);
         }
    }


    /**
     * TODO this method is too long 
     * 
     * Handles new Queue addition requests coming for this node
     * @param queueName Queue to be added
     * @throws CoordinationException In case of a unexpected Error happening when running
     * the cluster coordination algorithm
     */
    public void handleQueueAddition(String queueName) throws CoordinationException {
        //get all children from the qpid worker
        //Select a leader
        //Update the node with adding new queue to the list
        //Select PMC
        //Update PMC to look for this leader
        //Node data Format broker Id : q1,q2,q3,q4 ... : node_name_1=q1,node_name_2=q2


        //If we are using the OnceInOrder delivery mode we do not need to run the Queue Worker election Algorithm

        ClusterConfiguration config = ClusterResourceHolder.getInstance().getClusterConfiguration();

        if(config.isOnceInOrderSupportEnabled()) {
            //TODO handle this in once in order impl
            return;
        }


        /**
         * In Non Cluster Mode  we do not need any zookeeper related Coordination.
         * Just add the A Global Queue Worker
         */
        if(!config.isClusteringEnabled()) {
          //   nodeMap.get(nodeId).addGlobalQueueWorker(queueName);

            String[] queues = queueNodeMap.get(nodeId);
            if(queues == null || queues.length == 0) {
                queues = new String[]{queueName};
                queueNodeMap.put(nodeId,queues);
            } else {
                List<String> list  = new ArrayList<String>();


                for(String q : queues) {
                    list.add(q);
                }
                if(!list.contains(queueName)) {
                    list.add(queueName);
                }

                queues = list.toArray(new String[list.size()]);
                queueNodeMap.put(nodeId,queues);
            }


            globalQueueManager.addGlobalQueue(queueName);
            return;
        }

        if(workerAssignedQueues.contains(queueName)) {
            return;
        }

        try {
            log.debug("Adding Queue : " + queueName + " to the cluster ");

            ClusterNode myNode = nodeMap.get(nodeId);

            if(myNode != null && myNode.getGlobalQueueWokers().contains(queueName)) {
                return;
            }

            List<String> nodeList = zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);

            int currentMinQueueCountPerNode = Integer.MAX_VALUE;
            String currentCandidateNode=zkNode;
            String currentCandidateNodeData=""+ nodeId+":";



            for(String node : nodeList) {
                String path = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                        CoordinationConstants.NODE_SEPARATOR + node;
                byte[] data = zkAgent.getZooKeeper().getData(path, false, null);

                String dataStr = new String(data);

                // Data formats
                //1)id:q1,q2,q3,:node=q,node=q2
                //2)id:
                //3)id:q1,q2,
                //4)id::node=q,node=q2

                if(dataStr.contains(queueName)) {
                   break;
                }
                String[] parts = dataStr.split(":");

                if (parts.length >= 1) {

                    if (parts.length != 1 && parts[1].length() > 0) {
                        String[] queues = parts[1].split(",");
                        queueNodeMap.put(Integer.parseInt(parts[0]),queues);
                        if (queues.length < currentMinQueueCountPerNode) {
                            currentMinQueueCountPerNode = queues.length;
                            currentCandidateNode = node;
                            currentCandidateNodeData = dataStr;
                        }
                    } else {
                        if (0 < currentMinQueueCountPerNode) {
                            currentMinQueueCountPerNode = 0;
                            currentCandidateNode = node;
                            currentCandidateNodeData = dataStr;
                        }
                    }
                }


            }




            log.debug("Current Candidiate znode : " + currentCandidateNode + " znode data : " + currentCandidateNodeData);

            log.debug("Node selected  to add queue worker node : " + currentCandidateNode +
                    " for queue : " + queueName);
            String[] candidateNodeDataParts = currentCandidateNodeData.split(":");
            String newData;

            // Possible Data formats formats for currentCandidateNodeData
            //1) id:q1,q2,q3,:node=q,node=q2 (candidateNodeDataParts.length = 3)
            //2)id:
            //3)id:q1,q2, (candidateNodeDataParts.length =2)
            //4)id::node=q,node=q2  (candidateNodeDataParts.length = 3)
            if (candidateNodeDataParts.length > 1) {

                String replacePart = candidateNodeDataParts[1];


                //Handle id::node=q,node=q2
                if (replacePart.length() == 0) {
                    newData = currentCandidateNodeData.replace(":" + replacePart + ":",
                            ":" + replacePart + queueName + "," + ":");
                } else {

                    newData = currentCandidateNodeData.replace(replacePart,
                            replacePart + queueName + ",");
                }
            } else {
                newData = candidateNodeDataParts[0] + ":" + queueName + ",";
            }


            String leaderPath = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                    CoordinationConstants.NODE_SEPARATOR + currentCandidateNode;
            log.debug("Adding new queue data to znode to add new queue workers  : " + newData);
            zkAgent.getZooKeeper().setData(leaderPath, newData.getBytes(), -1);

            //Select and update PMC

            selectPMCForQueue(queueName, currentCandidateNode, nodeList);
        } catch (Exception e) {
            e.printStackTrace();
            String msg = "Error while handling Queue worker Addition";
            log.error(msg, e);
            throw new CoordinationException(msg, e);
        }


    }


    public void handleQueueRemoval(String queueName) throws CoordinationException {

         //If we are using the OnceInOrder delivery mode we do not need to run the Queue Worker election Algorithm
        ClusterConfiguration config = ClusterResourceHolder.getInstance().getClusterConfiguration();

        if(config.isOnceInOrderSupportEnabled()) {

            return;
        }


        /**
         * In Non Cluster Mode  we do not need any zookeeper related Coordination.
         * Just add the A Global Queue Worker
         */
        if(!config.isClusteringEnabled()) {
            globalQueueManager.removeWorker(queueName);
            return;
        }


        try {
            log.debug("Removing Queue " + queueName + " From the cluster");

            List<String> nodeList = zkAgent.getZooKeeper().
                      getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);

            for(String node : nodeList) {
                // We need to remove workers and PMC Entries for the queues
                String path = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                        CoordinationConstants.NODE_SEPARATOR + node;
                byte[] data = zkAgent.getZooKeeper().getData(path, false, null);

                String dataStr = new String(data);

                if(dataStr.contains(queueName)) {

                    //Data formats
                    //1)id:q1,q2,q3,:node=q,node=q2
                    //2)id:q1,q2,
                    //3)id::node=q,node=q2

                    String[] dataParts = dataStr.split(":");
                    String newDataString =null;
                    switch (dataParts.length) {
                        case 2 : {
                            //id:q1,q2,
                            newDataString = dataStr.replace(queueName+",","");
                            zkAgent.getZooKeeper().setData(path,newDataString.getBytes(),-1);
                            break;
                        }
                        case 3 : {

                            // id:q1,q2,q3,:node=q,node=q2
                            // id::node=q,node=q2

                            String newDp1= dataParts[1];

                            if(dataParts[1].contains(queueName)) {
                                newDp1 = dataParts[1].replace(queueName + ",","");
                            }

                            String newDp2 = dataParts[2];
                            if (dataParts[2].contains(queueName)) {
                                String[] pmcParts = dataParts[2].split(",");
                                List<String> newPmcParts = new ArrayList<String>();
                                for(int i = 0;i < pmcParts.length ; i++) {

                                    if(!pmcParts[i].contains(queueName)) {
                                        newPmcParts.add(pmcParts[i]);
                                    }
                                }
                                newDp2 = "";
                                for(String newPart : newPmcParts) {
                                    newDp2 +=newPart + ",";
                                }
                            }

                            newDataString = dataParts[0]+ ":" + newDp1+ ":" + newDp2;
                            zkAgent.getZooKeeper().setData(path,newDataString.getBytes(),-1);
                            break;
                        }

                     default: {
                         newDataString = dataStr;
                         break;
                     }

                    }
                }
            }
        } catch (KeeperException e) {
            throw new CoordinationException("Error while removing Queue " ,e);
        } catch (InterruptedException e) {
            throw new CoordinationException("Error while removing Queue " ,e);
        } finally {
        }


    }

    /**
     * This will Select the Set of nodes which handle the leader failures and update with
     * appropriate data
     * @param queue  Queue name
     * @param electedLeader  Current leader node
     * @param nodeList   Node List
     * @throws InterruptedException     When interrupted communicating with zookeeper
     * @throws KeeperException   When error happen when communicating with zookeeper
     */
    private void selectPMCForQueue(String queue, String electedLeader, List<String> nodeList)
            throws InterruptedException, KeeperException {
        //Select max of 3 nodes as PMC
        // Get data in nodes
        // update each data adding leader node and queue name.


        if (nodeList != null && nodeList.size() > 1) {

            List<String> pmc = new ArrayList<String>();

            // We remove the elected leader so that it will
            nodeList.remove(electedLeader);


            Random rand = new Random();
            if (nodeList.size() < leaderBackNodeGroupSize) {

                int i = rand.nextInt(nodeList.size());
                pmc.add(nodeList.get(i));

            } else {


                for (; ;) {
                    int i = rand.nextInt(nodeList.size());

                    if (!pmc.contains(nodeList.get(i))) {
                        pmc.add(nodeList.get(i));
                        if (pmc.size() >= leaderBackNodeGroupSize) {
                            break;
                        }
                    }
                }

            }

            for (String member : pmc) {
                //Get Member data
                //Add new leader details
                //update the data.
                byte[] mdata = zkAgent.getZooKeeper().getData(CoordinationConstants.
                        QUEUE_WORKER_COORDINATION_PARENT +
                        CoordinationConstants.NODE_SEPARATOR +
                        member, false, null);


                //Node data Format broker Id : q1,q2,q3,q4 ... : node_name_1=q1,node_name_2=q2
                String dataStr = new String(mdata);
                String[] parts = dataStr.split(":");

                String newDataStr = null;
                String newPart = electedLeader + "=" + queue;

                switch (parts.length) {
                    case 1:
                    case 2: {
                        newDataStr = dataStr + ":" + newPart;
                        break;
                    }
                    case 3: {
                        newDataStr = dataStr + "," + newPart;
                        break;
                    }
                }

                //TODO review this
                if (parts.length == 2) {

                    newDataStr = dataStr + ":" + newPart;

                } else if (parts.length == 3) {

                    newDataStr = dataStr + "," + newPart;
                } else if (parts.length == 1) {

                }


                //update the node
                if (newDataStr != null) {
                    zkAgent.getZooKeeper().setData(
                            CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                                    CoordinationConstants.NODE_SEPARATOR + member, newDataStr.getBytes(),
                            -1);

                }


            }


        }


    }



    /**
     * Initialize the Cluster manager. This will create ZNodes related to nodes and assign node ids
     * @throws CoordinationException in a Error when communicating with Zookeeper
     */
    public void init() throws CoordinationException {
        final ClusterConfiguration config = ClusterResourceHolder.getInstance().getClusterConfiguration();
        final CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();

        //If Clustering is disabled
        if(!config.isClusteringEnabled()) {

            List<String> nodeList = messageStore.storedNodeDetails();

            for(String node : nodeList) {
                messageStore.deleteNodeData(node);
            }

            queueNodeMap.put(new Integer(nodeId),new String[]{});
            messageStore.addNodeDetails(""+ nodeId,config.getBindIpAddress());
            return;
        }


        try {

            // create a new node with a generated randomId
            // get the node name and id

            zkAgent = new ZooKeeperAgent(connectionString);
            zkAgent.initQueueWorkerCoordination();
            final String nodeName = CoordinationConstants.QUEUE_WORKER_NODE+
                    (UUID.randomUUID()).toString().replace("-","_");
            String path = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT
                    + nodeName;
           //Register the watcher for zoo keeper parent to be fired when children changed
            zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, new Watcher(){

                        @Override
                        public void process(WatchedEvent watchedEvent) {
                           if(Event.EventType.NodeChildrenChanged == watchedEvent.getType()) {
                               try {
                                   List<String> nodeList =
                                           zkAgent.getZooKeeper().
                                                   getChildren(
                                                           CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);
                                   for (String node : nodeList) {
                                       // Splitting out the id assigned to the node by zookeeper
                                       String id = node.substring(
                                               nodeName.length());


                                       if((CoordinationConstants.NODE_SEPARATOR +node).contains(nodeName)) {
                                            zkNode = node;


                                           nodeId = Integer.parseInt(id);

                                            log.info("Initializing Cluster Manager , " +
                                                    "Selected Node id : " + nodeId);
                                            messageStore.addNodeDetails(""+ nodeId,config.getBindIpAddress());


                                           List<String> zkNodes = zkAgent.getZooKeeper().getChildren(
                                                    CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT,false);

                                           List<String> zkNodeIds = new ArrayList<String>();

                                           for(String zNode : zkNodes) {
                                               zkNodeIds.add(""+ getNodeIdFromZkNode(zNode));
                                           }

                                           List<String> storedNodes = messageStore.storedNodeDetails();

                                           for (String storedNode : storedNodes) {

                                               if (!zkNodeIds.contains(storedNode)) {
                                                   messageStore.deleteNodeData(storedNode);
                                               }

                                           }


                                            String data = ""+ nodeId + ":";
                                            zkAgent.getZooKeeper().setData(CoordinationConstants.
                                                            QUEUE_WORKER_COORDINATION_PARENT +
                                                            CoordinationConstants.NODE_SEPARATOR +
                                                            node, data.getBytes(),-1);
                                            zkAgent.getZooKeeper().
                                                    getData(CoordinationConstants.
                                                            QUEUE_WORKER_COORDINATION_PARENT +
                                                            CoordinationConstants.NODE_SEPARATOR +
                                                            node,new NodeDataChangeListener(),null);


                                       }

                                   }

                               } catch (Exception e) {
                                   e.printStackTrace();
                                   log.error(e);
                               }
                           }
                        }
                    });
            // Once this method called above defined watcher will be fired
            zkAgent.getZooKeeper().create(path, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);



        } catch (Exception e) {
            e.printStackTrace();

            String msg = "Error while initializing the zookeeper coordination ";
            log.error("Error while initializing the zookeeper coordination " ,e);
            throw new CoordinationException(msg,e);
        }
    }



    private class NodeDataChangeListener implements Watcher {

        @Override
        public void process(WatchedEvent event) {
                if(Event.EventType.NodeDataChanged == event.getType()) {


                    //Date Change Event received. This can be Queue addition , Queue removal , PMC Change
                    try {
                       byte[] data = zkAgent.getZooKeeper().getData(CoordinationConstants.
                            QUEUE_WORKER_COORDINATION_PARENT +
                            CoordinationConstants.NODE_SEPARATOR
                            + zkNode, this, null);


                        String dataStr = new String(data);

                        //Data formats
                        //1)id:q1,q2,q3,:node=q,node=q2
                        //2)id:q1,q2,
                        //3)id::node=q,node=q2
                        //4)id:
                        String[] parts = dataStr.split(":");
                        if (parts.length > 1 && parts[1].length() > 0) {
                            String[] queues = parts[1].split(",");
                            queueNodeMap.put(Integer.parseInt(parts[0]),queues);
                            if (queues.length > 0) {

                                int nId = Integer.parseInt(parts[0]);
                                ClusterNode node = nodeMap.get(nId);
                                if (node == null) {
                                    node = new ClusterNode(nId);
                                    nodeMap.put(nId, node);
                                }

                                String[] queuesToBescheduled = getQueueWorkersToBeScheduled(node, queues);
                                for (String q : queuesToBescheduled) {
                                    log.debug("Adding Queue worker for queue : " + q + " from node :"
                                            + nodeId);
                                    node.addGlobalQueueWorker(q);
                                    globalQueueManager.addGlobalQueue(q);
                                }
                                String[] queuesToBeRemoved = getQueueWorkersToBeRemoved(node,queues);
                                for(String q : queuesToBeRemoved) {
                                    log.debug("Removing Queue Worker for queue : " + q + " from node :" + nodeId );
                                    node.removeGlobalQueueWorker(q);
                                    globalQueueManager.removeWorker(q);
                                }



                            }

                        } else {
                            // This means This node does not have any queues or PMC scenarios to handle
                            int nId = Integer.parseInt(parts[0]);
                            ClusterNode node = nodeMap.get(nId);

                            if(node != null) {
                                for(String q : node.getGlobalQueueWokers()) {
                                    globalQueueManager.removeWorker(q);
                                    node.removeGlobalQueueWorker(q);
                                }
                            }


                            //We clean up the Leader Backup List
                            leaderBackUpList.clear();


                        }

                      //  dataStr = 0:testQueue112,:queue_worker_nodec82df71c_1714_49a1_a55f_e4329a6ed6910000000001=testQueue112
                        if (parts.length == 3) {
                            String[] leaderNodes = parts[2].split(",");

                            if (leaderNodes.length > 0) {
                                for (String lNode : leaderNodes) {
                                    String[] details = lNode.split("=");

                                    if (!leaderBackUpList.containsKey(details[0])) {

                                        String leaderNode = details[0];
                                        leaderBackUpList.put(leaderNode, details[1]);
                                        //Add Listener for node existence
                                        zkAgent.getZooKeeper().exists(CoordinationConstants.
                                                QUEUE_WORKER_COORDINATION_PARENT +
                                                CoordinationConstants.NODE_SEPARATOR + leaderNode,
                                                new NodeExistenceListener(leaderNode));

                                    } else {
                                        // Get the queues of the node to be backed up
                                        String d = leaderBackUpList.get(details[0]);
                                        // If not containing the new queue to be backed up, adding that
                                        if (!d.contains(details[1])) {
                                            d += ":" + details[1];
                                            leaderBackUpList.put(details[0], d);
                                        }
                                    }
                                }

                                syncLeaderBackUpList(leaderNodes);



                            } else {
                                leaderBackUpList.clear();
                            }




                        }

                    } catch (Exception e) {
                        log.fatal("Error processing the Node data change : This might cause serious " +
                                "issues in distributed queue management", e);
                    }
                }
        }


        private String[] getQueueWorkersToBeScheduled(ClusterNode node, String[] queues) {

            ArrayList<String> queueList = new ArrayList<String>();

            for(String q : queues) {
                if(!node.getGlobalQueueWokers().contains(q)) {
                    queueList.add(q);
                    node.addGlobalQueueWorker(q);
                }
            }

            return queueList.toArray(new String[queueList.size()]);
        }

        private String[] getQueueWorkersToBeRemoved(ClusterNode node , String[] queues) {
            ArrayList<String> queueList = new ArrayList<String>();
            List<String> qs = Arrays.asList(queues);
            for (String q :node.getGlobalQueueWokers()) {
                if(!qs.contains(q)) {
                    queueList.add(q);
                }
            }

            return queueList.toArray(new String[queueList.size()]);
        }


        private void syncLeaderBackUpList(String[]  pmcData) {

            List<String> nodesToRemove = new ArrayList<String>();

            for(String node : leaderBackUpList.keySet()) {

                boolean contains = false;
                List<String> queuesToRemove = new ArrayList<String>();
                for(String pmcElm : pmcData) {

                    if(pmcElm.contains(node)) {
                        contains = true;

                        String[] parts = pmcElm.split("=");

                        String q = parts[1].trim();
                        if(!leaderBackUpList.get(node).contains(q)) {
                            queuesToRemove.add(q);
                        }
                    }

                }

                String nodeData = leaderBackUpList.get(node);
                for(String q : queuesToRemove) {
                    if(nodeData.contains(q+":")) {
                        nodeData.replace(q+":","");
                    } else {
                        nodeData.replace(q,"");
                    }
                }

                if(!contains) {
                    nodesToRemove.add(node);
                }
            }

        }


    }

    private class NodeExistenceListener implements Watcher {

        private String watchZNode = null;

        public NodeExistenceListener(String zNode) {
            this.watchZNode = zNode;
        }

        @Override
        public void process(WatchedEvent watchedEvent) {
            //Remove the leader node from local backup list
            //remove the leader node from znode

            //elect new leader and pmc
            if (Event.EventType.NodeDeleted == watchedEvent.getType()) {
                String path = watchedEvent.getPath();


                String[] parts = path.split(CoordinationConstants.NODE_SEPARATOR);
                String deletedNode = parts[parts.length - 1];

                try {
                    int deletedNodeId = getNodeIdFromZkNode(deletedNode);
                    ClusterResourceHolder.getInstance().getCassandraMessageStore().
                            deleteNodeData("" + deletedNodeId);
                } catch (Exception e) {
                    log.error("Error while removing node details");
                }


                try {

                     if (leaderBackUpList.containsKey(deletedNode)) {
                        String queueData = leaderBackUpList.get(deletedNode);
                        String[] queues = queueData.split(":");
                        leaderBackUpList.remove(deletedNode);

                        for (String queue : queues) {
                            removeFromQLeaderFromZnode(deletedNode, queue);
                             if (isLeaderToHandleTheFailOver(queue)) {
                                log.debug(" Running Leader election again for queue : " + queue +
                                        " from node :" + nodeId );
                                handleQueueAddition(queue);
                            } else {
                                log.debug("Node :"+ nodeId+ " is not handling the Fail over for" +
                                   " queue : " + queue);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e);
                }

            } else {
                try {
                    zkAgent.getZooKeeper().exists(CoordinationConstants.
                                                QUEUE_WORKER_COORDINATION_PARENT +
                                                CoordinationConstants.NODE_SEPARATOR + watchZNode,
                                                this);

                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Error while registering a watch for loader node : " + watchZNode  ,e);
                }
            }


        }


        private void removeFromQLeaderFromZnode(String node, String queue)
                throws InterruptedException, KeeperException {
            String zkDataElement = node + "=" + queue;
            byte[] data = zkAgent.getZooKeeper().getData(CoordinationConstants.
                    QUEUE_WORKER_COORDINATION_PARENT +
                    CoordinationConstants.NODE_SEPARATOR + zkNode, false, null);

            String zkNodeData = new String(data);

            if (zkDataElement.contains(zkDataElement)) {
                String newData = zkNodeData.replace(zkDataElement, "");

                zkAgent.getZooKeeper().setData(
                        CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                                CoordinationConstants.NODE_SEPARATOR + zkNode, newData.getBytes(),
                        -1);
            }


        }


        /**
         * This will check whether this node must handle the queue worker node fail over scenario
         * in case of a the node that contain the queue worker for a given queue is down.
         * @param queue
         * @return true if this node get elected to do the fail over handling
         * @throws CoordinationException
         */
        private boolean isLeaderToHandleTheFailOver(String queue) throws CoordinationException {
            boolean returnValue;
            try {

                ZooKeeperAgent zooKeeperAgent = new ZooKeeperAgent(connectionString);
                zooKeeperAgent.initQueueFailOverMCProcess(queue);

                final ZooKeeper localZk = zooKeeperAgent.getZooKeeper();
                String guid = (UUID.randomUUID()).toString();
                final String nodeName = CoordinationConstants.QUEUE_FAIL_OVER_HANDLING_NODE +
                        guid.replace("-", "_");
                String path = CoordinationConstants.QUEUE_FAIL_OVER_HANDLING_PARENT+"_"+queue
                        + nodeName;


                localZk.create(path, new byte[0],
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

                List<String> strList =
                        localZk.getChildren(CoordinationConstants.QUEUE_FAIL_OVER_HANDLING_PARENT+"_"+queue,
                                false);
                String selectedNode = null;
                int currentMin = Integer.MAX_VALUE;
                for(String child : strList) {
                    String id = child.substring(nodeName.length());
                    int seqNumber = Integer.parseInt(id);
                    if(seqNumber < currentMin) {
                        selectedNode = child;
                        currentMin = seqNumber;
                    }
                }

                if(selectedNode.contains(guid.replace("-", "_"))) {
                    // i m the selected Node
                    returnValue = true;
                } else {
                    returnValue = false;
                }


                localZk.close();



            } catch (Exception e) {
                String msg = "Error while selecting node to handle the node deletion";
                throw new CoordinationException(msg, e);
            }

            return returnValue;
        }
    }

    public int getNodeId() {
       return nodeId;
    }

    public String getZkConnectionString() {
        return connectionString;
    }


    public String getNodeAddress(int nodeId) {
        return ClusterResourceHolder.getInstance().getCassandraMessageStore().getNodeData(""+nodeId);
    }

    public List<Integer> getZkNodes(){
        List<String> storedNodes = ClusterResourceHolder.getInstance().getCassandraMessageStore().storedNodeDetails();
        ArrayList<Integer> zkNodeIdList = new ArrayList<Integer>();
        for(String storedNode : storedNodes)
        {
            Integer ZKId = Integer.parseInt(storedNode);
            zkNodeIdList.add(ZKId);
        }

        return zkNodeIdList;
    }

    public String[] getQueues(int nodeId) {
        String[] queuesListofNode = null;
        try {
            List<String> nodeList = zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);

            for (String node : nodeList) {

                // Splitting out the id assigned to the node by zookeeper
                int currentId = getNodeIdFromZkNode(node);
                if (currentId == nodeId) {

                    String path = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                            CoordinationConstants.NODE_SEPARATOR + node;
                    byte[] data = zkAgent.getZooKeeper().getData(path, false, null);

                    String dataStr = new String(data);

                    // Data formats
                    //1)id:q1,q2,q3,:node=q,node=q2
                    //2)id:
                    //3)id:q1,q2,
                    //4)id::node=q,node=q2
                    String[] parts = dataStr.split(":");

                    if (parts.length >= 1) {

                        if (parts.length != 1 && parts[1].length() > 0) {
                            queuesListofNode = parts[1].split(",");
                        }
                    }

                    break;
                }

            }

        } catch (Exception e) {
            log.error("Error processing node data", e);
        }
        return queuesListofNode;

    }

    public int numberOfMessagesInGlobalQueue(String queue) {
        return globalQueueManager.getMessageCountOfGlobalQueue(queue);
    }

    public List<String> getTopics() throws Exception {
        return globalQueueManager.getTopics();
    }

    public List<String> getSubscribers(String topic) throws Exception {
        return globalQueueManager.getSubscribers(topic);
    }

    public int getSubscriberCount(String topic) throws Exception {
        return globalQueueManager.getSubscribers(topic).size();
    }

    public boolean updateWorkerForQueue(String queueToBeMoved, String newNodeToAssign) {
        boolean successful = false;
        try {
            //find the leader of the queue
            String oldLeader = "";
            String newLeader = "";
            
            
            List<String> nodeList = zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);
            for(String node : nodeList) {
                String path = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                        CoordinationConstants.NODE_SEPARATOR + node;
                byte[] data = zkAgent.getZooKeeper().getData(path, false, null);

                String dataStr = new String(data);
                //Data formats
                //1)id:q1,q2,q3,:node=q,node=q2
                //2)id:q1,q2,
                //3)id::node=q,node=q2
                //4)id:
                String[] parts = dataStr.split(":");
                if(parts.length >1 && parts[1].contains(queueToBeMoved)) {
                   oldLeader = node; 
                }
                if(getNodeIdFromZkNode(node) == Integer.parseInt(newNodeToAssign)) {
                    newLeader = node;
                }
                
            }

            for(String node : nodeList) {
                String path = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                        CoordinationConstants.NODE_SEPARATOR + node;
                byte[] data = zkAgent.getZooKeeper().getData(path, false, null);

                String dataStr = new String(data);
                //Data formats
                //1)id:q1,q2,q3,:node=q,node=q2
                //2)id:q1,q2,
                //3)id::node=q,node=q2
                //4)id:
                String[] parts = dataStr.split(":");

                //remove queue from oldLeader. It's PMC data does not have that queue
                if(node.equals(oldLeader)) {
                    if(dataStr.contains(queueToBeMoved)) {
                        dataStr = dataStr.replace(queueToBeMoved+",","");
                    }
                }
                //add queue to the new leader
                if(node.equals(newLeader)) {

                    String newData;

                    // Possible Data formats formats for currentCandidateNodeData
                    //1) id:q1,q2,q3,:node=q,node=q2 (candidateNodeDataParts.length = 3)
                    //2)id:
                    //3)id:q1,q2, (candidateNodeDataParts.length =2)
                    //4)id::node=q,node=q2  (candidateNodeDataParts.length = 3)
                    if (parts.length > 1) {

                        String replacePart = parts[1];
                        if(replacePart.contains(queueToBeMoved)) {
                           return successful;
                        }
                        //Handle id::node=q,node=q2
                        if (replacePart.length() == 0) {
                            newData = dataStr.replace(":" + replacePart + ":",
                                    ":" + replacePart + queueToBeMoved + "," + ":");
                        } else {

                            newData = dataStr.replace(replacePart,
                                    replacePart + queueToBeMoved + ",");
                        }
                    } else {
                        newData = parts[0] + ":" + queueToBeMoved + ",";
                    }

                    dataStr = newData;
                }

                //update each PMC node which handled old leader node.
                if (parts.length == 3) {
                    String[] leaderNodes = parts[2].split(",");

                    if (leaderNodes.length > 0) {
                        if(dataStr.contains(oldLeader+"="+queueToBeMoved)) {
                            //Avoid setting PMC for leader node
                            if(node.equals(newLeader)) {
                                dataStr = dataStr.replace(oldLeader+"="+queueToBeMoved,"");
                            } else {
                                dataStr = dataStr.replace(oldLeader+"="+queueToBeMoved,newLeader+"="+queueToBeMoved);
                            }
                            //update with all queue changes andPMC changes together
                            zkAgent.getZooKeeper().setData(path, dataStr.getBytes(),-1);
                        } else if(node.equals(oldLeader) ||(node.equals(newLeader))){
                            zkAgent.getZooKeeper().setData(path, dataStr.getBytes(),-1);
                        }
                    } else if(node.equals(oldLeader) ||(node.equals(newLeader))) {
                        zkAgent.getZooKeeper().setData(path, dataStr.getBytes(),-1);
                    }
                }else if(node.equals(oldLeader) ||(node.equals(newLeader))) {
                    zkAgent.getZooKeeper().setData(path, dataStr.getBytes(),-1);
                }
                //zkAgent.getZooKeeper().setData(path,newDataString.getBytes(),-1);
                log.debug("Successfully moved queue worker for "+queueToBeMoved+" from node: "+oldLeader+" to node: "+ newLeader);
                successful=true;
            }

        }  catch (Exception e) {
            log.fatal("Error processing the Node data change : This might cause serious " +
                    "issues in distributed queue management", e);
        }

        return successful;
    }

    public boolean isClusteringEnabled() {
        ClusterConfiguration config = ClusterResourceHolder.getInstance().getClusterConfiguration();
        return config.isClusteringEnabled();
    }

    public String getMyNodeID()
    {
        String nodeID = Integer.toString(nodeId);
        return nodeID;
    }

    private static int getNodeIdFromZkNode(String node) {
        return Integer.parseInt(node.substring(node.length() - 5));
    }

    public void shutDownMyNode() {
        try {
            //stop all global queue Workers
            globalQueueManager.removeAllQueueWorkersLocally();
        } catch (Exception e) {
            log.error("Error stopping global queues while shutting down", e);
        }

    }

    public GlobalQueueManager getGlobalQueueManager() {
        return this.globalQueueManager;
    }

}
