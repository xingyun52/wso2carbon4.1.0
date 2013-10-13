/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.registry.resource.services.utils;

import javax.activation.DataSource;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class InputStreamBasedDataSource implements DataSource {

    private InputStream inputStream;

    public InputStreamBasedDataSource(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("Writting new data is not supported for InputStreamBasedDataSource.");
    }

    public String getContentType() {
        return "application/octet-stream";
    }

    public String getName() {
        return "inputstream based datasource";
    }
}
