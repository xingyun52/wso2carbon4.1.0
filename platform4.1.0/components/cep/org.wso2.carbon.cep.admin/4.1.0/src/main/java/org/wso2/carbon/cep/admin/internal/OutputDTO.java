package org.wso2.carbon.cep.admin.internal;

/**
 * This class used to configure the out put from the CEP Engine
 */
public class OutputDTO {
    /**
     * OutputDTO Topic which is used to publish processed data from CEP
     */
    private String topic;

    /**
     * XML OutputXMLMappingDTO document
     */
    private OutputXMLMappingDTO outputXmlMappingDTO;

    /**
     * Tuple OutputTupleMappingDTO document
     */
    private OutputTupleMappingDTO outputTupleMappingDTO;
    /**
     * Map OutputMapMappingDTO document
     */
    private OutputMapMappingDTO outputMapMappingDTO;

    /**
     * Name of the broker to be used
     */
    private String brokerName;

    /**
     * Text OutputTextMappingDTO document
     */
    private OutputTextMappingDTO outputTextMappingDTO;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public OutputXMLMappingDTO getOutputXmlMapping() {
        return outputXmlMappingDTO;
    }

    public void setOutputXmlMapping(OutputXMLMappingDTO outputXmlMappingDTO) {
        this.outputXmlMappingDTO = outputXmlMappingDTO;
    }

    public OutputTupleMappingDTO getOutputTupleMapping() {
        return outputTupleMappingDTO;
    }

    public void setOutputTupleMapping(OutputTupleMappingDTO outputTupleMappingDTO) {
        this.outputTupleMappingDTO = outputTupleMappingDTO;
    }

    public OutputMapMappingDTO getOutputMapMapping() {
        return outputMapMappingDTO;
    }

    public void setOutputMapMapping(OutputMapMappingDTO outputMapMappingDTO) {
        this.outputMapMappingDTO = outputMapMappingDTO;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public OutputTextMappingDTO getOutputTextMapping() {
        return outputTextMappingDTO;
    }

    public void setOutputTextMapping(OutputTextMappingDTO outputTextMappingDTO) {
        this.outputTextMappingDTO = outputTextMappingDTO;
    }
}
