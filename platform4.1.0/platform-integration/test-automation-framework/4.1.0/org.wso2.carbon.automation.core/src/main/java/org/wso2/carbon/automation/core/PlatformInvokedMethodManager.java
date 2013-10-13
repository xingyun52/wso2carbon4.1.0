/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.automation.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.wso2.carbon.automation.core.utils.UnknownArtifactTypeException;

public class PlatformInvokedMethodManager implements IInvokedMethodListener {
    private static String beforeExecutionClassName = "";
    private static ArtifactManager artifactManager;
    private static final Log log = LogFactory.getLog(PlatformInvokedMethodManager.class);

    public void beforeInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {


        if (!beforeExecutionClassName.equals(iTestResult.getTestClass().getName())) {

            try {
                artifactManager = ArtifactManager.getInstance();
                assert artifactManager != null : "Artifact Manger is null";
                artifactManager.cleanArtifacts(beforeExecutionClassName);
            } catch (UnknownArtifactTypeException e) { /*cannot throw the exception */
                log.error("Unknown Artifact type to be cleared ", e);
            } catch (Exception e) {
                log.error("Artifact Cleaning Error ", e);
            }

            beforeExecutionClassName = iTestResult.getTestClass().getName();


            log.info("Before executing the test class :###########" + beforeExecutionClassName + "############");
            if (beforeExecutionClassName != null) {
                try {
                    artifactManager.deployArtifacts(beforeExecutionClassName);
                } catch (Exception e) {
                    log.error("Artifact Deployment Error ", e);
                }
            }
        }
    }

    public void afterInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {
    }

}
