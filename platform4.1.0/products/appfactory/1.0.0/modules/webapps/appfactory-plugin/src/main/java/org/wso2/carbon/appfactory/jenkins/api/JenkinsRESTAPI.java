/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.appfactory.jenkins.api;

import hudson.model.Hudson;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.jenkins.AppfactoryPluginManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Implementation of {@link JenkinsAPI} to access apis through REST calls.
 * 
 * @author shamika
 * 
 */
public class JenkinsRESTAPI implements JenkinsAPI {

	private static final Log log = LogFactory.getLog(JenkinsRESTAPI.class);

	private HttpClient client = null;
	
	private static AppfactoryPluginManager.DescriptorImpl descriptor = new AppfactoryPluginManager.DescriptorImpl();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.wso2.carbon.appfactory.jenkins.api.JenkinsAPI#
	 * buildInfoMapgetLastBuildInformation()
	 */
	public Map<String, String> getLastBuildInformation(String jobName)
	                                                                              throws JenkinsAPIException {
		String url = Hudson.getInstance().getRootUrlFromRequest() + "job/" + jobName + "/api/json";
		log.info("Calling jenkins api : " + url);
		GetMethod get = new GetMethod(url);
		NameValuePair valuePair =
		                          new NameValuePair("tree",
		                                            "builds[number,status,timestamp,id,result]");
		get.setQueryString(new org.apache.commons.httpclient.NameValuePair[] { valuePair });

		getHttpClient().getState()
		               .setCredentials(AuthScope.ANY,
		                               new UsernamePasswordCredentials(descriptor.getAdminUserName(), descriptor.getAdminPassword()));
		getHttpClient().getParams().setAuthenticationPreemptive(true);

		Map<String, String> buildInformarion = null;

		try {
			log.debug("Retrieving last build information for job : " + jobName);
			getHttpClient().executeMethod(get);
			log.info("Retrieving last build information for job : " + jobName +
			         " status received : " + get.getStatusCode());
			if (get.getStatusCode() == HttpStatus.SC_OK) {
				String response = get.getResponseBodyAsString();
				log.debug("Returns build information for job : " + jobName + " - " + response);
				buildInformarion = extractBuildInformarion(response);
			} else {
				String msg =
				             "Error while retrieving  build information for job : " + jobName +
				                     " Jenkins returned status code : " + get.getStatusCode();
				log.error(msg);
				throw new JenkinsAPIException(msg, JenkinsAPIException.INVALID_RESPONSE);
			}

		} catch (HttpException e) {
			String msg = "Error occuered while calling the API";
			throw new JenkinsAPIException(msg);

		} catch (IOException e) {
			String msg = "Error occuered while calling the API";
			throw new JenkinsAPIException(msg);
		} finally {
			get.releaseConnection();
		}
		return buildInformarion;
	}

	/**
	 * Extracts last build information from given JSON response.
	 * 
	 * @param response
	 * @return Last Build info as a map
	 *         null - if no last build is available.
	 */
	private Map<String, String> extractBuildInformarion(String response) {
		Gson gson = new Gson();
		Map<String, List<Map<String, String>>> buildInfoMap =
		                                                      gson.fromJson(response,
		                                                                    new TypeToken<Map<String, List<Map<String, String>>>>() {
		                                                                    }.getType());
		List<Map<String, String>> buildList = buildInfoMap.get("builds");
		if (buildList.size() > 0) {
			return buildList.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Returns initiated http client.
	 * 
	 * @return {@link HttpClient}
	 */
	private HttpClient getHttpClient() {

		if (client == null) {
			client = new HttpClient();
		}
		return client;
	}

	/**
	 * Method will be used to inject HTTPClient
	 * Eg:- Mocking purposes.
	 * 
	 * @param client
	 */
	void setHttpClient(HttpClient client) {
		this.client = client;
	}

}
