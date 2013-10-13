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

import static org.wso2.carbon.appfactory.core.util.CommonUtil.getAdminUsername;
import static org.wso2.carbon.appfactory.core.util.CommonUtil.getServerAdminPassword;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.Storage;
import org.wso2.carbon.appfactory.core.governance.RxtManager;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;
import org.wso2.carbon.appfactory.core.util.AppFactoryCoreUtil;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

/**
 * This service will deploy an artifact (specified as a combination of
 * application, stage, version and revision) to a set of servers associated with
 * specified stage ( e.g. QA, PROD)
 */
public class ApplicationDeployer {

    private static final Log log = LogFactory.getLog(ApplicationDeployer.class);
    
    /**
	 * Service method to get the latest deployed build information.
	 * 
	 * 
	 * @param buildInfo
	 * @throws AppFactoryException
	 */
    public String getDeployedArtifactInformation(String applicationId, String version, String stage) throws AppFactoryException{
		String buildNumber = "-1" ;
		
		RxtManager rxtManager  = new RxtManager();
		try {
			buildNumber = rxtManager.getAppVersionRxtValue(applicationId, stage, version,"appversion_lastdeployedid");
		} catch (AppFactoryException e) {
			throw new AppFactoryException(e.getMessage());
		}
		
		return buildNumber ;
	}
    
    
    
    /**
	 * Service method to get the artifact information for the given applicationId.
	 * 
	 * 
	 * @param applicationId
	 * @throws AppFactoryException
	 */
    public List<Artifact> getArtifactInformation(String applicationId) throws AppFactoryException{
		RxtManager rxtManager  = new RxtManager();
		try {
			List<Artifact> artifacts = rxtManager.getAppVersionRxtForApplication(applicationId);			
			return artifacts;
			
		} catch (AppFactoryException e) {
			log.error("Error while retrieving artifat information from rxt");
			throw new AppFactoryException(e.getMessage());
		} catch (RegistryException e) {
			log.error("Error while retrieving artifat information from rxt");
			throw new AppFactoryException(e.getMessage());
        }
	}
    
    /**
	 * Service method to update the latest deployed build information.
	 * This service will be called from Jenkins when the deployment is done.
	 * 
	 * @param buildInfo
	 * @throws AppFactoryException
	 */
	public void updateDeploymentInformation(String applicationId,String stage ,String version,String buildId) throws AppFactoryException {

		log.info("Deployment information updation service called.");
		RxtManager rxtManager = new RxtManager();
		rxtManager.updateAppVersionRxt(applicationId, stage, version, "appversion_lastdeployedid", buildId);
		
		log.info("Deployment information successfuly updated ");
	}

    /**
     * Deploys the Artifact to specified stage.
     *
     * @param applicationId The application Id.
     * @param stage         The stage to deploy ( e.g. QA, PROD)
     * @param version       Version of the application
     * @return An array of {@link ArtifactDeploymentStatusBean} indicating the
     *         status of each deployment operation.
     * @throws AppFactoryException
     */
    public ArtifactDeploymentStatusBean[] deployArtifact(String applicationId,
                                                         String stage, String version,
                                                         String tagName, String deployAction)
            throws AppFactoryException {

        String key = AppFactoryConstants.DEPLOYMENT_STAGES + "." + stage + "." + AppFactoryConstants.DEPLOYMENT_URL;
        String[] deploymentServerUrls = ServiceHolder.getAppFactoryConfiguration().getProperties(key);

        if (deploymentServerUrls.length == 0) {
            handleException("No deployment paths are configured for stage:" + stage);
        }
        Storage storage = ServiceHolder.getStorage();

        // job name : <applicationId>-<version>-default
        String jobName = applicationId + '-' + version + '-' + "default";
        String applicationType = null;
        try {
            applicationType = getApplicationType(applicationId);
        } catch (RegistryException e) {
            String errorMsg = "Unable to find the application type for application id : " + applicationId;
            handleException(errorMsg,e);
        }

        if("deploy".equals(deployAction)) {
            if (tagName == null || tagName.equals("")) {
                storage.deployLatestSuccessArtifact(jobName, applicationType, stage);
            } else {
                storage.deployTaggedArtifact(jobName, applicationType, tagName, stage, deployAction);
            }
        } else if ("promote".equals(deployAction)) {
            storage.deployTaggedArtifact(jobName, applicationType, tagName, stage, deployAction);
        } else if("rePromote".equals(deployAction)) {
            ServiceHolder.getStorage().deployPromotedArtifact(jobName, applicationType, stage);
        }
        return null;
    }

    public String getArtifactDetails(File file) throws AppFactoryException{
        String artifactDetails = null;
        String fileName;

        if (file == null) {
            return "Not Found";
        }

        fileName = file.getName();
        if (fileName.endsWith(".war")) {
            String artifactVersion = fileName.substring(fileName.indexOf('-') + 1,
                    fileName.indexOf(".war"));

            String artifactName = fileName.substring(0, (fileName.indexOf('-')));
            artifactDetails = artifactName + '-' + artifactVersion;
            return artifactDetails;

        } else if (fileName.endsWith(".car")) {
            fileName = file.getAbsolutePath();
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(fileName);
            } catch (FileNotFoundException e) {
                String msg = "Unable to find file : " + fileName;
                log.error(msg,e);
                throw new AppFactoryException(msg,e);
            }

            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            ZipEntry zipEntry;

            try {
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    String entryName = zipEntry.getName();
                    if (!(entryName.equals("artifacts.xml"))) {
                        //  byte[] buf = new byte[1024];
                        log.info("Name of  Zip Entry : " + entryName);
                        String artifactVersion = entryName.substring(entryName.indexOf('_') + 1);
                        String artifactName = entryName.substring(0, (entryName.indexOf('_')));
                        zipInputStream.close();
                        fileInputStream.close();

                        artifactDetails = artifactName + '-' + artifactVersion;
                        return artifactDetails;
                    }

                }
            } catch (IOException e) {
                String msg = "Unable to complete operation";
                log.error(msg,e);
                throw new AppFactoryException(msg,e);
            }
        }
        return artifactDetails;

    }

    public String getStage(String applicationId, String version) throws AppFactoryException {
        return new RxtManager().getStage(applicationId, version);
    }

    private String getApplicationType(String applicationId) throws RegistryException {
        try {
            String applicationType;
            RegistryService registryService = ServiceHolder.getRegistryService();
            UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
            Resource resource = userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
                    RegistryConstants.PATH_SEPARATOR + applicationId + RegistryConstants.PATH_SEPARATOR
                            + "appinfo");
            GovernanceUtils.loadGovernanceArtifacts(userRegistry);
            GenericArtifactManager artifactManager = new GenericArtifactManager(
                    userRegistry, "application");
            GenericArtifact artifact = artifactManager
                    .getGenericArtifact(resource.getUUID());
            applicationType = artifact.getAttribute("application_type");
            return applicationType;
        } catch (RegistryException e) {
            log.error(e);
            throw e;
        }
    }

//    Is called from the jaggery app
    public String[] getTagNamesOfPersistedArtifacts(String applicationId, String version) throws AppFactoryException {
        // job name : <applicationId>-<version>-default
        String jobName = applicationId + '-' + version + '-' + "default";
        return ServiceHolder.getStorage().getTagNamesOfPersistedArtifacts(jobName);
    }

    private void handleException(String msg) throws AppFactoryException {
        log.error(msg);
        throw new AppFactoryException(msg);
    }

    private void handleException(String msg, Throwable throwable)
            throws AppFactoryException {
        log.error(msg, throwable);
        throw new AppFactoryException(msg, throwable);
    }

    private String getDeploymentHostFromUrl(String url) throws AppFactoryException {
        String hostName = null;
        try {
            URL deploymentURL = new URL(url);
            hostName = deploymentURL.getHost();
        } catch (MalformedURLException e) {
            handleException("Deployment url is malformed.", e);
        }

        return hostName;
    }

    /**
     * Deleting an application from given environment
     *
     * @param stage         Stage to identify the environment
     * @param applicationId Application ID which needs to delete
     * @return boolean
     * @throws AppFactoryException An error
     */
    public boolean unDeployArtifact(String stage, String applicationId)
            throws AppFactoryException {

        log.info("Deleting application " + applicationId + ", from " + stage + " stage");
        String event = "Deleting application: " + applicationId + ", from: " + stage + " stage";

        String key = AppFactoryConstants.DEPLOYMENT_STAGES + "." + stage + "." + AppFactoryConstants.DEPLOYMENT_URL;

        String[] deploymentServerUrls = ServiceHolder.getAppFactoryConfiguration().getProperties(key);

        if (deploymentServerUrls.length == 0) {
            handleException("No deployment paths are configured for stage:" + stage);
        }

        String applicationType;
        try {
            applicationType = getApplicationType(applicationId);
        } catch (RegistryException e) {
            String errorMsg = String.format("Unable to find the application type for application id: %s",
                    applicationId);
            log.error(errorMsg, e);
            AppFactoryCoreUtil.sendEventNotification(applicationId, event, "failed");
            throw new AppFactoryException(errorMsg, e);
        }


        if ("war".equals(applicationType)||"jaxws".equals(applicationType)||"jaxrs".equals(applicationType) ) {
            // undeploy the webapp(war/jaxws/jaxrs file)
            deleteWebApp(applicationId, deploymentServerUrls,".war");
        } else if ("car".equals(applicationType)) {
            // un-deploy the cApp (car file)
            deleteCApp(applicationId, deploymentServerUrls);
        }else if("jaggery".equals(applicationType)){
            deleteWebApp(applicationId, deploymentServerUrls,"");
        }else {
            handleException("Can not detect application type to delete the application");
        }

        return true;
    }


    /**
     * Delete CApp from given deployment servers
     *
     * @param applicationId        application ID
     * @param deploymentServerUrls deployment servers
     * @throws AppFactoryException an error
     */
    private void deleteCApp(String applicationId, String[] deploymentServerUrls) throws AppFactoryException {
        for (String deploymentServerUrl : deploymentServerUrls) {
            try {
                String deploymentServerIp = getDeploymentHostFromUrl(deploymentServerUrl);
                ApplicationDeleteClient applicationDeleteClient = new ApplicationDeleteClient(deploymentServerUrl);

                if (applicationDeleteClient.authenticate(getAdminUsername(applicationId), getServerAdminPassword(),
                        deploymentServerIp)) {
                    applicationDeleteClient.deleteCarbonApp(applicationId);
                    log.debug(applicationId + " is successfully undeployed.");
                } else {
                    handleException("Failed to login to " + deploymentServerIp + " to undeploy the artifact:" +
                            applicationId);
                }

            } catch (Exception e) {
                handleException("Error occurred when un-deploying car file for application ID : " + applicationId, e);

            }
        }
    }

    /**
     * Delete web application from given deployment servers
     *
     * @param applicationId        application ID
     * @param deploymentServerUrls deployment servers
     * @throws AppFactoryException an error
     */
    private void deleteWebApp(String applicationId, String[] deploymentServerUrls,String type) throws AppFactoryException {
        for (String deploymentServerUrl : deploymentServerUrls) {
            try {
                String deploymentServerIp = getDeploymentHostFromUrl(deploymentServerUrl);
                ApplicationDeleteClient applicationDeleteClient = new ApplicationDeleteClient(deploymentServerUrl);

                if (applicationDeleteClient.authenticate(getAdminUsername(applicationId), getServerAdminPassword(),
                        deploymentServerIp)) {
                    applicationDeleteClient.deleteWebApp(applicationId,type);
                    log.info(applicationId + " is successfully undeployed.");
                } else {
                    handleException("Failed to login to " + deploymentServerIp +
                            " to undeploy the artifact:" + applicationId);
                }
            } catch (Exception e) {
                handleException("Error occurred when un-deploying war file for application ID : " + applicationId, e);

            }
        }
    }

}
