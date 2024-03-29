WSO2 Business Activity Monitor, v2.2.0
================================

11th February 2012

Welcome to the WSO2 Business Activity Monitor, v2.2.0 release

WSO2 Business Activity Monitor (WSO2 BAM) is a comprehensive framework designed to solve the problems in the wide area of business activity monitoring. WSO2 BAM comprises of many modules to give the best of performance, scalability and customizability. This allows to achieve requirements of business users, dev ops, CEOs without spending countless months on customizing the solution without sacrificing performance or the ability to scale. 

WSO2 BAM is powered by WSO2 Carbon, the SOA middleware component
platform. 


New Features
============
* Data archival support
	This functionality can be used to archive your Cassandra data, and archiving process use map-reduce jobs to achive the task, therefore it supports for archiving 		large amount of data  using cluster of hadoop nodes. You can run your archive process manually or you can schedule it using a cron expression.
* Multi tenancy support for all BAM features
	You can add a tenant in BAM server and you can obtain the Multi Tenant functionality by following some easy steps as mentioned in the documentation. Multi tenancy   		in BAM has tenant seperation in the sense of Data storage in cassandra, Hive space, seperate tenant data recieving, etc, with high performance.
* Undefined data support for BAM in data publisher and hive
	You can send additionally a <String, String> Map, which holds key/value pairs with your payload. This enables to send some more data other than what you have 		defined in your stream definition. And Hive support for analyze the data received in map is also added.
* Load balancing and High availability for all data-agents
	You can load balance and achive high availability with BAM in all data agents. This enables to load balance within a set of servers, and send multiple servers same 		event at a time, so that high availability is possible in this feature. 
* JMX Agent and JMX toolbox
	The JMX agent for BAM is used to monitor JMX attributes of a required JMX enabled server (Ex- Carbon based servers) and store the monitored data in BAM. The user 		can install the JMX agent either on a BAM server or any Carbon based server. The monitored data is sent to the BAM server using the Thrift API of BAM. 


Features
========

* Data Agents
        A re-usable Agent API to publish events to the BAM server from any application (samples included)
        Apache Thrift based Agents to publish data at extremely high throughput rates
* Event Storage
        Apache Cassandra based scalable data architecture for high throughput of writes and reads
        Carbon based security mechanism on top of Cassandra
* Analytics
        An Analyzer Framework with the capability of Writing the the Hive Scripts and run Hadoop Jobs, which enables provides more performance in big data. 
	Capability for read/write from cassandra and read/write in JDBC database.
	This has the capability of writing and plugging in any custom analysis tasks
	Scheduling capability of analysis tasks 
* Visualization
        Step-by-Step Easy Gadget Generation Wizard
        Jaggery based gadgets generation
	Capability use with most commonly used data sources, such as MySQL, H2, etc.
	Google gadgets based dashboard
* Tool Box Deployement
	Easy deployment of default scenarios
	Bundled required hive scripts and gadgets and deploy and undeploys with a click go.
	capability to deploy custom toolboxes
* Tool Boxes
    Service stats toolbox
    Mediation statistic toolbox
    Activity Monitoring toolbox



Issues Fixed in This Release
============================
* WSO2 Business Activity Monitor related components of the WSO2 Carbon Platform - https://wso2.org/jira/secure/IssueNavigator.jspa?mode=hide&requestId=10921

Known Issues in This Release
============================

All known issues have been recorded at https://wso2.org/jira/secure/IssueNavigator.jspa?mode=hide&requestId=10920

Installation & Running
======================
1. Extract the downloaded zip file
2. Run the wso2server.sh or wso2server.bat file in the bin directory
3. Once the server starts, point your Web browser to
   https://localhost:9443/carbon/

For more details, see the Installation Guide

System Requirements
===================

1. Minimum memory - 1 GB
2. Processor      - Core i5 2.7 GHz or equivalent at minimum
3. The Management Console requires full Javascript enablement of the Web browser

For more details see the Installation guide or,
http://docs.wso2.org/wiki/display/BAM200/Installation+Prerequisites

Including External Dependencies
===============================
For a complete guide on adding external dependencies to WSO2 Application Server & other carbon related products refer to the article:
http://wso2.org/library/knowledgebase/add-external-jar-libraries-wso2-carbon-based-products

WSO2 Business Activity Monitor Binary Distribution Directory Structure
======================================================
     CARBON_HOME
        |-- bin <directory>
        |-- dbscripts <directory>
        |-- lib <directory>
        |-- samples <directory>
        |   |-- toolboxes <directory>
        |-- repository <directory>
        |   |-- carbonapps <directory>
        |   |-- components <directory>
        |   |-- conf <directory>
        |   |-- data <directory>
        |   |-- database <directory>
        |   |-- deployment <directory>
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

	- samples
          Contains the samples which describes the usage and fetures of 
	  WSO2 Business Activity Monitor. This includes four samples: 

		- toolboxes
		  Complete toolboxes containing artifacts for the use cases of BAM

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
      Release information for WSO2 Application Server 2.0.1

Secure sensitive information in carbon configuration files
==========================================================

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
http://docs.wso2.org/wiki/display/Carbon400/WSO2+Carbon+Secure+Vault


Training
========

WSO2 Inc. offers a variety of professional Training Programs, including
training on general Web services as well as WSO2 Business Activity Monitor and number of 
other products.

For additional support information please refer to
http://wso2.com/training/


Support
=======

We are committed to ensuring that your enterprise middleware deployment is completely supported
from evaluation to production. Our unique approach ensures that all support leverages our open
development methodology and is provided by the very same engineers who build the technology.

For additional support information please refer to http://wso2.com/support/

For more information on WSO2 Application Server, visit the WSO2 Oxygen Tank (http://wso2.org)

Crypto Notice
=============

   This distribution includes cryptographic software.  The country in
   which you currently reside may have restrictions on the import,
   possession, use, and/or re-export to another country, of
   encryption software.  BEFORE using any encryption software, please
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

For further details, see the WSO2 Carbon documentation at
http://wso2.org/wiki/display/carbon/2.0

---------------------------------------------------------------------------
(c)  @copyright.year@, WSO2 Inc.


