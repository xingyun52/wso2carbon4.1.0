/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.rssmanager.core.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.rssmanager.core.RSSManagerException;
import org.wso2.carbon.rssmanager.core.entity.RSSInstance;
import org.wso2.carbon.rssmanager.core.internal.dao.RSSDAOFactory;
import org.wso2.carbon.rssmanager.core.internal.util.RSSConfig;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

import java.util.List;

/**
 * This class loads the tenant specific data.
 */
public class RSSManagerAxis2ConfigContextObserver extends AbstractAxis2ConfigurationContextObserver {

    private static final Log log = LogFactory.getLog(RSSManagerAxis2ConfigContextObserver.class);

    public void createdConfigurationContext(ConfigurationContext configurationContext) {
        int tid = PrivilegedCarbonContext.getCurrentContext(configurationContext).getTenantId();
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tid);

            /* Initializing tenant RSS instance repository */
            this.initializeTenantRSSInstanceRepository(tid);
        } catch (Exception e) {
            log.error("Error occurred while loading tenant RSS configurations ", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    public void terminatingConfigurationContext(ConfigurationContext configurationContext) {
    }

    private void initializeTenantRSSInstanceRepository(int tid) throws RSSManagerException {
        List<RSSInstance> tenantOwnedInstances = null;
        try {
            RSSConfig.getInstance().getRssManager().beginTransaction();
            tenantOwnedInstances = RSSDAOFactory.getRSSDAO().getAllRSSInstances(tid);
            RSSConfig.getInstance().getRssManager().endTransaction();
        } catch (RSSManagerException e) {
            if (RSSConfig.getInstance().getRssManager().isInTransaction()) {
                RSSConfig.getInstance().getRssManager().rollbackTransaction();
            }
            throw e;
        }
        TenantRSSInstanceRepository repository =
                RSSConfig.getInstance().getRssManager().getRSSInstancePool().
                        getTenantRSSRepository(tid);
        if (repository == null) {
            repository = new TenantRSSInstanceRepository();
        }
        for (RSSInstance rssInstance : tenantOwnedInstances) {
            repository.addRSSInstance(rssInstance);
        }
        RSSConfig.getInstance().getRssManager().getRSSInstancePool().
                setTenantRSSRepository(tid, repository);
    }


}
