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
package org.wso2.carbon.cassandra.sample.cqlclient;

import me.prettyprint.cassandra.model.CqlQuery;
import me.prettyprint.cassandra.model.CqlRows;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.ThriftKsDef;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.QueryResult;
import org.apache.cassandra.utils.ByteBufferUtil;

import java.util.Scanner;

/**
 * Simple sample using hector API to connect to Cassandra
 */
public class CQL3Sample {
    private static String keyspaceName;
    private static Cluster cluster;
    private static Serializer se = new StringSerializer();
    private static final LongSerializer le = new LongSerializer();
    private static Keyspace keyspace;
    private static String cf = "StandardLong1";
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
        System.out.print("Keyspace:");
        keyspaceName = scanner.nextLine();
        cluster=ExampleHelper.createCluster(host,port,tenantId,tenantPasswd);
        //Create Keyspace
        KeyspaceDefinition definition = new ThriftKsDef(keyspaceName);
        cluster.addKeyspace(definition);
        try {
            keyspace = HFactory.createKeyspace(keyspaceName, cluster);
        } catch (Exception e) {
            System.out.println("Error in adding Keyspace" + e);
        }
        createColumnFamily();
        insertData();
        simpleSelectQueries();

        countQuery();

        updateSyntaxCQL();

    }

    public static void createColumnFamily()
    {
        CqlQuery<String,String,Long> cqlQuery = new CqlQuery<String,String,Long>(keyspace, se, se, le);
        String query="CREATE COLUMNFAMILY StandardLong1 (key text PRIMARY KEY,birthyear int,name text,age int)\n" +
                     "        WITH comparator=UTF8Type AND default_validation=UTF8Type";
        cqlQuery.setQuery(query);
        cqlQuery.execute();
    }

    private static void insertData()
    {
        CqlQuery<String,String,Long> cqlQuery = new CqlQuery<String,String,Long>(keyspace, se, se, le);
        cqlQuery.setQuery("INSERT INTO StandardLong1 (KEY, birthyear,name,age)\n" +
                          "                VALUES ('cqlQueryTest_key1',1975,'tom',24)");
        cqlQuery.execute();
        cqlQuery.setQuery("INSERT INTO StandardLong1 (KEY, birthyear,name,age)\n" +
                          "                VALUES ('cqlQueryTest_key2',1976,'jerry',25)");
        cqlQuery.execute();
        cqlQuery.setQuery("INSERT INTO StandardLong1 (KEY, birthyear,name,age)\n" +
                          "                VALUES ('cqlQueryTest_key3',1977,'peter',26)");
        cqlQuery.execute();
        cqlQuery.setQuery("INSERT INTO StandardLong1 (KEY, birthyear,name,age)\n" +
                          "                VALUES ('cqlQueryTest_key4',1978,'olivia',27)");
        cqlQuery.execute();
        cqlQuery.setQuery("INSERT INTO StandardLong1 (KEY, birthyear,name,age)\n" +
                          "                VALUES ('cqlQueryTest_key5',1979,'walter',28)");
        cqlQuery.execute();
        cqlQuery.setQuery("INSERT INTO StandardLong1 (KEY, birthyear,name,age)\n" +
                          "                VALUES ('cqlQueryTest_key5',1980,'nina',29)");
        cqlQuery.execute();
        cqlQuery.setQuery("INSERT INTO StandardLong1 (KEY, birthyear,name,age)\n" +
                          "                VALUES ('cqlQueryTest_key6',1981,'bell',30)");
        cqlQuery.execute();
        cqlQuery.setQuery("INSERT INTO StandardLong1 (KEY, birthyear,name,age)\n" +
                          "                VALUES ('cqlQueryTest_key7',1982,'cate',31)");
        cqlQuery.execute();

    }

    public static void simpleSelectQueries() {
        CqlQuery<String,String,Long> cqlQuery = new CqlQuery<String,String,Long>(keyspace, se, se, le);
        //Select data in name column
        cqlQuery.setQuery("select name from StandardLong1");
        QueryResult<CqlRows<String,String,Long>> result1 = cqlQuery.execute();
        CqlRows<String, String, Long> rows1 = result1.get();
        System.out.println((rows1.getList().get(0).getColumnSlice().getColumnByName("name")));
        System.out.println(rows1.getCount());
        //select all column family data in every column
        cqlQuery.setQuery("select * from StandardLong1");
        QueryResult<CqlRows<String,String,Long>> result2 = cqlQuery.execute();
        CqlRows<String, String, Long> rows = result2.get();
        // check that we contain a 'key' column
        System.out.println((rows.getList().get(0).getColumnSlice().getColumnByName("name")));

    }

    public static void countQuery() {
        CqlQuery<String,String,Long> cqlQuery = new CqlQuery<String,String,Long>(keyspace, se, se, le);
        //count of two keys
        cqlQuery.setQuery("SELECT COUNT(*) FROM StandardLong1 WHERE KEY in ('cqlQueryTest_key1', 'cqlQueryTest_key2')");
        QueryResult<CqlRows<String,String,Long>> result = cqlQuery.execute();
        System.out.println(result.get().getAsCount());
        //FUll key count of the column family
        cqlQuery.setQuery("SELECT COUNT(*) FROM StandardLong1");
        QueryResult<CqlRows<String,String,Long>> result1 = cqlQuery.execute();
        System.out.println(result.get().getAsCount());
    }

    public static void updateSyntaxCQL() {
        CqlQuery<String,String,Long> cqlQuery = new CqlQuery<String,String,Long>(keyspace, se, se, le);
        String query = String.format("UPDATE StandardLong1 SET name = tommy WHERE KEY = cqlQueryTest_key1");
        cqlQuery.setQuery(query);
        cqlQuery.execute();
    }
}
