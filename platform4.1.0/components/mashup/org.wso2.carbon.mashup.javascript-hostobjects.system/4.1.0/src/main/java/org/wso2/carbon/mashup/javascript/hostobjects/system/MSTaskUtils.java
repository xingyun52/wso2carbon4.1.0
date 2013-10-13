/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.mashup.javascript.hostobjects.system;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.apache.axiom.om.util.Base64;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskInfo.TriggerInfo;

/**
 * This class represents a utility class for scheduled tasks.  
 */
public class MSTaskUtils {

	public static MSTaskInfo convert(TaskInfo taskInfo) {
		MSTaskInfo msTaskInfo = new MSTaskInfo();
		msTaskInfo.setName(taskInfo.getName());
		TriggerInfo triggerInfo = taskInfo.getTriggerInfo();
		msTaskInfo.setCronExpression(triggerInfo.getCronExpression());
		msTaskInfo.setStartTime(dateToCal(triggerInfo.getStartTime()));
		msTaskInfo.setEndTime(dateToCal(triggerInfo.getEndTime()));
		msTaskInfo.setTaskCount(triggerInfo.getRepeatCount());
		msTaskInfo.setTaskInterval(triggerInfo.getIntervalMillis());
		return msTaskInfo;
	}
	
	public static TaskInfo convert(MSTaskInfo msTaskInfo) {
		TriggerInfo triggerInfo = new TriggerInfo();
		triggerInfo.setCronExpression(msTaskInfo.getCronExpression());
		if (msTaskInfo.getStartTime() != null) {
		    triggerInfo.setStartTime(msTaskInfo.getStartTime().getTime());
		}
		if (msTaskInfo.getEndTime() != null) {
		    triggerInfo.setEndTime(msTaskInfo.getEndTime().getTime());
		}
		triggerInfo.setIntervalMillis((int)msTaskInfo.getTaskInterval());
		triggerInfo.setRepeatCount(msTaskInfo.getTaskCount());
		return new TaskInfo(msTaskInfo.getName(), MSTask.class.getName(), 
				msTaskInfo.getTaskProperties(), triggerInfo);
	}

	
	public static Calendar dateToCal(Date date) {
		if (date == null) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}

	public static synchronized Object fromString(String encodedString) 
			throws IOException , ClassNotFoundException {
        byte [] data = Base64.decode(encodedString);
        ObjectInputStream objectInputStream = new ObjectInputStream( 
                                        new ByteArrayInputStream(data));
        Object readObject  = objectInputStream.readObject();
        objectInputStream.close();
        return readObject;
    }

	public static synchronized String toString(Serializable object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.close();
        return new String(Base64.encode(byteArrayOutputStream.toByteArray()));
    }

    public static String getTenantDomainFromId(int tid) {
        PrivilegedCarbonContext.startTenantFlow();
        PrivilegedCarbonContext.getCurrentContext().setTenantId(tid);
        String tenantDomain = PrivilegedCarbonContext.getCurrentContext().getTenantDomain();
        PrivilegedCarbonContext.endTenantFlow();
        return tenantDomain;
    }
}
