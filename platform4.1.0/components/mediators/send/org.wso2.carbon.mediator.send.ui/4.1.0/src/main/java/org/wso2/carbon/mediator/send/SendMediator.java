/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.mediator.send;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.config.xml.ValueFactory;
import org.apache.synapse.config.xml.ValueSerializer;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.xml.endpoints.EndpointFactory;
import org.apache.synapse.config.xml.endpoints.EndpointSerializer;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.xmlbeans.impl.xb.xmlconfig.Qnameconfig;
import org.wso2.carbon.mediator.service.ui.AbstractMediator;
import org.apache.synapse.mediators.Value;

import javax.xml.namespace.QName;
import java.util.Properties;


public class SendMediator extends AbstractMediator {

    private static final QName ENDPOINT_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "endpoint");
    private static final QName BUILD_MESSAGE = new QName("buildmessage");

    private Endpoint endpoint = null;
    private Value receivingSeqValue;
    private boolean buildMessage = false;

    public boolean isBuildMessage() {
        return buildMessage;
    }

    public void setBuildMessage(boolean buildMessage) {
        this.buildMessage = buildMessage;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public String getTagLocalName() {
        return "send";
    }

    public Value getReceivingSeqValue() {
        return receivingSeqValue;
    }

    public void setReceivingSeqValue(Value receivingSeqValue) {
        this.receivingSeqValue = receivingSeqValue;
    }

    public OMElement serialize(OMElement parent) {
        OMElement send = fac.createOMElement("send", synNS);
        saveTracingState(send, this);

        Endpoint activeEndpoint = getEndpoint();
        if (activeEndpoint != null) {
            send.addChild(EndpointSerializer.getElementFromEndpoint(activeEndpoint));
        }

        if (parent != null) {
            parent.addChild(send);
        }

        if (receivingSeqValue != null) {
            ValueSerializer keySerializer = new ValueSerializer();
            keySerializer.serializeValue(receivingSeqValue, "receive", send);
        }
        //send.addAttribute("receive", receivingSeq, null);

        if (buildMessage) {
            send.addAttribute(fac.createOMAttribute("buildmessage", nullNS,"true"));
        }

        return send;
    }

    public void build(OMElement elem) {
        endpoint = null;

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        processAuditStatus(this, elem);

        OMElement epElement = elem.getFirstChildWithName(ENDPOINT_Q);
        if (epElement != null) {
            // create the endpoint and set it in the send medaitor
            Endpoint endpoint = EndpointFactory.getEndpointFromElement(epElement, true,
                    new Properties());
            if (endpoint != null) {
                setEndpoint(endpoint);
            }
        }
        if (elem.getAttributeValue(new QName(null, "receive")) != null) {
            ValueFactory keyFactory = new ValueFactory();
            receivingSeqValue = keyFactory.createValue("receive", elem);
        }

        String buildMessage = elem.getAttributeValue(BUILD_MESSAGE);
        if (buildMessage != null && "true".equalsIgnoreCase(buildMessage)) {
            setBuildMessage(true);
        }

    }
}
