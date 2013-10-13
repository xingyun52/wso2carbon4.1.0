/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.service;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.deployment.SynchronizeGitRepositoryRequest;
import org.wso2.carbon.adc.mgt.custom.domain.RegistryManager;
import org.wso2.carbon.adc.mgt.dao.CartridgeSubscription;
import org.wso2.carbon.adc.mgt.internal.DataHolder;
import org.wso2.carbon.adc.mgt.utils.CartridgeConstants;
import org.wso2.carbon.adc.mgt.utils.PersistenceManager;
import org.wso2.carbon.adc.topology.mgt.service.TopologyManagementService;
import org.wso2.carbon.utils.CarbonUtils;


public class RepoNotificationService {

	private static final Log log = LogFactory.getLog(RepoNotificationService.class);
	

	public void notifyRepoUpdate(String tenantDomain, String cartridgeType) throws Exception {

		log.info(" Repo is updated with tenant : " + tenantDomain + " , cartridge: " +
		         cartridgeType);

		CartridgeSubscription subscription =
		                                     PersistenceManager.getSubscription(tenantDomain,
		                                                                        cartridgeType);
		handleRepoSynch(subscription);

	}

	public void synchronize(String repositoryURL) throws Exception {

		log.info(" repository URL received : " + repositoryURL);
		List<CartridgeSubscription> subscription = PersistenceManager.getSubscription(repositoryURL);
		for (CartridgeSubscription cartridgeSubscription : subscription) {
			handleRepoSynch(cartridgeSubscription);   
        }
	}

	private void handleRepoSynch(CartridgeSubscription subscription) throws Exception {

		if (subscription != null && CartridgeConstants.PROVIDER_NAME_WSO2.equals(subscription.getProvider())) {
			log.info(" wso2 cartridge.. ");
			createAndSendClusterMessage(subscription.getTenantId(), subscription.getTenantDomain(),
			                            UUID.randomUUID(), subscription.getClusterDomain(),
			                            subscription.getClusterSubdomain());

		} else {

			// Query DB and get all the IP s for this tenant 
			// Invoke update-instance script
			
			String appPath = subscription.getBaseDirectory();
			String carbonHome = System.getProperty("carbon.home");
			String cartridgePrivateKey = carbonHome+"/id_rsa"; 

			if (subscription != null) {
				TopologyManagementService topologyMgtService = DataHolder.getTopologyMgtService();

				
				if (topologyMgtService == null) {
					String msg = " Topology Management Service is null ";
					log.error(msg);
					throw new Exception(msg);
				}

				String[] activeIpArray =
				                         topologyMgtService.getActiveIPs(subscription.getCartridge(),
				                                                         subscription.getClusterDomain(),
				                                                         subscription.getClusterSubdomain());

				try {

					for (String instanceIp : activeIpArray) {
						String command =
						                 CarbonUtils.getCarbonHome() + File.separator + "bin" +
						                         File.separator + "update-instance.sh " +
						                         instanceIp + " " + appPath + " " +
						                         cartridgePrivateKey + " /";
						log.debug("Update instance command.... " + command);
						Process proc = Runtime.getRuntime().exec(command);
						proc.waitFor();
					}

				} catch (Exception e) {
					log.error("Exception is occurred in notify update operation. Reason : " +
					          e.getMessage());
					throw e;
				}
			}
		}
	}

	private void createAndSendClusterMessage(int tenantId, String tenantDomain, UUID uuid,
	                                         String clusterDomain, String clusterSubdomain) {

		SynchronizeGitRepositoryRequest request =
		                                          new SynchronizeGitRepositoryRequest(tenantId,
		                                                                              tenantDomain,
		                                                                              uuid);

		ClusteringAgent clusteringAgent =
		                                  DataHolder.getServerConfigContext()
		                                            .getAxisConfiguration().getClusteringAgent();
		GroupManagementAgent groupMgtAgent =
		                                     clusteringAgent.getGroupManagementAgent(clusterDomain,
		                                                                             clusterSubdomain);

		try {
			log.info("Sending Request to.. " + clusterDomain + " : " + clusterSubdomain);
			groupMgtAgent.send(request);
			
		} catch (ClusteringFault e) {
			e.printStackTrace();
		}
		 

	}

}
