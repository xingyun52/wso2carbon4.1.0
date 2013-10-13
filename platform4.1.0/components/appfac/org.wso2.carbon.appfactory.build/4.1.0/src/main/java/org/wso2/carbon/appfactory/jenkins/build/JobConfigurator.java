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

package org.wso2.carbon.appfactory.jenkins.build;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.InputStream;
import java.util.Map;

public class JobConfigurator {

    private static final Log log = LogFactory.getLog(JobConfigurator.class);

    Map<String, String> parameters;

    public JobConfigurator(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public OMElement configure() throws AppFactoryException {
        @SuppressWarnings("UnnecessaryLocalVariable")
        OMElement jobTemplate = getJobConfigurationTemplate();
        // Configure
        return jobTemplate;
    }

    /**
     * This method will get the jenkins job config file related to the repository type used and
     * will replace the default values of the required parameters with the actual values
     * @return the modified job config file which has actual data
     * @throws AppFactoryException
     */
    private OMElement getJobConfigurationTemplate() throws AppFactoryException {

        InputStream jobConfigTemplateInputStream = getBaseJobConfigTemplate(parameters.get
                (JenkinsCIConstants.REPOSITORY_TYPE));
        OMElement jobConfigTemplate;

        if (jobConfigTemplateInputStream != null) {

            try {
                StAXOMBuilder builder = new StAXOMBuilder(jobConfigTemplateInputStream);
                jobConfigTemplate = builder.getDocumentElement();
            } catch (XMLStreamException e) {
                throw new AppFactoryException(e.getMessage(), e);
            }

        } else {
            throw new AppFactoryException(
                    "Class loader is unable to find the jenkins job configuration template");
        }

        // set the svn repo for the application
        jobConfigTemplate = configureRepositoryData(jobConfigTemplate, parameters);
        /* setValueUsingXpath(jobConfigTemplate, JenkinsCIConstants.SVN_REPOSITORY_XPATH_SELECTOR,
        parameters.get(JenkinsCIConstants.REPOSITORY_URL));*/

        // set the maven 3 config name 
        setValueUsingXpath(jobConfigTemplate,
                JenkinsCIConstants.MAVEN3_CONFIG_NAME_XAPTH_SELECTOR,
                parameters.get(JenkinsCIConstants.MAVEN3_CONFIG_NAME));


        // set the maven 3 config name 
        //setValueUsingXpath(jobConfigTemplate,
        //                 JenkinsCIConstants.PREBUILDERS_MAVEN3_CONFIG_NAME_XPATH_SELECTOR,
        //               parameters.get(JenkinsCIConstants.MAVEN3_CONFIG_NAME));

        // Support for post build listener residing in jenkins server
        setValueUsingXpath(jobConfigTemplate,
                JenkinsCIConstants.PUBLISHERS_APPFACTORY_POST_BUILD_APP_EXTENSION_XPATH_SELECTOR,
                parameters.get(JenkinsCIConstants.APPLICATION_EXTENSION));

        setValueUsingXpath(jobConfigTemplate,
                JenkinsCIConstants.PUBLISHERS_APPFACTORY_POST_BUILD_APP_ID_XPATH_SELECTOR,
                parameters.get(JenkinsCIConstants.APPLICATION_ID));


        setValueUsingXpath(jobConfigTemplate,
                JenkinsCIConstants.PUBLISHERS_APPFACTORY_POST_BUILD_APP_VERSION_XPATH_SELECTOR,
                parameters.get(JenkinsCIConstants.APPLICATION_VERSION));
        /*setValueUsingXpath(jobConfigTemplate,
                JenkinsCIConstants.APPLICATION_TRIGGER_PERIOD,
                parameters.get("PollingPeriod"));*/

        return jobConfigTemplate;
    }

    /**
     * Add the repository related information to the jenkinsJobConfig file
     * @param jobConfigTemplate template of config file
     * @param parameters parameters to be added
     * @return the template after replacing the default values of repository related configurations
     * @throws AppFactoryException if an error occurs
     */
    private OMElement configureRepositoryData(OMElement jobConfigTemplate,
                                              Map<String, String> parameters)
            throws AppFactoryException {
        if ("git".equals(parameters.get(JenkinsCIConstants.REPOSITORY_TYPE))) {
            String url = parameters.get(JenkinsCIConstants.REPOSITORY_URL);
            setValueUsingXpath(jobConfigTemplate, JenkinsCIConstants.GIT_REPOSITORY_XPATH_SELECTOR,
                    url);
            String repositoryBranchName = parameters.get(JenkinsCIConstants.APPLICATION_VERSION);
            if ("trunk".equals(repositoryBranchName)) {
                repositoryBranchName = "master";
            }
            setValueUsingXpath(jobConfigTemplate,
                               JenkinsCIConstants.GIT_REPOSITORY_VERSION_XPATH_SELECTOR,
                               repositoryBranchName);
        } else {
            setValueUsingXpath(jobConfigTemplate, JenkinsCIConstants.SVN_REPOSITORY_XPATH_SELECTOR,
                    parameters.get(JenkinsCIConstants.REPOSITORY_URL));
        }
        return jobConfigTemplate;
    }

    /**
     * get the job config file template of the given repository type
     * this will be jenkinsJobConfig-git.xml or jenkinsJobConfig-svn.xml
     * @param repositoryType repository type
     * @return the jenkins job configuration file
     */
    private InputStream getBaseJobConfigTemplate(String repositoryType) {
        String configFileName = File.separator + "jenkinsJobConfig-".concat(repositoryType).
                concat(".xml");
        return this.getClass().getResourceAsStream(configFileName);
    }

    private void setValueUsingXpath(OMElement template, String selector, String value)
            throws AppFactoryException {

        try {
            AXIOMXPath axiomxPath = new AXIOMXPath(selector);
            Object selectedObject = axiomxPath.selectSingleNode(template);

            if (selectedObject != null && selectedObject instanceof OMElement) {
                OMElement svnRepoPathElement = (OMElement) selectedObject;
                svnRepoPathElement.setText(value);
            } else {
                log.warn("Unable to find xml element matching selector : " + selector);
            }

        } catch (Exception ex) {
            throw new AppFactoryException("Unable to set value to job config", ex);
        }
    }

}
