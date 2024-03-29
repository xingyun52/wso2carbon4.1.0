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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>
<%@page import="org.wso2.carbon.utils.ServerConstants"%>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@page import="org.apache.axis2.context.ConfigurationContext"%>
<%@page import="org.wso2.carbon.CarbonConstants"%>
<%@page import="java.lang.Exception"%>
<%@page import="org.wso2.carbon.ui.util.CharacterEncoder"%>
<script type="text/javascript" src="extensions/js/vui.js"></script>
<script type="text/javascript" src="../extensions/core/js/vui.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>

<jsp:include page="../dialog/display_messages.jsp" />
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="java.util.*" %>
<%@ page import="org.wso2.carbon.identity.mgt.ui.IdentityManagementAdminClient" %>
<%@ page import="org.wso2.carbon.identity.mgt.stub.dto.UserChallengesDTO" %>

<%
    IdentityManagementAdminClient client;
    String forwardTo = null;
    List<UserChallengesDTO> userChallengesDTOs = new ArrayList<UserChallengesDTO>();
    int challengeNumber = 1;

    while(true){
        UserChallengesDTO dto = new UserChallengesDTO();
        String question = CharacterEncoder.getSafeText(request.getParameter("challengeQuestion_" + challengeNumber));
        if(question == null || question.trim().length() < 1){
            break;
        }
        String answer = CharacterEncoder.getSafeText(request.getParameter("challengeAnswer_" + challengeNumber));
        String id = CharacterEncoder.getSafeText(request.getParameter("challengeId_" + challengeNumber));
        if(answer !=  null && answer.trim().length() > 0 && id != null && id.trim().length() > 0){
            dto.setQuestion(question);
            dto.setAnswer(answer);
            dto.setPrimary(false);
            dto.setId(id);
            userChallengesDTOs.add(dto);
        }
        challengeNumber ++;
    }

    String userName = CharacterEncoder.getSafeText(request.getParameter("userName"));
    if (userName == null) {
        userName = (String) request.getSession().getAttribute("logged-user");
    }
    String BUNDLE = "org.wso2.carbon.identity.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    try {

        String cookie = (String) session
				.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config
				.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config
				.getServletContext().getAttribute(
						CarbonConstants.CONFIGURATION_CONTEXT);
        client = new IdentityManagementAdminClient(cookie,
                backendServerURL, configContext);
        client.setChallengeQuestionsOfUser(userName,
                userChallengesDTOs.toArray(new UserChallengesDTO[userChallengesDTOs.size()]));
        forwardTo = "account-recovery.jsp?username=" + userName;
	} catch (Exception e) {
		String message = resourceBundle.getString("error.while.persisting.account.recovery.data");
		CarbonUIMessage.sendCarbonUIMessage(message,CarbonUIMessage.ERROR, request);
		forwardTo = "../admin/error.jsp";
	}
%>

<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>

