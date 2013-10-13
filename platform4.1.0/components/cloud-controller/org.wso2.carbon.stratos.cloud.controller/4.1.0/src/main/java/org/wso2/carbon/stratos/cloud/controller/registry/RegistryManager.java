package org.wso2.carbon.stratos.cloud.controller.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;
import org.wso2.carbon.stratos.cloud.controller.persist.Serializer;
import org.wso2.carbon.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerServiceReferenceHolder;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;

/**
 *
 */
public class RegistryManager {
	private final static Log log = LogFactory.getLog(RegistryManager.class);
	private static Registry registryService;
	private static RegistryManager registryManager;
	
	public static RegistryManager getInstance() {

		registryService = CloudControllerServiceReferenceHolder.getInstance().getRegistry();
				
		if (registryManager == null) {
			synchronized(RegistryManager.class){
				if (registryManager == null) {
					if(registryService == null){
//						log.warn("Registry Service is null. Hence unable to fetch data from registry.");
						return registryManager;
					}
					registryManager = new RegistryManager();
				}
			}
		}

		return registryManager;
	}
	
	private RegistryManager() {
		try {
			if (!registryService.resourceExists(CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE)) {
				registryService.put(CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE,
				                    registryService.newCollection());
			}
		} catch (RegistryException e) {
			String msg =
			             "Failed to create the registry resource " +
			                     CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE;
			log.error(msg, e);
			throw new CloudControllerException(msg, e);
		}
	}

	/**
     * Persist an object in the local registry.
     * @param dataObj object to be persisted.
     */
	public void persist(FasterLookUpDataHolder dataObj) throws RegistryException {
		try {

			registryService.beginTransaction();
			
			Resource nodeResource = registryService.newResource();

			nodeResource.setContent(Serializer.serializeToByteArray(dataObj));

			registryService.put(CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE+ CloudControllerConstants.DATA_RESOURCE, nodeResource);
			
			registryService.commitTransaction();
			
		} catch (Exception e) {
			String msg = "Failed to persist the cloud controller data in registry.";
			registryService.rollbackTransaction();
			log.error(msg, e);
			throw new CloudControllerException(msg, e);
			
		} 
	}
	
	public Object retrieve(){
		
		try {
	        Resource resource = registryService.get(CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE+ CloudControllerConstants.DATA_RESOURCE);
	        
	        return resource.getContent();
	        
        } catch (ResourceNotFoundException ignore) {
        	// this means, we've never persisted CC info in registry
        	return null;
        }catch (RegistryException e) {
        	String msg = "Failed to retrieve cloud controller data from registry.";
			log.error(msg, e);
			throw new CloudControllerException(msg, e);
        }
		
	}

}
