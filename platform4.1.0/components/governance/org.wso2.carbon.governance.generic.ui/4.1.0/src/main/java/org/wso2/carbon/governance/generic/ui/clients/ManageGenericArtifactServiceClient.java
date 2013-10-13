/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.governance.generic.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.governance.generic.stub.ManageGenericArtifactServiceStub;
import org.wso2.carbon.governance.generic.stub.beans.xsd.ArtifactsBean;
import org.wso2.carbon.governance.generic.stub.beans.xsd.ContentArtifactsBean;
import org.wso2.carbon.governance.generic.stub.beans.xsd.StoragePathBean;
import org.wso2.carbon.governance.generic.ui.utils.GenericUtil;
import org.wso2.carbon.governance.generic.ui.utils.ManageGenericArtifactUtil;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.String;
import java.rmi.RemoteException;

public class ManageGenericArtifactServiceClient {

    private static final Log log = LogFactory.getLog(ManageGenericArtifactServiceClient.class);

    private ManageGenericArtifactServiceStub stub;
    private String epr;

    @SuppressWarnings("unused")
    public ManageGenericArtifactServiceClient(
            String cookie, String backendServerURL, ConfigurationContext configContext)
            throws RegistryException {

        epr = backendServerURL + "ManageGenericArtifactService";

        try {
            stub = new ManageGenericArtifactServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate ManageGenericArtifactServiceClient. " +
                    axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public ManageGenericArtifactServiceClient(ServletConfig config, HttpSession session)
            throws RegistryException {

        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        epr = backendServerURL + "ManageGenericArtifactService";

        try {
            stub = new ManageGenericArtifactServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate ManageGenericArtifactServiceClient. " +
                    axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public String addArtifact(String key, String info, String lifecycleAttribute) throws Exception {
        return stub.addArtifact(key, info, lifecycleAttribute);
    }

    public String editArtifact(String path, String key, String info, String lifecycleAttribute)
            throws Exception {
        return stub.editArtifact(path != null ? path : "", key, info, lifecycleAttribute);
    }

    public ArtifactsBean listArtifacts(String key, String criteria) throws Exception {
        return stub.listArtifacts(key, criteria);
    }

    public ArtifactsBean listArtifactsByName(String key, String name) throws Exception {
        return stub.listArtifactsByName(key, name);
    }

    public ArtifactsBean listArtifactsByLC(String key, String LCName, String LCState, String LCInOut, String LCStateInOut) throws Exception {
        return stub.listArtifactsByLC(key, LCName, LCState, LCInOut, LCStateInOut);
    }

    public ContentArtifactsBean listContentArtifacts(String mediaType) throws Exception {
        return stub.listContentArtifacts(mediaType);
    }

    public ContentArtifactsBean listContentArtifactsbByLC(String mediaType, String LCName, String LCState, String LCInOut, String LCStateInOut) throws Exception {
        return stub.listContentArtifactsByLC(mediaType, LCName, LCState, LCInOut, LCStateInOut);
    }

    public ContentArtifactsBean listContentArtifactsByName(String mediaType, String criteria)
            throws Exception {
        return stub.listContentArtifactsByName(mediaType, criteria);
    }

    public StoragePathBean getStoragePath(String key) throws Exception {
        return stub.getStoragePath(key);
    }

    public String getArtifactContent(String path) throws Exception {
        return stub.getArtifactContent(path);
    }

    public String getArtifactUIConfiguration(String key) throws Exception {
        return stub.getArtifactUIConfiguration(key);
    }

    public boolean setArtifactUIConfiguration(String key, String content) throws Exception {
        return stub.setArtifactUIConfiguration(key, content);
    }

    public boolean canChange(String path) throws Exception {
        return stub.canChange(path);
    }

    /* get available aspects */
    public String[] getAvailableAspects() throws Exception {
        return stub.getAvailableAspects();
    }

    public boolean addRXTResource(HttpServletRequest request, String config,String path)
            throws Exception {
        boolean result = stub.addRXTResource(config, path);
        HttpSession session = request.getSession();
        if (session != null) {
            GenericUtil.buildMenuItems(request, getSessionParam(session, "logged-user"),
                    getSessionParam(session, "tenantDomain"),
                    getSessionParam(session, "ServerURL"));
        }
        return result;
    }

    private String getSessionParam(HttpSession session, String name) {
        return (String) session.getAttribute(name);
    }

    public String[] getInstalledRXTs(String cookie, ServletConfig config, HttpSession session) throws Exception {
        return ManageGenericArtifactUtil.getInstalledRxts(cookie,config,session);
    }

    public String getRxtAbsPathFromRxtName(String name) throws Exception {
     return stub.getRxtAbsPathFromRxtName(name);
    }

    public String getArtifactViewRequestParams(String key) throws Exception {
        return stub.getArtifactViewRequestParams(key);
    }

    public String[] getAllLifeCycleState(String LCName) throws RemoteException {
        return stub.getAllLifeCycleState(LCName);
    }
}
