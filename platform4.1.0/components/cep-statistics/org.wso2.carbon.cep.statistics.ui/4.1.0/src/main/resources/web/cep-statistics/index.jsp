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
<%@ page import="org.wso2.carbon.cep.statistics.stub.types.carbon.CountDTO" %>
<%@ page import="org.wso2.carbon.cep.statistics.ui.CEPStatisticsAdminClient" %>
<%@ page import="org.wso2.carbon.cep.statistics.ui.Utils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>


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
    int statRefreshInterval = 6000;
    statRefreshInterval = Utils.getPositiveIntegerValue(session, request, statRefreshInterval, "statRefreshInterval");

    int cepGraphWidth = 1200;
    cepGraphWidth = Utils.getPositiveIntegerValue(session, request, cepGraphWidth, "cepGraphWidth");

    int cepGraphXScale = 25;
    cepGraphXScale = Utils.getPositiveIntegerValue(session, request, cepGraphXScale, "cepGraphXScale");


    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CEPStatisticsAdminClient client = new CEPStatisticsAdminClient(cookie, backendServerURL,
                                                                   configContext, request.getLocale());
    try {
        Utils.setCepCount(client.getGlobalCount(), session);
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
<script id="source" type="text/javascript">
    jQuery.noConflict();
    var cepGraphWidth = <%= cepGraphWidth %>;

    var cepXScale = <%= cepGraphXScale%>;

    initStats(cepXScale);

    function drawCepGraph() {
        jQuery.plot(jQuery("#cepGraph"), [
            {
                label:"<fmt:message key="request.count"/>",
                data:graphCepRequest.get(),
                lines:{ show:true, fill:true }
            },
            {
                label:"<fmt:message key="response.count"/>",
                data:graphCepResponse.get(),
                lines:{ show:true, fill:true }
            }
        ], {
            xaxis:{
                ticks:graphCepRequest.tick(),
                min:0
            },
            yaxis:{
                ticks:10,
                min:0
            }
        });
    }

    function draw() {
        drawCepGraph();
    }

    function checkMinValues() {
        var statRefreshInterval = document.statsConfigForm.statRefreshInterval.value;
        if (statRefreshInterval < 500) {
            document.statsConfigForm.statRefreshInterval.value = 500;
        }

        var cepGraphXScale = document.statsConfigForm.cepGraphXScale.value;
        if (cepGraphXScale < 10) {
            document.statsConfigForm.cepGraphXScale.value = 10;
        }

        var cepGraphWidth = document.statsConfigForm.cepGraphWidth.value;
        if (cepGraphWidth < 250) {
            document.statsConfigForm.cepGraphWidth.value = 250;
        }
    }

    function restoreDefaultValues() {
        CARBON.showConfirmationDialog("<fmt:message key="restore.defaults.prompt"/>", function () {
            document.statsConfigForm.statRefreshInterval.value = 6000;
            document.statsConfigForm.cepGraphXScale.value = 50;
            document.statsConfigForm.cepGraphWidth.value = 1200;
            document.statsConfigForm.submit();
        });
    }
</script>

<div id="middle">
    <h2><fmt:message key="cep.statistics"/></h2>

    <div id="workArea">
        <div id="result"></div>
        <script type="text/javascript">
            jQuery.noConflict()
            var refresh;
            function refreshStats() {
                var url = "cep_stats_ajaxprocessor.jsp";
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

        <form action="index.jsp" method="post" name="statsConfigForm">
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

                    <td colspan="2" width="50%"><strong><fmt:message
                            key="cep.overall.graph"/></strong></td>

                </tr>
                <tr>

                    <td width="20%">
                        <fmt:message key="x.scale"/>
                    </td>

                    <td width="30%">
                        <input type="text" size="5" value="<%= cepGraphXScale%>"
                               name="cepGraphXScale"
                               maxlength="4"/>
                    </td>


                </tr>
                <tr>

                    <td width="20%">
                        <fmt:message key="x.width"/>
                    </td>
                    <td width="30%">
                        <input type="text" size="5" value="<%= cepGraphWidth%>"
                               name="cepGraphWidth"
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
