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

package org.wso2.carbon.apimgt.usage.client;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.AXIOMUtil;
import org.wso2.carbon.apimgt.api.APIConsumer;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.API;
import org.wso2.carbon.apimgt.api.model.SubscribedAPI;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.impl.APIManagerConfiguration;
import org.wso2.carbon.apimgt.impl.APIManagerFactory;
import org.wso2.carbon.apimgt.usage.client.billing.PaymentPlan;
import org.wso2.carbon.apimgt.usage.client.dto.*;
import org.wso2.carbon.apimgt.usage.client.exception.APIMgtUsageQueryServiceClientException;
import org.wso2.carbon.apimgt.usage.client.internal.APIUsageClientServiceComponent;
import org.wso2.carbon.bam.presentation.stub.QueryServiceStub;
import org.wso2.carbon.utils.CarbonUtils;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class APIUsageStatisticsClient {

    private APIProvider apiProviderImpl;
    private APIConsumer apiConsumerImpl;
    private static volatile DataSource dataSource = null;
    private static PaymentPlan paymentPlan;
    private static final String API_USAGE_TRACKING = "APIUsageTracking.";
    private static final String DATA_SOURCE_NAME = API_USAGE_TRACKING + "DataSourceName";
   /* private static String text = "    <PaymentPlan name=\"platinam\">    <parameter name=\"call\">  " +
            "      <range0><start>0</start><end>5</end><value>0.0</value></range0>      " +
            "  <range1><start>5</start><end>10</end><value>2.0</value></range1>   " +
            "     <range2><start>10</start><end>15000</end><value>5.0</value></range2>  " +
            "  </parameter>    <parameter name=\"data\">        " +
            "<range0><start>0</start><end>10</end><value>0.0</value></range0>    " +
            "    <range1><start>10</start><end>20</end><value>1.0</value></range1>   " +
            "     <range2><start>20</start><end>30000</end><value>2.0</value></range2>   " +
            " </parameter>    <parameter name=\"messages\">        " +
            "<range0><start>0</start><end>10</end><value>0.5</value></range0>       " +
            " <range1><start>10</start><end>20000</end><value>1.0</value></range1>    </parameter></PaymentPlan>";
     */
    /* public APIUsageStatisticsClient(String username) throws APIMgtUsageQueryServiceClientException {
        APIManagerConfiguration config = APIUsageClientServiceComponent.getAPIManagerConfiguration();
        String targetEndpoint = config.getFirstProperty(APIMgtUsagePublisherConstants.API_USAGE_BAM_SERVER_URL);
        if (targetEndpoint == null || targetEndpoint.equals("")) {
            throw new APIMgtUsageQueryServiceClientException("Required BAM server URL parameter unspecified");
        }

        try {
            apiProviderImpl = APIManagerFactory.getInstance().getAPIProvider(username);
        } catch (APIManagementException e) {
            throw new APIMgtUsageQueryServiceClientException("Exception while instantiating API manager core objects", e);
        }

    }*/
    public APIUsageStatisticsClient(String username)
            throws APIMgtUsageQueryServiceClientException {
        OMElement element = null;
        APIManagerConfiguration config;
        try {
            config = APIUsageClientServiceComponent.getAPIManagerConfiguration();
           // text = config.getFirstProperty("BillingConfig");
            String billingConfig = config.getFirstProperty("EnableBillingAndUsage");
            boolean isBillingEnabled = Boolean.parseBoolean(billingConfig);
            if(isBillingEnabled){
            String filePath = (new StringBuilder()).append(CarbonUtils.getCarbonHome()).append(File.separator).append("repository").append(File.separator).append("conf").append(File.separator).append("billing-conf.xml").toString();
            element = buildOMElement(new FileInputStream(filePath));
            paymentPlan = new PaymentPlan(element);
            }
            String targetEndpoint = config.getFirstProperty("APIUsageTracking.BAMServerURL");
            if (targetEndpoint == null || targetEndpoint.equals(""))
                throw new APIMgtUsageQueryServiceClientException("Required BAM server URL parameter unspecified");
            apiProviderImpl = APIManagerFactory.getInstance().getAPIProvider(username);

        } catch (Exception e) {
            throw new APIMgtUsageQueryServiceClientException("Exception while instantiating API manager core objects", e);
        }


    }

    public static void initializeDataSource() throws APIMgtUsageQueryServiceClientException {
        if (dataSource != null) {
            return;
        }
        APIManagerConfiguration config = APIUsageClientServiceComponent.getAPIManagerConfiguration();
        String dataSourceName = config.getFirstProperty(DATA_SOURCE_NAME);

        if (dataSourceName != null) {
            try {
                Context ctx = new InitialContext();
                dataSource = (DataSource) ctx.lookup(dataSourceName);
            } catch (NamingException e) {
                throw new APIMgtUsageQueryServiceClientException("Error while looking up the data " +
                        "source: " + dataSourceName);
            }

        }
    }

    /**
     * Returns a list of APIUsageDTO objects that contain information related to APIs that
     * belong to a particular provider and the number of total API calls each API has processed
     * up to now. This method does not distinguish between different API versions. That is all
     * versions of a single API are treated as one, and their individual request counts are summed
     * up to calculate a grand total per each API.
     *
     * @param providerName Name of the API provider
     * @return a List of APIUsageDTO objects - possibly empty
     * @throws APIMgtUsageQueryServiceClientException
     *          if an error occurs while contacting backend services
     */
    public List<APIUsageDTO> getUsageByAPIs(String providerName, int limit)
            throws APIMgtUsageQueryServiceClientException {

        OMElement omElement = this.queryDatabase(
                APIUsageStatisticsClientConstants.API_VERSION_USAGE_SUMMARY, null);
        Collection<APIUsage> usageData = getUsageData(omElement);
        List<API> providerAPIs = getAPIsByProvider(providerName);
        Map<String, APIUsageDTO> usageByAPIs = new TreeMap<String, APIUsageDTO>();
        for (APIUsage usage : usageData) {
            for (API providerAPI : providerAPIs) {
                if (providerAPI.getId().getApiName().equals(usage.apiName) &&
                        providerAPI.getId().getVersion().equals(usage.apiVersion) &&
                        providerAPI.getContext().equals(usage.context)) {
                    String apiName = usage.apiName + " (" + providerAPI.getId().getProviderName() + ")";
                    APIUsageDTO usageDTO = usageByAPIs.get(apiName);
                    if (usageDTO != null) {
                        usageDTO.setCount(usageDTO.getCount() + usage.requestCount);
                    } else {
                        usageDTO = new APIUsageDTO();
                        usageDTO.setApiName(apiName);
                        usageDTO.setCount(usage.requestCount);
                        usageByAPIs.put(apiName, usageDTO);
                    }
                }
            }
        }
        return getAPIUsageTopEntries(new ArrayList<APIUsageDTO>(usageByAPIs.values()), limit);
    }

    /**
     * Returns a list of APIVersionUsageDTO objects that contain information related to a
     * particular API of a specified provider, along with the number of API calls processed
     * by each version of that API.
     *
     * @param providerName Name of the API provider
     * @param apiName      Name of th API
     * @return a List of APIVersionUsageDTO objects, possibly empty
     * @throws APIMgtUsageQueryServiceClientException
     *          on error
     */
    public List<APIVersionUsageDTO> getUsageByAPIVersions(String providerName,
                                                          String apiName) throws APIMgtUsageQueryServiceClientException {

        QueryServiceStub.CompositeIndex[] compositeIndex = new QueryServiceStub.CompositeIndex[1];
        compositeIndex[0] = new QueryServiceStub.CompositeIndex();
        compositeIndex[0].setIndexName("api");
        compositeIndex[0].setRangeFirst(apiName);
        compositeIndex[0].setRangeLast(getNextStringInLexicalOrder(apiName));
        OMElement omElement = this.queryDatabase(
                APIUsageStatisticsClientConstants.API_VERSION_USAGE_SUMMARY, compositeIndex);
        Collection<APIUsage> usageData = getUsageData(omElement);
        List<API> providerAPIs = getAPIsByProvider(providerName);
        Map<String, APIVersionUsageDTO> usageByVersions = new TreeMap<String, APIVersionUsageDTO>();

        for (APIUsage usage : usageData) {
            for (API providerAPI : providerAPIs) {
                if (providerAPI.getId().getApiName().equals(usage.apiName) &&
                        providerAPI.getId().getVersion().equals(usage.apiVersion) &&
                        providerAPI.getContext().equals(usage.context)) {

                    APIVersionUsageDTO usageDTO = new APIVersionUsageDTO();
                    usageDTO.setVersion(usage.apiVersion);
                    usageDTO.setCount(usage.requestCount);
                    usageByVersions.put(usage.apiVersion, usageDTO);
                }
            }
        }

        return new ArrayList<APIVersionUsageDTO>(usageByVersions.values());
    }

    /**
     * Returns a list of APIVersionUsageDTO objects that contain information related to a
     * particular API of a specified provider, along with the number of API calls processed
     * by each resource path of that API.
     *
     * @param providerName Name of the API provider
     * @return a List of APIResourcePathUsageDTO objects, possibly empty
     * @throws APIMgtUsageQueryServiceClientException
     *          on error
     */
    public List<APIResourcePathUsageDTO> getAPIUsageByResourcePath(String providerName)
            throws APIMgtUsageQueryServiceClientException {

        OMElement omElement = this.queryDatabase(
                APIUsageStatisticsClientConstants.API_Resource_Path_USAGE_SUMMARY, null);
        Collection<APIUsageByResourcePath> usageData = getUsageDataByResourcePath(omElement);
        List<API> providerAPIs = getAPIsByProvider(providerName);
        List<APIResourcePathUsageDTO> usageByResourcePath = new ArrayList<APIResourcePathUsageDTO>();

        for (APIUsageByResourcePath usage : usageData) {
            for (API providerAPI : providerAPIs) {
                if (providerAPI.getId().getApiName().equals(usage.apiName) &&
                        providerAPI.getId().getVersion().equals(usage.apiVersion) &&
                        providerAPI.getContext().equals(usage.context)) {

                    APIResourcePathUsageDTO usageDTO = new APIResourcePathUsageDTO();
                    usageDTO.setApiName(usage.apiName);
                    usageDTO.setVersion(usage.apiVersion);
                    usageDTO.setContext(usage.context);
                    usageDTO.setResource(usage.resource);
                    usageDTO.setCount(usage.requestCount);
                    usageByResourcePath.add(usageDTO);
                }
            }
        }
        return usageByResourcePath;
    }

    /**
     * Gets a list of APIResponseTimeDTO objects containing information related to APIs belonging
     * to a particular provider along with their average response times.
     *
     * @param providerName Name of the API provider
     * @return a List of APIResponseTimeDTO objects, possibly empty
     * @throws APIMgtUsageQueryServiceClientException
     *          on error
     */
    public List<APIResponseTimeDTO> getResponseTimesByAPIs(String providerName, int limit)
            throws APIMgtUsageQueryServiceClientException {

        OMElement omElement = this.queryDatabase(
                APIUsageStatisticsClientConstants.API_VERSION_SERVICE_TIME_SUMMARY, null);
        Collection<APIResponseTime> responseTimes = getResponseTimeData(omElement);
        List<API> providerAPIs = getAPIsByProvider(providerName);
        Map<String, Double> apiCumulativeServiceTimeMap = new HashMap<String, Double>();
        Map<String, Long> apiUsageMap = new TreeMap<String, Long>();
        for (APIResponseTime responseTime : responseTimes) {
            for (API providerAPI : providerAPIs) {
                if (providerAPI.getId().getApiName().equals(responseTime.apiName) &&
                        providerAPI.getId().getVersion().equals(responseTime.apiVersion) &&
                        providerAPI.getContext().equals(responseTime.context)) {
                    Double cumulativeResponseTime = apiCumulativeServiceTimeMap.get(responseTime.apiName);
                    String apiName = responseTime.apiName + " (" + providerAPI.getId().getProviderName() + ")";
                    if (cumulativeResponseTime != null) {
                        apiCumulativeServiceTimeMap.put(apiName,
                                cumulativeResponseTime + responseTime.responseTime * responseTime.responseCount);
                        apiUsageMap.put(apiName,
                                apiUsageMap.get(apiName) + responseTime.responseCount);
                    } else {
                        apiCumulativeServiceTimeMap.put(apiName,
                                responseTime.responseTime * responseTime.responseCount);
                        apiUsageMap.put(apiName, responseTime.responseCount);
                    }
                }
            }
        }

        Map<String, APIResponseTimeDTO> responseTimeByAPI = new TreeMap<String, APIResponseTimeDTO>();
        DecimalFormat format = new DecimalFormat("#.##");
        for (String key : apiUsageMap.keySet()) {
            APIResponseTimeDTO responseTimeDTO = new APIResponseTimeDTO();
            responseTimeDTO.setApiName(key);
            double responseTime = apiCumulativeServiceTimeMap.get(key) / apiUsageMap.get(key);
            responseTimeDTO.setServiceTime(Double.parseDouble(format.format(responseTime)));
            responseTimeByAPI.put(key, responseTimeDTO);
        }
        return getResponseTimeTopEntries(new ArrayList<APIResponseTimeDTO>(responseTimeByAPI.values()), limit);
    }

    /**
     * Returns a list of APIVersionLastAccessTimeDTO objects for all the APIs belonging to the
     * specified provider. Last access times are calculated without taking API versions into
     * account. That is all the versions of an API are treated as one.
     *
     * @param providerName Name of the API provider
     * @return a list of APIVersionLastAccessTimeDTO objects, possibly empty
     * @throws APIMgtUsageQueryServiceClientException
     *          on error
     */
    public List<APIVersionLastAccessTimeDTO> getLastAccessTimesByAPI(String providerName, int limit)
            throws APIMgtUsageQueryServiceClientException {

        OMElement omElement = this.queryDatabase(
                APIUsageStatisticsClientConstants.API_VERSION_KEY_LAST_ACCESS_SUMMARY, null);
        Collection<APIAccessTime> accessTimes = getAccessTimeData(omElement);
        List<API> providerAPIs = getAPIsByProvider(providerName);
        Map<String, APIAccessTime> lastAccessTimes = new TreeMap<String, APIAccessTime>();
        for (APIAccessTime accessTime : accessTimes) {
            for (API providerAPI : providerAPIs) {
                if (providerAPI.getId().getApiName().equals(accessTime.apiName) &&
                        providerAPI.getId().getVersion().equals(accessTime.apiVersion) &&
                        providerAPI.getContext().equals(accessTime.context)) {

                    String apiName = accessTime.apiName + " (" + providerAPI.getId().getProviderName() + ")";
                    APIAccessTime lastAccessTime = lastAccessTimes.get(apiName);
                    if (lastAccessTime == null || lastAccessTime.accessTime < accessTime.accessTime) {
                        lastAccessTimes.put(apiName, accessTime);
                        break;
                    }
                }
            }
        }
        Map<String, APIVersionLastAccessTimeDTO> accessTimeByAPI = new TreeMap<String, APIVersionLastAccessTimeDTO>();
        List<APIVersionLastAccessTimeDTO> accessTimeDTOs = new ArrayList<APIVersionLastAccessTimeDTO>();
        DateFormat dateFormat = new SimpleDateFormat();
        for (Map.Entry<String, APIAccessTime> entry : lastAccessTimes.entrySet()) {
            APIVersionLastAccessTimeDTO accessTimeDTO = new APIVersionLastAccessTimeDTO();
            accessTimeDTO.setApiName(entry.getKey());
            APIAccessTime lastAccessTime = entry.getValue();
            accessTimeDTO.setApiVersion(lastAccessTime.apiVersion);
            accessTimeDTO.setLastAccessTime(dateFormat.format(lastAccessTime.accessTime));
            accessTimeDTO.setUser(lastAccessTime.username);
            accessTimeByAPI.put(entry.getKey(), accessTimeDTO);
        }
        return getLastAccessTimeTopEntries(new ArrayList<APIVersionLastAccessTimeDTO>(accessTimeByAPI.values()), limit);

    }

    /**
     * Returns a sorted list of PerUserAPIUsageDTO objects related to a particular API. The returned
     * list will only have at most limit + 1 entries. This method does not differentiate between
     * API versions.
     *
     * @param providerName API provider name
     * @param apiName      Name of the API
     * @param limit        Number of sorted entries to return
     * @return a List of PerUserAPIUsageDTO objects - Possibly empty
     * @throws APIMgtUsageQueryServiceClientException
     *          on error
     */
    public List<PerUserAPIUsageDTO> getUsageBySubscribers(String providerName, String apiName, int limit)
            throws APIMgtUsageQueryServiceClientException {

        OMElement omElement = this.queryDatabase(
                APIUsageStatisticsClientConstants.KEY_USAGE_SUMMARY, null);
        Collection<APIUsageByUser> usageData = getUsageBySubscriber(omElement);
        Map<String, PerUserAPIUsageDTO> usageByUsername = new TreeMap<String, PerUserAPIUsageDTO>();
        List<API> apiList = getAPIsByProvider(providerName);
        for (APIUsageByUser usageEntry : usageData) {
            for (API api : apiList) {
                if (api.getContext().equals(usageEntry.context) &&
                        api.getId().getApiName().equals(apiName)) {
                    PerUserAPIUsageDTO usageDTO = usageByUsername.get(usageEntry.username);
                    if (usageDTO != null) {
                        usageDTO.setCount(usageDTO.getCount() + usageEntry.requestCount);
                    } else {
                        usageDTO = new PerUserAPIUsageDTO();
                        usageDTO.setUsername(usageEntry.username);
                        usageDTO.setCount(usageEntry.requestCount);
                        usageByUsername.put(usageEntry.username, usageDTO);
                    }
                    break;
                }
            }
        }

        return getTopEntries(new ArrayList<PerUserAPIUsageDTO>(usageByUsername.values()), limit);
    }

    public List<APIResponseFaultCountDTO> getAPIResponseFaultCount(String providerName)
            throws APIMgtUsageQueryServiceClientException {

        OMElement omElement = this.queryDatabase(
                APIUsageStatisticsClientConstants.API_FAULT_SUMMARY, null);
        Collection<APIResponseFaultCount> faultyData = getAPIResponseFaultCount(omElement);
        List<API> providerAPIs = getAPIsByProvider(providerName);
        List<APIResponseFaultCountDTO> faultyCount = new ArrayList<APIResponseFaultCountDTO>();
        List<APIVersionUsageDTO> apiVersionUsageList;
        APIVersionUsageDTO apiVersionUsageDTO;
        for (APIResponseFaultCount fault : faultyData) {
            for (API providerAPI : providerAPIs) {
                if (providerAPI.getId().getApiName().equals(fault.apiName) &&
                        providerAPI.getId().getVersion().equals(fault.apiVersion) &&
                        providerAPI.getContext().equals(fault.context)) {

                    APIResponseFaultCountDTO faultyDTO = new APIResponseFaultCountDTO();
                    faultyDTO.setApiName(fault.apiName);
                    faultyDTO.setVersion(fault.apiVersion);
                    faultyDTO.setContext(fault.context);
                    faultyDTO.setCount(fault.faultCount);

                    apiVersionUsageList = getUsageByAPIVersions(providerName, fault.apiName);
                    for (int i = 0; i < apiVersionUsageList.size(); i++) {
                        apiVersionUsageDTO = apiVersionUsageList.get(i);
                        if (apiVersionUsageDTO.getVersion().equals(fault.apiVersion)) {
                            double requestCount = apiVersionUsageDTO.getCount();
                            double faultPercentage = (requestCount - fault.faultCount) / requestCount * 100;
                            DecimalFormat twoDForm = new DecimalFormat("#.##");
                            faultPercentage = 100 - Double.valueOf(twoDForm.format(faultPercentage));
                            faultyDTO.setFaultPercentage(faultPercentage);
                            break;
                        }
                    }

                    faultyCount.add(faultyDTO);

                }
            }
        }
        return faultyCount;
    }

    public List<APIResponseFaultCountDTO> getAPIFaultyAnalyzeByTime(String providerName)
            throws APIMgtUsageQueryServiceClientException {

        OMElement omElement = this.queryDatabase(
                APIUsageStatisticsClientConstants.API_REQUEST_TIME_FAULT_SUMMARY, null);
        Collection<APIResponseFaultCount> faultyData = getAPIResponseFaultCount(omElement);
        List<API> providerAPIs = getAPIsByProvider(providerName);
        List<APIResponseFaultCountDTO> faultyInvocations = new ArrayList<APIResponseFaultCountDTO>();

        for (APIResponseFaultCount fault : faultyData) {
            for (API providerAPI : providerAPIs) {
                if (providerAPI.getId().getApiName().equals(fault.apiName) &&
                        providerAPI.getId().getVersion().equals(fault.apiVersion) &&
                        providerAPI.getContext().equals(fault.context)) {

                    APIResponseFaultCountDTO faultyDTO = new APIResponseFaultCountDTO();
                    faultyDTO.setApiName(fault.apiName + ":" + providerAPI.getId().getProviderName());
                    faultyDTO.setVersion(fault.apiVersion);
                    faultyDTO.setContext(fault.context);
                    faultyDTO.setRequestTime(fault.requestTime);
                    faultyInvocations.add(faultyDTO);
                }
            }
        }
        return faultyInvocations;
    }

    public List<PerUserAPIUsageDTO> getUsageBySubscribers(String providerName, String apiName,
                                                          String apiVersion, int limit) throws APIMgtUsageQueryServiceClientException {

        OMElement omElement = this.queryDatabase(
                APIUsageStatisticsClientConstants.KEY_USAGE_SUMMARY, null);

        Collection<APIUsageByUser> usageData = getUsageBySubscriber(omElement);
        Map<String, PerUserAPIUsageDTO> usageByUsername = new TreeMap<String, PerUserAPIUsageDTO>();
        List<API> apiList = getAPIsByProvider(providerName);
        for (APIUsageByUser usageEntry : usageData) {
            for (API api : apiList) {
                if (api.getContext().equals(usageEntry.context) &&
                        api.getId().getApiName().equals(apiName) &&
                        api.getId().getVersion().equals(apiVersion) &&
                        apiVersion.equals(usageEntry.apiVersion)) {
                    PerUserAPIUsageDTO usageDTO = usageByUsername.get(usageEntry.username);
                    if (usageDTO != null) {
                        usageDTO.setCount(usageDTO.getCount() + usageEntry.requestCount);
                    } else {
                        usageDTO = new PerUserAPIUsageDTO();
                        usageDTO.setUsername(usageEntry.username);
                        usageDTO.setCount(usageEntry.requestCount);
                        usageByUsername.put(usageEntry.username, usageDTO);
                    }
                    break;
                }
            }
        }

        return getTopEntries(new ArrayList<PerUserAPIUsageDTO>(usageByUsername.values()), limit);
    }

    public List<APIVersionUserUsageDTO> getUsageBySubscriber(String subscriberName, String period) throws Exception, APIManagementException {
        Map<String, Object> result = new HashMap<String, Object>();
        OMElement omElement;
        // Collection<APIVersionUsageByUser> usageData = null;
        List<APIVersionUserUsageDTO> apiUserUsages = new ArrayList<APIVersionUserUsageDTO>();

        Calendar cal = Calendar.getInstance();
        int year = cal.get(cal.YEAR);
        int month = cal.get(cal.MONTH)+1;
        if (!period.equals(""+year+"-"+month)) {
            omElement = this.queryDatabase(
                    APIUsageStatisticsClientConstants.KEY_USAGE_MONTH_SUMMARY, null);
            Collection<APIVersionUsageByUserMonth> usageData = getUsageAPIBySubscriberMonthly(omElement);
            int i = 0;
            for (APIVersionUsageByUserMonth usageEntry : usageData) {


                if (usageEntry.username.equals(subscriberName) && usageEntry.month.equals(period)) {

                    APIVersionUserUsageDTO userUsageDTO = new APIVersionUserUsageDTO();
                    userUsageDTO.setApiname(usageEntry.apiName);
                    userUsageDTO.setContext(usageEntry.context);
                    userUsageDTO.setVersion(usageEntry.apiVersion);
                    userUsageDTO.setCount(usageEntry.requestCount);
                    String cost = evaluate(usageEntry.apiName, (int) usageEntry.requestCount).get("total").toString();
                    String costPerAPI = evaluate(usageEntry.apiName, (int) usageEntry.requestCount).get("cost").toString();
                    userUsageDTO.setCost(cost);
                    userUsageDTO.setCostPerAPI(costPerAPI);
                    apiUserUsages.add(userUsageDTO);
                    i++;

                }


            }

        } else {
            omElement = this.queryDatabase(
                    APIUsageStatisticsClientConstants.KEY_USAGE_SUMMARY, null);
            Collection<APIVersionUsageByUser> usageData = getUsageAPIBySubscriber(omElement);
            int i = 0;
            for (APIVersionUsageByUser usageEntry : usageData) {


                if (usageEntry.username.equals(subscriberName)) {


                    APIVersionUserUsageDTO userUsageDTO = new APIVersionUserUsageDTO();
                    userUsageDTO.setApiname(usageEntry.apiName);
                    userUsageDTO.setContext(usageEntry.context);
                    userUsageDTO.setVersion(usageEntry.apiVersion);
                    userUsageDTO.setCount(usageEntry.requestCount);
                    String cost = evaluate(usageEntry.apiName, (int) usageEntry.requestCount).get("total").toString();
                    String costPerAPI = evaluate(usageEntry.apiName + i, (int) usageEntry.requestCount).get("cost").toString();
                    userUsageDTO.setCost(cost);
                    userUsageDTO.setCostPerAPI(costPerAPI);
                    apiUserUsages.add(userUsageDTO);
                    i++;
                }


            }

        }


        return apiUserUsages;
    }

    private Set<SubscribedAPI> getSubscribedAPIs(String subscriberName) throws APIManagementException {
        return apiConsumerImpl.getSubscribedAPIs(new Subscriber(subscriberName));
    }

    private List<PerUserAPIUsageDTO> getTopEntries(List<PerUserAPIUsageDTO> usageData, int limit) {
        Collections.sort(usageData, new Comparator<PerUserAPIUsageDTO>() {
            public int compare(PerUserAPIUsageDTO o1, PerUserAPIUsageDTO o2) {
                // Note that o2 appears before o1
                // This is because we need to sort in the descending order
                return (int) (o2.getCount() - o1.getCount());
            }
        });
        if (usageData.size() > limit) {
            PerUserAPIUsageDTO other = new PerUserAPIUsageDTO();
            other.setUsername("[Other]");
            for (int i = limit; i < usageData.size(); i++) {
                other.setCount(other.getCount() + usageData.get(i).getCount());
            }
            while (usageData.size() > limit) {
                usageData.remove(limit);
            }
            usageData.add(other);
        }

        return usageData;
    }

    private List<APIUsageDTO> getAPIUsageTopEntries(List<APIUsageDTO> usageData, int limit) {
        Collections.sort(usageData, new Comparator<APIUsageDTO>() {
            public int compare(APIUsageDTO o1, APIUsageDTO o2) {
                // Note that o2 appears before o1
                // This is because we need to sort in the descending order
                return (int) (o2.getCount() - o1.getCount());
            }
        });
        if (usageData.size() > limit) {
            APIUsageDTO other = new APIUsageDTO();
            other.setApiName("[Other]");
            for (int i = limit; i < usageData.size(); i++) {
                other.setCount(other.getCount() + usageData.get(i).getCount());
            }
            while (usageData.size() > limit) {
                usageData.remove(limit);
            }
            usageData.add(other);
        }

        return usageData;
    }

    private List<APIResponseTimeDTO> getResponseTimeTopEntries(List<APIResponseTimeDTO> usageData,
                                                               int limit) {
        Collections.sort(usageData, new Comparator<APIResponseTimeDTO>() {
            public int compare(APIResponseTimeDTO o1, APIResponseTimeDTO o2) {
                // Note that o2 appears before o1
                // This is because we need to sort in the descending order
                return (int) (o2.getServiceTime() - o1.getServiceTime());
            }
        });
        if (usageData.size() > limit) {
            while (usageData.size() > limit) {
                usageData.remove(limit);
            }
        }
        return usageData;
    }

    private List<APIVersionLastAccessTimeDTO> getLastAccessTimeTopEntries(
            List<APIVersionLastAccessTimeDTO> usageData, int limit) {
        Collections.sort(usageData, new Comparator<APIVersionLastAccessTimeDTO>() {
            public int compare(APIVersionLastAccessTimeDTO o1, APIVersionLastAccessTimeDTO o2) {
                // Note that o2 appears before o1
                // This is because we need to sort in the descending order
                return o2.getLastAccessTime().compareToIgnoreCase(o1.getLastAccessTime());
            }
        });
        if (usageData.size() > limit) {
            while (usageData.size() > limit) {
                usageData.remove(limit);
            }
        }

        return usageData;
    }

    private String getNextStringInLexicalOrder(String str) {
        if ((str == null) || (str.equals(""))) {
            return str;
        }
        byte[] bytes = str.getBytes();
        byte last = bytes[bytes.length - 1];
        last = (byte) (last + 1);        // Not very accurate. Need to improve this more to handle overflows.
        bytes[bytes.length - 1] = last;
        return new String(bytes);
    }

    private OMElement queryDatabase(String columnFamily,
                                    QueryServiceStub.CompositeIndex[] compositeIndex)
            throws APIMgtUsageQueryServiceClientException {

        if (dataSource == null) {
            throw new APIMgtUsageQueryServiceClientException("BAM data source hasn't been initialized. Ensure " +
                    "that the data source is properly configured in the APIUsageTracker configuration.");
        }

        String selectRowsByColumnName = null;
        String selectRowsByColumnValue = null;
        if (compositeIndex != null) {
            selectRowsByColumnName = compositeIndex[0].getIndexName();
            selectRowsByColumnValue = compositeIndex[0].getRangeFirst();
        }

        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        try {
            connection = dataSource.getConnection();
            statement = connection.createStatement();
            String query;
            if (selectRowsByColumnName != null) {
                query = "SELECT * FROM  " + columnFamily + " WHERE " + selectRowsByColumnName +
                        "=\'" + selectRowsByColumnValue + "\'";
            } else {
                query = "SELECT * FROM  " + columnFamily;
            }
            rs = statement.executeQuery(query);
            StringBuilder returnStringBuilder = new StringBuilder("<omElement><rows>");
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                returnStringBuilder.append("<row>");
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    String columnValue = rs.getString(columnName);
                    returnStringBuilder.append("<" + columnName.toLowerCase() + ">" + columnValue +
                            "</" + columnName.toLowerCase() + ">");
                }
                returnStringBuilder.append("</row>");
            }
            returnStringBuilder.append("</rows></omElement>");
            String returnString = returnStringBuilder.toString();
            return AXIOMUtil.stringToOM(returnString);

        } catch (Exception e) {
            throw new APIMgtUsageQueryServiceClientException("Error occurred while querying from JDBC database", e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignore) {

                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {

                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {

                }
            }
        }
    }

    private List<API> getAPIsByProvider(String providerId) throws APIMgtUsageQueryServiceClientException {
        try {
            if (APIUsageStatisticsClientConstants.ALL_PROVIDERS.equals(providerId)) {
                return apiProviderImpl.getAllAPIs();
            } else {
                return apiProviderImpl.getAPIsByProvider(providerId);
            }
        } catch (APIManagementException e) {
            throw new APIMgtUsageQueryServiceClientException("Error while retrieving APIs by " + providerId, e);
        }
    }

    private Collection<APIUsage> getUsageData(OMElement data) {
        List<APIUsage> usageData = new ArrayList<APIUsage>();
        OMElement rowsElement = data.getFirstChildWithName(new QName(
                APIUsageStatisticsClientConstants.ROWS));
        Iterator rowIterator = rowsElement.getChildrenWithName(new QName(
                APIUsageStatisticsClientConstants.ROW));
        if (rowIterator != null) {
            while (rowIterator.hasNext()) {
                OMElement rowElement = (OMElement) rowIterator.next();
                usageData.add(new APIUsage(rowElement));
            }
        }
        return usageData;
    }

    private Collection<APIUsageByResourcePath> getUsageDataByResourcePath(OMElement data) {
        List<APIUsageByResourcePath> usageData = new ArrayList<APIUsageByResourcePath>();
        OMElement rowsElement = data.getFirstChildWithName(new QName(
                APIUsageStatisticsClientConstants.ROWS));
        Iterator rowIterator = rowsElement.getChildrenWithName(new QName(
                APIUsageStatisticsClientConstants.ROW));
        if (rowIterator != null) {
            while (rowIterator.hasNext()) {
                OMElement rowElement = (OMElement) rowIterator.next();
                usageData.add(new APIUsageByResourcePath(rowElement));
            }
        }
        return usageData;
    }

    private Collection<APIResponseFaultCount> getAPIResponseFaultCount(OMElement data) {
        List<APIResponseFaultCount> faultyData = new ArrayList<APIResponseFaultCount>();
        OMElement rowsElement = data.getFirstChildWithName(new QName(
                APIUsageStatisticsClientConstants.ROWS));
        Iterator rowIterator = rowsElement.getChildrenWithName(new QName(
                APIUsageStatisticsClientConstants.ROW));
        if (rowIterator != null) {
            while (rowIterator.hasNext()) {
                OMElement rowElement = (OMElement) rowIterator.next();
                faultyData.add(new APIResponseFaultCount(rowElement));
            }
        }
        return faultyData;
    }


    private Collection<APIResponseTime> getResponseTimeData(OMElement data) {
        List<APIResponseTime> responseTimeData = new ArrayList<APIResponseTime>();

        OMElement rowsElement = data.getFirstChildWithName(new QName(
                APIUsageStatisticsClientConstants.ROWS));

        Iterator rowIterator = rowsElement.getChildrenWithName(new QName(
                APIUsageStatisticsClientConstants.ROW));
        if (rowIterator != null) {
            while (rowIterator.hasNext()) {
                OMElement rowElement = (OMElement) rowIterator.next();
                if (rowElement.getFirstChildWithName(new QName(
                        APIUsageStatisticsClientConstants.SERVICE_TIME)) != null) {
                    responseTimeData.add(new APIResponseTime(rowElement));
                }
            }
        }
        return responseTimeData;
    }

    private Collection<APIAccessTime> getAccessTimeData(OMElement data) {
        List<APIAccessTime> accessTimeData = new ArrayList<APIAccessTime>();
        OMElement rowsElement = data.getFirstChildWithName(new QName(
                APIUsageStatisticsClientConstants.ROWS));
        Iterator rowIterator = rowsElement.getChildrenWithName(new QName(
                APIUsageStatisticsClientConstants.ROW));
        if (rowIterator != null) {
            while (rowIterator.hasNext()) {
                OMElement rowElement = (OMElement) rowIterator.next();
                accessTimeData.add(new APIAccessTime(rowElement));
            }
        }
        return accessTimeData;
    }

    private Collection<APIUsageByUser> getUsageBySubscriber(OMElement data) {
        List<APIUsageByUser> usageData = new ArrayList<APIUsageByUser>();
        OMElement rowsElement = data.getFirstChildWithName(new QName(
                APIUsageStatisticsClientConstants.ROWS));
        Iterator rowIterator = rowsElement.getChildrenWithName(new QName(
                APIUsageStatisticsClientConstants.ROW));
        if (rowIterator != null) {
            while (rowIterator.hasNext()) {
                OMElement rowElement = (OMElement) rowIterator.next();
                usageData.add(new APIUsageByUser(rowElement));
            }
        }
        return usageData;
    }

    private Collection<APIVersionUsageByUser> getUsageAPIBySubscriber(OMElement data) {
        List<APIVersionUsageByUser> usageData = new ArrayList<APIVersionUsageByUser>();
        OMElement rowsElement = data.getFirstChildWithName(new QName(
                APIUsageStatisticsClientConstants.ROWS));
        Iterator rowIterator = rowsElement.getChildrenWithName(new QName(
                APIUsageStatisticsClientConstants.ROW));
        if (rowIterator != null) {
            while (rowIterator.hasNext()) {
                OMElement rowElement = (OMElement) rowIterator.next();
                for (int i = 0; i < usageData.size(); i++) {
                    if (usageData.get(i).apiName.equals(rowElement.getFirstChildWithName(new QName(
                            APIUsageStatisticsClientConstants.API)).getText()) && usageData.get(i).apiVersion.equals(rowElement.getFirstChildWithName(new QName(
                            APIUsageStatisticsClientConstants.VERSION)).getText())) {
                        usageData.get(i).requestCount = usageData.get(i).requestCount + (long) Double.parseDouble(rowElement.getFirstChildWithName(new QName(
                                APIUsageStatisticsClientConstants.REQUEST)).getText());
                        return usageData;
                    }

                }
                usageData.add(new APIVersionUsageByUser(rowElement));
            }
        }
        return usageData;
    }

    private Collection<APIVersionUsageByUserMonth> getUsageAPIBySubscriberMonthly(OMElement data) {
        List<APIVersionUsageByUserMonth> usageData = new ArrayList<APIVersionUsageByUserMonth>();
        OMElement rowsElement = data.getFirstChildWithName(new QName(
                APIUsageStatisticsClientConstants.ROWS));
        Iterator rowIterator = rowsElement.getChildrenWithName(new QName(
                APIUsageStatisticsClientConstants.ROW));
        if (rowIterator != null) {
            while (rowIterator.hasNext()) {
                OMElement rowElement = (OMElement) rowIterator.next();
                for (int i = 0; i < usageData.size(); i++) {
                    if (usageData.get(i).apiName.equals(rowElement.getFirstChildWithName(new QName(
                            APIUsageStatisticsClientConstants.API)).getText()) && usageData.get(i).apiVersion.equals(rowElement.getFirstChildWithName(new QName(
                            APIUsageStatisticsClientConstants.VERSION)).getText())) {
                        usageData.get(i).requestCount = usageData.get(i).requestCount + (long) Double.parseDouble(rowElement.getFirstChildWithName(new QName(
                                APIUsageStatisticsClientConstants.REQUEST)).getText());
                        return usageData;
                    }

                }
                usageData.add(new APIVersionUsageByUserMonth(rowElement));
            }
        }
        return usageData;
    }

    private static class APIUsage {

        private String apiName;
        private String apiVersion;
        private String context;
        private long requestCount;

        public APIUsage(OMElement row) {
            apiName = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.API)).getText();
            apiVersion = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.VERSION)).getText();
            context = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.CONTEXT)).getText();
            requestCount = (long) Double.parseDouble(row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.REQUEST)).getText());
        }
    }

    private static class APIUsageByUser {

        private String context;
        private String username;
        private long requestCount;
        private String apiVersion;

        public APIUsageByUser(OMElement row) {
            context = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.CONTEXT)).getText();
            username = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.USER_ID)).getText();
            requestCount = (long) Double.parseDouble(row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.REQUEST)).getText());
            apiVersion = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.VERSION)).getText();
        }
    }

    private static class APIUsageByResourcePath {

        private String apiName;
        private String apiVersion;
        private String context;
        private String resource;
        private long requestCount;

        public APIUsageByResourcePath(OMElement row) {
            apiName = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.API)).getText();
            apiVersion = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.VERSION)).getText();
            context = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.CONTEXT)).getText();
            resource = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.RESOURCE)).getText();
            requestCount = (long) Double.parseDouble(row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.REQUEST)).getText());
        }
    }

    private static class APIResponseFaultCount {

        private String apiName;
        private String apiVersion;
        private String context;
        private String requestTime;
        private long faultCount;

        public APIResponseFaultCount(OMElement row) {
            apiName = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.API)).getText();
            apiVersion = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.VERSION)).getText();
            context = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.CONTEXT)).getText();
            OMElement invocationTimeEle = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.INVOCATION_TIME));
            OMElement faultCountEle = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.FAULT));
            if (invocationTimeEle != null) {
                requestTime = invocationTimeEle.getText();
            }
            if (faultCountEle != null) {
                faultCount = (long) Double.parseDouble(faultCountEle.getText());
            }
        }
    }

    private static class APIVersionUsageByUser {

        private String context;
        private String username;
        private long requestCount;
        private String apiVersion;
        private String apiName;


        public APIVersionUsageByUser(OMElement row) {
            apiName = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.API)).getText();
            context = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.CONTEXT)).getText();
            username = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.USER_ID)).getText();
            requestCount = (long) Double.parseDouble(row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.REQUEST)).getText());
            apiVersion = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.VERSION)).getText();

        }
    }

    private static class APIVersionUsageByUserMonth {

        private String context;
        private String username;
        private long requestCount;
        private String apiVersion;
        private String apiName;
        private String month;

        public APIVersionUsageByUserMonth(OMElement row) {
            apiName = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.API)).getText();
            context = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.CONTEXT)).getText();
            username = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.USER_ID)).getText();
            requestCount = (long) Double.parseDouble(row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.REQUEST)).getText());
            apiVersion = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.VERSION)).getText();
            month = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.MONTH)).getText();
        }
    }

    private static class APIResponseTime {

        private String apiName;
        private String apiVersion;
        private String context;
        private double responseTime;
        private long responseCount;

        public APIResponseTime(OMElement row) {
            String nameVersion = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.API_VERSION)).getText();
            int index = nameVersion.lastIndexOf(":v");
            apiName = nameVersion.substring(0, index);
            apiVersion = nameVersion.substring(index + 2);
            context = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.CONTEXT)).getText();
            responseTime = Double.parseDouble(row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.SERVICE_TIME)).getText());
            responseCount = (long) Double.parseDouble(row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.RESPONSE)).getText());
        }
    }

    private static class APIAccessTime {

        private String apiName;
        private String apiVersion;
        private String context;
        private double accessTime;
        private String username;

        public APIAccessTime(OMElement row) {
            String nameVersion = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.API_VERSION)).getText();
            int index = nameVersion.lastIndexOf(":v");
            apiName = nameVersion.substring(0, index);
            apiVersion = nameVersion.substring(index + 2);
            context = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.CONTEXT)).getText();
            accessTime = Double.parseDouble(row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.REQUEST_TIME)).getText());
            username = row.getFirstChildWithName(new QName(
                    APIUsageStatisticsClientConstants.USER_ID)).getText();
        }
    }

    public static OMElement buildOMElement(InputStream inputStream) throws Exception {
        XMLStreamReader parser;
        try {
            parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
        } catch (XMLStreamException e) {
            String msg = "Error in initializing the parser to build the OMElement.";
            throw new Exception(msg, e);
        } finally {
        }
        StAXOMBuilder builder = new StAXOMBuilder(parser);
        return builder.getDocumentElement();
    }

    public Map<String, Object> evaluate(String param, int calls) throws Exception {
        return paymentPlan.evaluate(param, calls);
    }

}