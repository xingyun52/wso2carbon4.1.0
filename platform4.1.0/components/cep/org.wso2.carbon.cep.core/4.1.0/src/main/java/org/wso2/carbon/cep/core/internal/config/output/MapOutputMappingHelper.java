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
import org.wso2.carbon.cep.core.mapping.output.mapping.MapOutputMapping;
import org.wso2.carbon.cep.core.mapping.output.property.MapOutputProperty;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapOutputMappingHelper {
    private static final Log log = LogFactory.getLog(MapOutputMappingHelper.class);

    public static MapOutputMapping fromOM(OMElement mapMappingElement) {
        MapOutputMapping mapOutputMapping = new MapOutputMapping();


        List<MapOutputProperty> outputPropertyList = null;
        if (mapMappingElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_PROPERTY)) != null) {
            for (Iterator iterator = mapMappingElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                    CEPConstants.CEP_CONF_ELE_PROPERTY)); iterator.hasNext(); ) {
                if (outputPropertyList == null) {
                    outputPropertyList = new ArrayList<MapOutputProperty>();
                }
                OMElement propertyElement = (OMElement) iterator.next();

                String name = (propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_NAME)));
                String valueOf = (propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_VALUE_OF)));
                outputPropertyList.add(new MapOutputProperty(name, valueOf));
            }
        }
        mapOutputMapping.setPropertyList(outputPropertyList);

        return mapOutputMapping;

    }


    public static OMElement mapOutputMappingToOM(MapOutputMapping outputMapping) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement queryMapOutputMapping = factory
                .createOMElement(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                           CEPConstants.CEP_CONF_ELE_MAP_MAPPING,
                                           CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        if (outputMapping.getPropertyList() != null) {
            for (MapOutputProperty mapOutputProperty : outputMapping.getPropertyList()) {

                OMElement property = factory.createOMElement(new QName(
                        CEPConstants.CEP_CONF_NAMESPACE,
                        CEPConstants.CEP_CONF_ELE_PROPERTY,
                        CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
                property.addAttribute(CEPConstants.CEP_CONF_ATTR_NAME, mapOutputProperty.getName(), null);
                property.addAttribute(CEPConstants.CEP_CONF_ATTR_VALUE_OF, mapOutputProperty.getValueOf(), null);
                queryMapOutputMapping.addChild(property);
            }
        }
        return queryMapOutputMapping;

    }
}
