/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package org.wso2.carbon.appfactory.repository.mgt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.common.util.AppFactoryUtil;
import org.wso2.carbon.appfactory.core.RevisionControlDriver;
import org.wso2.carbon.appfactory.core.RevisionControlDriverListener;
import org.wso2.carbon.appfactory.repository.mgt.client.AppfactoryRepositoryClient;
import org.wso2.carbon.appfactory.repository.mgt.internal.Util;

import java.io.File;

/**
 *
 */
public class AppfactoryRevisionControlDriver implements RevisionControlDriver {
    private static final Log log = LogFactory.getLog(AppfactoryRevisionControlDriver.class);
    private RepositoryManager repositoryManager;

    public AppfactoryRevisionControlDriver() {
        repositoryManager = new RepositoryManager();
    }

    public void getSource(String applicationId, String version, String revision,
                          RevisionControlDriverListener listener)
            throws AppFactoryException {
        String checkoutUrl = null;
        String repositoryType = Util.getConfiguration().getFirstProperty(AppFactoryConstants.PREFERRED_REPOSITORY_TYPE);
        try {
            checkoutUrl = repositoryManager.getAppRepositoryURL(applicationId, repositoryType);

        } catch (RepositoryMgtException e) {
            String msg = "Error while getting repository url";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }
        if ("svn".equals(repositoryType)) {
            if ("trunk".equals(version)) {
                checkoutUrl = (new StringBuilder()).append(checkoutUrl).append("/").append("trunk").toString();
            } else {
                checkoutUrl = (new StringBuilder()).append(checkoutUrl).append("/").append("branches").append("/").append(version).toString();
            }
        }
        File workingDirectory = AppFactoryUtil.getApplicationWorkDirectory(applicationId, version, revision);

        if (!workingDirectory.mkdir()) {
            String msg = "Error while creating working directory";
            log.error(msg);
            throw new AppFactoryException(msg);
        }

        AppfactoryRepositoryClient client = null;
        try {
            client = repositoryManager.getRepositoryProvider(repositoryType).getRepositoryClient();
            client.init(Util.getConfiguration().getFirstProperty("AdminUserName"), Util.getConfiguration().getFirstProperty("AdminPassword"));
            if ("svn".equals(repositoryType)) {
                client.checkOut(checkoutUrl, workingDirectory, revision);
            } else if ("git".endsWith(repositoryType)) {
                client.checkOutVersion(checkoutUrl, workingDirectory, version);
            }
            client.close();
        } catch (RepositoryMgtException e) {
            String msg = "Error while checking out repository ";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }
        listener.onGetSourceCompleted(applicationId, version, revision);
    }


}