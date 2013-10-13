package org.wso2.carbon.appfactory.jenkins.build;

import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.Storage;

/**
 * This class will be used to connect to the jenkins persistent storage and deploy artifacts in
 * jenkins side
 */
public class JenkinsStorage implements Storage {

    RestBasedJenkinsCIConnector connector;

    public JenkinsStorage(RestBasedJenkinsCIConnector connector) {
        this.connector = connector;
    }

    /**
     *
     * @param jobName jobName of which we need to get the tag names of
     * @return the tag names of the persisted artifacts for the given job name
     * @throws AppFactoryException
     */
    public String[] getTagNamesOfPersistedArtifacts(String jobName) throws AppFactoryException{
        return connector.getTagNamesOfPersistedArtifacts(jobName);
    }

    /**
     * Deploy the latest built artifact of the given job
     * @param jobName jobname
     * @param artifactType car/war
     * @param stage URLs to which the artifact will be deployed into
     * @throws AppFactoryException
     */
    public void deployLatestSuccessArtifact(String jobName, String artifactType,
                                            String stage) throws
                                                                           AppFactoryException{
        connector.deployLatestSuccessArtifact(jobName, artifactType, stage);
    }

    /**
     * Deploy the artifact with the given tag name of the given job
     * @param jobName job name
     * @param artifactType car/war
     * @param tagName tagName of the artifact to be deployed
     * @param stage URLs to which the artifact will be deployed into
     * @throws AppFactoryException
     */
    public void deployTaggedArtifact(String jobName, String artifactType, String tagName,
                                     String stage) throws AppFactoryException{
        connector.deployTaggedArtifact(jobName, artifactType, tagName, stage);
    }

	@Override
    public void createNewTagByLastSuccessBuild(String jobName, String artifactType, String newTagName, String version,
                                               String stage) throws AppFactoryException {
	    connector.createNewTagByLastSuccessBuild(jobName, artifactType, newTagName, version,stage);
	    
    }

}
