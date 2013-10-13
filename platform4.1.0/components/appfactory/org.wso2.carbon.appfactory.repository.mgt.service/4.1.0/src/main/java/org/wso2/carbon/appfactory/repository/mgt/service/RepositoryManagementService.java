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

package org.wso2.carbon.appfactory.repository.mgt.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryManager;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryMgtException;
import org.wso2.carbon.appfactory.repository.mgt.client.AppfactoryRepositoryClient;
import org.wso2.carbon.appfactory.repository.mgt.internal.Util;
import org.wso2.carbon.core.AbstractAdmin;

/**
 *
 *
 */
public class RepositoryManagementService extends AbstractAdmin {
    private static final Log log = LogFactory.getLog(RepositoryManagementService.class);
    private RepositoryManager repositoryManager;

    public RepositoryManagementService() {
        this.repositoryManager = new RepositoryManager();
    }

    public String createRepository(String applicationKey, String type)
            throws RepositoryMgtException {
        return repositoryManager.createRepository(applicationKey, type);
    }

    public String getURL(String applicationKey, String type) throws RepositoryMgtException {
        return repositoryManager.getAppRepositoryURL(applicationKey, type);
    }

    public String getURLForAppVersion(String applicationKey, String version, String type)
            throws RepositoryMgtException {
        return repositoryManager.getURLForAppversion(applicationKey, version, type);
    }

    public void branch(String appId, String type, String currentVersion, String targetVersion,
                       String currentRevision) throws RepositoryMgtException {
        String sourceURL = getURL(appId, type);
        if (AppFactoryConstants.TRUNK.equals(currentVersion)) {
            sourceURL = sourceURL + "/" + currentVersion;
        } else {
            sourceURL = sourceURL + "/" + AppFactoryConstants.BRANCH + "/" + currentVersion;
        }
        AppfactoryRepositoryClient client = repositoryManager.getRepositoryClient(type);
        client.init(Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME),
                    Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_PASSWORD));
        client.branch(sourceURL, targetVersion, currentRevision);
        client.close();
    }

    public void tag(String appId, String type, String currentVersion, String targetVersion,
                    String currentRevision) throws AppFactoryException, RepositoryMgtException {
        String sourceURL = getURL(appId, type);
        if (AppFactoryConstants.TRUNK.equals(currentVersion)) {
            sourceURL = sourceURL + "/" + currentVersion;
        } else {
            sourceURL = sourceURL + "/" + AppFactoryConstants.BRANCH + "/" + currentVersion;
        }

        AppfactoryRepositoryClient client = repositoryManager.getRepositoryClient(type);
        client.init(Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME)
                , Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_PASSWORD));
        client.tag(sourceURL, targetVersion, currentRevision);
        client.close();

    }

}
