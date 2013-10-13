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
package org.wso2.carbon.cep.statistics.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.brokermanager.stub.BrokerManagerAdminServiceStub;
import org.wso2.carbon.cep.statistics.stub.types.carbon.CollectionDTO;
import org.wso2.carbon.cep.statistics.stub.types.carbon.CountDTO;
import org.wso2.carbon.cep.stub.admin.CEPAdminServiceStub;
import org.wso2.carbon.ui.CarbonUIUtil;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 *
 */
public class Utils {
    private static final String CEP_STATS = "cep.stats";
    private static final String BROKER_STATS = "cep.broker.stats";
    private static final String BUCKET_STATS = "cep.bucket.stats";

    public static int getPositiveIntegerValue(HttpSession session, HttpServletRequest request,
                                              int defaultValue, String keyName) {
        if (request.getParameter(keyName) != null) {
            try {
                defaultValue = Integer.parseInt(request.getParameter(keyName));
                if (defaultValue > 0) {
                    session.setAttribute(keyName, String.valueOf(defaultValue));
                } else {
                    defaultValue = 1;
                }
            } catch (NumberFormatException ignored) {
                if (session.getAttribute(keyName) != null) {
                    defaultValue = Integer.parseInt((String) session.getAttribute(keyName));
                }
            }
        } else if (session.getAttribute(keyName) != null) {
            defaultValue = Integer.parseInt((String) session.getAttribute(keyName));
        } else {
            session.setAttribute(keyName, String.valueOf(defaultValue));
        }
        return defaultValue;
    }

    public static BrokerManagerAdminServiceStub getBrokerManagerAdminService(ServletConfig config,
                                                                             HttpSession session,
                                                                             HttpServletRequest request)
            throws AxisFault {
        ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        //Server URL which is defined in the server.xml
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(),
                                                     session) + "BrokerManagerAdminService.BrokerManagerAdminServiceHttpsSoap12Endpoint";
        BrokerManagerAdminServiceStub stub = new BrokerManagerAdminServiceStub(configContext, serverURL);

        String cookie = (String) session.getAttribute(org.wso2.carbon.utils.ServerConstants.ADMIN_SERVICE_COOKIE);

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

//        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
//        backendServerURL = backendServerURL + "BrokerManagerAdminService";
//        BrokerManagerAdminServiceStub stub = new BrokerManagerAdminServiceStub(backendServerURL);
        return stub;
    }

    public static CEPAdminServiceStub getCEPAdminService(ServletConfig config,
                                                         HttpSession session,
                                                         HttpServletRequest request)
            throws AxisFault {
        ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        //Server URL which is defined in the server.xml
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(),
                                                     session) + "CEPAdminService.CEPAdminServiceHttpsSoap12Endpoint";
        CEPAdminServiceStub stub = new CEPAdminServiceStub(configContext, serverURL);

        String cookie = (String) session.getAttribute(org.wso2.carbon.utils.ServerConstants.ADMIN_SERVICE_COOKIE);

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        return stub;
    }

    public static CountDTO calculateCepCount(CountDTO count, HttpSession session) {
        CountDTO old = (CountDTO) session.getAttribute(CEP_STATS);
        if (old == null) {
            session.setAttribute(CEP_STATS, count);
            return count;
        } else {
            old.setRequestCount(count.getRequestCount() - old.getRequestCount());
            old.setResponseCount(count.getResponseCount() - old.getResponseCount());
            session.setAttribute(CEP_STATS, count);
            return old;
        }
    }


    public static CollectionDTO calculateSubCount(String name, CollectionDTO collection,
                                                  HttpSession session,
                                                  boolean isBucket) {
        String fullName;
        if (isBucket) {
            fullName = BUCKET_STATS + "." + name;
        } else {
            fullName = BROKER_STATS + "." + name;
        }

        CollectionDTO old = (CollectionDTO) session.getAttribute(fullName);
        if (old == null) {
            session.setAttribute(fullName, collection);
            return collection;
        } else {
            old.getCount().setRequestCount(collection.getCount().getRequestCount() - old.getCount().getRequestCount());
            old.getCount().setResponseCount(collection.getCount().getResponseCount() - old.getCount().getResponseCount());

            String[] topicNames = old.getTopicNames();
            if (topicNames != null) {
                for (int iOld = 0, topicNamesLength = topicNames.length; iOld < topicNamesLength; iOld++) {
                    String oldTopicName = topicNames[iOld];
                    String[] topicNames1 = collection.getTopicNames();
                    for (int iNew = 0, topicNames1Length = topicNames1.length; iNew < topicNames1Length; iNew++) {
                        String newTopicName = topicNames1[iNew];
                        if (oldTopicName.equals(newTopicName)) {
                            old.getTopicCounts()[iOld].setRequestCount(collection.getTopicCounts()[iNew].getRequestCount() - old.getTopicCounts()[iOld].getRequestCount());
                            old.getTopicCounts()[iOld].setResponseCount(collection.getTopicCounts()[iNew].getResponseCount() - old.getTopicCounts()[iOld].getResponseCount());
                            break;
                        }
                    }
                }
            }
            session.setAttribute(fullName, collection);
            return old;
        }

    }

    public static void setCepCount(CountDTO count, HttpSession session) {
        session.setAttribute(CEP_STATS, count);
    }

    public static void setSubCount(String name, CollectionDTO collection,
                                   HttpSession session,
                                   boolean isBucket) {
        String fullName;
        if (isBucket) {
            fullName = BUCKET_STATS + "." + name;
        } else {
            fullName = BROKER_STATS + "." + name;
        }
        session.setAttribute(fullName, collection);
    }
}
