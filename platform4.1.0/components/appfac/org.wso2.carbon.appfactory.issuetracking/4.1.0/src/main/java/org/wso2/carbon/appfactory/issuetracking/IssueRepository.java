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

import org.wso2.carbon.appfactory.issuetracking.beans.GenericIssue;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericIssueType;
import org.wso2.carbon.appfactory.issuetracking.beans.IssueRepositoryConnector;
import org.wso2.carbon.appfactory.issuetracking.exception.IssueTrackerException;
import org.wso2.carbon.appfactory.issuetracking.internal.ServiceContainer;
import org.wso2.carbon.appfactory.issuetracking.redmine.AppFactoryRedmineIssueTrackerConnector;

import java.util.List;

/**
 *
 *
 */
public class IssueRepository {
    private IssueRepositoryConnector connector;
    private static IssueRepository issueRepository;

    private IssueRepository() {
        this.connector = new AppFactoryRedmineIssueTrackerConnector();
        this.connector.setConfiguration(ServiceContainer.getAppFactoryConfiguration());

    }

    public String reportIssue(GenericIssue genericIssue, String appID) throws
                                                                       IssueTrackerException {
        String issueID;
        try {
            this.connector.init();
            issueID = connector.reportIssue(genericIssue, getProjectKey(appID));
        } finally {
            this.connector.close();

        }

        return issueID;
    }

    private String getProjectKey(String appID) {
        String projectKey;

        projectKey = connector.getProjectApplicationMapping().getProjectKey(appID);

        return projectKey;
    }

    public String updateIssue(GenericIssue genericIssue, String appID)
            throws IssueTrackerException {
        String issueID;
        try {
            this.connector.init();
            issueID = connector.updateIssue(genericIssue, getProjectKey(appID));
        } finally {
            this.connector.close();

        }
        return issueID;
    }

    public List<GenericIssue> getAllIssuesOfProject(String appId) throws IssueTrackerException {
        List<GenericIssue> list;
        try {
            this.connector.init();
            list = connector.getAllIssuesOfProject(getProjectKey(appId));
        } finally {
            this.connector.close();
        }
        return list;
    }

    public GenericIssue getIssueByKey(String key, String appID) throws IssueTrackerException {
        GenericIssue issue;
        try {
            this.connector.init();
            issue = connector.getIssueByKey(key, getProjectKey(appID));
        } finally {
            this.connector.close();

        }
        return issue;
    }

    public String[] getIssueStatus() throws IssueTrackerException {
        String[] statuses;
        try {
            this.connector.init();
            statuses = connector.getIssueStatuses();
        } finally {
            this.connector.close();

        }
        return statuses;
    }

    public GenericIssueType[] getIssueTypes() throws IssueTrackerException {
        GenericIssueType[] types;
        try {
            this.connector.init();
            types = connector.getIssueTypes();
        } finally {
            this.connector.close();

        }
        return types;
    }

    public static IssueRepository getIssueRepository() {
        if (issueRepository == null) {
            issueRepository = new IssueRepository();
        }
        return issueRepository;
    }

    public IssueRepositoryConnector getConnector() {
        return connector;
    }

    public void setConnector(IssueRepositoryConnector connector) {
        this.connector = connector;
    }

    public String[] getAvailableAssignees(String appID) throws IssueTrackerException {
        String[] assignees;
        try {
            this.connector.init();
            assignees = this.connector.getAvailableAssignees(getProjectKey(appID));
        } finally {
            this.connector.close();

        }
        return assignees;
    }
    public String getUrlForReportIssue(String appID)
            throws IssueTrackerException{
        String url;
        try {
            this.connector.init();
            url = this.connector.getUrlForReportIssue(getProjectKey(appID));
        } finally {
            this.connector.close();

        }
        return url;
    }

}
