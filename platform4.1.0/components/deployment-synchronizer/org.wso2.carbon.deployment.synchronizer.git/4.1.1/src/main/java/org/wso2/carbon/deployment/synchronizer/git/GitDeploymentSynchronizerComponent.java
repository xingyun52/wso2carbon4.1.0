package org.wso2.carbon.deployment.synchronizer.git;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.deployment.synchronizer.ArtifactRepository;


/**
 * @scr.component name="org.wso2.carbon.deployment.synchronizer.git" immediate="true"
 */
public class GitDeploymentSynchronizerComponent {

    private static final Log log = LogFactory.getLog(GitDeploymentSynchronizerComponent.class);

    private ServiceRegistration gitDepSyncServiceRegistration;

    protected void activate(ComponentContext context) {

        ArtifactRepository gitBasedArtifactRepository = new GitBasedArtifactRepository();
        gitDepSyncServiceRegistration = context.getBundleContext().registerService(ArtifactRepository.class.getName(),
                gitBasedArtifactRepository, null);

        log.debug("Git based deployment synchronizer component activated");
    }

    protected void deactivate(ComponentContext context) {

        if(gitDepSyncServiceRegistration != null){
            gitDepSyncServiceRegistration.unregister();
            gitDepSyncServiceRegistration = null;
        }

        log.debug("Git based deployment synchronizer component deactivated");
    }

}
