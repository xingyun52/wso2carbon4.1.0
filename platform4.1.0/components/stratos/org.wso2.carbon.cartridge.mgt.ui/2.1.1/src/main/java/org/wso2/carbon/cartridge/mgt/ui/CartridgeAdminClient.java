/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
 *                                                                             
 * Licensed under the Apache License, Version 2.0 (the "License");             
 * you may not use this file except in compliance with the License.            
 * You may obtain a copy of the License at                                     
 *                                                                             
 *      http://www.apache.org/licenses/LICENSE-2.0                             
 *                                                                             
 * Unless required by applicable law or agreed to in writing, software         
 * distributed under the License is distributed on an "AS IS" BASIS,           
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    
 * See the License for the specific language governing permissions and         
 * limitations under the License.                                              
 */
package org.wso2.carbon.cartridge.mgt.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.mgt.dto.xsd.Cartridge;
import org.wso2.carbon.adc.mgt.stub.ApplicationManagementServiceADCExceptionException;
import org.wso2.carbon.adc.mgt.stub.ApplicationManagementServiceStub;

import java.rmi.RemoteException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Client which communicates with the Application Management service of ADC
 */
public class CartridgeAdminClient {
    public static final String BUNDLE = "org.wso2.carbon.cartridge.mgt.ui.i18n.Resources";
    public static final int MILLISECONDS_PER_MINUTE = 60 * 1000;
    private static final Log log = LogFactory.getLog(CartridgeAdminClient.class);
    private ResourceBundle bundle;
    public ApplicationManagementServiceStub stub;

    public CartridgeAdminClient(String cookie,
                                String backendServerURL,
                                ConfigurationContext configCtx,
                                Locale locale) throws AxisFault {
        String serviceURL = backendServerURL  + "ApplicationManagementService";
        bundle = ResourceBundle.getBundle(BUNDLE, locale);

        stub = new ApplicationManagementServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        option.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
    }

    public Cartridge[] getCartridgesList() throws AxisFault {
        Cartridge[] cartridges = new Cartridge[0];
        try {
            cartridges = stub.listAvailableCartridges();
        } catch (RemoteException e) {
            handleException("cannot.list.cartridges", e);
        } catch (ApplicationManagementServiceADCExceptionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return cartridges;
    }

    public int getCartridgeClusterMaximumLimit() throws AxisFault {
        int max = 5;
        try {
            max = stub.getCartridgeClusterMaxLimit();
        } catch (RemoteException e) {
            handleException("cannot.list.cartridges", e);
        }
        return max;
    }

    public String unsubscribe(String alias) throws AxisFault {
        String unsubscribeResult = null;
        try {
            unsubscribeResult = stub.unsubscribe(alias);
        } catch (RemoteException e) {
            handleException("cannot.unsubscribe", e);
        } catch (ApplicationManagementServiceADCExceptionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return unsubscribeResult;
    }


    public Cartridge getInfo(String alias) throws AxisFault, ApplicationManagementServiceADCExceptionException {
        Cartridge cartridge = null;
        try {
            cartridge = stub.listCartridgeInfo(alias);
        } catch (RemoteException e) {
            handleException("cannot.unsubscribe", e);
        }
        return cartridge;
    }


    public String subscribeToCartridge(String cartridgeType,
                                       int min, int max,
                                       String cartridgeName,
                                       String repositoryUrl,
                                       String otherCartridgeType,
                                       String otherCartridgeAlias)
            throws AxisFault {
        //TODO : need to retrieve repository user name and password
        log.info("min " + min);
        log.info("max " + max);
        log.info(cartridgeType);
        log.info(cartridgeName);
        String repoUrl = null;
        String checkedRepositoryUrl = ("".equalsIgnoreCase(repositoryUrl.trim()))? null: repositoryUrl;
        String checkedOtherType = (otherCartridgeAlias == null)? null: otherCartridgeType.trim();
        String checkedOtherAlias = (otherCartridgeAlias == null)? null: otherCartridgeAlias.trim();
        try {
            if(checkedOtherType != null && !cartridgeType.equalsIgnoreCase("mysql")){
                //currently passing empty strings for repo user name and passwords
                stub.subscribe(1, 1, true, checkedOtherAlias, checkedOtherType.toLowerCase(),
                        null ,"","", null, null);
                stub.subscribe(min, max, true, cartridgeName, cartridgeType , checkedRepositoryUrl
                            ,"","", checkedOtherType, checkedOtherAlias);

            } else {
            	if(cartridgeType.equalsIgnoreCase("mysql")){
            		checkedOtherType = null;
            		checkedOtherAlias = null;
            	}
                stub.subscribe(min, max, true, cartridgeName, cartridgeType , null
                                                            , null,"","", null);
            }
        } catch (RemoteException e) {
            handleException("cannot.subscribe", e);
        } catch (ApplicationManagementServiceADCExceptionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //TODO remove true when activate is corrected at back-end
        return repoUrl;
    }

    private void handleException(String msgKey, Exception e) throws AxisFault {
        String msg = bundle.getString(msgKey);
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

}
