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


package org.wso2.carbon.appfactory.svn.repository.mgt.impl;


import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.ArtifactStorage;
import org.wso2.carbon.utils.CarbonUtils;

import javax.activation.DataHandler;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileArtifactStorage implements ArtifactStorage {
    private static final Log log = LogFactory.getLog(FileArtifactStorage.class);
    private static final String ARTIFACT_STORAGE = "artifactdb";

    @Override
    public File retrieveArtifact(String applicationId, String version, String revision) throws AppFactoryException {
        File targetDir = null;
        List<File> artifactFiles = null;
        File workDir = new File(getApplicationStorageDirectoryPath(applicationId, version, revision));
        String[] fileExtension = {"car"};
        artifactFiles = (List<File>) FileUtils.listFiles(workDir, fileExtension, false);
        targetDir= artifactFiles.get(0);
        return targetDir;
    }

    @Override
    public void storeArtifact(String s, String s2, String s3, DataHandler dataHandler, String s4)
            throws AppFactoryException {
        //TODO
    }

    public void storeArtifact(String applicationId, String version, String revision, File file) throws AppFactoryException {
    	String path = getApplicationStorageDirectoryPath(applicationId, version, revision)  + File.separator + file.getName();
    	try {
			File destFile = new File(path);
			FileUtils.copyFile(file, destFile);
		} catch (IOException e) {
			log.error("Error storing the artifact file : " + e.getMessage(), e);
			throw new AppFactoryException("Error storing the artifact file : " + e.getMessage(), e);
		}
    }
    
    
	public String getApplicationStorageDirectoryPath(String applicationId,
			String version, String revision) {
		return CarbonUtils.getTmpDir() + File.separator + ARTIFACT_STORAGE + File.separator + applicationId + File.separator + version;
	}
}
