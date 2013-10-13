package org.wso2.carbon.cep.core.internal.config.input.mapping;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.XpathDefinition;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.input.mapping.InputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.QueryEventTypeConvertor;
import org.wso2.carbon.cep.core.mapping.input.mapping.XMLInputMapping;
import org.wso2.carbon.cep.core.mapping.input.property.XMLInputProperty;
import org.wso2.carbon.databridge.commons.Event;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This Class will help to build XMLMapping object from a given OMElement
 */
public class XMLInputMappingHelper {

    private static final Log log = LogFactory.getLog(XMLInputMappingHelper.class);


    public static InputMapping fromOM(OMElement xmlMappingElement)
            throws CEPConfigurationException {

        XMLInputMapping xmlInputMapping = new XMLInputMapping();

        String stream =
                xmlMappingElement.getAttributeValue(
                        new QName(CEPConstants.CEP_CONF_ATTR_STREAM));
        xmlInputMapping.setStream(stream);

        String className = xmlMappingElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_QUERY_EVENT_TYPE));
        xmlInputMapping.setMappingClass(QueryEventTypeConvertor.mappingClassFromType(className));




        for (Iterator iterator = xmlMappingElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                                 CEPConstants.CEP_CONF_ELE_XPATH_DEFINITON)); iterator.hasNext(); ) {
            OMElement xpathDefinitionElement = (OMElement) iterator.next();
            String prefix = xpathDefinitionElement.getAttributeValue(
                    new QName(CEPConstants.CEP_CONF_ATTR_PREFIX));
            String namespace = xpathDefinitionElement.getAttributeValue(
                    new QName(CEPConstants.CEP_CONF_ATTR_NAMESPACE));
            XpathDefinition xpathDefinition = new XpathDefinition();
            xpathDefinition.setPrefix(prefix);
            xpathDefinition.setNamespace(namespace);
            xmlInputMapping.addXpathDefinition(xpathDefinition);
        }

        for (Iterator iterator = xmlMappingElement.getChildrenWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                                 CEPConstants.CEP_CONF_ELE_PROPERTY)); iterator.hasNext(); ) {
            OMElement propertyElement = (OMElement) iterator.next();
            XMLInputProperty property = InputPropertyHelper.xmlPropertyFromOM(propertyElement);
            xmlInputMapping.addProperty(property);
            if (xmlInputMapping.getMappingClass() != Map.class && xmlInputMapping.getMappingClass() != Event.class) {
                xmlInputMapping.putWriteMethod(property.getName(), InputMappingHelper.getMethod(xmlInputMapping.getMappingClass(), property.getName()));
            }
        }

        return xmlInputMapping;
    }


	public static OMElement xmlInputMappingToOM(XMLInputMapping xmlInputMapping) {
		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMElement omXMLInputMapping = factory.createOMElement(new QName(
				CEPConstants.CEP_CONF_NAMESPACE,
				CEPConstants.CEP_CONF_ELE_XML_MAPPING,
				CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
		String inputStream = xmlInputMapping.getStream();
		List<XpathDefinition> xpathDefinitionList = xmlInputMapping
				.getXpathDefinitionList();

        Class mappingClass = xmlInputMapping.getMappingClass();
        String className = QueryEventTypeConvertor.mappingClassToType(mappingClass);
        omXMLInputMapping.addAttribute(CEPConstants.CEP_CONF_ATTR_QUERY_EVENT_TYPE, className, null);

        List<XMLInputProperty> xmlPropertyList = xmlInputMapping.getProperties();
		for (XpathDefinition xpathDefinition : xpathDefinitionList) {
			OMElement xpathChild = factory.createOMElement(new QName(
					CEPConstants.CEP_CONF_NAMESPACE,
					CEPConstants.CEP_CONF_ELE_XPATH_DEFINITON,
					CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
			String inputXpathPrefix = xpathDefinition.getPrefix();
			String inputXpathNameSpace = xpathDefinition.getNamespace();
			xpathChild.addAttribute(CEPConstants.CEP_CONF_ATTR_PREFIX,
					inputXpathPrefix, null);
			xpathChild.addAttribute(CEPConstants.CEP_CONF_ATTR_NAMESPACE,
					inputXpathNameSpace, null);
			omXMLInputMapping.addChild(xpathChild);
		}
		for (XMLInputProperty xmlProperty : xmlPropertyList) {
			OMElement propertychild = InputPropertyHelper
					.xmlPropertyToOM(xmlProperty);
			omXMLInputMapping.addChild(propertychild);
		}
		omXMLInputMapping.addAttribute(CEPConstants.CEP_CONF_ATTR_STREAM,
				inputStream, null);
		return omXMLInputMapping;
	}





}
