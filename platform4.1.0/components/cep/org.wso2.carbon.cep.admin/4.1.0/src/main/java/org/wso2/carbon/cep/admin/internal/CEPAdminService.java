package org.wso2.carbon.cep.admin.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.admin.internal.exception.CEPAdminException;
import org.wso2.carbon.cep.admin.internal.util.CEPAdminDSHolder;
import org.wso2.carbon.cep.core.Bucket;
import org.wso2.carbon.cep.core.BucketBasicInfo;
import org.wso2.carbon.cep.core.CEPServiceInterface;
import org.wso2.carbon.cep.core.Query;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.mapping.input.Input;
import org.wso2.carbon.core.AbstractAdmin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class is published as a web service. so that front end can invoke the
 * methods to invoke the cep engine.
 */
public class CEPAdminService extends AbstractAdmin {
    private static final Log log = LogFactory.getLog(CEPAdminService.class);

    /**
     * This method will add bucketDTO to the CEP Engine
     *
     * @param bucketDTO - Admin module bucketDTO
     */
    public boolean addBucket(BucketDTO bucketDTO) throws CEPAdminException {

        List<Input> backendInputList = new ArrayList<Input>();
        Bucket backEndBucket = new Bucket();

        QueryDTO[] queryDTOs = bucketDTO.getQueries();
        InputDTO[] inputDTOs = bucketDTO.getInputs();
        if (inputDTOs != null) {
            for (InputDTO inputDTO : inputDTOs) {
                backendInputList.add(CEPAdminUtils.adaptInput(inputDTO));
            }
        }

        List<Query> backEndQueryList = new ArrayList<Query>();
        if (queryDTOs != null) {
            int queryIndex = 0;
            for (QueryDTO queryDTO : queryDTOs) {
                Query query = CEPAdminUtils.adaptQuery(queryDTO);
                query.setQueryIndex(queryIndex);
                backEndQueryList.add(query);
                queryIndex++;
            }
        }

        Properties providerConfig = new Properties();
        if (bucketDTO.getEngineProviderConfigProperty() != null) {
            CEPEngineProviderConfigPropertyDTO[] engineProviderConfigProperty = bucketDTO.getEngineProviderConfigProperty();
            for (CEPEngineProviderConfigPropertyDTO providerConfigPropertyDTO : engineProviderConfigProperty) {
                providerConfig.setProperty(providerConfigPropertyDTO.getNames(), providerConfigPropertyDTO.getValues());
            }
        }

        backEndBucket.setName(bucketDTO.getName());
        backEndBucket.setDescription(bucketDTO.getDescription());
        backEndBucket.setEngineProvider(bucketDTO.getEngineProvider());
        backEndBucket.setProviderConfigurationProperties(providerConfig);
        backEndBucket.setInputs(backendInputList);
        backEndBucket.setQueries(backEndQueryList);

        CEPServiceInterface cepServiceInterface = CEPAdminDSHolder.getInstance().getCEPService();
        try {
            cepServiceInterface.addBucket(backEndBucket, getAxisConfig());
            return true;
        } catch (CEPConfigurationException e) {
            String errorMessage = "Error in Adding bucket to back end, "+e.getMessage();
            log.error(errorMessage, e);
            throw new CEPAdminException(errorMessage, e);
        }
    }

    public boolean editBucket(BucketDTO bucketDTO) throws CEPAdminException {

        try {
            List<Input> backendInputList = new ArrayList<Input>();
            Bucket backEndBucket = new Bucket();


            QueryDTO[] queryDTOs = bucketDTO.getQueries();
            InputDTO[] inputDTOs = bucketDTO.getInputs();
            if (inputDTOs != null) {
                for (InputDTO inputDTO : inputDTOs) {
                    backendInputList.add(CEPAdminUtils.adaptInput(inputDTO));
                }
            }

            List<Query> backEndQueryList = new ArrayList<Query>();
            if (queryDTOs != null) {
                int queryIndex = 0;
                for (QueryDTO queryDTO : queryDTOs) {
                    Query query = CEPAdminUtils.adaptQuery(queryDTO);
                    query.setQueryIndex(queryIndex);
                    backEndQueryList.add(query);
                    queryIndex++;
                }
            }

            Properties providerConfig = new Properties();
            if (bucketDTO.getEngineProviderConfigProperty() != null) {
                CEPEngineProviderConfigPropertyDTO[] engineProviderConfigProperty = bucketDTO.getEngineProviderConfigProperty();
                for (CEPEngineProviderConfigPropertyDTO providerConfigPropertyDTO : engineProviderConfigProperty) {
                    providerConfig.setProperty(providerConfigPropertyDTO.getNames(), providerConfigPropertyDTO.getValues());
                }
            }


            if (!(inputDTOs == null && queryDTOs == null)) {
                backEndBucket.setName(bucketDTO.getName());
                backEndBucket.setDescription(bucketDTO.getDescription());
                backEndBucket.setEngineProvider(bucketDTO.getEngineProvider());
                backEndBucket.setProviderConfigurationProperties(providerConfig);
                backEndBucket.setInputs(backendInputList);
                backEndBucket.setQueries(backEndQueryList);

                CEPServiceInterface cepServiceInterface = CEPAdminDSHolder.getInstance().getCEPService();

                cepServiceInterface.editBucket(backEndBucket);
            }
            return true;
        } catch (CEPConfigurationException e) {
            String errorMessage = "Error in editing the bucket";
            if (e.getCause().getMessage().contains("Error during creating rule")) {
                errorMessage = "Error in processing the query ";
            }
            log.error(errorMessage, e);
            throw new CEPAdminException(errorMessage, e);
        }

    }


    /**
     * This method will return all the bucket names from backEnd
     */
    public BucketBasicInfoDTO[] getAllBucketNames(int startingIndex, int maxBucketCount, String searchString)
            throws CEPConfigurationException {
        CEPServiceInterface cepServiceInterface = CEPAdminDSHolder.getInstance().getCEPService();
        BucketBasicInfoDTO[] bucketBasicInfoDTOArray = null;
        List<BucketBasicInfoDTO> bucketBasicInfoDTOList = null;
        if(searchString==null||searchString.trim().length() ==0){
            searchString=null;
        }

        List<BucketBasicInfo> backEndBucketBasicInfoList =
                cepServiceInterface.getBucketList();
        Collections.sort(backEndBucketBasicInfoList);
        bucketBasicInfoDTOList = new ArrayList<BucketBasicInfoDTO>();

        int index = 0;
        int basicInfoIndex = 0;
        for (BucketBasicInfo basicInfo : backEndBucketBasicInfoList) {
            if ((startingIndex == index || startingIndex < index) && isServiceSatisfySearchString(searchString, basicInfo.getName())) {
                BucketBasicInfoDTO bucketBasicInfoDTO = new BucketBasicInfoDTO();
                bucketBasicInfoDTO.setName(basicInfo.getName());
                bucketBasicInfoDTO.setDescription(basicInfo.getDescription());
                bucketBasicInfoDTOList.add(bucketBasicInfoDTO);
                basicInfoIndex++;
                if (basicInfoIndex == maxBucketCount) {
                    break;
                }
            }
            index++;
        }
        bucketBasicInfoDTOArray = bucketBasicInfoDTOList.toArray(new BucketBasicInfoDTO[bucketBasicInfoDTOList.size()]);

        return bucketBasicInfoDTOArray;
    }

    private boolean isServiceSatisfySearchString(String searchString,
                                                 String bucketName) {
        if (searchString != null) {
            String regex = searchString.toLowerCase().
                    replace("..?", ".?").replace("..*", ".*").
                    replaceAll("\\?", ".?").replaceAll("\\*", ".*?");

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(bucketName.toLowerCase());

            return regex.trim().length() == 0 || matcher.find();
        }
        return true;
    }

    /**
     * this method will return the full bucket count
     */
    public int getAllBucketCount() throws CEPConfigurationException {
        CEPServiceInterface cepServiceInterface = CEPAdminDSHolder.getInstance().getCEPService();
        return cepServiceInterface.getBucketList().size();
    }

    /**
     * This method will return the bucket with the given name from backEnd
     *
     * @param bucketName - Name of the bucket
     */

    public BucketDTO getBucket(String bucketName) throws CEPAdminException {
        CEPServiceInterface cepServiceInterface = CEPAdminDSHolder.getInstance().getCEPService();
        BucketDTO bucketDTO = new BucketDTO();
        Bucket backEndBucket = null;
        try {
            backEndBucket = cepServiceInterface.getBucket(bucketName);
        } catch (CEPConfigurationException e) {
            String errorMessage = "Error in getting the bucketDTO ";
            log.error(errorMessage, e);
            throw new CEPAdminException(errorMessage, e);
        }
        bucketDTO.setName(backEndBucket.getName());
        bucketDTO.setDescription(backEndBucket.getDescription());
        bucketDTO.setEngineProvider(backEndBucket.getEngineProvider());
        bucketDTO.setEngineProviderConfigProperty(CEPAdminUtils.adaptEngineProviderConfigs(backEndBucket.getProviderConfigurationProperties()));
        bucketDTO.setInputs(CEPAdminUtils.adaptInput(backEndBucket.getInputs()));
        bucketDTO.setQueries(CEPAdminUtils.adaptQueries(backEndBucket.getQueries()));

        return bucketDTO;
    }

    /**
     * This method will remove the bucket with the provided bucket name from the back end
     *
     * @param bucketName - name of the bucket to be removed
     * @return status - status of the operation
     */

    public boolean removeBucket(String bucketName) throws CEPAdminException {
        CEPServiceInterface cepServiceInterface = CEPAdminDSHolder.getInstance().getCEPService();
        try {
            return cepServiceInterface.removeBucket(bucketName);
        } catch (CEPConfigurationException e) {
            String errorMessage = "Error in removing the bucket from back end";
            log.error(errorMessage, e);
            throw new CEPAdminException(errorMessage, e);
        }
    }

    public boolean removeAllBuckets() throws CEPAdminException {
        CEPServiceInterface cepServiceInterface = CEPAdminDSHolder.getInstance().getCEPService();
        try {
            return cepServiceInterface.removeAllBuckets();
        } catch (CEPConfigurationException e) {
            String errorMessage = "Error in removing all buckets from back end ";
            log.error(errorMessage, e);
            throw new CEPAdminException(errorMessage, e);
        }
    }

    public CEPEngineProviderInfoDTO[] getEngineProvidersInfo() throws CEPAdminException {
        CEPServiceInterface cepServiceInterface = CEPAdminDSHolder.getInstance().getCEPService();
        try {
            CEPEngineProviderInfoDTO[] info = new CEPEngineProviderInfoDTO[cepServiceInterface.getCEPEngineProviders().length];
            String[] cepEngineProviders = cepServiceInterface.getCEPEngineProviders();
            for (int i = 0, cepEngineProvidersLength = cepEngineProviders.length; i < cepEngineProvidersLength; i++) {
                String providerName = cepEngineProviders[i];
                info[i] = new CEPEngineProviderInfoDTO();
                info[i].setProviderName(providerName);
                info[i].setConfigNames(cepServiceInterface.getCEPEngineProviderConfigNames(providerName));
            }
            return info;
        } catch (CEPConfigurationException e) {
            String errorMessage = "Error in getting engine providers from back end ";
            log.error(errorMessage, e);
            throw new CEPAdminException(errorMessage, e);
        }
    }

    public String[] getBrokerNames() throws CEPAdminException {
        CEPServiceInterface cepServiceInterface = CEPAdminDSHolder.getInstance().getCEPService();
        try {
            return cepServiceInterface.getCEPBrokerNames();
        } catch (CEPConfigurationException e) {
            throw new CEPAdminException("Error in getting engine providers from back end : " + e);
        }
    }


}

