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

package org.wso2.carbon.apimgt.impl;

import org.apache.axis2.AxisFault;
import org.wso2.carbon.apimgt.api.APIConsumer;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.api.model.Tag;
import org.wso2.carbon.apimgt.handlers.security.stub.types.APIKeyMapping;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.utils.APIAuthenticationAdminClient;
import org.wso2.carbon.apimgt.impl.utils.APINameComparator;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.utils.APIVersionComparator;
import org.wso2.carbon.apimgt.impl.utils.RemoteAuthorizationManager;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.core.*;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.realm.RegistryAuthorizationManager;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides the core API store functionality. It is implemented in a very
 * self-contained and 'pure' manner, without taking requirements like security into account,
 * which are subject to frequent change. Due to this 'pure' nature and the significance of
 * the class to the overall API management functionality, the visibility of the class has
 * been reduced to package level. This means we can still use it for internal purposes and
 * possibly even extend it, but it's totally off the limits of the users. Users wishing to
 * programmatically access this functionality should use one of the extensions of this
 * class which is visible to them. These extensions may add additional features like
 * security to this class.
 */
class APIConsumerImpl extends AbstractAPIManager implements APIConsumer {

    public APIConsumerImpl() throws APIManagementException {
        super();
    }

    public APIConsumerImpl(String username) throws APIManagementException {
        super(username);
    }

    public Subscriber getSubscriber(String subscriberId) throws APIManagementException {
        Subscriber subscriber = null;
        try {
            subscriber = apiMgtDAO.getSubscriber(subscriberId);
        } catch (APIManagementException e) {
            handleException("Failed to get Subscriber", e);
        }
        return subscriber;
    }

    public Set<API> getAPIsWithTag(String tag) throws APIManagementException {
        Set<API> apiSet = new TreeSet<API>(new APINameComparator());
        try {
            String resourceByTagQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/resource-by-tag";
            Map<String, String> params = new HashMap<String, String>();
            params.put("1", tag);
            params.put(RegistryConstants.RESULT_TYPE_PROPERTY_NAME, RegistryConstants.RESOURCE_UUID_RESULT_TYPE);
            Collection collection = registry.executeQuery(resourceByTagQueryPath, params);

            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                                                                                APIConstants.API_KEY);

            for (String row : collection.getChildren()) {
                String uuid = row.substring(row.indexOf(";") + 1, row.length());
                GenericArtifact genericArtifact = artifactManager.getGenericArtifact(uuid);
                apiSet.add(APIUtil.getAPI(genericArtifact));
            }
//            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,APIConstants.API_KEY);
//            GenericArtifact[] genericArtifacts = artifactManager.getAllGenericArtifacts();
//            if (genericArtifacts == null || genericArtifacts.length == 0) {
//                return apiSet;
//            }
//            for (GenericArtifact artifact : genericArtifacts) {
//                String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);
//                if (!status.equals(APIConstants.PUBLISHED)) {
//                    continue;
//                }
//                String artifactPath = artifact.getPath();
//                org.wso2.carbon.registry.core.Tag[] tags = registry.getTags(artifactPath);
//                if (tags == null || tags.length == 0) {
//                    continue;
//                }
//                for (org.wso2.carbon.registry.core.Tag tag1 : tags) {
//                    if (tag.equals(tag1.getTagName())) {
//                        apiSet.add(APIUtil.getAPI(artifact, registry));
//                        break;
//                    }
//                }
//            }
        } catch (RegistryException e) {
            handleException("Failed to get API for tag " + tag, e);
        }
        return apiSet;
    }

    public Set<API> getAllPublishedAPIs() throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        try {
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            GenericArtifact[] genericArtifacts = artifactManager.getAllGenericArtifacts();
            if (genericArtifacts == null || genericArtifacts.length == 0) {
                return apiSortedSet;
            }
            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean allowMultipleVersions=isAllowDisplayMultipleVersions();
            Boolean showAllAPIs=isAllowDisplayAllAPIs();
            for (GenericArtifact artifact : genericArtifacts) {
                // adding the API provider can mark the latest API .
                String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                API api = null;
                //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                if (!showAllAPIs) {
                    // then we are only interested in published APIs here...
                    if (status.equals(APIConstants.PUBLISHED)) {
                        api = APIUtil.getAPI(artifact);
                    }
                } else {   // else we are interested in both deprecated/published APIs here...
                    if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                        api = APIUtil.getAPI(artifact);

                    }

                }
                if (api != null) {
                    String key;
                    //Check the configuration to allow showing multiple versions of an API true/false
                    if (!allowMultipleVersions) { //If allow only showing the latest version of an API
                        key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                        API existingAPI = latestPublishedAPIs.get(key);
                        if (existingAPI != null) {
                            // If we have already seen an API with the same name, make sure
                            // this one has a higher version number
                            if (versionComparator.compare(api, existingAPI) > 0) {
                                latestPublishedAPIs.put(key, api);
                            }
                        } else {
                            // We haven't seen this API before
                            latestPublishedAPIs.put(key, api);
                        }
                    } else { //If allow showing multiple versions of an API
                        key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                .getVersion();
                        multiVersionedAPIs.add(api);
                    }
                }

            }
            if (!allowMultipleVersions) {
                for (API api : latestPublishedAPIs.values()) {
                    apiSortedSet.add(api);
                }
                return apiSortedSet;
            } else {
                for (API api : multiVersionedAPIs) {
                    apiVersionsSortedSet.add(api);
                }
                return apiVersionsSortedSet;
            }


        } catch (RegistryException e) {
            handleException("Failed to get all publishers", e);
            return null;
        }

    }


    public Set<API> getTopRatedAPIs(int limit) throws APIManagementException {
        int returnLimit = 0;
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        try {
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            GenericArtifact[] genericArtifacts = artifactManager.getAllGenericArtifacts();
            if (genericArtifacts == null || genericArtifacts.length == 0) {
                return apiSortedSet;
            }
            for (GenericArtifact genericArtifact : genericArtifacts) {
                String status = genericArtifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);
                if (status.equals(APIConstants.PUBLISHED)) {
                    String artifactPath = genericArtifact.getPath();

                    float rating = registry.getAverageRating(artifactPath);
                    if (rating > APIConstants.TOP_TATE_MARGIN && (returnLimit < limit)) {
                        returnLimit++;
                        apiSortedSet.add(APIUtil.getAPI(genericArtifact, registry));
                    }
                }
            }
        } catch (RegistryException e) {
            handleException("Failed to get top rated API", e);
        }
        return apiSortedSet;
    }

    public Set<API> getRecentlyAddedAPIs(int limit) throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());

        try {

            Boolean allowMultipleVersions = isAllowDisplayMultipleVersions();
            Boolean showAllAPIs = isAllowDisplayAllAPIs();
            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();

            String latestAPIQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/latest-apis";
            Map<String, String> params = new HashMap<String, String>();
            params.put(RegistryConstants.RESULT_TYPE_PROPERTY_NAME, RegistryConstants.RESOURCES_RESULT_TYPE);
            Collection collection = registry.executeQuery(latestAPIQueryPath, params);
            int resultSetSize = Math.min(limit, collection.getChildCount());
            String[] recentlyAddedAPIPaths = new String[resultSetSize];
            for (int i = 0; i < resultSetSize; i++) {
                recentlyAddedAPIPaths[i] = collection.getChildren()[i];
            }

            //Find UUID
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                                                                                APIConstants.API_KEY);
            for (int a = 0; a < recentlyAddedAPIPaths.length; a++) {
                Resource resource = registry.get(recentlyAddedAPIPaths[a]);
                if (resource != null) {
                    GenericArtifact genericArtifact = artifactManager.getGenericArtifact(resource.getUUID());
                    API api = null;
                    String status = genericArtifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);
                    //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                    if (!showAllAPIs) {
                        // then we are only interested in published APIs here...
                        if (status.equals(APIConstants.PUBLISHED)) {
                            api = APIUtil.getAPI(genericArtifact, registry);
                        }
                    } else {   // else we are interested in both deprecated/published APIs here...
                        if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                            api = APIUtil.getAPI(genericArtifact, registry);

                        }

                    }
                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!allowMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }

                }
            }
            if (!allowMultipleVersions) {
                for (API api : latestPublishedAPIs.values()) {
                    apiSortedSet.add(api);
                }
                return apiSortedSet;
            } else {
                for (API api : multiVersionedAPIs) {
                    apiVersionsSortedSet.add(api);
                }
                return apiVersionsSortedSet;
            }


        } catch (RegistryException e) {
            handleException("Failed to get recently added APIs", e);
            return null;
        }

    }

    public Set<Tag> getAllTags() throws APIManagementException {
        Set<Tag> tagSet = new TreeSet<Tag>(new Comparator<Tag>() {
            @Override
            public int compare(Tag o1, Tag o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        try {
            String tagsQueryPath = RegistryConstants.QUERIES_COLLECTION_PATH + "/tag-summary";
            Map<String, String> params = new HashMap<String, String>();
            params.put(RegistryConstants.RESULT_TYPE_PROPERTY_NAME, RegistryConstants.TAG_SUMMARY_RESULT_TYPE);
            Collection collection = registry.executeQuery(tagsQueryPath, params);
            for (String fullTag : collection.getChildren()) {
                //remove hardcoded path value
                String tagName = fullTag.substring(fullTag.indexOf(";") + 1, fullTag.indexOf(":"));
                String tagOccurenceCountStr = fullTag.substring(fullTag.indexOf(":") + 1, fullTag.length());
                int tagOccurenceCount = Integer.valueOf(tagOccurenceCountStr).intValue();
                tagSet.add(new Tag(tagName, tagOccurenceCount));
            }
        } catch (RegistryException e) {
            handleException("Failed to get all the tags", e);
        }
        return tagSet;
    }

    public void rateAPI(APIIdentifier apiId, APIRating rating,
                        String user) throws APIManagementException {
        String path = APIUtil.getAPIPath(apiId);
        try {
            registry.rateResource(path, rating.getRating());
        } catch (RegistryException e) {
            handleException("Failed to rate API : " + path, e);
        }
    }

    public int getUserRating(APIIdentifier apiId, String user) throws APIManagementException {
        int rating = -1;
        String path = APIUtil.getAPIPath(apiId);
        try {
            UserRegistry userRegistry = ServiceReferenceHolder.getInstance().
                    getRegistryService().getGovernanceUserRegistry(user);
            rating = userRegistry.getRating(path, user);
        } catch (RegistryException e) {
            handleException("Failed to get rating of user : " + user, e);
        }
        return rating;
    }

    public Set<API> getPublishedAPIsByProvider(String providerId, int limit)
            throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        try {
            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean allowMultipleVersions = isAllowDisplayMultipleVersions();
            Boolean showAllAPIs = isAllowDisplayAllAPIs();
            String providerPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                                  providerId;
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                                                                                APIConstants.API_KEY);
            Association[] associations = registry.getAssociations(providerPath,
                                                                  APIConstants.PROVIDER_ASSOCIATION);
            if (associations.length < limit || limit == -1) {
                limit = associations.length;
            }
            for (int i = 0; i < limit; i++) {
                Association association = associations[i];
                String apiPath = association.getDestinationPath();
                Resource resource = registry.get(apiPath);
                String apiArtifactId = resource.getUUID();
                if (apiArtifactId != null) {
                    GenericArtifact artifact = artifactManager.getGenericArtifact(apiArtifactId);
                    // check the API status
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api = null;
                    //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                    if (!showAllAPIs) {
                        // then we are only interested in published APIs here...
                        if (status.equals(APIConstants.PUBLISHED)) {
                            api = APIUtil.getAPI(artifact);
                        }
                    } else {   // else we are interested in both deprecated/published APIs here...
                        if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                            api = APIUtil.getAPI(artifact);

                        }

                    }
                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!allowMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                } else {
                    throw new GovernanceException("artifact id is null of " + apiPath);
                }
            }
            if (!allowMultipleVersions) {
                for (API api : latestPublishedAPIs.values()) {
                    apiSortedSet.add(api);
                }
                return apiSortedSet;
            } else {
                for (API api : multiVersionedAPIs) {
                    apiVersionsSortedSet.add(api);
                }
                return apiVersionsSortedSet;
            }

        } catch (RegistryException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        }


    }
    public Set<API> getPublishedAPIsByProvider(String providerId, String loggedUsername,int limit)
            throws APIManagementException {
        SortedSet<API> apiSortedSet = new TreeSet<API>(new APINameComparator());
        SortedSet<API> apiVersionsSortedSet = new TreeSet<API>(new APIVersionComparator());
        try {
            Map<String, API> latestPublishedAPIs = new HashMap<String, API>();
            List<API> multiVersionedAPIs = new ArrayList<API>();
            Comparator<API> versionComparator = new APIVersionComparator();
            Boolean allowMultipleVersions = isAllowDisplayMultipleVersions();
            Boolean showAllAPIs = isAllowDisplayAllAPIs();
            String providerPath = APIConstants.API_ROOT_LOCATION + RegistryConstants.PATH_SEPARATOR +
                                  providerId;
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry,
                                                                                APIConstants.API_KEY);
            Association[] associations = registry.getAssociations(providerPath,
                                                                  APIConstants.PROVIDER_ASSOCIATION);
            if (associations.length < limit || limit == -1) {
                limit = associations.length;
            }
            for (int i = 0; i < limit; i++) {
                Association association = associations[i];
                String apiPath = association.getDestinationPath();
                UserRealm realm = ServiceReferenceHolder.getUserRealm();
                RegistryAuthorizationManager authorizationManager = new RegistryAuthorizationManager(realm);
                Resource resource = null;
                String path = RegistryUtils.getAbsolutePath(RegistryContext.getBaseInstance(),
                                                            RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + apiPath);
                boolean checkAuthorized;
                if(loggedUsername==""){
                checkAuthorized=authorizationManager.isRoleAuthorized(APIConstants.ANONYMOUS_ROLE,path,ActionConstants.GET);
                }else{
                checkAuthorized= authorizationManager.isUserAuthorized(loggedUsername,path,ActionConstants.GET);
                }
                String apiArtifactId=null;
                if(checkAuthorized){
                resource = registry.get(apiPath);
                apiArtifactId = resource.getUUID();
                }

                if (apiArtifactId != null) {
                    GenericArtifact artifact = artifactManager.getGenericArtifact(apiArtifactId);
                    // check the API status
                    String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);

                    API api = null;
                    //Check the api-manager.xml config file entry <DisplayAllAPIs> value is false
                    if (!showAllAPIs) {
                        // then we are only interested in published APIs here...
                        if (status.equals(APIConstants.PUBLISHED)) {
                            api = APIUtil.getAPI(artifact);
                        }
                    } else {   // else we are interested in both deprecated/published APIs here...
                        if (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED)) {
                            api = APIUtil.getAPI(artifact);

                        }

                    }
                    if (api != null) {
                        String key;
                        //Check the configuration to allow showing multiple versions of an API true/false
                        if (!allowMultipleVersions) { //If allow only showing the latest version of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName();
                            API existingAPI = latestPublishedAPIs.get(key);
                            if (existingAPI != null) {
                                // If we have already seen an API with the same name, make sure
                                // this one has a higher version number
                                if (versionComparator.compare(api, existingAPI) > 0) {
                                    latestPublishedAPIs.put(key, api);
                                }
                            } else {
                                // We haven't seen this API before
                                latestPublishedAPIs.put(key, api);
                            }
                        } else { //If allow showing multiple versions of an API
                            key = api.getId().getProviderName() + ":" + api.getId().getApiName() + ":" + api.getId()
                                    .getVersion();
                            multiVersionedAPIs.add(api);
                        }
                    }
                }
            }
            if (!allowMultipleVersions) {
                for (API api : latestPublishedAPIs.values()) {
                    apiSortedSet.add(api);
                }
                return apiSortedSet;
            } else {
                for (API api : multiVersionedAPIs) {
                    apiVersionsSortedSet.add(api);
                }
                return apiVersionsSortedSet;
            }

        } catch (RegistryException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        } catch (UserStoreException e) {
            handleException("Failed to get Published APIs for provider : " + providerId, e);
            return null;
        }


    }

    public Set<API> searchAPI(String searchTerm) throws APIManagementException {
        Set<API> apiSet = new HashSet<API>();
        String regex = "(?i)[a-zA-Z0-9_.-|]*" + searchTerm.trim() + "(?i)[a-zA-Z0-9_.-|]*";
        Pattern pattern;
        Matcher matcher;
        try {
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            GenericArtifact[] genericArtifacts = artifactManager
                    .getAllGenericArtifacts();
            if (genericArtifacts == null || genericArtifacts.length == 0) {
                return apiSet;
            }
            pattern = Pattern.compile(regex);
            for (GenericArtifact artifact : genericArtifacts) {
                String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);
                String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
                matcher = pattern.matcher(apiName);
                if (matcher.matches() && status.equals(APIConstants.PUBLISHED)) {
                    apiSet.add(APIUtil.getAPI(artifact));
                }
            }
        } catch (RegistryException e) {
            handleException("Failed to Search APIs", e);
        }
        return apiSet;
    }

    public Set<API> searchAPI(String searchTerm, String searchType) throws APIManagementException {
        SortedSet<API> apiSet = new TreeSet<API>(new APINameComparator());
        String regex = "(?i)[a-zA-Z0-9_.-|]*" + searchTerm.trim() + "(?i)[a-zA-Z0-9_.-|]*";
        Pattern pattern;
        Matcher matcher;
        try {
            GenericArtifactManager artifactManager = APIUtil.getArtifactManager(registry, APIConstants.API_KEY);
            GenericArtifact[] genericArtifacts = artifactManager
                    .getAllGenericArtifacts();
            if (genericArtifacts == null || genericArtifacts.length == 0) {
                return apiSet;
            }
            pattern = Pattern.compile(regex);
            for (GenericArtifact artifact : genericArtifacts) {
                String status = artifact.getAttribute(APIConstants.API_OVERVIEW_STATUS);
                if (searchType.equalsIgnoreCase("Provider")) {
                    String api = artifact.getAttribute(APIConstants.API_OVERVIEW_PROVIDER);
                    matcher = pattern.matcher(api);
                } else if (searchType.equalsIgnoreCase("Version")) {
                    String api = artifact.getAttribute(APIConstants.API_OVERVIEW_VERSION);
                    matcher = pattern.matcher(api);
                } else if (searchType.equalsIgnoreCase("Context")) {
                    String api = artifact.getAttribute(APIConstants.API_OVERVIEW_CONTEXT);
                    matcher = pattern.matcher(api);
                } else {
                    String apiName = artifact.getAttribute(APIConstants.API_OVERVIEW_NAME);
                    matcher = pattern.matcher(apiName);
                }
                if (isAllowDisplayAllAPIs()) {
                    if (matcher.matches() && (status.equals(APIConstants.PUBLISHED) || status.equals(APIConstants.DEPRECATED))) {
                        apiSet.add(APIUtil.getAPI(artifact, registry));
                    }
                } else {
                    if (matcher.matches() && status.equals(APIConstants.PUBLISHED)) {
                        apiSet.add(APIUtil.getAPI(artifact, registry));
                    }
                }
            }
        } catch (RegistryException e) {
            handleException("Failed to search APIs with type", e);
        }
        return apiSet;
    }

    public Set<SubscribedAPI> getSubscribedAPIs(Subscriber subscriber) throws APIManagementException {
        Set<SubscribedAPI> subscribedAPIs = null;
        try {
            subscribedAPIs = apiMgtDAO.getSubscribedAPIs(subscriber);
        } catch (APIManagementException e) {
            handleException("Failed to get APIs of " + subscriber.getName(), e);
        }
        return subscribedAPIs;
    }

    public Set<APIIdentifier> getAPIByConsumerKey(String accessToken) throws APIManagementException {
        try {
            return apiMgtDAO.getAPIByConsumerKey(accessToken);
        } catch (APIManagementException e) {
            handleException("Error while obtaining API from API key", e);
        }
        return null;
    }

    public boolean isSubscribed(APIIdentifier apiIdentifier, String userId)
            throws APIManagementException {
        boolean isSubscribed;
        try {
            isSubscribed = apiMgtDAO.isSubscribed(apiIdentifier, userId);
        } catch (APIManagementException e) {
            String msg = "Failed to check if user(" + userId + ") has subscribed to " + apiIdentifier;
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }
        return isSubscribed;
    }

    public void addSubscription(APIIdentifier identifier, String userId, int applicationId)
            throws APIManagementException {
        API api = getAPI(identifier);
        if (api.getStatus().equals(APIStatus.PUBLISHED)) {
            apiMgtDAO.addSubscription(identifier, api.getContext(), applicationId);
            invalidateCachedKeys(applicationId, identifier);
        } else {
            throw new APIManagementException("Subscriptions not allowed on APIs in the state: " +
                                             api.getStatus().getStatus());
        }
    }

    public void removeSubscription(APIIdentifier identifier, String userId, int applicationId)
            throws APIManagementException {
        apiMgtDAO.removeSubscription(identifier, applicationId);
        invalidateCachedKeys(applicationId, identifier);
    }

    private void invalidateCachedKeys(int applicationId, APIIdentifier identifier) throws APIManagementException {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        if (config.getFirstProperty(APIConstants.API_GATEWAY_SERVER_URL) == null) {
            return;
        }

        Set<String> keys = apiMgtDAO.getApplicationKeys(applicationId);
        if (keys.size() > 0) {
            List<APIKeyMapping> mappings = new ArrayList<APIKeyMapping>();
            API api = getAPI(identifier);
            for (String key : keys) {
                APIKeyMapping mapping = new APIKeyMapping();
                mapping.setKey(key);
                mapping.setApiVersion(identifier.getVersion());
                mapping.setContext(api.getContext());
                mappings.add(mapping);
            }

            try {
                APIAuthenticationAdminClient client = new APIAuthenticationAdminClient();
                client.invalidateKeys(mappings);
            } catch (AxisFault axisFault) {
                log.warn("Error while invalidating API keys at the gateway", axisFault);
            }
        }
    }

    public void removeSubscriber(APIIdentifier identifier, String userId)
            throws APIManagementException {
        throw new UnsupportedOperationException("Unsubscribe operation is not yet implemented");
    }

    public void updateSubscriptions(APIIdentifier identifier, String userId, int applicationId)
            throws APIManagementException {
        API api = getAPI(identifier);
        apiMgtDAO.updateSubscriptions(identifier, api.getContext(), applicationId);
    }

    public void addComment(APIIdentifier identifier, String s, String user) throws APIManagementException {
        String apiPath = APIUtil.getAPIPath(identifier);
        org.wso2.carbon.registry.core.Comment comment = new org.wso2.carbon.registry.core.Comment(s);
        try {
            registry.addComment(apiPath, comment);
        } catch (RegistryException e) {
            handleException("Failed to add comment for api " + apiPath, e);
        }
    }

    public org.wso2.carbon.apimgt.api.model.Comment[] getComments(APIIdentifier identifier)
            throws APIManagementException {
        List<org.wso2.carbon.apimgt.api.model.Comment> commentList =
                new ArrayList<org.wso2.carbon.apimgt.api.model.Comment>();
        org.wso2.carbon.registry.core.Comment[] comments;
        String apiPath = APIUtil.getAPIPath(identifier);
        try {
            comments = registry.getComments(apiPath);
            for (org.wso2.carbon.registry.core.Comment comment : comments) {
                org.wso2.carbon.apimgt.api.model.Comment comment1 =
                        new org.wso2.carbon.apimgt.api.model.Comment();
                comment1.setText(comment.getText());
                comment1.setUser(comment.getUser());
                comment1.setCreatedTime(comment.getCreatedTime());
                commentList.add(comment1);
            }
            return commentList.toArray(new org.wso2.carbon.apimgt.api.model.Comment[commentList.size()]);
        } catch (RegistryException e) {
            handleException("Failed to get comments for api " + apiPath, e);
        }
        return null;
    }

    public void addApplication(Application application, String userId)
            throws APIManagementException {
        apiMgtDAO.addApplication(application, userId);
    }

    public void updateApplication(Application application) throws APIManagementException {
        apiMgtDAO.updateApplication(application);
    }

    public void removeApplication(Application application) throws APIManagementException {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        boolean gatewayExists = config.getFirstProperty(APIConstants.API_GATEWAY_SERVER_URL) != null;
        Set<SubscribedAPI> apiSet = null;
        Set<String> keys = null;
        if (gatewayExists) {
            keys = apiMgtDAO.getApplicationKeys(application.getId());
            apiSet = getSubscribedAPIs(application.getSubscriber());
        }
        apiMgtDAO.deleteApplication(application);

        if (gatewayExists && apiSet != null && keys != null) {
            Set<SubscribedAPI> removables = new HashSet<SubscribedAPI>();
            for (SubscribedAPI api : apiSet) {
                if (!api.getApplication().getName().equals(application.getName())) {
                    removables.add(api);
                }
            }

            for (SubscribedAPI api : removables) {
                apiSet.remove(api);
            }

            List<APIKeyMapping> mappings = new ArrayList<APIKeyMapping>();
            for (String key : keys) {
                for (SubscribedAPI api : apiSet) {
                    APIKeyMapping mapping = new APIKeyMapping();
                    API apiDefinition = getAPI(api.getApiId());
                    mapping.setApiVersion(api.getApiId().getVersion());
                    mapping.setContext(apiDefinition.getContext());
                    mapping.setKey(key);
                    mappings.add(mapping);
                }
            }

            if (mappings.size() > 0) {
                try {
                    APIAuthenticationAdminClient client = new APIAuthenticationAdminClient();
                    client.invalidateKeys(mappings);
                } catch (AxisFault axisFault) {
                    // Just logging the error is enough - We have already deleted the application
                    // which is what's important
                    log.warn("Error while invalidating API keys at the gateway", axisFault);
                }
            }
        }
    }

    public Application[] getApplications(Subscriber subscriber) throws APIManagementException {
        return apiMgtDAO.getApplications(subscriber);
    }

    public boolean isApplicationTokenExists(String accessToken) throws APIManagementException {
        return apiMgtDAO.isAccessTokenExists(accessToken);
    }

    public Set<SubscribedAPI> getSubscribedIdentifiers(Subscriber subscriber, APIIdentifier identifier)
            throws APIManagementException {
        Set<SubscribedAPI> subscribedAPISet = new HashSet<SubscribedAPI>();
        Set<SubscribedAPI> subscribedAPIs = getSubscribedAPIs(subscriber);
        for (SubscribedAPI api : subscribedAPIs) {
            if (api.getApiId().equals(identifier)) {
                subscribedAPISet.add(api);
            }
        }
        return subscribedAPISet;
    }

    /**
     * Returns a list of pre-defined # {@link org.wso2.carbon.apimgt.api.model.Tier} in the system.
     *
     * @return Set<Tier>
     */
    public Set<Tier> getTiers() throws APIManagementException {
        Set<Tier> tiers = new TreeSet<Tier>(new Comparator<Tier>() {
            public int compare(Tier o1, Tier o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        Map<String,Tier> tierMap = APIUtil.getTiers();
        tiers.addAll(tierMap.values());
        return tiers;
    }

    private boolean isAllowDisplayAllAPIs() {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();
        String displayAllAPIs = config.getFirstProperty(APIConstants.API_STORE_DISPLAY_ALL_APIS);
        if (displayAllAPIs == null) {
            log.warn("The configurations related to show deprecated APIs in APIStore " +
                     "are missing in api-manager.xml.");
            return false;
        }
        return Boolean.parseBoolean(displayAllAPIs);
    }

    private boolean isAllowDisplayMultipleVersions() {
        APIManagerConfiguration config = ServiceReferenceHolder.getInstance().
                getAPIManagerConfigurationService().getAPIManagerConfiguration();

        String displayMultiVersions = config.getFirstProperty(APIConstants.API_STORE_DISPLAY_MULTIPLE_VERSIONS);
        if (displayMultiVersions == null) {
            log.warn("The configurations related to show multiple versions of API in APIStore " +
                     "are missing in api-manager.xml.");
            return false;
        }
        return Boolean.parseBoolean(displayMultiVersions);
    }
}
