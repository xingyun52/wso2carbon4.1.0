/**
 *
 */
package org.wso2.carbon.esb.message.store.test;

import org.apache.axis2.AxisFault;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.annotations.SetEnvironment;
import org.wso2.carbon.automation.core.utils.httpserverutils.RequestInterceptor;
import org.wso2.carbon.automation.core.utils.httpserverutils.SimpleHttpClient;
import org.wso2.carbon.automation.core.utils.httpserverutils.SimpleHttpServer;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
import org.wso2.carbon.esb.ESBIntegrationTest;
import org.wso2.carbon.esb.util.controller.JMSBrokerController;
import org.wso2.carbon.esb.util.controller.config.JMSBrokerConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.assertTrue;

/**
 * @author wso2
 */
public class SpecialCharacterTest extends ESBIntegrationTest {


    private TestRequestInterceptor interceptor = new TestRequestInterceptor();
    private JMSBrokerController jmsBrokerController;
    private ServerConfigurationManager serverConfigurationManager;

    private final String ACTIVEMQ_CORE = "activemq-core-5.2.0.jar";
    private final String GERONIMO_J2EE_MANAGEMENT = "geronimo-j2ee-management_1.1_spec-1.0.1.jar";
    private final String GERONIMO_JMS = "geronimo-jms_1.1_spec-1.1.1.jar";
    private final String JAR_LOCATION = "/artifacts/ESB/jar";
    private SimpleHttpServer httpServer;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        setUpJMSBroker();
        httpServer = new SimpleHttpServer();
        try {
            httpServer.start();
            Thread.sleep(5000);
        } catch (IOException e) {
            log.error("Error while starting the HTTP server", e);
        }

        interceptor = new TestRequestInterceptor();
        httpServer.getRequestHandler().setInterceptor(interceptor);


        super.init(5);

        serverConfigurationManager = new ServerConfigurationManager(esbServer.getBackEndUrl());
        serverConfigurationManager.copyToComponentLib(new File(getClass().getResource(JAR_LOCATION + File.separator + ACTIVEMQ_CORE).toURI()));
        serverConfigurationManager.copyToComponentLib(new File(getClass().getResource(JAR_LOCATION + File.separator + GERONIMO_J2EE_MANAGEMENT).toURI()));
        serverConfigurationManager.copyToComponentLib(new File(getClass().getResource(JAR_LOCATION + File.separator + GERONIMO_JMS).toURI()));
        serverConfigurationManager.applyConfiguration(new File(getClass().getResource(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "messageStore" + File.separator + "axis2.xml").getPath()));

        super.init(5);
        loadESBConfigurationFromClasspath(File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "messageStore" + File.separator + "special_character.xml");

    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.integration_all})
    @Test(groups = {"wso2.esb"})
    public void testSpecialCharacterMediation() throws Exception {
//        serverConfigurationManager.restartGracefully();
//        super.init(5);
        SimpleHttpClient httpClient = new SimpleHttpClient();
        String payload = "<test>This payload is üsed to check special character mediation</test>";
        try {

            HttpResponse response = httpClient.doPost(getProxyServiceURL("InOutProxy"), null, payload, "application/xml");
        } catch (AxisFault e) {
            log.error("Response not expected here, Exception can be accepted ");
        }
        Thread.sleep(10000);
        assertTrue(interceptor.getPayload().contains(payload));
    }

    private void setUpJMSBroker() {
        jmsBrokerController = new JMSBrokerController("localhost", getJMSBrokerConfiguration());
        jmsBrokerController.start();
    }


    private JMSBrokerConfiguration getJMSBrokerConfiguration() {
        JMSBrokerConfiguration jmsBrokerConfiguration = new JMSBrokerConfiguration();
        jmsBrokerConfiguration.setInitialNamingFactory("org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        jmsBrokerConfiguration.setProviderURL("tcp://localhost:61616");
        return jmsBrokerConfiguration;
    }


    private static class TestRequestInterceptor implements RequestInterceptor {

        private String payload;

        public void requestReceived(HttpRequest request) {
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                try {
                    InputStream in = entity.getContent();
                    String inputString = IOUtils.toString(in, "UTF-8");
                    payload = inputString;
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        public String getPayload() {
            return payload;
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        try {
            super.cleanup();

        } finally {
            try {
                jmsBrokerController.stop();
            } catch (Exception e) {
                log.warn("Error while shutting down the JMS Broker", e);
            }
            try {
                httpServer.stop();
            } catch (Exception e) {
                log.warn("Error while shutting down the HTTP server", e);
            }
            serverConfigurationManager.removeFromComponentLib(ACTIVEMQ_CORE);
            serverConfigurationManager.removeFromComponentLib(GERONIMO_J2EE_MANAGEMENT);
            serverConfigurationManager.removeFromComponentLib(GERONIMO_JMS);
            Thread.sleep(3000);
            serverConfigurationManager.restoreToLastConfiguration();
            serverConfigurationManager = null;
        }
    }
}
