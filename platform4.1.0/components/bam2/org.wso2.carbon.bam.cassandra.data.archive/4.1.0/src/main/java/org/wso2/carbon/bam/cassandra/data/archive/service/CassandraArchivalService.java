package org.wso2.carbon.bam.cassandra.data.archive.service;


/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.SliceQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.analytics.hive.service.HiveExecutorService;
import org.wso2.carbon.analytics.hive.web.HiveScriptStoreService;
import org.wso2.carbon.bam.cassandra.data.archive.util.ArchiveConfiguration;
import org.wso2.carbon.bam.cassandra.data.archive.util.CassandraArchiveUtil;
import org.wso2.carbon.bam.cassandra.data.archive.util.GenerateHiveScript;
import org.wso2.carbon.cassandra.dataaccess.ClusterInformation;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.core.Utils.DataBridgeUtils;
import org.wso2.carbon.databridge.core.exception.StreamDefinitionStoreException;
import org.wso2.carbon.databridge.persistence.cassandra.datastore.CassandraConnector;

import java.util.List;



public class CassandraArchivalService {

    private static final Log log = LogFactory.getLog(CassandraArchivalService.class);

    private static StringSerializer stringSerializer = StringSerializer.get();

    private static CassandraConnector cassandraConnector;

    private StreamDefinition streamDefinition;

    public void archiveCassandraData(ArchiveConfiguration archiveConfiguration) throws Exception {

        ClusterInformation clusterInformation = new ClusterInformation("admin",
                "admin");
        Cluster cluster = CassandraArchiveUtil.getDataAccessService().getCluster(clusterInformation);

        cassandraConnector = CassandraArchiveUtil.getCassandraConnectorService();
        String streamIdKey =
                DataBridgeUtils.constructStreamKey(archiveConfiguration.getStreamName().trim(), archiveConfiguration.getVersion().trim());


        String streamId = getStreamIdFromCassandra(cluster, streamIdKey);
        try {
            streamDefinition = cassandraConnector.getStreamDefinitionFromStore(cluster, streamId);
            GenerateHiveScript generateHiveScript = new GenerateHiveScript(cluster);
            String hiveQuery = generateHiveScript.generateMappingForReadingCassandraOriginalCF(streamDefinition);
            hiveQuery = hiveQuery + generateHiveScript.generateMappingForWritingToArchivalCF(streamDefinition) + "\n";
            hiveQuery = hiveQuery + generateHiveScript.hiveQueryForWritingDataToArchivalCF(streamDefinition)+ "\n";
            hiveQuery = hiveQuery + generateHiveScript.generateMappingForWritingToTmpCF(streamDefinition)+ "\n";
            hiveQuery = hiveQuery + generateHiveScript.hiveQueryForWritingDataToTmpCF(streamDefinition)+ "\n";
            hiveQuery = hiveQuery + generateHiveScript.mapReduceJobAsHiveQuery();
            HiveScriptStoreService hiveScriptStoreService = CassandraArchiveUtil.getHiveScriptStoreService();
            String scriptName = streamDefinition.getName() + streamDefinition.getVersion() + "ArchiveScript";
            hiveScriptStoreService.saveHiveScript(scriptName,hiveQuery,"3 * * * * ? *");

        } catch (StreamDefinitionStoreException e) {
            log.error("Failed to get stream definition from Cassandra",e);
        }



    }

    public String getStreamIdFromCassandra(Cluster cluster, String streamIdKey) throws Exception {
        String streamId = null;
        Keyspace keyspace = HFactory.createKeyspace(CassandraConnector.BAM_META_KEYSPACE, cluster);
        SliceQuery<String, String, String> sliceQuery = HFactory.createSliceQuery(keyspace, stringSerializer, stringSerializer, stringSerializer);
        sliceQuery.setColumnFamily(CassandraConnector.BAM_META_STREAM_ID_CF).setKey(streamIdKey);
        sliceQuery.setRange("", "", false, 1);
        QueryResult<ColumnSlice<String, String>> result = sliceQuery.execute();
        List<HColumn<String, String>> columnList = result.get().getColumns();
        if(columnList.size()>0){
            streamId = columnList.get(0).getValue();
        }else {
            String errorMsg = "Stream key: "+ streamIdKey +" doesn't exist, Please check the stream name and version";
            log.error(errorMsg);
            throw new Exception(errorMsg);
        }
        return streamId;
    }
}
