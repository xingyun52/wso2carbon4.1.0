<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.InputMapPropertyDTO" %>
<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.InputTuplePropertyDTO" %>
<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.InputXMLPropertyDTO" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.List" %>
<%
    String format = request.getParameter("format");
    String propName = request.getParameter("propName");
    String propType = request.getParameter("propType");
    String propValue1 = request.getParameter("propValue1");
    String propValue2 = request.getParameter("propValue2");

    if (propName != null) {
        propName = propName.trim();
    }

    if (propType != null) {
        propType = propType.trim();
    }

    if (propValue1 != null) {
        propValue1 = propValue1.trim();
    }

    if (propValue2 != null) {
        propValue2 = propValue2.trim();
    }

    if (format.equals("xml")) {

        InputXMLPropertyDTO property = new InputXMLPropertyDTO();
        property.setName(propName);
        property.setType(propType);
        property.setXpath(propValue1); //is Xpath

        HashSet propertyHashSet = (HashSet) session.getAttribute("inputXMLPropertyHashSet");
        if (propertyHashSet == null) {
            propertyHashSet = new HashSet();
            propertyHashSet.add(property);
            session.setAttribute("inputXMLPropertyHashSet", propertyHashSet);
        } else {
            propertyHashSet.add(property);
        }

    } else  if (format.equals("map")) {

        InputMapPropertyDTO property = new InputMapPropertyDTO();
        property.setName(propName);
        property.setInputName(propValue1);
        property.setType(propType);

        List propertyList = (List) session.getAttribute("inputMapPropertyList");
        if (propertyList == null) {
            propertyList = new ArrayList();
            propertyList.add(property);
            session.setAttribute("inputMapPropertyList", propertyList);
        } else {
            propertyList.add(property);
        }

    } else { //tuple

        InputTuplePropertyDTO property = new InputTuplePropertyDTO();
        property.setName(propName);
        property.setInputName(propValue1);
        property.setInputDataType(propValue2);
        property.setType(propType);

        List propertyList = (List) session.getAttribute("inputTuplePropertyList");
        if (propertyList == null) {
            propertyList = new ArrayList();
            propertyList.add(property);
            session.setAttribute("inputTuplePropertyList", propertyList);
        } else {
            propertyList.add(property);
        }
    }


%>