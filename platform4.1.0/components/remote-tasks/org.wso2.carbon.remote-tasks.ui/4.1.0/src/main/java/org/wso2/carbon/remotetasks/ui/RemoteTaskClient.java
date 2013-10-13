package org.wso2.carbon.remotetasks.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.remotetasks.stub.admin.common.RemoteTaskAdminStub;
import org.wso2.carbon.remotetasks.stub.admin.common.xsd.StaticTaskInformation;
import org.wso2.carbon.remotetasks.stub.admin.common.xsd.TriggerInformation;
import org.wso2.carbon.remotetasks.stub.admin.common.xsd.DeployedTaskInformation;

public class RemoteTaskClient {
	
	private RemoteTaskAdminStub stub;
	private static final Log log = LogFactory.getLog(RemoteTaskClient.class);
	
	public RemoteTaskClient(String cookie, String backendUrl,
            ConfigurationContext ctxt) throws AxisFault {
		String serviceEPR = null;
		try {
			serviceEPR = backendUrl + "RemoteTaskAdmin";
			stub = new RemoteTaskAdminStub(ctxt, serviceEPR);
			ServiceClient client = stub._getServiceClient();
			Options options = client.getOptions();
			options.setManageSession(true);
			options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
		} catch (AxisFault e) {
			log.error("Error occurred while connecting via stub to : " + serviceEPR, e);
			throw e;
		}
	}
	
	public void addRemoteTask(StaticTaskInformation taskInfo) throws Exception {
		stub.addRemoteTask(taskInfo);
	}
	
	public void rescheduleRemoteTask(String taskName, 
			TriggerInformation stTriggerInfo) throws Exception {
		stub.rescheduleRemoteTask(taskName, stTriggerInfo);
	}
	
	public DeployedTaskInformation getRemoteTask(String name) throws Exception {
		return stub.getRemoteTask(name);
	}
	
	public boolean deleteRemoteTask(String name) throws Exception {
		return stub.deleteRemoteTask(name);
	}
	
	public void pauseRemoteTask(String name) throws Exception {
		stub.pauseRemoteTask(name);
	}
	
	public void resumeRemoteTask(String name) throws Exception {
		stub.resumeRemoteTask(name);
	}
	
	public String[] getAllRemoteTasks() throws Exception {
		return stub.getAllRemoteTasks();
	}
	
}
