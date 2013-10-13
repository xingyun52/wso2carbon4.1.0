package org.wso2.carbon.stratos.cloud.controller.consumers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.lb.common.conf.structure.Node;
import org.wso2.carbon.lb.common.conf.structure.NodeBuilder;
import org.wso2.carbon.lb.common.conf.util.Constants;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.stratos.cloud.controller.util.CloudControllerServiceReferenceHolder;
import org.wso2.carbon.stratos.cloud.controller.util.ServiceContext;

public class TopologyBuilder implements Runnable {

    private BlockingQueue<List<ServiceContext>> sharedQueue;
	private File topologyFile, backup;
	private static final Log log = LogFactory.getLog(TopologyBuilder.class);
	private CloudControllerServiceReferenceHolder data = CloudControllerServiceReferenceHolder.getInstance();
	
	public TopologyBuilder(BlockingQueue<List<ServiceContext>> queue){
		
		sharedQueue = queue;
		topologyFile = new File(CloudControllerConstants.TOPOLOGY_FILE_PATH);
		backup = new File(CloudControllerConstants.TOPOLOGY_FILE_PATH+".back");
		
	}
	
	@Override
	public void run() {

		while(true){
            try {
            	Object obj = sharedQueue.take();
            	boolean isAdded = false;
            	
            	if(obj instanceof List<?>){
            		@SuppressWarnings("unchecked")
                    List<ServiceContext> ctxts = (List<ServiceContext>) obj;
            		
            		for (ServiceContext serviceContext : ctxts) {

            			Node newNode = serviceContext.toNode();
            			
            			if(!topologyFile.exists()){
            				FileUtils.writeStringToFile(topologyFile, "services {\n}");
            			}
            			
            			String currentContent = FileUtils.readFileToString(topologyFile);
            			Node currentNode = NodeBuilder.buildNode(currentContent);
                   			
            			for (Node aNode : currentNode.getChildNodes()) {
            				// similar service element is present
	                        if(aNode.getName().equals(newNode.getName())){
	                        	// hence, we should not override, but append
	                        	Node domainsNode = aNode.findChildNodeByName(Constants.DOMAIN_ELEMENT);
	                        	
	                        	if(domainsNode == null){
	                        		// existing node is useless
	                        		currentNode.removeChildNode(aNode.getName());
	                        		currentNode.appendChild(newNode);
	                        		break;
	                        	}
	                        	
	                        	// append the new node/s
	                        	for (Node serNode : newNode.findChildNodeByName(Constants.DOMAIN_ELEMENT).getChildNodes()) {
	                                
	                        		for (Node currentSerNode : domainsNode.getChildNodes()) {
	                                    String prop = Constants.SUB_DOMAIN_ELEMENT;
	                        			if(serNode.getName().equals(currentSerNode.getName()) &&
	                        					serNode.getProperty(prop).equals(currentSerNode.getProperty(prop))){
	                        				// if domain and sub domain, are not unique, we shouldn't append, but override
	                        				domainsNode.removeChildNode(currentSerNode.getName());
	                        				break;
	                        			}
                                    }
	                        		
	                        		domainsNode.appendChild(serNode);
                                }
	                        	isAdded = true;
	                        	break;
	                        }
                        }
            			
						if (!isAdded) {
							currentNode.appendChild(newNode);
						}
						
            			if (topologyFile.exists()) {
            				backup.delete();
            				topologyFile.renameTo(backup);
            			}
            			
            			// overwrite the topology file
            			FileUtils.writeStringToFile(topologyFile, currentNode.toString());
            			
            			// publish to the topic - to sync immediately
            	        data.getConfigPub().publish(CloudControllerConstants.TOPIC_NAME, currentNode.toString());

                    }
            	}
                
            } catch (InterruptedException ignore) {
            } catch (IOException e) {
            	log.error(e.getMessage(), e);
            	throw new CloudControllerException(e.getMessage(), e);
            }
        }

	}

}
