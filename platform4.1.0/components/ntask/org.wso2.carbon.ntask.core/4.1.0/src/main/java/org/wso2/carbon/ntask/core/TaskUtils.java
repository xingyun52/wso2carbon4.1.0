/**
 *  Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.ntask.core;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.axiom.om.OMElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.common.TaskException.Code;
import org.wso2.carbon.ntask.core.internal.TasksDSComponent;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

/**
 * This class contains utitilty functions related to tasks.
 */
public class TaskUtils {
	
	public static final String SECURE_VAULT_NS = "http://org.wso2.securevault/configuration";
	
	public static final String SECRET_ALIAS_ATTR_NAME = "secretAlias";
	
	public static final String TASK_PAUSED_PROPERTY = "TASK_PAUSED_PROPERTY";
	
	private static SecretResolver secretResolver;

	public static Registry getGovRegistryForTenant(int tid) throws TaskException {
		try {
			PrivilegedCarbonContext.startTenantFlow();
			PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tid);
			return TasksDSComponent.getRegistryService().getGovernanceSystemRegistry(tid);
		} catch (RegistryException e) {
			throw new TaskException("Error in retrieving registry instance", Code.UNKNOWN, e);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}
	
    public static Document convertToDocument(File file) throws TaskException {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        fac.setNamespaceAware(true);
        try {
            return fac.newDocumentBuilder().parse(file);
        } catch (Exception e) {
            throw new TaskException("Error in creating an XML document from file: " +
                    e.getMessage(), Code.CONFIG_ERROR, e);
        }
    }
    
	private static void secureLoadElement(Element element)
			throws CryptoException {
		Attr secureAttr = element.getAttributeNodeNS(SECURE_VAULT_NS,
				SECRET_ALIAS_ATTR_NAME);
		if (secureAttr != null) {
			element.setTextContent(loadFromSecureVault(secureAttr.getValue()));
			element.removeAttributeNode(secureAttr);
		}
		NodeList childNodes = element.getChildNodes();
		int count = childNodes.getLength();
		Node tmpNode;
		for (int i = 0; i < count; i++) {
			tmpNode = childNodes.item(i);
			if (tmpNode instanceof Element) {
				secureLoadElement((Element) tmpNode);
			}
		}
	}
    
	private static synchronized String loadFromSecureVault(String alias) {
		if (secretResolver == null) {
		    secretResolver = SecretResolverFactory.create((OMElement) null, false);
		    secretResolver.init(
		    		TasksDSComponent.getSecretCallbackHandlerService().getSecretCallbackHandler());
		}
		return secretResolver.resolve(alias);
	}
	
	public static void secureResolveDocument(Document doc) throws TaskException {
		Element element = doc.getDocumentElement();
		if (element != null) {
			try {
				secureLoadElement(element);
			} catch (CryptoException e) {
				throw new TaskException("Error in secure load of document: " + e.getMessage(), 
						Code.UNKNOWN, e);
			}
		}
	}
	
	public static void setTaskPaused(TaskRepository taskRepo, String taskName, 
			boolean paused) throws TaskException {
		taskRepo.setTaskMetadataProp(taskName, TASK_PAUSED_PROPERTY, Boolean.toString(paused));
	}
	
	public static boolean isTaskPaused(TaskRepository taskRepo, 
			String taskName) throws TaskException {
		String paused = taskRepo.getTaskMetadataProp(taskName, TASK_PAUSED_PROPERTY);
		if (paused == null) {
			return false;
		} else {
			return Boolean.parseBoolean(paused);
		}
	}
		
}
