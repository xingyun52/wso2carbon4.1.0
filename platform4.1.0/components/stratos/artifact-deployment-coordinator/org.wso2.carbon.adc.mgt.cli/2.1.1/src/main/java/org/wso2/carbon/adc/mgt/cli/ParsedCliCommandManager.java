/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.cli;

import net.sf.json.JSONObject;
import org.wso2.carbon.adc.mgt.cli.clients.BasicAuthApplicationManagementServiceClient;
import org.wso2.carbon.adc.mgt.cli.utils.CliConstants;
import org.wso2.carbon.adc.mgt.dto.xsd.Cartridge;
import org.wso2.carbon.adc.mgt.stub.ApplicationManagementServiceADCExceptionException;

import java.rmi.RemoteException;

/**
 * All the service requests go through this class. Here it will print the outputs from the service request calls.
 * All the exceptions are handled and messages to users are printed here. Also tries to find real cause of errors
 * and inform with clearest possible way.
 */
public class ParsedCliCommandManager {

    BasicAuthApplicationManagementServiceClient basicAuthApplicationManagementServiceClient;
    private static final String CONNECTION_LOST_MESSAGE = "Connection to host is broken, backend server may be unavailable! ";

    /**
     * This will crete the stub and also validate for prompt mode.
     *
     * @param host ADC host
     * @param port ADC port if available
     * @param userName stratos tenant username
     * @param passWord stratos tenant password
     * @param validateAuth validation will only happen with command prompt mode. Single call mode this will be false
     * @return whether the validation is successful
     */
    public boolean loggingToRemoteServer(String host, String port, String userName, String passWord, boolean validateAuth) {

        String backEndServerUrl;
        if(port == null){
            backEndServerUrl = "https://" + host;
        } else {
            backEndServerUrl = "https://" + host + ":" + port;
        }
        basicAuthApplicationManagementServiceClient =
                new BasicAuthApplicationManagementServiceClient(backEndServerUrl, userName, passWord );
        try {
            if(validateAuth){
                return basicAuthApplicationManagementServiceClient.validateAuthentication();
            } else {
                return true;
            }
        } catch (RemoteException e) {
            System.out.println("Authentication failed!");
        }
        return false;
    }

    public void subscribe(String cartridgeType, String name, String min, String max,
                          String externalRepoURL, String userName, String password,
                          String mySqlCartridge, String mySQLAlias) {
        int minimum = 1, maximum = 1; //define default values
        if(min != null ) minimum = Integer.parseInt(min);
        if(max != null ) maximum = Integer.parseInt(max);
        if(minimum < 0 || maximum < 0){
            System.out.println("\nEnter positive numbers for min and max");
            return;
        }

        if(minimum <= maximum){
            int cartridgeClusterLimit = getCartridgeClusterMaxLimit();
            if(maximum > cartridgeClusterLimit){
                System.out.println("\nMaximum you provided is higher than the limit of the cartridge cluster, \n" +
                        "limit is " + cartridgeClusterLimit);
                return;
            }
            String repoURL = null;
            try {
                repoURL = basicAuthApplicationManagementServiceClient.subscribeToCartridge(cartridgeType,
                                                                                                  minimum,
                                                                                                  maximum,
                                                                                                  name,
                                                                                                  externalRepoURL,
                                                                                                  userName,
                                                                                                  password,
                                                                                                  mySqlCartridge,
                                                                                                  mySQLAlias);


                String commonMsg = "You have successfully subscribed to " + cartridgeType + " cartridge.";
                String RepoCreatedMsg = " repository is created.";
                System.out.println(commonMsg);

                if (repoURL != null && !cartridgeType.equals("mysql")) {
                    System.out.println(repoURL + RepoCreatedMsg);

                    Cartridge cart = basicAuthApplicationManagementServiceClient.listCartridgeInfo(name);
                    String hostNameMsg = "Your application is being published here. " + getAccessUrl(cart.getProvider(),
                            cart.getHostName());
                    System.out.println(hostNameMsg);
                }
                if(externalRepoURL != null) {

                    String takeTimeMsg = "(this might take a minute... depending on repo size)";
                    System.out.println(takeTimeMsg);

                }
            } catch (RemoteException e) {
                System.out.println(CONNECTION_LOST_MESSAGE);
            } catch (ApplicationManagementServiceADCExceptionException e) {
                System.out.println(e.getFaultMessage().getADCException().getMessage());
            }
        } else {
            System.out.println("\nMinimum is larger than Maximum, please recheck the values entered");
            return;
        }
    }

    public void listTypes() {
        try {
            Cartridge[] cartridges = basicAuthApplicationManagementServiceClient.getTypes();
            int typeLength = 4, statusLength = 6, insLength = 9, nameLength = 4, accUrlLength = 10, repoUrlLength = 8;
            //These variables keep the maximum length of the each cartridge attribute for printing purposes
            for(Cartridge cartridge:cartridges ){
                boolean subscribedCartridge = !cartridge.getStatus().equalsIgnoreCase("NOT-SUBSCRIBED");
                String accUrl = null;
                if(cartridge.getHostName() != null && subscribedCartridge){
                    accUrl = getAccessUrl(cartridge.getProvider(), cartridge.getHostName());
                }
                if(typeLength < cartridge.getCartridgeType().length()) {
                    typeLength = cartridge.getCartridgeType().length();
                }
                if(statusLength < cartridge.getStatus().length()) {
                    statusLength = cartridge.getStatus().length();
                }
                if(insLength < (cartridge.getActiveInstances() + "").length()) {
                    insLength = (cartridge.getActiveInstances()+ "").length();
                }
                if(nameLength < cartridge.getCartridgeName().length()) {
                    nameLength = cartridge.getCartridgeName().length();
                }
                if(cartridge.getHostName() != null &&
                        subscribedCartridge &&
                        accUrlLength < accUrl.length()) {
                    accUrlLength = accUrl.length();
                }
                if(cartridge.getRepoURL() != null &&
                        subscribedCartridge &&
                        repoUrlLength < cartridge.getRepoURL().length()) {
                    repoUrlLength = cartridge.getRepoURL().length();
                }
            }
            System.out.println();
            printSet(typeLength, statusLength, insLength, nameLength, accUrlLength, repoUrlLength);
            System.out.println();
            printTitles(typeLength, statusLength, insLength, nameLength, accUrlLength, repoUrlLength);
            System.out.println();
            printSet(typeLength, statusLength, insLength, nameLength, accUrlLength, repoUrlLength);
            System.out.println();

            for(Cartridge cartridge:cartridges ){
                boolean subscribedCartridge = !cartridge.getStatus().equalsIgnoreCase("NOT-SUBSCRIBED");
                printCartridges(typeLength, statusLength, insLength, nameLength, accUrlLength, repoUrlLength, cartridge
                        , subscribedCartridge);
                System.out.println();
            }
            printSet(typeLength, statusLength, insLength, nameLength, accUrlLength, repoUrlLength);

            System.out.println();

        } catch (ApplicationManagementServiceADCExceptionException e) {
            System.out.println(e.getFaultMessage().getADCException().getMessage());
        } catch (RemoteException e) {
            System.out.println(CONNECTION_LOST_MESSAGE);
        }

    }

    private void printSet(int typeLength,int statusLength,int insLength,int nameLength,int accUrlLength ,int repoUrlLength){

        int i;
        System.out.print('+');
        for (i=0; i <= typeLength ; i++) {
            System.out.print('-');
        }
        System.out.print('+');
        for (i=0; i <= statusLength ; i++) {
            System.out.print('-');
        }
        System.out.print('+');
        for (i=0; i <= insLength ; i++) {
            System.out.print('-');
        }
        System.out.print('+');
        for (i=0; i <= nameLength ; i++) {
            System.out.print('-');
        }
        System.out.print('+');
        for (i=0; i <= accUrlLength ; i++) {
            System.out.print('-');
        }
        System.out.print('+');
        for (i=0; i <= repoUrlLength ; i++) {
            System.out.print('-');
        }
        System.out.print('+');
    }

    private void printTitles(int typeLength,int statusLength,int insLength,int nameLength,int accUrlLength ,int repoUrlLength){
        int i;
        System.out.print('|');
        System.out.print("Type");
        for (i = 0; i <= typeLength - 4 ; i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        System.out.print("Status");
        for (i = 0; i <= statusLength - 6 ; i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        System.out.print("Instances");
        for (i = 0; i <= insLength - 9; i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        System.out.print("Name");
        for (i = 0; i <= nameLength -4; i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        System.out.print("Access url");
        for (i = 0; i <= accUrlLength - 10; i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        System.out.print("Repo url");
        for (i = 0; i <= repoUrlLength - 8; i++) {
            System.out.print(' ');
        }
        System.out.print('|');
    }

    private void printCartridges(int typeLength, int statusLength, int insLength, int nameLength, int accUrlLength,
                                 int repoUrlLength,
                                 Cartridge cartridge,
                                 boolean subscribedCartridge){
        String accUrl = "";
        if(cartridge.getHostName() != null && subscribedCartridge){
            accUrl = getAccessUrl(cartridge.getProvider(), cartridge.getHostName());
        }
        int i;
        System.out.print('|');
        System.out.print(cartridge.getCartridgeType());
        for (i=0; i <= typeLength - cartridge.getCartridgeType().length() ; i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        System.out.print(cartridge.getStatus());
        for (i=0; i <= statusLength - cartridge.getStatus().length() ; i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        System.out.print(cartridge.getActiveInstances());
        for (i=0; i <= insLength - (cartridge.getActiveInstances() + "").length(); i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        System.out.print(cartridge.getCartridgeName());
        for (i=0; i <= nameLength - cartridge.getCartridgeName().length(); i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        System.out.print(accUrl);
        for (i=0; i <= accUrlLength - accUrl.length(); i++) {
            System.out.print(' ');
        }
        System.out.print('|');
        String repoUrl = " ";
        if(cartridge.getRepoURL() != null && subscribedCartridge)  {
            repoUrl = cartridge.getRepoURL();
        }
        System.out.print(repoUrl);
        for (i=0; i <= repoUrlLength - repoUrl.length(); i++) {
            System.out.print(' ');
        }
        System.out.print('|');
    }


     public String addDomainMapping(String mappedDomain,
                                    String alias){
        try {
            return basicAuthApplicationManagementServiceClient.addDomainMapping(mappedDomain, alias);
        }catch (ApplicationManagementServiceADCExceptionException e) {
            System.out.println(e.getFaultMessage().getADCException().getMessage());
        }catch (RemoteException e) {
            System.out.println(CONNECTION_LOST_MESSAGE);
        }
         return null;
    }


    public void listCartridgeInfo(String alias, String verbose){
    	try {
            Cartridge cartridge = null;
            try {
                cartridge = basicAuthApplicationManagementServiceClient.listCartridgeInfo(alias);
            } catch (ApplicationManagementServiceADCExceptionException e) {
                System.out.println(e.getFaultMessage().getADCException().getMessage());
                return;
            }
            System.out.println();
			System.out.println("Cartridge Info");
			System.out.println("--------------");
			if (verbose != null) {
				JSONObject jsonObject = JSONObject.fromObject( cartridge );
				System.out.println( jsonObject );
				System.out.println();
			} else {
				System.out.println("Cartridge: " + cartridge.getCartridgeType());
				System.out.println("Alias: " + cartridge.getCartridgeName());
                System.out.println("Access Url: " +getAccessUrl(cartridge.getHostName(), cartridge.getProvider()));
				if (cartridge.getIp() != null) {
					System.out.println("Host: " + cartridge.getIp());
				}
				if (cartridge.getPassword() != null) {
					System.out.println("Password: " + cartridge.getPassword());
				}
                if (cartridge.getRepoURL() != null) {
                    System.out.println("Repository URL: " + cartridge.getRepoURL());
                }
				System.out.println("Status: " + cartridge.getStatus());
				System.out.println("Active Instances: " + cartridge.getActiveInstances());
				System.out.println();
			}

        } catch (RemoteException e) {
            System.out.println(CONNECTION_LOST_MESSAGE);
        }
    }

    private String getAccessUrl(String provider, String hostName){
        String accessUrl = "";
        if(CliConstants.DATA_PROVIDER.equalsIgnoreCase(provider)) {
            accessUrl = "https://" + hostName +":8280/phpmyadmin";
        } else if(CliConstants.WSO2_PROVIDER.equalsIgnoreCase(provider)){
            accessUrl = "https://" + hostName +":8243";
        } else {
            accessUrl = "http://" + hostName +":8280";
        }
        return accessUrl;
    }

    public boolean setMySqlPassword(String ip, String password) {
    	try {
	        return basicAuthApplicationManagementServiceClient.setMySqlPassword(ip, password);
        } catch (Exception e) {
            String msg = "Error while setting mysql password";
            System.err.println(msg);
        }
        return false;
    }

    public void unsubscribe(String alias) {
    	try {
	        System.out.println(basicAuthApplicationManagementServiceClient.unsubscribe(alias));
        } catch (ApplicationManagementServiceADCExceptionException e) {
            System.out.println(e.getFaultMessage().getADCException().getMessage());
        } catch (RemoteException e) {
            System.out.println(CONNECTION_LOST_MESSAGE);
        }
    }

    public int getCartridgeClusterMaxLimit() {
        try {
            return basicAuthApplicationManagementServiceClient.getCartridgeClusterMaxLimit();
        } catch (RemoteException e) {
            System.out.println(CONNECTION_LOST_MESSAGE);
            return 5;//TODO remove this
        }
    }
}
