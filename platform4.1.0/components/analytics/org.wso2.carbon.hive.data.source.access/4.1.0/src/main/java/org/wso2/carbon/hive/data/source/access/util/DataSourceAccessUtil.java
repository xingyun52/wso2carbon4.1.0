package org.wso2.carbon.hive.data.source.access.util;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.HiveContext;
import org.apache.hadoop.hive.metastore.hooks.JDOConnectionURLHook;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.w3c.dom.Element;
import org.wso2.carbon.analytics.hive.multitenancy.HiveRSSMetastoreManager;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.ndatasource.common.DataSourceException;
import org.wso2.carbon.ndatasource.core.DataSourceService;
import org.wso2.carbon.ndatasource.core.utils.DataSourceUtils;
import org.wso2.carbon.ndatasource.rdbms.RDBMSConfiguration;
import org.wso2.carbon.ndatasource.rdbms.RDBMSDataSourceReader;
import org.wso2.carbon.rssmanager.ui.stub.RSSAdminStub;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.HashMap;
import java.util.Map;

public class DataSourceAccessUtil implements JDOConnectionURLHook {

    private static final String HIVE_METASTORE_DB = "metastore_db";

    private static DataSourceService carbonDataSourceService;

    private static RSSAdminStub rssAdminStub;

     private static Log log = LogFactory.getLog(DataSourceAccessUtil.class);
    private static RealmService realmService;

    public static DataSourceService getCarbonDataSourceService() {
        return carbonDataSourceService;
    }

    public static void setCarbonDataSourceService(
            DataSourceService dataSourceService) {
        carbonDataSourceService = dataSourceService;
    }


    public static void setRealmService(RealmService realmSvc) {
        realmService = realmSvc;
    }

    public static RealmService getRealmService() {
        return realmService;
    }

    public static Map<String, String> getDataSourceProperties(String dataSourceName) {

        int tenantId = HiveContext.getCurrentContext().getTenantId();
        //int tenantId = 0;

        Map<String, String> dataSourceProperties = new HashMap<String, String>();
        try {

            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getCurrentContext().setTenantId(tenantId);

            Element element = (Element) carbonDataSourceService.getDataSource(dataSourceName).
                    getDSMInfo().getDefinition().getDsXMLConfiguration();
            RDBMSConfiguration rdbmsConfiguration = RDBMSDataSourceReader.loadConfig(
                    DataSourceUtils.elementToString(element));

            dataSourceProperties = setDataSourceProperties(dataSourceProperties, rdbmsConfiguration);

        } catch (DataSourceException e) {
            e.printStackTrace();
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return dataSourceProperties;
    }

    public static String getMetaStoreConnectionURL(int tenantId) {
        if (isMultiTenantMode()) {
              HiveRSSMetastoreManager rssMetastoreManager =  HiveRSSMetastoreManager.getInstance();
            try {
                rssMetastoreManager.prepareRSSMetaStore(realmService.getTenantManager().getDomain(tenantId), tenantId);
            } catch (UserStoreException ignored) {
                log.error("Error occured while checking the rss database for tenant id: "+tenantId);
            }
            return rssMetastoreManager.getMetaDataStoreConnectionURL(tenantId);
        }
        return null;

    }

    public String getJdoConnectionUrl(Configuration configuration) throws Exception {
        int tenantId = configuration.getInt("hive.current.tenant", -1234);
        if(log.isDebugEnabled())log.debug("%%%%%%%%%%%%%% Tenant id in JDO connection :"+tenantId +"%%%%%%%%%%%%%%");
        String metaUrl =  getMetaStoreConnectionURL(tenantId);
        if(log.isDebugEnabled())log.debug("%%%%%%%%%%%%%% Meta Store URL :"+metaUrl+" %%%%%%%%%%%%%%%%%%%%%%%%%%%");
        return metaUrl;

    }

    public void notifyBadConnectionUrl(String s) {
        // Do nothing
    }

    private static Map<String, String> setDataSourceProperties(
            Map<String, String> dataSourceProperties, RDBMSConfiguration rdbmsConfiguration) {
        setProperties(DBConfiguration.URL_PROPERTY,
                      rdbmsConfiguration.getUrl(), dataSourceProperties);
        setProperties(DBConfiguration.DRIVER_CLASS_PROPERTY,
                      rdbmsConfiguration.getDriverClassName(), dataSourceProperties);
        setProperties(DBConfiguration.USERNAME_PROPERTY,
                      rdbmsConfiguration.getUsername(), dataSourceProperties);
        setProperties(DBConfiguration.PASSWORD_PROPERTY,
                      rdbmsConfiguration.getPassword(), dataSourceProperties);
        return dataSourceProperties;
    }


    private static void setProperties(String propertyKey, Object value,
                                      Map<String, String> dataSourceProperties) {
        if (value != null) {
            if (value instanceof Boolean) {
                dataSourceProperties.put(propertyKey, Boolean.toString((Boolean) value));
            } else if (value instanceof String) {
                dataSourceProperties.put(propertyKey, (String) value);
            } else if (value instanceof Integer) {
                dataSourceProperties.put(propertyKey, Integer.toString((Integer) value));
            } else if (value instanceof Long) {
                dataSourceProperties.put(propertyKey, Long.toString((Long) value));
            }
        }
    }

    private static boolean isMultiTenantMode() {
        RealmService realmService = DataSourceAccessUtil.getRealmService();

        Tenant[] tenants;
        try {
            tenants = realmService.getTenantManager().getAllTenants();
        } catch (UserStoreException e) {
            return false;
        }

        return tenants != null && tenants.length > 0;

    }

}
