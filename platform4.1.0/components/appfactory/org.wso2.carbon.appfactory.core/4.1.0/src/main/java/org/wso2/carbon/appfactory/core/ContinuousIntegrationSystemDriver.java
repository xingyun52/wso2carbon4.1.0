package org.wso2.carbon.appfactory.core;

import java.util.List;

import org.wso2.carbon.appfactory.common.AppFactoryException;

/**
 * Defines the contact what needs to be implemented by any CI Driver (i.e.
 * Jenkins, Bambo)
 * 
 * 
 */
public interface ContinuousIntegrationSystemDriver {

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
    public void startBuild(String jobName) throws AppFactoryException;

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
}
