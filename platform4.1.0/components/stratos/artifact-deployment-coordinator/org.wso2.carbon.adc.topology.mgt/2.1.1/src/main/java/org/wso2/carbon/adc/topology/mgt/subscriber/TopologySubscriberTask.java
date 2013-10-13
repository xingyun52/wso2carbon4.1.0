/*
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.adc.topology.mgt.subscriber;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.ntask.core.Task;

public class TopologySubscriberTask implements Task{
    
    private static final Log log = LogFactory.getLog(TopologySubscriberTask.class);
    
    @Override
    public void execute() {
//    	log.info("Topology Subscription Task is running ... ");
        
    	// subscribe to the topic 
//		TopologySubscriber.subscribe(TopologyConstants.TOPIC_NAME);
    }
    
    @Override
    public void init() {

//    	log.info("Topology Subscription is initialized!");
    }

    @Override
    public void setProperties(Map<String, String> arg0) {}
    
}
