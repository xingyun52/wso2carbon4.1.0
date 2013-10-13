/*
*  Copyright (c) WSO2 Inc. (http://wso2.com) All Rights Reserved.

  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*
*/

package org.wso2.carbon.bam.jmx.agent.ui;


import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.bam.jmx.agent.stub.JmxAgentIOExceptionException;
import org.wso2.carbon.bam.jmx.agent.stub.JmxAgentInstanceNotFoundExceptionException;
import org.wso2.carbon.bam.jmx.agent.stub.JmxAgentIntrospectionExceptionException;
import org.wso2.carbon.bam.jmx.agent.stub.JmxAgentMalformedObjectNameExceptionException;
import org.wso2.carbon.bam.jmx.agent.stub.JmxAgentProfileAlreadyExistsExceptionException;
import org.wso2.carbon.bam.jmx.agent.stub.JmxAgentProfileDoesNotExistExceptionException;
import org.wso2.carbon.bam.jmx.agent.stub.JmxAgentReflectionExceptionException;
import org.wso2.carbon.bam.jmx.agent.stub.JmxAgentStub;
import org.wso2.carbon.bam.jmx.agent.stub.ArrayOfString;
import org.wso2.carbon.bam.jmx.agent.stub.profiles.xsd.Profile;

import java.rmi.RemoteException;

public class JmxConnector {

    private JmxAgentStub stub;
    private static final Log log = LogFactory.getLog(JmxConnector.class);

    public JmxConnector(ConfigurationContext configCtx, String backendServerURL, String cookie) {
        String serviceURL = backendServerURL + "JmxAgent";

        try {
            stub = new JmxAgentStub(configCtx, serviceURL);
            ServiceClient client = stub._getServiceClient();
            Options options = client.getOptions();

            options.setManageSession(true);
            options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
                                cookie);


        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        }


    }


    public String[][] getMBeans(String url, String userName, String Password)
            throws RemoteException, JmxAgentIOExceptionException {
        try {


            ArrayOfString[] arr = stub.getMBeans(url, userName, Password);
            int width = arr[0].getArray().length;
            int height = arr.length;

            String[][] strArr = new String[height][width];

            for (int i = 0; i < height; i++) {
                for (int k = 0; k < width; k++) {
                    strArr[i][k] = arr[i].getArray()[k];
                }
            }


            return strArr;

        } catch (RemoteException e) {
            e.printStackTrace();
            throw new RemoteException(e.getMessage());
        } catch (JmxAgentIOExceptionException e) {
            e.printStackTrace();
            throw new JmxAgentIOExceptionException(e);
        }

    }

    public String[][] getMBeanAttributes(String objName, String url, String userName,
                                         String Password)
            throws RemoteException {

        try {


            ArrayOfString[] arr = stub.getMBeanAttributeInfo(objName, url, userName, Password);
            int height = arr.length;

            String[][] strArr = new String[height][];

            for (int i = 0; i < height; i++) {
                int width = arr[i].getArray().length;
                strArr[i] = new String[width];
                for (int k = 0; k < width; k++) {
                    strArr[i][k] = arr[i].getArray()[k];
                }
            }


            return strArr;


        } catch (RemoteException e) {
            e.printStackTrace();
            throw new RemoteException(e.getMessage());
        } catch (JmxAgentIntrospectionExceptionException e) {
            e.printStackTrace();
        } catch (JmxAgentReflectionExceptionException e) {
            e.printStackTrace();
        } catch (JmxAgentMalformedObjectNameExceptionException e) {
            e.printStackTrace();
        } catch (JmxAgentInstanceNotFoundExceptionException e) {
            e.printStackTrace();
        } catch (JmxAgentIOExceptionException e) {
            e.printStackTrace();
        }
        catch (NullPointerException e){
            //if there are no attributes in the mBean
            return null;
        }
        return new String[0][0];
    }


    public boolean addProfile(Profile profile)
            throws RemoteException,
                   JmxAgentProfileAlreadyExistsExceptionException {

        try {
            return stub.addProfile(profile);
        } catch (RemoteException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        } catch (JmxAgentProfileAlreadyExistsExceptionException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    public Profile getProfile(String profileName) throws
                                                  JmxAgentProfileDoesNotExistExceptionException,
                                                  RemoteException {

        try {
            return stub.getProfile(profileName);
        } catch (RemoteException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        } catch (JmxAgentProfileDoesNotExistExceptionException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    public boolean updateProfile(Profile profile)
            throws RemoteException, JmxAgentProfileDoesNotExistExceptionException {
        try {
            return stub.updateProfile(profile);
        } catch (RemoteException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        } catch (JmxAgentProfileDoesNotExistExceptionException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    public boolean deleteProfile(String profileName)
            throws RemoteException, JmxAgentProfileDoesNotExistExceptionException {
        try {
            return stub.deleteProfile(profileName);
        } catch (RemoteException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        } catch (JmxAgentProfileDoesNotExistExceptionException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    public Profile[] getActiveProfiles() throws RemoteException {
        try {
            return stub.getActiveProfiles();

        } catch (RemoteException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    public Profile[] getAllProfiles() throws RemoteException {
        try {
            return stub.getAllProfiles();
        } catch (RemoteException e) {
            log.error(e);
            e.printStackTrace();
            throw e;
        }
    }

    public void enableProfile(String profileName)
            throws JmxAgentProfileDoesNotExistExceptionException, RemoteException {
        stub.startMonitoringProfile(profileName);
    }

    public void disableProfile(String profileName)
            throws JmxAgentProfileDoesNotExistExceptionException, RemoteException {
        stub.stopMonitoringProfile(profileName);
    }

    public boolean checkDataPublisherAvailability(String connectionType, String url, int port)
            throws RemoteException {
        return stub.testDataPublisherAvailability(connectionType, url, port);
    }

    public boolean addToolboxProfile() throws RemoteException {
        return stub.addToolboxProfile();
    }


}
