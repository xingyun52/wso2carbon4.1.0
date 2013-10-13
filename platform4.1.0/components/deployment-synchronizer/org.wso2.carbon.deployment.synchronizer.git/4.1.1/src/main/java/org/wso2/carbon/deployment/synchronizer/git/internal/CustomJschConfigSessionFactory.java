package org.wso2.carbon.deployment.synchronizer.git.internal;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.wso2.carbon.base.ServerConfiguration;

/**
 * overrides the default org.eclipse.jgit.transport.JschConfigSessionFactory
 */
public class CustomJschConfigSessionFactory extends JschConfigSessionFactory {

    @Override
    protected void configure(OpenSshConfig.Host host, Session session) {
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {

        JSch def = super.createDefaultJSch(fs);
        String keyName = ServerConfiguration.getInstance().
                getFirstProperty(GitDeploymentSynchronizerConstants.SSH_PRIVATE_KEY_NAME);
        String keyPath = ServerConfiguration.getInstance().
                getFirstProperty(GitDeploymentSynchronizerConstants.SSH_PRIVATE_KEY_PATH);

        if(keyName == null || keyName.isEmpty())
            keyName = GitDeploymentSynchronizerConstants.SSH_KEY;

        if(keyPath == null || keyPath.isEmpty())
            keyPath = System.getProperty("user.home") + "/" +GitDeploymentSynchronizerConstants.SSH_KEY_DIRECTORY;

        if(keyPath.endsWith("/"))
            def.addIdentity(keyPath + keyName);
        else
            def.addIdentity(keyPath + "/" + keyName);

        return def;
    }
}
