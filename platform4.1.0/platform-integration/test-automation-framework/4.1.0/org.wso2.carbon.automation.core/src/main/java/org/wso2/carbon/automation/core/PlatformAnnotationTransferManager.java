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
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.ExecutionMode;
import org.wso2.carbon.automation.core.annotations.InputEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class PlatformAnnotationTransferManager implements IAnnotationTransformer {
    private static final Log log = LogFactory.getLog(PlatformAnnotationTransferManager.class);

    public void transform(ITestAnnotation iTestAnnotation, Class aClass, Constructor constructor,
                          Method method) {

        EnvironmentBuilder environmentBuilder = new EnvironmentBuilder();
//        aClass = method.getClass();
        String executionMode = environmentBuilder.getFrameworkSettings().getEnvironmentSettings().executionMode();

        String environment = environmentBuilder.getFrameworkSettings().getEnvironmentSettings()
                .executionEnvironment();

        if (method.getDeclaringClass().getAnnotation(SetEnvironment.class) != null) {
            ExecutionEnvironment[] classAnnotationList = method.getDeclaringClass().getAnnotation(SetEnvironment.class).executionEnvironments();
            compareAnnotation(iTestAnnotation, method, executionMode, environment, classAnnotationList);
        } else if (method.getAnnotation(SetEnvironment.class) != null) {
            ExecutionEnvironment[] annotationList = method.getAnnotation(SetEnvironment.class).executionEnvironments();
            compareAnnotation(iTestAnnotation, method, executionMode, environment, annotationList);
        } else {
            iTestAnnotation.setGroups(new String[]{method.getClass().getName()});
            iTestAnnotation.setTestName(method.getClass().getName());
        }

    }

    private void compareAnnotation(ITestAnnotation iTestAnnotation, Method method,
                                   String executionMode, String environment,
                                   ExecutionEnvironment[] classAnnotationList) {
        for (ExecutionEnvironment annotation : classAnnotationList) {
            if (annotationComparator(annotation.toString(),
                                     setEnvironment(environment, executionMode),
                                     environment)) {
                iTestAnnotation.setGroups(new String[]{method.getClass().getName()});
                iTestAnnotation.setTestName(method.getClass().getName());
            } else {
                iTestAnnotation.setEnabled(false);
                log.info("Skipped method <" + method.getName() + "> on annotation <" +
                         annotation.name() + ">");
                break;
            }
        }
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
            compSetup = annotation.equals(annotationEnvironment);
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
