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
package org.wso2.carbon.registry.commentstream.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

/**
 * Registry based implementation for Comment Stream.
 * 
 * @author shamika
 * 
 */
public class RegistryCommentStreamDAO implements CommentStreamDAO {

	private static final Log log = LogFactory.getLog(RegistryCommentStreamDAO.class);

	private Registry userRegistry = null;
	
	private Registry systemRegistry = null;

	@Override
	public void addComment(CommentWrapper comment) throws CommentStreamException {

		try {
			log.debug("Adding the comment to comment stream coment - " + comment + " reference - " +
			          comment.getReference());
			
			getUserRegistry(comment.getUser()).addComment(comment.getReference(), comment.getRegistryComment());
			log.info("Comment - " + comment + " for the user " + comment.getUser() +
			         "added succesfully");
		} catch (RegistryException e) {
			log.error("Error Occured while adding the Registry Comment - " + e.getMessage());
			throw new CommentStreamException(e.getMessage());
		}

	}

	private Registry getUserRegistry(String userName) throws RegistryException {
		if (null == userRegistry) {
			userRegistry =
			           ServiceReferenceHolder.getInstance().getRegistryService()
			                                 .getGovernanceUserRegistry(userName);
		}
		return userRegistry;
	}
	
	private Registry getSystemRegistry() throws RegistryException {
		if (null == systemRegistry) {
			systemRegistry =
			           ServiceReferenceHolder.getInstance().getRegistryService()
			                                 .getGovernanceSystemRegistry();
		}
		return systemRegistry;
	}

	@Override
    public List<CommentWrapper> getComments(String ref) throws CommentStreamException {
	    
		try {
	        org.wso2.carbon.registry.core.Comment[] comments = getSystemRegistry().getComments(ref);
	        List<CommentWrapper> commentsList = new ArrayList<CommentWrapper>();
	        for(org.wso2.carbon.registry.core.Comment comment : comments){
	        	commentsList.add(new CommentWrapper(comment));
	        }
	        Collections.sort(commentsList);
	        return commentsList;
	        
        } catch (RegistryException e) {
        	log.error("Error Occured while loading the Registry Comments - " + e.getMessage());
			throw new CommentStreamException(e.getMessage());
        }
    }

}
