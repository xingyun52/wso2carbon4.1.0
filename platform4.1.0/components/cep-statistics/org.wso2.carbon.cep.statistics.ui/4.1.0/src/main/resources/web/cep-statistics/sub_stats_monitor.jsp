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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.cep.statistics.ui.CEPStatisticsAdminClient" %>
<%@ page import="org.wso2.carbon.cep.statistics.ui.Utils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.cep.statistics.stub.types.carbon.CollectionDTO" %>

<fmt:bundle basename="org.wso2.carbon.cep.statistics.ui.i18n.Resources">
<carbon:breadcrumb
        label="cep.statistics"
        resourceBundle="org.wso2.carbon.cep.statistics.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<script type="text/javascript" src="js/statistics.js"></script>
<script type="text/javascript" src="js/graphs.js"></script>

<script type="text/javascript" src="../admin/js/jquery.flot.js"></script>
<script type="text/javascript" src="../admin/js/excanvas.js"></script>

<%
    String bucketName = request.getParameter("bucketName");
    String brokerName = request.getParameter("brokerName");
    boolean bucket = false;
    if (bucketName != null) {
        bucket = true;
    }


    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CEPStatisticsAdminClient client = new CEPStatisticsAdminClient(cookie, backendServerURL,
                                                                   configContext, request.getLocale());


    CollectionDTO collectionDTO;
    if (bucket) {
        collectionDTO = client.getBucketStatistics(bucketName);
        Utils.setSubCount(bucketName, collectionDTO, session, bucket);
    } else {
        collectionDTO = client.getBrokerStatistics(brokerName);
        Utils.setSubCount(brokerName, collectionDTO, session, bucket);
    }
    String topicSimpleNames = "";
    String[] topicNamesArray = collectionDTO.getTopicNames();
    if (topicNamesArray != null) {

        for (String topic : topicNamesArray) {
            topicSimpleNames = topicSimpleNames + ",'" + topic.replaceAll("\\.","_").replaceAll("/","_sl_").replaceAll(" ","_").replaceAll(",","_c_").replaceAll("@","_at_") + "'";
        }
        topicSimpleNames = topicSimpleNames.substring(1);
    }
    int statRefreshInterval = 6000;
    statRefreshInterval = Utils.getPositiveIntegerValue(session, request, statRefreshInterval, "statRefreshInterval");

    int mainGraphWidth = 1200;
    mainGraphWidth = Utils.getPositiveIntegerValue(session, request, mainGraphWidth, "mainGraphWidth");

    int mainGraphXScale = 50;
    mainGraphXScale = Utils.getPositiveIntegerValue(session, request, mainGraphXScale, "mainGraphXScale");

    int topicGraphWidth = 550;
    topicGraphWidth = Utils.getPositiveIntegerValue(session, request, topicGraphWidth, "topicGraphWidth");

    int topicGraphXScale = 25;
    topicGraphXScale = Utils.getPositiveIntegerValue(session, request, topicGraphXScale, "topicGraphXScale");

    boolean isSuperTenant = CarbonUIUtil.isSuperTenant(request);
%>
<script id="source" type="text/javascript">
    jQuery.noConflict();
    var mainGraphWidth = <%= mainGraphWidth %>;
    var mainGraphXScale = <%= mainGraphXScale %>;
    var topicGraphWidth = <%= topicGraphWidth %>;
    var topicGraphXScale = <%= topicGraphXScale %>;
    var topicSimpleNames = [<%=topicSimpleNames %>];

    initSubStats(mainGraphXScale, topicGraphXScale, topicSimpleNames);

    function drawMainGraph() {
        jQuery.plot(jQuery("#mainGraph"), [
            {
                label:"<fmt:message key="request.count"/>",
                data:graphMainRequest.get(),
                lines:{ show:true, fill:true }
            },
            {
                label:"<fmt:message key="response.count"/>",
                data:graphMainResponse.get(),
                lines:{ show:true, fill:true }
            }
        ], {
            xaxis:{
                ticks:graphMainRequest.tick(),
                min:0
            },
            yaxis:{
                ticks:10,
                min:0
            }
        });
    }
    function drawTopicGraph(topicSimpleName) {
        jQuery.plot(jQuery("#topicGraph" + topicSimpleName), [
            {
                label:"<fmt:message key="request.count"/>",
                data:graphTopicRequest[topicSimpleName].get(),
                lines:{ show:true, fill:true }
            },
            {
                label:"<fmt:message key="response.count"/>",
                data:graphTopicResponse[topicSimpleName].get(),
                lines:{ show:true, fill:true }
            }
        ], {
            xaxis:{
                ticks:graphTopicRequest[topicSimpleName].tick(),
                min:0
            },
            yaxis:{
                ticks:10,
                min:0
            }
        });
    }

    function draw() {
        drawMainGraph();
        for (var i = 0; i < topicSimpleNames.length; i++) {
            drawTopicGraph(topicSimpleNames[i]);
        }
    }

    function checkMinValues() {
        var statRefreshInterval = document.statsConfigForm.statRefreshInterval.value;
        if (statRefreshInterval < 500) {
            document.statsConfigForm.statRefreshInterval.value = 500;
        }

        var mainGraphXScale = document.statsConfigForm.mainGraphXScale.value;
        if (mainGraphXScale < 10) {
            document.statsConfigForm.mainGraphXScale.value = 10;
        }
        var topicGraphXScale = document.statsConfigForm.topicGraphXScale.value;
        if (topicGraphXScale < 10) {
            document.statsConfigForm.topicGraphXScale.value = 10;
        }

        var mainGraphWidth = document.statsConfigForm.mainGraphWidth.value;
        if (mainGraphWidth < 250) {
            document.statsConfigForm.mainGraphWidth.value = 250;
        }
        var topicGraphWidth = document.statsConfigForm.topicGraphWidth.value;
        if (topicGraphWidth < 250) {
            document.statsConfigForm.topicGraphWidth.value = 250;
        }
    }

    function restoreDefaultValues() {
        CARBON.showConfirmationDialog("<fmt:message key="restore.defaults.prompt"/>", function () {
            document.statsConfigForm.statRefreshInterval.value = 6000;
            document.statsConfigForm.mainGraphXScale.value = 100;
            document.statsConfigForm.mainGraphWidth.value = 1200;
            document.statsConfigForm.topicGraphXScale.value = 50;
            document.statsConfigForm.topicGraphWidth.value = 550;
            document.statsConfigForm.submit();
        });
    }
</script>

<div id="middle">
    <%
        if (bucket) {
    %>
    <h2><%=bucketName%>&nbsp;<fmt:message key="bucket.statistics"/></h2>
    <%
    } else {
    %>
    <h2><%=brokerName%>&nbsp;<fmt:message key="broker.statistics"/></h2>
    <%
        }
    %>

    <div id="workArea">
        <div id="result"></div>
        <script type="text/javascript">
            jQuery.noConflict()
            var refresh;
            function refreshStats() {
            <%
                if(bucket){
            %>
                var url = "sub_stats_ajaxprocessor.jsp?bucketName=<%=bucketName%>";
            <%
                }else {
            %>
                var url = "sub_stats_ajaxprocessor.jsp?brokerName=<%=brokerName%>";
            <%
                }
            %>
                jQuery("#result").load(url, null, function (responseText, status, XMLHttpRequest) {
                    if (status != "success") {
                        stopRefreshStats();
                        document.getElementById('result').innerHTML = responseText;
                    }
                });
            }
            function stopRefreshStats() {
                if (refresh) {
                    clearInterval(refresh);
                }
            }
            jQuery(document).ready(function () {
                refreshStats();
                refresh = setInterval("refreshStats()", <%= statRefreshInterval %>);
            });
        </script>

        <p>&nbsp;</p>

        <form action="<%=bucket?"sub_stats_monitor.jsp?bucketName="+bucketName:"sub_stats_monitor.jsp?brokerName="+ brokerName%>" method="post" name="statsConfigForm">

            <table width="100%" class="styledLeft" style="margin-left: 0px;">
                <thead>
                <tr>
                    <th colspan="2"><fmt:message key="statistics.configuration"/></th>
                </tr>
                </thead>
                <tr>
                    <td width="20%"><fmt:message key="statistics.refresh.interval"/></td>
                    <td>
                        <input type="text" value="<%= statRefreshInterval%>"
                               name="statRefreshInterval"
                               size="5" maxlength="5"/>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">&nbsp;</td>
                </tr>

                <tr>

                    <%
                        if (bucket) {
                    %>
                    <td colspan="2" width="50%"><strong><fmt:message
                            key="bucket.chart"/></strong></td>
                    <%
                    } else {
                    %>
                    <td colspan="2" width="50%"><strong><fmt:message
                            key="broker.chart"/></strong></td>
                    <%
                        }
                    %>

                </tr>
                <tr>

                    <td width="20%">
                        <fmt:message key="x.scale"/>
                    </td>

                    <td width="30%">
                        <input type="text" size="5" value="<%= mainGraphXScale%>"
                               name="mainGraphXScale"
                               maxlength="4"/>
                    </td>


                </tr>
                <tr>

                    <td width="20%">
                        <fmt:message key="x.width"/>
                    </td>
                    <td width="30%">
                        <input type="text" size="5" value="<%= mainGraphWidth%>"
                               name="mainGraphWidth"
                               maxlength="4"/>
                    </td>

                </tr>
                <tr>

                    <td colspan="2" width="50%"><strong><fmt:message
                            key="topic.chart"/></strong></td>

                </tr>
                <tr>

                    <td width="20%">
                        <fmt:message key="x.scale"/>
                    </td>

                    <td width="30%">
                        <input type="text" size="5" value="<%= topicGraphXScale%>"
                               name="topicGraphXScale"
                               maxlength="4"/>
                    </td>


                </tr>
                <tr>

                    <td width="20%">
                        <fmt:message key="x.width"/>
                    </td>
                    <td width="30%">
                        <input type="text" size="5" value="<%= topicGraphWidth%>"
                               name="topicGraphWidth"
                               maxlength="4"/>
                    </td>

                </tr>
                <tr>
                    <td colspan="2">
                        &nbsp;
                    </td>
                </tr>
                <tr>
                    <td colspan="2" class="buttonRow">
                        <input type="button" class="button" value="<fmt:message key="update"/>"
                               id="updateStats"
                               onclick="checkMinValues();document.statsConfigForm.submit()"/>&nbsp;&nbsp;
                        <input type="reset" class="button" value="<fmt:message key="reset"/>"/>&nbsp;&nbsp;
                        <input type="button" class="button"
                               value="<fmt:message key="restore.defaults"/>"
                               id="restoreDefaults" onclick="restoreDefaultValues()"/>&nbsp;&nbsp;
                    </td>
                </tr>
            </table>
        </form>
    </div>
</div>
</fmt:bundle>
