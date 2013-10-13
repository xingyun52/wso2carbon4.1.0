package org.wso2.carbon.cep.admin.internal;

/**
 * This class is used to define mapping between input XML to CEP Engine Inputs
 * */
public class InputXMLMappingDTO {
    /**
     * Name of the mapping Stream
     * */
    private String stream;

    /**
     * Name of the to be converted event class
     */
    protected String queryEventType;

    /**
     * Definition of the XPaths to be used (Prefix Namespace mapping)
     * */

    private XpathDefinitionDTO[] xpathDefinitionDTOs;

    /**
     * Properties of the mapping
     * */
    private InputXMLPropertyDTO[] InputXMLPropertyDTOs;


    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public InputXMLPropertyDTO[] getProperties() {
        return InputXMLPropertyDTOs;
    }

    public void setProperties(InputXMLPropertyDTO[] InputXMLPropertyDTOs) {
        this.InputXMLPropertyDTOs = InputXMLPropertyDTOs;
    }

    public void setXpathDefinition(XpathDefinitionDTO[] xpathDefinitionDTOs){
        this.xpathDefinitionDTOs = xpathDefinitionDTOs;
    }
     public XpathDefinitionDTO[] getXpathDefinition() {
        return xpathDefinitionDTOs;
    }

    public String getQueryEventType() {
        return queryEventType;
    }

    public void setQueryEventType(String queryEventType) {
        this.queryEventType = queryEventType;
    }
}
