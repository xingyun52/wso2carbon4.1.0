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

import java.util.Date;

import org.wso2.carbon.registry.core.Comment;

/**
 * Domain class to represents Comment info.
 * Works as a wrapper to {@link org.wso2.carbon.registry.core.Comment};
 * 
 * @author shamika
 * 
 */
public class CommentWrapper implements Comparable<CommentWrapper>{

	private final String reference;

	private final String commentStr;
	
	private final String user;
	
	private final Date createdTime;
	
	public CommentWrapper(String reference, String commentStr, String user, Date createdTime) {
		super();
		this.reference = reference;
		this.commentStr = commentStr;
		this.user = user;
		this.createdTime = createdTime;
	}
	
	public CommentWrapper(Comment comment){
		this.reference = comment.getCommentPath();
		this.commentStr = comment.getText();
		this.user = comment.getAuthorUserName();
		this.createdTime = comment.getCreatedTime();
	}

	public String getReference() {
		return reference;
	}

	public String getCommentStr() {
		return commentStr;
	}

	public String getUser() {
		return user;
	}
	
	public Date getCreatedTime() {	   
	    return createdTime;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commentStr == null) ? 0 : commentStr.hashCode());
		result = prime * result + ((reference == null) ? 0 : reference.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommentWrapper other = (CommentWrapper) obj;
		if (commentStr == null) {
			if (other.commentStr != null)
				return false;
		} else if (!commentStr.equals(other.commentStr))
			return false;
		if (reference == null) { 
			if (other.reference != null)
				return false;
		} else if (!reference.equals(other.reference))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return commentStr;
	}

	@Override
    public int compareTo(CommentWrapper comment) {
		int compareTo = this.getCreatedTime().compareTo(comment.getCreatedTime());
		if(compareTo < 0){
			return 1;
		}else if(compareTo >0){
			return -1;
		}
		return 0;
	}

	/**
	 * Get {Comment } registry object
	 * 
	 * @return registry object
	 */
	public Comment getRegistryComment() {
		Comment comment = new Comment(this.commentStr);
		comment.setUser(this.user);
		return comment;
	}
}
