/*
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

package org.apache.synapse.transport.passthru;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.net.ssl.SSLContext;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.AddressingHelper;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.OutTransportInfo;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.threads.NativeThreadFactory;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.SSLSetupHandler;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.params.HttpParams;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.nhttp.util.MessageFormatterDecoratorFactory;
import org.apache.synapse.transport.nhttp.util.NhttpUtil;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.TargetConnections;
import org.apache.synapse.transport.passthru.jmx.MBeanRegistrar;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.jmx.TransportView;
import org.apache.synapse.transport.passthru.util.PassThroughTransportUtils;
import org.apache.synapse.transport.passthru.util.SourceResponseFactory;

/**
 * PassThroughHttpSender for Synapse based on HttpCore and NIO extensions
 */
public class PassThroughHttpSender extends AbstractHandler implements TransportSender {

    protected Log log;

    /** IOReactor used to create connections and manage them */
    private DefaultConnectingIOReactor ioReactor;

    /** Delivery agent used for delivering the messages to the servers */
    private DeliveryAgent deliveryAgent;

    /** The configuration of the sender */
    private TargetConfiguration targetConfiguration;

    /** state of the sender */
    private volatile int state = BaseConstants.STOPPED;

    private String namePrefix;

    public PassThroughHttpSender() {
        log = LogFactory.getLog(this.getClass().getName());
    }

    public void init(ConfigurationContext configurationContext,
                     TransportOutDescription transportOutDescription) throws AxisFault {
        log.info("Initializing Pass-through HTTP/S Sender...");

        // is this an SSL Sender?
        SSLContext sslContext = getSSLContext(transportOutDescription);
        SSLSetupHandler sslSetupHandler = getSSLSetupHandler(transportOutDescription);
        namePrefix = (sslContext == null) ? "HTTP" : "HTTPS";

        WorkerPool workerPool = null;
        Object obj = configurationContext.getProperty(
                PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL);
        if (obj != null) {
            workerPool = (WorkerPool) obj;                                   
        }

        targetConfiguration = new TargetConfiguration(configurationContext,
                transportOutDescription, workerPool);
        targetConfiguration.build();
        configurationContext.setProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL,
                targetConfiguration.getWorkerPool());

        PassThroughTransportMetricsCollector metrics = new
                PassThroughTransportMetricsCollector(false, sslContext != null);
        TransportView view = new TransportView(null, this, metrics, null);
        MBeanRegistrar.getInstance().registerMBean(view, "Transport",
                 "passthru-" + namePrefix.toLowerCase() + "-sender");
        targetConfiguration.setMetrics(metrics);

        try {
            String prefix = namePrefix + "-Sender I/O dispatcher";

            ioReactor = new DefaultConnectingIOReactor(
                            targetConfiguration.getIOThreadsPerReactor(),
                            new NativeThreadFactory(new ThreadGroup(prefix + " Thread Group"), prefix),
                            targetConfiguration.getHttpParameters());

            ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {

                public boolean handle(IOException ioException) {
                    log.warn("System may be unstable: " + namePrefix +
                            " ConnectingIOReactor encountered a checked exception : " +
                            ioException.getMessage(), ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    log.warn("System may be unstable: " + namePrefix +
                            " ConnectingIOReactor encountered a runtime exception : "
                            + runtimeException.getMessage(), runtimeException);
                    return true;
                }
            });
        } catch (IOReactorException e) {
            handleException("Error starting " + namePrefix + " ConnectingIOReactor", e);
        }

        ConnectCallback connectCallback = new ConnectCallback();
        // manage target connections
        TargetConnections targetConnections =
                new TargetConnections(ioReactor, targetConfiguration, connectCallback);
        targetConfiguration.setConnections(targetConnections);

        // create the delivery agent to hand over messages
        deliveryAgent = new DeliveryAgent(targetConfiguration, targetConnections);
        // we need to set the delivery agent
        connectCallback.setDeliveryAgent(deliveryAgent);        

        TargetHandler handler = new TargetHandler(deliveryAgent, targetConfiguration);
        final IOEventDispatch ioEventDispatch =
                getEventDispatch(handler, sslContext, sslSetupHandler,
                        targetConfiguration.getHttpParameters(), transportOutDescription);

        // start the sender in a separate thread
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (Exception ex) {
                   log.fatal("Exception encountered in the " + namePrefix + " Sender. " +
                            "No more connections will be initiated by this transport", ex);
                }
                log.info(namePrefix + " Sender shutdown");
            }
        }, "PassThrough" + namePrefix + "Sender");
        t.start();

        state = BaseConstants.STARTED;

        log.info("Pass-through " + namePrefix + " Sender started...");
    }

    public void cleanup(org.apache.axis2.context.MessageContext messageContext) throws AxisFault {

    }

    public void stop() {
        try {
            ioReactor.shutdown();
        } catch (IOException e) {
            log.error("Error shutting down the PassThroughHttpSender", e);
        }
    }


    public InvocationResponse invoke(org.apache.axis2.context.MessageContext msgContext) throws AxisFault {
        // remove unwanted HTTP headers (if any from the current message)
        PassThroughTransportUtils.removeUnwantedHeaders(msgContext,
                targetConfiguration.isPreserveServerHeader(),
                targetConfiguration.isPreserveUserAgentHeader());

        if (AddressingHelper.isReplyRedirected(msgContext)
                && !msgContext.getReplyTo().hasNoneAddress()) {

            msgContext.setProperty(PassThroughConstants.IGNORE_SC_ACCEPTED, Constants.VALUE_TRUE);
        }

        EndpointReference epr = PassThroughTransportUtils.getDestinationEPR(msgContext);
        if (epr != null) {
            if (!epr.hasNoneAddress()) {
                if (msgContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE) == null) {
                    Pipe pipe = new Pipe(targetConfiguration.getBufferFactory().getBuffer(),
                            "Test", targetConfiguration);
                    msgContext.setProperty(PassThroughConstants.PASS_THROUGH_PIPE, pipe);
                    msgContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
                }
                deliveryAgent.submit(msgContext, epr);
                sendRequestContent(msgContext);
            } else {
                handleException("Cannot send message to " + AddressingConstants.Final.WSA_NONE_URI);
            }
        } else {
            if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) != null) {
                if (msgContext.getProperty(Constants.OUT_TRANSPORT_INFO) instanceof ServerWorker) {
                    try {
                        submitResponse(msgContext);
                    } catch (Exception e) {
                        handleException("Failed to submit the response", e);
                    }
                }else {
                    //handleException("No valid destination EPR to send message");
                	//should be able to handle sendUsingOutputStream  Ref NHTTP_NIO
                	sendUsingOutputStream(msgContext);
                }
            } else {
                handleException("No valid destination EPR to send message");
            }
        }

        if (msgContext.getOperationContext() != null) {
            msgContext.getOperationContext().setProperty(
                Constants.RESPONSE_WRITTEN, Constants.VALUE_TRUE);
        }

        return InvocationResponse.CONTINUE;
    }
    
    
    private void sendUsingOutputStream(MessageContext msgContext) throws AxisFault {

        OMOutputFormat format = NhttpUtil.getOMOutputFormat(msgContext);
        MessageFormatter messageFormatter =
                MessageFormatterDecoratorFactory.createMessageFormatterDecorator(msgContext);
        OutputStream out = (OutputStream) msgContext.getProperty(MessageContext.TRANSPORT_OUT);

        if (msgContext.isServerSide()) {
            OutTransportInfo transportInfo =
                (OutTransportInfo) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);

            if (transportInfo != null) {
                transportInfo.setContentType(
                messageFormatter.getContentType(msgContext, format, msgContext.getSoapAction()));
            } else {
                throw new AxisFault(Constants.OUT_TRANSPORT_INFO + " has not been set");
            }
        }

        try {
            messageFormatter.writeTo(msgContext, format, out, false);
            out.close();
        } catch (IOException e) {
            handleException("IO Error sending response message", e);
        }
    }


	private void sendRequestContent(final MessageContext msgContext) throws AxisFault {
		if (Boolean.TRUE.equals(msgContext.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED))) {
			synchronized (msgContext) {
				while (!Boolean.TRUE.equals(msgContext.getProperty("READY2ROCK")) &&
				       !Boolean.TRUE.equals(msgContext.getProperty("PASSTHRU_CONNECT_ERROR"))) {
					try {
						msgContext.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			if (Boolean.TRUE.equals(msgContext.getProperty("PASSTHRU_CONNECT_ERROR"))) {
				return;
			}

			OutputStream out = (OutputStream) msgContext.getProperty("GET_ME_OUT");
			if (out != null) {
				String disableChunking = (String) msgContext.getProperty(PassThroughConstants.DISABLE_CHUNKING);
				String forceHttp10 = (String) msgContext.getProperty(PassThroughConstants.FORCE_HTTP_1_0);
				Pipe pipe = (Pipe) msgContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
				
				if("true".equals(disableChunking) || "true".equals(forceHttp10) ){
					ByteArrayOutputStream _out = new ByteArrayOutputStream();
					MessageFormatter formatter = MessageProcessorSelector.getMessageFormatter(msgContext);
					OMOutputFormat format = PassThroughTransportUtils.getOMOutputFormat(msgContext);
					formatter.writeTo(msgContext, format, _out, false);
					try {
	                    IOUtils.write(_out.toByteArray(),out);
	                    msgContext.setProperty(PassThroughConstants.PASSTROUGH_MESSAGE_LENGTH,new Long(_out.toByteArray().length));
                    } catch (IOException e) {
	                    // TODO Auto-generated catch block
                    	 handleException("IO while building message", e);
                    }
                	pipe.setSerializationComplete(true);
				}else {
					
					if ((disableChunking == null || !"true".equals(disableChunking)) ||
					    (forceHttp10 == null || !"true".equals(forceHttp10))) {
						MessageFormatter formatter = MessageProcessorSelector.getMessageFormatter(msgContext);
						OMOutputFormat format = PassThroughTransportUtils.getOMOutputFormat(msgContext);
						formatter.writeTo(msgContext, format, out, false);
					}
					
					if ((msgContext.getProperty(PassThroughConstants.REST_GET_DELETE_INVOKE) != null &&
						    (Boolean) msgContext.getProperty(PassThroughConstants.REST_GET_DELETE_INVOKE))) {
							pipe.setSerializationCompleteWithoutData(true);
					}else{
						pipe.setSerializationComplete(true);
					}
			
				}
			}
		}
	}

    /**
     * Return the IOEventDispatch implementation to be used. This is overridden by the
     * SSL sender
     *
     * @param handler The passthru target handler instance
     * @param sslContext SSL context used by the sender or null
     * @param sslIOSessionHandler SSL session handler or null
     * @param params HTTP parameters
     * @param trpOut Transport out description
     * @return an IOEventDispatch instance
     * @throws AxisFault on error
     */
    protected IOEventDispatch getEventDispatch(NHttpClientHandler handler,
                                               SSLContext sslContext,
                                               SSLSetupHandler sslIOSessionHandler,
                                               HttpParams params, TransportOutDescription trpOut) throws AxisFault {

        return new TargetIOEventDispatch(handler, params);
    }

    /**
     * Always return null, as this implementation does not support outgoing SSL
     *
     * @param transportOut The transport out description
     * @return null
     * @throws AxisFault on error
     */
    protected SSLContext getSSLContext(TransportOutDescription transportOut) throws AxisFault {
        return null;
    }

    /**
     * Create the SSL IO Session handler to be used by this listener
     *
     * @param transportOut Transport out description
     * @return always null
     * @throws AxisFault on error
     */
    protected SSLSetupHandler getSSLSetupHandler(TransportOutDescription transportOut)
        throws AxisFault {
        return null;
    }

    public void submitResponse(MessageContext msgContext)
            throws IOException, HttpException {
        SourceConfiguration sourceConfiguration = (SourceConfiguration) msgContext.getProperty(
                        PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION);

        NHttpServerConnection conn = (NHttpServerConnection) msgContext.getProperty(
                PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
        if (conn == null) {
            ServerWorker serverWorker = (ServerWorker) msgContext.getProperty(Constants.OUT_TRANSPORT_INFO);
            if (serverWorker != null) {
                MessageContext requestContext = serverWorker.getRequestContext();
                conn = (NHttpServerConnection) requestContext.getProperty(
                        PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION);
                sourceConfiguration = (SourceConfiguration) requestContext.getProperty(
                        PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION);
            } else {
                throw new IllegalStateException("Unable to correlate the response to a request");
            }
        }

        SourceRequest sourceRequest = SourceContext.getRequest(conn);

        SourceResponse sourceResponse = SourceResponseFactory.create(msgContext,
                sourceRequest, sourceConfiguration);

        SourceContext.setResponse(conn, sourceResponse);

        Boolean noEntityBody = (Boolean) msgContext.getProperty(PassThroughConstants.NO_ENTITY_BODY);
        Pipe pipe = (Pipe) msgContext.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
        if ((noEntityBody == null || !noEntityBody) || pipe != null) {
            if (pipe == null) {
                pipe = new Pipe(sourceConfiguration.getBufferFactory().getBuffer(),
                        "Test", sourceConfiguration);
                msgContext.setProperty(PassThroughConstants.PASS_THROUGH_PIPE, pipe);
                msgContext.setProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
            }

            pipe.attachConsumer(conn);
            sourceResponse.connect(pipe);
        }

        Integer errorCode = (Integer) msgContext.getProperty(PassThroughConstants.ERROR_CODE);
        if (errorCode != null) {
            sourceResponse.setStatus(HttpStatus.SC_BAD_GATEWAY);
            SourceContext.get(conn).setShutDown(true);
        }

        ProtocolState state = SourceContext.getState(conn);
        if (state != null && state.compareTo(ProtocolState.REQUEST_DONE) <= 0) {
            // start sending the response if we
            if (msgContext.isPropertyTrue(PassThroughConstants.MESSAGE_BUILDER_INVOKED) && pipe != null) {
                OutputStream out = pipe.getOutputStream();
                if (msgContext.isPropertyTrue(NhttpConstants.SC_ACCEPTED)) {
                    out.write(new byte[0]);
                }else {
                    MessageFormatter formatter = MessageProcessorSelector.getMessageFormatter(msgContext);
                    OMOutputFormat format = PassThroughTransportUtils.getOMOutputFormat(msgContext);
                    formatter.writeTo(msgContext, format, out, false);
                }
                pipe.setSerializationComplete(true);
                out.close();
            }
            
            if(noEntityBody != null && Boolean.TRUE == noEntityBody && pipe != null){
                OutputStream out = pipe.getOutputStream();
            	out.write(new byte[0]);
            	pipe.setRawSerializationComplete(true);
                out.close();
            }
            conn.requestOutput();
        } else {
            // nothing much to do as we have started the response already
            if (errorCode != null) {
                if (log.isDebugEnabled()) {
                    log.warn("A Source connection is closed because of an " +
                            "error in target: " + conn);
                }
            } else {
                log.debug("A Source Connection is closed, because source handler " +
                        "is already in the process of writing a response while " +
                        "another response is submitted: " + conn);
            }

            SourceContext.updateState(conn, ProtocolState.CLOSED);
            sourceConfiguration.getSourceConnections().shutDownConnection(conn);
        }
    }

    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) {
            return;
        }
        state = BaseConstants.PAUSED;
        log.info(namePrefix + " Sender Paused");
    }

    public void resume() throws AxisFault {
        if (state != BaseConstants.PAUSED) {
            return;
        }
        state = BaseConstants.STARTED;
        log.info(namePrefix + " Sender Resumed");
    }

    public void maintenanceShutdown(long millis) throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            long start = System.currentTimeMillis();
            ioReactor.shutdown(millis);
            state = BaseConstants.STOPPED;
            log.info("Sender shutdown in : " + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (IOException e) {
            handleException("Error shutting down the IOReactor for maintenence", e);
        }
    }

    private void handleException(String s, Exception e) throws AxisFault {
        log.error(s, e);
        throw new AxisFault(s, e);
    }

    private void handleException(String msg) throws AxisFault {
        log.error(msg);
        throw new AxisFault(msg);
    }
}
