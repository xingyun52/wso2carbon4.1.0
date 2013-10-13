/*
*  Copyright (c) WSO2 Inc. (http://wso2.com) All Rights Reserved.

  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*
*/

package org.wso2.carbon.bam.jmx.agent;

import org.apache.log4j.Logger;
import org.wso2.carbon.bam.jmx.agent.profiles.Profile;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

public class JmxAgent {

    private static Logger log = Logger.getLogger(JmxAgent.class);
    private MBeanServerConnection mbsc;

    public JmxAgent(Profile profile) {

        try {
            JMXServiceURL url = new JMXServiceURL(profile.getUrl());

            //set-up authentication
            HashMap map = new HashMap();
            String[] credentials = new String[2];
            credentials[0] = profile.getUserName();
            credentials[1] = profile.getPass();

            map.put("jmx.remote.credentials", credentials);

            JMXConnector jmxc = JMXConnectorFactory.connect(url, map);
            mbsc = jmxc.getMBeanServerConnection();

        } catch (MalformedURLException e) {
            log.error(e);
            e.printStackTrace();
        } catch (IOException e) {
            log.error(e);
            e.printStackTrace();
        }

    }

    public String[] getDomains() throws IOException {

        return mbsc.getDomains();
    }

    public String[][] getMBeans() throws IOException {

        int count = 0;
        Set<ObjectName> names =
                new TreeSet<ObjectName>(mbsc.queryNames(null, null));
        String[][] nameArr = new String[names.size()][2];

        for (ObjectName name : names) {

            nameArr[count][0] = name.getDomain();
            nameArr[count][1] = name.getCanonicalName();


            count++;

        }

        return nameArr;

    }

    public String[] getMBeanAttributeInfo(String objName)
            throws MalformedObjectNameException, IntrospectionException,
                   InstanceNotFoundException, IOException, ReflectionException {

        MBeanAttributeInfo[] attrs = mbsc.getMBeanInfo(new ObjectName(objName)).getAttributes();

        String[] strAttrs = new String[attrs.length];
        int count = 0;
        for (MBeanAttributeInfo info : attrs) {
            strAttrs[count] = info.getName();

            count++;

        }

        return strAttrs;
    }

    public Object getAttribute(String mBean, String attr) {


        //TODO: should find an elegant way to catch the non existing attributes
        try {
            ObjectName mBeanName = new ObjectName(mBean);

            return mbsc.getAttribute(mBeanName, attr);


        } catch (MBeanException e1) {
            e1.printStackTrace();
        } catch (AttributeNotFoundException e1) {
            e1.printStackTrace();
        } catch (InstanceNotFoundException e1) {
            e1.printStackTrace();
        } catch (ReflectionException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (MalformedObjectNameException e1) {
            e1.printStackTrace();
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }


        return null;
    }
}
