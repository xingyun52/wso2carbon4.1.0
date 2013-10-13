package org.wso2.carbon.appfactory.core;

import org.wso2.carbon.appfactory.common.AppFactoryException;

public interface Storage {

    public String[] getTagNamesOfPersistedArtifacts(String jobName) throws AppFactoryException;

    public void deployLatestSuccessArtifact(String jobName, String artifactType, String stage) throws AppFactoryException;

    public void deployTaggedArtifact(String jobName, String artifactType, String tagName, String stage) throws AppFactoryException;

    /**
     * Latest success build of the given {@code applicationId} will be tagged as given {@code newTagName}.  
     * @param newTagName The name that the last success build to be tagged.
     * @param version TODO
     * @param applicationKey The key of the application to be tagged.
     * @param stage of the artifact to be tagged.
     * @param version  Version of the application
     * @throws AppFactoryException
     */
    public void createNewTagByLastSuccessBuild(String jobName, String artifactType, String newTagName, String version,
                                               String stage) throws AppFactoryException;

}
