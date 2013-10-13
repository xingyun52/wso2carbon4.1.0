package org.wso2.carbon.apimgt.gateway.handlers.throttling;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.neethi.PolicyEngine;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.wso2.throttle.*;

public class ApplicationThrottleController {
    
    public static final String APPLICATION_THROTTLE_POLICY_KEY = "gov:/apimgt/applicationdata/tiers.xml";

    public static final String APP_THROTTLE_CONTEXT_PREFIX = "APP_THROTTLE_CONTEXT_";

    private static final Log log = LogFactory.getLog(ApplicationThrottleController.class);

    private static final Object lock = new Object();
    
    public static ThrottleContext getApplicationThrottleContext(MessageContext synCtx, ConfigurationContext cc, 
                                                                String applicationId){
        synchronized (lock) {
            Object throttleContext = cc.getProperty(APP_THROTTLE_CONTEXT_PREFIX + applicationId);
            if(throttleContext == null){
                return createThrottleContext(synCtx, cc, applicationId);
            }
            return (ThrottleContext)throttleContext;
        }
    }

    private static ThrottleContext createThrottleContext(MessageContext synCtx, ConfigurationContext cc, String applicationId){

        Entry entry = synCtx.getConfiguration().getEntryDefinition(APPLICATION_THROTTLE_POLICY_KEY);
        if (entry == null) {
            handleException("Cannot find throttling policy using key: " + APPLICATION_THROTTLE_POLICY_KEY);
            return null;
        }

        Object entryValue = synCtx.getEntry(APPLICATION_THROTTLE_POLICY_KEY);
        if (entryValue == null || !(entryValue instanceof OMElement)) {
            handleException("Unable to load throttling policy using key: " + APPLICATION_THROTTLE_POLICY_KEY);
            return null;
        }

        try {
            Throttle throttle = ThrottleFactory.createMediatorThrottle(PolicyEngine.getPolicy((OMElement) entryValue));
            ThrottleContext context = throttle.getThrottleContext(ThrottleConstants.ROLE_BASED_THROTTLE_KEY);
            cc.setProperty(APP_THROTTLE_CONTEXT_PREFIX + applicationId, context);
            return context;
        } catch (ThrottleException e) {
            handleException("Error processing the throttling policy", e);
        }
        return null;
    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    private static void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }
}
