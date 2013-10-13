package org.wso2.carbon.databridge.persistence.cassandra.internal.util;

import me.prettyprint.hector.api.ConsistencyLevelPolicy;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.commons.io.FileUtils;
import org.wso2.carbon.databridge.persistence.cassandra.internal.StreamDefnConsistencyLevelPolicy;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.InputStream;
import java.net.*;
import java.util.Enumeration;

/**
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class Utils {

    private static final String STREAMDEFN_XML = "streamdefn.xml";

    private static ConsistencyLevelPolicy globalConsistencyLevelPolicy;

    public static InetAddress getLocalAddress() throws SocketException, UnknownHostException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> addresses = iface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    return addr;
                }
            }
        }
        return InetAddress.getLocalHost();
    }


    private static int replicationFactor;
    private static String readConsistencyLevel;
    private static String writeConsistencyLevel;
    private static String strategyClass;

    public static void readConfigFile() {


        InputStream in;
        try {
            String configFilePath = CarbonUtils.getCarbonConfigDirPath() + File.separator + "advanced" + File.separator + STREAMDEFN_XML;
            in = FileUtils.openInputStream(new File(configFilePath));
        } catch (Exception e) {
            in = Utils.class.getClassLoader().getResourceAsStream(STREAMDEFN_XML);
        }

        OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(in);

        OMElement documentElement = builder.getDocumentElement();

        OMElement replicationFactorEl = documentElement.getFirstChildWithName(new QName("ReplicationFactor"));
        if (replicationFactorEl != null) {
            replicationFactor = Integer.parseInt(replicationFactorEl.getText());
        }

        OMElement readLevelEl = documentElement.getFirstChildWithName(new QName("ReadConsistencyLevel"));
        if (replicationFactorEl != null) {
            readConsistencyLevel = readLevelEl.getText();
        }

        OMElement writeLevelEl = documentElement.getFirstChildWithName(new QName("WriteConsistencyLevel"));
        if (writeLevelEl != null) {
            writeConsistencyLevel = writeLevelEl.getText();
        }

        globalConsistencyLevelPolicy = new StreamDefnConsistencyLevelPolicy(readConsistencyLevel, writeConsistencyLevel);
        
        OMElement strategyEl = documentElement.getFirstChildWithName(new QName("StrategyClass"));
        if (strategyEl != null) {
            strategyClass = strategyEl.getText();
        }


    }


    public static ConsistencyLevelPolicy getGlobalConsistencyLevelPolicy() {
        return globalConsistencyLevelPolicy;
    }

    public static String getStrategyClass() {
        return strategyClass;
    }

    public static int getReplicationFactor() {
        return replicationFactor;
    }

    public static String getReadConsistencyLevel() {
        return readConsistencyLevel;
    }

    public static String getWriteConsistencyLevel() {
        return writeConsistencyLevel;
    }
}
