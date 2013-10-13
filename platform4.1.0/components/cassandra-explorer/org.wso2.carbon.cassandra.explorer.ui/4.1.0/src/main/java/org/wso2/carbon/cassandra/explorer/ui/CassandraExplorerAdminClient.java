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
package org.wso2.carbon.cassandra.explorer.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.cassandra.explorer.stub.CassandraExplorerAdminCassandraExplorerException;
import org.wso2.carbon.cassandra.explorer.stub.CassandraExplorerAdminStub;
import org.wso2.carbon.cassandra.explorer.stub.data.xsd.Column;
import org.wso2.carbon.cassandra.explorer.stub.data.xsd.Row;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import java.rmi.RemoteException;

public class CassandraExplorerAdminClient {

    CassandraExplorerAdminStub explorerAdminStub;

    public CassandraExplorerAdminClient(ConfigurationContext ctx, String serverURL, String cookie)
            throws AxisFault {
        init(ctx, serverURL, cookie);
    }

    public CassandraExplorerAdminClient(javax.servlet.ServletContext servletContext,
                                        javax.servlet.http.HttpSession httpSession)
            throws Exception {
        ConfigurationContext ctx =
                (ConfigurationContext) servletContext.getAttribute(
                        CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) httpSession.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String serverURL = CarbonUIUtil.getServerURL(servletContext, httpSession);
        init(ctx, serverURL, cookie);
    }

    private void init(ConfigurationContext ctx,
                      String serverURL,
                      String cookie) throws AxisFault {
        String serviceURL = serverURL + "CassandraExplorerAdmin";
        explorerAdminStub = new CassandraExplorerAdminStub(ctx, serviceURL);
        ServiceClient client = explorerAdminStub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        options.setTimeOutInMilliSeconds(10000);

    }

    public Column[] getPaginateSliceforColumns(String keyspace, String columnFamily, String rowName,
                                               int startingNo, int limit)
            throws RemoteException, CassandraExplorerAdminCassandraExplorerException {
        return explorerAdminStub.getColumnPaginateSlice(keyspace, columnFamily, rowName, startingNo,
                                                        limit);
    }

    public int getNoOfColumns(String keyspace, String columnFamily, String rowName)
            throws RemoteException, CassandraExplorerAdminCassandraExplorerException {
        return explorerAdminStub.getNoOfColumns(keyspace, columnFamily, rowName);
    }

    public Column[] searchColumns(String keyspace, String columnFamily, String rowName,
                                  String searchKey,
                                  int startingNo, int limit)
            throws RemoteException, CassandraExplorerAdminCassandraExplorerException {
        return explorerAdminStub.searchColumns(keyspace, columnFamily, rowName, searchKey,
                                               startingNo, limit);
    }

    public int getNoOfFilteredResultsoforColumns(String keyspace, String columnFamily,
                                                 String rowName,
                                                 String searchKey)
            throws RemoteException, CassandraExplorerAdminCassandraExplorerException {
        return explorerAdminStub.getNoOfColumnSearchResults(keyspace, columnFamily, rowName, searchKey);
    }

    public Row[] getPaginateSliceforRows(String keyspace, String columnFamily,
                                         int startingNo, int limit)
            throws RemoteException, CassandraExplorerAdminCassandraExplorerException {
        return explorerAdminStub.getRowPaginateSlice(keyspace, columnFamily, startingNo,
                                                     limit);
    }


    public Row[] searchRows(String keyspace, String columnFamily, String searchKey,
                            int startingNo, int limit)
            throws RemoteException, CassandraExplorerAdminCassandraExplorerException {
        return explorerAdminStub.searchRows(keyspace, columnFamily, searchKey,
                                            startingNo, limit);
    }

    public int getNoOfFilteredResultsoforRows(String keyspace, String columnFamily,
                                              String searchKey)
            throws RemoteException, CassandraExplorerAdminCassandraExplorerException {
        return explorerAdminStub.getNoOfRowSearchResults(keyspace, columnFamily, searchKey);
    }


    public boolean connectToCassandraCluster(String clusterName, String connectionUrl,
                                             String userName,
                                             String password)
            throws CassandraExplorerAdminCassandraExplorerException, RemoteException {
        return explorerAdminStub.connectToCassandraCluster(clusterName, connectionUrl, userName,
                                                           password);
    }

    public String[] getKeyspaces()
            throws CassandraExplorerAdminCassandraExplorerException, RemoteException {
        return explorerAdminStub.getKeyspaces();
    }

    public String[] getColumnFamilies(String keyspace)
            throws CassandraExplorerAdminCassandraExplorerException, RemoteException {
        return explorerAdminStub.getColumnFamilies(keyspace);
    }

    public int getNoOfRows(String keyspace, String columnFamily)
            throws CassandraExplorerAdminCassandraExplorerException, RemoteException {
        return explorerAdminStub.getNoOfRows(keyspace, columnFamily);
    }

    public void setMaxRowCount(int maxRowCount)
            throws CassandraExplorerAdminCassandraExplorerException, RemoteException {
        explorerAdminStub.setMaxRowCount(maxRowCount);
    }

}
