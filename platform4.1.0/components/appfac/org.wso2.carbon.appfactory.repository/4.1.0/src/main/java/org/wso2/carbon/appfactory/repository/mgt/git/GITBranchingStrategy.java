package org.wso2.carbon.appfactory.repository.mgt.git;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.repository.mgt.BranchingStrategy;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryMgtException;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryProvider;
import org.wso2.carbon.appfactory.repository.mgt.client.AppfactoryRepositoryClient;
import org.wso2.carbon.appfactory.utilities.project.ProjectUtils;
import org.wso2.carbon.appfactory.utilities.version.AppVersionStrategyExecutor;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class GITBranchingStrategy implements BranchingStrategy {
    private static final Log log = LogFactory.getLog(GITBranchingStrategy.class);
    private RepositoryProvider provider;

    @Override
    public void prepareRepository(String applicationKey, String url) throws RepositoryMgtException {
        File workDir = new File(CarbonUtils.getTmpDir() + File.separator + applicationKey);
        if (!workDir.mkdirs()) {
            log.error("Error creating work directory at location" + workDir.getAbsolutePath());
        }
        if (provider != null) {


            AppfactoryRepositoryClient client = provider.getRepositoryClient();
            client.checkOut(url, workDir, "");

            try {
                String applicationType = ProjectUtils.getApplicationType(applicationKey);
                if (AppFactoryConstants.FILE_TYPE_CAR.equals(applicationType)) {
                    ProjectUtils.generateCAppArchetype(applicationKey, workDir.getAbsolutePath());
                } else if (AppFactoryConstants.FILE_TYPE_WAR.equals(applicationType)) {
                    ProjectUtils.generateWebAppArchetype(applicationKey, workDir.getAbsolutePath());
                }
            } catch (AppFactoryException e) {
//               There is an exception when generating the maven archetype.
                String msg = "Could not generate the project using maven archetype for application : " + applicationKey;
                log.error(msg, e);
                throw new RepositoryMgtException(msg, e);
            }
            generateGitIgnoreRecursively(workDir);


            client.addRecursively(url, workDir.getAbsolutePath());

            client.checkIn(url, workDir, "creating trunk,branches and tags ");
            client.close();
            try {
                FileUtils.deleteDirectory(workDir);
            } catch (IOException e) {
                log.error("Error deleting work directory " + e.getMessage(), e);
            }

        } else {
            String msg = new StringBuilder().append("Repository provider for the  ").append(applicationKey).append(" not found").toString();
            log.error(msg);
            throw new RepositoryMgtException(msg);
        }
    }

    @Override
    public void doRepositoryBranch(String appId, String currentVersion, String targetVersion,
                                   String currentRevision) throws RepositoryMgtException {
        String sourceURL = provider.getAppRepositoryURL(appId);
        File workDir = new File(CarbonUtils.getTmpDir() + File.separator + appId);
        if (!workDir.mkdir()) {
            log.error("Error creating work directory at location" + workDir.getAbsolutePath());
        }
        AppfactoryRepositoryClient client = provider.getRepositoryClient();

        if (currentVersion.equals("trunk")) {
            //this should be properly handled
            client.checkOut(sourceURL, workDir, "");
        } else {
            client.checkOutVersion(sourceURL, workDir, currentVersion);
        }

        client.branch(sourceURL, targetVersion, workDir.getPath());
        try {
            FileUtils.deleteDirectory(workDir);
        } catch (IOException e) {
            log.error("Error deleting work directory " + e.getMessage(), e);
        }
        workDir = new File(CarbonUtils.getTmpDir() + File.separator + appId);
        if (!workDir.mkdir()) {
            log.error("Error creating work directory at location" + workDir.getAbsolutePath());
        }
        client.checkOutVersion(sourceURL, workDir, targetVersion);
        new AppVersionStrategyExecutor().doVersion(targetVersion, workDir);
        client.checkIn(sourceURL, workDir, "branching", targetVersion);
        try {
            FileUtils.deleteDirectory(workDir);
        } catch (IOException e) {
            log.error("Error deleting work directory " + e.getMessage(), e);
        }
        client.close();
    }

    @Override
    public void doRepositoryTag(String appId, String currentVersion, String targetVersion,
                                String currentRevision) throws RepositoryMgtException {
        String sourceURL = provider.getAppRepositoryURL(appId);
        File workDir = new File(CarbonUtils.getTmpDir() + File.separator + appId);
        if (!workDir.mkdir()) {
            log.error("Error creating work directory at location" + workDir.getAbsolutePath());
        }
        AppfactoryRepositoryClient client = provider.getRepositoryClient();
        if (currentVersion.equals("trunk")) {
            client.checkOut(sourceURL, workDir, currentRevision);
        } else {
            client.checkOutVersion(sourceURL, workDir, currentVersion);
        }

        client.tag(sourceURL, targetVersion, workDir.getPath());
        try {
            FileUtils.deleteDirectory(workDir);
        } catch (IOException e) {
            log.error("Error deleting work directory " + e.getMessage(), e);
        }
        workDir = new File(CarbonUtils.getTmpDir() + File.separator + appId);
        if (!workDir.mkdir()) {
            log.error("Error creating work directory at location" + workDir.getAbsolutePath());
        }
        client.checkOutVersion(sourceURL, workDir, targetVersion);
        new AppVersionStrategyExecutor().doVersion(targetVersion, workDir);
        client.checkIn(sourceURL, workDir, "branching", targetVersion);
        try {
            FileUtils.deleteDirectory(workDir);
        } catch (IOException e) {
            log.error("Error deleting work directory " + e.getMessage(), e);
        }
        client.close();
    }

    @Override
    public void setRepositoryProvider(RepositoryProvider provider) {
        this.provider = provider;
    }

    @Override
    public RepositoryProvider getRepositoryProvider() {
        return this.provider;
    }

    @Override
    public String getURLForAppVersion(String applicationKey, String version)
            throws RepositoryMgtException {
        return getRepositoryProvider().getAppRepositoryURL(applicationKey);
    }

    private void generateGitIgnoreRecursively(File workDir) throws RepositoryMgtException {

        if (workDir.isDirectory()) {
            if (workDir.listFiles().length == 0) {
                try {
                    ProjectUtils.generateGitIgnore(workDir.getAbsolutePath());
                } catch (AppFactoryException e) {
                    String msg = "Could not add gitignore files ";
                    log.error(msg, e);
                    throw new RepositoryMgtException(msg, e);
                }
            } else {
                for (File child : workDir.listFiles()) {
                    if (child.isDirectory()) {
                        generateGitIgnoreRecursively(child);
                    }
                }
            }
        }

    }
}
