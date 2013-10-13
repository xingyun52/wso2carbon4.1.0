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

package org.wso2.carbon.cep.core.internal.config.input.mapping;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.input.mapping.MapInputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.QueryEventTypeConvertor;
import org.wso2.carbon.cep.core.mapping.input.property.MapInputProperty;
import org.wso2.carbon.databridge.commons.Event;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Map;

public class MapInputMappingHelper {

    public static MapInputMapping fromOM(OMElement mapMappingElement)
            throws CEPConfigurationException {
//        return new MapInputMapping();
        MapInputMapping mapInputMapping = new MapInputMapping();

        String stream =
                mapMappingElement.getAttributeValue(
                        new QName(CEPConstants.CEP_CONF_ATTR_STREAM));
        mapInputMapping.setStream(stream);

        String className = mapMappingElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_QUERY_EVENT_TYPE));
        mapInputMapping.setMappingClass(QueryEventTypeConvertor.mappingClassFromType(className));


        for (Iterator iterator = mapMappingElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                                   CEPConstants.CEP_CONF_ELE_PROPERTY)); iterator.hasNext(); ) {
            OMElement propertyElement = (OMElement) iterator.next();
            MapInputProperty inputProperty = InputPropertyHelper.mapPropertyFromOM(propertyElement);
            mapInputMapping.addProperty(inputProperty);
            if (mapInputMapping.getMappingClass() != Map.class && mapInputMapping.getMappingClass() != Event.class) {
                mapInputMapping.putWriteMethod(inputProperty.getName(), InputMappingHelper.getMethod(mapInputMapping.getMappingClass(), inputProperty.getName()));
            }
        }

        return mapInputMapping;
    }
    public static OMElement mapInputMappingToOM(MapInputMapping mapInputMap) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement mapInputMappingElement = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_MAP_MAPPING,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        String inputStream = mapInputMap.getStream();
        mapInputMappingElement.addAttribute(CEPConstants.CEP_CONF_ATTR_STREAM,
                                              inputStream, null);
        Class mappingClass = mapInputMap.getMappingClass();
        String className = QueryEventTypeConvertor.mappingClassToType(mappingClass);
        mapInputMappingElement.addAttribute(CEPConstants.CEP_CONF_ATTR_QUERY_EVENT_TYPE, className, null);

        for (MapInputProperty mapInputProperty : mapInputMap.getProperties()) {
            mapInputMappingElement.addChild(InputPropertyHelper.mapPropertyToOM(mapInputProperty));
        }

        return mapInputMappingElement;
    }
}
