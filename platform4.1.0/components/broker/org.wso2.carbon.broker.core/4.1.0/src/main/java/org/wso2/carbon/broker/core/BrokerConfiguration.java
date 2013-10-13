package org.wso2.carbon.broker.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class contain the configuration details of the broker
 */

public class BrokerConfiguration {
    /**
     * logical name use to identify this configuration
     */
    private String name;

    /**
     * broker  type for this configuration
     */
    private String type;

    /**
     * property values - these properties are depends on the properties defined in the
     * broker type. there must be a property here for each property defined in broker type
     */
    private Map<String, String> properties;

    public BrokerConfiguration() {
        this.properties = new ConcurrentHashMap<String, String>();
    }

    public void addProperty(String name, String value) {
        this.properties.put(name, value);
    }

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

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BrokerConfiguration that = (BrokerConfiguration) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
