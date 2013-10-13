/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.appfactory.core.governance.lifecycle;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.ContinuousIntegrationSystemDriver;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;
import org.wso2.carbon.governance.registry.extensions.executors.ServiceVersionExecutor;
import org.wso2.carbon.governance.registry.extensions.executors.utils.ExecutorConstants;
import org.wso2.carbon.governance.registry.extensions.interfaces.Execution;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourcePath;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;
import org.wso2.carbon.registry.core.utils.RegistryUtils;

import javax.xml.stream.XMLStreamException;
import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.governance.registry.extensions.executors.utils.Utils.populateParameterMap;


/**
 * Lifecycle executor to handle application lifecycles.
 * This executor will invoke when lifecycle state change from one state to another
 */
public class AppFactoryLCExecutor implements Execution {

    private static final Log log = LogFactory.getLog(ServiceVersionExecutor.class);
    private static final String KEY = ExecutorConstants.RESOURCE_VERSION;
    private Map parameterMap;

    public void init(Map map) {

        // This parameterMap stores parameters which is set on lifecycle config
        // for the moment we don't store any parameters in lifecycle config
        parameterMap = map;

    }

    public boolean execute(RequestContext requestContext, String currentState, String targetState) {
        // TODO this method content can be simplified
        // Absolute path for the current application
        // (i.e. /_system/governance/repository/applications/$Application/$Stage/$Version/appinfo )
        String resourcePath = requestContext.getResource().getPath();

        // Variable to store new path of the application
        String newPath;

        // Now we are going to get the list of parameters from the context and add it to a map
        Map<String, String> currentParameterMap = new HashMap<String, String>();

        // Here we are populating the parameter map that was given from the UI
        if (!populateParameterMap(requestContext, currentParameterMap)) {
            log.error("Failed to populate the parameter map");
            return false;
        }

        // Getting values from map
        final String applicationId = currentParameterMap.get(AppFactoryConstants.APPLICATION_ID);

        final String revision = currentParameterMap.get(AppFactoryConstants.APPLICATION_REVISION);

        final String version = currentParameterMap.get(AppFactoryConstants.APPLICATION_VERSION);

        //final String stage = currentParameterMap.get(AppFactoryConstants.APPLICATION_STAGE);

        final String build = currentParameterMap.get(AppFactoryConstants.APPLICATION_BUILD);

        final String adStatus = currentParameterMap.get("autodeployment");

        // new path will holds "/$Application/$Stage/$Version/appinfo"
        newPath = resourcePath.substring((AppFactoryConstants.REGISTRY_GOVERNANCE_PATH +
                                          AppFactoryConstants.REGISTRY_APPLICATION_PATH).length());

        // 0th element is "", 1st element is app name , 2nd element is $Stage ,
        // 3rd element $Version, 4th element is appinfo
        String newPathArray[] = newPath.split(RegistryConstants.PATH_SEPARATOR);

        String currentAppName = newPathArray[1];
        String currentAppStage = newPathArray[2];
        String currentAppVersion = newPathArray[3];
        String currentAppInfo = newPathArray[4];

        // if the app is trunk then we need version.

        if ((AppFactoryConstants.TRUNK).equals(currentAppVersion)) {

            // Append version from here
            if (version != null) {
                newPath = RegistryConstants.PATH_SEPARATOR + currentAppName + RegistryConstants.PATH_SEPARATOR +
                        targetState + RegistryConstants.PATH_SEPARATOR + version + RegistryConstants.PATH_SEPARATOR +
                        currentAppInfo;

                // make newPath a absolute path
                newPath = AppFactoryConstants.REGISTRY_GOVERNANCE_PATH +
                          AppFactoryConstants.REGISTRY_APPLICATION_PATH + newPath;

                try {
                    requestContext.getRegistry().copy(resourcePath, newPath);

                    Resource newResource = requestContext.getRegistry().get(newPath);
                    requestContext.setResource(newResource);
                    requestContext.setResourcePath(new ResourcePath(newPath));
                } catch (RegistryException e) {
                    log.error("Can not perform transition", e);
                }

            } else {
                log.error("Can not find application version. " +
                          "Application version is required to perform lifecycle operation");
                return false;
            }
        } else {
            // Application is not a trunk version. So it can have version with it or user can define version
            if (version != null) {
                newPath = RegistryConstants.PATH_SEPARATOR + currentAppName + RegistryConstants.PATH_SEPARATOR +
                        targetState + RegistryConstants.PATH_SEPARATOR + version + RegistryConstants.PATH_SEPARATOR +
                        currentAppInfo;

            } else {
                newPath = RegistryConstants.PATH_SEPARATOR + currentAppName + RegistryConstants.PATH_SEPARATOR +
                        targetState + RegistryConstants.PATH_SEPARATOR + currentAppVersion +
                        RegistryConstants.PATH_SEPARATOR + currentAppInfo;
            }

            // make newPath a absolute path
            newPath = AppFactoryConstants.REGISTRY_GOVERNANCE_PATH +
                      AppFactoryConstants.REGISTRY_APPLICATION_PATH + newPath;

            try {

                requestContext.getRegistry().move(resourcePath, newPath);
//                deleting the parent resource;
                requestContext.getRegistry().delete(RegistryUtils.getParentPath(resourcePath));

                Resource newResource = requestContext.getRegistry().get(newPath);

                //Edit content when lifecycle change
                StAXOMBuilder builder = new StAXOMBuilder(newResource.getContentStream());
                OMElement configurations = builder.getDocumentElement();

                AXIOMXPath axiomxPath = new AXIOMXPath("//m:autodeployment");
                axiomxPath.addNamespace("m", "http://www.wso2.org/governance/metadata");
                Object selectedObject = axiomxPath.selectSingleNode(configurations);
                if (selectedObject != null) {
                    OMElement selectedNode = (OMElement) selectedObject;
                    selectedNode.setText(adStatus);
                    String inputConfiguration = configurations.toString();
                    newResource.setContent(inputConfiguration);
                    requestContext.getRegistry().put(newPath, newResource);
                } else{
//                    TODO:add the correct fix

                }


                requestContext.setResource(newResource);
                requestContext.setResourcePath(new ResourcePath(newPath));
				String appVersion = (version != null) ? version : currentAppVersion;
                editApplicationOnLifeCycleChange(applicationId, appVersion, currentState, targetState);


            } catch (RegistryException e) {
                log.error("Can not perform transition", e);
            } catch (XMLStreamException e) {
                log.error("Can Read the registry resource", e);
            } catch (JaxenException e) {
                log.error("Can not edit the job", e);
            }

        }

        try {

            if (requestContext.getAction().equals("Promote")) {
                // Executing the BPEL
                log.debug("Executing BPEL to perform Promote action");
                //executeBPEL(applicationId, revision, version, targetState, build);

            } else {
                // Demote
                log.debug("Executing Application deletion for " + requestContext.getAction() + " action");
                // currentAppName equals to applicationID (derived from current registry path)
                //LCCommons.executeAppDeletion(currentAppStage, currentAppName);

            }

            return true;
        } catch (Exception e) {
            log.error("Error occurred", e);  //To change body of catch statement use File | Settings | File Templates.
            return false;
       }

    }
//
//    /**
//     * This method will execute the BPEL as a web service in asynchronous way
//     *
//     * @param applicationId : application key
//     * @param revision      : svn revision
//     * @param version       : version of the application
//     * @param stage         : stage (i.e. Development, QA, Production)
//     * @param build         : build status (this will hold true/false)
//     */
//    private void executeBPEL(final String applicationId, final String revision,
//                             final String version, final String stage, final String build) {
//
//        AppFactoryConfiguration configuration =
//                                                ServiceReferenceHolder.getInstance()
//                                                                      .getAppFactoryConfiguration();
//
//        // get the deployToStage EPR from "appfactory.xml"
//        final String EPR =
//                           configuration.getFirstProperty(AppFactoryConstants.BPS_SERVER_URL) +
//                                   "DeployToStage";
//
//        try {
//            // Create a service client
//
//            ServiceClient client =
//                                   new ServiceClient(
//                                                     ServiceReferenceHolder.getInstance()
//                                                                           .getConfigContextService()
//                                                                           .getClientConfigContext(),
//                                                     null);
//
//            // Set the endpoint address
//            client.getOptions().setTo(new EndpointReference(EPR));
//            client.engageModule("rampart");
//            client.engageModule("addressing");
//            String userName =
//                              ServiceReferenceHolder.getInstance()
//                                                    .getAppFactoryConfiguration()
//                                                    .getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME);
//            client.getOptions().setUserName(userName);
//
//            String configs = CarbonUtils.getCarbonConfigDirPath();
//            client.getOptions().setProperty(RampartMessageData.KEY_RAMPART_POLICY,
//                                            loadPolicy(configs + "/appfactory/bpel-policy.xml"));
//            client.getOptions().setAction("http://wso2.org/process");
//            client.sendRobust(getPayload(applicationId, revision, version, stage, build));
//        } catch (Exception e) {
//            log.error(e);
//        }
//
//    }
//
//    private static Policy loadPolicy(String xmlPath) throws Exception {
//        StAXOMBuilder builder = new StAXOMBuilder(xmlPath);
//        return PolicyEngine.getPolicy(builder.getDocumentElement());
//    }
//
//    private static OMElement getPayload(
//            final String applicationId, final String revision, final String version,
//            final String stage,
//            final String build) throws XMLStreamException, javax.xml.stream.XMLStreamException {
//
//
//        String payload = "   <p:DeployToStageRequest xmlns:p=\"http://wso2.org\">\n" +
//                         "      <applicationId xmlns=\"http://wso2.org\">" + applicationId + "</applicationId>\n" +
//                         "      <revision xmlns=\"http://wso2.org\">" + revision + "</revision>\n" +
//                         "      <version xmlns=\"http://wso2.org\">" + version + "</version>\n" +
//                         "      <stage xmlns=\"http://wso2.org\">" + stage + "</stage>\n" +
//                         "      <build xmlns=\"http://wso2.org\">" + build + "</build>\n" +
//                         "   </p:DeployToStageRequest>";
//
//
//        return new StAXOMBuilder(new ByteArrayInputStream(payload.getBytes())).getDocumentElement();
//    }
//
    public void editApplicationOnLifeCycleChange(String applicationId, String sourceVersion,
                                                 String previousStage, String nextStage) {


        String deploymentState = "";
        int pollingPeriod = 0;


        try {
            ContinuousIntegrationSystemDriver jenkinsCISystemDriver = ServiceHolder.getInstance().getContinuousIntegrationSystemDriver();
            /*AppFactoryConfiguration configuration = ServiceReferenceHolder.getInstance().getAppFactoryConfiguration();

            boolean nextDeploymentStage = Boolean.parseBoolean(configuration.getFirstProperty(
                    "ApplicationDeployment.DeploymentStage." + nextStage + ".AutomaticDeployment.Enabled"));
            if (nextDeploymentStage) {
                pollingPeriod = Integer.parseInt(configuration.getFirstProperty("ApplicationDeployment.DeploymentStage."
                                                                                + nextStage + ".AutomaticDeployment.PollingPeriod"));
                deploymentState = "addAD";

            } else {*/
                deploymentState = "removeAD";
            //}
            jenkinsCISystemDriver.editADJobConfiguration(applicationId, sourceVersion, deploymentState, pollingPeriod);

        } catch (AppFactoryException ex) {
            String errorMsg = "Unable to publish lifeCycle change due to " + ex.getMessage();
            log.error(errorMsg, ex);
        }
    }

}
