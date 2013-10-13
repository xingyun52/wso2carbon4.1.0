package org.wso2.carbon.user.multicredentials.test;/*
 *   Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.user.cassandra.CFConstants;
import org.wso2.carbon.user.mgt.multiplecredentials.stub.types.Credential;
import org.wso2.carbon.user.mgt.stub.MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException;
import org.wso2.carbon.user.mgt.stub.MultipleCredentialsUserAdminStub;

import java.rmi.RemoteException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class BasicTest {

    private MultipleCredentialsUserAdminStub adminStub;
    private String backendServerURL;
    private String keyStoreLocation;

//    public static void main(String[] args) {
//        BasicTest basicTest = new BasicTest();
//        basicTest.init();
//        try {
//            basicTest.CRDUser();
//        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        } catch (RemoteException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//
//    }

    @BeforeClass
    public void init() {
        backendServerURL = "https://localhost:9443/services/";
        keyStoreLocation = this.getClass().getResource("client-truststore.jks").getFile();
        initMultiCredsStub();
    }

    @Test
    public void CRDUser()
            throws MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException,
                   RemoteException {
        try {
            adminStub.deleteUser("Tharindu", CFConstants.DEFAULT_TYPE);
        } catch (RemoteException e) {
            // ignore
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            // ignore
        }
        Credential credential = new Credential();
        credential.setCredentialsType(CFConstants.DEFAULT_TYPE);
        credential.setIdentifier("Tharindu");

        // add user
        adminStub.addUser(credential, null, null, null);

        // get user creds
        Credential[] credentials = adminStub.getCredentials("Tharindu", CFConstants.DEFAULT_TYPE);
        for (Credential returnedCreds : credentials) {
            Assert.assertEquals(returnedCreds.getIdentifier(), credential.getIdentifier(),
                                "Returned creds not equal to actual creds");
            Assert.assertEquals(returnedCreds.getCredentialsType(), credential.getCredentialsType(),
                                "Returned creds not equal to actual creds");
        }

        // delete user
        adminStub.deleteUser("Tharindu", CFConstants.DEFAULT_TYPE);
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedException = null;

        // get non-existing user creds
        try {
            adminStub.getCredentials("Tharindu", CFConstants.DEFAULT_TYPE);
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedException = e;
        } finally {
            assertNotNull(expectedException);
        }
    }

    @Test
    public void CRUDCredentials()
            throws MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException,
                   RemoteException {
        try {
            adminStub.deleteUser("Caressa", CFConstants.DEFAULT_TYPE);
        } catch (RemoteException e) {
            // ignore
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            // ignore
        }
        // add user with default creds
        Credential credential = new Credential();
        credential.setCredentialsType(CFConstants.DEFAULT_TYPE);
        credential.setIdentifier("Caressa");
        adminStub.addUser(credential, null, null, null);

        // add device creds
        Credential deviceCreds = new Credential();
        deviceCreds.setIdentifier("Aa:SSD:SDSA:SA:01");
        deviceCreds.setCredentialsType(CFConstants.DEVICE_TYPE);
        adminStub.addCredential(credential.getIdentifier(), credential.getCredentialsType(), deviceCreds);

        // get creds with default
        Credential[] returnedCreds1 = adminStub.getCredentials(credential.getIdentifier(), credential.getCredentialsType());
        assertTrue(returnedCreds1.length == 2, "returned creds does not contain both credentials");

        // get same creds with device
        Credential[] returnedCreds2 = adminStub.getCredentials(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType());

        // both should be equal to submitted creds
//        assertEquals(returnedCreds1, returnedCreds2, "Both creds not equal");
        for (Credential credential1 : returnedCreds1) {
            if (credential1.getCredentialsType().equals(CFConstants.DEFAULT_TYPE)) {
                assertEquals(credential1.getIdentifier(), credential.getIdentifier());
            } else if (credential1.getCredentialsType().equals(CFConstants.DEVICE_TYPE)) {
                assertEquals(credential1.getIdentifier(), deviceCreds.getIdentifier());
            }
        }

        for (Credential credential1 : returnedCreds2) {
            if (credential1.getCredentialsType().equals(CFConstants.DEFAULT_TYPE)) {
                assertEquals(credential1.getIdentifier(), credential.getIdentifier());
            } else if (credential1.getCredentialsType().equals(CFConstants.DEVICE_TYPE)) {
                assertEquals(credential1.getIdentifier(), deviceCreds.getIdentifier());
            }
        }

        // update default creds
        Credential updatedCredential = new Credential();
        updatedCredential.setIdentifier("Caressa2");
        updatedCredential.setCredentialsType(CFConstants.DEFAULT_TYPE);
        adminStub.updateCredential(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType(), updatedCredential);

        // use earlier creds - should not work
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedException = null;
        try {
            adminStub.getCredentials(credential.getIdentifier(), credential.getCredentialsType());
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedException = e;
        } finally {
            assertNotNull(expectedException);
        }

        // get using new creds
        Credential[] updatedReturnedCreds1 = adminStub.getCredentials(updatedCredential.getIdentifier(), updatedCredential.getCredentialsType());
        assertTrue(updatedReturnedCreds1.length == 2, "returned creds does not contain both credentials");

        // get using non - updated device creds
        Credential[] updatedReturnedCreds2 = adminStub.getCredentials(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType());

        // both should be equal
//        assertEquals(updatedReturnedCreds1, updatedReturnedCreds2, "Both creds not equal");
        for (Credential credential1 : updatedReturnedCreds2) {
            if (credential1.getCredentialsType().equals(CFConstants.DEFAULT_TYPE)) {
                assertEquals(credential1.getIdentifier(), updatedCredential.getIdentifier());
            } else if (credential1.getCredentialsType().equals(CFConstants.DEVICE_TYPE)) {
                assertEquals(credential1.getIdentifier(), deviceCreds.getIdentifier());
            }
        }

        for (Credential credential1 : updatedReturnedCreds1) {
            if (credential1.getCredentialsType().equals(CFConstants.DEFAULT_TYPE)) {
                assertEquals(credential1.getIdentifier(), updatedCredential.getIdentifier());
            } else if (credential1.getCredentialsType().equals(CFConstants.DEVICE_TYPE)) {
                assertEquals(credential1.getIdentifier(), deviceCreds.getIdentifier());
            }
        }

        // delete creds
        Credential nonExistentCredential = credential;

        // should throw exception
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedDeletingException = null;
        try {
            adminStub.deleteCredential(nonExistentCredential.getIdentifier(), nonExistentCredential.getCredentialsType());
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedDeletingException = e;
        } finally {
            assertNotNull(expectedDeletingException);
        }

        // delete inserted creds
        adminStub.deleteCredential(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType());
        // get using new creds
        Credential[] returnedCredsAfterDelete = adminStub.getCredentials(updatedCredential.getIdentifier(), updatedCredential.getCredentialsType());
        assertTrue(returnedCredsAfterDelete.length == 1, "returned creds should not contain deleted creds");

        // returned creds should be equal to the updated creds, i.e. the only creds left
        assertEquals(returnedCredsAfterDelete[0].getCredentialsType(), updatedCredential.getCredentialsType());
        assertEquals(returnedCredsAfterDelete[0].getIdentifier(), updatedCredential.getIdentifier());

        //  check if it's possible to get creds from the deleted creds
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedExceptionWhenGettingCredsAfterDeletion = null;
        try {
            adminStub.getCredentials(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType());
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedExceptionWhenGettingCredsAfterDeletion = e;
        } finally {
            assertNotNull(expectedExceptionWhenGettingCredsAfterDeletion);
        }

        // check if it's possible to update deleted creds
        MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException expectedExceptionWhenUpdatingCredsAfterDeletion = null;
        try {
            adminStub.updateCredential(deviceCreds.getIdentifier(), deviceCreds.getCredentialsType(), deviceCreds);
        } catch (RemoteException e) {
        } catch (MultipleCredentialsUserAdminMultipleCredentialsUserAdminExceptionException e) {
            expectedExceptionWhenUpdatingCredsAfterDeletion = e;
        } finally {
            assertNotNull(expectedExceptionWhenUpdatingCredsAfterDeletion);
        }
    }





    private void initMultiCredsStub() {

        String keyStoreLoc = keyStoreLocation;
        System.setProperty("javax.net.ssl.trustStore", keyStoreLoc);
        System.setProperty("javax.net.ssl.keyStore", keyStoreLoc);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.keyStorePassword", "wso2carbon");

        /**
         * Axis2 configuration context
         */
        ConfigurationContext configContext;

        try {

            /**
             * Create a configuration context. A configuration context contains
             * information for
             * axis2 environment. This is needed to create an axis2 client
             */
            configContext =
                    ConfigurationContextFactory.createConfigurationContextFromFileSystem(null,
                                                                                         null);

            String serviceEndPoint = backendServerURL + "MultipleCredentialsUserAdmin";

            adminStub = new MultipleCredentialsUserAdminStub(configContext, serviceEndPoint);
            ServiceClient client = adminStub._getServiceClient();
            Options option = client.getOptions();

            /**
             * setting basic auth headers for authentication for user admin
             */
            HttpTransportProperties.Authenticator auth =
                    new HttpTransportProperties.Authenticator();
            auth.setUsername("admin");
            auth.setPassword("admin");
            auth.setPreemptiveAuthentication(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, auth);
            option.setManageSession(true);
            option.setTimeOutInMilliSeconds(10 * 60 * 1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
