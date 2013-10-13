package org.wso2.carbon.appfactory.jenkins.build;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.ApplicationEventsListener;
import org.wso2.carbon.appfactory.core.dto.Application;
import org.wso2.carbon.appfactory.core.dto.UserInfo;
import org.wso2.carbon.appfactory.core.dto.Version;
import org.wso2.carbon.appfactory.jenkins.build.internal.ServiceContainer;
import org.wso2.carbon.appfactory.utilities.project.ProjectUtils;

/**
 * Listens to Application events (such as creation, user addition etc) and makes
 * relevant changes on Jenkins CI server.
 */
public class JenkinsApplicationEventsListener extends ApplicationEventsListener {

    private static Log log = LogFactory.getLog(JenkinsApplicationEventsListener.class);

    private int priority;

    /**
     * Creates a listener instance with given priority.
     *
     * @param priority The Priority
     * @scr.reference name="appfactory.configuration" interface=
     * "org.wso2.carbon.appfactory.common.AppFactoryConfiguration"
     * cardinality="1..1" policy="dynamic"
     * bind="setAppFactoryConfiguration"
     * unbind="unsetAppFactoryConfiguration"
     */
    public JenkinsApplicationEventsListener(int priority) {

        this.priority = priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreation(Application application) throws AppFactoryException {

        log.info("Application Creation event recieved for : " + application.getId() + " " +
                application.getName());
        ServiceContainer.getJenkinsCISystemDriver().setupApplicationAccount(application.getId());

        Version[] versions = ProjectUtils.getVersions(application.getId());

        if (ArrayUtils.isNotEmpty(versions)) {
            ServiceContainer.getJenkinsCISystemDriver().createJob(application.getId(),
                    versions[0].getId(), "");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUserAddition(Application application, UserInfo user) throws AppFactoryException {

        log.info("User Addition event recieved for : " + application.getId() + " " +
                application.getName() + " User Name : " + user.getUserName());

        ServiceContainer.getJenkinsCISystemDriver()
                .addUsersToApplication(application.getId(),
                        new String[]{user.getUserName()});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRevoke(Application application) throws AppFactoryException {
        // Improvement : remove the jobs from jenkins
        // Improvement : Remore roles (since appfactory uses role strategy
        // plugin) associated with the app
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onVersionCreation(Application application, Version source, Version target)
            throws AppFactoryException {

        log.info("Version Creation event recieved for : " + application.getId() + " " +
                application.getName() + " Version : " + target.getId());

        ServiceContainer.getJenkinsCISystemDriver().createJob(application.getId(), target.getId(),
                "");

    }

    /**
     * onLifeCycleStateChange update the job configuration if needed
     *
     * @param application application of which LC stage got changed
     * @param version version of which the LC stage got changed
     * @param previousStage previous LC stage
     * @param nextStage new LC stage
     * @throws AppFactoryException
     */

    public void onLifeCycleStageChange(Application application, Version version,
                                       String previousStage, String nextStage) throws
                                                                               AppFactoryException {

        String deploymentState = "";
        int pollingPeriod = 0;

        AppFactoryConfiguration configuration = ServiceContainer.getAppFactoryConfiguration();
        boolean previousDeploymentStage = Boolean.parseBoolean(configuration.getFirstProperty(
                "ApplicationDeployment.DeploymentStage." + previousStage +
                ".AutomaticDeployment.Enabled"));
        boolean nextDeploymentStage = Boolean.parseBoolean(configuration.getFirstProperty(
                "ApplicationDeployment.DeploymentStage." + nextStage +
                ".AutomaticDeployment.Enabled"));
        if (!previousDeploymentStage && nextDeploymentStage) {
            pollingPeriod = Integer.parseInt(configuration.getFirstProperty(
                    "ApplicationDeployment.DeploymentStage."
                    + previousStage + ".AutomaticDeployment.PollingPeriod"));
            deploymentState = "addAD";

        } else if (previousDeploymentStage && !nextDeploymentStage) {
            deploymentState = "removeAD";
        }

        ServiceContainer.getJenkinsCISystemDriver().editADJobConfiguration(
                application.getId(), version.getId(), deploymentState, pollingPeriod);

    }

    @SuppressWarnings("UnusedDeclaration")
    public void onAutoDeploymentVersionChange(Application application, Version previousVersion,
                                              Version newVersion, String newStage)
            throws AppFactoryException {

        log.info("AutoDeployment Version Change event recieved for : " + application.getId() + " " +
                 application.getName() + " From Version : " + previousVersion.getId() +
                 " To Version : " + newVersion.getId());
        int pollingPeriod = 0;

        //noinspection ConstantConditions
        if (previousVersion != null) {
            ServiceContainer.getJenkinsCISystemDriver().editADJobConfiguration(
                    application.getId(), previousVersion.getId(), "removeAD", pollingPeriod);
        }

        //noinspection ConstantConditions
        if (newVersion != null) {
            AppFactoryConfiguration configuration = ServiceContainer.getAppFactoryConfiguration();
            pollingPeriod = Integer.parseInt(configuration.getFirstProperty(
                    "ApplicationDeployment.DeploymentStage." + newStage +
                    ".AutomaticDeployment.PollingPeriod"));
            ServiceContainer.getJenkinsCISystemDriver().editADJobConfiguration(
                    application.getId(), newVersion.getId(), "addAD", pollingPeriod);


        }


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUserDeletion(Application application, UserInfo user) throws AppFactoryException {
        // Improvement : remove the user from project role created for
        // application and the global roles assigned to him.
    }

    /**
     * {@inheritDoc}.
     */
    public int getPriority() {
        return priority;
    }
}
