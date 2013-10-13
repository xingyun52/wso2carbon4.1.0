/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.cep.core;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.AbstractDeployer;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.internal.CEPService;
import org.wso2.carbon.cep.core.internal.config.BucketHelper;
import org.wso2.carbon.cep.core.internal.ds.CEPServiceValueHolder;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.internal.util.NotDeployedBucketElement;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deploy cep buckets as axis2 service
 */
@SuppressWarnings("unused")
public class BucketDeployer extends AbstractDeployer {

    private static Log log = LogFactory.getLog(BucketDeployer.class);
    private ConfigurationContext configurationContext;
    private static Map<String, String> fileNameToBucketNameMap = new ConcurrentHashMap<String, String>();


    public void init(ConfigurationContext configurationContext) {
        this.configurationContext = configurationContext;
    }

    /**
     * Process the bucket file, create a cep buckets and deploy it
     *
     * @param deploymentFileData information about the cep bucket
     * @throws org.apache.axis2.deployment.DeploymentException
     *          for any errors
     */
    public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {
        String path = deploymentFileData.getAbsolutePath();
        File bucketFile = new File(path);

        //   PrivilegedCarbonContext.getCurrentContext().setUsername(CarbonConstants.REGISTRY_SYSTEM_USERNAME);

        try {

            OMElement bucketElement = getBucketOMElement(path, bucketFile);

            if (!new QName(CEPConstants.CEP_CONF_NAMESPACE, CEPConstants.CEP_CONF_ELE_BUCKET).equals(bucketElement.getQName())) {
                throw new DeploymentException("Invalid root element " + bucketElement.getQName() + " in " + bucketFile.getName());
            }

            String bucketName = bucketElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_NAME));
            fileNameToBucketNameMap.put(deploymentFileData.getAbsolutePath(), bucketName);
            //If the CepService is present adds buckets element to the registry.
            // else keep that in memory for CepService to add them to the registry

            CEPService cepService = CEPServiceValueHolder.getInstance().getCepService();
            if (null != cepService) {
                AxisConfiguration axisConfiguration = this.configurationContext.getAxisConfiguration();
                int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();
                if (cepService.deployBucket(BucketHelper.fromOM(bucketElement), axisConfiguration, deploymentFileData.getAbsolutePath())) {
                    log.info("Successfully deployed the bucket " + bucketName);
                }
            } else {
                CEPServiceValueHolder.getInstance().getNotDeployedBucketElements().add(new NotDeployedBucketElement(bucketElement, deploymentFileData.getAbsolutePath()));
                log.info(bucketFile.getName() + " is scheduled for deployment");
            }


        } catch (CEPConfigurationException e) {
            String errorMessage = "wrong configuration provided for adding " + bucketFile.getName();
            log.error(errorMessage, e);
            throw new DeploymentException(errorMessage, e);
        } catch (Throwable t) {
            log.error("The deployment of " + bucketFile.getName() + " is not valid.", t);
            throw new DeploymentException(t);
        }
    }

    private OMElement getBucketOMElement(String path,
                                         File bucketFile) throws DeploymentException {
        OMElement bucketElement;
        BufferedInputStream inputStream = null;
        try {
            //read  and build the bucket from file
            inputStream = new BufferedInputStream(new FileInputStream(bucketFile));
            XMLStreamReader parser = XMLInputFactory.newInstance().
                    createXMLStreamReader(inputStream);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            bucketElement = builder.getDocumentElement();
            bucketElement.build();

        } catch (FileNotFoundException e) {
            String errorMessage = " .xml file cannot be found in the path : " + path;
            log.error(errorMessage, e);
            throw new DeploymentException(errorMessage, e);
        } catch (XMLStreamException e) {
            String errorMessage = "Invalid XML for " + bucketFile.getName() + " located in the path : " + path;
            log.error(errorMessage, e);
            throw new DeploymentException(errorMessage, e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                String errorMessage = "Can not close the input stream";
                log.error(errorMessage, e);
            }
        }
        return bucketElement;
    }

    public void setExtension(String extension) {

    }

    /**
     * Removing already deployed bucket
     *
     * @param filePath the path to the bucket to be removed
     * @throws org.apache.axis2.deployment.DeploymentException
     *
     */
    public void undeploy(String filePath) throws DeploymentException {
        CEPService cepService = CEPServiceValueHolder.getInstance().getCepService();
        String bucketName = fileNameToBucketNameMap.remove(filePath);
        if (bucketName != null) {
            try {
                AxisConfiguration axisConfiguration = this.configurationContext.getAxisConfiguration();
                int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();
                if (cepService.unDeployBucket(bucketName, tenantId)) {
                    log.info("Undeployed bucket " + bucketName);
                }
            } catch (CEPConfigurationException e) {
                throw new DeploymentException("Can not undeploy the cep bucket with file name " + filePath);
            }
        }

    }

    public void setDirectory(String directory) {

    }


    public static void addToFileNameBucketNameMap(String bucketPath, String bucketName) {
        fileNameToBucketNameMap.put(bucketPath, bucketName);
    }

}


