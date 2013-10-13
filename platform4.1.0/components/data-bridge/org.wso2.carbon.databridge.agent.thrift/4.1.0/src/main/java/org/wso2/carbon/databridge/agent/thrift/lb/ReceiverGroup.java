package org.wso2.carbon.databridge.agent.thrift.lb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.agent.thrift.internal.utils.AgentServerURL;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
public class ReceiverGroup implements ReceiverStateObserver {
    private static Log log = LogFactory.getLog(ReceiverGroup.class);

    private ArrayList<DataPublisherHolder> dataPublisherCache =
            new ArrayList<DataPublisherHolder>();

    private AtomicInteger currentDataPublisherIndex;

    private int maximumDataPublisherIndex;

    private final Integer START_INDEX = 0;

    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public ReceiverGroup(ArrayList<DataPublisherHolder> properties) {
        for (DataPublisherHolder aHolder : properties) {

            dataPublisherCache.add(aHolder);
        }
        maximumDataPublisherIndex = properties.size() - 1;
        currentDataPublisherIndex = new AtomicInteger(START_INDEX);
    }

    protected void createDataPublishers(Agent agent) {
        for (DataPublisherHolder aHolder : dataPublisherCache) {
            aHolder.setAgent(agent);
            aHolder.generateDataPublisher();
            aHolder.getDataPublisher().registerReceiverObserver(this);
        }
        long reconnectionInterval = agent.
                getAgentConfiguration().getReconnectionInterval();
        scheduledExecutorService
                .scheduleAtFixedRate(new ReconnectionTask(), 0,
                        reconnectionInterval, TimeUnit.SECONDS);


    }


    protected void publish(String streamName, String streamVersion,
                           long timeStamp,
                           Object[] metaDataArray, Object[] correlationDataArray,
                           Object[] payloadDataArray, Map<String, String> arbitraryDataMap) throws AgentException {
        AsyncDataPublisher dataPublisher = getDataPublisher();
        if (null != dataPublisher) {
            dataPublisher.publish(streamName, streamVersion,
                    timeStamp,
                    metaDataArray, correlationDataArray,
                    payloadDataArray,arbitraryDataMap);
        }
//        else {
//            log.error("No receiver is reachable, can't publish the event.");
//        }
    }


    protected void publish(String streamName, String streamVersion,
                           Object[] metaDataArray, Object[] correlationDataArray,
                           Object[] payloadDataArray,Map<String, String> arbitraryDataMap) throws AgentException {
        AsyncDataPublisher dataPublisher = getDataPublisher();
        if (null != dataPublisher) {
            dataPublisher.publish(streamName, streamVersion,
                    metaDataArray, correlationDataArray,
                    payloadDataArray,arbitraryDataMap);
        }
//        else {
//            log.error("No receiver is reachable, can't publish the event.");
//        }
    }

    protected void publish(Event event) {
        AsyncDataPublisher dataPublisher = getDataPublisher();
        if (null != dataPublisher) {
            try {
                dataPublisher.publish(event);
            } catch (AgentException e) {
                log.error("No receiver is reachable, can't publish the event.");
            }
        }
//        else {
//            log.error("No receiver is reachable, can't publish the event.");
//        }
    }


    protected void publish(String streamName, String streamVersion, Event event)
            throws AgentException {
        AsyncDataPublisher dataPublisher = getDataPublisher();
        if (null != dataPublisher) {
            dataPublisher.publish(streamName, streamVersion,
                    event);
        }
//        else {
//            log.error("No receiver is reachable, can't publish the event.");
//        }
    }


    private AsyncDataPublisher getDataPublisher() {
        int startIndex = getDataPublisherIndex();
        int index = startIndex;

        while (true) {
            DataPublisherHolder publisherHolder = dataPublisherCache.get(index);
            if (publisherHolder.getConnected().get()) {
                return publisherHolder.getDataPublisher();
            } else {
                index++;
                if (index > maximumDataPublisherIndex) {
                    index = START_INDEX;
                }
                if (index == startIndex) {
                    break;
                }
            }

        }
        return null;
    }

    private synchronized int getDataPublisherIndex() {
        int index = currentDataPublisherIndex.getAndIncrement();
        if (index == maximumDataPublisherIndex) {
            currentDataPublisherIndex.set(START_INDEX);
        }
        return index;
    }

    public void addStreamDefinition(String streamDefn, String streamName, String version) {
        for (int i = START_INDEX; i <= maximumDataPublisherIndex; i++) {
            DataPublisherHolder holder = dataPublisherCache.get(i);
            holder.getDataPublisher().addStreamDefinition(streamDefn, streamName, version);
        }
    }

    public void addStreamDefinition(StreamDefinition streamDefn) {
        for (int i = START_INDEX; i <= maximumDataPublisherIndex; i++) {
            DataPublisherHolder holder = dataPublisherCache.get(i);
            holder.getDataPublisher().addStreamDefinition(streamDefn);
        }
    }


    private AsyncDataPublisher setConnectionStatus(String receiverUrl, String username, String password, boolean status) {
        for (int i = START_INDEX; i <= maximumDataPublisherIndex; i++) {
            DataPublisherHolder holder = dataPublisherCache.get(i);
            if (holder.getReceiverUrl().equalsIgnoreCase(receiverUrl) &&
                    holder.getUsername().equalsIgnoreCase(username) &&
                    holder.getPassword().equalsIgnoreCase(password)) {
                holder.setConnected(status);
                return holder.getDataPublisher();
            }
        }
        return null;
    }


    public void notifyConnectionFailure(String receiverUrl, String username, String password) {
        setConnectionStatus(receiverUrl, username, password, false);
    }

    public void resendEvents(LinkedBlockingQueue<Event> events) {
        if (null != events) {
            if (events.size() > 0) log.info("Resending the failed events....");
            while (true) {
                Event event = events.poll();
                if (null != event) {
                    publish(event);
                } else {
                    break;
                }
            }
        }
    }

    public void resendPublishedData(LinkedBlockingQueue<AsyncDataPublisher.PublishData> publishDatas) {
        if (null != publishDatas) {
            if (publishDatas.size() > 0) log.info("Resending the failed published data...");
            while (true) {
                AsyncDataPublisher.PublishData data = publishDatas.poll();
                if (null != data) {
                    try {
                        publish(data.getStreamName(), data.getStreamVersion(), data.getEvent());
                    } catch (AgentException e) {
                        log.error(e);
                    }
                } else {
                    break;
                }
            }
        }

    }

    public void notifyConnectionSuccess(String receiverUrl, String username, String password) {
        setConnectionStatus(receiverUrl, username, password, true);
    }

    protected void stop() {
        for (DataPublisherHolder aHolder : dataPublisherCache) {
            if (null != aHolder.getDataPublisher()) aHolder.getDataPublisher().stop();
        }
    }


    private class ReconnectionTask implements Runnable {

        public void run() {
            boolean isOneReceiverConnected = false;
            for (int i = START_INDEX; i <= maximumDataPublisherIndex; i++) {
                DataPublisherHolder dataPublisherHolder = dataPublisherCache.get(i);
                AgentServerURL serverURL = null;
                try {
                    serverURL = new AgentServerURL(dataPublisherHolder.getReceiverUrl());
                } catch (MalformedURLException ignored) {
                }
                if (!dataPublisherHolder.getConnected().get()) {
                    dataPublisherHolder.getDataPublisher().reconnect();
                } else {
                    if (null != serverURL && !isServerExists(serverURL.getHost(), serverURL.getPort())) {
                        dataPublisherHolder.setConnected(false);
                    }
                }
                if (dataPublisherHolder.getConnected().get()) {
                    isOneReceiverConnected = true;
                }
            }
            if (!isOneReceiverConnected) {
               log.error("No receiver is reachable, can't publish the events");
            }
        }

        private boolean isServerExists(String ip, int port) {
            try {
                new Socket(ip, port);
                return true;
            } catch (UnknownHostException e) {
                return false;
            } catch (IOException e) {
                return false;
            } catch (Exception e) {
                return false;
            }
        }
    }
}