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
<%@ page import="org.apache.commons.dbcp.BasicDataSource" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>





<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>StratosLive RSS Sample - Update Record</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="">
    <meta name="author" content="">

    <!-- Le styles -->
    <link href="bootstrap/css/bootstrap.css" rel="stylesheet">
    <style type="text/css">

    </style>
    <link href="bootstrap/css/bootstrap-responsive.css" rel="stylesheet">
    <link href="css/local.css" rel="stylesheet">

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
    <%
        String recordId=request.getParameter("id");
        String customerName=request.getParameter("customerName");
    %>
    <script type="text/javascript">
        function save()
        {
            var customerNameUpdated=document.getElementById('customerNameUpdated').value;
            location.href = 'save-update-delete-record.jsp?id=' + '<%=recordId%>'+'&customerName='+customerNameUpdated+'&mode=update'
        }
    </script>
    <div class="container-narrow">

      <div class="masthead">
        <ul class="nav nav-pills pull-right">
          <li class="active"><a href="index.jsp">Home</a></li>
          <li><a href="insert-records.jsp">Add New Item</a></li>
        </ul>
        <h3 class="muted">StratosLive RSS Sample</h3>
      </div>


      <form class="form-horizontal">
      <div class="row-fluid marketing">
            <div class="span12">
                <h2>Update Record</h2>

                <div class="control-group">
                    <label class="control-label" for="customerId">Record ID</label>
                    <div class="controls">
                      <%=recordId%>
                    </div>
                </div>


                <div class="control-group">
                    <label class="control-label" for="customerName">Customer Name</label>
                    <div class="controls">
                      <input id="customerNameUpdated" name="customerNameUpdated" value="<%=customerName%>"/>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls">
                        <input id="save" name="save" type="button"
                               onclick="save()"
                               value="Save"
                               class="btn btn-primary" />

                    </div>
                </div>





            </div>
      </div>
      </form>
      <hr>

      <div class="footer">
        <p>&copy; WSO2 2013</p>
      </div>

    </div> <!-- /container -->

  </body>
</html>

