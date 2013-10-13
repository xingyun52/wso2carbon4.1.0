/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.carbon.ss.cassandra.test;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.cassandra.mgt.stub.ks.CassandraKeyspaceAdminStub;
import org.wso2.carbon.cassandra.mgt.stub.ks.xsd.ColumnFamilyInformation;
import org.wso2.carbon.cassandra.mgt.stub.ks.xsd.ColumnInformation;
import org.wso2.carbon.cassandra.mgt.stub.ks.xsd.KeyspaceInformation;

import java.rmi.RemoteException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class KeyspaceManagementSuperTenantTestCase {
    private CassandraTestHelper cassandraTestHelper;
    private final String KEYSPACE_NAME="TestKeyspace123";
    private final String COLUMN_FAMILY_NAME="TestColumnFamily123";
    private final String COLUMN_NAME="TestColumn123";
    private final String INDEX_TYPE ="keys";
    private final String INDEX_NAME="test";
    private final double KEY_CACHE_SIZE=0.5;
    private final double ROW_CACHE_SIZE=0.5;
    private final int REPLICATION_FACTOR=1;
    CassandraKeyspaceAdminStub cassandraKeyspaceAdminStub;
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws LoginAuthenticationExceptionException, RemoteException {
        cassandraTestHelper=new CassandraTestHelper();
        cassandraTestHelper.initialize(0);
        String endPoint=cassandraTestHelper.getBackendUrl()+"CassandraKeyspaceAdmin";
        cassandraKeyspaceAdminStub=new CassandraKeyspaceAdminStub(endPoint);
    }

    @Test(description = "Add keyspace by super tenant")
    public void addKeyspaceBySuperTenant()
            throws Exception {
        boolean isKeyspaceContains=false;
        AuthenticateStub.authenticateStub(cassandraTestHelper.getSessionCookie(), cassandraKeyspaceAdminStub);
        KeyspaceInformation keyspaceInformation=new KeyspaceInformation();
        keyspaceInformation.setName(KEYSPACE_NAME);
        keyspaceInformation.setReplicationFactor(REPLICATION_FACTOR);
        keyspaceInformation.setStrategyClass(CassandraUtils.SIMPLE_CLASS);
        cassandraKeyspaceAdminStub.addKeyspace(keyspaceInformation);
        for(String keyspace:cassandraKeyspaceAdminStub.listKeyspacesOfCurrentUser())
        {
            if(KEYSPACE_NAME.equals(keyspace))
            {
                isKeyspaceContains=true;
            }
        }
        assertTrue(isKeyspaceContains);
        keyspaceInformation=cassandraKeyspaceAdminStub.getKeyspaceofCurrentUser(KEYSPACE_NAME);
        assertNotNull(keyspaceInformation);
        assertEquals(keyspaceInformation.getName(),KEYSPACE_NAME);
        assertEquals(keyspaceInformation.getReplicationFactor(),REPLICATION_FACTOR);
        assertEquals(keyspaceInformation.getStrategyClass(),CassandraUtils.SIMPLE_CLASS);
    }

    @Test(dependsOnMethods = "addKeyspaceBySuperTenant",description = "update keyspace by super tenant")
    public void updateKeyspaceBySuperTenant()
            throws Exception {
        boolean isKeyspaceContains=false;
        KeyspaceInformation keyspaceInformation=new KeyspaceInformation();
        keyspaceInformation.setName(KEYSPACE_NAME);
        keyspaceInformation.setReplicationFactor(REPLICATION_FACTOR);
        keyspaceInformation.setStrategyClass(CassandraUtils.OLD_NETWORK_CLASS);
        cassandraKeyspaceAdminStub.updatedKeyspace(keyspaceInformation);
        for(String keyspace:cassandraKeyspaceAdminStub.listKeyspacesOfCurrentUser())
        {
            if(KEYSPACE_NAME.equals(keyspace))
            {
                isKeyspaceContains=true;
            }
        }
        assertTrue(isKeyspaceContains);
        keyspaceInformation=cassandraKeyspaceAdminStub.getKeyspaceofCurrentUser(KEYSPACE_NAME);
        assertNotNull(keyspaceInformation);
        assertEquals(keyspaceInformation.getName(),KEYSPACE_NAME);
        assertEquals(keyspaceInformation.getReplicationFactor(),REPLICATION_FACTOR);
        assertEquals(keyspaceInformation.getStrategyClass(),CassandraUtils.OLD_NETWORK_CLASS);
    }

    @Test(dependsOnMethods = {"addKeyspaceBySuperTenant", "updateKeyspaceBySuperTenant", "updateColumnFamilyBySuperTenant", "addColumnFamilyBySuperTenant", "addColumnBySuperTenant", "updateColumnBySuperTenant", "deleteColumnBySuperTenant", "deleteColumnFamilyBySuperTenant"},description = "delete keyspace by super tenant")
    public void deleteKeyspaceBySuperTenant()
            throws Exception {
        assertTrue(cassandraKeyspaceAdminStub.deleteKeyspace(KEYSPACE_NAME));
    }

    @Test(dependsOnMethods = {"addKeyspaceBySuperTenant", "updateKeyspaceBySuperTenant"},description = "Add column family by super tenant")
    public void addColumnFamilyBySuperTenant()
            throws Exception {
        boolean isCFContains=false;
        ColumnFamilyInformation columnFamilyInformation=new ColumnFamilyInformation();
        columnFamilyInformation.setName(COLUMN_FAMILY_NAME);
        columnFamilyInformation.setKeyspace(KEYSPACE_NAME);
        columnFamilyInformation.setId(2);
        columnFamilyInformation.setGcGraceSeconds(CassandraUtils.DEFAULT_GCGRACE);
        columnFamilyInformation.setMaxCompactionThreshold(CassandraUtils.DEFAULT_MAX_COMPACTION_THRESHOLD);
        columnFamilyInformation.setMinCompactionThreshold(CassandraUtils.DEFAULT_MIN_COMPACTION_THRESHOLD);
        columnFamilyInformation.setRowCacheSavePeriodInSeconds(CassandraUtils.DEFAULT_RAW_CACHE_TIME);
        columnFamilyInformation.setKeyCacheSize(KEY_CACHE_SIZE);
        columnFamilyInformation.setRowCacheSize(ROW_CACHE_SIZE);
        columnFamilyInformation.setType(CassandraUtils.COLUMN_TYPE_STANDARD);
        columnFamilyInformation.setComparatorType(CassandraUtils.BYTESTYPE);
        columnFamilyInformation.setDefaultValidationClass(CassandraUtils.BYTESTYPE);
        columnFamilyInformation.setSubComparatorType(CassandraUtils.ASCIITYPE);
        columnFamilyInformation.setComment("Test column family");
        cassandraKeyspaceAdminStub.addColumnFamily(columnFamilyInformation);
        for(String columnFamily:cassandraKeyspaceAdminStub.listColumnFamiliesOfCurrentUser(KEYSPACE_NAME))
        {
            if(COLUMN_FAMILY_NAME.equals(columnFamily))
            {
                isCFContains=true;
            }
        }
        assertTrue(isCFContains);
        columnFamilyInformation=cassandraKeyspaceAdminStub.getColumnFamilyOfCurrentUser(KEYSPACE_NAME,COLUMN_FAMILY_NAME);
        assertEquals(columnFamilyInformation.getName(),COLUMN_FAMILY_NAME);
        assertEquals(columnFamilyInformation.getKeyspace(),KEYSPACE_NAME);
        //assertEquals(columnFamilyInformation.getId(),2,"CF id mismatch");
        assertEquals(columnFamilyInformation.getGcGraceSeconds(),CassandraUtils.DEFAULT_GCGRACE);
        assertEquals(columnFamilyInformation.getMaxCompactionThreshold(),CassandraUtils.DEFAULT_MAX_COMPACTION_THRESHOLD);
        assertEquals(columnFamilyInformation.getMinCompactionThreshold(),CassandraUtils.DEFAULT_MIN_COMPACTION_THRESHOLD);
        //assertEquals(columnFamilyInformation.getRowCacheSavePeriodInSeconds(),CassandraUtils.DEFAULT_RAW_CACHE_TIME);
        //assertEquals(columnFamilyInformation.getKeyCacheSize(),KEY_CACHE_SIZE);
        //assertEquals(columnFamilyInformation.getRowCacheSize(),ROW_CACHE_SIZE);
        assertEquals(columnFamilyInformation.getType(),CassandraUtils.COLUMN_TYPE_STANDARD);
        //assertEquals(columnFamilyInformation.getComparatorType(),CassandraUtils.BYTESTYPE);
        //assertEquals(columnFamilyInformation.getDefaultValidationClass(),CassandraUtils.BYTESTYPE);
        //assertEquals(columnFamilyInformation.getSubComparatorType(),CassandraUtils.ASCIITYPE);
        //assertEquals(columnFamilyInformation.getComment(),"Test column family");
    }

    @Test(dependsOnMethods = {"addKeyspaceBySuperTenant", "updateKeyspaceBySuperTenant", "addColumnFamilyBySuperTenant"},description = "Update column family by super tenant")
    public void updateColumnFamilyBySuperTenant()
            throws Exception {
        boolean isCFContains=false;
        KeyspaceInformation keyspaceInformation=cassandraKeyspaceAdminStub.getKeyspaceofCurrentUser(KEYSPACE_NAME);
        ColumnFamilyInformation columnFamilyInformation=CassandraClientHelper.getColumnFamilyInformationOfCurrentUser(keyspaceInformation,COLUMN_FAMILY_NAME);
        columnFamilyInformation.setDefaultValidationClass(CassandraUtils.ASCIITYPE);
        cassandraKeyspaceAdminStub.updateColumnFamily(columnFamilyInformation);
        for(String columnFamily:cassandraKeyspaceAdminStub.listColumnFamiliesOfCurrentUser(KEYSPACE_NAME))
        {
            if(COLUMN_FAMILY_NAME.equals(columnFamily))
            {
                isCFContains=true;
            }
        }
        assertTrue(isCFContains);
        columnFamilyInformation=cassandraKeyspaceAdminStub.getColumnFamilyOfCurrentUser(KEYSPACE_NAME,COLUMN_FAMILY_NAME);
        assertEquals(columnFamilyInformation.getName(),COLUMN_FAMILY_NAME);
        assertEquals(columnFamilyInformation.getKeyspace(),KEYSPACE_NAME);
        //assertEquals(columnFamilyInformation.getId(),2);
        assertEquals(columnFamilyInformation.getGcGraceSeconds(),CassandraUtils.DEFAULT_GCGRACE);
        assertEquals(columnFamilyInformation.getMaxCompactionThreshold(),CassandraUtils.DEFAULT_MAX_COMPACTION_THRESHOLD);
        assertEquals(columnFamilyInformation.getMinCompactionThreshold(),CassandraUtils.DEFAULT_MIN_COMPACTION_THRESHOLD);
        //assertEquals(columnFamilyInformation.getRowCacheSavePeriodInSeconds(),CassandraUtils.DEFAULT_RAW_CACHE_TIME);
        //assertEquals(columnFamilyInformation.getKeyCacheSize(),0.75);
        //assertEquals(columnFamilyInformation.getRowCacheSize(),0.75);
        assertEquals(columnFamilyInformation.getType(),CassandraUtils.COLUMN_TYPE_STANDARD);
        //assertEquals(columnFamilyInformation.getComparatorType(),CassandraUtils.BYTESTYPE);
        //assertEquals(columnFamilyInformation.getDefaultValidationClass(),CassandraUtils.ASCIITYPE);
        //assertEquals(columnFamilyInformation.getSubComparatorType(),CassandraUtils.ASCIITYPE);
        //assertEquals(columnFamilyInformation.getComment(),"Test column family");
    }

    @Test(dependsOnMethods = {"addKeyspaceBySuperTenant", "updateKeyspaceBySuperTenant", "updateColumnFamilyBySuperTenant", "addColumnFamilyBySuperTenant", "addColumnBySuperTenant", "updateColumnBySuperTenant", "deleteColumnBySuperTenant"},description = "Add column family by super tenant")
    public void deleteColumnFamilyBySuperTenant()
            throws Exception {
        assertTrue(cassandraKeyspaceAdminStub.deleteColumnFamily(KEYSPACE_NAME,COLUMN_FAMILY_NAME));
    }

    @Test(dependsOnMethods = {"addKeyspaceBySuperTenant", "updateKeyspaceBySuperTenant", "updateColumnFamilyBySuperTenant", "addColumnFamilyBySuperTenant"},description = "Add column family by super tenant")
    public void addColumnBySuperTenant()
            throws Exception {
        KeyspaceInformation keyspaceInformation =cassandraKeyspaceAdminStub.getKeyspaceofCurrentUser(KEYSPACE_NAME);
        ColumnFamilyInformation columnFamilyInformation = CassandraClientHelper.getColumnFamilyInformationOfCurrentUser(keyspaceInformation, COLUMN_FAMILY_NAME);
        ColumnInformation columnInformation=new ColumnInformation();
        columnInformation.setName(COLUMN_NAME);
        columnInformation.setIndexName(INDEX_NAME);
        columnInformation.setIndexType(INDEX_TYPE);
        columnInformation.setValidationClass(CassandraUtils.BYTESTYPE);
        columnFamilyInformation.addColumns(columnInformation);
        cassandraKeyspaceAdminStub.updateColumnFamily(columnFamilyInformation);
        columnFamilyInformation=cassandraKeyspaceAdminStub.getColumnFamilyOfCurrentUser(KEYSPACE_NAME,COLUMN_FAMILY_NAME);
        columnInformation=CassandraClientHelper.getColumnInformation(columnFamilyInformation, COLUMN_NAME);
        assertNotNull(columnInformation);
        assertEquals(columnInformation.getName(), COLUMN_NAME);
        assertEquals(columnInformation.getIndexName(), INDEX_NAME);
        //assertEquals(columnInformation.getIndexType(),INDEX_TYPE);
        //assertEquals(columnInformation.getValidationClass(),CassandraUtils.BYTESTYPE);
    }

    @Test(dependsOnMethods = {"addKeyspaceBySuperTenant", "updateKeyspaceBySuperTenant", "updateColumnFamilyBySuperTenant", "addColumnFamilyBySuperTenant", "addColumnBySuperTenant", "updateColumnBySuperTenant"},description = "Add column family by super tenant")
    public void deleteColumnBySuperTenant()
            throws Exception {
        KeyspaceInformation keyspaceInformation =cassandraKeyspaceAdminStub.getKeyspaceofCurrentUser(KEYSPACE_NAME);
        if (keyspaceInformation != null) {
            ColumnFamilyInformation columnFamilyInformation =
                    CassandraClientHelper.getColumnFamilyInformationOfCurrentUser(keyspaceInformation, COLUMN_FAMILY_NAME);
            CassandraClientHelper.removeColumnInformation(columnFamilyInformation, COLUMN_NAME);
            cassandraKeyspaceAdminStub.updateColumnFamily(columnFamilyInformation);
            columnFamilyInformation=cassandraKeyspaceAdminStub.getColumnFamilyOfCurrentUser(KEYSPACE_NAME,COLUMN_FAMILY_NAME);
            ColumnInformation columnInformation=CassandraClientHelper.getColumnInformation(columnFamilyInformation,COLUMN_NAME);
            assertNull(columnInformation);
        }
    }

    @Test(dependsOnMethods = {"addKeyspaceBySuperTenant", "updateKeyspaceBySuperTenant", "updateColumnFamilyBySuperTenant", "addColumnFamilyBySuperTenant", "addColumnBySuperTenant"},description = "Add column family by super tenant")
    public void updateColumnBySuperTenant()
            throws Exception {
        KeyspaceInformation keyspaceInformation =cassandraKeyspaceAdminStub.getKeyspaceofCurrentUser(KEYSPACE_NAME);
        ColumnFamilyInformation columnFamilyInformation = CassandraClientHelper.getColumnFamilyInformationOfCurrentUser(keyspaceInformation, COLUMN_FAMILY_NAME);
        ColumnInformation columnInformation=CassandraClientHelper.getColumnInformation(columnFamilyInformation,COLUMN_NAME);
        cassandraKeyspaceAdminStub.updateColumnFamily(columnFamilyInformation);
        columnFamilyInformation=cassandraKeyspaceAdminStub.getColumnFamilyOfCurrentUser(KEYSPACE_NAME,COLUMN_FAMILY_NAME);
        columnInformation=CassandraClientHelper.getColumnInformation(columnFamilyInformation,COLUMN_NAME);
        assertNotNull(columnInformation);
        assertEquals(columnInformation.getName(), COLUMN_NAME);
        assertEquals(columnInformation.getIndexName(),INDEX_NAME);
        //assertEquals(columnInformation.getIndexType(),INDEX_TYPE);
        //assertEquals(columnInformation.getValidationClass(),CassandraUtils.ASCIITYPE);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws Exception
    {
        for(String keyspace:cassandraKeyspaceAdminStub.listKeyspacesOfCurrentUser())
        {
            if(KEYSPACE_NAME.equals(keyspace))
            {
                cassandraKeyspaceAdminStub.deleteKeyspace(KEYSPACE_NAME);
            }
        }
    }
}
