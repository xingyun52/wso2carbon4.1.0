/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.wso2.carbon.ndatasource.rdbms.RDBMSConfiguration;
import org.wso2.carbon.rssmanager.common.RSSManagerHelper;
import org.wso2.carbon.rssmanager.core.internal.util.RSSManagerUtil;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to represent an RSS Server Instance.
 */
public class RSSInstance {

    private int id;

	private String name;
	
	private String serverURL;
	
	private String dbmsType;
	
	private String instanceType;

    private String serverCategory;
	
	private String adminUsername;
	
	private String adminPassword;
	
	private int tenantId;

    private DataSource dataSource;

	public RSSInstance(int id, String name, String serverURL,
			String dbmsType, String instanceType, String serverCategory, String adminUsername,
			String adminPassword, int tenantId) {
        this.id = id;
		this.name = name;
		this.serverURL = serverURL;
		this.dbmsType = dbmsType;
		this.instanceType = instanceType;
        this.serverCategory = serverCategory;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
		this.tenantId = tenantId;
        this.dataSource = initDataSource();
	}

    public RSSInstance(int id, String name, String serverURL,
			String dbmsType, String instanceType, String serverCategory, String adminUsername,
			String adminPassword, int tenantId, DataSource dataSource) {
        this.id = id;
		this.name = name;
		this.serverURL = serverURL;
		this.dbmsType = dbmsType;
		this.instanceType = instanceType;
        this.serverCategory = serverCategory;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
		this.tenantId = tenantId;
        this.dataSource = dataSource;
	}

    public RSSInstance() {}

    private DataSource initDataSource() {
        RDBMSConfiguration config = new RDBMSConfiguration();
        List<RDBMSConfiguration.DataSourceProperty> dataSourceProps =
                new ArrayList<RDBMSConfiguration.DataSourceProperty>();
        RDBMSConfiguration.DataSourceProperty userProp = new RDBMSConfiguration.DataSourceProperty();
        userProp.setName("user");
        userProp.setValue(this.getAdminUsername());
        dataSourceProps.add(userProp);

        RDBMSConfiguration.DataSourceProperty passwordProp = new RDBMSConfiguration.DataSourceProperty();
        passwordProp.setName("password");
        passwordProp.setValue(this.getAdminPassword());
        dataSourceProps.add(passwordProp);

        RDBMSConfiguration.DataSourceProperty urlProp = new RDBMSConfiguration.DataSourceProperty();
        urlProp.setName("url");
        urlProp.setValue(this.getServerURL());
        dataSourceProps.add(urlProp);

        RDBMSConfiguration.DataSourceProperty portProp = new RDBMSConfiguration.DataSourceProperty();
        portProp.setName("port");
        portProp.setValue("3306");
        dataSourceProps.add(portProp);

        config.setDataSourceProps(dataSourceProps);
        config.setDataSourceClassName(RSSManagerHelper.getDatabaseDriver(this.getServerURL()));
        config.setTestOnBorrow(true);
        config.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer;org.wso2.carbon.ndatasource.rdbms.ConnectionRollbackOnReturnInterceptor");
        return RSSManagerUtil.createDataSource(config);
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}

	public String getDbmsType() {
		return dbmsType;
	}

	public void setDbmsType(String dbmsType) {
		this.dbmsType = dbmsType;
	}

	public String getInstanceType() {
		return instanceType;
	}

	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}

	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public int getTenantId() {
		return tenantId;
	}

	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

    public String getServerCategory() {
        return serverCategory;
    }

    public void setServerCategory(String serverCategory) {
        this.serverCategory = serverCategory;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
}
