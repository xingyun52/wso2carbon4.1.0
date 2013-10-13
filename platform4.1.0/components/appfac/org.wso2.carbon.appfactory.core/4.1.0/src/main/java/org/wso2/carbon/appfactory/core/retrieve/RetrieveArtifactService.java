package org.wso2.carbon.appfactory.core.retrieve;


import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.ArtifactStorage;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.io.*;
import java.lang.System;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;

public class RetrieveArtifactService {
    private static final Log log = LogFactory.getLog(RetrieveArtifactService.class);

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

    public String retrieveArtifactId(String applicationId, String version, String revision) {
        String fileName = null;
        String entryName = null;
        File file = null;
        ArtifactStorage storage = ServiceHolder.getArtifactStorage();
        String artifactDetails = null;


        try {
            file = storage.retrieveArtifact(applicationId, version, revision);
              if (file == null) {
                return "Not Found";
            } else if ((file.getName()).endsWith(".war")) {
                String fileName1 = FilenameUtils.removeExtension(file.getName());
                String artifactVersion = (file.getName()).substring(file.getName().indexOf('-') + 1, file.getName().indexOf(".war"));
                String artifactName = file.getName().substring(0, (file.getName().indexOf('-')));
                artifactDetails = artifactName + '-' + artifactVersion;
                //System.out.print(artifactDetails);
                return artifactDetails;

            } else if ((file.getName()).endsWith(".car")){
                fileName = file.getAbsolutePath();
                FileInputStream fin = null;
                fin = new FileInputStream(fileName);

                ZipInputStream zin = new ZipInputStream(fin);
                ZipEntry zentry = null;

                while ((zentry = zin.getNextEntry()) != null) {
                    if (!(zentry.getName().equals("artifacts.xml"))) {

                        //  byte[] buf = new byte[1024];
                        entryName = zentry.getName();
                        log.info("Name of  Zip Entry : " + entryName);
                        String artifactVersion = entryName.substring(entryName.indexOf('_') + 1);
                        String artifactName = entryName.substring(0, (entryName.indexOf('_')));
                        zin.close();
                        fin.close();

                        artifactDetails = artifactName + '-' + artifactVersion;
                        return artifactDetails;
                    }

                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AppFactoryException e) {
            e.printStackTrace();
        }
        return artifactDetails;

    }
}











