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
 * Task information which is required to add/update tasks, and also the current status of them.
 */
public class DeployedTaskInformation {

	private StaticTaskInformation staticTaskInfo;
	
	private String status;
	
	public DeployedTaskInformation() {
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}

	public StaticTaskInformation getStaticTaskInfo() {
		return staticTaskInfo;
	}

	public void setStaticTaskInfo(StaticTaskInformation staticTaskInfo) {
		this.staticTaskInfo = staticTaskInfo;
	}
	
}
