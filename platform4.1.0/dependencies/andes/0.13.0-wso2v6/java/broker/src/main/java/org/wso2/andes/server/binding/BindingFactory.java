/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.andes.server.binding;

import org.wso2.andes.AMQException;
import org.wso2.andes.AMQInternalException;
import org.wso2.andes.AMQSecurityException;
import org.wso2.andes.exchange.ExchangeDefaults;
import org.wso2.andes.framing.AMQShortString;
import org.wso2.andes.framing.FieldTable;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.cassandra.TopicDeliveryWorker;
import org.wso2.andes.server.cassandra.CassandraTopicPublisherManager;
import org.wso2.andes.server.configuration.BindingConfig;
import org.wso2.andes.server.configuration.BindingConfigType;
import org.wso2.andes.server.configuration.ConfigStore;
import org.wso2.andes.server.configuration.ConfiguredObject;
import org.wso2.andes.server.exchange.Exchange;
import org.wso2.andes.server.logging.actors.CurrentActor;
import org.wso2.andes.server.logging.messages.BindingMessages;
import org.wso2.andes.server.logging.subjects.BindingLogSubject;
import org.wso2.andes.server.queue.AMQQueue;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.store.DurableConfigurationStore;
import org.wso2.andes.server.store.util.CassandraDataAccessException;
import org.wso2.andes.server.virtualhost.VirtualHost;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BindingFactory
{
    private final VirtualHost _virtualHost;
    private final DurableConfigurationStore.Source _configSource;
    private final Exchange _defaultExchange;

    private final ConcurrentHashMap<BindingImpl, BindingImpl> _bindings = new ConcurrentHashMap<BindingImpl, BindingImpl>();


    public BindingFactory(final VirtualHost vhost)
    {
        this(vhost, vhost.getExchangeRegistry().getDefaultExchange());
    }

    public BindingFactory(final DurableConfigurationStore.Source configSource, final Exchange defaultExchange)
    {
        _configSource = configSource;
        _defaultExchange = defaultExchange;
        if (configSource instanceof VirtualHost)
        {
            _virtualHost = (VirtualHost) configSource;
        }
        else
        {
            _virtualHost = null;
        }
    }

    public VirtualHost getVirtualHost()
    {
        return _virtualHost;
    }



    private final class BindingImpl extends Binding implements AMQQueue.Task, Exchange.Task, BindingConfig
    {
        private final BindingLogSubject _logSubject;
        //TODO : persist creation time
        private long _createTime = System.currentTimeMillis();

        private BindingImpl(String bindingKey, final AMQQueue queue, final Exchange exchange, final Map<String, Object> arguments)
        {
            super(queue.getVirtualHost().getConfigStore().createId(), bindingKey, queue, exchange, arguments);
            _logSubject = new BindingLogSubject(bindingKey,exchange,queue);

        }


        public void doTask(final AMQQueue queue) throws AMQException
        {
            removeBinding(this);
        }

        public void onClose(final Exchange exchange) throws AMQSecurityException, AMQInternalException
        {
            removeBinding(this);
        }

        void logCreation()
        {
            CurrentActor.get().message(_logSubject, BindingMessages.CREATED(String.valueOf(getArguments()), getArguments() != null && !getArguments().isEmpty()));
        }

        void logDestruction()
        {
            CurrentActor.get().message(_logSubject, BindingMessages.DELETED());
        }

        public String getOrigin()
        {
            return (String) getArguments().get("qpid.fed.origin");
        }

        public long getCreateTime()
        {
            return _createTime;
        }

        public BindingConfigType getConfigType()
        {
            return BindingConfigType.getInstance();
        }

        public ConfiguredObject getParent()
        {
            return _virtualHost;
        }

        public boolean isDurable()
        {
            return getQueue().isDurable() && getExchange().isDurable();
        }

    }



    public boolean addBinding(String bindingKey, AMQQueue queue, Exchange exchange, Map<String, Object> arguments) throws AMQSecurityException, AMQInternalException 
    {
    //    CassandraMessageStore.getInstance().addBinding(exchange,queue,bindingKey);
        return makeBinding(bindingKey, queue, exchange, arguments, false, false);
    }

    /**
     * to avoid javax.jms.InvalidDestinationException: Queue: direct://amq.direct/myQueueNew/<queueName>?
     * routingkey='<queueName>'&durable='true' is not a valid destination exception at client side we create a binding
     * and let it synchronize across the cluster
     * @param queueName
     * @param routingKey
     * @return is operation success
     */
    public boolean addInitialBindingForQueue(String queueName, String routingKey) {
        boolean success = false;
        try {
            ClusterResourceHolder.getInstance().getCassandraMessageStore().addBinding(ExchangeDefaults.DEFAULT_EXCHANGE_NAME.toString(),queueName,routingKey);
            ClusterResourceHolder.getInstance().getCassandraMessageStore().addBinding(ExchangeDefaults.DIRECT_EXCHANGE_NAME.toString(),queueName,routingKey);
            success = true;
        } catch (CassandraDataAccessException e) {
            success = false;
        }
        return success;
    }


    public boolean replaceBinding(final String bindingKey,
                               final AMQQueue queue,
                               final Exchange exchange,
                               final Map<String, Object> arguments) throws AMQSecurityException, AMQInternalException
    {
        return makeBinding(bindingKey, queue, exchange, arguments, false, true);
    }

    private boolean makeBinding(String bindingKey, AMQQueue queue, Exchange exchange, Map<String, Object> arguments, boolean restore, boolean force) throws AMQSecurityException, AMQInternalException
    {
        assert queue != null;
        if (bindingKey == null)
        {
            bindingKey = "";
        }
        if (exchange == null)
        {
            exchange = _defaultExchange;
        }
        if (arguments == null)
        {
            arguments = Collections.emptyMap();
        }
      
        //Perform ACLs
        if (!getVirtualHost().getSecurityManager().authoriseBind(exchange, queue, new AMQShortString(bindingKey)))
        {
            throw new AMQSecurityException("Permission denied: binding " + bindingKey);
        }
        
        BindingImpl b = new BindingImpl(bindingKey,queue,exchange,arguments);
        BindingImpl existingMapping = _bindings.putIfAbsent(b,b);
        if (existingMapping == null || force)
        {
            if (existingMapping != null)
            {
                removeBinding(existingMapping);
            }

            if (b.isDurable() && !restore)
            {
                _configSource.getDurableConfigurationStore().bindQueue(exchange,new AMQShortString(bindingKey),queue,FieldTable.convertToFieldTable(arguments));
            }

            queue.addQueueDeleteTask(b);
            exchange.addCloseTask(b);
            queue.addBinding(b);
            exchange.addBinding(b);
            getConfigStore().addConfiguredObject(b);
            b.logCreation();
             if (exchange.getName().equalsIgnoreCase("amq.direct")) {
                /* CassandraMessageStore cassandraMessageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
                 ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();

                 try {
                     cassandraMessageStore.createGlobalQueue(b.getNodeQueue().getResourceName());
                     clusterManager.handleQueueAddition(b.getNodeQueue().getResourceName());
                 } catch (Exception e) {
                     throw new AMQInternalException("Error while adding a queue to direct exchange ", e);
                 }*/
             }
             if (exchange.getName().equalsIgnoreCase("amq.topic")) {
                boolean isInMemoryMode = ClusterResourceHolder.getInstance().getClusterConfiguration().isInMemoryMode();
                TopicDeliveryWorker topicDeliveryWorker = new TopicDeliveryWorker(b,queue,exchange,getVirtualHost(),isInMemoryMode);
                try {
                    handleTask(topicDeliveryWorker);
                } catch (AMQException e) {
                   throw new AMQInternalException("Error in creating publisher for queue" + queue.getName() ,e);
                }
             }
            return true;
        }
        else
        {
            return false;
        }
    }

    public void handleTask(TopicDeliveryWorker topicDeliveryWorker) throws AMQException {

        if(ClusterResourceHolder.getInstance().getCassandraTopicPublisherManager() == null) {
            CassandraTopicPublisherManager cassandraTopicPublisherManager = new CassandraTopicPublisherManager();
            ClusterResourceHolder.getInstance().setCassandraTopicPublisherManager(cassandraTopicPublisherManager);
        }
        ClusterResourceHolder.getInstance().getCassandraTopicPublisherManager().
                addWork(topicDeliveryWorker.getQueueId(), topicDeliveryWorker);
    }


    private ConfigStore getConfigStore()
    {
        return getVirtualHost().getConfigStore();
    }

    public void restoreBinding(final String bindingKey, final AMQQueue queue, final Exchange exchange, final Map<String, Object> argumentMap) throws AMQSecurityException, AMQInternalException
    {
        makeBinding(bindingKey,queue,exchange,argumentMap,true, false);
    }

    public void removeBinding(final Binding b) throws AMQSecurityException, AMQInternalException
    {
        removeBinding(b.getBindingKey(), b.getQueue(), b.getExchange(), b.getArguments());

        ClusterResourceHolder.getInstance().getCassandraTopicPublisherManager().
                markSubscriptionForRemoval(b.getQueue().getName());

        CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().
                getCassandraMessageStore();
        messageStore.unRegisterQueueFromTopic(b.getBindingKey(), b.getQueue().getName());
    }


    public Binding removeBinding(String bindingKey, AMQQueue queue, Exchange exchange, Map<String, Object> arguments) throws AMQSecurityException, AMQInternalException
    {
        assert queue != null;
        if (bindingKey == null)
        {
            bindingKey = "";
        }
        if (exchange == null)
        {
            exchange = _defaultExchange;
        }
        if (arguments == null)
        {
            arguments = Collections.emptyMap();
        }

        // Check access
        if (!getVirtualHost().getSecurityManager().authoriseUnbind(exchange, new AMQShortString(bindingKey), queue))
        {
            throw new AMQSecurityException("Permission denied: binding " + bindingKey);
        }
        
        BindingImpl b = _bindings.remove(new BindingImpl(bindingKey,queue,exchange,arguments));

        if (b != null)
        {
            exchange.removeBinding(b);
            queue.removeBinding(b);
            exchange.removeCloseTask(b);
            queue.removeQueueDeleteTask(b);

            if (b.isDurable())
            {
                _configSource.getDurableConfigurationStore().unbindQueue(exchange,
                                         new AMQShortString(bindingKey),
                                         queue,
                                         FieldTable.convertToFieldTable(arguments));
            }
            b.logDestruction();
            getConfigStore().removeConfiguredObject(b);
        }



        return b;
    }

    public Binding getBinding(String bindingKey, AMQQueue queue, Exchange exchange, Map<String, Object> arguments)
    {
        assert queue != null;
        if(bindingKey == null)
        {
            bindingKey = "";
        }
        if(exchange == null)
        {
            exchange = _defaultExchange;
        }
        if(arguments == null)
        {
            arguments = Collections.emptyMap();
        }

        BindingImpl b = new BindingImpl(bindingKey,queue,exchange,arguments);
        return _bindings.get(b);
    }
}
