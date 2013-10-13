/*
 *  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.remotetasks.core;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.ntask.core.AbstractTask;
import org.wso2.carbon.remotetasks.common.RemoteTasksConstants;

/**
 * Task class implementation for remote tasks.
 */
public class RemoteTask extends AbstractTask {

	private static final int DEFAULT_CONNECTION_TIMEOUT = 20000;
	
	private final Log log = LogFactory.getLog(RemoteTask.class);
	
	@Override
	public void execute() {
		boolean systemTask = this.isSystemTask();
		if (!systemTask) {
		    this.notifyTaskManager();
		}
		String targetURI = this.getProperties().get(RemoteTasksConstants.REMOTE_TASK_URI);
		if (targetURI == null || targetURI.length() == 0) {
			return;
		}
		HttpClient client = new HttpClient();
		client.getParams().setSoTimeout(DEFAULT_CONNECTION_TIMEOUT);
		HttpMethod method = new GetMethod(targetURI);
		if (systemTask) {
			method.setRequestHeader(RemoteTasksConstants.REMOTE_SYSTEM_TASK_HEADER_ID, 
					this.getProperties().get(RemoteTasksConstants.REMOTE_SYSTEM_TASK_ID));
		}
		try {
			if (log.isDebugEnabled()) {
				log.debug("Executing remote task to URI: " + targetURI);
			}
			client.executeMethod(method);
			if (log.isDebugEnabled()) {
				StringBuilder builder = new StringBuilder();
				builder.append("Response Headers:-\n");
				for (Header header : method.getResponseHeaders()) {
					builder.append("\t" + header.getName() + ": " + header.getValue() + "\n");
				}
				log.debug(builder.toString());
			}
			String body = method.getResponseBodyAsString();
			if (log.isDebugEnabled()) {
				log.debug("Response Body:-\n\t" + body);
			}
			method.releaseConnection();
		} catch (Exception e) {
			log.error("Error executing remote task: " + e.getMessage(), e);
		}
	}
	
	private boolean isSystemTask() {
		String systemTaskFlag = this.getProperties().get(RemoteTasksConstants.SYSTEM_TASK_FLAG);
		return (systemTaskFlag != null && Boolean.parseBoolean(systemTaskFlag));
	}
	
	/**
	 * This notifies the task manager that this task was executed at this time, this can be used
	 * for monitoring, billing etc.. requirements.
	 */
	private void notifyTaskManager() {
		int tenantId = PrivilegedCarbonContext.getCurrentContext().getTenantId();
		String taskName = this.getProperties().get(RemoteTasksConstants.REMOTE_TASK_NAME);
		RemoteTaskManager.getInstance().userTaskExecuted(tenantId, taskName);
	}

}
