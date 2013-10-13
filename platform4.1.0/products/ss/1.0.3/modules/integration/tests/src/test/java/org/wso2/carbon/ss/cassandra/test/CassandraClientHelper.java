/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.ss.cassandra.test;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cassandra.mgt.stub.ks.xsd.ColumnFamilyInformation;
import org.wso2.carbon.cassandra.mgt.stub.ks.xsd.ColumnInformation;
import org.wso2.carbon.cassandra.mgt.stub.ks.xsd.KeyspaceInformation;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains helper methods required for CassandraAdmin
 */
public class CassandraClientHelper {
    private static final Log log = LogFactory.getLog(CassandraClientHelper.class);

    private static final String CASSANDRA_ENDPOINT_CONF = "repository" + File.separator + "conf"
                                                          + File.separator + "etc" + File.separator
                                                          + "cassandra-endpoint.xml";

    /**
     * Gets the alias for the class name of the RF
     *
     * @param className the class name of the replication strategy
     * @return alias
     */
    public static String getAliasForReplicationStrategyClass(String className) {
        if (className == null || "".equals(className)) {
            return CassandraUtils.SIMPLE;
        }
        className = className.trim();
        if (CassandraUtils.OLD_NETWORK_CLASS.equals(className)) {
            return CassandraUtils.OLD_NETWORK;
        } else if (CassandraUtils.NETWORK_CLASS.equals(className)) {
            return CassandraUtils.NETWORK;
        } else {
            return CassandraUtils.SIMPLE;
        }
    }

    /**
     * Gets the class name of the RF for the alias
     *
     * @param alias the alias of the replication strategy
     * @return class name
     */
    public static String getReplicationStrategyClassForAlias(String alias) {
        if (alias == null || "".equals(alias)) {
            return CassandraUtils.SIMPLE_CLASS;
        }
        alias = alias.trim();
        if (CassandraUtils.OLD_NETWORK.equals(alias)) {
            return CassandraUtils.OLD_NETWORK_CLASS;
        } else if (CassandraUtils.NETWORK.equals(alias)) {
            return CassandraUtils.NETWORK_CLASS;
        } else {
            return CassandraUtils.SIMPLE_CLASS;
        }
    }

    /**
     * Gets the alias for the class name of the ComparatorType
     *
     * @param className the class name of the ComparatorType
     * @return alias
     */
    public static String getAliasForComparatorTypeClass(String className) {
        if (className == null || "".equals(className)) {
            return CassandraUtils.BYTESTYPE_CLASS;
        }
        className = className.trim();
        if (CassandraUtils.ASCIITYPE_CLASS.equals(className)) {
            return CassandraUtils.ASCIITYPE;
        } else if (CassandraUtils.UTF8TYPE_CLASS.equals(className)) {
            return CassandraUtils.UTF8TYPE;
        } else if (CassandraUtils.LEXICALUUIDTYPE_CLASS.equals(className)) {
            return CassandraUtils.LEXICALUUIDTYPE;
        } else if (CassandraUtils.TIMEUUIDTYPE_CLASS.equals(className)) {
            return CassandraUtils.TIMEUUIDTYPE;
        } else if (CassandraUtils.LONGTYPE_CLASS.equals(className)) {
            return CassandraUtils.LONGTYPE;
        } else if (CassandraUtils.INTEGERTYPE_CLASS.equals(className)) {
            return CassandraUtils.INTEGERTYPE;
        } else {
            return CassandraUtils.BYTESTYPE;
        }
    }

    /**
     * Gets the class name of the ComparatorType for the alias
     *
     * @param alias the alias of the Comparator Type
     * @return class name
     */
    public static String getComparatorTypeClassForAlias(String alias) {
        if (alias == null || "".equals(alias)) {
            return CassandraUtils.BYTESTYPE;
        }
        alias = alias.trim();
        if (CassandraUtils.ASCIITYPE.equals(alias)) {
            return CassandraUtils.ASCIITYPE_CLASS;
        } else if (CassandraUtils.UTF8TYPE.equals(alias)) {
            return CassandraUtils.UTF8TYPE_CLASS;
        } else if (CassandraUtils.LEXICALUUIDTYPE.equals(alias)) {
            return CassandraUtils.LEXICALUUIDTYPE_CLASS;
        } else if (CassandraUtils.TIMEUUIDTYPE.equals(alias)) {
            return CassandraUtils.TIMEUUIDTYPE_CLASS;
        } else if (CassandraUtils.LONGTYPE.equals(alias)) {
            return CassandraUtils.LONGTYPE_CLASS;
        } else if (CassandraUtils.INTEGERTYPE.equals(alias)) {
            return CassandraUtils.INTEGERTYPE_CLASS;
        } else {
            return CassandraUtils.BYTESTYPE;
        }
    }

    /**
     * Fill the <code>ColumnFamilyInformation</code> with the default values to be used in UI
     *
     * @param information meta-data about a CF
     */
    public static void fillDefaultValuesForCF(ColumnFamilyInformation information) {

        String name = information.getName();
        if (name == null) {
            information.setName("");
        } else {
            information.setName(name.trim());
        }

        String type = information.getType();
        if (type == null || "".equals(type.trim())) {
            information.setType(CassandraUtils.COLUMN_TYPE_STANDARD);
        }

        String comparatorType = information.getComparatorType();
        if (comparatorType == null || "".equals(comparatorType.trim())) {
            information.setComparatorType(CassandraUtils.BYTESTYPE_CLASS);
        }

        if (!CassandraUtils.COLUMN_TYPE_SUPER.equalsIgnoreCase(type)) {
            information.setSubComparatorType("");
        } else {
            String subComparatorType = information.getSubComparatorType();
            if (subComparatorType == null || "".equals(subComparatorType.trim())) {
                information.setSubComparatorType(CassandraUtils.BYTESTYPE_CLASS);
            }
        }

        String comment = information.getComment();
        if (comment == null) {
            information.setComment("");
        } else {
            information.setComment(comment.trim());
        }

        double keyCacheSize = information.getKeyCacheSize();
        if (keyCacheSize < 0) {
            information.setKeyCacheSize(0);
        }

        double rawCacheSize = information.getRowCacheSize();
        if (rawCacheSize < 0) {
            information.setRowCacheSize(0);
        }

        int gcGrace = information.getGcGraceSeconds();
        if (gcGrace <= 0) {
            information.setGcGraceSeconds(CassandraUtils.DEFAULT_GCGRACE);
        }

        int minCompaction = information.getMinCompactionThreshold();
        if (minCompaction <= 0) {
            information.setMinCompactionThreshold(CassandraUtils.DEFAULT_MIN_COMPACTION_THRESHOLD);
        }

        int maxCompaction = information.getMaxCompactionThreshold();
        if (maxCompaction <= 0) {
            information.setMaxCompactionThreshold(CassandraUtils.DEFAULT_MAX_COMPACTION_THRESHOLD);
        }

        int rowCacheSavePeriod = information.getRowCacheSavePeriodInSeconds();
        if (rowCacheSavePeriod <= 0) {
            information.setRowCacheSavePeriodInSeconds(CassandraUtils.DEFAULT_RAW_CACHE_TIME);
        }

        String validationClass = information.getDefaultValidationClass();
        if (validationClass == null || "".equals(validationClass.trim())) {
            information.setDefaultValidationClass(CassandraUtils.BYTESTYPE_CLASS);
        }
    }

    /**
     * Fill the <code>ColumnInformation</code> with the default values to be used in UI
     *
     * @param information meta-data about a CF
     */
    public static void fillDefaultValuesForCL(ColumnInformation information) {

        String indexType = information.getIndexType();
        if (indexType == null) {
            information.setIndexType("keys");
        }

        String validationClass = information.getValidationClass();
        if (validationClass == null || "".equals(validationClass.trim())) {
            information.setValidationClass(CassandraUtils.BYTESTYPE_CLASS);
        }

        String indexName = information.getIndexName();
        if (indexName == null) {
            information.setIndexName("");
        }
    }

    public static ColumnFamilyInformation getColumnFamilyInformationOfCurrentUser(
            KeyspaceInformation keyspaceInformation,
            String cfName) {
        ColumnFamilyInformation[] columnFamilies = keyspaceInformation.getColumnFamilies();
        if (columnFamilies != null && columnFamilies.length > 0) {
            for (ColumnFamilyInformation cf : columnFamilies) {
                if (cf != null && cfName.equals(cf.getName())) {
                    return cf;
                }
            }
        }
        return null;
    }

    public static void removeColumnFamilyInformation(KeyspaceInformation keyspaceInformation,
                                                     String cfName) {
        ColumnFamilyInformation[] columnFamilies = keyspaceInformation.getColumnFamilies();
        List<ColumnFamilyInformation> newCFS = new ArrayList<ColumnFamilyInformation>();
        for (ColumnFamilyInformation cf : columnFamilies) {
            if (cf != null && cfName.equals(cf.getName())) {
                continue;
            }
            newCFS.add(cf);
        }
        keyspaceInformation.setColumnFamilies(newCFS.toArray(new ColumnFamilyInformation[newCFS.size()]));
    }

    public static void removeColumnInformation(ColumnFamilyInformation columnFamilyInformation,
                                               String clName) {
        ColumnInformation[] columns = columnFamilyInformation.getColumns();
        List<ColumnInformation> newCLS = new ArrayList<ColumnInformation>();
        for (ColumnInformation cl : columns) {
            if (cl != null && clName.equals(cl.getName())) {
                continue;
            }
            newCLS.add(cl);
        }
        columnFamilyInformation.setColumns(newCLS.toArray(new ColumnInformation[newCLS.size()]));
    }

    public static ColumnInformation getColumnInformation(ColumnFamilyInformation familyInformation,
                                                         String clName) {
        ColumnInformation[] columnInformations = familyInformation.getColumns();
        if (columnInformations != null && columnInformations.length > 0) {
            for (ColumnInformation column : columnInformations) {
                if (column != null && clName.equals(column.getName())) {
                    return column;
                }
            }
        }
        return null;
    }

    public static void addColumnInformation(ColumnFamilyInformation columnFamilyInformation,
                                            ColumnInformation newCL) {
        ColumnInformation[] columnInformations = columnFamilyInformation.getColumns();
        if (columnInformations != null && columnInformations.length > 0) {
            List<ColumnInformation> newCls = new ArrayList<ColumnInformation>();
            for (ColumnInformation cl : columnInformations) {
                newCls.add(cl);
            }
            newCls.add(newCL);
            columnFamilyInformation.setColumns(newCls.toArray(new ColumnInformation[newCls.size()]));
        } else {
            columnFamilyInformation.setColumns(new ColumnInformation[]{newCL});
        }
    }

    public static void addColumnFamilyInformation(KeyspaceInformation keyspaceInformation,
                                                  ColumnFamilyInformation newCF) {
        ColumnFamilyInformation[] columnFamilies = keyspaceInformation.getColumnFamilies();
        if (columnFamilies != null && columnFamilies.length > 0) {
            List<ColumnFamilyInformation> newCfs = new ArrayList<ColumnFamilyInformation>();
            for (ColumnFamilyInformation cf : columnFamilies) {
                newCfs.add(cf);
            }
            newCfs.add(newCF);
            keyspaceInformation.setColumnFamilies(newCfs.toArray(new ColumnFamilyInformation[newCfs.size()]));
        } else {
            keyspaceInformation.setColumnFamilies(new ColumnFamilyInformation[]{newCF});
        }
    }

    /**
     * Read cassandra-endpoint.xml
     *
     * @return
     */
    public static OMElement readCassandraEndpoints() {
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String path = carbonHome + File.separator + CASSANDRA_ENDPOINT_CONF;
        BufferedInputStream inputStream = null;
        try {
            File file = new File(path);
            if (!file.exists()) {
                log.info("There is no " + CASSANDRA_ENDPOINT_CONF + ". Using the default configuration");
                inputStream = new BufferedInputStream(
                        new ByteArrayInputStream("<Cassandra/>".getBytes()));
            } else {
                inputStream = new BufferedInputStream(new FileInputStream(file));
            }
            XMLStreamReader parser = XMLInputFactory.newInstance().
                    createXMLStreamReader(inputStream);
            StAXOMBuilder builder = new StAXOMBuilder(parser);
            return builder.getDocumentElement();
        } catch (FileNotFoundException e) {
            log.error(CASSANDRA_ENDPOINT_CONF + "cannot be found in the path : " + path, e);
        } catch (XMLStreamException e) {
            log.error("Invalid XML for " + CASSANDRA_ENDPOINT_CONF + " located in " +
                      "the path : " + path, e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ingored) {
            }
        }
        return null;
    }

    /**
     * Return the endpoint list of the $CARBON_HOME/repository/conf/advanced/cassandra-endpoint.xml
     *
     * @return
     */
    public static List<String> getCassandraEndPointList() {
        List endPoints = new ArrayList();
        OMElement cassandraEndPointConfig = readCassandraEndpoints();
        Iterator omElementIterator = cassandraEndPointConfig.getChildrenWithName(new QName("EndPoints"));
        while (omElementIterator.hasNext()) {
            OMElement endPoint = (OMElement) omElementIterator.next();
            Iterator hostName = endPoint.getChildrenWithName(new QName("EndPoint"));
            while (hostName.hasNext()) {
                OMElement cssNode = (OMElement) hostName.next();
                OMElement cssNodeName = cssNode.getFirstChildWithName(new QName("HostName"));
                endPoints.add(cssNodeName.getText());
            }
        }
        return endPoints;
    }
}
