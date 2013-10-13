package org.wso2.carbon.appfactory.core.dto;

public class APIMetadata {
    private String type;
    private String value;
    private String reference;
    private String environment;

    public APIMetadata(String type, String value, String reference) {
        this.type = type;
        this.value = value;
        this.reference = reference;
    }
    public APIMetadata(String type, String value, String reference,String environment) {
        this.type = type;
        this.value = value;
        this.reference = reference;
        this.environment = environment;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}