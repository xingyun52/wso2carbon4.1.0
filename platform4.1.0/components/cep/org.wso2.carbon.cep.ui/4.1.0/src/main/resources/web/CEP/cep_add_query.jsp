<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.*" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.List" %>
<%
    String queryName = request.getParameter("queryName");
    String outputMapping = request.getParameter("outputMapping");

    if (queryName != null) {
        queryName = queryName.trim();
    }

    List metaDataPropertyList = (List) session.getAttribute("outputTupleMetaDataPropertyList");
    List correlationDataPropertyList = (List) session.getAttribute("outputTupleCorrelationDataPropertyList");
    List payloadDataPropertyList = (List) session.getAttribute("outputTuplePayloadDataPropertyList");

    OutputTupleMappingDTO tupleMapping = null;
    if (metaDataPropertyList != null) {
        if (tupleMapping == null) {
            tupleMapping = new OutputTupleMappingDTO();
        }
        OutputTuplePropertyDTO[] properties = new OutputTuplePropertyDTO[metaDataPropertyList.size()];
        for (int i = 0, propertiesLength = properties.length; i < propertiesLength; i++) {
            properties[i]=(OutputTuplePropertyDTO)metaDataPropertyList.get(i);
        }
        tupleMapping.setMetaDataProperties(properties);
    }
    if (correlationDataPropertyList != null) {
        if (tupleMapping == null) {
            tupleMapping = new OutputTupleMappingDTO();
        }
        OutputTuplePropertyDTO[] properties = new OutputTuplePropertyDTO[correlationDataPropertyList.size()];
        for (int i = 0, propertiesLength = properties.length; i < propertiesLength; i++) {
            properties[i]=(OutputTuplePropertyDTO)correlationDataPropertyList.get(i);
        }
        tupleMapping.setCorrelationDataProperties(properties);
    }
    if (payloadDataPropertyList != null) {
        if (tupleMapping == null) {
            tupleMapping = new OutputTupleMappingDTO();
        }
        OutputTuplePropertyDTO[] properties = new OutputTuplePropertyDTO[payloadDataPropertyList.size()];
        for (int i = 0, propertiesLength = properties.length; i < propertiesLength; i++) {
            properties[i]=(OutputTuplePropertyDTO)payloadDataPropertyList.get(i);
        }
        tupleMapping.setPayloadDataProperties(properties);
    }


    List mapPropertyList = (List) session.getAttribute("outputMapPropertyList");

    OutputMapMappingDTO mapMapping = null;
    if (mapPropertyList != null) {
        if (mapMapping == null) {
            mapMapping = new OutputMapMappingDTO();
        }
        OutputMapPropertyDTO[] properties = new OutputMapPropertyDTO[mapPropertyList.size()];
        for (int i = 0, propertiesLength = properties.length; i < propertiesLength; i++) {
            properties[i]=(OutputMapPropertyDTO)mapPropertyList.get(i);
        }
        mapMapping.setMapProperties(properties);
    }

    String outputTopicName = request.getParameter("outputTopic");
    String brokerName = request.getParameter("brokerName");

    if (outputTopicName != null) {
        outputTopicName = outputTopicName.trim();
    }
    if (brokerName != null) {
        brokerName = brokerName.trim();
    }

    String xmlMappingText = request.getParameter("xmlMappingText");
    OutputXMLMappingDTO xmlMapping = null;
    if (xmlMappingText != null && !xmlMappingText.equals("")) {
        xmlMappingText = xmlMappingText.trim();

        xmlMapping = new OutputXMLMappingDTO();
        xmlMapping.setMappingXMLText(xmlMappingText);
    }

    String textMappingText = request.getParameter("textMappingText");
    OutputTextMappingDTO textMapping = null;
    if (textMappingText != null && !textMappingText.equals("")) {
        textMappingText = textMappingText.trim();

        textMapping = new OutputTextMappingDTO();
        textMapping.setMappingText(textMappingText);
    }

    OutputDTO output = null;
    if (outputTopicName != null && !outputTopicName.equals("")) {
        output = new OutputDTO();
        output.setTopic(outputTopicName);
        output.setBrokerName(brokerName);
        if (outputMapping.equals("xml")) {
            output.setOutputTupleMapping(null);
            output.setOutputMapMapping(null);
            output.setOutputXmlMapping(xmlMapping);
            output.setOutputTextMapping(null);
        } else if (outputMapping.equals("text")) {
            output.setOutputTupleMapping(null);
            output.setOutputMapMapping(null);
            output.setOutputXmlMapping(null);
            output.setOutputTextMapping(textMapping);
        } else if (outputMapping.equals("map")) {
            output.setOutputTupleMapping(null);
            output.setOutputMapMapping(mapMapping);
            output.setOutputXmlMapping(null);
            output.setOutputTextMapping(null);
        } else {  //tuple
            output.setOutputTupleMapping(tupleMapping);
            output.setOutputMapMapping(null);
            output.setOutputXmlMapping(null);
            output.setOutputTextMapping(null);
        }
    }

    String querySourceText = request.getParameter("sourceText");
    String expressionType = request.getParameter("type");

    if (querySourceText != null) {
        querySourceText = querySourceText.trim();
    }
    if (expressionType != null) {
        expressionType = expressionType.trim();
    }

    ExpressionDTO expression = new ExpressionDTO();
    expression.setType(expressionType);
    expression.setText(querySourceText);


    String tableIndex = request.getParameter("tableIndex");
    int index = -1;
    try {
        index = Integer.parseInt(tableIndex.trim());
    } catch (NumberFormatException e) {
        index = -1;
    }

    LinkedList<QueryDTO> queries = (LinkedList<QueryDTO>) session.getAttribute("queries");

    if (queries == null) {
        queries = new LinkedList<QueryDTO>();
        session.setAttribute("queries", queries);
    }

    QueryDTO query;
    if (queries.size() > index) {
        query = queries.get(index);
    } else {
        query = new QueryDTO();
        queries.add(query);
    }

    query.setName(queryName.trim());
    query.setExpression(expression);
    query.setOutput(output);

    session.removeAttribute("outputXMLPropertyHashSet");
    session.removeAttribute("outputTextPropertyHashSet");
    session.removeAttribute("outputMapPropertyList");
    session.removeAttribute("outputTuplePayloadDataPropertyList");
    session.removeAttribute("outputTupleCorrelationDataPropertyList");
    session.removeAttribute("outputTupleMetaDataPropertyList");
%>