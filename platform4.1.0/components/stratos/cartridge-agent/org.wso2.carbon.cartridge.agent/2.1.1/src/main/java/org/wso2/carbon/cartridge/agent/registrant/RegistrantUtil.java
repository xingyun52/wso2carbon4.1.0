/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.cartridge.agent.registrant;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cartridge.agent.exception.CartridgeAgentException;
import org.wso2.carbon.cartridge.agent.ClusteringClient;

/**
 * Utility method collection for handling {@link Registrant}s
 *
 * @see Registrant
 */
public class RegistrantUtil {
    private static final Log log = LogFactory.getLog(RegistrantHealthChecker.class);

    /**
     * Before adding a member, we will try to verify whether we can connect to it
     *
     * @param registrant The member whose connectvity needs to be verified
     * @return true, if the member can be contacted; false, otherwise.
     */
    public static boolean isHealthy(Registrant registrant) {
        if (log.isDebugEnabled()) {
            log.debug("Trying to connect to registrant " + registrant + "...");
        }
        String registrantRemoteHost = registrant.getRemoteHost();
        if(registrantRemoteHost == null){
            registrantRemoteHost = "localhost";
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(registrantRemoteHost);
        } catch (UnknownHostException e) {
            log.error("Registrant " + registrant + " is unhealthy");
            return false;
        }
        PortMapping[] portMappings = registrant.getPortMappings();
        for (int retries = 3; retries > 0; retries--) {
            try {
                for (PortMapping portMapping : portMappings) {
                    int port = portMapping.getPrimaryPort();
                    if (port != -1 && port != 0) {
                        SocketAddress httpSockaddr = new InetSocketAddress(addr, port);
                        new Socket().connect(httpSockaddr, 10000);
                    }
                }
                return true;
            } catch (IOException e) {
                String msg = e.getMessage();
                if (!msg.contains("Connection refused") && !msg.contains("connect timed out")) {
                    String msg2 = "Cannot connect to registrant " + registrant;
                    log.error(msg2, e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
        return false;
    }

    /**
     * Reload all the registrants persisted in the file system
     * @param clusteringClient ClusteringClient
     * @param configurationContext   ConfigurationContext
     * @param registrantDatabase  RegistrantDatabase
     * @throws CartridgeAgentException If reloading registrants fails
     */
    public static void reloadRegistrants(ClusteringClient clusteringClient,
                                         ConfigurationContext configurationContext,
                                         RegistrantDatabase registrantDatabase) throws CartridgeAgentException {
        File registrants = new File("registrants");
        if (!registrants.exists()) {
            return;
        }
        File[] files = registrants.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                Registrant registrant =
                        deserializeRegistrant("registrants" + File.separator + file.getName());
                if (!registrantDatabase.containsActive(registrant)) {
                    clusteringClient.joinGroup(registrant, configurationContext);
                }
            } catch (IOException e) {
                log.error("Cannot deserialize registrant file " + file.getName(), e);
            }
        }
    }

    private static Registrant deserializeRegistrant(String fileName) throws IOException {
        Registrant registrant = null;
        ObjectInputStream in = null;
        try {
            // Deserialize from a file
            File file = new File(fileName);
            in = new ObjectInputStream(new FileInputStream(file));
            // Deserialize the object
            registrant = (Registrant) in.readObject();
        } catch (ClassNotFoundException ignored) {
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return registrant;
    }
    
    
}
