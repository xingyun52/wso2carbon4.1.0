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

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;

import java.util.HashMap;
import java.util.Map;

public class CassandraConnector {

    public static final String CLUSTER_NAME = "ToolKitCluster";
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";

    /**
     * Create a Cluster
     *
     * @param username the name to be used to authenticate to Cassandra cluster
     * @param password the password to be used to authenticate to Cassandra cluster
     * @return <code>Cluster</code> instance
     */
    public static Cluster createCluster(String username, String password, String hostPool) {
        Map<String, String> credentials =
                new HashMap<String, String>();
        credentials.put(USERNAME_KEY, username);
        credentials.put(PASSWORD_KEY, password);

        return HFactory.createCluster(CLUSTER_NAME,
                new CassandraHostConfigurator(hostPool), credentials);
    }
}
