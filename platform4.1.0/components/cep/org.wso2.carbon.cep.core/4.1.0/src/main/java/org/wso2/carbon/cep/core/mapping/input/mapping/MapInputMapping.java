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

import org.wso2.carbon.cep.core.exception.CEPEventProcessingException;
import org.wso2.carbon.cep.core.mapping.input.property.MapInputProperty;
import org.wso2.carbon.databridge.commons.Event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapInputMapping extends InputMapping {

    private List<MapInputProperty> properties;

    private Map<String, Method> writeMethodMap;

    public MapInputMapping() {
        this.writeMethodMap = new HashMap<String, Method>();
        this.properties = new ArrayList<MapInputProperty>();
        mappingClass = Event.class;
    }


    public void putWriteMethod(String name, Method writeMethod) {
        this.writeMethodMap.put(name, writeMethod);
    }

    public void addProperty(MapInputProperty inputProperty) {
        this.properties.add(inputProperty);
    }

    @Override
    protected Map convertToEventMap(Object event) {
        Map<String, Object> mapEvent = new HashMap<String, Object>();
        for (MapInputProperty inputProperty : properties) {
            mapEvent.put(inputProperty.getName(), ((Map) event).get(inputProperty.getName()));
        }
        return mapEvent;
    }

    @Override
    protected Object convertToEventObject(Object event, Object resultEvent)
            throws CEPEventProcessingException {
        for (MapInputProperty inputProperty : properties) {
            Object propertyValue = ((Map) event).get(inputProperty.getName());
            try {
                this.writeMethodMap.get(inputProperty.getName()).invoke(resultEvent, propertyValue);
            } catch (Exception e) {
                throw new CEPEventProcessingException("Cannot invoke " + inputProperty.getName() +
                                                      " in Event class " + this.mappingClass.getName(), e);
            }
        }
        return resultEvent;
    }

    @Override
    protected Event convertToEventTuple(Object event) {
        int propertySize = properties.size();
        Object[] eventData = new Object[propertySize];

        for (int i = 0, propertiesSize = properties.size(); i < propertiesSize; i++) {
            eventData[i] = ((Map) event).get(properties.get(i).getInputName());
        }

        Event tupleEvent =new Event();
        tupleEvent.setTimeStamp(System.currentTimeMillis());
        tupleEvent.setMetaData(null);
        tupleEvent.setCorrelationData(null);
        tupleEvent.setPayloadData(eventData);
        return tupleEvent;
    }

    @Override
    public void addEventDefinition(Object eventDef) {

    }

    @Override
    public void removeEventDefinition(Object eventDef) {

    }


    public List<MapInputProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<MapInputProperty> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MapInputMapping)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        MapInputMapping that = (MapInputMapping) o;

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
