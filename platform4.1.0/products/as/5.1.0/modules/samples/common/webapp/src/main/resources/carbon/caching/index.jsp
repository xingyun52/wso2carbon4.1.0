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
<%@ page import="org.wso2.carbon.context.CarbonContext" %>

<h2>WSO2 Carbon Caching Demo</h2>

<hr/>
<p>

<h3>Add to Cache</h3>

<form action="index.jsp" method="POST">
    <table border="0">
        <tr>
            <td>Key</td>
            <td><input type="text" name="key"/></td>
        </tr>
        <tr>
            <td>Value</td>
            <td><input type="text" name="value"/></td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td><input type="submit" value="Add" name="add"></td>
        </tr>
    </table>
</form>
</p>
<hr/>
<p>

<h3>Read from Cache</h3>

<form action="index.jsp" method="POST">
    <table border="0">
        <tr>
            <td>Key</td>
            <td><input type="text" name="key"/></td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td><input type="submit" value="View" name="view"></td>
        </tr>
    </table>
</form>
</p>
<hr/>

<%
    // The CarbonContext instance used to obtain the cache
    CarbonContext cCtx = CarbonContext.getCurrentContext();

    if (request.getParameter("add") != null) {
        String key = request.getParameter("key");
        cCtx.getCache().put(key, request.getParameter("value"));
%>
<p>
    Added entry: <%= key %>
</p>
<%
    } else if (request.getParameter("view") != null) {
        String key = request.getParameter("key");
        if (cCtx.getCache().get(key) != null) {
            String content = (String)cCtx.getCache().get(key);
            response.addHeader("cache-value", content);
%>
            <p>
                Value of entry <%= key%> : <%= content %>
            </p>
<%
        } else {
%>
            <p>
                Unable to find an entry by the given key <%= key%>!
            </p>
<%
        }
    }
%>
