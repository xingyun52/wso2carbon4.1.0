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

package org.wso2.carbon.cep.core.internal;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.broker.core.BrokerConfiguration;
import org.wso2.carbon.broker.core.BrokerService;
import org.wso2.carbon.broker.core.exception.BrokerConfigException;
import org.wso2.carbon.broker.core.exception.BrokerEventProcessingException;
import org.wso2.carbon.cep.core.Bucket;
import org.wso2.carbon.cep.core.Query;
import org.wso2.carbon.cep.core.backend.CEPBackEndRuntime;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.exception.CEPEventProcessingException;
import org.wso2.carbon.cep.core.internal.config.BrokerConfigurationHelper;
import org.wso2.carbon.cep.core.internal.ds.CEPServiceValueHolder;
import org.wso2.carbon.cep.core.internal.util.CEPRegistryUtils;
import org.wso2.carbon.cep.core.listener.BrokerEventListener;
import org.wso2.carbon.cep.core.listener.CEPEventListener;
import org.wso2.carbon.cep.core.listener.TopicEventListener;
import org.wso2.carbon.cep.core.mapping.input.Input;
import org.wso2.carbon.cep.core.mapping.input.mapping.InputMapping;
import org.wso2.carbon.cep.core.mapping.output.mapping.TupleOutputMapping;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.util.Map;

/**
 * this class is used to keep the Bucket related details. In cep point of view
 * a bucket is used to keep a cep runtime to which users and andd queries and set
 * input streams to receive data.
 */

public class CEPBucket {

    private static final Log log = LogFactory.getLog(CEPBucket.class);
    /**
     * real back end runtime. this can either be an esper runtime or an fusion runtime
     */
    private CEPBackEndRuntime cepBackEndRuntime;

    /**
     * details fo the buck attributes
     */
    private Bucket bucket;

    private String bucketPath;

    private int tenantId;

    private String userName;

    private AxisConfiguration axisConfiguration;


    public CEPBucket(CEPBackEndRuntime cepBackEndRuntime, Bucket bucket,
                     AxisConfiguration axisConfiguration, String bucketPath) {
        this.cepBackEndRuntime = cepBackEndRuntime;
        this.bucket = bucket;
        this.axisConfiguration = axisConfiguration;
        tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();
        this.userName = bucket.getOwner();
        this.bucketPath = bucketPath;
    }

    public void init() throws CEPConfigurationException {

        try {
            // create the listeners for subscriptions.
            if (bucket.getInputs() != null) {
                for (Input input : bucket.getInputs()) {
                    processInput(input);
                }
            }
        } catch (Throwable e) {
            throw new CEPConfigurationException("Error in creating the listeners for subscriptions in Siddhi backend Runtime," + e.getMessage(), e);
        }
        try {
            // register the queries
            if (bucket.getQueries() != null) {
                Map<Integer, Query> queries = bucket.getQueriesMap();
                for (int i = 0; i < queries.size(); i++) {
                    processQuery(queries.get(i));
                }
            }
        } catch (Throwable e) {
            throw new CEPConfigurationException("Error in registering the queries in Siddhi backend Runtime," + e.getMessage(), e);
        }

        try {
            this.cepBackEndRuntime.init();
        } catch (Throwable e) {
            throw new CEPConfigurationException("Error in initializing Siddhi backend Runtime," + e.getMessage(), e);
        }

    }

    public void processQuery(Query query) throws CEPConfigurationException {

        CEPEventListener cepEventListener = null;
        if (query.getOutput() != null) {
            String topic=query.getOutput().getTopic();
            if(CEPRegistryUtils.isRegistryPath(topic)){
                topic =CEPRegistryUtils.getResource(topic);
            }
            if (query.getOutput().getOutputMapping() != null && query.getOutput().getOutputMapping() instanceof TupleOutputMapping) {

                ((TupleOutputMapping) (query.getOutput().getOutputMapping())).initStreamDefinition(topic);
            }
            try {
                cepEventListener = new CEPEventListener(query.getOutput(), tenantId, userName, CEPServiceValueHolder.getInstance().getCepStatisticsManager().createNewCEPStatisticMonitor(topic, query.getOutput().getBrokerName(), bucket.getName(), tenantId));
            } catch (BrokerConfigException e) {
                String errorMessage = "Can not subscribe to output the broker, No broker config found for " + query.getOutput().getBrokerName();
                log.error(errorMessage);
                throw new CEPConfigurationException(errorMessage, e);
            }
            this.cepBackEndRuntime.addQuery(query.getName(), query.getExpression(), cepEventListener);
        } else {
            this.cepBackEndRuntime.addQuery(query.getName(), query.getExpression(), null);
        }

    }

    public void processInput(Input input) throws CEPConfigurationException {

        cepBackEndRuntime.addInput(input);
        TopicEventListener topicEventListener = new TopicEventListener(this, input, CEPServiceValueHolder.getInstance().getCepStatisticsManager().createNewCEPStatisticMonitor(input.getTopic(), input.getBrokerName(), bucket.getName(), tenantId));
        // subscribe to the broker if given.
        if (input.getBrokerName() != null) {
            BrokerEventListener brokerEventListener = new BrokerEventListener(topicEventListener);
            BrokerService brokerService = CEPServiceValueHolder.getInstance().getBrokerService();
            BrokerConfigurationHelper brokerConfigurationHelper = new BrokerConfigurationHelper();
            BrokerConfiguration brokerConfiguration =
                    null;
            try {
                brokerConfiguration = brokerConfigurationHelper.getBrokerConfiguration(input.getBrokerName(), tenantId);

                input.setSubscriptionId(brokerService.subscribe(brokerConfiguration, input.getTopic(),
                                                                brokerEventListener, axisConfiguration));
            } catch (BrokerEventProcessingException e) {
                String errorMessage = "Can not subscribe to the broker " + input.getBrokerName();
                log.error(errorMessage);
                throw new CEPConfigurationException(errorMessage, e);
            } catch (BrokerConfigException e) {
                String errorMessage = "Can not subscribe, no broker config found for " + input.getBrokerName();
                log.error(errorMessage);
                throw new CEPConfigurationException(errorMessage, e);
            }
        }

    }

    public void insertEvent(Object event, InputMapping inputMapping)
            throws CEPEventProcessingException {
        this.cepBackEndRuntime.insertEvent(event, inputMapping);
    }

    public Bucket getBucket() {
        return bucket;
    }

    public void setBucket(Bucket bucket) {
        this.bucket = bucket;
    }

    public void removeQuery(Query query) throws CEPConfigurationException {
        this.cepBackEndRuntime.removeQuery(query.getName());
    }

    public void removeAllQueries() throws CEPConfigurationException {
        this.cepBackEndRuntime.removeAllQueries();
    }

    public void editQuery(Query query) throws CEPConfigurationException {
        removeQuery(query);
        processQuery(query);
    }

    public void unSubscribeFromInput(Input input) throws CEPConfigurationException {
        cepBackEndRuntime.removeInput(input);
        if (input.getBrokerName() != null) {
            BrokerService brokerService = CEPServiceValueHolder.getInstance().getBrokerService();
            BrokerConfigurationHelper brokerConfigurationHelper = new BrokerConfigurationHelper();
            try {
                BrokerConfiguration brokerConfiguration =
                        brokerConfigurationHelper.getBrokerConfiguration(input.getBrokerName(), tenantId);
                brokerService.unsubscribe(input.getTopic(), brokerConfiguration, axisConfiguration, input.getSubscriptionId());
            } catch (BrokerEventProcessingException e) {
                String errorMessage = "Can not unsubscribe to the broker " + input.getBrokerName();
                throw new CEPConfigurationException(errorMessage, e);
            } catch (BrokerConfigException e) {
                String errorMessage = "Can not unsubscribe to the broker, No broker config found for " + input.getBrokerName();
                log.error(errorMessage);
                throw new CEPConfigurationException(errorMessage, e);
            }
        }
    }

    public void unSubscribeFromAllInputs() throws CEPConfigurationException {
        for (Input input : this.bucket.getInputs()) {
            unSubscribeFromInput(input);
        }
    }

    /**
     * unsubscribe from all the subscriptions and remove all queries
     *
     * @throws CEPConfigurationException
     */
    public void delete() throws CEPConfigurationException {

        unSubscribeFromAllInputs();
        removeAllQueries();
        log.info("bucket " + bucket.getName() + " deleted");


    }

    public String getBucketPath() {
        return bucketPath;
    }

    public void setBucketPath(String bucketPath) {
        this.bucketPath = bucketPath;
    }

    public void shutdown() {
        cepBackEndRuntime.shutdown();

    }
}
