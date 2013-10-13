/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.automation.core.utils.suiteutills;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.wso2.carbon.automation.core.PlatformAnnotationTransferManager;
import org.wso2.carbon.automation.core.PlatformExecutionManager;
import org.wso2.carbon.automation.core.PlatformPriorityManager;
import org.wso2.carbon.automation.core.PlatformReportManager;
import org.wso2.carbon.automation.core.PlatformSuiteManager;
import org.wso2.carbon.automation.core.PlatformTestManager;
import org.wso2.carbon.automation.core.ProductConstant;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MasterSuite {
    private static final Log log = LogFactory.getLog(MasterVirtualTestSuite.class);
    private Map<String, String> parameters = new HashMap<String, String>();
    private static int counter = 0;


    public TestNG superSuite(String SuiteName, List<SuiteVariables> suiteVariablesList) {
        XmlSuite suite = new XmlSuite();
        suite.setName(SuiteName);
        suite.setVerbose(1);
        suite.setThreadCount(2);
        log.info("[TESTAUTOMATION]----" + SuiteName);

        parameters.put("first-name", "Automation");

        suite.setParameters(parameters);
        for (SuiteVariables suiteVariables : suiteVariablesList) {

            XmlTest test = new XmlTest(suite);
            test.setName(suiteVariables.geTestName());
            test.setExcludedGroups(Arrays.asList(suiteVariables.getExcludeGrops()));
            XmlClass[] classes = new XmlClass[]{
                    new XmlClass(suiteVariables.getTestClass()),
            };
            test.setXmlClasses(Arrays.asList(classes));
        }
        TestNG tng = new TestNG();
        List<Class> listnerClasses = new ArrayList<Class>();
        listnerClasses.add(PlatformTestManager.class);
        listnerClasses.add(PlatformExecutionManager.class);
        listnerClasses.add(PlatformSuiteManager.class);
        listnerClasses.add(PlatformReportManager.class);
        listnerClasses.add(PlatformAnnotationTransferManager.class);

        System.out.println("XXXXXXXX + count " + counter);

        if (counter == 0) {
            listnerClasses.add(PlatformExecutionManager.class);
            counter++;
        }
        listnerClasses.add(PlatformPriorityManager.class);
        tng.setListenerClasses(listnerClasses);
        tng.setDefaultSuiteName(SuiteName);
        tng.setXmlSuites(Arrays.asList(new XmlSuite[]{suite}));
        tng.setOutputDirectory(ProductConstant.REPORT_LOCATION + File.separator + "surefire-reports");
        return tng;
    }

    public void setServerList(String serverList) {
        parameters.put("server.list", serverList);
    }

}
