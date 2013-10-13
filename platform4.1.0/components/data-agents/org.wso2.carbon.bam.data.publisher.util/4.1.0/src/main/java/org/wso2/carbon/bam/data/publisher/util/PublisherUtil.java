/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.bam.data.publisher.util;


import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PublisherUtil {

    private static final String PORTS_OFFSET = "Ports.Offset";
    private static final int CARBON_SERVER_DEFAULT_PORT = 9763;

    private static Log log = LogFactory.getLog(PublisherUtil.class);
    private static final String UNKNOWN_HOST = "UNKNOWN_HOST";

    private static String hostAddressAndPort = null;
    public static final String CLOUD_DEPLOYMENT_PROP = "IsCloudDeployment";
    public static final String HOST_NAME = "HostName";


    public static String getHostAddress() {

        if (hostAddressAndPort != null) {
            return hostAddressAndPort;
        }
        String hostAddress =   ServerConfiguration.getInstance().getFirstProperty(HOST_NAME);
        if(null == hostAddress){
        hostAddress = getLocalAddress().getHostName();
        if (hostAddress == null) {
            hostAddress = UNKNOWN_HOST;
        }
        int portsOffset = Integer.parseInt(CarbonUtils.getServerConfiguration().getFirstProperty(
                PORTS_OFFSET));
        int portValue = CARBON_SERVER_DEFAULT_PORT + portsOffset;
        hostAddressAndPort = hostAddress +  ":" + portValue;
        return hostAddressAndPort;
        }else {
            return hostAddress.trim();
        }
    }

    private static InetAddress getLocalAddress(){
        Enumeration<NetworkInterface> ifaces = null;
        try {
            ifaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            log.error("Failed to get host address", e);
        }
        if (ifaces != null) {
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
        }

        return null;
    }

    public static int getTenantId(MessageContext msgContext) {

        int tenantID = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        if (tenantID == MultitenantConstants.INVALID_TENANT_ID) {
            AxisConfiguration axisConfiguration = msgContext.getConfigurationContext().getAxisConfiguration();
            tenantID = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();
        }
        return tenantID;
    }

     public static ArrayList<String> getReceiverGroups(String urls) {
        ArrayList<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("\\{.*?\\}");
        Matcher regexMatcher = regex.matcher(urls);
        while (regexMatcher.find()) {
            matchList.add(regexMatcher.group().replace("{", "").replace("}", ""));
        }
        if (matchList.size() == 0) {
            matchList.add(urls.replace("{", "").replace("}", ""));
        }
        return matchList;
    }


}
