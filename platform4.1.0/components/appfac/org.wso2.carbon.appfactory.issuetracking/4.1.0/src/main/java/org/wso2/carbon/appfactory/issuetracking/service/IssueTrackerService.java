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
package org.wso2.carbon.appfactory.issuetracking.service;

import org.wso2.carbon.appfactory.issuetracking.IssueRepository;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericIssue;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericIssueType;
import org.wso2.carbon.appfactory.issuetracking.exception.IssueTrackerException;

import java.util.List;

/**
 *
 *
 */
public class IssueTrackerService {
    private IssueRepository repository;

    public IssueTrackerService() {
        this.repository = IssueRepository.getIssueRepository();
    }

    public String reportIssue(GenericIssue genericIssue, String appID) throws
                                                                       IssueTrackerException {

        return repository.reportIssue(genericIssue, appID);
    }

    public String updateIssue(GenericIssue genericIssue, String appID)
            throws IssueTrackerException {
        return repository.updateIssue(genericIssue, appID);
    }

    public List<GenericIssue> getAllIssuesOfApplication(String appId) throws IssueTrackerException {
        return repository.getAllIssuesOfProject(appId);
    }

    public GenericIssue getIssueByKey(String key, String appID) throws IssueTrackerException {
        return repository.getIssueByKey(key, appID);
    }

    public String[] getIssueStatus() throws IssueTrackerException {
        return repository.getIssueStatus();
    }

    public GenericIssueType[] getIssueTypes() throws IssueTrackerException {
        return repository.getIssueTypes();
    }

    public String[] getAvailableAssignees(String appID) throws IssueTrackerException {
        return this.repository.getAvailableAssignees(appID);
    }
    public String getUrlForReportIssue(String appID) throws IssueTrackerException {
        return this.repository.getUrlForReportIssue(appID);
    }
}
