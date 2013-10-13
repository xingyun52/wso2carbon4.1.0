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
package org.wso2.carbon.rssmanager.core.internal.util;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.rssmanager.common.RSSManagerConstants;
import org.wso2.carbon.rssmanager.core.RSSManagerException;
import org.wso2.carbon.rssmanager.core.entity.RSSInstance;
import org.wso2.carbon.rssmanager.core.internal.manager.RSSManager;
import org.wso2.carbon.rssmanager.core.internal.manager.RSSManagerFactory;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.sql.DataSource;
import javax.xml.namespace.QName;
import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Represents a WSO2 RSS configuration.
 */
public class RSSConfig {
    
    private DataSource dataSource;

    private String rssType;

    private RSSManager rssManager;

    private static RSSConfig currentRSSConfig;

    private List<RSSInstance> systemRSSInstances;

    private static final Log log = LogFactory.getLog(RSSConfig.class);

    /**
     * Retrieves the RSS config reading the rss-instance configuration file.
     *
     * @return RSSConfig
     * @throws RSSManagerException Is thrown if the RSS configuration is not initialized properly
     */
    public static synchronized RSSConfig getInstance() throws RSSManagerException {
        if (currentRSSConfig == null) {
            throw new RSSManagerException("RSS configuration is not initialized and is null");
        }
        return currentRSSConfig;
    }

    public static void init() throws RSSManagerException {
        String rssConfigXMLPath = CarbonUtils.getCarbonConfigDirPath()
                + File.separator + "etc" + File.separator + RSSManagerConstants.RSS_CONFIG_XML_NAME;
        try {
            currentRSSConfig = new RSSConfig(AXIOMUtil.stringToOM(
                    new String(CarbonUtils.getBytesFromFile(new File(rssConfigXMLPath)))));
        } catch (Exception e) {
            throw new RSSManagerException("Error occurred while initializing RSS config", e);
        }
    }

    @SuppressWarnings("unchecked")
    private RSSConfig(OMElement configEl) throws RSSManagerException {
        /* Initializing the RSS manager type being used */
        this.initRSSManager(configEl);
        /* Initializing RSS manager metadata repository */
        this.intiRSSManagerMetaDataRepository(configEl);

        this.systemRSSInstances = new ArrayList<RSSInstance>();
        OMElement systemRSSInstancesEl =
                (OMElement) configEl.getChildrenWithLocalName("system-rss-instances").next();
        if (systemRSSInstancesEl != null) {
            Iterator<OMElement> instances =
                    systemRSSInstancesEl.getChildrenWithLocalName("system-rss-instance");
            while (instances.hasNext()) {
                RSSInstance rssInstance = this.createRSSInstanceFromXMLConfig(instances.next());
                this.getSystemRSSInstances().add(rssInstance);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void initRSSManager(OMElement configEl) throws RSSManagerException {
        Iterator<OMElement> tmpItr = configEl.getChildrenWithLocalName("rss-type");
        if (!tmpItr.hasNext()) {
            throw new RSSManagerException("RSS type is missing");
        }
        OMElement rssTypeEl = tmpItr.next();
        this.rssType = rssTypeEl.getText().trim();
        this.rssManager = RSSManagerFactory.getRSSManager(this.getRssType());
    }

    @SuppressWarnings("unchecked")
    private void intiRSSManagerMetaDataRepository(OMElement configEl) throws RSSManagerException {
        Iterator<OMElement> tmpItr = configEl.getChildrenWithLocalName("rss-mgt-repository");
        if (!tmpItr.hasNext()) {
            throw new RSSManagerException("RSS management repository configuration is missing");
        }

        OMElement rssMgtRepositoryConfigEl = tmpItr.next();
        tmpItr = rssMgtRepositoryConfigEl.getChildrenWithLocalName("datasource-config");
        if (!tmpItr.hasNext()) {
            throw new RSSManagerException("RSS management repository datasource configuration " +
                    "is missing");
        }
        OMElement dsEl = tmpItr.next();
        this.dataSource = RSSManagerUtil.createDataSource(dsEl);
    }

    @SuppressWarnings("unchecked")
    private RSSInstance createRSSInstanceFromXMLConfig(
            OMElement rssInstEl) throws RSSManagerException {
        Iterator<OMElement> tmpItr = rssInstEl.getChildrenWithLocalName("name");
        if (!tmpItr.hasNext()) {
            throw new RSSManagerException("Server instance name is missing in RSS database " +
                    "definition");
        }
        OMElement tmpEl = tmpItr.next();
        String name = tmpEl.getText().trim();

        tmpItr = rssInstEl.getChildrenWithLocalName("dbms-type");
        if (!tmpItr.hasNext()) {
            throw new RSSManagerException("Server instance DBMS type is missing in RSS database " +
                    "definition");
        }
        tmpEl = tmpItr.next();
        String dbmsType = tmpEl.getText().trim();

        tmpItr = rssInstEl.getChildrenWithLocalName("server-category");
        if (!tmpItr.hasNext()) {
            throw new RSSManagerException("Server category is missing in RSS database definition");
        }
        tmpEl = tmpItr.next();
        String serverCategory = tmpEl.getText().trim();

        tmpItr = rssInstEl.getChildrenWithLocalName("admin-datasource-config");
        if (!tmpItr.hasNext()) {
            throw new RSSManagerException("Administrative datasource configuration of the RSS " +
                    "instance is missing");
        }
        OMElement adminDSConfigEl = tmpItr.next();
        tmpItr = adminDSConfigEl.getChildrenWithLocalName("dataSourceClassName");
        if (!tmpItr.hasNext()) {
            throw new RSSManagerException("Administrative datasource class name is missing in " +
                    "RSS datasource definition");
        }
        OMElement dsClassNameEl = tmpItr.next();
        String dsClassName = dsClassNameEl.getText();

        tmpItr = adminDSConfigEl.getChildrenWithLocalName("dataSourceProps");
        if (!tmpItr.hasNext()) {
            throw new RSSManagerException("Administrative datasource properties are not " +
                    "configured properly");
        }
        OMElement dsPropsEl = tmpItr.next();
        tmpItr = dsPropsEl.getChildrenWithLocalName("property");
        if (!tmpItr.hasNext()) {
           throw new RSSManagerException("No datasource properties found");
        }
        Properties xaProps = new Properties();
        while (tmpItr.hasNext()) {
            OMElement propEl = tmpItr.next();
            OMAttribute nameAttr = propEl.getAttribute(new QName("name"));
            xaProps.setProperty(nameAttr.getAttributeValue(), propEl.getText());
        }

        DataSource dataSource = RSSManagerUtil.createDataSource(xaProps, dsClassName);

        return this.createRSSInstance(name, dbmsType, serverCategory,
                MultitenantConstants.SUPER_TENANT_ID, RSSManagerConstants.WSO2_RSS_INSTANCE_TYPE,
                xaProps, dataSource);
    }

    private RSSInstance createRSSInstance(String name, String dbmsType, String serverCategory,
                                         int tenantId, String instanceType, Properties xaProps,
                                         DataSource dataSource) {
        String serverUrl = xaProps.getProperty(RSSManagerConstants.RSS_DS_PROPERTIES.URL);
        String adminUsername = xaProps.getProperty(RSSManagerConstants.RSS_DS_PROPERTIES.USER);
        String adminPassword = xaProps.getProperty(RSSManagerConstants.RSS_DS_PROPERTIES.PASSWORD);

        return new RSSInstance(-1, name, serverUrl, dbmsType, instanceType, serverCategory, 
                adminUsername, adminPassword, tenantId, dataSource);
    }

    public Connection getRSSDBConnection() throws RSSManagerException {
        if (this.getDataSource() == null) {
            throw new RSSManagerException("RSS manager repository datasource is not initialized");
        }
        return this.getRssManager().createConnection(this.getDataSource());
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getRssType() {
        return rssType;
    }

    public RSSManager getRssManager() {
        return rssManager;
    }

    public List<RSSInstance> getSystemRSSInstances() {
        return systemRSSInstances;
    }

}
