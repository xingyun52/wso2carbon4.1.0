<!DOCTYPE html>
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
<%@page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConstants"%>


<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.stratos.identity.saml2.sso.mgt.ui.Util" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<html lang="en">
  <head>
      <meta charset="utf-8">
      <title>WSO2 App Factory</title>
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <meta name="description" content="">
      <meta name="author" content="">
      <!-- Le styles -->
      <link href="../carbon/appfactory/lib/bootstrap/assets/css/bootstrap.css" rel="stylesheet">
      <link rel="stylesheet" href="../carbon/appfactory/css/stylesheet.css" type="text/css" charset="utf-8"/>

      <!-- Le HTML5 shim, for IE6-8 support of HTML5 elements -->
      <!--[if lt IE 9]>
      <script src="../carbon/appfactory/lib/bootstrap/assets/js/html5.js"></script>
      <![endif]-->
      <script src="../carbon/appfactory/lib/jquery/jquery-1.7.2.min.js"></script>
      <script src="../carbon/appfactory/js/slider.js"></script>

      <!-- Le fav and touch icons -->
      <link rel="shortcut icon" href="../carbon/appfactory/lib/bootstrap/assets/ico/favicon.ico">
      <link rel="apple-touch-icon-precomposed" sizes="144x144" href="../carbon/appfactory/lib/bootstrap/assets/ico/apple-touch-icon-144-precomposed.png">
      <link rel="apple-touch-icon-precomposed" sizes="114x114" href="../carbon/appfactory/lib/bootstrap/assets/ico/apple-touch-icon-114-precomposed.png">
      <link rel="apple-touch-icon-precomposed" sizes="72x72" href="../carbon/appfactory/lib/bootstrap/assets/ico/apple-touch-icon-72-precomposed.png">
      <link rel="apple-touch-icon-precomposed" href="../carbon/appfactory/lib/bootstrap/assets/bootstrap/assets/ico/apple-touch-icon-57-precomposed.png">

      <!--[if gte IE 9]>
      <link rel="stylesheet" href="../carbon/appfactory/css/stylesheet-ie9.css" type="text/css" />
      <![endif]-->

      <!--[if IE 7]>
      <link rel="stylesheet" href="../carbon/appfactory/css/stylesheet-ie7.css" type="text/css" />
      <![endif]-->

      <!--[if IE 8]>
      <link rel="stylesheet" href="../carbon/appfactory/css/stylesheet-ie8.css" type="text/css" />
      <![endif]-->
  </head>

  <body>
    <jsp:include page="../carbon/appfactory/fonts/tmp.jsp" />
    <fmt:bundle basename="org.wso2.stratos.identity.saml2.sso.mgt.ui.i18n.Resources">
    <%
        String errorMessage = "login.fail.message";
        String tenantRegistrationPageURL = Util.getTenantRegistrationPageURL();

        if (request.getAttribute(SAMLSSOProviderConstants.AUTH_FAILURE) != null &&
            (Boolean)request.getAttribute(SAMLSSOProviderConstants.AUTH_FAILURE)) {
            if(request.getAttribute(SAMLSSOProviderConstants.AUTH_FAILURE_MSG) != null){
                errorMessage = (String) request.getAttribute(SAMLSSOProviderConstants.AUTH_FAILURE_MSG);
            }
    %>
    <script type="text/javascript">
        $(document).ready(function() {
            alert('<fmt:message key="<%=errorMessage%>"/>');
        });
    </script>
    <%
        }  else if (request.getSession().getAttribute(CarbonUIMessage.ID) !=null) {
            CarbonUIMessage carbonMsg = (CarbonUIMessage)request.getSession().getAttribute(CarbonUIMessage.ID);
            %>

                <script type="text/javascript">
                    $(document).ready(function() {
                        alert("<%=carbonMsg.getMessage()%>");
                    });
                </script>
      <%}
    %>
    <script type="text/javascript">
        function doLogin() {
            var loginForm = document.getElementById('loginForm');
            loginForm.submit();
        }
        function doRegister() {
            document.getElementById('registrationForm').submit();
        }
    </script>
    <!--
    START Header back ground
    No real content is here just to display the head
    -->
    <div class="container-fluid header">
        <div class="row-fluid">
            <div class="span6"></div>
            <div class="span6 top-menu"></div>
        </div>
    </div>
    <div class="clearfix"></div>
    <!--END Header back ground-->

    <!--START Header menu-->
    <div class="container">
        <div class="row">
            <div class="span3 logo-section">
                <a class="brand" href="#"></a>
            </div>
            <div class="span9 menu-back">
                <div class="menu-content">
                    <div class="navbar">
                        <div class="navbar-inner pull-right">

                            <ul class="nav">
                                <li class="active">
                                <a href="http://wso2.com/solutions/app-factory/" target="_blank">
                                    <i class="icon-home"></i>
                                    <br />
                                    APP Factory @ wso2.com
                                </a>
                                </li>
                                <li class="active">
                                <a href="http://wso2.com/support/" target="_blank">
                                    <i class="icon-support"></i>
                                    <br />
                                    Support
                                </a>
                                </li>
                                <li class="active">
                                <a href="http://wso2.com/contact/" target="_blank">
                                    <i class="icon-contact"></i>
                                    <br />
                                    Contacts
                                </a>
                                </li>
                            </ul>

                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <!--END Header menu-->

    <!--START breadcrumb section-->
    <div class="container breadcrumb-section">
        <div class="row">
            <div class="span3"></div>
            <div class="span9 top-slider-icons" style="height:27px;">
                <a class="" onClick="goto(1, this); return false"><i class="icon-slide_1"></i> </a>
                <a class="" onClick="goto(2, this); return false"><i class="icon-slide_2"></i> </a>
                <a class="" onClick="goto(3, this); return false"><i class="icon-slide_3"></i> </a>
                <a class="" onClick="goto(4, this); return false"><i class="icon-slide_4"></i> </a>
                <a class="" onClick="goto(5, this); return false"><i class="icon-slide_5"></i> </a>
            </div>
        </div>
    </div>
    <!--END breadcrumb section-->
    <div class="clouds">
        <div class="clouds-container" id="small-clouds">
            <div class="clouds-small"></div>
            <div class="clouds-small"></div>
            <div class="clouds-small"></div>
            <div class="clouds-small"></div>
            <div class="clouds-small"></div>
        </div>
    </div>
    <div class="clouds">
        <div class="clouds-container" id="big-clouds">
            <div class="clouds-big"></div>
            <div class="clouds-big"></div>
            <div class="clouds-big"></div>
            <div class="clouds-big"></div>
            <div class="clouds-big"></div>
        </div>
    </div>



    <!--START  content section-->
    <div class="container content-container wrapper">
        <div class="row">
            <form class="form-horizontal" action="../samlsso" method="post"   id="loginForm">
            <div class="span3 login-box">
                <h2 class="login-head">LOGIN</h2>
                <label class="special">Email address:</label>
                <input type="text" id="username" name="username">

                <input type="hidden" name="<%= SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL %>"
                       value="<%= request.getAttribute(SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL) %>"/>
                <input type="hidden" name="<%= SAMLSSOProviderConstants.ISSUER %>"
                       value="<%= request.getAttribute(SAMLSSOProviderConstants.ISSUER) %>"/>
                <input type="hidden" name="<%= SAMLSSOProviderConstants.REQ_ID %>"
                       value="<%= request.getAttribute(SAMLSSOProviderConstants.REQ_ID) %>"/>
                <input type="hidden" name="<%= SAMLSSOProviderConstants.SUBJECT %>"
                       value="<%= request.getAttribute(SAMLSSOProviderConstants.SUBJECT) %>"/>
                <input type="hidden" name="<%= SAMLSSOProviderConstants.RP_SESSION_ID %>"
                       value="<%= request.getAttribute(SAMLSSOProviderConstants.RP_SESSION_ID) %>"/>
                <input type="hidden" name="<%= SAMLSSOProviderConstants.REQ_MSG_STR%>"
                       value="<%= request.getAttribute(SAMLSSOProviderConstants.REQ_MSG_STR) %>"/>
                <input type="hidden" name="<%= SAMLSSOProviderConstants.RELAY_STATE %>"
                       value="<%= request.getAttribute(SAMLSSOProviderConstants.RELAY_STATE) %>"/>


                <label><fmt:message key='password'/>:</label>
                <input type="password" id="password" name="password" />
                <div class="control-group register-btn-gap">
                    <button type="btn btn-primary" class="btn btn-primary">Sign in</button>
                </div>
                <div class="clearfix"></div>
                Not Registered yet?
                <a href="../appmgt/site/pages/register.jag">Register for Free</a>

                <hr />
                <h2 class="video-title">About App Factory</h2>
                <div style="opacity:1;position:absolute;">
                    <iframe width="225" height="127" src="http://www.youtube.com/embed/6BoiQscIczc?rel=0" frameborder="0" allowfullscreen></iframe>
                </div>
            </div>
            <div class="span9 add-section">
                <div class="container-fluid">
                    <div class="row-fluid">
                        <div class="span1">
                            <a class="slide-left" data-value="1" style="display:none"><img src="../carbon/appfactory/images/arrow-left.png"></a>
                        </div>
                        <div class="span10">
                            <div class="slide-headings">
                                <h2><span>WSO2 </span><span> App Factory</span></h2>
                                <h2 style="display:none"><span>Create </span><span> and Govern </span><span>Application</span><span>Projects</span></h2>
                                <h2 style="display:none"><span>Develop  </span><span> Any Kind </span><span> of Applications</span></h2>
                                <h2 style="display:none"><span>Publish  </span><span>APIs to API Stores</span></h2>
                                <h2 style="display:none"><span>Integrated </span><span> Developer </span><span> Experience</span></h2>
                            </div>

                            <div id="content" class="add-content">

                                <div class="contentbox-wrapper">

                                    <div id="slide_1" class="contentbox">

                                        <span class="special">WSO2 App Factory </span> is a platform for managed
                                        application development for the <span class="special">entire lifecycle</span> of
                                        applications. Supporting you from cradle to grave, you can create, develop,
                                        test, deploy to production and retire applications with a single click.
                                        Applications can be web applications to mobile apps that require <span
                                            class="special">any type of middleware</span> to run on including even
                                        non-Java and non-WSO2 technologies.

                                    </div>

                                    <div id="slide_2" class="contentbox">

                                        Create resource including <span class="special">source repository, issue tracker</span>, forums and runtimes
                                        for application component types.
                                        Manage application scale parameters for development, testing, staging and
                                        production.
                                        <span class="special">Invite and manage developers to applications.</span>
                                        Complete <span class="special">version management</span> of applications.
                                    </div>

                                    <div id="slide_3" class="contentbox">

                                        Application components can include Web applications, services of various kinds
                                        including JAX-WS/JAX-RS services, business processes and <span class="special">backends for mobile
                                        applications.</span>
                                        Supported runtimes include the entire WSO2 product suite as well as 3rd party
                                        products including PHP, JBoss and more.
                                    </div>
                                    <div id="slide_4" class="contentbox">
                                        Applications & APIs can be auto published to enterprise or external app/API
                                        store at certain lifecycle stages.
                                        Integrates with WSO2 API Manager as an API store.
                                    </div>
                                    <div id="slide_5" class="contentbox">
                                        <span class="special">Supports complete developer lifecycle</span> from checking out project location to building to checking in to pushing up stream for later stages of development.
                                        Support for not only <span class="special">engineering</span> but also <span class="special">QA</span> and <span class="special">performance testing</span> during staging.
                                        Integrated to <span class="special">WSO2 Developer Studio</span> for one-click deployment.
                                    </div>
                                </div>

                            </div>


                            <div class="container-fluid">
								<div class="row-fluid">
                                    <div class="span12 big-get-started-btn"><a class="btn" href="../appmgt/site/pages/register.jag">SIGN UP</a></div>
                                </div>
                                <div class="row-fluid">
                                    <div class="span12">
                                        <div id="content_images" class="add-image-content">

                                            <div class="contentbox-image-wrapper">

                                                <div id="slide_image_1" class="contentbox">

                                                    <img src="../carbon/appfactory/images/big-icon/wso2-app-factory_1.png">

                                                </div>

                                                <div id="slide_image_2" class="contentbox">

                                                    <img src="../carbon/appfactory/images/big-icon/wso2-app-factory_2.png">

                                                </div>

                                                <div id="slide_image_3" class="contentbox">

                                                    <img src="../carbon/appfactory/images/big-icon/wso2-app-factory_1.png">

                                                </div>
                                                <div id="slide_image_4" class="contentbox">
                                                    <img src="../carbon/appfactory/images/big-icon/wso2-app-factory_1.png">

                                                </div>
                                                <div id="slide_image_5" class="contentbox">
                                                    <img src="../carbon/appfactory/images/big-icon/wso2-app-factory_1.png">

                                                </div>
                                            </div>

                                        </div>

                                    </div>
                                    <!--<div class="span12" style="text-align:center;padding-top:10px;"></div>-->
                                </div>

                            </div>
                        </div>
                        <div class="span1">
                            <a class="slide-right" data-value="1"><img src="../carbon/appfactory/images/arrow-right.png"></a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
         <div class="push"></div>
    </div>
    <div class="clearfix"></div>

    <div class="container-fluid footer">
        <div class="row-fluid">
            <div class="span12">
                <div class="container">
                    <div class="row">
                        <div class="span12 footer-text">&copy; WSO2 2012</div>
                    </div>
                </div>

            </div>
        </div>
    </div>



    <!-- Le javascript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->




  <script>

  </script>
    </fmt:bundle>

  </body>
</html>
