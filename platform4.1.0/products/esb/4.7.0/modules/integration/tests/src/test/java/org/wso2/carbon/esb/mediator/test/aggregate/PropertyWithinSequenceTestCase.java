/*
* Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.esb.mediator.test.aggregate;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.axiom.om.util.AXIOMUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.esb.ESBIntegrationTest;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Iterator;

public class PropertyWithinSequenceTestCase extends ESBIntegrationTest {

    private AggregatedRequestClient aggregatedRequestClient;
    private int no_of_requests=0;
    private boolean complete=false;


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        loadESBConfigurationFromClasspath("/artifacts/ESB/synapseconfig/propertyWithinIterateConfig/synapse.xml");
        aggregatedRequestClient= new AggregatedRequestClient();
        aggregatedRequestClient.setProxyServiceUrl(getProxyServiceURL("aggregateMediatorTestProxy"));
        aggregatedRequestClient.setSymbol("IBM");
        no_of_requests=15;
        aggregatedRequestClient.setNo_of_iterations(no_of_requests);

    }



    @Test(groups = {"wso2.esb"}, description = "more number of messages than maximum count")
    public void testMoreNumberThanMaximum() throws IOException, XMLStreamException {
        int responseCount=0;

        String Response= aggregatedRequestClient.getResponse();
        Assert.assertNotNull(Response);
        OMElement Response2= AXIOMUtil.stringToOM(Response);
        OMElement soapBody = Response2.getFirstElement();
        Iterator iterator =soapBody.getChildrenWithName(new QName("http://services.samples",
                "getQuoteResponse"));
        soapBody.getChildElements();

        while (iterator.hasNext()) {

            OMNode omNode = ((OMElement)iterator.next()).getFirstElement().getFirstElement().getNextOMSibling();

            while(true){
                if(((OMElementImpl)omNode).getLocalName().toString().contains("name")){
                    break;
                }
                if(omNode.getNextOMSibling()!=null){
                    omNode=omNode.getNextOMSibling();
                }  else{
                    break;
                }

            }
            if(((OMElementImpl) omNode).getText().contains(Float.toString(no_of_requests))){
                complete=true;
            }


        }

        Assert.assertTrue(complete);
    }
    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        aggregatedRequestClient = null;
        super.cleanup();
    }
}
