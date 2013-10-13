/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package org.wso2.carbon.appfactory.core.deploy;

/**
 * DTO to represent all the artifact related information.
 * @author shamika
 *
 */
public class Artifact {
	
	private String applicationKey;

	private String lastBuildStatus;
	
	private String version;
	
	private boolean isAutoBuild = false;
	
	private boolean isAutoDeploy = false;
	
	private String lastDeployedId;

	public Artifact(String applicationKey, String lastBuildStatus,
                    String version, boolean isAutoBuild, boolean isAutoDeploy, String lastDeployedId) {
	    super();
	    this.applicationKey = applicationKey;
	    this.lastBuildStatus = lastBuildStatus;
	    this.version = version;
	    this.isAutoBuild = isAutoBuild;
	    this.isAutoDeploy = isAutoDeploy;
	    this.lastDeployedId = lastDeployedId;
    }

	public String getApplicationKey() {
		return applicationKey;
	}

	public void setApplicationKey(String applicationKey) {
		this.applicationKey = applicationKey;
	}

	public String getLastBuildStatus() {
		return lastBuildStatus;
	}

	public void setLastBuildStatus(String lastSuccessBuildStatus) {
		this.lastBuildStatus = lastSuccessBuildStatus;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isAutoBuild() {
		return isAutoBuild;
	}

	public void setAutoBuild(boolean isAutoBuild) {
		this.isAutoBuild = isAutoBuild;
	}

	public boolean isAutoDeploy() {
		return isAutoDeploy;
	}

	public void setAutoDeploy(boolean isAutoDeploy) {
		this.isAutoDeploy = isAutoDeploy;
	}
	
	public String getLastDeployedId() {
		return lastDeployedId;
	}

	public void setLastDeployedId(String lastDeployedId) {
		this.lastDeployedId = lastDeployedId;
	}
	
	
}
