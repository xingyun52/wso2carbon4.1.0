/*
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.adc.topology.mgt.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration.ServiceConfiguration;
import org.wso2.carbon.lb.common.conf.util.LoadBalancerConfigUtil;
import org.wso2.carbon.adc.topology.mgt.service.TopologyManagementService;
import org.wso2.carbon.adc.topology.mgt.util.ConfigHolder;

public class TopologyManagementServiceImpl implements TopologyManagementService {
	
	ConfigHolder data = ConfigHolder.getInstance();

	@Override
	public String[] getDomains(String cartridgeType, int tenantId) {
		List<String> domains = new ArrayList<String>();
		if(data.getServiceConfigs() != null){
			List<ServiceConfiguration> serviceConfigs = data.getServiceConfigs().get(cartridgeType);
			if(serviceConfigs != null){
				
				for (ServiceConfiguration serviceConfiguration : serviceConfigs) {
	                
					List<Integer> tenantIds = LoadBalancerConfigUtil.getTenantIds(serviceConfiguration.getTenantRange());
					if(!tenantIds.isEmpty() && (tenantIds.contains(tenantId) || tenantIds.contains(0))){
						if (!domains.contains(serviceConfiguration.getDomain())) {
							domains.add(serviceConfiguration.getDomain());
						}
					}
                }
			}
		}
		return domains.toArray(new String[domains.size()]);
	}

	@Override
	public String[] getSubDomains(String cartridgeType, int tenantId) {
		List<String> subDomains = new ArrayList<String>();
		if(data.getServiceConfigs() != null){
			List<ServiceConfiguration> serviceConfigs = data.getServiceConfigs().get(cartridgeType);
			if(serviceConfigs != null){
				
				for (ServiceConfiguration serviceConfiguration : serviceConfigs) {
	                
					List<Integer> tenantIds = LoadBalancerConfigUtil.getTenantIds(serviceConfiguration.getTenantRange());
					if(!tenantIds.isEmpty() && (tenantIds.contains(tenantId) || tenantIds.contains(0))){
						if (!subDomains.contains(serviceConfiguration.getSubDomain())) {
							subDomains.add(serviceConfiguration.getSubDomain());
						}
					}
                }
			}
		}
		return subDomains.toArray(new String[subDomains.size()]);
	}

//	@Override
//	public String[] getActiveIPs(String cartridgeType, int tenantId) {
//		List<String> publicIps = new ArrayList<String>();
//		if(data.getServiceConfigs() != null){
//			List<ServiceConfiguration> serviceConfigs = data.getServiceConfigs().get(cartridgeType);
//			if(serviceConfigs != null){
//				
//				for (ServiceConfiguration serviceConfiguration : serviceConfigs) {
//	                
//					List<Integer> tenantIds = LoadBalancerConfigUtil.getTenantIds(serviceConfiguration.getTenantRange());
//					if(!tenantIds.isEmpty() && tenantIds.contains(tenantId)){
//						String ipStr = serviceConfiguration.getPublicIp();
//						if(ipStr != null && !ipStr.isEmpty()){
//							for (String ip : ipStr.split(",")) {
//								if (!publicIps.contains(ip)) {
//									publicIps.add(ip);
//								}
//                            }
//						}
//					}
//                }
//			}
//		}
//		return publicIps.toArray(new String[publicIps.size()]);
//	}

	@Override
    public String[] getActiveIPs(String cartridgeType, String domain, String subDomain) {
		List<String> publicIps = new ArrayList<String>();
		
		if(domain == null || subDomain == null){
			return new String[0];
		}
		
		if(data.getServiceConfigs() != null){
			List<ServiceConfiguration> serviceConfigs = data.getServiceConfigs().get(cartridgeType);
			if(serviceConfigs != null){
				
				for (ServiceConfiguration serviceConfiguration : serviceConfigs) {
	                
					if(domain.equals(serviceConfiguration.getDomain()) && subDomain.equals(serviceConfiguration.getSubDomain())){
						
						String ipStr = serviceConfiguration.getPublicIp();
						if(ipStr != null && !ipStr.isEmpty()){
							for (String ip : ipStr.split(",")) {
								if (!publicIps.contains(ip)) {
									publicIps.add(ip);
								}
							}
						}
					}
                }
			}
		}
		return publicIps.toArray(new String[publicIps.size()]);
	}

}
