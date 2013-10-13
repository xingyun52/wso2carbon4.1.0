/**
 * 
 */
package org.wso2.carbon.hosting.mgt.service;


import org.apache.axis2.AxisFault;
import org.wso2.carbon.hosting.mgt.client.AutoscaleServiceClient;
import org.wso2.carbon.hosting.mgt.utils.CartridgeConstants;
import org.wso2.carbon.hosting.mgt.utils.FileUploadData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;

import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.hosting.mgt.utils.AppsWrapper;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;


/**
 *
 */
public class ApplicationManagementService extends AbstractAdmin{

    private static final Log log = LogFactory.getLog(ApplicationManagementService.class);


    /**
     * Upload apps passed to method. Will be uploaded to the directory relevant to tenant
     *
     * @param fileUploadDataList Array of data representing the apps(.zip) that are to be uploaded
     * @return true - if upload was successful
     * @throws org.apache.axis2.AxisFault If an error occurs while uploading
     */
    public boolean uploadApp(FileUploadData[] fileUploadDataList, String cartridge) throws AxisFault {
        File appsDir = new File(getAppDeploymentDirPath(cartridge));

        if (!appsDir.exists() && !appsDir.mkdirs()) {
            log.warn("Could not create directory " + appsDir.getAbsolutePath());
        }

        for (FileUploadData uploadData : fileUploadDataList) {
            String fileName = uploadData.getFileName();
            File destFile = new File(appsDir, fileName);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(destFile);
                uploadData.getDataHandler().writeTo(fos);
                log.info("Files are successfully uploaded !" );
            } catch (IOException e) {
                handleException("Error occurred while uploading the application " + fileName, e);
            } finally {
                try {
                    if (fos != null) {
                        fos.flush();
                        fos.close();
                    }
                } catch (IOException e) {
                    log.warn("Could not close file " + destFile.getAbsolutePath());
                }
            }
            log.info("Files are successfully uploaded !" );
        }
        return true;
    }


    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }


    protected String getAppDeploymentDirPath(String cartridge) {
        return getAxisConfig().getRepository().getPath() + File.separator + cartridge.toLowerCase();
    }

    /**
     * Retrieve and display the applications
     */
    private String[] listApplications(String cartridge) {
        String appPath = getAppDeploymentDirPath(cartridge);
        File appDirectory = new File(appPath);
        String[] children;
        if(appDirectory.exists()){
            // This return any files that ends with '.zip'.
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".zip");
                }
            };
            children = appDirectory.list(filter);
        }else {
            children = null;
        }
        return children;

    }

    /**
     * returns a summery of the apps deployed in the directory relevant to tenant
     * @return
     */
    public AppsWrapper getPagedAppsSummary(String cartridge){
         AppsWrapper appsWrapper = new AppsWrapper();
        String[] apps= listApplications(cartridge);
        appsWrapper.setApps(apps);
        appsWrapper.setNumberOfPages(1);
        return appsWrapper;
    }


    public void deleteAllApps(String cartridge){
        deleteFromDirectory(listApplications(cartridge), cartridge);
    }

    public void deleteApps(String[] appFileNames, String cartridge){
        deleteFromDirectory(appFileNames, cartridge);
    }

    private void deleteFromDirectory(String apps[], String cartridge){
        File appFile;
        for (String app : apps) {
            appFile = new File(getAppDeploymentDirPath(cartridge) +  File.separator + app + ".zip");
            appFile.delete();
        }
    }

    public String[] getCartridgeTitles(){
        String cartridgeTitles[];
        String imageIdsString = System.getProperty(CartridgeConstants.CARTRIDGE_TITLES);
        cartridgeTitles = imageIdsString.split(",");
        return cartridgeTitles;
    }

    public void registerCartridge(String cartridgeType, int min, int max, String svnPassword,
                                  boolean attachVolume ) {
        try{
            int tenantId = MultitenantUtils.getTenantId(configurationContext);

            String msg = "Registering " + cartridgeType + " cartridge with min and max instances, " + min + ", "
                         + max + " for tenant " + tenantId;

            log.debug(msg);
            //TODO call new autoscaler register service
        }catch (Exception e){
            String msg = "Error while calling autoscaler to start instance";
            log.error(msg);
        }
    }

}
