package org.wso2.carbon.automation.api.clients.cep;


import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.cep.statistics.stub.CEPStatisticsAdminStub;
import org.wso2.carbon.cep.statistics.stub.types.carbon.CountDTO;

import java.rmi.RemoteException;

public class CEPStatisticsAdminServiceClient {
    private final String serviceName = "CEPStatisticsAdmin";
    private static final Log log = LogFactory.getLog(CEPStatisticsAdminServiceClient.class);
    private CEPStatisticsAdminStub cepStatisticsAdminStub;

    private String endPoint;

    public CEPStatisticsAdminServiceClient(String backEndUrl, String sessionCookie) throws
                                                                                    AxisFault {
        this.endPoint = backEndUrl + serviceName;
        cepStatisticsAdminStub = new CEPStatisticsAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, cepStatisticsAdminStub);

    }

    public CEPStatisticsAdminServiceClient(String backEndUrl, String userName, String password)
            throws AxisFault {
        this.endPoint = backEndUrl + serviceName;
        cepStatisticsAdminStub = new CEPStatisticsAdminStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, cepStatisticsAdminStub);

    }

    public CountDTO getGlobalCount() throws RemoteException {
        CountDTO countDTO = null;
        try {
             countDTO = cepStatisticsAdminStub.getGlobalCount();
        } catch (RemoteException e) {
           throw new RemoteException("RemoteException", e);
        }
        return countDTO;
    }


}
