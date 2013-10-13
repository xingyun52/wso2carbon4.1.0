package org.wso2.carbon.ss.cassandra.test;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentVariables;
import org.wso2.carbon.cassandra.mgt.stub.cluster.CassandraClusterAdminStub;
import org.wso2.carbon.cassandra.mgt.stub.cluster.xsd.NodeInformation;

import java.rmi.RemoteException;

import static org.testng.Assert.assertTrue;

public class ClusterMgtTestCase {
    private EnvironmentVariables ssServer;
    private UserInfo userInfo;
    private EnvironmentBuilder builder;
    private final String keyspaceName = "TestKeyspace";
    private String backendUrl;
    private String sessionCookie;
    private String serviceeUrl;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws LoginAuthenticationExceptionException, RemoteException {
        userInfo = UserListCsvReader.getUserInfo(0);
        builder = new EnvironmentBuilder().ss(0);
        ssServer = builder.build().getSs();
        backendUrl = ssServer.getBackEndUrl();
        sessionCookie = ssServer.getSessionCookie();
        serviceeUrl = ssServer.getServiceUrl();
    }

    @Test
    public void testCreateCluster()
            throws Exception {
        String endPoint = backendUrl + "CassandraClusterAdmin";
        CassandraClusterAdminStub cassandraClusterAdminStub = new CassandraClusterAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, cassandraClusterAdminStub);

        NodeInformation[] nodeInformations = cassandraClusterAdminStub.getNodes();
        for (NodeInformation nodeInformation : nodeInformations) {
            String token = nodeInformation.getToken();
            if (token != null) {
                assertTrue(token.length() > 0);
            }
        }
    }
}
