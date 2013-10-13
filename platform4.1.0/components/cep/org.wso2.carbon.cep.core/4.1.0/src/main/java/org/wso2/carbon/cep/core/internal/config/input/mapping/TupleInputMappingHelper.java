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
package org.wso2.carbon.cep.core.internal.config.input.mapping;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.input.mapping.InputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.QueryEventTypeConvertor;
import org.wso2.carbon.cep.core.mapping.input.mapping.TupleInputMapping;
import org.wso2.carbon.cep.core.mapping.input.property.TupleInputProperty;
import org.wso2.carbon.databridge.commons.Event;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Map;

/**
 * This Class will help to build TupleMapping object from a given OMElement
 */
public class TupleInputMappingHelper {
    private static final Log log = LogFactory.getLog(XMLInputMappingHelper.class);

    public static InputMapping fromOM(OMElement tupleMappingElement)
            throws CEPConfigurationException {
        TupleInputMapping tupleInputMapping = new TupleInputMapping();

        String stream =
                tupleMappingElement.getAttributeValue(
                        new QName(CEPConstants.CEP_CONF_ATTR_STREAM));
        tupleInputMapping.setStream(stream);

        String className = tupleMappingElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_QUERY_EVENT_TYPE));
        tupleInputMapping.setMappingClass(QueryEventTypeConvertor.mappingClassFromType(className));

        for (Iterator iterator = tupleMappingElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                                   CEPConstants.CEP_CONF_ELE_PROPERTY)); iterator.hasNext(); ) {
            OMElement propertyElement = (OMElement) iterator.next();
            TupleInputProperty property = InputPropertyHelper.tuplePropertyFromOM(propertyElement);
            tupleInputMapping.addProperty(property);
            if (tupleInputMapping.getMappingClass() != Map.class && tupleInputMapping.getMappingClass() != Event.class) {
                tupleInputMapping.putWriteMethod(property.getName(), InputMappingHelper.getMethod(tupleInputMapping.getMappingClass(), property.getName()));
            }
        }

        return tupleInputMapping;
    }

    public static OMElement tupleInputMappingToOM(TupleInputMapping tupleInputMap) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement tupleInputMappingElement = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_TUPLE_MAPPING,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        String inputStream = tupleInputMap.getStream();
        tupleInputMappingElement.addAttribute(CEPConstants.CEP_CONF_ATTR_STREAM,
                                              inputStream, null);
        Class mappingClass = tupleInputMap.getMappingClass();
        String className = QueryEventTypeConvertor.mappingClassToType(mappingClass);
        tupleInputMappingElement.addAttribute(CEPConstants.CEP_CONF_ATTR_QUERY_EVENT_TYPE, className, null);

        for (TupleInputProperty tupleProperty : tupleInputMap.getProperties()) {
            tupleInputMappingElement.addChild(InputPropertyHelper.tuplePropertyToOM(tupleProperty));
        }

        return tupleInputMappingElement;
    }
}
