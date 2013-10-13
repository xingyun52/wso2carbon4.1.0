/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.appfactory.issuetracking;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.ApplicationEventsListener;
import org.wso2.carbon.appfactory.core.dto.Application;
import org.wso2.carbon.appfactory.core.dto.UserInfo;
import org.wso2.carbon.appfactory.core.dto.Version;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericUser;
import org.wso2.carbon.appfactory.issuetracking.beans.IssueRepositoryConnector;
import org.wso2.carbon.appfactory.issuetracking.beans.Project;
import org.wso2.carbon.appfactory.issuetracking.exception.IssueTrackerException;

/**
 *
 */
public class AppFactoryApplicationEventListener extends ApplicationEventsListener {
    private static final Log log = LogFactory.getLog(AppFactoryApplicationEventListener.class);
    private IssueRepository repository;
    private int listnerPriority;

    public AppFactoryApplicationEventListener(int listnerPriority) {
        repository = IssueRepository.getIssueRepository();
        this.listnerPriority = listnerPriority;
        log.info("Application listener for redmine was initiated.");
        
    }

    @Override
    public void onCreation(Application application) throws AppFactoryException {

        IssueRepositoryConnector connector = null;
        try {
            log.info("On creation event received for redmine application listener.");
            connector = repository.getConnector();
            connector.init();
            Project project = new Project();
            project.setName(application.getName());
            project.setKey(connector.getProjectApplicationMapping().getProjectKey(application.getId()));
            project.setDescription(application.getDescription());
            connector.createProject(project);
            log.info("On creation event successfully handled by redmine application listener.");
        } catch (IssueTrackerException e) {
            String msg = "Error while  creating project in issue repository for " + application.getName();
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        } finally {
            connector.close();
        }
    }

    @Override
    public void onUserAddition(Application application, UserInfo userInfo)
            throws AppFactoryException {
        IssueRepositoryConnector connector = null;


        try {
            log.info("On user addition event received for redmine application listener.");
            connector = repository.getConnector();
            connector.init();
            Project project = new Project();
            project.setName(application.getName());
            project.setKey(connector.getProjectApplicationMapping().getProjectKey(application.getId()));
            project.setDescription(application.getDescription());
            GenericUser user = new GenericUser();
            user.setUsername(userInfo.getUserName());
            user.setRoles(userInfo.getRoles());
            connector.addUserToProject(user, project);
            log.info("On user addition event successfully handled by redmine application listener.");
        } catch (IssueTrackerException e) {
            String msg = "Error while adding the user " + userInfo.getUserName() + " of " + application.getName();
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        } finally {
            connector.close();
        }
    }

    @Override
    public void onUserDeletion(Application application, UserInfo userInfo)
            throws AppFactoryException {
        //Todo
    }

    @Override
    public void onRevoke(Application application) throws AppFactoryException {
        //Todo
    }

    @Override
    public void onVersionCreation(Application application, Version version, Version version1)
            throws AppFactoryException {
        IssueRepositoryConnector connector = null;


        try {
            log.info("On version creation event received for redmine application listener.");
            connector = repository.getConnector();
            connector.init();
            Project project = new Project();
            org.wso2.carbon.appfactory.issuetracking.beans.Version isstrackerVersion=new org.wso2.carbon.appfactory.issuetracking.beans.Version();
            isstrackerVersion.setName(version1.getId());
            project.setName(application.getName());
            project.setKey(connector.getProjectApplicationMapping().getProjectKey(application.getId()));
            project.setDescription(application.getDescription());
            connector.createVersionInProject(project,isstrackerVersion);
            log.info("On version creation event successfully handled by redmine application listener.");
        } catch (IssueTrackerException e) {
            String msg = "Error while creating version "+version1.getId() +" for "+application.getId();
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        } finally {
            connector.close();
        }

    }
    
    public void onLifeCycleStageChange(Application application,
            Version version, String previosStage, String nextStage) throws AppFactoryException {

    }

    public void onAutoDeploymentVersionChange(Application application, Version previousVersion,
                                                         Version newVersion, String newStage)throws AppFactoryException {
    }

    @Override
    public int getPriority() {
        return listnerPriority;
    }
}
