/*
 * Copyright 2004,2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.wso2.carbon.cep.siddhi.backend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.Expression;
import org.wso2.carbon.cep.core.backend.CEPBackEndRuntime;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.internal.ds.CEPServiceValueHolder;
import org.wso2.carbon.cep.core.listener.CEPEventListener;
import org.wso2.carbon.cep.core.mapping.input.Input;
import org.wso2.carbon.cep.core.mapping.input.mapping.InputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.MapInputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.TupleInputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.XMLInputMapping;
import org.wso2.carbon.cep.core.mapping.input.property.MapInputProperty;
import org.wso2.carbon.cep.core.mapping.input.property.TupleInputProperty;
import org.wso2.carbon.cep.core.mapping.input.property.XMLInputProperty;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.compiler.exception.SiddhiPraserException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Siddhi based CEP back end runtime.
 */

public class SiddhiBackEndRuntime implements CEPBackEndRuntime {

    private static final Log log = LogFactory.getLog(SiddhiBackEndRuntime.class);
    private String bucketName;
    private SiddhiManager siddhiManager;
    private Map<String, InputHandler> siddhiInputHandlerMap;
    private Map<String, String> queryReferenceMap;
    private ScheduledExecutorService persistenceScheduler = Executors.newScheduledThreadPool(1);

    //    private Map<String, StreamReference> queryReferenceMap;
    private int tenantId;

    static ConcurrentHashMap<String, Attribute.Type> javaToSiddhiType;
    private long persistenceTimeInterval;

    static {
        javaToSiddhiType = new ConcurrentHashMap<String, Attribute.Type>();
        javaToSiddhiType.put(Integer.class.getName(), Attribute.Type.INT);
        javaToSiddhiType.put(String.class.getName(), Attribute.Type.STRING);
        javaToSiddhiType.put(Double.class.getName(), Attribute.Type.DOUBLE);
        javaToSiddhiType.put(Long.class.getName(), Attribute.Type.LONG);
        javaToSiddhiType.put(Float.class.getName(), Attribute.Type.FLOAT);
        javaToSiddhiType.put(Boolean.class.getName(), Attribute.Type.BOOL);
    }

    public SiddhiBackEndRuntime(String bucketName, SiddhiManager siddhiManager,
                                Map<String, InputHandler> siddhiInputHandlerMap, int tenantId,
                                long persistenceTimeInterval) {
        this.bucketName = bucketName;
        this.siddhiManager = siddhiManager;
        this.siddhiInputHandlerMap = siddhiInputHandlerMap;
        this.tenantId = tenantId;
        this.queryReferenceMap = new HashMap<String, String>();
        this.persistenceTimeInterval = persistenceTimeInterval;

    }

    /**
     * get the cep input handler and insert the event.
     *
     * @param event        - object representing the event data
     * @param inputMapping - Mapping to the topic which publish data
     *                     details of the input stream to add events.
     */
    public void insertEvent(Object event, InputMapping inputMapping) {

        String streamName = inputMapping.getStream();
        InputHandler inputHandler = siddhiInputHandlerMap.get(streamName);
        try {
            inputHandler.send(((Event) event).getTimeStamp(),
                              ((Event) event).getPayloadData());
        } catch (InterruptedException e) {
            log.error("Unable to send event: " + event);
        }
    }

    /**
     * adds the query to the siddhi backend. if the expression is inline it takes the query
     * from it. otherwise read from the registry.
     *
     * @param expression       - cep rule source
     * @param queryName        - Name of the Query To be added
     * @param cepEventListener - wso2 cep engine pass this object to receive the events
     *                         back from the cep engine.
     * @throws org.wso2.carbon.cep.core.exception.CEPConfigurationException
     *
     */
    public void addQuery(String queryName, Expression expression,
                         final CEPEventListener cepEventListener)
            throws CEPConfigurationException {
        String siddhiQuery = expression.getText();
        String queryReference;
        try {
            queryReference = siddhiManager.addQuery(siddhiQuery);
            queryReferenceMap.put(queryName, queryReference);
        } catch (SiddhiPraserException e) {
            throw new CEPConfigurationException("Error in query "+e.getMessage(), e);
        }
//            StreamReference streamReference = siddhiManager.addQuery(siddhiQuery);
//            queryReferenceMap.put(queryName, streamReference);

        if (cepEventListener != null) {
            String outputStreamId = siddhiManager.getQuery(queryReference).getOutputStream().getStreamId();
            StreamDefinition streamDefinition = siddhiManager.getStreamDefinition(outputStreamId);
            try {
                cepEventListener.setStreamDefinition(createStreamTypeDef(streamDefinition));
            } catch (MalformedStreamDefinitionException e) {
                throw new CEPConfigurationException("Cannot create proper stream definition using the configuration, "+e.getErrorMessage(), e);
            }
            siddhiManager.addCallback(queryReference, new SiddhiEventListner(streamDefinition,
                                                                             cepEventListener));
        }


    }

    private org.wso2.carbon.databridge.commons.StreamDefinition createStreamTypeDef(
            StreamDefinition streamDefinition)
            throws MalformedStreamDefinitionException {
        org.wso2.carbon.databridge.commons.StreamDefinition typeDef = new org.wso2.carbon.databridge.commons.StreamDefinition("rowStreamDefinition");
        List<org.wso2.carbon.databridge.commons.Attribute> attributeList = new ArrayList<org.wso2.carbon.databridge.commons.Attribute>();
        List<Attribute> attributes = streamDefinition.getAttributeList();
        for (Attribute attribute : attributes) {
            Attribute.Type type = attribute.getType();
            if (type == Attribute.Type.STRING) {
                attributeList.add(new org.wso2.carbon.databridge.commons.Attribute(attribute.getName(), AttributeType.STRING));
            } else if (type == Attribute.Type.INT) {
                attributeList.add(new org.wso2.carbon.databridge.commons.Attribute(attribute.getName(), AttributeType.INT));
            } else if (type == Attribute.Type.LONG) {
                attributeList.add(new org.wso2.carbon.databridge.commons.Attribute(attribute.getName(), AttributeType.LONG));
            } else if (type == Attribute.Type.BOOL) {
                attributeList.add(new org.wso2.carbon.databridge.commons.Attribute(attribute.getName(), AttributeType.BOOL));
            } else if (type == Attribute.Type.LONG) {
                attributeList.add(new org.wso2.carbon.databridge.commons.Attribute(attribute.getName(), AttributeType.LONG));
            } else if (type == Attribute.Type.FLOAT) {
                attributeList.add(new org.wso2.carbon.databridge.commons.Attribute(attribute.getName(), AttributeType.FLOAT));
            } else if (type == Attribute.Type.DOUBLE) {
                attributeList.add(new org.wso2.carbon.databridge.commons.Attribute(attribute.getName(), AttributeType.DOUBLE));
            }
        }
        typeDef.setPayloadData(attributeList);
        return typeDef;
    }

    public void removeQuery(String queryName) throws CEPConfigurationException {
//        StreamReference streamReference = queryReferenceMap.remove(queryName);
//        if (streamReference != null) {
//            siddhiManager.removeStream(streamReference);
//        }
    }

    public void removeAllQueries() throws CEPConfigurationException {
//        for (StreamReference streamReference : queryReferenceMap.values()) {
//            siddhiManager.removeStream(streamReference);
//        }
    }

    @Override
    public void addInput(Input input) throws CEPConfigurationException {
        InputMapping mapping = input.getInputMapping();

        StreamDefinition streamDefinition = new StreamDefinition();
        streamDefinition.name(mapping.getStream());

        if (mapping instanceof TupleInputMapping) {
            TupleInputMapping tupleInputMapping = (TupleInputMapping) mapping;
            List<TupleInputProperty> properties = tupleInputMapping.getProperties();
            for (TupleInputProperty property1 : properties) {
                streamDefinition.attribute(property1.getName(), javaToSiddhiType.get(property1.getType()));
            }
        } else if (mapping instanceof MapInputMapping) {
            MapInputMapping mapInputMapping = (MapInputMapping) mapping;
            List<MapInputProperty> properties = mapInputMapping.getProperties();
            for (MapInputProperty property1 : properties) {
                streamDefinition.attribute(property1.getName(), javaToSiddhiType.get(property1.getType()));
            }
        } else { //Xml mapping
            XMLInputMapping xmlInputMapping = (XMLInputMapping) mapping;
            List<XMLInputProperty> properties = xmlInputMapping.getProperties();
            for (XMLInputProperty property1 : properties) {
                streamDefinition.attribute(property1.getName(), javaToSiddhiType.get(property1.getType()));
            }
        }


        if (siddhiManager.getStreamDefinition(streamDefinition.getStreamId()) == null) {
//        try {
            siddhiInputHandlerMap.put(mapping.getStream(),
                                      siddhiManager.defineStream(streamDefinition));
        }
//        } catch  {
//            throw new CEPConfigurationException("Invalid input stream configuration for " +
//                                                mapping.getStream(), e);
//        }
//        try {
//            siddhiManager.update();
//        } catch (SiddhiException e) {
//            throw new CEPConfigurationException("Cannot add input topic " + input.getTopic() + " to  Siddhi Backend", e);
//        }
    }

    @Override
    public void removeInput(Input input) throws CEPConfigurationException {
//        String stream = input.getInputMapping().getStream();
//        InputHandler inputHandler = siddhiInputHandlerMap.get(stream);
//        if (inputHandler != null) {
//            siddhiManager.removeStream(new StreamReference(inputHandler.getStreamId(), inputHandler.getNodeId()));
//        }
//        try {
//            siddhiManager.update();
//        } catch (SiddhiException e) {
//            throw new CEPConfigurationException("Cannot remove input topic " + input.getTopic() + " from Siddhi Backend", e);
//        }
    }

    @Override
    public void init() {
        if (persistenceTimeInterval > 0) {
            siddhiManager.restoreLastRevision();
            persistenceScheduler.scheduleWithFixedDelay(new PersistenceWorker(siddhiManager), persistenceTimeInterval, persistenceTimeInterval, TimeUnit.MINUTES);
        }
    }

    @Override
    public void shutdown() {
        if (persistenceTimeInterval > 0) {
            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getCurrentContext().setTenantId(tenantId);
                PrivilegedCarbonContext.getCurrentContext().getTenantDomain(true);
                siddhiManager.persist();
                log.info("Siddhi successfully persisted the sates of bucket " + bucketName+" before shutdown");
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }
        }
        siddhiManager.shutdown();
    }

    private String readSourceTextFromRegistry(String key) throws RegistryException {
        Registry registry = CEPServiceValueHolder.getInstance().getRegistry(tenantId);
        Resource resource = registry.get(key);
        return new String((byte[]) resource.getContent());
    }

    class PersistenceWorker implements Runnable {
        private SiddhiManager siddhiManager;

        PersistenceWorker(SiddhiManager siddhiManager) {
            this.siddhiManager = siddhiManager;
        }

        @Override
        public void run() {
            try {
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getCurrentContext().setTenantId(tenantId);
                PrivilegedCarbonContext.getCurrentContext().getTenantDomain(true);
                siddhiManager.persist();
                log.info("Siddhi persisted sates of bucket " + bucketName);
            } finally {
                PrivilegedCarbonContext.endTenantFlow();
            }

        }
    }


}
