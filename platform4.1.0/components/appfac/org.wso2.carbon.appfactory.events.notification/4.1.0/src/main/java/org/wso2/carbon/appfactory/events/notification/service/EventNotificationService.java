/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
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

package org.wso2.carbon.appfactory.events.notification.service;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.application.mgt.service.ApplicationManagementException;
import org.wso2.carbon.appfactory.events.notification.internal.AppFactoryEventNotificationComponent;
import org.wso2.carbon.appfactory.events.notification.internal.EventRepository;
import org.wso2.carbon.core.AbstractAdmin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EventNotificationService extends AbstractAdmin{

    private static Log log = LogFactory.getLog(EventNotificationService.class);
    private Gson gson = new Gson();

    /**
     * The service method to publish events
     * @param event event to be published
     */
    public void publishEvent(EventBean event) {
        EventRepository.getInstance().addEvent(event);
    }

    /**
     * Service method to get the events for the given applications
     * @param appIDs applicationIds that we need to get the events of
     * @param userName logged in user
     * @return the events for the given application
     */
    public EventBean[] getEventsForApplications(String[] appIDs, String userName) {
        List<EventBean> eventList = new ArrayList<EventBean>();
        ArrayList userApps = getAppsOfUser(userName);
        for(String appID : appIDs) {
            // this is to ensure that we give events of the applications of which user has access
            if(userApps.contains(appID)) {
                eventList.addAll(getEventsForApplication(appID));
            }
        }
        EventBean[] eventBeanArray = eventList.toArray(new EventBean[eventList.size()]);
        return eventBeanArray;
    }

    /**
     * Returns a list of events related to the given application id
     * @param appID application id
     * @return event list of the given application id
     */
    private List<EventBean> getEventsForApplication(String appID) {
        //ArrayList<String> events = new ArrayList<String>();
        List<EventBean> eventList = new ArrayList<EventBean>();

        for (Object eventObj : EventRepository.getInstance().getEventBuffer()) {
            EventBean eventBean = (EventBean) eventObj;
            if (appID.equals(eventBean.getApplicationId())) {
                //events.add(gson.toJson(eventBean));
                eventList.add(eventBean);
            }
        }
        return eventList;
    }

    /**
     * Get applications that user has access to
     * @param userName logged in user
     * @return application list that the given user has access to
     */
    private ArrayList getAppsOfUser(String userName) {
        ArrayList appList = new ArrayList();
        try {
            String[] userApps = AppFactoryEventNotificationComponent
                    .getApplicationManagementService().getAllApplications(userName);
            Collections.addAll(appList, userApps);
        } catch (ApplicationManagementException e) {
            log.error("Error while retrieving the application of user "+userName);
        }
        return appList;
    }
}
