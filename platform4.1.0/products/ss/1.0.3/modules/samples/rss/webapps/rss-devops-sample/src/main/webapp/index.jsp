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


<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>StratosLive RSS Sample</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">

    <!-- Le styles -->
    <link href="bootstrap/css/bootstrap.css" rel="stylesheet">
      <link href="css/local.css" rel="stylesheet">

    <link href="bootstrap/css/bootstrap-responsive.css" rel="stylesheet">

    <!-- HTML5 shim, for IE6-8 support of HTML5 elements -->
    <!--[if lt IE 9]>
      <script src="http://html5shim.googlecode.com/svn/trunk/html5.js"></script>
    <![endif]-->

    <!-- Fav and touch icons -->
    <link rel="apple-touch-icon-precomposed" sizes="144x144" href="../assets/ico/apple-touch-icon-144-precomposed.png">
    <link rel="apple-touch-icon-precomposed" sizes="114x114" href="../assets/ico/apple-touch-icon-114-precomposed.png">
      <link rel="apple-touch-icon-precomposed" sizes="72x72" href="../assets/ico/apple-touch-icon-72-precomposed.png">
                    <link rel="apple-touch-icon-precomposed" href="../assets/ico/apple-touch-icon-57-precomposed.png">
                                   <link rel="shortcut icon" href="../assets/ico/favicon.png">
  </head>

  <body>

    <div class="container-narrow">

      <div class="masthead">
        <ul class="nav nav-pills pull-right">
          <li class="active"><a href="index.jsp">Home</a></li>
          <li><a href="insert-records.jsp">Add New Item</a></li>
        </ul>
        <h3 class="muted">StratosLive RSS Sample</h3>
      </div>


      <div class="jumbotron">
        <h1>RSS Feeds</h1>
        <p class="lead">Cras justo odio, dapibus ac facilisis in, egestas eget quam. Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus.</p>
        <a class="btn btn-large btn-success"  id="add" name="add"
                   href="insert-records.jsp"
                   >Add</a>
      </div>

      <hr>

      <div class="row-fluid marketing">
        <div class="span12">
              <%
                String jdbcUrl = application.getInitParameter("dbUrl");
                String dbUser =  application.getInitParameter("dbUser");
                String dbPassword = application.getInitParameter("dbPassword");
                String driverClass = application.getInitParameter("driverClass");

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
                    rs = stmt.executeQuery("SELECT * FROM CUSTOMER");
            %>
            <table class="table table-striped table-bordered">
                <thead>
                    <tr>
                        <th><strong>Customer ID</strong></th>
                        <th><strong>Customer Name</strong></th>
                        <th><strong>Operation</strong></th>
                    </tr>
                </thead>
                <tbody>
                <%
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                %>
                <tr>
                    <td><%=id%>
                    </td>
                    <td><%=name%>
                    </td>
                    <td><a id="edt"
                           onclick="location.href = 'update-records.jsp?id=' + '<%=id%>'+'&customerName='+'<%=name%>'"
                           href="#"  style="padding-right:10px;">Edit</a>

                        <a id="delete"
                           onclick="location.href = 'save-update-delete-record.jsp?id='+'<%=id%>'+'&mode=delete';"
                           href="#">Delete
                        </a>
                    </td>
                </tr>
                <%
                    }
                %>
                </tbody>
            </table>

            <%
                } catch (SQLException e) {
                    e.printStackTrace();
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
                }
            %>
        </div>
      </div>

      <hr>

      <div class="footer">
        <p>&copy; WSO2 2013</p>
      </div>

    </div> <!-- /container -->

  </body>
</html>
