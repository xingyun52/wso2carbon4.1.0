/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.authorization.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PaginatedRoleDTO;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionDTO;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionModuleDTO;
import org.wso2.carbon.identity.authorization.core.dto.xsd.PermissionTreeNodeDTO;
import org.wso2.carbon.identity.authorization.stub.AuthorizationAdminServiceIdentityAuthorizationException;
import org.wso2.carbon.identity.authorization.stub.AuthorizationAdminServiceStub;


import java.rmi.RemoteException;

/**
 * 
 */
public class IdentityAuthorizationClient {

    private AuthorizationAdminServiceStub stub;

    private static final Log log = LogFactory.getLog(IdentityAuthorizationClient.class);

    /**
     * Instantiates IdentityAuthorizationClient
     *
     * @param cookie For session management
     * @param backendServerURL URL of the back end server 
     * @param configCtx ConfigurationContext
     * @throws org.apache.axis2.AxisFault
     */
    public IdentityAuthorizationClient(String cookie, String backendServerURL,
            ConfigurationContext configCtx) throws AxisFault {
        String serviceURL = backendServerURL + "AuthorizationAdminService";
        stub = new AuthorizationAdminServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setTimeOutInMilliSeconds(15 * 60 * 1000);
        option.setProperty(HTTPConstants.SO_TIMEOUT, 15 * 60 * 1000);
        option.setProperty(HTTPConstants.CONNECTION_TIMEOUT, 15 * 60 * 1000);
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }


    public String[] getPermissionModules() throws AxisFault {
        try {
            return stub.getPermissionModules();
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        }
        return null;
    }

    public PermissionTreeNodeDTO getPermissionTreeNodes(String moduleName, String root,
                                                        String secondary, String filter) throws AxisFault {
        try {
            return stub.getPermissionTreeNode(moduleName, root, secondary, filter);
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        }
        return null;
    }

    public String[] getRootNodeNames(String moduleName, String filter) throws AxisFault {
        try {
            return stub.getRootNodeNames(moduleName, filter);
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        }
        return null;
    }

    public PermissionModuleDTO getModuleInfo(String moduleName) throws AxisFault {
        try {
            return stub.getModuleInfo(moduleName);
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        }
        return null;
    }

    public PermissionDTO[] getUserPermissions(String userName, String moduleName) throws AxisFault {
        try {
            return stub.getExplicitUserPermissions(userName, moduleName);
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        } catch (AuthorizationAdminServiceIdentityAuthorizationException e) {
            handleException(e.getMessage(), e);
        }
        return null;
    }

     public PermissionDTO[] getRolePermissions(String roleName, String moduleName) throws AxisFault {
        try {
            return stub.getRolePermissions(roleName, moduleName);
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        } catch (AuthorizationAdminServiceIdentityAuthorizationException e) {
            handleException(e.getMessage(), e);
        }
        return null;
    }

    public String[] getSecondaryRootNodeNames(String moduleName, String root, String filter) throws AxisFault {
        try {
            return stub.getRootSecondaryNodeNames(moduleName, root, filter);
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        }
        return null;
    }

    public void configurePermission(PermissionDTO[] permissionDTOs, String moduleName) throws AxisFault {
        try {
            stub.setPermissions(permissionDTOs, moduleName);
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        } catch (AuthorizationAdminServiceIdentityAuthorizationException e) {
            handleException(e.getMessage(), e);
        }
    }

    public void clearUserAuthorization(PermissionDTO[] permissionDTOs, String moduleName) throws AxisFault {
        try {
            stub.clearUserPermissions(permissionDTOs, moduleName);
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        } catch (AuthorizationAdminServiceIdentityAuthorizationException e) {
            handleException(e.getMessage(), e);
        }
    }

    public PaginatedRoleDTO getRoleList(String filter, int pageNumber) throws AxisFault {
        try {
            return stub.getRoleList(filter, pageNumber);
        } catch (RemoteException e) {
            handleException(e.getMessage(), e);
        } catch (AuthorizationAdminServiceIdentityAuthorizationException e) {
              handleException(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Logs and wraps the given exception.
     *
     * @param msg Error message
     * @param e Exception
     * @throws AxisFault
     */
    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }    
}
