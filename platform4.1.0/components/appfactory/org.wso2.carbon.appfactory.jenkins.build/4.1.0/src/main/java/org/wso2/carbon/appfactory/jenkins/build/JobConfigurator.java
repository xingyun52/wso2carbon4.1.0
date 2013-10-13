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

import java.io.InputStream;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;

public class JobConfigurator {

    private static final Log log = LogFactory.getLog(JobConfigurator.class);

    Map<String, String> parameters;

    public JobConfigurator(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public OMElement configure() throws AppFactoryException {
        OMElement jobTemplate = getJobConfigurationTemplate();
        // Configure
        return jobTemplate;
    }

    private OMElement getJobConfigurationTemplate() throws AppFactoryException {

        InputStream jobConfigTemplateInputStream =
                                                   this.getClass()
                                                       .getResourceAsStream("/jenkinsJobConfig.xml");
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
        setValueUsingXpath(jobConfigTemplate, JenkinsCIConstants.SVN_REPOSITORY_XPATH_SELECTOR,
                           parameters.get(JenkinsCIConstants.SVN_REPOSITORY));

        // set the maven 3 config name 
        setValueUsingXpath(jobConfigTemplate,
                           JenkinsCIConstants.MAVEN3_CONFIG_NAME_XAPTH_SELECTOR,
                           parameters.get(JenkinsCIConstants.MAVEN3_CONFIG_NAME));

    
        // set the maven 3 config name 
        setValueUsingXpath(jobConfigTemplate,
                           JenkinsCIConstants.PREBUILDERS_MAVEN3_CONFIG_NAME_XPATH_SELECTOR,
                           parameters.get(JenkinsCIConstants.MAVEN3_CONFIG_NAME));

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

        return jobConfigTemplate;
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
