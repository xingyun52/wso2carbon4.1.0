/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.rssmanager.core.entity;

public class DatabasePrivilegeTemplate {

    private int id;

    private String name;

    private DatabasePrivilegeSet privileges;

    private int tenantId;

    public DatabasePrivilegeTemplate(int id, String name, DatabasePrivilegeSet privileges) {
        this.id = id;
        this.name = name;
        this.privileges = privileges;
    }

    public DatabasePrivilegeTemplate(int id, String name, DatabasePrivilegeSet privileges,
                                     int tenantId) {
        this.id = id;
        this.name = name;
        this.privileges = privileges;
        this.tenantId = tenantId;
    }

    public DatabasePrivilegeTemplate() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DatabasePrivilegeSet getPrivileges() {
        return privileges;
    }

    public void setPrivileges(DatabasePrivilegeSet privileges) {
        this.privileges = privileges;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
    
}