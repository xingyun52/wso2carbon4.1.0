<%@ page import="org.wso2.carbon.context.CarbonContext" %>
<%@ page import="org.wso2.carbon.context.RegistryType" %>
<%@ page import="org.wso2.carbon.registry.core.Registry" %>
<%@ page import="org.wso2.carbon.registry.core.Resource" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.apache.commons.httpclient.HttpClient" %>
<%@ page import="org.apache.commons.httpclient.HttpStatus" %>
<%@ page import="org.apache.commons.httpclient.methods.GetMethod" %>

<% 
    String endpoint = request.getParameter("endpoint");
    String key = request.getParameter("key");
    if (endpoint == null) {
        endpoint = "";
        key="";
    } else {
        CarbonContext cCtx = CarbonContext.getCurrentContext();
        Registry registry = (Registry) cCtx.getRegistry(RegistryType.SYSTEM_GOVERNANCE);
    //curl -v -H "Authorization: Bearer zGgZkYg6ZhhOH_e1Ebn_jGwfwoka" http://apimanager.example.com:8286/weatherme/1.0.0   	
        Resource resource = registry.get("Key");
        if(resource.getContent() instanceof String){
            key = (String) resource.getContent();
        }else{
            key = new String((byte[]) resource.getContent());
        }
    }
    
%>

<html>
<body>
<h2>Calling the Whether API</h2>
<form action="index.jsp" method="get">
<p>Enter the endpoint URL : <input type="text" name="endpoint" value="<%=endpoint%>"></input> </p>
<p>API Manager Key : <input type="text" name="key" value="<%=key%>" disabled="disabled" ></input> </p>
<input type="submit" value="Invoke"/>
</form>
<%
if (endpoint != null && endpoint.length() > 0) {
    String value = "";
    // Obtain the reference to the registry from the CarbonContext    
    HttpClient client = new HttpClient();
    GetMethod method = new GetMethod(endpoint);
    method.addRequestHeader("Authorization","Bearer " + key);
    int httpStatusCode = client.executeMethod(method);
    if (HttpStatus.SC_ACCEPTED != httpStatusCode) {
        value = method.getResponseBodyAsString();
        value = StringUtils.replaceEach(value, new String[]{"&", "\"", "<", ">"}, new String[]{"&amp;", "&quot;", "&lt;", "&gt;"});
    } else {
        value = "Eror occurred invoking the service " + httpStatusCode;
    }
    %>
    <p><%=value%></p>
    <%
}
    
%>
</body>
</html>
