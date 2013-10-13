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
package org.wso2.siddhi.query.api.expression;

import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.api.exception.MalformedAttributeException;
import org.wso2.siddhi.query.api.query.QueryEventStream;

import java.util.List;

public class Variable extends Expression {

    private String streamId;
    private int position = -1;
    private String attributeName;

    public Variable(String streamId, String attributeName) {
        this.streamId = streamId;
        this.attributeName = attributeName;
    }

    public Variable(String attributeName) {
        this.attributeName = attributeName;
    }

    public Variable(String streamId, int position, String attributeName) {
        this.streamId = streamId;
        this.position = position;
        this.attributeName = attributeName;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public int getPosition() {
        return position;
    }

    @Override
    protected void validate(List<QueryEventStream> queryEventStreamList, String streamReferenceId,
                            boolean processInStreamDefinition) {
        if (streamId == null) {
            streamId = streamReferenceId;
        }
        if (streamId == null) {
            int occurrences = 0;
            String inputStreamName = "";
            for (QueryEventStream queryEvent : queryEventStreamList) {
                StreamDefinition streamDefinition;
                if (processInStreamDefinition) {
                    streamDefinition = queryEvent.getInStreamDefinition();
                } else {
                    streamDefinition = queryEvent.getOutStreamDefinition();
                }
                for (Attribute attribute : streamDefinition.getAttributeList()) {
                    if (this.getAttributeName().equals(attribute.getName())) {
                        occurrences++;
                        inputStreamName = queryEvent.getStreamId();
                        break;
                    }
                }
            }
            if (occurrences == 1) {
                streamId = inputStreamName;
            } else if (occurrences > 1) {
                throw new MalformedAttributeException("Attribute \"" + getAttributeName() + "\" exists in more than one inputstream.Specify the input stream");
            } else if (occurrences == 0) {
                throw new MalformedAttributeException("Attribute \"" + getAttributeName() + "\" does not exist in " + streamId);
            }

        }

        boolean isValid = false;
        for (QueryEventStream queryEvent : queryEventStreamList) {
            if (queryEvent.getStreamId().equals(streamId) || queryEvent.getReferenceStreamId().equals(streamId)) {
                StreamDefinition streamDefinition;
                if (processInStreamDefinition) {
                    streamDefinition = queryEvent.getInStreamDefinition();
                } else {
                    streamDefinition = queryEvent.getOutStreamDefinition();
                }
                for (Attribute attribute : streamDefinition.getAttributeList()) {
                    if (this.getAttributeName().equals(attribute.getName())) {
                        isValid = true;
                        break;
                    }
                }
                break;
            }
        }
        if (!isValid) {
            throw new MalformedAttributeException("Attribute \"" + getAttributeName() + "\" does not exist in " + streamId);
        }
    }
}
