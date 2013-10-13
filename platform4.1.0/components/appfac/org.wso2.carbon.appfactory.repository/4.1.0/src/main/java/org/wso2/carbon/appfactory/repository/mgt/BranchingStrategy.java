package org.wso2.carbon.appfactory.repository.mgt;

/**
 *
 */
public interface BranchingStrategy {
    public void prepareRepository(String appId, String url)
            throws RepositoryMgtException;

    public void doRepositoryBranch(String appId, String currentVersion, String targetVersion,
                                   String currentRevision) throws RepositoryMgtException;

    public void doRepositoryTag(String appId, String currentVersion, String targetVersion,
                                String currentRevision) throws RepositoryMgtException;

    public void setRepositoryProvider(RepositoryProvider provider);

    public RepositoryProvider getRepositoryProvider();

    public String getURLForAppVersion(String applicationKey, String version)
            throws RepositoryMgtException;
}
