package org.wso2.carbon.databridge.agent.thrift.lb;

import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;

import java.util.concurrent.atomic.AtomicBoolean;

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
public class DataPublisherHolder {
    private String authenticationUrl;
    private String receiverUrl;
    private String username;
    private String password;
    private Agent agent;
    private AtomicBoolean connected = new AtomicBoolean(true);

    private AsyncDataPublisher dataPublisher;

    public DataPublisherHolder(String authenticationUrl, String receiverUrl, String username, String password) {
        this.authenticationUrl = authenticationUrl;
        this.receiverUrl = receiverUrl;
        this.username = username;
        this.password = password;
        this.agent = null;
    }

    protected void setAgent(Agent agent){
        this.agent = agent;
    }


    public void generateDataPublisher() {
        if (null != authenticationUrl) {
            if (null != agent) {
                dataPublisher = new AsyncDataPublisher(authenticationUrl, receiverUrl, username, password, agent);
            } else {
                dataPublisher = new AsyncDataPublisher(authenticationUrl, receiverUrl, username, password);
            }
        } else if (null != agent) {
            dataPublisher = new AsyncDataPublisher(receiverUrl, username, password, agent);
        } else {
            dataPublisher = new AsyncDataPublisher(receiverUrl, username, password);
        }
    }

    public String getAuthenticationUrl() {
        return authenticationUrl;
    }

    public String getReceiverUrl() {
        return receiverUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Agent getAgent() {
        return agent;
    }

    public AtomicBoolean getConnected() {
        return connected;
    }

    public AsyncDataPublisher getDataPublisher() {
        return dataPublisher;
    }

    public void setConnected(boolean state) {
        connected.set(state);
    }
}
