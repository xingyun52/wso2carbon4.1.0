/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.wso2.carbon.appfactory.svn.repository.provider;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryMgtException;
import org.wso2.carbon.appfactory.repository.mgt.client.AppfactoryRepositoryClient;
import org.wso2.carbon.appfactory.repository.mgt.internal.Util;
import org.wso2.carbon.appfactory.repository.provider.common.AbstractRepositoryProvider;
import org.wso2.carbon.appfactory.repository.provider.common.bean.Permission;
import org.wso2.carbon.appfactory.repository.provider.common.bean.PermissionType;
import org.wso2.carbon.appfactory.repository.provider.common.bean.Repository;

import java.io.IOException;
import java.util.ArrayList;

/**
 * SCM-manager specific repository manager implementation for svn
 */
public class SCMManagerBasedSVNRepositoryProvider extends AbstractRepositoryProvider {
    private static final Log log = LogFactory.getLog(SCMManagerBasedSVNRepositoryProvider.class);

    public static final String REST_GET_REPOSITORY_URI = "/repositories/svn/";

    public static final String TYPE = "svn";

    @Override
    public String createRepository(String applicationKey) throws RepositoryMgtException {

        HttpClient client = getClient();
        PostMethod post = new PostMethod(getServerURL() + REST_BASE_URI +
                                         REST_CREATE_REPOSITORY_URI);
        Repository repository = new Repository();
        repository.setName(applicationKey);
        repository.setType("svn");

        Permission permission = new Permission();
        permission.setGroupPermission(true);
        permission.setName(applicationKey);
        permission.setType(PermissionType.WRITE);
        ArrayList<Permission> permissions = new ArrayList<Permission>();
        permissions.add(permission);
        repository.setPermissions(permissions);

        post.setRequestEntity(new ByteArrayRequestEntity(getRepositoryAsString(repository)));
        post.setDoAuthentication(true);
        post.addRequestHeader("Content-Type", "application/xml;charset=UTF-8");

        String url;
        try {
            client.executeMethod(post);
        } catch (IOException e) {
            String msg = "Error while invoking the web service";
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        } finally {
            post.releaseConnection();
        }
        if (post.getStatusCode() == HttpStatus.SC_CREATED) {
            url = getAppRepositoryURL(applicationKey);
        } else {
            String msg = "Repository creation is failed for " + applicationKey + " server returned status " +
                         post.getStatusText();
            log.error(msg);
            throw new RepositoryMgtException(msg);
        }
        return url;
    }


    @Override
    public String getAppRepositoryURL(String applicationKey) throws RepositoryMgtException {
        HttpClient client = getClient();
        GetMethod get = new GetMethod(getServerURL() + REST_BASE_URI + REST_GET_REPOSITORY_URI
                                      + applicationKey);
        get.setDoAuthentication(true);
        get.addRequestHeader("Content-Type", "application/xml;charset=UTF-8");
        String repository = null;
        try {
            client.executeMethod(get);
            if (get.getStatusCode() == HttpStatus.SC_OK) {
                repository = getRepositoryFromStream(get.getResponseBodyAsStream()).getUrl();
            } else if (get.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                if (log.isDebugEnabled()) {
                    log.debug("Repository is not found " + applicationKey);
                }
            } else {
                String msg = "Repository action is failed for " + applicationKey +
                             " server returned status " + get.getStatusText();
                log.error(msg);
                throw new RepositoryMgtException(msg);
            }
        } catch (IOException e) {
            String msg = "Error while invoking the service";
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        } finally {
            HttpConnectionManager manager = client.getHttpConnectionManager();
            if (manager instanceof SimpleHttpConnectionManager) {
                ((SimpleHttpConnectionManager) manager).shutdown();
            }
        }
        return repository;
    }

//    No need to override. The abstract class has the implementation
/*    @Override
    public AppfactoryRepositoryClient getRepositoryClient() throws RepositoryMgtException {
        this.appfactoryRepositoryClient = null;
        this.appfactoryRepositoryClient = new AppfactoryRepositoryClient(getType());
        try {
            this.appfactoryRepositoryClient.init(Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME),
                                                 Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_PASSWORD));
        } catch (RepositoryMgtException e) {
            String msg = "Error while invoking the service";
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return this.appfactoryRepositoryClient;
    }*/

    @Override
    protected String getType() {
        return TYPE;
    }
}
