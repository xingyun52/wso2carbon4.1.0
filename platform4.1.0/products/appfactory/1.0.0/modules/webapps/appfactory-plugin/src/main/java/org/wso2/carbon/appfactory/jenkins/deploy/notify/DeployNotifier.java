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
package org.wso2.carbon.appfactory.jenkins.deploy.notify;

import java.rmi.RemoteException;
import java.util.Map;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.application.deployer.stub.ApplicationDeployerAppFactoryExceptionException;
import org.wso2.carbon.appfactory.application.deployer.stub.ApplicationDeployerStub;

import org.wso2.carbon.appfactory.jenkins.api.JenkinsAPI;
import org.wso2.carbon.appfactory.jenkins.api.JenkinsAPIException;
import org.wso2.carbon.appfactory.jenkins.api.JenkinsRESTAPI;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Class which notifies registered observers when the Deployment
 * Events are occured.
 * 
 * @author shamika
 * 
 */
public class DeployNotifier {

	private static final Log log = LogFactory.getLog(DeployNotifier.class);
	
	
	/**
	 * Notifications are sent when deployed.
	 * 
	 * @param jobName
	 */
	public void deployed(String jobName, String stage, String userName, String password,String endpoint) {
	
		log.info("Observer is called to updated deployed information");
		JenkinsAPI jenkinsApi = new JenkinsRESTAPI();
		try {
			Map<String, String> lastBuildInformation = jenkinsApi.getLastBuildInformation(jobName);
			
			String buildNumber = lastBuildInformation.get("number");
			String[] jobValues = jobName.split("-");

            String serviceEndpoint = endpoint + "/services/ApplicationDeployer";

			ApplicationDeployerStub deployer = new ApplicationDeployerStub(serviceEndpoint);
			ServiceClient client = deployer._getServiceClient();
		    CarbonUtils.setBasicAccessSecurityHeaders(userName, password, client);
			deployer.updateDeploymentInformation(jobValues[0], stage, jobValues[1], buildNumber);

		} catch (JenkinsAPIException e) {
			String msg = "Problem when retrieving Build Information";
			log.error(msg, e);
		} catch (AxisFault e) {
			String msg = "Problem when calling  appfactory update service";
			log.error(msg, e);
		} catch (RemoteException e) {
			String msg = "Problem when calling  appfactory update service";
			log.error(msg, e);
		} catch (ApplicationDeployerAppFactoryExceptionException e) {
			String msg = "Problem when calling  appfactory update service";
			log.error(msg, e);
		}
	}

}
