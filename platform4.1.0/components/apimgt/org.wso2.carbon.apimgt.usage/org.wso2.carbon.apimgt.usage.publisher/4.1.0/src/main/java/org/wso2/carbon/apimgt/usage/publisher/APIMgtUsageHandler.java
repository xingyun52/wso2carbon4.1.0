/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.apimgt.usage.publisher;

import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.AbstractHandler;
import org.apache.synapse.rest.RESTConstants;
import org.wso2.carbon.apimgt.gateway.handlers.security.APISecurityUtils;
import org.wso2.carbon.apimgt.gateway.handlers.security.AuthenticationContext;
import org.wso2.carbon.apimgt.usage.publisher.dto.RequestPublisherDTO;
import org.wso2.carbon.apimgt.usage.publisher.dto.ResponsePublisherDTO;
import org.wso2.carbon.apimgt.usage.publisher.internal.UsageComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class APIMgtUsageHandler extends AbstractHandler {

    private static final Log log   = LogFactory.getLog(APIMgtUsageHandler.class);

    private volatile APIMgtUsageDataPublisher publisher;

    private boolean enabled = UsageComponent.getApiMgtConfigReaderService().isEnabled();

    private String publisherClass = UsageComponent.getApiMgtConfigReaderService().getPublisherClass();

    public boolean handleRequest(MessageContext mc) {
        long currentTime = System.currentTimeMillis();

        if (!enabled) {
            return true;
        }

        if (publisher == null) {
            synchronized (this){
                if (publisher == null) {
                    try {
                        log.debug("Instantiating Data Publisher");
                        publisher = (APIMgtUsageDataPublisher)Class.forName(publisherClass).newInstance();
                        publisher.init();
                    } catch (ClassNotFoundException e) {
                        log.error("Class not found " + publisherClass);
                    } catch (InstantiationException e) {
                        log.error("Error instantiating " + publisherClass);
                    } catch (IllegalAccessException e) {
                        log.error("Illegal access to " + publisherClass);
                    }
                }
            }
        }

        AuthenticationContext authContext = APISecurityUtils.getAuthenticationContext(mc);
        String consumerKey = "";
        String username = "";
        if (authContext != null) {
            consumerKey = authContext.getApiKey();
            username = authContext.getUsername();
        }
        String context = (String)mc.getProperty(RESTConstants.REST_API_CONTEXT);
        String api_version =  (String)mc.getProperty(RESTConstants.SYNAPSE_REST_API);
        int index = api_version.indexOf("--");
        if (index != -1) {
            api_version = api_version.substring(index + 2);
        }

        String api = api_version.split(":")[0];
        index = api.indexOf("--");
        if (index != -1) {
            api = api.substring(index + 2);
        }
        String version = (String)mc.getProperty(RESTConstants.SYNAPSE_REST_API_VERSION);
        String resource = extractResource(mc);
        String method =  (String)((Axis2MessageContext) mc).getAxis2MessageContext().getProperty(
                Constants.Configuration.HTTP_METHOD);

        RequestPublisherDTO requestPublisherDTO = new RequestPublisherDTO();
        requestPublisherDTO.setConsumerKey(consumerKey);
        requestPublisherDTO.setContext(context);
        requestPublisherDTO.setApi_version(api_version);
        requestPublisherDTO.setApi(api);
        requestPublisherDTO.setVersion(version);
        requestPublisherDTO.setResource(resource);
        requestPublisherDTO.setMethod(method);
        requestPublisherDTO.setRequestTime(currentTime);
        requestPublisherDTO.setUsername(username);
        try {
            publisher.publishEvent(requestPublisherDTO);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        mc.setProperty(APIMgtUsagePublisherConstants.CONSUMER_KEY, consumerKey);
        mc.setProperty(APIMgtUsagePublisherConstants.USER_ID, username);
        mc.setProperty(APIMgtUsagePublisherConstants.CONTEXT, context);
        mc.setProperty(APIMgtUsagePublisherConstants.API_VERSION, api_version);
        mc.setProperty(APIMgtUsagePublisherConstants.API, api);
        mc.setProperty(APIMgtUsagePublisherConstants.VERSION, version);
        mc.setProperty(APIMgtUsagePublisherConstants.RESOURCE, resource);
        mc.setProperty(APIMgtUsagePublisherConstants.HTTP_METHOD, method);
        mc.setProperty(APIMgtUsagePublisherConstants.REQUEST_TIME, currentTime);

        return true;
    }

    public boolean handleResponse(MessageContext mc) {
        Long currentTime = System.currentTimeMillis();

        if (!enabled) {
            return true;
        }

        Long serviceTime = currentTime - (Long) mc.getProperty(APIMgtUsagePublisherConstants.REQUEST_TIME);

        ResponsePublisherDTO responsePublisherDTO = new ResponsePublisherDTO();
        responsePublisherDTO.setConsumerKey((String)mc.getProperty(APIMgtUsagePublisherConstants.CONSUMER_KEY));
        responsePublisherDTO.setUsername((String)mc.getProperty(APIMgtUsagePublisherConstants.USER_ID));
        responsePublisherDTO.setContext((String) mc.getProperty(APIMgtUsagePublisherConstants.CONTEXT));
        responsePublisherDTO.setApi_version((String) mc.getProperty(APIMgtUsagePublisherConstants.API_VERSION));
        responsePublisherDTO.setApi((String) mc.getProperty(APIMgtUsagePublisherConstants.API));
        responsePublisherDTO.setVersion((String) mc.getProperty(APIMgtUsagePublisherConstants.VERSION));
        responsePublisherDTO.setResource((String) mc.getProperty(APIMgtUsagePublisherConstants.RESOURCE));
        responsePublisherDTO.setMethod((String)mc.getProperty(APIMgtUsagePublisherConstants.HTTP_METHOD));
        responsePublisherDTO.setResponseTime(currentTime);
        responsePublisherDTO.setServiceTime(serviceTime);
        try{
            publisher.publishEvent(responsePublisherDTO);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return true; // Should never stop the message flow
    }

    private String extractResource(MessageContext mc){
        String resource = "/";
        Pattern pattern = Pattern.compile("^/.+?/.+?([/?].+)$");
        Matcher matcher = pattern.matcher((String) mc.getProperty(RESTConstants.REST_FULL_REQUEST_PATH));
        if (matcher.find()){
            resource = matcher.group(1);
        }
        return resource;
    }

}
