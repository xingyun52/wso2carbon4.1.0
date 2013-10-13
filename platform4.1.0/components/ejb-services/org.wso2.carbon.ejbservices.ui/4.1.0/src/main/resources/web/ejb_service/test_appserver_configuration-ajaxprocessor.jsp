<%--
~  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~  Licensed under the Apache License, Version 2.0 (the "License");
~  you may not use this file except in compliance with the License.
~  You may obtain a copy of the License at
~
~        http://www.apache.org/licenses/LICENSE-2.0
~
~  Unless required by applicable law or agreed to in writing, software
~  distributed under the License is distributed on an "AS IS" BASIS,
~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~  See the License for the specific language governing permissions and
~  limitations under the License.
--%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ejbservices.ui.EJBServicesAdminClient" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%
    String providerURL = request.getParameter("providerURL1");
    String jndiContextClass = request.getParameter("jndiContextClass");
    String userName = request.getParameter("userName");
    String password = request.getParameter("password");

    boolean isSuccessful = false;
    try {
        EJBServicesAdminClient servicesAdminClient = new EJBServicesAdminClient(config.getServletContext(), session);
        isSuccessful = servicesAdminClient.testAppServerConnection(providerURL, jndiContextClass, userName, password);
        if(isSuccessful) {
            response.setStatus(200);
        } else {
            response.setStatus(500);
        }
    } catch (Exception e) {
        response.setStatus(500);
    }
%>
