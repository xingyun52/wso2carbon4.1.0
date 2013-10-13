package org.wso2.carbon.governance.registry.extensions.listeners;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.services.callback.LoginEvent;
import org.wso2.carbon.core.services.callback.LoginListener;
import org.wso2.carbon.governance.registry.extensions.utils.CommonUtil;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.internal.RegistryCoreServiceComponent;
import org.wso2.carbon.registry.core.service.RegistryService;

public class RxtLoader implements LoginListener {
        private Log log = LogFactory.getLog(RxtLoader.class);

        public void onLogin(Registry registry, LoginEvent loginEvent) {
            RegistryService service = RegistryCoreServiceComponent.getRegistryService();
            try {
                CommonUtil.addRxtConfigs(service.getRegistry(loginEvent.getUsername(), loginEvent.getTenantId()), loginEvent.getTenantId());
            } catch (RegistryException e) {
                log.error("Failed to add rxt files to registry", e);
            }
        }
    }
