/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.governance.custom.lifecycles.history.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

public class
        ResourceServiceClient {

    private static final Log log = LogFactory.getLog(ResourceServiceClient.class);

    private ResourceAdminServiceStub stub;
    private String epr;
    private static final String ADDRESSING_MODULE = "addressing";


    public ResourceServiceClient(
            String cookie, String backendServerURL, ConfigurationContext configContext)
            throws RegistryException {

        epr = backendServerURL + "ResourceAdminService";

        try {
            stub = new ResourceAdminServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate resource service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public ResourceServiceClient(String cookie, ServletConfig config, HttpSession session)
            throws RegistryException {

        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        epr = backendServerURL + "ResourceAdminService";

        try {
            stub = new ResourceAdminServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate resource service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public ResourceServiceClient(ServletConfig config, HttpSession session)
            throws RegistryException {

        String cookie = (String)session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        epr = backendServerURL + "ResourceAdminService";

        try {
            stub = new ResourceAdminServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate resource service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }


    public String getTextContent(String path) throws Exception {


        String textContent = null;
        try {
            textContent = stub.getTextContent(path);

        } catch (Exception e) {

            String msg = "Failed get text content of the resource " +
                    path + ". " + e.getMessage();
            log.error(msg, e);
            throw e;
        }
        return textContent;
    }


    public String getProperty(String path, String key) throws Exception {

        try {
            return stub.getProperty(path, key);
        } catch (Exception e) {

            String msg = "Failed to get property with key :" + key + " form the resource in path " +
                    path + ". Error :" + e.getMessage();
            log.error(msg, e);
            throw e;
        }
    }

}
