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
package org.wso2.carbon.appfactory.issuetracking.redmine;

import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueStatus;
import com.taskadapter.redmineapi.bean.Membership;
import com.taskadapter.redmineapi.bean.Role;
import com.taskadapter.redmineapi.bean.Tracker;
import com.taskadapter.redmineapi.bean.User;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.application.mgt.service.ApplicationManagementException;
import org.wso2.carbon.appfactory.application.mgt.service.UserInfoBean;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.issuetracking.AbstractRepositoryConnector;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericIssue;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericIssueType;
import org.wso2.carbon.appfactory.issuetracking.beans.GenericUser;
import org.wso2.carbon.appfactory.issuetracking.beans.Project;
import org.wso2.carbon.appfactory.issuetracking.beans.ProjectApplicationMapping;
import org.wso2.carbon.appfactory.issuetracking.beans.Version;
import org.wso2.carbon.appfactory.issuetracking.exception.IssueTrackerException;
import org.wso2.carbon.appfactory.issuetracking.internal.ServiceContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class AppFactoryRedmineIssueTrackerConnector extends AbstractRepositoryConnector {
    private static final Log log = LogFactory.getLog(AppFactoryRedmineIssueTrackerConnector.class);
    public static final String REDMINE_ISSUE_TRACKER_CONFIG = "IssueTrackerConnector.redmine.Property.";
    public static final String REDMINE_URL = REDMINE_ISSUE_TRACKER_CONFIG + "Url";
    public static final String REDMINE_ADMIN_USERNAME = REDMINE_ISSUE_TRACKER_CONFIG + "AdminUsername";
    public static final String REDMINE_ADMIN_PASSWORD = REDMINE_ISSUE_TRACKER_CONFIG + "AdminPassword";
    public static final String REDMINE_DEFAULT_ROLE = REDMINE_ISSUE_TRACKER_CONFIG + "DefaultRole";
    public static final String REDMINE_AUTHENTICATOR_ID = REDMINE_ISSUE_TRACKER_CONFIG + "AuthenticatorId";
    public static final String REDMINE_ISSUES = "issues";

    private RedmineManager manager;

    public AppFactoryRedmineIssueTrackerConnector() {

    }

    public void init() {
        String redmineURL = getConfiguration().getFirstProperty(REDMINE_URL);
        String adminUsername = getConfiguration().getFirstProperty(REDMINE_ADMIN_USERNAME);
        String adminPassword = getConfiguration().getFirstProperty(REDMINE_ADMIN_PASSWORD);
        manager = new RedmineManager(redmineURL, adminUsername, adminPassword);

    }

    @Override
    public String reportIssue(GenericIssue genericIssue, String projectID)
            throws IssueTrackerException {
        Issue issue;
        try {
            issue = manager.createIssue(projectID, getNewRedmineIssue(genericIssue));
        } catch (RedmineException e) {
            String msg = "Error while  reporting issue for " + projectID;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return String.valueOf(issue.getId());
    }

    private Issue getNewRedmineIssue(GenericIssue genericIssue) throws IssueTrackerException {
        genericIssue.setIssueKey("1");
        Issue redmineIssue = getRedmineIssue(genericIssue);
        redmineIssue.setId(null);
        return redmineIssue;
    }

    private User getUserByLogin(String assignee) throws RedmineException {
        List<User> userList = manager.getUsers();
        for (User user : userList) {
            if (user.getLogin().equals(assignee)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public String updateIssue(GenericIssue genericIssue, String projectID)
            throws IssueTrackerException {
        Issue issue = getRedmineIssue(genericIssue);
        try {
            manager.update(issue);
        } catch (RedmineException e) {
            String msg = "Error while  updating issue " + genericIssue.getIssueKey() + " for " + projectID;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return String.valueOf(issue.getId());
    }

    @Override
    public List<GenericIssue> getAllIssuesOfProject(String project) throws IssueTrackerException {
        List<GenericIssue> issues;
        try {
            issues = getGenericIssues(manager.getIssues(project, null));
        } catch (RedmineException e) {
            String msg = "Error while getting all issues of " + project;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return issues;
    }

    private List<GenericIssue> getGenericIssues(List<Issue> issues) throws RedmineException {
        List<GenericIssue> issueList = new ArrayList<GenericIssue>();
        List<User> users = manager.getUsers();
        List<IssueStatus> statuses = manager.getStatuses();
        for (Issue redmineIssue : issues) {
            issueList.add(getGenericIssue(redmineIssue, users, statuses));
        }
        return issueList;
    }

    private GenericIssue getGenericIssue(Issue redmineIssue, List<User> users,
                                         List<IssueStatus> statuses) {
        GenericIssue issue = new GenericIssue();
        issue.setIssueKey(String.valueOf(redmineIssue.getId()));
        issue.setDescription(redmineIssue.getDescription());
        issue.setAssignee(getUserById(redmineIssue.getAssignee().getId(), users));
        issue.setReporter(getUserById(redmineIssue.getAuthor().getId(), users));
        issue.setStatus(getIssueStatusFromId(redmineIssue.getStatusId(), statuses));
        issue.setSummary(redmineIssue.getSubject());
        issue.setType(redmineIssue.getTracker().getName());
        issue.setUrl(getConfiguration().getFirstProperty(REDMINE_URL) + "/" + REDMINE_ISSUES + "/" + redmineIssue.getId());
        return issue;
    }

    private String getUserById(Integer assigneeId, List<User> users) {
        for (User user : users) {
            if (user.getId().equals(assigneeId)) {
                return user.getLogin();
            }
        }
        return null;
    }

    private Issue getRedmineIssue(GenericIssue issue) throws IssueTrackerException {
        Issue redmineIssue = new Issue();
        try {
            redmineIssue.setId(Integer.parseInt(issue.getIssueKey()));
            redmineIssue.setDescription(issue.getDescription());
            redmineIssue.setTracker(getTrackerByName(issue.getType()));
            redmineIssue.setAssignee(getUserByLogin(issue.getAssignee()));
            IssueStatus status = getIssueStatusByName(issue.getStatus());
            redmineIssue.setStatusId(status.getId());
            redmineIssue.setStatusName(status.getName());
            redmineIssue.setSubject(issue.getSummary());
            redmineIssue.setAuthor(getUserByLogin(issue.getReporter()));
        } catch (RedmineException e) {
            String msg = "Error while converting issue from " + issue.getIssueKey() + " to Redmine issue";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return redmineIssue;
    }

    private IssueStatus getIssueStatusByName(String statusName) throws IssueTrackerException {
        try {
            List<IssueStatus> statuses = manager.getStatuses();
            for (IssueStatus status : statuses) {
                if (status.getName().equals(statusName)) {
                    return status;
                }
            }
        } catch (RedmineException e) {
            String msg = "Error while getting Redmine issue status for " + statusName;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return null;
    }

    private String getIssueStatusFromId(Integer statusId, List<IssueStatus> statuses) {
        for (IssueStatus status : statuses) {
            if (status.getId().equals(statusId)) {
                return status.getName();
            }
        }
        return null;
    }

    @Override
    public boolean createProject(Project project) throws IssueTrackerException {
        com.taskadapter.redmineapi.bean.Project redmineProject = new com.taskadapter.redmineapi.bean.Project();
        redmineProject.setName(project.getName());
        redmineProject.setIdentifier(project.getKey());
        redmineProject.setDescription(project.getDescription());
        try {
            manager.createProject(redmineProject);
            return true;
        } catch (RedmineException e) {
            String msg = "Error while creating project in Redmine " + project.getName();
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
    }

    @Override
    public boolean addUserToProject(GenericUser user, Project project)
            throws IssueTrackerException {
        //TODO:differentiate invite user and update user role
        Membership membership = new Membership();
        com.taskadapter.redmineapi.bean.Project redmineProject = getProjectByKey(project.getKey());
        // even though project is created successfully, project is not retrieved.
        // As a workaround, try to add the project again. This will cause error but when we try to
        // retrieve project again, it succeeds.
        if (redmineProject == null) {
            try {
                this.createProject(project);
            } catch (Exception e) {
                //ignore
            }
            redmineProject = getProjectByKey(project.getKey());
            log.debug("Redmine project retrieved for adding user is " + redmineProject);
        }
        membership.setProject(redmineProject);
        User redmineUser;
        try {
            redmineUser = getUserByLogin(user.getUsername());
            if (redmineUser == null) {
                UserInfoBean userInfoBean = ServiceContainer.getApplicationManagementService().getUserInfoBean(user.getUsername());
                activateUser(userInfoBean);
                redmineUser = getUserByLogin(user.getUsername());
            }
            membership.setUser(redmineUser);
            List<Role> roles = new ArrayList<Role>();
            String[] appFactoryRoles = user.getRoles();
            /*  key:appFactoryRoles role value:redmineUser role*/
            Map<String, String> roleMap = getRoleMap(getConfiguration());
            List<Role> availableRedmineRoles = manager.getRoles();
            String defaultRole = getConfiguration().getFirstProperty(REDMINE_DEFAULT_ROLE);

            for (String appFactoryRole : appFactoryRoles) {
                String redmineRole = roleMap.get(appFactoryRole);
                if (redmineRole != null) {
                    roles.add(getRedmineRoleByName(redmineRole, availableRedmineRoles));
                } else if (defaultRole != null) {
                    roles.add(getRedmineRoleByName(defaultRole, availableRedmineRoles));
                } else {
                    String msg = "Define proper role mapping or default role";
                    log.error(msg);
                    throw new IssueTrackerException(msg);

                }
            }
            membership.setRoles(roles);
            manager.addMembership(membership);
        } catch (RedmineException e) {
            String msg = "Error while adding a user " + user.getUsername() + " to project " + project.getName();
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        } catch (ApplicationManagementException e) {
            String msg = "Error while getting user information of " + user.getUsername();
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return false;
    }

    private Role getRedmineRoleByName(String redmineRole, List<Role> availableRedmineRoles) {
        for (Role role : availableRedmineRoles) {
            if (redmineRole.equals(role.getName())) {
                return role;
            }
        }
        return null;
    }

    private Map<String, String> getRoleMap(AppFactoryConfiguration configuration) {
        /*  key:appFactoryRoles role value:Redmine  role*/
        Map<String, String> roleMap = new HashMap<String, String>();
        String roles[] = configuration.getProperties("IssueTrackerConnector.redmine.RoleMap.Role");
        for (String role : roles) {
            roleMap.put(role, configuration.getFirstProperty("IssueTrackerConnector.redmine.RoleMap.Role." + role + ".RedmineRole"));
        }
        return roleMap;
    }

    private boolean activateUser(UserInfoBean user) throws IssueTrackerException {
        int authenticatorId = Integer.parseInt(getConfiguration().getFirstProperty(REDMINE_AUTHENTICATOR_ID));
        return RedmineUserUtil.addUser(user, authenticatorId);
    }

    private com.taskadapter.redmineapi.bean.Project getProjectByKey(String key)
            throws IssueTrackerException {
        try {
            List<com.taskadapter.redmineapi.bean.Project> projects = manager.getProjects();
            for (com.taskadapter.redmineapi.bean.Project project : projects) {
                if (project.getIdentifier().equals(key)) {
                    return project;
                }
            }
        } catch (RedmineException e) {
            String msg = "Error while getting Redmine project " + key;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }

        return null;
    }

    @Override
    public ProjectApplicationMapping getProjectApplicationMapping() {
        return new ProjectApplicationMapping() {
            @Override
            public String getProjectKey(String applicationKey) {
                return applicationKey.toLowerCase();
            }

            @Override
            public String getApplicationKey(String projectKey) {
                return projectKey;
            }
        };
    }

    @Override
    public String[] getIssueStatuses() throws IssueTrackerException {
        List<String> statuses = new ArrayList<String>();
        try {
            for (IssueStatus status : manager.getStatuses()) {
                statuses.add(status.getName());
            }
        } catch (RedmineException e) {
            String msg = "Error while getting all Redmine issue statuses";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return statuses.toArray(new String[statuses.size()]);
    }

    @Override
    public GenericIssueType[] getIssueTypes() throws IssueTrackerException {
        List<GenericIssueType> types = new ArrayList<GenericIssueType>();
        GenericIssueType type;
        try {
            for (Tracker tracker : manager.getTrackers()) {
                type = new GenericIssueType();
                type.setIssueType(tracker.getName());
                types.add(type);
            }

        } catch (RedmineException e) {
            String msg = "Error while getting all Redmine issue types";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return types.toArray(new GenericIssueType[types.size()]);
    }

    public Tracker getTrackerByName(String name) throws IssueTrackerException {

        try {
            for (Tracker tracker : manager.getTrackers()) {
                if (tracker.getName().equals(name)) {
                    return tracker;
                }
            }
        } catch (RedmineException e) {
            String msg = "Error while getting all Redmine Tracker for  issue statuses " + name;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return null;
    }

    @Override
    public GenericIssue getIssueByKey(String key, String projectID) throws IssueTrackerException {
        Integer id;
        Issue redmineIssue;
        List<User> users;
        List<IssueStatus> statuses;
        try {
            id = Integer.parseInt(key);
        } catch (NumberFormatException ex) {
            String msg = "Invalid key is provided " + key;
            log.error(msg, ex);
            throw new IssueTrackerException(msg, ex);
        }
        try {
            redmineIssue = manager.getIssueById(id, RedmineManager.INCLUDE.changesets);
            users = manager.getUsers();
            statuses = manager.getStatuses();
        } catch (RedmineException e) {
            String msg = "Error while getting issue details of " + key + " for " + projectID;
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return getGenericIssue(redmineIssue, users, statuses);
    }

    @Override
    public String[] getAvailableAssignees(String projectID) throws IssueTrackerException {
        List<String> users = new ArrayList<String>();
        List<User> redmineUsers;
        try {
            List<Membership> membershipList = manager.getMemberships(projectID);
            redmineUsers = manager.getUsers();
            for (Membership membership : membershipList) {
                users.add(getUserById(membership.getUser().getId(), redmineUsers));
            }
        } catch (RedmineException e) {
            String msg = "Error while getting all available Redmine issue assignees";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }
        return users.toArray(new String[users.size()]);
    }

    @Override
    public void createVersionInProject(Project project, Version version)
            throws IssueTrackerException {
        com.taskadapter.redmineapi.bean.Version redmineVersion = new com.taskadapter.redmineapi.bean.Version();
        redmineVersion.setName(version.getName());
        try {
            redmineVersion.setProject(getProjectByKey(project.getKey()));
            manager.createVersion(redmineVersion);
        } catch (IssueTrackerException e) {
            String msg = "Error while getting Redmine project by name";
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        } catch (RedmineException e) {
            String msg = "Error while creating a Redmine project version for " + project.getKey();
            log.error(msg, e);
            throw new IssueTrackerException(msg, e);
        }

    }

    @Override
    public String getUrlForReportIssue(String project) throws IssueTrackerException {
        return (getConfiguration().getFirstProperty(REDMINE_URL) + "/projects/" + project);
    }

    public void close() {
        manager.shutdown();
    }
}
