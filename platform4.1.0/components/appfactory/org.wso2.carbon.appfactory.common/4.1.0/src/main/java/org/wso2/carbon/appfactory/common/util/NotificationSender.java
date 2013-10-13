/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.appfactory.common.util;

import org.apache.axis2.client.ServiceClient;
import org.wso2.carbon.appfactory.events.notification.stub.EventNotificationServiceStub;
import org.wso2.carbon.appfactory.events.notification.stub.xsd.EventBean;
import org.wso2.carbon.utils.CarbonUtils;

import java.rmi.RemoteException;

public class NotificationSender {

    private String backendServerURL;

    public NotificationSender(String backendServerURL) {
        if (!backendServerURL.endsWith("/")) {
            backendServerURL += "/";
        }
        this.backendServerURL = backendServerURL;
    }

    public void publishEvents(String applicationId, String event, String result) throws RemoteException {
        String serviceURL = backendServerURL + "EventNotificationService";
        EventNotificationServiceStub stub = new EventNotificationServiceStub(serviceURL);
        ServiceClient client = stub._getServiceClient();
        CarbonUtils.setBasicAccessSecurityHeaders(AppFactoryUtil.getAdminUsername(), AppFactoryUtil.getAdminPassword(), client);
        stub.publishEvent(createEventBean(applicationId, event, result));
    }

    private EventBean createEventBean(String applicationId, String event, String result) {
        EventBean eventBean = new EventBean();
        eventBean.setApplicationId(applicationId);
        eventBean.setEvent(event);
        eventBean.setResult(result);
        return eventBean;
    }
}
