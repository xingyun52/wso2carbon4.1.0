WSO2 Carbon ${carbon.version}
-----------------

${buildNumber}

Welcome to the WSO2 Carbon ${carbon.version} release

WSO2 Carbon is the base platform for all WSO2 products, powered by OSGi.  It is a
lightweight, high performing platform which is a collection of OSGi bundles. All
the major features which are included in WSO2 products have been developed as
pluggable Carbon components and installed into this base Carbon platform.

What's New In This Release
----------------------------
1. Enhanced Deployment Synchronizer
2. JDK 1.7 support
3. Tomcat 7
4. Equinox SDK 3.7
5. P2 Repository: Features grouped by product
6. Documentation Enhancements
7. Various bug fixes(https://wso2.org/jira/secure/IssueNavigator.jspa?mode=hide&requestId=10742) & enhancements including stabilizing WSO2 Carbon.


Key Features
------------
1. Bundling(OSGi) embedded version of tomcat and exposing it as standard HTTPService - This new architectural improvement allows web-apps to see 
   carbon server classpath and provides native CXF support etc.
2. Supporting standard Catalina-server.xml - Carbon now supports standard server.xml file that gets shipped with standalone tomcat-distribution. 
   Users who are already familiar with apache-tomcat configuration find it easy to work with WSO2 carbon products.
3. Latest Equinox-SDK - WSO2 carbon now embeds the latest Equinox SDK (3.7) as its OSGi runtime.
4. Improvements to feature manager functionality - It now allows you to install feature categories based on products. 
   If you want to make ESB out of Carbon, just install ESB feature category.
5. Persistence - Completely re-architected persistence layer that allows convenient artifact management across clusters.
6. Coordination: This component which is based on Apache ZooKeeper, brings distributed coordination support to the Carbon Core.
   It implements features such as distributed queues, barriers, group communication and leader election functionality. 
   This is mainly used in clustering setups for components such as scheduled tasks, data sources and message broker.
7. Security related improvements : Carbon user store is supported to operate with AD (Active directory) and AD LDS (Light-weight version of AD)
   with both read/write mode.
8. Registry improvements.


Installation & Running
----------------------
1. Extract the downloaded zip file
2. Run the wso2server.sh or wso2server.bat file in the bin directory
3. Once the server starts, point your Web browser to
   https://localhost:9443/carbon/

Hardware Requirements
-------------------
1. Minimum memory - 1GB
2. Processor      - Pentium 800MHz or equivalent at minimum

Software Requirements
-------------------
1. Java SE Development Kit - 1.6 (1.6.0_24 onwards)
2. Apache Ant - An Apache Ant version is required. Ant 1.7.0 version is recommended. 
3. The Management Console requires full Javascript enablement of the Web browser.

For more details see
http://docs.wso2.org/wiki/display/Carbon400/Installation+Prerequisites

Known Issues
------------

All known issues have been recorded at https://wso2.org/jira/browse/CARBON

Carbon Binary Distribution Directory Structure
--------------------------------------------

     CARBON_HOME
        |-- bin <directory>
        |-- dbscripts <directory>
        |-- lib <directory>
        |-- repository <directory>
        |   |-- components <directory>
        |   |-- conf <directory>
        |   |-- data <directory>
        |   |-- database <directory>
        |   |-- deployment <directory>
        |   |-- logs <directory>
        |   |-- resources <directory>
        |   |   |-- security <directory>
        |   |-- tenants <directory>
        |-- tmp <directory>
	|-- webapp-mode <directory>
        |-- LICENSE.txt <file>
        |-- README.txt <file>
        |-- INSTALL.txt <file>
        |-- release-notes.html <file>

    - bin
      Contains various scripts .sh & .bat scripts.

    - dbscripts
      Contains the database creation & seed data population SQL scripts for
      various supported databases.

    - lib
      Contains the basic set of libraries required to startup Carbon.

    - repository
      The repository where Carbon artifacts & Axis2 services and 
      modules deployed in WSO2 Carbon are stored. 
      In addition to this other custom deployers such as
      dataservices and axis1services are also stored.

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
      This document contains information on installing WSO2 Carbon.

    - release-notes.html
      Release information for WSO2 Carbon ${carbon.version}.

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

2. Start server by running wso2server sciprt from bin directory

> wso2server.sh   (in UNIX)

By default mode, it would ask you to enter the master password 
(By default, master password is the password of carbon keystore and private key) 

3. Change any password by running ciphertool script from bin directory.  

> ciphertool -Dchange  (in UNIX)

For more details see
http://docs.wso2.org/wiki/display/Carbon400/WSO2+Carbon+Secure+Vault

Support
-------

WSO2 Inc. offers a variety of development and production support
programs, ranging from Web-based support up through normal business
hours, to premium 24x7 phone support.

For additional support information please refer to http://wso2.com/support/

For more information on WSO2 Carbon, visit WSO2 Carbon Home Page (http://wso2.com/products/carbon)

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

Apache Rampart   : http://ws.apache.org/rampart/
Apache WSS4J     : http://ws.apache.org/wss4j/
Apache Santuario : http://santuario.apache.org/
Bouncycastle     : http://www.bouncycastle.org/

---------------------------------------------------------------------------
(c) Copyright 2012 WSO2 Inc.
