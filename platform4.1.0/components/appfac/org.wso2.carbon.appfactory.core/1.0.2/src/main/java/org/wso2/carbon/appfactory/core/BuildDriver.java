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

package org.wso2.carbon.appfactory.core;

import org.wso2.carbon.appfactory.common.AppFactoryException;

/**
 * This will drive the build.
 * Maven2, Maven3 and Ant are some possible implementations of this
 */
public interface BuildDriver {

	/**
	 * Trigger the build.
	 * 
	 * @param applicationId
	 * @param version
	 * @param revision
	 */
	public void buildArtifact(String applicationId, String version, String revision,
	                          BuildDriverListener listener) throws AppFactoryException;

}
