package org.wso2.carbon.adc.mgt.cli.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.wso2.carbon.adc.mgt.dto.xsd.Cartridge;
import org.wso2.carbon.adc.mgt.stub.ApplicationManagementServiceADCExceptionException;
import org.wso2.carbon.adc.mgt.stub.ApplicationManagementServiceStub;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class BasicAuthApplicationManagementServiceClient {
    private Map<String, ApplicationManagementServiceStub> entitlementStub = new ConcurrentHashMap<String, ApplicationManagementServiceStub>();
    HttpTransportProperties.Authenticator authenticator;
    private String serverUrl;

    public BasicAuthApplicationManagementServiceClient(String serverUrl, String userName, String password){
        this.serverUrl = serverUrl;
        authenticator = new HttpTransportProperties.Authenticator();
        authenticator.setUsername(userName);
        authenticator.setPassword(password);
        authenticator.setPreemptiveAuthentication(true);

    }

    private ApplicationManagementServiceStub getApplicationManagementStub(String serverUrl) throws AxisFault {

        if (entitlementStub.containsKey(serverUrl)) {
            return entitlementStub.get(serverUrl);
        }
        ApplicationManagementServiceStub stub;
        ConfigurationContext configurationContext = null;
        try {
            configurationContext = ConfigurationContextFactory.createDefaultConfigurationContext();
        } catch (Exception e) {
            String msg = "Backend error occurred. Please contact the service admins!";
            System.out.println(msg);
        }
        HashMap<String, TransportOutDescription> transportsOut = configurationContext
                .getAxisConfiguration().getTransportsOut();
        for (TransportOutDescription transportOutDescription : transportsOut.values()) {
            transportOutDescription.getSender().init(configurationContext, transportOutDescription);
        }
        stub = new ApplicationManagementServiceStub(configurationContext, serverUrl + "/services/ApplicationManagementService");
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, authenticator);
        option.setTimeOutInMilliSeconds(300000);
        entitlementStub.put(serverUrl, stub);
        return stub;
    }

    public String addDomainMapping(String mappedDomain, String alias) throws RemoteException, ApplicationManagementServiceADCExceptionException {

        ApplicationManagementServiceStub stub = getApplicationManagementStub(serverUrl);
        return stub.addDomainMapping(mappedDomain, alias);
    }

    public String subscribeToCartridge(String cartridgeType,
                                       int min, int max,
                                       String cartridgeName,
                                       String repositoryUrl,
                                       String userName,
                                       String password,
                                       String mySqlCartridge,
                                       String mySqlAlias)
            throws RemoteException,
            ApplicationManagementServiceADCExceptionException {
        ApplicationManagementServiceStub stub = getApplicationManagementStub(serverUrl);
        //return stub.subscribe(min, max, true, cartridgeName, cartridgeType, repositoryUrl,mySqlCartridge, mySqlAlias);
        return stub.subscribe(min, max, true, cartridgeName, cartridgeType, repositoryUrl,userName,password,mySqlCartridge, mySqlAlias);
    }


    public Cartridge[] getTypes() throws RemoteException, ApplicationManagementServiceADCExceptionException {
        ApplicationManagementServiceStub stub = getApplicationManagementStub(serverUrl);
        return stub.listAvailableCartridges();
    }

    public Cartridge listCartridgeInfo(String alias) throws RemoteException, ApplicationManagementServiceADCExceptionException {
        ApplicationManagementServiceStub stub = getApplicationManagementStub(serverUrl);
        return stub.listCartridgeInfo(alias);
    }

    public boolean setMySqlPassword(String ip, String password) throws RemoteException {
        ApplicationManagementServiceStub stub = getApplicationManagementStub(serverUrl);
        return stub.setMySqlPassword(ip, password);
    }

    public boolean validateAuthentication() throws RemoteException {
        ApplicationManagementServiceStub stub = getApplicationManagementStub(serverUrl);
        return stub.authenticateValidation();
    }
    
    public String unsubscribe(String alias) throws RemoteException, ApplicationManagementServiceADCExceptionException {
    	ApplicationManagementServiceStub stub = getApplicationManagementStub(serverUrl);
        return stub.unsubscribe(alias);
    }

    public int getCartridgeClusterMaxLimit() throws RemoteException {
        ApplicationManagementServiceStub stub = getApplicationManagementStub(serverUrl);
        return stub.getCartridgeClusterMaxLimit();
    }

}
