/*
 * Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.provider.openid.ui;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.identity.provider.openid.ui.handlers.OpenIDHandler;
import org.wso2.carbon.identity.provider.openid.ui.util.OpenIDUtil;

/**
 * This Servlet is the OpenID Provider endpoint  
 *
 */
public class OpenIDProviderServlet extends HttpServlet {

	private static final long serialVersionUID = 58052109007507494L;

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
	                                                                        throws ServletException,
	                                                                        IOException {
		String frontEndUrl = OpenIDUtil.getAdminConsoleURL(req);
		OpenIDHandler provider = OpenIDHandler.getInstance(null);
		provider.setFrontEndUrl(frontEndUrl + "openid-provider/openid_auth.jsp");
		String response = null;

		try {
			response = provider.processRequest(req, resp);
			resp.sendRedirect(response);
		} catch (Exception e) {
			throw new ServletException(e);
		} 
	}

}