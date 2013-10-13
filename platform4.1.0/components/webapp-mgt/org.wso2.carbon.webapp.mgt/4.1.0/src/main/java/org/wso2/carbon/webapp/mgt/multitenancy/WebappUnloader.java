/*
 * Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.webapp.mgt.multitenancy;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.ArtifactUnloader;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.utils.deployment.GhostDeployer;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.carbon.webapp.mgt.DataHolder;
import org.wso2.carbon.webapp.mgt.TomcatGenericWebappsDeployer;
import org.wso2.carbon.webapp.mgt.WebApplication;
import org.wso2.carbon.webapp.mgt.WebApplicationsHolder;
import org.wso2.carbon.webapp.mgt.utils.GhostWebappDeployerUtils;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * The unloader used in lazy loading for unloading inactive webapps
 */
public class WebappUnloader implements ArtifactUnloader {
    private static final Log log = LogFactory.getLog(WebappUnloader.class);

    // Maximum allowed inactive time period for webapps. Default is set to 10 mins
    private static final long DEFAULT_MAX_INACTIVE_INTERVAL = 10;


    @Override
    public void unload() {
        ConfigurationContext mainConfigCtx = DataHolder.getServerConfigContext();
        if (mainConfigCtx == null) {
            return;
        }
        // iterate through all tenant config contexts
        Set<Map.Entry<String, ConfigurationContext>> ccEntries =
                TenantAxisUtils.getTenantConfigurationContexts(mainConfigCtx).entrySet();
        for (Map.Entry<String, ConfigurationContext> entry : ccEntries) {
            String tenantDomain = entry.getKey();
            unloadInactiveWebapps(entry.getValue(), tenantDomain);
        }
        // unload from super tenant as well..
        unloadInactiveWebapps(mainConfigCtx, "Super Tenant");
    }

    private void unloadInactiveWebapps(ConfigurationContext configCtx,
                                       String tenantDomain) {
        WebApplicationsHolder webApplicationsHolder = (WebApplicationsHolder)
                configCtx.getProperty(CarbonConstants.WEB_APPLICATIONS_HOLDER);
        int tenantId = MultitenantUtils.getTenantId(configCtx);

        try {
            if (tenantId > 0) {
                PrivilegedCarbonContext.startTenantFlow();
            }
            PrivilegedCarbonContext privilegedCarbonContext =
                    PrivilegedCarbonContext.getThreadLocalCarbonContext();
            privilegedCarbonContext.setTenantId(tenantId);
            privilegedCarbonContext.setTenantDomain(tenantDomain);

            if (webApplicationsHolder != null) {
                for (WebApplication webApplication :
                        webApplicationsHolder.getStartedWebapps().values()) {
                    if (!GhostWebappDeployerUtils.isGhostWebApp(webApplication)) {
                        Long lastUsageTime = Long.parseLong((String) webApplication.
                                getProperty(CarbonConstants.WEB_APP_LAST_USED_TIME));
                        if (lastUsageTime != null && isInactive(lastUsageTime)) {
                            GhostDeployer ghostDeployer = GhostWebappDeployerUtils.
                                    getGhostDeployer(configCtx.getAxisConfiguration());

                            DeploymentFileData webappFileData = ghostDeployer.
                                    getFileData(webApplication.getWebappFile().getPath());
                            log.info("Unloading actual webapp : " + webApplication.getWebappFile().
                                    getName() + " and adding Ghost webapp. Tenant Domain: " +
                                     tenantDomain);
                            // Adding this parameter to keep track of this webapp in GhostWebappDeployerValve
                            webApplication.setProperty(CarbonConstants.IS_ARTIFACT_BEING_UNLOADED,
                                                       "true");
                            Map<String, WebApplication> transitGhostList =
                                        GhostWebappDeployerUtils.getTransitGhostWebAppsMap(configCtx);
                                transitGhostList.put(webApplication.getContextName(), webApplication);
                            try {
                                TomcatGenericWebappsDeployer tomcatWebappDeployer = webApplication.
                                        getTomcatGenericWebappsDeployer();
                                tomcatWebappDeployer.undeploy(webApplication.getWebappFile());
                                File ghostFile = GhostWebappDeployerUtils.
                                        getGhostFile(webappFileData.getAbsolutePath(),
                                                     configCtx.getAxisConfiguration());
                                if (ghostFile.exists()) {
                                    WebApplication ghostWebapp = GhostWebappDeployerUtils.
                                            createGhostWebApp(ghostFile, webappFileData.getFile(),
                                                    webApplication.getTomcatGenericWebappsDeployer(),
                                                    configCtx);

                                    webApplicationsHolder.getStartedWebapps().
                                            put(webappFileData.getName(), ghostWebapp);
                                    webApplicationsHolder.getFaultyWebapps().
                                            remove(webappFileData.getName());
                                    transitGhostList.remove(ghostWebapp.getContextName());
                                }
                            } catch (Exception e) {
                                log.error("Error while unloading webapp : "
                                          + webApplication.getWebappFile().getName(), e);
                            }
                        }
                    }
                }
            }
        } finally {
            if (tenantId > 0) {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
    }

    private boolean isInactive(Long lastUsedTime) {
        long inactiveInterval = System.currentTimeMillis() - lastUsedTime;
        // set the default value
        long maxInactiveInterval = DEFAULT_MAX_INACTIVE_INTERVAL;
        // check the system property
        String property = System.getProperty(CarbonConstants.WEBAPP_IDLE_TIME);
        if (property != null) {
            maxInactiveInterval = Long.parseLong(property);
        }
        return inactiveInterval > maxInactiveInterval * 60 * 1000;
    }

}
