package org.wso2.carbon.deployment.synchronizer.git.internal;

import static org.wso2.carbon.deployment.synchronizer.DeploymentSynchronizerConstants.DEPLOYMENT_SYNCHRONIZER;

/**
 * Git based DeploymentSynchronizer constansts
 */
public class GitDeploymentSynchronizerConstants {

    //Git repo url related constansts
    //public static final String GITHUB_HTTP_REPO_URL_PREFIX = "http://github.com";
    public static final String GIT_HTTP_REPO_URL_PREFIX = "http://";
    //public static final String GITHUB_HTTPS_REPO_URL_PREFIX = "https://github.com";
    public static final String GIT_HTTPS_REPO_URL_PREFIX = "https://";
    public static final String GITHUB_READ_ONLY_REPO_URL_PREFIX = "git://github.com";
    public static final String GIT_REPO_SSH_URL_PREFIX = "ssh://";
    public static final String GIT_REPO_SSH_URL_SUBSTRING = "@";

    //SSH related constants
    public static final String SSH_KEY_DIRECTORY = ".ssh";
    public static final String SSH_KEY = "wso2";

    //super tenant Id
    public static final int SUPER_TENANT_ID = -1234;

    //ServerKey property name from carbon.xml, for the cartridge short name --> not used. CARTRIDGE_ALIAS is used instead.
    //public static final String SERVER_KEY = "ServerKey";

    //EPR for the repository Information Service
    public static final String REPO_INFO_SERVICE_EPR = "RepoInfoServiceEpr";

    //CartridgeAlias property name from carbon.xml
    public static final String CARTRIDGE_ALIAS = "CartridgeAlias";

    //key name and path for ssh based authentication
    public static final String SSH_PRIVATE_KEY_NAME = DEPLOYMENT_SYNCHRONIZER + ".SshPrivateKeyName";
    public static final String SSH_PRIVATE_KEY_PATH = DEPLOYMENT_SYNCHRONIZER + ".SshPrivateKeyPath";

    //regular expressions for extracting username and password form json string
    public static final String USERNAME_REGEX = "username:(.*?),";
    public static final String PASSWORD_REGEX = "password:(.*?)}";

}
