/*
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.adc.topology.mgt.subscriber;

import java.util.Properties;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.topology.mgt.util.TopologyConstants;

public class TopologySubscriber {

	private static final Log log = LogFactory.getLog(TopologySubscriber.class);
	
	public static void subscribe(String topicName) {
        Properties initialContextProperties = new Properties();
        initialContextProperties.put("java.naming.factory.initial",
                "org.wso2.andes.jndi.PropertiesFileInitialContextFactory");
		String mbServerIp = 
		                    System.getProperty(TopologyConstants.MB_SERVER_IP) == null
		                                                                               ? TopologyConstants.DEFAULT_MB_SERVER_IP
		                                                                               : System.getProperty(TopologyConstants.MB_SERVER_IP);
		String connectionString =
		                          "amqp://admin:admin@clientID/carbon?brokerlist='tcp://" +
		                                  mbServerIp + "'";
        initialContextProperties.put("connectionfactory.qpidConnectionfactory", connectionString);

        try {
        	InitialContext initialContext = new InitialContext(initialContextProperties);
            TopicConnectionFactory topicConnectionFactory =
                    (TopicConnectionFactory) initialContext.lookup("qpidConnectionfactory");
            TopicConnection topicConnection = topicConnectionFactory.createTopicConnection();
            topicConnection.start();
            TopicSession topicSession =
                    topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            Topic topic = topicSession.createTopic(topicName);
            TopicSubscriber topicSubscriber =
                    topicSession.createSubscriber(topic);

            topicSubscriber.setMessageListener(new TopologyListener(
                    topicConnection, topicSession, topicSubscriber));


        } catch (NamingException e) {
        	log.error(e.getMessage(), e);
        } catch (JMSException e) {
        	log.error(e.getMessage(), e);
        }
    }

}
