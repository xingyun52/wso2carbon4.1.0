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

package org.wso2.carbon.cep.core;

import org.apache.axis2.engine.AxisConfiguration;
import org.wso2.carbon.cep.core.backend.CEPEngineProvider;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;

import java.util.List;

public interface CEPServiceInterface {
    /**
     * This method is used to add a new bucket to the back end
     * @param bucket - bucket to be added
     * */
    public void addBucket(Bucket bucket, AxisConfiguration axisConfiguration) throws CEPConfigurationException;

    /**
     * This method is used to get the bucket which having the provided name
     * from back end
     * @param bucketName - Name of the bucket
     * @return Bucket
     * */
    public Bucket getBucket(String  bucketName) throws CEPConfigurationException;

    /**
     * This method is used to remove a bucket from the back end
     * @param bucketName - Name of the bucket to be removed
     * @return - status of the removing operation : true - Successful ;false - UnSuccessful
     *  */
    public boolean removeBucket(String bucketName) throws CEPConfigurationException;
    /**
     * This method is used to get the list of "BucketsInfo"objects which contains
     * basic information about available buckets in the back end
     * @return List of available buckets
     * */
    public List<BucketBasicInfo> getBucketList() throws CEPConfigurationException;

    /**
     * This method is used to edit the existing bucket in the back end
     * @param bucket - Bucket to be edited which consists new data
     * */
    public void editBucket(Bucket bucket) throws CEPConfigurationException;

    /**
     * This method is used to remove all the available buckets from the back end
     * @return status of the operation performed
     * */
    public boolean removeAllBuckets() throws CEPConfigurationException;

    /**
     * This method is used to get names of the all Engine providers
     * @return Array of Names of available engine providers
     * */
    public String[]  getCEPEngineProviders() throws CEPConfigurationException;

    /**
     * This method is used to get names of the all Engine providers
     * @return Array of Names of available engine providers
     * */
    String[] getCEPEngineProviderConfigNames(String providerName) throws CEPConfigurationException;

    /**
     * This method is used to get names if the all brokers registered in the back end
     * @return Array of Names of the brokers
     * */
    public String[] getCEPBrokerNames() throws CEPConfigurationException;


    /**
     * CEP Back end runtime engines registers with the cep core using this method.
     * They will provide the class name to pick
     * @param cepEngineProvider
     * @throws CEPConfigurationException
     */
    public void registerCEPEngineProvider(CEPEngineProvider cepEngineProvider);


}
