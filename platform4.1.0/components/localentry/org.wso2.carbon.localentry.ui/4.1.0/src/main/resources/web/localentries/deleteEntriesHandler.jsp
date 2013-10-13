<%--
 ~ Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~      http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
--%>

<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.localentry.ui.client.LocalEntryAdminClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.localentry.stub.types.ConfigurationObject" %>
<%@ page import="java.util.ResourceBundle" %>

<script type="text/javascript">
    function forward() {
        location.href = "index.jsp";
    }
</script>

<body>
<%
    String url = CarbonUIUtil.getServerURL(this.getServletConfig().getServletContext(),
                                                  session);
    ConfigurationContext configContext =
    (ConfigurationContext)config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    LocalEntryAdminClient client = new LocalEntryAdminClient(cookie,url,configContext);
    String entryName = request.getParameter("entryName");
    boolean forceDelete = "true".equals(request.getParameter("force"));

    if (entryName != null) {
        if (!forceDelete) {
            try {
                ConfigurationObject[] dependents = client.getDependents(entryName);
                if (dependents != null) {
                    String msg = "";
                    ResourceBundle bundle = ResourceBundle.getBundle("org.wso2.carbon.mediation.initializer.ui.i18n.Resources",
                            request.getLocale());
                    for (ConfigurationObject o : dependents) {
                        msg += "&ensp;&ensp;- " + o.getId();
                        if (bundle != null) {
                            msg += " (" + bundle.getString("dependency.mgt." + o.getType()) + ")";
                        }
                        msg += "<br/>";
                    }
                    request.getSession().setAttribute("d.mgt.error.msg", msg);
                    request.getSession().setAttribute("d.mgt.error.entry.name", entryName);
                } else {
                    doForceDelete(client, entryName, request);
                }
            } catch (Exception e) {
                String msg = "Could not delete local entry: " + e.getMessage();
                CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.ERROR, request);
            }
        } else {
            doForceDelete(client, entryName, request);
        }
    }
%>
<%-- Get the endpoint name and then call the service to delete
     the relevant endpoint with the given name and move back
     to the index page
--%>

<script type="text/javascript">
    forward();
</script>
<%-- <jsp:forward page="<%="index.jsp"%>"/>--%>

<%!
    private void doForceDelete(LocalEntryAdminClient adminClient, String entry,
                             HttpServletRequest request) {
        try {
            adminClient.deleteEntry(entry);
        } catch (Exception e) {
            String msg = "Could not delete local entry: " + e.getMessage();
            CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.ERROR, request);
        }
    }
%>