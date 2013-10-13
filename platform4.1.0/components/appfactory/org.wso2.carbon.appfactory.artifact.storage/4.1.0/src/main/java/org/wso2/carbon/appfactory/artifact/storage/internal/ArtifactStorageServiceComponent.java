/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.wso2.carbon.appfactory.artifact.storage.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.appfactory.core.ArtifactStorage;


/**
 * @scr.component name="org.wso2.carbon.appfactory.artifact.storage" immediate="true"
 * @scr.reference name="appfactory.artifact"
 * interface="org.wso2.carbon.appfactory.core.ArtifactStorage"
 * cardinality="1..1" policy="dynamic"
 * bind="setArtifactStorage"
 * unbind="unsetArtifactStorage"
 */
public class ArtifactStorageServiceComponent {
    Log log = LogFactory.getLog(org.wso2.carbon.appfactory.artifact.storage.internal.ArtifactStorageServiceComponent.class);

    protected void activate(ComponentContext context) {
        try {
            if (log.isDebugEnabled()) {
                log.info("************** artifact storage bundle is activated*************");
            }
        } catch (Throwable e) {
            log.error("Error in creating appfactory configuration", e);
        }


    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.info("************* artifact storage bundle is deactivated*************");
        }
    }

    protected void unsetArtifactStorage(ArtifactStorage artifactStorage) {
        ServiceHolder.setArtifactStorage(null);
    }

    protected void setArtifactStorage(ArtifactStorage artifactStorage) {
        ServiceHolder.setArtifactStorage(artifactStorage);
    }

}
