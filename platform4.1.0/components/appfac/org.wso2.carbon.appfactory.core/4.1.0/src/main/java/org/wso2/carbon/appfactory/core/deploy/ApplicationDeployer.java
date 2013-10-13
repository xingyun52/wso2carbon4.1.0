/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.appfactory.core.deploy;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.ServiceClient;
//import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
//import org.wso2.carbon.appfactory.core.ArtifactStorage;
import org.wso2.carbon.appfactory.core.Storage;
import org.wso2.carbon.appfactory.core.governance.RxtManager;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;
import org.wso2.carbon.appfactory.core.util.AppFactoryCoreUtil;
import org.wso2.carbon.application.mgt.stub.upload.types.carbon.UploadedFileItem;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappUploadData;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.wso2.carbon.appfactory.core.util.CommonUtil.getAdminUsername;
import static org.wso2.carbon.appfactory.core.util.CommonUtil.getServerAdminPassword;

/**
 * This service will deploy an artifact (specified as a combination of
 * application, stage, version and revision) to a set of servers associated with
 * specified stage ( e.g. QA, PROD)
 */
public class ApplicationDeployer {

    private static final Log log = LogFactory.getLog(ApplicationDeployer.class);
    private static final String EVENT = "deployment";

    public ArtifactDeploymentStatusBean[] deployArtifactByArtifactId(String applicationId, String autoTriggered,
                                                                     String version, String artifactId)
            throws Exception {

        File file = null;
        if (ServiceHolder.getContinuousIntegrationSystemDriver() == null) {
            //TODO
        } else {
            file = ServiceHolder.getContinuousIntegrationSystemDriver().getArtifact(applicationId, version, artifactId);
        }

        String stage = AppFactoryCoreUtil.getStage(applicationId, version);
        String revision = "";
        String key = AppFactoryConstants.DEPLOYMENT_STAGES + "." + stage + "." + AppFactoryConstants.DEPLOYMENT_URL;
        String[] deploymentServerUrls = ServiceHolder
                .getAppFactoryConfiguration().getProperties(key);

        if (deploymentServerUrls.length == 0) {
            handleException("No deployment paths are configured for stage:" + stage);
        }
        return deployToServers(deploymentServerUrls, applicationId, file, stage, version, revision);

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
                                                         String tagName)
            throws AppFactoryException {

//        ArtifactStorage storage = ServiceHolder.getArtifactStorage();
        //     File file = storage.retrieveArtifact(applicationId, version, revision, buildId);


        String key = AppFactoryConstants.DEPLOYMENT_STAGES + "." + stage + "." + AppFactoryConstants.DEPLOYMENT_URL;
        String[] deploymentServerUrls = ServiceHolder.getAppFactoryConfiguration().getProperties(key);

        if (deploymentServerUrls.length == 0) {
            handleException("No deployment paths are configured for stage:" + stage);
        }

        //return deployToServers(deploymentServerUrls, applicationId, file, stage, version, revision);
        Storage jenkinsStorage = ServiceHolder.getStorage();

        // job name : <applicationId>-<version>-default
        String jobName = applicationId + '-' + version + '-' + "default";
        String applicationType = null;
        try {
            applicationType = getApplicationType(applicationId);
        } catch (RegistryException e) {
            String errorMsg = "Unable to find the application type for application id : " + applicationId;
            handleException(errorMsg,e);
        }

        // todo when we add the latest success under tags, can differentiate by tagName != LATEST_SUCCESS
        if (tagName == null || tagName.equals("")) {
            jenkinsStorage.deployLatestSuccessArtifact(jobName, applicationType, stage);
        } else {
            jenkinsStorage.deployTaggedArtifact(jobName, applicationType, tagName, stage);
        }
        return null;
    }
    
    /**
     * Service method to tag latest successful build as given {@code newTagName} of given {@code applicationId}.  
     * @param applicationKey The key of the application to be tagged
     * @param stage The stage of the tag to be created.
     * @param version  Version of the application
     * @param newTagName The name that the last success build to be tagged.
     * @throws AppFactoryException
     */
	public ArtifactDeploymentStatusBean createNewTagByLastSuccessBuild(String applicationId, String stage,
	                                              String version, String newTagName) throws AppFactoryException {
		
		log.debug("Service invoked : createNewTagByLastSuccessBuild with applicationKey - " + applicationId + " version -" + version + " newTagName" + newTagName);
		
		String key = new StringBuilder(AppFactoryConstants.DEPLOYMENT_STAGES).append(".")
                        .append(stage)
                        .append(".")
                        .append(AppFactoryConstants.DEPLOYMENT_URL)
                        .toString();
		
        Storage jenkinsStorage = ServiceHolder.getStorage();
        String jobName = new StringBuilder(applicationId).append('-').append(version).append('-').append("default").toString();
        
        String applicationType = "";
		try {
			applicationType = getApplicationType(applicationId);
		} catch (RegistryException e) {
			String errorMsg = "Unable to find the application type for applicaiton id : " + applicationId;
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}
        
		jenkinsStorage.createNewTagByLastSuccessBuild(jobName, applicationType, newTagName, version,stage);
		
		return new ArtifactDeploymentStatusBean(applicationId, stage, version, null, null, true, null);

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

    private ArtifactDeploymentStatusBean[] deployToServers(
            String[] deploymentServerUrls, String applicationId, File file, String stage, String version,
            String revision) throws AppFactoryException {
        String artifactDetailsKey = "artifactDetails";
        String details = getArtifactDetails(file);
//        String[] artifactDetails = {details};

        RxtManager rxtManager = new RxtManager();
        rxtManager.updateAppVersionRxt(applicationId, stage, version, "appversion_" + artifactDetailsKey, details);

        DataHandler dataHandler = new DataHandler(new FileDataSource(file));
        ArtifactDeploymentStatusBean[] artifactDeploymentStatuses =
                new ArtifactDeploymentStatusBean[deploymentServerUrls.length];

        String applicationType;

        String event = "deployment of " + applicationId + " version " + version + " to " + stage;

        // todo resolve cyclic dependency to utilities and use it
        try {
            applicationType = getApplicationType(applicationId);
        } catch (RegistryException e) {
            String errorMsg = String.format("Unable to find the application type for application id: %s",
                    applicationId);
            log.error(errorMsg, e);
            AppFactoryCoreUtil.sendEventNotification(applicationId, event, "failed");
            throw new AppFactoryException(errorMsg, e);
        }

        int noOfFailedDeployment = 0;

        for (int i = 0; i < deploymentServerUrls.length; i++) {

            try {
                String deploymentServerIp = getDeploymentHostFromUrl(deploymentServerUrls[i]);

                ArtifactUploadClient artifactUploadClient = new ArtifactUploadClient(
                        deploymentServerUrls[i]);

                if (AppFactoryConstants.FILE_TYPE_CAR.equals(applicationType)) {
                    UploadedFileItem uploadedFileItem = new UploadedFileItem();
                    uploadedFileItem.setDataHandler(dataHandler);
                    uploadedFileItem.setFileName(file.getName());
                    uploadedFileItem.setFileType("jar");
                    UploadedFileItem[] uploadedFileItems = {uploadedFileItem};

                    if (artifactUploadClient.authenticate(
                            getAdminUsername(applicationId),
                            getServerAdminPassword(), deploymentServerIp)) {

                        artifactUploadClient.uploadCarbonApp(uploadedFileItems);
                        log.debug(file.getName() + " is successfully uploaded.");
                        artifactDeploymentStatuses[i] = new ArtifactDeploymentStatusBean(
                                applicationId, stage, version, revision,
                                deploymentServerUrls[i], true, null);
                    } else {
                        handleException("Failed to login to " + deploymentServerIp + " to deploy the artifact:" +
                                file.getName());
                    }

                } else if (AppFactoryConstants.FILE_TYPE_WAR
                        .equals(applicationType)) {
                    WebappUploadData webappUploadData = new WebappUploadData();
                    webappUploadData.setDataHandler(dataHandler);
                    webappUploadData.setFileName(file.getName());
                    WebappUploadData[] webAppUploadDataItems = {webappUploadData};
                    if (artifactUploadClient.authenticate(
                            getAdminUsername(applicationId),
                            getServerAdminPassword(), deploymentServerIp)) {

                        artifactUploadClient
                                .uploadWebApp(webAppUploadDataItems);
                        log.debug(file.getName() + " is successfully uploaded.");
                        artifactDeploymentStatuses[i] = new ArtifactDeploymentStatusBean(
                                applicationId, stage, version, revision,
                                deploymentServerUrls[i], true, null);
                    } else {
                        handleException("Failed to login to " + deploymentServerIp + " to deploy the artifact:" +
                                file.getName());
                    }
                }
            } catch (Exception e) {
                ++noOfFailedDeployment;
                artifactDeploymentStatuses[i] = new ArtifactDeploymentStatusBean(
                        applicationId, stage, version, revision,
                        deploymentServerUrls[i], false, e.getMessage());

                log.error("Failed to upload the artifact:" + file.getName() + " of application:" + applicationId +
                        " to deployment location:" + deploymentServerUrls[i], e);
            }

        }
       /* sendDeploymentNotification(
                applicationId,
                String.valueOf(isDeploymentSuccessful(artifactDeploymentStatuses)));*/

        String result = "failed";
        if (deploymentServerUrls.length > noOfFailedDeployment) {
            result = "Successfully deployed to " + (deploymentServerUrls.length - noOfFailedDeployment)
                    + " servers. Deployment for " + noOfFailedDeployment + " servers  failed";
        }
        AppFactoryCoreUtil.sendEventNotification(applicationId, event, result);

        return artifactDeploymentStatuses;
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

    private Boolean isDeploymentSuccessful(ArtifactDeploymentStatusBean[] deploymentStatusBeans) {
        for (ArtifactDeploymentStatusBean deploymentStatus : deploymentStatusBeans) {
            if (deploymentStatus.isSuccessful()) {
                return true;
            }
        }
        return false;
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

    private void sendDeploymentNotification(final String applicationId, final String result) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
                try {
                    AppFactoryConfiguration configuration = ServiceHolder.getAppFactoryConfiguration();
                    final String NOTIFICATION_EPR = configuration.getFirstProperty(
                            AppFactoryConstants.APPFACTORY_SERVER_URL) + "EventNotificationService";

                    ServiceClient client = new ServiceClient();
                    client.getOptions().setTo(new EndpointReference(NOTIFICATION_EPR));
                    CarbonUtils.setBasicAccessSecurityHeaders(getAdminUsername(), getServerAdminPassword(), false,
                            client);

                    //Make the request and get the response
                    client.sendRobust(getNotificationPayload(applicationId, EVENT, result));
                } catch (AxisFault e) {
                    log.error(e);
//                    e.printStackTrace();
                } catch (XMLStreamException e) {
                    log.error(e);
                }
            }
        }).start();
    }

    private static OMElement getNotificationPayload(String applicationId, String event,
                                                    String result)
            throws XMLStreamException {

        String payload =
                "<ser:publishEvent xmlns:ser=\"http://service.notification.events.appfactory.carbon.wso2.org\">" +
                "<ser:event xmlns:ser=\"http://service.notification.events.appfactory.carbon.wso2.org\">" +
                "<xsd:applicationId xmlns:xsd=\"http://service.notification.events.appfactory.carbon.wso2.org/xsd\">" +
                        applicationId + "</xsd:applicationId>" +
                "<xsd:event xmlns:xsd=\"http://service.notification.events.appfactory.carbon.wso2.org/xsd\">" + event +
                        "</xsd:event>" +
                "<xsd:result xmlns:xsd=\"http://service.notification.events.appfactory.carbon.wso2.org/xsd\">" +
                        result + "</xsd:result>" +
                "</ser:event></ser:publishEvent>";
        return new StAXOMBuilder(new ByteArrayInputStream(payload.getBytes())).getDocumentElement();
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


        if ("war".equals(applicationType)) {
            // undeploy the webapp(war file)
            deleteWebApp(applicationId, deploymentServerUrls);
        } else if ("car".equals(applicationType)) {
            // un-deploy the cApp (car file)
            deleteCApp(applicationId, deploymentServerUrls);
        } else {
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
    private void deleteWebApp(String applicationId, String[] deploymentServerUrls) throws AppFactoryException {
        for (String deploymentServerUrl : deploymentServerUrls) {
            try {
                String deploymentServerIp = getDeploymentHostFromUrl(deploymentServerUrl);
                ApplicationDeleteClient applicationDeleteClient = new ApplicationDeleteClient(deploymentServerUrl);

                if (applicationDeleteClient.authenticate(getAdminUsername(applicationId), getServerAdminPassword(),
                        deploymentServerIp)) {
                    applicationDeleteClient.deleteWebApp(applicationId);
                    log.debug(applicationId + " is successfully undeployed.");
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
