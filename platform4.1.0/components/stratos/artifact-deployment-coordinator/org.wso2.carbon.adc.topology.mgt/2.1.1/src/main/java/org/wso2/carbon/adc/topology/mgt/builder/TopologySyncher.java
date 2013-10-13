/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.adc.topology.mgt.builder;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration.ServiceConfiguration;
import org.wso2.carbon.lb.common.conf.structure.Node;
import org.wso2.carbon.lb.common.conf.structure.NodeBuilder;
import org.wso2.carbon.adc.topology.mgt.group.mgt.GroupMgtAgentBuilder;
import org.wso2.carbon.adc.topology.mgt.util.ConfigHolder;


public class TopologySyncher implements Runnable {

	@SuppressWarnings("rawtypes")
    private BlockingQueue sharedQueue;
	private static final Log log = LogFactory.getLog(TopologySyncher.class);
	
	public TopologySyncher(@SuppressWarnings("rawtypes") BlockingQueue queue){
		
		sharedQueue = queue;
		
	}
	
	@Override
	public void run() {

	    LoadBalancerConfiguration lbconfig = LoadBalancerConfiguration.getInstance();
	    
	    //FIXME Currently there has to be at least one dummy cluster defined in the loadbalancer conf
	    // in order to proper initialization of TribesClusteringAgent.
	    generateGroupMgtAgents(lbconfig);
	    
		while (true) {
            try {

                Object obj;
                String msg = null;

                obj = sharedQueue.take();
                msg = (String) obj;

                ConfigHolder data = ConfigHolder.getInstance();

//                if (msg != null &&
//                    (data.getPreviousMessage() == null || !data.getPreviousMessage().equals(msg))) {

//                    data.setPreviousMessage(msg);

                    Node topologyNode = NodeBuilder.buildNode(msg);

//                    lbconfig.resetData();
                    lbconfig.createServicesConfig(topologyNode);

                    data.setServiceConfigs(lbconfig.getServiceNameToServiceConfigurations());
//                }

                // TODO performance improvement - later - Nirmal
                // get the diff
                // MapDifference<String, List<ServiceConfiguration>> diff =
                // Maps.difference(data.getServiceConfigs() == null
                // ? new HashMap<String, List<ServiceConfiguration>>()
                // : data.getServiceConfigs(),
                // lbconfig.getServiceNameToServiceConfigurations());
                //
                // data.setServiceConfigs(new HashMap<String,
                // List<ServiceConfiguration>>(lbconfig.getServiceNameToServiceConfigurations()));

                // for (List<ServiceConfiguration> serviceConfigsList :
                // diff.entriesOnlyOnRight()
                // .values()) {
                generateGroupMgtAgents(lbconfig);
                // }

            } catch (InterruptedException ignore) {
			}
		}

	}

    /**
     * @param lbconfig
     */
    private void generateGroupMgtAgents(LoadBalancerConfiguration lbconfig) {
        for (List<ServiceConfiguration> serviceConfigsList : lbconfig.getServiceNameToServiceConfigurations()
                                                                     .values()) {

        	for (ServiceConfiguration serviceConfiguration : serviceConfigsList) {
        		GroupMgtAgentBuilder.createGroupMgtAgent(serviceConfiguration.getDomain(),
        		                                         serviceConfiguration.getSubDomain());
        		// GroupMgtAgentBuilder.createGroupMgtAgents();
        	}
        }
    }

}
