package org.wso2.carbon.deployment.synchronizer.git;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.deployment.synchronizer.ArtifactRepository;
import org.wso2.carbon.deployment.synchronizer.DeploymentSynchronizerException;
import org.wso2.carbon.deployment.synchronizer.git.internal.CustomJschConfigSessionFactory;
import org.wso2.carbon.deployment.synchronizer.git.internal.GitDeploymentSynchronizerConstants;
import org.wso2.carbon.deployment.synchronizer.git.internal.GitRepositoryContext;
import org.wso2.carbon.deployment.synchronizer.git.internal.RepositoryInformationServiceClient;
import org.wso2.carbon.deployment.synchronizer.git.util.Utilities;
import org.wso2.carbon.deployment.synchronizer.internal.DeploymentSynchronizerConstants;
import org.wso2.carbon.deployment.synchronizer.internal.util.RepositoryConfigParameter;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Git based artifact repository
 */

public class GitBasedArtifactRepository implements ArtifactRepository {

    private static final Log log = LogFactory.getLog(GitBasedArtifactRepository.class);

    //Map to keep track of git context per tenant (remote urls, jgit git objects, etc.)
    private ConcurrentHashMap<String, GitRepositoryContext> tenantGitRepoContext;

    /*
    * Constructor
    * */
    public GitBasedArtifactRepository () {

        tenantGitRepoContext = new ConcurrentHashMap<String, GitRepositoryContext>();
    }

    /**
     * called at tenant load to do initialization of the tenant
     *
     * @param tenantId id of the tenant
     *
     * @throws DeploymentSynchronizerException
     */
    public void init (int tenantId) throws DeploymentSynchronizerException {

        initGitContext(tenantId);
    }

    /**
     * initializes and populates the git context with relevant data
     *
     * @param tenantId id of the tenant
     *
     * @throws DeploymentSynchronizerException
     */
    synchronized private void initGitContext (int tenantId) throws DeploymentSynchronizerException {

        if (tenantId == GitDeploymentSynchronizerConstants.SUPER_TENANT_ID)
            return;

        String gitLocalRepoPath = MultitenantUtils.getAxis2RepositoryPath(tenantId);
        if(tenantGitRepoContext.containsKey(gitLocalRepoPath)) {
            log.info("Cached git repository context detected for tenant " + tenantId);
            return;
        }

        GitRepositoryContext gitRepoCtx = new GitRepositoryContext();
        gitRepoCtx.setTenantId(tenantId);
        gitRepoCtx.setGitLocalRepoPath(gitLocalRepoPath);

        String cartridgeShortName = ServerConfiguration.getInstance().
                getFirstProperty(GitDeploymentSynchronizerConstants.CARTRIDGE_ALIAS);

        String gitRemoteRepoUrl = null;
        RepositoryInformationServiceClient repoInfoServiceClient = null;

        try {
            repoInfoServiceClient = new RepositoryInformationServiceClient(ServerConfiguration.getInstance().
                    getFirstProperty(GitDeploymentSynchronizerConstants.REPO_INFO_SERVICE_EPR));
            gitRemoteRepoUrl = repoInfoServiceClient.getGitRepositoryUrl(tenantId, cartridgeShortName);

        }  catch (AxisFault axisFault) {
            String errorMsg = "Repository Information Service initialization failed";
            log.error(errorMsg);
            throw new DeploymentSynchronizerException(errorMsg);

        } catch (Exception e) {
            String errorMsg = "Repository Information Service invocation failed";
            log.error(errorMsg);
            throw new DeploymentSynchronizerException(errorMsg);
        }
        if(gitRemoteRepoUrl == null) {
            String errorMsg = "Repository url null for tenant " + tenantId + ", cartridge type " + cartridgeShortName;
            log.error(errorMsg);
            throw new DeploymentSynchronizerException(errorMsg);
        }

        gitRepoCtx.setRepoInfoServiceClient(repoInfoServiceClient);
        gitRepoCtx.setGitRemoteRepoUrl(gitRemoteRepoUrl);

        if(isKeyBasedAuthentication(gitRemoteRepoUrl, tenantId)) {
            gitRepoCtx.setKeyBasedAuthentication(true);
            initSSHAuthentication();
        }
        else
            gitRepoCtx.setKeyBasedAuthentication(false);

        FileRepository localRepo = null;
        try {
            localRepo = new FileRepository(new File(gitLocalRepoPath + "/.git"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        gitRepoCtx.setLocalRepo(localRepo);
        gitRepoCtx.setGit(new Git(localRepo));
        gitRepoCtx.setCloneExists(false);

        cacheGitRepoContext(gitLocalRepoPath, gitRepoCtx);
    }

    /**
     * Checks if key based authentication (SSH) is required
     *
     * @param url git repository url for the tenant
     * @param tenantId id of the tenant
     *
     * @return true if SSH authentication is required, else false
     *
     * @throws DeploymentSynchronizerException
     */
    private boolean isKeyBasedAuthentication(String url, int tenantId) throws DeploymentSynchronizerException {

        if (url.startsWith(GitDeploymentSynchronizerConstants.GIT_HTTP_REPO_URL_PREFIX) ||
                url.startsWith(GitDeploymentSynchronizerConstants.GIT_HTTPS_REPO_URL_PREFIX)) {//http or https url
            // authentication with username and password, not key based
            return false;
        }

        else if (url.startsWith(GitDeploymentSynchronizerConstants.GITHUB_READ_ONLY_REPO_URL_PREFIX)) { //github read-only repo url
            // no authentication required
            return false;
        }

        else if (url.startsWith(GitDeploymentSynchronizerConstants.GIT_REPO_SSH_URL_PREFIX) ||
                url.contains(GitDeploymentSynchronizerConstants.GIT_REPO_SSH_URL_SUBSTRING)) { //other repo, needs ssh authentication
            // key based authentication
            return true;
        }

        else {
            log.error("Invalid git URL provided for tenant " + tenantId);
            throw new DeploymentSynchronizerException("Invalid git URL provided for tenant " + tenantId);
        }
    }

    /**
     * Initializes SSH authentication
     */
    private void initSSHAuthentication () {

        SshSessionFactory.setInstance(new CustomJschConfigSessionFactory());
    }

    /**
     * Caches GitRepositoryContext against tenant repository path
     *
     * @param tenantLocalRepoPath tenant repository path
     * @param gitRepoCtx GitRepositoryContext instance for tenant
     */
    private void cacheGitRepoContext(String tenantLocalRepoPath, GitRepositoryContext gitRepoCtx) {

        tenantGitRepoContext.put(tenantLocalRepoPath, gitRepoCtx);
    }

    /**
     * Retrieve cached GitRepositoryContext relevant to the tenant's local repo path
     *
     * @param tenantLocalRepoPath tenant's local repository path
     *
     * @return corresponding GitRepositoryContext instance for the
     * tenant's local repo if available, else null
     */
    private GitRepositoryContext retrieveCachedGitContext (String tenantLocalRepoPath) {

        return tenantGitRepoContext.get(tenantLocalRepoPath);
    }

    /**
     * Commits any changes in the local repository to the relevant remote repository
     *
     * @param localRepoPath tenant's local repository path
     *
     * @return
     *
     * @throws DeploymentSynchronizerException
     */
    public boolean commit(String localRepoPath) throws DeploymentSynchronizerException {

        GitRepositoryContext gitRepoCtx = retrieveCachedGitContext(localRepoPath);
        if (gitRepoCtx == null) {
            if(log.isDebugEnabled())
                log.debug("No git repository context information found for deployment synchronizer at " + localRepoPath);

            return false;
        }

        Git git = gitRepoCtx.getGit();
        StatusCommand statusCmd = git.status();
        Status status = null;
        try {
            status = statusCmd.call();

        } catch (GitAPIException e) {
            log.error("Git status operation for tenant " + gitRepoCtx.getTenantId() + " failed, ", e);
            e.printStackTrace();
            return false;
        }

        if(status.isClean()) {//no changes, nothing to commit
            if(log.isDebugEnabled())
                log.debug("No changes detected in the local repository at " + localRepoPath);
            return false;
        }

        addArtifacts(gitRepoCtx, getNewArtifacts(status));
        addArtifacts(gitRepoCtx, getModifiedArtifacts(status));
        removeArtifacts(gitRepoCtx, getRemovedArtifacts(status));

        commitToLocalRepo(gitRepoCtx);
        pushToRemoteRepo(gitRepoCtx);

        return true;
    }

    /**
     * Returns the newly added artifact set relevant to the current status of the repository
     *
     * @param status git status
     *
     * @return artifact names set
     */
    private Set<String> getNewArtifacts (Status status) {

        return status.getUntracked();
    }

    /**
     * Returns the removed (undeployed) artifact set relevant to the current status of the repository
     *
     * @param status git status
     *
     * @return artifact names set
     */
    private Set<String> getRemovedArtifacts (Status status) {

        return status.getMissing();
    }

    /**
     * Return the modified artifacts set relevant to the current status of the repository
     *
     * @param status git status
     *
     * @return artifact names set
     */
    private Set<String> getModifiedArtifacts (Status status) {

        return status.getModified();
    }

    /**
     * Adds the artifacts to the local staging area
     *
     * @param gitRepoCtx GitRepositoryContext instance
     * @param artifacts set of artifacts
     */
    private void addArtifacts (GitRepositoryContext gitRepoCtx, Set<String> artifacts) {

        if(artifacts.isEmpty())
            return;

        AddCommand addCmd = gitRepoCtx.getGit().add();
        Iterator<String> it = artifacts.iterator();
        while(it.hasNext())
            addCmd.addFilepattern(it.next());

        try {
            addCmd.call();

        } catch (GitAPIException e) {
            log.error("Adding artifact to the local repository at " + gitRepoCtx.getGitLocalRepoPath() + "failed", e);
            e.printStackTrace();
        }
    }

    /**
     * Removes the set of artifacts from local repo
     *
     * @param gitRepoCtx GitRepositoryContext instance
     * @param artifacts Set of artifact names to remove
     */
    private void removeArtifacts (GitRepositoryContext gitRepoCtx, Set<String> artifacts) {

        if(artifacts.isEmpty())
            return;

        RmCommand rmCmd = gitRepoCtx.getGit().rm();
        Iterator<String> it = artifacts.iterator();
        while (it.hasNext()) {
            rmCmd.addFilepattern(it.next());
        }

        try {
            rmCmd.call();

        } catch (GitAPIException e) {
            log.error("Removing artifact from the local repository at " + gitRepoCtx.getGitLocalRepoPath() + "failed", e);
            e.printStackTrace();
        }
    }

    /**
     * Commits changes for a tenant to relevant the local repository
     *
     * @param gitRepoCtx GitRepositoryContext instance for the tenant
     */
    private void commitToLocalRepo (GitRepositoryContext gitRepoCtx) {

        CommitCommand commitCmd = gitRepoCtx.getGit().commit();
        commitCmd.setMessage("tenant " + gitRepoCtx.getTenantId() + "'s artifacts committed to local repo at " +
                gitRepoCtx.getGitLocalRepoPath());

        try {
            commitCmd.call();

        } catch (GitAPIException e) {
            log.error("Committing artifacts to local repository failed for tenant " + gitRepoCtx.getTenantId(), e);
            e.printStackTrace();
        }
    }

    /**
     * Pushes the artifacts of the tenant to relevant remote repository
     *
     * @param gitRepoCtx GitRepositoryContext instance for the tenant
     */
    private void pushToRemoteRepo(GitRepositoryContext gitRepoCtx) {

        PushCommand pushCmd = gitRepoCtx.getGit().push();
        if(!gitRepoCtx.getKeyBasedAuthentication()) {
            UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);
            if (credentialsProvider != null)
                pushCmd.setCredentialsProvider(credentialsProvider);
        }

        try {
            pushCmd.call();

        } catch (GitAPIException e) {
            log.error("Pushing artifacts to remote repository failed for tenant " + gitRepoCtx.getTenantId(), e);
            e.printStackTrace();
        }
    }

    /**
     * Method inherited from ArtifactRepository for initializing checkout
     *
     * @param localRepoPath local repository path of the tenant
     *
     * @return true if success, else false
     *
     * @throws DeploymentSynchronizerException
     */
    public boolean checkout (String localRepoPath) throws DeploymentSynchronizerException {

        GitRepositoryContext gitRepoCtx = retrieveCachedGitContext(localRepoPath);
        if(gitRepoCtx == null) { //to handle super tenant scenario
            if(log.isDebugEnabled())
                log.debug("No git repository context information found for deployment synchronizer at " + localRepoPath);

            return true;
        }

        if(gitRepoCtx.getTenantId() == GitDeploymentSynchronizerConstants.SUPER_TENANT_ID)
            return true;  //Super Tenant is inactive
        if(!gitRepoCtx.cloneExists())
            cloneRepository(gitRepoCtx);

        return pullArtifacts(gitRepoCtx);
    }

    /**
     * Pulling if any updates are available in the remote git repository. If basic authentication is required,
     * will call 'RepositoryInformationService' for credentials.
     *
     * @param gitRepoCtx GitRepositoryContext instance for tenant
     *
     * @return true if success, else false
     */
    private boolean pullArtifacts (GitRepositoryContext gitRepoCtx) {

        PullCommand pullCmd = gitRepoCtx.getGit().pull();

        if(!gitRepoCtx.getKeyBasedAuthentication()) {
            UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);
            if (credentialsProvider != null)
                pullCmd.setCredentialsProvider(credentialsProvider);
        }

        try {
            pullCmd.call().getMergeResult().getFailingPaths();

        } catch (InvalidConfigurationException e) {
            log.error("Git pull unsuccessful for tenant " + gitRepoCtx.getTenantId() + ", invalid configuration", e);
            return false;

        } catch (TransportException e) {
            log.error("Accessing remote git repository " + gitRepoCtx.getGitRemoteRepoUrl() + " failed for tenant " + gitRepoCtx.getTenantId(), e);
            e.printStackTrace();
            return false;

        } catch (CheckoutConflictException e) { //TODO: handle conflict efficiently. Currently the whole directory is deleted and re-cloned
            log.warn("Git pull for the path " + e.getConflictingPaths().toString() + " failed due to conflicts");
            Utilities.deleteFolderStructure(new File(gitRepoCtx.getGitLocalRepoPath()));
            cloneRepository(gitRepoCtx);
            return true;

        } catch (GitAPIException e) {
            log.error("Git pull operation for tenant " + gitRepoCtx.getTenantId() + " failed", e);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Clones the remote repository to the local one. If basic authentication is required,
     * will call 'RepositoryInformationService' for credentials.
     *
     * @param gitRepoCtx GitRepositoryContext for the tenant
     */
    private void cloneRepository (GitRepositoryContext gitRepoCtx) { //should happen only at the beginning

        File gitRepoDir = new File(gitRepoCtx.getGitLocalRepoPath());
        if (gitRepoDir.exists()) {
            if(isValidGitRepo(gitRepoCtx)) { //check if a this is a valid git repo
                log.info("Existing git repository detected for tenant " + gitRepoCtx.getTenantId() + ", no clone required");
                gitRepoCtx.setCloneExists(true);
                return;
            }
            else {
                if(log.isDebugEnabled())
                    log.debug("Repository for tenant " + gitRepoCtx.getTenantId() + " is not a valid git repo");
                Utilities.deleteFolderStructure(gitRepoDir); //if not a valid git repo but non-empty, delete it (else the clone will not work)
            }
        }

        CloneCommand cloneCmd =  gitRepoCtx.getGit().cloneRepository().
                        setURI(gitRepoCtx.getGitRemoteRepoUrl()).
                        setDirectory(gitRepoDir);

        if(!gitRepoCtx.getKeyBasedAuthentication()) {
            UsernamePasswordCredentialsProvider credentialsProvider = createCredentialsProvider(gitRepoCtx);
            if (credentialsProvider != null)
                cloneCmd.setCredentialsProvider(credentialsProvider);
        }

        try {
            cloneCmd.call();
            log.info("Git clone operation for tenant " + gitRepoCtx.getTenantId() + " successful");
            gitRepoCtx.setCloneExists(true);

        } catch (TransportException e) {
            log.error("Accessing remote git repository failed for tenant " + gitRepoCtx.getTenantId(), e);
            e.printStackTrace();

        } catch (GitAPIException e) {
            log.error("Git clone operation for tenant " + gitRepoCtx.getTenantId() + " failed", e);
            e.printStackTrace();
        }
    }

    /**
     * Queries the RepositoryInformationService to obtain credentials for the tenant id + cartridge type
     * and creates a UsernamePasswordCredentialsProvider from a valid username and a password
     *
     * @param gitRepoCtx GitRepositoryContext instance
     *
     * @return UsernamePasswordCredentialsProvider instance or null if service invocation failed or
     * username/password is not valid
     */
    private UsernamePasswordCredentialsProvider createCredentialsProvider (GitRepositoryContext gitRepoCtx) {

        String cartridgeShortName = ServerConfiguration.getInstance().
                getFirstProperty(GitDeploymentSynchronizerConstants.CARTRIDGE_ALIAS);

        String repoInfoJsonString = null;
        try {
            repoInfoJsonString = gitRepoCtx.getRepoInfoServiceClient().
                    getJsonRepositoryInformation(gitRepoCtx.getTenantId(), cartridgeShortName);

        } catch (Exception e) {
            log.error("Git json repository information query failed", e);
            return null;
        }

        String userName = getUserName(repoInfoJsonString);
        String password = getPassword(repoInfoJsonString);
        if (!userName.isEmpty() || !password.isEmpty()) {
            return new UsernamePasswordCredentialsProvider(userName, password);
        }

        return null;
    }

    /**
     * Checks if an existing local repository is a valid git repository
     *
     * @param gitRepoCtx GitRepositoryContext instance
     *
     * @return true if a valid git repo, else false
     */
    private boolean isValidGitRepo (GitRepositoryContext gitRepoCtx) {

        for (Ref ref : gitRepoCtx.getLocalRepo().getAllRefs().values()) { //check if has been previously cloned successfully, not empty
            if (ref.getObjectId() == null)
                continue;
            return true;
        }

        return false;
    }

    /**
     * Calls a utility method to extract the username from a json string
     *
     * @param repoInfoJsonString json format string
     *
     * @return username if exists, else an empty String
     */
    private String getUserName (String repoInfoJsonString) {
        return Utilities.getMatch(repoInfoJsonString,
                GitDeploymentSynchronizerConstants.USERNAME_REGEX, 1);
    }

    /**
     * Calls a utility method to extract the password from a json string
     *
     * @param repoInfoJsonString json format string
     *
     * @return password if exists, else an empty String
     */
    private String getPassword (String repoInfoJsonString) {
         return Utilities.getMatch(repoInfoJsonString,
                 GitDeploymentSynchronizerConstants.PASSWORD_REGEX, 1);
    }

    public void initAutoCheckout(boolean b) throws DeploymentSynchronizerException {

    }

    public void cleanupAutoCheckout() {

    }

    public String getRepositoryType() {

        return DeploymentSynchronizerConstants.REPOSITORY_TYPE_GIT;
    }

    public List<RepositoryConfigParameter> getParameters() {

        return null;
    }

    public boolean checkout(String filePath, int depth) throws DeploymentSynchronizerException {

        GitRepositoryContext gitRepoCtx = retrieveCachedGitContext(filePath);
        if(gitRepoCtx == null) {
            if(log.isDebugEnabled())
                log.debug("No git repository context information found for deployment synchronizer at " + filePath);

            return false;
        }
        if(gitRepoCtx.getTenantId() == GitDeploymentSynchronizerConstants.SUPER_TENANT_ID)
            return true; //Super Tenant is inactive
        if(gitRepoCtx.cloneExists())
            return pullArtifacts(gitRepoCtx);

        return false;
    }

    public boolean update(String rootPath, String filePath, int depth) throws DeploymentSynchronizerException {

        GitRepositoryContext gitRepoCtx = retrieveCachedGitContext(filePath);
        if(gitRepoCtx == null) {
            if(log.isDebugEnabled())
                log.debug("No git repository context information found for deployment synchonizer at " + filePath);

            return false;
        }
        if(gitRepoCtx.getTenantId() == GitDeploymentSynchronizerConstants.SUPER_TENANT_ID)
            return true; //Super Tenant is inactive
        if(gitRepoCtx.cloneExists())
            return pullArtifacts(gitRepoCtx);

        return false;
    }

    @Override
    public ArtifactRepository clone() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
