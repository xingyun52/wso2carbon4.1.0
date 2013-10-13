package org.wso2.carbon.deployment.synchronizer.git.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String Utility methods
 */
public class Utilities {

    private static final Log log = LogFactory.getLog(Utilities.class);

    /**
     * Searches for a match in a input String against a regex
     *
     * @param input input String
     * @param regex regex to match
     * @param group grouping,
     *
     * @return result of the match if found, else empty String
     */
    public static String getMatch (String input, String regex, int group) {

        String whitespaceRemovedJsonString = input.replaceAll("\\s+","");
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(whitespaceRemovedJsonString);
        if(!matcher.find())
            return "";
        else
            return matcher.group(group).trim();
    }

    /**
     * Deletes a folder structure recursively
     *
     * @param existingDir folder to delete
     */
    public static void deleteFolderStructure (File existingDir) {

        try {
            FileUtils.deleteDirectory(existingDir);

        } catch (IOException e) {
            log.error("Deletion of existing non-git repository structure failed");
            e.printStackTrace();
        }
    }
}
