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
package org.wso2.siddhi.core.query.processor.window;

import org.wso2.siddhi.core.event.AtomicEvent;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.event.StreamEvent;
import org.wso2.siddhi.core.event.in.InEvent;
import org.wso2.siddhi.core.event.in.InListEvent;
import org.wso2.siddhi.core.event.remove.RemoveEvent;
import org.wso2.siddhi.core.event.remove.RemoveListEvent;
import org.wso2.siddhi.core.util.collection.map.Map;
import org.wso2.siddhi.core.util.collection.map.MapGrid;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.expression.constant.StringConstant;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class UniqueWindowProcessor extends WindowProcessor {

    private String[] uniqueAttributeNames;
    private Map<StreamEvent> map;
    private int[] attributePositions;

    @Override
    public void setParameters(Expression[] parameters) {
        uniqueAttributeNames = new String[parameters.length];
        for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
            Object parm = parameters[i];
            uniqueAttributeNames[i] = ((StringConstant) parm).getValue();
        }
    }

    @Override
    protected void processEvent(InEvent event) {
        acquireLock();
        try {
            StreamEvent oldEvent = map.put(generateKey(event), new RemoveEvent(event, Long.MAX_VALUE));
            if (oldEvent != null) {
                nextProcessor.process((AtomicEvent) oldEvent);
            }
            nextProcessor.process(event);
        } finally {
            releaseLock();
        }
    }

    @Override
    protected void processEvent(InListEvent listEvent) {
        acquireLock();
        try {
            LinkedHashMap<String, InEvent> tempMap = new LinkedHashMap<String, InEvent>();
            for (int i = 0; i < listEvent.getActiveEvents(); i++) {
                InEvent inEvent = (InEvent) listEvent.getEvent(i);
                tempMap.put(generateKey(inEvent), inEvent);
            }
            int tempMapSize = tempMap.size();
            if (tempMapSize == 1) {
                for (java.util.Map.Entry<String, InEvent> entry : tempMap.entrySet()) {
                    StreamEvent oldEvent = map.put(entry.getKey(), new RemoveEvent(entry.getValue(), Long.MAX_VALUE));
                    if (oldEvent != null) {
                        nextProcessor.process((AtomicEvent) oldEvent);
                    }
                    nextProcessor.process(entry.getValue());
                }
            } else if (tempMapSize > 1) {
                RemoveListEvent removeListEvent = new RemoveListEvent(tempMap.size());

                for (java.util.Map.Entry<String, InEvent> entry : tempMap.entrySet()) {
                    StreamEvent oldEvent = map.put(entry.getKey(), new RemoveEvent(entry.getValue(), Long.MAX_VALUE));
                    if (oldEvent != null) {
                        removeListEvent.addEvent((Event) oldEvent);
                    }
                }
                if (removeListEvent.getActiveEvents() == 1) {
                    nextProcessor.process(removeListEvent.getEvent(0));
                } else if (removeListEvent.getActiveEvents() > 1) {
                    nextProcessor.process(removeListEvent);
                }
                nextProcessor.process(new InListEvent(tempMap.values().toArray(new InEvent[tempMapSize])));
            }
        } catch (Throwable t) {
            System.out.println(t);
        } finally {
            releaseLock();
        }

    }

    @Override
    public Iterator<StreamEvent> iterator() {
        return map.iterator();
    }

    @Override
    public Iterator<StreamEvent> iterator(String predicate) {
        if (siddhiContext.isDistributedProcessing()) {
            return ((MapGrid<StreamEvent>) map).iterator(predicate);
        } else {
            return map.iterator();
        }
    }

    @Override
    protected Object[] currentState() {
        return map.currentState();
    }

    @Override
    protected void restoreState(Object[] data) {
        map.restoreState(data);
    }

    @Override
    protected void initWindow() {
        if (siddhiContext.isDistributedProcessing()) {
            map = new MapGrid<StreamEvent>(elementId, siddhiContext);
        } else {
            map = new Map<StreamEvent>();
        }
        attributePositions = new int[uniqueAttributeNames.length];
        for (int i = 0; i < uniqueAttributeNames.length; i++) {
            String attributeName = uniqueAttributeNames[i];
            attributePositions[i] = streamDefinition.getAttributePosition(attributeName);
        }
    }

    private String generateKey(InEvent event) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int position : attributePositions) {
            stringBuilder.append(event.getData(position));
        }
        return stringBuilder.toString();
    }

}
