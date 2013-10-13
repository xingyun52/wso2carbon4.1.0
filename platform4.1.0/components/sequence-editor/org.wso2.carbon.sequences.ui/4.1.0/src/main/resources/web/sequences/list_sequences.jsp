<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%--
  ~  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~  Licensed under the Apache License, Version 2.0 (the "License");
  ~  you may not use this file except in compliance with the License.
  ~  You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing, software
  ~  distributed under the License is distributed on an "AS IS" BASIS,
  ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~  See the License for the specific language governing permissions and
  ~  limitations under the License.
  --%>
<%@ page import="org.wso2.carbon.sequences.common.to.SequenceInfo" %>
<%@ page import="org.wso2.carbon.sequences.ui.client.SequenceAdminClient" %>
<%@ page import="org.wso2.carbon.sequences.ui.SequenceEditorConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<fmt:bundle basename="org.wso2.carbon.sequences.ui.i18n.Resources">
<carbon:jsi18n
        resourceBundle="org.wso2.carbon.sequences.ui.i18n.JSResources"
        request="<%=request%>"/>

<link type="text/css" href="../dialog/js/jqueryui/tabs/ui.all.css" rel="stylesheet"/>
<script type="text/javascript" src="../dialog/js/jqueryui/tabs/jquery-1.2.6.min.js"></script>
<script type="text/javascript"
        src="../dialog/js/jqueryui/tabs/jquery-ui-1.6.custom.min.js"></script>
<script type="text/javascript" src="../dialog/js/jqueryui/tabs/jquery.cookie.js"></script>

<script type="text/javascript">
    function confirmForceDelete(sequenceName, msg) {
        CARBON.showConfirmationDialog('<fmt:message key="sequence.dependency.mgt.warning"/><br/><br/>'
                + msg + '<br/><fmt:message key="force.delete"/>', function() {
            location.href = "delete_sequence.jsp?sequenceName=" + sequenceName + "&force=true";
        });
    }
</script>
<link rel="stylesheet" type="text/css" href="../yui/build/container/assets/skins/sam/container.css">

<script type="text/javascript" src="../yui/build/yahoo-dom-event/yahoo-dom-event.js"></script>
<script type="text/javascript" src="../yui/build/container/container-min.js"></script>
<script type="text/javascript" src="../yui/build/element/element-min.js"></script>
<script type="text/javascript" src="../admin/js/widgets.js"></script>
<%
    //remove session variables if user exited form design sequence of proxy admin
    session.removeAttribute("sequence");

    //remove any sessions related to templates since template mode settings should not interfere
    //with sequence editor mode settings

    //remove attribute to restate sequence-editor mode
    session.removeAttribute("editorClientFactory");
    session.removeAttribute("sequenceAnonOriginator");
    //remove any endpoint template related session attribs to avoid any confilcts
    session.removeAttribute("endpointTemplate");
    session.removeAttribute("templateEdittingMode");
    session.removeAttribute("templateRegKey");

    SequenceAdminClient sequenceAdminClient
            = new SequenceAdminClient(this.getServletConfig(), session);
    SequenceInfo[] sequences = null;
    SequenceInfo[] dynamicSequences = null;
    String pageNumberStr = request.getParameter("pageNumber");
    String dynamicPageNumberStr = request.getParameter("dynamicPageNumber");
    int pageNumber = 0;
    int dynamicPageNumber = 0;
    if (pageNumberStr != null) {
        pageNumber = Integer.parseInt(pageNumberStr);
    }
    if (dynamicPageNumberStr != null) {
        dynamicPageNumber = Integer.parseInt(dynamicPageNumberStr);
    }
    int numberOfPages = 0;
    int numberOfDynamicPages = 0;
    try {
        sequences = sequenceAdminClient.getSequences(pageNumber, SequenceEditorConstants.SEQUENCE_PER_PAGE);
        dynamicSequences = sequenceAdminClient.getDynamicSequences(dynamicPageNumber,
                SequenceEditorConstants.SEQUENCE_PER_PAGE);
        int seqCount = sequenceAdminClient.getSequencesCount();
        int dynamicSequenceCount = sequenceAdminClient.getDynamicSequenceCount();

        if (seqCount % SequenceEditorConstants.SEQUENCE_PER_PAGE == 0) {
            numberOfPages = seqCount / SequenceEditorConstants.SEQUENCE_PER_PAGE;
        } else {
            numberOfPages = seqCount / SequenceEditorConstants.SEQUENCE_PER_PAGE + 1;
        }

        if (dynamicSequenceCount % SequenceEditorConstants.SEQUENCE_PER_PAGE == 0) {
            numberOfDynamicPages = dynamicSequenceCount / SequenceEditorConstants.SEQUENCE_PER_PAGE;
        } else {
            numberOfDynamicPages = dynamicSequenceCount / SequenceEditorConstants.SEQUENCE_PER_PAGE + 1;
        }
    } catch (Exception e) {
        CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
%>
<script type="text/javascript">
    location.href = "../admin/error.jsp";
</script>
<%
        return;
    }

    ResourceBundle bundle = ResourceBundle.getBundle("org.wso2.carbon.sequences.ui.i18n.Resources",
            request.getLocale());
    if ("fail".equals(session.getAttribute("dynamic_edit"))) {
%>
<script type="text/javascript">
    CARBON.showErrorDialog("<%= bundle.getString(
                    "unable.to.build.sequence.object.from.the.given.sequence.information") %>");
</script>
<%
        session.removeAttribute("dynamic_edit");
    }
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    session.removeAttribute("mediator.position");

    String dependencyMgtError = (String) session.getAttribute("seq.d.mgt.error.msg");
    if (dependencyMgtError != null) {
        String seqToDelete = (String) session.getAttribute("seq.d.mgt.error.name");
%>
<script type="text/javascript">
    confirmForceDelete('<%=seqToDelete%>', '<%=dependencyMgtError%>');
</script>
<%
        session.removeAttribute("seq.d.mgt.error.msg");
        session.removeAttribute("seq.d.mgt.error.name");
    }

%>


<script type="text/javascript" src="../carbon/global-params.js"></script>

<script type="text/javascript">

    wso2.wsf.Util.initURLs();
    var ENABLE = "enable";
    var DISABLE = "disable";
    var STAT = "statistics";
    var TRACE = "Tracing";

    var frondendURL = wso2.wsf.Util.getServerURL() + "/";

    function addSequence() {
        document.location.href = "design_sequence.jsp?sequenceAction=add";
    }

    function disableStat(sequenceName) {
        $.ajax({
            type: 'POST',
            url: 'stat_tracing-ajaxprocessor.jsp',
            data: 'sequenceName=' + sequenceName + '&action=disableStat',
            success: function(msg) {
                handleCallback(sequenceName, DISABLE, STAT);
            },
            error: function(msg) {
                CARBON.showErrorDialog('<fmt:message key="sequence.stat.disable.error"/>' +
                        ' ' + sequenceName);
            }
        });
    }

    function enableStat(sequenceName) {
        $.ajax({
            type: 'POST',
            url: 'stat_tracing-ajaxprocessor.jsp',
            data: 'sequenceName=' + sequenceName + '&action=enableStat',
            success: function(msg) {
                handleCallback(sequenceName, ENABLE, STAT);
            },
            error: function(msg) {
                CARBON.showErrorDialog('<fmt:message key="sequence.stat.enable.error"/>' +
                        ' ' + sequenceName);
            }
        });
    }

    function handleCallback(seq, action, type) {
        var element;
        if (action == "enable") {
            if (type == "statistics") {
                element = document.getElementById("disableStat" + seq);
                element.style.display = "";
                element = document.getElementById("enableStat" + seq);
                element.style.display = "none";
            } else {
                element = document.getElementById("disableTracing" + seq);
                element.style.display = "";
                element = document.getElementById("enableTracing" + seq);
                element.style.display = "none";
            }
        } else {
            if (type == "statistics") {
                element = document.getElementById("disableStat" + seq);
                element.style.display = "none";
                element = document.getElementById("enableStat" + seq);
                element.style.display = "";
            } else {
                element = document.getElementById("disableTracing" + seq);
                element.style.display = "none";
                element = document.getElementById("enableTracing" + seq);
                element.style.display = "";
            }
        }
    }

    function enableTracing(sequenceName) {
        $.ajax({
            type: 'POST',
            url: 'stat_tracing-ajaxprocessor.jsp',
            data: 'sequenceName=' + sequenceName + '&action=enableTracing',
            success: function(msg) {
                handleCallback(sequenceName, ENABLE, TRACE);
            },
            error: function(msg) {
                CARBON.showErrorDialog('<fmt:message key="sequence.trace.enable.link"/>' +
                        ' ' + sequenceName);
            }
        });
    }

    function disableTracing(sequenceName) {
        $.ajax({
            type: 'POST',
            url: 'stat_tracing-ajaxprocessor.jsp',
            data: 'sequenceName=' + sequenceName + '&action=disableTracing',
            success: function(msg) {
                handleCallback(sequenceName, DISABLE, TRACE);
            },
            error: function(msg) {
                CARBON.showErrorDialog('<fmt:message key="sequence.trace.disable.error"/>' +
                        ' ' + sequenceName);
            }
        });
    }

    function editSequence() {
        document.location.href = "design_sequence.jsp?sequenceAction=edit&sequenceName=" + arguments[0];
    }

    function deleteSequence(sequenceName) {
        if (sequenceName == "main" || sequenceName == "fault") {
            CARBON.showWarningDialog('<fmt:message key="sequence.main.fault.cannot.delete"/>');
        } else {
            CARBON.showConfirmationDialog("<fmt:message key="sequence.delete.confirmation"/> " + sequenceName + "?", function() {
                location.href = "delete_sequence.jsp?sequenceName=" + sequenceName;
            });
        }
    }

    function getResponseValue(responseXML) {
        var returnElementList = responseXML.getElementsByTagName("ns:return");
        // Older browsers might not recognize namespaces (e.g. FF2)
        if (returnElementList.length == 0)
            returnElementList = responseXML.getElementsByTagName("return");
        var returnElement = returnElementList[0];

        return returnElement.firstChild.nodeValue;
    }
    function editRegistrySequence(key) {
        if (key != null && key != undefined && key != "") {
            location.href = "registry_sequence.jsp?action=edit&key=" + key;
        } else {
            CARBON.showErrorDialog("Specify the key of the Sequence to be edited");
        }
    }

    function deleteRegistrySequence(sequenceName) {
        if (sequenceName == "main" || sequenceName == "fault") {
            CARBON.showWarningDialog('<fmt:message key="sequence.main.fault.cannot.delete"/>');
        } else {
            CARBON.showConfirmationDialog("<fmt:message key="sequence.delete.confirmation"/> " + sequenceName + "?", function() {
                location.href = "delete_sequence.jsp?type=registry&sequenceName=" + sequenceName;
            });
        }
    }

    function minMaxReg() {
        var minMaxRegBox = $('minMaxRegBox');
        if (minMaxRegBox.style.display == "none") {
            minMaxRegBox.style.display = "";
        } else {
            minMaxRegBox.style.display = "none";
        }
    }
    $(function() {
        $("#tabs").tabs();
    });

    //tab handling logic
    var tabIndex = -1;
    <%
    String tab = request.getParameter("tab");
    if(tab!=null && tab.equals("0")){
    %>
    tabIndex = 0;
    <%
    } else if (tab!=null && tab.equals("1")) {
    %>
    tabIndex = 1;
    <%}%>
    $(document).ready(function() {
        var $tabs = $('#tabs > ul').tabs({ cookie: { expires: 30 } });
        $('a', $tabs).click(function() {
            if ($(this).parent().hasClass('ui-tabs-selected')) {
                $tabs.tabs('load', $('a', $tabs).index(this));
            }
        });
        if (tabIndex == 0) {
            $tabs.tabs('option', 'selected', 0);
        } else if (tabIndex == 1) {
            $tabs.tabs('option', 'selected', 1);
        }
    });
</script>

<style type="text/css">
    .inlineDiv div {
        float: left;
    }
</style>

<carbon:breadcrumb
        label="sequence.menu.text"
        resourceBundle="org.wso2.carbon.sequences.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<div id="middle">

<h2>
    <fmt:message key="mediation.sequences.header"/>
</h2>

<div id="workArea" style="background-color:#F4F4F4;">
<div style="height:25px;">
    <a class="icon-link" style="background-image: url(../admin/images/add.gif);"
       href="javascript:addSequence()"><fmt:message key="sequence.button.add.text"/></a>
</div>
<div id="tabs">
<ul>
    <li><a href="#tabs-1"><fmt:message key="defined.sequences"/></a></li>
    <li><a href="#tabs-2"><fmt:message key="dynamic.sequencs"/></a></li>
</ul>
<div id="tabs-1">
    <p><fmt:message key="sequences.defined.text"/></p>
    <br/>
    <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                      page="list_sequences.jsp" pageNumberParameterName="pageNumber"
                      resourceBundle="org.wso2.carbon.sequences.ui.i18n.Resources"
                      prevKey="prev" nextKey="next"
                      parameters="<%=""%>"/>
    <br>
    <table class="styledLeft" cellspacing="1" id="sequencesTable">
        <thead>
        <tr>
            <th>
                <fmt:message key="sequence.name"/>
            </th>
            <th colspan="4">
                <fmt:message key="sequence.actions"/>
            </th>
        </tr>
        </thead>
        <tbody>
        <% for (SequenceInfo sequence : sequences) { %>
        <tr>
            <td>
                <span href="#" <% if(sequence.getDescription()!= null){ %>onmouseover="showTooltip(this,'<%=sequence.getDescription()%>')" <% } %>><%= sequence.getName() %></span>
            </td>

            <% if (sequence.isEnableStatistics()) { %>
            <td style="border-right:none;border-left:none;width:200px">
                <div class="inlineDiv">
                    <div id="disableStat<%= sequence.getName()%>">
                        <a href="#" onclick="disableStat('<%= sequence.getName() %>')"
                           class="icon-link"
                           style="background-image:url(../admin/images/static-icon.gif);"><fmt:message
                                key="sequence.stat.disable.link"/></a>
                    </div>
                    <div id="enableStat<%= sequence.getName()%>" style="display:none;">
                        <a href="#" onclick="enableStat('<%= sequence.getName() %>')"
                           class="icon-link"
                           style="background-image:url(../admin/images/static-icon-disabled.gif);"><fmt:message
                                key="sequence.stat.enable.link"/></a>
                    </div>
                </div>
            </td>
            <% } else { %>
            <td style="border-right:none;border-left:none;width:200px">
                <div class="inlineDiv">
                    <div id="enableStat<%= sequence.getName()%>">
                        <a href="#" onclick="enableStat('<%= sequence.getName() %>')"
                           class="icon-link"
                           style="background-image:url(../admin/images/static-icon-disabled.gif);"><fmt:message
                                key="sequence.stat.enable.link"/></a>
                    </div>
                    <div id="disableStat<%= sequence.getName()%>" style="display:none">
                        <a href="#" onclick="disableStat('<%= sequence.getName() %>')"
                           class="icon-link"
                           style="background-image:url(../admin/images/static-icon.gif);"><fmt:message
                                key="sequence.stat.disable.link"/></a>
                    </div>
                </div>
            </td>
            <% } %>
            <% if (sequence.isEnableTracing()) { %>
            <td style="border-right:none;border-left:none;width:200px">
                <div class="inlineDiv">
                    <div id="disableTracing<%= sequence.getName()%>">
                        <a href="#"
                           onclick="disableTracing('<%= sequence.getName() %>')"
                           class="icon-link"
                           style="background-image:url(../admin/images/trace-icon.gif);"><fmt:message
                                key="sequence.trace.disable.link"/></a>
                    </div>
                    <div id="enableTracing<%= sequence.getName()%>"
                         style="display:none;">
                        <a href="#" onclick="enableTracing('<%= sequence.getName() %>')"
                           class="icon-link"
                           style="background-image:url(../admin/images/trace-icon-disabled.gif);"><fmt:message
                                key="sequence.trace.enable.link"/></a>
                    </div>
                </div>
            </td>
            <% } else { %>
            <td style="border-right:none;border-left:none;width:200px">
                <div class="inlineDiv">
                    <div id="enableTracing<%= sequence.getName()%>">
                        <a href="#" onclick="enableTracing('<%= sequence.getName() %>')"
                           class="icon-link"
                           style="background-image:url(../admin/images/trace-icon-disabled.gif);"><fmt:message
                                key="sequence.trace.enable.link"/></a>
                    </div>
                    <div id="disableTracing<%= sequence.getName()%>"
                         style="display:none">
                        <a href="#"
                           onclick="disableTracing('<%= sequence.getName() %>')"
                           class="icon-link"
                           style="background-image:url(../admin/images/trace-icon.gif);"><fmt:message
                                key="sequence.trace.disable.link"/></a>
                    </div>
                </div>
            </td>
            <% } %>
            <td style="border-left:none;border-right:none;width:100px">
                <div class="inlineDiv">
                    <a href="#" onclick="editSequence('<%= sequence.getName() %>')"
                       class="icon-link"
                       style="background-image:url(../admin/images/edit.gif);"><fmt:message
                            key="sequence.edit.action"/></a>
                </div>
            </td>
            <td style="border-left:none;width:100px">
                <div class="inlineDiv">
                    <a href="#" onclick="deleteSequence('<%= sequence.getName() %>')"
                       class="icon-link"
                       style="background-image:url(../admin/images/delete.gif);"><fmt:message
                            key="sequence.delete.action"/></a>
                </div>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>

    <script type="text/javascript">
        alternateTableRows('sequencesTable', 'tableEvenRow', 'tableOddRow');
    </script>
    <p>&nbsp;</p>
    <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                      page="list_sequences.jsp" pageNumberParameterName="pageNumber"
                      resourceBundle="org.wso2.carbon.sequences.ui.i18n.Resources"
                      prevKey="prev" nextKey="next"
                      parameters="<%=""%>"/>
</div>
<div id="tabs-2">
    <% if ((dynamicSequences != null) && (dynamicSequences.length > 0)) { %>
    <p><fmt:message key="sequences.dynamic.text"/></p>
    <br/>
    <carbon:paginator pageNumber="<%=dynamicPageNumber%>"
                      numberOfPages="<%=numberOfDynamicPages%>"
                      page="list_sequences.jsp"
                      pageNumberParameterName="dynamicPageNumber"
                      resourceBundle="org.wso2.carbon.sequences.ui.i18n.Resources"
                      prevKey="prev" nextKey="next"
                      parameters="<%=""%>"/>
    <br/>
    <table class="styledLeft" cellspacing="1" id="dynamicSequencesTable">
        <thead>
        <tr>
            <th>
                <fmt:message key="sequence.name"/>
            </th>
            <th style="width:200px" colspan="2">
                <fmt:message key="sequence.actions"/>
            </th>
        </tr>
        </thead>
        <tbody>
        <% for (SequenceInfo sequence : dynamicSequences) { %>
        <tr>
            <td style="width:200px">
                <%= sequence.getName() %>
            </td>
            <td style="border-right:none;width:100px">
                <div class="inlineDiv">
                    <a href="#"
                       onclick="editRegistrySequence('<%= sequence.getName() %>')"
                       class="icon-link"
                       style="background-image:url(../admin/images/edit.gif);"><fmt:message
                            key="sequence.edit.action"/></a>
                </div>
            </td>
            <td style="border-left:none;width:100px">
                <div class="inlineDiv">
                    <a href="#"
                       onclick="deleteRegistrySequence('<%= sequence.getName() %>')"
                       class="icon-link"
                       style="background-image:url(../admin/images/delete.gif);"><fmt:message
                            key="sequence.delete.action"/></a>
                </div>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <br>
    <carbon:paginator pageNumber="<%=dynamicPageNumber%>"
                      numberOfPages="<%=numberOfDynamicPages%>"
                      page="list_sequences.jsp"
                      pageNumberParameterName="dynamicPageNumber"
                      resourceBundle="org.wso2.carbon.sequences.ui.i18n.Resources"
                      prevKey="prev" nextKey="next"
                      parameters="<%=""%>"/>
    <script type="text/javascript">
        alternateTableRows('dynamicSequencesTable', 'tableEvenRow', 'tableOddRow');
    </script>

    <% } else { %>
    <p><fmt:message key="no.sequences.dynamic.text"/></p>
    <% } %>
</div>
</div>
</div>
</div>
</fmt:bundle>
