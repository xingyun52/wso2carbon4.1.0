WSO2 Application Server ${appserver.version}
---------------

${buildNumber}

Welcome to the WSO2 Application Server ${appserver.version} release

 The WSO2 Application Server is an enterprise-ready cloud-enabled application server, powered by Apache Tomcat and also integrates Apache Axis2 and Apache CXF frameworks. It provides first class support for standard Web applications, JAX-WS/RS applications and Jaggery scripting applications. Coupled with features of WSO2 Carbon, users can now manage their applications including JAX-WS and JAX-RS to web applications in a unified manner within the management console itself.

 Application Server also provides a comprehensive Web services server platform, using Axis2 and CXF as its Web services runtimes and provide many value additions on top of service runtime. It can expose services using both SOAP and REST models and supports a comprehensive set of WS-* specifications such as WS-Security, WS-Trust, WS-SecureConversation, WS-Reliable Messaging, WS-Addressing, WS-Policy, WS-SecurityPolicy, etc. WSO2 Application Server also has inbuilt support for Mashup services and WSO2 Data services. WSO2 Application Server can be installed on on-premise or any public/private cloud infrastructure and provide unified management console and lifestyle management features which are independent from underlying deployment option.

WSO2 Application Server is a part of WSO2 Stratos cloud platform (PaaS) and hosted offering of WSO2 Application Server as a service  can be accessed from here https://appserver.stratoslive.wso2.com.


New Features In This Release
----------------------------

* New classloader environment customization architecture which can configure classloader per server or per web application.
* User defined classloaders environments.
* Upgraded Tomcat runtime into Tomcat 7.0.34.
* In built support for Servlet -3, JSP 2.2, EL 2.2, JSTL 1.2 specifications.
* Full JNDI based data source support.
* Enhanced service dashboard page for JAX-WS and JAX-RS services.
* Try-It feature for JAX-WS services.
* Maven based client side code generation support for JAX-WS, JAX-RS services.
* Maven based new code generation feature for Axis2 services.
* Upgraded CXF runtime into 2.7.3.


Key Features
------------
* Full JAX-WS 2.2 and JAX-RS 2.0 Specification support
* Integration of Jaggery - server side scripting framework
* Unified Application listing and management UI for WebApps, JAX-WS/RS, Jaggery
* Inbuilt Mashup services support
* Multi Tenant support for standalone deployment
* 100% Apache Tomcat compliance runtime
   - Added support for server descriptor file (server.xml) for embedded tomcat
   - Added support for WebApp specific context descriptor file (context.xml) for webapps
* Lazy loading for applications and services
* Tooling - Application Server related artifacts can be easily generated using WSO2 Developer Studio
* Clustering support for High Availability &amp; High Scalability
* Full support for WS-Security, WS-Trust, WS-Policy and WS-Secure Conversation
* JMX &amp; Web interface based monitoring and management
* WS-* &amp; REST support
* GUI, command line &amp; IDE based tools for Web service development
* Equinox P2 based provisioning support
* WSDL2Java/Java2WSDL/WSDL 1.1 &amp; try it(invoke any remote Web service)

Issues Fixed in This Release
----------------------------

* Application Server related components of the WSO2 Carbon Platform -
       https://wso2.org/jira/secure/IssueNavigator.jspa?mode=hide&requestId=10910

Installation & Running
----------------------
1. Extract the downloaded zip file
2. Run the wso2server.sh or wso2server.bat file in the bin directory
3. Once the server starts, point your Web browser to
   https://localhost:9443/carbon/

For more details, see the Installation Guide

System Requirements
-------------------

1. Minimum memory - 1 GB
2. Processor      - Pentium 800MHz or equivalent at minimum
3. The Management Console requires full Javascript enablement of the Web browser

For more details see the Installation guide or,
http://docs.wso2.org/wiki/display/AS510/Installation+Prerequisites

Known Issues in This Release
----------------------------

* Application Server related components of the WSO2 Carbon Platform -
       https://wso2.org/jira/secure/IssueNavigator.jspa?mode=hide&requestId=10951

Including External Dependencies
--------------------------------
For a complete guide on adding external dependencies to WSO2 Application Server & other carbon related products refer to the article:
http://wso2.org/library/knowledgebase/add-external-jar-libraries-wso2-carbon-based-products

Application Server Binary Distribution Directory Structure
--------------------------------------------

     CARBON_HOME
        |-- bin <directory>
        |-- dbscripts <directory>
        |-- lib <directory>
             `-- runtimes <directory>
		   |-- cxf <directory>
		   `-- ext <directory>
        |-- repository <directory>
        |   |-- carbonapps <directory>
        |   |-- components <directory>
        |   |-- conf <directory>
        |   |-- data <directory>
        |   |-- database <directory>
        |   |-- deployment <directory>
        |   |-- lib <directory>
        |   |-- logs <directory>
        |   |-- resources <directory>
        |   |   `-- security <directory>
        |   `-- tenants <directory>
        |-- tmp <directory>
	    |-- webapp-mode <directory>
        |-- LICENSE.txt <file>
        |-- README.txt <file>
        |-- INSTALL.txt <file>
        `-- release-notes.html <file>

    - bin
      Contains various scripts .sh & .bat scripts.

    - dbscripts
      Contains the database creation & seed data population SQL scripts for
      various supported databases.

    - lib
      Contains the basic set of libraries required to startup Application Server
      in standalone mode

    - repository
      The repository where Carbon artifacts & Axis2 services and
      modules deployed in WSO2 Carbon are stored.
      In addition to this other custom deployers such as
      dataservices and axis1services are also stored.

        - carbonapps
          Carbon Application hot deployment directory.

    	- components
          Contains all OSGi related libraries and configurations.

        - conf
          Contains server configuration files. Ex: axis2.xml, carbon.xml

        - data
          Contains internal LDAP related data.

        - database
          Contains the WSO2 Registry & User Manager database.

        - deployment
          Contains server side and client side Axis2 repositories.
	      All deployment artifacts should go into this directory.

        - logs
          Contains all log files created during execution.

        - resources
          Contains additional resources that may be required.

	- tenants
	  Directory will contain relevant tenant artifacts
	  in the case of a multitenant deployment.

    - tmp
      Used for storing temporary files, and is pointed to by the
      java.io.tmpdir System property.

    - webapp-mode
      The user has the option of running WSO2 Carbon in webapp mode (hosted as a web-app in an application server).
      This directory contains files required to run Carbon in webapp mode.

    - LICENSE.txt
      Apache License 2.0 under which WSO2 Carbon is distributed.

    - README.txt
      This document.

    - INSTALL.txt
      This document contains information on installing WSO2 Application Server.

    - release-notes.html
      Release information for WSO2 Application Server ${appserver.version}

Secure sensitive information in carbon configuration files
----------------------------------------------------------

There are sensitive information such as passwords in the carbon configuration.
You can secure them by using secure vault. Please go through following steps to
secure them with default mode.

1. Configure secure vault with default configurations by running ciphertool
	script from bin directory.

> ciphertool.sh -Dconfigure   (in UNIX)

This script would do following configurations that you need to do by manually

(i) Replaces sensitive elements in configuration files,  that have been defined in
		 cipher-tool.properties, with alias token values.
(ii) Encrypts plain text password which is defined in cipher-text.properties file.
(iii) Updates secret-conf.properties file with default keystore and callback class.

cipher-tool.properties, cipher-text.properties and secret-conf.properties files
			can be found at repository/conf/security directory.

2. Start server by running wso2server script from bin directory

> wso2server.sh   (in UNIX)

By default mode, it would ask you to enter the master password
(By default, master password is the password of carbon keystore and private key)

3. Change any password by running ciphertool script from bin directory.

> ciphertool -Dchange  (in UNIX)

For more details see
http://docs.wso2.org/wiki/display/Carbon410/WSO2+Carbon+Secure+Vault

Training
--------

WSO2 Inc. offers a variety of professional Training Programs, including
training on general Web services as well as WSO2 Application Server, Apache Axis2,
Data Services and a number of other products.

For additional support information please refer to
http://wso2.com/training/


Support
-------

We are committed to ensuring that your enterprise middleware deployment is completely supported
from evaluation to production. Our unique approach ensures that all support leverages our open
development methodology and is provided by the very same engineers who build the technology.

For additional support information please refer to http://wso2.com/support/

For more information on WSO2 Application Server, visit the WSO2 Oxygen Tank (http://wso2.org)

Crypto Notice
-------------

This distribution includes cryptographic software.  The country in
which you currently reside may have restrictions on the import,
possession, use, and/or re-export to another country, of
encryption software.  Before using any encryption software, please
check your country's laws, regulations and policies concerning the
import, possession, or use, and re-export of encryption software, to
see if this is permitted.  See <http://www.wassenaar.org/> for more
information.

The U.S. Government Department of Commerce, Bureau of Industry and
Security (BIS), has classified this software as Export Commodity
Control Number (ECCN) 5D002.C.1, which includes information security
software using or performing cryptographic functions with asymmetric
algorithms.  The form and manner of this Apache Software Foundation
distribution makes it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception (see the BIS
Export Administration Regulations, Section 740.13) for both object
code and source code.

The following provides more details on the included cryptographic
software:

Apacge Rampart   : http://ws.apache.org/rampart/
Apache WSS4J     : http://ws.apache.org/wss4j/
Apache Santuario : http://santuario.apache.org/
Bouncycastle     : http://www.bouncycastle.org/


For further details, see the WSO2 Application Server documentation at
http://docs.wso2.org/wiki/application-server-documentation

---------------------------------------------------------------------------
(c) Copyright 2012 WSO2 Inc.
