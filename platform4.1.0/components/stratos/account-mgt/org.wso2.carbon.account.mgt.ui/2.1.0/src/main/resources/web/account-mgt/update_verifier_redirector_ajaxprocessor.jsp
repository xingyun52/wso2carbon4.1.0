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
<%@ page import="org.wso2.carbon.account.mgt.ui.utils.Util" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
//this jsp is used just to redirect to the correct update verifier.

String data = (String)session.getAttribute("intermediate-data");

Util.readIntermediateData(request, data);
String domain = (String)request.getAttribute("tenantDomain");
String contextPath = "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" + domain;
response.sendRedirect("../account-mgt/update_verifier.jsp");
%>
