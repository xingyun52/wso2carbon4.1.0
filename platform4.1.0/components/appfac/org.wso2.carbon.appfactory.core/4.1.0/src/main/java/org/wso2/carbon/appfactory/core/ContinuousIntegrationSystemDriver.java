package org.wso2.carbon.appfactory.core;

import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.dto.Statistic;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Defines the contact what needs to be implemented by any CI Driver (i.e.
 * Jenkins, Bambo)
 * 
 * 
 */
public interface ContinuousIntegrationSystemDriver {
	
	
	public File getArtifact(String applicationId, String version, String artifactName) throws AppFactoryException;

    /**
     * Setup CI job for given application and versions
     * 
     * @param applicationId
     *            Id of the application
     * @param version
     *            version id
     * @param revision
     *            revision (This parameters is deprecated and need to be removed
     *            in future)
     * @throws AppFactoryException
     *             if a error occurs
     */
    public void createJob(String applicationId, String version, String revision)
                                                                                throws AppFactoryException;

    /**
     * Removes a specified job from CI System.
     * 
     * @param jobName
     *            Name of the job
     * @throws AppFactoryException
     *             If a error occurs
     */
    public void deleteJob(String jobName) throws AppFactoryException;

    /**
     * Returns jobs available in CI System.
     * 
     * @return A {@link List} of job names
     * @throws AppFactoryException
     *             If an error occurs
     */
    public List<String> getAllJobNames() throws AppFactoryException;

    /**
     * Starts building the specified CI job.
     * 
     * @param jobName
     *            Name of the CI Job
     * @throws AppFactoryException
     *             if an error occurs
     */
    public void startBuild(String jobName, boolean doDeploy, String stageName, String tagName) throws AppFactoryException;

    /**
     * Checks weather a specified job is available on CI system.
     * 
     * @param jobName
     *            Name of the job
     * @return true if job is available, false otherwise
     * @throws AppFactoryException
     *             if error occurs
     */
    public boolean isJobExists(String jobName) throws AppFactoryException;

    /**
     * Constructs a job name based on supplied parameter. Rational of this
     * method is to enable CI driver to have control over the job naming scheme.
     * 
     * @param applicationId
     *            application Id
     * @param version
     *            version Id
     * @param revision
     *            revision id - this parameter is deprecated and will be removed
     *            in future
     * @return name of the job
     */
    public String getJobName(String applicationId, String version, String revision);

    /**
     * Change the Auto Deployment configurations of the given job
     * @param applicationId
     * @param version
     * @param updateState
     * @param pollingPeriod
     * @throws AppFactoryException
     */
    public void editADJobConfiguration(String applicationId, String version, String updateState,
                                      int pollingPeriod) throws AppFactoryException;

    /**
     * Provides a array of {@link Statistic} about the ci server.
     * 
     * @param parameters
     *            any parameters that might be useful for stat calculation.
     * @return a list of {@link Statistic}
     * @throws AppFactoryException
     *             an error
     */
    public Statistic[] getGlobalStatistics(Map<String, String> parameters)
                                                                          throws AppFactoryException;;

    /**
     * Provides a array of {@link Statistic} about the builds related to a
     * specified application.
     * 
     * @param applicationId
     *            Id of the application.
     * 
     * @param parameters
     *            any parameters that might be useful for stat calculation.
     * @return a list of {@link Statistic}
     * @throws AppFactoryException
     *             an error
     */
    public Statistic[] getApplicationStatistics(String applicationId, Map<String, String> parameters)
                                                                                            throws AppFactoryException;;

    /**
     * Provides a String in a form of a JSON object with the information requested from Jenkins remote API.
     * @param jobName the name of the job, which you need the information about eg: applicationKey-trunk-default
     * @param treeStructure the structure of the returning JSON object eg: builds[param1,param2param3]
     * @return a String in a form of a JSON object
     * @throws AppFactoryException
     */
	public String getValuesForJobAsJsonTree(String jobName,String treeStructure)throws AppFactoryException;
}
