<%@ page import="org.apache.axiom.om.OMElement" %>
<%@ page import="org.apache.axiom.om.util.AXIOMUtil" %>
<%@ page import="org.wso2.carbon.message.processor.ui.utils.MessageProcessorData" %>

<%
    String configuration = request.getParameter("messageProcessorString");
    String mpName = request.getParameter("mpName");
    String mpProvider = request.getParameter("mpProvider");
    String mpStore = request.getParameter("mpStore");
    configuration = configuration.replaceAll("\\s\\s+|\\n|\\r", ""); // remove the pretty printing from the string
    configuration = configuration.replace("&", "&amp;"); // this is to ensure that url is properly encoded
    OMElement messageProcessorElement = AXIOMUtil.stringToOM(configuration);
    MessageProcessorData processorData = new MessageProcessorData(messageProcessorElement.toString());

    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("PARAMS:");
    for (String key : processorData.getParams().keySet()) {
        String paramName = key;
        String paramValue = processorData.getParams().get(key);
        if (paramValue != "") {
            stringBuffer.append("|" + paramName + "#" + paramValue);
        }
    }
%>


<input type="hidden" id="tableParams" name="tableParams" value="<%=stringBuffer.toString()%>"/>
<input id="Name" name="Name" type="hidden" value="<%=mpName%>"/>
<input name="Provider" id="Provider" type="hidden" value="<%=mpProvider%>"/>
<input name="MessageStore" id="MessageStore" type="hidden" value="<%=mpStore%>"/>