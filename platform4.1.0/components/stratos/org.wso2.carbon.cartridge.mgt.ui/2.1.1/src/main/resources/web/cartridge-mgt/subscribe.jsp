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
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.ResourceBundle" %>
<jsp:include page="../dialog/display_messages.jsp"/>


<%
    response.setHeader("Cache-Control", "no-cache");
    String cartridgeType = request.getParameter("cartridgeType");
    String cartridgeProvider = request.getParameter("cartridgeType");
    int maximum = 5;

    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
          (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CartridgeAdminClient client;

    try{
        client = new CartridgeAdminClient(cookie, backendServerURL, configContext,request.getLocale());
        maximum = client.getCartridgeClusterMaximumLimit();
    }catch (Exception e) {
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
        %>
            <jsp:include page="../admin/error.jsp"/>
        <%
        return;
    }

%>
<fmt:bundle basename="org.wso2.carbon.cartridge.mgt.ui.i18n.Resources">
<carbon:breadcrumb
    label="main.header"
    resourceBundle="org.wso2.carbon.cartridge.mgt.ui.i18n.Resources"
    topPage="true"
    request="<%=request%>"/>
<div id="middle">
<%if(cartridgeType != null) {%>
    <h2><fmt:message key="subscribe.to"/> <%=cartridgeType %></h2>
<%} else {%>
	<h2><fmt:message key="subscribe.new.cartridge"/></h2>
    <p>&nbsp;</p>
<%} %>
<div id="workArea">
    <form action="call_subscribe.jsp" name="subscribeToCartridge">
        <table id="subscribeToCartridgeTbl" width="100%" class="styledLeft">
            <tbody>
                <thead>
                </thead>
                <tr >
                    <td width="50%" class="formRow"><label>Cartridge Type</label><font color="red">*</font></td>
                    <td width="50%" class="formRow">
                    <%if(cartridgeType != null) {%>
                        <label><%=cartridgeType%></label></td>
                        <input name="cartridge_type" type="hidden" value="<%=cartridgeType%>"/>
                    <%} else {%>
                        <input name="cartridge_type" type="text" /></td>
                    <%} %>
                </tr>
                <tr>
                    <td class="formRow"><label>Name<font color="red">*</font></label></td>
                    <td class="formRow"><input name="cartridge_name" type="text"/></td>
                </tr>
                <%if(cartridgeType == null || !cartridgeType.equalsIgnoreCase("mysql")) {%>
                    <tr>
                        <td class="formRow"><label>Select minimum number of instances</label></td>
                        <td>
                            <select name="minSelect" onclick="matchMinMax()">
                            <%for(int i = 1; i < maximum; i++){
                            %>
                                <option value="<%=i%>"><%=i%></option>
                            <%}%>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td class="formRow"><label>Select maximum number of instances</label></td>
                        <td>
                            <select name="maxSelect" onclick="matchMinMax()">
                            <%for(int i = 1; i < maximum; i++){
                            %>
                                <option value="<%=i%>"><%=i%></option>
                            <%}%>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td class="formRow"><label>Public git repository URL</br>E.g.: https://github.com/lakwarus/sugarcrm.git</label></td>
                        <td class="formRow"><input name="repo_url" type="text"/></td>
                    </tr>
                <%}%>
            </tbody>
        </table>
        <table width="100%" id="controlTable" class="styledLeft">
            <tr>
                <td  class="buttonRow"><input type="button" class="button" onclick="validate();" value ="Submit">
                <%if(!cartridgeType.equalsIgnoreCase("mysql")){%>
                    <a onclick="showOtherCartridges();">Connect another cartridge...</a>
                <%}%></td>
            </tr>
        </table>
    </form>

    <p>&nbsp;</p>
    </div>

</div>

<script type="text/javascript">
    function validate() {
        if (document.subscribeToCartridge.cartridge_name.value == "" ) {
            CARBON.showWarningDialog('Please fill cartridge name field ');
            return;
        }
        if (document.subscribeToCartridge.cartridge_type.value == "" ) {
            CARBON.showWarningDialog('Please fill cartridge type field ');
            return;
        }
        document.subscribeToCartridge.submit();
    }

    var rawAdded = false;
    function display_alias_field() {
        if(!rawAdded){
            var table = document.getElementById("subscribeToCartridgeTbl");
                //add a alias row
            var newRow = document.getElementById("subscribeToCartridgeTbl").insertRow(-1);
            newRow.id = 'file' + 6;

            var oCell = newRow.insertCell(-1);
            oCell.innerHTML = '<label>Data Cartridge Alias</label>';
            oCell.className = "formRow";

            oCell = newRow.insertCell(-1);
            oCell.innerHTML = '<input name="other_alias" type="text"/>';
            oCell.className = "formRow";
            rawAdded = true;
            alternateTableRows('subscribeToCartridgeTbl', 'tableEvenRow', 'tableOddRow');
        }
    }
    function matchMinMax(){
        var min = document.subscribeToCartridge.minSelect.value;
        var max = document.subscribeToCartridge.maxSelect.value;
            if(min >= 1 && max >= 1 && min > max){
                CARBON.showWarningDialog('Please select a maximum higher than the minimum');
                document.subscribeToCartridge.minSelect.value = 1;
                document.subscribeToCartridge.maxSelect.value = 1;
                return;
            }
    }

    var rawsAdded = false;
    function showOtherCartridges(){
    if(!rawsAdded){
            //add a alias row
        var newRow = document.getElementById("subscribeToCartridgeTbl").insertRow(-1);
        newRow.id = 'file' + 6;

        var oCell = newRow.insertCell(-1);
        oCell.innerHTML = '<label>Select another Cartridge to connect</label>';
        oCell.className = "formRow";

        oCell = newRow.insertCell(-1);
        oCell.innerHTML = '<select name="other_cartridge_type" ><option value="mysql">mysql</option></select>';
        oCell.className = "formRow";
        showOtherCartridgesAliasField();
        rawsAdded = true;
        alternateTableRows('subscribeToCartridgeTbl', 'tableEvenRow', 'tableOddRow');

        var table = document.getElementById("controlTable");
        table.deleteRow(0);
        var newRow = document.getElementById("controlTable").insertRow(-1);
        newRow.id = 'file' + 6;

        var oCell = newRow.insertCell(-1);
        oCell.innerHTML = '<input type="button" class="button" onclick="validate();" value ="Submit"><a onclick="hideOtherCartridgesFields();"> Hide connecting cartridge fields...</a>';
        oCell.className = "formRow";

    }
    }

    function showOtherCartridgesAliasField(){
            //add a alias row
        var newRow = document.getElementById("subscribeToCartridgeTbl").insertRow(-1);
        newRow.id = 'file' + 6;

        var oCell = newRow.insertCell(-1);
        oCell.innerHTML = '<label>Other Cartridge Alias</label>';
        oCell.className = "formRow";

        oCell = newRow.insertCell(-1);
        oCell.innerHTML = '<input name="other_alias" type="text"/>';
        oCell.className = "formRow";

        alternateTableRows('subscribeToCartridgeTbl', 'tableEvenRow', 'tableOddRow');
    }


    function hideOtherCartridgesFields(){
            //add a alias row
        var table = document.getElementById("subscribeToCartridgeTbl");
        document.getElementById("subscribeToCartridgeTbl").deleteRow(5);
        hideOtherCartridgesAlias();
        rawsAdded = false;
    }
    function hideOtherCartridgesAlias(){
            //add a alias row
        var table = document.getElementById("subscribeToCartridgeTbl");
        document.getElementById("subscribeToCartridgeTbl").deleteRow(5);


        var table = document.getElementById("controlTable");
        table.deleteRow(0);
        var newRow = document.getElementById("controlTable").insertRow(-1);
        newRow.id = 'file' + 6;

        var oCell = newRow.insertCell(-1);
        oCell.innerHTML = '<input type="button" class="button" onclick="validate();" value ="Submit"><a onclick="showOtherCartridges();"> Connect another cartridge...</a>';
        oCell.className = "formRow";

    }


    alternateTableRows('subscribeToCartridgeTbl', 'tableEvenRow', 'tableOddRow');
</script>

</fmt:bundle>
