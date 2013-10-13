package org.wso2.carbon.esb.mediator.test.payload.factory;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.esb.ESBIntegrationTest;

import static org.testng.Assert.assertFalse;

/**
 * Created by IntelliJ IDEA.
 * User: charitha
 * Date: 12/4/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class FormatPayloadWithOMTypeArgsExpressionTestCase extends ESBIntegrationTest {
    @BeforeClass(alwaysRun = true)
    public void uploadSynapseConfig() throws Exception {
        super.init();
        loadESBConfigurationFromClasspath("/artifacts/ESB/mediatorconfig/payload/factory/om_arg_payload_factory_synapse.xml");
    }


    @Test(groups = {"wso2.esb"}, description = "Do transformation with a Payload Format that has OM type arguments")
    public void transformPayloadByArgsValue() throws AxisFault {
        sendRobust(getMainSequenceURL(), "IBM");
    }


    private void sendRobust(String trpUrl, String symbol)
            throws AxisFault {
        ServiceClient sender;
        Options options;

        sender = new ServiceClient();
        options = new Options();
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.CHUNKED, Boolean.FALSE);
        options.setAction("urn:placeOrder");

        if (trpUrl != null && !"null".equals(trpUrl)) {
            options.setProperty(Constants.Configuration.TRANSPORT_URL, trpUrl);
        }

        sender.setOptions(options);

        OMElement response = sender.sendReceive(createRequest(symbol));
        assertFalse(response.toString().contains("&lt;"), "Transformed message contains &lt; instead of <");
    }

    private OMElement createRequest(String symbol) {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://services.samples", "ns");
        OMElement method = fac.createOMElement("payload", omNs);
        OMElement value1 = fac.createOMElement("request", omNs);
        OMElement value2 = fac.createOMElement("symbol", omNs);

        value2.addChild(fac.createOMText(value1, symbol));
        value1.addChild(value2);
        method.addChild(value1);

        return method;
    }

    @AfterClass(alwaysRun = true)
    private void destroy() throws Exception {
        super.cleanup();
    }
}
