/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package org.wso2.carbon.appfactory.tenant.roles.S2Integration;

public class CartridgeInfo {
    private int minInstances;
    private int maxInstances;
    private boolean shouldActivate;
    private String alias;
    private String cartridgeType;
    private String repoURL;
    private String dataCartridgeType;
    private String dataCartridgeAlias;

    private String endpoint;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getMinInstances() {
        return minInstances;
    }

    public void setMinInstances(int minInstances) {
        this.minInstances = minInstances;
    }

    public int getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    public boolean isShouldActivate() {
        return shouldActivate;
    }

    public void setShouldActivate(boolean shouldActivate) {
        this.shouldActivate = shouldActivate;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCartridgeType() {
        return cartridgeType;
    }

    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }

    public String getRepoURL() {
        return repoURL;
    }

    public void setRepoURL(String repoURL) {
        this.repoURL = repoURL;
    }

    public String getDataCartridgeType() {
        return dataCartridgeType;
    }

    public void setDataCartridgeType(String dataCartridgeType) {
        this.dataCartridgeType = dataCartridgeType;
    }

    public String getDataCartridgeAlias() {
        return dataCartridgeAlias;
    }

    public void setDataCartridgeAlias(String dataCartridgeAlias) {
        this.dataCartridgeAlias = dataCartridgeAlias;
    }
}