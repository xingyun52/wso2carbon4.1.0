/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.appfactory.issuetracking.redmine;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.application.mgt.service.UserInfoBean;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.issuetracking.exception.IssueTrackerException;
import org.wso2.carbon.appfactory.issuetracking.internal.ServiceContainer;

import java.io.IOException;

/**
 *
 *
 */
public final class RedmineUserUtil {
    private static final Log log = LogFactory.getLog(RedmineUserUtil.class);

    public static boolean addUser(UserInfoBean user, int authenticatorId)
            throws IssueTrackerException {
        HttpClient client = new HttpClient();
        AppFactoryConfiguration configuration = ServiceContainer.getAppFactoryConfiguration();
        String userName = configuration.getFirstProperty(AppFactoryRedmineIssueTrackerConnector.
                                                                 REDMINE_ADMIN_USERNAME);
        String password = configuration.getFirstProperty(AppFactoryRedmineIssueTrackerConnector.
                                                                 REDMINE_ADMIN_PASSWORD);
        String url = configuration.getFirstProperty(AppFactoryRedmineIssueTrackerConnector.REDMINE_URL);
        AuthScope authScope = AuthScope.ANY;
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);
        client.getState().setCredentials(authScope, credentials);
        PostMethod post = new PostMethod(url + "/users.xml");
        post.setDoAuthentication(true);
        post.addRequestHeader("Content-Type", "application/xml;charset=UTF-8");
        String payload =
                "<user>" +
                "  <login>username</login>" +
                "  <firstname>fName</firstname>" +
                "  <lastname>lName</lastname>" +
                "  <password></password>" +
                "  <mail>mail-address</mail>" +
                "  <auth_source_id>auth_id</auth_source_id>" +
                "</user>";
        payload = payload.replace("username", user.getUserName());
        payload = payload.replace("fName", user.getFirstName());
        payload = payload.replace("lName", user.getLastName());
        payload = payload.replace("mail-address", user.getEmail());
        payload = payload.replace("auth_id", String.valueOf(authenticatorId));
        post.setRequestEntity(new ByteArrayRequestEntity(payload.getBytes()));
        try {
            client.executeMethod(post);
        } catch (IOException e) {
            String msg = "Error while adding the user to Redmie " + user.getUserName();
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }

        return post.getStatusCode() == HttpStatus.SC_CREATED;
    }
}
