/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.application.deployer.mashup;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.application.deployer.AppDeployerUtils;
import org.wso2.carbon.application.deployer.CarbonApplication;
import org.wso2.carbon.application.deployer.config.Artifact;
import org.wso2.carbon.application.deployer.config.CappFile;
import org.wso2.carbon.application.deployer.handler.AppDeploymentHandler;
import org.wso2.carbon.application.deployer.mashup.internal.MashupAppDeployerDSComponent;
import org.wso2.carbon.utils.ArchiveManipulator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MashupAppDeployer implements AppDeploymentHandler {

    private static final Log log = LogFactory.getLog(MashupAppDeployer.class);

    public static final String MASHUP_TYPE = "wso2/mashup";
    public static final String MASHUP_CONTEXT = "carbon";
    public static final String MASHUP_DIR = "jsservices";

    private Map<String, Boolean> acceptanceList = null;

    /**
     * Check the artifact type and if it is a Mashup, copy it to the Mashup deployment hot folder
     *
     * @param carbonApp  - CarbonApplication instance to check for Mashup artifacts
     * @param axisConfig - AxisConfiguration of the current tenant
     */
    public void deployArtifacts(CarbonApplication carbonApp, AxisConfiguration axisConfig) {
        List<Artifact.Dependency> artifacts =
                carbonApp.getAppConfig().getApplicationArtifact().getDependencies();

        ArchiveManipulator archiveManipulator = new ArchiveManipulator();

        String repo = axisConfig.getRepository().getPath();

        String artifactPath, destPath;
        for (Artifact.Dependency dep : artifacts) {
            Artifact artifact = dep.getArtifact();
            if (artifact == null) {
                continue;
            }

            String artifactName = artifact.getName();
            if (!isAccepted(artifact.getType())) {
                log.warn("Can't deploy artifact : " + artifactName + " of type : " +
                        artifact.getType() + ". Required features are not installed in the system");
                continue;
            }

            if (MASHUP_TYPE.equals(artifact.getType())) {
                destPath = repo + File.separator + MASHUP_DIR + File.separator + MASHUP_CONTEXT;
            } else {
                continue;
            }

            List<CappFile> files = artifact.getFiles();
            if (files.size() != 1) {
                log.error("A Mashup must have a single file. But " +
                        files.size() + " files found.");
                continue;
            }
            String fileName = artifact.getFiles().get(0).getName();
            artifactPath = artifact.getExtractedPath() + File.separator + fileName;
            try {
                archiveManipulator.extract(artifactPath, destPath);
            } catch (IOException e) {
                log.error("Unable to copy the Mashup : " + artifactName, e);
            }

        }
    }

    /**
     * Check the artifact type and if it is a Gadget, delete the file from the Gadget deployment hot
     * folder
     *
     * @param carbonApp  - CarbonApplication instance to check for Gadget artifacts
     * @param axisConfig - AxisConfiguration of the current tenant
     */
    public void undeployArtifacts(CarbonApplication carbonApp, AxisConfiguration axisConfig) {

        List<Artifact.Dependency> artifacts =
                carbonApp.getAppConfig().getApplicationArtifact().getDependencies();
        ArchiveManipulator archiveManipulator = new ArchiveManipulator();

        String repo = axisConfig.getRepository().getPath();
        String artifactPath, destPath;
        for (Artifact.Dependency dep : artifacts) {
            Artifact artifact = dep.getArtifact();
            if (artifact == null) {
                continue;
            }
            if (MashupAppDeployer.MASHUP_TYPE.equals(artifact.getType())) {
                destPath = repo + File.separator + MashupAppDeployer.MASHUP_DIR + File.separator +
                        MashupAppDeployer.MASHUP_CONTEXT;
            } else {
                continue;
            }

            List<CappFile> files = artifact.getFiles();
            if (files.size() != 1) {
                log.error(
                        "A Mashup must have a single file. But " + files.size() + " files found.");
                continue;
            }
            String fileName = artifact.getFiles().get(0).getName();
            artifactPath = artifact.getExtractedPath() + File.separator + fileName;
            File artifactInRepo;
            if (new File(artifactPath).exists()) {
                try {
                    String[] filesInZip = archiveManipulator.check(artifactPath);
                    File jsFile = null;
                    for (String file : filesInZip) {
                        String artifactRepoPath = destPath + File.separator + file;
                        if (file.indexOf("/") == -1) {
                            String extension = file.substring(file.indexOf(".") + 1);
                            if ("js".equals(extension)) {
                                jsFile = new File(destPath + File.separator + file);
                            } else {
                                artifactInRepo = new File(artifactRepoPath);
                                if (artifactInRepo.exists() && artifactInRepo.delete()) {
                                    log.warn("Couldn't delete Mashup artifact file : " + artifactPath);
                                }
                            }
                        }
                    }
                    if (jsFile != null && jsFile.exists() && !jsFile.delete()) {
                        log.warn("Couldn't delete Mashup artifact file : " + artifactPath);
                    }
                } catch (IOException e) {
                    log.error("Error reading the content of the artifact : " + artifact.getName(), e);
                }
            }
        }
    }

    /**
     * Check whether a particular artifact type can be accepted for deployment. If the type doesn't
     * exist in the acceptance list, we assume that it doesn't require any special features to be
     * installed in the system. Therefore, that type is accepted. If the type exists in the
     * acceptance list, the acceptance value is returned.
     *
     * @param serviceType - service type to be checked
     * @return true if all features are there or entry is null. else false
     */
    private boolean isAccepted(String serviceType) {
        if (acceptanceList == null) {
            acceptanceList = AppDeployerUtils
                    .buildAcceptanceList(MashupAppDeployerDSComponent.getRequiredFeatures());
        }
        Boolean acceptance = acceptanceList.get(serviceType);
        return (acceptance == null || acceptance);
    }

}
