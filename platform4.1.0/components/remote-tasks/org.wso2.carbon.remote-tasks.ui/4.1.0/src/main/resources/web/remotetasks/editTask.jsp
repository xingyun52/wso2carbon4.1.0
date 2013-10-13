<!--
~ Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.commons.logging.Log" %>
<%@ page import="org.apache.commons.logging.LogFactory" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.remotetasks.stub.admin.common.xsd.DeployedTaskInformation" %>
<%@ page import="org.wso2.carbon.remotetasks.stub.admin.common.xsd.StaticTaskInformation" %>
<%@ page import="org.wso2.carbon.remotetasks.ui.RemoteTaskClient" %>
<%@ page import="org.wso2.carbon.remotetasks.ui.RemoteTaskManagementHelper" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.remotetasks.stub.admin.common.xsd.TriggerInformation" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>


<link href="css/task.css" rel="stylesheet" type="text/css" media="all"/>
<script type="text/javascript" src="js/remotetaskcommon.js"></script>
<fmt:bundle basename="org.wso2.carbon.remotetasks.ui.i18n.Resources">
    <%
        RemoteTaskClient client;
        String backendServerUrl = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configurationContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String taskName = request.getParameter("taskName");

        try {
            client = new RemoteTaskClient(cookie, backendServerUrl, configurationContext);
            client.getRemoteTask(taskName);
            String targetUrl = request.getParameter("targetUrl");
            int taskCount = Integer.parseInt(request.getParameter("taskCount"));
            int taskInterval = Integer.parseInt(request.getParameter("taskInterval"));
            String cronExpression = request.getParameter("cronExpression");
            String startTime = request.getParameter("startTime");
            String endTime = request.getParameter("endTime");

            cronExpression = (cronExpression == null) ? "" : cronExpression;
            startTime = (startTime == null) ? "" : startTime;
            endTime = (endTime == null) ? "" : endTime;
    %>
    <form method="post" name="taskcreationform" id="taskcreationform" action="saveTask.jsp"
    	onsubmit="return validateTaskInputs();">
	<div id="middle">
            <h2><fmt:message key="remotetask.header.edit"/></h2>

            <div id="workArea">

                <table class="styledLeft noBorders" cellspacing="0" cellpadding="0" border="0">
                    <thead>
                    <tr>
                        <th colspan="3"><fmt:message
                                key="remotetask.basic.information"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    	<tr>
	                        <td style="width:150px"><fmt:message key="remotetask.name"/><span
	                                class="required">*</span></td>
	                        <td align="left">
	                            <input id="taskName" name="taskName" class="longInput" type="text"
                                   value="<%=taskName%>" readonly="readonly"/>
	                        </td>
                    	</tr>
                    	<tr>
	                        <td style="width:150px"><fmt:message key="remotetask.target.url"/><span
	                                class="required">*</span></td>
	                        <td align="left">
	                            <input id="targetUrl" name="targetUrl" class="longInput" type="text"
                                        value="<%=targetUrl%>" readonly="readonly"/>
	                        </td>
                    	</tr>
                    	<tr>
                        <td colspan="2" class="middle-header"><fmt:message
                                key="remotetask.trigger.text"/></td>
                    	</tr>


                    	<tr id="triggerCountTR">
	                        <td><fmt:message key="remoteask.trigger.count"/></td>
	                        <td>
	                            <input id="triggerCount" name="triggerCount" class="longInput"
	                                   type="text" value="<%=taskCount%>"/>
                                <fmt:message key="remotetask.repeat.count.infinite"/>
	                        </td>
                    	</tr>
                   	 	<tr id="triggerIntervalTR">
	                        <td><fmt:message key="remoteask.trigger.interval"/></td>
	                        <td>
	                            <input id="triggerInterval" name="triggerInterval" class="longInput"
	                                   type="text"
	                                   value="<%=taskInterval%>"/>
                                <fmt:message key="remoteask.interval.units"/>
	                        </td>
                    	</tr>
                    	<tr id="cronExpTR">
	                        <td><fmt:message key="remotetask.cron.expression"/></td>
	                        <td>
	                            <input id="cronExp" name="cronExp" class="longInput"
	                                   type="text" value="<%=cronExpression%>"/>

	                        </td>
                    	</tr>
                    	<tr id="startTimeTR">
	                        <td><fmt:message key="remotetask.trigger.start.time"/></td>
	                        <td>
	                            <input id="startTime" name="startTime" class="longInput"
	                                   type="text"
	                                   value="<%=startTime%>"/>
                                <fmt:message key="remotetask.start.time.format"/>
	                        </td>
                    	</tr>
                    	<tr id="endTimeTR">
	                        <td><fmt:message key="remotetask.trigger.end.time"/></td>
	                        <td>
	                            <input id="endTime" name="endTime" class="longInput"
	                                   type="text"
	                                   value="<%=endTime%>"/>
                                <fmt:message key="remotetask.start.time.format"/>
	                        </td>
                    	</tr>
                    	<tr>
                        <td class="buttonRow" colspan="3">
                        	<input type="hidden" name="saveMode" id="saveMode" value="edit"/>
                            <input class="button" type="submit"
                                   value="<fmt:message key="remotetask.button.schedule.text"/>"
                                   onclick="return validateTaskInputs();"/>
                            <input class="button" type="button"
                                   value="<fmt:message key="remotetask.cancel.button.text"/>"
                                   onclick="document.location.href='remoteTasks.jsp?region=region1&item=remote_task_menu&ordinal=0';"/>
                        </td>
                    </tr>
                    </tbody>
               </table>
           </div>
    </div>

</form>
    <%

    } catch (Throwable e) {
        Log log = LogFactory.getLog(this.getClass());
        log.error(e);
    %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showErrorDialog('<%=e.getMessage()%>');
        });
    </script>
    <%
            return;
        }
    %>
</fmt:bundle>

