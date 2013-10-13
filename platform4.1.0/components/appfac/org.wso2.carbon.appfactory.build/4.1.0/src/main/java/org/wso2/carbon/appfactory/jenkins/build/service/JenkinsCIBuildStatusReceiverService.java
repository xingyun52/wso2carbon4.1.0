/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
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

package org.wso2.carbon.appfactory.jenkins.build.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.core.build.DefaultBuildDriverListener;
import javax.activation.DataHandler;

/**
 * This service class is used to receive the build status
 */
public class JenkinsCIBuildStatusReceiverService {

    private static Log log = LogFactory.getLog(JenkinsCIBuildStatusReceiverService.class);

    /**
     * This is to be called after a completion of a build
     * @param buildStatus bean which contains the information of the completed build
     */
    @SuppressWarnings("UnusedDeclaration")
    public void onBuildCompletion(BuildStatusBean buildStatus, DataHandler data, String fileName) {
        /// TODO remove DataHandler and fileName params from here and change stub and jenkins side
        // which call this
        log.info("Build completed for "+buildStatus.getApplicationId() +
                 " with buildId "+buildStatus.getBuildId());
        DefaultBuildDriverListener listener = new DefaultBuildDriverListener();
        if(buildStatus.isBuildSuccessful()) {
            listener.onBuildSuccessful(buildStatus.getApplicationId(), buildStatus.getVersion(),
                                       null, buildStatus.getBuildId(), data, fileName);
        } else {
            listener.onBuildFailure(buildStatus.getApplicationId(), buildStatus.getVersion(),
                                    null, buildStatus.getBuildId(),  buildStatus.getLogMsg());
        }
    }
}
