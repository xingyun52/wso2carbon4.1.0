/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.authorization.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.authorization.core.dto.PaginatedRoleDTO;
import org.wso2.carbon.identity.authorization.core.dto.PermissionDTO;
import org.wso2.carbon.identity.authorization.core.dto.PermissionModuleDTO;
import org.wso2.carbon.identity.authorization.core.internal.AuthorizationServiceComponent;
import org.wso2.carbon.identity.authorization.core.permission.PermissionFinder;
import org.wso2.carbon.identity.authorization.core.dto.PermissionTreeNodeDTO;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class AuthorizationAdminService {

    private static Log log = LogFactory.getLog(AuthorizationAdminService.class);

    /**
     * Returns names of all permission module that have been configured with Server.
     *
     * @return  String array of permission module names
     */
    public String[] getPermissionModules(){

        PermissionFinder finder = new PermissionFinder();
        return finder.getModuleNames();
    }

    /**
     * Returns filtered out root node names of a given module.
     *
     * @param moduleName  module name
     * @param filter filter value
     * @return  String array of root node names
     */
    public String[] getRootNodeNames(String moduleName,  String filter){

        PermissionFinder finder = new PermissionFinder();
        return finder.getRootNodes(moduleName, filter);
    }

    /**
     * Returns filtered out secondary root node names of a given module and a given root node. 
     *
     * @param moduleName  module name
     * @param root root node name
     * @param filter filter value
     * @return  String array of root node names
     */
    public String[] getRootSecondaryNodeNames(String moduleName, String root,  String filter){

        PermissionFinder finder = new PermissionFinder();
        return finder.getRootSecondaryNodes(moduleName, root, filter);
    }


    /**
     * Returns filtered out instance of <code>PermissionTreeNodeDTO</code> for a given module and
     * a given root node and a given secondary node
     *
     * @param moduleName module name
     * @param root root node name
     * @param secondaryRoot secondary node name
     * @param filter  filter value
     * @return instance of <code>PermissionTreeNodeDTO</code>
     */
    public PermissionTreeNodeDTO getPermissionTreeNode(String moduleName, String root,
                                                        String secondaryRoot,  String filter){

        PermissionFinder finder = new PermissionFinder();
        return finder.getPermissionTreeNodes(moduleName, root, secondaryRoot, filter);
    }

    /**
     * Returns module information for a given module
     *
     * @param moduleName module name
     * @return instance of <code>PermissionModuleDTO</code>
     */
    public PermissionModuleDTO getModuleInfo(String moduleName){

        PermissionFinder finder = new PermissionFinder();
        return finder.getModuleInfo(moduleName);
    }

    /**
     * Configure and persist permissions
     *
     * @param permissionDTOs Array of <code>PermissionDTO</code>
     * @param moduleName module name
     * @throws IdentityAuthorizationException if fails to configure and persist permission data
     */
    public void setPermissions(PermissionDTO[] permissionDTOs, String moduleName)
                                                            throws IdentityAuthorizationException {
        String subject;

        if(moduleName == null || moduleName.trim().length() == 0){
            log.error("Module name can not be null");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to " +
                                                                        "configure authorization");
        }

        PermissionFinder finder = new PermissionFinder();

        PermissionModuleDTO  moduleDTO = finder.getModuleInfo(moduleName);

        String rootId;

        if(moduleDTO == null){
            log.error("Can not find module for given module name");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to get authorization");
        } else {
            rootId = moduleDTO.getRootIdentifier();
        }        

        if(permissionDTOs != null){
            for(PermissionDTO dto : permissionDTOs){
                subject = dto.getSubject();
                if(subject == null || subject.trim().length() == 0){
                    log.error("Subject name can not be null");
                    throw new IdentityAuthorizationException("Not sufficient data has been provided to " +
                                                                                "configure authorization");
                }
                String action;
                if(dto.getAction() != null){
                    action = dto.getAction();
                } else {
                    if(log.isDebugEnabled()){
                        log.debug("Action can not be found.  Default action is set");
                    }
                    action = AuthorizationConstants.DEFAULT_ACTION;
                }

                if(dto.getResources() != null && dto.getResources().length > 0){
                    if(dto.getResources().length == 1){
                        IdentityAuthorizationManager.getInstance().setAuthorization(subject,
                                rootId + AuthorizationConstants.SEPARATOR + dto.getResources()[0],
                                action, dto.isAuthorized(), !dto.isUserPermission());
                    } else {
                        for(String resource : dto.getResources()){
                            IdentityAuthorizationManager.getInstance().setAuthorization(subject,
                                    rootId + AuthorizationConstants.SEPARATOR + resource,
                                    action, dto.isAuthorized(), !dto.isUserPermission());
                        }
                    }
                } else if(dto.getPermissionId() != null){
                    IdentityAuthorizationManager.getInstance().setAuthorization(subject,
                            rootId + AuthorizationConstants.SEPARATOR +  dto.getPermissionId() ,
                            action, dto.isAuthorized(), !dto.isUserPermission());
                } else {
                    log.error("Resource id can not be null");
                    throw new IdentityAuthorizationException("Not sufficient data has been provided to " +
                                                                                "configure authorization");
                }
            }
        } else {
            log.error("Null permission data has been provided");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to " +
                                                                        "configure authorization");
        }
    }

    /**
     * Returns explicitly defined user permission for user 
     *
     * @param userName  user name
     * @param moduleName module name
     * @return Array of <code>PermissionDTO</code>
     * @throws IdentityAuthorizationException if there is any error with permission retrieve
     */
    public  PermissionDTO[] getExplicitUserPermissions(String userName, String moduleName)
                                                            throws IdentityAuthorizationException {

        if(userName == null || userName.trim().length() == 0){
            log.error("User Name can not be null");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to get authorization");
        }

        if(moduleName == null || moduleName.trim().length() == 0){
            log.error("Module name can not be null");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to " +
                                                                        "configure authorization");
        }

        PermissionFinder finder = new PermissionFinder();

        PermissionModuleDTO  moduleDTO = finder.getModuleInfo(moduleName);

        if(moduleDTO == null){
            log.error("Can not find module for given module name");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to get authorization");
        }

        return  IdentityAuthorizationManager.getInstance().getUserPermission(userName,
                                                                    moduleDTO.getRootIdentifier());
    }

    /**
     * Clear permissions that are configured for a given user and a given module
     *
     * @param permissionDTOs  Array of <code>PermissionDTO</code>
     * @param moduleName module name
     * @throws IdentityAuthorizationException if fails to clear permission data
     */
    public  void clearUserPermissions(PermissionDTO[] permissionDTOs, String moduleName)
                                                            throws IdentityAuthorizationException {

        if(moduleName == null || moduleName.trim().length() == 0){
            log.error("Module name can not be null");
            throw new IdentityAuthorizationException("Module name can not be null");
        }

        if(permissionDTOs == null || permissionDTOs.length == 0){
            log.error("Null permission data has been provided");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to clear authorization");
        }

        PermissionFinder finder = new PermissionFinder();

        PermissionModuleDTO  moduleDTO = finder.getModuleInfo(moduleName);

        String rootId;

        if(moduleDTO == null){
            log.error("Can not find module for given module name");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to get authorization");
        } else {
            rootId = moduleDTO.getRootIdentifier();
        }

        for(PermissionDTO dto : permissionDTOs){
            if(dto.getSubject() != null){
                if(dto.getResources() != null && dto.getResources().length > 0){
                    if(dto.getResources().length == 1){
                        IdentityAuthorizationManager.getInstance().clearUserPermissions(dto.getSubject(),
                                rootId + AuthorizationConstants.SEPARATOR + dto.getResources()[0],
                                dto.getAction(), !dto.isUserPermission());
                    } else {
                        for(String resource : dto.getResources()){
                            IdentityAuthorizationManager.getInstance().clearUserPermissions(dto.getSubject(),
                                    rootId + AuthorizationConstants.SEPARATOR + resource,
                                    dto.getAction(), !dto.isUserPermission());
                        }
                    }
                } else if(dto.getPermissionId() != null){
                    IdentityAuthorizationManager.getInstance().clearUserPermissions(dto.getSubject(),
                            rootId + AuthorizationConstants.SEPARATOR + dto.getPermissionId(),
                            dto.getAction(), !dto.isUserPermission());
                } 
            } else {
                log.error("Subject can not be null");
                throw new IdentityAuthorizationException("Not sufficient data has been provided to " +
                        "clear authorization");
            }
        }
    }

    /**
     * Returns explicitly defined role permission for role 
     *
     * @param roleName  role name
     * @param moduleName module name
     * @return Array of <code>PermissionDTO</code>
     * @throws IdentityAuthorizationException if there is any error with permission retrieve
     */
    public  PermissionDTO[] getRolePermissions(String roleName, String moduleName)
                                                            throws IdentityAuthorizationException {

        if(roleName == null || roleName.trim().length() == 0){
            log.error("User Name can not be null");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to get authorization");
        }

        if(moduleName == null || moduleName.trim().length() == 0){
            log.error("Module name can not be null");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to " +
                                                                        "configure authorization");
        }

        PermissionFinder finder = new PermissionFinder();

        PermissionModuleDTO  moduleDTO = finder.getModuleInfo(moduleName);

        if(moduleDTO == null){
            log.error("Can not find module for given module name");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to get authorization");
        }

        return  IdentityAuthorizationManager.getInstance().getRolePermission(roleName,
                                                                    moduleDTO.getRootIdentifier());
    }

    /**
     * Returns all permission associated with the user
     *
     * @param userName user name
     * @param action action name
     * @param moduleName module name
     * @return Array of permit resources for user
     * @throws IdentityAuthorizationException if there is any error with permission retrieve
     */
    public String[] getAllPermitResourcesOfUser(String userName, String action, String moduleName)
                                                            throws IdentityAuthorizationException {

        if(userName == null || userName.trim().length() == 0){
            log.error("User Name can not be null");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to get authorization");
        }

        if(moduleName == null || moduleName.trim().length() == 0){
            log.error("Module name can not be null");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to " +
                                                                        "configure authorization");
        }

        PermissionFinder finder = new PermissionFinder();

        PermissionModuleDTO  moduleDTO = finder.getModuleInfo(moduleName);

        if(moduleDTO == null){
            log.error("Can not find module for given module name");
            throw new IdentityAuthorizationException("Not sufficient data has been provided to get authorization");
        }

        if(action == null || action.trim().length() == 0){
            if(log.isDebugEnabled()){
                log.debug("Action can not be found.  Default action is set");
            }
            action = AuthorizationConstants.DEFAULT_ACTION;
        }


        List<String>  resources = new ArrayList<String>();

        PermissionDTO[] permissionDTOs = IdentityAuthorizationManager.getInstance().getUserPermission(userName,
                                                                    moduleDTO.getRootIdentifier());
        for(PermissionDTO dto : permissionDTOs){
            if(action.equals(dto.getAction())){
                resources.add(dto.getPermissionId());
            }
        }

        String[] roles = getRoleListOfUser(userName);
        if(roles != null){
            for(String role : roles){
                PermissionDTO[] rolePermissionDTOs = IdentityAuthorizationManager.getInstance().
                                                getRolePermission(role, moduleDTO.getModuleName());
                for(PermissionDTO dto : rolePermissionDTOs){
                    if(action.equals(dto.getAction())){
                        resources.add(dto.getPermissionId());
                    }
                }
            }
        }

        return resources.toArray(new String[resources.size()]);
    }


    /**
     *
     * @param filter
     * @param pageNumber
     * @return
     * @throws IdentityAuthorizationException
     */
    public PaginatedRoleDTO getRoleList(String filter, int pageNumber)
                                                            throws IdentityAuthorizationException {

        int tenantId = CarbonContext.getCurrentContext().getTenantId();

        try{
            UserStoreManager userStoreManager = AuthorizationServiceComponent.getRealmService().
                                                getTenantUserRealm(tenantId).getUserStoreManager();
            String[] roleNames = userStoreManager.getRoleNames();

            List<String> filteredRoles = new ArrayList<String>();

            if(filter != null && filter.trim().length() > 0){
                for(String roleName : roleNames){
                    if(roleName.startsWith(filter)){
                        filteredRoles.add(roleName);
                    }
                }
                return doPaging(pageNumber, filteredRoles.toArray(new String[filteredRoles.size()]));
            } else {
                return doPaging(pageNumber, roleNames);
            }
        } catch (UserStoreException e) {
            log.error("Error while retrieving roles", e);
            throw new IdentityAuthorizationException("Error while retrieving roles");            
        }
    }

    /**
     * Return role list of user w.r.t under line user store
     *
     * @param userName user name
     * @return  String array of users
     * @throws IdentityAuthorizationException if fails to retrieve
     */
    private String[] getRoleListOfUser(String userName) throws IdentityAuthorizationException {

        int tenantId = CarbonContext.getCurrentContext().getTenantId();

        try{
            UserStoreManager userStoreManager = AuthorizationServiceComponent.getRealmService().
                                                getTenantUserRealm(tenantId).getUserStoreManager();
            return userStoreManager.getRoleListOfUser(userName);

        } catch (UserStoreException e) {
            log.error("Error while retrieving roles of user : " + userName, e);
            throw new IdentityAuthorizationException("Error while retrieving roles of user");            
        }
    }

    /**
	 * This method is used internally to do the pagination purposes.
	 *
	 * @param pageNumber page Number
	 * @param roleNames set of role names
	 * @return PaginatedPolicySetDTO object containing the number of pages and the set of roles
	 *         that reside in the given page.
	 */
	private PaginatedRoleDTO doPaging(int pageNumber, String[] roleNames) {

		PaginatedRoleDTO paginatedPolicySet = new PaginatedRoleDTO();
		if (roleNames.length == 0) {
			paginatedPolicySet.setRoleNames(new String[0]);
			return paginatedPolicySet;
		}
		String itemsPerPage = ServerConfiguration.getInstance().getFirstProperty("ItemsPerPage");
		int itemsPerPageInt = AuthorizationConstants.DEFAULT_ITEMS_PER_PAGE;
		if (itemsPerPage != null) {
			itemsPerPageInt = Integer.parseInt(itemsPerPage);
		}
		int numberOfPages = (int) Math.ceil((double) roleNames.length / itemsPerPageInt);
		if (pageNumber > numberOfPages - 1) {
			pageNumber = numberOfPages - 1;
		}
		int startIndex = pageNumber * itemsPerPageInt;
		int endIndex = (pageNumber + 1) * itemsPerPageInt;
		String[] returnedRoleSet = new String[itemsPerPageInt];

		for (int i = startIndex, j = 0; i < endIndex && i < roleNames.length; i++, j++) {
			returnedRoleSet[j] = roleNames[i];
		}

		paginatedPolicySet.setRoleNames(returnedRoleSet);
		paginatedPolicySet.setNumberOfPages(numberOfPages);

		return paginatedPolicySet;
	}


}
