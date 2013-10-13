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

package org.wso2.carbon.bam.jmx.agent.profiles;


import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.bam.jmx.agent.exceptions.ProfileAlreadyExistsException;
import org.wso2.carbon.bam.jmx.agent.exceptions.ProfileDoesNotExistException;
import org.wso2.carbon.bam.jmx.agent.tasks.internal.JmxTaskServiceComponent;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.util.CryptoException;
import org.wso2.carbon.core.util.CryptoUtil;
import org.wso2.carbon.registry.common.services.RegistryAbstractAdmin;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;

public class ProfileManager extends RegistryAbstractAdmin {


    private static final String REG_LOCATION =
            "repository/components/org.wso2.carbon.publish.jmx.agent/";
    private static final Log log = LogFactory.getLog(ProfileManager.class);
    private Registry registry;
    private XStream xstream;

    private RegistryService registryService;
    private TenantRegistryLoader tenantRegistryLoader;
    private int tenantId;


    public ProfileManager() {
        registryService = JmxTaskServiceComponent.getRegistryService();
        tenantRegistryLoader = JmxTaskServiceComponent.getTenantRegistryLoader();

        //get the tenant's registry
        tenantId = CarbonContext.getCurrentContext().getTenantId();
        tenantRegistryLoader.loadTenantRegistry(tenantId);
        try {
            registry = registryService.getGovernanceSystemRegistry(tenantId);
        } catch (RegistryException e) {
            log.error("Error obtaining the registry" + e);
            return;
        }


        xstream = new XStream(new DomDriver());
        xstream.setClassLoader(Profile.class.getClassLoader());


    }

    /**
     * Encrypts the sensitive data in the Profile.
     * Currently encrypts only the passwords of the JMX
     * server and Data publisher.
     *
     * @param profile The profile which has the data to be encrypted
     * @return The profile with the encrypted data
     */
    private Profile encryptData(Profile profile) throws CryptoException {

        //encrypt the JMX server password
        String password = profile.getPass();

        //encrypt the password
        String cipherT =
                CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(password.getBytes());
        profile.setPass(cipherT);

        //encrypt the data publisher password
        String dpPassword = profile.getDpPassword();

        //encrypt the password
        String dpCipherT =
                CryptoUtil.getDefaultCryptoUtil().encryptAndBase64Encode(dpPassword.getBytes());
        profile.setDpPassword(dpCipherT);


        return profile;


    }

    /**
     * Decrypts the sensitive data in the Profile.
     *
     * @param profile The profile which has the data to be decrypted
     * @return The profile with the decrypted data
     */
    private Profile decryptData(Profile profile) throws CryptoException {

        String cipherT = profile.getPass();

        //decrypt the jmx server password
        byte[] decodedBArr = Base64.decodeBase64(cipherT.getBytes());
        byte[] passwordBArr = CryptoUtil.getDefaultCryptoUtil().decrypt(decodedBArr);
        String password = new String(passwordBArr);

        profile.setPass(password);

        //decrypt the data publisher password
        String dpCipherT = profile.getDpPassword();

        byte[] decodedDPBArr = Base64.decodeBase64(dpCipherT.getBytes());
        byte[] passwordDPBArr = CryptoUtil.getDefaultCryptoUtil().decrypt(decodedDPBArr);
        String dpPassword = new String(passwordDPBArr);

        profile.setDpPassword(dpPassword);


        return profile;
    }

    /**
     * Used to add a new JMX monitoring profile.
     *
     * @param profile The profile that needs to be added
     * @return Returns whether adding the profile was successful or not
     * @throws ProfileAlreadyExistsException
     */
    public boolean addProfile(Profile profile) throws ProfileAlreadyExistsException {

        //check whether the profile already exists

        String path = REG_LOCATION + profile.getName();

        try {
            if (registry.resourceExists(path)) {
                String error = "The profile " + profile.getName() + " already exists.";
                log.error(error);
                throw new ProfileAlreadyExistsException(error);


            } else {

                //encrypt data
                //TODO: What is the best approach to handle this exception?
                profile = encryptData(profile);

                //create the xml representation of the profile
                String xmlProfile = xstream.toXML(profile);

                Resource res = registry.newResource();
                res.setContent(xmlProfile);

                //save the profile
                registry.put(path, res);
                return true;


            }


        } catch (RegistryException e) {
            e.printStackTrace();
            log.error(e);

        } catch (CryptoException e) {
            log.error(e);
            e.printStackTrace();
        }

        return false;

    }

    public Profile getProfile(String profileName) throws ProfileDoesNotExistException {

        String path = REG_LOCATION + profileName;

        //if the profile does not exist

        try {
            if (!registry.resourceExists(path)) {
                String error = "The profile " + profileName + " does not exist.";
                log.error(error);
                throw new ProfileDoesNotExistException(error);


            }
        } catch (RegistryException e) {
            log.error(e);
            e.printStackTrace();
        }

        //if the profile exists
        try {
            Resource res = registry.get(path);
            String xmlProfile = new String((byte[]) res.getContent());


            //set the class loader
            //to escape from a XStream bug
            xstream.setClassLoader(Profile.class.getClassLoader());


            //get the profile
            Profile profile = (Profile) xstream.fromXML(xmlProfile);

            //encrypt data
            //TODO: What is the best approach to handle this exception?
            profile = decryptData(profile);

            //return profile
            return profile;


        } catch (RegistryException e) {
            log.error(e);
            e.printStackTrace();
        } catch (CryptoException e) {
            log.error(e);
            e.printStackTrace();
        }


        return null;
    }

    public boolean updateProfile(Profile profile) throws ProfileDoesNotExistException {

        String path = REG_LOCATION + profile.getName();

        //check whether the profile already exists
        try {
            if (!registry.resourceExists(path)) {
                String error =
                        "Cannot Update: The profile " + profile.getName() + " does not exist.";
                log.error(error);
                throw new ProfileDoesNotExistException(error);
            }
        } catch (RegistryException e) {
            log.error(e);
            e.printStackTrace();
        }

        //replace the profile if it exists
        try {
            //encrypt data
            //TODO: What is the best approach to handle this exception?
            profile = encryptData(profile);


            //create the xml representation of the profile
            String xmlProfile = xstream.toXML(profile);


            Resource res = registry.newResource();
            res.setContent(xmlProfile);

            //delete the existing profile
            registry.delete(path);

            //save the new profile
            registry.put(path, res);
            return true;


        } catch (CryptoException e) {
            log.error(e);
            e.printStackTrace();
        } catch (RegistryException e) {
            log.error(e);
            e.printStackTrace();
        }


        return false;
    }

    public boolean deleteProfile(String profileName) throws ProfileDoesNotExistException {

        String path = REG_LOCATION + profileName;


        try {
            //check whether the profile already exists
            if (!registry.resourceExists(path)) {
                String error = "Cannot Delete: The profile " + profileName + " does not exist.";
                log.error(error);
                throw new ProfileDoesNotExistException(error);
            }
            //if the profile exists
            else {

                registry.delete(path);
                return true;
            }
        } catch (RegistryException e) {
            log.error(e);
            e.printStackTrace();
        }


        return false;
    }

    /**
     * Returns all the active profiles.
     *
     * @return all the active profiles.
     */
    public Profile[] getActiveProfiles() {

        Profile[] profiles;

        //iterate through the profiles
        try {
            Resource folder = registry.get(REG_LOCATION);
            String[] content = (String[]) folder.getContent();

            //initiate the profiles array
            profiles = new Profile[content.length];

            //set the class loader
            //to escape from a XStream bug
            xstream.setClassLoader(Profile.class.getClassLoader());

            int counter = 0;

            for (String path : content) {
                Resource res = registry.get(path);
                String xmlProfile = new String((byte[]) res.getContent());
                Profile prf = (Profile) xstream.fromXML(xmlProfile);

                //if the profile is active
                if (prf.isActive()) {
                    //decrypt data
                    //TODO: What is the best approach to handle this exception?
                    prf = decryptData(prf);
                    profiles[counter++] = prf;
                }


            }

            return profiles;
        } catch (RegistryException e) {
            log.error(e);
            e.printStackTrace();
        } catch (CryptoException e) {
            log.error(e);
            e.printStackTrace();
        }


        return null;
    }


    /**
     * Returns an array of all the profiles regardless of their state
     *
     * @return an array of all the profiles regardless of their state
     */
    public Profile[] getAllProfiles() {
        Profile[] profiles;

        //iterate through the profiles
        try {
            Resource folder = registry.get(REG_LOCATION);
            String[] content = (String[]) folder.getContent();

            //initiate the profiles array
            profiles = new Profile[content.length];


            int counter = 0;

            for (String path : content) {
                Resource res = registry.get(path);
                String xmlProfile = new String((byte[]) res.getContent());
                //set the class loader
                //to escape from a XStream bug
                xstream.setClassLoader(Profile.class.getClassLoader());

                Profile profile = (Profile) xstream.fromXML(xmlProfile);

                //decrypt data
                //TODO: What is the best approach to handle this exception?
                profile = decryptData(profile);
                profiles[counter++] = profile;

            }

            return profiles;
        } catch (ResourceNotFoundException e) {
            //handle the case when no profiles are in the registry

            System.out.println();
            log.error("Resource does not exist");
            System.out.println();

            return null;
        } catch (RegistryException e) {
            //handle the case when no profiles are in the registry
            log.error(e);
            e.printStackTrace();
            return null;
        } catch (CryptoException e) {
            log.error(e);
            e.printStackTrace();
        }


        return null;
    }

    /**
     * Creates the profile for the toolbox
     *
     * @return - Returns the created profile
     */
    public Profile createToolboxProfile() throws ProfileAlreadyExistsException {


        Profile tbProfile = new Profile();

        //set basic information
        tbProfile.setName("toolbox");
        tbProfile.setVersion(1);

        //set the data publisher info
        tbProfile.setDpReceiverConnectionType("tcp://");
        tbProfile.setDpReceiverAddress("127.0.0.1:7611");
        tbProfile.setDpSecureUrlConnectionType("ssl://");
        tbProfile.setDpSecureAddress("127.0.0.1:7711");

        tbProfile.setDpUserName("admin");
        tbProfile.setDpPassword("admin");

        tbProfile.setCronExpression("0/2 * * ? * *");

        //set the JMX server information
        tbProfile.setUrl("service:jmx:rmi://localhost:11111/jndi/rmi://localhost:9999/jmxrmi");
        tbProfile.setUserName("admin");
        tbProfile.setPass("admin");

        //set the attributes to be monitored
        String[][][] attributes = new String[2][][];

        //add the attributes of the first mbean

        attributes[0] = new String[9][3];

        //set the mbean name
        attributes[0][0][0] = "java.lang:type=Memory";

        //add an attribute
        attributes[0][1][0] = "HeapMemoryUsage";
        //add the keys of that attribute
        attributes[0][1][1] = "committed";
        //add the alias of that attribute
        attributes[0][1][2] = "heap_mem_committed";


        //add an attribute
        attributes[0][2][0] = "HeapMemoryUsage";
        //add the key of that attribute
        attributes[0][2][1] = "init";
        //add the alias of that attribute
        attributes[0][2][2] = "heap_mem_init";

        //add an attribute
        attributes[0][3][0] = "HeapMemoryUsage";
        //add the keys of that attribute
        attributes[0][3][1] = "max";
        //add the alias of that attribute
        attributes[0][3][2] = "heap_mem_max";

        //add an attribute
        attributes[0][4][0] = "HeapMemoryUsage";
        //add the keys of that attribute
        attributes[0][4][1] = "used";
        //add the alias of that attribute
        attributes[0][4][2] = "heap_mem_used";

        //add another attribute
        attributes[0][5][0] = "NonHeapMemoryUsage";
        //add the keys of that attribute
        attributes[0][5][1] = "committed";
        //add the alias of that attribute
        attributes[0][5][2] = "non_heap_mem_committed";

        //add another attribute
        attributes[0][6][0] = "NonHeapMemoryUsage";
        //add the keys of that attribute
        attributes[0][6][1] = "init";
        //add the alias of that attribute
        attributes[0][6][2] = "non_heap_mem_init";

        //add another attribute
        attributes[0][7][0] = "NonHeapMemoryUsage";
        //add the keys of that attribute
        attributes[0][7][1] = "max";
        //add the alias of that attribute
        attributes[0][7][2] = "non_heap_mem_max";

        //add another attribute
        attributes[0][8][0] = "NonHeapMemoryUsage";
        //add the keys of that attribute
        attributes[0][8][1] = "used";
        //add the alias of that attribute
        attributes[0][8][2] = "non_heap_mem_used";


        attributes[1] = new String[2][2];

        //set the mbean name
        attributes[1][0][0] = "java.lang:type=OperatingSystem";

        //add an attribute
        attributes[1][1][0] = "ProcessCpuTime";
        //add the alias of that attribute
        attributes[1][1][1] = "processCpuTime";


        tbProfile.setAttributes(attributes);


        //keep the profile deactivated at the beginning
        tbProfile.setActive(false);


        this.addProfile(tbProfile);


        return tbProfile;

    }

}
