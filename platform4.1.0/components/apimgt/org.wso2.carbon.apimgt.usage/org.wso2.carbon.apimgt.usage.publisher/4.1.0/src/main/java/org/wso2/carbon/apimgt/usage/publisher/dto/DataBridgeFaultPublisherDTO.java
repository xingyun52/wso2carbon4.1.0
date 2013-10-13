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

package org.wso2.carbon.apimgt.usage.publisher.dto;

import org.wso2.carbon.apimgt.usage.publisher.APIMgtUsagePublisherConstants;
import org.wso2.carbon.databridge.agent.thrift.DataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.exception.DifferentStreamDefinitionAlreadyDefinedException;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.exception.NoStreamDefinitionExistException;
import org.wso2.carbon.databridge.commons.exception.StreamDefinitionException;

public class DataBridgeFaultPublisherDTO extends FaultPublisherDTO{

    public DataBridgeFaultPublisherDTO(FaultPublisherDTO faultPublisherDTO){
        setConsumerKey(faultPublisherDTO.getConsumerKey());
        setContext(faultPublisherDTO.getContext());
        setApi_version(faultPublisherDTO.getApi_version());
        setApi(faultPublisherDTO.getApi());
        setResource(faultPublisherDTO.getResource());
        setMethod(faultPublisherDTO.getMethod());
        setVersion(faultPublisherDTO.getVersion());
        setErrorCode(faultPublisherDTO.getErrorCode());
        setErrorMessage(faultPublisherDTO.getErrorMessage());
        setRequestTime((faultPublisherDTO.getRequestTime()));
        setUsername(faultPublisherDTO.getUsername());
    }

    public static String addStreamId(DataPublisher dataPublisher) throws AgentException,
            MalformedStreamDefinitionException,
            StreamDefinitionException,
            DifferentStreamDefinitionAlreadyDefinedException,
            NoStreamDefinitionExistException {

        try {
            dataPublisher.findStream(APIMgtUsagePublisherConstants.API_MANAGER_FAULT_STREAM_NAME,
                    APIMgtUsagePublisherConstants.API_MANAGER_FAULT_STREAM_VERSION);

        } catch (NoStreamDefinitionExistException e) {
            dataPublisher.defineStream("{" +
                    "  'name':'" + APIMgtUsagePublisherConstants.API_MANAGER_FAULT_STREAM_NAME + "'," +
                    "  'version':'" + APIMgtUsagePublisherConstants.API_MANAGER_FAULT_STREAM_VERSION + "'," +
                    "  'nickName': 'API Manager Fault Data'," +
                    "  'description': 'Fault Data'," +
                    "  'metaData':[" +
                    "          {'name':'clientType','type':'STRING'}" +
                    "  ]," +
                    "  'payloadData':[" +
                    "          {'name':'consumerKey','type':'STRING'}," +
                    "          {'name':'context','type':'STRING'}," +
                    "          {'name':'api_version','type':'STRING'}," +
                    "          {'name':'api','type':'STRING'}," +
                    "          {'name':'resource','type':'STRING'}," +
                    "          {'name':'method','type':'STRING'}," +
                    "          {'name':'version','type':'STRING'}," +
                    "          {'name':'errorCode','type':'STRING'}," +
                    "          {'name':'errorMessage','type':'STRING'}," +
                    "          {'name':'requestTime','type':'STRING'}," +
                    "          {'name':'userId','type':'STRING'}" +
                    "  ]" +

                    "}");

        }
        return dataPublisher.findStream(APIMgtUsagePublisherConstants.API_MANAGER_FAULT_STREAM_NAME,
                APIMgtUsagePublisherConstants.API_MANAGER_FAULT_STREAM_VERSION);
    }

    public Object createPayload(){
        return new Object[]{getConsumerKey(),getContext(),getApi_version(),getApi(),getResource(),getMethod(),
                getVersion(),getErrorCode(),getErrorMessage(), String.valueOf(getRequestTime()),getUsername()};
    }
}
