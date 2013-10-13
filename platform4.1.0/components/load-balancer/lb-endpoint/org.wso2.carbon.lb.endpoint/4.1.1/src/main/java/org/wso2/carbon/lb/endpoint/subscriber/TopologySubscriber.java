package org.wso2.carbon.lb.endpoint.subscriber;

import java.util.Properties;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.lb.endpoint.util.ConfigHolder;
import org.wso2.carbon.lb.endpoint.util.TopologyConstants;

public class TopologySubscriber {

	private static final Log log = LogFactory.getLog(TopologySubscriber.class);
	
	public static void subscribe(String topicName) {
        Properties initialContextProperties = new Properties();
        initialContextProperties.put("java.naming.factory.initial",
                "org.wso2.andes.jndi.PropertiesFileInitialContextFactory");
        
        String mbServerUrl = null;
        if(ConfigHolder.getInstance().getLbConfig() != null){
        	mbServerUrl = ConfigHolder.getInstance().getLbConfig().getLoadBalancerConfig().getMbServerUrl();
        }
		String connectionString =
		                          "amqp://admin:admin@clientID/carbon?brokerlist='tcp://" + 
		                        		  (mbServerUrl==null ? TopologyConstants.DEFAULT_MB_SERVER_URL:mbServerUrl) + "?reconnect='true''";
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
