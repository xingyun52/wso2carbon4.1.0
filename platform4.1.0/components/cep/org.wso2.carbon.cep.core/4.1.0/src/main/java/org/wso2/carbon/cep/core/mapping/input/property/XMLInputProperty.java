package org.wso2.carbon.cep.core.mapping.input.property;

/**
 * This class contains properties of inputs and outputs
 * */
public class XMLInputProperty extends InputProperty {

    /**
     * Xpath of the property
     * */
    private String xpath;

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

}
