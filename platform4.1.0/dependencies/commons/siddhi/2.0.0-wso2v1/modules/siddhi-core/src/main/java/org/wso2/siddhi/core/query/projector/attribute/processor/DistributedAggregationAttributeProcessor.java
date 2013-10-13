/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.siddhi.core.query.projector.attribute.processor;

import com.hazelcast.core.IMap;
import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.event.AtomicEvent;
import org.wso2.siddhi.core.query.projector.attribute.handler.OutputAttributeProcessor;
import org.wso2.siddhi.core.query.projector.attribute.factory.OutputAttributeProcessorFactory;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.query.QueryEventStream;

import java.util.List;

public class DistributedAggregationAttributeProcessor extends AbstractAggregationAttributeProcessor implements NonGroupingAttributeProcessor{

    static final Logger log = Logger.getLogger(DistributedAggregationAttributeProcessor.class);

    private IMap<String, Object> distributedAggregatorMap;
    private volatile boolean lockedAcquired = false;
    private volatile OutputAttributeProcessor outputAttributeProcessor = null;


    public DistributedAggregationAttributeProcessor(Expression[] expressions, List<QueryEventStream> queryEventStreamList, OutputAttributeProcessorFactory outputAttributeProcessorFactory, String elementId, SiddhiContext siddhiContext) {
        super(expressions, queryEventStreamList, outputAttributeProcessorFactory, elementId, siddhiContext);
        distributedAggregatorMap = siddhiContext.getHazelcastInstance().getMap("SimpleDistributedAggregatorMap");
        distributedAggregatorMap.lock(elementId);
        outputAttributeProcessor = sampleOutputAttributeProcessor.createNewInstance();
        distributedAggregatorMap.putIfAbsent(elementId, outputAttributeProcessor);
        distributedAggregatorMap.unlock(elementId);
    }


    public synchronized Object process(AtomicEvent event) {
        OutputAttributeProcessor currentOutputAttributeProcessor = null;
        if (!lockedAcquired) {
            distributedAggregatorMap.lock(elementId);
            try {
                currentOutputAttributeProcessor = (OutputAttributeProcessor) distributedAggregatorMap.getAsync(elementId).get();
            } catch (Exception e) {
                log.error(e);
            }
        } else {
            currentOutputAttributeProcessor = outputAttributeProcessor;
        }
        Object value = process(event, currentOutputAttributeProcessor);
        if (!lockedAcquired) {
            distributedAggregatorMap.putAndUnlock(elementId, currentOutputAttributeProcessor);
        }
        return value;
    }

    @Override
    protected Object[] currentState() {
        distributedAggregatorMap.lock(elementId);
        OutputAttributeProcessor currentOutputAttributeProcessor = (OutputAttributeProcessor) distributedAggregatorMap.get(elementId);
        Object[] data = new Object[]{currentOutputAttributeProcessor};
        distributedAggregatorMap.unlock(elementId);
        return data;
    }

    @Override
    protected void restoreState(Object[] data) {
        distributedAggregatorMap.lock(elementId);
        distributedAggregatorMap.put(elementId, data[0]);
        distributedAggregatorMap.unlock(elementId);
    }


    @Override
    public synchronized void lock() {
        distributedAggregatorMap.lock(elementId);
        lockedAcquired = true;
        try {
            outputAttributeProcessor = (OutputAttributeProcessor) distributedAggregatorMap.getAsync(elementId).get();
        } catch (Exception e) {
            log.error(e);
        }
    }

    @Override
    public synchronized void unlock() {
        lockedAcquired = false;
        distributedAggregatorMap.putAndUnlock(elementId, outputAttributeProcessor);
    }

}
