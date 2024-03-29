/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package org.wso2.carbon.appfactory.repository.mgt.client;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.scm.*;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.branch.BranchScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.mkdir.MkdirScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
import org.wso2.carbon.appfactory.repository.mgt.RepositoryMgtException;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.PathSegment;

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

    public SCMClient(Embedder embedder) {
        //creating plexus per client
        plexus = embedder;
    }

    public void init(String username, String password) throws RepositoryMgtException {
        this.username = username;
        this.password = password;
    }

    public void close() {

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
        return new File(CarbonUtils.getTmpDir() + File.separator + BASE_WORKING_DIR_NAME
                + File.separator + randomFileName);
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
            repository.getProviderRepository().setPushChanges(true);
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

    public boolean branch(String baseURL, String version, String checkOutDirectoryToBranch)
            throws RepositoryMgtException {
        BranchScmResult result;
        File checkOutDirectory = new File(checkOutDirectoryToBranch);
        ScmRepository repository = getRepository(baseURL);
        try {
            result = scmManager.branch(repository, new ScmFileSet(checkOutDirectory), version);
        } catch (ScmException e) {
            String msg = "Error in executing branch operation on " + baseURL;
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return processResult(result, null);
    }
    
    private StatusScmResult status(String baseURL, String version, String checkOutDirectoryToBranch)
            throws RepositoryMgtException {
        StatusScmResult result;
        File checkOutDirectory = new File(checkOutDirectoryToBranch);
        ScmRepository repository = getRepository(baseURL);
        try {
            result = scmManager.status(repository, new ScmFileSet(checkOutDirectory));
            result.getChangedFiles().get(1).getStatus().toString();
        } catch (ScmException e) {
            String msg = "Error in executing branch operation on " + baseURL;
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return result;
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

    /**
     * Do the commit with add/delete the resource to the repository
     * 
     * @param url
     * @param checkInDirectory
     * @param msg
     * @return
     * @throws RepositoryMgtException
     */
    public boolean forceCheckIn(String url, File checkInDirectory, String msg)
            throws RepositoryMgtException {
        CheckInScmResult checkInScmResult;
        ScmRepository repository = getRepository(url);
        repository.getProviderRepository().setPushChanges(true);
        try {
        	StatusScmResult statusScmResult = scmManager.status(repository, new ScmFileSet(checkInDirectory));
        	List<ScmFile> scmFiles = statusScmResult.getChangedFiles();
        
        	for (ScmFile scmFile : scmFiles) {
        		if(scmFile.getStatus()==ScmFileStatus.UNKNOWN){
        			addRecursively(url,checkInDirectory.getAbsolutePath()+"/"+ scmFile.getPath(),repository);
    			}else if(scmFile.getStatus()==ScmFileStatus.MISSING) {
    				delete(url, new File(checkInDirectory.getAbsolutePath()+"/"+ scmFile.getPath()),msg);
    			}
            }
        	
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
     * @param url         the svn repository url
     * @param currentFile the file that needs to be added to the svn repository
     * @return processResult Result in executing command
     * @throws RepositoryMgtException if add operation in SCM
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
     * Method to delete files to the given repository.
     *
     * @param url the  repository url
     * @param currentFile the file that needs to be added to the repository
     * @return processResult Result in executing command
     * @throws RepositoryMgtException if add operation in SCM
     */
    public boolean delete(String url, File currentFile,String msg) throws RepositoryMgtException {
    	
    	ScmResult scmResult ;
        ScmFileSet fileSet ;
        
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
        	scmResult = scmManager.remove(repository, fileSet,msg);

        } catch (ScmException e) {
            String errorMsg = "Error in executing add operation on " + url;
            log.error(errorMsg, e);
            throw new RepositoryMgtException(errorMsg, e);

        }
        return processResult(scmResult, null);
    }

    /**
     * Method to add files to the given svn repository.
     * Note that this method adds all the sub files and folders to the repo as well.
     *
     * @param url             the svn repository url
     * @param currentFilePath the file that needs to be added to the svn repository
     * @param repository      the svn repository instance
     * @return processResult Result in executing command
     * @throws RepositoryMgtException if add operation fails
     */
    public boolean addRecursively(String url, String currentFilePath, ScmRepository repository)
            throws RepositoryMgtException {
        AddScmResult addScmResult;
        ScmFileSet fileSet;
        File currentFile = new File(currentFilePath);
        if (repository == null) {
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
            if (!child.getAbsolutePath().equals(currentFile.getAbsolutePath())
                    && !child.getName().contains(".svn") && !child.getName().contains(".git")) {
                addRecursively(url, child.getAbsolutePath(), repository);
            }
        }

        return processResult(addScmResult, null);
    }

    public boolean checkOut(String url, File checkOutDirectory, String revision,
                            String version) throws RepositoryMgtException {
        CheckOutScmResult checkOutScmResult;
        ScmRepository repository = getRepository(url);
        try {
            checkOutScmResult = scmManager.checkOut(repository, new ScmFileSet(checkOutDirectory),
                    new ScmBranch(version));
        } catch (ScmException e) {
            String msg = "Error in executing checkout operation on " + url;
            log.error(msg, e);
            throw new RepositoryMgtException(msg, e);
        }
        return processResult(checkOutScmResult, null);
    }

    public boolean checkIn(String sourceURL, File workDir, String msg, String targetVersion)
            throws RepositoryMgtException {

        CheckInScmResult checkInScmResult;
        ScmRepository repository = getRepository(sourceURL);
        repository.getProviderRepository().setPushChanges(true);
        try {
            checkInScmResult = scmManager.checkIn(repository, new ScmFileSet(workDir), new ScmBranch(targetVersion), msg);
        } catch (ScmException e) {
            String message = "Error in executing checkIn operation on " + sourceURL;
            log.error(msg, e);
            throw new RepositoryMgtException(message, e);
        }
        return processResult(checkInScmResult, null);
    }
    
    public boolean forceCheckIn(String url, File checkInDirectory, String msg, String targetVersion)
            throws RepositoryMgtException {
        CheckInScmResult checkInScmResult;
        ScmRepository repository = getRepository(url);
        repository.getProviderRepository().setPushChanges(true);
    
        try {
        	addRecursively(url, checkInDirectory.getAbsolutePath(), null);
            checkInScmResult = scmManager.checkIn(repository, new ScmFileSet(checkInDirectory),new ScmBranch(targetVersion), msg);
        } catch (ScmException e) {
            String message = "Error in executing checkIn operation on " + url;
            log.error(msg, e); 
            throw new RepositoryMgtException(message, e);
        }
        return processResult(checkInScmResult, null);
    }
    
    
}
