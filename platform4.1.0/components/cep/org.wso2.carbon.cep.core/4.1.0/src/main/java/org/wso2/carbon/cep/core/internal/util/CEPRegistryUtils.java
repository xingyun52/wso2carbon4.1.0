/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.cep.core.internal.util;

import org.wso2.carbon.cep.core.internal.ds.CEPServiceValueHolder;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.utils.RegistryUtils;

public class CEPRegistryUtils {

    private static String CONFIG_REGISTRY="config-registry";
    public static String getResource(String path) {
        Registry registry = null;
        try {
            registry = CEPServiceValueHolder.getInstance().getRegistry(CarbonContext.getCurrentContext().getTenantId());

            path = path.trim().replaceFirst(CONFIG_REGISTRY+":", "");
            Resource resource = registry.get(path);
            if (resource == null) {
                throw new RuntimeException("Cannot find resource in registry at : " + path);
            }
            return (RegistryUtils.decodeBytes((byte[]) resource.getContent())).toString();
        } catch (RegistryException e) {
            throw new RuntimeException("Cannot load resource " + path + " from registry, " + e.getMessage(), e);
        }
    }

    public static boolean isRegistryPath(String path) {
        return path.trim().startsWith(CONFIG_REGISTRY+":");
    }
}
