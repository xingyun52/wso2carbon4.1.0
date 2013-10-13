<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->

<%@page import="org.wso2.carbon.identity.provider.openid.ui.util.OpenIDUtil"%>
<%@page import="org.wso2.carbon.identity.provider.openid.ui.OpenIDConstants"%>
<%@page import="org.wso2.carbon.identity.provider.openid.ui.client.OpenIDAdminClient"%>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@page import="org.apache.axis2.context.ConfigurationContext"%>
<%@page import="org.wso2.carbon.CarbonConstants"%>
<%@page import="org.wso2.carbon.utils.ServerConstants"%>
<%@page import="java.util.ResourceBundle"%>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@page import="org.openid4java.message.ParameterList"%>
<%@page import="org.wso2.carbon.identity.base.IdentityConstants.OpenId"%>

<script type="text/javascript">

  function Set_Cookie( name, value, expires, path, domain, secure )
  {
  // set time, it's in milliseconds
  var today = new Date();
  today.setTime( today.getTime() );

  /*
  if the expires variable is set, make the correct
  expires time, the current script below will set
  it for x number of days, to make it for hours,
  delete * 24, for minutes, delete * 60 * 24
  */
  if ( expires )
  {
  expires = expires * 1000 * 60 * 60 * 24;
  }
  var expires_date = new Date( today.getTime() + (expires) );

  document.cookie = name + "=" +value+
  ( ( expires ) ? ";expires=" + expires_date.toGMTString() : "" ) +
  ( ( path ) ? ";path=" + path : "" ) +
  ( ( domain ) ? ";domain=" + domain : "" ) +
  ( ( secure ) ? ";secure" : "" );
  }

  function Get_Cookie( name ) {
	  var start = document.cookie.indexOf( name + "=" );
	  var len = start + name.length + 1;
	  if ( ( !start ) &&
	  ( name != document.cookie.substring( 0, name.length ) ) )
	  {
	  return null;
	  }
	  if ( start == -1 ) return null;
	  var end = document.cookie.indexOf( ";", len );
	  if ( end == -1 ) end = document.cookie.length;
	  return unescape( document.cookie.substring( len, end ) );
  }
	    

  //this deletes the cookie when called
  function Delete_Cookie( name, path, domain ) {
  if ( Get_Cookie( name ) ) document.cookie = name + "=" +
  ( ( path ) ? ";path=" + path : "") +
  ( ( domain ) ? ";domain=" + domain : "" ) +
  ";expires=Thu, 01-Jan-1970 00:00:01 GMT";
  }
</script>

<%
	String rememberMe = request.getParameter(OpenIDConstants.RequestParameter.REMEMBER);
	boolean isRemembered = false;
	String openid =
	                (request.getParameter(OpenIDConstants.RequestParameter.OPENID) != null)
	                                                                                       ? request.getParameter(OpenIDConstants.RequestParameter.OPENID)
	                                                                                       : (String) session.getAttribute(OpenIDConstants.SessionAttribute.OPENID);

	// Directed Identity handling		
	String userName = request.getParameter(OpenIDConstants.RequestParameter.OPENID);
	if ((userName == null || "".equals(userName.trim())) && openid.endsWith("/openid/")) {
		userName = (String) session.getAttribute(OpenIDConstants.SessionAttribute.USERNAME);
		Cookie[] cookies = request.getCookies();
		if (userName == null && cookies != null) {
			Cookie curCookie = null;
			for (Cookie cooki : cookies) {
				curCookie = cooki;
				if (curCookie.getName()
				             .equalsIgnoreCase(OpenIDConstants.Cookie.OPENID_REMEMBER_ME)) {
					userName = curCookie.getValue();
					break;
				}
			}
		}
	} else {
		session.setAttribute(OpenIDConstants.SessionAttribute.USERNAME, userName);
	}
	if (userName != null && !"".equals(userName.trim())) {
		openid = openid + userName;
	}

	session.setAttribute(OpenIDConstants.SessionAttribute.OPENID, openid);
	if ("true".equals(rememberMe)) {
		isRemembered = true;
	}
	OpenIDAdminClient client = OpenIDUtil.getOpenIDAdminClient(session);
	boolean isAuthenticated =
	                          client.authenticateWithOpenID(openid,
	                                                        request.getParameter(OpenIDConstants.RequestParameter.PASSWORD),
	                                                        session, request, response,
	                                                        isRemembered);

	if (isAuthenticated ||  openid.equals(OpenIDConstants.SessionAttribute.AUTHENTICATED_OPENID)) {
		session.setAttribute(OpenIDConstants.SessionAttribute.IS_OPENID_AUTHENTICATED, "true");
		session.setAttribute(OpenIDConstants.SessionAttribute.AUTHENTICATED_OPENID, openid);
		
		// user approval is always bypased based on the identity.xml config
		if (client.isOpenIDUserApprovalBypassEnabled()) {
			session.setAttribute(OpenIDConstants.SessionAttribute.SELECTED_PROFILE, "default");
			session.setAttribute(OpenIDConstants.SessionAttribute.ACTION, "complete");
			session.setAttribute(OpenIDConstants.SessionAttribute.USER_APPROVED, "true");
			session.removeAttribute(OpenIDConstants.SessionAttribute.IS_OPENID_AUTHENTICATED);
%>
		    <script type="text/javascript">
		        Set_Cookie("openidtoken","<%=client.getNewCookieValue()%>",14,"/",null,true);
		        Set_Cookie("openidrememberme","<%=userName%>",14,"/",null,true);
		        location.href="../../openidserver";
		    </script>
<%
	    } else {  // reading RP info from the database
		    String[] rpInfo =
		                  client.getOpenIDUserRPInfo(openid,
		                                             ((ParameterList) session.getAttribute(OpenId.PARAM_LIST)).getParameterValue(OpenId.ATTR_RETURN_TO));
		    if (rpInfo[0].equals("true")) { // approve always
			    session.setAttribute(OpenIDConstants.SessionAttribute.ACTION, "complete");
			    session.setAttribute(OpenIDConstants.SessionAttribute.USER_APPROVED, "true");
			    session.setAttribute(OpenIDConstants.SessionAttribute.USER_APPROVED_ALWAYS, "true");
			    session.setAttribute(OpenIDConstants.SessionAttribute.SELECTED_PROFILE, rpInfo[1]);
			    session.removeAttribute(OpenIDConstants.SessionAttribute.IS_OPENID_AUTHENTICATED);
%>
		        <script type="text/javascript">
		            Set_Cookie("openidtoken","<%=client.getNewCookieValue()%>",14,"/",null,true);
		            Set_Cookie("openidrememberme","<%=userName%>",14,"/",null,true);
		            location.href="../../openidserver";
		        </script>
<%
            } else { // redirect to user approval page
%>
		       <script type="text/javascript">
		          Set_Cookie("openidtoken","<%=client.getNewCookieValue()%>", 14, "/", null,true);
		          Set_Cookie("openidrememberme","<%=userName%>",14,"/",null,true);
		          location.href = "openid_profile_view.jsp";
		       </script>
<%
            }
	    }

	} else {
		session.removeAttribute(OpenIDConstants.SessionAttribute.IS_OPENID_AUTHENTICATED);
		String BUNDLE = "org.wso2.carbon.identity.provider.openid.ui.i18n.Resources";
		ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());
		String message = resourceBundle.getString("error.while.user.auth");
		session.removeAttribute("openId");
		CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
%>
<script type="text/javascript">
	Delete_Cookie("openidtoken", "/", null);
	Delete_Cookie("openidrememberme", "/", null);
	location.href = "openid_auth.jsp";
</script>
<%
	}
%>
