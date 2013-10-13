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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.topology.mgt.util.ConfigHolder;

public class TopologyListener implements MessageListener {

	private static final Log log = LogFactory.getLog(TopologyListener.class);
	private TopicConnection topicConnection;
    private TopicSession topicSession;
    private TopicSubscriber topicSubscriber;
	
	public TopologyListener(TopicConnection topicConnection,
                                 TopicSession topicSession,
                                 TopicSubscriber topicSubscriber) {
        this.topicConnection = topicConnection;
        this.topicSession = topicSession;
        this.topicSubscriber = topicSubscriber;
    }

	
	@Override
    public void onMessage(Message message) {
		TextMessage receivedMessage = (TextMessage) message;
        try {
//            System.out.println("Got the message ==> \n" + receivedMessage.getText());
            
            ConfigHolder.getInstance().getSharedTopologyDiffQueue().add(receivedMessage.getText());
            
//            this.topicSubscriber.close();
//            this.topicSession.close();
//            this.topicConnection.stop();
//            this.topicConnection.close();

        } catch (JMSException e) {
        	log.error(e.getMessage(), e);
        }

    }

}
