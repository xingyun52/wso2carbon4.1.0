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
<%@page import="org.wso2.carbon.rest.api.stub.types.carbon.APIData"%>
<%@page import="org.wso2.carbon.rest.api.ui.util.RestAPIConstants"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.rest.api.ui.client.RestApiAdminClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonSecuredHttpContext" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.List" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<!-- This page is included to display messages which are set to request scope or session scope -->

<%
    response.setHeader("Cache-Control", "no-cache");

    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    RestApiAdminClient client;
    
    String serviceContextPath = configContext.getServiceContextPath();
    
    String serverContext = "";
    APIData[] apis = null;

    int pageNumber;
    int numberOfPages;
    
    String pageNumberStr = request.getParameter("pageNumber");
    if (pageNumberStr == null) {
        pageNumber = 0;
    }
    else{
    	try {
    		pageNumber = Integer.parseInt(pageNumberStr);
        } catch (NumberFormatException e) {
        	response.setStatus(500);
            CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, 
            		"pageNumber parameter is not an integer", e);
            session.setAttribute(CarbonUIMessage.ID, uiMsg);
		    %>
		    <jsp:include page="../admin/error.jsp"/>
		    <%
            return;
        }
    }
    
    String serviceTypeFilter = request.getParameter("serviceTypeFilter");
    if (serviceTypeFilter == null) {
        serviceTypeFilter = "ALL";
    }
    String serviceSearchString = request.getParameter("serviceSearchString");
    if (serviceSearchString == null) {
        serviceSearchString = "";
    }
    boolean isAuthorizedToManage =
            CarbonUIUtil.isUserAuthorized(request, "/permission/admin/manage/mediation");
    try {
        client = new RestApiAdminClient(configContext, backendServerURL, cookie, request.getLocale());
        
        serverContext = client.getServerContext();
        int apiCount = client.getAPICount();
        if(apiCount % RestAPIConstants.APIS_PER_PAGE == 0){
        	numberOfPages = apiCount / RestAPIConstants.APIS_PER_PAGE;
        }
        else{
        	numberOfPages = (apiCount / RestAPIConstants.APIS_PER_PAGE) + 1;
        }
        
        apis = client.getAPIsForListing(pageNumber, RestAPIConstants.APIS_PER_PAGE);
    } catch (Exception e) {
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
%>
<jsp:include page="../admin/error.jsp"/>
<%
        return;
    }

    session.removeAttribute("index");

    //int correctServiceGroups = servicesInfo.getNumberOfCorrectServiceGroups();
    //int faultyServiceGroups = servicesInfo.getNumberOfFaultyServiceGroups();
    
    boolean loggedIn = session.getAttribute(CarbonSecuredHttpContext.LOGGED_USER) != null;
    
    //boolean hasDownloadableServices = false;
%>

<carbon:breadcrumb
        label="deployed.apis"
        resourceBundle="org.wso2.carbon.rest.api.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<%
    if (apis == null) {
%>
        <fmt:bundle basename="org.wso2.carbon.rest.api.ui.i18n.Resources">
            <div id="middle">
                <h2><fmt:message key="deployed.apis"/></h2>
                <div id="workArea">
                	<a style="background-image: url(../admin/images/add.gif);" href="manageAPI.jsp?mode=add" class="icon-link">
                                <fmt:message key="add.api"/>
                    </a><br/><br/>
                    <fmt:message key="no.deployed.apis.found"/>
                </div>
            </div>
        </fmt:bundle>
<%
        return;
    }
%>

<fmt:bundle basename="org.wso2.carbon.rest.api.ui.i18n.Resources">
<%
    if (session.getAttribute(CarbonSecuredHttpContext.LOGGED_USER) != null) {
%>
<script type="text/javascript">
    var allServicesSelected = false;

    function selectAllInThisPage(isSelected) {
        allServicesSelected = false;
        if (document.servicesForm.serviceGroups != null &&
            document.servicesForm.serviceGroups[0] != null) { // there is more than 1 service
            if (isSelected) {
                for (var j = 0; j < document.servicesForm.serviceGroups.length; j++) {
                    document.servicesForm.serviceGroups[j].checked = true;
                }
            } else {
                for (j = 0; j < document.servicesForm.serviceGroups.length; j++) {
                    document.servicesForm.serviceGroups[j].checked = false;
                }
            }
        } else if (document.servicesForm.serviceGroups != null) { // only 1 service
            document.servicesForm.serviceGroups.checked = isSelected;
        }
        return false;
    }

    function selectAllInAllPages() {
        selectAllInThisPage(true);
        allServicesSelected = true;
        return false;
    }

    function resetVars() {
        allServicesSelected = false;

        var isSelected = false;
        if (document.servicesForm.serviceGroups[0] != null) { // there is more than 1 service
            for (var j = 0; j < document.servicesForm.serviceGroups.length; j++) {
                if (document.servicesForm.serviceGroups[j].checked) {
                    isSelected = true;
                }
            }
        } else if (document.servicesForm.serviceGroups != null) { // only 1 service
            if (document.servicesForm.serviceGroups.checked) {
                isSelected = true;
            }
        }
        return false;
    }
    
    function deleteApi(apiName) {
         CARBON.showConfirmationDialog("<fmt:message key="api.delete.confirmation"/> " + apiName + "?", function() {
             location.href = "delete_api.jsp?apiName=" + apiName;
         });
    }
</script>
<%
    }
%>

<div id="middle">
<h2><fmt:message key="deployed.apis"/></h2>

<div id="workArea">
<a style="background-image: url(../admin/images/add.gif);" href="manageAPI.jsp?mode=add" class="icon-link">
                                <fmt:message key="add.api"/>
</a>
<p>&nbsp;</p>
<%
    if (apis != null) {
        /*String parameters = "serviceTypeFilter=" + serviceTypeFilter +
                "&serviceSearchString=" + serviceSearchString;*/
%> 

<%
	if (loggedIn && isAuthorizedToManage) {
%>
<% } %>
<p>&nbsp;</p>

<form action="delete_service_groups.jsp" name="servicesForm" method="post">
    <input type="hidden" name="pageNumber" value="<%= pageNumber%>"/>
    <carbon:paginator pageNumber="<%=pageNumber%>"
                      numberOfPages="<%=numberOfPages%>"
                      page="index.jsp"
                      pageNumberParameterName="pageNumber"
                      resourceBundle="org.wso2.carbon.rest.api.ui.i18n.Resources"
                      prevKey="prev" nextKey="next"
                      parameters="<%=""%>"/>
    <table class="styledLeft" id="sgTable" width="100%">
        <thead>
        <tr>
        	<th><fmt:message key="api.name"/></th>
        	<th><fmt:message key="api.invocation.url"/></th>
        	<th colspan="2"><fmt:message key="apis.table.action.header"/></th>
        </tr>
        </thead>
        <tbody>

        <%
            int position = 0;
            for (APIData apiData : apis) {
                String bgColor = ((position % 2) == 1) ? "#EEEFFB" : "white";
                position++;
                if (apiData == null) {
                    continue;
                }
        %>

        <tr bgcolor="<%=bgColor%>">
                    <% if (loggedIn) {%>
                    <% } %>
            <td width="100px">
                <nobr>
                    <%=apiData.getName()%>
                </nobr>
            </td>
            <td width="100px">
                <nobr>
                    <%=serverContext + apiData.getContext()%>
                </nobr>
            </td>
            <td width="20px" style="text-align:left;border-left:none;border-right:none;width:100px;">
                <div class="inlineDiv">
                    <a style="background-image:url(../admin/images/edit.gif);" class="icon-link" href="manageAPI.jsp?mode=edit&amp;apiName=<%=apiData.getName()%>">Edit</a>
                </div>
            </td>
            <td width="20px" style="text-align:left;border-left:none;width:100px;">
                <div class="inlineDiv">
                    <a style="background-image:url(../admin/images/delete.gif);" class="icon-link" href="#"
                    	onclick="deleteApi('<%=apiData.getName()%>')">Delete</a>
                </div>
            </td>
        </tr>
        <%
            } // for each api
        %>
        </tbody>
    </table>
    <carbon:paginator pageNumber="<%=pageNumber%>"
                      numberOfPages="<%=numberOfPages%>"
                      page="index.jsp"
                      pageNumberParameterName="pageNumber"
                      resourceBundle="org.wso2.carbon.rest.api.ui.i18n.Resources"
                      prevKey="prev" nextKey="next"
                      parameters="<%=""%>"/>
</form>
<p>&nbsp;</p>
<%
    if (loggedIn && isAuthorizedToManage) {
%>
<% } %>
<%
} else {
%>
<b><fmt:message key="no.deployed.services.found"/></b>
<%
    }
%>
</div>
</div>
</fmt:bundle>
