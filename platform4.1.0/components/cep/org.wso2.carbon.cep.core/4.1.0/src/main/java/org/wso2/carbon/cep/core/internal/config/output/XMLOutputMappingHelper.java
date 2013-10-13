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
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.output.mapping.XMLOutputMapping;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * This class will help to build the Output Mapping from a given OMELement
 */
public class XMLOutputMappingHelper {

    private static final Log log = LogFactory.getLog(XMLOutputMappingHelper.class);


    public static XMLOutputMapping fromOM(OMElement xmlMappingElement) {
        XMLOutputMapping xmlOutMapping = new XMLOutputMapping();
        String xmlMappingText = xmlMappingElement.toString();
        int index1 = xmlMappingText.indexOf(">");
        int index2 = xmlMappingText.lastIndexOf("<");
        xmlMappingText = xmlMappingText.substring(index1 + 1, index2);
        xmlOutMapping.setMappingXMLText(xmlMappingText);
        return xmlOutMapping;
    }

	public static OMElement xmlOutputMappingToOM(
			XMLOutputMapping xmlOutputMapping) {
		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMElement queryXMLOutputMapping = factory.createOMElement(new QName(
				CEPConstants.CEP_CONF_NAMESPACE,
				CEPConstants.CEP_CONF_ELE_XML_MAPPING,
				CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
		String mappingXMLText = xmlOutputMapping.getMappingXMLText();
        try {
            queryXMLOutputMapping.addChild(AXIOMUtil.stringToOM(mappingXMLText));
        } catch (XMLStreamException e) {
            log.error("XML mapping is not in XML format :" +mappingXMLText);
            factory.createOMText(queryXMLOutputMapping, mappingXMLText,
                                 XMLStreamReader.CDATA);
        }
		return queryXMLOutputMapping;
}





}
