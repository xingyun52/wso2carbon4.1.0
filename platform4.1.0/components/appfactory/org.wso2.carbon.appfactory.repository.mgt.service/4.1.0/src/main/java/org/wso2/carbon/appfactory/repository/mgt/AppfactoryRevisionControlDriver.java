package org.wso2.carbon.appfactory.repository.mgt;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.common.util.AppFactoryUtil;
import org.wso2.carbon.appfactory.core.RevisionControlDriver;
import org.wso2.carbon.appfactory.core.RevisionControlDriverListener;
import org.wso2.carbon.appfactory.repository.mgt.client.AppfactoryRepositoryClient;
import org.wso2.carbon.appfactory.repository.mgt.internal.Util;

/**
 *
 */
public class AppfactoryRevisionControlDriver  implements RevisionControlDriver
{
    private static final Log log = LogFactory.getLog(AppfactoryRevisionControlDriver.class);
    private RepositoryManager repositoryManager;
    public AppfactoryRevisionControlDriver()
    {
        repositoryManager = new RepositoryManager();
    }

    public void getSource(String applicationId, String version, String revision, RevisionControlDriverListener listener)
            throws AppFactoryException
    {
        String checkoutUrl = null;
        try
        {
            checkoutUrl = repositoryManager.getAppRepositoryURL(applicationId, "svn");
        }
        catch(RepositoryMgtException e)
        {
            String msg = "Error while getting repository url";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }
        if("trunk".equals(version))
            checkoutUrl = (new StringBuilder()).append(checkoutUrl).append("/").append("trunk").toString();
        else
            checkoutUrl = (new StringBuilder()).append(checkoutUrl).append("/").append("branches").append("/").append(version).toString();
        File workingDirectory = AppFactoryUtil.getApplicationWorkDirectory(applicationId, version, revision);
        workingDirectory.deleteOnExit();
        if(!workingDirectory.mkdir())
        {
            String msg = "Error while creating working directory";
            log.error(msg);
            throw new AppFactoryException(msg);
        }
        AppfactoryRepositoryClient client = null;
        try
        {
            client = repositoryManager.getRepositoryClient("svn");
            client.init(Util.getConfiguration().getFirstProperty("AdminUserName"), Util.getConfiguration().getFirstProperty("AdminPassword"));
            client.checkOut(checkoutUrl, workingDirectory, revision);
            client.close();
        }
        catch(RepositoryMgtException e)
        {
            String msg = "Error while checking out repository ";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }
        listener.onGetSourceCompleted(applicationId, version, revision);
    }


}