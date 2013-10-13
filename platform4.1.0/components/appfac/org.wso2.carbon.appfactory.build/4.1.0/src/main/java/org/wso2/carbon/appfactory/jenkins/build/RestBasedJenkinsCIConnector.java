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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AXIOMUtil;
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
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.dto.Statistic;
import org.wso2.carbon.appfactory.jenkins.build.internal.ServiceContainer;

/**
 * Connects to a jenkins server using its 'Remote API'.
 */
public class RestBasedJenkinsCIConnector {

    private static final Log log = LogFactory.getLog(RestBasedJenkinsCIConnector.class);

    /**
     * The http client used to connect jenkins.
     */
    private HttpClient httpClient;

    /**
     * Base url of the jenkins
     */
    private String jenkinsUrl;

    /**
     * Flag weather this connector needs to authenticate it self.
     */
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

    @SuppressWarnings("UnusedDeclaration")
    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }

    /**
     * Creates a project/job role in jenkins server
     * <p>
     * <b>NOTE: this method assumes a modified version (by WSO2) of
     * 'role-strategy' plugin is installed in jenkins server</b>
     * </p>
     *
     * @param roleName Name of the role.
     * @param pattern  a regular expression to match jobs (e.g. app1.*)
     * @throws AppFactoryException if an error occurs
     */
    public void createRole(String roleName, String pattern,
                           String permissions []) throws AppFactoryException {
        String createRoleUrl = "/descriptorByName/com.michelin.cio.hudson.plugins.rolestrategy" +
                               ".RoleBasedAuthorizationStrategy/createProjectRoleSubmit";
        ArrayList<NameValuePair> parameters=new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("name", roleName));
        parameters.add( new NameValuePair("pattern", pattern));
        for (String  permission:permissions){
            parameters.add(new NameValuePair("permission",permission));
        }

        PostMethod addRoleMethod = createPost(createRoleUrl,
                                              parameters.toArray(new NameValuePair[0]), null);

        try {
            int httpStatusCode = getHttpClient().executeMethod(addRoleMethod);
            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = String.format("Unable to create the role. jenkins returned, " +
                                                "http status : %d",
                                httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }
        } catch (Exception ex) {
            String errorMsg = String.format("Unable to create role in jenkins : %s",
                            ex.getMessage());
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            addRoleMethod.releaseConnection();
        }

    }

    /**
     * Assigns a set of global and/or project roles(s) to a specified user(s)
     * <p>
     * <b>NOTE: this method assumes a modified version (by WSO2) of
     * 'role-strategy' plugin is installed in jenkins server</b>
     * </p>
     *
     * @param userIds          list of user Ids
     * @param projectRoleNames list of project roles
     * @param globalRoleNames  list of global roles
     * @throws AppFactoryException if an error occurs
     */
    public void assignUsers(String[] userIds, String[] projectRoleNames, String[] globalRoleNames)
            throws AppFactoryException {

        String assignURL = "/descriptorByName/com.michelin.cio.hudson.plugins.rolestrategy" +
                           ".RoleBasedAuthorizationStrategy/assignRolesSubmit";

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
                String errorMsg = String.format("Unable to assign roles to given sides. jenkins " +
                                                "returned, http status : %d",
                                httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }
        } catch (Exception ex) {
            String errorMsg = String.format("Unable to assign roles in jenkins : %s",
                            ex.getMessage());
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            assignRolesMethod.releaseConnection();
        }

    }

    /**
     * Returns all the jobs defined in jenkins server.
     *
     * @return list of job names
     * @throws AppFactoryException if an error occurs
     */
    public List<String> getAllJobs() throws AppFactoryException {
        return getJobNames(null);
    }

    /**
     * Returns a list of job names which contains given text as a substring. If
     * the filter text is not specified (i.e. null) then all the jobs will be
     * returned.
     *
     * @param filterText (specifying null return names of all jobs available)
     *                   text to match
     * @return {@link List} of Job names (in jenkins CI)
     * @throws AppFactoryException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public List<String> getJobNames(String filterText) throws AppFactoryException {
        List<String> jobNames = new ArrayList<String>();

        final String wrapperTag = "JobNames";

        final String xpathExpression =
                StringUtils.isNotEmpty(filterText)
                        ? String.format("/*/job/name[contains(., '%s')]",
                        filterText)
                        : "/*/job/name";

        NameValuePair[] queryParameters =
                {new NameValuePair("wrapper", wrapperTag),
                        new NameValuePair("xpath", xpathExpression)};

        GetMethod getJobsMethod = createGet("/view/All/api/xml", queryParameters);
        try {
            int httpStatusCode = getHttpClient().executeMethod(getJobsMethod);

            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = String.format("Unable to retrieve job names: filter text :%s, " +
                                                "jenkins returned, http status : %d",
                                filterText, httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

            StAXOMBuilder builder = new StAXOMBuilder(getJobsMethod.getResponseBodyAsStream());

            Iterator<OMElement> jobNameElements = builder.getDocumentElement().getChildElements();

            while (jobNameElements.hasNext()) {
                OMElement jobName = jobNameElements.next();
                jobNames.add(jobName.getText());
            }
        } catch (Exception ex) {
            String errorMsg = String.format("Unable to retrieve available jobs from jenkins : %s",
                            ex.getMessage());
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            getJobsMethod.releaseConnection();
        }
        return jobNames;

    }

    public File getArtifact(String jobName, String artifactName) throws AppFactoryException {
        File file = null;
        String url = "/job/" + jobName + "/ws/" + artifactName;
        GetMethod getArtifactMethod = createGet(url, null);
        try {
            int httpStatusCode = getHttpClient().executeMethod(getArtifactMethod);

            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = String.format("Unable to retrieve artifact from jenkins. " +
                                                "jenkins returned, http status : %d",
                                httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

            String carbonHome = System.getProperty("carbon.home"); //TODO find the constnat

            String fileName = artifactName.substring(artifactName.lastIndexOf("/") + 1);

            InputStream ins = getArtifactMethod.getResponseBodyAsStream();
            @SuppressWarnings("UnusedAssignment")
            int read = 0;
            byte[] bytes = new byte[1024];
            file = new File(carbonHome + "/tmp/" + fileName);
            FileOutputStream out = new FileOutputStream(file);
            while ((read = ins.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            ins.close();
        } catch (Exception ex) {
            String errorMsg = String.format("Unable to retrieve available jobs from jenkins : %s",
                            ex.getMessage());
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            getArtifactMethod.releaseConnection();
        }
        return file;
    }

    /**
     * Create a job in Jenkins
     *
     * @param jobName   name of the job
     * @param jobParams Job configuration parameters
     * @throws AppFactoryException if an error occures.
     */
    public void createJob(String jobName, Map<String, String> jobParams) throws AppFactoryException {

        OMElement jobConfiguration = new JobConfigurator(jobParams).configure();
        NameValuePair[] queryParams = {new NameValuePair("name", jobName)};
        PostMethod createJob = null;
        boolean jobCreatedFlag = false;

        try {
            createJob =
                    createPost("/createItem", queryParams,
                            new StringRequestEntity(jobConfiguration.toStringWithConsume(),
                                    "text/xml", "utf-8"));
            int httpStatusCode = getHttpClient().executeMethod(createJob);

            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = String.format("Unable to create the job: [%s]. jenkins " +
                                                "returned, http status : %d",
                                jobName, httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            } else {
                jobCreatedFlag = true;
            }
            if ("svn".equals(jobParams.get(JenkinsCIConstants.REPOSITORY_TYPE))) {
                setSvnCredentials(jobName,
                                  jobParams.get(JenkinsCIConstants.
                                                        REPOSITORY_ACCESS_CREDENTIALS_USERNAME),
                                  jobParams.get(JenkinsCIConstants.
                                                        REPOSITORY_ACCESS_CREDENTIALS_PASSWORD),
                                  jobParams.get(JenkinsCIConstants.REPOSITORY_URL));
            }

        } catch (Exception ex) {
            String errorMsg = "Error while trying creating job: " +jobName;
            log.error(errorMsg, ex);

            if (jobCreatedFlag) {
                // the job was created but setting svn
                // credentials failed. Therefore try
                // deleting the entire job (instead of
                // keeping a unusable job in jenkins)
                try {
                    deleteJob(jobName);
                } catch (AppFactoryException delExpception) {
                    log.error("Unable to delete the job after failed attempt set svn credentials," +
                              " job: " +jobName, delExpception);
                }
            }

            throw new AppFactoryException(errorMsg, ex);
        } finally {

            if (createJob != null) {
                createJob.releaseConnection();
            }
        }
    }

    /**
     * Checks weather a job exists in Jenkins server
     *
     * @param jobName name of the job.
     * @return true if job exits, false otherwise.
     * @throws AppFactoryException if an error occurs.
     */
    public boolean isJobExists(String jobName) throws AppFactoryException {

        final String wrapperTag = "JobNames";
        NameValuePair[] queryParameters =
                {
                        new NameValuePair("wrapper", wrapperTag),
                        new NameValuePair(
                                "xpath",
                                String.format("/*/job/name[text()='%s']", jobName))};

        GetMethod checkJobExistsMethod = createGet("/api/xml", queryParameters);

        boolean isExists = false;

        try {
            checkJobExistsMethod.setQueryString(queryParameters);
            int httpStatusCode = getHttpClient().executeMethod(checkJobExistsMethod);

            if (HttpStatus.SC_OK != httpStatusCode) {
                final String errorMsg = String.format("Unable to check the existance of job: [%s]" +
                                                      ". jenkins returned, http status : %d",
                                jobName, httpStatusCode);

                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

            StAXOMBuilder builder =
                    new StAXOMBuilder(
                            checkJobExistsMethod.getResponseBodyAsStream());
            isExists = builder.getDocumentElement().getChildElements().hasNext();
        } catch (Exception ex) {
            String errorMsg = "Error while checking the existance of job: " + jobName;
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            checkJobExistsMethod.releaseConnection();
        }

        return isExists;
    }

    /**
     * Deletes a job
     *
     * @param jobName name of the job
     * @return true if job exited on Jenkins and successfully deleted.
     * @throws AppFactoryException if an error occures.
     */
    public boolean deleteJob(String jobName) throws AppFactoryException {
        PostMethod deleteJobMethod =
                createPost(String.format("/job/%s/doDelete", jobName), null,
                        null);
        int httpStatusCode = -1;
        try {
            httpStatusCode = getHttpClient().executeMethod(deleteJobMethod);

            if (HttpStatus.SC_FORBIDDEN == httpStatusCode) {
                final String errorMsg = String.format("Unable to delete: [%s]. jenkins returned, " +
                                                      "http status : %d", jobName, httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

        } catch (Exception ex) {
            String errorMsg = "Error while deleting the job: " + jobName;
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            deleteJobMethod.releaseConnection();
        }
        return HttpStatus.SC_NOT_FOUND != httpStatusCode;

    }

    /**
     * Starts a build job available in Jenkins
     *
     * @param jobName Name of the job
     * @throws AppFactoryException if an error occurs.
     */
    public void startBuild(String jobName, boolean doDeploy, String stageName, String tagName) throws AppFactoryException {

        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("isAutomatic","false"));
        parameters.add(new NameValuePair("doDeploy", Boolean.toString(doDeploy)));
        parameters.add(new NameValuePair("deployStage", stageName));

        // TODO should get the persistArtifact parameter value from the user and set here
        if(tagName != null && !tagName.equals("")){
            parameters.add(new NameValuePair("persistArtifact", String.valueOf(true)));
            parameters.add(new NameValuePair("tagName", tagName));
        } else {
            parameters.add(new NameValuePair("persistArtifact", String.valueOf(false)));
        }

        PostMethod startBuildMethod = createPost(String.format("/job/%s/buildWithParameters", jobName),
                                                  parameters.toArray(new NameValuePair[parameters.size()]), null);

        int httpStatusCode = -1;
        try {
            httpStatusCode = getHttpClient().executeMethod(startBuildMethod);

        } catch (Exception ex) {
            String errorMsg = String.format("Unable to delete start the build on job : %s",
                                            jobName);
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            startBuildMethod.releaseConnection();
        }

        if (HttpStatus.SC_FORBIDDEN == httpStatusCode) {
            final String errorMsg = "Unable to start a build for job [".concat(jobName)
                    .concat("] due to invalid credentials.")
                    .concat("Jenkins returned, http status : [")
                    .concat(String.valueOf(httpStatusCode))
                    .concat("]");
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg);
        }

        if (HttpStatus.SC_NOT_FOUND == httpStatusCode) {
            final String errorMsg = "Unable to find the job [" + jobName + "Jenkins returned, " +
                                    "http status : [" + httpStatusCode + "]";
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg);
        }

    }

    /**
     * Logs out of the jenkins server
     *
     * @throws AppFactoryException if an error occurs
     */
    @SuppressWarnings("UnusedDeclaration")
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


    public String getbuildStatus(String buildUrl) throws AppFactoryException {

        String buildStatus = "Unknown";

        NameValuePair[] queryParameters = {new NameValuePair("xpath", "/*/result")};

        GetMethod checkJobExistsMethod =
                createGet(buildUrl, "api/xml",
                        queryParameters);

        try {
            int httpStatusCode = getHttpClient().executeMethod(checkJobExistsMethod);

            if (HttpStatus.SC_OK != httpStatusCode) {
                final String errorMsg = String.format("Unable to check the status  of build: [%s]" +
                                                      ". jenkins returned, http status : %d",
                                buildUrl, httpStatusCode);

                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

            StAXOMBuilder builder =
                    new StAXOMBuilder(
                            checkJobExistsMethod.getResponseBodyAsStream());
            OMElement resultElement = builder.getDocumentElement();
            if (resultElement != null) {
                buildStatus = resultElement.getText();

            }

        } catch (Exception ex) {
            String errorMsg = "Error while checking the status of build: " + buildUrl;
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            checkJobExistsMethod.releaseConnection();
        }

        return buildStatus;
    }

    public List<String> getBuildUrls(String jobName) throws AppFactoryException {

        List<String> listOfUrls = new ArrayList<String>();

        final String wrapperTag = "Builds";
        NameValuePair[] queryParameters =
                {new NameValuePair("wrapper", wrapperTag),
                        new NameValuePair("xpath", "/*/build/url")};

        GetMethod getBuildsMethod =
                createGet(String.format("/job/%s/api/xml", jobName),
                        queryParameters);
        try {
            int httpStatusCode = getHttpClient().executeMethod(getBuildsMethod);

            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = String.format("Unable to retrieve available build urls from " +
                                                "jenkins for job %s. jenkins returned," +
                                                " http status : %d",
                                jobName, httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

            StAXOMBuilder builder = new StAXOMBuilder(getBuildsMethod.getResponseBodyAsStream());
            @SuppressWarnings("unchecked")
            Iterator<OMElement> urlElementsIte = builder.getDocumentElement().getChildElements();
            while (urlElementsIte.hasNext()) {
                OMElement urlElement = urlElementsIte.next();
                listOfUrls.add(urlElement.getText());
            }

        } catch (Exception ex) {
            String errorMsg = String.format("Unable to retrieve available jobs from jenkins : %s",
                            ex.getMessage());
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            getBuildsMethod.releaseConnection();
        }
        return listOfUrls;
    }

    public List<Statistic> getOverallLoad() throws AppFactoryException {

        GetMethod overallLoad = createGet("/overallLoad/api/xml", null);

        List<Statistic> list = new ArrayList<Statistic>();

        try {

            int httpStatusCode = getHttpClient().executeMethod(overallLoad);

            if (HttpStatus.SC_OK != httpStatusCode) {
                final String errorMsg = String.format("Unable to check the overal load of jenkins" +
                                                      ". jenkins returned, http status : %d",
                                httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

            StAXOMBuilder builder = new StAXOMBuilder(overallLoad.getResponseBodyAsStream());
            @SuppressWarnings("unchecked")
            Iterator<OMElement> elementIterator =
                    (Iterator<OMElement>) builder.getDocumentElement()
                            .getChildElements();

            while (elementIterator.hasNext()) {

                OMElement statElement = elementIterator.next();
                String value =
                        StringUtils.isEmpty(statElement.getText()) ? "-1"
                                : statElement.getText();

                Statistic stat = new Statistic(statElement.getLocalName(), value);
                list.add(stat);
            }

        } catch (Exception ex) {
            String errorMsg = "Error while checking the overall load of jenkins";
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            overallLoad.releaseConnection();
        }
        return list;

    }
    
    /**
     * Returns Jenkins-API information as a JSON array.
     * @param jobName       eg: applicationKey023-trunk-default
     * @param treeStructure eg: builds[number,duration,result]
     * @return  buildsInfo (Which is a JSON array with requested information
     * @throws AppFactoryException
     */
    public String getValuesForJobAsJsonTree(String jobName,String treeStructure) throws AppFactoryException {

        String buildUrl = String.format("%s/job/%s/", this.getJenkinsUrl(), jobName);
        String buildsInfo = null;

        NameValuePair[] queryParameters = {new NameValuePair("tree", treeStructure)};

        GetMethod getBuildsHistoryMethod =
                createGet(buildUrl, "api/json",
                        queryParameters);

        try {
            int httpStatusCode = getHttpClient().executeMethod(getBuildsHistoryMethod);

            if (HttpStatus.SC_OK != httpStatusCode) {
                final String errorMsg = String.format("Unable to fetch information from Jenkins for : %d",
                        getBuildsHistoryMethod.getURI(), httpStatusCode);

                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }
            buildsInfo = getBuildsHistoryMethod.getResponseBodyAsString();
        } catch (Exception ex) {
            String errorMsg = String.format("Error while fetching information tree %s for : %s",treeStructure,jobName);
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            getBuildsHistoryMethod.releaseConnection();
        }

        return buildsInfo;
    }

    /**
     * A convenient methods to pass credentials of a svn repository (specified
     * in the job)
     *
     * @param jobName  Name of job
     * @param userName svn username
     * @param password password
     * @param svnRepo  repo url
     * @throws AppFactoryException if an error occurs.
     */
    private void setSvnCredentials(String jobName, String userName, String password, String svnRepo)
            throws AppFactoryException {
        final String setCredentialsURL = String.format("/job/%s/descriptorByName/hudson.scm" +
                                                       ".SubversionSCM/postCredential", jobName);

        PostMethod setCredentialsMethod = createPost(setCredentialsURL, null, null);

        Part[] parts =
                {new StringPart("url", svnRepo), new StringPart("kind", "password"),
                        new StringPart("username1", userName),
                        new StringPart("password1", password),};
        setCredentialsMethod.setRequestEntity(new MultipartRequestEntity(
                parts,
                setCredentialsMethod.getParams()));

        final String redirectedURlFragment = String.format("/job/%s/descriptorByName/hudson.scm" +
                                                           ".SubversionSCM/credentialOK", jobName);

        try {
            int httpStatus = getHttpClient().executeMethod(setCredentialsMethod);
            Header locationHeader = setCredentialsMethod.getResponseHeader("Location");

            // if operation completed successfully Jenkins returns http 302,
            // which location header ending with '..../credentialOK'

            if (HttpStatus.SC_MOVED_TEMPORARILY != httpStatus ||
                    (locationHeader != null && !StringUtils.endsWithIgnoreCase(
                            StringUtils.trimToEmpty(locationHeader.getValue()),
                            redirectedURlFragment))) {

                String errorMsg = "Unable to set svn credentials for the new job: jenkins " +
                                  "returned - Https status " +
                                httpStatus + " ,Location header " + locationHeader;
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

        } catch (IOException e) {
            String errorMsg = String.format("Unable to send svn credentials to jenkins for job: " +
                                            "%s", jobName);
            throw new AppFactoryException(errorMsg, e);
        } finally {
            setCredentialsMethod.releaseConnection();
        }
    }

    /**
     * This method will call jenkins to deploy the latest successfully built artifact of the given job name
     * @param jobName job name of which the artifact is going to get deployed
     * @param artifactType artifact type (car/war) that is going to get deployed
     * @param stage server Urls that we need to deploy the artifact into
     * @throws AppFactoryException
     */
    public void deployLatestSuccessArtifact(String jobName, String artifactType,
                                            String stage)
            throws AppFactoryException {
        String deployLatestSuccessArtifactUrl = "/plugin/appfactory-plugin/deployLatestSuccessArtifact";

        String applicationId = getAppId(jobName);

        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("applicationId", applicationId));
        parameters.add(new NameValuePair("artifactType", artifactType));
        parameters.add(new NameValuePair("jobName", jobName));

//        for (String serviceUrl : deploymentServerUrls) {
//            parameters.add(new NameValuePair("deploymentServerUrl", serviceUrl));
//        }
        addStageSpecificParameters(stage, parameters);


        PostMethod deployLatestSuccessArtifactMethod = createPost(deployLatestSuccessArtifactUrl,
                                                                  parameters.toArray(
                                                                          new NameValuePair[
                                                                          parameters.size()]), null);

        try {
            int httpStatusCode = getHttpClient().executeMethod(deployLatestSuccessArtifactMethod);
            log.info("status code for deploy latest success artifact : " + httpStatusCode);
            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = "Unable to deploy the latest success artifact. jenkins " +
                                  "returned, http status : " + httpStatusCode;
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }
        } catch (Exception ex) {
            String errorMsg = "Unable to deploy the latest success artifact : " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            deployLatestSuccessArtifactMethod.releaseConnection();
        }
    }

    private void addStageSpecificParameters(String stage, List<NameValuePair> parameters) {
//        We send all the configuration elements in the appfactory xml to jenkins
        AppFactoryConfiguration configuration = ServiceContainer.getAppFactoryConfiguration();

        Map<String,List<String>> allProperties = configuration.getAllProperties();

        String prefix = AppFactoryConstants.DEPLOYMENT_STAGES + "." + stage;
        for (Map.Entry<String, List<String>> entry : allProperties.entrySet()) {
            if(entry.getKey().startsWith(prefix)){
                String propName = entry.getKey().replace(prefix + ".","");

                for (String value : entry.getValue()) {
                    parameters.add(new NameValuePair(propName,value));
                }
            }
        }
    }

    /**
     * This method will call jenkins to deploy the artifact with the given tag of the given job name
     * @param jobName job name of which the artifact is going to get deployed
     * @param artifactType artifact type (car/war) that is going to get deployed
     * @param tagName tag name of the artifact that needs to be deployed
     * @param stage server Urls that we need to deploy the artifact into
     * @throws AppFactoryException
     */
    public void deployTaggedArtifact(String jobName, String artifactType, String tagName,
                                     String stage) throws AppFactoryException {

        String deployTaggedArtifactUrl = "/plugin/appfactory-plugin/deployTaggedArtifact";

        String applicationId = getAppId(jobName);

        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("applicationId", applicationId));
        parameters.add(new NameValuePair("artifactType", artifactType));
        parameters.add(new NameValuePair("jobName", jobName));
        parameters.add(new NameValuePair("tagName", tagName));
//        for (String serviceUrl : deploymentServerUrls) {
//            parameters.add(new NameValuePair("deploymentServerUrl", serviceUrl));
//        }

        addStageSpecificParameters(stage,parameters);

        PostMethod deployTaggedArtifactMethod = createPost(deployTaggedArtifactUrl,
                                                           parameters.toArray(
                                                                   new NameValuePair[
                                                                   parameters.size()]), null);

        try {
            int httpStatusCode = getHttpClient().executeMethod(deployTaggedArtifactMethod);
            log.info("status code for deploy tagged artifact : " + httpStatusCode);
            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = "Unable to deploy the tagged artifact for tag name " + tagName
                                  + ". jenkins returned, http status : " + httpStatusCode;
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }
        } catch (Exception ex) {
            String errorMsg = "Unable to deploy the tagged artifact for tag name " + tagName +
                              ": " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            deployTaggedArtifactMethod.releaseConnection();
        }
    }

    /**
     * Creates the applicationId from the job name
     * @param jobName jobName
     * @return applicationId
     */
    private String getAppId(String jobName) {
        // job name : <applicationId>-<version>-default

        //removing the '-default' part
        String temp = jobName.substring(0, jobName.lastIndexOf("-"));
        //removing the app version
        String applicationId = temp.substring(0,temp.lastIndexOf("-"));
        return applicationId;
    }

    /**
     * This will return the tag names of the persisted artifact of the given job
     * @param jobName job name of which we need to get the tag names
     * @return tag names of the persisted artifacts
     * @throws AppFactoryException
     */
    public String[] getTagNamesOfPersistedArtifacts(String jobName) throws AppFactoryException {
        String getIdentifiersOfArtifactsUrl = "/plugin/appfactory-plugin/getTagNamesOfPersistedArtifacts";
        @SuppressWarnings("UnusedAssignment")
        String[] tagNamesOfPersistedArtifacts = new String[0];
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("jobName", jobName));

        PostMethod getIdsOfPersistArtifactMethod = createPost(getIdentifiersOfArtifactsUrl,
                                                              parameters.toArray(
                                                                      new NameValuePair[
                                                                      parameters.size()]), null);
        try {
            int httpStatusCode = getHttpClient().executeMethod(getIdsOfPersistArtifactMethod);
            log.info("status code for getting tag names of persisted artifacts : " + httpStatusCode);
            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = "Unable to get the tag names of persisted artifact for job " +
                                  jobName + ". jenkins returned, http status : " + httpStatusCode;
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }
            tagNamesOfPersistedArtifacts = getIdsOfPersistArtifactMethod.
                    getResponseBodyAsString().split(",");
            return tagNamesOfPersistedArtifacts;
        } catch (Exception ex) {
            String errorMsg = "Error while retrieving the tags of persisted artifact for job " +
                              jobName + " : " + ex.getMessage();
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            getIdsOfPersistArtifactMethod.releaseConnection();
        }
    }

    /**
     * edit job in lifeCycle change
     *
     * @param jobName       jobName
     * @param updateState   (addAD/removeAD) flag to remove or add Auto Deploy trigger configurations
     * @param pollingPeriod AD pollingPeriod
     * @throws AppFactoryException
     */
    public void editJob(String jobName, String updateState, int pollingPeriod)
            throws AppFactoryException {
        OMElement configuration = getConfiguration(jobName, updateState, pollingPeriod);
        OMElement tmpConfiguration = configuration.cloneOMElement();
        setConfiguration(jobName, tmpConfiguration);


    }

    /**
     * fetch job configurations from jenkins
     *
     * @param jobName job name of which we need to get the configuration of
     * @param updateState  (addAD/removeAD) flag to remove or add Auto Deploy trigger configurations
     * @param pollingPeriod AutoDeployment pollingPeriod
     * @return configuration after adding or removing AD configurations
     * @throws AppFactoryException
     */
    private OMElement getConfiguration(String jobName, String updateState, int pollingPeriod)
            throws AppFactoryException {
        GetMethod getFetchMethod = createGet(String.format("/job/%s/config.xml", jobName), null);
        OMElement configurations = null;

        try {
            int httpStatusCode = getHttpClient().executeMethod(getFetchMethod);
            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = String.format("Unable to retrieve available config urls from " +
                                                "jenkins for job %s. " +
                                                "jenkins returned, http status : %d",
                                jobName, httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }

            StAXOMBuilder builder = new StAXOMBuilder(getFetchMethod.getResponseBodyAsStream());
            configurations = builder.getDocumentElement();

            if (updateState.equals("removeAD")) {
                AXIOMXPath axiomxPath = new AXIOMXPath("//triggers");
                Object selectedObject = axiomxPath.selectSingleNode(configurations);
                if(selectedObject != null) {
                    OMElement selectedNode = (OMElement) selectedObject;
                    selectedNode.detach();
                }

            } else if (updateState.equals("addAD")) {
                String payload = "<triggers class=\"vector\">" +
                        "<hudson.triggers.SCMTrigger>" +
                        "<spec>*/" + pollingPeriod + " * * * *</spec>" +
                        "</hudson.triggers.SCMTrigger>" +
                        "</triggers>";
                OMElement triggerParam = AXIOMUtil.stringToOM(payload);
                configurations.addChild(triggerParam);

            }

        } catch (Exception ex) {
            String errorMsg = String.format("Unable to retrieve available jobs from jenkins : %s",
                            ex.getMessage());
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            getFetchMethod.releaseConnection();
        }
        return configurations;

    }

    /**
     * update the job configuration
     *
     * @param jobName job name of which we need to update the configuration of
     * @param jobConfiguration new configurations that needs to be set
     * @throws AppFactoryException
     */
    private void setConfiguration(String jobName, OMElement jobConfiguration)
            throws AppFactoryException {

        NameValuePair[] queryParams = {new NameValuePair("name", jobName)};
        PostMethod createJob = null;
        boolean jobCreatedFlag = false;

        try {
            createJob =
                    createPost(String.format("/job/%s/config.xml", jobName), queryParams,
                            new StringRequestEntity(jobConfiguration.toStringWithConsume(),
                                    "text/xml", "utf-8"));
            int httpStatusCode = getHttpClient().executeMethod(createJob);

            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg = String.format("Unable to create the job: [%s]. jenkins " +
                                                "returned, http status : %d",
                                jobName, httpStatusCode);
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            } else {
                jobCreatedFlag = true;
            }

        } catch (Exception ex) {
            String errorMsg = "Error while trying creating job: " + jobName;
            log.error(errorMsg, ex);

            //noinspection ConstantConditions
            if (jobCreatedFlag) {
                try {
                    deleteJob(jobName);
                } catch (AppFactoryException delExpception) {
                    log.error("Unable to delete the job after failed attempt set svn credentials, " +
                              "job: " + jobName, delExpception);
                }
            }

            throw new AppFactoryException(errorMsg, ex);
        } finally {

            if (createJob != null) {
                createJob.releaseConnection();
            }
        }

    }

    /**
     * Util method to create a http GET method.
     *
     * @param urlFragment     Url fragments
     * @param queryParameters query parameters.
     * @return a {@link GetMethod}
     */
    private GetMethod createGet(String urlFragment, NameValuePair[] queryParameters) {
        return createGet(getJenkinsUrl(), urlFragment, queryParameters);
    }

    /**
     * Util method to create a http get method
     *
     * @param baseUrl         the base url
     * @param urlFragment     the url fragment
     * @param queryParameters query parameters
     * @return a {@link GetMethod}
     */
    private GetMethod createGet(String baseUrl, String urlFragment, NameValuePair[] queryParameters) {
        GetMethod get = new GetMethod(baseUrl.concat(urlFragment));
        if (authenticate) {
            get.setDoAuthentication(true);
        }
        if (queryParameters != null) {
            get.setQueryString(queryParameters);
        }
        return get;
    }


    /**
     * Util method to create a POST method
     *
     * @param urlFragment     Url fragments.
     * @param queryParameters Query parameters.
     * @param requestEntity   A request entity
     * @return a {@link PostMethod}
     */
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

    /**
     * Calls to Jenkins and creates a tag from the latest successful built artifact by given {@code newTagName}.
     *
     * @param jobName      Name of the job to be tagged.
     * @param artifactType artifactType artifact type (car/war) that is going to get tagged.
     * @param newTagName   the name that the new tag will be created.
     * @param version      TODO
     * @throws AppFactoryException
     */
    public void createNewTagByLastSuccessBuild(String jobName, String artifactType,
                                               String newTagName, String version,String stage)
            throws AppFactoryException {

        String createNewTagByLastSuccessBuildUrl =
                "/plugin/appfactory-plugin/createNewTagByLastSuccessBuild";

        String applicationId = getAppId(jobName);

        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("applicationId", applicationId));
        parameters.add(new NameValuePair("artifactType", artifactType));
        parameters.add(new NameValuePair("jobName", jobName));
        parameters.add(new NameValuePair("tagName", newTagName));
        parameters.add(new NameValuePair("version", version));

        addStageSpecificParameters(stage,parameters);

        PostMethod createNewTagByLastSuccessBuildMethod =
                createPost(createNewTagByLastSuccessBuildUrl,
                        parameters.toArray(new NameValuePair[parameters.size()]),
                        null);
        log.debug("Trying to invoke the jenkins service : createNewTagByLastSuccessBuild with applicationId -" +
                applicationId + " tagName -" + newTagName);

        try {
            int httpStatusCode = getHttpClient().executeMethod(createNewTagByLastSuccessBuildMethod);
            log.info("status code for create new tag from last successful build : " + httpStatusCode);
            if (HttpStatus.SC_OK != httpStatusCode) {
                String errorMsg =
                        "Unable to crate tag from last successful build jenkins " +
                                "returned, http status : " + httpStatusCode;
                log.error(errorMsg);
                throw new AppFactoryException(errorMsg);
            }
        } catch (Exception ex) {
            String errorMsg =
                    "Unable to crate tag from last successful build jenkins : " +
                            ex.getMessage();
            log.error(errorMsg, ex);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            createNewTagByLastSuccessBuildMethod.releaseConnection();
        }

    }

}
