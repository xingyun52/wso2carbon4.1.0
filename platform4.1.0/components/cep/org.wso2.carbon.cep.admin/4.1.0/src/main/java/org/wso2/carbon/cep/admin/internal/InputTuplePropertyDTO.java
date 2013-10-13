package org.wso2.carbon.cep.admin.internal;

/**
 * This class contains properties of inputs and outputs
 */
public class InputTuplePropertyDTO {

    /**
     * Name of the property
     */
    private String name;

    /**
     * Type of the property
     */
    private String type;

    /**
     * InputName of the property
     */
    private String inputName;

    /**
     * Type of the type of the data mataData, correlationData, payloadData field
     */
    private String inputDataType;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInputDataType() {
        return inputDataType;
    }

    public void setInputDataType(String inputDataType) {
        this.inputDataType = inputDataType;
    }

    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
