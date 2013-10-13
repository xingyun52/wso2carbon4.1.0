/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
 *                                                                             
 * Licensed under the Apache License, Version 2.0 (the "License");             
 * you may not use this file except in compliance with the License.            
 * You may obtain a copy of the License at                                     
 *                                                                             
 *      http://www.apache.org/licenses/LICENSE-2.0                             
 *                                                                             
 * Unless required by applicable law or agreed to in writing, software         
 * distributed under the License is distributed on an "AS IS" BASIS,           
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    
 * See the License for the specific language governing permissions and         
 * limitations under the License.                                              
 */
package org.wso2.carbon.cartridge.mgt.ui.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.adc.mgt.stub.ApplicationManagementServiceStub;
import org.wso2.carbon.utils.CarbonUtils;

import java.rmi.RemoteException;

/**
 * Activator for the Cartridge Management Bundle
 */
public class CartridgeManagementUIActivator implements BundleActivator {

    private static final Log log = LogFactory.getLog(CartridgeManagementUIActivator.class);

    public void start(final BundleContext bundleContext) {

        if (!CarbonUtils.isRunningInStandaloneMode()) {
        // uninstall this component
            Thread th = new Thread() {
                public void run() {
                    try {
                        bundleContext.getBundle().uninstall();
                    } catch (Throwable e) {
                        log.warn("Error occurred while uninstalling webapp-mgt UI bundle", e);
                    }
                }
            };
            try {
                th.join();
            } catch (InterruptedException ignored) {
            }
            th.start();
        }
    }

    public void stop(BundleContext bundleContext) {
        // No implementation required for this method
    }

}
