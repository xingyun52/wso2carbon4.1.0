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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.cep.statistics.stub.types.carbon.CountDTO" %>
<%@ page import="org.wso2.carbon.cep.statistics.ui.CEPStatisticsAdminClient" %>
<%@ page import="org.wso2.carbon.cep.statistics.ui.Utils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>

<%
    response.setHeader("Cache-Control", "no-cache");

    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CEPStatisticsAdminClient client = new CEPStatisticsAdminClient(cookie, backendServerURL,
            configContext, request.getLocale());

    int cepGraphWidth = 1200;
    cepGraphWidth = Utils.getPositiveIntegerValue(session, request, cepGraphWidth, "cepGraphWidth");

    CountDTO count;
    try {
        count =  Utils.calculateCepCount(client.getGlobalCount(), session);
    } catch (Exception e) {
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
%>
<jsp:include page="../admin/error.jsp"/>
<%
        return;
    }
%>

<fmt:bundle basename="org.wso2.carbon.cep.statistics.ui.i18n.Resources">


    <table width="100%">

        <tr>
            <td colspan="1">&nbsp;</td>
        </tr>
        <tr>
            <%--<td width="1%">&nbsp;</td>--%>

            <td width="100%">

                <table class="styledLeft" id="overallCepCountPlot" width="100%">
                    <thead>
                    <tr>
                        <th><fmt:message key="count.vs.time.units"/></th>
                    </tr>
                    </thead>
                    <tr>
                        <td>
                            <div id="cepGraph"
                                 style="width:<%= cepGraphWidth%>px;height:300px;"></div>
                        </td>
                    </tr>
                </table>


            </td>
            <%--<td width="1%">&nbsp;</td>--%>

        </tr>
        <script type="text/javascript">

            graphCepRequest.add(<%= count.getRequestCount()%>);
            graphCepResponse.add(<%= count.getResponseCount()%>);

            drawCepGraph();


        </script>
    </table>


</fmt:bundle>
