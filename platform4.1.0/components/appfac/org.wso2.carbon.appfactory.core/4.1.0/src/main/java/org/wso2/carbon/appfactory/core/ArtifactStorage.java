package org.wso2.carbon.appfactory.core;

import java.io.File;

import org.wso2.carbon.appfactory.common.AppFactoryException;

import javax.activation.DataHandler;

/**
 * This is the artifact storage.
 * The built artifacts are stored in this storage for retrieval by the deployment service
 */
public interface ArtifactStorage {
    
    /**
     * This will retrieve the artifact as a file.
     * This is a blocking call.
     * 
     * @param applicationId Application Id
     * @param version Version of the artifact
     * @param revision The revision of the artifacct
     * @return
     */
    public File retrieveArtifact(String applicationId, String version, String revision) throws AppFactoryException;

    /**
     *
     * @param applicationId
     * @param version
     * @param revision
     * @param buildId
     * @return
     * @throws AppFactoryException
     */
    public File retrieveArtifact(String applicationId, String version, String revision, String buildId) throws AppFactoryException;

    /**
     * This will store the artifact in the artifact storage.
     * 
     * @param applicationId Application Id
     * @param version  Version of the artifact
     * @param revision The revision of the artifact
     * @param data The artifact
     */
    public void storeArtifact(String applicationId, String version, String revision, DataHandler data, String fileName) throws AppFactoryException;

    /**
     * This will store the artifact in artifact storage, considering the buildId
     * @param applicationId
     * @param version
     * @param revision
     * @param buildId
     * @param data
     * @param fileName
     * @throws AppFactoryException
     */
    public void storeArtifact(String applicationId, String version, String revision, String buildId, DataHandler data, String fileName) throws AppFactoryException;

}
