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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>

<%@page import="org.wso2.carbon.claim.mgt.stub.dto.ClaimDialectDTO" %>

<%@page import="java.lang.Exception" %>
<%@page import="org.wso2.carbon.claim.mgt.ui.client.ClaimAdminClient" %>

<%
    String serverURL = CarbonUIUtil.getServerURL(config
            .getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config
            .getServletContext().getAttribute(
                    CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session
            .getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String forwardTo = null;
    ClaimDialectDTO[] claimDialectDTO = null;
    boolean haveExternalUserStore = false;
    String BUNDLE = "org.wso2.carbon.claim.mgt.ui.i18n.Resources";
    ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE, request.getLocale());

    try {
        ClaimAdminClient client = new ClaimAdminClient(cookie, serverURL, configContext);
        claimDialectDTO = client.getAllClaimMappings();
    } catch (Exception e) {
        String message = resourceBundle.getString("error.while.loading.claim.mappings");
        CarbonUIMessage.sendCarbonUIMessage(message, CarbonUIMessage.ERROR, request);
        forwardTo = "../admin/error.jsp";
%>

<%@page import="org.wso2.carbon.user.core.UserCoreConstants" %>
<%@page import="java.util.ResourceBundle" %>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<script type="text/javascript">
    function forward() {
        location.href = "<%=forwardTo%>";
    }
</script>

<script type="text/javascript">
    forward();
</script>
<%
    }
%>

<fmt:bundle basename="org.wso2.carbon.claim.mgt.ui.i18n.Resources">
    <carbon:breadcrumb
            label="claim.dialects"
            resourceBundle="org.wso2.carbon.claim.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <div id="middle">
        <h2><fmt:message key='claim.management'/></h2>

        <div id="workArea">
            <script type="text/javascript">
                function remove(store, dialect, defaultDialect) {
                    if (dialect == defaultDialect) {
                        CARBON.showWarningDialog('<fmt:message key="cannot.remove.default.carbon.dialect"/>', null, null);
                        return;
                    }
                    CARBON.showConfirmationDialog('<fmt:message key="remove.message1"/>' + dialect + '<fmt:message key="remove.message2"/>',
                                                  function() {
                                                      location.href = "remove-dialect.jsp?store=" + store + "&dialect=" + dialect;
                                                  }, null);
                }
            </script>
            <div style="height:30px;">
                <a href="javascript:document.location.href='add-dialect.jsp?extuser=<%=haveExternalUserStore%>'"
                   class="icon-link"
                   style="background-image:url(../admin/images/add.gif);"><fmt:message
                        key='add.new.claim.dialect'/></a>
            </div>
            <table style="width: 100%" class="styledLeft">
                <thead>
                <tr>
                    <th colspan="2"><fmt:message
                            key='available.claim.dialctes.for.internal.user.stores'/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (claimDialectDTO != null && claimDialectDTO.length > 0) {
                        for (int i = 0; i < claimDialectDTO.length; i++) {
                %>
                <tr>
                    <td width="50%"><a
                            href="claim-view.jsp?store=<%=UserCoreConstants.INTERNAL_USERSTORE%>&dialect=<%=claimDialectDTO[i].getDialectURI()%>"><%=claimDialectDTO[i].getDialectURI()%>
                    </a></td>
                    <td width="50%"><a title="<fmt:message key='remove.claim.dialect'/>"
                                       onclick="remove('<%=UserCoreConstants.INTERNAL_USERSTORE%>','<%=claimDialectDTO[i].getDialectURI()%>','<%=UserCoreConstants.DEFAULT_CARBON_DIALECT%>');return false;"
                                       href="#" style="background-image: url(images/delete.gif);"
                                       class="icon-link"><fmt:message key='delete'/></a></td>
                </tr>
                <%
                    }
                } else {
                %>
                <tr>
                    <td width="100%" colspan="2"><i><fmt:message
                            key='no.claim.mappings.available'/></i></td>
                </tr>
                <%
                    }
                %>
                </tbody>
            </table>
            <br/>
        </div>
    </div>
</fmt:bundle>
