package org.wso2.carbon.appfactory.artifact.storage.service;

import org.wso2.carbon.appfactory.artifact.storage.internal.ServiceHolder;
import org.wso2.carbon.appfactory.core.ArtifactStorage;
import org.wso2.carbon.appfactory.common.AppFactoryException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.io.File;


public class ArtifactStorageService {

    public DataHandler retrieveArtifact(String applicationId, String version, String revision) {
        String fileName = null;
        File file = null;
        ArtifactStorage storage = ServiceHolder.getArtifactStorage();

        try {
            file = storage.retrieveArtifact(applicationId, version, revision);
            fileName = file.getAbsolutePath();

        } catch (AppFactoryException e) {
            e.printStackTrace();
        }

        FileDataSource dataSource = new FileDataSource(fileName);
        DataHandler fileDataHandler = new DataHandler(dataSource);
        return fileDataHandler;
    }


     public void storeArtifact(String applicationId, String version, String revision, DataHandler data, String fileName) {
       //String name = null;
        File file = null;
        ArtifactStorage storage = ServiceHolder.getArtifactStorage();

        try {
            storage.storeArtifact(applicationId, version, revision, data, fileName);

        } catch (AppFactoryException e) {
            e.printStackTrace();
        }

     }
}


