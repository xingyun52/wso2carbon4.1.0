/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse;

import org.apache.synapse.config.SynapsePropertiesLoader;
import org.wso2.securevault.secret.SecretManager;
import org.wso2.securevault.secret.mbean.SecretManagerAdminMBean;

import javax.management.StandardMBean;
import javax.management.NotCompliantMBeanException;


/**
 * Admin service for managing SecretManager
 */

public class SecretManagerAdminMBeanImpl extends StandardMBean implements SecretManagerAdminMBean {

    private final SecretManager secretManager = SecretManager.getInstance();

    public SecretManagerAdminMBeanImpl() throws NotCompliantMBeanException {
        super(SecretManagerAdminMBean.class);
    }

    /**
     * @see SecretManagerAdminMBean
     */
    public void init() {
        this.secretManager.init(SynapsePropertiesLoader.loadSynapseProperties());
    }

    public void shutDown() {
        this.secretManager.shoutDown();
    }
}
