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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskInfo.TriggerInfo;
import org.wso2.carbon.remotetasks.common.RemoteTasksConstants;
import org.wso2.carbon.remotetasks.common.RemoteTasksException;
import org.wso2.carbon.remotetasks.common.StaticTaskInformation;
import org.wso2.carbon.remotetasks.common.TriggerInformation;

/**
 * This class contains utility methods for remote task related activities.
 */
public class RemoteTaskUtils {

	public static StaticTaskInformation convert(TaskInfo taskInfo) throws RemoteTasksException {
		StaticTaskInformation stTaskInfo = new StaticTaskInformation();
		stTaskInfo.setName(taskInfo.getName());
		Map<String, String> taskProps = taskInfo.getProperties();
		stTaskInfo.setTargetURI(taskProps.get(RemoteTasksConstants.REMOTE_TASK_URI));
		TriggerInfo triggerInfo = taskInfo.getTriggerInfo();
		TriggerInformation stTriggerInfo = new TriggerInformation();
		stTriggerInfo.setCronExpression(triggerInfo.getCronExpression());
		stTriggerInfo.setStartTime(dateToCal(triggerInfo.getStartTime()));
		stTriggerInfo.setEndTime(dateToCal(triggerInfo.getEndTime()));
		stTriggerInfo.setTaskCount(triggerInfo.getRepeatCount());
		stTriggerInfo.setTaskInterval(triggerInfo.getIntervalMillis());
		stTaskInfo.setTriggerInformation(stTriggerInfo);
		stTaskInfo.setAllowConcurrentExecutions(!triggerInfo.isDisallowConcurrentExecution());
		return stTaskInfo;
	}
	
	public static TaskInfo convert(StaticTaskInformation stTaskInfo, 
			boolean system) throws RemoteTasksException {
		TriggerInfo triggerInfo = new TriggerInfo();
		TriggerInformation stTriggerInfo = stTaskInfo.getTriggerInformation();
		if (stTriggerInfo == null) {
			throw new RemoteTasksException("Trigger Information cannot be empty");
		}
		String cron = stTriggerInfo.getCronExpression();
		if (cron != null && cron.trim().length() == 0) {
			cron = null;
		}
		triggerInfo.setCronExpression(cron);
		if (stTriggerInfo.getStartTime() != null) {
		    triggerInfo.setStartTime(stTriggerInfo.getStartTime().getTime());
		}
		if (stTriggerInfo.getEndTime() != null) {
		    triggerInfo.setEndTime(stTriggerInfo.getEndTime().getTime());
		} 
		if (cron == null && !system && stTriggerInfo.getTaskInterval() < 1000) {
			throw new RemoteTasksException("Task interval cannot be less than 1 second for user tasks");
		}
		triggerInfo.setIntervalMillis(stTriggerInfo.getTaskInterval());
		triggerInfo.setRepeatCount(stTriggerInfo.getTaskCount());
		triggerInfo.setDisallowConcurrentExecution(!stTaskInfo.isAllowConcurrentExecutions());
		Map<String, String> props = new HashMap<String, String>();
		props.put(RemoteTasksConstants.REMOTE_TASK_NAME, stTaskInfo.getName());
		String targetUrl = stTaskInfo.getTargetURI();
		if (system) {
			int i1 = targetUrl.lastIndexOf("/");
			String systemTaskId = targetUrl.substring(i1 + 1);
			targetUrl = targetUrl.substring(0, i1);
			props.put(RemoteTasksConstants.SYSTEM_TASK_FLAG, Boolean.toString(true));
			props.put(RemoteTasksConstants.REMOTE_SYSTEM_TASK_ID, systemTaskId);
		}
		props.put(RemoteTasksConstants.REMOTE_TASK_URI, targetUrl);
		return new TaskInfo(stTaskInfo.getName(), RemoteTask.class.getName(), props, triggerInfo);
	}
	
	private static Calendar dateToCal(Date date) {
		if (date == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}
	
}
