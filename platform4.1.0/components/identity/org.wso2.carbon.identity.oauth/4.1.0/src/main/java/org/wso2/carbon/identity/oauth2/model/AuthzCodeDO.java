/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.identity.oauth2.model;

import org.wso2.carbon.caching.core.CacheEntry;

import java.sql.Timestamp;

/**
 * Results holder for Authz Code validation query
 */
public class AuthzCodeDO extends CacheEntry {

    private static final long serialVersionUID = 3308401412530535040L;

    private String authorizedUser;

    private String[] scope;

    private Timestamp issuedTime;

    private long validityPeriod;

    public AuthzCodeDO(String authorizedUser, String[] scope,
                       Timestamp issuedTime, long validityPeriod) {
        this.authorizedUser = authorizedUser;
        this.scope = scope;
        this.issuedTime = issuedTime;
        this.validityPeriod = validityPeriod;
    }

    public String getAuthorizedUser() {
        return authorizedUser;
    }

    public String[] getScope() {
        return scope;
    }

    public Timestamp getIssuedTime() {
        return issuedTime;
    }

    public long getValidityPeriod() {
        return validityPeriod;
    }
}
