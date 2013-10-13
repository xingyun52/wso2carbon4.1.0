package org.wso2.carbon.appfactory.jenkins.build;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
 * 
 */
public class JenkinsApplicationEventsListener extends ApplicationEventsListener {

    private static Log log = LogFactory.getLog(JenkinsApplicationEventsListener.class);

    private int priority;

    /**
     * Creates a listener instance with given priority.
     * 
     * @param priority
     *            The Priority
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
                                               new String[] { user.getUserName() });
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
