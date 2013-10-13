package org.wso2.carbon.databridge.receiver.restapi;

import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.databridge.core.AbstractDataReceiver;
import org.wso2.carbon.databridge.core.DataBridgeReceiverService;

/**
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class RestDataReceiver extends AbstractDataReceiver {


    @Override
    protected DataBridgeReceiverService getDatabridgeReceiver() {
        return (DataBridgeReceiverService) PrivilegedCarbonContext.getCurrentContext()
                .getOSGiService(DataBridgeReceiverService.class);
    }
}
