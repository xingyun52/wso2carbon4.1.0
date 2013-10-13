/*
 *  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.qpid.service;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.qpid.commons.registry.RegistryClient;
import org.wso2.carbon.qpid.commons.registry.RegistryClientException;
import org.wso2.carbon.qpid.internal.QpidServiceDataHolder;
import org.wso2.carbon.utils.NetworkUtils;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.SocketException;

import org.wso2.carbon.qpid.commons.QueueDetails;
import org.wso2.carbon.qpid.commons.SubscriptionDetails;

public class QpidServiceImpl implements QpidService {

    private static final Log log = LogFactory.getLog(QpidServiceImpl.class);

    private static String CARBON_CLIENT_ID = "carbon";
    private static String CARBON_VIRTUAL_HOST_NAME = "carbon";
    private static String CARBON_DEFAULT_HOSTNAME = "localhost";
    private static String CARBON_DEFAULT_PORT = "5672";
    private static String CARBON_DEFAULT_SSL_PORT = "8672";
    private static int CARBON_DEFAULT_PORT_OFFSET = 0;

    private static final String QPID_CONF_DIR = "/repository/conf/advanced/";
    private static final String QPID_CONF_FILE = "qpid-config.xml";
    private static final String QPID_CONF_CONNECTOR_NODE = "connector";
    private static final String QPID_CONF_PORT_NODE = "port";
    private static final String QPID_CONF_SSL_PORT_NODE = "sslport";

    private static String CARBON_CONFIG_QPID_PORT_NODE = "Ports.EmbeddedQpid.BrokerPort";
    private static String CARBON_CONFIG_QPID_SSL_PORT_NODE = "Ports.EmbeddedQpid.BrokerSSLPort";
    private static String CARBON_CONFIG_PORT_OFFSET_NODE = "Ports.Offset";

    private static final String DOMAIN_NAME_SEPARATOR = "@";
    private static final String DOMAIN_NAME_SEPARATOR_INTERNAL = "!";

    private String accessKey = "";
    private String hostname = "";
    private String port = "";
    private String sslPort = "";
    private int portOffset = 0;

    public QpidServiceImpl(String accessKey) {
        this.accessKey = accessKey;

        // Get the hostname that Carbon runs on
        try {
            hostname = NetworkUtils.getLocalHostname();
        } catch (SocketException e) {
            hostname = CARBON_DEFAULT_HOSTNAME;
        }

        // Read Port Offset
        portOffset = readPortOffset();

        // Read Qpid broker port from configuration file
        port = readPortFromConfig();

        // Read Qpid broker SSL port from configuration file
        sslPort = readSSLPortFromConfig();
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getClientID() {
        return CARBON_CLIENT_ID;
    }

    public String getVirtualHostName() {
        return CARBON_VIRTUAL_HOST_NAME;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPort() {
        return port;
    }

    public String getInVMConnectionURL(String username) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{accessKey}@carbon/carbon?brokerlist='vm://:1'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(accessKey)
                .append("@").append(CARBON_CLIENT_ID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='vm://:1'").toString();
    }

    public String getTCPConnectionURL(String username, String password) {
        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(CARBON_CLIENT_ID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(hostname).append(":").append(port).append("'")
                .toString();
    }

    public String getTCPConnectionURL(String username, String password, String clientID) {
        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(clientID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(hostname).append(":").append(port).append("'")
                .toString();
    }

    public String getInternalTCPConnectionURL(String username, String password) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(CARBON_CLIENT_ID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(hostname).append(":").append(port).append("'")
                .toString();
    }

    public String getInternalTCPConnectionURL(String username, String password, String clientID) {
        username = getInternalTenantUsername(username);
        
        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(clientID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(hostname).append(":").append(port).append("'")
                .toString();
    }

    public String getQpidHome() {
        return System.getProperty(ServerConstants.CARBON_HOME) + QPID_CONF_DIR;
    }

    public QueueDetails[] getQueues(boolean isDurable) {
        QueueDetails[] queueDetails = null;

        try {
            queueDetails = RegistryClient.getQueues();
        } catch (RegistryClientException e) {
            log.warn("Erro while retrieving queue details : " + e.getMessage());
        }

        return queueDetails;
    }

    public SubscriptionDetails[] getSubscriptions(String topic, boolean isDurable) {
        SubscriptionDetails[] subsDetails = null;

        try {
            subsDetails = RegistryClient.getSubscriptions(topic);
        } catch (RegistryClientException e) {
            log.warn("Erro while retrieving subscription details : " + e.getMessage());
        }

        return subsDetails;
    }

    public String getSSLPort(){
        return sslPort;
    }

    private int readPortOffset() {
        ServerConfigurationService carbonConfig = QpidServiceDataHolder.getInstance().getCarbonConfiguration();
        String portOffset = carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET_NODE);

        try {
            return ((portOffset != null) ? Integer.parseInt(portOffset.trim()) : CARBON_DEFAULT_PORT_OFFSET);
        } catch (NumberFormatException e) {
            return CARBON_DEFAULT_PORT_OFFSET;
        }
    }

    private String readPortFromConfig() {
        String port = CARBON_DEFAULT_PORT;

        // Port defined in carbon.xml overrides others
        String portInCarbonConfig = readPortFromCarbonConfig();
        if (!portInCarbonConfig.isEmpty()) {
            port = portInCarbonConfig;
        } else {
            String portInQpidConfig = readPortFromQpidConfig();
            if (!portInQpidConfig.isEmpty()) {
                port = portInQpidConfig;
            }
        }

        // Offset
        try {
            port = Integer.toString(Integer.parseInt(port) + portOffset);
        } catch (NumberFormatException e) {
            port = CARBON_DEFAULT_PORT;
        }

        return port;
    }

    /**
        * Read port from carbon.xml
        *
        * @return
        */
    private String readPortFromCarbonConfig() {
        ServerConfigurationService carbonConfig = QpidServiceDataHolder.getInstance().getCarbonConfiguration();
        String port = carbonConfig.getFirstProperty(CARBON_CONFIG_QPID_PORT_NODE);

        return ((port != null) ? port.trim() : "");
    }

    /**
        * Read port from qpid-config.xml
        *
        * @return
        */
    private String readPortFromQpidConfig() {
        String port = "";

        try {
            File confFile = new File(getQpidHome() + QPID_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement connectorNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CONNECTOR_NODE));
            OMElement portNode = connectorNode.getFirstChildWithName(
                    new QName(QPID_CONF_PORT_NODE));

            port = portNode.getText();
        } catch (FileNotFoundException e) {
            log.error(getQpidHome() + QPID_CONF_FILE + " not found");
        } catch (XMLStreamException e) {
            log.error("Error while reading " + getQpidHome() +
                      QPID_CONF_FILE + " : " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Invalid configuration : " + getQpidHome() + QPID_CONF_FILE);
        }

        return ((port != null) ? port.trim() : "");
    }

    private String readSSLPortFromConfig() {
        String port = CARBON_DEFAULT_SSL_PORT;

        // Port defined in carbon.xml overrides others
        String portInCarbonConfig = readSSLPortFromCarbonConfig();
        if (!portInCarbonConfig.isEmpty()) {
            port = portInCarbonConfig;
        } else {
            String portInQpidConfig = readSSLPortFromQpidConfig();
            if (!portInQpidConfig.isEmpty()) {
                port = portInQpidConfig;
            }
        }

        // Offset
        try {
            port = Integer.toString(Integer.parseInt(port) + portOffset);
        } catch (NumberFormatException e) {
            port = CARBON_DEFAULT_SSL_PORT;
        }

        return port;
    }

    /**
        * Read port from carbon.xml
        *
        * @return
        */
    private String readSSLPortFromCarbonConfig() {
        ServerConfigurationService carbonConfig = QpidServiceDataHolder.getInstance().getCarbonConfiguration();
        String port = carbonConfig.getFirstProperty(CARBON_CONFIG_QPID_SSL_PORT_NODE);

        return ((port != null) ? port.trim() : "");
    }

    /**
        * Read port from qpid-config.xml
        *
        * @return
        */
    private String readSSLPortFromQpidConfig() {
        String port = "";

        try {
            File confFile = new File(getQpidHome() + QPID_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement connectorNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CONNECTOR_NODE));
            OMElement portNode = connectorNode.getFirstChildWithName(
                    new QName(QPID_CONF_SSL_PORT_NODE));

            port = portNode.getText();
        } catch (FileNotFoundException e) {
            log.error(getQpidHome() + QPID_CONF_FILE + " not found");
        } catch (XMLStreamException e) {
            log.error("Error while reading " + getQpidHome() +
                      QPID_CONF_FILE + " : " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Invalid configuration : " + getQpidHome() + QPID_CONF_FILE);
        }

        return ((port != null) ? port.trim() : "");
    }

    private String getInternalTenantUsername(String username) {
        // Replace @ with ! in tenant username as Qpid does not support @ in username
        // E.g. foo@bar.com -> foo!bar.com
        // Note : The Qpid authorization handler uses ! to extract domain name from username when authorizing
        return username.replace(DOMAIN_NAME_SEPARATOR, DOMAIN_NAME_SEPARATOR_INTERNAL);
    }
}
