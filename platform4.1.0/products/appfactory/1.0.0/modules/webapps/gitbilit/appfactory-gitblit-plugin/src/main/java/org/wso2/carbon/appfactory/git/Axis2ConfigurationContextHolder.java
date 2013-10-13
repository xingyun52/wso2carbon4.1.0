package org.wso2.carbon.appfactory.git;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to hold axis2 configuration context
 */
public class Axis2ConfigurationContextHolder {
    private static final Logger log = LoggerFactory.getLogger(Axis2ConfigurationContextHolder.class);
    private static Axis2ConfigurationContextHolder holder;
    private ConfigurationContext configurationContext;

    public ConfigurationContext getConfigurationContext() {
        return configurationContext;
    }

    public static Axis2ConfigurationContextHolder getHolder() {
        if(holder==null){
            holder=new Axis2ConfigurationContextHolder();
            try {
                log.info("Creating Default Axis2 ConfigurationContext");
                holder.configurationContext= ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
            } catch (AxisFault fault) {
                log.error("Error occurred while initializing  ConfigurationContext", fault);
            }
        }
        return holder;
    }



    private Axis2ConfigurationContextHolder() {
    }

}
