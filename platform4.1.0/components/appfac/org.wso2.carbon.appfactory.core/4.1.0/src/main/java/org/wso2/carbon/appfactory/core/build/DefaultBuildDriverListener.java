/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.wso2.carbon.appfactory.core.build;

import javax.activation.DataHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.BuildDriverListener;
import org.wso2.carbon.appfactory.core.governance.RxtManager;
import org.wso2.carbon.appfactory.core.util.AppFactoryCoreUtil;


public class DefaultBuildDriverListener implements BuildDriverListener {

    private static final Log log = LogFactory.getLog(DefaultBuildDriverListener.class);
    private static final String EVENT = "build";
    private static final String SUCCESS = "successful";
    private static final String FAILED = "failed";
    private static String LAST_BUILD_STATUS_KEY = "LastBuildStatus";


    @Override
    public void onBuildSuccessful(String applicationId, String version, String revision, String buildId,
                                  DataHandler dataHandler, String fileName) {
        log.info(applicationId + "-" + version + " build successfully");
        String event = "version "+version + " build with buildId " +buildId;
        updateLastBuildStatus(applicationId, version, SUCCESS);
        AppFactoryCoreUtil.sendEventNotification(applicationId, event, SUCCESS);
    }

    @Override
    public void onBuildFailure(String applicationId, String version, String revision, String buildId, String errorMessage) {
        log.info(applicationId + "-" + version + " failed to build");
        log.info(errorMessage);
        updateLastBuildStatus(applicationId, version, FAILED);
        String event = "version "+version + " build with buildId "+buildId;
        AppFactoryCoreUtil.sendEventNotification(applicationId, event, FAILED);
    }

    /**
     *
     * @param applicationId
     * @param version
     * @param result
     * @throws AppFactoryException
     */
    public static void updateLastBuildStatus(String applicationId, String version, String result) {
        try {
            RxtManager rxtManager = new RxtManager();
            String stage = rxtManager.getStage(applicationId, version);
            String key = "appversion_"+LAST_BUILD_STATUS_KEY;
            rxtManager.updateAppVersionRxt(applicationId, stage, version, key, result);
        } catch (AppFactoryException e) {
            log.error("Error updating the appversion rxt with build status : " + e.getMessage(), e);
        }
    }

  

}
