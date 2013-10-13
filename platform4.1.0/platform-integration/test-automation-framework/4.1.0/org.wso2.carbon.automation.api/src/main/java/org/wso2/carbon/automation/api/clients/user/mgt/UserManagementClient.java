/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.carbon.automation.api.clients.user.mgt;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.api.clients.utils.AuthenticateStub;
import org.wso2.carbon.user.mgt.stub.DeleteUserUserAdminExceptionException;
import org.wso2.carbon.user.mgt.stub.GetAllRolesNamesUserAdminExceptionException;
import org.wso2.carbon.user.mgt.stub.GetUsersOfRoleUserAdminExceptionException;
import org.wso2.carbon.user.mgt.stub.ListUsersUserAdminExceptionException;
import org.wso2.carbon.user.mgt.stub.UpdateUsersOfRoleUserAdminExceptionException;
import org.wso2.carbon.user.mgt.stub.UserAdminStub;
import org.wso2.carbon.user.mgt.stub.types.carbon.ClaimValue;
import org.wso2.carbon.user.mgt.stub.types.carbon.FlaggedName;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class UserManagementClient {
    private final Log log = LogFactory.getLog(UserManagementClient.class);

    private UserAdminStub userAdminStub;
    private final String serviceName = "UserAdmin";

    public UserManagementClient(String backendURL, String sessionCookie) throws AxisFault {

        String endPoint = backendURL + serviceName;
        userAdminStub = new UserAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, userAdminStub);
    }

    public UserManagementClient(String backendURL, String userName, String password)
            throws AxisFault {

        String endPoint = backendURL + serviceName;
        userAdminStub = new UserAdminStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, userAdminStub);
    }

    public void addRole(String roleName, String[] userList, String[] permissions)
            throws Exception {

        userAdminStub.addRole(roleName, userList, permissions);

    }

    public void addUser(String userName, String password, String[] roles,
                        String profileName) throws Exception {

        userAdminStub.addUser(userName, password, roles, null, profileName);

    }

    public static ClaimValue[] toADBClaimValues(
            org.wso2.carbon.user.mgt.common.ClaimValue[] claimValues) {
        if (claimValues == null) {
            return new ClaimValue[0];
        }
        ClaimValue[] values = new ClaimValue[claimValues.length];
        for (org.wso2.carbon.user.mgt.common.ClaimValue cvalue : claimValues) {
            ClaimValue value = new ClaimValue();
            value.setClaimURI(cvalue.getClaimURI());
            value.setValue(cvalue.getValue());
        }
        return values;
    }

    public void deleteRole(String roleName) throws Exception {
        FlaggedName[] existingRoles;
        try {
            userAdminStub.deleteRole(roleName);
            existingRoles = userAdminStub.getAllRolesNames();

            for (FlaggedName existingRole : existingRoles) {
                if (roleName.equals(existingRole.getItemName())) {
                    assert false : "Deleted role still exists..";
                }
            }
        } catch (RemoteException e) {
            handleException("Failed to get all roles", e);
        } catch (GetAllRolesNamesUserAdminExceptionException e) {
            handleException("Failed to get all role names", e);
        } catch (Exception e) {
            handleException("Failed to delete role", e);
        }
    }

    public void deleteUser(String userName) throws Exception {

        String[] userList;
        try {
            userAdminStub.deleteUser(userName);
            userList = userAdminStub.listUsers(userName);
            assert (userList == null || userList.length == 0);
        } catch (RemoteException e) {
            handleException("Failed to list users", e);
        } catch (ListUsersUserAdminExceptionException e) {
            handleException("Failed to list users", e);
        } catch (DeleteUserUserAdminExceptionException e) {
            if (e.getFaultMessage().isUserAdminExceptionSpecified()) {
                handleException(e.getFaultMessage().getUserAdminException().getErrorMessage(), e);
            }
            handleException("Failed to delete user", e);
        }
    }

    private void addRoleWithUser(String roleName, String userName)
            throws Exception {
        userAdminStub.addRole(roleName, new String[]{userName}, null);
        FlaggedName[] roles = userAdminStub.getAllRolesNames();
        for (FlaggedName role : roles) {
            if (!role.getItemName().equals(roleName)) {
                continue;
            } else {
                assert (role.getItemName().equals(roleName));
            }
            assert false : "Role: " + roleName + " was not added properly.";
        }
    }


    protected void handleException(String msg, Exception e) throws Exception {
        log.error(msg, e);
        throw new Exception(msg + ": " + e);
    }

    public void updateUserListOfRole(String roleName, String[] addingUsers,
                                     String[] deletingUsers) throws Exception {
        List<FlaggedName> updatedUserList = new ArrayList<FlaggedName>();
        if (addingUsers != null) {
            for (String addUser : addingUsers) {
                FlaggedName fName = new FlaggedName();
                fName.setItemName(addUser);
                fName.setSelected(true);
                updatedUserList.add(fName);
            }
        }
        //add deleted users to the list
        if (deletingUsers != null) {
            for (String deletedUser : deletingUsers) {
                FlaggedName fName = new FlaggedName();
                fName.setItemName(deletedUser);
                fName.setSelected(false);
                updatedUserList.add(fName);
            }
        }
        //call userAdminStub to update user list of role
        try {
            userAdminStub.updateUsersOfRole(roleName, updatedUserList.toArray(
                    new FlaggedName[updatedUserList.size()]));


            //if delete users in retrieved list, fail
            if (deletingUsers != null) {
                for (String deletedUser : deletingUsers) {
                    FlaggedName[] verifyingList;
                    verifyingList = userAdminStub.getUsersOfRole(roleName, deletedUser);

                    assert (!verifyingList[0].getSelected());
                }
            }

            if (addingUsers != null) {
                //if all added users are not in list fail
                for (String addingUser : addingUsers) {
                    FlaggedName[] verifyingList = userAdminStub.getUsersOfRole(roleName, addingUser);
                    assert (verifyingList[0].getSelected());
                }
            }
        } catch (UpdateUsersOfRoleUserAdminExceptionException e) {
            handleException("Failed to update users of role", e);
        } catch (RemoteException e1) {
            handleException("Failed to update role", e1);
        } catch (GetUsersOfRoleUserAdminExceptionException e1) {
            handleException("Failed to retrieve user of role", e1);
        }
    }

    public boolean roleNameExists(String roleName)
            throws RemoteException, GetAllRolesNamesUserAdminExceptionException {
        FlaggedName[] roles = new FlaggedName[0];
        try {
            roles = userAdminStub.getAllRolesNames();
        } catch (RemoteException e) {
            log.error("Unable to get role names list", e);
            throw new RemoteException("Unable to get role names list", e);
        } catch (GetAllRolesNamesUserAdminExceptionException e) {
            log.error("Unable to get role names list", e);
            throw new GetAllRolesNamesUserAdminExceptionException("Unable to get role names list", e);
        }

        for (FlaggedName role : roles) {
            if (role.getItemName().equals(roleName)) {
                log.info("Role name " + roleName + " already exists");
                return true;
            }
        }
        return false;
    }

    public boolean userNameExists(String roleName, String userName)
            throws RemoteException, GetAllRolesNamesUserAdminExceptionException {
        FlaggedName[] users = new FlaggedName[0];
        try {
            users = userAdminStub.getUsersOfRole(roleName, "*");
        } catch (RemoteException e) {
            log.error("Unable to get user names list");
            throw new RemoteException("Unable to get user names list");
        } catch (GetUsersOfRoleUserAdminExceptionException e) {
            log.error("Unable to get user names list");
            throw new GetAllRolesNamesUserAdminExceptionException("Unable to get user names list");
        }
        for (FlaggedName user : users) {
            if (user.getItemName().equals(userName)) {
                log.info("User name " + userName + " already exists");
                return true;
            }
        }
        return false;
    }
}
