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

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axis2.AxisFault;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.adc.mgt.client.CartridgeAgentClient;
import org.wso2.carbon.adc.mgt.client.CloudControllerServiceClient;
import org.wso2.carbon.adc.mgt.custom.domain.RegistryManager;
import org.wso2.carbon.adc.mgt.dao.CartridgeSubscription;
import org.wso2.carbon.adc.mgt.dao.DataCartridge;
import org.wso2.carbon.adc.mgt.dao.PortMapping;
import org.wso2.carbon.adc.mgt.dao.Repository;
import org.wso2.carbon.adc.mgt.dns.DNSManager;
import org.wso2.carbon.adc.mgt.dto.Cartridge;
import org.wso2.carbon.adc.mgt.exception.ADCException;
import org.wso2.carbon.adc.mgt.internal.DataHolder;
import org.wso2.carbon.adc.mgt.internal.MySQLPasswordConfigurer;
import org.wso2.carbon.adc.mgt.utils.CartridgeConstants;
import org.wso2.carbon.adc.mgt.utils.PersistenceManager;
import org.wso2.carbon.adc.mgt.utils.RepositoryCreator;
import org.wso2.carbon.adc.mgt.utils.RepositoryFactory;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;
import org.wso2.carbon.stratos.cloud.controller.util.xsd.CartridgeInfo;
import org.wso2.carbon.stratos.cloud.controller.util.xsd.Properties;
import org.wso2.carbon.stratos.cloud.controller.util.xsd.Property;
import org.wso2.carbon.adc.topology.mgt.service.TopologyManagementService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 *
 *
 *
 */
public class ApplicationManagementService extends AbstractAdmin {

	
	private static final Log log = LogFactory.getLog(ApplicationManagementService.class);
	RegistryManager registryManager = new RegistryManager();
                                             
    PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
    String tenantDomain = carbonContext.getTenantDomain();    
   
    private static ExecutorService repoCreationExecutor = Executors.newSingleThreadExecutor();
    
	/**
	 * 
	 * @param ip
	 * @param password
	 * @return
	 * @throws AxisFault
	 */
	public boolean setMySqlPassword(String ip, String password) {
		
		return false;
	}

	/**
	 * @throws AxisFault
	 * 
	 * 
	 */
	public Cartridge listCartridgeInfo(String alias) throws ADCException, AxisFault {

		return doListCartridgeInfo(alias);
	}
	
	private Cartridge doListCartridgeInfo(String alias) throws ADCException, AxisFault {
		
		Cartridge cartridge = new Cartridge();
        log.info("Alias: " + alias);
        if(alias == null) {
            String msg = "Provided alias is empty";
            log.error(msg);
            throw new AxisFault("Alias you provided is empty.");
        }

        CartridgeSubscription sub;
        try {
            sub = PersistenceManager.getSubscription(tenantDomain, alias);
        } catch (Exception e) {
            String msg = " Exception is occurred, Reason: " + e.getMessage();
            log.error(msg, e);
            throw new ADCException("An error occurred while getting cartridge information ", e);
        }
        if(sub.getCartridge() == null) {
            String msg = "Info request for not subscribed cartridge";
            log.error(msg);
            throw new ADCException("You have not subscribed to " + alias);
        }

        log.info("Cluster domain : " + sub.getClusterDomain() + " cartridge: " + sub.getCartridge());


        TopologyManagementService topologyMgtService = DataHolder.getTopologyMgtService();

        String[] ips =
                      topologyMgtService.getActiveIPs(sub.getCartridge(),
                                                      sub.getClusterDomain(),
                                                      sub.getClusterSubdomain());
        populateCartridgeInfo(cartridge, sub, ips);
		return cartridge;
	}

	private void populateCartridgeInfo(Cartridge cartridge, CartridgeSubscription sub, String[] ips) throws AxisFault {
	    //cartridge.setVersion("1.0.0");
	    if (ips != null && ips.length > 0) {

	    	if (CartridgeConstants.MYSQL_CARTRIDGE_NAME.equals(sub.getCartridge())) {
	    		cartridge.setIp(ips[0]);
				if (sub.getDataCartridge() != null) {
                    cartridge.setDbUserName(sub.getDataCartridge().getUserName());
				}
	    		cartridge.setVersion("5.5");
	    		
	    		if(isMySqlActive(ips[0], 3306)) {
	    			cartridge.setActiveInstances(1);
					cartridge.setStatus(CartridgeConstants.ACTIVE);
	    		} else {
	    			cartridge.setActiveInstances(0);
					cartridge.setStatus(CartridgeConstants.NOT_READY);
	    		}
	    	} else {
				if (sub.getProvider().equals(CartridgeConstants.PROVIDER_NAME_WSO2)) { // TODO refactor logic for carbon cartridges.
					cartridge.setStatus(CartridgeConstants.ACTIVE);
					cartridge.setActiveInstances(ips.length);
				} else {
					Map<String, String> instanceIpMap =
					                                    PersistenceManager.getCartridgeInstanceInfo(ips,
					                                                                                sub.getClusterDomain(),
					                                                                                sub.getClusterSubdomain());
					cartridge.setActiveInstances(Collections.frequency(instanceIpMap.values(),
					                                                   CartridgeConstants.ACTIVE));
					cartridge.setStatus(checkCartridgeStatus(instanceIpMap));
				}
	    	}
	    } else {
	    	log.warn(" IP s have not returned through Topology Management");
	    	cartridge.setStatus(CartridgeConstants.SUBSCRIBED);
	    }
	    cartridge.setCartridgeName(sub.getName());
	    cartridge.setCartridgeType(sub.getCartridge());
	    cartridge.setHostName(sub.getHostName());
	    cartridge.setMaxInstanceCount(sub.getMaxInstances());
	    cartridge.setMinInstanceCount(sub.getMinInstances());
	    cartridge.setProvider(sub.getProvider());
	    if (sub.getRepository() != null) {
	    	cartridge.setRepoURL(convertRepoURL(sub.getRepository().getRepoName()));
	    }
    }

	private boolean isMySqlActive(String ip, int port) {
	   
        boolean isIpPortAvailable = false;
        InputStream is = null;
        DataInputStream dis = null;
        Socket s1 = null;
        try {
            s1 = new Socket(ip, port);
            is = s1.getInputStream();
            dis = new DataInputStream(is);
            if (dis != null) {
                log.info("Connected with ip " + ip + " and port " + port);
                isIpPortAvailable = true;
            }

        } catch (Exception e) {
            log.error("Exception occurred .. retrying " + e.getMessage());
            isIpPortAvailable = false;
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (Exception e) {
                log.error("Error in closing datainstream. " + e.getMessage());
            }
            try {
                if (s1 != null) {
                    s1.close();
                }
            } catch (IOException e) {
                log.error("Error in closing socket. " + e.getMessage());
            }
        }

        return isIpPortAvailable;
		
    }

	private String checkCartridgeStatus(Map<String, String> instanceIpMap) {
	    if(instanceIpMap.values().contains(CartridgeConstants.ACTIVE)) {
	    	return CartridgeConstants.ACTIVE;
	    }
	    else 
	    	return CartridgeConstants.NOT_READY;
    }

	/**
	 * Lists available Cartridges
	 * 
	 * @return Available Cartridges
	 * @throws AxisFault
	 * 
	 */
	public Cartridge[] listAvailableCartridges() throws AxisFault, ADCException {

		List<Cartridge> cartridgeList = new ArrayList<Cartridge>();

		try {
			List<CartridgeSubscription> subscriptionList =
			                                               PersistenceManager.retrieveSubscribedCartridges(getTenantId());
			List<Cartridge> nonSubscribedCartridges = new ArrayList<Cartridge>();
			
			String[] availableCartridges = getServiceClient().getRegisteredCartridges();
			for (String cartridge : availableCartridges) {
				// Check for non subscribed cartridges
				if (cartridgeNotSubscribed(subscriptionList, cartridge)) {
					Cartridge cartridgeInfo = new Cartridge();
					cartridgeInfo.setCartridgeType(cartridge);
					cartridgeInfo.setCartridgeName("-");
					cartridgeInfo.setActiveInstances(0);
					cartridgeInfo.setStatus(CartridgeConstants.NOT_SUBSCRIBED);
					nonSubscribedCartridges.add(cartridgeInfo);
				}
			}
			cartridgeList.addAll(populateSubscribedCartridges(subscriptionList));
			cartridgeList.addAll(nonSubscribedCartridges);
		} catch (Exception e) {
			String msg = "Exception is occurred in listing available Cartridges. Reason :" +
			                     e.getMessage();
            log.error(msg);
			throw new ADCException("An Error occurred while listing ", e);
		}
        return cartridgeList.toArray(new Cartridge[cartridgeList.size()]);
	}

	private int getTenantId() {
		int tenantId = MultitenantUtils.getTenantId(getConfigContext());
		log.info("Returning tenant ID : " + tenantId);
	    return tenantId;
    }

	private List<Cartridge> populateSubscribedCartridges(List<CartridgeSubscription> subscriptionList) throws Exception {

		List<Cartridge> subscribedCartridges = new ArrayList<Cartridge>();

		for (CartridgeSubscription subscription : subscriptionList) {
			Cartridge cartridge = new Cartridge();
			cartridge.setCartridgeName(subscription.getName());
			cartridge.setCartridgeType(subscription.getCartridge());

			TopologyManagementService topologyMgtService = DataHolder.getTopologyMgtService();
			String[] ips =
			              topologyMgtService.getActiveIPs(subscription.getCartridge(),
			                                              subscription.getClusterDomain(),
			                                              subscription.getClusterSubdomain());
			populateCartridgeInfo(cartridge, subscription, ips);
			subscribedCartridges.add(cartridge);
		}

		return subscribedCartridges;
	}

	private boolean cartridgeNotSubscribed(List<CartridgeSubscription> subscriptionList,
	                                       String cartridge) {

		for (CartridgeSubscription subscribedCartridge : subscriptionList) {
			if (subscribedCartridge.getCartridge().equals(cartridge)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Describes a Cartridge Type
	 * 
	 * @param type
	 * @return
	 * @throws AxisFault
	 */
	public String describeCartridgeType(String type) throws AxisFault, ADCException {

		String cartridgeDescription = null;
        try {
		    CloudControllerServiceClient cloudControllerServiceClient = getServiceClient();
			cartridgeDescription = cloudControllerServiceClient.describeCartridge(type);
		} catch (Exception e) {
			String msg = "Exception is occurred in describeCartridge operation. Reason: " + e.getMessage();
            log.error(msg);
			throw new ADCException("Error is occurred while describing cartridge. ", e);
		}
		return cartridgeDescription;
	}

	/**
	 * 
	 * Subscribe to a cartridge
	 * 
	 * @param minInstances
	 * @param maxInstances
	 * @param shouldActivate
	 * @param cartridgeType
	 * @return
	 * @throws AxisFault
	 */
	public String subscribe(int minInstances, int maxInstances, boolean shouldActivate,
	                                    String alias, String cartridgeType, 
	                                    String repoURL,
                                        String repoUserName,
                                        String repoPassword,
	                                    String dataCartridgeType,
	                                    String dataCartridgeAlias) throws ADCException, AxisFault {
		
		return doSubscribe(minInstances, maxInstances, shouldActivate, alias,
		                   cartridgeType, repoURL,repoUserName,repoPassword,dataCartridgeType, dataCartridgeAlias);

	}
	
		
	/***
     * Unsubscribing the cartridge
	 * 
	 * @param alias name of the cartridge to be unsubscribed
	 * @return result as a message to UIs
	 */
	public String unsubscribe(String alias) throws AxisFault, ADCException {

		CartridgeSubscription subscription = null;
		try {
			subscription = PersistenceManager.getSubscription(tenantDomain, alias);

			if (subscription == null) {
				String msg = "Tenant " + tenantDomain + " is not subscribed for " + alias;
                log.error(msg);
				throw new ADCException("You have not subscribed for " + alias);
			}

			String clusterDomain = subscription.getClusterDomain();
			String clusterSubDomain = subscription.getClusterSubdomain();
			
			if(!CartridgeConstants.PROVIDER_NAME_WSO2.equals(subscription.getProvider())) {
				log.info("Terminating all instances of " + clusterDomain + " " + clusterSubDomain);
				getServiceClient().terminateAllInstances(clusterDomain, clusterSubDomain);
				getServiceClient().unregisterService(clusterDomain, clusterSubDomain);
				log.info("Successfully terminated instances ..");
			}
			
			new RepositoryFactory().destroyRepository(alias, tenantDomain, getUsername());
			log.info("Repo is destroyed successfully.. ");
			
		    PersistenceManager.updateSubscriptionState(subscription.getSubscriptionId(), "UNSUBSCRIBED");
		    new DNSManager().removeSubDomain(subscription.getHostName());
            registryManager.removeDomainMappingFromRegistry(subscription.getHostName());
            
            
		} catch (Exception e1) {
            String msg1 = "Exception occurred :" + e1.getMessage();
			log.error(msg1);
            throw new ADCException("Unsubscribe failed for cartridge " + alias, e1);
		}
		return "You have successfully unsubscribed " + alias;
	}
	
	/**
	 * 
	 * Connects given application cartridge and a data cartridge
	 * 
	 * 
	 * @param applicationCartridge
	 * @param dataCartridge
	 * @return
	 */
	public String connect(String applicationCartridge, String dataCartridge) {
		
		// check if applicationCartridge alias exists		
		// check if dataCartridge alias exists
		
		return null;
	}
	
	private String doSubscribe(int minInstances, int maxInstances, boolean shouldActivate,
	                           String alias, String cartridgeType, String repoURL , String repoUserName,
                               String repoUserPassword,String dataCartridgeType,
                               String dataCartridgeAlias) throws AxisFault, ADCException {

        String clusterDomain = "";
		String clusterSubDomain = CartridgeConstants.DEFAULT_SUBDOMAIN;
		CartridgeSubscription subscription = null;
		shouldActivate = false;
		String mysqlPassword = null;
		Repository repository = null;
		DataCartridge dataCartridge = null;
		String cartName = (alias != null) ? alias : cartridgeType;
        String payloadZipFileName = "/tmp/" + UUID.randomUUID().toString() + ".zip";

			log.info("Subscribing tenant [" + getTenantId() + "] with username [" + getUsername()+"]");
			
			validate(cartridgeType, cartName);			
			CartridgeInfo cartridgeInfo = getCartridgeInfo(cartridgeType);			

			if (cartridgeType.equals(CartridgeConstants.MYSQL_CARTRIDGE_NAME)) {				
				shouldActivate = true;
				dataCartridge = new DataCartridge();
				mysqlPassword = generatePassword();
				dataCartridge.setPassword(mysqlPassword);
				dataCartridge.setDataCartridgeType(cartridgeType);
				dataCartridge.setUserName(CartridgeConstants.MYSQL_DEFAULT_USER);
				clusterDomain = getDynamicClusterDomain(cartridgeType, cartName, cartridgeInfo);
				
				registerService(cartridgeType,
				                clusterDomain,
				                clusterSubDomain,
				                createPayload(cartridgeInfo, cartName, 1,
				                              1, repoURL, mysqlPassword, "localhost",payloadZipFileName),
				                "*",
				                cartName + "." + cartridgeInfo.getHostName(), 
				                setRegisterServiceProperties());
                deletePayloadFile(payloadZipFileName);
				//sleepUntilRegistered();
				//activateInstance(clusterDomain, clusterSubDomain);				
				new Thread(new MySQLPasswordConfigurer(cartridgeType, clusterDomain,
					                                       clusterSubDomain, mysqlPassword)).start();

			} else {

				if (!cartridgeInfo.getProvider().equals(CartridgeConstants.PROVIDER_NAME_WSO2)) {
					clusterDomain = getDynamicClusterDomain(cartridgeType, cartName, cartridgeInfo);
					
					String mySQLPassword = null;
					String mySQLHostName = null;
					
					if (dataCartridgeType != null) {
						log.info(" Retriveing MYSQL Cartridge info for connect ... alias : " + dataCartridgeAlias);
					
						while(true) {							
							Cartridge c = getCartrdigeInfo(dataCartridgeAlias);
							if (c != null) {								
								if(!c.getStatus().equals("ACTIVE")) {
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException ignore) {
                                    }
                                } else {
									mySQLPassword = c.getPassword();
									mySQLHostName = c.getIp();
									break;
								}								
							}							
						}	
						log.info(" MYSQL Cartridge info retrieved ");
					}
					Properties properties = new Properties();
					registerService(cartridgeType,
					                clusterDomain,
					                clusterSubDomain,
					                createPayload(cartridgeInfo, cartName, minInstances,
					                              maxInstances, repoURL, mySQLPassword, mySQLHostName,payloadZipFileName), "*",
					                cartName + "." + cartridgeInfo.getHostName(),
					                properties);
                    deletePayloadFile(payloadZipFileName);
				} else {

					TopologyManagementService topologyService = DataHolder.getTopologyMgtService();
					String[] domains = topologyService.getDomains(cartridgeType, getTenantId());
					String[] subDomains = topologyService.getSubDomains(cartridgeType, getTenantId());
					log.info("Retrieved domains and subdomains : " + domains.length + " " +
					         subDomains.length);

					if (domains.length > 0) {
						clusterDomain = domains[0];
					} else {
						log.warn("Domains not found .. assigning default values");
						clusterDomain = "wso2.as.domain";
					}
					if (subDomains.length > 0) {
						clusterSubDomain = subDomains[0];
					} else {
						log.warn("Subdomains not found .. assigning default values");
						clusterSubDomain = "worker";
					}
				}

				repository = manageRepository(repoURL,repoUserName,repoUserPassword, cartName, cartridgeInfo);
			}

			subscription =
			               createCartridgeSubscription(cartridgeInfo, minInstances, maxInstances,
			                                           cartridgeType, cartName, getTenantId(), tenantDomain,
			                                           repository, clusterDomain, clusterSubDomain, dataCartridge);

			PersistenceManager.persistSubscription(subscription);
			addDNSEntry(alias, cartridgeType);			
			return createSubscriptionMessage(cartridgeType, repository);

    }

    private void deletePayloadFile(String payloadZipFileName) {
        File payloadFile = new File(payloadZipFileName);
        payloadFile.delete();
        log.info(" Payload file is deleted. ");
    }

	private Properties setRegisterServiceProperties() {
	    Properties properties = new Properties();				
	    Property minProp = new Property();
	    minProp.setName("min_app_instances");
	    minProp.setValue("1");
	    Property maxProp = new Property();
	    maxProp.setName("max_app_instances");
	    maxProp.setValue("1");
	    Property[] propArray = new Property[]{minProp, maxProp};								
	    properties.setProperties(propArray);
	    return properties;
    }

	private void sleepUntilRegistered() {
	    try {
	    Thread.sleep(15000);
	    } catch (InterruptedException e1) {
	    }
    }

	private void addDNSEntry(String alias, String cartridgeType) {
	    new DNSManager().addNewSubDomain(alias + "." + cartridgeType, System.getProperty(CartridgeConstants.ELB_IP));
    }

	private String createSubscriptionMessage(String cartridgeType, Repository repository) {
	    if (repository != null && repository.getRepoName() != null) {
	    	return convertRepoURL(repository.getRepoName());
	    } else {
	    	return "You have successfully subscribed to " + cartridgeType + " cartridge.";
	    }
    }

	private String getDynamicClusterDomain(String cartridgeType, String cartName,
                                           CartridgeInfo cartridgeInfo) {
	    return cartName + "." + cartridgeInfo.getHostName() + "." + cartridgeType +
	            ".domain";
    }

	private Repository manageRepository(String repoURL,String repoUserName,String repoUserPassword, String cartName, CartridgeInfo cartridgeInfo) throws AxisFault, ADCException {
				
	    Repository repository = new Repository();
	    if (repoURL != null) {
	    	log.info("External REPO URL is provided as [" + repoURL +
	    	         "]. Therefore not creating a new repo.");
	    	//repository.setRepoName(repoURL.substring(0, repoURL.length()-4)); // remove .git part
	    	repository.setRepoName(repoURL);
            repository.setRepoUserName(repoUserName);
            repository.setRepoUserPassword(repoUserPassword);
	    } else {
	    	
	    	log.info("External REPO URL is not provided. Therefore creating a new repo. Adding to Executor");
	    	
	    	repoCreationExecutor.execute(new RepositoryCreator(new RepositoryInfoBean(repoURL,
                                                                          cartName,
                                                                          tenantDomain,
                                                                          getUsername(),
                                                                          cartridgeInfo.getDeploymentDirs(),
                                                                          cartridgeInfo)));	    		    	
	    	String repoName = tenantDomain + "/" + cartName; 
            repository.setRepoName("git@" + System.getProperty(CartridgeConstants.GIT_HOST_NAME) + ":" +repoName);
            
	    }
	    return repository;
    }

	
	private String convertRepoURL (String gitURL) {
		String convertedHttpURL = null;
		if(gitURL != null && gitURL.startsWith("git@")){			
			StringBuilder httpRepoUrl = new StringBuilder();
			httpRepoUrl.append("http://");
			String[] urls = gitURL.split(":");
			String[] hostNameArray = urls[0].split("@");
			String hostName = hostNameArray[1];
			httpRepoUrl.append(hostName).append("/").append(urls[1]);			
			convertedHttpURL = httpRepoUrl.toString();			
		} else if (gitURL != null && gitURL.startsWith("http"))  {
			convertedHttpURL = gitURL;
		}
		return convertedHttpURL;
	}
	
	private void validate(String cartridgeType, String cartName) throws AxisFault, ADCException {
	    if (!isValidCartridge(cartridgeType)) {
            String msg = cartridgeType + " is not a valid cartridge type. Please try again with a valid cartridge type. ";
            log.error(msg);
            throw new ADCException(msg);
	    }			

	    if (isAliasAlreadyTaken(cartName, cartridgeType)) {
            String msg = "The alias you have provided, " + cartName + " is already taken. Please try again with a different alias. ";
            log.error(msg);
            throw new ADCException(msg);
	    }
    }

	private Cartridge getCartrdigeInfo(String mySqlAlias) throws ADCException, AxisFault {
		return doListCartridgeInfo(mySqlAlias);
    }

	private String generatePassword() {

		final int PASSWORD_LENGTH = 8;
		StringBuffer sb = new StringBuffer();
		for (int x = 0; x < PASSWORD_LENGTH; x++) {
			sb.append((char) ((int) (Math.random() * 26) + 97));
		}
		return sb.toString();

	}

	public String addDomainMapping(String mappedDomain, String cartridgeAlias) throws ADCException {

        CartridgeSubscription subscription = null;
        String actualHost = null;
        try {
            subscription = PersistenceManager.getSubscription(tenantDomain, cartridgeAlias);
            if (subscription == null) {
                String msg = "Tenant " + tenantDomain + " is not subscribed for " + cartridgeAlias;
                log.error(msg);
                throw new ADCException("You have not subscribed for " + cartridgeAlias);
            }
            actualHost = getActualHost(cartridgeAlias);
        } catch (Exception e) {
            String msg = "Unable to add the mapping while extracting actual host from DB";
            log.error(msg , e);
            throw new ADCException("Unable to add the mapping due to internal error!", e);
        }
        try {
            if (registryManager.addDomainMappingToRegistry(mappedDomain, actualHost)) {
                log.info("Domain mapping is added for " + mappedDomain + " tenant: " +
                         tenantDomain );
            return actualHost;
            }
        } catch (RegistryException e) {
            String msg = "Unable to add the mapping due to registry transaction error";
            log.error(msg , e);
            throw new ADCException("Unable to add the mapping due to internal error!", e);
        }
        return null;
	}

	// public CartridgeAppType[] getAppTypes(String alias){
	//
	// try {
	// String cartridgeType =
	// PersistenceManager.getTypeForCartridgeName(getCurrentTenantId(), alias);
	// TODO implement above if required
	// CartridgeInfo cartridgeInfo = getCartridgeInfo(cartridgeType);
	// TODO get types correctly , if app specific domain mapping requirement is
	// confirmed
	// } catch (Exception e) {
	// e.printStackTrace(); //To change body of catch statement use File |
	// Settings | File Templates.
	// }
	// CartridgeAppType[] cartridgeAppTypes =new CartridgeAppType[4];
	// return cartridgeAppTypes;
	// }

	private String getActualHost(String cartridgeName) throws Exception {
		PersistenceManager persistenceManager = new PersistenceManager();
			return persistenceManager.getHostNameForCartridgeName(/*tenantId*/getTenantId(),
			                                                      cartridgeName);

	}

	// private String getTenantContext(String appType){
	// if(CartridgeConstants.DomainMappingInfo.AXIS2_SERVICES.equalsIgnoreCase(appType)){
	// return "/services/t/" + getTenantDomain() ;
	// } else {
	// return "/t/" + getTenantDomain();
	// }
	// }
	private List<PortMapping> createPortMappings(CartridgeInfo cartridgeInfo) {
		List<PortMapping> portMappings = new ArrayList<PortMapping>();

		if (cartridgeInfo.getPortMappings() != null) {
			for (org.wso2.carbon.stratos.cloud.controller.util.xsd.PortMapping portMapping : cartridgeInfo.getPortMappings()) {
				PortMapping portMap = new PortMapping();
				portMap.setPrimaryPort(portMapping.getPort());
				portMap.setProxyPort(portMapping.getProxyPort());
				portMap.setType(portMapping.getProtocol());
				portMappings.add(portMap);
			}
		}
		return portMappings;
	}

	private CartridgeSubscription createCartridgeSubscription(CartridgeInfo cartridgeInfo,
	                                                          int minInstances, int maxInstances,
	                                                          String cartridgeType,
	                                                          String cartridgeName, 
	                                                          int tenantId,
	                                                          String tenantDomain,
	                                                          Repository repository,
	                                                          String clusterDomain,
	                                                          String clusterSubDomain,
	                                                          DataCartridge dataCartridge) {

		CartridgeSubscription cartridgeSubscription = new CartridgeSubscription();
		cartridgeSubscription.setCartridge(cartridgeType);
		cartridgeSubscription.setName(cartridgeName);
		cartridgeSubscription.setClusterDomain(clusterDomain);
		cartridgeSubscription.setClusterSubdomain(clusterSubDomain);
		String hostName = null;
		if(CartridgeConstants.PROVIDER_NAME_WSO2.equals(cartridgeInfo.getProvider())){
			hostName = cartridgeInfo.getHostName();
		}else {
			hostName = cartridgeName + "." + cartridgeInfo.getHostName();
		}
		cartridgeSubscription.setHostName(hostName);
		cartridgeSubscription.setMaxInstances(maxInstances);
		cartridgeSubscription.setMinInstances(minInstances);
		cartridgeSubscription.setRepository(repository);
		cartridgeSubscription.setPortMappings(createPortMappings(cartridgeInfo));
		cartridgeSubscription.setProvider(cartridgeInfo.getProvider());
		cartridgeSubscription.setDataCartridge(dataCartridge);
		cartridgeSubscription.setTenantId(tenantId);
		cartridgeSubscription.setTenantDomain(tenantDomain);
		cartridgeSubscription.setBaseDirectory(cartridgeInfo.getBaseDir());
		cartridgeSubscription.setState("PENDING");
		return cartridgeSubscription;
	}

	private CartridgeInfo getCartridgeInfo(String cartridgeType) throws AxisFault {

        try {
            return getServiceClient().getCartridgeInfo(cartridgeType);
        } catch (Exception e) {
            String msg = "Exception : " + e.getMessage();
            log.error(msg, e);
            throw new AxisFault("Failure in Subscribing to the cartridge", e);
        }
    }

	private boolean isValidCartridge(String cartridgeType) throws AxisFault {
		String[] cartridges;
        try {
	        cartridges = getServiceClient().getRegisteredCartridges();
        } catch (Exception e) {
            String msg = "Exception : " + e.getMessage();
        	log.error(msg, e);
	        throw new AxisFault("Please enter a valid Cartridge type", e);
        }
		List<String> cartridgeList = Arrays.asList(cartridges);
		return cartridgeList.contains(cartridgeType);
	}

	private boolean isAliasAlreadyTaken(String alias, String cartridgeType) throws AxisFault {
		try {
	        return PersistenceManager.isAliasAlreadyTaken(alias, cartridgeType);
        } catch (Exception e) {
            String msg = "Exception : " + e.getMessage();
        	log.error(msg, e);
        	throw new AxisFault("Alias you entered is already taken. Please enter a different alias", e);
        }
	}

	
	/**
     *             This operation creates a git repo for an application
	 *
	 * @param setWebRoot
	 * @param cartridgeType
	 * @param appName
	 * @return
     * @throws AxisFault
	 *
	 */
	public boolean addApplication(boolean setWebRoot, String cartridgeType, String appName)
	                                                                                       throws AxisFault {


		String repoName = tenantDomain + "." + cartridgeType + ".git";

		// TODO - Domain mapping related changes

		return false;
	}


	/**
	 * 
	 * @param minInstances
	 * @param maxInstance
	 * @throws AxisFault
	 * 
	 *             This operation starts instances in relevant IaaS
	 */
	public boolean activate(int minInstances, int maxInstance, String name) throws AxisFault, AxisFault {

		// min max is -1 if not provided
		boolean isActivationSuccessful = false;
		CartridgeSubscription subcription = null;

		try {
			subcription = PersistenceManager.getSubscription(tenantDomain, name);
		} catch (Exception e1) {
            String msg = "Exception is occurred, reason : " + e1.getMessage();
			log.error(msg, e1);
			throw new AxisFault("An error occurred in activation, Please try again. ", e1);
		}

		if (subcription == null) {
			throw new AxisFault("No subscriptions to " + name);
		}

		if (subcription.getState().equals("ACTIVE") || subcription.getState().equals("PENDING")) {
			throw new AxisFault("Cartridge " + name + " is already activated. ");
		}

		// TODO for carbon cartridges
		// TODO retrieve dynamic clusterdomain and subdomain info from topology

		if (minInstances != -1 && maxInstance != -1) {
			try {
				PersistenceManager.updateMinMax(/*tenantId*/getTenantId(), name, minInstances, maxInstance);
			} catch (Exception e) {
                String msg = "Exception is occurred. Reason : " + e.getMessage();
				log.error(msg);
				throw new AxisFault("An error occurred while activating...");
			}
		}

		String instanceIP = null;
		try {
			instanceIP =
			             activateInstance(subcription.getClusterDomain(),
			                              subcription.getClusterSubdomain());
			isActivationSuccessful = true;
		} catch (Exception e) {
			String msg = "Exception is occurred in Subscribing. Reason :" + e.getMessage();
			log.error(msg, e);
			throw new AxisFault("An error occurred while subscribing", e);
		}
		if (instanceIP == null) {
			log.warn(" Instance is successfully activated, but no public IP is associated. ");
		}

		return isActivationSuccessful;
	}

	/**
	 * 
	 * @param cartridgeType
	 * @return
	 * @throws AxisFault
	 * 
	 *             This operation lists available applications
	 */
	public String[] listApplications(String cartridgeType) throws AxisFault {

		try {
			List<String> applications =
			                            PersistenceManager.retrieveApplications(/*tenantId*/getTenantId(),
			                                                                    cartridgeType);
			return applications.toArray(new String[applications.size()]);
		} catch (Exception e) {
			String msg = "Exception is occurred in Subscribing.  Reason :" + e.getMessage();
			log.error(msg, e);
			throw new AxisFault("An error occurred while listing cartridges", e);
		}
	}


	private String activateInstance(String domain, String subDomain) throws AxisFault {

		// Eventhough start instance call returns IP s of spawned instances,
		// persisting those at
		// this point is not useful, because if so, there is no way to get IP s
		// of instances
		// spawned through LB (autoscaling)
		// Hence using InstanceInformationMangemenetService

		log.info("Activating......");

        try {
            return getServiceClient().startInstance(domain, subDomain);
        } catch (Exception e) {
            String msg = "Exception : " + e.getMessage();
            log.error(msg, e);
            throw new AxisFault("Subscribe failed ", e);
        }
    }

	private void registerService(String cartridgeType, String domain, String subDomain,
	                             DataHandler payload, String tenantRange, String hostName, Properties properties)
	                                                                                      throws AxisFault {
		log.info("Register service..");
		try {
			getServiceClient().register(domain, subDomain, cartridgeType, payload, tenantRange,
			                            hostName, properties);
		} catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            String msg = "Exception is occurred in register service operation. Reason :" + e.getMessage();
			log.error(msg);
			throw new AxisFault("An error occurred in subscribing process");
		} catch (RemoteException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

	}

	private CloudControllerServiceClient getServiceClient() throws AxisFault {
		return new CloudControllerServiceClient(
		                                        System.getProperty(CartridgeConstants.AUTOSCALER_SERVICE_URL));
	}
	
	private CartridgeAgentClient getCartridgeAgentClient() throws AxisFault {
		return new CartridgeAgentClient(System.getProperty(CartridgeConstants.CARTRIDGE_AGENT_EPR));
	}

	private DataHandler createPayload(CartridgeInfo cartridgeInfo, String cartridgeName, int min,
	            int max, String repoURL, String mySQLPwd, String mySQLHost,String payloadZipFileName)throws AxisFault {

        FileDataSource dataSource =null;
        File payloadFile = null;
        try {
            payloadFile = getPayload(cartridgeInfo, cartridgeName,
                    min, max, repoURL,
                    mySQLPwd, mySQLHost, payloadZipFileName);
            dataSource = new FileDataSource(payloadFile);
        } catch (Exception e) {
            String msg = "Exception : " + e.getMessage();
            log.error(msg, e);
            throw new AxisFault("Subscribe failed ", e);
        }
        DataHandler payloadDataHandler = new DataHandler(dataSource);
		return payloadDataHandler;
	}

    protected String getAppDeploymentDirPath(String cartridge) {
        return getAxisConfig().getRepository().getPath() + File.separator + cartridge;
	}

	
	private void writeKeyFileToCarbonHome() {		
		try {
	        FileUtils.copyFile(new File(System.getProperty(CartridgeConstants.REPO_KEY_PATH)),
	                           new File("id_rsa"));
        } catch (IOException e) {
        	log.error("Exception is occurred.." + e.getMessage());
        }
	}
	
	private File getPayload(CartridgeInfo cartridgeInfo, String cartridgeName, int minInstances,
	                        int maxInstances, String repoURL, 
	                        String mySQLPassword, String mySQLHost,String payloadZipFileName) throws Exception {
		String payloadString = "";

		payloadString += "TENANT_RANGE=" + "*";
		payloadString +=
		                 ",CARTRIDGE_AGENT_EPR=" +
		                         System.getProperty(CartridgeConstants.CARTRIDGE_AGENT_EPR);
		
		 
		payloadString += createPortMappingPayloadString(cartridgeInfo);
		payloadString += ",HOST_NAME=" + cartridgeName + "." + cartridgeInfo.getHostName();
		payloadString += ",MIN=" + minInstances;
		payloadString += ",MAX=" + maxInstances;
		payloadString += ",SERVICE=" + cartridgeInfo.getType();
		payloadString += ",TENANT_CONTEXT=" + tenantDomain;
		
		String gitRepoURL = null;
		if(repoURL!=null){
			gitRepoURL = repoURL; 
		} else {
			gitRepoURL = "git@" + System.getProperty(CartridgeConstants.GIT_HOST_IP) + ":" +tenantDomain
                    + System.getProperty("file.separator") + cartridgeName + ".git";
		}
		payloadString += ",GIT_REPO="+gitRepoURL;
		payloadString += ",APP_PATH=" + cartridgeInfo.getBaseDir();
		payloadString += ",BAM_IP=" + System.getProperty(CartridgeConstants.BAM_IP);
		payloadString += ",BAM_PORT=" + System.getProperty(CartridgeConstants.BAM_PORT);
		
		// MYSQL params
		payloadString += ",MYSQL_HOST=" + mySQLHost;
		payloadString += ",MYSQL_USER=" + "root";
		payloadString += ",MYSQL_PASSWORD=" + mySQLPassword;
		
		// Autoscaling params
		payloadString += ",ALARMING_LOWER_RATE=" + "0.2"; // TODO get values from cartridges xmls 
		payloadString += ",ALARMING_UPPER_RATE=" + "0.7";
		payloadString += ",MAX_REQUESTS_PER_SEC=" + "5";
		payloadString += ",ROUNDS_TO_AVERAGE=" + "2";
		payloadString += ",SCALE_DOWN_FACTOR=" + "0.25";
		
		log.info("** Payload ** " + payloadString);
		// byte[] payload = payloadString.getBytes();
		// write payload string to a tmp file

		
		// Write keyfile to carbonhome -- now this is done by startup script
		// writeKeyFileToCarbonHome();
		
		String payloadStringTempFile = "launch-params";

		FileWriter fstream = new FileWriter(payloadStringTempFile);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(payloadString);
		out.close();

		// read private key ( for now from file system )
		String privateKeyFile = "id_rsa";

        FileOutputStream fos = new FileOutputStream(payloadZipFileName);
		ZipOutputStream zos = new ZipOutputStream(fos);

		addToZipFile(payloadStringTempFile, zos);
		addToZipFile(privateKeyFile, zos);

		zos.close();
		fos.close();

        return new File(payloadZipFileName);
	}

	private String createPortMappingPayloadString(CartridgeInfo cartridgeInfo) {
		// port mappings
		StringBuilder portMapBuilder = new StringBuilder();
		org.wso2.carbon.stratos.cloud.controller.util.xsd.PortMapping[] portMappings = cartridgeInfo.getPortMappings();
		for (org.wso2.carbon.stratos.cloud.controller.util.xsd.PortMapping portMapping : portMappings) {
	        String port = portMapping.getPort();
	        String protocol = portMapping.getProtocol();
	        String proxyPort = portMapping.getProxyPort();	        
	        portMapBuilder.append(protocol).append(":").append(port).append(":").append(proxyPort).append("|");
        }

		// remove last "|" character
		String portMappingString = portMapBuilder.toString();
		String portMappingPayloadString = null;
		if(portMappingString.charAt(portMappingString.length()-1)=='|') {
			portMappingPayloadString = portMappingString.substring(0,portMappingString.length()-1);
		} else {
			portMappingPayloadString = portMappingString;
		}		
		
		return ",PORTS="+portMappingPayloadString;
    }

	private void addToZipFile(String fileName, ZipOutputStream zos) throws FileNotFoundException,
	                                                               IOException {

		log.info("Writing '" + fileName + "' to zip file");

		File file = new File(fileName);
		FileInputStream fis = new FileInputStream(file);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zos.putNextEntry(zipEntry);

		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}

		zos.closeEntry();
		fis.close();
	}
    /**
     * Validate the authentication
     *
     * First call of cli tool in the prompt mode after log in.
     * @return true if validation successful.
     */

    public boolean authenticateValidation(){
        boolean isValidated = false;
        if(tenantDomain != null){
            log.info("Tenant " + tenantDomain + " has authenticated to Application Management service!");
            isValidated = true;
        }
        return isValidated;
    }

    public int getCartridgeClusterMaxLimit(){
        return Integer.parseInt(System.getProperty(CartridgeConstants.CARTRIDGE_CLUSTER_MAX_LIMIT));
    }

}