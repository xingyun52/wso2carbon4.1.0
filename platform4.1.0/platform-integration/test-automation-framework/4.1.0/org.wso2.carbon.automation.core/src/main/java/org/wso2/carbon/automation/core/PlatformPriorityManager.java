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

package org.wso2.carbon.automation.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.TestRunner;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.ExecutionMode;
import org.wso2.carbon.automation.core.annotations.InputEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This uses for implement any changes to the execution list of the TestNg , This will reorder the
 * default TestNg xmllist with priority 10
 */


public class PlatformPriorityManager implements IMethodInterceptor {
    private static final Log log = LogFactory.getLog(PlatformPriorityManager.class);

    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context)
            throws SkipException {
        List<IMethodInstance> result = new ArrayList<IMethodInstance>();
        EnvironmentBuilder environmentBuilder = new EnvironmentBuilder();
//        List<XmlClass> testList = new ArrayList<XmlClass>();
//        testList = context.getCurrentXmlTest().getXmlClasses();
        ((TestRunner) context).getTestClasses().toArray()[0].getClass().getAnnotations();
        String executionMode = environmentBuilder.getFrameworkSettings().getEnvironmentSettings()
                .executionMode();
        String environment = environmentBuilder.getFrameworkSettings().getEnvironmentSettings()
                .executionEnvironment();

        for (IMethodInstance method : methods) {
            if (method.getMethod().getMethod().getAnnotation(SetEnvironment.class) != null) {
                ExecutionEnvironment[] annotationList = method.getMethod().getMethod()
                        .getAnnotation(SetEnvironment.class).executionEnvironments();
                for (ExecutionEnvironment annotation : annotationList) {
                    if (annotationComparator(annotation.toString(),
                                             setEnvironment(environment, executionMode),
                                             environment)) {
                        method.getMethod().setTestClass(method.getMethod().getTestClass());
//                        method.getMethod().setMissingGroup(method.getMethod().getTestClass().getName());
                        result.add(method);
                        break;   //  make sure only one test method is added at one time
                    } else {
                        log.info("Skipped method <" + method.getMethod().getMethodName() + "> on annotation <" +
                                 annotation.name() + ">");
                    }
                }
            } else {
                method.getMethod().setTestClass(method.getMethod().getTestClass());
//                method.getMethod().setMissingGroup(method.getMethod().getTestClass().getName());
                result.add(method);
            }
        }
        Comparator<IMethodInstance> comparator = new Comparator<IMethodInstance>() {
            public int compare(IMethodInstance o1, IMethodInstance o2) {
                return (o1.getMethod().getMethod().getAnnotation(Test.class).priority() -
                        o2.getMethod().getMethod().getAnnotation(Test.class).priority());
            }

            public boolean equals(Object obj) {
                return false;
            }
        };
        Collections.sort(result, comparator);

        return result;
    }


    private IMethodInstance addExecutionGroupToMethod(IMethodInstance method, String group) {
        method.getMethod().setMissingGroup(group);
        return method;
    }

    private boolean annotationComparator(String annotation, String annotationEnvironment,
                                         String executionEnvironment) {
        boolean compSetup = false;
        if (annotation.equals(ExecutionEnvironment.all.name())) {
            compSetup = true;
        } else if (annotation.equals(ExecutionEnvironment.integration_all.name())) {
            if (executionEnvironment.equals(InputEnvironment.integration.name())) {
                compSetup = true;
            }
        } else if (annotation.equals(ExecutionEnvironment.platform_all.name())) {
            if (executionEnvironment.equals(InputEnvironment.platform.name())) {
                compSetup = true;
            }
        } else if (annotation.equals(ExecutionEnvironment.stratos.name())) {
            if (executionEnvironment.equals(InputEnvironment.stratos.name())) {
                compSetup = true;
            }
        } else {
            if (annotation.equals(annotationEnvironment)) {
                compSetup = true;
            } else {
                compSetup = false;
            }
        }
        return compSetup;
    }


    private String setEnvironment(String executionEnvironment, String executionMode) {
        String environment = null;
        if (executionEnvironment.equals(InputEnvironment.integration.name())) {
            if (executionMode.equals(ExecutionMode.all.name())) {
                environment = ExecutionEnvironment.integration_all.toString();
            } else if (executionMode.equals(ExecutionMode.user.name())) {
                environment = ExecutionEnvironment.integration_user.toString();
            } else if (executionMode.equals(ExecutionMode.tenant.name())) {
                environment = ExecutionEnvironment.integration_tenant.toString();
            }
        } else if (executionEnvironment.equals(InputEnvironment.platform.name())) {
            if (executionMode.equals(ExecutionMode.all.name())) {
                environment = ExecutionEnvironment.platform_all.toString();
            } else if (executionMode.equals(ExecutionMode.user.name())) {
                environment = ExecutionEnvironment.platform_user.toString();
            } else if (executionMode.equals(ExecutionMode.tenant.name())) {
                environment = ExecutionEnvironment.platform_tenant.toString();
            }
        } else if (executionEnvironment.equals(InputEnvironment.stratos.name())) {
            environment = ExecutionEnvironment.stratos.toString();
        } else if (executionEnvironment.equals(InputEnvironment.all.name())) {
            environment = ExecutionEnvironment.all.toString();
        }
        return environment;
    }

}
