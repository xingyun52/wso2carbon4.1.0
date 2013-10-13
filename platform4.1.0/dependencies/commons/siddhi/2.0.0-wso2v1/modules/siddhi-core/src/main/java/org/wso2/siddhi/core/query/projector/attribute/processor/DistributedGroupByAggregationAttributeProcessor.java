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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DistributedGroupByAggregationAttributeProcessor extends AbstractAggregationAttributeProcessor implements GroupByAttributeProcessor {

    static final Logger log = Logger.getLogger(DistributedGroupByAggregationAttributeProcessor.class);

    private IMap<String, OutputAttributeProcessor> distributedAggregatorMap;
    private volatile boolean lockedAcquired = false;
    private volatile Map<String, OutputAttributeProcessor> tempAggregatorMap = new HashMap<String, OutputAttributeProcessor>();

    public DistributedGroupByAggregationAttributeProcessor(Expression[] expressions, List<QueryEventStream> queryEventStreamList, OutputAttributeProcessorFactory outputAttributeProcessorFactory, String elementId, SiddhiContext siddhiContext) {
        super(expressions, queryEventStreamList, outputAttributeProcessorFactory, elementId, siddhiContext);
        distributedAggregatorMap = siddhiContext.getHazelcastInstance().getMap(elementId + "-GroupByMap");
    }

    public synchronized Object process(AtomicEvent event, String key) {

        OutputAttributeProcessor currentOutputAttributeProcessor = null;
        if (!lockedAcquired) {
            distributedAggregatorMap.lock(key);

            try {
                currentOutputAttributeProcessor = distributedAggregatorMap.getAsync(key).get();
                if (currentOutputAttributeProcessor == null) {
                    currentOutputAttributeProcessor = sampleOutputAttributeProcessor.createNewInstance();
                }
            } catch (Exception e) {
                log.error(e);
            }
        } else {
            currentOutputAttributeProcessor = tempAggregatorMap.get(key);
            if (currentOutputAttributeProcessor == null) {
                try {
                    currentOutputAttributeProcessor = distributedAggregatorMap.getAsync(key).get();
                    if (currentOutputAttributeProcessor == null) {
                        currentOutputAttributeProcessor = sampleOutputAttributeProcessor.createNewInstance();
                    }
                    tempAggregatorMap.put(key, currentOutputAttributeProcessor);
                } catch (Exception e) {
                    log.error(e);
                }
            }

        }
        Object value = process(event, currentOutputAttributeProcessor);
        if (!lockedAcquired) {
            distributedAggregatorMap.putAndUnlock(key, currentOutputAttributeProcessor);
        }
        return value;
    }


    public Object[] currentState() {
        Map<String, OutputAttributeProcessor> tempMap = new HashMap<String, OutputAttributeProcessor>();
        for (Map.Entry<String, OutputAttributeProcessor> entry : distributedAggregatorMap.entrySet()) {
            tempMap.put(entry.getKey(), entry.getValue());
        }
        //todo may be we throw an error saying we don't support for distributed cases
        return new Object[]{tempMap};
    }

    public void restoreState(Object[] objects) {
        distributedAggregatorMap.putAll((Map<String, OutputAttributeProcessor>) objects[0]);
    }

    @Override
    public synchronized void lock() {
        if (!lockedAcquired) {
            lockedAcquired = distributedAggregatorMap.lockMap(Integer.MAX_VALUE, TimeUnit.SECONDS);
            if (!lockedAcquired) {
                log.warn("Map lock for elementId " + elementId + " could not be acquired within " + Integer.MAX_VALUE + " Secs");
            }
        }
    }

    @Override
    public synchronized void unlock() {
        if (lockedAcquired) {
            for (Map.Entry<String, OutputAttributeProcessor> entry : tempAggregatorMap.entrySet()) {
                distributedAggregatorMap.putAsync(entry.getKey(), entry.getValue());
            }
            tempAggregatorMap.clear();
            lockedAcquired = false;
            distributedAggregatorMap.unlockMap();
        }
    }

}
