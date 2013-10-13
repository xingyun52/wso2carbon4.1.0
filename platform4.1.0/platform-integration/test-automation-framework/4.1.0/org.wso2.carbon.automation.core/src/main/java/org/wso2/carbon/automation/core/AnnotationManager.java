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

import org.testng.IAnnotationTransformer2;
import org.testng.annotations.IConfigurationAnnotation;
import org.testng.annotations.IDataProviderAnnotation;
import org.testng.annotations.IFactoryAnnotation;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class AnnotationManager implements IAnnotationTransformer2 {
    /**
     * Transform an IConfiguration annotation.
     * <p/>
     * Note that only one of the three parameters testClass,
     * testConstructor and testMethod will be non-null.
     *
     * @param annotation      The annotation that was read from your
     *                        test class.
     * @param testClass       If the annotation was found on a class, this
     *                        parameter represents this class (null otherwise).
     * @param testConstructor If the annotation was found on a constructor,
     *                        this parameter represents this constructor (null otherwise).
     * @param testMethod      If the annotation was found on a method,
     *                        this parameter represents this method (null otherwise).
     */
    public void transform(IConfigurationAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {

        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Transform an IDataProvider annotation.
     *
     * @param method The method annotated with the IDataProvider annotation.
     */
    public void transform(IDataProviderAnnotation annotation, Method method) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Transform an IFactory annotation.
     *
     * @param method The method annotated with the IFactory annotation.
     */
    public void transform(IFactoryAnnotation annotation, Method method) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * This method will be invoked by TestNG to give you a chance
     * to modify a TestNG annotation read from your test classes.
     * You can change the values you need by calling any of the
     * setters on the ITest interface.
     * <p/>
     * Note that only one of the three parameters testClass,
     * testConstructor and testMethod will be non-null.
     *
     * @param annotation      The annotation that was read from your
     *                        test class.
     * @param testClass       If the annotation was found on a class, this
     *                        parameter represents this class (null otherwise).
     * @param testConstructor If the annotation was found on a constructor,
     *                        this parameter represents this constructor (null otherwise).
     * @param testMethod      If the annotation was found on a method,
     *                        this parameter represents this method (null otherwise).
     */
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor,
                          Method testMethod) {
      //  annotation.setDataProvider("create");
        //annotation.setDataProviderClass(StaticProvider.class);
                                                                System.out.println("llllllllllllllllllllllllllllllllllllllllllllllllll");
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
