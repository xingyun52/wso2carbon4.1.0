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

public class RunnerSetter {
    private static boolean firstRun = true;
    private static int count;
    private static String suiteName;
    private static boolean isMixedModeRun;

    public static boolean getIsFirstRun() {
        return firstRun;
    }

    public static int getCount() {
        return count;
    }

    public static String getSuiteName() {
        return suiteName;
    }

    public static boolean getMixedModeRun() {
        return isMixedModeRun;
    }

    public static void setRunner(String suiteName, boolean firstRun, int count) {
        RunnerSetter.firstRun = firstRun;
        RunnerSetter.count = count;
        RunnerSetter.suiteName = suiteName;
    }

    public static void setRunner(String suiteName, boolean firstRun) {
        RunnerSetter.firstRun = firstRun;
        RunnerSetter.suiteName = suiteName;
    }

    public static void setRunner(String suiteName, int count) {
        RunnerSetter.count = count;
        RunnerSetter.suiteName = suiteName;
    }

    public static void initRunner() {
        if (firstRun) {
            RunnerSetter.count = 0;
            RunnerSetter.suiteName = "initSuite";
            RunnerSetter.firstRun = true;
        }
    }

    public static void resetRunner() {
        if (firstRun) {
            RunnerSetter.count = 0;
            RunnerSetter.firstRun = true;
        }
    }

    public static void setMixedModeRun(boolean mixedMode) {
        RunnerSetter.isMixedModeRun = mixedMode;
    }

}
