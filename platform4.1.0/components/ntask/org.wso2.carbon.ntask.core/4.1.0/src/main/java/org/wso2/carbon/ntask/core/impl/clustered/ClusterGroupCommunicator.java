/**
 *  Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.coordination.common.CoordinationException;
import org.wso2.carbon.coordination.common.CoordinationException.ExceptionCode;
import org.wso2.carbon.coordination.core.sync.Group;
import org.wso2.carbon.coordination.core.sync.GroupEventListener;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.common.TaskException.Code;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.ntask.core.internal.TasksDSComponent;
import org.wso2.carbon.ntask.core.service.TaskService;

/**
 * This class represents the cluster group communicator used by clustered task managers.
 */
public class ClusterGroupCommunicator {

	public static final String TASK_SERVER_COUNT_SYS_PROP = "task.server.count";
	
	public static final String CARBON_TASK_GROUP_BASE = "__CARBON_TASK_GROUP_";
	
	public static final String CARBON_TASK_SERVER_STARTUP_GROUP = CARBON_TASK_GROUP_BASE + "__SERVER_STARTUP_GROUP__";
	
	private static final Log log = LogFactory.getLog(ClusterGroupCommunicator.class);
	
	private static ClusterGroupCommunicator instance;
	
	private Map<String, ClusterGroup> clusterGroupMap;
	
	private TaskService taskService;
	
	public static ClusterGroupCommunicator getInstance() throws TaskException {
		if (instance == null) {
			synchronized (ClusterGroupCommunicator.class) {
				if (instance == null) {
					instance = new ClusterGroupCommunicator(TasksDSComponent.getTaskService());
				}
			}
		}
		return instance;
	}
	
	private ClusterGroupCommunicator(TaskService taskService) throws TaskException {
		this.taskService = taskService;
		this.clusterGroupMap = new HashMap<String, ClusterGroupCommunicator.ClusterGroup>();
	}
	
	public void checkServers() throws CoordinationException {
		int serverCount = this.getTaskService().getServerConfiguration().getTaskServerCount();
		if (serverCount != -1) {
			log.info("Waiting for " + serverCount + " task servers...");
			Group group = TasksDSComponent.getCoordinationService().createGroup(
					CARBON_TASK_SERVER_STARTUP_GROUP);
			group.waitForMemberCount(serverCount);
			log.info("All task servers activated.");
		}		
	}
	
	public void newTaskTypeAdded(String taskType) throws TaskException {
		this.clusterGroupMap.put(taskType, new ClusterGroup(taskType));
	}
	
	public TaskService getTaskService() {
		return taskService;
	}
	
	public ClusterGroup getClusterGroup(String taskType) {
		return clusterGroupMap.get(taskType);
	}
	
	public String getLeaderId(String taskType) throws CoordinationException {
		return this.getClusterGroup(taskType).getLeaderId();
	}
	
	public List<String> getMemberIds(String taskType) throws CoordinationException {
		return this.getClusterGroup(taskType).getMemberIds();
	}
	
	public String getMemberId(String taskType) throws CoordinationException {
		return this.getClusterGroup(taskType).getMemberId();
	}
	
	public boolean isLeader(String taskType) throws TaskException {
		return this.getClusterGroup(taskType).isLeader();
	}
	
	public byte[] sendReceive(int tenantId, String taskType, String memberId,
			String messageHeader, byte[] payload) throws Exception {
		return this.getClusterGroup(taskType).sendReceive(
				tenantId, memberId, messageHeader, payload);
	}
	
	public class ClusterGroup implements GroupEventListener {
		
		private String taskType;
		
		private Group group;
		
        private boolean leader;
				
		public ClusterGroup(String taskType) throws TaskException {
			this.taskType = taskType;
			try {
				this.group = TasksDSComponent.getCoordinationService().createGroup(
						CARBON_TASK_GROUP_BASE + this.getTaskType());
				this.group.setGroupEventListener(this);
				try {
				    this.leader = this.getGroup().getLeaderId().equals(this.getGroup().getMemberId());
				} catch (CoordinationException e) {
					throw new TaskException("Error in creating cluster group: " + 
				            e.getMessage(), Code.UNKNOWN, e);
				}
			} catch (CoordinationException e) {
				throw new TaskException(e.getMessage(), Code.UNKNOWN, e);
			}
		}
		
		public List<String> getMemberIds() throws CoordinationException {
			return this.getGroup().getMemberIds();
		}
		
		public String getMemberId() throws CoordinationException {
			return this.getGroup().getMemberId();
		}
		
		public String getLeaderId() throws CoordinationException {
			return this.getGroup().getLeaderId();
		}
		
		public Group getGroup() {
			return group;
		}

		public byte[] sendReceive(int tenantId, String memberId,
				String messageHeader, byte[] payload) throws Exception {
			OperationRequest req = new OperationRequest(tenantId, messageHeader, payload);
			byte[] result = this.getGroup().sendReceive(memberId, objectToBytes(req));
			OperationResponse res = (OperationResponse) bytesToObject(result);
			return res.getPayload();
		}
		
		@Override
		public void onLeaderChange(String leaderId) {
			if (log.isDebugEnabled()) {
				log.info("Task server leader changed [" + this.getTaskType() + "]: " + leaderId);
			}
			this.leader = leaderId.equals(this.getGroup().getMemberId());
			try {
				if (this.isLeader()) {
					log.info("Task server leader changed [" + this.getTaskType() + "], rescheduling missing tasks...");
					this.scheduleAllMissingTasks();
				}
			} catch (TaskException e) {
				log.error("Error in scheduling missing tasks: " + e.getMessage(), e);
			}
		}
		
		public boolean isLeader() {
			return leader;
		}
		
		public String getTaskType() {
			return taskType;
		}
		
		@Override
		public void onGroupMessage(byte[] buff) {
		}

		@Override
		public void onMemberArrival(String mid) {
			if (log.isDebugEnabled()) {
				log.debug("Task member arrived: " + mid);
			}
		}
		
		private void scheduleAllMissingTasks() throws TaskException {
			for (TaskManager tm : getTaskService().getAllTenantTaskManagersForType(this.getTaskType())) {
				if (tm instanceof ClusteredTaskManager) {
					((ClusteredTaskManager) tm).scheduleMissingTasks();
				}
			}
		}

		@Override
		public void onMemberDeparture(String mid) {
			if (log.isDebugEnabled()) {
				log.debug("Task member departed: " + mid);
			}
			try {
				if (this.isLeader()) {
					log.info("Task member departed [" + this.getTaskType() + "], rescheduling missing tasks...");
					this.scheduleAllMissingTasks();
				}
			} catch (TaskException e) {
				log.error("Error in scheduling missing tasks: " + e.getMessage(), e);
			}
		}

		@Override
		public byte[] onPeerMessage(byte[] buff) throws CoordinationException {
			try {
			    OperationRequest req = (OperationRequest) bytesToObject(buff);
			    try {
			    	PrivilegedCarbonContext.startTenantFlow();
			    	PrivilegedCarbonContext.getCurrentContext().setTenantId(req.getTenantId());
			    	TaskManager tm = getTaskService().getTaskManager(this.getTaskType());
			    	if (tm instanceof ClusteredTaskManager) {
			    		OperationResponse res = ((ClusteredTaskManager) tm).onOperationRequest(req);
			    		return objectToBytes(res);
			    	} else {
			    		throw new CoordinationException(
			    				"Invalid task manager type, expected 'clustered' type, got: " + tm, 
			    				ExceptionCode.GENERIC_ERROR);
			    	}
			    } finally {
			    	PrivilegedCarbonContext.endTenantFlow();
			    }
			} catch (Exception e) {
				throw new CoordinationException(e.getMessage(), ExceptionCode.GENERIC_ERROR, e);
			}
		}
		
	}

	public static byte[] objectToBytes(Object obj) throws Exception {
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
		objOut.writeObject(obj);
		objOut.close();
		return byteOut.toByteArray();
	}
	
	public static Object bytesToObject(byte[] data) throws Exception {
		ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
		ObjectInputStream objIn = new ObjectInputStream(byteIn);
		Object obj = objIn.readObject();
		objIn.close();
		return obj;
	}
	
	public static class OperationRequest implements Serializable {
		
		private static final long serialVersionUID = 1L;

		private int tenantId;
				
		private String opName;
		
		private byte[] payload;
		
		public OperationRequest(int tenantId, String opName, byte[] payload) {
			this.tenantId = tenantId;
			this.opName = opName;
			this.payload = payload;
		}

		public int getTenantId() {
			return tenantId;
		}

		public String getOpName() {
			return opName;
		}

		public byte[] getPayload() {
			return payload;
		}

	}
	
	public static class OperationResponse implements Serializable {
				
		private static final long serialVersionUID = 1L;
		
		private byte[] payload;
		
		public OperationResponse(byte[] payload) {
			this.payload = payload;
		}

		public byte[] getPayload() {
			return payload;
		}

	}

}
