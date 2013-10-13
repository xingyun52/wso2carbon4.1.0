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

package org.wso2.carbon.appfactory.git.repository.provider;

import com.gitblit.Constants;
import com.gitblit.models.RepositoryModel;
import com.gitblit.utils.RpcUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryMgtException;
import org.wso2.carbon.appfactory.repository.provider.common.AbstractRepositoryProvider;

import java.io.IOException;

/**
 * GITBlit specific repository manager implementation for git
 */
public class GITBlitBasedGITRepositoryProvider extends AbstractRepositoryProvider {
    private static final Log log = LogFactory.getLog(GITBlitBasedGITRepositoryProvider.class);

    public static final String BASE_URL = "RepositoryProviderConfig.git.Property.BaseURL";
    public static final String GITBLIT_ADMIN_USERNAME =
            "RepositoryProviderConfig.git.Property.GitblitAdminUserName";
    public static final String GITBLIT_ADMIN_PASS =
            "RepositoryProviderConfig.git.Property.GitblitAdminPassword";
    public static final String REPO_TYPE = "git";

    private boolean isCreated = true;

    public static final String TYPE = "git";

    @Override
    public String createRepository(String applicationKey) throws RepositoryMgtException {
        String repoCreateUrl = config.getFirstProperty(BASE_URL) + "rpc?req=CREATE_REPOSITORY&name="
                               + applicationKey;
        String adminUsername = config.getFirstProperty(GITBLIT_ADMIN_USERNAME);
        String adminPassword = config.getFirstProperty(GITBLIT_ADMIN_PASS);

        //Create the gitblit repository model
        RepositoryModel model = new RepositoryModel();
        model.name = applicationKey;
        //authenticated users can clone, push and view the repository
        model.accessRestriction = Constants.AccessRestrictionType.VIEW;
        log.info(repoCreateUrl);

        try {
            isCreated = RpcUtils.createRepository(model, repoCreateUrl, adminUsername,
                    adminPassword.toCharArray());
            if (isCreated) {
                String url = getAppRepositoryURL(applicationKey);
                log.info(url);
                return url;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getAppRepositoryURL(String applicationKey) throws RepositoryMgtException {
        return config.getFirstProperty(BASE_URL) + REPO_TYPE + "/" + applicationKey + ".git";
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
