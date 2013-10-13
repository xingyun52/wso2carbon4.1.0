/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.automation.tools.jmeter;

import java.io.File;

public class JMeterTest {
    public String jmeterLogLevel = "1";
    private File testFile = null;
    private File jMeterPropertyFile = null;

    public JMeterTest(File script) {
        testFile = script;
    }

    public void setTestFile(File testFile) {
        testFile = testFile;
    }

    public String getLogLevel() {
        return jmeterLogLevel;
    }

    public File getTestFile() {
        return testFile;
    }

    public File getJMeterPropertyFile() {
        return jMeterPropertyFile;
    }

    public void setJMeterPropertyFile(File jMeterPropertyFile) {
        this.jMeterPropertyFile = jMeterPropertyFile;
    }
}
