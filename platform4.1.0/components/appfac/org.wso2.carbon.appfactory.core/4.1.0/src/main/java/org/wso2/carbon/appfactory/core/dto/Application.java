/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.appfactory.core.dto;

import org.wso2.carbon.appfactory.core.ApplicationEventsListener;

/**
 * Holds information about an Application. Refer {@link ApplicationEventsListener} for usage.
 */
public class Application {

    /**
     * Name of the application
     */
    private String name;

    /**
     * Description of the application
     */
    private String description;

    /**
     * The application Id
     */
    private String id;

    private Version[] versions;

    /**
     * Type of the application (e.g. car, war)
     */
    private String type;
    /**
     * Repository type to be used in application(e.g svn,git)
     */
    private  String repositoryType;

    public Application() {

    }

    public Application(String id, String name, String appType,String repositoryType,
                       String description) {
        this.name = name;
        this.description = description;
        this.id = id;
        this.type = appType;
        this.repositoryType=repositoryType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Version[] getVersions() {
        return versions;
    }

    public void setVersions(Version[] versions) {
        this.versions = versions;
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }
}
