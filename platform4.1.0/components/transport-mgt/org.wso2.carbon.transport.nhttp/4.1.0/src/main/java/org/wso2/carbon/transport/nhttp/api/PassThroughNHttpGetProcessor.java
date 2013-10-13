/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.transport.nhttp.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axiom.util.blob.OverflowBlob;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.nhttp.NHttpConfiguration;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.HttpGetRequestProcessor;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.ProtocolState;
import org.apache.synapse.transport.passthru.SourceContext;
import org.apache.synapse.transport.passthru.SourceHandler;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.core.transports.CarbonHttpRequest;
import org.wso2.carbon.core.transports.CarbonHttpResponse;
import org.wso2.carbon.utils.ServerConstants;

/**
 * Get Processor implementation for ESB. This implementation will be using GetProcessors
 * defined in the Carbon core.
 */
public class PassThroughNHttpGetProcessor  implements HttpGetRequestProcessor{

	
    private Map<String, org.wso2.carbon.core.transports.HttpGetRequestProcessor> getRequestProcessors =
            new LinkedHashMap<String, org.wso2.carbon.core.transports.HttpGetRequestProcessor>();

    private ConfigurationContext cfgCtx;
    private SourceHandler sourceHandler;
    private static final QName ITEM_QN = new QName(ServerConstants.CARBON_SERVER_XML_NAMESPACE, "Item");
    private static final QName CLASS_QN = new QName(ServerConstants.CARBON_SERVER_XML_NAMESPACE, "Class");
    
  

    private static final Log log = LogFactory.getLog(PassThroughNHttpGetProcessor.class);

    private void populateGetRequestProcessors() throws AxisFault {
        try {
            OMElement docEle = XMLUtils.toOM(ServerConfiguration.getInstance().getDocumentElement());
            if (docEle != null) {
                SimpleNamespaceContext nsCtx = new SimpleNamespaceContext();
                nsCtx.addNamespace("wsas", ServerConstants.CARBON_SERVER_XML_NAMESPACE);
                XPath xp = new AXIOMXPath("//wsas:HttpGetRequestProcessors/wsas:Processor");
                xp.setNamespaceContext(nsCtx);
                List nodeList = xp.selectNodes(docEle);
                for (Object aNodeList : nodeList) {
                    OMElement processorEle = (OMElement) aNodeList;
                    OMElement itemEle = processorEle.getFirstChildWithName(ITEM_QN);
                    if (itemEle == null) {
                        throw new ServletException("Required element, 'Item' not found!");
                    }
                    OMElement classEle = processorEle.getFirstChildWithName(CLASS_QN);
                    org.wso2.carbon.core.transports.HttpGetRequestProcessor processor;
                    if (classEle == null) {
                        throw new ServletException("Required element, 'Class' not found!");
                    } else {
                        processor =
                                (org.wso2.carbon.core.transports.HttpGetRequestProcessor)
                                        Class.forName(classEle.getText().trim()).newInstance();
                    }
                    getRequestProcessors.put(itemEle.getText().trim(), processor);
                }
            }
        } catch (Exception e) {
            handleException("Error populating GetRequestProcessors", e);
        }
    }

    private void processWithGetProcessor(HttpRequest request,
                                         HttpResponse response,
                                         String requestUri,
                                         String requestUrl,
                                         String queryString,
                                         String item,
                                         OutputStream outputStream,
                                         NHttpServerConnection conn) throws Exception {
        OverflowBlob temporaryData = new OverflowBlob(256, 4048, "_nhttp", ".dat");
        try {
            CarbonHttpRequest carbonHttpRequest = new CarbonHttpRequest(
                    "GET", requestUri, requestUrl);

            String uri = request.getRequestLine().getUri();
            // setting the parameters for nhttp transport
            int pos = uri.indexOf("?");
            if (pos != -1) {
                StringTokenizer st = new StringTokenizer(uri.substring(pos + 1), "&");
                while (st.hasMoreTokens()) {
                    String param = st.nextToken();
                    pos = param.indexOf("=");
                    if (pos != -1) {
                        carbonHttpRequest.setParameter(
                                param.substring(0, pos), param.substring(pos + 1));
                    } else {
                        carbonHttpRequest.setParameter(param, null);
                    }
                }
            }

            carbonHttpRequest.setContextPath(cfgCtx.getServiceContextPath());
            carbonHttpRequest.setQueryString(queryString);

            CarbonHttpResponse carbonHttpResponse = new CarbonHttpResponse(
                    temporaryData.getOutputStream());

            (getRequestProcessors.get(item)).process(carbonHttpRequest,
                    carbonHttpResponse, cfgCtx);
            
             // adding headers
            Map<String, String> responseHeaderMap = carbonHttpResponse.getHeaders();
            for (Object key : responseHeaderMap.keySet()) {
                Object value = responseHeaderMap.get(key);
                response.addHeader(key.toString(), value.toString());
            }

            // setting status code
            response.setStatusCode(carbonHttpResponse.getStatusCode());

            // setting error codes
            if (carbonHttpResponse.isError()) {
                if (carbonHttpResponse.getStatusMessage() != null) {
                    response.setStatusLine(response.getProtocolVersion(),
                            carbonHttpResponse.getStatusCode(),
                            carbonHttpResponse.getStatusMessage());
                } else {
                    response.setStatusLine(response.getProtocolVersion(),
                            carbonHttpResponse.getStatusCode());
                }
            }

            if (carbonHttpResponse.isRedirect()) {
                response.addHeader("Location", carbonHttpResponse.getRedirect());
                response.setStatusLine(response.getProtocolVersion(), 302);
            }

            SourceContext.updateState(conn, ProtocolState.WSDL_RESPONSE_DONE);
           
            
            try{
            temporaryData.writeTo(outputStream);
            }catch (Exception e) {
				e.printStackTrace();
			}

            try {
                outputStream.flush();
                outputStream.close();
            } catch (Exception ignored) {}
        } finally {
            temporaryData.release();
            sourceHandler.commitResponseHideExceptions(conn, response);
            
        }
    }

    public void init(ConfigurationContext configurationContext, SourceHandler sourceHandler)
            throws AxisFault {
        
        //super.init(configurationContext, sourceHandler);
    	this.cfgCtx  =  configurationContext;
    	this.sourceHandler =  sourceHandler;
        if (cfgCtx.getProperty("GETRequestProcessorMap") != null) {
            getRequestProcessors = (Map<String, org.wso2.carbon.core.transports.HttpGetRequestProcessor>)
                    cfgCtx.getProperty("GETRequestProcessorMap");
        } else {
            populateGetRequestProcessors();
        }

    }
    


    public void process(HttpRequest request, HttpResponse response,
                        MessageContext messageContext,
                        NHttpServerConnection conn,
                        OutputStream outputStream, boolean b) {

        boolean isRequestHandled = false;


        String uri = request.getRequestLine().getUri();

        String servicePath = cfgCtx.getServiceContextPath();
        if (!servicePath.startsWith("/")) {
            servicePath = "/" + servicePath;
        }
        String serviceName = getServiceName(request);

        boolean loadBalancer = Boolean.parseBoolean(System.getProperty("wso2.loadbalancer", "false"));
        if (uri.equals("/favicon.ico")) {
            response.setStatusCode(HttpStatus.SC_MOVED_PERMANENTLY);
            response.addHeader("Location", "http://wso2.org/favicon.ico");
            sourceHandler.commitResponseHideExceptions(conn, response);
        } else if(uri.startsWith(servicePath) &&
                (serviceName == null || serviceName.length() == 0)){
            //check if service listing request is blocked
            if (isServiceListBlocked(uri)) {
                //response.setStatusCode(HttpStatus.SC_FORBIDDEN);
                //serverHandler.commitResponseHideExceptions(conn,  response);
            } else{
                //generateServicesList(response, conn, outputStream, servicePath);
            }
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException ignore) {
            }
            isRequestHandled = true ;
        } else {
            int pos = uri.indexOf('?');
            if (pos != -1) {
                String queryString = uri.substring(pos + 1);
                String requestUri = uri.substring(0, pos);
                String requestUrl = uri;
                if (requestUri.indexOf("://") == -1) {
                    HttpInetConnection inetConn = (HttpInetConnection) conn;

                    String hostName = "localhost";
                    ServerConfiguration serverConfig = ServerConfiguration.getInstance();
                    if (serverConfig.getFirstProperty("HostName") != null) {
                        hostName = serverConfig.getFirstProperty("HostName");
                    }

                    requestUrl = "http://" +
                            hostName + ":" + inetConn.getLocalPort() + requestUri;
                }

                String contextPath = cfgCtx.getServiceContextPath();
                int beginIndex = -1;
                if (requestUri.indexOf(contextPath) != -1) {
                    beginIndex = requestUri.indexOf(contextPath) + contextPath.length() + 1;
                }
                AxisService axisService = null;
                if (!(beginIndex < 0 || beginIndex > requestUri.length())) {
                    serviceName = requestUri.substring(beginIndex);
                    axisService = cfgCtx.getAxisConfiguration().getServiceForActivation(serviceName);
                }

                if (axisService == null && !loadBalancer && serviceName != null) {
                    // Try to see whether the service is available in a tenant
                    try {
                        axisService = TenantAxisUtils.getAxisService(serviceName, cfgCtx);
                    } catch (AxisFault axisFault) {
                        axisFault.printStackTrace();
                    }
                }

                if (queryString != null) {
                    for (String item : getRequestProcessors.keySet()) {
                        if (queryString.indexOf(item) == 0 &&
                                (queryString.equals(item) ||
                                        queryString.indexOf("&") == item.length() ||
                                        queryString.indexOf("=") == item.length())) {
                            if (axisService == null) {
                                continue;
                            }

                            try {
                                processWithGetProcessor(request, response, requestUri,
                                        requestUrl, queryString,
                                        item, outputStream, conn);
                                messageContext.setProperty("WSDL_GEN_HANDLED", true);
                            } catch (Exception e) {
                                handleBrowserException(response, conn, outputStream,
                                        "Error processing request", e);
                            }
                            isRequestHandled = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!isRequestHandled) {
            //processGetAndDelete(request, response, messageContext, conn, outputStream, "GET", b);
        	messageContext.setProperty(PassThroughConstants.REST_GET_DELETE_INVOKE, true);
        }
    }
    
    
    
    
    /**
     * Is the incoming URI is requesting service list and http.block_service_list=true in
     * nhttp.properties
     * @param incomingURI incoming URI
     * @return whether to proceed with incomingURI

     */
    protected boolean isServiceListBlocked(String incomingURI) {
        String isBlocked = NHttpConfiguration.getInstance().isServiceListBlocked();

        return (("/services").equals(incomingURI) || ("/services" + "/").equals(incomingURI)) &&
               Boolean.parseBoolean(isBlocked);
    }
    
    
    
    /**
     * Handles browser exception.
     *
     * @param response HttpResponse
     * @param conn     NHttpServerConnection
     * @param os       OutputStream
     * @param msg      message
     * @param e        Exception
     */
    protected void handleBrowserException(HttpResponse response,
                                          NHttpServerConnection conn, OutputStream os,
                                          String msg, Exception e) {
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }

        if (!response.containsHeader(HTTP.TRANSFER_ENCODING)) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.setReasonPhrase(msg);
            //response.addHeader(CONTENT_TYPE, TEXT_HTML);
            //serverHandler.commitResponseHideExceptions(conn, response);
            try {
                os.write(msg.getBytes());
                os.close();
            } catch (IOException ignore) {
            }
        }

        if (conn != null) {
            try {
                conn.shutdown();
            } catch (IOException ignore) {
            }
        }
    }
    
    /**
     * Returns the service name.
     *
     * @param request HttpRequest
     * @return service name as a String
     */
    protected String getServiceName(HttpRequest request) {
        String uri = request.getRequestLine().getUri();

        String servicePath = cfgCtx.getServiceContextPath();
        if (!servicePath.startsWith("/")) {
            servicePath = "/" + servicePath;
        }

        String serviceName = null;
        if (uri.startsWith(servicePath)) {
            serviceName = uri.substring(servicePath.length());
            if (serviceName.startsWith("/")) {
                serviceName = serviceName.substring(1);
            }
            if (serviceName.indexOf("?") != -1) {
                serviceName = serviceName.substring(0, serviceName.indexOf("?"));
            }
        } else {
            // this may be a custom URI
            String incomingURI = request.getRequestLine().getUri();

            Map serviceURIMap = (Map) cfgCtx.getProperty(NhttpConstants.EPR_TO_SERVICE_NAME_MAP);
            if (serviceURIMap != null) {
                Set keySet = serviceURIMap.keySet();
                for (Object key : keySet) {
                    if (incomingURI.toLowerCase().contains(((String) key).toLowerCase())) {
                        return (String) serviceURIMap.get(key);
                    }
                }
            }
        }

        if (serviceName != null) {
            int opnStart = serviceName.indexOf("/");
            if (opnStart != -1) {
                serviceName = serviceName.substring(0, opnStart);
            }
        }
        return serviceName;
    }

    public void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }
}
