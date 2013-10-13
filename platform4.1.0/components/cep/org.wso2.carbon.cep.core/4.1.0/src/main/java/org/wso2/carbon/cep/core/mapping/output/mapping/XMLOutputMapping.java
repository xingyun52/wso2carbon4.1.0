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

package org.wso2.carbon.cep.core.mapping.output.mapping;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.exception.CEPEventProcessingException;
import org.wso2.carbon.cep.core.internal.util.CEPRegistryUtils;

import javax.xml.stream.XMLStreamException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Iterator;

public class XMLOutputMapping extends OutputMapping {

    private static final Log log = LogFactory.getLog(XMLOutputMapping.class);

    private String mappingXMLText;
    private String xmlTemplate;

    public String getMappingXMLText() {
        return mappingXMLText;
    }

    public void setMappingXMLText(String mappingXMLText) {
        this.mappingXMLText = mappingXMLText;
        xmlTemplate = mappingXMLText;
        if (CEPRegistryUtils.isRegistryPath(mappingXMLText)) {
            xmlTemplate = CEPRegistryUtils.getResource(mappingXMLText);
        }
    }

    public Object convert(Object event) throws CEPEventProcessingException {

        OMElement payload = null;
        try {
            payload = AXIOMUtil.stringToOM(this.xmlTemplate);
        } catch (XMLStreamException e) {
            log.error("Error in creating OM Element from given XML Mapping text " + e);
        }
        if (payload != null && payload.getChildElements() != null) {
            return buildOuputOMElement(event, payload, this.methodCache);
        }
        return null;
    }

    public OMElement buildOuputOMElement(Object event, OMElement omElement, Map<Class, Map<String, Method>> methodCache)
            throws CEPEventProcessingException {
        Iterator<OMElement> iterator = omElement.getChildElements();
        while (iterator.hasNext()) {
            OMElement childElement = iterator.next();
            Iterator<OMAttribute> iteratorAttr = childElement.getAllAttributes();
            while (iteratorAttr.hasNext()) {
                OMAttribute omAttribute = iteratorAttr.next();
                String text = omAttribute.getAttributeValue();
                if (text != null) {
                    if (text.indexOf("{") > -1 && text.indexOf("}") > 0) {
                        String propertyToReplace = text.substring(text.indexOf("{") + 1, text.indexOf("}"));
                        String value = getPropertyValue(event, propertyToReplace).toString();
                        omAttribute.setAttributeValue(value);
                    }
                }
            }

            String text = childElement.getText();
            if (text != null) {
                if (text.indexOf("{") > -1 && text.indexOf("}") > 0) {
                    String propertyToReplace = text.substring(text.indexOf("{") + 1, text.indexOf("}"));
                    String value = getPropertyValue(event, propertyToReplace).toString();
                    childElement.setText(value);
                }
            }

            buildOuputOMElement(event, childElement, methodCache);
        }
        return omElement;
    }


}
