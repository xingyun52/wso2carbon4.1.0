package org.wso2.carbon.lb.endpoint.subscriber;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.lb.endpoint.util.ConfigHolder;

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
