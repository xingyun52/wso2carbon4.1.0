package org.wso2.carbon.user.cassandra;/*
 *   Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.wso2.carbon.user.cassandra.CFConstants;
import org.wso2.carbon.user.core.multiplecredentials.Credential;

public class Util {

    public static String createRowKeyForReverseLookup(String identifier, String credentialTypeName) {
        return credentialTypeName + "::" + identifier;
    }

    public static String createRowKeyForReverseLookup(Credential credential) {
        return createRowKeyForReverseLookup(credential.getIdentifier(), credential.getCredentialsType());
    }

    public static String getExistingUserId(String credentialTypeName, String identifier,
                                           Keyspace keyspace) {

        identifier = createRowKeyForReverseLookup(identifier, credentialTypeName);
        Serializer<String> stringSerializer = StringSerializer.get();
        ColumnQuery<String, String, String> usernameIndexQuery = HFactory
                .createColumnQuery(keyspace, stringSerializer, stringSerializer, stringSerializer);

        usernameIndexQuery.setColumnFamily(CFConstants.USERNAME_INDEX).setKey(identifier)
                .setName(CFConstants.USER_ID);

        QueryResult<HColumn<String, String>> result = usernameIndexQuery.execute();

        HColumn<String, String> userIdCol = result.get();

        if (userIdCol == null) {
            return null;
        }

        return userIdCol.getValue();
    }
}
