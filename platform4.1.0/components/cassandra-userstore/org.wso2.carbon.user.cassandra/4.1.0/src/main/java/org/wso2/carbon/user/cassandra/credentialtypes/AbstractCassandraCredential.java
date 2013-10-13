package org.wso2.carbon.user.cassandra.credentialtypes;/*
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

import me.prettyprint.cassandra.serializers.BooleanSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;
import org.wso2.carbon.user.cassandra.Util;
import org.wso2.carbon.user.cassandra.CFConstants;
import org.wso2.carbon.user.core.multiplecredentials.Credential;
import org.wso2.carbon.user.core.multiplecredentials.CredentialType;
import org.wso2.carbon.user.core.multiplecredentials.UserDoesNotExistException;

public abstract class AbstractCassandraCredential implements CredentialType {

    protected Keyspace keyspace;
    protected Serializer<String> stringSerializer = StringSerializer.get();
    protected String credentialTypeName;

    public Keyspace getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(Keyspace keyspace) {
        this.keyspace = keyspace;
    }

    public String getExistingUserId(String identifier) throws UserDoesNotExistException {

        String existingUserId = Util.getExistingUserId(credentialTypeName, identifier, keyspace);
        if (existingUserId == null) {
            throw new UserDoesNotExistException("User not found for identifier : " + identifier);
        }
        return existingUserId;
    }

    @Override
    public String getCredentialTypeName() {
        return credentialTypeName;
    }

    @Override
    public void setCredentialTypeName(String credentialTypeName) {
        this.credentialTypeName = credentialTypeName;
    }

    protected String createRowKeyForReverseLookup(String identifier) {
        return Util.createRowKeyForReverseLookup(identifier, credentialTypeName);
    }

    @Override
    public void delete(Credential credential) throws UserDoesNotExistException {
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);

        String deviceId = credential.getIdentifier();
        String userId = getExistingUserId(deviceId);

        // delete credential off user row
        mutator.addDeletion(userId, CFConstants.USERS, credentialTypeName, stringSerializer);

        // delete reverse look up
        mutator.addDeletion(createRowKeyForReverseLookup(deviceId), CFConstants.USERNAME_INDEX, null, stringSerializer);

        mutator.execute();
    }

    @Override
    public void activate(String identifier) throws UserDoesNotExistException {
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);



        // set is active to true
        mutator.addInsertion(createRowKeyForReverseLookup(identifier),
                             CFConstants.USERNAME_INDEX,
                             HFactory.createColumn(CFConstants.IS_ACTIVE, true));
        mutator.execute();
    }

    @Override
    public void deactivate(String identifier) throws UserDoesNotExistException {
        Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);


        // set is active to false
        mutator.addInsertion(createRowKeyForReverseLookup(identifier),
                             CFConstants.USERNAME_INDEX,
                             HFactory.createColumn(CFConstants.IS_ACTIVE, false));
        mutator.execute();
    }

    @Override
    public boolean isActive(String identifier) throws UserDoesNotExistException {
        BooleanSerializer booleanSerializer = BooleanSerializer.get();
        ColumnQuery<String, String, Boolean> isActiveQuery = HFactory
                .createColumnQuery(keyspace, stringSerializer, stringSerializer, booleanSerializer);

        isActiveQuery.setColumnFamily(CFConstants.USERNAME_INDEX).setKey(createRowKeyForReverseLookup(identifier))
                .setName(CFConstants.IS_ACTIVE);

        QueryResult<HColumn<String, Boolean>> result = isActiveQuery.execute();

        HColumn<String, Boolean> isActiveResult = result.get();

        if (isActiveResult != null) {
            return isActiveResult.getValue();
        }

        return false;

    }
}
