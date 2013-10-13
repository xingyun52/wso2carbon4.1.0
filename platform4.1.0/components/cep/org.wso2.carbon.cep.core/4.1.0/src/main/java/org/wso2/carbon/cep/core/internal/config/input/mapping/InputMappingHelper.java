package org.wso2.carbon.cep.core.internal.config.input.mapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * This class will help to build Mappin Object from a given OMElement
 */
public class InputMappingHelper {
    private static final Log log = LogFactory.getLog(InputMappingHelper.class);

    static Method getMethod(Class eventClass, String name)
            throws CEPConfigurationException {
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(eventClass).getPropertyDescriptors()) {
                if (name.equals(pd.getName())) {
                    return pd.getWriteMethod();
                }
            }
            throw new CEPConfigurationException("WriteMethod " + name + " not found in Event Class " + eventClass);
        } catch (IntrospectionException e) {
            throw new CEPConfigurationException("Cannot get the methods for Event Class " + eventClass, e);
        }
    }


}
