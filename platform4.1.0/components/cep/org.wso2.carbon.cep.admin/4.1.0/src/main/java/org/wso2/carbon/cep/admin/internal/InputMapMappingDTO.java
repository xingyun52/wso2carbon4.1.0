package org.wso2.carbon.cep.admin.internal;

/**
 * This class is used to define mapping between input XML to CEP Engine Inputs
 * */
public class InputMapMappingDTO {


    /**
     * Name of the mapping Stream
     * */
    private String stream;

    /**
     * Name of the to be converted event class
     */
    protected String queryEventType;

    /**
     * Properties of the mapping
     * */
    private InputMapPropertyDTO[] inputMapPropertyDTOs;

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getQueryEventType() {
        return queryEventType;
    }

    public void setQueryEventType(String queryEventType) {
        this.queryEventType = queryEventType;
    }

    public InputMapPropertyDTO[] getProperties() {
        return inputMapPropertyDTOs;
    }

    public void setProperties(InputMapPropertyDTO[] inputMapPropertyDTOs) {
        this.inputMapPropertyDTOs = inputMapPropertyDTOs;
    }


}
