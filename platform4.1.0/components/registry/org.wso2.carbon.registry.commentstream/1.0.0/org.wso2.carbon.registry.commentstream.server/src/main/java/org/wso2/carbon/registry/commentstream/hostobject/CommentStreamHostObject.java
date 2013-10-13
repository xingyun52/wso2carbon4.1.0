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
package org.wso2.carbon.registry.commentstream.hostobject;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.wso2.carbon.registry.commentstream.internal.CommentStreamException;
import org.wso2.carbon.registry.commentstream.internal.CommentWrapper;
import org.wso2.carbon.registry.commentstream.internal.RegistryCommentStreamDAO;

/**
 * Host object class to handle Comments Stream operations
 * @author shamika
 * 
 */
public class CommentStreamHostObject extends ScriptableObject {

	private static final long serialVersionUID = 1L;

	private static final Log log = LogFactory.getLog(CommentStreamHostObject.class);

	private static final String HOST_OBJ_NAME = "CommentStream";

	@Override
	public String getClassName() {
		return HOST_OBJ_NAME;
	}

	public static Scriptable jsConstructor(Context cx, Object[] args, Function ctorObj,
	                                       boolean inNewExpr) throws Exception {
		return new CommentStreamHostObject();
	}

	/**
	 * Adds comment to the given reference.
	 * @param cx
	 * @param thisObj
	 * @param args - args[0] - reference, args[1] - comment, args[2] - user
	 * @param funObj
	 * @return
	 */
	public static NativeArray jsFunction_addComment(Context cx, Scriptable thisObj, Object[] args,
	                                                Function funObj) {

		log.debug("Initiated adding comment host object method");
		NativeArray nativeArr = new NativeArray(0);
		if (validate(args)) {
			String reference = args[0].toString();
			String comment = args[1].toString();
			String user = args[2].toString();
			try {

				new RegistryCommentStreamDAO().addComment(new CommentWrapper(reference, comment,
				                                                             user, new Date()));
			} catch (CommentStreamException e) {
				log.error("Error while adding comment.....", e);
			}
		}
		return nativeArr;

	}

	/**
	 * Retrieves all the comments associated with the given resource.
	 * @param cx
	 * @param thisObj
	 * @param args
	 * @param funObj
	 * @return native array of comments.
	 */
	public static NativeArray jsFunction_getComments(Context cx, Scriptable thisObj, Object[] args,
	                                                 Function funObj) {
		log.debug("Initiated loading comment host object method");
		NativeArray nativeArr = new NativeArray(0); 
		try {
			if (args != null && args[0] instanceof String) {
				String ref = args[0].toString();
				List<CommentWrapper> comments = new RegistryCommentStreamDAO().getComments(ref);
				int i = 0;
				nativeArr = new NativeArray(comments.size());
				for (CommentWrapper comment : comments) {
					NativeObject commentNativeObj = new NativeObject();
					commentNativeObj.put("userName", commentNativeObj, comment.getUser());
					commentNativeObj.put("comment", commentNativeObj, comment.getCommentStr());
					commentNativeObj.put("createdTime", commentNativeObj, comment.getCreatedTime().toString());
					nativeArr.put(i, nativeArr, commentNativeObj);
					i++;
				}
			}
		} catch (CommentStreamException e) {
			log.error("Error while loading comments.....", e);
		}
		return nativeArr;

	}

	private static boolean validate(Object[] args) {
		int argsCount = args.length;
		if (argsCount != 3) {
			return false;
		}
		for (int i = 0; i < argsCount; i++) {
			if (!(args[i] instanceof String)) {
				return false;
			}
		}
		return true;
	}

}
