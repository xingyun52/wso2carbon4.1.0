/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.andes.server;

import org.wso2.andes.server.cassandra.CassandraTopicPublisherManager;
import org.wso2.andes.server.cassandra.ClusteringEnabledSubscriptionManager;
import org.wso2.andes.server.cassandra.OnceInOrderEnabledSubscriptionManager;
import org.wso2.andes.server.cluster.ClusterManager;
import org.wso2.andes.server.cluster.coordination.SubscriptionCoordinationManager;
import org.wso2.andes.server.cluster.coordination.TopicSubscriptionCoordinationManager;
import org.wso2.andes.server.configuration.ClusterConfiguration;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.virtualhost.VirtualHostConfigSynchronizer;

/**
 * Class <code>ClusterResourceHolder</code> holds the Cluster implementation specific
 * Objects.This act as a Singleton object holder for that objects with in the broker.
 */
public class ClusterResourceHolder {

    private static ClusterResourceHolder resourceHolder;

    /**
     * Holds the Cassandra MessageStore instance to use throughout the broker
     */
    private CassandraMessageStore cassandraMessageStore;


    /**
     * Holds Cassandra SubscriptionManager
     */
    private ClusteringEnabledSubscriptionManager subscriptionManager;


    /**
     * Holds CassandraTopicPublisherManager instance
     */
    private CassandraTopicPublisherManager cassandraTopicPublisherManager;

    /**
     * Holds OnceInOrderEnabledSubscriptionManager instance
     * */

    private OnceInOrderEnabledSubscriptionManager onceInorderEnabledSubscriptionManager;


    /**
     * Holds the Cluster Configuration Data
     */
    private ClusterConfiguration clusterConfiguration;


    /**
     * Holds VirtualHost Config Synchronizer
     */
    private VirtualHostConfigSynchronizer virtualHostConfigSynchronizer;


    /**
     * Holds Subscription Coordination manager
     */
    private SubscriptionCoordinationManager subscriptionCoordinationManager;

    /**
     * Holds Topic Subscription Coordination manager
     */
    private TopicSubscriptionCoordinationManager topicSubscriptionCoordinationManager;
    /*
    *  Holds Cluster Manager Instance
     */
    private ClusterManager clusterManager;

    private ClusterResourceHolder() {

    }


    public static ClusterResourceHolder getInstance() {
        if (resourceHolder == null) {

            synchronized (ClusterResourceHolder.class) {
                if(resourceHolder == null)  {
                    resourceHolder = new ClusterResourceHolder();
                }
            }
        }

        return resourceHolder;
    }


    public CassandraMessageStore getCassandraMessageStore() {
        return cassandraMessageStore;
    }

    public ClusteringEnabledSubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    public void setCassandraMessageStore(CassandraMessageStore cassandraMessageStore) {
        this.cassandraMessageStore = cassandraMessageStore;
    }

    public void setSubscriptionManager(ClusteringEnabledSubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }


    public CassandraTopicPublisherManager getCassandraTopicPublisherManager() {
        return cassandraTopicPublisherManager;
    }

    public void setCassandraTopicPublisherManager(CassandraTopicPublisherManager cassandraTopicPublisherManager) {
        this.cassandraTopicPublisherManager = cassandraTopicPublisherManager;
    }

    public OnceInOrderEnabledSubscriptionManager getOnceInorderEnabledSubscriptionManager() {
        return onceInorderEnabledSubscriptionManager;
    }

    public void setOnceInorderEnabledSubscriptionManager(OnceInOrderEnabledSubscriptionManager onceInorderEnabledSubscriptionManager) {
        this.onceInorderEnabledSubscriptionManager = onceInorderEnabledSubscriptionManager;
    }

    public void setClusterManager(ClusterManager clusterManager)
    {
        this.clusterManager = clusterManager;
    }

    public ClusterManager getClusterManager()
    {
        return this.clusterManager;
    }

    public ClusterConfiguration getClusterConfiguration() {
        return clusterConfiguration;
    }

    public void setClusterConfiguration(ClusterConfiguration clusterConfiguration) {
        this.clusterConfiguration = clusterConfiguration;
    }


    public VirtualHostConfigSynchronizer getVirtualHostConfigSynchronizer() {
        return virtualHostConfigSynchronizer;
    }

    public void setVirtualHostConfigSynchronizer(VirtualHostConfigSynchronizer virtualHostConfigSynchronizer) {
        this.virtualHostConfigSynchronizer = virtualHostConfigSynchronizer;
    }

    public SubscriptionCoordinationManager getSubscriptionCoordinationManager() {
        return subscriptionCoordinationManager;
    }

    public void setSubscriptionCoordinationManager(SubscriptionCoordinationManager subscriptionCoordinationManager) {
        this.subscriptionCoordinationManager = subscriptionCoordinationManager;
    }

    public TopicSubscriptionCoordinationManager getTopicSubscriptionCoordinationManager() {
        return topicSubscriptionCoordinationManager;
    }

    public void setTopicSubscriptionCoordinationManager(TopicSubscriptionCoordinationManager topicSubscriptionCoordinationManager) {
        this.topicSubscriptionCoordinationManager = topicSubscriptionCoordinationManager;
    }
}
