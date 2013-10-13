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

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.KeyIterator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

import java.util.Iterator;

public class KSUtils {

    private static Cluster cluster;
    private static StringSerializer stringSerializer = StringSerializer.get();

    public static void getKeyCount(Keyspace keyspace, String columnFamilyName){

        RangeSlicesQuery<String, String, String> rangeSlicesQuery =
                HFactory.createRangeSlicesQuery(keyspace, stringSerializer, stringSerializer, stringSerializer);
        KeyIterator<String> keyIterator = new KeyIterator<String>(keyspace,columnFamilyName,stringSerializer);
        long keyCount = 0;
        for(Iterator<String> iterator = keyIterator.iterator(); keyIterator.iterator().hasNext();){
            System.out.println(iterator.next());
            keyCount ++;
        }
        System.out.println( "Key count: " + keyCount);
    }
}
