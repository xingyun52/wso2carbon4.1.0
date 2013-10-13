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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.common.TaskException.Code;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.remotetasks.common.DeployedTaskInformation;
import org.wso2.carbon.remotetasks.common.RemoteTasksConstants;
import org.wso2.carbon.remotetasks.common.RemoteTasksException;
import org.wso2.carbon.remotetasks.common.StaticTaskInformation;
import org.wso2.carbon.remotetasks.common.TriggerInformation;
import org.wso2.carbon.remotetasks.core.internal.RemoteTasksServiceComponent;

/**
 * This class represents the remote task manager.
 */
public class RemoteTaskManager {

	private final Log log = LogFactory.getLog(RemoteTaskManager.class);
	
	private static RemoteTaskManager instance;
	
	static {
		instance = new RemoteTaskManager();
	}
	
	private RemoteTaskManager() {
	}
	
	public static RemoteTaskManager getInstance() {
		return instance;
	}
	
	private TaskManager getTaskManager(boolean system) throws RemoteTasksException {
		try {
			return RemoteTasksServiceComponent.getTaskService().getTaskManager(
					system ? RemoteTasksConstants.TASK_TYPE_SYSTEM : 
						RemoteTasksConstants.TASK_TYPE_USER);
		} catch (TaskException e) {
			throw new RemoteTasksException(e.getMessage(), e);
		}
	}
	
	private int getCurrentTenantId() {
		return CarbonContext.getCurrentContext().getTenantId();
	}
	
	private void checkSystemRequest() throws RemoteTasksException {
		if (this.getCurrentTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
			throw new RemoteTasksException(
					"System request verification failed, " +
					"only Super-Tenant can make this type of requests");
		}
	}
	
	private void addTask(StaticTaskInformation stTaskInfo, 
			boolean system) throws RemoteTasksException {
		try {
			TaskManager tm = this.getTaskManager(system);
			TaskInfo taskInfo = RemoteTaskUtils.convert(stTaskInfo, system);
			tm.registerTask(taskInfo);
			tm.scheduleTask(taskInfo.getName());
		} catch (TaskException e) {
			throw new RemoteTasksException(e.getMessage(), e);
		}
	}
	
	public void addTask(StaticTaskInformation stTaskInfo) throws RemoteTasksException {
		this.addTask(stTaskInfo, false);
	}
	
	public void addSystemTask(int targetTenantId, 
			StaticTaskInformation stTaskInfo) throws RemoteTasksException {
		this.checkSystemRequest();
		try {
			PrivilegedCarbonContext.startTenantFlow();
			PrivilegedCarbonContext.getCurrentContext().setTenantId(targetTenantId);
			this.addTask(stTaskInfo, true);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}
	
	private void rescheduleTask(String taskName, TriggerInformation stTriggerInfo, 
			boolean system) throws RemoteTasksException {
		try {
			TaskManager tm = this.getTaskManager(system);
			TaskInfo taskInfo = tm.getTask(taskName);
			StaticTaskInformation stTaskInfo = RemoteTaskUtils.convert(taskInfo);
			stTaskInfo.setTriggerInformation(stTriggerInfo);
			taskInfo = RemoteTaskUtils.convert(stTaskInfo, system);
			tm.registerTask(taskInfo);
			tm.rescheduleTask(taskInfo.getName());
		} catch (TaskException e) {
			throw new RemoteTasksException(e.getMessage(), e);
		}
	}
	
	public void rescheduleTask(String taskName, 
			TriggerInformation stTriggerInfo) throws RemoteTasksException {
		this.rescheduleTask(taskName, stTriggerInfo, false);
	}
	
	public void rescheduleSystemTask(int targetTenantId, String taskName, 
			TriggerInformation stTriggerInfo) throws RemoteTasksException {
		this.checkSystemRequest();
		try {
			PrivilegedCarbonContext.startTenantFlow();
			PrivilegedCarbonContext.getCurrentContext().setTenantId(targetTenantId);
			this.rescheduleTask(taskName, stTriggerInfo, true);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}
	
	private boolean deleteTask(String taskName, 
			boolean system) throws RemoteTasksException {
		try {
			TaskManager tm = this.getTaskManager(system);
			return tm.deleteTask(taskName);
		} catch (TaskException e) {
			throw new RemoteTasksException(e.getMessage(), e);
		}
	}
	
	public boolean deleteTask(String taskName) throws RemoteTasksException {
		return this.deleteTask(taskName, false);
	}
	
	public boolean deleteSystemTask(int targetTenantId,
			String taskName) throws RemoteTasksException {
		this.checkSystemRequest();
		try {
			PrivilegedCarbonContext.startTenantFlow();
			PrivilegedCarbonContext.getCurrentContext().setTenantId(targetTenantId);
			return this.deleteTask(taskName, true);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}
	
	private void pauseTask(String taskName, 
			boolean system) throws RemoteTasksException {
		try {
			TaskManager tm = this.getTaskManager(system);
			tm.pauseTask(taskName);
		} catch (TaskException e) {
			throw new RemoteTasksException(e.getMessage(), e);
		}
	}
	
	public void pauseTask(String taskName) throws RemoteTasksException {
		this.pauseTask(taskName, false);
	}
	
	public void pauseSystemTask(int targetTenantId,
			String taskName) throws RemoteTasksException {
		this.checkSystemRequest();
		try {
			PrivilegedCarbonContext.startTenantFlow();
			PrivilegedCarbonContext.getCurrentContext().setTenantId(targetTenantId);
			this.pauseTask(taskName, true);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}
	
	private void resumeTask(String taskName, 
			boolean system) throws RemoteTasksException {
		try {
			TaskManager tm = this.getTaskManager(system);
			tm.resumeTask(taskName);
		} catch (TaskException e) {
			throw new RemoteTasksException(e.getMessage(), e);
		}
	}
	
	public void resumeTask(String taskName) throws RemoteTasksException {
		this.resumeTask(taskName, false);
	}
	
	public void resumeSystemTask(int targetTenantId,
			String taskName) throws RemoteTasksException {
		this.checkSystemRequest();
		try {
			PrivilegedCarbonContext.startTenantFlow();
			PrivilegedCarbonContext.getCurrentContext().setTenantId(targetTenantId);
			this.resumeTask(taskName, true);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}
	
	private DeployedTaskInformation getTask(String taskName, 
			boolean system) throws RemoteTasksException {
		try {
			TaskManager tm = this.getTaskManager(system);
			TaskInfo taskInfo;
			try {
			    taskInfo = tm.getTask(taskName);
			} catch (TaskException e) {
				if (e.getCode() == Code.NO_TASK_EXISTS) {
					return null;
				} else {
				    throw e;
				}
			}
			StaticTaskInformation stTaskInfo = RemoteTaskUtils.convert(taskInfo);
			DeployedTaskInformation depTaskInfo = new DeployedTaskInformation();
			depTaskInfo.setStaticTaskInfo(stTaskInfo);
			depTaskInfo.setStatus(tm.getTaskState(taskName).toString());
			return depTaskInfo;
		} catch (TaskException e) {
			throw new RemoteTasksException(e.getMessage(), e);
		}
	}
	
	public DeployedTaskInformation getTask(String taskName) throws RemoteTasksException {
		return this.getTask(taskName, false);
	}
	
	public DeployedTaskInformation getSystemTask(int targetTenantId, 
			String taskName) throws RemoteTasksException {
		this.checkSystemRequest();
		try {
			PrivilegedCarbonContext.startTenantFlow();
			PrivilegedCarbonContext.getCurrentContext().setTenantId(targetTenantId);
			return this.getTask(taskName, true);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}
	
	private String[] getAllTasks(boolean system) throws RemoteTasksException {
		try {
			TaskManager tm = this.getTaskManager(system);
			List<TaskInfo> taskList = tm.getAllTasks();
			List<String> result = new ArrayList<String>();
			for (TaskInfo taskInfo : taskList) {
				result.add(taskInfo.getName());
			}
			return result.toArray(new String[result.size()]);
		} catch (TaskException e) {
			throw new RemoteTasksException(e.getMessage(), e);
		}
	}
	
	public String[] getAllTasks() throws RemoteTasksException {
		return this.getAllTasks(false);
	}
	
	public String[] getAllSystemTasks(int targetTenantId) throws RemoteTasksException {
		this.checkSystemRequest();
		try {
			PrivilegedCarbonContext.startTenantFlow();
			PrivilegedCarbonContext.getCurrentContext().setTenantId(targetTenantId);
			return this.getAllTasks(true);
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}
	
	public void userTaskExecuted(int tenantId, String taskName) {
		if (log.isDebugEnabled()) {
			log.debug("Task Manager notified of task execution, TID: " + 
					tenantId + ", Task Name: " + taskName);
		}
	}
	
}
