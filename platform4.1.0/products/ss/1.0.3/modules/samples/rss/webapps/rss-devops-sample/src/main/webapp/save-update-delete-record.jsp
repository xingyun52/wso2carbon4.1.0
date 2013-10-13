<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@page import="org.apache.commons.dbcp.BasicDataSource" %>
<%@page import="java.sql.Connection" %>
<%@page import="java.sql.ResultSet" %>
<%@page import="java.sql.SQLException" %>
<%@page import="java.sql.Statement" %>

<html>
<head>

</head>
<body>
<h2>StratosLive RSS Sample</h2>
<%
    String jdbcUrl = application.getInitParameter("dbUrl");
    String dbUser =  application.getInitParameter("dbUser");
    String dbPassword = application.getInitParameter("dbPassword");
    String driverClass = application.getInitParameter("driverClass");
    String mode=request.getParameter("mode");
    String customerName=request.getParameter("customerName");
    int customerId=Integer.parseInt(request.getParameter("id"));
    BasicDataSource ds = new BasicDataSource();
    ds.setDriverClassName(driverClass);
    ds.setUrl(jdbcUrl);
    ds.setUsername(dbUser);
    ds.setPassword(dbPassword);

    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
        connection = ds.getConnection();
        stmt = connection.createStatement();
        if("add".equals(mode))
        {
            stmt.executeUpdate("INSERT INTO CUSTOMER VALUES("+customerId+",'"+customerName+"')");
        }
        else if("update".equals(mode))
        {
            stmt.executeUpdate("UPDATE CUSTOMER SET NAME='"+customerName+"' WHERE ID="+customerId+"");
        }
        else if("delete".equals(mode))
        {
            stmt.executeUpdate("DELETE FROM CUSTOMER WHERE ID="+customerId+"");
        }
    } catch (SQLException e) {%>
        <script type="text/javascript">
                     alert("Error while perform operation!");
        location.href='index.jsp';
        </script>
    <%    e.printStackTrace();
    } finally {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        %>
<%=customerName%>   oo
<%=customerId%>       pp
<%=mode%>               oo
<%
    }
%>
<script type="text/javascript">
    location.href='index.jsp';
</script>
</body>
</html>
