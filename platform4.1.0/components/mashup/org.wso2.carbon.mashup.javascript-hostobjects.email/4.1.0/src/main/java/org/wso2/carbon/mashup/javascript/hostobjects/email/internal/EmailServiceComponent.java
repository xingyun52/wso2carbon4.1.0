/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.mashup.javascript.hostobjects.email.internal;

import org.wso2.carbon.mashup.javascript.hostobjects.hostobjectservice.service.HostObjectService;

/**
 * @scr.component name="mashup.javascript.hostobjects.email.dscomponent"" immediate="true"
 * @scr.reference name="mashup.javascript.hostobjects.hostobjectservice"
 * interface="org.wso2.carbon.mashup.javascript.hostobjects.hostobjectservice.service.HostObjectService"
 * cardinality="1..1" policy="dynamic" bind="setHostObjectService" unbind="unsetHostObjectService"
 */
public class EmailServiceComponent {

    private static HostObjectService hostObjectService = null;

    protected void setHostObjectService(HostObjectService hostObjectService) {
        EmailServiceComponent.hostObjectService = hostObjectService;
    }

    protected void unsetHostObjectService(HostObjectService hostObjectService) {
        EmailServiceComponent.hostObjectService = null;
    }

    public static HostObjectService getHostObjectService() {
        return hostObjectService;
    }
}
