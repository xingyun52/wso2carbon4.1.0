package org.wso2.carbon.appfactory.utilities.project;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.SystemOutHandler;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.dto.Application;
import org.wso2.carbon.appfactory.core.dto.Version;
import org.wso2.carbon.appfactory.utilities.internal.ServiceReferenceHolder;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifactImpl;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectUtils {

    private static final Log log = LogFactory.getLog(ProjectUtils.class);

    public static void generateCAppArchetype(final String appId, String filePath)
            throws AppFactoryException {
        generateProjectArchetype(appId, filePath, getArchetypeRequest(appId , AppFactoryConstants.FILE_TYPE_CAR));
    }

    public static void generateWebAppArchetype(final String appId, String filePath)
            throws AppFactoryException {
        generateProjectArchetype(appId, filePath, getArchetypeRequest(appId, AppFactoryConstants.FILE_TYPE_WAR));
    }

    public static void generateProjectArchetype(final String appId, String filePath, String archetypeRequest) throws AppFactoryException {

//        Check whether the maven home is set. If not, can not proceed further.
        String MAVEN_HOME;
        if ((MAVEN_HOME = System.getenv("M2_HOME")) == null) {
            if ((MAVEN_HOME = System.getenv("M3_HOME")) == null) {
                String msg = "valid maven installation is not found with M2_HOME or M3_HOME environment variable";
                log.error(msg);
                throw new AppFactoryException(msg);
            }
        }

        File workDir = new File(filePath);
//        Checking whether the app directory exists. If not, the previous process has failed. Hence returning
        if (!workDir.exists()) {
            log.warn(String.format("Work directory for application id : %s does not exist", appId));
            return;
        }

        List<String> goals = new ArrayList<String>();
        goals.add("archetype:generate");

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(workDir);
        request.setShowErrors(true);
        request.setGoals(goals);
        request.setMavenOpts(archetypeRequest);

        InvocationResult result = null;
        try {
            Invoker invoker = new DefaultInvoker();

            InvocationOutputHandler outputHandler = new SystemOutHandler();
            invoker.setErrorHandler(outputHandler);
            invoker.setMavenHome(new File(MAVEN_HOME));
            invoker.setOutputHandler(new InvocationOutputHandler() {
                @Override
                public void consumeLine(String s) {
                    log.info(appId + ":" + s);
                }
            });

            result = invoker.execute(request);
        } catch (MavenInvocationException e) {
            String msg = "Failed to invoke maven archetype generation";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        } finally {
            if (result != null && result.getExitCode() == 0) {
                log.info("Maven archetype generation completed successfully");
            }
        }
    }

    /*TODO: The best way to do this is to read these from a file. Then we have the option of not changing the code when these parameters change*/
    public static String getArchetypeRequest(String appId, String applicationType) throws AppFactoryException {

//        We read all the configuration parameters from the appfactory configuration.
        AppFactoryConfiguration configuration = ServiceReferenceHolder.getInstance().getAppFactoryConfiguration();
        String[] archetypeConfigProps = null;
        if(AppFactoryConstants.FILE_TYPE_CAR.equals(applicationType)) {
            archetypeConfigProps  = configuration.getProperties(AppFactoryConstants.CAPP_MAVEN_ARCHETYPE_PROP_NAME);
        } else if (AppFactoryConstants.FILE_TYPE_WAR.equals(applicationType)) {
            archetypeConfigProps  = configuration.getProperties(AppFactoryConstants.WEBAPP_MAVEN_ARCHETYPE_PROP_NAME);
        }

        if(archetypeConfigProps == null || archetypeConfigProps.length == 0){
            String msg = "Could not find the maven archetype configuration";
            log.error(msg);
            throw new AppFactoryException(msg);
        }else if(archetypeConfigProps.length > 1){
            log.warn("Multiple configurations have been found.");
        }

        String value = archetypeConfigProps[0];

        String replacement = " -DartifactId=" + appId;
        if (value.contains("-DartifactId=")) {
            String currentArtifactId = value.substring(value.indexOf("-DartifactId=")).split(" ")[0];
            value = value.replace(currentArtifactId, replacement);
        } else {
            value = value + replacement;
        }
        return value;

/*
//Commenting out the rest because we read the props from the appfactory config

        StringBuilder optsBuilder = new StringBuilder();

        optsBuilder.append(" -DarchetypeGroupId=org.wso2.carbon.appfactory.maven.archetype");
        optsBuilder.append(" -DarchetypeArtifactId=af-archetype");
        optsBuilder.append(" -DarchetypeVersion=1.0.0");
        optsBuilder.append(" -DgroupId=org.wso2.af");
        optsBuilder.append(" -Dversion=1.0.1");
        optsBuilder.append(" -DinteractiveMode=false");
        optsBuilder.append(" -DarchetypeCatalog=local");
        optsBuilder.append(" -DartifactId=").append(appid);


        return optsBuilder.toString();*/
    }

    /**
     * Returns the type of the application given the application Id
     * 
     * @param applicationId
     *            Id of the application
     * @return the type
     * @throws AppFactoryException
     *             if an error occurs.
     */
    public static String getApplicationType(String applicationId) throws AppFactoryException {

        GenericArtifactImpl artifact = getApplicationArtifact(applicationId);

        if (artifact == null) {
            String errorMsg =
                              String.format("Unable to find applcation information for id : %s",
                                            applicationId);
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg);

        }

        try {
            return artifact.getAttribute("application_type");
        } catch (RegistryException e) {
            String errorMsg =
                              String.format("Unable to find the application type for applicaiton id: %s",
                                            applicationId);
            log.error(errorMsg, e);
            throw new AppFactoryException(errorMsg, e);
        }
    }

    /**
     * Provides information about an application.
     * 
     * @param applicationId
     *            id of the application
     * @return {@link Application}
     * @throws AppFactoryException
     *             if an error occurs
     */
    public static Application getApplicationInfo(String applicationId) throws AppFactoryException {

        GenericArtifactImpl artifact = getApplicationArtifact(applicationId);

        if (artifact == null) {
            String errorMsg =
                              String.format("Unable to find applcation information for id : %s",
                                            applicationId);
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg);

        }
        Application appInfo = null;

        try {
            appInfo =
                      new Application(artifact.getAttribute("application_key"),
                                      artifact.getAttribute("application_name"),
                                      artifact.getAttribute("application_type"),
                                      artifact.getAttribute("application_description"));
        } catch (GovernanceException e) {
            String errorMsg =
                              String.format("Unable to extract information for application id : %s",
                                            applicationId);
            log.error(errorMsg);
            throw new AppFactoryException(errorMsg);
        }

        return appInfo;
    }

    /**
     * Returns all available versions of a application
     * 
     * @param applicationId
     *            Id of the application
     * @return an Array of {@link Version}
     * @throws AppFactoryException
     *             if an error occurres
     */
    public static Version[] getVersions(String applicationId) throws AppFactoryException {
        List<Version> versions = new ArrayList<Version>();
        try {
            RegistryService registryService =
                                              ServiceReferenceHolder.getInstance()
                                                                    .getRegistryService();
            UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
            // child nodes of this will contains folders for all life cycles (
            // e.g. QA, Dev, Prod)
            Resource application =
                                   userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
                                                    File.separator + applicationId);

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
                        String[] versionPaths = ((Collection) versionsInLCResource).getChildren();

                        for (String versionPath : versionPaths) {
                            // extract the name of the resource ( which will be
                            // the version id)
                            String versionId = RegistryUtils.getResourceName(versionPath);
                            Version version = new Version(versionId);
                            versions.add(version);
                        }
                    }

                }

            }

        } catch (RegistryException e) {
            String errorMsg =
                              String.format("Unable to load the application information for applicaiton id: %s",
                                            applicationId);
            log.error(errorMsg, e);
            throw new AppFactoryException(errorMsg, e);
        }

        return versions.toArray(new Version[versions.size()]);
    }

    /**
     * A Util method to load an Application artifact from the registry.
     * 
     * @param applicationId
     *            the application Id
     * @return a {@link GenericArtifactImpl} representing the application or
     *         null if application (by the id is not in registry)
     * @throws AppFactoryException
     *             if an error occurs.
     */
    private static GenericArtifactImpl getApplicationArtifact(String applicationId)
                                                                                   throws AppFactoryException {
        GenericArtifactImpl artifact = null;
        try {

            RegistryService registryService =
                                              ServiceReferenceHolder.getInstance()
                                                                    .getRegistryService();
            UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
            Resource resource =
                                userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
                                                 File.separator + applicationId + File.separator +
                                                 "appinfo");
            GovernanceUtils.loadGovernanceArtifacts(userRegistry);
            GenericArtifactManager artifactManager =
                                                     new GenericArtifactManager(userRegistry,
                                                                                "application");
            // GenericArtifact artifact =
            // artifactManager.getGenericArtifact(resource.getUUID());
            artifact = (GenericArtifactImpl) artifactManager.getGenericArtifact(resource.getUUID());

        } catch (RegistryException e) {
            String errorMsg =
                              String.format("Unable to load the application information for applicaiton id: %s",
                                            applicationId);
            log.error(errorMsg, e);
            throw new AppFactoryException(errorMsg, e);
        }

        return artifact;
    }
}
