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
package org.wso2.carbon.um.ws.api;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.um.ws.api.stub.RemoteClaimManagerServiceStub;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.Claim;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.claim.ClaimMapping;

public class WSClaimManager implements ClaimManager {

    private RemoteClaimManagerServiceStub stub = null;

    private static Log log = LogFactory.getLog(WSClaimManager.class);

    public WSClaimManager(String serverUrl, String cookie, ConfigurationContext configCtxt)
                                                                                           throws UserStoreException {
        try {
            stub =
                   new RemoteClaimManagerServiceStub(configCtxt, serverUrl +
                                                                 "RemoteClaimManagerService");

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        } catch (AxisFault e) {
            throw new UserStoreException();
        }
    }

    public void addNewClaimMapping(ClaimMapping mapping) throws UserStoreException {
        try {
            org.wso2.carbon.um.ws.api.stub.ClaimMapping claimMapping = WSRealmUtil
                    .convertToADBClaimMapping(mapping);
            stub.addNewClaimMapping(claimMapping);
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }

    }

    public void deleteClaimMapping(ClaimMapping mapping) throws UserStoreException {
        try {
            org.wso2.carbon.um.ws.api.stub.ClaimMapping claimMapping = WSRealmUtil
                    .convertToADBClaimMapping(mapping);
            stub.deleteClaimMapping(claimMapping);
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }

    }

    public Claim[] getAllClaims() throws UserStoreException {
        try {
            return WSRealmUtil.convertToClaims(stub.getAllClaims(null));
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return new Claim[0];
    }

    public Claim[] getAllClaims(String dialectUri) throws UserStoreException {
        try {
            return WSRealmUtil.convertToClaims(stub.getAllClaims(dialectUri));
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return new Claim[0];
    }

    public String[] getAllClaimUris() throws UserStoreException {
        try {
            return stub.getAllClaimUris();
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return new String[0];
    }

    public Claim[] getAllRequiredClaims() throws UserStoreException {
        try {
            return WSRealmUtil.convertToClaims(stub.getAllRequiredClaims());
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return new Claim[0];
    }

    public Claim[] getAllSupportClaimsByDefault() throws UserStoreException {
        try {
            return WSRealmUtil.convertToClaims(stub.getAllSupportClaimsByDefault());
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return new Claim[0];
    }

    public String getAttributeName(String claimURI) throws UserStoreException {
        try {
            return stub.getAttributeName(claimURI);
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return null;
    }

    public Claim getClaim(String claimURI) throws UserStoreException {
        try {
            return WSRealmUtil.convertToClaim(stub.getClaim(claimURI));
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return null;
    }

    public ClaimMapping getClaimMapping(String claimURI) throws UserStoreException {
        try {
            return WSRealmUtil.convertToClaimMapping(stub.getClaimMapping(claimURI));
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }
        return null;
    }

    public void updateClaimMapping(ClaimMapping mapping) throws UserStoreException {
        try {
            stub.updateClaimMapping(WSRealmUtil.convertToADBClaimMapping(mapping));
        } catch (Exception e) {
            this.handleException(e.getMessage(), e);
        }

    }

    private String[] handleException(String msg, Exception e) throws UserStoreException {
        log.error(e.getMessage(), e);
        throw new UserStoreException(msg, e);
    }

    public void addNewClaimMapping(org.wso2.carbon.user.api.ClaimMapping mapping)
            throws org.wso2.carbon.user.core.UserStoreException {
        addNewClaimMapping(ClaimMapping.class.cast(mapping));

    }

    public void deleteClaimMapping(org.wso2.carbon.user.api.ClaimMapping mapping)
            throws org.wso2.carbon.user.core.UserStoreException {
        deleteClaimMapping(ClaimMapping.class.cast(mapping));

    }

    public void updateClaimMapping(org.wso2.carbon.user.api.ClaimMapping mapping)
            throws org.wso2.carbon.user.core.UserStoreException {
        updateClaimMapping(ClaimMapping.class.cast(mapping));
    }
}
