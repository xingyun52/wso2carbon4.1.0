/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.carbon.cassandra.cluster.mgt.publisher;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cassandra.cluster.mgt.Util.ClusterMonitorConfig;
import org.wso2.carbon.cassandra.cluster.mgt.Util.StreamsDefinitions;
import org.wso2.carbon.cassandra.cluster.mgt.data.ColumnFamilyInformation;
import org.wso2.carbon.cassandra.cluster.mgt.data.KeyspaceInfo;
import org.wso2.carbon.cassandra.cluster.mgt.data.NodeInformation;
import org.wso2.carbon.cassandra.cluster.mgt.exception.ClusterDataAdminException;
import org.wso2.carbon.cassandra.cluster.mgt.query.ClusterMBeanServiceHandler;
import org.wso2.carbon.databridge.agent.thrift.DataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.*;
import org.wso2.carbon.ntask.core.AbstractTask;

import java.net.MalformedURLException;
import java.util.ArrayList;

public class ClusterDataPublisher extends AbstractTask{
    private static Log log = LogFactory.getLog(ClusterDataPublisher.class);

    private static DataPublisher dataPublisher;
    @Override

    public void execute(){
        String columnFamilyStatsStreamId = null;
        String nodeInfoStreamId=null;
        DataPublisher dataPublisher = null;
        try {
             dataPublisher = getDataPublisher();
        } catch (AgentException e) {
            log.info(e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AuthenticationException e) {
            log.info(e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (TransportException e) {
            log.info(e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (MalformedURLException e) {
            log.info(e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        try {
            columnFamilyStatsStreamId=dataPublisher.findStream(StreamsDefinitions.COLUMN_FAMILY_STATS,StreamsDefinitions.VERSION);
        } catch (StreamDefinitionException e) {
            log.error(e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoStreamDefinitionExistException e) {
            try {
                columnFamilyStatsStreamId=dataPublisher.defineStream(StreamsDefinitions.COLUMN_FAMILY_STATS_STREAM_DEF);
            } catch (MalformedStreamDefinitionException e1) {
                log.error(e);
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (StreamDefinitionException e1) {
                log.error(e);
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (DifferentStreamDefinitionAlreadyDefinedException e1) {
                log.error(e);
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (AgentException e1) {
                log.error(e);
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } catch (AgentException e) {
            log.error(e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        try {
            nodeInfoStreamId=dataPublisher.findStream(StreamsDefinitions.NODE_STATS,StreamsDefinitions.VERSION);
        } catch (AgentException e) {
            log.error(e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (StreamDefinitionException e) {
            log.error(e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoStreamDefinitionExistException e) {
            try {
                nodeInfoStreamId=dataPublisher.defineStream(StreamsDefinitions.NODE_STATS_STREAM_DEF);
            } catch (AgentException e1) {
                log.error(e);
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (MalformedStreamDefinitionException e1) {
                log.error(e);
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (StreamDefinitionException e1) {
                log.error(e);
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (DifferentStreamDefinitionAlreadyDefinedException e1) {
                log.error(e);
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        if(columnFamilyStatsStreamId!=null && dataPublisher!=null)
        {
            try {
                publishCFSTats(dataPublisher,columnFamilyStatsStreamId);
            } catch (ClusterDataAdminException e) {
                if(log.isDebugEnabled())
                {
                    log.error("Error while publishing the column family stats",e);
                }
            }
        }

        if(nodeInfoStreamId!=null && dataPublisher!=null)
        {
            try {
                publishNodeInfoStats(dataPublisher,nodeInfoStreamId);
            } catch (ClusterDataAdminException e) {
                if(log.isDebugEnabled())
                {
                    log.error("Error while publishing the data node info stats",e);
                }
            }
        }
    }

    private static DataPublisher getDataPublisher()
            throws AgentException, MalformedURLException,
            AuthenticationException, TransportException {
        if(null == dataPublisher){
            dataPublisher = new DataPublisher(ClusterMonitorConfig.getSecureUrl(),
                                                  ClusterMonitorConfig.getReceiverUrl(),
                                                  ClusterMonitorConfig.getUsername(), ClusterMonitorConfig.getPassword());
        }
        return dataPublisher;
    }

    private void publishCFSTats(DataPublisher dataPublisher, String streamId)
            throws ClusterDataAdminException {
        ArrayList<Object> cfstats;
        ClusterMBeanServiceHandler clusterMBeanServiceHandler=new ClusterMBeanServiceHandler();
        String[] nodeBasicInfo=clusterMBeanServiceHandler.getClusterBasicInfo();
        for(KeyspaceInfo keyspaceInfo:clusterMBeanServiceHandler.getColumnFamilyStats())
        {
            for(ColumnFamilyInformation columnFamilyInformation:keyspaceInfo.getColumnFamilyInformations())
            {
                cfstats=new ArrayList<Object>();
                cfstats.add(ClusterMonitorConfig.getNodeId());
                cfstats.add(nodeBasicInfo[0]);
                cfstats.add(nodeBasicInfo[1]);
                cfstats.add(nodeBasicInfo[2]);
                cfstats.add(keyspaceInfo.getKeyspaceName());
                cfstats.add(columnFamilyInformation.getColumnFamilyName());
                cfstats.add(columnFamilyInformation.getSSTableCount());
                cfstats.add(columnFamilyInformation.getLiveDiskSpaceUsed());
                cfstats.add(columnFamilyInformation.getTotalDiskSpaceUsed());
                cfstats.add(columnFamilyInformation.getMemtableColumnsCount());
                cfstats.add(columnFamilyInformation.getMemtableDataSize());
                cfstats.add(columnFamilyInformation.getMemtableSwitchCount());
                cfstats.add(columnFamilyInformation.getReadCount());
                cfstats.add(columnFamilyInformation.getReadLatency());
                cfstats.add(columnFamilyInformation.getWriteCount());
                cfstats.add(columnFamilyInformation.getWriteLatency());
                cfstats.add(columnFamilyInformation.getPendingTasks());
                cfstats.add(columnFamilyInformation.getNumberOfKeys());
                cfstats.add(columnFamilyInformation.getBloomFilterFalsePostives());
                cfstats.add(columnFamilyInformation.getBloomFilterFalseRatio());
                cfstats.add(columnFamilyInformation.getBloomFilterSpaceUsed());
                cfstats.add(columnFamilyInformation.getCompactedRowMinimumSize());
                cfstats.add(columnFamilyInformation.getCompactedRowMaximumSize());
                cfstats.add(columnFamilyInformation.getCompactedRowMeanSize());
                Event cfstatsEvent = new Event(streamId, System.currentTimeMillis(), new Object[]{"external"}, null,cfstats.toArray(new Object[cfstats.size()] ));
                try {
                    dataPublisher.publish(cfstatsEvent);
                } catch (AgentException e) {
                    if(log.isDebugEnabled())
                    {
                        log.error("Error while publishing the data column family stats",e);
                    }
                }
            }
        }

    }
    private void publishNodeInfoStats(DataPublisher dataPublisher,String streamId)
            throws ClusterDataAdminException {
        ArrayList<Object> nodeInfo;
        ClusterMBeanServiceHandler clusterMBeanServiceHandler=new ClusterMBeanServiceHandler();
        String[] nodeBasicInfo=clusterMBeanServiceHandler.getClusterBasicInfo();
        nodeInfo=new ArrayList<Object>();
        nodeInfo.add(ClusterMonitorConfig.getNodeId());
        nodeInfo.add(nodeBasicInfo[0]);
        nodeInfo.add(nodeBasicInfo[1]);
        nodeInfo.add(nodeBasicInfo[2]);
        NodeInformation nodeInformation=clusterMBeanServiceHandler.getNodeInfo();
        nodeInfo.add(nodeInformation.getLoad().split(" ")[1]);
        nodeInfo.add(Double.parseDouble(nodeInformation.getLoad().split(" ")[0]));

        nodeInfo.add(nodeInformation.getUptime());
        nodeInfo.add(nodeInformation.getExceptions());
        nodeInfo.add(nodeInformation.getHeapMemory().getUseMemory());
        nodeInfo.add(nodeInformation.getHeapMemory().getMaxMemory());
        nodeInfo.add(nodeInformation.getDataCenter());

        nodeInfo.add(nodeInformation.getRack());
        nodeInfo.add(nodeInformation.getKeyCacheProperties().getCacheCapacity());
        nodeInfo.add(nodeInformation.getKeyCacheProperties().getCacheSize());
        nodeInfo.add(nodeInformation.getKeyCacheProperties().getCacheSize());
        nodeInfo.add(nodeInformation.getKeyCacheProperties().getCacheRequests());
        nodeInfo.add(nodeInformation.getKeyCacheProperties().getCacheHits());

        nodeInfo.add(nodeInformation.getKeyCacheProperties().getCacheSavePeriodInSeconds());
        nodeInfo.add(nodeInformation.getKeyCacheProperties().getCacheRecentHitRate());

        nodeInfo.add(nodeInformation.getRowCacheProperties().getCacheCapacity());
        nodeInfo.add(nodeInformation.getRowCacheProperties().getCacheSize());
        nodeInfo.add(nodeInformation.getRowCacheProperties().getCacheSize());
        nodeInfo.add(nodeInformation.getRowCacheProperties().getCacheRequests());
        nodeInfo.add(nodeInformation.getRowCacheProperties().getCacheHits());
        nodeInfo.add(nodeInformation.getRowCacheProperties().getCacheSavePeriodInSeconds());
        nodeInfo.add(nodeInformation.getRowCacheProperties().getCacheRecentHitRate());
        Event nodeInfoStats = new Event(streamId, System.currentTimeMillis(), new Object[]{"external"}, null,nodeInfo.toArray(new Object[nodeInfo.size()] ));
        try {
            dataPublisher.publish(nodeInfoStats);
        } catch (AgentException e) {
            if(log.isDebugEnabled())
            {
                log.error("Error while publishing the data column family stats",e);
            }
        }
    }
}
