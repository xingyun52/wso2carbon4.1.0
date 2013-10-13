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

<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.apache.commons.logging.Log" %>
<%@ page import="org.apache.commons.logging.LogFactory" %>
<%@ page import="org.wso2.carbon.remotetasks.stub.admin.common.xsd.DeployedTaskInformation" %>
<%@page import="org.wso2.carbon.remotetasks.stub.admin.common.xsd.StaticTaskInformation" %>
<%@ page import="org.wso2.carbon.remotetasks.stub.admin.common.xsd.TriggerInformation" %>
<%@page import="org.wso2.carbon.remotetasks.ui.RemoteTaskClient" %>
<%@ page import="org.wso2.carbon.remotetasks.ui.RemoteTaskManagementHelper" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<link href="css/task.css" rel="stylesheet" type="text/css" media="all"/>
<script type="text/javascript" src="js/remotetaskcommon.js"></script>

<fmt:bundle basename="org.wso2.carbon.remotetasks.ui.i18n.Resources">
    <carbon:breadcrumb resourceBundle="org.wso2.carbon.remotetasks.ui.i18n.Resources" topPage="true" request="<%=request%>" label="remotetasks.header"/>
    
    <div id="middle">
        <h2><fmt:message key="remotetasks.header"/></h2>

        <div id="workArea">
        <%
        RemoteTaskClient client;
    	String backendServerUrl = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    	ConfigurationContext configurationContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    	String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    	client = new RemoteTaskClient(cookie, backendServerUrl, configurationContext);
    	String [] taskNames = client.getAllRemoteTasks();
    	 if (taskNames != null && taskNames.length != 0) {
        %>
        <p><fmt:message key="available.defined.remotetasks"/></p>
            <br/>
            <table id="myTable" class="styledLeft">
                <thead>
                <tr>
                    <th><fmt:message key="remotetask.name"/></th>
                    <th><fmt:message key="remotetask.status"/></th>
                    <th><fmt:message key="remotetask.action"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    try {
                        for (String taskName : taskNames) {
                            if (taskName != null) {
                                DeployedTaskInformation deployedTaskInfo = client.getRemoteTask(taskName);
                                String status = deployedTaskInfo.getStatus();

                                StaticTaskInformation staticTaskInfo = deployedTaskInfo.getStaticTaskInfo();
                                TriggerInformation triggerInfo = staticTaskInfo.getTriggerInformation();

                                String targetURI = staticTaskInfo.getTargetURI();
                                int taskCount = triggerInfo.getTaskCount();
                                int taskInterval = triggerInfo.getTaskInterval();
                                String cronExpression = RemoteTaskManagementHelper.formatGeneralString(triggerInfo.getCronExpression());
                                String startTime = RemoteTaskManagementHelper.formatStartTime(triggerInfo.getStartTime());
                                String endTime = RemoteTaskManagementHelper.formatStartTime(triggerInfo.getEndTime());

                %>
                <tr id="tr_<%=taskName%>">

                    <td>
                        <%=taskName%>
                    </td>
                    <td>
                    	<%=status%>
                    </td>
                    <td>
                        <a href="javascript:editRow('<%=taskName%>','<%=targetURI%>', '<%=taskCount%>',
                        '<%=taskInterval%>', '<%=cronExpression%>', '<%=startTime%>', '<%=endTime%>')"
                           id="config_link" class="edit-icon-link"><fmt:message key="remotetask.edit"/></a>
                        <a href="javascript:deleteRow('<%=taskName%>', '<fmt:message key="remotetask.delete.waring"/>')"
                           id="delete_link" class="delete-icon-link"><fmt:message
                                key="remotetask.property.delete"/></a>
                        <%if(status != null && status.equals("PAUSED")) {%>
                        	<a href="javascript:resumeRow('<%=taskName%>', '<fmt:message key="remotetask.resume.waring"/>')"
                           id="pause_link" class="resume-icon-link"><fmt:message
                                key="remotetask.property.resume"/></a>
                        <%} else {%>
                        	<a href="javascript:pauseRow('<%=taskName%>', '<fmt:message key="remotetask.pause.waring"/>')"
                           id="pause_link" class="pause-icon-link"><fmt:message
                                key="remotetask.property.pause"/></a>
                        <%} %>
                    </td>

                </tr>
                <%
                        }
                    }
                %>
                </tbody>
            </table>
            <%
            } catch (Exception e) {
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
            <%} else {%>
            <p><fmt:message key="remotetask.list.empty.text"/></p>
            <br/>
            <%}%>   
        <div style="height:30px;">
                <a href="javascript:document.location.href='addNewTask.jsp?ordinal=1'"
                   class="add-icon-link"><fmt:message key="remotetasks.add.button.text"/></a>
        </div>
        </div>
    </div>
</fmt:bundle>