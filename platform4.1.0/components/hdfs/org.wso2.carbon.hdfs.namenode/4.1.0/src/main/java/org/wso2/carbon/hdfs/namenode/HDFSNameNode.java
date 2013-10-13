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
package org.wso2.carbon.hdfs.namenode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;


/**
 * Activate and deactivate HDFS Name Node daemon.
 */
public class HDFSNameNode {
    private static Log log = LogFactory.getLog(HDFSNameNode.class);

    private static String HDFS_NAMENODE_STARTUP_DELAY = "hdfs.namenode.startup.delay";

    private static final String CORE_SITE_XML = "core-site.xml";
    private static final String HDFS_SITE_XML = "hdfs-site.xml";
    private static final String HADOOP_POLICY_XML = "hadoop-policy.xml";
    private static final String MAPRED_SITE_XML = "mapred-site.xml";
    private static final String METRICS2_PROPERTIES = "hadoop-metrics2.properties";


    private Thread thread;

    public HDFSNameNode() {
        Configuration conf = new Configuration(false);
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String hadoopConf = carbonHome + File.separator + "repository" + File.separator +
                "conf" + File.separator + "etc" + File.separator + "hadoop";
        String hadoopCoreSiteConf = carbonHome + File.separator + "repository" + File.separator +
                "conf" + File.separator + "etc" + File.separator + "hadoop" + File.separator + CORE_SITE_XML;
        String hdfsCoreSiteConf = carbonHome + File.separator + "repository" + File.separator +
                "conf" + File.separator + "etc" + File.separator + "hadoop" + File.separator + HDFS_SITE_XML;
        String hadoopPolicyConf = carbonHome + File.separator + "repository" + File.separator +
                "conf" + File.separator + "etc" + File.separator + "hadoop" + File.separator + HADOOP_POLICY_XML;
        String mapredSiteConf = carbonHome + File.separator + "repository" + File.separator +
                "conf" + File.separator + "etc" + File.separator + "hadoop" + File.separator + MAPRED_SITE_XML;
        String hadoopMetrics2Properties = carbonHome + File.separator + "repository" + File.separator +
                "conf" + File.separator + "etc" + File.separator + "hadoop" + File.separator + METRICS2_PROPERTIES;
        conf.addResource(new Path(hadoopCoreSiteConf));
        conf.addResource(new Path(hdfsCoreSiteConf));
        conf.addResource(new Path(hadoopPolicyConf));
        conf.addResource(new Path(mapredSiteConf));
        String alterdJobNameNodeKeyTabPath = hadoopConf + File.separator + conf.get("dfs.namenode.keytab.file");
        conf.set("dfs.namenode.keytab.file", alterdJobNameNodeKeyTabPath);

        try {
            // DefaultMetricsSystem.initialize("namenode");
            NameNode namenode = new NameNode(conf);
//            if (namenode != null) {
//                namenode.join();
//            }
        } catch (Throwable e) {
            log.error("NameNode initialization error." + e);
        }
    }

    /**
     * Starts the Hadoop Name Node daemon
     */
    public void start() {
        thread = new Thread(new Runnable() {
            public void run() {
                if (log.isDebugEnabled()) {
                    log.debug("Activating the HDFS Name Node");
                }
                new HDFSNameNode();
            }
        }, "HDFSNameNode");
        long nameNodeStartupDelay = 0;
        nameNodeStartupDelay = Long.parseLong(System.getProperty(HDFS_NAMENODE_STARTUP_DELAY));
        if (nameNodeStartupDelay > 0) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Waiting for other services - datanode,mapred");
                    log.debug("Name node starup delay is " + nameNodeStartupDelay);
                }
                Thread.sleep(nameNodeStartupDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        thread.start();
    }


    /**
     * Stops the Hadoop Name Node daemon
     */
    public void shutdown() {
        if (log.isDebugEnabled()) {
            log.debug("Deactivating the HDFS Name Node");
        }
        try {
            thread.join();
        } catch (InterruptedException ignored) {
        }
        log.info("HDFS name node shutdown");
    }
}
