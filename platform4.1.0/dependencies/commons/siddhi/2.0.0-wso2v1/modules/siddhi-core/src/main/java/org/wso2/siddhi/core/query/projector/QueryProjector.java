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
package org.wso2.siddhi.core.query.projector;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.event.AtomicEvent;
import org.wso2.siddhi.core.event.BundleEvent;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.event.ListEvent;
import org.wso2.siddhi.core.event.StreamEvent;
import org.wso2.siddhi.core.event.in.InEvent;
import org.wso2.siddhi.core.event.in.InListEvent;
import org.wso2.siddhi.core.event.in.InStream;
import org.wso2.siddhi.core.event.remove.RemoveStream;
import org.wso2.siddhi.core.executor.conditon.ConditionExecutor;
import org.wso2.siddhi.core.query.QueryPostProcessingElement;
import org.wso2.siddhi.core.query.output.QueryCallback;
import org.wso2.siddhi.core.query.projector.attribute.factory.OutputAttributeProcessorFactory;
import org.wso2.siddhi.core.query.projector.attribute.processor.AbstractAggregationAttributeProcessor;
import org.wso2.siddhi.core.query.projector.attribute.processor.AttributeProcessor;
import org.wso2.siddhi.core.query.projector.attribute.processor.AttributeProcessorFactory;
import org.wso2.siddhi.core.query.projector.attribute.processor.GroupByAttributeProcessor;
import org.wso2.siddhi.core.query.projector.attribute.processor.NonGroupingAttributeProcessor;
import org.wso2.siddhi.core.query.projector.attribute.processor.PassThroughAttributeProcessor;
import org.wso2.siddhi.core.stream.StreamJunction;
import org.wso2.siddhi.core.util.LogHelper;
import org.wso2.siddhi.core.util.SiddhiClassLoader;
import org.wso2.siddhi.core.util.extensionholder.OutputAttributeExtensionHolder;
import org.wso2.siddhi.core.util.parser.ExecutorParser;
import org.wso2.siddhi.query.api.condition.Condition;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.query.QueryEventStream;
import org.wso2.siddhi.query.api.query.input.handler.Filter;
import org.wso2.siddhi.query.api.query.output.OutStream;
import org.wso2.siddhi.query.api.query.projection.Projector;
import org.wso2.siddhi.query.api.query.projection.attribute.ComplexAttribute;
import org.wso2.siddhi.query.api.query.projection.attribute.OutputAttribute;
import org.wso2.siddhi.query.api.query.projection.attribute.OutputAttributeExtension;
import org.wso2.siddhi.query.api.query.projection.attribute.SimpleAttribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public class QueryProjector implements QueryPostProcessingElement {

    static final Logger log = Logger.getLogger(QueryProjector.class);

    //    private List<Object[]> dataList = new ArrayList<Object[]>();
    private List<AttributeProcessor> attributeProcessorList;
    private List<AttributeProcessor> aggregateAttributeProcessorList;
    private List<QueryCallback> callbacks;
    private int outputSize;
    private String outputStreamId;
    private StreamDefinition outputStreamDefinition;
    private StreamJunction outputStreamJunction;
    private Projector projector;
    private ConditionExecutor havingConditionExecutor = null;
    private OutStream outStream;
    public boolean currentOn = false;
    public boolean expiredOn = false;
    public OutputGroupByKeyGenerator groupByKeyGenerator = null;
    private boolean groupBy = false;
    private boolean distributedProcessing = false;

    public QueryProjector(OutStream outStream, Projector projector,
                          List<QueryEventStream> queryEventStreamList,
                          ConcurrentMap<String, StreamJunction> streamJunctionMap,
                          SiddhiContext siddhiContext) {
        if (outStream != null) {
            this.outStream = outStream;
            if (outStream.getOutputEvents() == OutStream.OutputEvents.CURRENT_EVENTS || outStream.getOutputEvents() == OutStream.OutputEvents.ALL_EVENTS) {
                currentOn = true;
            }
            if (outStream.getOutputEvents() == OutStream.OutputEvents.EXPIRED_EVENTS || outStream.getOutputEvents() == OutStream.OutputEvents.ALL_EVENTS) {
                expiredOn = true;
            }
            this.outputStreamId = outStream.getStreamId();

            outputStreamJunction = streamJunctionMap.get(outputStreamId);
            if (outputStreamJunction == null) {
                outputStreamJunction = new StreamJunction(outputStreamId);
                streamJunctionMap.putIfAbsent(outputStreamId, outputStreamJunction);
            }
        } else {
            currentOn = true;
            expiredOn = true;
        }
        this.projector = projector;

        outputSize = projector.getProjectionList().size();
        attributeProcessorList = new ArrayList<AttributeProcessor>(outputSize);
        aggregateAttributeProcessorList = new ArrayList<AttributeProcessor>(outputSize);
        callbacks = new ArrayList<QueryCallback>();
        outputStreamDefinition = new StreamDefinition();
        outputStreamDefinition.name(outputStreamId);

        if (projector.getGroupByList().size() > 0) {
            groupBy = true;
            groupByKeyGenerator = new OutputGroupByKeyGenerator(projector.getGroupByList(), queryEventStreamList, siddhiContext);
        }

        populateOutputAttributes(queryEventStreamList, siddhiContext);

        havingConditionExecutor = generateHavingExecutor(projector.getHavingCondition(), outputStreamId, outputStreamDefinition, siddhiContext);

        distributedProcessing = siddhiContext.isDistributedProcessing();

    }


    private ConditionExecutor generateHavingExecutor(Condition condition, String outputStreamId,
                                                     StreamDefinition outputStreamDefinition,
                                                     SiddhiContext siddhiContext) {
        if (condition != null) {
            List<QueryEventStream> queryEventStreamList = new ArrayList<QueryEventStream>();
            queryEventStreamList.add(new QueryEventStream(outputStreamId, outputStreamId, outputStreamDefinition, new Filter(condition), null, null));
            return ExecutorParser.parseCondition(condition, queryEventStreamList, outputStreamId, true, siddhiContext);
        }
        return null;
    }

    private void populateOutputAttributes(List<QueryEventStream> queryEventStreamList,
                                          SiddhiContext siddhiContext) {
        for (OutputAttribute outputAttribute : projector.getProjectionList()) {
            if (outputAttribute instanceof SimpleAttribute) {
                PassThroughAttributeProcessor attributeGenerator = new PassThroughAttributeProcessor(ExecutorParser.parseExpression(((SimpleAttribute) outputAttribute).getExpression(), queryEventStreamList, null, false, siddhiContext));
                attributeProcessorList.add(attributeGenerator);
                outputStreamDefinition.attribute(outputAttribute.getRename(), attributeGenerator.getOutputType());
            } else {
                OutputAttributeProcessorFactory outputAttributeProcessorFactory;
                if (outputAttribute instanceof ComplexAttribute) {
                    outputAttributeProcessorFactory = (OutputAttributeProcessorFactory) SiddhiClassLoader.loadSiddhiImplementation(((ComplexAttribute) outputAttribute).getAttributeName(), OutputAttributeProcessorFactory.class);
                } else {//extension
                    outputAttributeProcessorFactory = (OutputAttributeProcessorFactory) SiddhiClassLoader.loadExtensionImplementation(((OutputAttributeExtension) outputAttribute), OutputAttributeExtensionHolder.getInstance(siddhiContext));
                }
                Expression[] expressions = null;
                if (outputAttribute instanceof ComplexAttribute) {
                    expressions = ((ComplexAttribute) outputAttribute).getExpressions();
                } else {
                    expressions = ((OutputAttributeExtension) outputAttribute).getExpressions();
                }
                AttributeProcessor attributeProcessor = AttributeProcessorFactory.createAttributeProcessor(expressions, queryEventStreamList, outputAttributeProcessorFactory, siddhiContext, groupBy);
                if (outputAttributeProcessorFactory.getProcessorType() == OutputAttributeProcessorFactory.ProcessorType.AGGREGATOR) {
                    //for persistence
                    siddhiContext.getPersistenceService().addPersister((AbstractAggregationAttributeProcessor) attributeProcessor);
                    aggregateAttributeProcessorList.add(attributeProcessor);
                }
                attributeProcessorList.add(attributeProcessor);
                outputStreamDefinition.attribute(outputAttribute.getRename(), attributeProcessor.getOutputType());
            }
        }


    }


    public StreamDefinition getOutputStreamDefinition() {
        return outputStreamDefinition;
    }

    public void setStreamJunction(StreamJunction streamJunction) {
        this.outputStreamJunction = streamJunction;
    }

    public void process(AtomicEvent atomicEvent) {
        LogHelper.logMethod(log, atomicEvent);
        try {
            String groupByKey = null;
            if (groupBy) {
                groupByKey = groupByKeyGenerator.constructEventKey(atomicEvent);
            }
            if ((!(atomicEvent instanceof InStream) || !currentOn) && (!(atomicEvent instanceof RemoveStream) || !expiredOn)) {
                for (AttributeProcessor attributeProcessor : aggregateAttributeProcessorList) {
                    processOutputAttributeGenerator(atomicEvent, groupByKey, attributeProcessor);
                }
                return;
            }

            Object[] data = new Object[outputSize];
            for (int i = 0; i < outputSize; i++) {
                AttributeProcessor attributeProcessor = attributeProcessorList.get(i);
                data[i] = processOutputAttributeGenerator(atomicEvent, groupByKey, attributeProcessor);
            }

            //   dataList.add(data);
//            if (outputStreamJunction != null) {
            StreamEvent event = null;
            if (havingConditionExecutor == null) {
                if (atomicEvent instanceof InStream) {
                    event = new InEvent(outputStreamId, atomicEvent.getTimeStamp(), data);
                    send(event.getTimeStamp(), event, null, event);
                } else {
                    event = new InEvent(outputStreamId, ((RemoveStream) atomicEvent).getExpiryTime(), data);
                    send(event.getTimeStamp(), null, event, event);
                }
            } else {
                if (atomicEvent instanceof InStream) {
                    event = new InEvent(outputStreamId, atomicEvent.getTimeStamp(), data);
                    if (havingConditionExecutor.execute((AtomicEvent) event)) {
                        send(event.getTimeStamp(), event, null, event);
                    }
                } else {
                    event = new InEvent(outputStreamId, ((RemoveStream) atomicEvent).getExpiryTime(), data);
                    if (havingConditionExecutor.execute((AtomicEvent) event)) {
                        send(event.getTimeStamp(), null, event, event);
                    }
                }
            }

//            }
        } catch (ClassCastException e) {
            log.error("Input event attribute type " + e.getMessage() + " type defined in the stream definition!");
        }
    }

    private void send(long timeStamp, StreamEvent currentEvent, StreamEvent expiredEvent,
                      StreamEvent allEvent) {
        if (outputStreamJunction != null) {
            outputStreamJunction.send(allEvent);
        }
        if (callbacks.size() > 0) {
            for (QueryCallback callback : callbacks) {
                callback.receive(timeStamp, currentEvent, expiredEvent);
            }
        }
    }

    public void process(BundleEvent bundleEvent) {

        if (distributedProcessing) {
            //to improve performance
            for (AttributeProcessor attributeProcessor : aggregateAttributeProcessorList) {
                attributeProcessor.lock();
            }
        }
        try {
            if ((!(bundleEvent instanceof InStream) || !currentOn) && (!(bundleEvent instanceof RemoveStream) || !expiredOn)) {
                for (int i = 0, eventsLength = bundleEvent.getActiveEvents(); i < eventsLength; i++) {
                    AtomicEvent event = bundleEvent.getEvent(i);
                    String groupByKey = null;
                    if (groupBy) {
                        groupByKey = groupByKeyGenerator.constructEventKey(event);
                    }
                    for (AttributeProcessor attributeProcessor : aggregateAttributeProcessorList) {
                        processOutputAttributeGenerator(event, groupByKey, attributeProcessor);
                    }
                }
                return;
            }
            if (!groupBy) {
                if (aggregateAttributeProcessorList.size() > 0) {
                    for (int i = 0, iterateLength = bundleEvent.getActiveEvents() - 1; i < iterateLength; i++) {
                        for (AttributeProcessor attributeProcessor : aggregateAttributeProcessorList) {
                            processOutputAttributeGenerator(bundleEvent.getEvent(i), null, attributeProcessor);
                        }
                    }
                    process(bundleEvent.getEvent(bundleEvent.getActiveEvents() - 1));
                } else {
                    List<Object[]> groupedEventData = new ArrayList<Object[]>();
                    for (int j = 0, eventsLength = bundleEvent.getActiveEvents(); j < eventsLength; j++) {
                        AtomicEvent event = bundleEvent.getEvent(j);
                        Object[] data = new Object[outputSize];
                        for (int i = 0; i < outputSize; i++) {
                            AttributeProcessor attributeProcessor = attributeProcessorList.get(i);
                            data[i] = processOutputAttributeGenerator(event, null, attributeProcessor);
                        }
                        groupedEventData.add(data);
                    }
                    sendEvents(bundleEvent, groupedEventData);
                }
            } else {
                LinkedHashMap<String, Object[]> groupedEvents = new LinkedHashMap<String, Object[]>();
                for (int j = 0, eventsLength = bundleEvent.getActiveEvents(); j < eventsLength; j++) {
                    AtomicEvent event = bundleEvent.getEvent(j);
                    String groupByKey = groupByKeyGenerator.constructEventKey(event);
                    Object[] data = new Object[outputSize];
                    for (int i = 0; i < outputSize; i++) {
                        AttributeProcessor attributeProcessor = attributeProcessorList.get(i);
                        data[i] = processOutputAttributeGenerator(event, groupByKey, attributeProcessor);
                    }
                    groupedEvents.put(groupByKey, data);
                }
                sendEvents(bundleEvent, groupedEvents.values());

            }
        } catch (ClassCastException e) {
            log.error("Input event attribute type " + e.getMessage() + " type defined in the stream definition!", e);
        }

        if (distributedProcessing) {
            for (AttributeProcessor attributeProcessor : aggregateAttributeProcessorList) {
                attributeProcessor.unlock();
            }
        }
    }

    private void sendEvents(ComplexEvent referenceEvent,
                            Collection<Object[]> groupedEventData) {
        if (outputStreamJunction != null) {
            long timeStamp;
            if (referenceEvent instanceof InStream) {
                timeStamp = referenceEvent.getTimeStamp();
            } else {
                timeStamp = ((RemoveStream) referenceEvent).getExpiryTime();
            }
            ListEvent outputListEvent = new InListEvent(groupedEventData.size());
            if (havingConditionExecutor == null) {
                for (Object[] data : groupedEventData) {
                    outputListEvent.addEvent(new InEvent(outputStreamId, timeStamp, data));
                }
            } else {
                for (Object[] data : groupedEventData) {
                    Event event = new InEvent(outputStreamId, timeStamp, data);
                    if (havingConditionExecutor.execute(event)) {
                        outputListEvent.addEvent(event);

                    }
                }
            }
            if (outputListEvent.getActiveEvents() > 0) {
                if (referenceEvent instanceof InStream) {
                    send(outputListEvent.getTimeStamp(), outputListEvent, null, outputListEvent);
                } else {
                    send(outputListEvent.getTimeStamp(), null, outputListEvent, outputListEvent);
                }
            }
        }
    }

    private Object processOutputAttributeGenerator(AtomicEvent atomicEvent, String groupByKey,
                                                   AttributeProcessor attributeProcessor) {
        if (attributeProcessor instanceof NonGroupingAttributeProcessor) {
            return ((NonGroupingAttributeProcessor) attributeProcessor).process(atomicEvent);
        } else {
            return ((GroupByAttributeProcessor) attributeProcessor).process(atomicEvent, groupByKey);
        }
    }


    public void addCallback(QueryCallback callback) {
        callbacks.add(callback);
    }
}

