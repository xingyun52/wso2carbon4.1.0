<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->

<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.remotetasks.ui.RemoteTaskClient" %>
<%@ page import="org.wso2.carbon.remotetasks.ui.RemoteTaskManagementHelper" %>
<%@ page import="org.wso2.carbon.remotetasks.stub.admin.common.xsd.StaticTaskInformation" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<link href="css/task.css" rel="stylesheet" type="text/css" media="all"/>
<script type="text/javascript" src="js/remotetaskcommon.js"></script>  
    <%
    try {
    	RemoteTaskClient client;
		
		String saveMode = request.getParameter("saveMode");
        String taskName = request.getParameter("taskName");
        
    	String backendServerUrl = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    	ConfigurationContext configurationContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    	String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    	client = new RemoteTaskClient(cookie, backendServerUrl, configurationContext);
    	    	
    	if ("add".equals(saveMode)) {
    		StaticTaskInformation staticTaskInfo = RemoteTaskManagementHelper.createStaticTaskInformation(request);
    		if (client.getRemoteTask(staticTaskInfo.getName()) != null ) {
        		throw new Exception("The task " + staticTaskInfo.getName() + " is already scheduled");
        	}
    		client.addRemoteTask(staticTaskInfo);
    	} else if ("delete".equals(saveMode)) {
            client.deleteRemoteTask(taskName);
        } else if ("edit".equals(saveMode)){
        	StaticTaskInformation staticTaskInfo = RemoteTaskManagementHelper.createStaticTaskInformation(request);
        	client.rescheduleRemoteTask(taskName, staticTaskInfo.getTriggerInformation());
        } else if ("pause".equals(saveMode)){
        	client.pauseRemoteTask(taskName);
        } else if ("resume".equals(saveMode)){
        	client.resumeRemoteTask(taskName);
        }
	 %>
    <script type="text/javascript">
        forward("remoteTasks.jsp?region=region1&item=remote_task_menu");
    </script>
    <%
    } catch (Exception e) {
    %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showErrorDialog('<%=e.getMessage()%>', function () {
                goBackOnePage();
            }, function () {
                goBackOnePage();
            });
        });
    </script>
    <%}%>