package org.wso2.carbon.cep.core.internal.config.input.mapping;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.input.property.MapInputProperty;
import org.wso2.carbon.cep.core.mapping.input.property.TupleInputProperty;
import org.wso2.carbon.cep.core.mapping.input.property.XMLInputProperty;

import javax.xml.namespace.QName;

/**
 * This class will help to build InputProperty object from a given OMElement
 */
public class InputPropertyHelper {
    public static XMLInputProperty xmlPropertyFromOM(OMElement propertyElement) {
        XMLInputProperty property = new XMLInputProperty();
        String name = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_NAME));
        String xpath = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_XPATH));
        String type = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_TYPE));

        property.setName(name);
        property.setXpath(xpath);
        property.setType(type);

        return property;
    }

    public static OMElement xmlPropertyToOM(XMLInputProperty xmlProperty) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement propertyChild = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_PROPERTY,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        String propertyName = xmlProperty.getName();
        String propertyXPath = xmlProperty.getXpath();
        String propertyType = xmlProperty.getType();
        propertyChild.addAttribute(CEPConstants.CEP_CONF_ATTR_NAME,
                                   propertyName, null);
        if (propertyXPath != null) {
            propertyChild.addAttribute(CEPConstants.CEP_CONF_ATTR_XPATH,
                                       propertyXPath, null);
        }
        if (propertyType != null) {
            propertyChild.addAttribute(CEPConstants.CEP_CONT_ATTR_TYPE,
                                       propertyType, null);
        }
        return propertyChild;
    }

    public static TupleInputProperty tuplePropertyFromOM(OMElement propertyElement) {
        TupleInputProperty property = new TupleInputProperty();
        String name = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_NAME));
        String dataType = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_INPUT_TUPLE_DATA_TYPE));
        String inputName = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_INPUT_NAME));
        String type = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_TYPE));

        property.setName(name);
        property.setInputName(inputName);
        property.setInputDataType(dataType);
        property.setType(type);

        return property;
    }

    public static OMElement tuplePropertyToOM(TupleInputProperty tupleProperty) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement propertyElement = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_PROPERTY,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        String name = tupleProperty.getName();
        String inputName = tupleProperty.getInputName();
        String dataType = tupleProperty.getInputDataType();
        String type = tupleProperty.getType();
        propertyElement.addAttribute(CEPConstants.CEP_CONF_ATTR_NAME, name, null);
        propertyElement.addAttribute(CEPConstants.CEP_CONF_ATTR_INPUT_NAME, inputName, null);
        propertyElement.addAttribute(CEPConstants.CEP_CONF_ATTR_INPUT_TUPLE_DATA_TYPE, dataType, null);
        propertyElement.addAttribute(CEPConstants.CEP_CONF_ATTR_TYPE, type, null);
        return propertyElement;
    }

    public static MapInputProperty mapPropertyFromOM(OMElement propertyElement) {
        MapInputProperty inputProperty = new MapInputProperty();
        String name = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_NAME));
        String inputName = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_INPUT_NAME));
        String type = propertyElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_TYPE));

        inputProperty.setName(name);
        inputProperty.setInputName(inputName);
        inputProperty.setType(type);

        return inputProperty;
    }

    public static OMElement mapPropertyToOM(MapInputProperty mapInputProperty) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement propertyElement = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_PROPERTY,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        String name = mapInputProperty.getName();
        String inputName = mapInputProperty.getInputName();
        String type = mapInputProperty.getType();
        propertyElement.addAttribute(CEPConstants.CEP_CONF_ATTR_NAME, name, null);
        propertyElement.addAttribute(CEPConstants.CEP_CONF_ATTR_INPUT_NAME, inputName, null);
        propertyElement.addAttribute(CEPConstants.CEP_CONF_ATTR_TYPE, type, null);
        return propertyElement;
    }




}
