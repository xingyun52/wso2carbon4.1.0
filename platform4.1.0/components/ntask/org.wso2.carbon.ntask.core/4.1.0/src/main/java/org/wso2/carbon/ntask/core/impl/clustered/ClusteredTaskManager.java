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
package org.wso2.carbon.ntask.core.impl.clustered;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.coordination.common.CoordinationException;
import org.wso2.carbon.coordination.common.CoordinationException.ExceptionCode;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.common.TaskException.Code;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskLocationResolver;
import org.wso2.carbon.ntask.core.TaskRepository;
import org.wso2.carbon.ntask.core.TaskServiceContext;
import org.wso2.carbon.ntask.core.TaskUtils;
import org.wso2.carbon.ntask.core.impl.AbstractQuartzTaskManager;
import org.wso2.carbon.ntask.core.impl.clustered.ClusterGroupCommunicator.OperationRequest;
import org.wso2.carbon.ntask.core.impl.clustered.ClusterGroupCommunicator.OperationResponse;

/**
 * This class represents a clustered task manager, which is used when tasks are distributed across a
 * cluster.
 */
public class ClusteredTaskManager extends AbstractQuartzTaskManager {
	
	private static final Log log = LogFactory.getLog(ClusteredTaskManager.class);
	
	private static final String TASK_MEMBER_LOCATION_META_PROP_ID = "TASK_MEMBER_LOCATION_META_PROP_ID";
	
	public ClusteredTaskManager(TaskRepository taskRepository) throws TaskException {
		super(taskRepository);
	}
	
	public int getTenantId() {
		return this.getTaskRepository().getTenantId();
	}
	
	public String getTaskType() {
		return this.getTaskRepository().getTasksType();
	}
	
	public ClusterGroupCommunicator getClusterComm() throws TaskException {
		return ClusterGroupCommunicator.getInstance();
	}

	public void scheduleAllTasks() throws TaskException {
		if (this.isLeader()) {
			List<TaskInfo> tasks = this.getAllTasks();
			for (TaskInfo task : tasks) {
				try {
				    this.scheduleTask(task.getName());
				} catch (Exception e) {
					/* we should not want to throw an exception here, we will continue
					 * scheduling rest of the tasks */
					log.error("Error in scheduling task: " + e.getMessage(), e);
				}
			}
		}
	}
	
	public void scheduleMissingTasks() throws TaskException {
		List<List<TaskInfo>> tasksInServers = this.getAllTasksInServers();
		List<TaskInfo> scheduledTasks = new ArrayList<TaskInfo>();
		for (List<TaskInfo> entry : tasksInServers) {
			scheduledTasks.addAll(entry);
		}
		List<TaskInfo> allTasks = this.getAllTasks();
		List<TaskInfo> missingTasks = new ArrayList<TaskInfo>(allTasks);
		missingTasks.removeAll(scheduledTasks);
		for (TaskInfo task : missingTasks) {
			try {
			    this.scheduleTask(task.getName());
			} catch (Exception e) {
				log.error("Error in scheduling missing task: " + e.getMessage(), e);
			}
		}
	}
	
	public void scheduleTask(String taskName) throws TaskException {
		try {
			String memberId = this.getMemberIdFromTaskName(taskName, true);
		    this.scheduleTask(memberId, taskName);
		} catch (Exception e) {
			throw new TaskException("Error in scheduling task: " + taskName + " : "
		            + e.getMessage(), Code.UNKNOWN, e);
		}
	}
	
	public void rescheduleTask(String taskName) throws TaskException {
		try {
			String memberId = this.getMemberIdFromTaskName(taskName, true);
		    this.rescheduleTask(memberId, taskName);
		} catch (Exception e) {
			throw new TaskException("Error in rescheduling task: " + taskName + " : "
		            + e.getMessage(), Code.UNKNOWN, e);
		}
	}
	
	public List<TaskInfo> getTasksInServer(int location) throws TaskException {
		try {
			List<String> ids = this.getMemberIds();
		    String memberId = ids.get(location % ids.size());
		    return this.getTasksInServer(memberId);
		} catch (Exception e) {
			throw new TaskException("Error in getting tasks in server: " + location + " : " +
		            e.getMessage(), Code.UNKNOWN, e);
		}
	}
	
	public Map<String, TaskState> getAllTaskStates() throws TaskException {
		try {
			List<TaskInfo> tasks = this.getAllTasks();
		    Map<String, TaskState> result = new HashMap<String, TaskState>();
		    for (TaskInfo task : tasks) {
		    	result.put(task.getName(), this.getTaskState(task.getName()));
		    }
		    return result;
		} catch (Exception e) {
			throw new TaskException("Error in getting all task states: " + 
		            e.getMessage(), Code.UNKNOWN, e);
		}
	}
	
	public TaskState getTaskState(String taskName) throws TaskException {
		try {
			String memberId = this.getMemberIdFromTaskName(taskName, false);
		    return this.getTaskState(memberId, taskName);
		} catch (TaskException e) {
			if (e.getCode() == Code.NO_TASK_EXISTS) {
				return TaskState.NONE;
			} else {
				throw e;
			}
		} catch (Exception e) {
			throw new TaskException("Error in getting task state: " + taskName + " : "
		            + e.getMessage(), Code.UNKNOWN, e);
		}
	}

	public boolean deleteTask(String taskName) throws TaskException {
		try {
		    String memberId = this.getMemberIdFromTaskName(taskName, false);
		    boolean result = this.deleteTask(memberId, taskName);
		    /* the delete has to be done here, because, this would be the admin node with read/write
		     * registry access, and the target slave will not have write access */
		    result &= this.getTaskRepository().deleteTask(taskName);
		    return result;
		} catch (Exception e) {
			throw new TaskException("Error in deleting task: " + taskName + " : "
		            + e.getMessage(), Code.UNKNOWN, e);
		}
	}

	public void pauseTask(String taskName) throws TaskException {
		try {
		    String memberId = this.getMemberIdFromTaskName(taskName, false);
		    this.pauseTask(memberId, taskName);
		    TaskUtils.setTaskPaused(this.getTaskRepository(), taskName, true);
		} catch (Exception e) {
			throw new TaskException("Error in pausing task: " + taskName + " : "
		            + e.getMessage(), Code.UNKNOWN, e);
		}
	}
	
	public void resumeTask(String taskName) throws TaskException {
		try {
		    String memberId = this.getMemberIdFromTaskName(taskName, false);
		    this.resumeTask(memberId, taskName);
		    TaskUtils.setTaskPaused(this.getTaskRepository(), taskName, true);
		} catch (Exception e) {
			throw new TaskException("Error in resuming task: " + taskName + " : "
		            + e.getMessage(), Code.UNKNOWN, e);
		}
	}

	@Override
	public void registerTask(TaskInfo taskInfo) throws TaskException {
		this.registerLocalTask(taskInfo);
	}

	@Override
	public TaskInfo getTask(String taskName) throws TaskException {
		return this.getTaskRepository().getTask(taskName);
	}

	@Override
	public List<TaskInfo> getAllTasks() throws TaskException {
		return this.getTaskRepository().getAllTasks();
	}

	public int getServerCount() throws TaskException {
		try {
			return this.getMemberIds().size();
		} catch (CoordinationException e) {
			throw new TaskException("Error in getting server count: " + e.getMessage(), 
					Code.UNKNOWN, e);
		}
	}

	private TaskServiceContext getTaskServiceContext() throws TaskException {
		TaskServiceContext context = new TaskServiceContext(this.getTaskRepository(), 
				this.getServerCount());
		return context;
	}
	
	private String locateMemberForTask(String taskName) throws TaskException {
		int location = getTaskLocation(taskName);
		List<String> ids;
		try {
		    ids = this.getMemberIds();
		} catch (Exception e) {
			throw new TaskException("Error in getting member ids: " + 
					e.getMessage(), Code.UNKNOWN, e);
		}
		int index = location % ids.size();
		return ids.get(index);
	}
	
	private int getTaskLocation(String taskName) throws TaskException {
		TaskInfo taskInfo = this.getTask(taskName);
		TaskLocationResolver locationResolver;
		try {
			locationResolver = (TaskLocationResolver) Class.forName(
					taskInfo.getLocationResolverClass()).newInstance();
		} catch (Exception e) {
			throw new TaskException(e.getMessage(), Code.UNKNOWN, e);
		}
		return locationResolver.getLocation(this.getTaskServiceContext(), taskInfo);
	}
	
	public List<List<TaskInfo>> getAllTasksInServers() throws TaskException {
		List<List<TaskInfo>> result = new ArrayList<List<TaskInfo>>();
		try {
			List<String> ids = this.getMemberIds();
			for (int i = 0; i < ids.size(); i++) {
				result.add(this.getTasksInServer(i));
			}
		} catch (CoordinationException e) {
			throw new TaskException("Error in retreiving all tasks in servers: " + 
		            e.getMessage(), Code.UNKNOWN, e);
		}
		return result;
	}

	@Override
	public boolean isTaskScheduled(String taskName) throws TaskException {
		return this.getTaskState(taskName) != TaskState.NONE;
	}
	
	public byte[] sendReceive(String memberId,
			String messageHeader, byte[] payload) throws Exception {
		return this.getClusterComm().sendReceive(this.getTenantId(), 
				this.getTaskType(), memberId, messageHeader, payload);
	}
	
	public List<String> getMemberIds() throws CoordinationException, TaskException {
		return this.getClusterComm().getMemberIds(this.getTaskType());
	}
	
	public String getMemberId() throws CoordinationException, TaskException {
		return this.getClusterComm().getMemberId(this.getTaskType());
	}
	
	public String getLeaderId() throws CoordinationException, TaskException {
		return this.getClusterComm().getLeaderId(this.getTaskType());
	}
	
	public boolean isLeader() throws TaskException {
		return this.getClusterComm().isLeader(this.getTaskType());
	}

	public String getMemberIdFromTaskName(String taskName, 
			boolean createIfNotExists) throws TaskException, CoordinationException {
		String location = this.getServerLocationOfTask(taskName);
		if (location == null || !this.getMemberIds().contains(location)) {
			if (createIfNotExists) {
				location = this.locateMemberForTask(taskName);
			} else {
			    throw new TaskException("The task server cannot be located for task: " 
					    + taskName, Code.NO_TASK_EXISTS);
			}
		}
		return location;
	}

	private String getMemberIdFromTaskNameServer(String taskName)
			throws TaskException {
		return locateMemberForTask(taskName);
	}

	@SuppressWarnings("unchecked")
	public List<TaskInfo> getTasksInServer(String memberId) throws Exception {
		byte[] data = this.sendReceive(memberId,
				OperationNames.GET_TASKS_IN_SERVER, new byte[0]);
		return (List<TaskInfo>) ClusterGroupCommunicator.bytesToObject(data);
	}

	public List<TaskInfo> getTasksInServerServer() throws Exception {
		return getAllLocalScheduledTasks();
	}

	public TaskState getTaskState(String memberId, String taskName)
			throws Exception {
		byte[] data = this.sendReceive(memberId, OperationNames.GET_TASK_STATE,
				taskName.getBytes());
		return (TaskState) ClusterGroupCommunicator.bytesToObject(data);
	}

	public TaskState getTaskStateServer(String taskName) throws Exception {
		return getLocalTaskState(taskName);
	}

	public void scheduleTask(String memberId, String taskName) throws Exception {
		this.sendReceive(memberId, OperationNames.SCHEDULE_TASK,
				taskName.getBytes());
	}

	private void scheduleTaskServer(String taskName) throws Exception {
		this.scheduleLocalTask(taskName);
		this.setServerLocationOfTask(taskName, this.getMemberId());
	}

	public void rescheduleTask(String memberId, String taskName)
			throws Exception {
		this.sendReceive(memberId, OperationNames.RESCHEDULE_TASK,
				taskName.getBytes());
	}

	private void rescheduleTaskServer(String taskName) throws Exception {
		rescheduleLocalTask(taskName);
	}

	public boolean deleteTask(String memberId, String taskName) throws Exception {
		byte[] data = this.sendReceive(memberId, OperationNames.DELETE_TASK,
				taskName.getBytes());
		return (Boolean) ClusterGroupCommunicator.bytesToObject(data);
	}

	private boolean deleteTaskServer(String taskName) throws Exception {
		return deleteLocalTask(taskName, false);
	}

	public void pauseTask(String memberId, String taskName) throws Exception {
		this.sendReceive(memberId, OperationNames.PAUSE_TASK,
				taskName.getBytes());
	}

	private void pauseTaskServer(String taskName) throws Exception {
		pauseLocalTask(taskName);
	}

	public void resumeTask(String memberId, String taskName) throws Exception {
		this.sendReceive(memberId, OperationNames.RESUME_TASK,
				taskName.getBytes());
	}

	private void resumeTaskServer(String taskName) throws Exception {
		resumeLocalTask(taskName);
	}
	
	private void setServerLocationOfTask(String taskName, 
			String memberId) throws TaskException {
		this.getTaskRepository().setTaskMetadataProp(taskName, 
				TASK_MEMBER_LOCATION_META_PROP_ID, memberId);
	}
	
	private String getServerLocationOfTask(String taskName) throws TaskException {
		return this.getTaskRepository().getTaskMetadataProp(taskName, 
				TASK_MEMBER_LOCATION_META_PROP_ID);
	}

	public OperationResponse onOperationRequest(OperationRequest req)
			throws CoordinationException {
		try {
			byte[] result;
			if (OperationNames.MEMBER_ID_FROM_TASK_NAME.equals(req.getOpName())) {
				result = this.getMemberIdFromTaskNameServer(
						new String(req.getPayload())).getBytes();
			} else if (OperationNames.SCHEDULE_TASK.equals(req.getOpName())) {
				this.scheduleTaskServer(new String(req.getPayload()));
				result = new byte[0];
			} else if (OperationNames.RESCHEDULE_TASK.equals(req.getOpName())) {
				this.rescheduleTaskServer(new String(req.getPayload()));
				result = new byte[0];
			} else if (OperationNames.DELETE_TASK.equals(req.getOpName())) {
				boolean deleted = this.deleteTaskServer(new String(req.getPayload()));
				result = ClusterGroupCommunicator.objectToBytes(Boolean.valueOf(deleted));
			} else if (OperationNames.PAUSE_TASK.equals(req.getOpName())) {
				this.pauseTaskServer(new String(req.getPayload()));
				result = new byte[0];
			} else if (OperationNames.RESUME_TASK.equals(req.getOpName())) {
				this.resumeTaskServer(new String(req.getPayload()));
				result = new byte[0];
			} else if (OperationNames.GET_TASKS_IN_SERVER.equals(req.getOpName())) {
				List<TaskInfo> tasks = this.getTasksInServerServer();
				result = ClusterGroupCommunicator.objectToBytes(tasks);
			} else if (OperationNames.GET_TASK_STATE.equals(req.getOpName())) {
				TaskState taskState = this.getTaskStateServer(new String(req.getPayload()));
				result = ClusterGroupCommunicator.objectToBytes(taskState);
			} else {
				throw new CoordinationException("Unknown operation: " + req.getOpName());
			}
			return new OperationResponse(result);
		} catch (Exception e) {
			throw new CoordinationException(e.getMessage(), ExceptionCode.GENERIC_ERROR, e);
		}
	}

	public static final class OperationNames {
		
		public static final String MEMBER_ID_FROM_TASK_NAME = "MEMBER_ID_FROM_TASK_NAME";
		public static final String SCHEDULE_TASK = "SCHEDULE_TASK";
		public static final String RESCHEDULE_TASK = "RESCHEDULE_TASK";
		public static final String DELETE_TASK = "DELETE_TASK";
		public static final String PAUSE_TASK = "PAUSE_TASK";
		public static final String RESUME_TASK = "RESUME_TASK";
		public static final String GET_TASKS_IN_SERVER = "GET_TASKS_IN_SERVER";
		public static final String GET_TASK_STATE = "GET_TASK_STATE";
		
	}

}
