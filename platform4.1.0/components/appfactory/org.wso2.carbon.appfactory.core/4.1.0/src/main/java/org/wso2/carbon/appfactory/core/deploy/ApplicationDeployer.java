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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.ArtifactStorage;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;
import org.wso2.carbon.application.mgt.stub.upload.types.carbon.UploadedFileItem;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappUploadData;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This service will deploy an artifact (specified as a combination of
 * application, stage, version and revision) to a set of servers associated with
 * specified stage ( e.g. QA, PROD)
 * 
 */
public class ApplicationDeployer {

    private static final Log log = LogFactory.getLog(ApplicationDeployer.class);
    private static final String EVENT = "deployment";

    /**
     * Deploys the Artifact to specified stage.
     * 
     * @param applicationId
     *            The application Id.
     * @param stage
     *            The stage to deploy ( e.g. QA, PROD)
     * @param version
     *            Version of the application
     * @param revision
     *            Revision of the application return
     * @return An array of {@link ArtifactDeploymentStatusBean} indicating the
     *         status of each deployment operation.
     * @throws AppFactoryException
     */
    public ArtifactDeploymentStatusBean[] deployArtifact(String applicationId,
                                                         String stage, String version,
                                                         String revision)
                                                         throws AppFactoryException {

        ArtifactStorage storage = ServiceHolder.getArtifactStorage();
        File file = storage.retrieveArtifact(applicationId, version, revision);
        DataHandler dataHandler = new DataHandler(new FileDataSource(file));

        String key =
                     new StringBuilder(AppFactoryConstants.DEPLOYMENT_STAGES).append(".")
                                                                             .append(stage)
                                                                             .append(".")
                                                                             .append(AppFactoryConstants.DEPLOYMENT_URL)
                                                                             .toString();
        String[] deploymentServerUrls =
                                        ServiceHolder.getAppFactoryConfiguration()
                                                     .getProperties(key);

        if (deploymentServerUrls.length == 0) {
            handleException("No deployment paths are configured for stage:" + stage);
        }

        ArtifactDeploymentStatusBean[] artifactDeploymentStatuses =
                                                                   new ArtifactDeploymentStatusBean[deploymentServerUrls.length];
        String applicationType;
        //   todo resolve cyclic dependency to utilities and use it
        try {
            RegistryService registryService = ServiceHolder.getInstance().getRegistryService();
            UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
            Resource resource = userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
                                                 File.separator + applicationId + File.separator + "appinfo");
            GovernanceUtils.loadGovernanceArtifacts(userRegistry);
            GenericArtifactManager artifactManager = new GenericArtifactManager(userRegistry, "application");
            GenericArtifact artifact = artifactManager.getGenericArtifact(resource.getUUID());
            applicationType = artifact.getAttribute("application_type");
        } catch (RegistryException e) {
            String errorMsg =
                    String.format("Unable to find the application type for applicaiton id: %s", applicationId);
            log.error(errorMsg, e);
            throw new AppFactoryException(errorMsg, e);
        }

        for (int i = 0; i < deploymentServerUrls.length; i++) {
            try {
                String deploymentServerIp =
                                            getDeploymentHostFromUrl(deploymentServerUrls[i]);

                ArtifactUploadClient artifactUploadClient =
                                                            new ArtifactUploadClient(
                                                                                     deploymentServerUrls[i]);

                if (AppFactoryConstants.FILE_TYPE_CAR.equals(applicationType)) {
                    UploadedFileItem uploadedFileItem = new UploadedFileItem();
                    uploadedFileItem.setDataHandler(dataHandler);
                    uploadedFileItem.setFileName(file.getName());
                    uploadedFileItem.setFileType("jar");
                    UploadedFileItem[] uploadedFileItems = {uploadedFileItem};

                    if (artifactUploadClient.authenticate(getAdminUsername(applicationId),
                                                          getServerAdminPassword(),
                                                          deploymentServerIp)) {

                        artifactUploadClient.uploadCarbonApp(uploadedFileItems);
                        log.debug(file.getName() + " is successfully uploaded.");
                        artifactDeploymentStatuses[i] =
                                new ArtifactDeploymentStatusBean(
                                        applicationId,
                                        stage,
                                        version,
                                        revision,
                                        deploymentServerUrls[i],
                                        "success",
                                        null);
                    } else {
                        handleException("Failed to login to " + deploymentServerIp +
                                        " to deploy the artifact:" + file.getName());
                    }

                } else if (AppFactoryConstants.FILE_TYPE_WAR.equals(applicationType)) {
                    WebappUploadData webappUploadData = new WebappUploadData();
                    webappUploadData.setDataHandler(dataHandler);
                    webappUploadData.setFileName(file.getName());
                    WebappUploadData[] webappUploadDataItems = {webappUploadData};
                    if (artifactUploadClient.authenticate(getAdminUsername(applicationId),
                                                          getServerAdminPassword(),
                                                          deploymentServerIp)) {

                        artifactUploadClient.uploadWebApp(webappUploadDataItems);
                        log.debug(file.getName() + " is successfully uploaded.");
                        artifactDeploymentStatuses[i] =
                                new ArtifactDeploymentStatusBean(
                                        applicationId,
                                        stage,
                                        version,
                                        revision,
                                        deploymentServerUrls[i],
                                        "success",
                                        null);
                    } else {
                        handleException("Failed to login to " + deploymentServerIp +
                                        " to deploy the artifact:" + file.getName());
                    }
                }
            } catch (Exception e) {

                artifactDeploymentStatuses[i] =
                                                new ArtifactDeploymentStatusBean(
                                                                                 applicationId,
                                                                                 stage,
                                                                                 version,
                                                                                 revision,
                                                                                 deploymentServerUrls[i],
                                                                                 "failed",
                                                                                 e.getMessage());

                handleException("Failed to upload the artifact:" + file.getName() +
                                " of application:" + applicationId +
                                " to deployment location:" + deploymentServerUrls[i]);
            }

        }
        sendDeploymentNotification(applicationId,String.valueOf(isDeploymentSuccessful(artifactDeploymentStatuses)));

        return artifactDeploymentStatuses;
    }
    
    private Boolean isDeploymentSuccessful(ArtifactDeploymentStatusBean[] deploymentStatusBeans) {
        for(ArtifactDeploymentStatusBean deploymentStatus : deploymentStatusBeans) {
            if(false == Boolean.valueOf(deploymentStatus.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private String getAdminUsername() {
        return ServiceHolder.getAppFactoryConfiguration()
                       .getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME);
    }

    private String getAdminUsername(String applicationId) {
        return ServiceHolder.getAppFactoryConfiguration()
                            .getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME) +
               "@" + applicationId;
    }

    private String getServerAdminPassword() {
        return ServiceHolder.getAppFactoryConfiguration()
                            .getFirstProperty(AppFactoryConstants.SERVER_ADMIN_PASSWORD);
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
                    final String NOTIFICATION_EPR = configuration.getFirstProperty(AppFactoryConstants.APPFACTORY_SERVER_URL) + "EventNotificationService";

                    ServiceClient client = new ServiceClient();
                    client.getOptions().setTo(new EndpointReference(NOTIFICATION_EPR));
                    CarbonUtils.setBasicAccessSecurityHeaders(getAdminUsername(), getServerAdminPassword(), false, client);

                    //Make the request and get the response
                    client.sendRobust(getNotificationPayload(applicationId, EVENT, result));
                } catch (AxisFault e) {
                    log.error(e);
                    e.printStackTrace();
                } catch (XMLStreamException e) {
                    log.error(e);
                }
            }
        }).start();
    }

    private static OMElement getNotificationPayload(String applicationId, String event,
                                                    String result)
            throws XMLStreamException, javax.xml.stream.XMLStreamException {

        String payload = "<ser:publishEvent xmlns:ser=\"http://service.notification.events.appfactory.carbon.wso2.org\">" +
                         "<ser:event xmlns:ser=\"http://service.notification.events.appfactory.carbon.wso2.org\">" +
                         "<xsd:applicationId xmlns:xsd=\"http://service.notification.events.appfactory.carbon.wso2.org/xsd\">" + applicationId + "</xsd:applicationId>" +
                         "<xsd:event xmlns:xsd=\"http://service.notification.events.appfactory.carbon.wso2.org/xsd\">" + event + "</xsd:event>" +
                         "<xsd:result xmlns:xsd=\"http://service.notification.events.appfactory.carbon.wso2.org/xsd\">" + result + "</xsd:result>" +
                         "</ser:event></ser:publishEvent>";
        return new StAXOMBuilder(new ByteArrayInputStream(payload.getBytes())).getDocumentElement();
    }

    /**
     * Deleting an application from given environment
     *
     * @param stage
     *              Stage to identify the environment
     * @param applicationId
     *              Application ID which needs to delete
     * @return
     *              boolean
     * @throws AppFactoryException
     *              An error
     */
    public boolean unDeployArtifact(String stage, String applicationId)
            throws AppFactoryException {

        log.info("Deleting application: " + applicationId + ", from: " + stage + " stage");

        String key =
                new StringBuilder(AppFactoryConstants.DEPLOYMENT_STAGES).append(".")
                        .append(stage)
                        .append(".")
                        .append(AppFactoryConstants.DEPLOYMENT_URL)
                        .toString();

        String[] deploymentServerUrls =
                ServiceHolder.getAppFactoryConfiguration()
                        .getProperties(key);

        if (deploymentServerUrls.length == 0) {
            handleException("No deployment paths are configured for stage:" + stage);
        }

        for (int i = 0; i < deploymentServerUrls.length; i++) {
            try {
                String deploymentServerIp =
                        getDeploymentHostFromUrl(deploymentServerUrls[i]);

                ApplicationDeleteClient applicationDeleteClient = new
                        ApplicationDeleteClient(deploymentServerUrls[i]);

                if (applicationDeleteClient.authenticate(getAdminUsername(applicationId),
                        getServerAdminPassword(),
                        deploymentServerIp)) {

                    applicationDeleteClient.deleteCarbonApp(applicationId);
                    log.debug(applicationId + " is successfully undeployed.");
                } else {
                    handleException("Failed to login to " + deploymentServerIp +
                            " to undeploy the artifact:" + applicationId);
                }

            } catch (Exception e) {

            }
        }

        return true;
    }


}
