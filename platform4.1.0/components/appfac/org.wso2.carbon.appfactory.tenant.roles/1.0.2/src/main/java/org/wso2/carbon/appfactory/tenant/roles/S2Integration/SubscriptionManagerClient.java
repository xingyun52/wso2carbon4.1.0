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

package org.wso2.carbon.appfactory.tenant.roles.S2Integration;


import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.JaxenException;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.tenant.roles.util.Util;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;

import javax.xml.stream.XMLStreamException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.wso2.carbon.appfactory.tenant.roles.S2Integration.Constants.APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE;

/**
 * This client is used to subscribe to cartridges for production application deployment.
 * This client does 2 things.
 * 1. Create git repositories for subscriptions
 * 2. Subscribe to cartridges using the created git repo
 */
public class SubscriptionManagerClient {
    private static final Log log = LogFactory.getLog(SubscriptionManagerClient.class);
    public static final String DEPLOYER_APPLICATION_TYPE = ".Deployer.ApplicationType";

    //    Holds to cartridge information of each stage
    private static Map<String, Map<String,DeployerInfo>> deployerMap = new HashMap<String, Map<String,DeployerInfo>>();
    private ExecutorService service;


    public SubscriptionManagerClient() {
        init();
    }

    private void init() {
        try {
            if (deployerMap.isEmpty()) {
                AppFactoryConfiguration configuration = Util.getConfiguration();
                Map<String, List<String>> properties = configuration.getAllProperties();

                Set<String> stagesList = new HashSet<String>();

                for (Map.Entry<String, List<String>> property : properties.entrySet()) {
                    String key = property.getKey();
                    if (key.startsWith(APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE) &&
                            key.contains(DEPLOYER_APPLICATION_TYPE)) {
                        String stage = key.substring(APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE.length(),
                                key.indexOf(DEPLOYER_APPLICATION_TYPE));
                        stagesList.add(stage);


                    }
                }

                for (String stage : stagesList) {
                    String[] appType = configuration.getProperties(APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage +
                            DEPLOYER_APPLICATION_TYPE);

                    for (String type : appType) {
                        initDeployerMap(configuration, stage, type);
                    }
                }
            }
        } catch (AppFactoryException e) {
            String msg = "Unable to read subscription properties from configuration";
            log.error(msg, e);
        }
    }

    private void initDeployerMap(AppFactoryConfiguration configuration, String stage, String appType)
            throws AppFactoryException {
        Map<String,DeployerInfo> typeMap = new HashMap<String, DeployerInfo>();
        if (deployerMap.containsKey(stage)) {
            typeMap = deployerMap.get(stage);
        }

        DeployerInfo deployerInfo = new DeployerInfo();

        String endpoint = configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Endpoint");

//        No endpoint has been defined. This is not the S2 Deployer
        if(endpoint == null || "".equals(endpoint)){
            return;
        }
        deployerInfo.setEndpoint(endpoint);

        String minInstances = configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Properties.Property.minInstances");
        if (minInstances != null && !minInstances.equals("")) {
            deployerInfo.setMinInstances(Integer.parseInt(minInstances));
        }

        String maxInstances = configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Properties.Property.maxInstances");
        if (maxInstances != null && !maxInstances.equals("")) {
            deployerInfo.setMaxInstances(Integer.parseInt(maxInstances));
        }

        String shouldActive = configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Properties.Property.shouldActivate");
        if (shouldActive != null && !shouldActive.equals("")) {
            deployerInfo.setShouldActivate(Boolean.parseBoolean(shouldActive));
        }

        deployerInfo.setAlias(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Properties.Property.alias"));

        deployerInfo.setCartridgeType(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Properties.Property.cartridgeType"));

        deployerInfo.setRepoURL(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Properties.Property.repoURL"));

        deployerInfo.setDataCartridgeType(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Properties.Property.dataCartridgeType"));

        deployerInfo.setDataCartridgeAlias(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Properties.Property.dataCartridgeAlias"));

        deployerInfo.setEndpoint(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".Endpoint"));

        String className = configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".RepositoryProvider.Property.Class");
        deployerInfo.setClassName(className);

        try {
            ClassLoader loader = getClass().getClassLoader();
            Class<?> repoProvider = Class.forName(className, true, loader);
            deployerInfo.setRepoProvider(repoProvider);
        } catch (ClassNotFoundException e) {
            String msg = "Unable to load repository provider class";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }

        deployerInfo.setBaseURL(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".RepositoryProvider.Property.BaseURL"));

        deployerInfo.setAdminUserName(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".RepositoryProvider.Property.AdminUserName"));

        deployerInfo.setAdminPassword(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".RepositoryProvider.Property.AdminPassword"));

        deployerInfo.setRepoPattern(configuration.getFirstProperty(
                APPLICATION_DEPLOYMENT_DEPLOYMENT_STAGE + stage + DEPLOYER_APPLICATION_TYPE + "." + appType +
                        ".RepositoryProvider.Property.URLPattern"));

        deployerInfo.setAppType(appType);

        typeMap.put(appType,deployerInfo);
        deployerMap.put(stage, typeMap);
    }

    /**
     * This method does 2 things.
     * 1. Create a git repo for subscription
     * 2. Subscribe to the given cartridge
     *
     * @param applicationId The application ID of the newly created application
     * @throws AppFactoryException
     */
    public void subscribe(String applicationId) throws AppFactoryException {
        init();
        String applicationType = null;

        if (service == null) {
            service = Executors.newFixedThreadPool(50);
        }

        try {
            RegistryService registryService = Util.getRegistryService();
            Registry registry = registryService.getGovernanceSystemRegistry();

            String path = "/repository/applications/" + applicationId + "/appinfo";
            if (registry.resourceExists(path)) {
                Resource resource = registry.get(path);

                StAXOMBuilder builder = new StAXOMBuilder(resource.getContentStream());
                OMElement configuration = builder.getDocumentElement();

                AXIOMXPath xpath = new AXIOMXPath("//m:application/m:type");
                xpath.addNamespace("m",configuration.getNamespace().getNamespaceURI());
                Object selectedObject = xpath.selectSingleNode(configuration);
                if (selectedObject != null) {
                    OMElement selectedNode = (OMElement) selectedObject;
                    applicationType = selectedNode.getText();
                }
            }

        } catch (RegistryException e) {
            String msg = "Unable to find the rxt resource : " + applicationId;
            log.error(msg,e);
            throw new AppFactoryException(msg,e);
        } catch (XMLStreamException e) {
            String msg = "Unable to read the rxt resource : " + applicationId;
            log.error(msg,e);
            throw new AppFactoryException(msg,e);
        } catch (JaxenException e) {
            String msg = "Unable to parse the rxt resource : " + applicationId;
            log.error(msg,e);
            throw new AppFactoryException(msg,e);
        }

        if(applicationType == null){
            return;
        }

        for (Map.Entry<String, Map<String,DeployerInfo>> deployerInfoEntry : deployerMap.entrySet()) {
            String stage = deployerInfoEntry.getKey();
            DeployerInfo deployerInfo = null;

            if(deployerInfoEntry.getValue().containsKey(applicationType)){
                deployerInfo = deployerInfoEntry.getValue().get(applicationType);
            }else{
                deployerInfo = deployerInfoEntry.getValue().get("*");
            }

            SubscribeExecutor executor = new SubscribeExecutor();
            executor.setApplicationId(applicationId);
            executor.setDeployerInfo(deployerInfo);
            executor.setStage(stage);

            service.execute(executor);
            if (log.isDebugEnabled()) {
                log.debug("Successfully sent subscription request to stage : " + deployerInfoEntry.getKey());
            }
        }

    }


}
