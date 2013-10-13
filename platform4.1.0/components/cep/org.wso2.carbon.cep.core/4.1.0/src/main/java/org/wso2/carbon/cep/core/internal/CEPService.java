/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.cep.core.internal;

import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.Bucket;
import org.wso2.carbon.cep.core.BucketBasicInfo;
import org.wso2.carbon.cep.core.CEPServiceInterface;
import org.wso2.carbon.cep.core.backend.CEPBackEndRuntime;
import org.wso2.carbon.cep.core.backend.CEPBackEndRuntimeFactory;
import org.wso2.carbon.cep.core.backend.CEPEngineProvider;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.internal.ds.CEPServiceValueHolder;
import org.wso2.carbon.cep.core.internal.persistance.CEPResourcePersister;
import org.wso2.carbon.cep.core.internal.util.CEPConstants;
import org.wso2.carbon.cep.core.internal.util.NotDeployedBucket;
import org.wso2.carbon.cep.core.mapping.input.Input;
import org.wso2.carbon.cep.core.mapping.input.mapping.InputMapping;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CEPService implements CEPServiceInterface {

    private static final Log log = LogFactory.getLog(CEPService.class);

    private Map<Integer, Map<String, CEPBucket>> tenantSpecificCEPBuckets;

    private Map<String, CEPEngineProvider> cepEngineProviderMap;

    /**
     * if the corresponding cep engine provider is not available when the
     * buckets are added then we keep such buckets to deploy when the corresponding
     * cep back end runtime get registered with the cep service.
     */
    private List<NotDeployedBucket> notDeployedBuckets;

    private AxisConfiguration axisConfiguration;

    public CEPService() throws CEPConfigurationException {
        this.cepEngineProviderMap = new ConcurrentHashMap<String, CEPEngineProvider>();
        this.notDeployedBuckets = new ArrayList<NotDeployedBucket>();
        this.tenantSpecificCEPBuckets = new ConcurrentHashMap<Integer, Map<String, CEPBucket>>();
    }

    public void registerCEPEngineProvider(CEPEngineProvider cepEngineProvider) {
        this.cepEngineProviderMap.put(cepEngineProvider.getName(), cepEngineProvider);
        //deploy the undeployed buckets if they use this cep engine provider
        for (NotDeployedBucket notDeployedBucket : notDeployedBuckets) {
            if (notDeployedBucket.getBucket().getEngineProvider().equals(cepEngineProvider.getName())) {
                try {
                    deployBucket(notDeployedBucket.getBucket(), cepEngineProvider, axisConfiguration, notDeployedBucket.getPath());
                } catch (CEPConfigurationException e) {
                    log.error("Can not add bucket " + notDeployedBucket.getBucket().getName() + " to " + cepEngineProvider.getName() + " back end runtime ", e);
                }
            }
        }

    }

    public void addBucket(Bucket bucket, AxisConfiguration axisConfiguration)
            throws CEPConfigurationException {
        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        Map<String, CEPBucket> buckets = this.tenantSpecificCEPBuckets.get(tenantId);
        if (buckets != null && buckets.containsKey(bucket.getName())) {
            throw new CEPConfigurationException("A bucket with name " + bucket.getName() + " already exist!");
        }
        String bucketPath = createCEPBucketDirectories(bucket, axisConfiguration);
        try {
            CEPResourcePersister.save(bucket, bucketPath);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

    private String createCEPBucketDirectories(Bucket bucket, AxisConfiguration axisConfiguration)
            throws CEPConfigurationException {
        File directory = new File(axisConfiguration.getRepository().getPath());
        if (!directory.exists()) {
            if (directory.mkdir()) {
                throw new CEPConfigurationException("Cannot create directory to add tenant specific bucket :" + bucket.getName());
            }
        }
        directory = new File(directory.getAbsolutePath() + File.separator + CEPConstants.CEP_BUCKETS_REPO);
        if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new CEPConfigurationException("Cannot create directory " + CEPConstants.CEP_BUCKETS_REPO + " to add tenant specific bucket :" + bucket.getName());
            }
        }
        return directory.getAbsolutePath() + File.separator + bucket.getName() + ".xml";
    }

    public void editBucket(Bucket newBucket) throws CEPConfigurationException {
        try {
            int tenantId = PrivilegedCarbonContext.getCurrentContext(axisConfiguration).getTenantId();
            Map<String, CEPBucket> existingBuckets = this.tenantSpecificCEPBuckets.get(tenantId);
            CEPBucket existingBucket = existingBuckets.remove(newBucket.getName());
            if (existingBucket != null) {
                existingBucket.delete();
                CEPResourcePersister.save(newBucket, existingBucket.getBucketPath());
            } else {
                log.error("No bucket with name " + newBucket.getName() + " is found to Edit");
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }

    }

    public boolean removeBucket(String bucketName) throws CEPConfigurationException {
        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        Map<String, CEPBucket> buckets = this.tenantSpecificCEPBuckets.get(tenantId);
        CEPBucket bucket = buckets.remove(bucketName);
        if (bucket != null) {
            bucket.delete();
            CEPResourcePersister.delete(bucket.getBucketPath());
            return true;
        } else {
            return false;
        }
    }


    public Bucket getBucket(String bucketName) throws CEPConfigurationException {
        int tenantId = CarbonContext.getCurrentContext().getTenantId();

        Bucket bucket = this.tenantSpecificCEPBuckets.get(tenantId).get(bucketName).getBucket();
        if (bucket == null) {
            throw new CEPConfigurationException("Bucket " + bucketName + " does not exist");
        }
        return bucket;
    }

    public boolean deployBucket(Bucket bucket,
                                AxisConfiguration axisConfiguration, String bucketPath)
            throws CEPConfigurationException {
        CEPEngineProvider cepEngineProvider;
        this.axisConfiguration = axisConfiguration;
        if (bucket.getEngineProvider() == null) {
            String errorMessage = "No cep engine provider is given";
            log.error(errorMessage);
            throw new CEPConfigurationException(errorMessage);
        } else {
            cepEngineProvider = this.cepEngineProviderMap.get(bucket.getEngineProvider());
            if (cepEngineProvider == null) {
                // we save this bucket to unDeployed Buckets list and simply return
                this.notDeployedBuckets.add(new NotDeployedBucket(bucket, bucketPath));
                log.info("Bucket " + bucket.getName() + ", is scheduled for deployment");
                return false;
            }
        }
        return deployBucket(bucket, cepEngineProvider, axisConfiguration, bucketPath);
    }

    private boolean deployBucket(Bucket bucket, CEPEngineProvider cepEngineProvider,
                                 AxisConfiguration axisConfiguration, String bucketPath)
            throws CEPConfigurationException {

        try {
            int tenantId = CarbonContext.getCurrentContext().getTenantId();

            Map<String, CEPBucket> buckets = this.tenantSpecificCEPBuckets.get(tenantId);
            if (buckets == null) {
                buckets = new ConcurrentHashMap<String, CEPBucket>();
                this.tenantSpecificCEPBuckets.put(tenantId, buckets);
            } else if (buckets.containsKey(bucket.getName())) {
                log.info("Bucket " + bucket.getName() + " is not deployed hence bucket already exists.");
                return false;
//                throw new CEPConfigurationException("Error, bucket cannot be deployed, a bucket with name " + bucket.getName() + " already exist!");
            }

            String owner = CarbonContext.getCurrentContext().getUsername();
            if (owner != null && owner.length() > 0) {
                bucket.setOwner(owner);
            }

            CEPBackEndRuntimeFactory cepBackEndRuntimeFactory =
                    (CEPBackEndRuntimeFactory) cepEngineProvider.getProviderClass().newInstance();

            List<InputMapping> inputMappings = new ArrayList<InputMapping>();
            if (bucket.getInputs() != null) {
                for (Input input : bucket.getInputs()) {
                    inputMappings.add(input.getInputMapping());
                }
            }

            CEPBackEndRuntime cepBackEndRuntime =
                    cepBackEndRuntimeFactory.createCEPBackEndRuntime(bucket.getName(), bucket.getProviderConfigurationProperties(), inputMappings, tenantId);

            CEPBucket cepBucket = new CEPBucket(cepBackEndRuntime, bucket, axisConfiguration, bucketPath);
            cepBucket.init();


            buckets.put(bucket.getName(), cepBucket);
            log.info("Added bucket " + bucket.getName() + " to the cep engine successfully");

        } catch (InstantiationException e) {
            String errorMessage = "Can not instantiate factory class ";
            log.error(errorMessage, e);
            throw new CEPConfigurationException(errorMessage, e);
        } catch (IllegalAccessException e) {
            String errorMessage = "Error in adding buckets";
            log.error(errorMessage, e);
            throw new CEPConfigurationException(errorMessage, e);
        }
        return true;
    }

    public boolean unDeployBucket(String bucketName, int tenantId)
            throws CEPConfigurationException {

        Map<String, CEPBucket> buckets = this.tenantSpecificCEPBuckets.get(tenantId);
        CEPBucket cepBucket = buckets.remove(bucketName);
        if (cepBucket != null) {
            cepBucket.delete();
            return true;
        } else {
            return false;
        }

    }

    public boolean removeAllBuckets() throws CEPConfigurationException {
        try {
            int tenantId = CarbonContext.getCurrentContext().getTenantId();
            Map<String, CEPBucket> tenantBuckets = this.tenantSpecificCEPBuckets.get(tenantId);
            for (CEPBucket cepBucket : tenantBuckets.values()) {
                removeBucket(cepBucket.getBucket().getName());
            }
            return true;
        } catch (Exception e) {
            throw new CEPConfigurationException("Error in removing all the buckets " + e);
        }
    }

    public List<BucketBasicInfo> getBucketList() throws CEPConfigurationException {
        List<CEPBucket> cepBucketList;
        List<BucketBasicInfo> bucketBasicInfoList = new ArrayList<BucketBasicInfo>();
        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        if (this.tenantSpecificCEPBuckets.get(tenantId) != null) {
            cepBucketList = new ArrayList<CEPBucket>(this.tenantSpecificCEPBuckets.get(
                    tenantId).values());
            for (CEPBucket cepBucket : cepBucketList) {
                BucketBasicInfo bucketBasicInfo = new BucketBasicInfo();
                bucketBasicInfo.setName(cepBucket.getBucket().getName());
                bucketBasicInfo.setDescription(cepBucket.getBucket().getDescription());
                bucketBasicInfoList.add(bucketBasicInfo);
            }
        }
        return bucketBasicInfoList;
    }

    public String[] getCEPEngineProviders() throws CEPConfigurationException {
        Set engineProviders = cepEngineProviderMap.keySet();
        String[] providers = new String[engineProviders.size()];
        engineProviders.toArray(providers);
        return providers;
    }

    @Override
    public String[] getCEPEngineProviderConfigNames(String providerName)
            throws CEPConfigurationException {
        if (cepEngineProviderMap.get(providerName).getConfigurationPropertyNames() != null) {
            return cepEngineProviderMap.get(providerName).getConfigurationPropertyNames().toArray(new String[cepEngineProviderMap.get(providerName).getConfigurationPropertyNames().size()]);
        } else {
            return new String[0];
        }
    }

    public String[] getCEPBrokerNames() throws CEPConfigurationException {
        int tenantId = CarbonContext.getCurrentContext().getTenantId();
        List<String> brokerConfigurationNames = CEPServiceValueHolder.getInstance().getBrokerManagerService().
                getAllBrokerConfigurationNames(tenantId);
        String[] brokerNames = new String[brokerConfigurationNames.size()];
        brokerConfigurationNames.toArray(brokerNames);
        return brokerNames;
    }

    public void shutdown() {
        for (Map<String, CEPBucket> map : tenantSpecificCEPBuckets.values()) {
            for (CEPBucket cepBucket : map.values()) {
                cepBucket.shutdown();
            }
        }
    }
}
