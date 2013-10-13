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
package org.wso2.carbon.identity.sso.saml.ui;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.sso.saml.stub.IdentityException;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOAuthnReqDTO;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOReqValidationResponseDTO;
import org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSORespDTO;
import org.wso2.carbon.identity.sso.saml.ui.client.SAMLSSOServiceClient;
import org.wso2.carbon.identity.sso.saml.ui.logout.LogoutRequestSender;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * This is the entry point for authentication process in an SSO scenario. This servlet is registered
 * with the URL pattern /samlsso and act as the control servlet. The message flow of an SSO scenario
 * is as follows.
 * <ol>
 * <li> SP sends a SAML Request via HTTP POST to the https://<ip>:<port>/samlsso endpoint. </li>
 * <li> IdP validates the SAML Request and checks whether this user is already authenticated.</li>
 * <li> If the user is authenticated, it will generate a SAML Response and send it back the SP via the
 * redirect_ajaxprocessor.jsp. </li>
 * <li> If the user is not authenticated, it will send him to the login page and prompts user to enter
 * his credentials. </li>
 * <li> If these credentials are valid, then the user will be redirected back the SP with a valid SAML
 * Assertion. If not, he will be prompted again for credentials. </li>
 * </ol>
 */
public class SAMLSSOProvider extends HttpServlet {

	private static final long serialVersionUID = -5182312441482721905L;
	private static Log log = LogFactory.getLog(SAMLSSOProvider.class);

	/**
     * session timeout happens in 10 hours
     */
    private static final int SSO_SESSION_EXPIRE = 36000;

    @Override
	protected void doGet(HttpServletRequest httpServletRequest,
	                     HttpServletResponse httpServletResponse) throws ServletException,
	                                                             IOException {
		doPost(httpServletRequest, httpServletResponse);
	}

    @Override
	/**
	 * The main logic is handled in the this doPost method. If the request does not contain 
	 * username password, then it means this is a request from a Service Provider with a 
	 * SAMLRequest. In case of an authentication request in that case we need to check if the 
	 * user already has a session. If there is no session found the user will be redirected to
	 * the authentication page, from the authentication page use will be again redirected back
	 * to this Servlet. Then the after successful authentication user will be redirected back 
	 * the service provider. In case of logout requests, the IDP will send logout requests
	 * to the other session participants and then send the logout response back to the initiator.  
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	                                                                       throws ServletException,
	                                                                       IOException {
		String username = req.getParameter(SAMLSSOProviderConstants.USERNAME);
		String password = req.getParameter(SAMLSSOProviderConstants.PASSWORD);
		String federatedIdp = req.getParameter(SAMLSSOProviderConstants.FEDERATED_IDP);
		if (federatedIdp == null) {
			federatedIdp = req.getHeader(SAMLSSOProviderConstants.FEDERATED_IDP);
		}
		HttpSession session = req.getSession();
		String ssoTokenID = session.getId();
		Cookie tokenCookie = getSSOTokenCookie(req);
		if (tokenCookie != null) {
			ssoTokenID = tokenCookie.getValue();
		}
		try {
			if (federatedIdp != null) {
				handleFederatedLogin(req, resp);
			} else if (username == null && password == null) {// SAMLRequest received.
				String authMode = req.getParameter("authMode");
				if (!SAMLSSOProviderConstants.AuthnModes.OPENID.equals(authMode)) {
					authMode = SAMLSSOProviderConstants.AuthnModes.USERNAME_PASSWORD;
				}
				String relayState = req.getParameter(SAMLSSOProviderConstants.RELAY_STATE);
				if (relayState == null) {
					log.debug("RelayState is not present in the request.");
					sendNotification(SAMLSSOProviderConstants.Notification.NORELAY_STATUS,
					                 SAMLSSOProviderConstants.Notification.NORELAY_MESSAGE, req,
					                 resp);
					return;
				}
				String samlRequest = req.getParameter("SAMLRequest");
				if (samlRequest != null) {
					handleSAMLRequest(req, resp, ssoTokenID, samlRequest, relayState, authMode);
				} else {
					log.debug("Invalid request message " + samlRequest);
					sendNotification(SAMLSSOProviderConstants.Notification.INVALID_MESSAGE_STATUS,
					                 SAMLSSOProviderConstants.Notification.INVALID_MESSAGE_MESSAGE,
					                 req, resp);
					return;
				}
			} else {
				handleRequestFromLoginPage(req, resp, ssoTokenID);
			}
		} catch (IdentityException e) {
			log.error("Error when processing the authentication request!", e);
			sendNotification(SAMLSSOProviderConstants.Notification.EXCEPTION_STATUS,
			                 SAMLSSOProviderConstants.Notification.EXCEPTION_MESSAGE, req, resp);
		}
    }

	/**
	 * Federated IDP scenario. This will redirect the user to the IDP of the
	 * users domain in the federation.
	 * 
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	private void handleFederatedLogin(HttpServletRequest req, HttpServletResponse resp)
	                                                                                   throws ServletException,
	                                                                                   IOException {
		log.debug("Federated Login Request Received. Redirecting..");
		RequestDispatcher reqDispatcher =
		                                  req.getRequestDispatcher("/carbon/sso-saml/federation_ajaxprocessor.jsp");
		reqDispatcher.forward(req, resp);
	}

	/**
	 * Prompts user a notification with the status and message
	 * 
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	private void sendNotification(String status, String message, HttpServletRequest req,
	                              HttpServletResponse resp) throws ServletException, IOException {
		req.setAttribute(SAMLSSOProviderConstants.STATUS, status);
		req.setAttribute(SAMLSSOProviderConstants.STATUS_MSG, message);
		RequestDispatcher reqDispatcher =
		                                  getServletContext().getRequestDispatcher("/carbon/sso-saml/notification_ajaxprocessor.jsp");
		reqDispatcher.forward(req, resp);
	}
	
	/**
	 * If the SAMlRequest is a Logout request then IDP will send logout requests
	 * to other session participants and then sends the logout Response back
	 * to the initiator. In case of authentication request, check if there is
	 * a valid session for the user, if there is, the user will be redirected
	 * directly to the Service Provider, if not the user will be redirected to
	 * the login page.
	 * 
	 * @param req
	 * @param resp
	 * @param ssoTokenID
	 * @param samlRequest
	 * @param relayState
	 * @param authMode
	 * @throws IdentityException
	 * @throws IOException
	 * @throws ServletException
	 */
	private void handleSAMLRequest(HttpServletRequest req, HttpServletResponse resp,
	                               String ssoTokenID, String samlRequest, String relayState,
	                               String authMode) throws IdentityException, IOException,
	                                               ServletException {
		String queryString = req.getQueryString();
		if(log.isDebugEnabled()) {
			log.debug("Query string : " + queryString);
		}
		String rpSessionId = req.getParameter(MultitenantConstants.SSO_AUTH_SESSION_ID);
		SAMLSSOServiceClient client = getSAMLSSOServiceClient(req);
		SAMLSSOReqValidationResponseDTO signInRespDTO =
		                                                client.validate(samlRequest, queryString, ssoTokenID, rpSessionId, authMode);
		if (!signInRespDTO.getLogOutReq()) { // an <AuthnRequest> received
			if (signInRespDTO.getValid() && signInRespDTO.getResponse() == null) {
				// user doesn't have an existing SSO session, so authenticate
				sendToAuthenticate(req, resp, signInRespDTO, relayState);
			} else if (signInRespDTO.getResponse() != null) {
				// user already has an existing SSO session, redirect
				if (SAMLSSOProviderConstants.AuthnModes.OPENID.equals(authMode)) {
					storeSSOTokenCookie(ssoTokenID, req, resp);
				}
				sendResponse(req, resp, relayState, signInRespDTO.getResponse(),
				             signInRespDTO.getAssertionConsumerURL(), signInRespDTO.getSubject());
			}
		} else { // a <LogoutRequest> received
			// sending LogoutRequests to other session participants
			LogoutRequestSender.getInstance().sendLogoutRequests(signInRespDTO.getLogoutRespDTO());
			// sending LogoutResponse back to the initiator
			sendResponse(req, resp, relayState, signInRespDTO.getLogoutResponse(),
			             signInRespDTO.getAssertionConsumerURL(), signInRespDTO.getSubject());
		}
	}

	/**
	 * Returns the service client. First if there is a client already in this
	 * session, it will be returned, otherwise a new client will be created,
	 * added to the session and returned.
	 * 
	 * @param req
	 * @return
	 * @throws AxisFault
	 */
	private SAMLSSOServiceClient getSAMLSSOServiceClient(HttpServletRequest req) throws AxisFault {
		HttpSession session = req.getSession();
		if (session.getAttribute(SAMLSSOProviderConstants.SAMLSSOServiceClient) == null) {
			String serverURL = CarbonUIUtil.getServerURL(session.getServletContext(), session);
			ConfigurationContext configContext =
			                                     (ConfigurationContext) session.getServletContext()
			                                                                   .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
			SAMLSSOServiceClient client = new SAMLSSOServiceClient(serverURL, configContext);
			session.setAttribute(SAMLSSOProviderConstants.SAMLSSOServiceClient, client);
		}
		return (SAMLSSOServiceClient) session.getAttribute(SAMLSSOProviderConstants.SAMLSSOServiceClient);
	}
	
	/**
	 * Sends the user for authentication to the login page
	 * 
	 * @param req
	 * @param resp
	 * @param signInRespDTO
	 * @param relayState
	 * @throws ServletException
	 * @throws IOException
	 */
	private void sendToAuthenticate(HttpServletRequest req, ServletResponse resp,
	                                SAMLSSOReqValidationResponseDTO signInRespDTO,
	                                String relayState)
	                                                                                 throws ServletException,
	                                                                                 IOException {
		// keeping the query string in the session
		req.getSession().setAttribute(SAMLSSOProviderConstants.HTTP_QUERY_STRING, req.getQueryString());
		req.setAttribute(SAMLSSOProviderConstants.RELAY_STATE, relayState);
		req.setAttribute(SAMLSSOProviderConstants.REQ_MSG_STR,
		                 signInRespDTO.getRequestMessageString());
		req.setAttribute(SAMLSSOProviderConstants.ISSUER, signInRespDTO.getIssuer());
		req.setAttribute(SAMLSSOProviderConstants.REQ_ID, signInRespDTO.getId());
		req.setAttribute(SAMLSSOProviderConstants.SUBJECT, signInRespDTO.getSubject());
		req.setAttribute(SAMLSSOProviderConstants.RP_SESSION_ID, signInRespDTO.getRpSessionId());
		req.setAttribute(SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL,
		                 signInRespDTO.getAssertionConsumerURL());
		String forwardingPath = getLoginPage(signInRespDTO.getLoginPageURL());
		RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(forwardingPath);
		dispatcher.forward(req, resp);
	}
	
	/**
	 * Sends the Response message back to the Service Provider.
	 * 
	 * @param req
	 * @param resp
	 * @param signInRespDTO
	 * @param relayState
	 * @throws ServletException
	 * @throws IOException
	 */
	private void sendResponse(ServletRequest req, ServletResponse resp, String relayState,
	                          String response, String acUrl, String subject)
	                                                                        throws ServletException,
	                                                                        IOException {
		req.setAttribute(SAMLSSOProviderConstants.RELAY_STATE, relayState);
		req.setAttribute(SAMLSSOProviderConstants.SAML_RESP, response);
		req.setAttribute(SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL, acUrl);
		req.setAttribute(SAMLSSOProviderConstants.SUBJECT, subject);
		RequestDispatcher reqDispatcher =
		                                  getServletContext().getRequestDispatcher("/carbon/sso-saml/redirect_ajaxprocessor.jsp");
		reqDispatcher.forward(req, resp);
	}
	
	/**
	 * This method handles authentication and sends authentication Response
	 * message back to the Service Provider after successful authentication. In
	 * case of authentication failure the user is prompted back for
	 * authentication.
	 * 
	 * @param req
	 * @param resp
	 * @param ssoTokenID
	 * @throws IdentityException
	 * @throws IOException
	 * @throws ServletException
	 */
	private void handleRequestFromLoginPage(HttpServletRequest req, HttpServletResponse resp,
	                                        String ssoTokenID) throws IdentityException,
	                                                          IOException, ServletException {

		String relayState = req.getParameter(SAMLSSOProviderConstants.RELAY_STATE);
		SAMLSSOAuthnReqDTO authnReqDTO = new SAMLSSOAuthnReqDTO();
		populateAuthnReqDTO(req, authnReqDTO);
		SAMLSSOServiceClient ssoServiceClient = getSAMLSSOServiceClient(req);
		SAMLSSORespDTO authRespDTO = ssoServiceClient.authenticate(authnReqDTO, ssoTokenID);

		if (authRespDTO.getSessionEstablished()) { // authenticated
			storeSSOTokenCookie(ssoTokenID, req, resp);
			sendResponse(req, resp, relayState, authRespDTO.getRespString(),
			             authRespDTO.getAssertionConsumerURL(), authRespDTO.getSubject());
		} else { // authentication FAILURE
			req.setAttribute(SAMLSSOProviderConstants.AUTH_FAILURE, Boolean.parseBoolean("true"));
			req.setAttribute(SAMLSSOProviderConstants.AUTH_FAILURE_MSG, authRespDTO.getErrorMsg());
			populateReAuthenticationRequest(req);
			// send back to the login.page for the next authentication attempt.
			String forwardingPath = getLoginPage(authRespDTO.getLoginPageURL());
			RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(forwardingPath);
			dispatcher.forward(req, resp);
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param authnReqDTO
	 */
	private void populateAuthnReqDTO(HttpServletRequest req, SAMLSSOAuthnReqDTO authnReqDTO) {
		authnReqDTO.setAssertionConsumerURL(getRequestParameter(req,
		                                                        SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL));
		authnReqDTO.setId(getRequestParameter(req, SAMLSSOProviderConstants.REQ_ID));
		authnReqDTO.setIssuer(getRequestParameter(req, SAMLSSOProviderConstants.ISSUER));
		authnReqDTO.setUsername(getRequestParameter(req, SAMLSSOProviderConstants.USERNAME));
		authnReqDTO.setPassword(getRequestParameter(req, SAMLSSOProviderConstants.PASSWORD));
		authnReqDTO.setSubject(getRequestParameter(req, SAMLSSOProviderConstants.SUBJECT));
		authnReqDTO.setRpSessionId(getRequestParameter(req, SAMLSSOProviderConstants.RP_SESSION_ID));
		authnReqDTO.setRequestMessageString(getRequestParameter(req,
		                                                        SAMLSSOProviderConstants.REQ_MSG_STR));
		authnReqDTO.setQueryString((String) req.getAttribute(SAMLSSOProviderConstants.HTTP_QUERY_STRING));
		//removing from the session
		req.removeAttribute(SAMLSSOProviderConstants.HTTP_QUERY_STRING);
	}

	/**
	 * 
	 * @param req
	 */
	private void populateReAuthenticationRequest(HttpServletRequest req) {
		req.setAttribute(SAMLSSOProviderConstants.ISSUER,
		                 req.getParameter(SAMLSSOProviderConstants.ISSUER));
		req.setAttribute(SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL,
		                 req.getParameter(SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL));
		req.setAttribute(SAMLSSOProviderConstants.REQ_ID,
		                 req.getParameter(SAMLSSOProviderConstants.REQ_ID));
		req.setAttribute(SAMLSSOProviderConstants.SUBJECT,
		                 req.getParameter(SAMLSSOProviderConstants.SUBJECT));
		req.setAttribute(SAMLSSOProviderConstants.RP_SESSION_ID,
		                 req.getParameter(SAMLSSOProviderConstants.RP_SESSION_ID));
		req.setAttribute(SAMLSSOProviderConstants.REQ_MSG_STR,
		                 req.getParameter(SAMLSSOProviderConstants.REQ_MSG_STR));
        req.setAttribute(SAMLSSOProviderConstants.RELAY_STATE,
        		                 req.getParameter(SAMLSSOProviderConstants.RELAY_STATE));
	}

    /**
     * 
     * @param req
     * @return
     */
    private Cookie getSSOTokenCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(SAMLSSOProviderConstants.SSO_TOKEN_ID)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    /**
     * 
     * @param ssoTokenID
     * @param req
     * @param resp
     */
    private void storeSSOTokenCookie(String ssoTokenID, HttpServletRequest req,
                                     HttpServletResponse resp) {
        Cookie ssoTokenCookie = getSSOTokenCookie(req);
        if (ssoTokenCookie == null) {
            ssoTokenCookie = new Cookie(SAMLSSOProviderConstants.SSO_TOKEN_ID, ssoTokenID);
        }
        ssoTokenCookie.setMaxAge(SSO_SESSION_EXPIRE);
        resp.addCookie(ssoTokenCookie);
    }

    /**
     * 
     * @param customLoginPage
     * @return
     */
    private String getLoginPage(String customLoginPage) {
        if (customLoginPage != null) {
            return "/carbon/" + customLoginPage.trim();
        } else {
            return "/carbon/" + "sso-saml/login_ajaxprocessor.jsp";
        }
    }

    /**
     * 
     * @param req
     * @param paramName
     * @return
     */
    private String getRequestParameter(HttpServletRequest req, String paramName) {
        // This is to handle "null" values coming as the parameter values from the JSP.
        if (req.getParameter(paramName) != null && req.getParameter(paramName).equals("null")) {
            return null;
        }
        return req.getParameter(paramName);
    }

}
