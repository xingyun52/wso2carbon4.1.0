/*
*  Copyright (c)  WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.entitlement.policy.finder.registry;

import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.handlers.Handler;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;

/**
 *  Registry policy handler
 */
public class RegistryPolicyHandler extends Handler {

    @Override
    public void put(RequestContext requestContext) throws RegistryException {
       
        new RegistryPolicyFinderModule().clearCache();
        super.put(requestContext);     
    }

    @Override
    public void delete(RequestContext requestContext) throws RegistryException {

        new RegistryPolicyFinderModule().clearCache();
        super.delete(requestContext);
    }
}