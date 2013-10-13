package org.wso2.carbon.governance.registry.extensions.executors.utils;

import org.wso2.carbon.governance.api.util.GovernanceConstants;
import org.wso2.carbon.registry.core.RegistryConstants;

public class ExecutorConstants {

    /**
     * Defines the name of the property where we keep the lifecycle name
     */
    public static final String REGISTRY_LC_NAME = "registry.LC.name";

    /**
     * Defines the target environment key
     */
    public static final String TARGET_ENVIRONMENT = "targetEnvironment";

    /**
     * Defines the current state key
     */
    public static final String CURRENT_ENVIRONMENT = "currentEnvironment";

    /**
     * Defines the service media type key
     */
    public static final String SERVICE_MEDIA_TYPE_KEY = "service.mediatype";

    /**
     * defines the endpoint media type
     */
    public static final String WSDL_MEDIA_TYPE = GovernanceConstants.WSDL_MEDIA_TYPE;

    /**
     * defines the endpoint media type
     */
    public static final String SCHEMA_MEDIA_TYPE = GovernanceConstants.SCHEMA_MEDIA_TYPE;

    /**
     * defines the endpoint media type
     */
    public static final String ENDPOINT_MEDIA_TYPE = GovernanceConstants.ENDPOINT_MEDIA_TYPE;

    /**
     * defines the policy media type
     */
    public static final String POLICY_MEDIA_TYPE = GovernanceConstants.POLICY_XML_MEDIA_TYPE;


    /**
     * Defines the copy comments key
     */
    public static final String COPY_COMMENTS = "copyComments";

    /**
     * Defines the copy tags key
     */
    public static final String COPY_TAGS = "copyTags";

    /**
     * Defines the copy ratings key
     */
    public static final String COPY_RATINGS = "copyRatings";

    /**
     * Defines the copy  dependencies key
     */
    public static final String COPY_DEPENDENCIES = "copyDependencies";

    /**
     * Defines the copy all associations key
     */
    public static final String COPY_ASSOCIATIONS = "copyAssociations";

    /**
     * Defines the override key. This only works for services
     */
    public static final String OVERRIDE = "override";

    /**
     * Defines the resource name key
     */
    public static final String RESOURCE_NAME = "{@resourceName}";

    /**
     * Defines the resource path key
     */
    public static final String RESOURCE_PATH = "{@resourcePath}";

    /**
     * Defines the resource version key
     */
    public static final String RESOURCE_VERSION = "{@version}";

    /**
     * Defines the xpath expression that is used to find import elements
     */
    public static final String IMPORT_XPATH_STRING = "//x:import";

    /**
     * Defines the xpath expression that is used to find the embedded schema
     */
    public static final String XSD_XPATH_STRING = "//x:schema";
}