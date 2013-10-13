package org.wso2.carbon.appfactory.jenkins.deploy;

import hudson.FilePath;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.StaplerRequest;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.jenkins.AppfactoryPluginManager;
import org.wso2.carbon.appfactory.jenkins.Constants;
import org.wso2.carbon.appfactory.jenkins.deploy.notify.DeployNotifier;
import org.wso2.carbon.application.mgt.stub.upload.types.carbon.UploadedFileItem;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappUploadData;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class AbstractDeployer implements JenkinsDeployer {

    private static final Log log = LogFactory.getLog(AbstractDeployer.class);

    protected static final String APPLICATION_TYPE_WAR = Constants.APPLICATION_TYPE_WAR;
    protected static final String APPLICATION_TYPE_CAR = Constants.APPLICATION_TYPE_CAR;
    protected static final String APPLICATION_TYPE_ZIP = Constants.APPLICATION_TYPE_ZIP;
    protected static final String APPLICATION_TYPE_JAXWS = Constants.APPLICATION_TYPE_JAXWS;
    protected static final String APPLICATION_TYPE_JAXRS = Constants.APPLICATION_TYPE_JAXRS;
    protected static final String APPLICATION_TYPE_JAGGERY = Constants.APPLICATION_TYPE_JAGGERY;
    protected static final String APPLICATION_TYPE_DBS = Constants.APPLICATION_TYPE_DBS;
    protected static final String APPLICATION_TYPE_BPEL = Constants.APPLICATION_TYPE_BPEL;
    protected static final String APPLICATION_TYPE_PHP = Constants.APPLICATION_TYPE_PHP;
    protected static final String APPLICATION_TYPE_ESB = Constants.APPLICATION_TYPE_ESB;
    protected static final String APPLICATION_TYPE_XML = Constants.APPLICATION_TYPE_XML;
    protected static final String APPLICATION_ID = Constants.APPLICATION_ID;
    protected static final String JOB_NAME = Constants.JOB_NAME;
    protected static final String TAG_NAME = Constants.TAG_NAME;
    protected static final String DEPLOY_STAGE = Constants.DEPLOY_STAGE;
    protected static final String ARTIFACT_TYPE = Constants.ARTIFACT_TYPE;
    protected static final String DEPLOYMENT_SERVER_URLS = Constants.DEPLOYMENT_SERVER_URLS;
    protected static final String ESBDEPLOYMENT_SERVER_URLS = Constants.ESBDEPLOYMENT_SERVER_URLS;

    protected static final String DEPLOY_ACTION = Constants.DEPLOY_ACTION;

    protected AppfactoryPluginManager.DescriptorImpl descriptor;


    @SuppressWarnings("unused")
    public AbstractDeployer() {
        descriptor = new AppfactoryPluginManager.DescriptorImpl();
    }

    /**
     * This will deploy the artifact in the given job with the specified tag name
     *
     * @param req request
     */
    public void deployTaggedArtifact(StaplerRequest req) throws Exception {
        String jobName = req.getParameter(JOB_NAME);
        String tagName = req.getParameter(TAG_NAME);
        String artifactType = req.getParameter(ARTIFACT_TYPE);
        String deployAction = req.getParameter(DEPLOY_ACTION);

        //TODO: MOve this to a separate call from the client. : Promote last successful build.
        if (deployAction.equals("promote") || tagName.isEmpty()) {

            log.info("Since no tag name is specified latest successful build will be deployed.");
            deployLatestSuccessArtifact(req);
            log.info("Initial deployement was successfull");
            return;
        }

        try {
            deployTaggedArtifact(req, jobName, tagName, artifactType);
            if (deployAction.equals("promote")) {
                labelAsPromotedArtifact(jobName, tagName);
            }
        } catch (AppFactoryException e) {
            log.error("deployment of tagged artifact " + tagName + " failed for " + jobName, e);
        }
    }

    /**
     * Method labels the last successful build as PROMOTED by copiting last sucessful build into PROMOTED location.
     *
     * @param jobName
     * @param artifactType
     * @throws AppFactoryException
     * @throws IOException
     * @throws InterruptedException
     */
    private void labelLastSuccessAsPromoted(String jobName, String artifactType) throws AppFactoryException, IOException, InterruptedException {

        String lastSucessBuildFilePath = System.getenv("JENKINS_HOME") + File.separator + "jobs" +
                File.separator + jobName + File.separator +
                "lastSuccessful";
        log.debug("Last success build path is :" + lastSucessBuildFilePath);

        String jobPromotedPath = descriptor.getStoragePath() + File.separator + "PROMOTED" + File.separator + jobName;
        String dest = jobPromotedPath + File.separator + "lastSuccessful";
        File toBeCleaned = new File(jobPromotedPath);

        if (toBeCleaned.exists()) {
            // since only one artifact can be promoted for a version
            FileUtils.cleanDirectory(toBeCleaned);
        }
        File destDir = new File(dest);
        if (!destDir.mkdirs()) {
            log.error("Unable to create promoted tag for job:" + jobName);
            throw new AppFactoryException("Error occured while creating dir for last successful as PROMOTED:" + jobName);
        }

        File[] lastSucessFiles = getArtifact(lastSucessBuildFilePath, artifactType);
        for (File lastSucessFile : lastSucessFiles) {
            FilePath lastSuccessArtifactJenkinsPath = new FilePath(lastSucessFile);
            File destFile = new File(destDir.getAbsolutePath() + File.separator + lastSuccessArtifactJenkinsPath.getName());
            // given tag is copied to <jenkins-home>/storage/PROMOTED/<job-name>/<tag-name>/
            FilePath destinationFile = new FilePath(destFile);
            if (lastSuccessArtifactJenkinsPath.isDirectory()) {
                lastSuccessArtifactJenkinsPath.copyRecursiveTo(destinationFile);
            } else {
                lastSuccessArtifactJenkinsPath.copyTo(destinationFile);
            }
            log.info("labeled the lastSuccessful as PROMOTED");
        }
    }

    private void deployTaggedArtifact(StaplerRequest req, String jobName, String tagName, String artifactType) throws AppFactoryException {
        String path = descriptor.getStoragePath() + File.separator + jobName + File.separator + tagName;
        File[] artifactToDeploy = getArtifact(path, artifactType);
        deploy(artifactType, artifactToDeploy, req);
    }

    /**
     * Used to store the promoted artifact. This will store the artifact in jenkins storage
     *
     * @param jobName
     * @param tagName
     */
    private void labelAsPromotedArtifact(String jobName, String tagName) {

        try {

            String path = descriptor.getStoragePath() + File.separator + jobName + File.separator + tagName;
            FilePath tagPath = new FilePath(new File(path));

            String jobPromotedPath = descriptor.getStoragePath() + File.separator + "PROMOTED" + File.separator + jobName;
            String dest = jobPromotedPath + File.separator + tagName;

            File toBeCleaned = new File(jobPromotedPath);

            if (toBeCleaned.exists()) {
                // since only one artifact can be promoted for a version
                FileUtils.cleanDirectory(toBeCleaned);
            }

            File destDir = new File(dest);
            if (!destDir.mkdirs()) {
                log.error("Unable to create promoted tag for job:" + jobName + "tag:" + tagName);
            }
            // given tag is copied to <jenkins-home>/storage/PROMOTED/<job-name>/<tag-name>/
            tagPath.copyRecursiveTo(new FilePath(destDir));
            log.info("labeled the tag: " + tagName + " as PROMOTED");

        } catch (Exception e) {
            log.error("Error while labeling the tag: " + tagName + "as PROMOTED", e);
        }
    }

    /**
     * This method can be used to deploy a promoted artifact to a stage
     * We have used this method after first promote action of an application, so the artifact
     * deployed in first promote action will be deployed in the next promote action
     */
    public void deployPromotedArtifact(StaplerRequest req) throws Exception {
        String jobName = req.getParameter(JOB_NAME);
        String artifactType = req.getParameter(ARTIFACT_TYPE);
        String pathToPromotedArtifact = descriptor.getStoragePath() + File.separator + "PROMOTED" + File.separator + jobName;
//        File promotedArtifact = new File(pathToPromotedArtifact);
        File[] fileToDeploy = getArtifact(pathToPromotedArtifact, artifactType);
        deploy(artifactType, fileToDeploy, req);
    }

//    private File getArtifactFromStorage(String artifactType,String path ) throws AppFactoryException {
//        String[] fileExtension = new String[0];
//        if (APPLICATION_TYPE_JAXWS.equals(artifactType) || APPLICATION_TYPE_JAXRS.equals(artifactType)) {
//            fileExtension = new String[]{APPLICATION_TYPE_WAR};
//        } else if (APPLICATION_TYPE_JAGGERY.equals(artifactType)) {
//            fileExtension = new String[]{APPLICATION_TYPE_ZIP};
//        } else if (APPLICATION_TYPE_DBS.equals(artifactType)) {
//            fileExtension = new String[]{APPLICATION_TYPE_DBS};
//        } else if (APPLICATION_TYPE_BPEL.equals(artifactType)) {
//            fileExtension = new String[]{APPLICATION_TYPE_ZIP};
//        } else if (APPLICATION_TYPE_PHP.equals(artifactType)) {
//            File phpAppParentDirectory=new File(path);
//            File directoryToDeploy=null;
//            for(File phpAppDir:phpAppParentDirectory.listFiles()){
//                if(phpAppDir.isDirectory() && phpAppDir.getName().contains("-")){
//                    return  directoryToDeploy=phpAppDir.getAbsoluteFile();
//                }
//            }
//        } else {
//            fileExtension = new String[]{artifactType};
//        }
//
//        List<File> artifactFiles = (List<File>) FileUtils.listFiles(new File(path), fileExtension,
//                true);
//
//        if (!(artifactFiles.size() > 0)) {
//            log.error("No promoted artifacts for " + artifactType + ". Cannot complete deploy promoted action");
//            throw new AppFactoryException("No promoted artifact found");
//        }
//        return artifactFiles.get(0);
//    }


    /**
     * This will deploy the latest successfully built artifact of the given job
     *
     * @param req request
     */
    public void deployLatestSuccessArtifact(StaplerRequest req) throws Exception {
        String jobName = req.getParameter(JOB_NAME);
        String artifactType = req.getParameter(ARTIFACT_TYPE);
        String stageName = req.getParameter(DEPLOY_STAGE);
        String deployAction = req.getParameter(DEPLOY_ACTION);

        if (deployAction == null || deployAction.isEmpty()) {
            deployAction = "deploy";
        }
//        We don't need to deployment server URL since this is read from the appfactory configurations
//        String[] deploymentServerUrls = req.getParameterValues(DEPLOYMENT_SERVER_URLS);

        try {
            String path = System.getenv("JENKINS_HOME") + File.separator + "jobs" + File.separator +
                    jobName + File.separator + "lastSuccessful";
            File lastSuccess = new File(path);

            // if no successful builds are there, we trigger a build first in order to deploy the
            // latest success artifact
            if (!lastSuccess.exists()) {
                log.info("No builds have been triggered for " + jobName + ". Building " + jobName +
                        " first to deploy the latest built artifact");
                String jenkinsUrl = req.getRootPath();
                List<NameValuePair> parameters = new ArrayList<NameValuePair>();
                parameters.add(new NameValuePair("isAutomatic", "false"));
                parameters.add(new NameValuePair("doDeploy", deployAction));
                parameters.add(new NameValuePair("deployStage", stageName));
                parameters.add(new NameValuePair("persistArtifact", String.valueOf(false)));

                String buildUrl = jenkinsUrl + "/job/" + jobName + "/buildWithParameters";
                triggerBuild(jobName, buildUrl, parameters.toArray(new NameValuePair[parameters.size()]));
                // since automatic build deploy the latest artifact of successful builds to the
                // server, return after triggering the build
                return;
            }
            File[] artifactToDeploy = getArtifact(path, artifactType);
            deploy(artifactType, artifactToDeploy, req);
            if (deployAction.equalsIgnoreCase("promote")) {
                log.debug("Making last successful build as PROMOTED");
                labelLastSuccessAsPromoted(jobName, artifactType);
            }
        } catch (AppFactoryException e) {
            log.error("deployment of latest success artifact failed for " + jobName, e);
        }
    }

    /**
     * This method is used to build the specified job
     * build parameters are set in such a way that it does not execute any post build actions
     *
     * @param jobName  job that we need to build
     * @param buildUrl url used to trigger the build
     * @throws AppFactoryException
     */
    protected void triggerBuild(String jobName, String buildUrl, NameValuePair[] queryParameters) throws AppFactoryException {
        PostMethod buildMethod = new PostMethod(buildUrl);
        buildMethod.setDoAuthentication(true);
        if (queryParameters != null) {
            buildMethod.setQueryString(queryParameters);
        }
        HttpClient httpClient = new HttpClient();
        httpClient.getState().setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(getAdminUsername(),
                        getServerAdminPassword()));
        httpClient.getParams().setAuthenticationPreemptive(true);
        int httpStatusCode = -1;
        try {
            httpStatusCode = httpClient.executeMethod(buildMethod);

        } catch (Exception ex) {
            String errorMsg = String.format("Unable to start the build on job : %s",
                    jobName);
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg, ex);
        } finally {
            buildMethod.releaseConnection();
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

    protected void handleException(String msg) throws AppFactoryException {
        log.error(msg);
        throw new AppFactoryException(msg);
    }

    protected void handleException(String msg, Exception e) throws AppFactoryException {
        log.error(msg, e);
        throw new AppFactoryException(msg, e);
    }

    /**
     * This method will be used to retrieve the artifact in the given path
     *
     * @param path         path were artifact has been stored
     * @param artifactType artifact type (car/war)
     * @return the artifact
     * @throws AppFactoryException
     */
    protected File[] getArtifact(String path, String artifactType) throws AppFactoryException {
        String[] fileExtension = new String[0];

        List<File> fileList = new ArrayList<File>();
        if (APPLICATION_TYPE_JAXWS.equals(artifactType) || APPLICATION_TYPE_JAXRS.equals(artifactType)) {
            fileExtension = new String[]{APPLICATION_TYPE_WAR};
        } else if (APPLICATION_TYPE_JAGGERY.equals(artifactType)) {
            fileExtension = new String[]{APPLICATION_TYPE_ZIP};
        } else if (APPLICATION_TYPE_DBS.equals(artifactType)) {
            fileExtension = new String[]{APPLICATION_TYPE_DBS,APPLICATION_TYPE_XML};
        } else if (APPLICATION_TYPE_BPEL.equals(artifactType)) {
            fileExtension = new String[]{APPLICATION_TYPE_ZIP};
        } else if (APPLICATION_TYPE_PHP.equals(artifactType)) {
            File phpAppParentDirectory = new File(path + File.separator + "archive");
            for (File phpAppDir : phpAppParentDirectory.listFiles()) {
                if (phpAppDir.isDirectory() && phpAppDir.getName().contains("-")) {
                    fileList.add(phpAppDir.getAbsoluteFile());
                }
            }
        } else if (APPLICATION_TYPE_ESB.equals(artifactType)) {
            path = path + File.separator + "archive";
            fileExtension = new String[]{APPLICATION_TYPE_XML};
        } else {
            fileExtension = new String[]{artifactType};
        }

        fileList.addAll((List<File>) FileUtils.listFiles(new File(path), fileExtension, true));

        if (!(fileList.size() > 0)) {
            log.error("No built artifact found");
            throw new AppFactoryException("No built artifact found");
        }
        return fileList.toArray(new File[fileList.size()]);
    }

    /**
     * Deploy the given artifact to the given server URLs
     *
     * @param artifactType      artifact type
     * @param artifactsToDeploy artifacts that needs to be deployed
     */

    protected void deploy(String artifactType, File[] artifactsToDeploy, StaplerRequest request) {
        DeployNotifier notifier = new DeployNotifier();

        if (APPLICATION_TYPE_CAR.equals(artifactType)) {
//            We expect only one artifact here.
            File artifactToDeploy = artifactsToDeploy[0];
            DataHandler dataHandler = new DataHandler(new FileDataSource(artifactToDeploy));

            UploadedFileItem uploadedFileItem = new UploadedFileItem();
            uploadedFileItem.setDataHandler(dataHandler);
            uploadedFileItem.setFileName(artifactToDeploy.getName());
            uploadedFileItem.setFileType("jar");

            UploadedFileItem[] uploadedFileItems = {uploadedFileItem};

            deployCarbonApp(uploadedFileItems, request.getParameterMap());
            notifier.deployed(request.getParameter(JOB_NAME), request.getParameter(DEPLOY_STAGE),
                    descriptor.getAdminUserName(), descriptor.getAdminPassword(), descriptor.getAppfactoryServerURL());

        } else if (APPLICATION_TYPE_WAR.equals(artifactType)) {
//            We expect only one artifact here.
            File artifactToDeploy = artifactsToDeploy[0];
            DataHandler dataHandler = new DataHandler(new FileDataSource(artifactToDeploy));

            WebappUploadData webappUploadData = new WebappUploadData();
            webappUploadData.setDataHandler(dataHandler);
            webappUploadData.setFileName(artifactToDeploy.getName());

            WebappUploadData[] webappUploadDataItems = {webappUploadData};
            uploadWebApp(webappUploadDataItems, request.getParameterMap());
            log.info("Application Deployed Successfully. Job Name :" + request.getParameter(JOB_NAME));

            notifier.deployed(request.getParameter(JOB_NAME), request.getParameter(DEPLOY_STAGE),
                    descriptor.getAdminUserName(), descriptor.getAdminPassword(), descriptor.getAppfactoryServerURL());

        } else if (APPLICATION_TYPE_JAXWS.equals(artifactType) || APPLICATION_TYPE_JAXRS.equals(artifactType)) {
//            We expect only one artifact here.
            File artifactToDeploy = artifactsToDeploy[0];
            DataHandler dataHandler = new DataHandler(new FileDataSource(artifactToDeploy));

            WebappUploadData webappUploadData = new WebappUploadData();
            webappUploadData.setDataHandler(dataHandler);
            webappUploadData.setFileName(artifactToDeploy.getName());

            WebappUploadData[] webappUploadDataItems = {webappUploadData};
            uploadJaxWebApp(webappUploadDataItems, request.getParameterMap());

            log.info("Application Deployed Successfully. Job Name :" + request.getParameter(JOB_NAME));

            notifier.deployed(request.getParameter(JOB_NAME), request.getParameter(DEPLOY_STAGE),
                    descriptor.getAdminUserName(), descriptor.getAdminPassword(), descriptor.getAppfactoryServerURL());
        } else if (APPLICATION_TYPE_JAGGERY.equals(artifactType)) {
//            We expect only one artifact here.
            File artifactToDeploy = artifactsToDeploy[0];
            DataHandler dataHandler = new DataHandler(new FileDataSource(artifactToDeploy));

            org.jaggeryjs.jaggery.app.mgt.stub.types.carbon.WebappUploadData webappUploadData =
                    new org.jaggeryjs.jaggery.app.mgt.stub.types.carbon.WebappUploadData();
            webappUploadData.setDataHandler(dataHandler);
            webappUploadData.setFileName(artifactToDeploy.getName());

            org.jaggeryjs.jaggery.app.mgt.stub.types.carbon.WebappUploadData[] webappUploadDataItems = {webappUploadData};
            uploadJaggeryApp(webappUploadDataItems, request.getParameterMap());

            log.info("Application Deployed Successfully. Job Name :" + request.getParameter(JOB_NAME));

            notifier.deployed(request.getParameter(JOB_NAME), request.getParameter(DEPLOY_STAGE),
                    descriptor.getAdminUserName(), descriptor.getAdminPassword(), descriptor.getAppfactoryServerURL());
        } else if (APPLICATION_TYPE_DBS.equals(artifactType)) {
//            We expect 2 artifact artifact here.
            List<UploadItem> dataItems = new ArrayList<UploadItem>();
            for (File artifactToDeploy : artifactsToDeploy) {
                DataHandler dataHandler = new DataHandler(new FileDataSource(artifactToDeploy));

                UploadItem dataItem = new UploadItem();
                dataItem.setFileName(artifactToDeploy.getName());
                dataItem.setDataHandler(dataHandler);

                dataItems.add(dataItem);
            }
            uploadDBSApp(dataItems.toArray(new UploadItem[dataItems.size()]), request.getParameterMap());
            log.info("Application Deployed Successfully. Job Name :" + request.getParameter(JOB_NAME));

            notifier.deployed(request.getParameter(JOB_NAME), request.getParameter(DEPLOY_STAGE),
                    descriptor.getAdminUserName(), descriptor.getAdminPassword(), descriptor.getAppfactoryServerURL());
        } else if (APPLICATION_TYPE_BPEL.equals(artifactType)) {
//            We expect only one artifact here.
            File artifactToDeploy = artifactsToDeploy[0];
            DataHandler dataHandler = new DataHandler(new FileDataSource(artifactToDeploy));

            org.wso2.carbon.bpel.stub.upload.types.UploadedFileItem uploadedData =
                    new org.wso2.carbon.bpel.stub.upload.types.UploadedFileItem();
            uploadedData.setDataHandler(dataHandler);
            uploadedData.setFileName(artifactToDeploy.getName());
            uploadedData.setFileType(APPLICATION_TYPE_ZIP);

            org.wso2.carbon.bpel.stub.upload.types.UploadedFileItem[] uploadedDataItems = {uploadedData};
            uploadBPEL(uploadedDataItems, request.getParameterMap());

            log.info("Application Deployed Successfully. Job Name :" + request.getParameter(JOB_NAME));

            notifier.deployed(request.getParameter(JOB_NAME), request.getParameter(DEPLOY_STAGE),
                    descriptor.getAdminUserName(), descriptor.getAdminPassword(), descriptor.getAppfactoryServerURL());
        } else if (APPLICATION_TYPE_PHP.equals(artifactType)) {
//            We expect only one artifact here.
            File artifactToDeploy = artifactsToDeploy[0];
            DataHandler dataHandler = new DataHandler(new FileDataSource(artifactToDeploy));

            UploadItem dataItem = new UploadItem();
            dataItem.setFileName(artifactToDeploy.getName());
            dataItem.setDataHandler(dataHandler);

            UploadItem[] uploadItems = {dataItem};
            uploadPHP(uploadItems, request.getParameterMap());

            log.info("Application Deployed Successfully. Job Name :" + request.getParameter(JOB_NAME));

            notifier.deployed(request.getParameter(JOB_NAME), request.getParameter(DEPLOY_STAGE),
                    descriptor.getAdminUserName(), descriptor.getAdminPassword(), descriptor.getAppfactoryServerURL());
        } else if (APPLICATION_TYPE_ESB.equals(artifactType)) {
//            We expect only one artifact here.
            File artifactToDeploy = artifactsToDeploy[0];
            DataHandler dataHandler = new DataHandler(new FileDataSource(artifactToDeploy));

            UploadItem dataItem = new UploadItem();
            dataItem.setFileName(artifactToDeploy.getName());
            dataItem.setDataHandler(dataHandler);

            UploadItem[] uploadItems = {dataItem};
            uploadESBApp(uploadItems, request.getParameterMap());

            log.info("Application Deployed Successfully. Job Name :" + request.getParameter(JOB_NAME));

            notifier.deployed(request.getParameter(JOB_NAME), request.getParameter(DEPLOY_STAGE),
                    descriptor.getAdminUserName(), descriptor.getAdminPassword(), descriptor.getAppfactoryServerURL());
        }

    }

    protected String getAdminUsername() {
        return descriptor.getAdminUserName();
    }

    protected String getAdminUsername(String applicationId) {
        return descriptor.getAdminUserName() + "@" + applicationId;
    }

    protected String getServerAdminPassword() {
        return descriptor.getAdminPassword();
    }

    protected String getParameterValue(Map metadata, String key) {
        if (metadata.get(key) == null) {
            return null;
        }
        if (metadata.get(key) instanceof String[]) {
            String[] values = (String[]) metadata.get(key);
            if (values.length > 0) {
                return values[0];
            }
            return null;
        } else if (metadata.get(key) instanceof String) {
            return metadata.get(key).toString();
        }

        return null;
    }

    protected String[] getParameterValues(Map metadata, String key) {
        if (metadata.get(key) == null) {
            return null;
        }
        if (metadata.get(key) instanceof String[]) {
            return (String[]) metadata.get(key);
        } else if (metadata.get(key) instanceof String) {
            return new String[]{metadata.get(key).toString()};
        }

        return null;
    }

    public abstract void deployCarbonApp(UploadedFileItem[] uploadedFileItems, Map metadata);

    public abstract void uploadWebApp(WebappUploadData[] webappUploadDatas, Map metadata);

    public abstract void uploadJaxWebApp(WebappUploadData[] webappUploadDatas, Map metadata);

    public abstract void uploadJaggeryApp(
            org.jaggeryjs.jaggery.app.mgt.stub.types.carbon.WebappUploadData[] webappUploadDatas, Map metadata);

    public abstract void uploadDBSApp(UploadItem[] uploadData, Map metadata);

    public abstract void uploadPHP(UploadItem[] uploadData, Map metadata);

    public abstract void uploadBPEL(
            org.wso2.carbon.bpel.stub.upload.types.UploadedFileItem uploadedFileItem[], Map metadata);

    public abstract void uploadESBApp(UploadItem[] uploadData, Map metadata);

}
