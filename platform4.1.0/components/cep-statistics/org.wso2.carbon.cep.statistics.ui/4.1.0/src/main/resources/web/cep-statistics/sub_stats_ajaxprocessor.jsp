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
<%@ page import="org.wso2.carbon.cep.statistics.stub.types.carbon.CollectionDTO" %>
<%@ page import="org.wso2.carbon.cep.statistics.ui.CEPStatisticsAdminClient" %>
<%@ page import="org.wso2.carbon.cep.statistics.ui.Utils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.cep.statistics.stub.types.carbon.CountDTO" %>

<%
    response.setHeader("Cache-Control", "no-cache");

    String bucketName = request.getParameter("bucketName");
    String brokerName = request.getParameter("brokerName");
    String name = brokerName;
    boolean bucket = false;
    if (bucketName != null) {
        bucket = true;
        name = bucketName;
    }

    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CEPStatisticsAdminClient client = new CEPStatisticsAdminClient(cookie, backendServerURL,
                                                                   configContext, request.getLocale());

    int mainGraphWidth = 1200;
    mainGraphWidth = Utils.getPositiveIntegerValue(session, request, mainGraphWidth, "mainGraphWidth");

    int topicGraphWidth = 550;
    topicGraphWidth = Utils.getPositiveIntegerValue(session, request, topicGraphWidth, "topicGraphWidth");


    CollectionDTO collectionDTO;

    try {
        if (bucket) {
            collectionDTO = Utils.calculateSubCount(bucketName, client.getBucketStatistics(name), session, bucket);
        } else {
            collectionDTO = Utils.calculateSubCount(brokerName, client.getBrokerStatistics(name), session, bucket);

        }
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
            <td colspan="3">&nbsp;</td>
        </tr>
        <tr>

            <td width="100%" colspan="3">

                <table class="styledLeft" id="mainCountPlot" width="100%">
                    <thead>
                    <tr>
                        <th>&quot;<%=name%>&quot;&nbsp;<fmt:message key="count.vs.time.units"/></th>
                    </tr>
                    </thead>
                    <tr>
                        <td>
                            <div id="mainGraph"
                                 style="width:<%= mainGraphWidth%>px;height:300px;"></div>
                        </td>
                    </tr>
                </table>


            </td>
                <%--<td width="1%">&nbsp;</td>--%>

        </tr>
        <tr>
            <td colspan="3">&nbsp;</td>
        </tr>

        <script type="text/javascript">
            graphMainRequest.add(<%= collectionDTO.getCount().getRequestCount()%>);
            graphMainResponse.add(<%= collectionDTO.getCount().getResponseCount()%>);
            drawMainGraph();
        </script>

        <%
            String[] topicNames = collectionDTO.getTopicNames();
            if (topicNames != null) {
                CountDTO[] topicCounts = collectionDTO.getTopicCounts();
                for (int i = 0, topicNamesLength = topicNames.length; i < topicNamesLength; i += 2) {
                    String topicSimpleName= topicNames[i].replaceAll("\\.","_").replaceAll("/","_sl_").replaceAll(" ","_").replaceAll(",","_c_").replaceAll("@","_at_") ;
        %>

        <tr>

            <td width="49%">
                <table class="styledLeft" id="topicCountPlot<%=topicSimpleName%>" width="100%">
                    <thead>
                    <tr>
                        <th>&quot;<%=topicNames[i]%>&quot;&nbsp;<fmt:message key="count.vs.time.units"/></th>
                    </tr>
                    </thead>
                    <tr>
                        <td>
                            <div id="topicGraph<%=topicSimpleName%>"
                                 style="width:<%= topicGraphWidth%>px;height:300px;"></div>
                        </td>
                    </tr>
                </table>
            </td>

            <script type="text/javascript">
                graphTopicRequest['<%= topicSimpleName%>'].add(<%= topicCounts[i].getRequestCount()%>);
                graphTopicResponse['<%= topicSimpleName%>'].add(<%= topicCounts[i].getResponseCount()%>);
                drawTopicGraph('<%= topicSimpleName%>');
            </script>

            <td width="2%">&nbsp;</td>

            <%
                if ((i + 1) < topicNamesLength) {
                    topicSimpleName= topicNames[i+1].replaceAll("\\.","_").replaceAll("/","_sl_").replaceAll(" ","_").replaceAll(",","_c_").replaceAll("@","_at_") ;
            %>
            <td width="49%">

                <table class="styledLeft" id="topicCountPlot<%=topicSimpleName%>" width="100%">
                    <thead>
                    <tr>
                        <th>&quot;<%=topicNames[i + 1]%>&quot;&nbsp;<fmt:message
                                key="count.vs.time.units"/></th>
                    </tr>
                    </thead>
                    <tr>
                        <td>
                            <div id="topicGraph<%=topicSimpleName%>"
                                 style="width:<%= topicGraphWidth%>px;height:300px;"></div>
                        </td>
                    </tr>
                </table>
            </td>

            <script type="text/javascript">
                graphTopicRequest['<%= topicSimpleName%>'].add(<%= topicCounts[i+1].getRequestCount()%>);
                graphTopicResponse['<%= topicSimpleName%>'].add(<%= topicCounts[i+1].getResponseCount()%>);
                drawTopicGraph('<%= topicSimpleName%>');
            </script>

            <%
            } else {
            %>

            <td width="49%">&nbsp;</td>
            <%
                }
            %>
        </tr>
        <tr>
            <td colspan="3">&nbsp;</td>
        </tr>
        <%
                }
            }
        %>
    </table>
</fmt:bundle>
