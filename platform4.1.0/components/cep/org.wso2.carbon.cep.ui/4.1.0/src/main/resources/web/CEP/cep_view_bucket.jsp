<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.*" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="java.util.HashMap" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<fmt:bundle basename="org.wso2.carbon.cep.ui.i18n.Resources">
<link type="text/css" href="../dialog/js/jqueryui/tabs/ui.all.css" rel="stylesheet"/>
<link type="text/css" href="../CEP/css/buckets.css" rel="stylesheet"/>
<script type="text/javascript" src="../dialog/js/jqueryui/tabs/jquery-1.2.6.min.js"></script>
<script type="text/javascript"
        src="../dialog/js/jqueryui/tabs/jquery-ui-1.6.custom.min.js"></script>
<script type="text/javascript" src="../dialog/js/jqueryui/tabs/jquery.cookie.js"></script>
<script type="text/javascript">
    jQuery(document).ready(function () {
        initSections('');
        editAreaLoader.init({
                                id:"xmlSource"        // textarea id, syntax:"xml"            // syntax to be uses for highgliting, start_highlight:true        // to display with highlight mode on start-up
                            });
    })
</script>
<script src="../editarea/edit_area_full.js" type="text/javascript"></script>

<script type="text/javascript">


    function loadBucketFromBackend(bucketName) {

        $.ajax({
                   type:"POST",
                   url:"cep_load_bucket_from_bEnd.jsp",
                   data:{'bucketName':bucketName},
                   async:false,
                   success:function (msg) {
//                    alert("Data Saved: " + msg);
                       location.href = 'cep_buckets.jsp?edit=true';
                   }
               });

    }
</script>
<%
    String fromEdit = request.getParameter("edit");
    if (fromEdit != null) {
        if (!fromEdit.equals("true")) {
            session.removeAttribute("editingBucket");
        }
    } else {
        session.removeAttribute("editingBucket");
    }
    session.removeAttribute("inputsHashMap");
    session.removeAttribute("queryHashMap");
    boolean isEditing = false;
    String bucketName = "";
    String description = "";
    String engineProvider = "";
    CEPEngineProviderConfigPropertyDTO[] engineProviderConfigProperties = null;
    try {
        if (session.getAttribute("editingBucket") != null) {
            isEditing = true;
            BucketDTO editingBucket = (BucketDTO) session.getAttribute("editingBucket");
            bucketName = editingBucket.getName();
            description = editingBucket.getDescription();
            engineProvider = editingBucket.getEngineProvider();
            engineProviderConfigProperties = editingBucket.getEngineProviderConfigProperty();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }


    InputDTO[] inputs = null;
    if (session.getAttribute("editingBucket") != null) {
        BucketDTO editingBucket = (BucketDTO) session.getAttribute("editingBucket");
        if (session.getAttribute("inputsHashMap") == null) {
            inputs = editingBucket.getInputs();
            HashMap<Integer, InputDTO> inputsHashMap = new HashMap<Integer, InputDTO>();
            int inputIndex = 0;
            if (inputs != null) {
                for (InputDTO input : inputs) {
                    inputsHashMap.put(inputIndex, input);
                    inputIndex++;
                }
            }
            session.setAttribute("inputsHashMap", inputsHashMap);
        } else {
            HashMap<Integer, InputDTO> inputsHashMap = (HashMap<Integer, InputDTO>) session.getAttribute("inputsHashMap");
            inputs = new InputDTO[inputsHashMap.size()];
            for (int key : inputsHashMap.keySet()) {
                inputs[key] = inputsHashMap.get(key);
            }
        }
    } else {
        if (session.getAttribute("inputsHashMap") != null) {
            HashMap<Integer, InputDTO> inputsHashMap = (HashMap<Integer, InputDTO>) session.getAttribute("inputsHashMap");
            inputs = new InputDTO[inputsHashMap.size()];
            for (int key : inputsHashMap.keySet()) {
                inputs[key] = inputsHashMap.get(key);
            }
        }
    }


    QueryDTO[] queries = null;
    HashMap<Integer, QueryDTO> queryHashMap = null;
    if (session.getAttribute("editingBucket") != null) {
        BucketDTO editingBucket = (BucketDTO) session.getAttribute("editingBucket");
        if (session.getAttribute("queryHashMap") == null) {
            queries = editingBucket.getQueries();
            queryHashMap = new HashMap<Integer, QueryDTO>();
            int queryIndex = 0;
            if (queries != null) {
                for (QueryDTO query : queries) {
                    queryHashMap.put(queryIndex, query);
                    queryIndex++;
                }
            }
            session.setAttribute("queryHashMap", queryHashMap);
        } else {
            queryHashMap = (HashMap<Integer, QueryDTO>) session.getAttribute("queryHashMap");
            queries = new QueryDTO[queryHashMap.size()];
            for (int key : queryHashMap.keySet()) {
                queries[key] = queryHashMap.get(key);
            }
        }
    } else {
        if (session.getAttribute("queryHashMap") != null) {
            queryHashMap = (HashMap<Integer, QueryDTO>) session.getAttribute("queryHashMap");
            queries = new QueryDTO[queryHashMap.size()];
            for (int key : queryHashMap.keySet()) {
                queries[key] = queryHashMap.get(key);
            }
        }
    }

    boolean isAuthorizedForEditingBuckets =
            CarbonUIUtil.isUserAuthorized(request, "/permission/admin/manage/cep/editBucket");


    final int NONE = -1;
    final int XML = 0;
    final int TUPLE = 1;
    final int MAP = 2;
    int inputMapping = NONE;


%>

<div id="middle">
<h2><img src="images/cep-buckets.gif" alt=""/> <fmt:message key="bucket.view"/></h2>
        <%
    if (isAuthorizedForEditingBuckets) {
%>
<table style="width:100%; margin-bottom:20px;">
    <tbody>
    <tr>
        <td><a class="icon-link" style="background-image:url(../admin/images/edit.gif)"
               onclick="loadBucketFromBackend('<%=bucketName%>')"><font color="#4682b4">
            <fmt:message key="bucket.edit"/></font></a></td>
    </tr>
    </tbody>
</table>

        <%
    }
%>

<div id="workArea">
<table class="styledLeft noBorders spacer-bot" style="width:100%">
<thead>
        <tr>
    <th><fmt:message key="bucket.info"/></th>
</tr>
</thead>
<tbody>
<tr>
<td class="formRaw">
<table class="normal" align="" style="width:100%">
    <tbody>
    <tr>

        <td class="leftCol-small labelField" for="bucketName"><fmt:message
                key="bucket.name"/><span
                class="required">*</span></td>
            <td><input id="bucketName" type="text" readonly="true" value="<%=bucketName%>"/>
            </td>
        </tr>
        <tr>
            <td class="leftCol-med labelField"><fmt:message
                    key="bucket.description"/></td>
            <td>
                <textArea class="expandedTextarea" id="bucketDescription"
                          cols="60" readonly="true"><%=description%>
                </textArea>
            </td>
        </tr>
        <tr>
            <td class="leftCol-med labelField"><fmt:message key="cep.runtime"/></td>
            <td><input id="engineProvider" type="text" readonly="true"
                       value="<%=engineProvider%>"/></td>
        </tr>
        <%
            if (engineProviderConfigProperties != null && engineProviderConfigProperties.length > 0) {
        %>
        <tr>
            <td class="heading_A"><fmt:message key="engine.provider.configuration"/></td>
        </tr>
        <%
            for (int k = 0; k < engineProviderConfigProperties.length; k++) {
        %>
        <tr name="providerConfig<%= "::"+engineProvider%>">
            <td class="leftCol-small labelField" for="providerConfig<%="::"+engineProvider+"::"+engineProviderConfigProperties[k].getNames()%>">

                <%
                    if (engineProviderConfigProperties[k].getNames() != null && "siddhi.persistence.snapshot.time.interval.minutes".equals(engineProviderConfigProperties[k].getNames())) {
                %>
                <fmt:message
                        key="siddhi.persistence.snapshot.time.interval.minutes"/>
                <%
                } else if (engineProviderConfigProperties[k].getNames() != null && "siddhi.enable.distributed.processing".equals(engineProviderConfigProperties[k].getNames())) {
                %>
                <fmt:message
                        key="siddhi.enable.distributed.processing"/>
                <%
                } else {
                %>
                <%=engineProviderConfigProperties[k].getNames()%>
                <%
                    }
                %>
            </td>
            <td>
                <input id="providerConfig<%="::"+engineProvider+"::"+engineProviderConfigProperties[k].getNames()%>"
                       type="text" readonly="true"
                       value="<%=engineProviderConfigProperties[k].getValues()%>"/>
            </td>
        </tr>
        <%
                }

            }
        %>
    </tbody>
    </table>

<div class="sectionSeperator togglebleTitle"><fmt:message key="inputs"/></div>
<div class="sectionSub">
<table class="normal" style="width:100%">

        <%
            if (inputs != null) {
                for (InputDTO input : inputs) {
                    XpathDefinitionDTO[] xpathDefinitions = null;
                    InputXMLPropertyDTO[] xmlProperties = null;
                    InputTuplePropertyDTO[] tupleProperties = null;
                    InputMapPropertyDTO[] mapProperties = null;
                    String stream = "";
                    String queryEventType = "";
                    if (input.getInputXMLMappingDTO() != null) {
                        inputMapping = XML;
                        xpathDefinitions = input.getInputXMLMappingDTO().getXpathDefinition();
                        xmlProperties = input.getInputXMLMappingDTO().getProperties();
                        stream = input.getInputXMLMappingDTO().getStream();
                        queryEventType = input.getInputXMLMappingDTO().getQueryEventType();
                    }
                    if (input.getInputTupleMappingDTO() != null) {
                        inputMapping = TUPLE;
                        tupleProperties = input.getInputTupleMappingDTO().getProperties();
                        stream = input.getInputTupleMappingDTO().getStream();
                        queryEventType = input.getInputTupleMappingDTO().getQueryEventType();

                    }
                    if (input.getInputMapMappingDTO() != null) {
                        inputMapping = MAP;
                        mapProperties = input.getInputMapMappingDTO().getProperties();
                        stream = input.getInputMapMappingDTO().getStream();
                        queryEventType = input.getInputMapMappingDTO().getQueryEventType();

                    }

        %>
        <tr>
            <td>
                <div class="sectionSeperator togglebleTitle"><%=input.getTopic()%>&nbsp;<fmt:message key="input"/></div>
                <div class="sectionSub">
    <table class="normal" style="width:100%">
        <tr>
            <td class="leftCol-small labelField"><fmt:message key="input.topic"/></td>
            <td><input type="text" readonly="true" id="inputTopic"
                       value="<%=input.getTopic()%>"></td>
        </tr>
        <tr>
            <td class="leftCol-small labelField"><fmt:message key="broker.name"/></td>
            <td><input type="text" readonly="true" id="inputBrokerName"
                       value="<%=input.getBrokerName()%>"></td>
        </tr>

        <tr>
            <td colspan="2">
                <div class="heading_A"><fmt:message
                        key="input.mapping.stream"/></div>
            </td>
        </tr>
        <tr>
            <td class="leftCol-med labelField"><fmt:message key="stream"/></td>
            <td><input type="text" readonly="true" id="mappingStream"
                       value="<%=stream%>">
            </td>
        </tr>
        <tr>
            <td class="leftCol-small labelField"><fmt:message key="query.event.type"/></td>
            <td><input type="text" readonly="true" id="eventClassName"
                       value="<%=queryEventType%>">
            </td>
        </tr>
        <%if (inputMapping == XML) {%>
        <tr>

            <td colspan="2">
                <div class="heading_B"><fmt:message key="xml.mapping"/></div>
            </td>

        </tr>
        <tr>
            <td colspan="2"><fmt:message key="xpath_definition"/></td>
        </tr>
        <tr>
            <td colspan="2">
                <table style="width:100%" class="styledLeft">
                    <thead>
                    <th class="leftCol-small"><fmt:message key="prefix"/></th>
                    <th class="leftCol-small"><fmt:message key="namespace"/></th>
                    </thead>
                    <tbody>
                    <% if (xpathDefinitions != null) {
                        for (XpathDefinitionDTO xpathDefinition : xpathDefinitions) {
                    %>
                    <tr>
                        <td><%=xpathDefinition.getPrefix()%>
                        </td>
                        <td><%=xpathDefinition.getNamespace()%>
                        </td>
                    </tr>
                    <%
                            }
                        }
                    %>
                    </tbody>
                </table>
            </td>
        </tr>
        <tr>
            <td colspan="2"><fmt:message
                    key="property"/></td>
        </tr>
        <tr>
            <td colspan="2">
                <table class="styledLeft" style="width:100%">
                    <thead>
                    <th class="leftCol-small"><fmt:message
                            key="property.name"/></th>
                    <th class="leftCol-small"><fmt:message
                            key="property.xpath"/></th>
                    <th class="leftCol-small"><fmt:message
                            key="property.type"/></th>
                    </thead>
                    <tbody>
                    <% if (xmlProperties != null) {
                        for (InputXMLPropertyDTO property : xmlProperties) {
                    %>
                    <tr>
                        <td><%=property.getName()%>
                        </td>
                        <td><%=property.getXpath()%>
                        </td>
                        <td><%=property.getType()%>
                        </td>
                    </tr>
                    <%
                            }
                        }
                    %>
                    </tbody>
                </table>
            </td>
        </tr>

                        <%
                            }
                            if (inputMapping == TUPLE) {
                        %>

                        <tr>
                            <td colspan="2">
                                <div class="heading_B"><fmt:message key="tuple.mapping"/></div>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2"><fmt:message key="property"/></td>
                        </tr>
                        <tr>
                            <td colspan="2">
                                <table class="styledLeft" style="width:100%">
                                    <thead>
                    <th class="leftCol-small"><fmt:message
                            key="property.name"/></th>
                    <th class="leftCol-small"><fmt:message
                            key="property.input.name"/></th>
                    <th class="leftCol-small"><fmt:message
                            key="property.input.data.type"/></th>
                    <th class="leftCol-small"><fmt:message
                            key="property.type"/></th>
                    </thead>
                    <tbody>
                    <% if (tupleProperties != null) {
                        for (InputTuplePropertyDTO property : tupleProperties) {
                    %>
                    <tr>
                        <td><%=property.getName()%>
                        </td>
                        <td><%=property.getInputName()%>
                        </td>
                        <td><%=property.getInputDataType()%>
                        </td>
                        <td><%=property.getType()%>
                        </td>
                    </tr>
                    <%
                            }
                        }
                    %>
                    </tbody>

                                </table>
                            </td>
                        </tr>
                        <%
                            }
                            if (inputMapping == MAP) {
                        %>

                        <tr>
                            <td colspan="2">
                                <div class="heading_B"><fmt:message key="map.mapping"/></div>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2"><fmt:message key="property"/></td>
                        </tr>
                        <tr>
                            <td colspan="2">
                                <table class="styledLeft" style="width:100%">
                                    <thead>
                    <th class="leftCol-small"><fmt:message
                            key="property.name"/></th>
                    <th class="leftCol-small"><fmt:message
                            key="property.input.name"/></th>
                    <th class="leftCol-small"><fmt:message
                                            key="property.type"/></th>
                                    </thead>
                                    <tbody>
                                    <% if (mapProperties != null) {
                                        for (InputMapPropertyDTO property : mapProperties) {
                                    %>
                                    <tr>
                                        <td><%=property.getName()%>
                                        </td>
                                        <td><%=property.getInputName()%>
                                        </td>
                                        <td><%=property.getType()%>
                                        </td>
                                    </tr>
                                    <%
                                            }
                                        }
                                    %>
                                    </tbody>

                                </table>
                            </td>
                        </tr>
                        <%}%>
                    </table>
                </div>
            </td>
        </tr>
        <%
                }
            }
        %>
    </table>
</div>
<div class="sectionSeperator togglebleTitle"><fmt:message key="queries"/></div>
<div class="sectionSub">
<table class="normal" style="width:100%">

        <%
    if (queryHashMap != null) {
        int queryIndex = 0;
        for (QueryDTO query : queryHashMap.values()) {
            ExpressionDTO expression = null;
            OutputDTO output = null;
            OutputXMLMappingDTO xmlMapping = null;
            OutputTextMappingDTO textMapping = null;
            OutputTupleMappingDTO tupleMapping = null;
            OutputMapMappingDTO mapMapping = null;
            expression = query.getExpression();
            output = query.getOutput();
            if (output != null) {
                xmlMapping = output.getOutputXmlMapping();
                textMapping = output.getOutputTextMapping();
                tupleMapping = output.getOutputTupleMapping();
                mapMapping = output.getOutputMapMapping();
            }
%>
<tr>
<td>

<div class="sectionSeperator togglebleTitle"><%=query.getName()%>&nbsp;<fmt:message key="query"/></div>
<div class="sectionSub">
<table class="normal" style="width:100%" >

<tr>

    <td class="leftCol-small labelField"><fmt:message key="query.name"/></td>
    <td><input class="longInput" type="text" id="queryName" readonly="true"
               value="<%=query.getName()%>"></td>

</tr>

<tr id="expressionInlined">
    <td class="leftCol-small labelField"><fmt:message key="query.expression"/></td>
    <td>
        <textarea id="querySource" name="querySource" class="expandedTextarea" cols="70"
                  readonly="true"><%=expression.getText()%>
        </textarea>
    </td>
</tr>

<% if (output != null && output.getTopic().length() > 0) { %>


<tr>
    <th colspan="2">
        <div class="heading_A"><fmt:message key="output"/></div>
    </th>
</tr>

<tr>
    <td class="leftCol-small labelField"><fmt:message key="output.topic"/></td>
    <td><input type="text" id="newTopic" readonly="true" value="<%=output.getTopic()%>">
    </td>
</tr>
<tr>
    <td class="leftCol-small labelField"><fmt:message key="broker.name"/></td>
    <td><input type="text" id="outputBrokerName" readonly="true"
               value="<%=output.getBrokerName()%>">
    </td>
</tr>


<%
    if (xmlMapping != null) {
%>

<tr>
    <td colspan="2">
        <div class="heading_B"><fmt:message key="xml.mapping"/></div>
    </td>
</tr>
<tr>
    <td class="leftCol-med labelField"><fmt:message key="xml.mapping.text"/></td>
    <td>
        <textarea class="expandedTextarea" id="xmlSource"
                  name="xmlSource"
                  readonly="true"><%=xmlMapping.getMappingXMLText()%>
        </textarea>
    </td>
</tr>
<%
    }
    if (textMapping != null) {
%>

<tr>
    <td colspan="2">
        <div class="heading_B"><fmt:message key="text.mapping"/></div>
    </td>
</tr>
<tr>
    <td class="leftCol-med labelField"><fmt:message key="text.mapping.text"/></td>
    <td>
        <textarea class="expandedTextarea" id="textSource"
                  name="textSource"
                  readonly="true"><%=textMapping.getMappingText()%>
        </textarea>
    </td>
</tr>
<%
    }
    if (tupleMapping != null) {
%>

<tr>
    <td colspan="2">
        <div class="heading_B"><fmt:message key="tuple.mapping"/></div>
    </td>
</tr>
<tr>
    <td colspan="2">
        <fmt:message key="property.data.type.meta"/>
    </td>
</tr>
<tr>
    <td colspan="2">
        <table class="styledLeft" style="width:100%">
            <thead>
            <th class="leftCol-small"><fmt:message key="property.name"/></th>
            <th class="leftCol-small"><fmt:message key="property.value.of"/></th>
            <th class="leftCol-small"><fmt:message key="property.type"/></th>
            </thead>
            <%
                if (tupleMapping != null && tupleMapping.getMetaDataProperties() != null) {
            %>
            <tbody>
            <%
                for (OutputTuplePropertyDTO property : tupleMapping.getMetaDataProperties()) {
            %>
            <tr>
                <td><%=property.getName()%> </td>
                <td><%=property.getValueOf()%> </td>
                <td><%=property.getType()%> </td>
            </tr>
            <%
                }
            %>
            </tbody>
            <%
                }
            %>
        </table>
    </td>
</tr>
<tr>
    <td colspan="2">
        <fmt:message key="property.data.type.correlation"/>
    </td>
</tr>
<tr>
    <td colspan="2">
        <table class="styledLeft" style="width:100%">
            <thead>
            <th class="leftCol-small"><fmt:message key="property.name"/></th>
            <th class="leftCol-small"><fmt:message key="property.value.of"/></th>
            <th class="leftCol-small"><fmt:message key="property.type"/></th>
            </thead>
            <%
                if (tupleMapping != null && tupleMapping.getCorrelationDataProperties() != null) {
            %>
            <tbody>
            <%
                for (OutputTuplePropertyDTO property : tupleMapping.getCorrelationDataProperties()) {
            %>
            <tr>
                <td><%=property.getName()%> </td>
                <td><%=property.getValueOf()%> </td>
                <td><%=property.getType()%> </td>
            </tr>
            <%
                }
            %>
            </tbody>
            <%
                }
            %>
        </table>
    </td>
</tr>
<tr>
    <td colspan="2">
        <fmt:message key="property.data.type.payload"/>
    </td>
</tr>
<tr>
    <td colspan="2">
        <table class="styledLeft" style="width:100%">
            <thead>
            <th class="leftCol-small"><fmt:message key="property.name"/></th>
            <th class="leftCol-small"><fmt:message key="property.value.of"/></th>
            <th class="leftCol-small"><fmt:message key="property.type"/></th>
            </thead>
            <%
                if (tupleMapping != null && tupleMapping.getPayloadDataProperties() != null) {
            %>
            <tbody>
            <%
                for (OutputTuplePropertyDTO property : tupleMapping.getPayloadDataProperties()) {
            %>
            <tr>
                <td><%=property.getName()%> </td>
                <td><%=property.getValueOf()%> </td>
                <td><%=property.getType()%> </td>
            </tr>
            <%
                }
            %>
            </tbody>
            <%
                }
            %>
        </table>
    </td>
</tr>
<%
    }
    if (mapMapping != null) {
%>

<tr>
    <td colspan="2">
        <div class="heading_B"><fmt:message key="map.mapping"/></div>
    </td>
</tr>
<tr>
    <td colspan="2">
        <table class="styledLeft" style="width:100%">
            <thead>
            <th class="leftCol-small"><fmt:message key="property.name"/></th>
            <th class="leftCol-small"><fmt:message key="property.value.of"/></th>
            </thead>
            <%
                if (mapMapping != null && mapMapping.getMapProperties() != null) {
            %>
            <tbody>
            <%
                for (OutputMapPropertyDTO property : mapMapping.getMapProperties()) {
            %>
            <tr>
                <td><%=property.getName()%></td>
                <td><%=property.getValueOf()%></td>
            </tr>
            <%
                }
            %>
            </tbody>
            <%
                }
            %>
        </table>
    </td>
</tr>
<%
        }
    }
%>
</table>
</div>
        <%

            queryIndex++;
        }
    }
%>
  </td>
</tr>
</table>
</div>


    <%--<table style="width:100%">--%>
    <%--<tbody>--%>
    <%--<tr>--%>
    <%--<td class="buttonRow">--%>
    <%--<input type="button" onclick="javascript:location.href='cep_deployed_buckets.jsp'"--%>
    <%--value=" &lt;Back" class="button">--%>
    <%--</td>--%>
    <%--</tr>--%>
    <%--</tbody>--%>

    <%--</table>--%>

<div class="buttonRow">
    <input class="button" type="button" value=" &lt;Back"
           onclick="javascript:location.href='cep_deployed_buckets.jsp'"/>
</div>

</fmt:bundle>
