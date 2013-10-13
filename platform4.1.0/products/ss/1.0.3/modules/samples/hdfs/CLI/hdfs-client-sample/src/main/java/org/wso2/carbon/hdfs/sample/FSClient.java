package org.wso2.carbon.hdfs.sample;

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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.File;
import java.io.IOException;


/**
 * HDFS Client sample
 */

public class FSClient {
    public static final String message = "Hello, HDFS World!\n";
    public static final String USER_HOME = "user";
    public static final String TENANT = "wso2";
    public static final String FILE_NAME = "wso2-hdfs-sample-9.txt";

    public static void main(String[] args) throws IOException {

        Configuration conf = new Configuration(false);
        /**
         * Create HDFS Client configuration to use name node hosted on host master.
         * Client configured to connect to a remote distributed file system.
         */
        conf.set("fs.default.name", "hdfs://localhost:54310");
        conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        conf.set("hadoop.security.authentication", "kerberos");
        conf.set("dfs.namenode.kerberos.principal","hdfs/node0@WSO2.ORG");

        UserGroupInformation.setConfiguration(conf);
        /**
         * Get connection to remote file sytem
         */
        FileSystem fs = FileSystem.get(conf);

        /**
         * Create file path object
         */
        Path tenantFileName = new Path(File.separator + USER_HOME + File.separator + TENANT + File.separator + FILE_NAME);
        /**
         * Do read / write operation with HDFS
         */
        try {
            if (fs.exists(tenantFileName)) {
                // remove the file first
                fs.delete(tenantFileName,true);
            }

            //create and put content to the file
            FSDataOutputStream out = fs.create(tenantFileName);
            out.writeUTF(message);
            out.close();

            FSDataInputStream in = fs.open(tenantFileName);
            String messageIn = in.readUTF();
            System.out.print(messageIn);
            in.close();

        } catch (IOException ioe) {
            System.err.println("IOException during operation: " + ioe.toString());
            System.exit(1);
        }
    }

}
