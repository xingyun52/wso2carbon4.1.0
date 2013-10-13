package org.wso2.carbon.cep.core.mapping.output.property;

/**
 * This class contains properties of inputs and outputs
 */
public class MapOutputProperty {

    /**
     * Name of the property
     */
    private String name;

    /**
     * Value of the property
     */
    private String valueOf;

    public MapOutputProperty(String name, String valueOf) {
        this.name=name;
        this.valueOf=valueOf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValueOf() {
        return valueOf;
    }

    public void setValueOf(String valueOf) {
        this.valueOf = valueOf;
    }
}
