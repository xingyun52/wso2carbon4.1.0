package org.wso2.carbon.automation.api.clients.cep;


import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.cep.stub.admin.CEPAdminServiceCEPAdminException;
import org.wso2.carbon.cep.stub.admin.CEPAdminServiceCEPConfigurationException;
import org.wso2.carbon.cep.stub.admin.CEPAdminServiceStub;
import org.wso2.carbon.cep.stub.admin.internal.xsd.BucketDTO;

import java.rmi.RemoteException;

public class CEPAdminServiceClient {
    private final String serviceName = "CEPAdminService";
    private static final Log log = LogFactory.getLog(CEPAdminServiceClient.class);
    private CEPAdminServiceStub cepAdminServiceStub;

    private String endPoint;

    public CEPAdminServiceClient(String backEndUrl, String sessionCookie) throws AxisFault {
        this.endPoint = backEndUrl + serviceName;
        cepAdminServiceStub = new CEPAdminServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, cepAdminServiceStub);

    }

    public CEPAdminServiceClient(String backEndUrl, String userName, String password)
            throws AxisFault {
        this.endPoint = backEndUrl + serviceName;
        cepAdminServiceStub = new CEPAdminServiceStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, cepAdminServiceStub);

    }

    public ServiceClient _getServiceClient() {
        return cepAdminServiceStub._getServiceClient();
    }

    public int getAllBucketCount()
            throws RemoteException, CEPAdminServiceCEPConfigurationException {
        try {
            return cepAdminServiceStub.getAllBucketCount();
        } catch (RemoteException e) {
            log.error("RemoteException", e);
            throw new RemoteException();
        } catch (CEPAdminServiceCEPConfigurationException e) {
            log.error("CEPAdminServiceConfigurationException", e);
            throw new CEPAdminServiceCEPConfigurationException();
        }

    }

    public void addBucket(BucketDTO bucketDTO)
            throws RemoteException, CEPAdminServiceCEPConfigurationException,
                   CEPAdminServiceCEPAdminException {
        try {
            cepAdminServiceStub.addBucket(bucketDTO);

        } catch (RemoteException e) {
            log.error("RemoteException", e);
            throw new RemoteException();
        } catch (CEPAdminServiceCEPAdminException e) {
            throw new CEPAdminServiceCEPAdminException("CEPAdminServiceCEPAdminException", e);
        }
    }

    public boolean removeBucket(String bucketName)
            throws RemoteException, CEPAdminServiceCEPAdminException {

        try {
            return cepAdminServiceStub.removeBucket(bucketName);
        } catch (RemoteException e) {
            log.error("RemoteException", e);
            throw new RemoteException();
        } catch (CEPAdminServiceCEPAdminException e) {
            log.error("CEPAdminServiceCEPAdminException", e);
            throw new CEPAdminServiceCEPAdminException();
        }


    }

    public BucketDTO getBucket(String bucketName)
            throws RemoteException, CEPAdminServiceCEPAdminException {

        try {
            return cepAdminServiceStub.getBucket(bucketName);
        } catch (RemoteException e) {
            log.error("RemoteException", e);
            throw new RemoteException();
        } catch (CEPAdminServiceCEPAdminException e) {
            log.error("CEPAdminServiceCEPAdminException", e);
            throw new CEPAdminServiceCEPAdminException();
        }


    }
}
