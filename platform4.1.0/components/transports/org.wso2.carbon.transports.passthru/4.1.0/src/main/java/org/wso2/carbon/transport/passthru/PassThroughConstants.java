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

package org.wso2.carbon.transport.passthru;

public class PassThroughConstants {

    public static final int DEFAULT_IO_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    public static final int DEFAULT_MAX_CONN_PER_HOST_PORT = Integer.MAX_VALUE;
    
    public static final String REQUEST_MESSAGE_CONTEXT = "REQUEST_MESSAGE_CONTEXT";
    public static final String CONNECTION_POOL = "CONNECTION_POOL";

    public static final String TRUE = "TRUE";

    public static final String FAULT_MESSAGE = "FAULT_MESSAGE"; // corresponds with BaseConstants
    public static final String FAULTS_AS_HTTP_200 = "FAULTS_AS_HTTP_200";
    public static final String SC_ACCEPTED = "SC_ACCEPTED";
    public static final String HTTP_SC = "HTTP_SC";
    public static final String FORCE_HTTP_1_0 = "FORCE_HTTP_1.0";
    public static final String DISABLE_CHUNKING = "DISABLE_CHUNKING";
    public static final String FULL_URI = "FULL_URI";
    public static final String NO_KEEPALIVE = "NO_KEEPALIVE";
    public static final String DISABLE_KEEPALIVE = "http.connection.disable.keepalive";
    public static final String IGNORE_SC_ACCEPTED = "IGNORE_SC_ACCEPTED";
    public static final String FORCE_SC_ACCEPTED = "FORCE_SC_ACCEPTED";
    public static final String DISCARD_ON_COMPLETE = "DISCARD_ON_COMPLETE";

    public static final String SERVICE_URI_LOCATION = "ServiceURI";

    public static final String WSDL_EPR_PREFIX = "WSDLEPRPrefix";

    public static final String EPR_TO_SERVICE_NAME_MAP = "service.epr.map";
    public static final String NON_BLOCKING_TRANSPORT = "NonBlockingTransport";
    public static final String SERIALIZED_BYTES = "SerializedBytes";

    public static final String CONTENT_TYPE = "CONTENT_TYPE";

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    public static final String HIDDEN_SERVICE_PARAM_NAME = "hiddenService";

    /** An Axis2 message context property indicating a transport send failure */
    public static final String SENDING_FAULT = "SENDING_FAULT";
    /** The message context property name which holds the error code for the last encountered exception */
    public static final String ERROR_CODE = "ERROR_CODE";
    /** The MC property name which holds the error message for the last encountered exception */
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    /** The message context property name which holds the error detail (stack trace) for the last encountered exception */
    public static final String ERROR_DETAIL = "ERROR_DETAIL";
    /** The message context property name which holds the exception (if any) for the last encountered exception */
    public static final String ERROR_EXCEPTION = "ERROR_EXCEPTION";

    // ********** DO NOT CHANGE THESE UNLESS CORRESPONDING SYNAPSE CONSTANT ARE CHANGED ************

    public static final String REST_URL_POSTFIX = "REST_URL_POSTFIX";
    public static final String SERVICE_PREFIX = "SERVICE_PREFIX";
    public static final String NO_ENTITY_BODY = "NO_ENTITY_BODY";

    protected static final String PASS_THROUGH_TRANSPORT_WORKER_POOL =
            "PASS_THROUGH_TRANSPORT_WORKER_POOL";
    protected static final String PASS_THROUGH_SOURCE_CONFIGURATION =
            "PASS_THROUGH_SOURCE_CONFIGURATION";
    protected static final String PASS_THROUGH_SOURCE_CONNECTION = "pass-through.Source-Connection";
    protected static final String PASS_THROUGH_SOURCE_REQUEST = "pass-through.Source-Request";

    protected static final String PASS_THROUGH_TARGET_CONNECTION = "pass-through.Target-Connection";
    protected static final String PASS_THROUGH_TARGET_RESPONSE = "pass-through.Target-Response";

    protected static final String PASS_THROUGH_PIPE = "pass-through.pipe";

    // used to define the default content type as a parameter in the axis2.xml
    public static final String REQUEST_CONTENT_TYPE = "DEFAULT_REQUEST_CONTENT_TYPE";

    // This is a workaround  for  axis2 RestUtils behaviour
    public static final String REST_REQUEST_CONTENT_TYPE = "synapse.internal.rest.contentType";
    
    public static final String REQ_ARRIVAL_TIME = "REQ_ARRIVAL_TIME";
	public static final String REQ_DEPARTURE_TIME = "REQ_DEPARTURE_TIME";
	public static final String RES_ARRIVAL_TIME = "RES_ARRIVAL_TIME";
	public static final String RES_HEADER_ARRIVAL_TIME = "RES_HEADER_ARRIVAL_TIME";
	public static final String RES_DEPARTURE_TIME = "RES_DEPARTURE_TIME";
	
	public static final String CONF_LOCATION = "conf.location";
	
	public static final String LOCATION = "Location";
	
}
