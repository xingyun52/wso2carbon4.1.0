/*
 * Copyright 2005-2012 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.appfactory.application.mgt.service;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;
import org.wso2.carbon.appfactory.application.mgt.internal.ServiceReferenceHolder;
import org.wso2.carbon.appfactory.application.mgt.util.Util;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;

/**
 * This class provides a queue for the application create process that using
 * CreateApplication BPEL.
 * This thread run on specific time interval and it can configure on
 * Appfactory.xml file as ApplicationCreatorDelay in Root.
 * 
 * ApplicationCreator is a Singleton class.
 * Every time when we use this creation process, after adding to the queue we
 * should call startCreator(); static method.
 * Because if there are no any application to create , thread exit and instance
 * goes to release.
 * 
 */
public class ApplicationCreator extends Thread {

    private static final Log log = LogFactory.getLog(ApplicationCreator.class);

    // Application creation default time gap
    private long timeDelay = 50;

    private static ApplicationCreator applicationCreator = null;

    public static synchronized ApplicationCreator startCreator() {
        if (ApplicationCreator.applicationCreator == null) {
            synchronized (ApplicationCreator.class) {
                if (ApplicationCreator.applicationCreator == null) {
                    ApplicationCreator.applicationCreator = new ApplicationCreator();
                }
            }
        }
        return ApplicationCreator.applicationCreator;
    }

    private ApplicationCreator() {
        try {
            // Read application creation time delay from the Appfactory.xml
            String[] delayValues = Util.getConfiguration().getProperties("ApplicationCreatorDelay");
            if (delayValues != null && delayValues.length > 0) {
                long tmp = Long.parseLong(delayValues[0]);
                if (tmp > 0) {
                    timeDelay = tmp;
                }
            }
        } catch (Exception e) {

        }
        start();
    }

    public void run() {
        try {
            AppFactoryConfiguration configuration = Util.getConfiguration();
            final String EPR =
                    configuration.getFirstProperty(AppFactoryConstants.BPS_SERVER_URL) +
                    "CreateApplication";

            // check availability of queue. If no , thread is being closed.
            while (ApplicationManagementService.applicationCreationQueue.size() > 0) {
                // poll the application object from the queue
                ApplicationInfoBean applicationInfoBean =
                                                          ApplicationManagementService.applicationCreationQueue.poll();
                if (applicationInfoBean != null) {

                    try {

                        ServiceClient client =
                                               new ServiceClient(
                                                                 ServiceReferenceHolder.getInstance()
                                                                                       .getConfigContextService()
                                                                                       .getClientConfigContext(),
                                                                 null);

                        // Set the endpoint address
                        client.getOptions().setTo(new EndpointReference(EPR));
                        client.engageModule("rampart");
                        client.engageModule("addressing");
                        String userName =
                                          configuration.getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME);
                        String password =
                                          configuration.getFirstProperty(AppFactoryConstants.SERVER_ADMIN_PASSWORD);
                        client.getOptions().setUserName(userName);
                        client.getOptions().setPassword(password);
                        client.getOptions().setTimeOutInMilliSeconds(1000000);

                        String configs = CarbonUtils.getCarbonConfigDirPath();
                        Policy policy = loadPolicy(configs + "/appfactory/bpel-policy.xml");

                        client.getOptions().setAction("http://wso2.org");
                        client.getOptions().setProperty(RampartMessageData.KEY_RAMPART_POLICY,
                                                        policy);

                        // call bpel ApplicationCreation using
                        // applicationInfoBean
                        client.fireAndForget(getPayload(applicationInfoBean));
                        client.cleanup();
                        log.info("application creation is initiated for application:"+applicationInfoBean.getApplicationKey());

                    } catch (Exception e) {
                        log.error(e);
                    }
                }

                try {
                    // waiting for specific time interval
                    Thread.sleep(timeDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            // if exit from the waiting loop, applicationCreator instance
            // release.
            ApplicationCreator.applicationCreator = null;
        }

    }
    //Generate Policy Document
    private static Policy loadPolicy(String xmlPath) throws Exception {
        StAXOMBuilder builder = new StAXOMBuilder(xmlPath);
        return PolicyEngine.getPolicy(builder.getDocumentElement());
    }

    //Generate Payload for the CreateApplicationRequest service operation that in ApplicationManagementService
    private static OMElement getPayload(ApplicationInfoBean applicationInfoBean) throws XMLStreamException,javax.xml.stream.XMLStreamException {

        String payload = "   <p:CreateApplicationRequest xmlns:p=\"http://wso2.org\">\n" +
                         "      <applicationId xmlns=\"http://wso2.org\">" + applicationInfoBean.getApplicationKey() + "</applicationId>\n" +
                         "      <userName xmlns=\"http://wso2.org\">" + applicationInfoBean.getOwnerUserName() + "</userName>\n" +
                         "      <repositoryType xmlns=\"http://wso2.org\">" + applicationInfoBean.getRepositoryType() + "</repositoryType>\n" +
                         "      <adminUserName xmlns=\"http://wso2.org\">" +  Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME) + "</adminUserName>\n" +
                         "   </p:CreateApplicationRequest>";
        return new StAXOMBuilder(new ByteArrayInputStream(payload.getBytes())).getDocumentElement();
    }


}