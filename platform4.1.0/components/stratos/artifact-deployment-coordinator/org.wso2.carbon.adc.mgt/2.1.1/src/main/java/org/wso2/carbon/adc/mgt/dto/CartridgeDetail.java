/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.dto;

import java.util.List;


// This is a temporary object, assuming the type of object returned by
// cloud controller service, getCartridgeInfo
public class CartridgeDetail {

	private String cartridgeName;
	private String provider;
	private String hostName;
	private List<String> deploymentPathList;
	private List<String> portMappings;
	private String appPath;

	public String getCartridgeName() {
		return cartridgeName;
	}

	public void setCartridgeName(String cartridgeName) {
		this.cartridgeName = cartridgeName;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public List<String> getDeploymentPathList() {
		return deploymentPathList;
	}

	public void setDeploymentPathList(List<String> deploymentPathList) {
		this.deploymentPathList = deploymentPathList;
	}

	public List<String> getPortMappings() {
		return portMappings;
	}

	public void setPortMappings(List<String> portMappings) {
		this.portMappings = portMappings;
	}

	public String getAppPath() {
		return appPath;
	}

	public void setAppPath(String appPath) {
		this.appPath = appPath;
	}
}
