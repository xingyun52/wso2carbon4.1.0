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

package org.wso2.carbon.appfactory.governance.lifecycle;

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
import org.wso2.carbon.appfactory.governance.internal.ServiceReferenceHolder;
import org.wso2.carbon.governance.registry.extensions.executors.ServiceVersionExecutor;
import org.wso2.carbon.governance.registry.extensions.executors.utils.ExecutorConstants;
import org.wso2.carbon.governance.registry.extensions.interfaces.Execution;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.ResourcePath;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
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


        // new path will holds "/$Application/$Stage/$Version/appinfo"
        newPath = resourcePath.substring((AppFactoryConstants.REGISTRY_GOVERNANCE_PATH +
                                          AppFactoryConstants.REGISTRY_APPLICATION_PATH).length());

        // 0th element is "", 1st element is app name , 2nd element is $Stage ,
        // 3rd element $Version, 4th element is appinfo
        String newPathArray[] = newPath.split("/");

        String currentAppName = newPathArray[1];
        String currentAppStage = newPathArray[2];
        String currentAppVersion = newPathArray[3];
        String currentAppInfo = newPathArray[4];

        // if the app is trunk then we need version.

        if ((AppFactoryConstants.TRUNK).equals(currentAppVersion)) {

            // Append version from here
            if (version != null) {
                newPath = "/" + currentAppName + "/" + targetState + "/" + version + "/" + currentAppInfo;

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
                newPath = "/" + currentAppName + "/" + targetState + "/" + version + "/" + currentAppInfo;

            } else {
                newPath = "/" + currentAppName + "/" + targetState + "/" + currentAppVersion + "/" + currentAppInfo;
            }

            // make newPath a absolute path
            newPath = AppFactoryConstants.REGISTRY_GOVERNANCE_PATH +
                      AppFactoryConstants.REGISTRY_APPLICATION_PATH + newPath;

            try {

                requestContext.getRegistry().move(resourcePath, newPath);

                Resource newResource = requestContext.getRegistry().get(newPath);
                requestContext.setResource(newResource);
                requestContext.setResourcePath(new ResourcePath(newPath));


            } catch (RegistryException e) {
                log.error("Can not perform transition", e);
            }

        }

        try {

            if (requestContext.getAction().equals("Promote")) {
                // Executing the BPEL
                log.debug("Executing BPEL to perform Promote action");
                executeBPEL(applicationId, revision, version, targetState, build);

            } else {
                // Demote
                log.debug("Executing Application deletion for " + requestContext.getAction() + " action");
                // currentAppName equals to applicationID (derived from current registry path)
                LCCommons.executeAppDeletion(currentAppStage, currentAppName);

            }

            return true;
        } catch (Exception e) {
            log.error("Error occurred", e);  //To change body of catch statement use File | Settings | File Templates.
            return false;
       }

    }

    /**
     * This method will execute the BPEL as a web service in asynchronous way
     *
     * @param applicationId : application key
     * @param revision      : svn revision
     * @param version       : version of the application
     * @param stage         : stage (i.e. Development, QA, Production)
     * @param build         : build status (this will hold true/false)
     */
    private void executeBPEL(final String applicationId, final String revision,
                             final String version, final String stage, final String build) {


        AppFactoryConfiguration configuration = ServiceReferenceHolder.getInstance().getAppFactoryConfiguration();

        // get the deployToStage EPR from "appfactory.xml"
        final String EPR = configuration.getFirstProperty(AppFactoryConstants.BPS_SERVER_URL) + "DeployToStage";

        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
                try {
                    //Create a service client
                    ServiceClient client = new ServiceClient();

                    //Set the endpoint address
                    client.getOptions().setTo(new EndpointReference(EPR));

                    //Make the request and get the response
                    client.sendRobust(getPayload(applicationId, revision, version, stage, build));
                } catch (AxisFault e) {
                    log.error(e);
                    e.printStackTrace();
                } catch (XMLStreamException e) {
                    log.error(e);
                }
            }
        }).start();
    }

    private static OMElement getPayload(
            final String applicationId, final String revision, final String version,
            final String stage,
            final String build) throws XMLStreamException, javax.xml.stream.XMLStreamException {


        String payload = "   <p:DeployToStageRequest xmlns:p=\"http://wso2.org\">\n" +
                         "      <applicationId xmlns=\"http://wso2.org\">" + applicationId + "</applicationId>\n" +
                         "      <revision xmlns=\"http://wso2.org\">" + revision + "</revision>\n" +
                         "      <version xmlns=\"http://wso2.org\">" + version + "</version>\n" +
                         "      <stage xmlns=\"http://wso2.org\">" + stage + "</stage>\n" +
                         "      <build xmlns=\"http://wso2.org\">" + build + "</build>\n" +
                         "   </p:DeployToStageRequest>";


        return new StAXOMBuilder(new ByteArrayInputStream(payload.getBytes())).getDocumentElement();
    }


}
