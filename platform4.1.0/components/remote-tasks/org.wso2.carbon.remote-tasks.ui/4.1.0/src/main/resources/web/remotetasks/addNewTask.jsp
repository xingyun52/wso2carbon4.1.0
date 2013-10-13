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
<%@ page import="org.wso2.carbon.remotetasks.ui.RemoteTaskClient" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<link href="css/task.css" rel="stylesheet" type="text/css" media="all"/>
<script type="text/javascript" src="js/remotetaskcommon.js"></script>
   
<fmt:bundle basename="org.wso2.carbon.remotetasks.ui.i18n.Resources">

<form method="post" name="taskcreationform" id="taskcreationform" action="saveTask.jsp"
	onsubmit="return validateTaskInputs();">
	<input id="saveMode" name="saveMode" value="add" type="hidden"/>
	<div id="middle">
            <h2><fmt:message key="remotetask.header.new"/></h2>

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
	                            <input id="taskName" name="taskName" class="longInput" type="text"/>
	                        </td>
                    	</tr>
                    	<tr>
	                        <td style="width:150px"><fmt:message key="remotetask.target.url"/><span
	                                class="required">*</span></td>
	                        <td align="left">
	                            <input id="targetUrl" name="targetUrl" class="longInput" type="text"/>
	                        </td>
                    	</tr>
                    	<tr>
                    	<tr>
	                        <td style="width:150px"><fmt:message key="remotetask.allow.concurrent.ex"/></td>
	                        <td align="left">
	                            <input type="checkbox" id="allowConcurrentEx" name="allowConcurrentEx"/>
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
	                                   type="text" value=""/>
	                            <fmt:message key="remotetask.repeat.count.infinite"/>
	                        </td>
                    	</tr>
                   	 	<tr id="triggerIntervalTR">
	                        <td><fmt:message key="remoteask.trigger.interval"/></td>
	                        <td>
	                            <input id="triggerInterval" name="triggerInterval" class="longInput"
	                                   type="text"
	                                   value=""/>
	                            <fmt:message key="remoteask.interval.units"/>
	                        </td>
                    	</tr>
                    	<tr id="cronExpTR">
	                        <td><fmt:message key="remotetask.cron.expression"/></td>
	                        <td>
	                            <input id="cronExp" name="cronExp" class="longInput"
	                                   type="text" value=""/>
	                            
	                        </td>
                    	</tr>
                    	<tr id="startTimeTR">
	                        <td><fmt:message key="remotetask.trigger.start.time"/></td>
	                        <td>
	                            <input id="startTime" name="startTime" class="longInput"
	                                   type="text"
	                                   value=""/>
	                            <fmt:message key="remotetask.start.time.format"/>
	                        </td>
                    	</tr>
                    	<tr id="endTimeTR">
	                        <td><fmt:message key="remotetask.trigger.end.time"/></td>
	                        <td>
	                            <input id="endTime" name="endTime" class="longInput"
	                                   type="text"
	                                   value=""/>
	                            <fmt:message key="remotetask.start.time.format"/>
	                        </td>
                    	</tr>
                    	<tr>
                        <td class="buttonRow" colspan="3">
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
</fmt:bundle>