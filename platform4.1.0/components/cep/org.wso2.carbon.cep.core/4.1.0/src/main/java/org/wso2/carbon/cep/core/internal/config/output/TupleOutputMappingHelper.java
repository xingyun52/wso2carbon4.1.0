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

package org.wso2.carbon.cep.core.internal.config.output;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.output.mapping.TupleOutputMapping;
import org.wso2.carbon.cep.core.mapping.output.property.TupleOutputProperty;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class will help to build the Tuple Output Object from a given OMELement
 */
public class TupleOutputMappingHelper {

    private static final Log log = LogFactory.getLog(TupleOutputMappingHelper.class);


    public static TupleOutputMapping fromOM(OMElement tupleMappingElement) {

        TupleOutputMapping tupleOutputMapping = new TupleOutputMapping();
        Iterator<OMElement> iterator = tupleMappingElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                                         CEPConstants.CEP_CONF_ELE_TUPLE_DATA_TYPE_META));

        if (iterator.hasNext()) {
            tupleOutputMapping.setMetaDataProperties(generatePropertyList(iterator.next()));
        }
        iterator = tupleMappingElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                     CEPConstants.CEP_CONF_ELE_TUPLE_DATA_TYPE_CORRELATION));

        if (iterator.hasNext()) {
            tupleOutputMapping.setCorrelationDataProperties(generatePropertyList(iterator.next()));
        }
        iterator = tupleMappingElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                     CEPConstants.CEP_CONF_ELE_TUPLE_DATA_TYPE_PAYLOAD));

        if (iterator.hasNext()) {
            tupleOutputMapping.setPayloadDataProperties(generatePropertyList(iterator.next()));
        }

        return tupleOutputMapping;
    }

    private static List<TupleOutputProperty> generatePropertyList(OMElement dataElement) {
        List<TupleOutputProperty> propertyList = null;
        for (Iterator iterator = dataElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                           CEPConstants.CEP_CONF_ELE_PROPERTY)); iterator.hasNext(); ) {
            if (propertyList == null) {
                propertyList = new ArrayList<TupleOutputProperty>();
            }
            OMElement propertyElement = (OMElement) iterator.next();
            String name = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_NAME));
            String valueOf = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_VALUE_OF));
            String type = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_TYPE));
            propertyList.add(new TupleOutputProperty(name, valueOf, type));
        }
        return propertyList;
    }

    public static OMElement tupleOutputMappingToOM(TupleOutputMapping outputMapping) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement queryTupleOutputMappingElement = factory
                .createOMElement(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                           CEPConstants.CEP_CONF_ELE_TUPLE_MAPPING,
                                           CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));

        OMElement metaDataPropertyElement = factory
                .createOMElement(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                           CEPConstants.CEP_CONF_ELE_TUPLE_DATA_TYPE_META,
                                           CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));

        queryTupleOutputMappingElement.addChild(metaDataPropertyElement);
        List<TupleOutputProperty> metaDataProperties = outputMapping.getMetaDataProperties();
        if (metaDataProperties != null) {
            AddProperties(factory, metaDataPropertyElement, metaDataProperties);
        }


        OMElement correlationDataPropertyElement = factory
                .createOMElement(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                           CEPConstants.CEP_CONF_ELE_TUPLE_DATA_TYPE_CORRELATION,
                                           CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));

        queryTupleOutputMappingElement.addChild(correlationDataPropertyElement);
        List<TupleOutputProperty> correlationDataProperties = outputMapping.getCorrelationDataProperties();
        if (correlationDataProperties != null) {
            AddProperties(factory, correlationDataPropertyElement, correlationDataProperties);
        }


        OMElement payloadDataPropertyElement = factory
                .createOMElement(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                           CEPConstants.CEP_CONF_ELE_TUPLE_DATA_TYPE_PAYLOAD,
                                           CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));

        queryTupleOutputMappingElement.addChild(payloadDataPropertyElement);
        List<TupleOutputProperty> payloadDataProperties = outputMapping.getPayloadDataProperties();
        if (payloadDataProperties != null) {
            AddProperties(factory, payloadDataPropertyElement, payloadDataProperties);
        }
        return queryTupleOutputMappingElement;

    }

    private static void AddProperties(OMFactory factory, OMElement parentElement,
                                      List<TupleOutputProperty> dataProperties) {
        for (TupleOutputProperty outputProperty : dataProperties) {

            OMElement property = factory.createOMElement(new QName(
                    CEPConstants.CEP_CONF_NAMESPACE,
                    CEPConstants.CEP_CONF_ELE_PROPERTY,
                    CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
            property.addAttribute(CEPConstants.CEP_CONF_ATTR_NAME, outputProperty.getName(), null);
            property.addAttribute(CEPConstants.CEP_CONF_ATTR_VALUE_OF, outputProperty.getValueOf(), null);
            property.addAttribute(CEPConstants.CEP_CONF_ATTR_TYPE, outputProperty.getType(), null);
            parentElement.addChild(property);
        }
    }
}
