package org.wso2.carbon.brokermanager.admin.internal;

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

/**
 * Broker property related attributes are stored
 */
public class BrokerProperty {
    // property name
    private String key;
    // value of the property
    private String value;
    // if this property is a required field or not
    private boolean isRequired;
    // if this property is a password field or not
    private boolean isSecured;
    // display name of the property
    private String displayName;

    public BrokerProperty() {
    }

    public BrokerProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public boolean isSecured() {
        return isSecured;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public void setSecured(boolean secured) {
        isSecured = secured;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
