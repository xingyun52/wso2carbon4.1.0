/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
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

package org.jenkins.wso2.appfactory;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.security.AbstractPasswordBasedSecurityRealm;
import hudson.security.GroupDetails;
import hudson.security.SecurityRealm;
import hudson.util.FormValidation;

import java.io.File;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationServiceException;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.axis2.AxisFault;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.springframework.dao.DataAccessException;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;

public class CarbonSecurityRealm extends AbstractPasswordBasedSecurityRealm {

    /**
     * Logger for debugging purposes.
     */
    private static final Logger LOGGER = Logger.getLogger(CarbonSecurityRealm.class.getName());

    private String clientTrustStore;
    private String clientTrustStorePassword;
    private String authenticationServiceEPR;

    @DataBoundConstructor
    public CarbonSecurityRealm(String clientTrustStore, String clientTrustStorePassword,
                               String authenticationServiceEPR) {
        this.authenticationServiceEPR = authenticationServiceEPR;
        this.clientTrustStore = clientTrustStore;
        this.clientTrustStorePassword = clientTrustStorePassword;
        
    }

    public String getClientTrustStore() {
        return clientTrustStore;
    }

    public void setClientTrustStore(String clientTrustStore) {
        this.clientTrustStore = clientTrustStore;
    }

    public String getClientTrustStorePassword() {
        return clientTrustStorePassword;
    }

    public void setClientTrustStorePassword(String clientTrustStorePassword) {
        this.clientTrustStorePassword = clientTrustStorePassword;
    }

    public String getAuthenticationServiceEPR() {
        return authenticationServiceEPR;
    }

    public void setAuthenticationServiceEPR(String authenticationServiceEPR) {
        this.authenticationServiceEPR = authenticationServiceEPR;
    }

    @Extension
    public static DescriptorImpl install() {
        return new DescriptorImpl();
    }

    public static final class DescriptorImpl extends Descriptor<SecurityRealm> {

        
        public DescriptorImpl(){
            load();
        }
        
        public FormValidation doCheckAuthenticationServiceEPR(@QueryParameter final String authenticationServiceEPR) {
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.error("User doesn't have enough privilage");
            }

            if (0 == authenticationServiceEPR.length()) {
                return FormValidation.error("invalid url");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckClientTrustStorePassword(@QueryParameter final String clientTrustStorePassword) {
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.error("User doesn't have enough privilage");
            }

            if (0 == clientTrustStorePassword.length()) {
                return FormValidation.error("invalid password");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckClientTrustStore(@QueryParameter final String clientTrustStore) {
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.error("User doesn't have enough privilage");
            }

            FormValidation formValidation = null;
            File path = new File(clientTrustStore);
            if (path.canRead()) {
                formValidation = FormValidation.ok();
            } else {
                formValidation = FormValidation.error("Client trust store doesn't exist");
            }

            return formValidation;
        }

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }
    }

    @Override
    protected UserDetails authenticate(String username, String password)
                                                                        throws AuthenticationException {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("login request recieved for : " + username);
        }

       

        try {
            
            AuthenticationAdminStub astub =
                                            new AuthenticationAdminStub(
                                                                        this.getAuthenticationServiceEPR());
            
            System.setProperty("javax.net.ssl.trustStore", this.getClientTrustStore());
            System.setProperty("javax.net.ssl.trustStorePassword", this.getClientTrustStorePassword());
            boolean loggedIn = astub.login(username, password, null);

            if (loggedIn) {
                
                if (LOGGER.isLoggable(Level.FINER)) {
                    LOGGER.finer("Sucessfully authenticated user : " + username);
                }
               
                GrantedAuthority[] authorities =
                                                 new GrantedAuthority[] { SecurityRealm.AUTHENTICATED_AUTHORITY };

                return new CarbonUserDetails(username, password, authorities);
            } else {
                throw new BadCredentialsException(
                                                        "Invalid credentials supplied user name - " +
                                                                username + "Password : *****");
            }
            
        } catch (AxisFault e) {
            throw new AuthenticationServiceException(e.getLocalizedMessage(), e);
        } catch (LoginAuthenticationExceptionException loginException) {
            throw new AuthenticationServiceException(loginException.getMessage(), loginException);
        } catch (RemoteException remoteException) {
            throw new AuthenticationServiceException(remoteException.getMessage(), remoteException);
        }

    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException,
                                                          DataAccessException {
        throw new UsernameNotFoundException("loading users by name is not supported");

    }

    @Override
    public GroupDetails loadGroupByGroupname(String groupname) throws UsernameNotFoundException,
                                                              DataAccessException {
        return new CarbonGroupDetails(groupname);
    }

    class CarbonGroupDetails extends GroupDetails {
        private String name;

        CarbonGroupDetails(String n) {
            this.name = n;
        }

        @Override
        public String getName() {
            return name;
        }

    }

}
