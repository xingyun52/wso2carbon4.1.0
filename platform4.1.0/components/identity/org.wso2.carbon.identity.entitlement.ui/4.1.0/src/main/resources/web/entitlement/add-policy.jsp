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

<%
    String policy = "";
%>
   
    <div style="display: none;">
       <form name="frmPolicyData" action="../policyeditor/index.jsp" method="post">
        <input type="hidden" name="policy" id="policy">
        <input type="hidden" name="visited" id="visited">
        <textarea id="txtPolicy" rows="50" cols="50"><%=policy%></textarea>
        <input type="hidden" name="callbackURL" value="../entitlement/add-policy-submit.jsp"/>
       </form>
    </div> 
    
    <script type="text/javascript">
    // Handling the browser back button for Firefox. The IE back button is handled form the policy editor index.jsp page
    if (document.frmPolicyData.visited.value == "")
    {
        // This is a fresh page load
        document.frmPolicyData.visited.value = "1";

        function submitForm() {
        	document.getElementById("policy").value = document.getElementById("txtPolicy").value;
            document.frmPolicyData.submit();
        }
        submitForm();
    }
    else
    {
        location.href = '<%=request.getHeader("Referer")%>';
    }
</script>
