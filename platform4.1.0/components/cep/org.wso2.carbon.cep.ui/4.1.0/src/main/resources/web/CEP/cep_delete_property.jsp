<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.InputMapPropertyDTO" %>
<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.InputTuplePropertyDTO" %>
<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.InputXMLPropertyDTO" %>
<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.XpathDefinitionDTO" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.List" %>
<%
    String propertyToDelete = request.getParameter("property");
    String type = request.getParameter("type");
    String format = request.getParameter("format");

    if (type.equals("input")) {
        if (format.equals("xml")) {
            HashSet propertySet = (HashSet) session.getAttribute("inputXMLPropertyHashSet");
            InputXMLPropertyDTO[] properties = new InputXMLPropertyDTO[propertySet.size()];
            propertySet.toArray(properties);
            for (InputXMLPropertyDTO property : properties) {
                if (property.getName().equals(propertyToDelete)) {
                    propertySet.remove(property);
                    break;
                }
            }
        } else  if (format.equals("map")) {
            List propertyList = (List) session.getAttribute("inputMapPropertyList");
            InputMapPropertyDTO[] properties = new InputMapPropertyDTO[propertyList.size()];
            propertyList.toArray(properties);
            for (InputMapPropertyDTO property : properties) {
                if (property.getName().equals(propertyToDelete)) {
                    propertyList.remove(property);
                    break;
                }
            }
        }else { //kind of tuple
            List propertyList = (List) session.getAttribute("inputTuplePropertyList");
            InputTuplePropertyDTO[] properties = new InputTuplePropertyDTO[propertyList.size()];
            propertyList.toArray(properties);
            for (InputTuplePropertyDTO property : properties) {
                if (property.getName().equals(propertyToDelete)) {
                    propertyList.remove(property);
                    break;
                }
            }
        }


    } else if (type.equals("output")) {
        if((format.equals("map"))){
            List list = (List) session.getAttribute("outputMapPropertyList");
            list.remove(propertyToDelete);
        } else { //tuple Meta, Correlation,or Payload
            List list = (List) session.getAttribute("outputTuple" + format + "DataPropertyList");
            list.remove(propertyToDelete);
        }
    } else if (type.equals("ns")) {
        HashSet nsPrefixHashSet = (HashSet) session.getAttribute("nsPrefixHashSet");
        XpathDefinitionDTO[] xpathDefinitions = new XpathDefinitionDTO[nsPrefixHashSet.size()];
        nsPrefixHashSet.toArray(xpathDefinitions);
        for (XpathDefinitionDTO xpathDefinition : xpathDefinitions) {
            if (xpathDefinition.getPrefix().equals(propertyToDelete)) {
                nsPrefixHashSet.remove(xpathDefinition);
                break;
            }
        }
    }


%>