/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.hdfs.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.hdfs.dataaccess.DataAccessService;
import org.wso2.carbon.hdfs.mgt.HDFSAdminComponentManager;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="org.wso2.carbon.hdfs.mgt.component" immediate="true"
 * @scr.reference name="org.wso2.carbon.hdfs.dataaccess.component"
 * interface="org.wso2.carbon.hdfs.dataaccess.DataAccessService" cardinality="1..1"
 * policy="dynamic" bind="setDataAccessService" unbind="unSetDataAccessService"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"  unbind="unsetRealmService"
 */

public class HDFSAdminDSComponent {
    private static Log log = LogFactory.getLog(HDFSAdminDSComponent.class);

     private DataAccessService dataAccessService;
     private RealmService realmService;

    protected void activate(ComponentContext componentContext) {
        if (log.isDebugEnabled()) {
            log.debug("HDFS Admin bundle is activated.");
        }
        try {
        //HDFSAdminComponentManager hdfsAdminComponentManager = new HDFSAdminComponentManager();
        HDFSAdminComponentManager.getInstance().init(dataAccessService, realmService);
        //hdfsAdminComponentManager.init(dataAccessService, realmService);
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    protected void deactivate(ComponentContext componentContext) {
        if (log.isDebugEnabled()) {
            log.debug("HDFS Admin bundle is deactivated.");
        }
       HDFSAdminComponentManager.getInstance().destroy();
    }

    protected void setDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    protected void unSetDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = null;
    }

    protected void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        this.realmService = null;
    }


}
