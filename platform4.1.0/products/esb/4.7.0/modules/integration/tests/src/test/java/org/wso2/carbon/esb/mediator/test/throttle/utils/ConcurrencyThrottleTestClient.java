/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.carbon.esb.mediator.test.throttle.utils;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.automation.utils.esb.StockQuoteClient;
import java.util.List;


public class ConcurrencyThrottleTestClient implements Runnable {

    private StockQuoteClient axis2Client;
    private String mainSequenceURL;
    private List list;
    private ThrottleTestCounter counter;


    public ConcurrencyThrottleTestClient(String MainSequenceURL,List list,ThrottleTestCounter counter) {
        this.mainSequenceURL=MainSequenceURL;
        this.list=list;
        this.counter=counter;
        axis2Client=new StockQuoteClient();
    }


    @Override
    public void run() {
        try {
            OMElement response = axis2Client.sendSimpleStockQuoteRequest(mainSequenceURL, null, "WSO2");
            if(response.toString().contains("WSO2")){
                list.add("Access Granted");
            }

        } catch (Exception e) {
           if(e.getMessage().contains("**Access Denied**")){
                list.add("Access Denied");
           }
        }
        counter.increment();
        axis2Client.destroy();
    }
}
