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

package org.apache.synapse.transport.passthru;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.SessionContext;
import org.apache.axis2.description.AxisModule;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisEvent;
import org.apache.axis2.engine.AxisObserver;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.base.BaseUtils;
import org.apache.axis2.transport.base.threads.NativeThreadFactory;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorExceptionHandler;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.http.conn.ServerConnFactory;
import org.apache.synapse.transport.nhttp.config.ServerConnFactoryBuilder;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.jmx.MBeanRegistrar;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.jmx.TransportView;

/**
 * This is the TransportListener listening for incoming connections. This class start the IOReactor
 * and registers the InRequest Handlers on it.
 */
public class PassThroughHttpListener implements TransportListener {

    protected Log log = LogFactory.getLog(this.getClass());

    /** The reactor being used */
    private DefaultListeningIOReactor ioReactor;
    /** The I/O dispatch */
    private ServerIODispatch ioEventDispatch;
    /** The protocol handler */
    private SourceHandler handler;
    /** The connection factory */
    private ServerConnFactory connFactory;
    /** The protocol scheme of the listener*/
    private Scheme scheme;
    /** The configuration of the listener */
    private SourceConfiguration sourceConfiguration = null;

    /** The custom URI map for the services if there are any */
    private Map<String, String> serviceNameToEPRMap = new HashMap<String, String>();
    /** The service name map for the custom URI if there are any */
    private Map<String, String> eprToServiceNameMap = new HashMap<String, String>();
    /** the axis observer that gets notified of service life cycle events*/
    private final AxisObserver axisObserver = new GenericAxisObserver();

    private volatile int state = BaseConstants.STOPPED;

    private String namePrefix;

    protected Scheme initScheme() {
        return new Scheme("http", 80, false);
    }
    
    protected ServerConnFactoryBuilder initConnFactoryBuilder(
            final TransportInDescription transportIn, final HttpHost host) throws AxisFault {
        return new ServerConnFactoryBuilder(transportIn, host);
    }    

    public void init(ConfigurationContext cfgCtx, TransportInDescription transportInDescription)
            throws AxisFault {

        log.info("Initializing Pass-through HTTP/S Listener...");

        namePrefix = transportInDescription.getName().toUpperCase(Locale.US);
        scheme = initScheme();
        
        int portOffset = Integer.parseInt(System.getProperty("portOffset", "0"));
        Parameter portParam = transportInDescription.getParameter("port");
        int port = Integer.parseInt(portParam.getValue().toString());
        port = port + portOffset;
        portParam.setValue(String.valueOf(port));
        portParam.getParameterElement().setText(String.valueOf(port));
        
        System.setProperty(transportInDescription.getName() + ".passthrough.port", String.valueOf(port));

        Object obj = cfgCtx.getProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL);
        WorkerPool workerPool = null;
        if (obj != null) {
            workerPool = (WorkerPool) obj;
        }

        PassThroughTransportMetricsCollector metrics = new PassThroughTransportMetricsCollector(
            true, scheme.getName());

        TransportView view = new TransportView(this, null, metrics, null);
        MBeanRegistrar.getInstance().registerMBean(
            view, "Transport",
            "passthru-" + namePrefix.toLowerCase() + "-receiver");
        
        sourceConfiguration = new SourceConfiguration(cfgCtx, transportInDescription, scheme, workerPool, metrics);
        sourceConfiguration.build();

        HttpHost host = new HttpHost(
            sourceConfiguration.getHostname(), 
            sourceConfiguration.getPort(), 
            sourceConfiguration.getScheme().getName());
        ServerConnFactoryBuilder connFactoryBuilder = initConnFactoryBuilder(transportInDescription, host);
        connFactory = connFactoryBuilder.build(sourceConfiguration.getHttpParams());
        
        handler = new SourceHandler(sourceConfiguration);
        ioEventDispatch = new ServerIODispatch(handler, connFactory);
        
        // register to receive updates on services for lifetime management
        cfgCtx.getAxisConfiguration().addObservers(axisObserver);
        cfgCtx.setProperty(PassThroughConstants.EPR_TO_SERVICE_NAME_MAP, eprToServiceNameMap);

        cfgCtx.setProperty(PassThroughConstants.PASS_THROUGH_TRANSPORT_WORKER_POOL,
                sourceConfiguration.getWorkerPool());
    }

    public void start() throws AxisFault {

        log.info("Starting Pass-through " + namePrefix + " Listener...");

        try {
            String prefix = namePrefix + "-Listener I/O dispatcher";

            ioReactor = new DefaultListeningIOReactor(
                            sourceConfiguration.getIOReactorConfig(),
                            new NativeThreadFactory(new ThreadGroup(prefix + " thread group"), prefix));
            
            ioReactor.setExceptionHandler(new IOReactorExceptionHandler() {

                public boolean handle(IOException ioException) {
                    log.warn("System may be unstable: " + namePrefix +
                            " ListeningIOReactor encountered a checked exception : " +
                            ioException.getMessage(), ioException);
                    return true;
                }

                public boolean handle(RuntimeException runtimeException) {
                    log.warn("System may be unstable: " + namePrefix +
                            " ListeningIOReactor encountered a runtime exception : "
                            + runtimeException.getMessage(), runtimeException);
                    return true;
                }
            });

        } catch (IOReactorException e) {
            handleException("Error starting " + namePrefix + " ListeningIOReactor", e);
        }

        if(sourceConfiguration.getHttpGetRequestProcessor() != null){
           sourceConfiguration.getHttpGetRequestProcessor().init(sourceConfiguration.getConfigurationContext(), handler);
        }

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    ioReactor.execute(ioEventDispatch);
                } catch (Exception e) {
                    log.fatal("Exception encountered in the " + namePrefix + " Listener. " +
                            "No more connections will be accepted by this transport", e);
                }
                log.info(namePrefix + " Listener shutdown.");
            }
        }, "PassThrough" + namePrefix + "Listener");
        t.start();

        startEndpoints();
        
        state = BaseConstants.STARTED;
    }

    private void startEndpoints() throws AxisFault {
        Queue<ListenerEndpoint> endpoints = new LinkedList<ListenerEndpoint>();
        
        Set<InetSocketAddress> addressSet = new HashSet<InetSocketAddress>();
        addressSet.addAll(connFactory.getBindAddresses());
        if (addressSet.isEmpty()) {
            addressSet.add(new InetSocketAddress(sourceConfiguration.getPort()));
        }
        
        // Ensure simple but stable order
        List<InetSocketAddress> addressList = new ArrayList<InetSocketAddress>(addressSet);
        Collections.sort(addressList, new Comparator<InetSocketAddress>() {

            public int compare(InetSocketAddress a1, InetSocketAddress a2) {
                String s1 = a1.toString();
                String s2 = a2.toString();
                return s1.compareTo(s2);
            }
            
        });
        for (InetSocketAddress address: addressList) {
            endpoints.add(ioReactor.listen(address));
        }

        // Wait for the endpoint to become ready, i.e. for the listener to start accepting
        // requests.
        while (!endpoints.isEmpty()) {
            ListenerEndpoint endpoint = endpoints.remove();
            try {
                endpoint.waitFor();
                if (log.isInfoEnabled()) {
                    InetSocketAddress address = (InetSocketAddress) endpoint.getAddress();
                    if (!address.isUnresolved()) {
                        log.info("Pass-through " + namePrefix + " Listener " + "started on " + 
                            address.getHostName() + ":" + address.getPort());
                    } else {
                        log.info("Pass-through " + namePrefix + " Listener " + "started on " + address);
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Listener startup was interrupted");
                break;
            }
        }
    }
    
    private void handleException(String s, Exception e) throws AxisFault {
        log.error(s, e);
        throw new AxisFault(s, e);
    }

    public EndpointReference getEPRForService(String serviceName, String ip) throws AxisFault {
        String trailer = "";
        //Strip out the operation name
        if (serviceName.indexOf('/') != -1) {
            trailer += serviceName.substring(serviceName.indexOf("/"));
            serviceName = serviceName.substring(0, serviceName.indexOf('/'));
        }
        // strip out the endpoint name if present
        if (serviceName.indexOf('.') != -1) {
            trailer += serviceName.substring(serviceName.indexOf("."));
            serviceName = serviceName.substring(0, serviceName.indexOf('.'));
        }

        if (serviceNameToEPRMap.containsKey(serviceName)) {
            return new EndpointReference(
                    sourceConfiguration.getCustomEPRPrefix() +
                            serviceNameToEPRMap.get(serviceName) + trailer);
        } else {
            return new EndpointReference(sourceConfiguration.getServiceEPRPrefix() +
                    serviceName + trailer);
        }
    }

    /**
     * Return the EPRs for the given service over this transport
     * @param serviceName name of the service
     * @param ip IP address
     * @return the EndpointReferences for this service over the transport
     * @throws AxisFault on error
     */
    public EndpointReference[] getEPRsForService(String serviceName, String ip) throws AxisFault {
        String trailer = "";
        //Strip out the operation name
        if (serviceName.indexOf('/') != -1) {
            trailer += serviceName.substring(serviceName.indexOf("/"));
            serviceName = serviceName.substring(0, serviceName.indexOf('/'));
        }
        // strip out the endpoint name if present
        if (serviceName.indexOf('.') != -1) {
            trailer += serviceName.substring(serviceName.indexOf("."));
            serviceName = serviceName.substring(0, serviceName.indexOf('.'));
        }

        EndpointReference[] endpointReferences = new EndpointReference[1];
        if (serviceNameToEPRMap.containsKey(serviceName)) {
            endpointReferences[0] = new EndpointReference(
                    sourceConfiguration.getCustomEPRPrefix() +
                            serviceNameToEPRMap.get(serviceName) + trailer);
        } else {
            endpointReferences[0]
                    = new EndpointReference(sourceConfiguration.getServiceEPRPrefix() +
                    serviceName + trailer);
        }
        return endpointReferences;
    }

    public SessionContext getSessionContext(MessageContext messageContext) {
        return null;
    }


    public void stop() throws AxisFault {
        log.info("Stopping Pass-through " + namePrefix + " Listener..");
        try {
            ioReactor.shutdown();
        } catch (IOException e) {
            handleException("Error shutting down " + namePrefix + " listening IO reactor", e);
        }
    }

    public void destroy() {
        log.info("Destroying PassThroughHttpListener");
        sourceConfiguration.getConfigurationContext().
                getAxisConfiguration().getObserversList().remove(axisObserver);

        sourceConfiguration.getMetrics().destroy();
    }

    /**
     * Pause the listener - Stops accepting new connections, but continues processing existing
     * connections until they complete. This helps bring an instance into a maintenance mode
     *
     * @throws AxisFault if pausing fails
     */
    public void pause() throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            ioReactor.pause();

            state = BaseConstants.PAUSED;
            log.info(namePrefix + " Listener Paused");
        } catch (IOException e) {
            handleException("Error pausing IOReactor", e);
        }
    }

    /**
     * Resume the lister - Brings the lister into active mode back from a paused state
     *
     * @throws AxisFault if the resume fails
     */
    public void resume() throws AxisFault {
        if (state != BaseConstants.PAUSED) return;
        try {
            ioReactor.resume();
            state = BaseConstants.STARTED;
            log.info(namePrefix + " Listener Resumed");
        } catch (IOException e) {
            handleException("Error resuming IOReactor", e);
        }
    }

    public void reload(final TransportInDescription transportIn) throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        
        // Close all listener endpoints and stop accepting new connections
        Set<ListenerEndpoint> endpoints = ioReactor.getEndpoints();
        for (ListenerEndpoint endpoint: endpoints) {
            endpoint.close();
        }
        
        // Rebuild connection factory
        HttpHost host = new HttpHost(
            sourceConfiguration.getHostname(), 
            sourceConfiguration.getPort(), 
            sourceConfiguration.getScheme().getName());
        ServerConnFactoryBuilder connFactoryBuilder = initConnFactoryBuilder(transportIn, host);
        connFactory = connFactoryBuilder.build(sourceConfiguration.getHttpParams());
        ioEventDispatch.update(connFactory);
        
        startEndpoints();
        
        log.info(namePrefix + " Reloaded");
    }
    
    /**
     * Stop accepting new connections, and wait the maximum specified time for in-flight
     * requests to complete before a controlled shutdown for maintenance
     *
     * @param milliSecs number of milliseconds to wait until pending requests complete
     * @throws AxisFault if the shutdown fails
     */
    public void maintenanceShutdown(long milliSecs) throws AxisFault {
        if (state != BaseConstants.STARTED) return;
        try {
            long start = System.currentTimeMillis();
            ioReactor.pause();
            ioReactor.shutdown(milliSecs);
            state = BaseConstants.STOPPED;
            log.info("Listener shutdown in : " + (System.currentTimeMillis() - start) / 1000 + "s");
        } catch (IOException e) {
            handleException("Error shutting down the IOReactor for maintenance", e);
        }
    }

    /**
     * An AxisObserver which will start listening for newly deployed or started services,
     * and stop listening when services are un-deployed or stopped.
     */
    private class GenericAxisObserver implements AxisObserver {
        public void init(AxisConfiguration axisConfig) {}

        public void serviceUpdate(AxisEvent event, AxisService service) {
            if (!ignoreService(service)
                    && BaseUtils.isUsingTransport(service,
                    sourceConfiguration.getInDescription().getName())) {
                switch (event.getEventType()) {
                    case AxisEvent.SERVICE_DEPLOY :
                        addToServiceURIMap(service);
                        break;
                    case AxisEvent.SERVICE_REMOVE :
                        removeServiceFfromURIMap(service);
                        break;
                    case AxisEvent.SERVICE_START  :
                        addToServiceURIMap(service);
                        break;
                    case AxisEvent.SERVICE_STOP   :
                        removeServiceFfromURIMap(service);
                        break;
                }
            }
        }

        public void moduleUpdate(AxisEvent event, AxisModule module) {}
        public void addParameter(Parameter parameter) throws AxisFault {}
        public void removeParameter(Parameter parameter) throws AxisFault {}
        public void deserializeParameters(OMElement parameterElement) throws AxisFault {}
        public Parameter getParameter(String name) { return null; }
        public ArrayList<Parameter> getParameters() { return null; }
        public boolean isParameterLocked(String parameterName) { return false; }
        public void serviceGroupUpdate(AxisEvent event, AxisServiceGroup serviceGroup) {}
    }

    private boolean ignoreService(AxisService service) {
        // these are "private" services
        return service.getName().startsWith("__") || JavaUtils.isTrueExplicitly(
                service.getParameter(PassThroughConstants.HIDDEN_SERVICE_PARAM_NAME));
    }

    private void addToServiceURIMap(AxisService service) {
        Parameter param = service.getParameter(PassThroughConstants.SERVICE_URI_LOCATION);
        if (param != null) {
            String uriLocation = param.getValue().toString();
            if (uriLocation.startsWith("/")) {
                uriLocation = uriLocation.substring(1);
            }
            serviceNameToEPRMap.put(service.getName(), uriLocation);
            eprToServiceNameMap.put(uriLocation, service.getName());
        }
    }

    private void removeServiceFfromURIMap(AxisService service) {
        eprToServiceNameMap.remove(serviceNameToEPRMap.get(service.getName()));
        serviceNameToEPRMap.remove(service.getName());
    }

}
