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

package org.apache.rampart.samples.policy.sample07;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;

import javax.xml.namespace.QName;

public class Client {

    public static void main(String[] args) throws Exception {
        
        if(args.length != 3) {
            System.out.println("Usage: $java Client endpoint_address client_repo_path policy_xml_path");
        }
        
        ConfigurationContext ctx = ConfigurationContextFactory.createConfigurationContextFromFileSystem(args[1], null);
        
        ServiceClient client = new ServiceClient(ctx, null);
        Options options = new Options();
        options.setAction("urn:echo");
        options.setTo(new EndpointReference(args[0]));
        options.setProperty(RampartMessageData.KEY_RAMPART_POLICY,  loadPolicy(args[2]));
        client.setOptions(options);
        
        client.engageModule("addressing");
        client.engageModule("rampart");

        OMElement response = client.sendReceive(getPayload("Hello world"));
        
        System.out.println(response);
        
    }
    
    private static Policy loadPolicy(String xmlPath) throws Exception {
        StAXOMBuilder builder = new StAXOMBuilder(xmlPath);
        return PolicyEngine.getPolicy(builder.getDocumentElement());
    }
    
    private static OMElement getPayload(String value) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace("http://sample07.policy.samples.rampart.apache.org","ns");
        OMElement elem = factory.createOMElement("echo", ns);
        OMElement childElem = factory.createOMElement("param0", ns);
        childElem.setText(value);
        elem.addChild(childElem);
        
        return elem;
    }
    
}
