/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru.util;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.nhttp.util.MessageFormatterDecoratorFactory;
import org.apache.synapse.transport.nhttp.util.NhttpUtil;
import org.apache.synapse.transport.passthru.SourceRequest;
import org.apache.synapse.transport.passthru.SourceResponse;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SourceResponseFactory {

    public static SourceResponse create(MessageContext msgContext,
                                        SourceRequest sourceRequest,
                                        SourceConfiguration sourceConfiguration) {
        // determine the status code to be sent
        int statusCode = PassThroughTransportUtils.determineHttpStatusCode(msgContext);

        SourceResponse sourceResponse =
                new SourceResponse(sourceConfiguration, statusCode, sourceRequest);

        // set any transport headers
        Map transportHeaders = (Map) msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);

        if (transportHeaders != null) {
            addResponseHeader(sourceResponse, transportHeaders);
        }else{
        	  Boolean noEntityBody = (Boolean) msgContext.getProperty(NhttpConstants.NO_ENTITY_BODY);
        	 if (noEntityBody == null || Boolean.FALSE == noEntityBody) {
        		 OMOutputFormat format = NhttpUtil.getOMOutputFormat(msgContext);
        		 transportHeaders = new HashMap();
            	 MessageFormatter messageFormatter =
                     MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
            	 transportHeaders.put(HTTP.CONTENT_TYPE, messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));
            	 addResponseHeader(sourceResponse, transportHeaders);
             }
        	 
        }

        return sourceResponse;
    }

	private static void addResponseHeader(SourceResponse sourceResponse, Map transportHeaders) {
	    for (Object entryObj : transportHeaders.entrySet()) {
	        Map.Entry entry = (Map.Entry) entryObj;
	        if (entry.getValue() != null && entry.getKey() instanceof String &&
	                entry.getValue() instanceof String) {
	            sourceResponse.addHeader((String) entry.getKey(), (String) entry.getValue());
	        }
	    }
    }
    
}
