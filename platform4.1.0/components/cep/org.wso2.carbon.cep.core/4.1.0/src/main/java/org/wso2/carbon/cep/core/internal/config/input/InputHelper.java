package org.wso2.carbon.cep.core.internal.config.input;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.internal.config.input.mapping.MapInputMappingHelper;
import org.wso2.carbon.cep.core.internal.config.input.mapping.TupleInputMappingHelper;
import org.wso2.carbon.cep.core.internal.config.input.mapping.XMLInputMappingHelper;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.input.Input;
import org.wso2.carbon.cep.core.mapping.input.mapping.MapInputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.TupleInputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.XMLInputMapping;

import javax.xml.namespace.QName;

/**
 * This class helps to build an Input Object from a given OMElement
 */
public class InputHelper {
    private static final Log log = LogFactory.getLog(InputHelper.class);

    public static Input fromOM(OMElement inputElement)
            throws CEPConfigurationException {

        Input input = new Input();
        String topic = inputElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ELE_TOPIC));
        input.setTopic(topic);

        String brokerProxy =
                inputElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ELE_BROKER_NAME));
        input.setBrokerName(brokerProxy);


        // if there are more mappings we can check for them and process accordingly.
        OMElement xmlMappingElement =
                inputElement.getFirstChildWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                             CEPConstants.CEP_CONF_ELE_XML_MAPPING));

        if (xmlMappingElement != null) {
            input.setInputMapping(XMLInputMappingHelper.fromOM(xmlMappingElement));
            input.getInputMapping().setStream( xmlMappingElement.getAttributeValue(new QName(CEPConstants.CEP_REGISTRY_STREAM)));
        } else {
            OMElement tupleMappingElement =
                    inputElement.getFirstChildWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                 CEPConstants.CEP_CONF_ELE_TUPLE_MAPPING));

            if (tupleMappingElement != null) {
                input.setInputMapping(TupleInputMappingHelper.fromOM(tupleMappingElement));
                input.getInputMapping().setStream( tupleMappingElement.getAttributeValue(new QName(CEPConstants.CEP_REGISTRY_STREAM)));

            } else {

                OMElement mapMappingElement =
                        inputElement.getFirstChildWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                                                                     CEPConstants.CEP_CONF_ELE_MAP_MAPPING));
                if (mapMappingElement != null){
                    input.setInputMapping(MapInputMappingHelper.fromOM(mapMappingElement));
                    input.getInputMapping().setStream(mapMappingElement.getAttributeValue(new QName(CEPConstants.CEP_REGISTRY_STREAM)));
                }
            }
        }
        return input;
    }
    
	public static OMElement inputToOM(Input input) {
		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMElement inputChild = factory.createOMElement(new QName(
				CEPConstants.CEP_CONF_NAMESPACE,
				CEPConstants.CEP_CONF_ELE_INPUT,
				CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        String inputName = input.getTopic();
        String inputBrokerName = input.getBrokerName();
        inputChild.addAttribute(CEPConstants.CEP_CONF_ELE_TOPIC, inputName,
                                null);
        inputChild.addAttribute(CEPConstants.CEP_CONF_ELE_BROKER_NAME,
                                inputBrokerName, null);

		if (input.getInputMapping() instanceof XMLInputMapping) {
			XMLInputMapping xmlInputMap = (XMLInputMapping) input
					.getInputMapping();
			OMElement xmlInputMapping = XMLInputMappingHelper
					.xmlInputMappingToOM(xmlInputMap);
            inputChild.addChild(xmlInputMapping);

		}else if (input.getInputMapping() instanceof TupleInputMapping) {
            TupleInputMapping tupleInputMap = (TupleInputMapping) input
					.getInputMapping();
			OMElement tupleInputMapping = TupleInputMappingHelper
					.tupleInputMappingToOM(tupleInputMap);
			inputChild.addChild(tupleInputMapping);
		}else if (input.getInputMapping() instanceof MapInputMapping) {
            MapInputMapping mapInputMap = (MapInputMapping) input
					.getInputMapping();
			OMElement mapInputMapping = MapInputMappingHelper
					.mapInputMappingToOM(mapInputMap);
			inputChild.addChild(mapInputMapping);
		}
		return inputChild;
	}

}
