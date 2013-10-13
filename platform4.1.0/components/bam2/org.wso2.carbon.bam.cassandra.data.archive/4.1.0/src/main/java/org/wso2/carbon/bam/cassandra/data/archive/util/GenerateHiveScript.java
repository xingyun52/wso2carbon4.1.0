package org.wso2.carbon.bam.cassandra.data.archive.util;


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


import me.prettyprint.cassandra.service.CassandraHost;
import me.prettyprint.hector.api.Cluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.persistence.cassandra.Utils.CassandraSDSUtils;
import org.wso2.carbon.databridge.persistence.cassandra.datastore.CassandraConnector;
import org.wso2.carbon.databridge.persistence.cassandra.datastore.DataType;

import java.util.Iterator;
import java.util.List;


public class GenerateHiveScript {
    private static final Log log = LogFactory.getLog(GenerateHiveScript.class);

    private String cassandraHostIp;
    private int cassandraPortValue;

    private static final String createTableIfNotExist = "CREATE EXTERNAL TABLE IF NOT EXISTS ";
    private static final String storedBy = " STORED BY 'org.apache.hadoop.hive.cassandra.CassandraStorageHandler' WITH SERDEPROPERTIES ";
    private static final String cassandraHost = "\"cassandra.host\"";
    private static final String cassandraPort = "\"cassandra.port\"";
    private static final String cassandraKs = "\"cassandra.ks.name\"";
    private static final String cassandraKsUsername = "\"cassandra.ks.username\"";
    private static final String cassandraKsPassword = "\"cassandra.ks.password\"";
    private static final String cassandraCFName = "\"cassandra.cf.name\"";
    private static final String cassandraColumnMapping = "\"cassandra.columns.mapping\"";

    private static final String rowKey = "rowKey STRING";
    private static final String name = "Name STRING";
    private static final String version = "Version STRING";
    private static final String timestamp = "Data_Timestamp BIGINT"; /* Timestamp is a keyword in Hive, so here we
                                                                        have changed the name to  Data_Timestamp */
    private static final String nickName = "Nick_Name STRING";
    private static final String description = "Description STRING";

    private static final String basicColumnMapping = ":key,Name,Version,Timestamp,Nick_Name,Description";


    public GenerateHiveScript(Cluster cluster) {
        Iterator iterator = cluster.getConnectionManager().getHosts().iterator();
        //Currently only take one cassandra host value. Need to check whether we can provide all ip addresses for cassandra cluster
        while (iterator.hasNext()) {
            CassandraHost cassandraHost = (CassandraHost) iterator.next();
            cassandraHostIp = cassandraHost.getIp();
            cassandraPortValue = cassandraHost.getPort();
        }
    }

    /**
     * CREATE EXTERNAL TABLE IF NOT EXISTS org_wso2_bam_phone_retail_store_kpi
     * (rowKey STRING, Name STRING, Version STRING,p_Timestamp BIGINT, Nick_Name STRING, Description STRING,
     * meta_clientType STRING,payload_brand STRING,payload_quantity INT,payload_total INT,payload_user STRING ) STORED BY
     * 'org.apache.hadoop.hive.cassandra.CassandraStorageHandler'
     * WITH SERDEPROPERTIES (
     * "cassandra.host" = "127.0.0.1" ,
     * "cassandra.port" = "9160" ,
     * "cassandra.ks.name" = "EVENT_KS" ,
     * "cassandra.ks.username" = "admin" ,
     * "cassandra.ks.password" = "admin" ,
     * "cassandra.cf.name" = "org_wso2_bam_phone_retail_store_kpi",
     * "cassandra.columns.mapping" =
     * ":key,Name,Version,Timestamp,Nick_Name,Description,meta_clientType,payload_brand,payload_quantity,payload_total,payload_user");
     * <p/>
     * insert overwrite table org_wso2_bam_phone_retail_store_kpi_arch select * from org_wso2_bam_phone_retail_store_kpi;
     */

    public String generateMappingForReadingCassandraOriginalCF(StreamDefinition streamDefinition) {
        String tableName = removeUnnecessaryCharsFromTableName(streamDefinition.getName());
        String cfName = CassandraSDSUtils.convertStreamNameToCFName(streamDefinition.getName());

        String hiveQuery = "drop table " + tableName + ";\n";
        hiveQuery = hiveQuery + generateHiveCassandraMapping(streamDefinition, tableName, cfName) +"\n";
        hiveQuery = hiveQuery + setHiveConf(CassandraArchiveUtil.CASSANDRA_ORIGINAL_CF,cfName);
        return hiveQuery;
    }

    public String generateMappingForWritingToArchivalCF(StreamDefinition streamDefinition) {
        String tableName = removeUnnecessaryCharsFromTableName(streamDefinition.getName()) + "_arch";
        String cfName = CassandraSDSUtils.convertStreamNameToCFName(streamDefinition.getName()) + "_arch";
        String hiveQuery = "drop table " + tableName + ";\n";
        hiveQuery = hiveQuery + generateHiveCassandraMapping(streamDefinition, tableName, cfName);
        return hiveQuery;
    }

    public String hiveQueryForWritingDataToArchivalCF(StreamDefinition streamDefinition) {
        String originalTableName = removeUnnecessaryCharsFromTableName(streamDefinition.getName());
        String archivalTableName = originalTableName + "_arch";
        String hiveQuery = "insert overwrite table " + archivalTableName + " select * from " + originalTableName + ";";
        return hiveQuery;
    }


    public String generateMappingForWritingToTmpCF(StreamDefinition streamDefinition) {
        String streamNameAndVersion = streamDefinition.getName() + streamDefinition.getVersion();
        String tableName = removeUnnecessaryCharsFromTableName(streamNameAndVersion);
        String cfName = CassandraSDSUtils.convertStreamNameToCFName(streamNameAndVersion);
        String hiveQuery = "drop table " + tableName + ";\n";
        hiveQuery = hiveQuery + generateHiveCassandraMappingForTmpCF(streamDefinition, tableName, cfName) + "\n";
        hiveQuery = hiveQuery + setHiveConfProperty(cfName);
        return hiveQuery;
    }

    private String setHiveConfProperty(String cfName) {
        String hiveConf = setHiveConf(CassandraArchiveUtil.COLUMN_FAMILY_NAME ,cfName);
        hiveConf = hiveConf + setHiveConf(CassandraArchiveUtil.CASSANDRA_PORT, Integer.toString(cassandraPortValue));
        hiveConf = hiveConf + setHiveConf(CassandraArchiveUtil.CASSANDRA_HOST_IP,cassandraHostIp);
        return hiveConf;
    }

    private String setHiveConf(String key, String value){
        return  "set " + key  + "=" + value + "; \n";
    }

    private String generateHiveCassandraMappingForTmpCF(StreamDefinition streamDefinition, String tableName, String cfName) {
        String hiveQuery = createTableIfNotExist + tableName + " (" + rowKey + "," + name;
        hiveQuery = hiveQuery + ")" + storedBy + "(" + "\n";
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraHost, cassandraHostIp);
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraPort, Integer.toString(cassandraPortValue));
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraKs, CassandraConnector.BAM_EVENT_DATA_KEYSPACE);
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraKsUsername, "admin");
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraKsPassword, "admin");
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraCFName, cfName);
        hiveQuery = hiveQuery + cassandraColumnMapping + "=\"" + ":key,Name" + "\"";
        hiveQuery = hiveQuery + ");";
        if (log.isDebugEnabled()) {
            log.debug("Hive Query for reading original CF: " + hiveQuery);
        }

        return hiveQuery;
    }

    public String hiveQueryForWritingDataToTmpCF(StreamDefinition streamDefinition) {
        String originalTableName = removeUnnecessaryCharsFromTableName(streamDefinition.getName());
        String streamNameAndVersion = streamDefinition.getName() + streamDefinition.getVersion();
        String tmpTableName = removeUnnecessaryCharsFromTableName(streamNameAndVersion);
        String hiveQuery = "insert overwrite table " + tmpTableName + " select rowKey,Name from " + originalTableName + ";";
        return hiveQuery;
    }


    public String mapReduceJobAsHiveQuery(){
       return "class org.wso2.carbon.bam.cassandra.data.archive.mapred.CassandraRowDeletionAnalyzer;";
    }


    private String generateHiveCassandraMapping(StreamDefinition streamDefinition, String tableName, String cfName) {
        List<Attribute> streamPayloadList = streamDefinition.getPayloadData();
        List<Attribute> streamCorrelationList = streamDefinition.getCorrelationData();
        List<Attribute> streamMetaList = streamDefinition.getMetaData();

        String hiveQuery = createTableIfNotExist + tableName + " (" + rowKey + "," + name + "," + version +
                "," + timestamp + "," + nickName + "," + description;

        hiveQuery = constructAttributeList(streamMetaList, hiveQuery, DataType.meta);
        hiveQuery = constructAttributeList(streamCorrelationList, hiveQuery, DataType.correlation);
        hiveQuery = constructAttributeList(streamPayloadList, hiveQuery, DataType.payload);

        hiveQuery = hiveQuery + ")" + storedBy + "(" + "\n";

        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraHost, cassandraHostIp);
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraPort, Integer.toString(cassandraPortValue));
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraKs, CassandraConnector.BAM_EVENT_DATA_KEYSPACE);
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraKsUsername, "admin");
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraKsPassword, "admin");
        hiveQuery = hiveQuery + addCassandraDetailsAsKeyValue(cassandraCFName, cfName);

        String columnMappingValue = basicColumnMapping + columnMappingForAttributes(streamMetaList, DataType.meta) +
                columnMappingForAttributes(streamCorrelationList, DataType.correlation) +
                columnMappingForAttributes(streamPayloadList, DataType.payload);

        hiveQuery = hiveQuery + cassandraColumnMapping + "=\"" + columnMappingValue + "\"";

        hiveQuery = hiveQuery + ");";

        if (log.isDebugEnabled()) {
            log.debug("Hive Query for reading original CF: " + hiveQuery);
        }

        return hiveQuery;

    }

    private String columnMappingForAttributes(List<Attribute> attributeList, DataType dataType) {
        String columnMapping = "";
        if (attributeList != null && attributeList.size() > 0) {
            for (Attribute attribute : attributeList) {
                columnMapping = columnMapping + "," + CassandraSDSUtils.getColumnName(dataType, attribute);
            }
        }
        return columnMapping;
    }

    private String addCassandraDetailsAsKeyValue(String key, String value) {
        return key + "=\"" + value + "\" ,\n";
    }


    private String constructAttributeList(List<Attribute> attributeList, String script, DataType dataType) {
        if (attributeList != null && attributeList.size() > 0) {
            for (Attribute attribute : attributeList) {
                String attributeName = CassandraSDSUtils.getColumnName(dataType, attribute);
                String attributeType = getAttributeTypeValue(attribute.getType());
                script = script + "," + attributeName + attributeType;
            }
        }
        return script;
    }

    private String getAttributeTypeValue(AttributeType type) {
        String value = null;
        switch (type) {
            case INT:
                value = " INT";
                break;
            case LONG:
                value = " BIGINT";
                break;
            case FLOAT:
                value = " FLOAT";
                break;
            case DOUBLE:
                value = " DOUBLE";
                break;
            case STRING:
                value = " STRING";
                break;
            case BOOL:
                value = " BOOLEAN";
                break;
        }
        return value;
    }

    private String removeUnnecessaryCharsFromTableName(String streamName) {
        if (streamName.contains(".")) {
            streamName = streamName.replace(".", "_");
        }
        if (streamName.contains("-")) {
            streamName = streamName.replace("-", "_");
        }
        return streamName;
    }
}
