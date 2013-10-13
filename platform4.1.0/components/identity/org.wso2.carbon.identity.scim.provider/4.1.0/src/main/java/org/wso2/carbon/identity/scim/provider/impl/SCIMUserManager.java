/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.scim.provider.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.identity.scim.common.impl.DefaultSCIMProvisioningHandler;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonConstants;
import org.wso2.charon.core.provisioning.ProvisioningHandler;
import org.wso2.carbon.identity.scim.common.config.SCIMProvisioningConfigManager;
import org.wso2.carbon.identity.scim.common.group.SCIMGroupHandler;
import org.wso2.carbon.identity.scim.common.utils.AttributeMapper;
import org.wso2.carbon.identity.scim.common.utils.IdentitySCIMException;
import org.wso2.carbon.identity.scim.common.utils.SCIMCommonUtils;
import org.wso2.carbon.user.api.Claim;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.charon.core.attributes.Attribute;
import org.wso2.charon.core.exceptions.CharonException;
import org.wso2.charon.core.exceptions.NotFoundException;
import org.wso2.charon.core.extensions.UserManager;
import org.wso2.charon.core.objects.Group;
import org.wso2.charon.core.objects.SCIMObject;
import org.wso2.charon.core.objects.User;
import org.wso2.charon.core.schema.SCIMConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SCIMUserManager implements UserManager {
    private UserStoreManager carbonUM = null;
    private ClaimManager carbonClaimManager = null;
    private String consumerName;

    private static Log log = LogFactory.getLog(SCIMUserManager.class);

    //variables used in runnable's run method in a particular instance of the object:
    private SCIMObject objectToBeProvisioned;
    private int provisioningMethod;

    //to make provisioning to other providers asynchronously happen.
    private ExecutorService provisioningThreadPool = Executors.newCachedThreadPool();

    SCIMProvisioningConfigManager provisioningConfigManager = SCIMProvisioningConfigManager.getInstance();

    public SCIMUserManager(UserStoreManager carbonUserStoreManager, String userName,
                           ClaimManager claimManager) {
        carbonUM = carbonUserStoreManager;
        consumerName = userName;
        carbonClaimManager = claimManager;
    }

    public User createUser(User user) throws CharonException {
        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {

            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            this.provisionSCIMOperation(SCIMConstants.POST, user, SCIMConstants.USER_INT, null);
            return user;

        } else {
            //else, persist in carbon user store
            if (log.isDebugEnabled()) {
                log.debug("Creating user: " + user.getUserName());
            }
            /*set thread local property to signal the downstream SCIMUserOperationListener
            about the provisioning route.*/
            SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
            Map<String, String> claimsMap = AttributeMapper.getClaimsMap(user);

            /*skip groups attribute since we map groups attribute to actual groups in ldap.
            and do not update it as an attribute in user schema*/
            if (claimsMap.containsKey(SCIMConstants.GROUPS_URI)) {
                claimsMap.remove(SCIMConstants.GROUPS_URI);
            }

            //TODO: Do not accept the roles list - it is read only.
            try {
                carbonUM.addUser(user.getUserName(), user.getPassword(), null, claimsMap, null);
                log.info("User: " + user.getUserName() + " is created through SCIM.");

            } catch (UserStoreException e) {
                throw new CharonException("Error in adding the user: " + user.getUserName() +
                                          " to the user store..", e);
            }
            return user;
        }
    }

    public User getUser(String userId) throws CharonException {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving user: " + userId);
        }
        User scimUser = null;
        try {
            //get the user name of the user with this id
            String[] userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                                                      UserCoreConstants.DEFAULT_PROFILE);

            if (userNames == null && userNames.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("User with SCIM id: " + userId + " does not exist in the system.");
                }
                return null;
            } else if (userNames != null && userNames.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("User with SCIM id: " + userId + " does not exist in the system.");
                }
                return null;
            } else {
                //we assume (since id is unique per user) only one user exists for a given id
                scimUser = this.getSCIMUser(userNames[0]);

                log.info("User: " + scimUser.getUserName() + " is retrieved through SCIM.");
            }

        } catch (UserStoreException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for" +
                                      "user: " + userId);
        }
        return scimUser;
    }

    public List<User> listUsers() throws CharonException {
        List<User> users = new ArrayList();
        try {
            String[] userNames = carbonUM.listUsers("*", -1);
            if (userNames != null && userNames.length != 0) {
                for (String userName : userNames) {
                    if (CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(userName)) {
                        continue;
                    }
                    User scimUser = this.getSCIMUser(userName);
                    //if SCIM-ID is not present in the attributes, skip
                    if (scimUser.getId() == null) {
                        continue;
                    }
                    Map<String, Attribute> attrMap = scimUser.getAttributeList();
                    if (attrMap != null && !attrMap.isEmpty()) {
                        users.add(scimUser);
                    }
                }
            } else {
                return null;
            }
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            throw new CharonException("Error while retrieving users from user store..");
        }
        return users;
    }

    public List<User> listUsersByAttribute(Attribute attribute) {
        return null;
    }

    public List<User> listUsersByFilter(String attributeName, String filterOperation,
                                        String attributeValue) throws CharonException {
        //since we only support eq filter operation at the moment, no need to check for that.
        if (log.isDebugEnabled()) {
            log.debug("Listing users by filter: " + attributeName + filterOperation +
                      attributeValue);
        }
        List<User> filteredUsers = new ArrayList<User>();
        User scimUser = null;
        try {
            //get the user name of the user with this id
            String[] userNames = carbonUM.getUserList(attributeName, attributeValue,
                                                      UserCoreConstants.DEFAULT_PROFILE);

            if (userNames == null || userNames.length == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Users with filter: " + attributeName + filterOperation +
                              attributeValue + " does not exist in the system.");
                }
                return null;
            } else {
                for (String userName : userNames) {
                    if (CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(userName)) {
                        continue;
                    }
                    scimUser = this.getSCIMUser(userName);
                    //if SCIM-ID is not present in the attributes, skip
                    if (scimUser.getId() == null) {
                        continue;
                    }
                    filteredUsers.add(scimUser);

                }
                log.info("Users filtered through SCIM for the filter: " + attributeName + filterOperation +
                         attributeValue);
            }

        } catch (UserStoreException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for" +
                                      "users:" + attributeValue);
        }
        return filteredUsers;
    }

    public List<User> listUsersBySort(String s, String s1) {
        return null;
    }

    public List<User> listUsersWithPagination(int i, int i1) {
        return null;
    }

    public User updateUser(User user) throws CharonException {
        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {

            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            this.provisionSCIMOperation(SCIMConstants.PUT, user, SCIMConstants.USER_INT, null);
            return user;

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Updating user: " + user.getUserName());
            }
            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                //get user claim values
                Map<String, String> claims = AttributeMapper.getClaimsMap(user);

                //check if username of the updating user existing in the userstore.
                //TODO:immutable userId can be something else other than username. eg: mail.
                //Therefore, correct way is to check the corresponding SCIM attribute for the
                //UserNameAttribute of user-mgt.xml.
                // Refer: SCIMUserOperationListener#isProvisioningActionAuthorized method.
                if (!carbonUM.isExistingUser(user.getUserName())) {
                    throw new CharonException("User name is immutable in carbon user store.");
                }

                /*skip groups attribute since we map groups attribute to actual groups in ldap.
                and do not update it as an attribute in user schema*/
                if (claims.containsKey(SCIMConstants.GROUPS_URI)) {
                    claims.remove(SCIMConstants.GROUPS_URI);
                }

                //set user claim values
                carbonUM.setUserClaimValues(user.getUserName(), claims, null);
                //if password is updated, set it separately
                if (user.getPassword() != null) {
                    carbonUM.updateCredentialByAdmin(user.getUserName(), user.getPassword());
                }
                log.info("User: " + user.getUserName() + " updated updated through SCIM.");
            } catch (org.wso2.carbon.user.core.UserStoreException e) {
                throw new CharonException("Error while updating attributes of user: " + user.getUserName());
            }

            return user;
        }
    }

    public User updateUser(List<Attribute> attributes) {
        return null;
    }

    public void deleteUser(String userId) throws NotFoundException, CharonException {
        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {
            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            User user = new User();
            user.setUserName(userId);
            this.provisionSCIMOperation(SCIMConstants.DELETE, user, SCIMConstants.USER_INT, null);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Deleting user: " + userId);
            }
            //get the user name of the user with this id
            String[] userNames = null;
            String userName = null;
            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                                                 UserCoreConstants.DEFAULT_PROFILE);
                if (userNames == null && userNames.length == 0) {
                    //resource with given id not found
                    if (log.isDebugEnabled()) {
                        log.debug("User with id: " + userId + " not found.");
                    }
                    throw new NotFoundException();
                } else if (userNames != null && userNames.length == 0) {
                    //resource with given id not found
                    if (log.isDebugEnabled()) {
                        log.debug("User with id: " + userId + " not found.");
                    }
                    throw new NotFoundException();
                } else {
                    //we assume (since id is unique per user) only one user exists for a given id
                    userName = userNames[0];
                    carbonUM.deleteUser(userName);
                    log.info("User: " + userName + " is deleted through SCIM.");
                }

            } catch (org.wso2.carbon.user.core.UserStoreException e) {
                throw new CharonException("Error in deleting user: " + userName);
            }
        }
    }

    public Group createGroup(Group group) throws CharonException {
        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {
            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            this.provisionSCIMOperation(SCIMConstants.POST, group, SCIMConstants.GROUP_INT, null);
            return group;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Creating group: " + group.getDisplayName());
            }
            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                /*if members are sent when creating the group, check whether users already exist in the
                user store*/
                List<String> userIds = group.getMembers();
                List<String> userDisplayNames = group.getMembersWithDisplayName();
                if (userIds != null && userIds.size() != 0) {
                    List<String> members = new ArrayList<String>();
                    for (String userId : userIds) {
                        String[] userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                                                                  UserCoreConstants.DEFAULT_PROFILE);
                        if (userNames == null || userNames.length == 0) {
                            String error = "User: " + userId + " doesn't exist in the user store. " +
                                           "Hence, can not create the group: " + group.getDisplayName();
                            throw new IdentitySCIMException(error);
                        } else {
                            members.add(userNames[0]);
                            if (userDisplayNames != null && userDisplayNames.size() != 0 &&
                                !userDisplayNames.contains(userNames[0])) {
                                throw new IdentitySCIMException("Given SCIM user Id and name not matching..");
                            }
                        }
                    }

                    //add other scim attributes in the identity DB since user store doesn't support some attributes.
                    SCIMGroupHandler scimGroupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
                    scimGroupHandler.createSCIMAttributes(group);
                    carbonUM.addRole(group.getDisplayName(), members.toArray(new String[members.size()]), null);
                    log.info("Group: " + group.getDisplayName() + " is created through SCIM.");
                } else {
                    //add other scim attributes in the identity DB since user store doesn't support some attributes.
                    SCIMGroupHandler scimGroupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
                    scimGroupHandler.createSCIMAttributes(group);
                    carbonUM.addRole(group.getDisplayName(), null, null);
                    log.info("Group: " + group.getDisplayName() + " is created through SCIM.");
                }
            } catch (UserStoreException e) {
                throw new CharonException(e.getMessage());
            } catch (IdentitySCIMException e) {
                throw new CharonException(e.getMessage());
            }
            //TODO:after the group is added, read it from user store and return
            return group;
        }
    }

    public Group getGroup(String id) throws CharonException {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving group with id: " + id);
        }
        Group group = null;
        try {
            SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
            //get group name by Id
            String groupName = groupHandler.getGroupName(id);

            if (groupName != null) {
                group = getGroupWithName(groupName);
            } else {
                //returning null will send a resource not found error to client by Charon.
                return null;
            }
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            throw new CharonException("Error in retrieving group: " + id);
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in retrieving SCIM Group information from database.");
        }
        return group;
    }

    public List<Group> listGroups() throws CharonException {
        List<Group> groupList = new ArrayList<Group>();
        try {
            String[] roleNames = carbonUM.getRoleNames();
            //remove everyone and wso2anonymous role
            if (roleNames != null && roleNames.length != 0) {
                for (String roleName : roleNames) {
                    //skip internal roles
                    if ((CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME.equals(roleName)) ||
                        (((AbstractUserStoreManager) carbonUM).getEveryOneRoleName().equals(roleName)) ||
                        (((AbstractUserStoreManager) carbonUM).getAdminRoleName().equals(roleName))) {
                        continue;
                    }
                    groupList.add(this.getGroupWithName(roleName));
                }
            } else {
                return null;
            }
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            throw new CharonException("Error in obtaining role names from user store.");
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in retrieving SCIM Group information from database.");
        }
        return groupList;
    }

    public List<Group> listGroupsByAttribute(Attribute attribute) throws CharonException {
        return null;
    }

    public List<Group> listGroupsByFilter(String filterAttribute, String filterOperation,
                                          String attributeValue) throws CharonException {
        //since we only support eq filter operation, no need to check for that.
        if (log.isDebugEnabled()) {
            log.debug("Listing groups with filter: " + filterAttribute + filterOperation +
                      attributeValue);
        }
        List<Group> filteredGroups = new ArrayList<Group>();
        Group group = null;
        try {
            if (attributeValue != null && carbonUM.isExistingRole(attributeValue)) {
                //skip internal roles
                if ((CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME.equals(attributeValue)) ||
                    (((AbstractUserStoreManager) carbonUM).getEveryOneRoleName().equals(attributeValue)) ||
                    (((AbstractUserStoreManager) carbonUM).getAdminRoleName().equals(attributeValue))) {
                    throw new IdentitySCIMException("Internal roles do not support SCIM.");
                }
                //we expect only one result
                group = getGroupWithName(attributeValue);
                filteredGroups.add(group);
            } else {
                //returning null will send a resource not found error to client by Charon.
                return null;
            }
        } catch (org.wso2.carbon.user.core.UserStoreException e) {
            throw new CharonException("Error in filtering group with filter: " + filterAttribute +
                                      filterOperation + attributeValue);
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in retrieving SCIM Group information from database.");
        }
        return filteredGroups;
    }

    public List<Group> listGroupsBySort(String s, String s1) throws CharonException {
        return null;
    }

    public List<Group> listGroupsWithPagination(int i, int i1) {
        return null;
    }

    public Group updateGroup(Group oldGroup, Group newGroup) throws CharonException {
        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {
            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            //add old role name details.
            Map<String, Object> additionalInformation = new HashMap<String, Object>();
            additionalInformation.put(SCIMCommonConstants.IS_ROLE_NAME_CHANGED_ON_UPDATE, true);
            additionalInformation.put(SCIMCommonConstants.OLD_GROUP_NAME, oldGroup.getDisplayName());

            this.provisionSCIMOperation(SCIMConstants.PUT, newGroup, SCIMConstants.GROUP_INT,
                                        additionalInformation);
            return newGroup;

        } else {
            if (log.isDebugEnabled()) {
                log.debug("Updating group: " + oldGroup.getDisplayName());
            }
            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);

                boolean updated = false;
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);
                //check if the user ids sent in updated group exist in the user store and the associated user name
                //also a matching one.
                List<String> userIds = newGroup.getMembers();
                List<String> userDisplayNames = newGroup.getMembersWithDisplayName();
                if (userIds != null && userIds.size() != 0) {
                    String[] userNames = null;
                    for (String userId : userIds) {
                        userNames = carbonUM.getUserList(SCIMConstants.ID_URI, userId,
                                                         UserCoreConstants.DEFAULT_PROFILE);
                        if (userNames == null || userNames.length == 0) {
                            String error = "User: " + userId + " doesn't exist in the user store. " +
                                           "Hence, can not update the group: " + oldGroup.getDisplayName();
                            throw new IdentitySCIMException(error);
                        } else {
                            if (!userDisplayNames.contains(userNames[0])) {
                                throw new IdentitySCIMException("Given SCIM user Id and name not matching..");
                            }
                        }
                    }
                }

                SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());

                //update name if it is changed
                if (!(oldGroup.getDisplayName().equals(newGroup.getDisplayName()))) {
                    //update group name in carbon UM
                    carbonUM.updateRoleName(oldGroup.getDisplayName(), newGroup.getDisplayName());
                    //update group name in SCIM_DB
                    groupHandler.updateRoleName(oldGroup.getDisplayName(), newGroup.getDisplayName());
                    updated = true;
                }

                //find out added members and deleted members..
                List<String> oldMembers = oldGroup.getMembersWithDisplayName();
                List<String> newMembers = newGroup.getMembersWithDisplayName();

                List<String> addedMembers = new ArrayList<String>();
                List<String> deletedMembers = new ArrayList<String>();

                //check for deleted members
                if (oldMembers != null && oldMembers.size() != 0) {
                    for (String oldMember : oldMembers) {
                        if (newMembers != null && newMembers.contains(oldMember)) {
                            continue;
                        }
                        deletedMembers.add(oldMember);
                    }
                }

                //check for added members
                if (newMembers != null && newMembers.size() != 0) {
                    for (String newMember : newMembers) {
                        if (oldMembers != null && oldMembers.contains(newMember)) {
                            continue;
                        }
                        addedMembers.add(newMember);
                    }
                }

                if (addedMembers.size() != 0 || deletedMembers.size() != 0) {
                    carbonUM.updateUserListOfRole(newGroup.getDisplayName(),
                                                  deletedMembers.toArray(new String[deletedMembers.size()]),
                                                  addedMembers.toArray(new String[addedMembers.size()]));
                    updated = true;
                }


                if (updated) {
                    log.info("Group: " + newGroup.getDisplayName() + " is updated through SCIM.");
                } else {
                    log.warn("There is no updated field in the group: " + oldGroup.getDisplayName() +
                             ". Therefore ignoring the provisioning.");
                }

            } catch (UserStoreException e) {
                throw new CharonException(e.getMessage());
            } catch (IdentitySCIMException e) {
                throw new CharonException(e.getMessage());
            }
            return newGroup;
        }
    }

    public Group updateGroup(List<Attribute> attributes) throws CharonException {
        return null;
    }

    public void deleteGroup(String groupId) throws NotFoundException, CharonException {
        //if operating in dumb mode, do not persist the operation, only provision to providers
        if (provisioningConfigManager.isDumbMode()) {
            if (log.isDebugEnabled()) {
                log.debug("This instance is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            Group group = new Group();
            group.setDisplayName(groupId);
            this.provisionSCIMOperation(SCIMConstants.DELETE, group, SCIMConstants.GROUP_INT, null);    
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Deleting group: " + groupId);
            }
            try {
                /*set thread local property to signal the downstream SCIMUserOperationListener
                about the provisioning route.*/
                SCIMCommonUtils.setThreadLocalIsManagedThroughSCIMEP(true);

                //get group name by id
                SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
                String groupName = groupHandler.getGroupName(groupId);

                if (groupName != null) {
                    //delete group in carbon UM
                    carbonUM.deleteRole(groupName);

                    //delete scim specific attributes stored in identity scim db
                    groupHandler.deleteGroupAttributes(groupName);

                    log.info("Group: " + groupName + " is deleted through SCIM.");

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Group with SCIM id: " + groupId + " doesn't exist in the system.");
                    }
                    throw new NotFoundException();
                }
            } catch (UserStoreException e) {
                throw new CharonException(e.getMessage());
            } catch (IdentitySCIMException e) {
                throw new CharonException(e.getMessage());
            }
        }
    }

    private User getSCIMUser(String userName) throws CharonException {
        User scimUser = null;
        try {
            //get claims related to SCIM claim dialect
            Claim[] claims = carbonClaimManager.getAllClaims(SCIMCommonUtils.SCIM_CLAIM_DIALECT);

            List<String> claimURIList = new ArrayList<String>();
            for (Claim claim : claims) {
                claimURIList.add(claim.getClaimUri());
            }
            //obtain user claim values
            Map<String, String> attributes = carbonUM.getUserClaimValues(
                    userName, claimURIList.toArray(new String[claimURIList.size()]), null);
            //skip simple type addresses claim coz it is complex with sub types in the schema
            if (attributes.containsKey(SCIMConstants.ADDRESSES_URI)) {
                attributes.remove(SCIMConstants.ADDRESSES_URI);
            }
            //get groups of user and add it as groups attribute
            String[] roles = carbonUM.getRoleListOfUser(userName);
            //construct the SCIM Object from the attributes
            scimUser = (User) AttributeMapper.constructSCIMObjectFromAttributes(
                    attributes, SCIMConstants.USER_INT);
            //add groups of user:
            for (String role : roles) {
                String everyOneRoleName =
                        ((AbstractUserStoreManager) carbonUM).getEveryOneRoleName();
                String adminRoleName =
                        ((AbstractUserStoreManager) carbonUM).getAdminRoleName();
                if (everyOneRoleName.equals(role) || adminRoleName.equals(role) ||
                    CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME.equals(role)) {
                    //carbon specific roles do not possess SCIM info, hence skipping them.
                    continue;
                }
                Group group = getGroupOnlyWithMetaAttributes(role);
                scimUser.setGroup(null, group.getId(), role);
            }
        } catch (UserStoreException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for" +
                                      "user: " + userName);
        } catch (CharonException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for" +
                                      "user: " + userName);
        } catch (NotFoundException e) {
            throw new CharonException("Error in getting user information from Carbon User Store for" +
                                      "user: " + userName);
        } catch (IdentitySCIMException e) {
            throw new CharonException("Error in getting group information from Identity DB for" +
                                      "user: " + userName);
        }
        return scimUser;
    }

    /**
     * Get the full group with all the details including users.
     *
     * @param groupName
     * @return
     * @throws CharonException
     * @throws org.wso2.carbon.user.core.UserStoreException
     *
     * @throws IdentitySCIMException
     */
    private Group getGroupWithName(String groupName)
            throws CharonException, org.wso2.carbon.user.core.UserStoreException,
                   IdentitySCIMException {
        Group group = new Group();
        group.setDisplayName(groupName);
        String[] userNames = carbonUM.getUserListOfRole(groupName);

        //get the ids of the users and set them in the group with id + display name
        if (userNames != null && userNames.length != 0) {
            for (String userName : userNames) {
                User user = this.getSCIMUser(userName);
                if (user != null) {
                    group.setMember(user.getId(), userName);
                }
            }
        }
        //get other group attributes and set.
        SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
        group = groupHandler.getGroupWithAttributes(group, groupName);
        return group;
    }

    /**
     * Get group with only meta attributes.
     *
     * @param groupName
     * @return
     * @throws CharonException
     * @throws IdentitySCIMException
     * @throws org.wso2.carbon.user.core.UserStoreException
     *
     */
    private Group getGroupOnlyWithMetaAttributes(String groupName)
            throws CharonException, IdentitySCIMException,
                   org.wso2.carbon.user.core.UserStoreException {
        //get other group attributes and set.
        Group group = new Group();
        group.setDisplayName(groupName);
        SCIMGroupHandler groupHandler = new SCIMGroupHandler(carbonUM.getTenantId());
        group = groupHandler.getGroupWithAttributes(group, groupName);
        return group;
    }

    /**
     * Provision the SCIM operation received at SCIM endpoint. In SCIMUserOperationListener,
     * we authorize the user who is performing the provisioning operation. But here, we do not need to
     * authorize since it is already done when obtaining the user manager instance.
     *
     * @param provisioningMethod
     * @param provisioningObject
     * @param provisioningObjectType
     * @throws CharonException
     */
    private void provisionSCIMOperation(int provisioningMethod, SCIMObject provisioningObject,
                                        int provisioningObjectType, Map<String, Object> properties)
            throws CharonException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Server is operating in dumb mode. " +
                          "Hence, operation is not persisted, it will only be provisioned.");
            }
            //read the connectors
            String[] provisioningHandlers = provisioningConfigManager.getProvisioningHandlers();
            if (provisioningHandlers != null && provisioningHandlers.length != 0) {
                //iterate configured set of connectors, initialize them, set properties and provision
                for (String provisioningHandler : provisioningHandlers) {
                    Class provisioningClass = Class.forName(provisioningHandler);
                    ProvisioningHandler provisioningAgent = (ProvisioningHandler) provisioningClass.newInstance();
                    provisioningAgent.setProvisioningConsumer(consumerName);
                    provisioningAgent.setProvisioningMethod(provisioningMethod);
                    provisioningAgent.setProvisioningObject(provisioningObject);
                    provisioningAgent.setProvisioningObjectType(provisioningObjectType);
                    provisioningAgent.setProperties(properties);
                    provisioningThreadPool.submit(provisioningAgent);
                }
            } else {
                throw new CharonException("Server is operating in dumb mode, " +
                                          "but no provisioning connectors are registered.");
            }
        } catch (ClassNotFoundException e) {
            throw new CharonException("Error in initializing provisioning handler", e);
        } catch (InstantiationException e) {
            throw new CharonException("Error in initializing provisioning handler", e);
        } catch (IllegalAccessException e) {
            throw new CharonException("Error in initializing provisioning handler", e);
        }
    }
}