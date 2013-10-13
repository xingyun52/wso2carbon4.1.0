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
<%@ page import="org.wso2.carbon.brokermanager.stub.BrokerManagerAdminServiceStub" %>
<%@ page import="org.wso2.carbon.brokermanager.stub.types.BrokerConfigurationDetails" %>
<%@ page import="org.wso2.carbon.cep.statistics.ui.Utils" %>

<fmt:bundle basename="org.wso2.carbon.cep.statistics.ui.i18n.Resources">

    <carbon:breadcrumb
            label="brokermanager.list"
            resourceBundle="org.wso2.carbon.cep.statistics.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>

    <div id="middle">
    <h2><img src="images/broker.gif" alt=""/> <fmt:message key="brokers"/></h2>

    <div id="workArea">
        <table class="styledLeft">
            <thead>
            <tr>
                <th><fmt:message key="broker.name"/></th>
                <th><fmt:message key="broker.type"/></th>
                <th><fmt:message key="monitor"/></th>
            </tr>
            </thead>
            <tbody>
            <%
                BrokerManagerAdminServiceStub stub = Utils.getBrokerManagerAdminService(config, session, request);
                BrokerConfigurationDetails[] brokerDetailsArray = stub.getAllBrokerConfigurationNamesAndTypes();
                if (brokerDetailsArray != null) {
                    for (BrokerConfigurationDetails brokerDetails : brokerDetailsArray) {

            %>
            <tr>
                <td>
                    <%=brokerDetails.getBrokerName()%>
                </td>
                <td><%=brokerDetails.getBrokerType()%>
                </td>
                <td>
                    <a style="background-image: url(images/chart_bar.gif);"
                       class="icon-link"
                       href="sub_stats_monitor.jsp?brokerName=<%=brokerDetails.getBrokerName()%>"><fmt:message
                            key="monitor"/>
                    </a>
                </td>

            </tr>
            <%
                    }
                }
            %>
            </tbody>
        </table>

        <div>
            <form id="deleteForm" name="input" action="" method="get"><input type="HIDDEN"
                                                                             name="brokername"
                                                                             value=""/></form>
        </div>
    </div>


    <script type="text/javascript">
        alternateTableRows('expiredsubscriptions', 'tableEvenRow', 'tableOddRow');
        alternateTableRows('validsubscriptions', 'tableEvenRow', 'tableOddRow');
    </script>

</fmt:bundle>
