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
import org.apache.maven.plugin.MojoFailureException;
import org.testng.Assert;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.ExecutionMode;
import org.wso2.carbon.automation.core.utils.coreutils.PlatformUtil;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.reportutills.CustomTestNgReportSetter;
import org.wso2.carbon.automation.core.utils.serverutils.ServerManager;
import org.wso2.carbon.automation.core.utils.virtualTestRunUtils.RunnerSetter;
import org.wso2.carbon.automation.core.utils.virtualTestRunUtils.VirtualSuiteRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlatformSuiteManager implements ISuiteListener {

    private static final Log log = LogFactory.getLog(PlatformSuiteManager.class);
    ServerManager serverManager = null;
    List<ServerManager> serverList = new ArrayList<ServerManager>();
    EnvironmentBuilder environmentBuilder;
    ServerGroupManager serverGroupManager;
    String environmet;
    String executionMode;
    List<String> defaultProductList;
    ISuite currentSuite = null;


    /**
     * This method is invoked before the SuiteRunner starts.
     */
    public synchronized void onStart(ISuite suite) {
        currentSuite = suite;
        PlatformUtil.setKeyStoreProperties();
        int exeCount = RunnerSetter.getCount();
        boolean isFirstExecution = RunnerSetter.getIsFirstRun();
        RunnerSetter.setRunner(suite.getName(), exeCount + 1);
        environmentBuilder = new EnvironmentBuilder();
        environmentBuilder = new EnvironmentBuilder();


        boolean deploymentEnabled =
                environmentBuilder.getFrameworkSettings().getEnvironmentSettings().isEnableDipFramework();
        boolean startosEnabled =
                environmentBuilder.getFrameworkSettings().getEnvironmentSettings().is_runningOnStratos();
        boolean builderEnabled =
                environmentBuilder.getFrameworkSettings().getEnvironmentSettings().is_builderEnabled();
        defaultProductList =
                environmentBuilder.getFrameworkSettings().getEnvironmentVariables().getProductList();
        environmet = environmentBuilder.getFrameworkSettings().getEnvironmentSettings().executionEnvironment();
        executionMode = environmentBuilder.getFrameworkSettings().getEnvironmentSettings().executionMode();
        log.info("**********Starting executing test Suite " + suite.getName() + " on "
                 + executionMode.toString() + "***********");
        try {
            /* If Execution mode is tenant executes main execution as tenant*/
            if (executionMode.equals(ExecutionMode.tenant.name()) && !environmet.equalsIgnoreCase(ExecutionEnvironment.stratos.name())) {
                setEnvoronmentSettingsForTenant(true);
            }
            serverGroupManager = new ServerGroupManager(0);
            if (startosEnabled) {
                UserPopulator populator = new UserPopulator();
                if (!environmet.equals(ExecutionEnvironment.stratos)) {
                    if (executionMode.equals(ExecutionMode.tenant.name()) || executionMode.equals(ExecutionMode.all.name())) {
                        populator.populateUsers(defaultProductList);
                    } else {
                        log.error("\n......................................................" +
                                  "Invalid Environment Settings.......\n " +
                                  "Please Check The automation.properties file \n" +
                                  "......................................................");
                    }
                } else {
                    populator.populateUsers(null);
                }
            } else if (deploymentEnabled) {
                if (suite.getParameter("server.list") != null) {
                    List<String> productList = Arrays.asList(suite.getParameter("server.list").split(","));
                    log.info("Starting servers...");
                    serverGroupManager.startServers(productList);
                    UserPopulator populator = new UserPopulator();
                    populator.populateUsers(defaultProductList);
                } else {
                    log.info("Starting servers with default product list...");
                    serverGroupManager.startServers(defaultProductList);
                    if (executionMode.equals(ExecutionMode.user.name()) || executionMode.equals(ExecutionMode.all.name())) {
                        UserPopulator populator = new UserPopulator();
                        populator.populateUsers(defaultProductList);
                    }
                }
            } else {
                UserPopulator populator = new UserPopulator();
                populator.populateUsers(defaultProductList);
            }

        } catch (Exception e) {  /*cannot throw the exception */
            log.error(e);
            CustomTestNgReportSetter reportSetter = new CustomTestNgReportSetter();
            reportSetter.createReport(suite, e);
        }
    }

    /**
     * This method is invoked after the SuiteRunner has run all
     * the test suites.
     */

    public void onFinish(ISuite suite) {
        VirtualSuiteRunner runner = new VirtualSuiteRunner();
        CustomTestNgReportSetter reportSetter = new CustomTestNgReportSetter();
        /*          If Execution mode is all attempts for the second test execution cycle*/
        log.info("***********Finishing executing test Suite " + suite.getName() + " on "
                 + executionMode.toString() + "*******");
        try {
            EnvironmentBuilder environmentBuilder = new EnvironmentBuilder();

            if (!environmentBuilder.getFrameworkSettings().getEnvironmentSettings().is_builderEnabled()) {
                stopMultipleServers(suite.getParameter("server.list"));
            }
            // Runtime.getRuntime().gc();
        } catch (Exception e) { /*cannot throw the exception */
            log.error(e);
            reportSetter.createReport(suite, e);
            Assert.fail("Fail to stop servers " + e);
        }
        if (((executionMode.equals(ExecutionMode.all.name()))) &&
            RunnerSetter.getCount() <= 1 && !RunnerSetter.getMixedModeRun()) {
            setEnvoronmentSettingsForTenant(true);
            reportSetter.createReport(suite);
            try {
                runner.testset(currentSuite);
                setEnvoronmentSettingsForTenant(false);
            } catch (MojoFailureException e) {
                log.error(e);
                reportSetter.createReport(suite, e);
            }
            reportSetter.createReport(suite);
        }
        RunnerSetter.resetRunner();
    }

    /**
     * Responsible for stopping multiple servers after test execution.
     * <p/>
     * Add the @AfterSuite TestNG annotation in the method overriding this method
     *
     * @param serverList server list required to run test scenario
     * @throws Exception if an error occurs while in server stop process
     */
    protected void stopMultipleServers(String serverList) throws Exception {
        EnvironmentBuilder environmentBuilder = new EnvironmentBuilder();

        if (environmentBuilder.getFrameworkSettings().getEnvironmentSettings().isEnableDipFramework()
            && !environmentBuilder.getFrameworkSettings().getEnvironmentSettings().is_runningOnStratos()) {
            List<String> productList = Arrays.asList(serverList.split(","));
            log.info("Stopping all server");
            ServerGroupManager.shutdownServers(productList);
        }
    }

    private void setEnvoronmentSettingsForTenant(boolean setting) {
        System.setProperty("integration.stratos.cycle", String.valueOf(setting));
    }
}
