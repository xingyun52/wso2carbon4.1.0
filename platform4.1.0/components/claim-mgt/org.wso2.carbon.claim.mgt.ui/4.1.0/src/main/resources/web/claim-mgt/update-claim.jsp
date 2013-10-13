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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>


<%@page import="java.lang.Exception"%>
<%@page import="org.wso2.carbon.claim.mgt.ui.client.ClaimAdminClient"%>
<%@page import="org.wso2.carbon.claim.mgt.stub.dto.ClaimMappingDTO"%>
<%@ page import="org.wso2.carbon.user.core.UserCoreConstants" %>
<%@ page import="org.wso2.carbon.claim.mgt.stub.dto.ClaimDialectDTO" %>


<%
    ClaimDialectDTO[] claimMappping = null;
    String dialectUri = request.getParameter("dialect");
    String claimUri = request.getParameter("claimUri");
    claimMappping = (ClaimDialectDTO[])session.getAttribute("claimMappping");

%>


<fmt:bundle basename="org.wso2.carbon.claim.mgt.ui.i18n.Resources">
	<carbon:breadcrumb label="update"
		resourceBundle="org.wso2.carbon.claim.mgt.ui.i18n.Resources"
		topPage="false" request="<%=request%>" />

	<script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
	<script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
	<script type="text/javascript" src="../carbon/admin/js/main.js"></script>

	<div id="middle">
	<h2><fmt:message key='claim.management'/></h2>
	<div id="workArea">
	
	   <script type="text/javascript">
	    function setType(chk,hidden) {
	    	var val = document.getElementById(chk).checked;
    		var hiddenElement = document.getElementById(hidden);

    		if (val){
    			hiddenElement.value="true";
    		}else {
    			hiddenElement.value="false";
    		}
   		}
        function remove(dialect,claim,length) {
            var defaultDialect = "<%=UserCoreConstants.DEFAULT_CARBON_DIALECT%>";
            if((dialect == defaultDialect) && (length < 2 )){
                CARBON.showWarningDialog('<fmt:message key="cannot.remove.default.carbon.dialect.all.claims"/>');
                return false;
            } else {
                CARBON.showConfirmationDialog('<fmt:message key="remove.message1"/>'+ claim +'<fmt:message key="remove.message2"/>',
                function() {
                    location.href ="remove-claim.jsp?dialect="+dialect+"&claimUri="+claim;
                }, null);
            }
   }

        function validate() {            
        	var value = document.getElementsByName("attribute")[0].value;
        	if (value == '') {
            	CARBON.showWarningDialog('<fmt:message key="attribute.is.required"/>');
            	return false;
        	}      

        	var value = document.getElementsByName("description")[0].value;
        	if (value == '') {
            	CARBON.showWarningDialog('<fmt:message key="description.is.required"/>');
            	return false;
        	}         

        	var value = document.getElementsByName("displayName")[0].value;
        	if (value == '') {
            	CARBON.showWarningDialog('<fmt:message key="displayname.is.required"/>');
            	return false;
        	} 

        	var value = document.getElementsByName("displayOrder")[0].value;
        	if (value != '') {
        		var IsFound = /^-?\d+$/.test(value);
            	if(!IsFound) {
                  CARBON.showWarningDialog('<fmt:message key="display.order.has.to.be.integer"/>');
            	  return false;
            	}
        	}
        	
        	document.updateclaim.submit();
    	}        
  	   </script>
  	   
    <div style="height:30px;">
        	<%for (int i=0; i<claimMappping.length;i++ ){
        	 if (claimMappping[i].getDialectURI().equals(dialectUri)) {
        	 ClaimMappingDTO[] claims =  claimMappping[i].getClaimMappings();
           %>
                <a href="#" class="icon-link"  style="background-image:url(../claim-mgt/images/delete.gif);"
                   onclick="remove('<%=dialectUri%>','<%=claimUri%>','<%=claims.length%>'  );return false;"><fmt:message key='remove.claim.mapping'/></a>
    </div>

    <form name="updateclaim" action="update-claim-submit.jsp?claimUri=<%=claimUri%>&dialect=<%=dialectUri%>" method="post">
	<table style="width: 100%" class="styledLeft">
		<% for (int j=0; j<claims.length;j++ ) {             
              if (claims[j].getClaim().getClaimUri().equals(claimUri)){%>
		<thead>
			<tr>
				<th colspan="2"><fmt:message key='update.claim.details'/></th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td class="formRow">
					<table class="normal" cellspacing="0" style="width: 100%" >
					    	<tr>
							<td class="leftCol-small"><fmt:message key='display.name'/><font class="required">*</font></td>
							<td><input type="text" name="displayName" id="displayName" value="<%=claims[j].getClaim().getDisplayTag()%>"/></td>
						</tr>
						
						<tr>
							<td class="leftCol-small"><fmt:message key='description'/><font class="required">*</font></td>
							<td><input type="text" name="description" id="description" value="<%=claims[j].getClaim().getDescription()%>"/></td>
						</tr>
			
						<tr>
							<td class="leftCol-small"><fmt:message key='claim.uri'/><font class="required">*</font></td>
							<td><%=claims[j].getClaim().getClaimUri()%></td>
						</tr>
			
						<tr>
							<td class="leftCol-small"><fmt:message key='mapped.attribute'/><font class="required">*</font></td>
							<td><input type="text" name="attribute" id="attribute" value="<%=claims[j].getMappedAttribute()%>"/></td>
						</tr>
			
						<tr>
							<td class="leftCol-small"><fmt:message key='regular.expression'/></td>
							<% if(claims[j].getClaim().getRegEx()!=null) {%>
							<td><input type="text" name="regex" id="regex" value="<%=claims[j].getClaim().getRegEx()%>"/></td>
							<%} else { %>
							<td><input type="text" name="regex" id="regex"/></td>
							<%} %>
						</tr>
			            		<tr>
							<td class="leftCol-small"><fmt:message key='display.order'/></td>
							<td><input type="text" name="displayOrder" id="displayOrder" value="<%=claims[j].getClaim().getDisplayOrder()%>" /></td>				
						</tr>
						<tr>
							<td class="leftCol-small"><fmt:message key='supported.by.default'/></td>
							<%if (claims[j].getClaim().getSupportedByDefault()) { %>
							<td>
							    <input type='checkbox' name='supported' id='supported' checked='checked' onclick="setType('supported','supportedhidden')" />
							    <input type='hidden' name='supportedhidden' id='supportedhidden' value='true' />
							</td>
							<% } else { %>
							<td>
							    <input type='checkbox' name='supported' id='supported' onclick="setType('supported','supportedhidden')" />
							    <input type='hidden' name='supportedhidden' id='supportedhidden' value='false' />
							</td>
							<%} %>
						</tr>
			
						<tr>
							<td class="leftCol-small"><fmt:message key='required'/></td>
							<%if (claims[j].getClaim().getRequired()) { %>
							<td>
							     <input type='checkbox' name='required' id='required' checked='checked' onclick="setType('required','requiredhidden')" />
							     <input type='hidden' name='requiredhidden' id='requiredhidden' value='true' />
							</td>
							<% } else { %>
							<td>
							     <input type='checkbox' name='required' id='required' onclick="setType('required','requiredhidden')" />
							     <input type='hidden' name='requiredhidden' id='requiredhidden' value='false' />
							</td>
							<%} %>
						</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td colspan="2" class="buttonRow">
		                      <input type="button" value="<fmt:message key='update'/>" class="button" onclick="validate();"/>
		                      <input class="button" type="reset" value="<fmt:message key='cancel'/>"  onclick="javascript:document.location.href='claim-view.jsp?dialect=<%=dialectUri%>&ordinal=1'"/ >
	                  	</td>
			</tr>

		</tbody>
		<%}}%>
	</table>
	<%} }%>
	</form>
	</div>	
	</div>
</fmt:bundle>
