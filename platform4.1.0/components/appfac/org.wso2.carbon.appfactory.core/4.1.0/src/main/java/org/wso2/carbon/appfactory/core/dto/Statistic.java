package org.wso2.carbon.appfactory.core.dto;

public class Statistic {

    String name;
    String value;
    
    public Statistic(String name, String val) {
        this.name = name;
        this.value = val;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Statistic [name=" + name + ", value=" + value + "]";
    }

    
    
}
