package org.wso2.carbon.appfactory.jenkins.build.notify;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.wso2.carbon.appfactory.jenkins.build.stub.JenkinsCIBuildStatusReceiverServiceStub;
import org.wso2.carbon.appfactory.jenkins.build.stub.xsd.BuildStatusBean;
import org.wso2.carbon.utils.CarbonUtils;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JenkinsCIBuildStatusReceiverClient {

    private static final Logger log = Logger.getLogger(JenkinsCIBuildStatusReceiverClient.
                                                               class.getName());
    private JenkinsCIBuildStatusReceiverServiceStub clientStub;

    public JenkinsCIBuildStatusReceiverClient(String epr, String username, String password)
            throws AxisFault {
        clientStub = new JenkinsCIBuildStatusReceiverServiceStub(epr);
        ServiceClient client = clientStub._getServiceClient();
        CarbonUtils.setBasicAccessSecurityHeaders(username, password, client);
    }

    /**
     * sends the build results to appfactory side
     * @param buildStatus
     */
    public void onBuildCompletion(BuildStatusBean buildStatus) {
        log.info(buildStatus.getApplicationId() + " build completed for the buildId " +
                 buildStatus.getBuildId());
        try {
            clientStub.onBuildCompletion(buildStatus, null, null);
        } catch (RemoteException e) {
            log.log(Level.SEVERE, "Failed to send build status in failed build for " +
                                  buildStatus.getApplicationId() + ":" + e);
        }
    }
}
