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
<%@ page import="org.wso2.carbon.rssmanager.ui.RSSManagerClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.rssmanager.ui.stub.types.DatabaseUserMetaData" %>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<!-- The v=1 value is appended to the js/uiValidator.js to invalidate the existing cache -->
<script type="text/javascript" src="js/uiValidator.js?v=1" language="javascript"></script>

<fmt:bundle basename="org.wso2.carbon.rssmanager.ui.i18n.Resources">
    <carbon:breadcrumb resourceBundle="org.wso2.carbon.rssmanager.ui.i18n.Resources"
                       topPage="true" request="<%=request%>" label="Database Users"/>

    <div id="middle">
        <h2><fmt:message key="rss.manager.users"/></h2>

        <%
            RSSManagerClient client = null;
            String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
            ConfigurationContext configContext = (ConfigurationContext) config.getServletContext().
                    getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
            String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

            try {
                client = new RSSManagerClient(cookie, backendServerURL, configContext, request.getLocale());

            } catch (Exception e) {
                CarbonUIMessage.sendCarbonUIMessage(e.getMessage(),
                        CarbonUIMessage.ERROR, request, e);
            }
        %>
        <div id="workArea">
            <form method="post" action="#" name="dataForm">
                <table class="styledLeft" id="databaseUserTable">
                    <%
                        if (client != null) {
                            try {
                                DatabaseUserMetaData[] users = client.getDatabaseUsers();
                                if (users != null && users.length > 0) {
                    %>
                    <thead>
                    <tr>
                        <th width="20%"><fmt:message key="rss.manager.user"/></th>
                        <th><fmt:message key="rss.manager.instance.name"/></th>
                        <th width="60%"><fmt:message key="rss.manager.actions"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <%
                        for (DatabaseUserMetaData user : users) {
                            if (user != null) {
                    %>
                    <tr>
                        <td id="<%=user.getUsername()%>"><%=user.getUsername()%>
                        </td>
                        <td><%=user.getRssInstanceName()%></td>
                        <td>
                            <a class="icon-link"
                               style="background-image:url(../admin/images/delete.gif);"
                               onclick="dropDatabaseUser('<%=user.getRssInstanceName()%>', '<%=user.getUsername()%>')"
                               href="#"><fmt:message
                                    key="rss.manager.delete.database"/></a>
                        </td>
                    </tr>

                    <%
                            }
                        }
                    } else {
                    %>
                    <tr>
                        <td colspan="3">No database users defined yet..</td>
                    </tr>
                    <%
                                }
                            } catch (Exception e) {
                                CarbonUIMessage.sendCarbonUIMessage(e.getMessage(),
                                        CarbonUIMessage.ERROR, request, e);
                            }
                        }
                    %>
                    </tbody>
                </table>
                <div id="connectionStatusDiv" style="display: none;"></div>
                <a class="icon-link"
                   style="background-image:url(../admin/images/add.gif);"
                   href="javascript:submitAddForm()"><fmt:message key="rss.manager.add.user"/></a>
                
                <div style="clear:both;"></div>
            </form>
            <script type="text/javascript">
                function submitAddForm() {
                    document.getElementById('addForm').submit();
                }
            </script>
            <form action="createDatabaseUser.jsp" method="post" id="addForm">
                <input type="hidden" id="rssInstanceName1" name="rssInstanceName"/>
            </form>
            <script type="text/javascript">
                function submitCancelForm() {
                    document.getElementById('cancelForm').submit();
                }
            </script>
            <form action="databases.jsp" method="post" id="cancelForm">
                <input type="hidden" name="rssInstanceName" id="rssInstanceName2"/>
            </form>
            <script type="text/javascript">
                function submitExploreForm(userName, url, driver) {
                    document.getElementById('dbConsoleUseruame').value = userName;
                    document.getElementById('url').value = encodeURIComponent(url);
                    document.getElementById('driver').value = encodeURIComponent(driver);
                    document.getElementById('exploreForm').submit();
                }
            </script>
            <form action="../dbconsole/login.jsp" method="post" id="exploreForm">
                <input type="hidden" id="dbConsoleUsername" name="userName"/>
                <input type="hidden" id="url" name="url"/>
                <input type="hidden" id="driver" name="driver"/>
            </form>
        </div>
    </div>
</fmt:bundle>