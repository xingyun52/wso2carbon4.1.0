package org.wso2.carbon.brokermanager.core.internal.build;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

public class Axis2ConfigurationContextObserverImpl
        extends AbstractAxis2ConfigurationContextObserver {
    private static Log log = LogFactory.getLog(Axis2ConfigurationContextObserverImpl.class);

    public void createdConfigurationContext(ConfigurationContext configurationContext) {
        String tenantDomain = PrivilegedCarbonContext.getCurrentContext(
                configurationContext).getTenantDomain();
        int tenantId = PrivilegedCarbonContext.getCurrentContext(
                configurationContext).getTenantId();
        System.out.println("--------------- A new Axis2 Configuration context is created for : " +
                           tenantDomain);
        log.info("Loading Buckets Specific to tenant when the tenant logged in");
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getCurrentContext().setTenantId(tenantId);
            PrivilegedCarbonContext.getCurrentContext().getTenantDomain(true);
            BrokerManagerServiceBuilder.loadConfigurationsFromRegistry();
        } catch (Exception e) {
            log.error("Unable to load brokers from registry ", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

}
