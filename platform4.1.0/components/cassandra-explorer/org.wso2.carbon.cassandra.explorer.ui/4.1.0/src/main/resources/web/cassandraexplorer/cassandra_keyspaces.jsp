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

<%@ page import="org.wso2.carbon.cassandra.explorer.ui.CassandraExplorerAdminClient" %>
<%@ page import="org.wso2.carbon.cassandra.explorer.stub.CassandraExplorerAdminCassandraExplorerException" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<script type="text/javascript" src="js/cassandra_cf_explorer.js"></script>
<script language="text/javascript" src="../admin/js/customControls.js"></script>

<fmt:bundle basename="org.wso2.carbon.cassandra.explorer.ui.i18n.Resources">
    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.cassandra.explorer.ui.i18n.Resources"
            request="<%=request%>" i18nObjectName="cassandrajsi18n"/>
    <carbon:breadcrumb
            label="cassandra.explorer.explore"
            resourceBundle="org.wso2.carbon.cassandra.explorer.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>
    <%
        String[] keyspaces = new String[0];
        CassandraExplorerAdminClient adminClient
                = new CassandraExplorerAdminClient(config.getServletContext(), session);
        try {
            keyspaces = adminClient.getKeyspaces();
        } catch (CassandraExplorerAdminCassandraExplorerException e) { %>
    <script type="text/javascript">
        jQuery(document).ready(function () {
            CARBON.showErrorDialog('Connection Error.<br>Please retry with correct connection details.', function () {
                CARBON.closeWindow();
                location.href = "cassandra_connect.jsp";
            }, function () {
                CARBON.closeWindow();
                location.href = "cassandra_connect.jsp";
            });
        });
    </script>
    <% }
    %>
    <script>
        jQuery(document).ready(function () {
            initSections("");
        });

    </script>
    <style>
        .sectionSeperator {
            margin-bottom: 0;
        }

        .sectionSub {
            padding: 0;
            margin: 0 0 10px 0;
        }
    </style>

    <div id="middle">
        <h2>Keyspaces</h2>

        <div id="workArea">
            <!-- Section 1 -->
            <%
                for (String keyspace : keyspaces) {%>
            <div class="sectionSeperator togglebleTitle"><%=keyspace%>
            </div>
            <div class="sectionSub">
                <table width="100%" id="internal" class="styledLeft">
                    <tbody>
                    <% String[] columnFamilies = adminClient.getColumnFamilies(keyspace);
                        if (columnFamilies != null) {
                            for (int i = 0; i < columnFamilies.length; i++) {
                                String rowType;
                                if (i + 1 % 2 == 0) {
                                    rowType = "tableEvenRow";
                                } else {
                                    rowType = "tableOddRow";
                                }
                    %>
                    <tr class=<%=rowType%>>
                        <td>
                            <a href="#"
                               onclick="viewRowExplorer('<%=keyspace%>','<%=columnFamilies[i]%>')"
                               style="background-image:url(images/column_familiy.png);"
                               class="icon-link"><%=columnFamilies[i]%>
                            </a>
                        </td>
                            <%--<td width="30%">
                                <a href="#" onclick="viewExplorer('<%=keyspace%>','<%=columnFamilies[i]%>')"
                                   style="background-image:url(images/column_familiy.png);" class="icon-link">Summary View</a>
                            </td>--%>

                    </tr>
                    <%
                            }
                        }
                    %>
                    </tbody>
                </table>
            </div>
            <%}%>
        </div>
    </div>
</fmt:bundle>
