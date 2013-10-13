package org.wso2.carbon.bam.activityMonitor;

import org.apache.log4j.Logger;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.DataPublisher;
import org.wso2.carbon.databridge.agent.thrift.conf.AgentConfiguration;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.DifferentStreamDefinitionAlreadyDefinedException;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.exception.NoStreamDefinitionExistException;
import org.wso2.carbon.databridge.commons.exception.StreamDefinitionException;
import org.wso2.carbon.databridge.commons.exception.TransportException;

import javax.security.sasl.AuthenticationException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;

public class ActivityMonitor {
    private static Logger logger = Logger.getLogger(ActivityMonitor.class);
    public static final String ACTIVITY_MONITORING_STREAM = "org.wso2.bam.activity.monitoring";
    public static final String VERSION = "1.0.0";

    public static final String[] activityId = {"1cecbb16-6b89-46f3-bd2f-fd9f7ac447b6",
                                               "2cecbb16-6b89-46f3-bd2f-fd9f7ac447b6",
                                               "3cecbb16-6b89-46f3-bd2f-fd9f7ac447b6",
                                               "4cecbb16-6b89-46f3-bd2f-fd9f7ac447b6",
                                               "5cecbb16-6b89-46f3-bd2f-fd9f7ac447b6"};


    public static void main(String[] args) throws AgentException,
                                                  MalformedStreamDefinitionException,
                                                  StreamDefinitionException,
                                                  DifferentStreamDefinitionAlreadyDefinedException,
                                                  MalformedURLException,
                                                  AuthenticationException,
                                                  NoStreamDefinitionExistException,
                                                  TransportException, SocketException,
                                                  org.wso2.carbon.databridge.commons.exception.AuthenticationException {
        System.out.println("Starting Activity Monitoring Sample");
        AgentConfiguration agentConfiguration = new AgentConfiguration();
        String currentDir = System.getProperty("user.dir");
        System.setProperty("javax.net.ssl.trustStore", currentDir + "/src/main/resources/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        Agent agent = new Agent(agentConfiguration);
        String host;

        if (getLocalAddress() != null) {
           host = getLocalAddress().getHostAddress();
        } else {
           host = "localhost"; // Defaults to localhost
        }
        //create data publisher

        DataPublisher dataPublisher = new DataPublisher("tcp://" + host + ":7611", "admin", "admin", agent);
        String streamId = null;

        try {
            streamId = dataPublisher.findStream(ACTIVITY_MONITORING_STREAM, VERSION);
            System.out.println("Stream already defined");

        } catch (NoStreamDefinitionExistException e) {
            streamId = dataPublisher.defineStream("{" +
                                                  "  'name':'" + ACTIVITY_MONITORING_STREAM + "'," +
                                                  "  'version':'" + VERSION + "'," +
                                                  "  'nickName': 'Activity_Monitoring'," +
                                                  "  'description': 'A sample for Activity Monitoring'," +
                                                  "  'metaData':[" +
                                                  "          {'name':'character_set_encoding','type':'STRING'}," +
                                                  "          {'name':'host','type':'STRING'}," +
                                                  "          {'name':'http_method','type':'STRING'}," +
                                                  "          {'name':'message_type','type':'STRING'}," +
                                                  "          {'name':'remote_address','type':'STRING'}," +
                                                  "          {'name':'remote_host','type':'STRING'}," +
                                                  "          {'name':'service_prefix','type':'STRING'}," +
                                                  "          {'name':'tenant_id','type':'INT'}," +
                                                  "          {'name':'transport_in_url','type':'STRING'}" +
                                                  "  ]," +
                                                  "  'correlationData':[" +
                                                  "          {'name':'bam_activity_id','type':'STRING'}" +
                                                  "  ]," +
                                                  "  'payloadData':[" +
                                                  "          {'name':'SOAPBody','type':'STRING'}," +
                                                  "          {'name':'SOAPHeader','type':'STRING'}," +
                                                  "          {'name':'message_direction','type':'STRING'}," +
                                                  "          {'name':'message_id','type':'STRING'}," +
                                                  "          {'name':'operation_name','type':'STRING'}," +
                                                  "          {'name':'service_name','type':'STRING'}," +
                                                  "          {'name':'timestamp','type':'LONG'}" +
                                                  "  ]" +
                                                  "}");
//            //Define event stream
        }


        //Publish event for a valid stream
        if (!streamId.isEmpty()) {
            System.out.println("Stream ID: " + streamId);

            for (int i = 0; i < 100; i++) {
                publishEvents(dataPublisher, streamId, i);
                System.out.println("Events published : " + (i + 1));
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }

            dataPublisher.stop();
        }
    }

    private static void publishEvents(DataPublisher dataPublisher, String streamId, int i) throws AgentException {
        Event eventOne = new Event(streamId, System.currentTimeMillis(), getMetadata(), getCorrelationdata(),
                                   getPayloadData());
        dataPublisher.publish(eventOne);
    }

    private static Object[] getMetadata(){
        return new Object[]{
                "UTF-8",
                "192.168.1.2:9764",
                "POST",
                "text/xml",
                "127.0.0.1",
                "localhost",
                "https://my:8244",
                123456,
                "/services/Simple_Stock_Quote_Service_Proxy"
        } ;
    }

    private static Object[] getCorrelationdata(){
        return new Object[]{
                getRandomActivityID()
        } ;
    }

    private static Object[] getPayloadData(){
        return new Object[]{
                "<soapenv:body xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><m0:getfullquote xmlns:m0=\"http://services.samples\"><m0:request><m0:symbol>aa</m0:symbol></m0:request></m0:getfullquote></soapenv:body>",
                "<soapenv:header xmlns:wsa=\"http://www.w3.org/2005/08/addressing\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><wsa:to>https://my:8244/services/Simple_Stock_Quote_Service_Proxy</wsa:to><wsa:messageid>urn:uuid:c70bae36-b163-4f3e-a341-d7079c58f1ba</wsa:messageid><wsa:action>urn:getFullQuote</wsa:action><ns:bamevent activityid=\"6cecbb16-6b89-46f3-bd2f-fd9f7ac447b6\" xmlns:ns=\"http://wso2.org/ns/2010/10/bam\"></ns:bamevent></soapenv:header>",
                "IN",
                "urn:uuid:c70bae36-b163-4f3e-a341-d7079c58f1ba",
                "mediate",
                "Simple_Stock_Quote_Service_Proxy",
                System.currentTimeMillis()
        } ;
    }

    private static String getRandomActivityID() {
        return activityId[getRandomId(5)];
    }

    private static int getRandomId(int i) {
        Random randomGenerator = new Random();
        return randomGenerator.nextInt(i);
    }

    public static InetAddress getLocalAddress() throws SocketException {
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        while (ifaces.hasMoreElements()) {
            NetworkInterface iface = ifaces.nextElement();
            Enumeration<InetAddress> addresses = iface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    return addr;
                }
            }
        }

        return null;
    }
}
