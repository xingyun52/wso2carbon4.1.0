/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.wso2.carbon.cep.core.mapping.input.mapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.exception.CEPEventProcessingException;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.input.property.TupleInputProperty;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TupleInputMapping extends InputMapping {


    private static final Log log = LogFactory.getLog(TupleInputMapping.class);

    private List<TupleInputProperty> properties;
    private int[][] propertyPositions = null; //int[meta=0,correlation=1,payload=2][position]
    private Map<String, Method> writeMethodMap;
    protected StreamDefinition eventStreamDefinition;
    private HashMap<Integer, String> mapProperties;

    public TupleInputMapping() {
        this.writeMethodMap = new HashMap<String, Method>();
        this.properties = new ArrayList<TupleInputProperty>();
        mappingClass = Event.class;
    }


    public void putWriteMethod(String name, Method writeMethod) {
        this.writeMethodMap.put(name, writeMethod);
    }

    public void addProperty(TupleInputProperty property) {
        this.properties.add(property);
    }

    @Override
    protected Map convertToEventMap(Object event) throws CEPEventProcessingException {
        if (propertyPositions != null) {
            Map<String, Object> mapEvent = new HashMap<String, Object>();
            for (int i = 0, size = properties.size(); i < size; i++) {
                mapEvent.put(properties.get(i).getName(), getValue((Event) event, i));
            }
            return mapEvent;
        }
        return null;
    }

    @Override
    protected Object convertToEventObject(Object event, Object resultEvent)
            throws CEPEventProcessingException {
        if (propertyPositions != null) {
            for (int i = 0, size = properties.size(); i < size; i++) {
                Object propertyValue = getValue((Event) event, i);
                try {
                    this.writeMethodMap.get(properties.get(i).getName()).invoke(resultEvent, propertyValue);
                } catch (Exception e) {
                    throw new CEPEventProcessingException("Cannot invoke " + properties.get(i).getName() +
                                                          " in Event class " + this.mappingClass.getName(), e);
                }
            }
            return resultEvent;
        }
        return null;
    }

    @Override
    protected Event convertToEventTuple(Object event) throws CEPEventProcessingException {
        if (propertyPositions != null) {
            int propertySize = properties.size();
            Object[] eventData = new Object[propertySize];

            for (int i = 0; i < propertySize; i++) {
                eventData[i] = getValue((Event) event, i);
            }
            if (((Event) event).getTimeStamp() == 0) {
                ((Event) event).setTimeStamp(System.currentTimeMillis());
            }
            ((Event) event).setMetaData(null);
            ((Event) event).setCorrelationData(null);
            ((Event) event).setPayloadData(eventData);
            return (Event) event;
        }
        return null;
    }

    @Override
    public void addEventDefinition(Object eventDef) {
        eventStreamDefinition = (StreamDefinition) eventDef;
        initPropertyPositions();
    }

    @Override
    public void removeEventDefinition(Object eventDef) {
        eventStreamDefinition = null;
        propertyPositions = null;
        mapProperties=null;
    }

    private Object getValue(Event event, int i) throws CEPEventProcessingException {
        int[] position = propertyPositions[i];
        if (position[1] < 0) {
            return event.getArbitraryDataMap().get(mapProperties.get(position[1]));
        } else {
            switch (position[0]) {
                case 0:
                    return event.getMetaData()[position[1]];
                case 1:
                    return event.getCorrelationData()[position[1]];
                case 2:
                    return event.getPayloadData()[position[1]];
            }
        }
        //this will never occur
        throw new CEPEventProcessingException("Invalid property position");
    }

    private void initPropertyPositions() {
        propertyPositions = new int[properties.size()][2];
        mapProperties = new HashMap<Integer, String>();
        int mapPropertiesCount = 0;
        for (int i = 0, propertySize = properties.size(); i < propertySize; i++) {
            TupleInputProperty property = properties.get(i);
            if (property.getInputDataType().equals(CEPConstants.CEP_CONF_TUPLE_DATA_TYPE_PAYLOAD)) {
                List<Attribute> payloadData = eventStreamDefinition.getPayloadData();
                propertyPositions[i][0] = 2;
                int position = getPosition(property, payloadData);
                if (position == -1) {
                    position = --mapPropertiesCount;
                    mapProperties.put(position, property.getInputName());
                }
                propertyPositions[i][1] = position;
            } else if (property.getInputDataType().equals(CEPConstants.CEP_CONF_TUPLE_DATA_TYPE_META)) {
                List<Attribute> metaData = eventStreamDefinition.getMetaData();
                propertyPositions[i][0] = 0;
                int position = getPosition(property, metaData);
                if (position == -1) {
                    position = --mapPropertiesCount;
                    mapProperties.put(position, "meta." + property.getInputName());
                }
                propertyPositions[i][1] = position;
            } else if (property.getInputDataType().equals(CEPConstants.CEP_CONF_TUPLE_DATA_TYPE_CORRELATION)) {
                List<Attribute> correlationData = eventStreamDefinition.getCorrelationData();
                propertyPositions[i][0] = 1;
                int position = getPosition(property, correlationData);
                if (position == -1) {
                    position = --mapPropertiesCount;
                    mapProperties.put(position, "correlation." + property.getInputName());
                }
                propertyPositions[i][1] = position;
            }
        }
    }

    private int getPosition(TupleInputProperty property, List<Attribute> payloadData) {

        for (int i = 0, payloadDataSize = payloadData.size(); i < payloadDataSize; i++) {
            Attribute attribute = payloadData.get(i);
            if (attribute.getName().equals(property.getInputName())) {
                return i;
            }
        }
        return -1;
//        throw new CEPEventProcessingException(" Data bridge stream definition does not match with the input mapping, Expected input property is : " + property.getInputName() + " ;But Available input properties are :" + payloadData.toString());
    }

    public List<TupleInputProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<TupleInputProperty> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TupleInputMapping)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        TupleInputMapping that = (TupleInputMapping) o;

        if (properties != null ? !properties.equals(that.properties) : that.properties != null) {
            return false;
        }
        if (writeMethodMap != null ? !writeMethodMap.equals(that.writeMethodMap) : that.writeMethodMap != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (writeMethodMap != null ? writeMethodMap.hashCode() : 0);
        return result;
    }
}
