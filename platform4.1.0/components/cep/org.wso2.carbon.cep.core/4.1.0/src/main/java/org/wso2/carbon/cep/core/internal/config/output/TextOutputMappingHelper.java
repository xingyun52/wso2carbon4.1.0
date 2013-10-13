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
import org.wso2.carbon.cep.core.mapping.output.mapping.TextOutputMapping;

import javax.xml.namespace.QName;

/**
 * This class will help to build the Output Mapping from a given OMELement
 */
public class TextOutputMappingHelper {

    private static final Log log = LogFactory.getLog(TextOutputMappingHelper.class);


    public static TextOutputMapping fromOM(OMElement textMappingElement) {
        TextOutputMapping textOutputMapping = new TextOutputMapping();
        String textMappingText = textMappingElement.toString();
        int index1 = textMappingText.indexOf(">");
        int index2 = textMappingText.lastIndexOf("<");
        textMappingText = textMappingText.substring(index1 + 1, index2);
        textOutputMapping.setMappingText(textMappingText);
        return textOutputMapping;
    }

    public static OMElement textOutputMappingToOM(
            TextOutputMapping textOutputMapping) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement queryXMLOutputMapping = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_TEXT_MAPPING,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        String mappingText = textOutputMapping.getMappingText();
        queryXMLOutputMapping.setText(mappingText);
        return queryXMLOutputMapping;
    }


}
