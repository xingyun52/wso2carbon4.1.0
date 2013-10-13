/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.mediator.autoscale.lbautoscale.callables;

import java.util.concurrent.Callable;

import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration.ServiceConfiguration;
import org.wso2.carbon.mediator.autoscale.lbautoscale.clients.CloudControllerClient;
import org.wso2.carbon.mediator.autoscale.lbautoscale.context.AppDomainContext;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleConstants;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleUtil;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscalerTaskDSHolder;

/** Take auto-scaling decisions for each service domain, sub domain combination **/
public class AutoscaleDeciderCallable implements Callable<Boolean> {

    private static final Log log = LogFactory.getLog(AutoscaleDeciderCallable.class);
    private String domain;
    private String subDomain;
    private CloudControllerClient client;
    private AppDomainContext appDomainContext;
    private ServiceConfiguration serviceConfig;
    private String clusterStr;
    
    public AutoscaleDeciderCallable(String domain, String subDomain, CloudControllerClient client, AppDomainContext appCtxt){
        this.domain = domain;
        this.subDomain = subDomain;
        this.client = client;
        this.appDomainContext = appCtxt;
        clusterStr = AutoscaleUtil.domainSubDomainString(domain, subDomain);
    }
    
    @Override
    public Boolean call() throws Exception {

        // expire tokens
        if (appDomainContext != null) {
            appDomainContext.expireRequestTokens();

            serviceConfig = appDomainContext.getServiceConfig();

            appDomainContext.recordRequestTokenListLength();
            if (!appDomainContext.canMakeScalingDecision()) {
                return true;
            }

            long average = appDomainContext.getAverageRequestsInFlight();
            int runningAppInstances = appDomainContext.getRunningInstanceCount();

            int maxRPS = serviceConfig.getMaxRequestsPerSecond();
            double taskInterval =
                AutoscalerTaskDSHolder
                    .getInstance()
                    .getWholeLoadBalancerConfig()
                    .getLoadBalancerConfig()
                    .getAutoscalerTaskInterval() / (double)1000;
            double aur = serviceConfig.getAlarmingUpperRate();
            double alr = serviceConfig.getAlarmingLowerRate();
            double scaleDownFactor = serviceConfig.getScaleDownFactor();

            // scale up early
            double maxhandleableReqs = maxRPS * taskInterval * aur;
            // scale down slowly
            double minhandleableReqs = maxRPS * taskInterval * alr * scaleDownFactor;

            if (log.isDebugEnabled()) {
                log.debug(clusterStr +": Average requests in flight: " + average + " **** Handleable requests: " +
                    (runningAppInstances * maxhandleableReqs));
            }
            if (average > (runningAppInstances * maxhandleableReqs)) {
                // current average is high than that can be handled by current nodes.
                scaleUp();
            } else if (average < ((runningAppInstances - 1) * minhandleableReqs)) {
                // current average is less than that can be handled by (current nodes - 1).
                scaleDown();
            }
        }

        return true;
    }

    private void scaleDown() {

        int runningInstances = appDomainContext.getRunningInstanceCount();
        int pendingInstances = appDomainContext.getPendingInstanceCount();
        int minAppInstances = serviceConfig.getMinAppInstances();
        int serverStartupDelay = AutoscalerTaskDSHolder
                                            .getInstance()
                                            .getWholeLoadBalancerConfig()
                                            .getLoadBalancerConfig()
                                            .getServerStartupDelay();

        if (runningInstances > minAppInstances) {

            if (log.isDebugEnabled()) {
                log.debug("Scale Down - " +
                    clusterStr +
                        ". Running instances:" +
                        runningInstances +
                        ". Pending instances: " +
                        pendingInstances +
                        ". Min instances:" +
                        minAppInstances);
            }
            // ask to scale down
            try {
                if (client.terminateInstance(domain, subDomain)) {
                    
                        log.debug(clusterStr +": There's an instance who's in shutting down state " +
                            "(but still not left ELB), hence we should wait till " +
                            "it leaves the cluster.");

                        int totalWaitedTime = 0;

                        log.debug(clusterStr +": Task will wait maximum of (milliseconds) : " +
                            serverStartupDelay +
                                ", to let the member leave the cluster.");
                        
                        // for each sub domain, get the clustering group management agent
                        GroupManagementAgent agent =
                            AutoscalerTaskDSHolder.getInstance().getAgent()
                                .getGroupManagementAgent(domain,
                                    subDomain);

                        // we give some time for the server to be terminated, we'll check time to time
                        // whether the instance has actually left the cluster.
                        while (agent.getMembers().size() == runningInstances &&
                            totalWaitedTime < serverStartupDelay) {

                            try {
                                Thread.sleep(AutoscaleConstants.INSTANCE_REMOVAL_CHECK_TIME);
                            } catch (InterruptedException ignore) {
                            }

                            totalWaitedTime += AutoscaleConstants.INSTANCE_REMOVAL_CHECK_TIME;
                        }

                        log.debug(clusterStr+ " : task waited for (milliseconds) : " + totalWaitedTime);

                        // we recalculate number of alive instances
                        runningInstances = agent.getMembers().size();
                        
                        appDomainContext.setRunningInstanceCount(runningInstances);

                        log.debug(clusterStr+" : New running instance count: " + runningInstances);
                }

            } catch (Exception e) {
                log.error("Instance termination failed for " + clusterStr);
            }
        }

    }

    private void scaleUp() {

        int maxAppInstances = serviceConfig.getMaxAppInstances();
        int instancesPerScaleUp = serviceConfig.getInstancesPerScaleUp();
        int runningInstances = appDomainContext.getRunningInstanceCount();
        int pendingInstances = appDomainContext.getPendingInstanceCount();

        int failedInstances = 0;
        if (runningInstances < maxAppInstances && pendingInstances == 0) {

            log.debug(clusterStr + " - Going to start " +
                instancesPerScaleUp + " instance/s. Running instances:" + runningInstances);

            int successfullyStarted =
                AutoscaleUtil.runInstances(client, appDomainContext, domain, subDomain,
                    instancesPerScaleUp);

            if (successfullyStarted != instancesPerScaleUp) {
                failedInstances = instancesPerScaleUp - successfullyStarted;
                if (log.isDebugEnabled()) {
                    log.debug(successfullyStarted +
                        " instances successfully started and\n" + failedInstances +
                        " instances failed to start for " + clusterStr);
                }
            }

            // we increment the pending instance count
            // appDomainContext.incrementPendingInstances(instancesPerScaleUp);
            else {
                log.debug("Successfully started " + successfullyStarted +
                    " instances of " + clusterStr);
            }

        } else if (runningInstances > maxAppInstances) {
            log.fatal("Number of running instances has over reached the maximum limit of " +
                maxAppInstances + " of " + clusterStr);
        }
    }

}
