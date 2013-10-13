package org.wso2.carbon.appfactory.jenkins.artifact.storage;

import hudson.FilePath;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.jenkins.AppfactoryPluginManager;
import org.wso2.carbon.appfactory.jenkins.Constants;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;


public class Utils {
    private static final Log log = LogFactory.getLog(Utils.class);
    private static AppfactoryPluginManager.DescriptorImpl descriptor = new AppfactoryPluginManager.DescriptorImpl();

    /**
     * Request should have the job-name that user wants to get the tag names of
     * This will send the list of tag names that user has asked to persist for the given job
     *
     * @param req request
     * @param rsp response
     */
    public static void getTagNamesOfPersistedArtifacts(StaplerRequest req, StaplerResponse rsp) {

        String storagePath = descriptor.getStoragePath();
        String jobName = req.getParameter(Constants.JOB_NAME);

        //artifact storage structure : <storage-path>/<job-name>/<tag-name>/artifact
        File jobDir = new File(storagePath + File.separator + jobName);
        String[] identifiers = jobDir.list();
        if (jobDir.exists() && identifiers.length > 0) {
            try {
                PrintWriter writer = rsp.getWriter();
                for (String identifier : identifiers) {
                    writer.write(identifier + ",");
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                log.error("Error while adding identifiers to response", e);
            }
        } else {
            log.info("No artifacts are tagged to persists for job " + jobName);
        }
    }
}
