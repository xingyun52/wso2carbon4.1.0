/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.appfactory.jenkins.build;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;

public class RestBasedJenkinsCIConnector {

    private static final Log log = LogFactory.getLog(RestBasedJenkinsCIConnector.class);

    private HttpClient httpClient;

    private String jenkinsUrl;

    private boolean authenticate;

    public RestBasedJenkinsCIConnector(String jenkinsUrl, boolean authenticate, String userName,
                                       String apiKeyOrpassword) {
        this.httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
        this.jenkinsUrl = jenkinsUrl;
        if (StringUtils.isBlank(this.jenkinsUrl)) {
            throw new IllegalArgumentException("Jenkins server url is unspecified");
        }

        this.authenticate = authenticate;
        if (this.authenticate) {
            httpClient.getState().setCredentials(AuthScope.ANY,
                                                 new UsernamePasswordCredentials(userName,
                                                                                 apiKeyOrpassword));
            httpClient.getParams().setAuthenticationPreemptive(true);
        }
    }

    public HttpClient getHttpClient() {

        return httpClient;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    public void createRole(String roleName, String pattern) throws AppFactoryException {
        String createRoleUrl =
                               "/descriptorByName/com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy/createProjectRoleSubmit";

        NameValuePair[] parameters =
                                     new NameValuePair[] {
                                                          new NameValuePair("name", roleName),
                                                          new NameValuePair("pattern", pattern),
                                                          new NameValuePair("permission",
                                                                            "hudson.model.Item.Build"),
                                                          new NameValuePair("permission",
                                                                            "hudson.model.Item.Configure"),
                                                          new NameValuePair("permission",
                                                                            "hudson.model.Item.Delete"),
                                                          new NameValuePair("permission",
                                                                            "hudson.model.Item.Read"),
                                                          new NameValuePair("permission",
                                                                            "hudson.model.Item.Workspace") };

        PostMethod addRoleMethod = createPost(createRoleUrl, parameters, null);

        try {
            int httpStatusCode = getHttpClient().executeMethod(addRoleMethod);
            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg =
                                  String.format("Unable to create the role. jenkins returned, http status : %d",
                                                httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }
        } catch (Exception ex) {
            String errorMsg =
                              String.format("Unable to create role in jenkins : %s",
                                            ex.getMessage());
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            addRoleMethod.releaseConnection();
        }

    }

    public void assignUsers(String[] userIds, String[] projectRoleNames, String[] globalRoleNames) throws AppFactoryException{

        String assignURL =
                           "/descriptorByName/com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy/assignRolesSubmit";

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        for (String id : userIds) {
            params.add(new NameValuePair("sid", id));
        }

        if (projectRoleNames != null) {

            for (String role : projectRoleNames) {
                params.add(new NameValuePair("projectRole", role));
            }

        }

        if (globalRoleNames != null) {
            for (String role : globalRoleNames) {
                params.add(new NameValuePair("globalRole", role));
            }
        }

        PostMethod assignRolesMethod =
                                       createPost(assignURL,
                                                  params.toArray(new NameValuePair[params.size()]),
                                                  null);

        try {
            int httpStatusCode = getHttpClient().executeMethod(assignRolesMethod);
            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg =
                                  String.format("Unable to assign roles to given sides. jenkins returned, http status : %d",
                                                httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }
        } catch (Exception ex) {
            String errorMsg =
                              String.format("Unable to assign roles in jenkins : %s",
                                            ex.getMessage());
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            assignRolesMethod.releaseConnection();
        }

    }

    public List<String> getAllJobs() throws AppFactoryException {

        List<String> jobNames = null;

        GetMethod getAllJobsMethod = createGet("/view/All/api/xml", null);
        try {
            int httpStatusCode = getHttpClient().executeMethod(getAllJobsMethod);

            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg =
                                  String.format("Unable to retrieve available jobs from jenkins. jenkins returned, http status : %d",
                                                httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

            StAXOMBuilder builder = new StAXOMBuilder(getAllJobsMethod.getResponseBodyAsStream());
            AXIOMXPath xpath = new AXIOMXPath("/*/job/name/text()");
            List<Object> jobNameElements = xpath.selectNodes(builder.getDocumentElement());

            jobNames = new ArrayList<String>(jobNameElements.size());

            for (Object node : jobNameElements) {

                if (node instanceof OMText) {
                    OMText element = (OMText) node;
                    jobNames.add(element.getText());
                }

            }
        } catch (Exception ex) {
            String errorMsg =
                              String.format("Unable to retrieve available jobs from jenkins : %s",
                                            ex.getMessage());
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            getAllJobsMethod.releaseConnection();
        }
        return jobNames;
    }

    public void createJob(String jobName, Map<String, String> jobParams) throws AppFactoryException {

        OMElement jobConfiguration = new JobConfigurator(jobParams).configure();
        NameValuePair[] queryParams = { new NameValuePair("name", jobName) };
        PostMethod createJob = null;
        boolean jobCreatedFlag = false;

        try {
            createJob =
                        createPost("/createItem", queryParams,
                                   new StringRequestEntity(jobConfiguration.toStringWithConsume(),
                                                           "text/xml", "utf-8"));
            int httpStatusCode = getHttpClient().executeMethod(createJob);

            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg =
                                  String.format("Unable to create the job: [%s]. jenkins returned, http status : %d",
                                                jobName, httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            } else {
                jobCreatedFlag = true;
            }

            setSvnCredentials(jobName, jobParams.get(JenkinsCIConstants.SVN_CREDENTIALS_USERNAME),
                              jobParams.get(JenkinsCIConstants.SVN_CREDENTIALS_PASSWORD),
                              jobParams.get(JenkinsCIConstants.SVN_REPOSITORY));

        } catch (Exception ex) {
            String errorMsg = String.format("Error while trying creating job: ", jobName);
            log.error(errorMsg, ex);

            if (jobCreatedFlag == true) {
                // the job was created but setting svn
                // credentials failed. Therefore try
                // deleting the entire job (instead of
                // keeping a unusable job in jenkins)
                try {
                    deleteJob(jobName);
                } catch (AppFactoryException delExpception) {
                    log.error("Unable to delete the job after failed attempt set svn credentials, job: " +
                                      jobName, delExpception);
                }
            }

            throw new AppFactoryException(errorMsg, ex);
        } finally {

            if (createJob != null) {
                createJob.releaseConnection();
            }
        }
    }

    public boolean isJobExists(String jobName) throws AppFactoryException {

        final String wrapperTag = "JobNames";
        NameValuePair[] queryParameters =
                                          {
                                           new NameValuePair("wrapper", wrapperTag),
                                           new NameValuePair(
                                                             "xpath",
                                                             String.format("/*/job/name[text()='%s']",
                                                                           jobName)) };

        GetMethod checkJobExistsMethod = createGet("/api/xml", queryParameters);

        boolean isExists = false;

        try {
            checkJobExistsMethod.setQueryString(queryParameters);
            int httpStatusCode = getHttpClient().executeMethod(checkJobExistsMethod);

            if (HttpStatus.SC_OK != httpStatusCode) {
                final String errorMsg =
                                        String.format("Unable to check the existance of job: [%s]. jenkins returned, http status : %d",
                                                      jobName, httpStatusCode);

                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

            StAXOMBuilder builder =
                                    new StAXOMBuilder(
                                                      checkJobExistsMethod.getResponseBodyAsStream());
            isExists = builder.getDocumentElement().getChildElements().hasNext();
        } catch (Exception ex) {
            String errorMsg = String.format("Error while checking the existance of job: ", jobName);
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            checkJobExistsMethod.releaseConnection();
        }

        return isExists;
    }

    public boolean deleteJob(String jobName) throws AppFactoryException {
        PostMethod deleteJobMethod =
                                     createPost(String.format("/job/%s/doDelete", jobName), null,
                                                null);
        int httpStatusCode = -1;
        try {
            httpStatusCode = getHttpClient().executeMethod(deleteJobMethod);

            if (HttpStatus.SC_FORBIDDEN == httpStatusCode) {
                final String errorMsg =
                                        String.format("Unable to delete: [%s]. jenkins returned, http status : %d",
                                                      jobName, httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

        } catch (Exception ex) {
            String errorMsg = String.format("Error while deleting the job: ", jobName);
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            deleteJobMethod.releaseConnection();
        }
        return HttpStatus.SC_NOT_FOUND != httpStatusCode;

    }

    public void startBuild(String jobName) throws AppFactoryException {
        PostMethod startBuildMethod =
                                      createPost(String.format("/job/%s/build", jobName), null,
                                                 null);

        int httpStatusCode = -1;
        try {
            httpStatusCode = getHttpClient().executeMethod(startBuildMethod);

        } catch (Exception ex) {
            String errorMsg =
                              String.format("Unable to delete start the build on job : %s", jobName);
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            startBuildMethod.releaseConnection();
        }

        if (HttpStatus.SC_FORBIDDEN == httpStatusCode) {
            final String errorMsg =
                                    new StringBuilder("Unable to start a build for job [").append(jobName)
                                                                                          .append("] due to invalid credentials.")
                                                                                          .append("Jenkins returned, http status : [")
                                                                                          .append(httpStatusCode)
                                                                                          .append("]")
                                                                                          .toString();
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg);
        }

        if (HttpStatus.SC_NOT_FOUND == httpStatusCode) {
            final String errorMsg =
                                    new StringBuilder("Unable to find the job [").append(jobName)
                                                                                 .append("Jenkins returned, http status : [")
                                                                                 .append(httpStatusCode)
                                                                                 .append("]")
                                                                                 .toString();
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg);
        }

    }

    public void logout() throws AppFactoryException {
        GetMethod logoutMethod = createGet("/logout", null);
        try {
            getHttpClient().executeMethod(logoutMethod);
        } catch (Exception ex) {
            String errorMsg = "Unable to login from jenkins";
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg, ex);

        } finally {
            logoutMethod.releaseConnection();
        }

    }

    private void setSvnCredentials(String jobName, String userName, String password, String svnRepo)
                                                                                                    throws AppFactoryException {
        final String setCredentialsURL =
                                         String.format("/job/%s/descriptorByName/hudson.scm.SubversionSCM/postCredential",
                                                       jobName);

        PostMethod setCredentialsMethod = createPost(setCredentialsURL, null, null);

        Part[] parts =
                       { new StringPart("url", svnRepo), new StringPart("kind", "password"),
                        new StringPart("username1", userName),
                        new StringPart("password1", password), };
        setCredentialsMethod.setRequestEntity(new MultipartRequestEntity(
                                                                         parts,
                                                                         setCredentialsMethod.getParams()));

        final String redirectedURlFragment =
                                             String.format("/job/%s/descriptorByName/hudson.scm.SubversionSCM/credentialOK",
                                                           jobName);

        try {
            int httpStatus = getHttpClient().executeMethod(setCredentialsMethod);
            Header locationHeader = setCredentialsMethod.getResponseHeader("Location");

            // if operation completed successfully Jenkins returns http 302,
            // which location header ending with '..../credentialOK'

            if (HttpStatus.SC_MOVED_TEMPORARILY != httpStatus ||
                (locationHeader != null && !StringUtils.endsWithIgnoreCase(StringUtils.trimToEmpty(locationHeader.getValue()),
                                                                           redirectedURlFragment))) {

                String errorMsg =
                                  "Unable to set svn credentials for the new job: jenkins returned - Https status " +
                                          httpStatus + " ,Location header " + locationHeader;
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

        } catch (IOException e) {
            String errorMsg =
                              String.format("Unable to send svn credentials to jenkins for job: %s",
                                            jobName);
            throw new AppFactoryException(errorMsg, e);
        } finally {
            setCredentialsMethod.releaseConnection();
        }
    }

    private GetMethod createGet(String urlFragment, NameValuePair[] queryParameters) {
        GetMethod get = new GetMethod(getJenkinsUrl() + urlFragment);
        if (authenticate) {
            get.setDoAuthentication(true);
        }
        if (queryParameters != null) {
            get.setQueryString(queryParameters);
        }
        return get;
    }

    private PostMethod createPost(String urlFragment, NameValuePair[] queryParameters,
                                  RequestEntity requestEntity) {
        PostMethod post = new PostMethod(getJenkinsUrl() + urlFragment);
        if (authenticate) {
            post.setDoAuthentication(true);
        }

        if (queryParameters != null) {
            post.setQueryString(queryParameters);
        }

        if (requestEntity != null) {
            post.setRequestEntity(requestEntity);
        }

        return post;
    }

}
