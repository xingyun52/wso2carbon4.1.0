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

package org.wso2.carbon.identity.oauth.endpoint.revoke;

import org.apache.amber.oauth2.as.response.OAuthASResponse;
import org.apache.amber.oauth2.common.OAuth;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.amber.oauth2.common.message.OAuthResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.oauth.common.OAuth2ErrorCodes;
import org.wso2.carbon.identity.oauth.endpoint.OAuthRequestWrapper;
import org.wso2.carbon.identity.oauth.ui.OAuthClientException;
import org.wso2.carbon.identity.oauth.ui.OAuthConstants;
import org.wso2.carbon.identity.oauth.ui.util.OAuthUIUtil;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuthRevocationRequestDTO;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuthRevocationResponseDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.Enumeration;

@Path("/revoke")
public class OAuthRevocationEndpoint {

    private static Log log = LogFactory.getLog(OAuthRevocationEndpoint.class);

    @POST
    @Path("/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/json")
    public Response revokeAccessToken(@Context HttpServletRequest request,
                                     MultivaluedMap<String, String> paramMap)
            throws OAuthSystemException {

        HttpServletRequestWrapper httpRequest = new OAuthRequestWrapper(request, paramMap);

        if (log.isDebugEnabled()) {
            logAccessTokenRevocationRequest(httpRequest);
        }

        // extract the basic auth credentials if present in the request and use for authentication.
        boolean basicAuthUsed = false;
        if(request.getHeader(OAuthConstants.HTTP_REQ_HEADER_AUTHZ) != null){
            try {
                String[] clientCredentials = OAuthUIUtil.extractCredentialsFromAuthzHeader(
                        request.getHeader(OAuthConstants.HTTP_REQ_HEADER_AUTHZ));

                // If the client has included client credentials as request parameters, then it is
                // not permitted as per the specification. sending invalid_client error back.
                if(paramMap.containsKey(OAuth.OAUTH_CLIENT_ID) &&
                        paramMap.containsKey(OAuth.OAUTH_CLIENT_SECRET)){
                    return handleBasicAuthFailure();
                }

                // add the credentials available in Authorization to the parameter map
                paramMap.add(OAuth.OAUTH_CLIENT_ID, clientCredentials[0]);
                paramMap.add(OAuth.OAUTH_CLIENT_SECRET, clientCredentials[1]);

                // keep track of the authentication mode : required for sending back auth. failure errors
                basicAuthUsed = true;

                log.debug("HTTP Authorization Header is available which will take precedence " +
                        "over the client credentials available as request parameters.");

            } catch (OAuthClientException e) {
                // malformed credential string is considered as an auth failure.
                return handleBasicAuthFailure();
            }
        }

        try {
            OAuthRevocationRequestDTO revokeRequest = new OAuthRevocationRequestDTO();
            revokeRequest.setConsumerKey(paramMap.get(OAuth.OAUTH_CLIENT_ID).get(0));
            revokeRequest.setConsumerSecret(paramMap.get(OAuth.OAUTH_CLIENT_SECRET).get(0));
            revokeRequest.setTokens(new String[]{request.getParameter("token")});
            OAuthRevocationClient revokeClient = new OAuthRevocationClient();
            OAuthRevocationResponseDTO oauthRevokeResp = revokeClient.revokeTokens(revokeRequest);
            // if there BE has returned an error
            if(oauthRevokeResp.getError()){
                // if the client has used Basic Auth and if there is an auth failure, HTTP 401 Status
                // Code should be sent back to the client.
                if(basicAuthUsed && OAuth2ErrorCodes.INVALID_CLIENT.equals(
                        oauthRevokeResp.getErrorCode())){
                    return handleBasicAuthFailure();
                }else if(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT.equals(
                        oauthRevokeResp.getErrorCode())){
                    OAuthResponse response = OAuthASResponse.errorResponse(
                            HttpServletResponse.SC_UNAUTHORIZED).setError(
                            oauthRevokeResp.getErrorCode()).setErrorDescription(
                            oauthRevokeResp.getErrorMsg()).buildJSONMessage();
                    return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
                }
                // Otherwise send back HTTP 400 Status Code
                OAuthResponse response = OAuthASResponse.errorResponse(
                        HttpServletResponse.SC_BAD_REQUEST).setError(
                        oauthRevokeResp.getErrorCode()).setErrorDescription(
                        oauthRevokeResp.getErrorMsg()).buildJSONMessage();
                return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
            }

            else {
                OAuthResponse response = CarbonOAuthASResponse.revokeResponse(HttpServletResponse.SC_OK).buildJSONMessage();

                ResponseBuilder respBuilder = Response
                        .status(response.getResponseStatus())
                        .header(OAuthConstants.HTTP_RESP_HEADER_CACHE_CONTROL,
                                OAuthConstants.HTTP_RESP_HEADER_VAL_CACHE_CONTROL_NO_STORE)
                        .header(OAuthConstants.HTTP_RESP_HEADER_PRAGMA,
                                OAuthConstants.HTTP_RESP_HEADER_VAL_PRAGMA_NO_CACHE);

                return respBuilder.entity(response.getBody()).build();
            }

        } catch (OAuthClientException e) {
            OAuthResponse response = OAuthASResponse.errorResponse(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR).setError(
                    OAuth2ErrorCodes.SERVER_ERROR).setErrorDescription(e.getMessage()).buildJSONMessage();
            return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
        }

    }

    private Response handleBasicAuthFailure() throws OAuthSystemException {
        OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                .setError(OAuth2ErrorCodes.INVALID_CLIENT)
                .setErrorDescription("Client Authentication was failed.").buildJSONMessage();
        return Response.status(response.getResponseStatus())
                .header(OAuthConstants.HTTP_RESP_HEADER_AUTHENTICATE, OAuthUIUtil.getRealmInfo())
                .entity(response.getBody()).build();
    }

    private void logAccessTokenRevocationRequest(HttpServletRequest request) {
        log.debug("Received a access token revocation request : " + request.getRequestURI());
        // log the headers.
        log.debug("----------logging request headers.----------");
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String)headerNames.nextElement();
            Enumeration headers = request.getHeaders(headerName);
            while (headers.hasMoreElements()) {
                log.debug(headerName + " : " + headers.nextElement());
            }
        }
        // log the parameters.
        log.debug("----------logging request parameters.----------");
        log.debug("token - " + request.getParameter("token"));
    }

}
