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

package org.wso2.carbon.automation.core.utils.virtualTestRunUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.plugin.MojoFailureException;
import org.testng.ISuite;
import org.testng.ITestNGMethod;
import org.testng.TestNG;
import org.wso2.carbon.automation.core.utils.reportutills.CustomTestNgReportSetter;
import org.wso2.carbon.automation.core.utils.suiteutills.MasterVirtualTestSuite;
import org.wso2.carbon.automation.core.utils.suiteutills.SuiteVariables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class VirtualSuiteRunner extends MasterVirtualTestSuite {
    private static final Log log = LogFactory.getLog(VirtualSuiteRunner.class);

    public void testset(ISuite suite) throws MojoFailureException {
        List<SuiteVariables> suiteVariablesList = new ArrayList<SuiteVariables>();
        List<Class> classLintInMethods = new ArrayList();
        int classcount = 0;
        HashSet hs = new HashSet();

        for (ITestNGMethod method : suite.getAllMethods()) {
            classLintInMethods.add(method.getRealClass());
        }
        hs.addAll(classLintInMethods);
        classLintInMethods.clear();
        classLintInMethods.addAll(hs);
        for (Class testClass : classLintInMethods) {
            classcount++;
            suiteVariablesList.add(new SuiteVariables("MixedTest-" + classcount+"-"+testClass.getName(), testClass));
        }
        try {
            TestNG testNG = superSuite("MixedModeSuite-" + suite.getName(), suiteVariablesList);
            RunnerSetter.setMixedModeRun(true);
            testNG.run();
            RunnerSetter.setMixedModeRun(false);
        } catch (Exception e) {
            RunnerSetter.setMixedModeRun(false);
            log.error("Error while executing suite " + suite.getName() + " in Mexed Mode" + e);
            CustomTestNgReportSetter reportSetter = new CustomTestNgReportSetter();
            reportSetter.createReport(suite, e);
        }
    }
}
