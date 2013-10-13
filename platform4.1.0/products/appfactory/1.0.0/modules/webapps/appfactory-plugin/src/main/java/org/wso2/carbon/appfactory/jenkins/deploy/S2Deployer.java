package org.wso2.carbon.appfactory.jenkins.deploy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.FileUtils;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.jenkins.clients.AppfactoryRepositoryClient;
import org.wso2.carbon.application.mgt.stub.upload.types.carbon.UploadedFileItem;
import org.wso2.carbon.webapp.mgt.stub.types.carbon.WebappUploadData;

import javax.activation.DataHandler;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class S2Deployer extends AbstractDeployer {
    private static final Log log = LogFactory.getLog(S2Deployer.class);

    @Override
    public void deployCarbonApp(UploadedFileItem[] uploadedFileItems, Map metadata) {
        addToGitRepo(uploadedFileItems[0].getFileName(), uploadedFileItems[0].getDataHandler(), metadata, "");

    }

    @Override
    public void uploadWebApp(WebappUploadData[] webappUploadDatas, Map metadata) {
        addToGitRepo(webappUploadDatas[0].getFileName(), webappUploadDatas[0].getDataHandler(), metadata, "webapps");
    }

    @Override
    public void uploadJaxWebApp(WebappUploadData[] webappUploadDatas, Map metadata) {
        addToGitRepo(webappUploadDatas[0].getFileName(), webappUploadDatas[0].getDataHandler(), metadata, "jaxwebapps");

    }

    @Override
    public void uploadJaggeryApp(org.jaggeryjs.jaggery.app.mgt.stub.types.carbon.WebappUploadData[] webappUploadDatas, Map metadata) {
        addToGitRepo(webappUploadDatas[0].getFileName(), webappUploadDatas[0].getDataHandler(), metadata, "jaggeryapps");

    }

    private void addToGitRepo(String fileName, DataHandler dataHandler, Map metadata, String rootFolder) {
        String applicationId = getParameterValue(metadata, APPLICATION_ID);
        String gitRepoUrl = generateRepoUrl(applicationId, metadata);
        String stageName = getParameterValue(metadata, DEPLOY_STAGE);

//        String defaultUser = descriptor.getAdminUserName();
//        String defaultPassword = descriptor.getAdminPassword();

//        This is a temporary code. the above commented should be used
        String applicationAdmin = getParameterValue(metadata, "Deployer.RepositoryProvider.Property.AdminUserName");
        String defaultPassword = getParameterValue(metadata, "Deployer.RepositoryProvider.Property.AdminPassword");

//        String applicationAdmin = defaultUser + "@" + applicationId;
        String tempPath = descriptor.getTempPath();

        File tempLocation = new File(tempPath);
        if (!tempLocation.exists()) {
            if (!tempLocation.mkdir()) {
                log.error("Unable to create temp directory");
                return;
            }
        }

        String applicationParentTempPath = tempPath + File.separator + applicationId;
        File applicationParentTempLocation = new File(applicationParentTempPath);
        if (!applicationParentTempLocation.exists()) {
            if (!applicationParentTempLocation.mkdir()) {
                log.error("Unable to create application temp directory");
                return;
            }
        }

        String applicationTempPath = applicationParentTempPath + File.separator + stageName;
        File applicationTempLocation = new File(applicationTempPath);

        if (!applicationTempLocation.exists()) {
            if (!applicationTempLocation.mkdir()) {
                log.error("Unable to create application temp directory");
                return;
            }
        }

        AppfactoryRepositoryClient repositoryClient = new AppfactoryRepositoryClient("git");
        try {
            repositoryClient.init(applicationAdmin, defaultPassword);
            repositoryClient.checkOut(gitRepoUrl, applicationTempLocation);

            String applicationRootPath = applicationTempPath + File.separator + rootFolder;
            File applicationRootFile = new File(applicationRootPath);
            if (!applicationRootFile.exists()) {
                if (!applicationRootFile.mkdir()) {
                    log.error("Unable to create application root path");
                    return;
                }
            }

            String targetFilePath = applicationRootPath + File.separator + fileName;
            File targetFile = new File(targetFilePath);

//           If there is a file in repo, we delete it first
            if (targetFile.exists()) {
                repositoryClient.remove(gitRepoUrl, targetFile, "Removing the old file to add the new one");
                repositoryClient.checkIn(gitRepoUrl, applicationTempLocation, "Removing the old file to add the new one");
            }

            copyFilesToGit(dataHandler, targetFile);

            repositoryClient.add(gitRepoUrl, new File(targetFilePath));
            repositoryClient.checkIn(gitRepoUrl, applicationTempLocation, "Adding the artifact to the repo");
        } catch (AppFactoryException e) {
            String msg = "Unable to copy files to git location";
            log.error(msg, e);
        }
    }

    private void copyFilesToGit(DataHandler datahandler, File destinationFile)
            throws AppFactoryException {

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(destinationFile);
            datahandler.writeTo(fileOutputStream);

            fileOutputStream.flush();
            fileOutputStream.close();

        } catch (FileNotFoundException e) {
            log.error(e);
            throw new AppFactoryException(e);
        } catch (IOException e) {
            log.error(e);
            throw new AppFactoryException(e);
        }
    }

    private String generateRepoUrl(String applicationId, Map metadata) {
        String baseUrl = getParameterValue(metadata, "Deployer.RepositoryProvider.Property.BaseURL");
        String template = getParameterValue(metadata, "Deployer.RepositoryProvider.Property.URLPattern");

        String gitRepoUrl = baseUrl + "git/" + template;
        return gitRepoUrl.replace("{@application_key}", applicationId).replace("{@stage}", getParameterValue(metadata, "deployStage"));
    }

    @Override
    public void uploadBPEL(org.wso2.carbon.bpel.stub.upload.types.UploadedFileItem[] uploadedFileItem, Map metadata) {
        //No implementation available for S2
        log.error("No implementation available for S2");

    }

    @Override
    public void uploadDBSApp(UploadItem[] uploadData, Map metadata) {
        String dbsFileName = null;
        for (UploadItem uploadItem : uploadData) {
            log.info(uploadItem.getFileName());
            if (uploadItem.getFileName().endsWith("dbs")) {
                addToGitRepo(uploadItem.getFileName(), uploadItem.getDataHandler(), metadata, "dataservices");
                dbsFileName = FileUtils.filename(uploadItem.getFileName());
                break;
//            }else if(uploadItem.getFileName().endsWith("xml")){
//                addToGitRepo(uploadItem.getFileName(), uploadItem.getDataHandler(), metadata, "servicemetafiles");
            }
        }
        if (dbsFileName != null) {
            for (UploadItem uploadItem : uploadData) {
                if (uploadItem.getFileName().startsWith(dbsFileName) &&
                        uploadItem.getFileName().endsWith(APPLICATION_TYPE_XML)) {
//                    This is the service xml file. It has the same name as the dbs file. So we are committing it here.
                    addToGitRepo(uploadItem.getFileName(), uploadItem.getDataHandler(), metadata, "servicemetafiles");
                    break;

                }
            }
        }
    }

    @Override
    public void uploadPHP(UploadItem[] uploadData, Map metadata) {
//        TODO: we have to implement this for S2
    }

    @Override
    public void uploadESBApp(UploadItem[] uploadData, Map metadata) {
//        TODO: we have to implement this for S2
    }
}

