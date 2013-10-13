package org.wso2.carbon.automation.api.clients.cep;


import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.brokermanager.stub.BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException;
import org.wso2.carbon.brokermanager.stub.BrokerManagerAdminServiceStub;
import org.wso2.carbon.brokermanager.stub.types.BrokerConfigurationDetails;
import org.wso2.carbon.brokermanager.stub.types.BrokerProperty;

import java.rmi.RemoteException;

public class BrokerManagerAdminServiceClient {
    private static final Log log = LogFactory.getLog(BrokerManagerAdminServiceClient.class);
    private final String serviceName = "BrokerManagerAdminService";
    private BrokerManagerAdminServiceStub brokerManagerAdminServiceStub;

    private String endPoint;

    public BrokerManagerAdminServiceClient(String backEndUrl, String sessionCookie)
            throws AxisFault {
        this.endPoint = backEndUrl + serviceName;
        brokerManagerAdminServiceStub = new BrokerManagerAdminServiceStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, brokerManagerAdminServiceStub);

    }

    public BrokerManagerAdminServiceClient(String backEndUrl, String userName, String password)
            throws AxisFault {
        this.endPoint = backEndUrl + serviceName;
        brokerManagerAdminServiceStub = new BrokerManagerAdminServiceStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, brokerManagerAdminServiceStub);

    }

    public ServiceClient _getServiceClient() {
        return brokerManagerAdminServiceStub._getServiceClient();
    }

    public String[] getBrokerNames() throws RemoteException,
                                            BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException {
        try {
            return brokerManagerAdminServiceStub.getBrokerNames();
        } catch (RemoteException e) {
            log.error("RemoteException", e);
            throw new RemoteException("RemoteException", e);

        } catch (BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException e) {

            log.error("BrokerManagerAdminServiceBrokerManagerAdminServiceException", e);
            throw new BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException("BrokerManagerAdminServiceBrokerManagerAdminServiceException", e);
        }
    }


    public BrokerProperty[] getBrokerProperties(String brokerName) throws RemoteException,
                                                                          BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException {
        try {
            return brokerManagerAdminServiceStub.getBrokerProperties(brokerName);
        } catch (RemoteException e) {
            log.error("RemoteException", e);
            throw new RemoteException();
        } catch (BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException e) {
            log.error("BrokerManagerAdminServiceBrokerAdminServiceException", e);
            throw new BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException();
        }
    }

    public BrokerConfigurationDetails[] getAllBrokerConfigurationNamesAndTypes()
            throws RemoteException,
                   BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException {
        try {
            return brokerManagerAdminServiceStub.getAllBrokerConfigurationNamesAndTypes();
        } catch (RemoteException e) {
            throw new RemoteException("RemoteException", e);
        } catch (BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException e) {
            throw new BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException("BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException", e);
        }
    }

    public void addBrokerConfiguration(String brokerName, String brokerType,
                                       BrokerProperty[] brokerProperty) throws RemoteException,
                                                                               BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException {
        try {
            brokerManagerAdminServiceStub.addBrokerConfiguration(brokerName, brokerType, brokerProperty);
        } catch (RemoteException e) {
            log.error("RemoteException", e);
            throw new RemoteException();
        } catch (BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException e) {
            log.error("BrokerManagerAdminServiceBrokerAdminServiceException", e);
            throw new BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException();
        }
    }

    public void removeBrokerConfiguration(String brokerName) throws RemoteException,
                                                                    BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException {
        try {
            brokerManagerAdminServiceStub.removeBrokerConfiguration(brokerName);
        } catch (RemoteException e) {
            log.error("RemoteException", e);
            throw new RemoteException();
        } catch (BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException e) {
            log.error("BrokerManagerAdminServiceBrokerAdminServiceException", e);
            throw new BrokerManagerAdminServiceBrokerManagerAdminServiceExceptionException();
        }
    }
}
