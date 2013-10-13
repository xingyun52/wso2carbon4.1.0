package org.wso2.carbon.cep.admin.internal;

/**
 * This class contains properties of inputs and outputs
 */
public class InputXMLPropertyDTO {

    /**
     * Name of the property
     */
    private String name;

    /**
     * Type of the property
     */
    private String type;

    /**
     * Xpath of the property
     */
    private String xpath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
