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

package org.wso2.carbon.cep.statistics.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.cep.statistics.CEPStatisticsManagerInterface;

/**
 * @scr.component name="cepstatisticmonitoringservice.component" immediate="true"
 */
public class CEPStatisticsMonitorDS {
    private static final Log log = LogFactory.getLog(CEPStatisticsMonitorDS.class);

    /**
     * initialize the cep service here.
     *
     * @param context
     */
    protected void activate(ComponentContext context) {

        try {
            CEPStatisticsManager cepStatisticsManager = new CEPStatisticsManager();
            CEPStatisticsServiceHolder.getInstance().setCepStatisticsManager(cepStatisticsManager);
            context.getBundleContext().registerService(CEPStatisticsManagerInterface.class.getName(),
                                                       cepStatisticsManager, null);
            log.info("Successfully deployed the cep statistics monitoring service");
        } catch (Throwable e) {
            log.error("Can not create the cep statistics monitoring service ", e);
        }
    }

    protected void deactivate(ComponentContext context) {
//        CEPServiceValueHolder.getInstance().getCepService().shutdown();
    }



}
