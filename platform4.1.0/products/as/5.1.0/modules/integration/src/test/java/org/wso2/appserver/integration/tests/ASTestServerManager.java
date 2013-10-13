/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.appserver.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.wso2.carbon.integration.framework.TestServerManager;
import org.wso2.carbon.utils.FileManipulator;

import java.io.File;
import java.io.IOException;

/**
 * Prepares the WSO2 AS for test runs, starts the server, and stops the server after
 * test runs
 */
public class ASTestServerManager extends TestServerManager {
    private static final Log log = LogFactory.getLog(ASTestServerManager.class);

    @Override
    @BeforeSuite(timeOut = 300000)
    public String startServer() throws IOException {
        String carbonHome = super.startServer();
        System.setProperty("carbon.home", carbonHome);
        return carbonHome;
    }

    @Override
    @AfterSuite(timeOut = 60000)
    public void stopServer() throws Exception {
        super.stopServer();
    }

    protected void copyArtifacts(String carbonHome) throws IOException {

        // HelloWorld sample
        String fileName = "HelloWorld.aar";
        String sourcePath = computeSourcePath("HelloWorld", fileName);
        String destPath = computeDestPath(carbonHome, "axis2services", fileName);
        copySampleFile(sourcePath, destPath);
        log.info("Copying "+ sourcePath + " to " + destPath);

        // CommodityQuote sample
        fileName = "CommodityQuoteService.aar";
        sourcePath = computeSourcePath("CommodityQuote", fileName);
        destPath = computeDestPath(carbonHome, "axis2services", fileName);
        copySampleFile(sourcePath, destPath);
        log.info("Copying "+ sourcePath + " to " + destPath);

        // JSON Sample
        fileName = "JSONService.aar";
        sourcePath = computeSourcePath("JSON", fileName);
        destPath = computeDestPath(carbonHome, "axis2services", fileName);
        copySampleFile(sourcePath, destPath);
        log.info("Copying "+ sourcePath + " to " + destPath);

        // JAXWS Sample
        fileName = "java_first_jaxws.war";
        sourcePath = computeSourcePath("Jaxws-Jaxrs" + File.separator +
                "java_first_jaxws", fileName);
        destPath = computeDestPath(carbonHome, "jaxwebapps", fileName);
        copySampleFile(sourcePath, destPath);
        log.info("Copying "+ sourcePath + " to " + destPath);

        //Shopping Cart Sample
        fileName = "ShoppingCartSample.car";
        sourcePath = getClass().getClassLoader().getResource(fileName).getFile();
        destPath = computeDestPath(carbonHome, "carbonapps", fileName);
        copySampleFile(sourcePath, destPath);
        log.info("Copying "+ sourcePath + " to " + destPath);
        
     // MS samples coping
		// Mashup sample for Email Host Object
		fileName = "emailTest.js";
		sourcePath = computeMSSourcePath(fileName);
		String destinationPath = computeMSDestPath(carbonHome, fileName);
		copySampleFile(sourcePath, destinationPath);
		log.info("coping mashup test cases");
		// Mashup sample for File Host Object
		fileName = "fileTest.js";
		sourcePath = computeMSSourcePath(fileName);
		destinationPath = computeMSDestPath(carbonHome, fileName);
		copySampleFile(sourcePath, destinationPath);

		// Mashup sample for Session Host Object
		fileName = "sessionTest.js";
		sourcePath = computeMSSourcePath(fileName);
		destinationPath = computeMSDestPath(carbonHome, fileName);
		copySampleFile(sourcePath, destinationPath);

		// Mashup sample for Request Host Object
		fileName = "requestTest.js";
		sourcePath = computeMSSourcePath(fileName);
		destinationPath = computeMSDestPath(carbonHome, fileName);
		copySampleFile(sourcePath, destinationPath);

		// Mashup sample for Scrapper Host Object
		fileName = "scrapperTest.js";
		sourcePath = computeMSSourcePath(fileName);
		destinationPath = computeMSDestPath(carbonHome, fileName);
		copySampleFile(sourcePath, destinationPath);

		// Mashup sample for System Host Object
		fileName = "systemTest.js";
		sourcePath = computeMSSourcePath(fileName);
		destinationPath = computeMSDestPath(carbonHome, fileName);
		copySampleFile(sourcePath, destinationPath);
		fileName = "concatscript.js";
		sourcePath = computeMSSourcePath(fileName);
		destinationPath =
		                  computeMSDestPath(carbonHome, "systemTest.resources" + File.separator +
		                                              fileName);
		copySampleFile(sourcePath, destinationPath);

		// Mashup sample for HttpClient Host Object
		fileName = "httpClientTest.js";
		sourcePath = computeMSSourcePath(fileName);
		destinationPath = computeMSDestPath(carbonHome, fileName);
		copySampleFile(sourcePath, destinationPath);
		
		//jaggery samples 
		log.info("coping jaggery test cases");
		  // Copying jaggery configuration file
        fileName = "jaggery.conf";
        sourcePath = computeJaggerySourcePath(fileName);
        destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //email host object
    	fileName = "email.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //database host object
    	fileName = "database.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //feed host object
    	fileName = "feed.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //file host object
    	fileName = "file.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //sample file to read
    	fileName = "testfile.txt";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //log host object
    	fileName = "log.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //wsrequest host object
    	fileName = "wsrequest.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //request object
    	fileName = "request.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //response object
    	fileName = "response.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //session object
    	fileName = "session.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //application object
    	fileName = "application.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //xmlhttprequest object
    	fileName = "xmlhttprequest.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //require object
    	fileName = "require.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //syntax object
    	fileName = "syntax.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //resources for test 
    	fileName = "testhtml.html";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //jsonTest
    	fileName = "jsonTest.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
        //http client object tests
    	fileName = "get.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
    	fileName = "post.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
    	fileName = "put.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
    	fileName = "delet.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
    	fileName = "uri.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
    	fileName = "inculde.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
    	fileName = "entry.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
        
    	fileName = "wsstub.jag";
    	sourcePath = computeJaggerySourcePath(fileName);
    	destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);
		
		fileName = "metadata.jag";
        sourcePath = computeJaggerySourcePath(fileName);
        destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);

        fileName = "resources.jag";
        sourcePath = computeJaggerySourcePath(fileName);
        destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);

        fileName = "collection.jag";
        sourcePath = computeJaggerySourcePath(fileName);
        destinationPath = computeJaggeryDestPath(carbonHome, fileName);
        copySampleFile(sourcePath, destinationPath);

		
    }

    private void copySampleFile(String sourcePath, String destPath) {
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        try {
            FileManipulator.copyFile(sourceFile, destFile);
        } catch (IOException e) {
            log.error("Error while copying the HelloWorld sample into AppServer", e);
        }
    }

    private String computeSourcePath(String sampleFolder, String fileName) {
        String samplesDir = System.getProperty("samples.dir");
        return samplesDir + File.separator + sampleFolder + File.separator
               + "target" + File.separator + fileName;
    }

    private String computeDestPath(String carbonHome,
                                   String deploymentFolder,
                                   String fileName) {
        // First create the deployment folder in the server if it doesn't already exist
        String deploymentPath = carbonHome + File.separator + "repository" + File.separator
                                + "deployment" + File.separator + "server" + File.separator +
                                deploymentFolder;
        File depFile = new File(deploymentPath);
        if (!depFile.exists() && !depFile.mkdir()) {
            log.error("Error while creating the deployment folder : " + deploymentPath);
        }
        return deploymentPath + File.separator + fileName;
    }
    
 // for MS samples

	private void copyMSSampleFile(String sourcePath, String destPath) {
		File sourceFile = new File(sourcePath);
		File destFile = new File(destPath);
		try {
			FileManipulator.copyFile(sourceFile, destFile);
		} catch (IOException e) {
			log.error("Error while copying the mashup sample into Application server", e);
		}
	}

	private String computeMSSourcePath(String fileName) {
		String samplesDir = System.getProperty("ms.samples.dir");
		return samplesDir + File.separator + fileName;
	}

	private String computeMSDestPath(String carbonHome, String fileName) {
		String deploymentPath =
		                        carbonHome + File.separator + "repository" + File.separator +
		                                "deployment" + File.separator + "server" + File.separator +
		                                "jsservices" + File.separator + "admin";
		File depFile = new File(deploymentPath);
		if (!depFile.exists() && !depFile.mkdir()) {
			log.error("Error while creating the deployment folder : " + deploymentPath);
		}
		return deploymentPath + File.separator + fileName;
	}
	
	//for jaggery sample dir
	
	 private void copyJaggerySampleFile(String sourcePath, String destPath) {
	        File sourceFile = new File(sourcePath);
	        File destFile = new File(destPath);
	        try {
	            FileManipulator.copyFile(sourceFile, destFile);	           
	        } catch (IOException e) {
	            log.error("Error while copying the Jaggery sample into  App server", e);
	        }
	    }

	    private String computeJaggerySourcePath(String fileName) {
	        String samplesDir = System.getProperty("jaggery.samples.dir");
	        return samplesDir + File.separator + fileName;
	        
	    }

	    private String computeJaggeryDestPath(String carbonHome, String fileName) {
	    	String deploymentPath =
                carbonHome + File.separator + "repository" + File.separator +
                        "deployment" + File.separator + "server" + File.separator +
                        "jaggeryapps" + File.separator + "testapp";
	    	
	    	File depFile = new File(deploymentPath);
	        if (!depFile.exists() && !depFile.mkdir()) {
	            log.error("Error while creating the deployment folder : " + deploymentPath);
	        }
	        return deploymentPath + File.separator + fileName;
	    }
}
