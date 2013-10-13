package org.wso2.carbon.cep.core.mapping.input.property;

/**
 * This class contains properties of inputs and outputs
 */
public abstract class InputProperty {

    /**
     * Name of the property
     */
    private String name;

    /**
     * Type of the property
     */
    private String type;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
