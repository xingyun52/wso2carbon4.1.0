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
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>

<%
	String serverURL = CarbonUIUtil.getServerURL(config
			.getServletContext(), session);
	ConfigurationContext configContext = (ConfigurationContext) config
			.getServletContext().getAttribute(
					CarbonConstants.CONFIGURATION_CONTEXT);
	String cookie = (String) session
			.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
	String forwardTo = null;
	String action = request.getParameter("action");
	String policyid = request.getParameter("policyid");
	PolicyDTO dto = new PolicyDTO();
	String BUNDLE = "org.wso2.carbon.identity.entitlement.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

	if ((request.getParameter("policy") != null)) {

		try {
			EntitlementPolicyAdminServiceClient client = new EntitlementPolicyAdminServiceClient(cookie, serverURL, configContext);
			dto.setPolicy(request.getParameter("policy"));
			dto.setPolicyId(policyid);
			client.updatePolicy(dto);
			//session.setAttribute("entitlementpolicy", dto.getPolicy());
			forwardTo = "index.jsp?region=region1&item=policy_menu";
			String message = resourceBundle.getString("updated.successfully");
			CarbonUIMessage.sendCarbonUIMessage(message,CarbonUIMessage.INFO, request);
		} catch (Exception e) {
			String message = resourceBundle.getString("invalid.policy.not.updated");
			//session.setAttribute("entitlementpolicy", dto.getPolicy());
			CarbonUIMessage.sendCarbonUIMessage(message,	CarbonUIMessage.ERROR, request);
			forwardTo = "index.jsp?region=region1&item=policy_menu";
		}
	} else {
		forwardTo = "index.jsp?region=region1&item=policy_menu";
	}
%>

<%@page
	import="org.wso2.carbon.identity.entitlement.ui.client.EntitlementPolicyAdminServiceClient"%>
<%@page import="java.util.ResourceBundle"%>
<%@ page import="org.wso2.carbon.identity.entitlement.stub.dto.PolicyDTO" %>
<script
	type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
	}
</script>

<script type="text/javascript">
	forward();
</script>