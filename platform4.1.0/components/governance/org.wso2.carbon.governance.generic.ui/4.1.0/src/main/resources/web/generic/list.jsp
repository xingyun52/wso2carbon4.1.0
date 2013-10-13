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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.wso2.carbon.governance.generic.ui.clients.ManageGenericArtifactServiceClient" %>
<%@ page import="org.wso2.carbon.governance.generic.ui.utils.UIGeneratorConstants" %>
<%@ page import="org.wso2.carbon.registry.core.RegistryConstants" %>
<%@ page import="org.wso2.carbon.registry.core.utils.RegistryUtils" %>
<%@ page
        import="org.wso2.carbon.registry.extensions.utils.CommonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.governance.generic.stub.beans.xsd.ArtifactsBean" %>
<%@ page import="org.wso2.carbon.governance.generic.stub.beans.xsd.ArtifactBean" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.governance.lcm.ui.clients.LifeCycleManagementServiceClient" %>
<%@ page import="org.wso2.carbon.governance.generic.ui.utils.GenericUIGenerator" %>
<%@ page import="org.apache.axiom.om.OMElement" %>

<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<link type="text/css" rel="stylesheet" href="css/menu.css"/>
<link type="text/css" rel="stylesheet" href="css/style.css"/>
<link type="text/css" rel="stylesheet" href="../resources/css/registry.css"/>
<jsp:include page="../dialog/display_messages.jsp"/>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<jsp:include page="../resources/resources-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../resources/js/resource_util.js"></script>
<script type="text/javascript" src="../generic/js/genericpagi.js"></script>
<script type="text/javascript" src="../generic/js/generic.js"></script>
<%
    String key = request.getParameter("key");
    String breadcrumb = request.getParameter("breadcrumb");
    String lc_name = request.getParameter("lc_name");
    String lc_state = request.getParameter("lc_state");
    String lc_in_out = request.getParameter("lc_in_out");
    String lc_state_in_out = request.getParameter("lc_state_in_out");

    String queryTrailer = "&key=" + key + "&breadcrumb=" + breadcrumb;
    String dataName = request.getParameter("dataName");
    if (dataName == null) {
        dataName = "metadata";
    } else {
        queryTrailer += "&dataName=" + dataName;
    }
    String dataNamespace = request.getParameter("dataNamespace");
    if (dataNamespace == null) {
        dataNamespace = UIGeneratorConstants.DATA_NAMESPACE;
    } else {
        queryTrailer += "&dataNamespace=" + dataNamespace;
    }
    String singularLabel = request.getParameter("singularLabel");
    if (singularLabel == null) {
        singularLabel = "Artifact";
    } else {
        queryTrailer += "&singularLabel=" + singularLabel;
    }
    String pluralLabel = request.getParameter("pluralLabel");
    if (pluralLabel == null) {
        pluralLabel = "Artifacts";
    } else {
        queryTrailer += "&pluralLabel=" + pluralLabel;
    }
    String criteria = null;
    boolean filter = request.getParameter("filter") != null;
    if (filter) {
        criteria = (String) session.getAttribute("criteria");
    }
    ArtifactsBean bean = null;
    String region = request.getParameter("region");
    String item = request.getParameter("item");
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String[] temp = null;
    LifeCycleManagementServiceClient LCClient;
    try{
        LCClient = new LifeCycleManagementServiceClient(cookie, config, session);
        temp = LCClient.getLifeCycleList(request);

    } catch (Exception e){
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
%>
<jsp:include page="../admin/error.jsp?<%=e.getMessage()%>"/>
<%
        return;
    }
    try {
        ManageGenericArtifactServiceClient client = new ManageGenericArtifactServiceClient(config, session);

        if(client != null) {
            if (lc_name == null && lc_state == null) {
                bean = client.listArtifacts(key, criteria);
            } else {
                bean = client.listArtifactsByLC(key, lc_name, lc_state, lc_in_out, lc_state_in_out);
            }
        }
    } catch (Exception e) {
        if (filter) {
%>
<script type="text/javascript">
    CARBON.showErrorDialog("<%=e.getMessage()%>", function() {
        location.href = "../generic/list.jsp?region=<%=region%>&item=<%=item%><%=queryTrailer%>";
        return;
    });

</script>
<%
} else {
%>
<script type="text/javascript">
    CARBON.showErrorDialog("<%=e.getMessage()%>", function() {
        location.href = "../admin/index.jsp";
        return;
    });

</script>
<%
        }
        return;
    }
%>
<fmt:bundle basename="org.wso2.carbon.governance.generic.ui.i18n.Resources">
    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.governance.generic.ui.i18n.JSResources"
            request="<%=request%>" namespace="org.wso2.carbon.governance.generic.ui"/>
    <carbon:breadcrumb
            label="<%=request.getParameter("breadcrumb")%>"
            topPage="true"
            request="<%=request%>"/>
    <br/>

    <script type="text/javascript">

         function downloadDependencies(path) {
            sessionAwareFunction(function() {
                new Ajax.Request('../generic/download_util_ajaxprocessor.jsp',
                        {
                            method:'post',
                            parameters: {path: path},

                            onSuccess: function(transport) {
                                var str = transport.responseText.trim();
                                var resp = str.substring(str.indexOf('{')+1).split('}')[0].trim();
                                var url = resp.split('**')[0];
                                var hasDependencies = resp.split('**')[1];
                                downloadWithDependencies(url,hasDependencies);
                            },

                            onFailure: function() {
                                CARBON.showErrorDialog(transport.responseText);
                            }
                        });

            }, org_wso2_carbon_governance_generic_ui_jsi18n["session.timed.out"]);
        }

        function submitFilterForm() {
            sessionAwareFunction(function() {
                var field = $('filterByList').value;

                if(field!=0 && field !=1){
                    var value = $('id_Search_Val').value;
                    document.getElementById('searchVal').name = toPascalCase(field).substring(0, field.length);
                    document.getElementById('searchVal').value = value;
                    submitToAdvanceFilter();
                }else if(field==1){
                    var lcname = $('lifeCycleList').value;
                    var state = $('stateList').value;
                    var lcinout =  $('inoutListLC').value;
                    var lcstateinout =  $('inoutListLCState').value;
                    document.getElementById('searchVal2').value = lcname;
                    if(state!="0"){
                        document.getElementById('searchVal3').value = state;
                        document.getElementById('searchVal4').value = lcinout;
                        document.getElementById('searchVal5').value = lcstateinout;
                    }else{
                        document.getElementById('searchVal3').value = "";
                        document.getElementById('searchVal4').value = lcinout;
                        document.getElementById('searchVal5').value = "";
                    }
                    if(lcname!= "Select"){
                        submitToLCFilter()
                    }
                }else if(field==0){
                    loadPagedList(1);
                }
            }, org_wso2_carbon_governance_generic_ui_jsi18n["session.timed.out"]);
        }

        function submitToLCFilter(){
            sessionAwareFunction(function() {
                var advancedSearchForm = $('filterLCForm');
                advancedSearchForm.submit();
            }, org_wso2_carbon_governance_generic_ui_jsi18n["session.timed.out"]);
        }

        function submitToAdvanceFilter(){
            sessionAwareFunction(function() {
                var advancedSearchForm = $('filterForm');
                advancedSearchForm.submit();
            }, org_wso2_carbon_governance_generic_ui_jsi18n["session.timed.out"]);
        }
    </script>

    <div id="middle">
        <h2><fmt:message key="artifact.list"><fmt:param value="<%=singularLabel%>"/><</fmt:message></h2>

        <div id="workArea">
            <%if ((bean.getArtifacts() != null && bean.getArtifacts().length != 0) || filter ) {%>
            <p style="padding:5px">

                    <%--This is a hidden form that is filled by the scripts when user search by any feild other than LC
          Will fill this form and will sent to the advance filter--%>
            <form id="filterForm" action="basic_filter_ajaxprocessor.jsp"
                  onsubmit="return submitToAdvanceFilter();" method="post">
                <input type="hidden" name="dataName" value="<%=dataName%>"/>
                <input type="hidden" name="singularLabel" value="<%=singularLabel%>"/>
                <input type="hidden" name="pluralLabel" value="<%=pluralLabel%>"/>
                <input type="hidden" name="dataNamespace" value="<%=dataNamespace%>">
                <input type="hidden" name="key" value="<%=key%>">
                <input type="hidden" name="region" value="<%=region%>">
                <input type="hidden" name="item" value="<%=item%>">
                <input type="hidden" name="breadcrumb" value="<%=breadcrumb%>">

                <input id="searchVal" type="hidden" name="" value="">

            </form>

                <%--This is a hidden form that is filled by the scripts when user search by LC. Will fill
       the form and will be sent to the LC filter--%>
            <form id="filterLCForm" action="filter_lc_ajaxprocessor.jsp"
                  onsubmit="return submitToLCFilter();" method="post">
                <input type="hidden" name="dataName" value="<%=dataName%>"/>
                <input type="hidden" name="singularLabel" value="<%=singularLabel%>"/>
                <input type="hidden" name="pluralLabel" value="<%=pluralLabel%>"/>
                <input type="hidden" name="dataNamespace" value="<%=dataNamespace%>">
                <input type="hidden" name="key" value="<%=key%>">
                <input type="hidden" name="region" value="<%=region%>">
                <input type="hidden" name="item" value="<%=item%>">
                <input type="hidden" name="breadcrumb" value="<%=breadcrumb%>">

                <input id="searchVal2" type="hidden" name="lc_name" value="">
                <input id="searchVal3" type="hidden" name="lc_state" value="">
                <input id="searchVal4" type="hidden" name="lc_in_out" value="">
                <input id="searchVal5" type="hidden" name="lc_state_in_out" value="">

            </form>

            <form id="tempFilterForm" onKeydown="Javascript: if (event.keyCode==13) submitFilterForm();"
                  onsubmit="return submitFilterForm();" method="post">


                <table id="#_innerTable" style="width:100%">
                    <tr id="buttonRow">
                        <td nowrap="nowrap" style="line-height:25px;padding-right:10px;width:50px;">Filter
                                                                                                    by
                        </td>
                        <td style="width:1px;">
                            <select id="filterByList" onchange="changeVisibility()">
                                <option value="1" selected="selected">LifeCycle</option>
                                <%
                                    GenericUIGenerator gen = new GenericUIGenerator();
                                    ManageGenericArtifactServiceClient client = new ManageGenericArtifactServiceClient(config,session);
                                    OMElement uiconfig = gen.getUIConfiguration(client.getArtifactUIConfiguration(request.getParameter("key")),request,config,session);
                                    String[] keyList = gen.getKeyList(uiconfig, bean.getKeys());

                                %>
                                <%

                                    for (String field : keyList) {
                                        int lastIndex = field.lastIndexOf("_");
                                        String name = field.substring(lastIndex+1);
                                %>

                                <option value="<%=field%>"><%=name%></option>
                                <%

                                    }
                                %>

                            </select>
                        </td>

                        <td style="width:1px;">
                            <input id="id_Search_Val"
                                   type="text" name="search_val" style="width:200px;margin-bottom:10px;display:none;">
                        </td>

                        <td style="width:1px;">
                            <select id="inoutListLC" onchange="changeInOutListLC()">
                                <option value="in">Is</option>
                                <option value="out">Is Not</option>
                            </select>
                        </td>

                        <td style="width:1px;">
                            <select id="lifeCycleList" onchange="changeLC()">
                                <option value="Select">Any</option>
                                <%
                                    boolean once = true;
                                    for (String next:temp) {
                                        if(once){
                                %>
                                <option value="<%=next%>" selected="selected"><%=next%></option>
                                <%
                                    once = false;
                                }else{
                                %>
                                <option value="<%=next%>"><%=next%></option>

                                <%
                                        }
                                    }

                                %>
                            </select>
                        </td>

                        <td style="width:1px;">
                            <select id="inoutListLCState">
                                <option value="in">In</option>
                                <option value="out">Not In</option>
                            </select>
                        </td>

                        <td style="width:1px;">
                            <select id="stateList">
                                    <%--will be filled out as soon as a LC is selected--%>
                            </select>
                        </td>
                        <td>
                            <table style="*width:430px !important;">
                                <tbody>
                                <tr>
                                    <td>
                                        <a class="icon-link" href="#"
                                           style="background-image: url(../search/images/search.gif);"
                                           onclick="submitFilterForm(); return false;" alt="Search"></a>
                                    </td>
                                    <td style="vertical-align:middle;padding-left:10px;padding-right:5px;"> |</td>
                                    <td style="vertical-align:middle;padding-left:10px;padding-right:5px;">
                                        <a class="icon-link" style="background-image:url(../search/images/search-top.png);" href="../generic/filter.jsp?list_region=<%=region%>&list_item=<%=item%>&dataNamespace=<%=dataNamespace%>&dataName=<%=dataName%>&singularLabel=<%=singularLabel%>&pluralLabel=<%=pluralLabel%>&key=<%=key%>&list_breadcrumb=<%=breadcrumb%>"><fmt:message
                                                key="filter.artifact.message"><fmt:param
                                                value="<%=singularLabel%>"/></fmt:message></a>
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                </table>
            </form>
            </p>
            <br>
            <%}%>
            <form id="profilesEditForm">
                <table class="styledLeft" id="customTable">
                    <%if (bean.getArtifacts() == null || bean.getArtifacts().length == 0) {%>
                    <thead>
                    <tr>
                        <%
                            if (filter) {
                        %>
                        <th><fmt:message key="no.artifact.matches.filter"><fmt:param
                                value="<%=singularLabel%>"/></fmt:message></th>
                        <% } else { %>
                        <th><fmt:message key="no.artifacts"><fmt:param value="<%=pluralLabel%>"/></fmt:message></th>
                        <% } %>
                    </tr>
                    </thead>
                    <%
                    } else {
                        int pageNumber;
                        String pageStr = request.getParameter("page");
                        if (pageStr != null) {
                            pageNumber = Integer.parseInt(pageStr);
                        } else {
                            pageNumber = 1;
                        }
                        int itemsPerPage = (int) (RegistryConstants.ITEMS_PER_PAGE * 1.5);
                        int numberOfPages;
                        if (bean.getArtifacts().length % itemsPerPage == 0) {
                            numberOfPages = bean.getArtifacts().length / itemsPerPage;
                        } else {
                            numberOfPages = bean.getArtifacts().length / itemsPerPage + 1;
                        }
                   boolean isBrowseAuthorized = CarbonUIUtil.isUserAuthorized(request,
                                "/permission/admin/manage/resources/browse");
                        boolean isLCAvailable = false;
                        for (int j = (pageNumber - 1) * itemsPerPage;
                             j < pageNumber * itemsPerPage && j < bean.getArtifacts().length; j++) {
                            if (bean.getArtifacts()[j].getLCName() != null && !bean.getArtifacts()[j].getLCName().equals("")) {
                                isLCAvailable = true;
                                break;
                            }
                        }
                    %>
                    <thead>
                    <tr>
                        <%
                            for (String name : bean.getNames()) {
                        %>
                        <th><%=name%>
                        </th>
                        <%
                            }
                        %>
                        <% if (isLCAvailable) {%><th><fmt:message key="lifecycle.info"/></th><%} %>
                        <%
                            if (isBrowseAuthorized) {%>
                        <th><fmt:message key="actions"/></th>
                        <%} %>
                    </tr>
                    </thead>
                    <tbody>
                    <%
                        for (int j = (pageNumber - 1) * itemsPerPage;
                             j < pageNumber * itemsPerPage && j < bean.getArtifacts().length; j++) {
                            ArtifactBean artifact = bean.getArtifacts()[j];

                    %>
                    <tr>
                        <%
                            if (isBrowseAuthorized) {
                                for (int i = 0; i < bean.getNames().length; i++) {
                                    if (bean.getTypes()[i].equals("path")) {
                        %>
                        <td>
                            <a href="../resources/resource.jsp?region=region3&item=resource_browser_menu&path=<%=URLEncoder.encode(artifact.getValuesB()[i], "UTF-8")%>"><%= artifact.getValuesA()[i] != null ? artifact.getValuesA()[i] : "" %>
                            </a></td>
                        <%
                        } else if (bean.getTypes()[i].equals("link")) {
                        %>
                            <td>
                                <a target="_blank" href="<%=artifact.getValuesB()[i]%>"><%= artifact.getValuesA()[i] != null ? artifact.getValuesA()[i] : "" %>
                                </a>
                            </td>
                        <%
                        } else {
                        %>
                        <td><%= artifact.getValuesA()[i] != null ? artifact.getValuesA()[i] : "" %>
                        </td>
                        <%
                                }
                            }
                        %>
                        <% String LCState = "";
                            if (isLCAvailable && artifact.getLCName() != null && !artifact.getLCName().equals("")) {
                                LCState = artifact.getLCName() + " / " + artifact.getLCState();
                            }
                        %>
                        <% if (isLCAvailable) {%><td><%=LCState%></td><%} %>
                        <td><% if (artifact.getCanDelete()) { %><a title="<fmt:message key="delete"/>"
                                                                   onclick="deleteArtifact('<%=artifact.getPath()%>','/','../generic/list.jsp?region=<%=region%>&item=<%=item%><%=queryTrailer%>')"
                                                                   href="#" class="icon-link registryWriteOperation"
                                                                   style="background-image:url(../admin/images/delete.gif);"><fmt:message
                                key="delete"/></a><% } else {%><a class="icon-link registryWriteOperation"
                                                                  style="background-image:url(../generic/images/delete-desable.gif);color:#aaa !important;cursor:default;"><fmt:message
                                key="delete"/></a><% } %>
                            <a onclick="downloadDependencies('<%=artifact.getPath()%>')"  href="#"
                                class="icon-link registryWriteOperation" style="background-image:url(../resources/images/icon-download.jpg);"><fmt:message key="download"/></a>

                        </td>
                        <%
                        } else {
                            for (int i = 0; i < bean.getNames().length; i++) {
                        %>
                        <td><%=artifact.getValuesA()[i]%>
                        </td>
                        <%
                                }
                            }
                        %>
                    </tr>

                    <%
                        }
                    %>
                    </tbody>
                </table>
                <table width="100%" style="text-align:center; padding-top: 10px; margin-bottom: -10px">
                    <carbon:resourcePaginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                                              resourceBundle="org.wso2.carbon.governance.generic.ui.i18n.Resources"
                                              nextKey="next" prevKey="prev"
                                              paginationFunction="loadPagedList({0})"/>
                    <%}%>
                </table>
            </form>
        </div>
    </div>
    <script type="text/javascript">
        alternateTableRows('customTable', 'tableEvenRow', 'tableOddRow');

        function loadPagedList(page) {
            window.location = '<%="../generic/list.jsp?region=" + request.getParameter("region") + "&item=" + request.getParameter("item") + "&dataName=" + request.getParameter("dataName") + "&singularLabel=" + request.getParameter("singularLabel") + "&pluralLabel=" + request.getParameter("pluralLabel") + "&dataNamespace=" + request.getParameter("dataNamespace") + "&key=" + request.getParameter("key") + "&breadcrumb=" + request.getParameter("breadcrumb") + (filter ? "&filter=filter" : "") + "&page=" %>' + page;
        }

        function changeVisibility() {
            var visible = $('filterByList').value;
            resetInputVisibility();
            switch (visible) {
                case "1":
                    $('lifeCycleList').style.display = "";
                    $('inoutListLC').style.display = "";
                    $('stateList').style.display = "";
                    $('inoutListLCState').style.display = "";
                    break;
                default:
                    $('id_Search_Val').style.display = "";
                    break;
            }

        }


        /**
         This method is called at the page load and when the selected LC is changed in the LC select drop-down
         This method load the state list related to the selected LC and fill the state list drop down using them
         uses the lc_state_list_gen_ajaxprocessor.jsp
         */
        function changeLC() {
            var visible = $('lifeCycleList').value;
            var inout = $('inoutListLC').value;
            if(visible == "Select"|| inout=="out" ){
                $('stateList').style.display = "none";
                $('inoutListLCState').style.display = "none";
            }
            else{

                var stateHtml = null;
                new Ajax.Request('../generic/lc_state_list_gen_ajaxprocessor.jsp', {
                    method:'post',
                    parameters: {LCName: visible},

                    onSuccess: function(data) {
                        stateHtml =  eval(data).responseText;
                        $('stateList').innerHTML = stateHtml;
                        $('stateList').style.display = "";
                        $('inoutListLCState').style.display = "";
                    },

                    onFailure: function(transport) {
                        CARBON.showErrorDialog("Failed to load all states of "+visible);
                    }
                });

            }

        }

        function changeInOutListLC() {

            var visible = $('inoutListLC').value;
            if(visible == "out"){
                $('stateList').style.display = "none";
                $('inoutListLCState').style.display = "none";
            }else{
                $('stateList').style.display = "";
                $('inoutListLCState').style.display = "";
            }
        }
        //        change the visibility of the search components to hidden state
        function resetInputVisibility() {
            $('lifeCycleList').style.display = "none";
            $('stateList').style.display = "none";
            $('id_Search_Val').style.display = "none";
            $('inoutListLC').style.display = "none";
            $('inoutListLCState').style.display = "none";

        }

        //        change the sting in to pascal Case e.g overview name -> Overview_Name
        function toPascalCase(str) {
            var arr = str.split(/\s|_/);
            for(var i=0,l=arr.length; i<l; i++) {
                arr[i] = arr[i].substr(0,1).toUpperCase() +
                         (arr[i].length > 1 ? arr[i].substr(1)+"_" : "_");
            }
            return arr.join("");
        }

    </script>
    <script>
        //        call after page loaded
        window.onload=changeLC;
    </script>
</fmt:bundle>
