/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.oauth.callback;

import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

@SuppressWarnings("unused")
public class DefaultCallbackHandler extends AbstractOAuthCallbackHandler {

    public boolean canHandle(Callback[] callbacks) throws IdentityOAuth2Exception {
        return true;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if(callbacks != null && callbacks.length > 0){
            OAuthCallback oauthCallback = (OAuthCallback) callbacks[0];
            // TODO : This needs to be implemented in XACML.
            // TODO : For the moment, let's approve everything.
            if(OAuthCallback.OAuthCallbackType.ACCESS_DELEGATION_AUTHZ.equals(
                    oauthCallback.getCallbackType())){
                oauthCallback.setAuthorized(true);
            }
            if(OAuthCallback.OAuthCallbackType.ACCESS_DELEGATION_TOKEN.equals(
                    oauthCallback.getCallbackType())){
                oauthCallback.setAuthorized(true);
            }
            if(OAuthCallback.OAuthCallbackType.SCOPE_VALIDATION_AUTHZ.equals(
                    oauthCallback.getCallbackType())){
            	oauthCallback.setApprovedScope(oauthCallback.getRequestedScope());
                oauthCallback.setValidScope(true);
            }
            if(OAuthCallback.OAuthCallbackType.SCOPE_VALIDATION_TOKEN.equals(
                    oauthCallback.getCallbackType())){
            	oauthCallback.setApprovedScope(oauthCallback.getRequestedScope());
                oauthCallback.setValidScope(true);
            }
        }
    }
}
