<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.cartridge.mgt.ui.CartridgeAdminClient" %>
<%@ page import="org.wso2.carbon.cartridge.mgt.ui.CartridgeConstans" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.wso2.carbon.adc.mgt.dto.xsd.Cartridge" %>
<%@ page import="org.wso2.carbon.adc.mgt.stub.ApplicationManagementServiceADCExceptionException" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.util.ArrayList" %>

<jsp:include page="../dialog/display_messages.jsp"/>


<%
    response.setHeader("Cache-Control", "no-cache");

    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
          (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CartridgeAdminClient client;
    Cartridge[] cartridges;
    try{
        client= new CartridgeAdminClient(cookie, backendServerURL, configContext,request.getLocale());

        cartridges = client.getCartridgesList();
    }catch (Exception e) {
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
        %>
            <jsp:include page="../admin/error.jsp"/>
        <%
        return;
    }

    ArrayList<String> cartridgeTypes = new ArrayList<String>();
%>

<fmt:bundle basename="org.wso2.carbon.cartridge.mgt.ui.i18n.Resources">
<carbon:breadcrumb
    label="main.header"
    resourceBundle="org.wso2.carbon.cartridge.mgt.ui.i18n.Resources"
    topPage="true"
    request="<%=request%>"/>
<div id="middle">
    <h2><fmt:message key="main.header"/></h2>

    <div id="workArea">
    <h3><fmt:message key="subscribed.cartridges"/></h3>
        <table class="styledLeft" id="cartridgesList" width="100%">
            <tbody>
                <%
                boolean noSubscribes = true;
                boolean firstTime = true;
                for (Cartridge cartridge: cartridges){
                    if(!cartridgeTypes.contains(cartridge.getCartridgeType())) {
                        //get all the types of cartridges to array list
                        cartridgeTypes.add(cartridge.getCartridgeType());
                    }
                    if(!"NOT-SUBSCRIBED".equalsIgnoreCase(cartridge.getStatus().trim())){
                        //only when it is a subscribed cartridge
                        String cartridgeRepoUrl = cartridge.getRepoURL();
                        if(cartridgeRepoUrl == null){
                            cartridgeRepoUrl = "";
                        }
                        String url="";
                        if(cartridge.getHostName()!= null){
                        	String provider = cartridge.getProvider();
                        	String hostName = cartridge.getHostName();
                               if(CartridgeConstans.DATA_PROVIDER.equalsIgnoreCase(provider)) {
                                	Cartridge dataCartridge = null;
                                   	try{
                               	    	dataCartridge = client.getInfo(cartridge.getCartridgeName());
	                               	} catch(ApplicationManagementServiceADCExceptionException ignore){
	                               	}
                               		if(dataCartridge.getIp() != null){
                                   		url = "https://" + dataCartridge.getIp() +"/phpmyadmin";
                                   	} else{
                                   		url = "";
                                   	}
                               } else if(CartridgeConstans.WSO2_PROVIDER.equalsIgnoreCase(provider)){
                               		url = "https://" + hostName +":8243";
                               } else {
                               		url = "http://" + hostName +":8280";
                               }
                           
                        }
                        if(firstTime){   firstTime = false; noSubscribes = false;
                        //first time it will print table titles%>
                        <thead>
                            <tr>
                                <th width="10%"><fmt:message key="type"/></th>
                                <th width="10%"><fmt:message key="status"/></th>
                                <th width="10%"><fmt:message key="instance.count"/></th>
                                <th width="10%"><fmt:message key="alias"/></th>
                                <th width="25%"><fmt:message key="url"/></th>
                                <th width="25%"><fmt:message key="repo.url"/></th>
                                <th width="15%"><fmt:message key="action"/></th>
                            </tr>
                        </thead>
                        <%
                        }
                        %>
                        <tr>
                            <td><%=cartridge.getCartridgeType() %></td>
                            <td><%=cartridge.getStatus() %></td>
                            <td><%=cartridge.getActiveInstances() %></td>
                            <td><%=cartridge.getCartridgeName() %></td>
                            <td><%=url %></td>
                            <td><%=cartridgeRepoUrl%></td>
                            <td><a href="./call_unsubscribe.jsp?cartridge_name=<%=cartridge.getCartridgeName() %>" style="background-image:url(images/unsubscribe.png);" class="icon-link">
                                <fmt:message key="unsubscribe"/></a></td>
                        </tr>
                    <%}
                } %>
                <tr></tr>
            </tbody>
        </table>
               <%if(noSubscribes){  %>
                    <fmt:message key="no.subscribed.cartridges"/>
               <%}%>
    <p>&nbsp;</p>
    <p>&nbsp;</p>
        <h3><fmt:message key="subscribe.new.cartridge"/></h3>
        <table class="styledLeft" id="newCartridgeTable">
            <tbody>
                <thead>
                    <tr>
                        <th width="50%"><fmt:message key="type"/></th>
                        <th width="50%"><fmt:message key="action"/></th>
                    </tr>
                </thead>
                    <%for (String cartridgeType: cartridgeTypes){ %>
                <tr>
                        <td><%=cartridgeType %></td>
                        <td><a href="./subscribe.jsp?cartridgeType=<%=cartridgeType%>" style="background-image:url(images/subscribe.gif);" class="icon-link">
                            <fmt:message key="subscribe"/></a></td>
                </tr><%} %>
                <tr></tr>
            </tbody>
        </table>

        <p>&nbsp;</p>
    </div>

</div>
<script type="text/javascript">
    alternateTableRows('cartridgesList', 'tableEvenRow', 'tableOddRow');
    alternateTableRows('newCartridgeTable', 'tableEvenRow', 'tableOddRow');
</script>
</fmt:bundle>
