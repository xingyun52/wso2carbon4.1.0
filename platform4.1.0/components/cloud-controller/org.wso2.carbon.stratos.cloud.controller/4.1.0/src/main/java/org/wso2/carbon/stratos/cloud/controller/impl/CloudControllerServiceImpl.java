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
package org.wso2.carbon.stratos.cloud.controller.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadata.Status;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.internal.NodeMetadataImpl;
import org.wso2.carbon.lb.common.conf.util.Constants;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskInfo.TriggerInfo;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.stratos.cloud.controller.consumers.TopologyBuilder;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;
import org.wso2.carbon.stratos.cloud.controller.exception.UnregisteredCartridgeException;
import org.wso2.carbon.stratos.cloud.controller.interfaces.CloudControllerService;
import org.wso2.carbon.stratos.cloud.controller.jcloud.ComputeServiceBuilderUtil;
import org.wso2.carbon.stratos.cloud.controller.persist.Deserializer;
import org.wso2.carbon.stratos.cloud.controller.publisher.CartridgeInstanceDataPublisherTask;
import org.wso2.carbon.stratos.cloud.controller.registry.RegistryManager;
import org.wso2.carbon.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.wso2.carbon.stratos.cloud.controller.topic.TopologySynchronizerTask;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.stratos.cloud.controller.util.Cartridge;
import org.wso2.carbon.stratos.cloud.controller.util.CartridgeInfo;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerServiceReferenceHolder;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerUtil;
import org.wso2.carbon.stratos.cloud.controller.util.IaasContext;
import org.wso2.carbon.stratos.cloud.controller.util.IaasProvider;
import org.wso2.carbon.stratos.cloud.controller.util.ServiceContext;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Cloud Controller Service is responsible for starting up new server instances,
 * terminating already
 * started instances, providing pending instance count etc.
 * 
 */
public class CloudControllerServiceImpl implements CloudControllerService {

	private static final Log log = LogFactory.getLog(CloudControllerServiceImpl.class);
	private FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

	public CloudControllerServiceImpl() {

		acquireData();

		if (dataHolder.getEnableBAMDataPublisher()) {

			// initialize and schedule the data publisher task
			TaskManager tm = null;
			TaskInfo taskInfo = null;

			try {

				if (!CloudControllerServiceReferenceHolder.getInstance()
				                                          .getTaskService()
				                                          .getRegisteredTaskTypes()
				                                          .contains(CloudControllerConstants.DATA_PUB_TASK_TYPE)) {

					CloudControllerServiceReferenceHolder.getInstance()
					                                     .getTaskService()
					                                     .registerTaskType(CloudControllerConstants.DATA_PUB_TASK_TYPE);

					tm =
					     CloudControllerServiceReferenceHolder.getInstance()
					                                          .getTaskService()
					                                          .getTaskManager(CloudControllerConstants.DATA_PUB_TASK_TYPE);

					if (!tm.isTaskScheduled(CloudControllerConstants.DATA_PUB_TASK_NAME)) {

						TriggerInfo triggerInfo =
						                          new TriggerInfo(
						                                          FasterLookUpDataHolder.getInstance()
						                                                                .getDataPublisherCron());
						taskInfo =
						           new TaskInfo(CloudControllerConstants.DATA_PUB_TASK_NAME,
						                        CartridgeInstanceDataPublisherTask.class.getName(),
						                        new HashMap<String, String>(), triggerInfo);
						tm.registerTask(taskInfo);
						// tm.rescheduleTask(AutoscalerConstant.DATA_PUB_TASK_NAME);
						// tm.scheduleTask(taskInfo.getName());
					}
				}

			} catch (Exception e) {
				String msg = "Error scheduling task: " + CloudControllerConstants.DATA_PUB_TASK_NAME;
				log.error(msg, e);
				if (tm != null) {
					try {
						tm.deleteTask(CloudControllerConstants.DATA_PUB_TASK_NAME);
					} catch (TaskException e1) {
						log.error(e1);
					}
				}
				throw new CloudControllerException(msg, e);
			}
		}

		if (dataHolder.getEnableTopologySync()) {
			
			// initialize TopologyBuilder Consumer
			Thread topologyConsumer =
					new Thread(
					           new TopologyBuilder(
					                               dataHolder.getSharedTopologyDiffQueue()));
			// start consumer
			topologyConsumer.start();

			TaskManager tm = null;
			try {

				if (!CloudControllerServiceReferenceHolder.getInstance()
				                                          .getTaskService()
				                                          .getRegisteredTaskTypes()
				                                          .contains(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE)) {
					// topology sync
					CloudControllerServiceReferenceHolder.getInstance()
					                                     .getTaskService()
					                                     .registerTaskType(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE);

					tm =
					     CloudControllerServiceReferenceHolder.getInstance()
					                                          .getTaskService()
					                                          .getTaskManager(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE);

					TriggerInfo triggerInfo =
					                          new TriggerInfo(
					                                          dataHolder.getTopologySynchronizerCron());
					TaskInfo taskInfo =
					                    new TaskInfo(CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME,
					                                 TopologySynchronizerTask.class.getName(),
					                                 new HashMap<String, String>(), triggerInfo);
					tm.registerTask(taskInfo);
				}

			} catch (Exception e) {
				String msg = "Error scheduling task: " + CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME;
				log.error(msg, e);
				if (tm != null) {
					try {
						tm.deleteTask(CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME);
					} catch (TaskException e1) {
						log.error(e1);
					}
				}
				throw new CloudControllerException(msg, e);
			}
		}
	}

	private void acquireData() {

		Object obj = RegistryManager.getInstance().retrieve();
		if (obj != null) {
			try {
				Object dataObj = Deserializer.deserializeFromByteArray((byte[]) obj);
				if (dataObj instanceof FasterLookUpDataHolder) {
					FasterLookUpDataHolder serializedObj = (FasterLookUpDataHolder) dataObj;
					FasterLookUpDataHolder currentData = FasterLookUpDataHolder.getInstance();
					
					// assign necessary data
					currentData.setNodeIdToServiceContextMap(serializedObj.getNodeIdToServiceContextMap());
					
					// traverse through current Service Contexts
					for (ServiceContext ctxt : currentData.getServiceCtxtList()) {
						// traverse through serialized Service Contexts
	                    for (ServiceContext serializedCtxt : serializedObj.getServiceCtxtList()) {
	                    	// if a matching Service Context found
	                        if(ctxt.equals(serializedCtxt)){
	                        	// persisted node ids of this Service Context
	                        	List<Object> nodeIds = serializedObj.getNodeIdsOfServiceCtxt(serializedCtxt);
	                        	for (Object nodeIdObj : nodeIds) {
	                                String nodeId = (String) nodeIdObj;
	                                
	                                // assign persisted data
	                                currentData.addNodeId(nodeId, ctxt);
	                                
                                }
	                        	
	                        	ctxt.setIaasContextMap(serializedCtxt.getIaasCtxts());
	                        	appendToPublicIpProperty(serializedCtxt.getProperty(CloudControllerConstants.PUBLIC_IP_PROPERTY), ctxt);
	                        	
	                        	// assign lastly used IaaS
	                        	if(serializedCtxt.getCartridge() != null && serializedCtxt.getCartridge().getLastlyUsedIaas() != null){
	                        		
	                        		if(ctxt.getCartridge() == null){
	                        			// load Cartridge
	                        			ctxt.setCartridge(loadCartridge(ctxt.getCartridgeType(), ctxt.getPayload()));
	                        		}
	                        		
	                        		IaasProvider serializedIaas = serializedCtxt.getCartridge().getLastlyUsedIaas();
	                        		ctxt.getCartridge().setLastlyUsedIaas(serializedIaas);
	                        		
	                        	}
	                        }
                        }
                    }
					
					log.debug("Data is retrieved from registry.");
				} else {
					log.debug("No data is persisted in registry.");
				}
			} catch (Exception e) {

				String msg =
				             "Unable to acquire data from Registry. Hence, any historical data will not get reflected.";
				log.warn(msg, e);
			}

		}
	}

	@Override
	public String startInstance(String domainName, String subDomainName) {

		ComputeService computeService;
		Template template;
		String ip;

		subDomainName = checkSubDomain(subDomainName);

		log.info("Starting new instance of domain : " + domainName + " and sub domain : " +
		         subDomainName);

		ServiceContext serviceCtxt =
		                             FasterLookUpDataHolder.getInstance()
		                                                   .getServiceContext(domainName,
		                                                                      subDomainName);

		if (serviceCtxt == null) {
			String msg =
			             "Not a registered service: domain - " + domainName + ", sub domain - " +
			                     subDomainName;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// load Cartridge
		serviceCtxt.setCartridge(loadCartridge(serviceCtxt.getCartridgeType(), serviceCtxt.getPayload()));
		
		if (serviceCtxt.getCartridge() == null) {
			String msg =
			             "There's no registered Cartridge found. Domain - " + domainName +
			                     ", sub domain - " + subDomainName;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}


		if (serviceCtxt.getCartridge().getIaases().isEmpty()) {
			String msg =
					"There's no registered IaaSes found for Cartridge type: " +
							serviceCtxt.getCartridge().getType();
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}
		
		// sort the IaasProviders according to scale up order
		Collections.sort(serviceCtxt.getCartridge().getIaases(),
		                 IaasProviderComparator.ascending(IaasProviderComparator.getComparator(IaasProviderComparator.SCALE_UP_SORT)));


		for (IaasProvider iaas : serviceCtxt.getCartridge().getIaases()) {

			iaas.getIaas().setDynamicPayload(iaas);

			// get the ComputeService
			computeService = iaas.getComputeService();

			// corresponding Template
			template = iaas.getTemplate();

			if (template == null) {
				String msg =
				             "Failed to start an instance in " +
				                     iaas.getType() +
				                     ". Reason : Template is null. You have not specify a matching service " +
				                     "element in the configuration file of Autoscaler.\n Hence, will try to " +
				                     "start in another IaaS if available.";
				log.error(msg);
				continue;
			}

			// set instance name as the host name
			// template.getOptions().userMetadata("Name",
			// serviceCtxt.getHostName());
			// template.getOptions().as(TemplateOptions.class).userMetadata("Name",
			// serviceCtxt.getHostName());

			// generate the group id from domain name and sub domain name.
			// Should have lower-case ASCII letters, numbers, or dashes.
			String str = domainName.concat("-" + subDomainName);
			String group = str.replaceAll("[^a-z0-9-]", "");

			try {
				// create and start a node
				Set<? extends NodeMetadata> nodes =
				                                    computeService.createNodesInGroup(group, 1,
				                                                                      template);

				NodeMetadata node = nodes.iterator().next();

				// allocate an IP address
				ip = iaas.getIaas().associateAddress(iaas, node);

				if (node.getId() == null) {
					String msg = "Node id of the starting instance is null.\n" + node.toString();
					log.fatal(msg);
					throw new CloudControllerException(msg);
				}

				// add node ID
				IaasContext ctxt = null;
				if ((ctxt = serviceCtxt.getIaasContext(iaas.getType())) == null) {
					ctxt = serviceCtxt.addIaasContext(iaas.getType());
				}
				ctxt.addNodeId(node.getId());
				ctxt.addNodeToPublicIp(node.getId(), ip);

				// to faster look up
				FasterLookUpDataHolder.getInstance().addNodeId(node.getId(), serviceCtxt);

				serviceCtxt.getCartridge().setLastlyUsedIaas(iaas);

				// add this ip to the topology
				appendToPublicIpProperty(ip, serviceCtxt);
				
				// persist in registry
				persist();

				// trigger topology consumers
				List<ServiceContext> list = new ArrayList<ServiceContext>();
				list.add(serviceCtxt);
				try {
					dataHolder.getSharedTopologyDiffQueue().put(list);
				} catch (InterruptedException ignore) {
				}

				if (log.isDebugEnabled()) {
					log.debug("Node details: \n" + node.toString() + "\n***************\n");
				}

			} catch (RunNodesException e) {
				log.warn("Failed to start an instance in " + iaas.getType() +
				         ". Hence, will try to start in another IaaS if available.", e);
				continue;
			}

			log.info("Instance is successfully starting up in IaaS " + iaas.getType() + " ...");

			return ip;
		}

		log.error("Failed to start an instance, in any available IaaS: " + domainName +
		          " and sub domain : " + subDomainName);

		return null;

	}

	/**
	 * Appends this ip to the Service Context's {@link CloudControllerConstants#PUBLIC_IP_PROPERTY}
     * @param ip
     * @param serviceCtxt
     */
	private void appendToPublicIpProperty(String ip, ServiceContext serviceCtxt) {
		String ipStr = serviceCtxt.getProperty(CloudControllerConstants.PUBLIC_IP_PROPERTY);
		if (ip != null && !"".equals(ip)) {
			serviceCtxt.setProperty(CloudControllerConstants.PUBLIC_IP_PROPERTY,
			                        ("".equals(ipStr) ? ""
			                                         : ipStr +
			                                           CloudControllerConstants.ENTRY_SEPARATOR) +
			                                ip);
		}
	}

	/**
     * Persist data in registry.
     */
    private void persist() {
	    try {
	        RegistryManager.getInstance().persist(FasterLookUpDataHolder.getInstance());
	    } catch (RegistryException e) {

	    	String msg = "Failed to persist the Cloud Controller data in registry. Further, transaction roll back also failed.";
	    	log.fatal(msg);
	    	throw new CloudControllerException(msg, e);
	    }
    }

	private Cartridge loadCartridge(String cartridgeType, byte[] payload) {

		for (Cartridge cartridge : FasterLookUpDataHolder.getInstance().getCartridges()) {
			if (cartridge.getType().equals(cartridgeType)) {
				for (IaasProvider iaas : cartridge.getIaases()) {
					iaas.setPayload(payload);
				}
				return cartridge;
			}
		}

		return null;
	}

	@Override
	public boolean terminateInstance(String domainName, String subDomainName) {

		subDomainName = checkSubDomain(subDomainName);

		log.info("Starting to terminate an instance of domain : " + domainName +
		         " and sub domain : " + subDomainName);

		ServiceContext serviceCtxt =
		                             FasterLookUpDataHolder.getInstance()
		                                                   .getServiceContext(domainName,
		                                                                      subDomainName);

		if (serviceCtxt == null) {
			String msg =
			             "Not a registered service: domain - " + domainName + ", sub domain - " +
			                     subDomainName;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// load Cartridge, if null
		if (serviceCtxt.getCartridge() == null) {
			serviceCtxt.setCartridge(loadCartridge(serviceCtxt.getCartridgeType(),
			                                       serviceCtxt.getPayload()));
		}
		
		// if still, Cartridge is null
		if (serviceCtxt.getCartridge() == null) {
			String msg =
			             "There's no registered Cartridge found. Domain - " + domainName +
			                     ", sub domain - " + subDomainName;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// sort the IaasProviders according to scale down order
		Collections.sort(serviceCtxt.getCartridge().getIaases(),
		                 IaasProviderComparator.ascending(IaasProviderComparator.getComparator(IaasProviderComparator.SCALE_DOWN_SORT)));

		// traverse in scale down order
		for (IaasProvider iaas : serviceCtxt.getCartridge().getIaases()) {

			String msg =
			             "Failed to terminate an instance in " + iaas.getType() +
			                     ". Hence, will try to terminate an instance in another IaaS if possible.";

			String nodeId = null;

			IaasContext ctxt = serviceCtxt.getIaasContext(iaas.getType());

			// terminate the last instance first
			for (String id : Lists.reverse(ctxt.getNodeIds())) {
				if (id != null) {
					nodeId = id;
					break;
				}
			}

			// if no matching node id can be found.
			if (nodeId == null) {

				log.warn(msg + " : Reason- No matching instance found for domain: " + domainName +
				         " and sub domain: " + subDomainName + ".");
				continue;
			}

			// terminate it!
			terminate(iaas, ctxt, nodeId);
			
			// log information
			logTermination(nodeId, ctxt, serviceCtxt);

			return true;

		}

		log.info("Termination of an instance which is belong to domain '" + domainName +
		         "' and sub domain '" + subDomainName + "' , failed! Reason: No matching " +
		         "running instance found in any available IaaS.");

		return false;

	}

	@Override
	public boolean terminateLastlySpawnedInstance(String domainName, String subDomainName) {

		subDomainName = checkSubDomain(subDomainName);

		log.info("Starting to terminate the last instance spawned, of domain : " + domainName +
		         " and sub domain : " + subDomainName);

		ServiceContext serviceCtxt =
		                             FasterLookUpDataHolder.getInstance()
		                                                   .getServiceContext(domainName,
		                                                                      subDomainName);

		if (serviceCtxt == null) {
			String msg =
			             "Not a registered service: domain - " + domainName + ", sub domain - " +
			                     subDomainName;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// load Cartridge, if null
		if (serviceCtxt.getCartridge() == null) {
			serviceCtxt.setCartridge(loadCartridge(serviceCtxt.getCartridgeType(),
			                                       serviceCtxt.getPayload()));
		}
		
		if (serviceCtxt.getCartridge() == null) {
			String msg =
			             "There's no registered Cartridge found. Domain - " + domainName +
			                     ", sub domain - " + subDomainName;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		IaasProvider iaas = serviceCtxt.getCartridge().getLastlyUsedIaas();
		// this is required since, we need to find the correct reference.
		// caz if the lastly used iaas retrieved from registry, it is not a reference.
		iaas = serviceCtxt.getCartridge().getIaasProvider(iaas.getType());

		if (iaas != null) {

			String nodeId = null;
			IaasContext ctxt = serviceCtxt.getIaasContext(iaas.getType());

			int i=0;
			for (i = ctxt.getNodeIds().size()-1; i >= 0 ; i--) {
	            String id = ctxt.getNodeIds().get(i);
				if (id != null) {
					nodeId = id;
					break;
				}
            }

			if (nodeId != null) {

				// terminate it!
				iaas = terminate(iaas, ctxt, nodeId);
				
				// log information
				logTermination(nodeId, ctxt, serviceCtxt);

				return true;
			}

		}

		log.info("Termination of an instance which is belong to domain '" + domainName +
		          "' and sub domain '" + subDomainName + "' , failed! Reason: No matching " +
		          "running instance found in lastly used IaaS.");

		return false;

	}

	@Override
	public boolean terminateAllInstances(String domainName, String subDomainName) {

		boolean isAtLeastOneTerminated = false;
		
		subDomainName = checkSubDomain(subDomainName);

		log.info("Starting to terminate all instances of domain : " + domainName +
		         " and sub domain : " + subDomainName);

		ServiceContext serviceCtxt =
		                             FasterLookUpDataHolder.getInstance()
		                                                   .getServiceContext(domainName,
		                                                                      subDomainName);

		if (serviceCtxt == null) {
			String msg =
			             "Not a registered service: domain - " + domainName + ", sub domain - " +
			                     subDomainName;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// load Cartridge, if null
		if (serviceCtxt.getCartridge() == null) {
			serviceCtxt.setCartridge(loadCartridge(serviceCtxt.getCartridgeType(),
			                                       serviceCtxt.getPayload()));
		}
		
		if (serviceCtxt.getCartridge() == null) {
			String msg =
			             "There's no registered Cartridge found. Domain - " + domainName +
			                     ", sub domain - " + subDomainName;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		// sort the IaasProviders according to scale down order
		Collections.sort(serviceCtxt.getCartridge().getIaases(),
		                 IaasProviderComparator.ascending(IaasProviderComparator.getComparator(IaasProviderComparator.SCALE_DOWN_SORT)));

		// traverse in scale down order
		for (IaasProvider iaas : serviceCtxt.getCartridge().getIaases()) {

			IaasContext ctxt = serviceCtxt.getIaasContext(iaas.getType());

			ArrayList<String> temp = new ArrayList<String>(ctxt.getNodeIds());
			for (String id : temp) {
				if (id != null) {
					// terminate it!
					terminate(iaas, ctxt, id);
					
					// log information
					logTermination(id, ctxt, serviceCtxt);
					
					isAtLeastOneTerminated = true;
				}
			}
		}
		
		if(isAtLeastOneTerminated){
			return true;
		}
		
		log.info("Termination of an instance which is belong to domain '" + domainName +
		          "' and sub domain '" + subDomainName + "' , failed! Reason: No matching " +
		          "running instance found in lastly used IaaS.");

		return false;

	}
	
	public int getPendingInstanceCount(String domainName, String subDomainName) {

		subDomainName = checkSubDomain(subDomainName);

		int pendingInstanceCount = 0;

		ServiceContext subjectedSerCtxt =
		                                  FasterLookUpDataHolder.getInstance()
		                                                        .getServiceContext(domainName,
		                                                                           subDomainName);

		if (subjectedSerCtxt != null && subjectedSerCtxt.getCartridgeType() != null) {
			
			// load cartridge
			subjectedSerCtxt.setCartridge(loadCartridge(subjectedSerCtxt.getCartridgeType(), subjectedSerCtxt.getPayload()));
			
			if(subjectedSerCtxt.getCartridge() == null){
				return pendingInstanceCount;
			}
			
			List<IaasProvider> iaases = subjectedSerCtxt.getCartridge().getIaases();
			
			for (IaasProvider iaas : iaases) {

				ComputeService computeService = iaas.getComputeService();

				IaasContext ctxt = null;
				if ((ctxt = subjectedSerCtxt.getIaasContext(iaas.getType())) == null) {
					ctxt = subjectedSerCtxt.addIaasContext(iaas.getType());
				}

				// get list of node Ids which are belong to this domain- sub
				// domain
				List<String> nodeIds = ctxt.getNodeIds();

				if (nodeIds.isEmpty()) {
					log.debug("Zero nodes spawned in the IaaS " + iaas.getType() + " of domain: " +
					          domainName + " and sub domain: " + subDomainName);
					continue;
				}

				// get all the nodes spawned by this IaasContext
				Set<? extends ComputeMetadata> set = computeService.listNodes();

				Iterator<? extends ComputeMetadata> iterator = set.iterator();

				// traverse through all nodes of this ComputeService object
				while (iterator.hasNext()) {
					NodeMetadataImpl nodeMetadata = (NodeMetadataImpl) iterator.next();

					// if this node belongs to the requested domain
					if (nodeIds.contains(nodeMetadata.getId())) {

						// get the status of the node
						Status nodeStatus = nodeMetadata.getStatus();

						// count nodes that are in pending state
						if (nodeStatus.equals(Status.PENDING)) {
							pendingInstanceCount++;
						}
					}

				}
			}
		}

		log.debug("Pending instance count of domain '" + domainName + "' and sub domain '" +
		          subDomainName + "' is " + pendingInstanceCount);

		return pendingInstanceCount;

	}

	
	/**
	 * A helper method to terminate an instance.
	 */
	private IaasProvider terminate(IaasProvider iaasTemp, IaasContext ctxt, String nodeId) {

		// this is just to be safe
		if (iaasTemp.getComputeService() == null) {
			String msg = "Unexpeced error occured! IaasContext's ComputeService is null!";
			log.error(msg);
			throw new CloudControllerException(msg);
		}

		// destroy the node
		iaasTemp.getComputeService().destroyNode(nodeId);

		// remove the node id
		ctxt.removeNodeId(nodeId);

		log.info("Node with Id: '" + nodeId + "' is terminated!");
		return iaasTemp;
	}
	
	private void logTermination(String nodeId, IaasContext ctxt, ServiceContext serviceCtxt) {

		// get the ip of the terminated node
		String ip = ctxt.getPublicIp(nodeId);
		String ipProp = CloudControllerConstants.PUBLIC_IP_PROPERTY;
		String ipStr = serviceCtxt.getProperty(ipProp);
		StringBuilder newIpStr = new StringBuilder("");

		for (String str : ipStr.split(CloudControllerConstants.ENTRY_SEPARATOR)) {
			if (!str.equals(ip)) {
				newIpStr.append(str + CloudControllerConstants.ENTRY_SEPARATOR);
			}
		}

		// add this ip to the topology
		serviceCtxt.setProperty(ipProp,
		                        newIpStr.length() == 0
		                                              ? ""
		                                              : newIpStr.substring(0, newIpStr.length() - 1)
		                                                        .toString());

		// remove the reference
		ctxt.removeNodeIdToPublicIp(nodeId);

		// persist
		persist();
		
		// trigger topology consumers
		List<ServiceContext> list = new ArrayList<ServiceContext>();
		list.add(serviceCtxt);
		try {
			dataHolder.getSharedTopologyDiffQueue().put(list);
		} catch (InterruptedException ignore) {
		}

	}

	/**
	 * Comparator to compare {@link IaasProvider} on different attributes.
	 */
	public enum IaasProviderComparator implements Comparator<IaasProvider> {
		SCALE_UP_SORT {
			public int compare(IaasProvider o1, IaasProvider o2) {
				return Integer.valueOf(o1.getScaleUpOrder()).compareTo(o2.getScaleUpOrder());
			}
		},
		SCALE_DOWN_SORT {
			public int compare(IaasProvider o1, IaasProvider o2) {
				return Integer.valueOf(o1.getScaleDownOrder()).compareTo(o2.getScaleDownOrder());
			}
		};

		public static Comparator<IaasProvider> ascending(final Comparator<IaasProvider> other) {
			return new Comparator<IaasProvider>() {
				public int compare(IaasProvider o1, IaasProvider o2) {
					return other.compare(o1, o2);
				}
			};
		}

		public static Comparator<IaasProvider> getComparator(final IaasProviderComparator... multipleOptions) {
			return new Comparator<IaasProvider>() {
				public int compare(IaasProvider o1, IaasProvider o2) {
					for (IaasProviderComparator option : multipleOptions) {
						int result = option.compare(o1, o2);
						if (result != 0) {
							return result;
						}
					}
					return 0;
				}
			};
		}
	}

	@Override
	public boolean registerService(String domain, String subDomain, String tenantRange,
	                               String cartridgeType, String hostName, byte[] payload)
	                                                                                     throws UnregisteredCartridgeException,
	                                                                                     CloudControllerException {

		// create a ServiceContext dynamically
		ServiceContext newServiceCtxt = new ServiceContext();
		newServiceCtxt.setDomainName(domain);
		newServiceCtxt.setSubDomainName(subDomain);
		newServiceCtxt.setTenantRange(tenantRange);
		newServiceCtxt.setHostName(hostName);
		newServiceCtxt.setCartridgeType(cartridgeType);

		for (Cartridge cartridge : FasterLookUpDataHolder.getInstance().getCartridges()) {
			if (cartridge.getType().equals(cartridgeType)) {
				newServiceCtxt.setCartridge(cartridge);
				break;
			}
		}

		if (newServiceCtxt.getCartridge() == null) {
			String msg = "Registration failed - Unregistered Cartridge type: " + cartridgeType;
			log.error(msg);
			throw new UnregisteredCartridgeException(msg);
		}

		if (payload != null && payload.length != 0) {
			// // replace User Data
			// for (IaasProvider iaas :
			// newServiceCtxt.getCartridge().getIaases()) {
			//
			// iaas.setPayload(payload);
			//
			// iaas.getIaas().setDynamicPayload(iaas);
			// }

			// write payload file
			try {
				String uniqueName = domain + "-" + subDomain + ".txt";
				FileUtils.forceMkdir(new File(CloudControllerConstants.PAYLOAD_DIR));
				File payloadFile = new File(CloudControllerConstants.PAYLOAD_DIR + uniqueName);
				FileUtils.writeByteArrayToFile(payloadFile, payload);
				newServiceCtxt.setPayloadFile(payloadFile.getPath());

			} catch (IOException e) {
				String msg =
				             "Failed while persisting the payload of domain : " + domain +
				                     ", sub domain : " + subDomain;
				log.error(msg, e);
				throw new CloudControllerException(msg, e);
			}

		} else {
			log.debug("Payload is null or empty for :\n "+newServiceCtxt.toNode().toString());
		}

		// FasterLookUpDataHolder.getInstance().addServiceContext(newServiceCtxt);

		// persist
		try {
			String uniqueName = domain + "-" + subDomain + "-" + UUID.randomUUID() + ".xml";
			FileUtils.writeStringToFile(new File(CloudControllerConstants.SERVICES_DIR + uniqueName),
			                            newServiceCtxt.toXml());
		} catch (IOException e) {
			String msg =
			             "Failed while persisting the service configuration - domain : " + domain +
			                     ", sub domain : " + subDomain + ", tenant range: " + tenantRange +
			                     ", cartridge type: " + cartridgeType;
			log.error(msg, e);
			throw new CloudControllerException(msg, e);
		}
		// CloudControllerUtil.persist(newServiceCtxt);

		// publish to the topic
		// ConfigurationPublisher.publish("cloud-controller-topology",
		// "Cloud Controller sends config details ....");

		log.info("Service successfully registered! Domain - " + domain + ", Sub domain - " +
		         newServiceCtxt.getSubDomainName() + ", Cartridge type - " + cartridgeType);

		return true;
	}

	@Override
	public String[] getRegisteredCartridges() {
		// get the list of cartridges registered
		List<Cartridge> cartridges = FasterLookUpDataHolder.getInstance().getCartridges();

		if (cartridges == null) {
			return new String[0];
		}

		String[] cartridgeTypes = new String[cartridges.size()];
		int i = 0;

		for (Cartridge cartridge : cartridges) {
			cartridgeTypes[i] = cartridge.getType();
			i++;
		}

		return cartridgeTypes;
	}

	@Override
	public boolean createKeyPairFromPublicKey(String cartridgeType, String keyPairName,
	                                          String publicKey) {

		Cartridge cartridge = FasterLookUpDataHolder.getInstance().getCartridge(cartridgeType);

		if (cartridge == null) {
			String msg = "Invalid Cartridge type specified : " + cartridgeType;
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		for (IaasProvider iaas : cartridge.getIaases()) {
			String region = ComputeServiceBuilderUtil.extractRegion(iaas);

			if (region == null) {
				String msg =
				             "Cannot find a region to create the key pair. Please add a property called 'region' under IaaS '" +
				                     iaas.getType() + "' of Cartridge - " + cartridgeType;
				log.fatal(msg);
				throw new CloudControllerException(msg);
			}

			return iaas.getIaas().createKeyPairFromPublicKey(iaas, region, keyPairName, publicKey);
		}

		return false;
	}

	@Override
	public String getCartridgeDescription(String cartridgeType)
	                                                           throws UnregisteredCartridgeException {

		Cartridge cartridge = FasterLookUpDataHolder.getInstance().getCartridge(cartridgeType);

		if (cartridge != null) {
			return cartridge.getDescription();
		}

		String msg =
		             "Cannot find a Cartridge having a type of " + cartridgeType +
		                     ". Hence unable to fetch a description.";
		log.error(msg);
		throw new UnregisteredCartridgeException(msg);
	}

	private String checkSubDomain(String subDomainName) {
		// if sub domain is null, we assume it as default one.
		if (subDomainName == null || "null".equalsIgnoreCase(subDomainName)) {
			subDomainName = Constants.DEFAULT_SUB_DOMAIN;
			log.debug("Sub domain is null, hence using the default value : " + subDomainName);
		}

		return subDomainName;
	}

	@Override
	public CartridgeInfo getCartridgeInfo(String cartridgeType)
	                                                           throws UnregisteredCartridgeException {
		Cartridge cartridge = FasterLookUpDataHolder.getInstance().getCartridge(cartridgeType);

		if (cartridge != null) {

			return CloudControllerUtil.toCartridgeInfo(cartridge);

		}

		String msg =
		             "Cannot find a Cartridge having a type of " + cartridgeType +
		                     ". Hence unable to find information.";
		log.error(msg);
		throw new UnregisteredCartridgeException(msg);
	}


}
