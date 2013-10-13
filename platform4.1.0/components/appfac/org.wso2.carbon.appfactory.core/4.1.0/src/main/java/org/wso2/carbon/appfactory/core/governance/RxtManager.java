package org.wso2.carbon.appfactory.core.governance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifactImpl;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RxtManager {
    /* todo: this class was moved from appfactory.governance to appfactory.core to remove the
     dependancy from core to governance.
     appfactory.core should be refactored to store only necessary classes */

    private static Log log = LogFactory.getLog(RxtManager.class);

    /**
     * This method will add the given newValue as the value of the key, replacing the existing value
     *
     * @param applicationId the Id of the current application
     * @param stage stage of the current application
     * @param version version of the current application
     * @param key the attribute key that is been updated
     * @param newValue the new value of the attribute key
     * @throws AppFactoryException
     */
    public void updateAppVersionRxt(String applicationId, String stage, String version, String key, String newValue)
            throws AppFactoryException {
        GenericArtifactImpl artifact = getAppVersionArtifact(applicationId, stage, version);
        log.info("=============== updating rxt =============== key:" + key + " value:" + newValue);
        if (artifact == null) {
            String errorMsg = String.format("Unable to find appversion information for id : %s", applicationId);
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg);
        }
        try {
            String currentVal = artifact.getAttribute(key);
            if (currentVal == null) {
                artifact.addAttribute(key, newValue);
            } else {
                artifact.setAttribute(key, newValue);
            }
            RegistryService registryService = ServiceHolder.getRegistryService();
            //RegistryService registryService = ServiceReferenceHolder.getInstance().getRegistryService();
            UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
            GovernanceUtils.loadGovernanceArtifacts(userRegistry);
            GenericArtifactManager artifactManager = new GenericArtifactManager(userRegistry, "appversion");
            artifactManager.updateGenericArtifact(artifact);
        } catch (RegistryException e) {
            String errorMsg = "Error while updating the artifact " + applicationId;
            log.error(errorMsg, e);
            throw new AppFactoryException(errorMsg, e);
        }
    }

    /**
     * This method will append the given newValues as values for the key given
     *
     * @param applicationId the ID of the current application
     * @param stage the stage of the current application
     * @param version the version of the current application
     * @param key the attribute key that is been updated
     * @param newValues array of new values for the attribute key
     * @throws AppFactoryException
     */
    public void updateAppVersionRxt(String applicationId, String stage, String version, String key, String[] newValues)
            throws AppFactoryException {
        GenericArtifactImpl artifact = getAppVersionArtifact(applicationId, stage, version);
        log.info("=============== updating rxt =============== key:" + key + " with " + newValues.length + " values");
        if (artifact == null) {
            String errorMsg = String.format("Unable to find appversion information for id : %s", applicationId);
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg);
        }
        try {
            for (String value : newValues) {
                artifact.addAttribute(key, value);
            }

            RegistryService registryService = ServiceHolder.getRegistryService();
            UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
            GovernanceUtils.loadGovernanceArtifacts(userRegistry);
            GenericArtifactManager artifactManager = new GenericArtifactManager(userRegistry, "appversion");
            artifactManager.updateGenericArtifact(artifact);
        } catch (RegistryException e) {
            String errorMsg = "Error while updating the artifact " + applicationId;
            log.error(errorMsg, e);
            throw new AppFactoryException(errorMsg, e);
        }
    }

    /**
     * This method returns the stage of a given application version
     *
     * @param applicationId the ID of the current application
     * @param appVersion the version of the current application
     *
     * @return the stage of the given application version
     * @throws AppFactoryException
     */
    public String getStage(String applicationId, String appVersion) throws AppFactoryException {
        String[] versionPaths = getVersionPaths(applicationId);
        //path to a version is in the structure  .../<appid>/<lifecycle>/1.0.1 )
        if (versionPaths != null) {
            for (String path : versionPaths) {
                String[] s = path.trim().split(RegistryConstants.PATH_SEPARATOR);
                if (appVersion.equals(s[s.length - 1])) {
                    // get the <lifecycle>
                    return s[s.length - 2];
                }
            }
        }
        return null;
    }

    private String[] getVersionPaths(String applicationId) throws AppFactoryException {
        List<String> versionPaths = new ArrayList<String>();
        try {
            RegistryService registryService = ServiceHolder.getRegistryService();
            UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
            // child nodes of this will contains folders for all life cycles (
            // e.g. QA, Dev, Prod)
            Resource application = userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
                    RegistryConstants.PATH_SEPARATOR + applicationId);

            if (application != null && application instanceof Collection) {

                // Contains paths to life cycles (.e.g .../<appid>/dev,
                // .../<appid>/qa , .../<appid>/prod )
                String[] definedLifeCyclePaths = ((Collection) application).getChildren();

                for (String lcPath : definedLifeCyclePaths) {

                    Resource versionsInLCResource = userRegistry.get(lcPath);
                    if (versionsInLCResource != null && versionsInLCResource instanceof Collection) {

                        // contains paths to a versions (e.g.
                        // .../<appid>/<lifecycle>/trunk,
                        // .../<appid>/<lifecycle>/1.0.1 )
                        Collections.addAll(versionPaths, ((Collection) versionsInLCResource).getChildren());
                    }

                }

            }

        } catch (RegistryException e) {
            String errorMsg = String.format("Unable to load the application information for applicaiton id: %s",
                            applicationId);
            log.error(errorMsg, e);
            throw new AppFactoryException(errorMsg, e);
        }
        return versionPaths.toArray(new String[versionPaths.size()]);
    }

    /**
     * @param applicationId the ID of the current application
     * @param stage the stage of the current application
     * @param version the version of the current application
     * @return generic artifact implementation of the artifact that matches the given applicationId, stage and version
     * @throws AppFactoryException
     */
    private GenericArtifactImpl getAppVersionArtifact(String applicationId, String stage, String version)
            throws AppFactoryException {
        GenericArtifactImpl artifact;
        try {

            RegistryService registryService = ServiceHolder.getRegistryService();
            UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
            Resource resource = userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
                    RegistryConstants.PATH_SEPARATOR + applicationId + RegistryConstants.PATH_SEPARATOR +
                    stage + RegistryConstants.PATH_SEPARATOR + version + RegistryConstants.PATH_SEPARATOR +
                    "appversion");
            GovernanceUtils.loadGovernanceArtifacts(userRegistry);
            GenericArtifactManager artifactManager = new GenericArtifactManager(userRegistry, "appversion");
            artifact = (GenericArtifactImpl) artifactManager.getGenericArtifact(resource.getUUID());

        } catch (RegistryException e) {
            String errorMsg = String.format("Unable to load the application information for applicaiton id: %s",
                            applicationId);
            log.error(errorMsg, e);
            throw new AppFactoryException(errorMsg, e);
        }

        return artifact;
    }
}
