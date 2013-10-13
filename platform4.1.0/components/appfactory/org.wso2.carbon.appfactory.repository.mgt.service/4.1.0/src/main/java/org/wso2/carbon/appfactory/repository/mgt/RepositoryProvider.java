/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.wso2.carbon.appfactory.repository.mgt;

import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;

/**
 * Every repository provider should implement this interface
 */
public interface RepositoryProvider {
    /**
     * Create a repository for a application key
     *
     * @param applicationKey
     * @return The url for created repository
     * @throws RepositoryMgtException
     */
    public String createRepository(String applicationKey) throws RepositoryMgtException;

    /**
     * Return the repository url if exists or return null
     *
     * @param applicationKey
     * @return repository url or null
     * @throws RepositoryMgtException
     */
    public String getAppRepositoryURL(String applicationKey) throws RepositoryMgtException;

    /**
     * This is the method  used to set appfactory configuration by appfactory.
     * Repository provider should implement this method to use the appfactory configs meaning fully
     *
     * @param configuration
     */
    public void setConfiguration(AppFactoryConfiguration configuration);
}
