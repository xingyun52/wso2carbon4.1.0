package org.apache.synapse.message.processors.forward;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;

public class MessageStoreServiceClient extends ServiceClient {

    public MessageStoreServiceClient(ConfigurationContext configContext, AxisService axisService)
            throws AxisFault {
        super(configContext, axisService);
        // TODO Auto-generated constructor stub
    }

    /**
     * Directly invoke a named operation with a Robust In-Only MEP. This method just sends your
     * supplied XML and possibly receives a fault. For more control, you can instead create a client
     * for the operation and use that client to execute the send.
     *
     * @param elem   XML to send
     * @param orgi   message context
     * @throws AxisFault  if something goes wrong
     */
    public void sendRobust(OMElement elem, MessageContext orgi) throws AxisFault {
        MessageContext mc = new MessageContext();
        if (orgi.isDoingREST()) {
            mc.setDoingREST(true);
        }
        fillSOAPEnvelope(mc, elem);
        OperationClient mepClient = createClient(ANON_ROBUST_OUT_ONLY_OP);
        mepClient.addMessageContext(mc);
        mepClient.execute(true);
    }

    /**
     * Prepare a SOAP envelope with the stuff to be sent.
     *
     * @param messageContext the message context to be filled
     * @param xmlPayload     the payload content
     * @throws AxisFault if something goes wrong
     */
    private void fillSOAPEnvelope(MessageContext messageContext, OMElement xmlPayload)
            throws AxisFault {
        messageContext.setServiceContext(getServiceContext());
        SOAPFactory soapFactory = getSOAPFactory();
        SOAPEnvelope envelope = soapFactory.getDefaultEnvelope();
        if (xmlPayload != null) {
            envelope.getBody().addChild(xmlPayload);
        }
        addHeadersToEnvelope(envelope);
        messageContext.setEnvelope(envelope);
    }


    /**
     * Return the SOAP factory to use depending on what options have been set. If the SOAP version
     * can not be seen in the options, version 1.1 is the default.
     *
     * @return the SOAP factory
     * @see Options#setSoapVersionURI(String)
     */
    private SOAPFactory getSOAPFactory() {
        String soapVersionURI = getOptions().getSoapVersionURI();
        if (SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(soapVersionURI)) {
            return OMAbstractFactory.getSOAP12Factory();
        } else {
            // make the SOAP 1.1 the default SOAP version
            return OMAbstractFactory.getSOAP11Factory();
        }
    }

}
