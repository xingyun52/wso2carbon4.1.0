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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.ContinuousIntegrationSystemDriver;
import org.wso2.carbon.appfactory.jenkins.build.internal.ServiceContainer;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryMgtException;
import org.wso2.carbon.appfactory.utilities.project.ProjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JenkinsCISystemDriver implements ContinuousIntegrationSystemDriver {

    private RestBasedJenkinsCIConnector connector;
    private String[] defaultGlobalRoles;
    
    private static final Log log = LogFactory.getLog(JenkinsCISystemDriver.class);

    public JenkinsCISystemDriver(RestBasedJenkinsCIConnector connector, String[] defaultGlobalRoles) {
        this.connector = connector;
        this.defaultGlobalRoles = defaultGlobalRoles;
    }

    @Override
    public void createJob(String applicationId, String version, String revision)
                                                                                throws AppFactoryException {
        // some how get svn location.

        Map<String, String> parameters = new HashMap<String, String>();
        final String repoType = "svn";

        try {

            String svnRepoUrl =
                                ServiceContainer.getRepositoryManager()
                                                .getURLForAppversion(applicationId, version,
                                                                     repoType);
            if (log.isDebugEnabled()) {
                log.debug(String.format("svn repo url for applcation id:%s, version: %s, repository type: %s, url: %s",
                                        applicationId, version, repoType, svnRepoUrl));
            }
            parameters.put(JenkinsCIConstants.SVN_REPOSITORY, svnRepoUrl);

            String applicationType = ProjectUtils.getApplicationType(applicationId);
            parameters.put(JenkinsCIConstants.APPLICATION_EXTENSION, applicationType);

        } catch (RepositoryMgtException repoEx) {
            String errorMsg =
                              String.format("Unable to find the repository url for applicaiton id: %s, version: %s, repository type: %s",
                                            applicationId, version, repoType);
            log.error(errorMsg, repoEx);
            throw new AppFactoryException(errorMsg, repoEx);
        }

        parameters.put(JenkinsCIConstants.MAVEN3_CONFIG_NAME,
                       ServiceContainer.getAppFactoryConfiguration()
                                       .getFirstProperty(JenkinsCIConstants.MAVEN3_CONFIG_NAME_CONFIG_SELECTOR));

        parameters.put(JenkinsCIConstants.SVN_CREDENTIALS_USERNAME,
                       ServiceContainer.getAppFactoryConfiguration()
                                    .getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME));
        parameters.put(JenkinsCIConstants.SVN_CREDENTIALS_PASSWORD,
                       ServiceContainer.getAppFactoryConfiguration()
                                    .getFirstProperty(AppFactoryConstants.SERVER_ADMIN_PASSWORD));

        parameters.put(JenkinsCIConstants.APPLICATION_ID, applicationId);
        parameters.put(JenkinsCIConstants.APPLICATION_VERSION, version);
        
        // TODO : Hard coded application extension here. in future this could be either 'car'or 'war'
        // and will be selected by user
        
        this.connector.createJob(getJobName(applicationId, version, revision), parameters);

    }

    public void deleteJob(String jobName) throws AppFactoryException {
        connector.deleteJob(jobName);
    }

    public List<String> getAllJobNames() throws AppFactoryException {
        return connector.getAllJobs();
    }

    public void startBuild(String jobName) throws AppFactoryException {
        connector.startBuild(jobName);
    }

    public boolean isJobExists(String jobName) throws AppFactoryException{
       return connector.isJobExists(jobName);
    }
    
    
    
    public String getJobName(String applicationId, String version, String revision){
      //Job name will be '<ApplicationId>-<version>-default'
        return new StringBuilder(applicationId).append('-').append(version)
                   .append('-').append("default").toString();
    }

    public void createApplicationAccount(String applicationId, String[] initialUserIds) throws AppFactoryException {
       String applicationSelectorRegEx = new StringBuilder(applicationId).append(".*").toString();
       connector.createRole(applicationId, applicationSelectorRegEx);
       if ( initialUserIds != null){
          assignUsersApplication(applicationId, initialUserIds);
       }
    }

    public void assignUsersApplication(String applicationId, String[] userIds)
                                                                              throws AppFactoryException {
        connector.assignUsers(userIds, new String[]{applicationId}, defaultGlobalRoles);
    }
    
    
}
