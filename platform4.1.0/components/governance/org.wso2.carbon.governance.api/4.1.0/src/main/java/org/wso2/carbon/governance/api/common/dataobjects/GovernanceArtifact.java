/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.governance.api.common.dataobjects;

import org.wso2.carbon.governance.api.exception.GovernanceException;

import javax.xml.namespace.QName;


public interface GovernanceArtifact {
    /**
     * Returns the QName of the artifact.
     *
     * @return the QName of the artifact
     */
    QName getQName();

    /**
     * Returns the id of the artifact
     *
     * @return the id
     */
    String getId();

    /**
     * Set the id
     *
     * @param id the id
     */
    void setId(String id);

    /**
     * Returns the path of the artifact, need to save the artifact before
     * getting the path.
     *
     * @return here we return the path of the artifact.
     *
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException if an error occurred.
     */
    String getPath() throws GovernanceException;

    /**
     * Returns the name of the lifecycle associated with this artifact.
     *
     * @return the name of the lifecycle associated with this artifact.
     *
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException if an error occurred.
     */
    String getLifecycleName() throws GovernanceException;

    /**
     * Associates the named lifecycle with the artifact
     *
     * @param name the name of the lifecycle to be associated with this artifact.
     *
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException if an error occurred.
     */
    void attachLifecycle(String name) throws GovernanceException;

    /**
     * Returns the state of the lifecycle associated with this artifact.
     *
     * @return the state of the lifecycle associated with this artifact.
     *
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException if an error occurred.
     */
    String getLifecycleState() throws GovernanceException;

    /**
     * Adding an attribute to the artifact. The artifact should be saved to get effect the change.
     * In the case of a single-valued attribute, this method will set or replace the existing
     * attribute with the provided value. In the case of a multi-valued attribute, this method will
     * append the provided value to the existing list.
     *
     * @param key   the key.
     * @param value the value.
     *
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException throws if the operation failed.
     */
    void addAttribute(String key, String value) throws GovernanceException;

    /**
     * Set/Update an attribute with multiple values. The artifact should be saved to get effect the
     * change.
     *
     * @param key       the key
     * @param newValues the value
     *
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException throws if the operation failed.
     */
    void setAttributes(String key, String[] newValues) throws GovernanceException;

    /**
     * Set/Update an attribute with a single value. The artifact should be saved to get effect the
     * change. This method will replace the existing attribute with the provided value. In the case
     * of a multi-valued attribute this will remove all existing values. If you want to append the
     * provided value to a list values of a multi-valued attribute, use the addAttribute method
     * instead.
     *
     * @param key      the key
     * @param newValue the value
     *
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException throws if the operation failed.
     */
    void setAttribute(String key, String newValue) throws GovernanceException;

    /**
     * Returns the attribute of a given key.
     *
     * @param key the key
     *
     * @return the value of the attribute, if there are more than one attribute for the key this
     *         returns the first value.
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException throws if the operation failed.
     */
    String getAttribute(String key) throws GovernanceException;

    /**
     * Returns the available attribute keys
     *
     * @return an array of attribute keys.
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException throws if the operation failed.
     */
    String[] getAttributeKeys() throws GovernanceException;

    /**
     * Returns the attribute values for a key.
     *
     * @param key the key.
     *
     * @return attribute values for the key.
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException throws if the operation failed.
     */
    String[] getAttributes(String key) throws GovernanceException;

    /**
     * Remove attribute with the given key. The artifact should be saved to get effect the change.
     *
     * @param key the key
     *
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException throws if the operation failed.
     */
    void removeAttribute(String key) throws GovernanceException;

    /**
     * Get dependencies of an artifacts. The artifacts should be saved, before calling this method.
     *
     * @return an array of dependencies of this artifact.
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException throws if the operation failed.
     */
    GovernanceArtifact[] getDependencies() throws GovernanceException;

    /**
     * Get dependents of an artifact. The artifacts should be saved, before calling this method.
     *
     * @return an array of artifacts that is dependent on this artifact.
     * @throws org.wso2.carbon.governance.api.exception.GovernanceException throws if the operation failed.
     */
    GovernanceArtifact[] getDependents() throws GovernanceException;
}
