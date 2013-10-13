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

package org.wso2.carbon.appfactory.core.build;

import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;
import org.wso2.carbon.appfactory.core.RevisionControlDriver;
import org.wso2.carbon.core.AbstractAdmin;

/**
 * Basic artifact creator uses
 * 
 */
public class ArtifactCreator extends AbstractAdmin {

    public void createArtifact(String applicationId, String version, String revision, boolean doDeploy, String deployStage, String tagName)
                                                                                     throws AppFactoryException {
        if (ServiceHolder.getContinuousIntegrationSystemDriver() != null) {
            // Since the CI system is enabled appfactory will give the preference to it. Appfactory will start
            // the job if it exists. Once the build is completed 'appfactory-post-build-notifier-plugin' will upload the
            // artifact onto artifact storage.
        	String jobName =
                             ServiceHolder.getContinuousIntegrationSystemDriver()
                                          .getJobName(applicationId, version, revision);
            if (ServiceHolder.getContinuousIntegrationSystemDriver().isJobExists(jobName)){
                ServiceHolder.getContinuousIntegrationSystemDriver().startBuild(jobName, doDeploy, deployStage, tagName);
            }
        } else {
            // Default builder will build the Application locally.
            DefaultRevisionControlDriverListener listener =
                                                            new DefaultRevisionControlDriverListener();
            RevisionControlDriver revisionControlDriver = ServiceHolder.getRevisionControlDriver();
            revisionControlDriver.getSource(applicationId, version, revision, listener);
        }
    }
}
