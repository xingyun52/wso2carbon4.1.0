package org.wso2.carbon.cep.core.mapping.input.property;

/**
 * This class contains properties of inputs and outputs
 */
public class TupleInputProperty extends InputProperty {

    /**
     * InputName of the property
     */
    private String inputName;

    /**
     * Type of the type of the data mataData, correlationData, payloadData field
     */
    private String inputDataType;


    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public String getInputDataType() {
        return inputDataType;
    }

    public void setInputDataType(String inputDataType) {
        this.inputDataType = inputDataType;
    }

}
