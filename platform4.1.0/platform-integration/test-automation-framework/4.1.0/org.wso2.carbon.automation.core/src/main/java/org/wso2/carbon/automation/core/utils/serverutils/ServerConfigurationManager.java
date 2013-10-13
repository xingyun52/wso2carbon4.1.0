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
package org.wso2.carbon.automation.core.utils.serverutils;

import org.apache.axis2.AxisFault;
import org.wso2.carbon.automation.api.clients.server.admin.ServerAdminClient;
import org.wso2.carbon.automation.core.utils.ClientConnectionUtil;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.coreutils.CodeCoverageUtils;
import org.wso2.carbon.automation.core.utils.fileutils.FileManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * This class can be used to configure server by  replacing axis2.xml or carbon.xml
 */
public class ServerConfigurationManager {
    private final String AXIS2_XML = "axis2.xml";
    //    private final String CARBON_XML = "carbon.xml";
    private static final long TIME_OUT = 240000;
    private boolean isFileBackUp = false;
    private File originalConfig;
    private File backUpConfig;
    private int port;
    private String hostname;
    private String backEndUrl;
    private UserInfo admin;


    /**
     * Create a  ServerConfigurationManager
     *
     * @param backEndUrl - server backend service url
     * @throws AxisFault
     * @throws MalformedURLException - if backend url is invalid
     */
    public ServerConfigurationManager(String backEndUrl) throws AxisFault, MalformedURLException {
        admin = UserListCsvReader.getUserInfo(0);
        URL serverUrl = new URL(backEndUrl);
        this.backEndUrl = backEndUrl;
        port = serverUrl.getPort();
        hostname = serverUrl.getHost();
    }

    /**
     * backup the current server configuration file
     *
     * @param fileName
     */
    private void backupConfiguration(String fileName) {
        //restore backup configuration
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String confDir = carbonHome + File.separator + "repository" + File.separator + "conf"
                         + File.separator;
        if (AXIS2_XML.equalsIgnoreCase(fileName)) {
            confDir = confDir + "axis2" + File.separator;
        }
        originalConfig = new File(confDir + fileName);
        backUpConfig = new File(confDir + fileName + ".backup");
        originalConfig.renameTo(backUpConfig);
        isFileBackUp = true;
    }

    /**
     * restore to a last configuration and restart the server
     *
     * @throws Exception
     */
    public void restoreToLastConfiguration() throws Exception {

        if (isFileBackUp) {
            backUpConfig.renameTo(originalConfig);
            isFileBackUp = false;
            restartGracefully();
        }
    }

    /**
     * apply configuration file and restart server to take effect the configuration
     *
     * @param newConfig
     * @throws Exception
     */
    public void applyConfiguration(File newConfig) throws Exception {
        //to backup existing configuration
        backupConfiguration(newConfig.getName());
        FileReader in = new FileReader(newConfig);
        FileWriter out = new FileWriter(originalConfig);
        int c;

        while ((c = in.read()) != -1) {
            out.write(c);
        }

        in.close();
        out.close();
        restartGracefully();
    }

    /**
     * Restart Server Gracefully  from admin user
     *
     * @throws Exception
     */
    public void restartGracefully()  throws Exception {
        //todo use ServerUtils class restart
        ServerAdminClient serverAdmin = new ServerAdminClient(backEndUrl, admin.getUserName(), admin.getPassword());
        serverAdmin.restartGracefully();
        CodeCoverageUtils.renameCoverageDataFile();
        Thread.sleep(20000);
        ClientConnectionUtil.waitForPort(port, TIME_OUT, true, hostname);
        ClientConnectionUtil.waitForLogin(port, hostname, backEndUrl);


    }

    /**
     * Restart server gracefully from current user session
     * @param sessionCookie
     * @throws Exception
     */
    public void restartGracefully(String sessionCookie) throws Exception {
            //todo use ServerUtils class restart
            ServerAdminClient serverAdmin = new ServerAdminClient(backEndUrl, sessionCookie);
            serverAdmin.restartGracefully();
            CodeCoverageUtils.renameCoverageDataFile();
            Thread.sleep(20000);
            ClientConnectionUtil.waitForPort(port, TIME_OUT, true, hostname);
            ClientConnectionUtil.waitForLogin(port, hostname, backEndUrl);
    
    
        }

    /**
     * Copy Jar file to server component/lib
     *
     * @param jar
     * @throws IOException
     * @throws URISyntaxException
     */
    public void copyToComponentLib(File jar) throws IOException, URISyntaxException {
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String lib = carbonHome + File.separator + "repository" + File.separator + "components" + File.separator
                     + "lib";
        FileManager.copyJarFile(jar, lib);
    }

    /**
     * @param fileName
     * @throws IOException
     * @throws URISyntaxException
     */
    public void removeFromComponentLib(String fileName) throws IOException, URISyntaxException {
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String filePath = carbonHome + File.separator + "repository" + File.separator + "components" + File.separator
                          + "lib" + File.separator + fileName;
        FileManager.deleteFile(filePath);

//      removing osgi bundle from dropins; OSGI bundle versioning starts with _1.0.0
        fileName=fileName.replace("-","_");
        fileName=fileName.replace(".jar","_1.0.0.jar");
        removeFromComponentDropins(fileName);
    }

    /**
     * /**
     * Copy Jar file to server component/dropins
     *
     * @param jar
     * @throws IOException
     * @throws URISyntaxException
     */
    public void copyToComponentDropins(File jar) throws IOException, URISyntaxException {
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String lib = carbonHome + File.separator + "repository" + File.separator + "components" + File.separator
                     + "dropins";
        FileManager.copyJarFile(jar, lib);
    }

    /**
     * @param fileName
     * @throws IOException
     * @throws URISyntaxException
     */
    public void removeFromComponentDropins(String fileName) throws IOException, URISyntaxException {
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String filePath = carbonHome + File.separator + "repository" + File.separator + "components" + File.separator
                          + "dropins" + File.separator + fileName;
        FileManager.deleteFile(filePath);
    }
}
