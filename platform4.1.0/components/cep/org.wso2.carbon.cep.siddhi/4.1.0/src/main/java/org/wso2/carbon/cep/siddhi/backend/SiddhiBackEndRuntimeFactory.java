/*
 * Copyright 2004,2012 The Apache Software Foundation.
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

package org.wso2.carbon.cep.siddhi.backend;

import me.prettyprint.hector.api.Cluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.backend.CEPBackEndRuntime;
import org.wso2.carbon.cep.core.backend.CEPBackEndRuntimeFactory;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.mapping.input.mapping.InputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.MapInputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.TupleInputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.XMLInputMapping;
import org.wso2.carbon.cep.core.mapping.input.property.MapInputProperty;
import org.wso2.carbon.cep.core.mapping.input.property.TupleInputProperty;
import org.wso2.carbon.cep.core.mapping.input.property.XMLInputProperty;
import org.wso2.carbon.cep.siddhi.internal.ds.SiddhiBackendRuntimeValueHolder;
import org.wso2.carbon.cep.siddhi.internal.ds.SiddhiConfigLoader;
import org.wso2.carbon.cep.siddhi.persistence.CasandraPersistenceStore;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.config.SiddhiConfiguration;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.query.api.definition.StreamDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class SiddhiBackEndRuntimeFactory implements CEPBackEndRuntimeFactory {

    private static final Log log = LogFactory.getLog(SiddhiBackEndRuntimeFactory.class);
    public static final String PERSISTENCE_SNAPSHOT_TIME_INTERVAL_MINUTES = "siddhi.persistence.snapshot.time.interval.minutes";
    public static final String ENABLE_DISTRIBUTED_PROCESSING = "siddhi.enable.distributed.processing";

    public CEPBackEndRuntime createCEPBackEndRuntime(String bucketName,
                                                     Properties providerConfiguration,
                                                     List<InputMapping> mappings,
                                                     int tenantId)
            throws CEPConfigurationException {

        try {
            long persistenceTimeInterval = 0;
            boolean isDistributedProcessingEnabled = false;
            if (providerConfiguration != null && providerConfiguration.size() > 0) {
                String timeString = providerConfiguration.getProperty(PERSISTENCE_SNAPSHOT_TIME_INTERVAL_MINUTES);
                try {
                    persistenceTimeInterval = Long.parseLong(timeString);
                } catch (NumberFormatException e) {
                    log.warn("Error reading siddhi persistence snapshot time interval, hence persistence is disabled for " + bucketName);
                }
                String isDistributedProcessingEnabledString = providerConfiguration.getProperty(ENABLE_DISTRIBUTED_PROCESSING);
                if (isDistributedProcessingEnabledString.toLowerCase().equals("true") || isDistributedProcessingEnabledString.toLowerCase().equals("false")) {
                    isDistributedProcessingEnabled = Boolean.parseBoolean(isDistributedProcessingEnabledString);
                } else {
                    log.warn("Error reading siddhi enable distributed processing, hence distributed processing is disabled for " + bucketName);
                }
            }
            SiddhiConfiguration siddhiConfig = new SiddhiConfiguration();
            siddhiConfig.setAsyncProcessing(false); //todo check which is good?
            siddhiConfig.setQueryPlanIdentifier(bucketName);
            siddhiConfig.setInstanceIdentifier("WSO2CEP-Siddhi-Instance-" + UUID.randomUUID().toString());
            siddhiConfig.setDistributedProcessing(isDistributedProcessingEnabled);
            if(null==SiddhiBackendRuntimeValueHolder.getInstance().getSiddhiExtentions()){
                SiddhiBackendRuntimeValueHolder.getInstance().setSiddhiExtentions(SiddhiConfigLoader.loadSiddhiExtensions());
            }
            siddhiConfig.setSiddhiExtensions(SiddhiBackendRuntimeValueHolder.getInstance().getSiddhiExtentions());
            SiddhiManager siddhiManager = new SiddhiManager(siddhiConfig);
            if (persistenceTimeInterval > 0) {
                if (null == SiddhiBackendRuntimeValueHolder.getInstance().getPersistenceStore()) {
                    Cluster cluster = SiddhiBackendRuntimeValueHolder.getInstance().getDataAccessService().getCluster(SiddhiBackendRuntimeValueHolder.getInstance().getClusterInformation());
                    SiddhiBackendRuntimeValueHolder.getInstance().setClusterName(cluster.getName());
                    CasandraPersistenceStore casandraPersistenceStore = new CasandraPersistenceStore(cluster);
                    SiddhiBackendRuntimeValueHolder.getInstance().setPersistenceStore(casandraPersistenceStore);
                }
                siddhiManager.setPersistStore(SiddhiBackendRuntimeValueHolder.getInstance().getPersistenceStore());

            }

            Map<String, InputHandler> siddhiInputHandlerMap = new HashMap<String, InputHandler>();

            for (InputMapping mapping : mappings) {

                StreamDefinition streamDefinition = new StreamDefinition();
                streamDefinition.name(mapping.getStream());

                if (mapping instanceof TupleInputMapping) {
                    TupleInputMapping tupleInputMapping = (TupleInputMapping) mapping;
                    List<TupleInputProperty> properties = tupleInputMapping.getProperties();
                    for (TupleInputProperty property1 : properties) {
                        streamDefinition.attribute(property1.getName(), SiddhiBackEndRuntime.javaToSiddhiType.get(property1.getType()));
                    }
                } else if (mapping instanceof MapInputMapping) {
                    MapInputMapping mapInputMapping = (MapInputMapping) mapping;
                    List<MapInputProperty> properties = mapInputMapping.getProperties();
                    for (MapInputProperty property1 : properties) {
                        streamDefinition.attribute(property1.getName(), SiddhiBackEndRuntime.javaToSiddhiType.get(property1.getType()));
                    }
                } else { //Xml mapping
                    XMLInputMapping xmlInputMapping = (XMLInputMapping) mapping;
                    List<XMLInputProperty> properties = xmlInputMapping.getProperties();
                    for (XMLInputProperty property1 : properties) {
                        streamDefinition.attribute(property1.getName(), SiddhiBackEndRuntime.javaToSiddhiType.get(property1.getType()));
                    }
                }


//            try {
                siddhiInputHandlerMap.put(mapping.getStream(), siddhiManager.defineStream(streamDefinition));
//            } catch (SiddhiException e) {
//                throw new CEPConfigurationException("Invalid input stream configuration for " +
//                                                    mapping.getStream(), e);
//            }
            }

            return new SiddhiBackEndRuntime(bucketName, siddhiManager, siddhiInputHandlerMap, tenantId, persistenceTimeInterval);
        } catch (Throwable e) {
            throw new CEPConfigurationException("Error occurred in creating Siddhi Backend Runtime," + e);
        }
    }
}
