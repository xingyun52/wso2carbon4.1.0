package org.wso2.carbon.cep.core.mapping.input.property;

/**
 * This class contains properties of inputs and outputs
 */
public class MapInputProperty extends InputProperty {


    /**
     * InputName of the property
     */
    private String inputName;


    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }
}
