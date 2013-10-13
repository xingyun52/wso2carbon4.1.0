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

package org.apache.rampart.samples.sample11;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.Parameter;
import org.apache.rampart.handler.WSSHandlerConstants;
import org.apache.rampart.handler.config.InflowConfiguration;
import org.apache.rampart.handler.config.OutflowConfiguration;

import javax.xml.namespace.QName;

public class Client {

    public static void main(String[] args) throws Exception {
        
        if(args.length != 2) {
            System.out.println("Usage: $java Client endpoint_address client_repo_path");
        }
        
        ConfigurationContext ctx = ConfigurationContextFactory.createConfigurationContextFromFileSystem(args[1], args[1] + "/conf/axis2.xml");
        
        ServiceClient client = new ServiceClient(ctx, null);
        Options options = new Options();
        options.setAction("urn:echo");
        options.setTo(new EndpointReference(args[0]));
        
        //Set the rampart parameters
        options.setProperty(WSSHandlerConstants.OUTFLOW_SECURITY, getOutflowConfiguration());
        options.setProperty(WSSHandlerConstants.INFLOW_SECURITY, getInflowConfiguration());
        
        client.setOptions(options);
        
        //Engage rampart
        client.engageModule("rampart");
        
        OMElement response = client.sendReceive(getPayload("Hello world"));
        
        System.out.println(response);
        
    }
    
    private static OMElement getPayload(String value) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace ns = factory.createOMNamespace("http://sample11.samples.rampart.apache.org","ns1");
        OMElement elem = factory.createOMElement("echo", ns);
        OMElement childElem = factory.createOMElement("param0", null);
        childElem.setText(value);
        elem.addChild(childElem);
        
        return elem;
    }
    
    private static Parameter getOutflowConfiguration() {
        OutflowConfiguration ofc = new OutflowConfiguration();
        ofc.setActionItems("Timestamp Signature Encrypt");
        ofc.setUser("client");
        ofc.setPasswordCallbackClass("org.apache.rampart.samples.sample11.PWCBHandler");
        ofc.setSignaturePropFile("client.properties");
        ofc.setSignatureKeyIdentifier(WSSHandlerConstants.BST_DIRECT_REFERENCE);
        ofc.setEncryptionKeyIdentifier(WSSHandlerConstants.ISSUER_SERIAL);
        ofc.setEncryptionUser("service");
        
        return ofc.getProperty();
    }
    
    private static Parameter getInflowConfiguration() {
        InflowConfiguration ifc = new InflowConfiguration();
        ifc.setActionItems("Timestamp Signature Encrypt");
        ifc.setPasswordCallbackClass("org.apache.rampart.samples.sample11.PWCBHandler");
        ifc.setSignaturePropFile("client.properties");
        
        return ifc.getProperty();
    }
    
}
