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
package org.wso2.carbon.rssmanager.core.internal.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.ndatasource.common.DataSourceException;
import org.wso2.carbon.ndatasource.core.DataSourceMetaInfo;
import org.wso2.carbon.ndatasource.core.utils.DataSourceUtils;
import org.wso2.carbon.ndatasource.rdbms.RDBMSConfiguration;
import org.wso2.carbon.ndatasource.rdbms.RDBMSDataSource;
import org.wso2.carbon.rssmanager.common.RSSManagerConstants;
import org.wso2.carbon.rssmanager.common.RSSManagerHelper;
import org.wso2.carbon.rssmanager.core.RSSManagerException;
import org.wso2.carbon.rssmanager.core.entity.*;
import org.wso2.carbon.rssmanager.core.entity.datasource.RSSRDBMSConfiguration;
import org.wso2.carbon.rssmanager.core.internal.RSSManagerServiceComponent;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RSSManagerUtil {

    public static TransactionManager transactionManager;
     private static final Log log= LogFactory.getLog(RSSManagerUtil.class);

    public static TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public static void setTransactionManager(TransactionManager transactionManager) {
        RSSManagerUtil.transactionManager = transactionManager;
    }

    /**
     * Retrieves the list of tenantIDs of the currently loaded tenants
     *
     * @return Tenant ID list
     * @throws RSSManagerException Thrown when an error occurs while retrieving the list of
     *                             tenants via the Tenant Tanager.
     */
    public static List<Integer> getAllTenants() throws RSSManagerException {
        List<Integer> tenantIds = new ArrayList<Integer>();
        TenantManager tenantMgr = RSSManagerServiceComponent.getTenantManager();
        if (tenantMgr != null) {
            try {
                for (Tenant tenant : tenantMgr.getAllTenants()) {
                    tenantIds.add(tenant.getId());
                }
                tenantIds.add(MultitenantConstants.SUPER_TENANT_ID);
            } catch (UserStoreException e) {
                throw new RSSManagerException("Error while retrieving tenant data", e);
            }
        }
        return tenantIds;
    }

    /**
     * Retrieves the tenant domain name for a given tenant ID
     *
     * @param tenantId Tenant Id
     * @return Domain name of corresponds to the provided tenant ID
     * @throws RSSManagerException Thrown when there's any error while retrieving the tenant
     *                             domain for the provided tenant ID
     */
    public static String getTenantDomainFromTenantId(int tenantId) throws RSSManagerException {
        TenantManager tenantMgr = RSSManagerServiceComponent.getTenantManager();
        try {
            return tenantMgr.getDomain(tenantId);
        } catch (UserStoreException e) {
            throw new RSSManagerException("Error occurred while retrieving tenant domain for " +
                    "the given tenant ID");
        }
    }

    /**
     * Returns the fully qualified name of the database to be created. This will append an
     * underscore and the tenant's domain name to the database to make it unique for that particular
     * tenant. It will return the database name as it is, if it is created in Super tenant mode.
     *
     * @param databaseName Name of the database
     * @return Fully qualified name of the database
     */
    public static String getFullyQualifiedDatabaseName(String databaseName) {
        String tenantDomain =
                null;
        try {
            tenantDomain =
                    RSSManagerServiceComponent.getTenantManager().getDomain(
                            PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId());
        } catch (UserStoreException ignore) {
        }
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
            return databaseName + "_" + RSSManagerHelper.processDomainName(tenantDomain);
        }
        return databaseName;
    }

    /**
     * Returns the fully qualified username of a particular database user. For an ordinary tenant,
     * the tenant domain will be appended to the username together with an underscore and the given
     * username will be returned as it is in the case of super tenant.
     *
     * @param username Username of the database user.
     * @return Fully qualified username of the database user.
     */
    public static String getFullyQualifiedUsername(String username) {
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {

            /* The maximum number of characters allowed for the username in mysql system tables is
             * 16. Thus, to adhere the aforementioned constraint as well as to give the username
             * an unique identification based on the tenant domain, we append a hash value that is
             * created based on the tenant domain */
            byte[] bytes = RSSManagerHelper.intToByteArray(tenantDomain.hashCode());
            return username + "_" + Base64.encode(bytes);
        }
        return username;
    }

    /**
     * Util method to prepare JDBC url of a particular RSS instance to be a valid url to be stored
     * in the metadata repository.
     *
     * @param url    JDBC url.
     * @param dbName Name of the database instance.
     * @return Processed JDBC url.
     */
    public static String processJdbcUrl(String url, String dbName) {
        if (url != null && !"".equals(url)) {
            return url.endsWith("/") ? (url + dbName) : (url + "/" + dbName);
        }
        return "";
    }

    public static DatabaseMetaData convertToDatabaseMetaData(Database database) throws
            RSSManagerException {
        DatabaseMetaData metadata = new DatabaseMetaData();
        String fullyQualifiedDatabaseName =
                RSSManagerUtil.getFullyQualifiedDatabaseName(database.getName());
        metadata.setName(fullyQualifiedDatabaseName);
        metadata.setRssInstanceName(metadata.getRssInstanceName());
        metadata.setUrl(database.getUrl());
        String tenantDomain = RSSManagerUtil.getTenantDomainFromTenantId(database.getTenantId());
        metadata.setRssTenantDomain(tenantDomain);

        return metadata;
    }

    public static DataSource createDataSource(OMElement dsEl) throws RSSManagerException {
        RDBMSConfiguration dsConfig = populateDataSourceProperties(dsEl);
        try {
            dsConfig.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer;org.wso2.carbon.ndatasource.rdbms.ConnectionRollbackOnReturnInterceptor");
            return (new RDBMSDataSource(dsConfig)).getDataSource();
        } catch (DataSourceException e) {
            throw new RuntimeException("Error in creating data source: " + e.getMessage(), e);
        }
    }

    public static DataSource createDataSource(RDBMSConfiguration config) {
        try {
            RDBMSDataSource dataSource = new RDBMSDataSource(config);
            return dataSource.getDataSource();
        } catch (DataSourceException e) {
            throw new RuntimeException("Error in creating data source: " + e.getMessage(), e);
        }
    }

    public static DataSource createDataSource(Properties properties, String dataSourceClassName) {
        RDBMSConfiguration config = new RDBMSConfiguration();
        config.setDataSourceClassName(dataSourceClassName);
        List<RDBMSConfiguration.DataSourceProperty> dsProps = new ArrayList<RDBMSConfiguration.DataSourceProperty>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            RDBMSConfiguration.DataSourceProperty property =
                    new RDBMSConfiguration.DataSourceProperty();
            property.setName((String) entry.getKey());
            property.setValue((String) entry.getValue());
            dsProps.add(property);
        }
        config.setDataSourceProps(dsProps);
        return createDataSource(config);
    }

    public static RDBMSConfiguration populateDataSourceProperties(OMElement dsEl) throws
            RSSManagerException {
        try {
            JAXBContext ctx = JAXBContext.newInstance(RSSRDBMSConfiguration.class);
            Unmarshaller um = ctx.createUnmarshaller();
            ByteArrayInputStream bis = new ByteArrayInputStream(dsEl.toString().getBytes());
            return (RSSRDBMSConfiguration) um.unmarshal(bis);
        } catch (JAXBException e) {
            throw new RSSManagerException("Error occurred while de-serializing datasource " +
                    "configuration", e);
        }
    }

    public static RSSInstanceMetaData convertRSSInstanceToMetadata(RSSInstance rssIns) throws
            RSSManagerException {
        RSSInstanceMetaData metadata = new RSSInstanceMetaData();
        metadata.setName(rssIns.getName());
        metadata.setServerUrl(rssIns.getServerURL());
        metadata.setInstanceType(rssIns.getDbmsType());
        metadata.setServerCategory(rssIns.getServerCategory());
        metadata.setTenantDomainName(getTenantDomainFromTenantId(rssIns.getTenantId()));
        return metadata;
    }

    public static DatabaseMetaData convertDatabaseToMetadata(Database database) throws
            RSSManagerException {
        DatabaseMetaData metadata = new DatabaseMetaData();
        metadata.setName(database.getName());
        if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(database.getType())) {
            metadata.setRssInstanceName(RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
        } else {
            metadata.setRssInstanceName(database.getRssInstanceName());
        }
        metadata.setUrl(database.getUrl());
        metadata.setRssTenantDomain(getTenantDomainFromTenantId(database.getTenantId()));
        return metadata;
    }

    public static DatabaseUserMetaData convertToDatabaseUserMetadata(DatabaseUser user) throws
            RSSManagerException {
        DatabaseUserMetaData metadata = new DatabaseUserMetaData();
        metadata.setUsername(user.getUsername());
        if (RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE.equals(user.getType())) {
            metadata.setRssInstanceName(RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE);
        } else {
            metadata.setRssInstanceName(user.getRssInstanceName());
        }
        metadata.setTenantDomain(getTenantDomainFromTenantId(user.getTenantId()));
        return metadata;
    }

    public static String composeDatabaseUrl(RSSInstance rssIns, String databaseName) {
        return rssIns.getServerURL() + "/" + databaseName;
    }

    private static DataSourceMetaInfo.DataSourceDefinition createDSXMLDefinition(
            RDBMSConfiguration rdbmsConfiguration) throws RSSManagerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            createMarshaller().marshal(rdbmsConfiguration, out);
        } catch (JAXBException e) {
            String msg = "Error occurred while marshalling datasource configuration";
            throw new RSSManagerException(msg, e);
        }
        DataSourceMetaInfo.DataSourceDefinition defn =
                new DataSourceMetaInfo.DataSourceDefinition();
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        defn.setType(RSSManagerConstants.RDBMS_DATA_SOURCE_TYPE);
        try {
            defn.setDsXMLConfiguration(DataSourceUtils.convertToDocument(in).getDocumentElement());
        } catch (DataSourceException e) {
            throw new RSSManagerException(e.getMessage(), e);
        }
        return defn;
    }

    public static DataSourceMetaInfo createDSMetaInfo(Database database,
                                                      String username) throws RSSManagerException {
        DataSourceMetaInfo metaInfo = new DataSourceMetaInfo();
        RDBMSConfiguration rdbmsConfiguration = new RDBMSConfiguration();
        String url = database.getUrl();
        String driverClassName = RSSManagerHelper.getDatabaseDriver(url);
        rdbmsConfiguration.setUrl(url);
        rdbmsConfiguration.setDriverClassName(driverClassName);
        rdbmsConfiguration.setUsername(username);

        metaInfo.setDefinition(createDSXMLDefinition(rdbmsConfiguration));
        metaInfo.setName(database.getName());

        return metaInfo;
    }

    public static String validateRSSInstanceUrl(String url) throws Exception {
        if (url != null && !"".equals(url)) {
            URI uri;
            try {
                uri = new URI(url.split("jdbc:")[1]);
                return RSSManagerConstants.JDBC_PREFIX + ":" + uri.getScheme() + "://" +
                        uri.getHost() + ":" + ((uri.getPort() != -1) ? uri.getPort() : "");
            } catch (URISyntaxException e) {
                throw new Exception("JDBC URL '" + url + "' is invalid. Please enter a " +
                        "valid JDBC URL.");
            }
        }
        return "";
    }

    private static Marshaller createMarshaller() throws RSSManagerException {
        JAXBContext ctx;
        try {
            ctx = JAXBContext.newInstance(RDBMSConfiguration.class);
            return ctx.createMarshaller();
        } catch (JAXBException e) {
            throw new RSSManagerException("Error creating rdbms data source configuration " +
                    "info marshaller: " + e.getMessage(), e);
        }
    }

}
