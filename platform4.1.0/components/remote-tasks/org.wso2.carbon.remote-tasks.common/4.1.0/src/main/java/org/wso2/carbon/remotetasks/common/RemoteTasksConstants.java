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
package org.wso2.carbon.remotetasks.common;

/**
 * Constants related to remote tasks.
 */
public class RemoteTasksConstants {

	public static final String TASK_TYPE_USER = "CARBON_USER_REMOTE_TASKS";
	
	public static final String TASK_TYPE_SYSTEM = "CARBON_SYSTEM_REMOTE_TASKS";
	
	public static final String REMOTE_SYSTEM_TASK_HEADER_ID = "REMOTE_SYSTEM_TASK_ID";
	
	public static final String REMOTE_SYSTEM_TASK_ID = "__REMOTE_SYSTEM_TASK_ID__";
	
	public static final String REMOTE_TASK_URI = "__REMOTE_TASK_URI__";
	
	public static final String REMOTE_TASK_NAME = "__REMOTE_TASK_NAME__";
	
	public static final String SYSTEM_TASK_FLAG = "__SYSTEM_TASK__";
		
	public static final class TaskProperties {
		
		public static final String ALLOW_CONCURRENT_EXECUTIONS = "allow_concurrent_executions";
		
	}
	
}
