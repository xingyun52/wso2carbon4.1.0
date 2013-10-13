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

public class Cartridge {

	private String cartridgeName;
	private String cartridgeType;
	private int activeInstances;
	private String status;
	private String ip;
	private String password;
	private String provider;
	private String version;
	private String hostName;
	private int minInstanceCount;
	private int maxInstanceCount;
	private String repoURL;
    private String dbUserName;

	public String getCartridgeName() {
		return cartridgeName;
	}

	public void setCartridgeName(String cartridgeName) {
		this.cartridgeName = cartridgeName;
	}

	public String getCartridgeType() {
		return cartridgeType;
	}

	public void setCartridgeType(String cartridgeType) {
		this.cartridgeType = cartridgeType;
	}

	public int getActiveInstances() {
		return activeInstances;
	}

	public void setActiveInstances(int activeInstances) {
		this.activeInstances = activeInstances;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getProvider() {
    	return provider;
    }

	public void setProvider(String provider) {
    	this.provider = provider;
    }

	public String getVersion() {
    	return version;
    }

	public void setVersion(String version) {
    	this.version = version;
    }

	public String getHostName() {
    	return hostName;
    }

	public void setHostName(String hostName) {
    	this.hostName = hostName;
    }

	public int getMinInstanceCount() {
    	return minInstanceCount;
    }

	public void setMinInstanceCount(int minInstanceCount) {
    	this.minInstanceCount = minInstanceCount;
    }

	public int getMaxInstanceCount() {
    	return maxInstanceCount;
    }

	public void setMaxInstanceCount(int maxInstanceCount) {
    	this.maxInstanceCount = maxInstanceCount;
    }

	public String getRepoURL() {
    	return repoURL;
    }

	public void setRepoURL(String repoURL) {
    	this.repoURL = repoURL;
    }


    public String getDbUserName() {
        return dbUserName;
    }

    public void setDbUserName(String dbUserName) {
        this.dbUserName = dbUserName;
    }
}
