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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.module.mgt.ui.client.ModuleManagementClient" %>
<%@ page import="org.wso2.carbon.module.mgt.stub.types.ModuleMetaData" %>
<%@ page
        import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<!-- This page is included to display messages which are set to request scope or session scope -->
<jsp:include page="../dialog/display_messages.jsp"/>
<script type="text/javascript" src="js/modulemgt.js"></script>

<%
        String allModules = "", globalModules = "";
        String moduleName, moduleId, status, button_name, jsFunction, label;

        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        ModuleManagementClient client;
        ModuleMetaData[] data;

        try {

            client = new ModuleManagementClient(configContext, serverURL,
                    cookie, false);

            data = client.listModules();

        } catch (Exception e) {
            CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
%>
            <script type="text/javascript">
                   location.href = "../admin/error.jsp";
            </script>
<%
            return;
    }
%>

<fmt:bundle basename="org.wso2.carbon.module.mgt.ui.i18n.Resources">
    <carbon:breadcrumb
            label="main.header"
            resourceBundle="org.wso2.carbon.module.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>
    <carbon:jsi18n
		resourceBundle="org.wso2.carbon.module.mgt.ui.i18n.JSResources"
		request="<%=request%>" />

    <script type="text/javascript">
        function submitHiddenForm(action) {
            document.getElementById("hiddenField").value = location.href;
            document.dataForm.action = action;
            document.dataForm.submit();
        }
    </script>

    <div id="middle">
        <h2><fmt:message key="main.header"/></h2>

        <div id="workArea">
            <table class="styledLeft" id="moduleTable">
                <thead>
                <tr>
                    <th width="15%"><fmt:message key="name"/></th>
                    <th width="5%"><fmt:message key="version"/></th>
                    <th width="50%"><fmt:message key="description"/></th>
                    <th width="25%" colspan="2"><fmt:message key="actions"/></th>
                </tr>
                </thead>
                <%
                    for (ModuleMetaData aData : data) {
                        moduleName = aData.getModulename();
                %>
                <tr>
                    <td>
                        <a href="./module_info.jsp?moduleName=<%=aData.getModulename()%>&moduleVersion=<%=aData.getModuleVersion()%>">
                            <%=aData.getModulename()%>
                        </a>
                    </td>
                    <td>
                        <%=aData.getModuleVersion()%>
                    </td>

                    <td>
                        <%=aData.getDescription()%>
                    </td>
                    <%
                        if (moduleName.equals("wso2throttle")) {
                    %>
                    <td>
                        <a href="" class="icon-link" onclick="submitHiddenForm('../throttling/index.jsp');return false;"
                           style="background-image:url(images/configure.gif);"><fmt:message key="configure"/></a>
                    </td>
                    <td/>
                    <%
                    } else if (moduleName.equals("wso2caching")) {
                    %>
                    <td>
                        <a href="" class="icon-link" onclick="submitHiddenForm('../caching/index.jsp');return false;"
                           style="background-image:url(images/configure.gif);"><fmt:message key="configure"/></a>


                    </td>
                    <td/>
                    <%
                    } else if (moduleName.equals("sandesha2")) {
                    %>
                    <td width="60">
                        <a href="" class="icon-link" onclick="submitHiddenForm('../rm/global.jsp');return false;"
                           style="background-image:url(images/configure.gif);"><fmt:message key="configure"/></a>

                    </td>
                    <td>
                     <%
                        if (aData.getEngagedGlobalLevel() == true) {
                     %>
                        <a href="#"
                           onclick="disengage('<%=serverURL%>','<%=aData.getModuleId()%>');"
                           class="icon-link" style="background-image:url(images/disengage.gif);"><fmt:message key="disengage"/></a>


                        <%
                        } else {
                        %>
                        <a href="#" onclick="engage('<%=serverURL%>','<%=aData.getModuleId()%>');"
                           class="icon-link"
                           style="background-image:url(images/engage.gif);"><fmt:message key="engage"/></a>


                        <%
                            }
                        %>
                    </td>
                    <%
                    } else if (moduleName.equals("addressing")) {
                    %>
                        <td width="12.5%">&nbsp;</td>
                        <td width="12.5%">&nbsp;</td>

                    <%
                    } else {
                    %>

                    <td>
                        <%
                            if (aData.getEngagedGlobalLevel() == true) {
                        %>
                        <a href="#"
                           onclick="disengage('<%=serverURL%>','<%=aData.getModuleId()%>');"
                           class="icon-link" style="background-image:url(images/disengage.gif);"><fmt:message key="disengage"/></a>


                        <%
                        } else {
                        %>
                        <a href="#" onclick="engage('<%=serverURL%>','<%=aData.getModuleId()%>');"
                           class="icon-link"
                           style="background-image:url(images/engage.gif);"><fmt:message key="engage"/></a>


                        <%
                            }
                        %>
                    </td>
                    <%
                        if (!aData.getManagedModule() && (session.getAttribute(MultitenantConstants.IS_SUPER_TENANT).toString() == "true")) {
                    %>
                    <td>
                        <a href="#" onclick="removeModule('<%=aData.getModuleId()%>');"
                           class="icon-link"
                           style="background-image:url(../admin/images/delete.gif);"><fmt:message key="delete"/></a>


                    </td>
                   <%
                        } else {
                   %>
                   <td/>     
                   <%
                           }
                       }
                    %>
                </tr>
                <%
                    }
                %>
            </table>
            <br/>

            <table class="styledLeft" id="globalModules" width="100%">
                <thead>
                <tr>
                    <th><fmt:message key="globally.engaged"/></th>
                </tr>
                </thead>
                <% for (ModuleMetaData aData : data) {
                        if (aData.getEngagedGlobalLevel()) {
                %>
                <tr>
                    <td><%=aData.getModuleId()%>
                    </td>
                </tr>
                <%
                        }
                    }
                %>
            </table>

        </div>
    </div>

    <form name="dataForm" method="post" action="">
        <input name="backURL" type="hidden" id="hiddenField" value="">
    </form>

    <script type="text/javascript">
        alternateTableRows('moduleTable', 'tableEvenRow', 'tableOddRow');
        alternateTableRows('globalModules', 'tableEvenRow', 'tableOddRow');
    </script>

    <script type="text/javascript">
        var msgId;
        <%
        if(request.getParameter("msgId") == null){
        %>
        msgId = '<%="MSG" + System.currentTimeMillis() + Math.random()%>';
        <%
        } else {
        %>
        msgId = '<%=request.getParameter("msgId")%>';
        <%
        }
        %>
    </script>
    <%
        if (request.getParameter("restart") != null && request.getParameter("restart").equals("true")) {
    %>
                <script type="text/javascript">
                    restartServer();
                </script>
    <%
        }
    %>
</fmt:bundle>
