/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.automation.tools.jmeter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.automation.tools.jmeter.util.Utils;

import java.io.File;
import java.io.IOException;

public class JMeterInstallationProvider {
    private static final Log log = LogFactory.getLog(JMeterInstallationProvider.class);

    private static File jMeterHome = null;
    private static File reportDir = null;
    private static File binDir = null;
    private static File logDir = null;
    private static File jmeterPropertyFile = null;
    private static JMeterInstallationProvider instance = new JMeterInstallationProvider();

    private JMeterInstallationProvider() {
        log.info("Creating jmeter installation directory structure");
        File targetDir = new File(System.getProperty("basedir") + File.separator + "target");
        File libDir;
        File binDir;

        File saveServiceProps;
        File upgradeProps;


        //creating jmeter directory
        jMeterHome = new File(targetDir, "jmeter");
        if (!jMeterHome.mkdirs()) {
            log.error("Unable to create jmeter directory");
        }

        reportDir = new File(jMeterHome, "reports");

        // now create lib dir for jmeter fallback mode
        libDir = new File(jMeterHome + File.separator + "lib");
        createLibDirectory(libDir);

        binDir = new File(jMeterHome + File.separator + "bin");
        if (!binDir.exists()) {
            if (!binDir.mkdirs()) {
                log.error("unable to create bin directory");
            }
        }

        //saving properties file in bin directory
        saveServiceProps = new File(binDir, "saveservice.properties");
        upgradeProps = new File(binDir, "upgrade.properties");
        jmeterPropertyFile = new File(binDir, "jmeter.properties");

        //copying saveservice.properties from classpath
        try {
            Utils.copyFromClassPath("bin/saveservice.properties", saveServiceProps);
        } catch (IOException e) {
            log.error("Could not create temporary saveservice.properties", e);
        }

        System.setProperty("saveservice_properties",
                           File.separator + "bin" + File.separator + "saveservice.properties");

        //copying upgrade.properties from classpath
        try {
            Utils.copyFromClassPath("bin/upgrade.properties", upgradeProps);


        } catch (IOException e) {
            log.error("Could not create temporary upgrade.properties", e);
        }

        System.setProperty("upgrade_properties",
                           File.separator + "bin" + File.separator + "upgrade.properties");

        try {
            // if the properties file is not specified in the papameters
            log.info("Loading default jmeter.properties...");
            Utils.copyFromClassPath("bin/jmeter.properties", jmeterPropertyFile);

        } catch (IOException e) {
            log.error("Could not create jmeter.properties", e);
        }
        System.setProperty("jmeter_properties",
                           File.separator + "bin" + File.separator + "jmeter.properties");

        logDir = new File(jMeterHome, "logs");
        if (!logDir.mkdirs()) {
            log.error("Unable to create log directory");
        }
    }

    public static JMeterInstallationProvider getInstance() {
        return instance;
    }

    public File getJMeterHome() {
        return jMeterHome;
    }

    public File getReportDir() {
        return reportDir;
    }

    public File getBinDir() {
        return binDir;
    }

    public File getLogDir() {
        return logDir;
    }

    public File getJMeterPropertyFile() {
        return jmeterPropertyFile;
    }

    private void createLibDirectory(File libDir) {
        File extDir;
        File junitDir;
        if (!libDir.exists()) {
            if (!libDir.mkdirs()) {
                log.error("Unable create lib directory");
            }
            extDir = new File(jMeterHome
                              + File.separator + "lib" + File.separator + "ext");
            if (!extDir.exists()) {
                if (!extDir.mkdirs()) {
                    log.error("Unable create ext directory");
                }
            }
            junitDir = new File(jMeterHome
                                + File.separator + "lib" + File.separator + "junit");
            if (!junitDir.exists()) {
                if (!junitDir.mkdirs()) {
                    log.error("Unable create junit directory");
                }
            }
        }
    }
}
