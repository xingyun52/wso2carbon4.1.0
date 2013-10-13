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

<%@ page import="org.wso2.carbon.appfactory.login.config.ServiceReferenceHolder" %>
<%@ page import="org.wso2.carbon.appfactory.common.AppFactoryConstants" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.stratos.identity.saml2.sso.mgt.ui.Util" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<html lang="en">
  <head>
      <meta charset="utf-8" />
	  <meta http-equiv="X-UA-Compatible" content="IE=edge" />
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
      <script src="../carbon/appfactory/js/messages.js"></script>
      
      <script type="text/javascript">
  	var _gaq = _gaq || [];
  	_gaq.push(['_setAccount', 'UA-38397051-1']);
  	_gaq.push(['_trackPageview']);

  	(function() {
    	var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    	ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    	var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  	})();
      </script>

      <!-- Le fav and touch icons -->
      <link rel="shortcut icon" href="../carbon/appfactory/images/favicon.png">

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

  <body class="backpic">
    <jsp:include page="../carbon/appfactory/fonts/tmp.jsp" />
    <fmt:bundle basename="org.wso2.stratos.identity.saml2.sso.mgt.ui.i18n.Resources">
    <%
        String regLink =  ServiceReferenceHolder.getInstance().getAppFactoryConfiguration().getFirstProperty(AppFactoryConstants.REGISTRATION_LINK);
        String signUpText =  ServiceReferenceHolder.getInstance().getAppFactoryConfiguration().getFirstProperty("SignUpText");
        String signInText =  ServiceReferenceHolder.getInstance().getAppFactoryConfiguration().getFirstProperty("SignInText");
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
            message({content:'<fmt:message key="<%=errorMessage%>"/>',type:'error' });
        });
    </script>
    <%
        } else if(request.getAttribute("urn:oasis:names:tc:SAML:2.0:status:Requester") !=null){
            session.invalidate();
            errorMessage = "Session timeout, please login back.";
    %>
       <script type="text/javascript">
           $(document).ready(function() {
               message({content:'<fmt:message key="<%=errorMessage%>"/>',type:'info' });
           });
       </script>
       <%
        }  else if (request.getSession().getAttribute(CarbonUIMessage.ID) !=null) {
            CarbonUIMessage carbonMsg = (CarbonUIMessage)request.getSession().getAttribute(CarbonUIMessage.ID);
            %>

                <script type="text/javascript">
                    $(document).ready(function() {
                        message({content:'<%=carbonMsg.getMessage()%>',type:'info' });
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
<div id="wrap">
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
                <a class="brand" href="../appmgt/"></a>
            </div>
            <div class="span9 menu-back">
                <div class="menu-content">
                    <div class="navbar">
                        <div class="navbar-inner pull-right">

                            <ul class="nav">
								<li class="active">
									<a href="http://docs.wso2.org/wiki/display/AF100/User+Guide" target="_blank">
										<i class="icon-user-guide"></i>
										<br>
										User Guide
									</a>
								</li>
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
    <div class="container">
        <div class="row">
            <div class="span12">
                <div class="breadcrumb-section"></div>
            </div>
        </div>
    </div>

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
    <div class="container content-container wrapper">
           <div class="row">
               <div class="span4">
                   <div class="get-started-box left-side-boxes">
                       <p><%=signUpText%></p>
                       <a class="btn btn-sign-up" href="<%=regLink%>" target="_blank">Get started</a>
                   </div>
                   <form class="form-horizontal" action="../samlsso" method="post"   id="loginForm">
                   <div class="get-started-box left-side-boxes login-box">
                       <p><%=signInText%></p>
                       <label class="special">Email:</label>
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
                            <button class="btn">Sign in</button>
                        </div>
                   </div>
                   </form>
               </div>
               <div class="span8">
                    <div class="get-started-box main-content">
                                <div class="contentbox-wrapper">

                                    <div id="slide_1" class="contentbox">
                                        <img src="../carbon/appfactory/images/01.png">
                                    </div>

                                    <!--div id="slide_2" class="contentbox">
                                        <h2>WSO2 App Factory</h2>
                                        <div class="video-wrapper">
                                            <iframe width="530" height="250" src="https://www.youtube.com/embed/6BoiQscIczc?rel=0" frameborder="0" allowfullscreen></iframe>
                                        </div>
                                    </div -->

                                    <div id="slide_2" class="contentbox">
                                         <img src="../carbon/appfactory/images/02.png">
                                    </div>

                                    <!--div id="slide_3" class="contentbox">
                                        <h2>Develop  any kind of Application</h2>

                                        Application components can include Web applications, services of various kinds
                                        including JAX-WS/JAX-RS services,  and <span class="special">backends for mobile
                                        applications.</span>
                                        Supported runtimes include WSO2 product suite 
                                    </div -->
                                    <div id="slide_3" class="contentbox">
                                        <img src="../carbon/appfactory/images/03.png">
                                    </div>
                                    <div id="slide_4" class="contentbox">
                                        <img src="../carbon/appfactory/images/04.png">
                                    </div>
                                </div>
								<div class="pagination pagination-centered" id="slider-buttons">
								  <ul>
									<li class="active slide-left" ><a href="#"><<</a></li>
									<li class="active" onClick="goto(1, this); return false"><a href="#">1</a></li>
									<li><a href="#" onClick="goto(2, this); return false">2</a></li>
									<li><a href="#" onClick="goto(3, this); return false">3</a></li>
									<li><a href="#" onClick="goto(4, this); return false">4</a></li>
									<li><a href="#" class="slide-right" >>></a></li>
								 </ul>
								</div>
                                
                    </div>
               </div>
           </div>
       </div>







    <!--START  content section-->

    <div class="clearfix"></div>





    <div id="push"></div>



   </div>
    <div id="footer">
      <div class="container-fluid footer">
          <div class="row-fluid">
              <div class="span12">
                  <div class="container">
                      <div class="row">
                          <div class="span3 footer-content">&copy; WSO2 2013</div>
                      </div>
                  </div>

              </div>
          </div>
      </div>
    </div>
     <!--Elements to display popups-->
<div class="modal fade" id="messageModal"></div>
<div id="confirmation-data" style="display:none;">
    <div class="modal-header">
        <button class="close" data-dismiss="modal">&#215;</button>
        <h3 class="modal-title">Modal header</h3>
    </div>
    <div class="modal-body">
        <p>One fine body…</p>
    </div>
    <div class="modal-footer">
        <a href="#" class="btn btn-primary">Save changes</a>
        <a href="#" class="btn btn-other" data-dismiss="modal">Close</a>
    </div>
</div>

    <script src="../carbon/appfactory/lib/bootstrap/assets/js/bootstrap.min.js"></script>
    <script src="../carbon/appfactory/lib/bootstrap/assets/js/bootstrap-modal.js"></script>

    </fmt:bundle>

  </body>
</html>
