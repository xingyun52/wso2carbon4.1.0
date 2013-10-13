/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.cep.core.internal.config;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class ProviderConfigurationHelper {
    private static final Log log = LogFactory.getLog(ProviderConfigurationHelper.class);

    public static Properties propertiesFromOM(OMElement configurationElement) {

        Properties properties = new Properties();

        for (Iterator iter = configurationElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                                CEPConstants.CEP_CONF_ELE_PROPERTY)); iter.hasNext(); ) {
            OMElement propertyElement = (OMElement) iter.next();

            if (propertyElement.getAttribute(new QName(CEPConstants.CEP_CONT_ATTR_NAME)) != null) {
                String name = propertyElement.getAttribute(new QName(CEPConstants.CEP_CONT_ATTR_NAME)).getAttributeValue();
                String value = propertyElement.getText();
                properties.setProperty(name, value);
            }
        }
        return properties;
    }

    public static String engineProviderFromOM(OMElement configurationElement) {
        return configurationElement.getAttributeValue(
                new QName(CEPConstants.CEP_CONF_ATTR_ENGINE_PROVIDER));
    }


    public static OMElement providerConfigurationToOM(String bucketEngineProvider,
                                                      Properties properties) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement propertyConfig = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_PROVIDER_CONFIG,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        propertyConfig.addAttribute(CEPConstants.CEP_CONF_ATTR_ENGINE_PROVIDER, bucketEngineProvider,
                                    null);
        if (properties != null) {
            for (Map.Entry entry : properties.entrySet()) {
                OMElement propertyOmElement = factory.createOMElement(new QName(
                        CEPConstants.CEP_CONF_NAMESPACE,
                        CEPConstants.CEP_CONF_ELE_PROPERTY,
                        CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
                propertyOmElement.setText(entry.getValue().toString());
                propertyOmElement.addAttribute(CEPConstants.CEP_CONF_ATTR_NAME, entry.getKey().toString(),
                                               null);
                propertyConfig.addChild(propertyOmElement);
            }
        }
        return propertyConfig;
    }

}
