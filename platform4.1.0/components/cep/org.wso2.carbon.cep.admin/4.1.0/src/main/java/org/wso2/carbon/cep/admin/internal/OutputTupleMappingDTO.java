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
package org.wso2.carbon.cep.admin.internal;

public class OutputTupleMappingDTO {

    private OutputTuplePropertyDTO[] payloadDataProperties;
    private OutputTuplePropertyDTO[] correlationDataProperties;
    private OutputTuplePropertyDTO[] metaDataProperties;

    public OutputTuplePropertyDTO[] getPayloadDataProperties() {
        return payloadDataProperties;
    }

    public void setPayloadDataProperties(OutputTuplePropertyDTO[] payloadDataProperties) {
        this.payloadDataProperties = payloadDataProperties;
    }

    public OutputTuplePropertyDTO[] getCorrelationDataProperties() {
        return correlationDataProperties;
    }

    public void setCorrelationDataProperties(OutputTuplePropertyDTO[] correlationDataProperties) {
        this.correlationDataProperties = correlationDataProperties;
    }

    public OutputTuplePropertyDTO[] getMetaDataProperties() {
        return metaDataProperties;
    }

    public void setMetaDataProperties(OutputTuplePropertyDTO[] metaDataProperties) {
        this.metaDataProperties = metaDataProperties;
    }
}
