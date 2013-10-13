package org.wso2.carbon.adc.mgt.utils;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.mgt.dao.Repository;
import org.wso2.carbon.adc.mgt.service.ApplicationManagementService;
import org.wso2.carbon.adc.mgt.service.RepositoryInfoBean;
import org.wso2.carbon.utils.CarbonUtils;

public class RepositoryCreator implements Runnable {

	private static final Log log = LogFactory.getLog(RepositoryCreator.class);
	private RepositoryInfoBean repoInfoBean;

	public RepositoryCreator(RepositoryInfoBean repoInfoBean) {
	    this.repoInfoBean = repoInfoBean;
    }

	@Override
	public void run() {

		if (repoInfoBean != null) {
			try {
				createRepository(repoInfoBean.getCartridgeAlias(), repoInfoBean.getTenantDomain(),
				                 repoInfoBean.getUserName());
				createGitFolderStructure(repoInfoBean.getTenantDomain(),
				                         repoInfoBean.getCartridgeAlias(),
				                         repoInfoBean.getDirArray());

			} catch (Exception e) {
				log.error(e);
			}
		}
	}

	private Repository createRepository(String cartridgeName, String tenantDomain, String userName)
	                                                                                               throws Exception {

		Repository repository = new Repository();
		String repoName = tenantDomain + "/" + cartridgeName; // removed .git
															  // part
		String repoUserName = userName + "@" + tenantDomain;

		Process proc;
		try {

			String command =
			                 CarbonUtils.getCarbonHome() + File.separator + "bin" + File.separator +
			                         "manage-git-repo.sh " + "create " + repoUserName + " " +
			                         tenantDomain + " " + cartridgeName + " " +
			                         System.getProperty(CartridgeConstants.REPO_NOTIFICATION_URL) +
			                         " " + System.getProperty(CartridgeConstants.GIT_HOST_NAME) +
			                         " /";
			proc = Runtime.getRuntime().exec(command);
			log.info("executing manage-git-repo script..... command :" + command);
			proc.waitFor();
			log.info(" Repo is created ..... for user: " + userName + ", tenantName: " +
			         tenantDomain + " ");
			repository.setRepoName("git@" + System.getProperty(CartridgeConstants.GIT_HOST_NAME) +
			                       ":" + repoName);
		} catch (Exception e) {
			log.error(" Exception is occurred when executing manage-git-repo script. Reason :" +
			          e.getMessage());
			handleException(e.getMessage(), e);
		}

		return repository;

	}
	
	private void createGitFolderStructure(String tenantDomain, String cartridgeName,
	  	                                            String[] dirArray) throws Exception {

	  		log.info("In create Git folder structure...!");

	  		StringBuffer dirBuffer = new StringBuffer();
	  		for (String dir : dirArray) {
	  			dirBuffer.append(dir).append(" ");
	  		}

	  		Process proc;
	  		try {
	  			String command =
	  			                 CarbonUtils.getCarbonHome() + File.separator + "bin" + File.separator +
	  			                         "git-folder-structure.sh " + tenantDomain + " " +
	  			                         cartridgeName + " " + dirBuffer.toString() + " /";
	  			proc = Runtime.getRuntime().exec(command);
	  			log.info("executing manage-git-repo script..... command : " + command);
	  			proc.waitFor();

	  		} catch (Exception e) {
	  			log.error(" Exception is occurred when executing manage-git-repo script. Reason :" +
	  			          e.getMessage());
	  			handleException(e.getMessage(), e);
	  		}

	  		log.info(" Folder structure  is created ..... ");

	  	}

	private void handleException(String msg, Exception e) throws Exception {
		log.error(msg, e);
		throw new Exception(msg, e);
	}
}
