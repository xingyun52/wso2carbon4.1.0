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
package org.wso2.carbon.appfactory.repository.mgt.client;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.ScmRevision;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.branch.BranchScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.mkdir.MkdirScmResult;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryMgtException;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.appfactory.utilities.version.AppVersionStrategyExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Repository client that wrapping maven SCM plugin
 */
public class SCMClient {
    private static final Log log = LogFactory.getLog(SCMClient.class);
    private static final String BASE_WORKING_DIR_NAME = "scmclient";
    private ScmManager scmManager;
    private Embedder plexus;
    private String username;
    private String password;

    public SCMClient() {
        //creating plexus per client
        plexus = new Embedder();
    }

    public void init(String username, String password) throws RepositoryMgtException {
        this.username = username;
        this.password = password;
        try {
            plexus.start();
        } catch (PlexusContainerException e) {
            String msg = "Could not able to start Plexus";
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
    }

    public void close() {
        try {
            plexus.stop();
        } catch (Exception ignore) {
            //According to docs  it can be ignored
        }
    }

    private File getWorkingDirectory() throws RepositoryMgtException {
        //working directory should be unique to allow parallel operation
        File temp = getFileWithRandomName();
        if (!temp.exists()) {
            if (!temp.mkdirs()) {
                String msg = "Error in creating working directory";
                log.error(msg);
                throw new RepositoryMgtException(msg);
            }
        }
        //generating random name,so there is no chance to get directory that already exists
        return temp;
    }

    private File getFileWithRandomName() {
        String randomFileName = UUIDGenerator.generateUUID();
        File temp = new File(CarbonUtils.getTmpDir() + File.separator + BASE_WORKING_DIR_NAME
                             + File.separator + randomFileName);
        return temp;
    }

    public boolean mkdir(String baseURL, String dirName) throws RepositoryMgtException {
        MkdirScmResult result;
        ScmRepository repository = getRepository(baseURL);
        ScmFileSet fileSet;
        File workDir = getWorkingDirectory();
        fileSet = new ScmFileSet(workDir, new File(dirName));
        try {
            result = scmManager.mkdir(repository, fileSet, "creating directory" + dirName, false);
        } catch (ScmException e) {
            String msg = "Could not able to execute mkdir on " + baseURL;
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return processResult(result, workDir);
    }

    private ScmRepository getRepository(String url) throws RepositoryMgtException {
        ScmRepository repository;
        scmManager = null;
        try {
            scmManager = (ScmManager) plexus.lookup(ScmManager.ROLE);
            repository = scmManager.makeScmRepository(url);
            repository.getProviderRepository().setUser(username);
            repository.getProviderRepository().setPassword(password);
        } catch (ScmRepositoryException e) {
            String msg = "Could not able to create repository object";
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        } catch (NoSuchScmProviderException e) {
            String msg = "There is no repository provider for " + url +
                         " install required dependencies";
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        } catch (ComponentLookupException e) {
            String msg = "Error in looking up ScmManager";
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return repository;
    }

    public boolean branch(String baseURL, String version, String revision)
            throws RepositoryMgtException {
        BranchScmResult result = null;
        ScmRepository repository = getRepository(baseURL);
        File checkOutDirectory = getWorkingDirectory();
        try {
            //In scm plugin we have to first checkout code locally,branch it then commit
            //Pre-request is the repository should contain trunk folder
            CheckOutScmResult checkOutScmResult = scmManager.checkOut(repository,
                                                                      new ScmFileSet(checkOutDirectory),
                                                                      new ScmRevision(revision));
            AppVersionStrategyExecutor exec = org.wso2.carbon.appfactory.repository.mgt.internal.Util.getVersionStrategyExecutor();
            exec.doVersion(version, checkOutDirectory);
            if (checkOutScmResult.getProviderMessage() == null) {
                result = scmManager.branch(repository, new ScmFileSet(checkOutDirectory), version);
            }
        } catch (ScmException e) {
            String msg = "Error in executing branch operation on " + baseURL;
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return processResult(result, checkOutDirectory);
    }


    public boolean tag(String baseURL, String version, String revision)
            throws RepositoryMgtException {
        TagScmResult result = null;
        ScmRepository repository = getRepository(baseURL);
        File checkOutDirectory = getWorkingDirectory();
        try {
            //look at comment in branch,same is true here
            CheckOutScmResult checkOutScmResult =
                    scmManager.checkOut(repository, new ScmFileSet(checkOutDirectory),
                                        new ScmRevision(revision));
            if (checkOutScmResult.getProviderMessage() == null) {
                result = scmManager.tag(repository, new ScmFileSet(checkOutDirectory), version);
            }
        } catch (ScmException e) {
            String msg = "Error in executing tag operation on " + baseURL;
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return processResult(result, checkOutDirectory);
    }

    public boolean checkOut(String url, File checkOutDirectory, String revision)
            throws RepositoryMgtException {
        CheckOutScmResult checkOutScmResult;
        ScmRepository repository = getRepository(url);
        try {
            checkOutScmResult = scmManager.checkOut(repository, new ScmFileSet(checkOutDirectory),
                                                    new ScmRevision(revision));
        } catch (ScmException e) {
            String msg = "Error in executing checkout operation on " + url;
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return processResult(checkOutScmResult, null);
    }

    public boolean checkIn(String url, File checkInDirectory, String msg)
            throws RepositoryMgtException {
        CheckInScmResult checkInScmResult;
        ScmRepository repository = getRepository(url);
        repository.getProviderRepository().setPushChanges(true);
        try {
            checkInScmResult = scmManager.checkIn(repository, new ScmFileSet(checkInDirectory), msg);
        } catch (ScmException e) {
            String message = "Error in executing checkIn operation on " + url;
            log.error(msg, e);
            throw new RepositoryMgtException(message, e);
        }
        return processResult(checkInScmResult, null);
    }

    private boolean processResult(ScmResult result, File workingDirectory)
            throws RepositoryMgtException {
        if (workingDirectory != null) {
            try {
                FileUtils.deleteDirectory(workingDirectory);
            } catch (IOException e) {
                String msg = "Error in deleting working directory " + workingDirectory;
                log.error(msg, e);
                throw new RepositoryMgtException(msg, e);
            }
        }
        boolean success = false;
        if (result != null) {
            if (result.isSuccess()) {
                success = true;
            } else {
                log.error("Error in executing command  CommandLine error is  "
                          + result.getCommandOutput());
            }
        }
        return success;
    }

    /**
     * Method to add files to the given svn repository.
     *
     * @param url the svn repository url
     * @param currentFile the file that needs to be added to the svn repository
     */
    public boolean add(String url, File currentFile) throws RepositoryMgtException {
        AddScmResult addScmResult;
        ScmFileSet fileSet;
        ScmRepository repository = getRepository(url);

        ArrayList<File> childrenList = new ArrayList<File>();
        File[] files = currentFile.listFiles();

        if (files != null && files.length > 0) {
            childrenList.add(currentFile);
            Collections.addAll(childrenList, files);
            fileSet = new ScmFileSet(currentFile.getParentFile(), childrenList);
        } else {
            fileSet = new ScmFileSet(currentFile.getParentFile(), currentFile);
        }

        try {
            addScmResult = scmManager.add(repository, fileSet);
        } catch (ScmException e) {
            String msg = "Error in executing add operation on " + url;
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return processResult(addScmResult, null);
    }

    /**
     * Method to add files to the given svn repository.
     * Note that this method adds all the sub files and folders to the repo as well.
     *
     * @param url the svn repository url
     * @param currentFile the file that needs to be added to the svn repository
     * @param repository the svn repository instance
     */
    public boolean addRecursively(String url, File currentFile,ScmRepository repository) throws RepositoryMgtException {
        AddScmResult addScmResult;
        ScmFileSet fileSet;

        if(repository == null){
            repository = getRepository(url);
        }

        ArrayList<File> childrenList = new ArrayList<File>();
        File[] files = currentFile.listFiles();

        if (files != null && files.length > 0) {
            childrenList.add(currentFile);
            Collections.addAll(childrenList, files);
            fileSet = new ScmFileSet(currentFile.getParentFile(), childrenList);
        } else {
            fileSet = new ScmFileSet(currentFile.getParentFile(), currentFile);
        }

        try {
            addScmResult = scmManager.add(repository, fileSet);
        } catch (ScmException e) {
            String msg = "Error in executing add operation on " + url;
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }

//        Adding the children recursively
        for (File child : childrenList) {
            if(!child.getAbsolutePath().equals(currentFile.getAbsolutePath()) && !child.getName().equals(".svn")){
                addRecursively(url,child,repository);
            }
        }

        return processResult(addScmResult, null);
    }
}
