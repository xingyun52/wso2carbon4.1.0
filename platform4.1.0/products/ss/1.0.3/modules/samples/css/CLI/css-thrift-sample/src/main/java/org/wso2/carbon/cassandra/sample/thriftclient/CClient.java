/*
 * Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.sample.thriftclient;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cassandra.thrift.*;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public class CClient
{
    public static void main(String[] args)
            throws TException, InvalidRequestException, UnavailableException, UnsupportedEncodingException, NotFoundException, TimedOutException, AuthorizationException, AuthenticationException, SchemaDisagreementException {
        //Read CLI inputs
        //-Dhost=localhost -Dport=9160 -Dkeyspace=ksoneone -Dcf=cfoneone -Dusername=admin -Dpassword=admin

        String userName = System.getProperty("username");
        String userPassword = System.getProperty("password");
        String host = System.getProperty("host");
        String port = System.getProperty("port");
        String keyspaceName = System.getProperty("keyspace");
        String cfName = System.getProperty("cf");

        //Create transport
        TTransport tr = new TFramedTransport(new TSocket(host, Integer.parseInt(port)));
        TProtocol proto = new TBinaryProtocol(tr);
        Cassandra.Client client = new Cassandra.Client(proto);
        tr.open();

        //Authenticate user
        Map<String,String> credentials = new HashMap<String, String>();
        credentials.put("username",userName);
        credentials.put("password",userPassword);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest(credentials);
        client.login(authenticationRequest);

        //Add keyspace and columnfamily
        KsDef ksDef = new KsDef();
        ksDef.setName(keyspaceName);
        ksDef.setStrategy_class("SimpleStrategy");
        Map<String,String> stratergyOptions = new HashMap<String, String>();
        stratergyOptions.put("replication_factor","3");
        ksDef.setStrategy_options(stratergyOptions);
        CfDef cfDef = new CfDef(keyspaceName,cfName);
        List<CfDef> listCfDef = new ArrayList<CfDef>();
        listCfDef.add(cfDef);
        ksDef.setCf_defs(listCfDef);
        client.system_add_keyspace(ksDef);

        String key_user_id = "1";

        // insert data
        long timestamp = System.currentTimeMillis();
        client.set_keyspace(keyspaceName);
        ColumnParent parent = new ColumnParent(cfName);

        Column nameColumn = new Column(toByteBuffer("name"));
        nameColumn.setValue(toByteBuffer("Chris Goffinet"));
        nameColumn.setTimestamp(timestamp);
        client.insert(toByteBuffer(key_user_id), parent, nameColumn, ConsistencyLevel.ONE);

        Column ageColumn = new Column(toByteBuffer("age"));
        ageColumn.setValue(toByteBuffer("24"));
        ageColumn.setTimestamp(timestamp);
        client.insert(toByteBuffer(key_user_id), parent, ageColumn, ConsistencyLevel.ONE);

        ColumnPath path = new ColumnPath(cfName);

        // read single column
        path.setColumn(toByteBuffer("name"));
        System.out.println(client.get(toByteBuffer(key_user_id), path, ConsistencyLevel.ONE));

        // read entire row
        SlicePredicate predicate = new SlicePredicate();
        SliceRange sliceRange = new SliceRange(toByteBuffer(""), toByteBuffer(""), false, 10);
        predicate.setSlice_range(sliceRange);

        List<ColumnOrSuperColumn> results = client.get_slice(toByteBuffer(key_user_id), parent, predicate, ConsistencyLevel.ONE);
        for (ColumnOrSuperColumn result : results)
        {
            Column column = result.column;
            System.out.println(toString(column.name) + " -> " + toString(column.value));
        }

        tr.close();
    }

    public static ByteBuffer toByteBuffer(String value)
            throws UnsupportedEncodingException
    {
        return ByteBuffer.wrap(value.getBytes("UTF-8"));
    }

    public static String toString(ByteBuffer buffer)
            throws UnsupportedEncodingException
    {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, "UTF-8");
    }
}