/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.ss.cassandra.test;

import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentVariables;

import java.rmi.RemoteException;

public class CassandraTestHelper {
    private EnvironmentVariables ssServer;
    private UserInfo userInfo;
    private String backendUrl;
    private String sessionCookie;
    private String serviceeUrl;

    public void initialize(int userID)
            throws LoginAuthenticationExceptionException, RemoteException {
        EnvironmentBuilder builder;
        userInfo = UserListCsvReader.getUserInfo(userID);
        builder = new EnvironmentBuilder().ss(userID);
        ssServer = builder.build().getSs();
        backendUrl=ssServer.getBackEndUrl();
        sessionCookie=ssServer.getSessionCookie();
        serviceeUrl=ssServer.getServiceUrl();
    }

    public EnvironmentVariables getSsServer() {
        return ssServer;
    }

    public void setSsServer(EnvironmentVariables ssServer) {
        this.ssServer = ssServer;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public void setBackendUrl(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public String getSessionCookie() {
        return sessionCookie;
    }

    public void setSessionCookie(String sessionCookie) {
        this.sessionCookie = sessionCookie;
    }

    public String getServiceeUrl() {
        return serviceeUrl;
    }

    public void setServiceeUrl(String serviceeUrl) {
        this.serviceeUrl = serviceeUrl;
    }
}
