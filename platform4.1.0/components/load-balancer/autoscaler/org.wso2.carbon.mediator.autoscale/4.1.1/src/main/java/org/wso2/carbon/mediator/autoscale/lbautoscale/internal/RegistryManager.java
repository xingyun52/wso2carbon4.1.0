/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.mediator.autoscale.lbautoscale.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscalerTaskDSHolder;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.DomainMapping;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;

public class RegistryManager {
    UserRegistry governanceRegistry = AutoscalerTaskDSHolder.getInstance().getGovernanceRegistry();
    private static final Log log = LogFactory.getLog(RegistryManager.class);
    /**
     *
     */
    private Resource resource = null;
    public static final String HOST_INFO = "hostinfo/";
    private static final String TENANT_CONTEXT = "tenant.context";
    private static final String APP = "app";
    private static final String APP_TYPE = "app.type";
    public static final String ACTUAL_HOST = "actual.host";

    public DomainMapping getMapping(String hostName) {
        DomainMapping domainMapping;
        try {
            if (governanceRegistry.resourceExists(HOST_INFO + hostName)) {
                resource = governanceRegistry.get(HOST_INFO + hostName);
                domainMapping = new DomainMapping(hostName);
                domainMapping.setActualHost(resource.getProperty(ACTUAL_HOST));
                domainMapping.setApp(resource.getProperty(APP));
                domainMapping.setAppType(resource.getProperty(APP_TYPE));
                domainMapping.setTenantContext(resource.getProperty(TENANT_CONTEXT));
                return domainMapping;
            }
        } catch (RegistryException e) {
            log.info("Error while getting registry resource");
            throw new RuntimeException(e);
        }
        return null;
    }
}
