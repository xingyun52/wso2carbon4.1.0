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

package org.wso2.carbon.appfactory.registry.handler;


import org.wso2.carbon.appfactory.registry.handler.utils.Utils;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.handlers.Handler;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;

/**
 * This is an handler to handle the reference paths prom applications.
 * The configuration elements should be as follows
 *
 * <handler class="org.wso2.carbon.appfactory.registry.handler.ReferenceHandler">
 *     <property name="systemVariable">mount.point</property>
 *      <filter class="org.wso2.carbon.registry.core.jdbc.handlers.filters.URLMatcher">
 *          <property name="pattern">/_system/governance/.*</property>
 *      </filter>
 * </handler>
 */
public class ReferenceHandler extends Handler {
    @Override
    public Resource get(RequestContext requestContext) throws RegistryException {
        String path = requestContext.getResourcePath().getPath();
        Registry registry = requestContext.getRegistry();

        String mountPoint = System.getProperty(getSystemVariable());

        String newPath = path.replace(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH,
                RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + RegistryConstants.PATH_SEPARATOR + mountPoint);

        if (registry.resourceExists(newPath)) {
            requestContext.setProcessingComplete(true);
            return registry.get(newPath);
        }

        return requestContext.getResource();
    }

    public void setSystemVariable(String systemVariable) throws RegistryException {
        Utils.setSystemVariable(systemVariable);
    }

    public String getSystemVariable() throws RegistryException {
        return Utils.getSystemVariable();
    }
}
