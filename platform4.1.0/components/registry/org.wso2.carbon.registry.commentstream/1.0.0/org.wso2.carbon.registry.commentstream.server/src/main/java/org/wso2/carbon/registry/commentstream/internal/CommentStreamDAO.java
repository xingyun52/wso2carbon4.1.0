/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.registry.commentstream.internal;

import java.util.List;


/**
 * Interface specifies all the data access tasks related to comments stream.
 * 
 * @author shamika
 *
 */
public interface CommentStreamDAO {

	/**
	 * Saves the given {@link CommentWrapper}
	 * @param comment
	 * @throws CommentStreamException
	 */
	public void addComment(CommentWrapper comment) throws CommentStreamException;
	
	/**
	 * Retrieves all the comments associated with given {@code reference}.
	 * @param ref
	 * @return
	 * @throws CommentStreamException
	 */
	public List<CommentWrapper> getComments(String ref) throws CommentStreamException;
	
}
