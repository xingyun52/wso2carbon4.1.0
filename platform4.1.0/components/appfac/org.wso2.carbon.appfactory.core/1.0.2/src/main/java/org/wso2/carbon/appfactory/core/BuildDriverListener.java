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

package org.wso2.carbon.appfactory.core;

import javax.activation.DataHandler;

import org.wso2.carbon.appfactory.common.AppFactoryException;

/**
 * Listens to the events of the BuildDriver
 */
public interface BuildDriverListener {

    /**
     *
     * @param applicationId
     * @param version
     * @param revision
     * @param data
     * @param fileName
     * @throws AppFactoryException
     */
    public void onBuildSuccessful(String applicationId, String version, String revision, String buildId, DataHandler data, String fileName)
            throws AppFactoryException;

    /**
     * Called upon build failure
     * 
     * @param applicationId
     * @param version
     * @param revision
     * @param revision
     * @param errorMessage
     * @throws AppFactoryException
     */
    public void onBuildFailure(String applicationId, String version, String revision, String buildId,
                               String errorMessage) throws AppFactoryException;

}
