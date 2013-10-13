/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.apimgt.impl.dao;


import org.apache.axiom.util.base64.Base64Utils;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.dto.UserApplicationAPIUsage;
import org.wso2.carbon.apimgt.api.model.*;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dto.APIInfoDTO;
import org.wso2.carbon.apimgt.impl.dto.APIKeyInfoDTO;
import org.wso2.carbon.apimgt.impl.dto.APIKeyValidationInfoDTO;
import org.wso2.carbon.apimgt.impl.internal.ServiceReferenceHolder;
import org.wso2.carbon.apimgt.impl.token.JWTGenerator;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.apimgt.impl.utils.APIVersionComparator;
import org.wso2.carbon.apimgt.impl.utils.LRUCache;
import org.wso2.carbon.identity.base.IdentityException;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.IdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.OAuthConstants;
import org.wso2.carbon.identity.oauth.OAuthUtil;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This class represent the ApiMgtDAO.
 */
public class ApiMgtDAO {

    private static final Log log = LogFactory.getLog(ApiMgtDAO.class);

    public static JWTGenerator jwtGenerator;
    public static Boolean removeUserNameInJWTForAppToken;

    private static final String ENABLE_JWT_GENERATION = "APIConsumerAuthentication.EnableTokenGeneration";

    public ApiMgtDAO() {
        String enableJWTGeneration = ServiceReferenceHolder.getInstance()
                .getAPIManagerConfigurationService().getAPIManagerConfiguration()
                .getFirstProperty(ENABLE_JWT_GENERATION);
        removeUserNameInJWTForAppToken = Boolean.parseBoolean(ServiceReferenceHolder.getInstance()
                                                                      .getAPIManagerConfigurationService().getAPIManagerConfiguration()
                                                                      .getFirstProperty(APIConstants.API_KEY_MANAGER_REMOVE_USERNAME_TO_JWT_FOR_APP_TOKEN));
        if (enableJWTGeneration != null && JavaUtils.isTrueExplicitly(enableJWTGeneration)) {
            jwtGenerator = new JWTGenerator();
        }
    }

    /**
     * Get access token key for given userId and API Identifier
     *
     * @param userId          id of the user
     * @param applicationName name of the Application
     * @param identifier      APIIdentifier
     * @param keyType         Type of the key required
     * @return Access token
     * @throws APIManagementException if failed to get Access token
     * @throws org.wso2.carbon.identity.base.IdentityException
     *                                if failed to get tenant id
     */
    public String getAccessKeyForAPI(String userId, String applicationName, APIInfoDTO identifier,
                                     String keyType)
            throws APIManagementException, IdentityException {

        String accessKey = null;

        //get the tenant id for the corresponding domain
        String tenantAwareUserId = MultitenantUtils.getTenantAwareUsername(userId);
        int tenantId = IdentityUtil.getTenantIdOFUser(userId);

        if (log.isDebugEnabled()) {
            log.debug("Searching for: " + identifier.getAPIIdentifier() + ", User: " + tenantAwareUserId +
                      ", ApplicationName: " + applicationName + ", Tenant ID: " + tenantId);
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery =
                "SELECT " +
                "   SKM.ACCESS_TOKEN AS ACCESS_TOKEN " +
                "FROM " +
                "   AM_SUBSCRIPTION SP," +
                "   AM_API API," +
                "   AM_SUBSCRIBER SB," +
                "   AM_APPLICATION APP, " +
                "   AM_SUBSCRIPTION_KEY_MAPPING SKM " +
                "WHERE " +
                "   SB.USER_ID=? " +
                "   AND SB.TENANT_ID=? " +
                "   AND API.API_PROVIDER=? " +
                "   AND API.API_NAME=?" +
                "   AND API.API_VERSION=?" +
                "   AND APP.NAME=? " +
                "   AND SKM.KEY_TYPE=? " +
                "   AND API.API_ID = SP.API_ID" +
                "   AND SB.SUBSCRIBER_ID = APP.SUBSCRIBER_ID " +
                "   AND APP.APPLICATION_ID = SP.APPLICATION_ID " +
                "   AND SP.SUBSCRIPTION_ID = SKM.SUBSCRIPTION_ID ";

        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, tenantAwareUserId);
            ps.setInt(2, tenantId);
            ps.setString(3, identifier.getProviderId());
            ps.setString(4, identifier.getApiName());
            ps.setString(5, identifier.getVersion());
            ps.setString(6, applicationName);
            ps.setString(7, keyType);

            rs = ps.executeQuery();

            while (rs.next()) {
                accessKey = rs.getString(APIConstants.SUBSCRIPTION_FIELD_ACCESS_TOKEN);
            }
        } catch (SQLException e) {
            handleException("Error when executing the SQL query to read the access key for user : "
                            + userId + "of tenant(id) : " + tenantId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return accessKey;
    }

    public String getAccessKeyForApplication(String userId, String applicationName,
                                             String keyType)
            throws APIManagementException, IdentityException {

        String accessKey = null;
        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(userId);
        }

        //get the tenant id for the corresponding domain
        String tenantAwareUserId = MultitenantUtils.getTenantAwareUsername(userId);
        int tenantId = IdentityUtil.getTenantIdOFUser(userId);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery =
                "SELECT " +
                "   IAT.ACCESS_TOKEN AS ACCESS_TOKEN " +
                "FROM " +
                "   AM_SUBSCRIBER SB," +
                "   AM_APPLICATION APP, " +
                "   AM_APPLICATION_KEY_MAPPING AKM," +
                  accessTokenStoreTable + " IAT," +
                "   IDN_OAUTH_CONSUMER_APPS ICA " +
                "WHERE " +
                "   SB.USER_ID=? " +
                "   AND SB.TENANT_ID=? " +
                "   AND APP.NAME=? " +
                "   AND AKM.KEY_TYPE=? " +
                "   AND SB.SUBSCRIBER_ID = APP.SUBSCRIBER_ID " +
                "   AND APP.APPLICATION_ID = AKM.APPLICATION_ID" +
                "   AND ICA.CONSUMER_KEY = AKM.CONSUMER_KEY" +
                "   AND ICA.USERNAME = IAT.AUTHZ_USER";

        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, tenantAwareUserId);
            ps.setInt(2, tenantId);
            ps.setString(3, applicationName);
            ps.setString(4, keyType);
            rs = ps.executeQuery();

            while (rs.next()) {
                accessKey = rs.getString(APIConstants.SUBSCRIPTION_FIELD_ACCESS_TOKEN);
            }
        } catch (SQLException e) {
            handleException("Error when executing the SQL query to read the access key for user : "
                            + userId + "of tenant(id) : " + tenantId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return accessKey;
    }

    /**
     * Get Subscribed APIs for given userId
     *
     * @param userId id of the user
     * @return APIInfoDTO[]
     * @throws APIManagementException if failed to get Subscribed APIs
     * @throws org.wso2.carbon.identity.base.IdentityException
     *                                if failed to get tenant id
     */
    public APIInfoDTO[] getSubscribedAPIsOfUser(String userId) throws APIManagementException,
                                                                      IdentityException {
        String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userId);
        int tenantId = IdentityUtil.getTenantIdOFUser(userId);
        List<APIInfoDTO> apiInfoDTOList = new ArrayList<APIInfoDTO>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery = "SELECT " +
                          "   API.API_PROVIDER AS API_PROVIDER," +
                          "   API.API_NAME AS API_NAME," +
                          "   API.API_VERSION AS API_VERSION " +
                          "FROM " +
                          "   AM_SUBSCRIPTION SP, " +
                          "   AM_API API," +
                          "   AM_SUBSCRIBER SB, " +
                          "   AM_APPLICATION APP " +
                          "WHERE " +
                          "   SB.USER_ID = ? " +
                          "   AND SB.TENANT_ID = ? " +
                          "   AND SB.SUBSCRIBER_ID = APP.SUBSCRIBER_ID " +
                          "   AND APP.APPLICATION_ID=SP.APPLICATION_ID " +
                          "   AND API.API_ID = SP.API_ID";
        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, tenantAwareUsername);
            ps.setInt(2, tenantId);
            rs = ps.executeQuery();
            while (rs.next()) {
                APIInfoDTO infoDTO = new APIInfoDTO();
                infoDTO.setProviderId(rs.getString("API_PROVIDER"));
                infoDTO.setApiName(rs.getString("API_NAME"));
                infoDTO.setVersion(rs.getString("API_VERSION"));
                apiInfoDTOList.add(infoDTO);
            }
        } catch (SQLException e) {
            handleException("Error while executing SQL", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return apiInfoDTOList.toArray(new APIInfoDTO[apiInfoDTOList.size()]);
    }

    /**
     * Get API key information for given API
     *
     * @param apiInfoDTO API info
     * @return APIKeyInfoDTO[]
     * @throws APIManagementException if failed to get key info for given API
     */
    public APIKeyInfoDTO[] getSubscribedUsersForAPI(APIInfoDTO apiInfoDTO)
            throws APIManagementException {

        APIKeyInfoDTO[] apiKeyInfoDTOs = null;
        // api_id store as "providerName_apiName_apiVersion" in AM_SUBSCRIPTION table
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery = "SELECT " +
                          "   SB.USER_ID, " +
                          "   SB.TENANT_ID " +
                          "FROM " +
                          "   AM_SUBSCRIBER SB, " +
                          "   AM_APPLICATION APP, " +
                          "   AM_SUBSCRIPTION SP, " +
                          "   AM_API API " +
                          "WHERE " +
                          "   API.API_PROVIDER = ? " +
                          "   AND API.API_NAME = ?" +
                          "   AND API.API_VERSION = ?" +
                          "   AND SP.APPLICATION_ID = APP.APPLICATION_ID " +
                          "   AND APP.SUBSCRIBER_ID=SB.SUBSCRIBER_ID " +
                          "   AND API.API_ID = SP.API_ID";
        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, apiInfoDTO.getProviderId());
            ps.setString(2, apiInfoDTO.getApiName());
            ps.setString(3, apiInfoDTO.getVersion());
            rs = ps.executeQuery();
            List<APIKeyInfoDTO> apiKeyInfoList = new ArrayList<APIKeyInfoDTO>();
            while (rs.next()) {
                String userId = rs.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID);
                //int tenantId = rs.getInt(APIConstants.SUBSCRIBER_FIELD_TENANT_ID);
                // If the tenant Id > 0, get the tenant domain and append it to the username.
                //if (tenantId > 0) {
                //  userId = userId + "@" + APIKeyMgtUtil.getTenantDomainFromTenantId(tenantId);
                //}
                APIKeyInfoDTO apiKeyInfoDTO = new APIKeyInfoDTO();
                apiKeyInfoDTO.setUserId(userId);
                // apiKeyInfoDTO.setStatus(rs.getString(3));
                apiKeyInfoList.add(apiKeyInfoDTO);
            }
            apiKeyInfoDTOs = apiKeyInfoList.toArray(new APIKeyInfoDTO[apiKeyInfoList.size()]);

        } catch (SQLException e) {
            handleException("Error while executing SQL", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return apiKeyInfoDTOs;
    }

    /**
     * This method is to update the access token
     *
     * @param userId     id of the user
     * @param apiInfoDTO Api info
     * @param statusEnum Status of the access key
     * @throws APIManagementException if failed to update the access token
     * @throws org.wso2.carbon.identity.base.IdentityException
     *                                if failed to get tenant id
     */
    public void changeAccessTokenStatus(String userId, APIInfoDTO apiInfoDTO,
                                        String statusEnum)
            throws APIManagementException, IdentityException {
        String tenantAwareUsername = MultitenantUtils.getTenantAwareUsername(userId);
        int tenantId = 0;
        IdentityUtil.getTenantIdOFUser(userId);

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(userId);
        }

        Connection conn = null;
        PreparedStatement ps = null;
        String sqlQuery = "UPDATE " +
                          accessTokenStoreTable + " IAT , AM_SUBSCRIBER SB," +
                          " AM_SUBSCRIPTION SP , AM_APPLICATION APP, AM_API API" +
                          " SET IAT.TOKEN_STATE=?" +
                          " WHERE SB.USER_ID=?" +
                          " AND SB.TENANT_ID=?" +
                          " AND API.API_PROVIDER=?" +
                          " AND API.API_NAME=?" +
                          " AND API.API_VERSION=?" +
                          " AND SP.ACCESS_TOKEN=IAT.ACCESS_TOKEN" +
                          " AND SB.SUBSCRIBER_ID=APP.SUBSCRIBER_ID" +
                          " AND APP.APPLICATION_ID = SP.APPLICATION_ID" +
                          " AND API.API_ID = SP.API_ID";
        try {

            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, statusEnum);
            ps.setString(2, tenantAwareUsername);
            ps.setInt(3, tenantId);
            ps.setString(4, apiInfoDTO.getProviderId());
            ps.setString(5, apiInfoDTO.getApiName());
            ps.setString(6, apiInfoDTO.getVersion());

            int count = ps.executeUpdate();
            if (log.isDebugEnabled()) {
                log.debug("Number of rows being updated : " + count);
            }
            conn.commit();
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                log.error("Failed to rollback the changeAccessTokenStatus operation", e);
            }
            handleException("Error while executing SQL", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, null);
        }
    }

    /**
     * Validate the provided key against the given API. First it will validate the key is valid
     * , ACTIVE and not expired.
     *
     * @param context     Requested Context
     * @param version     version of the API
     * @param accessToken Provided Access Token
     * @return APIKeyValidationInfoDTO instance with authorization status and tier information if
     *         authorized.
     * @throws APIManagementException Error when accessing the database or registry.
     */
    public APIKeyValidationInfoDTO validateKey(String context, String version, String accessToken, String requiredAuthenticationLevel)
            throws APIManagementException {

        if (log.isDebugEnabled()) {
            log.debug("A request is received to process the token : " + accessToken + " to access" +
                      " the context URL : " + context);
        }
        APIKeyValidationInfoDTO keyValidationInfoDTO = new APIKeyValidationInfoDTO();
        keyValidationInfoDTO.setAuthorized(false);

        String status;
        String tier;
        String type;
        String userType;
        String subscriberName;
        String applicationId;
        String applicationName;
        String applicationTier;
        String endUserName;
        long validityPeriod;
        long issuedTime;
        long timestampSkew;
        long currentTime;

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }

        // First check whether the token is valid, active and not expired.
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        String applicationSqlQuery = "SELECT " +
                                     "   IAT.VALIDITY_PERIOD, " +
                                     "   IAT.TIME_CREATED ," +
                                     "   IAT.TOKEN_STATE," +
                                     "   IAT.USER_TYPE," +
                                     "   IAT.AUTHZ_USER," +
                                     "   IAT.TIME_CREATED," +
                                     "   SUB.TIER_ID," +
                                     "   SUBS.USER_ID," +
                                     "   APP.APPLICATION_ID," +
                                     "   APP.NAME," +
                                     "   APP.APPLICATION_TIER," +
                                     "   AKM.KEY_TYPE" +
                                     " FROM " + accessTokenStoreTable + " IAT," +
                                     "   AM_SUBSCRIPTION SUB," +
                                     "   AM_SUBSCRIBER SUBS," +
                                     "   AM_APPLICATION APP," +
                                     "   AM_APPLICATION_KEY_MAPPING AKM," +
                                     "   AM_API API" +
                                     " WHERE " +
                                     "   IAT.ACCESS_TOKEN = ? " +
                                     "   AND API.CONTEXT = ? " +
                                     "   AND API.API_VERSION = ? " +
                                     "   AND IAT.CONSUMER_KEY=AKM.CONSUMER_KEY " +
                                     "   AND APP.APPLICATION_ID = APP.APPLICATION_ID" +
                                     "   AND SUB.APPLICATION_ID = APP.APPLICATION_ID" +
                                     "   AND APP.SUBSCRIBER_ID = SUBS.SUBSCRIBER_ID" +
                                     "   AND API.API_ID = SUB.API_ID" +
                                     "   AND AKM.APPLICATION_ID=APP.APPLICATION_ID";

        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(applicationSqlQuery);
            ps.setString(1, accessToken);
            ps.setString(2, context);
            ps.setString(3, version);
            rs = ps.executeQuery();
            if (rs.next()) {
                status = rs.getString(APIConstants.IDENTITY_OAUTH2_FIELD_TOKEN_STATE);
                tier = rs.getString(APIConstants.SUBSCRIPTION_FIELD_TIER_ID);
                type = rs.getString(APIConstants.SUBSCRIPTION_KEY_TYPE);
                userType = rs.getString(APIConstants.SUBSCRIPTION_USER_TYPE);
                subscriberName = rs.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID);
                applicationId = rs.getString(APIConstants.APPLICATION_ID);
                applicationName = rs.getString(APIConstants.APPLICATION_NAME);
                applicationTier = rs.getString(APIConstants.APPLICATION_TIER);
                endUserName = rs.getString(APIConstants.IDENTITY_OAUTH2_FIELD_AUTHORIZED_USER);
                issuedTime = rs.getTimestamp(APIConstants.IDENTITY_OAUTH2_FIELD_TIME_CREATED,
                                             Calendar.getInstance(TimeZone.getTimeZone("UTC"))).getTime();
                validityPeriod = rs.getLong(APIConstants.IDENTITY_OAUTH2_FIELD_VALIDITY_PERIOD);
                timestampSkew = OAuthServerConfiguration.getInstance().
                        getDefaultTimeStampSkewInSeconds() * 1000;
                currentTime = System.currentTimeMillis();

                //check if 'requiredAuthenticationLevel' & the one associated with access token matches
                //This check should only be done for 'Application' and 'Application_User' levels
                if(requiredAuthenticationLevel.equals(APIConstants.AUTH_APPLICATION_LEVEL_TOKEN)
                        || requiredAuthenticationLevel.equals(APIConstants.AUTH_APPLICATION_USER_LEVEL_TOKEN)){
                    if(log.isDebugEnabled()){
                        log.debug("Access token's userType : "+userType + ".Required type : "+requiredAuthenticationLevel);
                    }

                    if (!(userType.equalsIgnoreCase(requiredAuthenticationLevel))){
                        keyValidationInfoDTO.setValidationStatus(
                                APIConstants.KeyValidationStatus.API_AUTH_INCORRECT_ACCESS_TOKEN_TYPE);
                        keyValidationInfoDTO.setAuthorized(false);
                        return keyValidationInfoDTO;
                    }
                }

                // Check whether the token is ACTIVE
                if (APIConstants.TokenStatus.ACTIVE.equals(status)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Checking Access token: " + accessToken + " for validity." +
                                  "((currentTime - timestampSkew) > (issuedTime + validityPeriod)) : " +
                                  "((" + currentTime + "-" + timestampSkew + ")" + " > (" + issuedTime + " + " + validityPeriod + "))");
                    }
                    if ((currentTime - timestampSkew) > (issuedTime + validityPeriod)) {
                        keyValidationInfoDTO.setValidationStatus(
                                APIConstants.KeyValidationStatus.API_AUTH_ACCESS_TOKEN_EXPIRED);
                        if (log.isDebugEnabled()) {
                            log.debug("Access token: " + accessToken + " has expired. " +
                                      "Reason ((currentTime - timestampSkew) > (issuedTime + validityPeriod)) : " +
                                      "((" + currentTime + "-" + timestampSkew + ")" + " > (" + issuedTime + " + " + validityPeriod + "))");
                        }
                        //update token status as expired
                        updateTokenState(accessToken, conn, ps);
                    } else {
                        keyValidationInfoDTO.setAuthorized(true);
                        keyValidationInfoDTO.setTier(tier);
                        keyValidationInfoDTO.setType(type);
                        keyValidationInfoDTO.setSubscriber(subscriberName);
                        keyValidationInfoDTO.setIssuedTime(issuedTime);
                        keyValidationInfoDTO.setValidityPeriod(validityPeriod);
                        keyValidationInfoDTO.setUserType(userType);
                        if (jwtGenerator != null) {

                            if (removeUserNameInJWTForAppToken && !(userType == null)
                                && userType.equalsIgnoreCase(APIConstants.ACCESS_TOKEN_USER_TYPE_APPLICATION)) {
                                String calleeToken = jwtGenerator.generateToken(subscriberName, applicationName, context, version, tier, "null");
                                keyValidationInfoDTO.setEndUserToken(calleeToken);
                            } else {
                                String calleeToken = jwtGenerator.generateToken(subscriberName, applicationName, context, version, tier, endUserName);
                                keyValidationInfoDTO.setEndUserToken(calleeToken);
                            }
                        }
                        keyValidationInfoDTO.setEndUserName(endUserName);
                        keyValidationInfoDTO.setApplicationId(applicationId);
                        keyValidationInfoDTO.setApplicationName(applicationName);
                        keyValidationInfoDTO.setApplicationTier(applicationTier);
                    }
                } else {
                    keyValidationInfoDTO.setValidationStatus(
                            APIConstants.KeyValidationStatus.API_AUTH_ACCESS_TOKEN_INACTIVE);
                    if (log.isDebugEnabled()) {
                        log.debug("Access token: " + accessToken + " is inactive");
                    }
                }
            } else {
                //no record found. Invalid access token received
                keyValidationInfoDTO.setValidationStatus(
                        APIConstants.KeyValidationStatus.API_AUTH_INVALID_CREDENTIALS);
                if (log.isDebugEnabled()) {
                    log.debug("Access token: " + accessToken + " is invalid");
                }
            }
        } catch (SQLException e) {
            handleException("Error when executing the SQL ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return keyValidationInfoDTO;
    }

    private void updateTokenState(String accessToken, Connection conn, PreparedStatement ps)
            throws SQLException, APIManagementException {

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }
        String UPDATE_TOKE_STATE_SQL =
                "UPDATE " +
                accessTokenStoreTable +
                " SET " +
                "   TOKEN_STATE = ? " +
                "   ,TOKEN_STATE_ID = ? " +
                "WHERE " +
                "   ACCESS_TOKEN = ?";
        ps = conn.prepareStatement(UPDATE_TOKE_STATE_SQL);
        ps.setString(1, "EXPIRED");
        ps.setString(2, UUID.randomUUID().toString());
        ps.setString(3, accessToken);
        ps.executeUpdate();
    }

    public void addSubscriber(Subscriber subscriber) throws APIManagementException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            String query = "INSERT" +
                           " INTO AM_SUBSCRIBER (USER_ID, TENANT_ID, EMAIL_ADDRESS, DATE_SUBSCRIBED)" +
                           " VALUES (?,?,?,?)";

            ps = conn.prepareStatement(query, new String[]{"subscriber_id"});

            //ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, subscriber.getName());
            ps.setInt(2, subscriber.getTenantId());
            ps.setString(3, subscriber.getEmail());
            ps.setTimestamp(4, new Timestamp(subscriber.getSubscribedDate().getTime()));
            ps.executeUpdate();

            int subscriberId = 0;
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                //subscriberId = rs.getInt(1);
                subscriberId = Integer.valueOf(rs.getString(1)).intValue();
            }
            subscriber.setId(subscriberId);

            // Add default application
            Application defaultApp = new Application(APIConstants.DEFAULT_APPLICATION_NAME, subscriber);
            defaultApp.setTier(APIConstants.UNLIMITED_TIER);
            addApplication(defaultApp, subscriber.getName(), conn);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Error while rolling back the failed operation", e);
                }
            }
            handleException("Error in adding new subscriber: " + e.getMessage(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
    }

    public void updateSubscriber(Subscriber subscriber) throws APIManagementException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            String query = "UPDATE" +
                           " AM_SUBSCRIBER SET USER_ID=?, TENANT_ID=?, EMAIL_ADDRESS=?, DATE_SUBSCRIBED=?" +
                           " WHERE SUBSCRIBER_ID=?";
            ps = conn.prepareStatement(query);
            ps.setString(1, subscriber.getName());
            ps.setInt(2, subscriber.getTenantId());
            ps.setString(3, subscriber.getEmail());
            ps.setTimestamp(4, new Timestamp(subscriber.getSubscribedDate().getTime()));
            ps.setInt(5, subscriber.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            handleException("Error in updating subscriber: " + e.getMessage(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
    }

    public Subscriber getSubscriber(int subscriberId) throws APIManagementException {
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            String query =
                    "SELECT" +
                    " USER_ID, TENANT_ID, EMAIL_ADDRESS, DATE_SUBSCRIBED " +
                    "FROM " +
                    "AM_SUBSCRIBER" +
                    " WHERE " +
                    "SUBSCRIBER_ID=?";
            ps = conn.prepareStatement(query);
            ps.setInt(1, subscriberId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Subscriber subscriber = new Subscriber(rs.getString("USER_ID"));
                subscriber.setId(subscriberId);
                subscriber.setTenantId(rs.getInt("TENANT_ID"));
                subscriber.setEmail(rs.getString("EMAIL_ADDRESS"));
                subscriber.setSubscribedDate(new java.util.Date(
                        rs.getTimestamp("DATE_SUBSCRIBED").getTime()));
                return subscriber;
            }
        } catch (SQLException e) {
            handleException("Error while retrieving subscriber: " + e.getMessage(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return null;
    }

    public int addSubscription(APIIdentifier identifier, String context, int applicationId)
            throws APIManagementException {

        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;
        int subscriptionId = -1;
        int apiId = -1;

        try {
            conn = APIMgtDBUtil.getConnection();
            String getApiQuery = "SELECT API_ID FROM AM_API API WHERE API_PROVIDER = ? AND " +
                                 "API_NAME = ? AND API_VERSION = ?";
            ps = conn.prepareStatement(getApiQuery);
            ps.setString(1, identifier.getProviderName());
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            resultSet = ps.executeQuery();
            if (resultSet.next()) {
                apiId = resultSet.getInt("API_ID");
            }
            resultSet.close();
            ps.close();

            if (apiId == -1) {
                String msg = "Unable to get the API ID for: " + identifier;
                log.error(msg);
                throw new APIManagementException(msg);
            }

            //This query to update the AM_SUBSCRIPTION table
            String sqlQuery = "INSERT " +
                              "INTO AM_SUBSCRIPTION (TIER_ID,API_ID,APPLICATION_ID)" +
                              " VALUES (?,?,?)";

            //Adding data to the AM_SUBSCRIPTION table
            //ps = conn.prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
            ps = conn.prepareStatement(sqlQuery, new String[]{"SUBSCRIPTION_ID"});

            ps.setString(1, identifier.getTier());
            ps.setInt(2, apiId);
            ps.setInt(3, applicationId);

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                //subscriptionId = rs.getInt(1);
                subscriptionId = Integer.valueOf(rs.getString(1)).intValue();
            }
            ps.close();

            // finally commit transaction
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add subscription ", e);
                }
            }
            handleException("Failed to add subscriber data ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
        return subscriptionId;
    }

    public void removeSubscription(APIIdentifier identifier, int applicationId)
            throws APIManagementException {
        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;
        int subscriptionId = -1;
        int apiId = -1;

        try {
            conn = APIMgtDBUtil.getConnection();
            String getApiQuery = "SELECT API_ID FROM AM_API API WHERE API_PROVIDER = ? AND " +
                                 "API_NAME = ? AND API_VERSION = ?";
            ps = conn.prepareStatement(getApiQuery);
            ps.setString(1, identifier.getProviderName());
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            resultSet = ps.executeQuery();
            if (resultSet.next()) {
                apiId = resultSet.getInt("API_ID");
            }
            resultSet.close();
            ps.close();

            if (apiId == -1) {
                throw new APIManagementException("Unable to get the API ID for: " + identifier);
            }

            //This query to updates the AM_SUBSCRIPTION table
            String sqlQuery = "DELETE FROM AM_SUBSCRIPTION WHERE API_ID = ? AND APPLICATION_ID = ?";

            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, apiId);
            ps.setInt(2, applicationId);
            ps.executeUpdate();

            // finally commit transaction
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add subscription ", e);
                }
            }
            handleException("Failed to add subscriber data ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
    }

    /**
     * This method used tot get Subscriber from subscriberId.
     *
     * @param subscriberName id
     * @return Subscriber
     * @throws APIManagementException if failed to get Subscriber from subscriber id
     */
    public Subscriber getSubscriber(String subscriberName) throws APIManagementException {

        Connection conn = null;
        Subscriber subscriber = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        int tenantId;
        try {
            tenantId = IdentityUtil.getTenantIdOFUser(subscriberName);
        } catch (IdentityException e) {
            String msg = "Failed to get tenant id of user : " + subscriberName;
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }

        String sqlQuery = "SELECT " +
                          "   SUBSCRIBER_ID, " +
                          "   USER_ID, " +
                          "   TENANT_ID, " +
                          "   EMAIL_ADDRESS, " +
                          "   DATE_SUBSCRIBED " +
                          "FROM " +
                          "   AM_SUBSCRIBER " +
                          "WHERE " +
                          "   USER_ID = ? " +
                          "   AND TENANT_ID = ?";
        try {
            conn = APIMgtDBUtil.getConnection();

            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, subscriberName);
            ps.setInt(2, tenantId);
            result = ps.executeQuery();

            if (result.next()) {
                subscriber = new Subscriber(result.getString(
                        APIConstants.SUBSCRIBER_FIELD_EMAIL_ADDRESS));
                subscriber.setEmail(result.getString("EMAIL_ADDRESS"));
                subscriber.setId(result.getInt("SUBSCRIBER_ID"));
                subscriber.setName(subscriberName);
                subscriber.setSubscribedDate(result.getDate(
                        APIConstants.SUBSCRIBER_FIELD_DATE_SUBSCRIBED));
                subscriber.setTenantId(result.getInt("TENANT_ID"));
            }

        } catch (SQLException e) {
            handleException("Failed to get Subscriber for :" + subscriberName, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, result);
        }
        return subscriber;
    }

    public Set<APIIdentifier> getAPIByConsumerKey(String accessToken)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        String getAPISql = "SELECT" +
                           " API.API_PROVIDER," +
                           " API.API_NAME," +
                           " API.API_VERSION " +
                           "FROM" +
                           " AM_SUBSCRIPTION SUB," +
                           " AM_SUBSCRIPTION_KEY_MAPPING SKM, " +
                           " AM_API API " +
                           "WHERE" +
                           " SKM.ACCESS_TOKEN=?" +
                           " AND SKM.SUBSCRIPTION_ID=SUB.SUBSCRIPTION_ID" +
                           " AND API.API_ID = SUB.API_ID";

        Set<APIIdentifier> apiList = new HashSet<APIIdentifier>();
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getAPISql);
            nestedPS.setString(1, accessToken);
            ResultSet nestedRS = nestedPS.executeQuery();
            while (nestedRS.next()) {
                apiList.add(new APIIdentifier(nestedRS.getString("API_PROVIDER"),
                                              nestedRS.getString("API_NAME"),
                                              nestedRS.getString("API_VERSION")));
            }
        } catch (SQLException e) {
            handleException("Failed to get API ID for token: " + accessToken, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiList;
    }

    /**
     * This method returns the set of APIs for given subscriber
     *
     * @param subscriber subscriber
     * @return Set<API>
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to get SubscribedAPIs
     */
    public Set<SubscribedAPI> getSubscribedAPIs(Subscriber subscriber)
            throws APIManagementException {
        Set<SubscribedAPI> subscribedAPIs = new LinkedHashSet<SubscribedAPI>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        try {
            connection = APIMgtDBUtil.getConnection();

            String sqlQuery = "SELECT " +
                              "   SUBS.SUBSCRIPTION_ID" +
                              "   ,API.API_PROVIDER AS API_PROVIDER" +
                              "   ,API.API_NAME AS API_NAME" +
                              "   ,API.API_VERSION AS API_VERSION" +
                              "   ,SUBS.TIER_ID AS TIER_ID" +
                              "   ,APP.APPLICATION_ID AS APP_ID" +
                              "   ,SUBS.LAST_ACCESSED AS LAST_ACCESSED" +
                              "   ,APP.NAME AS APP_NAME " +
                              "FROM " +
                              "   AM_SUBSCRIBER SUB," +
                              "   AM_APPLICATION APP, " +
                              "   AM_SUBSCRIPTION SUBS, " +
                              "   AM_API API " +
                              "WHERE " +
                              "   SUB.USER_ID = ? " +
                              "   AND SUB.TENANT_ID = ? " +
                              "   AND SUB.SUBSCRIBER_ID=APP.SUBSCRIBER_ID " +
                              "   AND APP.APPLICATION_ID=SUBS.APPLICATION_ID " +
                              "   AND API.API_ID=SUBS.API_ID";

            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, subscriber.getName());
            int tenantId = IdentityUtil.getTenantIdOFUser(subscriber.getName());
            ps.setInt(2, tenantId);
            result = ps.executeQuery();

            if (result == null) {
                return subscribedAPIs;
            }

            Map<String, Set<SubscribedAPI>> map = new TreeMap<String, Set<SubscribedAPI>>();
            LRUCache<Integer, Application> applicationCache = new LRUCache<Integer, Application>(100);

            while (result.next()) {
                APIIdentifier apiIdentifier = new APIIdentifier(result.getString("API_PROVIDER"),
                                                                result.getString("API_NAME"), result.getString("API_VERSION"));

                SubscribedAPI subscribedAPI = new SubscribedAPI(subscriber, apiIdentifier);
                subscribedAPI.setTier(new Tier(
                        result.getString(APIConstants.SUBSCRIPTION_FIELD_TIER_ID)));
                subscribedAPI.setLastAccessed(result.getDate(
                        APIConstants.SUBSCRIPTION_FIELD_LAST_ACCESS));
                //setting NULL for subscriber. If needed, Subscriber object should be constructed &
                // passed in
                int applicationId = result.getInt("APP_ID");
                Application application = applicationCache.get(applicationId);
                if (application == null) {
                    application = new Application(result.getString("APP_NAME"), subscriber);
                    application.setId(result.getInt("APP_ID"));
                    String tenantAwareUserId = MultitenantUtils.getTenantAwareUsername(subscriber.getName());
                    Set<APIKey> keys = getApplicationKeys(tenantAwareUserId, applicationId);
                    for (APIKey key : keys) {
                        application.addKey(key);
                    }
                    applicationCache.put(applicationId, application);
                }
                subscribedAPI.setApplication(application);

                int subscriptionId = result.getInt(APIConstants.SUBSCRIPTION_FIELD_SUBSCRIPTION_ID);
                Set<APIKey> apiKeys = getAPIKeysBySubscription(subscriptionId);
                for (APIKey key : apiKeys) {
                    subscribedAPI.addKey(key);
                }

                if (!map.containsKey(application.getName())) {
                    map.put(application.getName(), new TreeSet<SubscribedAPI>(new Comparator<SubscribedAPI>() {
                        public int compare(SubscribedAPI o1, SubscribedAPI o2) {
                            int placement = o1.getApiId().getApiName().compareTo(o2.getApiId().getApiName());
                            if (placement == 0) {
                                return new APIVersionComparator().compare(new API(o1.getApiId()),
                                                                          new API(o2.getApiId()));
                            }
                            return placement;
                        }
                    }));
                }
                map.get(application.getName()).add(subscribedAPI);
            }

            for (String application : map.keySet()) {
                Set<SubscribedAPI> apis = map.get(application);
                for (SubscribedAPI api : apis) {
                    subscribedAPIs.add(api);
                }
            }

        } catch (SQLException e) {
            handleException("Failed to get SubscribedAPI of :" + subscriber.getName(), e);
        } catch (IdentityException e) {
            handleException("Failed get tenant id of user " + subscriber.getName(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscribedAPIs;
    }

    private Set<APIKey> getAPIKeysBySubscription(int subscriptionId) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        String getKeysSql = "SELECT " +
                            " SKM.ACCESS_TOKEN AS ACCESS_TOKEN," +
                            " SKM.KEY_TYPE AS TOKEN_TYPE " +
                            "FROM" +
                            " AM_SUBSCRIPTION_KEY_MAPPING SKM " +
                            "WHERE" +
                            " SKM.SUBSCRIPTION_ID = ?";

        Set<APIKey> apiKeys = new HashSet<APIKey>();
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getKeysSql);
            nestedPS.setInt(1, subscriptionId);
            ResultSet nestedRS = nestedPS.executeQuery();
            while (nestedRS.next()) {
                APIKey apiKey = new APIKey();
                apiKey.setAccessToken(nestedRS.getString("ACCESS_TOKEN"));
                apiKey.setType(nestedRS.getString("TOKEN_TYPE"));
                apiKeys.add(apiKey);
            }
        } catch (SQLException e) {
            handleException("Failed to get API keys for subscription: " + subscriptionId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiKeys;
    }

    public String getTokenScope(String consumerKey) throws APIManagementException {
        String tokenScope = null;

        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                   tokenScope = getTokenScope(consumerKey, getScopeSql(keyStoreTable));
                    if (tokenScope != null) {
                        break;
                    }
                }
            }
        } else {
                tokenScope = getTokenScope(consumerKey, getScopeSql(null));
        }
        return tokenScope;
    }

    private String getTokenScope(String consumerKey, String getScopeSql)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String tokenScope = null;

        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getScopeSql);
            nestedPS.setString(1, consumerKey);
            ResultSet nestedRS = nestedPS.executeQuery();
            if (nestedRS.next()) {
                tokenScope = nestedRS.getString("TOKEN_SCOPE");
            }
        } catch (SQLException e) {
            handleException("Failed to get token scope from consumer key: " + consumerKey, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }

        return tokenScope;
    }

    private String getScopeSql(String accessTokenStoreTable) {
        String tokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        return "SELECT" +
                " IAT.TOKEN_SCOPE AS TOKEN_SCOPE " +
                "FROM " +
                tokenStoreTable + " IAT," +
                " IDN_OAUTH_CONSUMER_APPS ICA " +
                "WHERE" +
                " IAT.CONSUMER_KEY = ?" +
                " AND IAT.CONSUMER_KEY = ICA.CONSUMER_KEY" +
                " AND IAT.AUTHZ_USER = ICA.USERNAME";
    }

    public Boolean isAccessTokenExists(String accessToken) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }

        String getTokenSql = "SELECT ACCESS_TOKEN " +
                             "FROM " + accessTokenStoreTable +
                             " WHERE ACCESS_TOKEN= ? ";
        Boolean tokenExists = false;
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            getToken.setString(1, accessToken);
            ResultSet getTokenRS = getToken.executeQuery();
            while (getTokenRS.next()) {
                tokenExists = true;
            }
        } catch (SQLException e) {
            handleException("Failed to check availability of the access token. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return tokenExists;
    }

    public Boolean isAccessTokenRevoked(String accessToken) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }

        String getTokenSql = "SELECT TOKEN_STATE " +
                             "FROM " + accessTokenStoreTable +
                             " WHERE ACCESS_TOKEN= ? ";
        Boolean tokenExists = false;
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            getToken.setString(1, accessToken);
            ResultSet getTokenRS = getToken.executeQuery();
            while (getTokenRS.next()) {
                if (!getTokenRS.getString("TOKEN_STATE").equals("REVOKED")) {
                    tokenExists = true;
                }
            }
        } catch (SQLException e) {
            handleException("Failed to check availability of the access token. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return tokenExists;
    }

    public APIKey getAccessTokenData(String accessToken) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        APIKey apiKey=new APIKey();

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }

        String getTokenSql = "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
                             "TIME_CREATED,VALIDITY_PERIOD " +
                             "FROM " + accessTokenStoreTable  +
                             " WHERE ACCESS_TOKEN= ? AND TOKEN_STATE='ACTIVE' ";
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            getToken.setString(1, accessToken);
            ResultSet getTokenRS = getToken.executeQuery();
            while (getTokenRS.next()) {

                apiKey.setAccessToken(getTokenRS.getString("ACCESS_TOKEN"));
                apiKey.setAuthUser(getTokenRS.getString("AUTHZ_USER"));
                apiKey.setTokenScope(getTokenRS.getString("TOKEN_SCOPE"));
                apiKey.setCreatedDate(getTokenRS.getTimestamp("TIME_CREATED").toString().split("\\.")[0]);
                apiKey.setConsumerKey(getTokenRS.getString("CONSUMER_KEY"));
                apiKey.setValidityPeriod("" + getTokenRS.getInt("VALIDITY_PERIOD"));

            }
        } catch (SQLException e) {
            handleException("Failed to get the access token data. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiKey;
    }

    public Map<Integer, APIKey> getAccessTokens(String query)
            throws APIManagementException {
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();
        if (APIUtil.checkAccessTokenPartitioningEnabled()
                && APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                    Map<Integer, APIKey> tokenDataMapTmp = getAccessTokens(query,
                            getTokenSql(keyStoreTable));
                    tokenDataMap.putAll(tokenDataMapTmp);
                }
            }
        } else {
                tokenDataMap = getAccessTokens(query, getTokenSql(null));
        }
        return tokenDataMap;
    }

    private Map<Integer, APIKey> getAccessTokens(String query, String getTokenSql)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();

        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            ResultSet getTokenRS = getToken.executeQuery();
            while (getTokenRS.next()) {
                String accessToken = getTokenRS.getString("ACCESS_TOKEN");
                String regex = "(?i)[a-zA-Z0-9_.-|]*" + query.trim() + "(?i)[a-zA-Z0-9_.-|]*";
                Pattern pattern;
                Matcher matcher;
                pattern = Pattern.compile(regex);
                matcher = pattern.matcher(accessToken);
                Integer i = 0;
                if (matcher.matches()) {
                    APIKey apiKey = new APIKey();
                    apiKey.setAccessToken(accessToken);
                    apiKey.setAuthUser(getTokenRS.getString("AUTHZ_USER"));
                    apiKey.setTokenScope(getTokenRS.getString("TOKEN_SCOPE"));
                    apiKey.setCreatedDate(getTokenRS.getTimestamp("TIME_CREATED").toString().split("\\.")[0]);
                    apiKey.setConsumerKey(getTokenRS.getString("CONSUMER_KEY"));
                    apiKey.setValidityPeriod("" + getTokenRS.getInt("VALIDITY_PERIOD"));
                    tokenDataMap.put(i, apiKey);
                    i++;
                }
            }
        } catch (SQLException e) {
            handleException("Failed to get access token data. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);

        }
        return tokenDataMap;
    }

    private String getTokenSql (String accessTokenStoreTable) {
        String tokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        return "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
                    "TIME_CREATED,VALIDITY_PERIOD " +
                    "FROM " + tokenStoreTable + " WHERE TOKEN_STATE='ACTIVE' ";
    }

    public Map<Integer, APIKey> getAccessTokensByUser(String user)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
                accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(user);
        }

        String getTokenSql = "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
                             "TIME_CREATED,VALIDITY_PERIOD " +
                             "FROM " + accessTokenStoreTable +
                             " WHERE AUTHZ_USER= ? AND TOKEN_STATE='ACTIVE' ";
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken = connection.prepareStatement(getTokenSql);
            getToken.setString(1, user);
            ResultSet getTokenRS = getToken.executeQuery();
            Integer i = 0;
            while (getTokenRS.next()) {
                String accessToken = getTokenRS.getString("ACCESS_TOKEN");
                APIKey apiKey=new APIKey();
                apiKey.setAccessToken(accessToken);
                apiKey.setAuthUser(getTokenRS.getString("AUTHZ_USER"));
                apiKey.setTokenScope(getTokenRS.getString("TOKEN_SCOPE"));
                apiKey.setCreatedDate(getTokenRS.getTimestamp("TIME_CREATED").toString().split("\\.")[0]);
                apiKey.setConsumerKey(getTokenRS.getString("CONSUMER_KEY"));
                apiKey.setValidityPeriod("" + getTokenRS.getInt("VALIDITY_PERIOD"));
                tokenDataMap.put(i, apiKey);
                i++;


            }
        } catch (SQLException e) {
            handleException("Failed to get access token data. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return tokenDataMap;
    }

    public Map<Integer, APIKey> getAccessTokensByDate(String date, boolean latest)
            throws APIManagementException {
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();

        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                    Map<Integer, APIKey> tokenDataMapTmp = getAccessTokensByDate
                            (date, latest, getTokenByDateSqls(keyStoreTable));
                    tokenDataMap.putAll(tokenDataMapTmp);
                }
            }
        } else {
            tokenDataMap = getAccessTokensByDate(date, latest, getTokenByDateSqls(null));
        }

        return tokenDataMap;
    }

    public Map<Integer, APIKey> getAccessTokensByDate(String date, boolean latest, String[] querySql)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Map<Integer, APIKey> tokenDataMap = new HashMap<Integer, APIKey>();

        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            java.util.Date searchDate = fmt.parse(date);
            Date sqlDate = new Date(searchDate.getTime());
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement getToken;
            if (latest) {
                getToken = connection.prepareStatement(querySql[0]);
            } else {
                getToken = connection.prepareStatement(querySql[1]);
            }
            getToken.setDate(1, sqlDate);

            ResultSet getTokenRS = getToken.executeQuery();
            Integer i = 0;
            while (getTokenRS.next()) {
                String accessToken = getTokenRS.getString("ACCESS_TOKEN");
                APIKey apiKey = new APIKey();
                apiKey.setAccessToken(accessToken);
                apiKey.setAuthUser(getTokenRS.getString("AUTHZ_USER"));
                apiKey.setTokenScope(getTokenRS.getString("TOKEN_SCOPE"));
                apiKey.setCreatedDate(getTokenRS.getTimestamp("TIME_CREATED").toString().split("\\.")[0]);
                apiKey.setConsumerKey(getTokenRS.getString("CONSUMER_KEY"));
                apiKey.setValidityPeriod("" + getTokenRS.getInt("VALIDITY_PERIOD"));
                tokenDataMap.put(i, apiKey);
                i++;
            }
        } catch (SQLException e) {
            handleException("Failed to get access token data. ", e);
        } catch (ParseException e) {
            handleException("Failed to get access token data. ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return tokenDataMap;
    }

    public String[] getTokenByDateSqls (String accessTokenStoreTable) {
        String[] querySqlArr = new String[2];
        String tokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        querySqlArr[0] = "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
                                  "TIME_CREATED,VALIDITY_PERIOD " +
                                  "FROM " + tokenStoreTable  +
                                  " WHERE TOKEN_STATE='ACTIVE' AND TIME_CREATED >= ? ";

        querySqlArr[1] = "SELECT ACCESS_TOKEN,AUTHZ_USER,TOKEN_SCOPE,CONSUMER_KEY," +
                                   "TIME_CREATED,VALIDITY_PERIOD " +
                                   "FROM " + tokenStoreTable +
                                   " WHERE TOKEN_STATE='ACTIVE' AND TIME_CREATED <= ? ";

        return querySqlArr;
    }

    private Set<APIKey> getApplicationKeys(String username, int applicationId)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(username);
        }

        String getKeysSql = "SELECT " +
                            " ICA.CONSUMER_KEY AS CONSUMER_KEY," +
                            " ICA.CONSUMER_SECRET AS CONSUMER_SECRET," +
                            " IAT.ACCESS_TOKEN AS ACCESS_TOKEN," +
                            " AKM.KEY_TYPE AS TOKEN_TYPE " +
                            "FROM" +
                            " AM_APPLICATION_KEY_MAPPING AKM," +
                            accessTokenStoreTable + " IAT," +
                            " IDN_OAUTH_CONSUMER_APPS ICA " +
                            "WHERE" +
                            " AKM.APPLICATION_ID = ? AND" +
                            " ICA.USERNAME = ? AND" +
                            " IAT.USER_TYPE = ? AND" +
                            " ICA.CONSUMER_KEY = AKM.CONSUMER_KEY AND" +
                            " ICA.CONSUMER_KEY = IAT.CONSUMER_KEY AND" +
                            " ICA.USERNAME = IAT.AUTHZ_USER";

        Set<APIKey> apiKeys = new HashSet<APIKey>();
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getKeysSql);
            nestedPS.setInt(1, applicationId);
            nestedPS.setString(2, username);
            nestedPS.setString(3, APIConstants.ACCESS_TOKEN_USER_TYPE_APPLICATION);
            ResultSet nestedRS = nestedPS.executeQuery();
            while (nestedRS.next()) {
                APIKey apiKey = new APIKey();
                apiKey.setConsumerKey(nestedRS.getString("CONSUMER_KEY"));
                apiKey.setConsumerSecret(nestedRS.getString("CONSUMER_SECRET"));
                apiKey.setAccessToken(nestedRS.getString("ACCESS_TOKEN"));
                apiKey.setType(nestedRS.getString("TOKEN_TYPE"));
                apiKeys.add(apiKey);
            }
        } catch (SQLException e) {
            handleException("Failed to get keys for application: " + applicationId, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiKeys;
    }

    public Set<String> getApplicationKeys(int applicationId)
            throws APIManagementException {
        Set<String> apiKeys = new HashSet<String>();
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                    apiKeys = getApplicationKeys(applicationId, getKeysSql(keyStoreTable));
                    if (apiKeys != null) {
                        break;
                    }
                }
            }
        } else {
            apiKeys = getApplicationKeys(applicationId, getKeysSql(null));
        }
        return apiKeys;
    }
    
    private Set<String> getApplicationKeys(int applicationId, String getKeysSql)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Set<String> apiKeys = new HashSet<String>();
            try {
                connection = APIMgtDBUtil.getConnection();
                PreparedStatement nestedPS = connection.prepareStatement(getKeysSql);
                nestedPS.setInt(1, applicationId);
                ResultSet nestedRS = nestedPS.executeQuery();
                while (nestedRS.next()) {
                    apiKeys.add(nestedRS.getString("ACCESS_TOKEN"));
                }
            } catch (SQLException e) {
                handleException("Failed to get keys for application: " + applicationId, e);
            } finally {
                APIMgtDBUtil.closeAllConnections(ps, connection, result);
            }
        return apiKeys;
    }

    private String getKeysSql(String accessTokenStoreTable) {
        String tokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        return "SELECT " +
                    " ICA.CONSUMER_KEY AS CONSUMER_KEY," +
                    " ICA.CONSUMER_SECRET AS CONSUMER_SECRET," +
                    " IAT.ACCESS_TOKEN AS ACCESS_TOKEN," +
                    " AKM.KEY_TYPE AS TOKEN_TYPE " +
                    "FROM" +
                    " AM_APPLICATION_KEY_MAPPING AKM," +
                    tokenStoreTable + " IAT," +
                    " IDN_OAUTH_CONSUMER_APPS ICA " +
                    "WHERE" +
                    " AKM.APPLICATION_ID = ? AND" +
                    " ICA.CONSUMER_KEY = AKM.CONSUMER_KEY AND" +
                    " ICA.CONSUMER_KEY = IAT.CONSUMER_KEY";
    }

    /**
     * Get access token data based on application ID
     *
     * @param subscriptionId Subscription Id
     * @return access token data
     * @throws APIManagementException
     */
    public Map<String, String> getAccessTokenData(int subscriptionId)
            throws APIManagementException {
        Map<String, String> apiKeys = new HashMap<String, String>();

        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            String[] keyStoreTables = APIUtil.getAvailableKeyStoreTables();
            if (keyStoreTables != null) {
                for (String keyStoreTable : keyStoreTables) {
                    apiKeys = getAccessTokenData(subscriptionId,
                            getKeysSqlUsingSubscriptionId(keyStoreTable));
                    if (apiKeys != null) {
                        break;
                    }
                }
            }
        } else {
            apiKeys = getAccessTokenData(subscriptionId, getKeysSqlUsingSubscriptionId(null));
        }
        return apiKeys;
    }

    private Map<String, String> getAccessTokenData(int subscriptionId, String getKeysSql)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Map<String, String> apiKeys = new HashMap<String, String>();
            try {
                connection = APIMgtDBUtil.getConnection();
                PreparedStatement nestedPS = connection.prepareStatement(getKeysSql);
                nestedPS.setInt(1, subscriptionId);
                ResultSet nestedRS = nestedPS.executeQuery();
                while (nestedRS.next()) {
                    apiKeys.put("token", nestedRS.getString("ACCESS_TOKEN"));
                    apiKeys.put("status", nestedRS.getString("TOKEN_STATE"));
                }
            } catch (SQLException e) {
                handleException("Failed to get keys for application: " + subscriptionId, e);
            } finally {
                APIMgtDBUtil.closeAllConnections(ps, connection, result);
            }
        return apiKeys;
    }

    private String getKeysSqlUsingSubscriptionId(String accessTokenStoreTable) {
        String tokenStoreTable = "IDN_OAUTH2_ACCESS_TOKEN";
        if (accessTokenStoreTable != null) {
            tokenStoreTable = accessTokenStoreTable;
        }

        return "SELECT " +
                    " IAT.ACCESS_TOKEN AS ACCESS_TOKEN," +
                    " IAT.TOKEN_STATE AS TOKEN_STATE" +
                    " FROM" +
                    " AM_APPLICATION_KEY_MAPPING AKM," +
                    " AM_SUBSCRIPTION SM," +
                    tokenStoreTable + " IAT," +
                    " IDN_OAUTH_CONSUMER_APPS ICA " +
                    "WHERE" +
                    " SM.SUBSCRIPTION_ID = ? AND" +
                    " SM.APPLICATION_ID= AKM.APPLICATION_ID AND" +
                    " ICA.CONSUMER_KEY = AKM.CONSUMER_KEY AND" +
                    " ICA.CONSUMER_KEY = IAT.CONSUMER_KEY";
    }

    /**
     * This method returns the set of Subscribers for given provider
     *
     * @param providerName name of the provider
     * @return Set<Subscriber>
     * @throws APIManagementException if failed to get subscribers for given provider
     */
    public Set<Subscriber> getSubscribersOfProvider(String providerName)
            throws APIManagementException {

        Set<Subscriber> subscribers = new HashSet<Subscriber>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        try {
            connection = APIMgtDBUtil.getConnection();

            String sqlQuery = "SELECT " +
                              "   SUBS.USER_ID AS USER_ID," +
                              "   SUBS.EMAIL_ADDRESS AS EMAIL_ADDRESS, " +
                              "   SUBS.DATE_SUBSCRIBED AS DATE_SUBSCRIBED " +
                              "FROM " +
                              "   AM_SUBSCRIBER  SUBS," +
                              "   AM_APPLICATION  APP, " +
                              "   AM_SUBSCRIPTION SUB, " +
                              "   AM_API API " +
                              "WHERE  " +
                              "   SUB.APPLICATION_ID = APP.APPLICATION_ID " +
                              "   AND SUBS. SUBSCRIBER_ID = APP.SUBSCRIBER_ID " +
                              "   AND API.API_ID = SUB.API_ID " +
                              "   AND API.API_PROVIDER = ?";


            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, providerName);
            result = ps.executeQuery();

            while (result.next()) {
                // Subscription table should have API_VERSION AND API_PROVIDER
                Subscriber subscriber =
                        new Subscriber(result.getString(
                                APIConstants.SUBSCRIBER_FIELD_EMAIL_ADDRESS));
                subscriber.setName(result.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID));
                subscriber.setSubscribedDate(result.getDate(
                        APIConstants.SUBSCRIBER_FIELD_DATE_SUBSCRIBED));
                subscribers.add(subscriber);
            }

        } catch (SQLException e) {
            handleException("Failed to subscribers for :" + providerName, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscribers;
    }

    public Set<Subscriber> getSubscribersOfAPI(APIIdentifier identifier)
            throws APIManagementException {

        Set<Subscriber> subscribers = new HashSet<Subscriber>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        try {
            connection = APIMgtDBUtil.getConnection();
            String sqlQuery = "SELECT " +
                              "SB.USER_ID, SB.DATE_SUBSCRIBED " +
                              "FROM AM_SUBSCRIBER SB, AM_SUBSCRIPTION SP,AM_APPLICATION APP,AM_API API" +
                              " WHERE API.API_PROVIDER=? " +
                              "AND API.API_NAME=? " +
                              "AND API.API_VERSION=? " +
                              "AND SP.APPLICATION_ID=APP.APPLICATION_ID" +
                              " AND APP.SUBSCRIBER_ID=SB.SUBSCRIBER_ID " +
                              " AND API.API_ID = SP.API_ID";

            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, identifier.getProviderName());
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            result = ps.executeQuery();
            if (result == null) {
                return subscribers;
            }
            while (result.next()) {
                Subscriber subscriber =
                        new Subscriber(result.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID));
                subscriber.setSubscribedDate(
                        result.getTimestamp(APIConstants.SUBSCRIBER_FIELD_DATE_SUBSCRIBED));
                subscribers.add(subscriber);
            }

        } catch (SQLException e) {
            handleException("Failed to get subscribers for :" + identifier.getApiName(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscribers;
    }

    public long getAPISubscriptionCountByAPI(APIIdentifier identifier)
            throws APIManagementException {

        String sqlQuery = "SELECT" +
                          " COUNT(SUB.SUBSCRIPTION_ID) AS SUB_ID" +
                          " FROM AM_SUBSCRIPTION SUB, AM_API API " +
                          " WHERE API.API_PROVIDER=? " +
                          " AND API.API_NAME=?" +
                          " AND API.API_VERSION=?" +
                          " AND API.API_ID=SUB.API_ID";
        long subscriptions = 0;

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;

        try {
            connection = APIMgtDBUtil.getConnection();

            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, identifier.getProviderName());
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            result = ps.executeQuery();
            if (result == null) {
                return subscriptions;
            }
            while (result.next()) {
                subscriptions = result.getLong("SUB_ID");
            }
        } catch (SQLException e) {
            handleException("Failed to get subscription count for API", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscriptions;
    }

    /**
     * This method is used to update the subscriber
     *
     * @param identifier    APIIdentifier
     * @param context       Context of the API
     * @param applicationId Application id
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to update subscriber
     */
    public void updateSubscriptions(APIIdentifier identifier, String context, int applicationId)
            throws APIManagementException {
        addSubscription(identifier, context, applicationId);
    }

    /**
     * This method is to renew access token
     *
     * @param keyType        key type
     * @param oldAccessToken old access token
     * @return Access Token
     * @throws IdentityException throws IdentityException
     */
    public String refreshAccessToken(String keyType, String oldAccessToken)
            throws IdentityException, APIManagementException {

        String accessToken = OAuthUtil.getRandomNumber();
        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkUserNameAssertionEnabled()) {
            String userName = APIUtil.getUserIdFromAccessToken(oldAccessToken);
            //use ':' for token & userName separation
            String accessTokenStrToEncode = accessToken + ":" + userName;
            accessToken = Base64Utils.encode(accessTokenStrToEncode.getBytes());

            if (APIUtil.checkAccessTokenPartitioningEnabled()) {
                accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(userName);
            }
        }

        // Update Access Token
        String sqlUpdateAccessToken = "UPDATE " +
                                      accessTokenStoreTable +
                                      " SET ACCESS_TOKEN=?, TOKEN_STATE=?, TIME_CREATED=?, VALIDITY_PERIOD=? " +
                                      " WHERE ACCESS_TOKEN=? AND TOKEN_SCOPE=? ";
        
        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = APIMgtDBUtil.getConnection();
            long validityPeriod = OAuthServerConfiguration.getInstance().getDefaultAccessTokenValidityPeriodInSeconds();
            prepStmt = connection.prepareStatement(sqlUpdateAccessToken);
            prepStmt.setString(1, accessToken);
            prepStmt.setString(2, APIConstants.TokenStatus.ACTIVE);
            prepStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()),
                                  Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            prepStmt.setLong(4, validityPeriod * 1000);
            prepStmt.setString(5, oldAccessToken);
            prepStmt.setString(6, keyType);


            prepStmt.execute();
            prepStmt.close();

            connection.commit();

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add access token ", e);
                }
            }

        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return accessToken;
    }


    /**
     * @param consumerKey     ConsumerKey
     * @param applicationName Application name
     * @param userId          User Id
     * @param tenantId        Tenant Id of the user
     * @param apiInfoDTO      Application Info DTO
     * @param keyType         Type (scope) of the key
     * @return accessToken
     * @throws IdentityException if failed to register accessToken
     */
    public String registerAccessToken(String consumerKey, String applicationName, String userId,
                                      int tenantId, APIInfoDTO apiInfoDTO, String keyType)
            throws IdentityException, APIManagementException {

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        String accessToken = OAuthUtil.getRandomNumber();

        if (APIUtil.checkUserNameAssertionEnabled()) {
            //use ':' for token & userName separation
            String accessTokenStrToEncode = accessToken + ":" + userId;
            accessToken = Base64Utils.encode(accessTokenStrToEncode.getBytes());

            if (APIUtil.checkAccessTokenPartitioningEnabled()) {
                accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(userId);
            }
        }

        // Add Access Token
        String sqlAddAccessToken = "INSERT" +
                " INTO " + accessTokenStoreTable +
                "(ACCESS_TOKEN, CONSUMER_KEY, TOKEN_STATE, TOKEN_SCOPE) " +
                " VALUES (?,?,?,?)";

        String getSubscriptionId = "SELECT SUBS.SUBSCRIPTION_ID " +
                                   "FROM " +
                                   "  AM_SUBSCRIPTION SUBS, " +
                                   "  AM_APPLICATION APP, " +
                                   "  AM_SUBSCRIBER SUB, " +
                                   "  AM_API API " +
                                   "WHERE " +
                                   "  SUB.USER_ID = ?" +
                                   "  AND SUB.TENANT_ID = ?" +
                                   "  AND APP.SUBSCRIBER_ID = SUB.SUBSCRIBER_ID" +
                                   "  AND APP.NAME = ?" +
                                   "  AND API.API_PROVIDER = ?" +
                                   "  AND API.API_NAME = ?" +
                                   "  AND API.API_VERSION = ?" +
                                   "  AND APP.APPLICATION_ID = SUBS.APPLICATION_ID" +
                                   "  AND API.API_ID = SUBS.API_ID";

        String addSubscriptionKeyMapping = "INSERT " +
                                           "INTO AM_SUBSCRIPTION_KEY_MAPPING (SUBSCRIPTION_ID, ACCESS_TOKEN, KEY_TYPE) " +
                                           "VALUES (?,?,?)";

        //String apiId = apiInfoDTO.getProviderId()+"_"+apiInfoDTO.getApiName()+"_"+apiInfoDTO.getVersion();
        Connection connection = null;
        PreparedStatement prepStmt = null;
        try {
            connection = APIMgtDBUtil.getConnection();
            //Add access token
            prepStmt = connection.prepareStatement(sqlAddAccessToken);
            prepStmt.setString(1, accessToken);
            prepStmt.setString(2, consumerKey);
            prepStmt.setString(3, APIConstants.TokenStatus.ACTIVE);
            prepStmt.setString(4, keyType);
            prepStmt.execute();
            prepStmt.close();

            //Update subscription with new key context mapping
            int subscriptionId = -1;
            prepStmt = connection.prepareStatement(getSubscriptionId);
            prepStmt.setString(1, userId);
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, applicationName);
            prepStmt.setString(4, apiInfoDTO.getProviderId());
            prepStmt.setString(5, apiInfoDTO.getApiName());
            prepStmt.setString(6, apiInfoDTO.getVersion());
            ResultSet getSubscriptionIdResult = prepStmt.executeQuery();
            while (getSubscriptionIdResult.next()) {
                subscriptionId = getSubscriptionIdResult.getInt(1);
            }
            prepStmt.close();

            prepStmt = connection.prepareStatement(addSubscriptionKeyMapping);
            prepStmt.setInt(1, subscriptionId);
            prepStmt.setString(2, accessToken);
            prepStmt.setString(3, keyType);
            prepStmt.execute();
            prepStmt.close();

            connection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add access token ", e);
                }
            }
            //  throw new IdentityException("Error when storing the access code for consumer key : " + consumerKey);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return accessToken;
    }

    public String registerApplicationAccessToken(String consumerKey, String applicationName,
                                                 String userId,
                                                 int tenantId, String keyType)
            throws IdentityException, APIManagementException {
        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        String accessToken = OAuthUtil.getRandomNumber();

        if (APIUtil.checkUserNameAssertionEnabled()) {
            //use ':' for token & userName separation
            String accessTokenStrToEncode = accessToken + ":" + userId;
            accessToken = Base64Utils.encode(accessTokenStrToEncode.getBytes());

            if (APIUtil.checkAccessTokenPartitioningEnabled()) {
                accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromUserId(userId);
            }
        }

        // Add Access Token
        String sqlAddAccessToken = "INSERT" +
                                   " INTO " +  accessTokenStoreTable +"(ACCESS_TOKEN, CONSUMER_KEY, TOKEN_STATE, TOKEN_SCOPE, AUTHZ_USER, USER_TYPE, TIME_CREATED, VALIDITY_PERIOD) " +
                                   " VALUES (?,?,?,?,?,?,?,?)";

        String getApplicationId = "SELECT APP.APPLICATION_ID " +
                                  "FROM " +
                                  "  AM_APPLICATION APP, " +
                                  "  AM_SUBSCRIBER SUB " +
                                  "WHERE " +
                                  "  SUB.USER_ID = ?" +
                                  "  AND SUB.TENANT_ID = ?" +
                                  "  AND APP.NAME = ?" +
                                  "  AND APP.SUBSCRIBER_ID = SUB.SUBSCRIBER_ID";

        String addApplicationKeyMapping = "INSERT " +
                                          "INTO AM_APPLICATION_KEY_MAPPING (APPLICATION_ID, CONSUMER_KEY, KEY_TYPE) " +
                                          "VALUES (?,?,?)";

        Connection connection = null;
        PreparedStatement prepStmt = null;
        long validityPeriod = OAuthServerConfiguration.getInstance().getDefaultAccessTokenValidityPeriodInSeconds();
        try {
            connection = APIMgtDBUtil.getConnection();
            //Add access token
            prepStmt = connection.prepareStatement(sqlAddAccessToken);
            prepStmt.setString(1, accessToken);
            prepStmt.setString(2, consumerKey);
            prepStmt.setString(3, APIConstants.TokenStatus.ACTIVE);
            prepStmt.setString(4, keyType);
            prepStmt.setString(5, userId);
            prepStmt.setString(6, APIConstants.ACCESS_TOKEN_USER_TYPE_APPLICATION);
            prepStmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()),
                                  Calendar.getInstance(TimeZone.getTimeZone("UTC")));
            prepStmt.setLong(8, validityPeriod * 1000);
            prepStmt.execute();
            prepStmt.close();

            int applicationId = -1;
            prepStmt = connection.prepareStatement(getApplicationId);
            prepStmt.setString(1, userId);
            prepStmt.setInt(2, tenantId);
            prepStmt.setString(3, applicationName);
            ResultSet getApplicationIdResult = prepStmt.executeQuery();
            while (getApplicationIdResult.next()) {
                applicationId = getApplicationIdResult.getInt(1);
            }
            prepStmt.close();

            prepStmt = connection.prepareStatement(addApplicationKeyMapping);
            prepStmt.setInt(1, applicationId);
            prepStmt.setString(2, consumerKey);
            prepStmt.setString(3, keyType);
            prepStmt.execute();
            prepStmt.close();

            connection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add access token ", e);
                }
            }
            //  throw new IdentityException("Error when storing the access code for consumer key : " + consumerKey);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, prepStmt);
        }
        return accessToken;
    }


    /**
     * @param apiIdentifier APIIdentifier
     * @param userId        User Id
     * @return true if user subscribed for given APIIdentifier
     * @throws APIManagementException if failed to check subscribed or not
     */
    public boolean isSubscribed(APIIdentifier apiIdentifier, String userId)
            throws APIManagementException {
        boolean isSubscribed = false;
        String apiId = apiIdentifier.getProviderName() + "_" + apiIdentifier.getApiName() + "_" +
                       apiIdentifier.getVersion();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        String sqlQuery = "SELECT " +
                          "   SUBS.TIER_ID ," +
                          "   API.API_PROVIDER ," +
                          "   API.API_NAME ," +
                          "   API.API_VERSION ," +
                          "   SUBS.LAST_ACCESSED ," +
                          "   SUBS.APPLICATION_ID " +
                          "FROM " +
                          "   AM_SUBSCRIPTION SUBS," +
                          "   AM_SUBSCRIBER SUB, " +
                          "   AM_APPLICATION  APP, " +
                          "   AM_API API " +
                          "WHERE " +
                          "   API.API_PROVIDER  = ?" +
                          "   AND API.API_NAME = ?" +
                          "   AND API.API_VERSION = ?" +
                          "   AND SUB.USER_ID = ?" +
                          "   AND SUB.TENANT_ID = ? " +
                          "   AND APP.SUBSCRIBER_ID = SUB.SUBSCRIBER_ID" +
                          "   AND API.API_ID = SUBS.API_ID";

        try {
            conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, apiIdentifier.getProviderName());
            ps.setString(2, apiIdentifier.getApiName());
            ps.setString(3, apiIdentifier.getVersion());
            ps.setString(4, userId);
            int tenantId;
            try {
                tenantId = IdentityUtil.getTenantIdOFUser(userId);
            } catch (IdentityException e) {
                String msg = "Failed to get tenant id of user : " + userId;
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }
            ps.setInt(5, tenantId);

            rs = ps.executeQuery();

            if (rs.next()) {
                isSubscribed = true;
            }
        } catch (SQLException e) {
            handleException("Error while checking if user has subscribed to the API ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
        return isSubscribed;
    }

    /**
     * @param providerName Name of the provider
     * @return UserApplicationAPIUsage of given provider
     * @throws org.wso2.carbon.apimgt.api.APIManagementException
     *          if failed to get
     *          UserApplicationAPIUsage for given provider
     */
    public UserApplicationAPIUsage[] getAllAPIUsageByProvider(String providerName)
            throws APIManagementException {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;


        try {
            connection = APIMgtDBUtil.getConnection();

            String sqlQuery = "SELECT " +
                              "   SUBS.SUBSCRIPTION_ID AS SUBSCRIPTION_ID, " +
                              "   SUBS.TIER_ID AS TIER_ID, " +
                              "   API.API_PROVIDER AS API_PROVIDER, " +
                              "   API.API_NAME AS API_NAME, " +
                              "   API.API_VERSION AS API_VERSION, " +
                              "   SUBS.LAST_ACCESSED AS LAST_ACCESSED, " +
                              "   SUB.USER_ID AS USER_ID, " +
                              "   APP.NAME AS APPNAME " +
                              "FROM " +
                              "   AM_SUBSCRIPTION SUBS, " +
                              "   AM_APPLICATION APP, " +
                              "   AM_SUBSCRIBER SUB, " +
                              "   AM_API API " +
                              "WHERE " +
                              "   SUBS.APPLICATION_ID = APP.APPLICATION_ID " +
                              "   AND APP.SUBSCRIBER_ID = SUB.SUBSCRIBER_ID " +
                              "   AND API.API_PROVIDER = ? " +
                              "   AND API.API_ID = SUBS.API_ID " +
                              "ORDER BY " +
                              "   APP.NAME";

            ps = connection.prepareStatement(sqlQuery);
            ps.setString(1, providerName);
            result = ps.executeQuery();

            Map<String, UserApplicationAPIUsage> userApplicationUsages = new TreeMap<String, UserApplicationAPIUsage>();
            while (result.next()) {
                int subId = result.getInt("SUBSCRIPTION_ID");
                Map<String, String> keyData = getAccessTokenData(subId);
                String accessToken = keyData.get("token");
                String tokenStatus = keyData.get("status");
                String userId = result.getString("USER_ID");
                String application = result.getString("APPNAME");
                String key = userId + "::" + application;
                UserApplicationAPIUsage usage = userApplicationUsages.get(key);
                if (usage == null) {
                    usage = new UserApplicationAPIUsage();
                    usage.setUserId(userId);
                    usage.setApplicationName(application);
                    usage.setAccessToken(accessToken);
                    usage.setAccessTokenStatus(tokenStatus);
                    userApplicationUsages.put(key, usage);
                }

                usage.addApiIdentifier(new APIIdentifier(result.getString("API_PROVIDER"),
                                                         result.getString("API_NAME"), result.getString("API_VERSION")));

            }
            return userApplicationUsages.values().toArray(
                    new UserApplicationAPIUsage[userApplicationUsages.size()]);

        } catch (SQLException e) {
            handleException("Failed to find API Usage for :" + providerName, e);
            return null;
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
    }

    /**
     * return the subscriber for given access token
     *
     * @param accessToken AccessToken
     * @return Subscriber
     * @throws APIManagementException if failed to get subscriber for given access token
     */
    public Subscriber getSubscriberById(String accessToken) throws APIManagementException {
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        Subscriber subscriber = null;
        String query = " SELECT" +
                       " SB.USER_ID, SB.DATE_SUBSCRIBED" +
                       " FROM AM_SUBSCRIBER SB , AM_SUBSCRIPTION SP, AM_APPLICATION APP, AM_SUBSCRIPTION_KEY_MAPPING SKM" +
                       " WHERE SKM.ACCESS_TOKEN=?" +
                       " AND SP.APPLICATION_ID=APP.APPLICATION_ID" +
                       " AND APP.SUBSCRIBER_ID=SB.SUBSCRIBER_ID" +
                       " AND SP.SUBSCRIPTION_ID=SKM.SUBSCRIPTION_ID";

        try {
            connection = APIMgtDBUtil.getConnection();
            ps = connection.prepareStatement(query);
            ps.setString(1, accessToken);

            result = ps.executeQuery();
            while (result.next()) {
                subscriber = new Subscriber(result.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID));
                subscriber.setSubscribedDate(result.getDate(APIConstants.SUBSCRIBER_FIELD_DATE_SUBSCRIBED));
            }

        } catch (SQLException e) {
            handleException("Failed to get Subscriber for accessToken", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return subscriber;
    }

    public String[] getOAuthCredentials(String accessToken, String tokenType)
            throws APIManagementException {

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String consumerKey = null;
        String consumerSecret = null;
        String sqlStmt = "SELECT " +
                         " ICA.CONSUMER_KEY AS CONSUMER_KEY," +
                         " ICA.CONSUMER_SECRET AS CONSUMER_SECRET " +
                         "FROM " +
                         " IDN_OAUTH_CONSUMER_APPS ICA," +
                         accessTokenStoreTable +
                         " WHERE " +
                         " IAT.ACCESS_TOKEN = ? AND" +
                         " IAT.TOKEN_SCOPE = ? AND" +
                         " IAT.CONSUMER_KEY = ICA.CONSUMER_KEY";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlStmt);
            prepStmt.setString(1, accessToken);
            prepStmt.setString(2, tokenType);
            rs = prepStmt.executeQuery();

            if (rs.next()) {
                consumerKey = rs.getString("CONSUMER_KEY");
                consumerSecret = rs.getString("CONSUMER_SECRET");
            }

        } catch (SQLException e) {
            handleException("Error when adding a new OAuth consumer.", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, rs, prepStmt);
        }
        return new String[]{consumerKey, consumerSecret};
    }

    public String[] addOAuthConsumer(String username, int tenantId, String appName)
            throws IdentityOAuthAdminException, APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        String sqlStmt = "INSERT INTO IDN_OAUTH_CONSUMER_APPS " +
                         "(CONSUMER_KEY, CONSUMER_SECRET, USERNAME, TENANT_ID, OAUTH_VERSION, APP_NAME) VALUES (?,?,?,?,?,?) ";
        String consumerKey;
        String consumerSecret = OAuthUtil.getRandomNumber();

        do {
            consumerKey = OAuthUtil.getRandomNumber();
        }
        while (isDuplicateConsumer(consumerKey));

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlStmt);
            prepStmt.setString(1, consumerKey);
            prepStmt.setString(2, consumerSecret);
            prepStmt.setString(3, username);
            prepStmt.setInt(4, tenantId);
            // it is assumed that the OAuth version is 1.0a because this is required with OAuth 1.0a
            prepStmt.setString(5, OAuthConstants.OAuthVersions.VERSION_1A);
            prepStmt.setString(6, appName);
            prepStmt.execute();

            connection.commit();

        } catch (SQLException e) {
            handleException("Error when adding a new OAuth consumer.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, null);
        }
        return new String[]{consumerKey, consumerSecret};
    }


    private boolean isDuplicateConsumer(String consumerKey) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rSet = null;
        String sqlQuery = "SELECT * FROM IDN_OAUTH_CONSUMER_APPS " +
                          "WHERE CONSUMER_KEY=?";

        boolean isDuplicateConsumer = false;

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setString(1, consumerKey);

            rSet = prepStmt.executeQuery();
            if (rSet.next()) {
                isDuplicateConsumer = true;
            }
        } catch (SQLException e) {
            handleException("Error when reading the application information from" +
                            " the persistence store.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rSet);
        }
        return isDuplicateConsumer;
    }


    public void addApplication(Application application, String userId)
            throws APIManagementException {
        Connection conn = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            addApplication(application, userId, conn);
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add Application ", e);
                }
            }
            handleException("Failed to add Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
    }

    /**
     * @param application Application
     * @param userId      User Id
     * @throws APIManagementException if failed to add Application
     */
    public void addApplication(Application application, String userId, Connection conn)
            throws APIManagementException, SQLException {
        PreparedStatement ps = null;

        try {
            int tenantId;
            try {
                tenantId = IdentityUtil.getTenantIdOFUser(userId);
            } catch (IdentityException e) {
                String msg = "Failed to get tenant id of user : " + userId;
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }
            //Get subscriber Id
            Subscriber subscriber = getSubscriber(userId, tenantId, conn);
            if (subscriber == null) {
                String msg = "Could not load Subscriber records for: " + userId;
                log.error(msg);
                throw new APIManagementException(msg);
            }
            //This query to update the AM_APPLICATION table
            String sqlQuery = "INSERT " +
                              "INTO AM_APPLICATION (NAME, SUBSCRIBER_ID, APPLICATION_TIER)" +
                              " VALUES (?,?,?)";
            // Adding data to the AM_APPLICATION  table
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, application.getName());
            ps.setInt(2, subscriber.getId());
            ps.setString(3, application.getTier());

            ps.executeUpdate();
            ps.close();

        } catch (SQLException e) {
            handleException("Failed to add Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, null, null);
        }
    }

    public void updateApplication(Application application) throws APIManagementException {
        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;

        try {
            conn = APIMgtDBUtil.getConnection();

            //This query to update the AM_APPLICATION table
            String sqlQuery = "UPDATE " +
                              "AM_APPLICATION" +
                              " SET NAME = ? " +
                              ", APPLICATION_TIER = ? " +
                              "WHERE" +
                              " APPLICATION_ID = ?";
            // Adding data to the AM_APPLICATION  table
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, application.getName());
            ps.setString(2, application.getTier());
            ps.setInt(3, application.getId());

            ps.executeUpdate();
            ps.close();
            // finally commit transaction
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the update Application ", e);
                }
            }
            handleException("Failed to update Application", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
    }

    /**
     * @param subscriber Subscriber
     * @return Applications for given subscriber.
     * @throws APIManagementException if failed to get Applications for given subscriber.
     */
    public Application[] getApplications(Subscriber subscriber) throws APIManagementException {
        if (subscriber == null) {
            return null;
        }
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Application[] applications = null;

        String sqlQuery = "SELECT " +
                          "   APPLICATION_ID " +
                          "   ,NAME" +
                          "   ,APPLICATION_TIER" +
                          "   ,SUBSCRIBER_ID  " +
                          "FROM " +
                          "   AM_APPLICATION " +
                          "WHERE " +
                          "   SUBSCRIBER_ID  = ?";

        try {
            int tenantId;
            connection = APIMgtDBUtil.getConnection();
            try {
                tenantId = IdentityUtil.getTenantIdOFUser(subscriber.getName());
            } catch (IdentityException e) {
                String msg = "Failed to get tenant id of user : " + subscriber.getName();
                log.error(msg, e);
                throw new APIManagementException(msg, e);
            }

            //getSubscriberId
            if (subscriber.getId() == 0) {
                Subscriber subs;
                subs = getSubscriber(subscriber.getName(), tenantId, connection);
                if (subs == null) {
                    return null;
                } else {
                    subscriber = subs;
                }
            }

            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setInt(1, subscriber.getId());
            rs = prepStmt.executeQuery();

            ArrayList<Application> applicationsList = new ArrayList<Application>();
            String tenantAwareUserId = MultitenantUtils.getTenantAwareUsername(subscriber.getName());
            Application application;
            while (rs.next()) {
                application = new Application(rs.getString("NAME"), subscriber);
                application.setId(rs.getInt("APPLICATION_ID"));
                application.setTier(rs.getString("APPLICATION_TIER"));
                Set<APIKey> keys = getApplicationKeys(tenantAwareUserId, rs.getInt("APPLICATION_ID"));
                for (APIKey key : keys) {
                    application.addKey(key);
                }
                applicationsList.add(application);
            }
            Collections.sort(applicationsList, new Comparator<Application>() {
                public int compare(Application o1, Application o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            applications = applicationsList.toArray(new Application[applicationsList.size()]);

        } catch (SQLException e) {
            handleException("Error when reading the application information from" +
                            " the persistence store.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return applications;
    }

    public void deleteApplication(Application application) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String getSubscriptionsQuery = "SELECT" +
                                       " SUBSCRIPTION_ID " +
                                       "FROM" +
                                       " AM_SUBSCRIPTION " +
                                       "WHERE" +
                                       " APPLICATION_ID = ?";

        String deleteKeyMappingQuery = "DELETE FROM AM_SUBSCRIPTION_KEY_MAPPING WHERE SUBSCRIPTION_ID = ?";
        String deleteSubscriptionsQuery = "DELETE FROM AM_SUBSCRIPTION WHERE APPLICATION_ID = ?";
        String deleteApplicationKeyQuery = "DELETE FROM AM_APPLICATION_KEY_MAPPING WHERE APPLICATION_ID = ?";
        String deleteApplicationQuery = "DELETE FROM AM_APPLICATION WHERE APPLICATION_ID = ?";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(getSubscriptionsQuery);
            prepStmt.setInt(1, application.getId());
            rs = prepStmt.executeQuery();

            List<Integer> subscriptions = new ArrayList<Integer>();
            while (rs.next()) {
                subscriptions.add(rs.getInt("SUBSCRIPTION_ID"));
            }
            prepStmt.close();
            rs.close();

            prepStmt = connection.prepareStatement(deleteKeyMappingQuery);
            for (Integer subscriptionId : subscriptions) {
                prepStmt.setInt(1, subscriptionId);
                prepStmt.execute();
            }
            prepStmt.close();

            prepStmt = connection.prepareStatement(deleteSubscriptionsQuery);
            prepStmt.setInt(1, application.getId());
            prepStmt.execute();
            prepStmt.close();

            prepStmt = connection.prepareStatement(deleteApplicationKeyQuery);
            prepStmt.setInt(1, application.getId());
            prepStmt.execute();
            prepStmt.close();

            prepStmt = connection.prepareStatement(deleteApplicationQuery);
            prepStmt.setInt(1, application.getId());
            prepStmt.execute();

            connection.commit();
        } catch (SQLException e) {
            handleException("Error while removing application details from the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }


    /**
     * returns a subscriber record for given username,tenant Id
     *
     * @param username   UserName
     * @param tenantId   Tenant Id
     * @param connection
     * @return Subscriber
     * @throws APIManagementException if failed to get subscriber
     */
    private Subscriber getSubscriber(String username, int tenantId, Connection connection)
            throws APIManagementException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Subscriber subscriber = null;
        String sqlQuery = "SELECT " +
                          "   SUB.SUBSCRIBER_ID AS SUBSCRIBER_ID" +
                          "   ,SUB.USER_ID AS USER_ID " +
                          "   ,SUB.TENANT_ID AS TENANT_ID" +
                          "   ,SUB.EMAIL_ADDRESS AS EMAIL_ADDRESS" +
                          "   ,SUB.DATE_SUBSCRIBED AS DATE_SUBSCRIBED " +
                          "FROM " +
                          "   AM_SUBSCRIBER SUB " +
                          "WHERE " +
                          "SUB.USER_ID = ? " +
                          "AND SUB.TENANT_ID = ?";


        try {
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setString(1, username);
            prepStmt.setInt(2, tenantId);
            rs = prepStmt.executeQuery();

            if (rs.next()) {
                subscriber = new Subscriber(rs.getString("USER_ID"));
                subscriber.setEmail(rs.getString("EMAIL_ADDRESS"));
                subscriber.setId(rs.getInt("SUBSCRIBER_ID"));
                subscriber.setSubscribedDate(rs.getDate("DATE_SUBSCRIBED"));
                subscriber.setTenantId(rs.getInt("TENANT_ID"));
                return subscriber;
            }
        } catch (SQLException e) {
            handleException("Error when reading the application information from" +
                            " the persistence store.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, null, rs);
        }
        return subscriber;
    }

    public void recordAPILifeCycleEvent(APIIdentifier identifier, APIStatus oldStatus,
                                        APIStatus newStatus, String userId)
            throws APIManagementException {
        Connection conn = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            recordAPILifeCycleEvent(identifier, oldStatus, newStatus, userId, conn);
        } catch (SQLException e) {
            handleException("Failed to record API state change", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(null, conn, null);
        }
    }

    public void recordAPILifeCycleEvent(APIIdentifier identifier, APIStatus oldStatus,
                                        APIStatus newStatus, String userId, Connection conn)
            throws APIManagementException {
        //Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;

        int tenantId;
        int apiId = -1;
        try {
            tenantId = IdentityUtil.getTenantIdOFUser(userId);
        } catch (IdentityException e) {
            String msg = "Failed to get tenant id of user : " + userId;
            log.error(msg, e);
            throw new APIManagementException(msg, e);
        }

        if (oldStatus == null && !newStatus.equals(APIStatus.CREATED)) {
            String msg = "Invalid old and new state combination";
            log.error(msg);
            throw new APIManagementException(msg);
        } else if (oldStatus != null && oldStatus.equals(newStatus)) {
            String msg = "No measurable differences in API state";
            log.error(msg);
            throw new APIManagementException(msg);
        }

        String getAPIQuery = "SELECT " +
                             "API.API_ID FROM AM_API API" +
                             " WHERE " +
                             "API.API_PROVIDER = ?" +
                             "AND API.API_NAME = ?" +
                             "AND API.API_VERSION = ?";

        String sqlQuery = "INSERT " +
                          "INTO AM_API_LC_EVENT (API_ID, PREVIOUS_STATE, NEW_STATE, USER_ID, TENANT_ID, EVENT_DATE)" +
                          " VALUES (?,?,?,?,?,?)";

        try {
            //conn = APIMgtDBUtil.getConnection();
            ps = conn.prepareStatement(getAPIQuery);
            ps.setString(1, identifier.getProviderName());
            ps.setString(2, identifier.getApiName());
            ps.setString(3, identifier.getVersion());
            resultSet = ps.executeQuery();
            if (resultSet.next()) {
                apiId = resultSet.getInt("API_ID");
            }
            resultSet.close();
            ps.close();
            if (apiId == -1) {
                String msg = "Unable to find the API: " + identifier + " in the database";
                log.error(msg);
                throw new APIManagementException(msg);
            }

            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, apiId);
            if (oldStatus != null) {
                ps.setString(2, oldStatus.getStatus());
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.setString(3, newStatus.getStatus());
            ps.setString(4, userId);
            ps.setInt(5, tenantId);
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));

            ps.executeUpdate();
            ps.close();
            // finally commit transaction
            //conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the API state change record", e);
                }
            }
            handleException("Failed to record API state change", e);
        } finally {
            //APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
    }

    public List<LifeCycleEvent> getLifeCycleEvents(APIIdentifier apiId)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String sqlQuery = "SELECT" +
                          " LC.API_ID AS API_ID," +
                          " LC.PREVIOUS_STATE AS PREVIOUS_STATE," +
                          " LC.NEW_STATE AS NEW_STATE," +
                          " LC.USER_ID AS USER_ID," +
                          " LC.EVENT_DATE AS EVENT_DATE " +
                          "FROM" +
                          " AM_API_LC_EVENT LC, " +
                          " AM_API API " +
                          "WHERE" +
                          " API.API_PROVIDER = ?" +
                          " AND API.API_NAME = ?" +
                          " AND API.API_VERSION = ?" +
                          " AND API.API_ID = LC.API_ID";

        List<LifeCycleEvent> events = new ArrayList<LifeCycleEvent>();

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setString(1, apiId.getProviderName());
            prepStmt.setString(2, apiId.getApiName());
            prepStmt.setString(3, apiId.getVersion());
            rs = prepStmt.executeQuery();

            while (rs.next()) {
                LifeCycleEvent event = new LifeCycleEvent();
                event.setApi(apiId);
                String oldState = rs.getString("PREVIOUS_STATE");
                event.setOldStatus(oldState != null ? APIStatus.valueOf(oldState) : null);
                event.setNewStatus(APIStatus.valueOf(rs.getString("NEW_STATE")));
                event.setUserId(rs.getString("USER_ID"));
                event.setDate(rs.getTimestamp("EVENT_DATE"));
                events.add(event);
            }

            Collections.sort(events, new Comparator<LifeCycleEvent>() {
                public int compare(LifeCycleEvent o1, LifeCycleEvent o2) {
                    return o1.getDate().compareTo(o2.getDate());
                }
            });
        } catch (SQLException e) {
            handleException("Error when executing the SQL : " + sqlQuery, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return events;
    }

    public void makeKeysForwardCompatible(String provider, String apiName, String oldVersion,
                                          String newVersion, String context)
            throws APIManagementException {

        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        String getSubscriptionDataQuery = "SELECT" +
                                          " SUB.SUBSCRIPTION_ID AS SUBSCRIPTION_ID," +
                                          " SUB.TIER_ID AS TIER_ID," +
                                          " SUB.APPLICATION_ID AS APPLICATION_ID," +
                                          " API.CONTEXT AS CONTEXT," +
                                          " SKM.ACCESS_TOKEN AS ACCESS_TOKEN," +
                                          " SKM.KEY_TYPE AS KEY_TYPE " +
                                          "FROM" +
                                          " AM_SUBSCRIPTION SUB," +
                                          " AM_SUBSCRIPTION_KEY_MAPPING SKM, " +
                                          " AM_API API " +
                                          "WHERE" +
                                          " API.API_PROVIDER = ?" +
                                          " AND API.API_NAME = ?" +
                                          " AND API.API_VERSION = ?" +
                                          " AND SKM.SUBSCRIPTION_ID = SUB.SUBSCRIPTION_ID" +
                                          " AND API.API_ID = SUB.API_ID";

        String addSubKeyMapping = "INSERT INTO" +
                                  " AM_SUBSCRIPTION_KEY_MAPPING (SUBSCRIPTION_ID, ACCESS_TOKEN, KEY_TYPE)" +
                                  " VALUES (?,?,?)";

        String getApplicationDataQuery = "SELECT" +
                                         " SUB.SUBSCRIPTION_ID AS SUBSCRIPTION_ID," +
                                         " SUB.TIER_ID AS TIER_ID," +
                                         " APP.APPLICATION_ID AS APPLICATION_ID," +
                                         " API.CONTEXT AS CONTEXT " +
                                         "FROM" +
                                         " AM_SUBSCRIPTION SUB," +
                                         " AM_APPLICATION APP," +
                                         " AM_API API " +
                                         "WHERE" +
                                         " API.API_PROVIDER = ?" +
                                         " AND API.API_NAME = ?" +
                                         " AND API.API_VERSION = ?" +
                                         " AND SUB.APPLICATION_ID = APP.APPLICATION_ID" +
                                         " AND API.API_ID = SUB.API_ID";

        try {
            // Retrieve all the existing subscription for the old version
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(getSubscriptionDataQuery);
            prepStmt.setString(1, provider);
            prepStmt.setString(2, apiName);
            prepStmt.setString(3, oldVersion);
            rs = prepStmt.executeQuery();

            List<SubscriptionInfo> subscriptionData = new ArrayList<SubscriptionInfo>();
            Set<Integer> subscribedApplications = new HashSet<Integer>();
            while (rs.next()) {
                SubscriptionInfo info = new SubscriptionInfo();
                info.subscriptionId = rs.getInt("SUBSCRIPTION_ID");
                info.tierId = rs.getString("TIER_ID");
                info.context = rs.getString("CONTEXT");
                info.applicationId = rs.getInt("APPLICATION_ID");
                info.accessToken = rs.getString("ACCESS_TOKEN");
                info.tokenType = rs.getString("KEY_TYPE");
                subscriptionData.add(info);
            }
            prepStmt.close();
            rs.close();

            Map<Integer, Integer> subscriptionIdMap = new HashMap<Integer, Integer>();
            APIIdentifier apiId = new APIIdentifier(provider, apiName, newVersion);
            for (SubscriptionInfo info : subscriptionData) {
                if (!subscriptionIdMap.containsKey(info.subscriptionId)) {
                    apiId.setTier(info.tierId);
                    int subscriptionId = addSubscription(apiId, context, info.applicationId);
                    if (subscriptionId == -1) {
                        String msg = "Unable to add a new subscription for the API: " + apiName +
                                     ":v" + newVersion;
                        log.error(msg);
                        throw new APIManagementException(msg);
                    }
                    subscriptionIdMap.put(info.subscriptionId, subscriptionId);
                }

                int subscriptionId = subscriptionIdMap.get(info.subscriptionId);
                prepStmt = connection.prepareStatement(addSubKeyMapping);
                prepStmt.setInt(1, subscriptionId);
                prepStmt.setString(2, info.accessToken);
                prepStmt.setString(3, info.tokenType);
                prepStmt.execute();
                prepStmt.close();

                subscribedApplications.add(info.applicationId);
            }

            prepStmt = connection.prepareStatement(getApplicationDataQuery);
            prepStmt.setString(1, provider);
            prepStmt.setString(2, apiName);
            prepStmt.setString(3, oldVersion);
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                int applicationId = rs.getInt("APPLICATION_ID");
                if (!subscribedApplications.contains(applicationId)) {
                    apiId.setTier(rs.getString("TIER_ID"));
                    addSubscription(apiId, rs.getString("CONTEXT"), applicationId);
                }
            }

            connection.commit();
        } catch (SQLException e) {
            handleException("Error when executing the SQL queries", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }

    public void addAPI(API api) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = "INSERT INTO AM_API (API_PROVIDER, API_NAME, API_VERSION, CONTEXT) " +
                       "VALUES (?,?,?,?)";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query, new String[]{"api_id"});
            prepStmt.setString(1, api.getId().getProviderName());
            prepStmt.setString(2, api.getId().getApiName());
            prepStmt.setString(3, api.getId().getVersion());
            prepStmt.setString(4, api.getContext());
            prepStmt.execute();


            rs = prepStmt.getGeneratedKeys();
            int applicationId = -1;
            if (rs.next()) {
                applicationId = rs.getInt(1);
            }
            addURLTemplates(applicationId, api, connection);
            recordAPILifeCycleEvent(api.getId(), null, APIStatus.CREATED, api.getId().getProviderName(), connection);
            connection.commit();
        } catch (SQLException e) {
            handleException("Error while adding the API: " + api.getId() + " to the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }

    /**
     * Adds URI templates define for an API
     *
     * @param apiId
     * @param api
     * @param connection
     * @throws APIManagementException
     */
    public void addURLTemplates(int apiId, API api, Connection connection)
            throws APIManagementException {
        if (apiId == -1) {
            //application addition has failed
            return;
        }
        PreparedStatement prepStmt = null;
        String query = "INSERT INTO AM_API_URL_MAPPING (API_ID,HTTP_METHOD,AUTH_SCHEME,URL_PATTERN) VALUES (?,?,?,?)";
        try {
            //connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);

            Iterator<URITemplate> uriTemplateIterator = api.getUriTemplates().iterator();
            URITemplate uriTemplate;
            for (; uriTemplateIterator.hasNext();) {
                uriTemplate = uriTemplateIterator.next();
                prepStmt.setInt(1, apiId);
                prepStmt.setString(2, uriTemplate.getHTTPVerb());
                prepStmt.setString(3, uriTemplate.getAuthType());
                prepStmt.setString(4, uriTemplate.getUriTemplate());
                prepStmt.addBatch();
            }
            prepStmt.executeBatch();
            prepStmt.clearBatch();

        } catch (SQLException e) {
            handleException("Error while adding URL template(s) to the database for API : " + api.getId().toString(), e);
        }
    }

    /**
     * update URI templates define for an API
     *
     * @param api
     * @param connection
     * @throws APIManagementException
     */
    public void updateURLTemplates(API api, Connection connection)
            throws APIManagementException {
        int apiId = getAPIID(api.getId(),connection);
        if (apiId == -1) {
            //application addition has failed
            return;
        }
        PreparedStatement prepStmt = null;
        String deleteOldMappingsQuery = "DELETE FROM AM_API_URL_MAPPING WHERE API_ID = ?";
        try {
            prepStmt = connection.prepareStatement(deleteOldMappingsQuery);
            prepStmt.setInt(1,apiId);
            prepStmt.execute();
        } catch (SQLException e) {
            handleException("Error while deleting URL template(s) for API : " + api.getId().toString(), e);
        }
        addURLTemplates(apiId,api,connection);
    }

    /**
     * returns all URL templates define for all active(PUBLISHED) APIs.
     */
    public static ArrayList<URITemplate> getAllURITemplates(String apiContext, String version)
            throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        ArrayList<URITemplate> uriTemplates = new ArrayList<URITemplate>();

        //TODO : FILTER RESULTS ONLY FOR ACTIVE APIs
        String query =
                "SELECT AUM.HTTP_METHOD,AUTH_SCHEME,URL_PATTERN FROM AM_API_URL_MAPPING AUM, AM_API API " +
                "WHERE API.CONTEXT= ? " +
                "AND API.API_VERSION = ? " +
                "AND AUM.API_ID = API.API_ID " +
                "ORDER BY " +
                "URL_MAPPING_ID";
        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);
            prepStmt.setString(1, apiContext);
            prepStmt.setString(2, version);

            rs = prepStmt.executeQuery();

            URITemplate uriTemplate;
            while (rs.next()) {
                uriTemplate = new URITemplate();
                uriTemplate.setHTTPVerb(rs.getString("HTTP_METHOD"));
                uriTemplate.setAuthType(rs.getString("AUTH_SCHEME"));
                uriTemplate.setUriTemplate(rs.getString("URL_PATTERN"));
                uriTemplates.add(uriTemplate);
            }
        } catch (SQLException e) {
            handleException("Error while fetching all URL Templates", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return uriTemplates;
    }


    public void updateAPI(API api) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        String query = "UPDATE AM_API SET CONTEXT = ? WHERE API_PROVIDER = ? AND API_NAME = ? AND" +
                " API_VERSION = ? ";
        try {
            connection = APIMgtDBUtil.getConnection();
            if(api.isApiHeaderChanged()){
                prepStmt = connection.prepareStatement(query);
                prepStmt.setString(1, api.getContext());
                prepStmt.setString(2, api.getId().getProviderName());
                prepStmt.setString(3, api.getId().getApiName());
                prepStmt.setString(4, api.getId().getVersion());
                prepStmt.execute();
            }
            updateURLTemplates(api, connection);
            connection.commit();

        } catch (SQLException e) {
            handleException("Error while updating the API: " + api.getId() + " in the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }

    public static int getAPIID(APIIdentifier apiId, Connection connection) throws APIManagementException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        int id = -1;
        String getAPIQuery = "SELECT " +
                "API.API_ID FROM AM_API API" +
                " WHERE " +
                "API.API_PROVIDER = ?" +
                "AND API.API_NAME = ?" +
                "AND API.API_VERSION = ?";

        try {
            prepStmt = connection.prepareStatement(getAPIQuery);
            prepStmt.setString(1, apiId.getProviderName());
            prepStmt.setString(2, apiId.getApiName());
            prepStmt.setString(3, apiId.getVersion());
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                id = rs.getInt("API_ID");
            }
            if (id == -1) {
                String msg = "Unable to find the API: " + apiId + " in the database";
                log.error(msg);
                throw new APIManagementException(msg);
            }
        } catch (SQLException e) {
            handleException("Error while locating API: " + apiId + " from the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, null, rs);
        }
        return id;
    }



    public void deleteAPI(APIIdentifier apiId) throws APIManagementException {
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        int id = -1;
        String deleteLCEventQuery = "DELETE FROM AM_API_LC_EVENT WHERE API_ID=? ";
        String deleteSubscriptionQuery = "DELETE FROM AM_SUBSCRIPTION WHERE API_ID=?";
        String deleteAPIQuery = "DELETE FROM AM_API WHERE API_PROVIDER=? AND API_NAME=? AND API_VERSION=? ";

        try {
            connection = APIMgtDBUtil.getConnection();
            id = getAPIID(apiId,connection);
            prepStmt = connection.prepareStatement(deleteSubscriptionQuery);
            prepStmt.setInt(1, id);
            prepStmt.execute();

            prepStmt = connection.prepareStatement(deleteLCEventQuery);
            prepStmt.setInt(1, id);
            prepStmt.execute();

            prepStmt = connection.prepareStatement(deleteAPIQuery);
            prepStmt.setString(1, apiId.getProviderName());
            prepStmt.setString(2, apiId.getApiName());
            prepStmt.setString(3, apiId.getVersion());
            prepStmt.execute();

            connection.commit();

        } catch (SQLException e) {
            handleException("Error while removing the API: " + apiId + " from the database", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
    }


    /**
     * Change access token status in to revoked in database level.
     *
     * @param key API Key to be revoked
     * @throws APIManagementException on error in revoking access token
     */
    public void revokeAccessToken(String key) throws APIManagementException {

        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(key);
        }
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            conn = APIMgtDBUtil.getConnection();
            String query = "UPDATE " + accessTokenStoreTable + " SET TOKEN_STATE='REVOKED' WHERE ACCESS_TOKEN= ? ";
            ps = conn.prepareStatement(query);
            ps.setString(1, key);
            ps.execute();
            conn.commit();
        } catch (SQLException e) {
            handleException("Error in revoking access token: " + e.getMessage(), e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, rs);
        }
    }


    /**
     * Get APIIdentifiers Associated with access token - access token associated with application
     * which has multiple APIs. so this returns all APIs associated with a access token
     *
     * @param accessToken String access token
     * @return APIIdentifier set for all API's associated with given access token
     * @throws APIManagementException error in getting APIIdentifiers
     */
    public Set<APIIdentifier> getAPIByAccessToken(String accessToken)
            throws APIManagementException {
        String accessTokenStoreTable = APIConstants.ACCESS_TOKEN_STORE_TABLE;
        if (APIUtil.checkAccessTokenPartitioningEnabled() &&
                APIUtil.checkUserNameAssertionEnabled()) {
            accessTokenStoreTable = APIUtil.getAccessTokenStoreTableFromAccessToken(accessToken);
        }
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String getAPISql = "SELECT AMA.API_ID,API_NAME,API_PROVIDER,API_VERSION FROM " +
                           "AM_API AMA," + accessTokenStoreTable +" ACT, AM_APPLICATION_KEY_MAPPING AKM, " +
                           "AM_SUBSCRIPTION AMS WHERE ACT.ACCESS_TOKEN=? " +
                           "AND ACT.CONSUMER_KEY=AKM.CONSUMER_KEY AND AKM.APPLICATION_ID=AMS.APPLICATION_ID AND " +
                           "AMA.API_ID=AMS.API_ID";
        Set<APIIdentifier> apiList = new HashSet<APIIdentifier>();
        try {
            connection = APIMgtDBUtil.getConnection();
            PreparedStatement nestedPS = connection.prepareStatement(getAPISql);
            nestedPS.setString(1, accessToken);
            ResultSet nestedRS = nestedPS.executeQuery();
            while (nestedRS.next()) {
                apiList.add(new APIIdentifier(nestedRS.getString("API_PROVIDER"),
                                              nestedRS.getString("API_NAME"),
                                              nestedRS.getString("API_VERSION")));
            }
        } catch (SQLException e) {
            handleException("Failed to get API ID for token: " + accessToken, e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, connection, result);
        }
        return apiList;
    }


    /**
     * Get all applications associated with given tier
     *
     * @param tier String tier name
     * @return Application object array associated with tier
     * @throws APIManagementException on error in getting applications array
     */
    public Application[] getApplicationsByTier(String tier) throws APIManagementException {
        if (tier == null) {
            return null;
        }
        Connection connection = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Application[] applications = null;

        String sqlQuery = "SELECT DISTINCT AMS.APPLICATION_ID,NAME,SUBSCRIBER_ID FROM AM_SUBSCRIPTION AMS,AM_APPLICATION AMA " +
                          "WHERE TIER_ID=? " +
                          "AND AMS.APPLICATION_ID=AMA.APPLICATION_ID";

        try {
            connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(sqlQuery);
            prepStmt.setString(1, tier);
            rs = prepStmt.executeQuery();
            ArrayList<Application> applicationsList = new ArrayList<Application>();
            Application application;
            while (rs.next()) {
                application = new Application(rs.getString("NAME"), getSubscriber(rs.getString("SUBSCRIBER_ID")));
                application.setId(rs.getInt("APPLICATION_ID"));
            }
            Collections.sort(applicationsList, new Comparator<Application>() {
                public int compare(Application o1, Application o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            applications = applicationsList.toArray(new Application[applicationsList.size()]);

        } catch (SQLException e) {
            handleException("Error when reading the application information from" +
                            " the persistence store.", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(prepStmt, connection, rs);
        }
        return applications;
    }

    private static void handleException(String msg, Throwable t) throws APIManagementException {
        log.error(msg, t);
        throw new APIManagementException(msg, t);
    }

    /**
     * Generates fresh JWT token for given information of validation information
     *
     * @param context         String context for API
     * @param version         version of API
     * @param subscriberName  subscribed user name
     * @param applicationName application name api belongs
     * @param tier            tier name
     * @param endUserName     name of end user
     * @return signed JWT token string
     * @throws APIManagementException error in generating token
     */
    public String createJWTTokenString(String context, String version, String subscriberName,
                                       String applicationName, String tier, String endUserName)
            throws APIManagementException {
        String calleeToken = null;
        if (jwtGenerator != null) {
            calleeToken = jwtGenerator.generateToken(subscriberName, applicationName, context, version, tier, endUserName);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("JWT generator not properly initialized. JWT token will not present in validation info");
            }
        }
        return calleeToken;
    }


    public static HashMap<String,String> getURITemplatesPerAPIAsString(APIIdentifier identifier)
            throws APIManagementException {
        Connection conn = null;
        ResultSet resultSet = null;
        PreparedStatement ps = null;
        int apiId = -1;
        HashMap<String,String> urlMappings = new LinkedHashMap<String, String>();

        try {
            conn = APIMgtDBUtil.getConnection();
            apiId = getAPIID(identifier,conn);

            String sqlQuery =
                    "SELECT " +
                            "URL_PATTERN" +
                            ",HTTP_METHOD" +
                            ",AUTH_SCHEME " +
                    "FROM " +
                            "AM_API_URL_MAPPING " +
                    "WHERE " +
                            "API_ID = ? " +
                    "ORDER BY " +
                            "URL_MAPPING_ID ASC ";


            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, apiId);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String uriPattern = resultSet.getString("URL_PATTERN");
                String httpMethod = resultSet.getString("HTTP_METHOD");
                String authScheme = resultSet.getString("AUTH_SCHEME");
                urlMappings.put(uriPattern + "::" + httpMethod + "::" + authScheme,null);
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    log.error("Failed to rollback the add subscription ", e);
                }
            }
            handleException("Failed to add subscriber data ", e);
        } finally {
            APIMgtDBUtil.closeAllConnections(ps, conn, resultSet);
        }
        return urlMappings;
    }

    private static class SubscriptionInfo {
        private int subscriptionId;
        private String tierId;
        private String context;
        private int applicationId;
        private String accessToken;
        private String tokenType;
    }
}
