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

import org.wso2.carbon.user.core.multiplecredentials.Credential;
import org.wso2.carbon.user.core.multiplecredentials.MultipleCredentialsException;
import org.wso2.carbon.user.core.multiplecredentials.UserDoesNotExistException;

public class EmailCredential extends AbstractCassandraCredential {

    @Override
    public void add(String userId, Credential credential) throws MultipleCredentialsException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(String identifier, Credential newCredential)
            throws UserDoesNotExistException, MultipleCredentialsException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean authenticate(Credential credential) throws MultipleCredentialsException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Credential get(String identifier)
            throws UserDoesNotExistException, MultipleCredentialsException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
