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

package org.wso2.carbon.cassandra.sample.keycounter;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;

public class KeyspaceStatistics {
    public static void main(String[] args) {
        String userName = null;
        String userPassword = null;
        String hostPool = null;
        String keyspaceName = null;
        String columnFamilyName = null;

        if (args.length > 0) {
            userName = args[0];
            userPassword = args[1];
            hostPool = args[2];
            keyspaceName = args[3];
            columnFamilyName = args[4];
        } else {
            System.out.println("Usage : KeyspaceStatistics USERNAME PASSWORD HOSTPOOL KS CF");
            System.exit(1);
        }

        Cluster cassandraConnection = CassandraConnector.createCluster(userName, userPassword, hostPool);
        Keyspace keyspace = null;
        try {
            keyspace = HFactory.createKeyspace(keyspaceName, cassandraConnection);
        } catch (Exception e) {
            System.out.println("Error in connecting keyspace" + e);
        }
        KSUtils.getKeyCount(keyspace, columnFamilyName);
    }
}
