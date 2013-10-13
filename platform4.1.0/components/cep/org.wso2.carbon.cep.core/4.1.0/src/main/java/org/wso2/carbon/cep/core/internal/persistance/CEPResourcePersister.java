/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.cep.core.internal.persistance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.Bucket;
import org.wso2.carbon.cep.core.internal.config.BucketHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class CEPResourcePersister {
    private static final Log log = LogFactory.getLog(CEPResourcePersister.class);

    public static boolean save(String bucket, String bucketName,
                               String bucketPath) {
        try {
            /* save contents to .xml file */
            BufferedWriter out = new BufferedWriter(new FileWriter(bucketPath));
            out.write(new XmlFormatter().format(bucket));
            out.close();
            log.info(bucketName + " successfully saved");
        } catch (IOException e) {
            log.error("Error while saving " + bucketName, e);
        }
        return true;
    }

    public static boolean delete(String bucketPath) {
        File file = new File(bucketPath);
        if (file.exists()) {
            boolean fileDeleted = file.delete();
            if (!fileDeleted) {
                log.error("Could not delete " + bucketPath);
            }
        }
        return true;
    }

    public static boolean save(Bucket bucket, String bucketPath) {
        return save(BucketHelper.bucketToOM(bucket).toString(), bucket.getName(), bucketPath);
    }

}
