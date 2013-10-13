package org.wso2.carbon.cep.core.mapping.input.mapping;

import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.databridge.commons.Event;

import java.util.Map;

public class QueryEventTypeConvertor {
    public static String mappingClassToType(Class mappingClass) {
        if (mappingClass != null) {
            if (mappingClass.equals(Event.class)) {
                return CEPConstants.CEP_CONF_TYPE_TUPLE;
            } else if (mappingClass.equals(Map.class)) {
                return CEPConstants.CEP_CONF_TYPE_MAP;
            } else {
                return mappingClass.getName();
            }
        } else {
            return CEPConstants.CEP_CONF_TYPE_TUPLE;
        }

    }

    public static Class mappingClassFromType(String type) throws CEPConfigurationException {
        if (type != null) {
            if (type.equals(CEPConstants.CEP_CONF_TYPE_TUPLE)) {
                return Event.class;
            } else if (type.equals(CEPConstants.CEP_CONF_TYPE_MAP)) {
                return Map.class;
            } else {
                try {
                    return Class.forName(type);
                } catch (ClassNotFoundException e) {
                    throw new CEPConfigurationException("No class found matching " + type, e);
                }
            }
        } else {
            return Event.class;
        }
    }
}