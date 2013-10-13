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
package org.wso2.siddhi.query.api.query.input;

import org.wso2.siddhi.query.api.condition.Condition;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.api.exception.StreamNotExistException;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.query.QueryEventStream;
import org.wso2.siddhi.query.api.query.input.handler.Filter;
import org.wso2.siddhi.query.api.query.input.handler.Transformer;
import org.wso2.siddhi.query.api.query.input.handler.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BasicStream implements FilteredStream {

    protected String streamId;
    protected StreamDefinition streamDefinition;
    protected String streamReferenceId;
    protected Filter filter = null;
    protected Transformer transformer = null;
    protected boolean isCounterStream = false;
    protected QueryEventStream queryEventStream=null;


    protected BasicStream(String streamId, String streamReferenceId,
                          Filter filter) {
        this.streamId = streamId;
        this.streamReferenceId = streamReferenceId;
        this.filter = filter;
    }

    protected BasicStream(String streamId) {
        this(streamId, streamId);
    }

    public BasicStream(String streamReferenceId, String streamId) {
        this.streamId = streamId;
        this.streamReferenceId = streamReferenceId;
    }

    public StreamDefinition getStreamDefinition() {
        return streamDefinition;
    }

    public void setStreamDefinition(StreamDefinition streamDefinition) {
        this.streamDefinition = streamDefinition;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getStreamReferenceId() {
        return streamReferenceId;
    }

    public BasicStream setStreamReferenceId(String streamReferenceId) {
        this.streamReferenceId = streamReferenceId;
        return this;
    }

    @Override
    public List<String> getStreamIds() {
        List<String> list = new ArrayList<String>();
        list.add(streamId);
        return list;
    }

    public void setCounterStream(boolean counterStream) {
        isCounterStream = counterStream;
    }

    @Override
    public List<QueryEventStream> constructQueryEventStreamList(
            Map<String, StreamDefinition> streamDefinitionMap,
            List<QueryEventStream> queryEventStreams) {
        streamDefinition = streamDefinitionMap.get(streamId);
        if(streamDefinition==null){
            throw new StreamNotExistException("Stream definition not exist! No stream defined with stream ID: "+streamId);
        }
        queryEventStream = new QueryEventStream(streamId, streamReferenceId,
                                                                 streamDefinition,
                                                                 filter, transformer, null);
        queryEventStream.setCounterStream(isCounterStream);
        queryEventStreams.add(queryEventStream);
        return queryEventStreams;
    }


    public Filter getFilter() {
        return filter;
    }

    @Override
    public QueryEventStream getQueryEventStream() {
        return queryEventStream;
    }

    public FilteredStream filter(Condition filterCondition) {
        filter = new Filter(filterCondition);
        return this;
    }

    public FilteredStream setFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    public WindowStream window(String name, Expression... parameters) {
        return new WindowStream(this, new Window(name, parameters));
    }

    @Override
    public WindowStream window(String namespace, String function, Expression... parameters) {
        return new WindowStream(this, new Window(namespace, function, parameters));
    }

    public WindowStream window(Window window) {
        return new WindowStream(this, window);
    }

    @Override
    public TransformedStream transform(String name, Expression... parameters) {
        transformer = new Transformer(name, parameters);
        return this;
    }

    @Override
    public TransformedStream transform(String extensionName, String functionName,
                                       Expression... parameters) {
        transformer = new Transformer(extensionName, functionName, parameters);
        return this;
    }

    public TransformedStream setTransformer(Transformer transformer) {
        this.transformer = transformer;
        return this;
    }

    public Transformer getTransformer() {
        return transformer;
    }
}