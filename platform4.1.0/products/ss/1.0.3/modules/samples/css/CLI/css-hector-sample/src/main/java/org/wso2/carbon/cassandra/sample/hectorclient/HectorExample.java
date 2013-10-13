/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.cassandra.sample.hectorclient;

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ThriftCfDef;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Simple sample using hector API to connect to Cassandra
 */
public class HectorExample {

    private static Cluster cluster;

    public static void main(String arg[]) {
        //Read User Inputs

        Scanner scanner = new Scanner(System.in);
        System.out.print("Host: ");
        String host = scanner.nextLine();
        System.out.print("Port: ");
        String port = scanner.nextLine();
        System.out.print("Tenant Id: ");
        String tenantId = scanner.nextLine();
        System.out.print("Tenant Password: ");
        String tenantPasswd = scanner.nextLine();
        System.out.print("Keyspace Name: ");
        String keyspaceName = scanner.nextLine();
        System.out.print("Column Family: ");
        String ColumnFamilyName = scanner.nextLine();
        System.out.print("Column Name List (Only 5 columns which are separated by colon eg (col1:col2:col3:col4:col5)) : ");
        String columnNameList = scanner.nextLine();
        System.out.print("Number of Row you need: ");
        String rowCount = scanner.nextLine();

        cluster = ExampleHelper.createCluster(host,port,tenantId, tenantPasswd);
        createKeyspace(host,port,keyspaceName, ColumnFamilyName, columnNameList, rowCount);
    }


    /**
     * Create a keyspace, add a column family and read a column's value
     *
     * @param keyspaceName
     * @param columnFamily
     * @param columnList
     * @param rowCount
     */
    private static void createKeyspace(String host,String port,String keyspaceName, String columnFamily, String columnList,
                                       String rowCount) {
        //Create Keyspace
        KeyspaceDefinition definition = new ThriftKsDef(keyspaceName);
        cluster.addKeyspace(definition);
        //add columnt family
        ColumnFamilyDefinition familyDefinition = new ThriftCfDef(keyspaceName, columnFamily);
        cluster.addColumnFamily(familyDefinition);
        //Add data to a column
        Keyspace keyspace = null;
        try {
            keyspace = HFactory.createKeyspace(keyspaceName, cluster);
        } catch (Exception e) {
            System.out.println("Error in adding Keyspace" + e);
        }
        Mutator<String> mutator = HFactory.createMutator(keyspace, new StringSerializer());
        String rowKey = null;
        List<String> keyList = new ArrayList<String>();
        for (int i = 0; i < Integer.parseInt(rowCount); i++) {
            rowKey = UUID.randomUUID().toString();
            keyList.add(rowKey);
            System.out.println("\nInserting Key " + rowKey + "To Column Family " + columnFamily + "\n");
            for (String columnName : columnList.split(":")) {
                String columnValue = UUID.randomUUID().toString();
                mutator.insert(rowKey, columnFamily, HFactory.createStringColumn(columnName, columnValue));
                System.out.println("Column Name: " + columnName + " Value: " + columnValue + "\n");
            }
        }
        //Read Data
        ColumnQuery<String, String, String> columnQuery =
                HFactory.createStringColumnQuery(keyspace);
        for (String key : keyList) {
            System.out.println("\nretrieving Key " + rowKey + "From Column Family " + columnFamily + "\n");
            for (String columnName : columnList.split(":")) {
                columnQuery.setColumnFamily(columnFamily).setKey(key).setName(columnName);
                QueryResult<HColumn<String, String>> result = columnQuery.execute();
                HColumn<String, String> hColumn = result.get();
                //sout data
                System.out.println("Column: " + hColumn.getName() + " Value : " + hColumn.getValue() + "\n");
            }
        }
        System.exit(0);
    }
}
