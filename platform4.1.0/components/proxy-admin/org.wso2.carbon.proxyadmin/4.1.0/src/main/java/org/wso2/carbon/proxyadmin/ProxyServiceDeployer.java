/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.proxyadmin;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.mediation.initializer.ServiceBusConstants;
import org.wso2.carbon.mediation.initializer.ServiceBusUtils;
import org.wso2.carbon.mediation.initializer.persistence.MediationPersistenceManager;

import java.util.Properties;

/**
 *
 */
public class ProxyServiceDeployer extends org.apache.synapse.deployers.ProxyServiceDeployer {

	private static final Log log = LogFactory.getLog(ProxyAdminServiceComponent.class);

	  
    MediationPersistenceManager mpm;

    @Override
    public void init(ConfigurationContext configCtx) {
        //set tenantIDs for tenant-aware logging
        int tenantId = PrivilegedCarbonContext.getCurrentContext().getTenantId();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);

        super.init(configCtx);
        int retry =0;
        while(mpm == null || retry < 10){
            mpm = ServiceBusUtils.getMediationPersistenceManager(configCtx.getAxisConfiguration());
            if(mpm != null){
            	break;
            }
            try {
	            Thread.sleep(1000);
	            retry++;
            } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	           log.error("Error while initializing persistance engine");
            }
        }
     
    
    }

    @Override
    public String deploySynapseArtifact(OMElement artifactConfig, String fileName,
                                        Properties properties) {
        //set tenantIDs for tenant-aware logging
        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);

        String proxyName = super.deploySynapseArtifact(artifactConfig, fileName, properties);
        mpm.saveItemToRegistry(proxyName, ServiceBusConstants.ITEM_TYPE_PROXY_SERVICE);
        return proxyName;
    }

    @Override
    public String updateSynapseArtifact(OMElement artifactConfig, String fileName,
                                        String existingArtifactName, Properties properties) {
        //set tenantIDs for tenant-aware logging
        int tenantId = PrivilegedCarbonContext.getCurrentContext().getTenantId();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);

        String proxyName = super.updateSynapseArtifact(
                artifactConfig, fileName, existingArtifactName, properties);
        mpm.saveItemToRegistry(proxyName, ServiceBusConstants.ITEM_TYPE_PROXY_SERVICE);
        return proxyName;
    }

    @Override
    public void undeploySynapseArtifact(String artifactName) {
        //set tenantIDs for tenant-aware logging
        int tenantId = PrivilegedCarbonContext.getCurrentContext().getTenantId();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);

        super.undeploySynapseArtifact(artifactName);
        mpm.deleteItemFromRegistry(artifactName, ServiceBusConstants.ITEM_TYPE_PROXY_SERVICE);
    }

    @Override
    public void restoreSynapseArtifact(String artifactName) {
        //set tenantIDs for tenant-aware logging
        int tenantId = PrivilegedCarbonContext.getCurrentContext().getTenantId();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);

        super.restoreSynapseArtifact(artifactName);
        mpm.saveItemToRegistry(artifactName, ServiceBusConstants.ITEM_TYPE_PROXY_SERVICE);
    }
}
