package org.wso2.carbon.cep.core.internal.config.output;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.mapping.output.Output;
import org.wso2.carbon.cep.core.mapping.output.mapping.MapOutputMapping;
import org.wso2.carbon.cep.core.mapping.output.mapping.TextOutputMapping;
import org.wso2.carbon.cep.core.mapping.output.mapping.TupleOutputMapping;
import org.wso2.carbon.cep.core.mapping.output.mapping.XMLOutputMapping;

import javax.xml.namespace.QName;

/**
 * This class will help to build Output Object from a given OMELement
 */
public class OutputHelper {
    private static final Log log = LogFactory.getLog(OutputHelper.class);

    public static Output fromOM(OMElement outputElement) {
        Output output = new Output();

        String topic = outputElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ATTR_TOPIC));
        output.setTopic(topic);

        String brokerProxy = outputElement.getAttributeValue(new QName(CEPConstants.CEP_CONF_ELE_BROKER_NAME));
        output.setBrokerName(brokerProxy);

        OMElement xmlMappingElement = outputElement.getFirstChildWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_XML_MAPPING));
        if (xmlMappingElement != null) {
            output.setOutputMapping(XMLOutputMappingHelper.fromOM(xmlMappingElement));
        }
        if (output.getOutputMapping() == null) {
            OMElement tupleMappingElement = outputElement.getFirstChildWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                    CEPConstants.CEP_CONF_ELE_TUPLE_MAPPING));
            if (tupleMappingElement != null) {
                TupleOutputMapping tupleOutputMapping = TupleOutputMappingHelper.fromOM(tupleMappingElement);
                output.setOutputMapping(tupleOutputMapping);
            }
        }

        if (output.getOutputMapping() == null) {
            OMElement textMappingElement = outputElement.getFirstChildWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                    CEPConstants.CEP_CONF_ELE_TEXT_MAPPING));
            if (textMappingElement != null) {
                TextOutputMapping textOutputMapping = TextOutputMappingHelper.fromOM(textMappingElement);
                output.setOutputMapping(textOutputMapping);
            }
        }

        if (output.getOutputMapping() == null) {
            OMElement mapMappingElement = outputElement.getFirstChildWithName(new QName(CEPConstants.CEP_CONF_NAMESPACE,
                    CEPConstants.CEP_CONF_ELE_MAP_MAPPING));
            if (mapMappingElement != null) {
                output.setOutputMapping(MapOutputMappingHelper.fromOM(mapMappingElement));
            }
        }


        return output;
    }

    public static OMElement outputToOM(Output output) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement queryOutput = factory.createOMElement(new QName(
                CEPConstants.CEP_CONF_NAMESPACE,
                CEPConstants.CEP_CONF_ELE_OUTPUT,
                CEPConstants.CEP_CONF_CEP_NAME_SPACE_PREFIX));
        String queryOutputName = output.getTopic();
        String queryOutputBrokerName = output.getBrokerName();
        queryOutput.addAttribute(CEPConstants.CEP_REGISTRY_TOPIC,
                queryOutputName, null);
        queryOutput.addAttribute(CEPConstants.CEP_CONF_ELE_BROKER_NAME,
                queryOutputBrokerName, null);
        if (output.getOutputMapping() instanceof XMLOutputMapping) {
            OMElement queryXMLOutputMapping = XMLOutputMappingHelper
                    .xmlOutputMappingToOM((XMLOutputMapping) output
                            .getOutputMapping());
            queryOutput.addChild(queryXMLOutputMapping);
        } else if (output.getOutputMapping() instanceof TextOutputMapping) {
            OMElement textOutputMapping = TextOutputMappingHelper
                    .textOutputMappingToOM((TextOutputMapping) output
                            .getOutputMapping());
            queryOutput.addChild(textOutputMapping);
        } else if (output.getOutputMapping() instanceof MapOutputMapping) {
            OMElement mapOutputMapping = MapOutputMappingHelper
                    .mapOutputMappingToOM((MapOutputMapping) output
                            .getOutputMapping());
            queryOutput.addChild(mapOutputMapping);
        } else if (output.getOutputMapping() instanceof TupleOutputMapping) {
            OMElement tupleOutputMapping = TupleOutputMappingHelper
                    .tupleOutputMappingToOM((TupleOutputMapping) output
                            .getOutputMapping());
            queryOutput.addChild(tupleOutputMapping);

        }
        return queryOutput;
    }
}
