/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.wso2.carbon.appfactory.repository.mgt;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.repository.mgt.client.AppfactoryRepositoryClient;
import org.wso2.carbon.appfactory.repository.mgt.internal.Util;
import org.wso2.carbon.appfactory.utilities.project.ProjectUtils;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.IOException;

/**
 *
 *
 * .
 */
public  class RepositoryManager {
       private static final Log log=LogFactory.getLog(RepositoryManager.class);
    public String createRepository(String applicationKey,String type) throws RepositoryMgtException{
       RepositoryProvider provider= Util.getRepositoryProvider(type);
       String url;
        File workDir=new File(CarbonUtils.getTmpDir()+File.separator+applicationKey);
        workDir.mkdirs();
       if(provider!=null){
          url= provider.createRepository(applicationKey);

           AppfactoryRepositoryClient client= getRepositoryClient(type);
          client.init(Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME),Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_PASSWORD));
          client.checkOut(url,workDir,"0");
          File trunk=new File(workDir.getAbsolutePath()+File.separator+AppFactoryConstants.TRUNK);
          trunk.mkdir();

           try {
               String applicationType = ProjectUtils.getApplicationType(applicationKey);
               if(AppFactoryConstants.FILE_TYPE_CAR.equals(applicationType)) {
                   ProjectUtils.generateCAppArchetype(applicationKey, trunk.getAbsolutePath());
               } else if(AppFactoryConstants.FILE_TYPE_WAR.equals(applicationType)) {
                   ProjectUtils.generateWebAppArchetype(applicationKey, trunk.getAbsolutePath());
               }

           } catch (AppFactoryException e) {
//               There is an exception when generating the maven archetype.
               String msg = "Could not generate the project using maven archetype for application : " + applicationKey;
               log.error(msg,e);
               throw new RepositoryMgtException(msg,e);
           }

          File branches=new File(workDir.getAbsolutePath()+File.separator+AppFactoryConstants.BRANCH);
           branches.mkdir();
          File tags=new File(workDir.getAbsolutePath()+File.separator+AppFactoryConstants.TAG);
           tags.mkdir();
           client.addRecursively(url,trunk);
           client.add(url,branches);
           client.add(url,tags);

          client.checkIn(url,workDir,"creating trunk,branches and tags ");
          client.close();
           try {
               FileUtils.deleteDirectory(workDir);
           } catch (IOException e) {
               log.error("Error deleting work directory " + e.getMessage(), e);
           }

       }else {
           throw new  RepositoryMgtException((new StringBuilder()).append("Repository provider for the type ").append(type).append(" not found").toString());
       }
        return url;
    }

    public String getAppRepositoryURL(String appId, String type) throws RepositoryMgtException {
        RepositoryProvider provider = Util.getRepositoryProvider(type);
        if (provider != null) {
            return provider.getAppRepositoryURL(appId);
        } else {
            throw new RepositoryMgtException((new StringBuilder()).
                    append("Repository provider for the type ").
                    append(type).
                    append(" not found").toString());
        }
    }

    public String getURLForAppversion(String applicationKey, String version, String type)
            throws RepositoryMgtException {
        StringBuilder builder = new StringBuilder(getAppRepositoryURL(applicationKey, type)).append('/');

        if (AppFactoryConstants.TRUNK.equals(version)) {
            builder.append(version);
        } else {
            builder.append(AppFactoryConstants.BRANCH).append('/').append(version);
        }
        return builder.toString();
    }

    public AppfactoryRepositoryClient getRepositoryClient(String type) {
        return new AppfactoryRepositoryClient(type);
    }
}
