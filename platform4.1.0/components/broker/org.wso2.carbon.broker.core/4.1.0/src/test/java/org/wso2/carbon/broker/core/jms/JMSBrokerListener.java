package org.wso2.carbon.broker.core.jms;

import org.wso2.carbon.broker.core.BrokerListener;
import org.wso2.carbon.broker.core.exception.BrokerEventProcessingException;

/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class JMSBrokerListener implements BrokerListener {


    @Override
    public void addEventDefinition(Object object) throws BrokerEventProcessingException {
        System.out.println(" Add Definition ==> " + object);
    }

    @Override
    public void removeEventDefinition(Object object) throws BrokerEventProcessingException {
        System.out.println(" Remove Definition ==> " + object);
    }

    public void onEvent(Object object) throws BrokerEventProcessingException {
        System.out.println("Message ==> " + object);
    }
}
