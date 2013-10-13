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
import org.testng.Assert;
import org.testng.IExecutionListener;
import org.wso2.carbon.aarservices.stub.ExceptionException;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.api.clients.aar.services.AARServiceUploaderClient;
import org.wso2.carbon.automation.api.clients.service.mgt.ServiceAdminClient;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.ExecutionMode;
import org.wso2.carbon.automation.core.utils.axis2serverutils.SampleAxis2Server;
import org.wso2.carbon.automation.core.utils.coreutils.PlatformUtil;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentVariables;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkFactory;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkProperties;
import org.wso2.carbon.automation.core.utils.virtualTestRunUtils.RunnerSetter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * implementation of  testNG IExecutionListener, this class will call before all test suite..
 * However if you use multiple test module, onExecutionStart and onExecutionFinish methods will call multiple times
 */
public class PlatformExecutionManager implements IExecutionListener {
    private static final Log log = LogFactory.getLog(PlatformExecutionManager.class);
    private ServerGroupManager serverGroupManager;
    private boolean builderEnabled;
    private List<String> serverList;
    private SampleAxis2Server sampleAxis2Server;

    /**
     * calls before all test suites execution
     */
    public void onExecutionStart() {
        EnvironmentBuilder environmentBuilder = new EnvironmentBuilder();
        RunnerSetter.initRunner();
        PlatformUtil.setKeyStoreProperties(); //set keyStore properties
        log.info("---------------Test Execution Started --------------------------");
        serverList = getServerList();
        builderEnabled =
                environmentBuilder.getFrameworkSettings().getEnvironmentSettings().is_builderEnabled();
        String executionMode = environmentBuilder.getFrameworkSettings().getEnvironmentSettings().executionMode();
        String environment = environmentBuilder.getFrameworkSettings().getEnvironmentSettings().executionEnvironment();
        /* If Execution mode is tenant executes main execution as tenant*/
        if (executionMode.equals(ExecutionMode.tenant.name()) && !environment.equalsIgnoreCase(ExecutionEnvironment.stratos.name())) {
            setEnvoronmentSettingsForTenant(true);
        }
        if (builderEnabled) {
            startSevers();
        } else {
            for (String server : serverList) {
                if (server.equals(ProductConstant.ESB_SERVER_NAME)) {
                    log.info("Uploading Services to App Server...");
                    try {
                        new UserPopulator().populateUsers(Arrays.asList(ProductConstant.APP_SERVER_NAME));
                    } catch (Exception e) {
                        log.fatal("User Creation failed. " + e);
                    }
                    log.info("Uploading Services to App Server...");
                    try {
                        EnvironmentBuilder builder;
                        builder = new EnvironmentBuilder().as(ProductConstant.ADMIN_USER_ID);

                        EnvironmentVariables appServer = builder.build().getAs();

                        AARServiceUploaderClient adminServiceAARServiceUploader =
                                new AARServiceUploaderClient(appServer.getBackEndUrl(), appServer.getSessionCookie());
                        ServiceAdminClient adminServiceService = new ServiceAdminClient(appServer.getBackEndUrl(), appServer.getSessionCookie());
                        if (adminServiceService.isServiceExists(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE)) {
                            adminServiceService.deleteService(new String[]{SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE});
                            isServiceUnDeployed(appServer.getBackEndUrl(), appServer.getSessionCookie()
                                    , SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE
                                    , builder.getFrameworkSettings().getEnvironmentVariables().getDeploymentDelay());
                        }

                        if (adminServiceService.isServiceExists(SampleAxis2Server.SECURE_STOCK_QUOTE_SERVICE)) {
                            adminServiceService.deleteService(new String[]{SampleAxis2Server.SECURE_STOCK_QUOTE_SERVICE});
                            isServiceUnDeployed(appServer.getBackEndUrl(), appServer.getSessionCookie()
                                    , SampleAxis2Server.SECURE_STOCK_QUOTE_SERVICE
                                    , builder.getFrameworkSettings().getEnvironmentVariables().getDeploymentDelay());
                        }
                        adminServiceAARServiceUploader.uploadAARFile(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE + ".aar"
                                , ProductConstant.getResourceLocations(ProductConstant.AXIS2_SERVER_NAME)
                                  + File.separator + "aar" + File.separator + "SimpleStockQuoteService.aar"
                                , "");
                        Assert.assertTrue(isServiceDeployed(appServer.getBackEndUrl(), appServer.getSessionCookie()
                                , SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE
                                , builder.getFrameworkSettings().getEnvironmentVariables().getDeploymentDelay())
                                , "SimpleStockQuoteService deployment failed in Application Server");
                        adminServiceAARServiceUploader.uploadAARFile(SampleAxis2Server.SECURE_STOCK_QUOTE_SERVICE + ".aar"
                                , ProductConstant.getResourceLocations(ProductConstant.AXIS2_SERVER_NAME)
                                  + File.separator + "aar" + File.separator + "SecureStockQuoteService.aar"
                                , "");
                        Assert.assertTrue(isServiceDeployed(appServer.getBackEndUrl(), appServer.getSessionCookie()
                                , SampleAxis2Server.SECURE_STOCK_QUOTE_SERVICE
                                , builder.getFrameworkSettings().getEnvironmentVariables().getDeploymentDelay())
                                , "SecureStockQuoteService deployment failed in Application Server");
                    } catch (RemoteException e) {
                        log.fatal("Artifact Deployment in Application Server failed. " + e);
                    } catch (LoginAuthenticationExceptionException e) {
                        log.fatal("Artifact Deployment in Application Server failed. " + e);
                    } catch (MalformedURLException e) {
                        log.fatal("Artifact Deployment in Application Server failed. " + e);
                    } catch (ExceptionException e) {
                        log.fatal("Artifact Deployment in Application Server failed. " + e);
                    } catch (Exception e) {
                        log.fatal("Artifact Deployment in Application Server failed. " + e);
                    }
                    break;
                }
            }

        }
    }


    /**
     * calls after all test suite execution
     */
    public void onExecutionFinish() {
        log.info("---------------Test Execution Finished --------------------------");
        stopServers();
    }

    private void startSevers() {
        assert serverList != null : "server list not provided, cannot start servers";
        int defaultPortOffset = 0;
        serverGroupManager = new ServerGroupManager(defaultPortOffset);
        if (!RunnerSetter.getMixedModeRun()) {
            for (String server : serverList) {
                try {
                    serverGroupManager.startServersForBuilder(server);

                    if (server.equals(ProductConstant.ESB_SERVER_NAME)) {
                        startSimpleAxis2Server();
                    }
                } catch (IOException e) {
                    log.error("Unable to start servers ", e);
                }
                /*try {
                    new UserPopulator().populateUsers(serverList);
                } catch (Exception e) {
                    log.error("Unable to populate users in to servers ", e);
                }*/
            }
        }
    }

    private void stopServers() {
        if (!RunnerSetter.getMixedModeRun()) {
            if (builderEnabled && serverList != null) {
                for (String server : serverList) {
                    FrameworkProperties frameworkProperties = FrameworkFactory.getFrameworkProperties(server);
                    try {
                        serverGroupManager.stopServer(frameworkProperties);
                        if (server.equals(ProductConstant.ESB_SERVER_NAME)) {
                            if (sampleAxis2Server != null) {
                                if (sampleAxis2Server.isStarted()) {
                                    stopSimpleAxis2Server();
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Unable to stop servers ", e);
                    }
                }
            }
        }
    }

    private void startSimpleAxis2Server() throws IOException {
        sampleAxis2Server = new SampleAxis2Server();
        sampleAxis2Server.start();
        log.info("Deploying services..............");
        sampleAxis2Server.deployService(SampleAxis2Server.LB_SERVICE_1);
        sampleAxis2Server.deployService(SampleAxis2Server.SIMPLE_STOCK_QUOTE_SERVICE);
        sampleAxis2Server.deployService(SampleAxis2Server.SECURE_STOCK_QUOTE_SERVICE);
    }

    private void stopSimpleAxis2Server() {
        sampleAxis2Server.stop();
    }

    private List<String> getServerList() {
        if (System.getProperty("server.list") != null) {
            return Arrays.asList(System.getProperty("server.list").split(","));
        }
        return null;
    }

    private void setEnvoronmentSettingsForTenant(boolean setting) {
        System.setProperty("integration.stratos.cycle", String.valueOf(setting));
    }

    private boolean isServiceDeployed(String backEndUrl, String sessionCookie, String serviceName,
                                      int deploymentDelay)
            throws RemoteException {
        log.info("waiting " + deploymentDelay + " millis for Service deployment " + serviceName);

        boolean isServiceDeployed = false;
        ServiceAdminClient adminServiceService = new ServiceAdminClient(backEndUrl, sessionCookie);
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) < deploymentDelay) {
            if (adminServiceService.isServiceExists(serviceName)) {
                isServiceDeployed = true;
                log.info(serviceName + " Service Deployed in " + time + " millis");
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
        }

        return isServiceDeployed;

    }

    private boolean isServiceUnDeployed(String backEndUrl, String sessionCookie, String serviceName,
                                        int deploymentDelay)
            throws RemoteException {
        log.info("waiting " + deploymentDelay + " millis for Service undeployment");
        ServiceAdminClient adminServiceService = new ServiceAdminClient(backEndUrl, sessionCookie);
        boolean isServiceDeleted = false;
        Calendar startTime = Calendar.getInstance();
        long time;
        while ((time = (Calendar.getInstance().getTimeInMillis() - startTime.getTimeInMillis())) < deploymentDelay) {
            if (!adminServiceService.isServiceExists(serviceName)) {
                isServiceDeleted = true;
                log.info(serviceName + " Service undeployed in " + time + " millis");
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }
        }
        return isServiceDeleted;
    }

}
