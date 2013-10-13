package org.wso2.carbon.appfactory.repository.mgt.svn;

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
public class SVNBranchingStrategy implements BranchingStrategy {
    private static final Log log = LogFactory.getLog(SVNBranchingStrategy.class);
    private RepositoryProvider provider;


    @Override
    public void prepareRepository(String applicationKey, String url)
            throws RepositoryMgtException {

        File workDir = new File(CarbonUtils.getTmpDir() + File.separator + applicationKey);
        if (!workDir.mkdirs()) {
            log.error("Error creating work directory at location" + workDir.getAbsolutePath());
        }
        if (provider != null) {


            AppfactoryRepositoryClient client = provider.getRepositoryClient();
            client.checkOut(url, workDir, "0");
            File trunk = new File(workDir.getAbsolutePath() + File.separator + AppFactoryConstants.TRUNK);
            if (!trunk.mkdir()) {
                log.error("Error creating work directory at location" + trunk.getAbsolutePath());
            }

            try {
                String applicationType = ProjectUtils.getApplicationType(applicationKey);
                if (AppFactoryConstants.FILE_TYPE_CAR.equals(applicationType)) {
                    ProjectUtils.generateCAppArchetype(applicationKey, trunk.getAbsolutePath());
                } else if (AppFactoryConstants.FILE_TYPE_WAR.equals(applicationType)) {
                    ProjectUtils.generateWebAppArchetype(applicationKey, trunk.getAbsolutePath());
                }
            } catch (AppFactoryException e) {
//               There is an exception when generating the maven archetype.
                String msg = "Could not generate the project using maven archetype for application : " + applicationKey;
                log.error(msg, e);
                throw new RepositoryMgtException(msg, e);
            }

            File branches = new File(workDir.getAbsolutePath() + File.separator + AppFactoryConstants.BRANCH);
            if (!branches.mkdir()) {
                log.error("Error creating work directory at location" + branches.getAbsolutePath());
            }

            File tags = new File(workDir.getAbsolutePath() + File.separator + AppFactoryConstants.TAG);
            if (!tags.mkdir()) {
                log.error("Error creating work directory at location" + tags.getAbsolutePath());
            }

            client.addRecursively(url, trunk.getAbsolutePath());
            client.add(url, branches);
            client.add(url, tags);
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
        if (AppFactoryConstants.TRUNK.equals(currentVersion)) {
            sourceURL = sourceURL + "/" + currentVersion;
        } else {
            sourceURL = sourceURL + "/" + AppFactoryConstants.BRANCH + "/" + currentVersion;
        }
        AppfactoryRepositoryClient client = provider.getRepositoryClient();
        client.checkOut(sourceURL, workDir, currentRevision);
        new AppVersionStrategyExecutor().doVersion(targetVersion, workDir);

        client.branch(sourceURL, targetVersion, workDir.getPath());
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
        if (AppFactoryConstants.TRUNK.equals(currentVersion)) {
            sourceURL = sourceURL + "/" + currentVersion;
        } else {
            sourceURL = sourceURL + "/" + AppFactoryConstants.BRANCH + "/" + currentVersion;
        }
        AppfactoryRepositoryClient client = provider.getRepositoryClient();
        client.checkOut(sourceURL, workDir, currentRevision);
        new AppVersionStrategyExecutor().doVersion(targetVersion, workDir);

        client.tag(sourceURL, targetVersion, workDir.getPath());
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
        return provider;
    }

    @Override
    public String getURLForAppVersion(String applicationKey, String version)
            throws RepositoryMgtException {
        StringBuilder builder = new StringBuilder(getRepositoryProvider().getAppRepositoryURL(applicationKey)).append('/');

        if (AppFactoryConstants.TRUNK.equals(version)) {
            builder.append(version);
        } else {
            builder.append(AppFactoryConstants.BRANCH).append('/').append(version);
        }
        return builder.toString();
    }
}
