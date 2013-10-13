/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.cep.statistics.internal.data;

public class CollectionDTO {
    private boolean bucket;
    private CountDTO count;
    private String[] topicNames;
    private CountDTO[] topicCounts;

    public boolean isBucket() {
        return bucket;
    }

    public void setBucket(boolean bucket) {
        this.bucket = bucket;
    }

    public CountDTO getCount() {
        return count;
    }

    public void setCount(CountDTO count) {
        this.count = count;
    }

    public String[] getTopicNames() {
        return topicNames;
    }

    public void setTopicNames(String[] topicNames) {
        this.topicNames = topicNames;
    }

    public CountDTO[] getTopicCounts() {
        return topicCounts;
    }

    public void setTopicCounts(CountDTO[] topicCounts) {
        this.topicCounts = topicCounts;
    }
}
