package org.wso2.carbon.deployment.synchronizer.subversion;

import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.SVNUrl;
import org.wso2.carbon.deployment.synchronizer.TenantRepositoryContext;
import org.wso2.carbon.deployment.synchronizer.internal.util.DeploymentSynchronizerConfiguration;

/**
 * Stores SVN related details of a given tenant. One per each tenant
 *
 */
public class TenantSVNRepositoryContext extends TenantRepositoryContext {

    private SVNUrl svnUrl;
    private ISVNClientAdapter svnClient;

    private DeploymentSynchronizerConfiguration conf;

    private boolean ignoreExternals = true;
    private boolean forceUpdate = true;

    public SVNUrl getSvnUrl() {
        return svnUrl;
    }

    public void setSvnUrl(SVNUrl svnUrl) {
        this.svnUrl = svnUrl;
    }

    public ISVNClientAdapter getSvnClient() {
        return svnClient;
    }

    public void setSvnClient(ISVNClientAdapter svnClient) {
        this.svnClient = svnClient;
    }

    public DeploymentSynchronizerConfiguration getConf() {
        return conf;
    }

    public void setConf(DeploymentSynchronizerConfiguration conf) {
        this.conf = conf;
    }

    public boolean isIgnoreExternals() {
        return ignoreExternals;
    }

    public void setIgnoreExternals(boolean ignoreExternals) {
        this.ignoreExternals = ignoreExternals;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

}
