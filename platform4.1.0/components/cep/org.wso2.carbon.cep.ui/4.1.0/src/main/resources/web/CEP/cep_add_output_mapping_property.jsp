<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.OutputMapPropertyDTO" %>
<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.OutputTuplePropertyDTO" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%

    String type = request.getParameter("format");
    String propName = request.getParameter("propName");
    if (propName != null) {
        propName = propName.trim();
    }
    if (type.equals("map")){
        String valueOf = request.getParameter("valueOf");
        List list = (List) session.getAttribute("outputMapPropertyList");
        if (list == null) {
            list = new ArrayList();
            session.setAttribute("outputMapPropertyList", list);
        }
        OutputMapPropertyDTO propertyDTO = new OutputMapPropertyDTO();
        propertyDTO.setName(propName);
        propertyDTO.setValueOf(valueOf);
        list.add(propertyDTO);
    }else {//tuple
        String dataType = request.getParameter("dataType");
        String valueOf = request.getParameter("valueOf");
        String tuplePropType = request.getParameter("type");

        if (dataType != null) {
            dataType = dataType.trim();
        }
        List list = (List) session.getAttribute("outputTuple" + dataType + "DataPropertyList");
        if (list == null) {
            list = new ArrayList();
            session.setAttribute("outputTuple" + dataType + "DataPropertyList", list);
        }
        OutputTuplePropertyDTO propertyDTO = new OutputTuplePropertyDTO();
        propertyDTO.setName(propName);
        propertyDTO.setType(tuplePropType);
        propertyDTO.setValueOf(valueOf);
        list.add(propertyDTO);

    }
%>