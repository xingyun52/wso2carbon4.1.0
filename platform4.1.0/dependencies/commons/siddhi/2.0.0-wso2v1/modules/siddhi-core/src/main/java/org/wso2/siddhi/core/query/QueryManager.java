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
package org.wso2.siddhi.core.query;

import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.query.processor.handler.HandlerProcessor;
import org.wso2.siddhi.core.query.processor.transform.TransformProcessor;
import org.wso2.siddhi.core.query.projector.QueryProjector;
import org.wso2.siddhi.core.stream.StreamJunction;
import org.wso2.siddhi.core.util.SiddhiClassLoader;
import org.wso2.siddhi.core.util.extensionholder.TransformExtensionHolder;
import org.wso2.siddhi.core.util.parser.StreamParser;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.expression.ExpressionValidator;
import org.wso2.siddhi.query.api.query.Query;
import org.wso2.siddhi.query.api.query.QueryEventStream;
import org.wso2.siddhi.query.api.query.input.SingleStream;
import org.wso2.siddhi.query.api.query.input.handler.Transformer;
import org.wso2.siddhi.query.api.query.projection.attribute.ComplexAttribute;
import org.wso2.siddhi.query.api.query.projection.attribute.OutputAttribute;
import org.wso2.siddhi.query.api.query.projection.attribute.OutputAttributeExtension;
import org.wso2.siddhi.query.api.query.projection.attribute.SimpleAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class QueryManager {
    private String queryId;
    private Query query;
    private QueryProjector queryProjector;
    private List<HandlerProcessor> queryStreamProcessorList;

    public QueryManager(Query query, ConcurrentMap<String, StreamDefinition> streamDefinitionMap,
                        ConcurrentMap<String, StreamJunction> streamJunctionMap,
                        SiddhiContext siddhiContext) {
        if (query.getOutputStream() != null) {
            this.queryId = query.getOutputStream().getStreamId() + "-" + UUID.randomUUID();
        } else {
            this.queryId = UUID.randomUUID().toString();
        }
        this.query = query;
        List<QueryEventStream> queryEventStreamList = query.getInputStream().constructQueryEventStreamList(streamDefinitionMap, new ArrayList<QueryEventStream>());

        updateOutDefinitionsToQueryEventStreams(queryEventStreamList, siddhiContext);

        validateOutput(queryEventStreamList);
        populateProjection(queryEventStreamList, query);

        queryProjector = new QueryProjector(query.getOutputStream(), query.getProjector(), queryEventStreamList, streamJunctionMap, siddhiContext);
        queryStreamProcessorList = StreamParser.parseStream(query.getInputStream(), queryEventStreamList, queryProjector, siddhiContext);

        for (HandlerProcessor queryStreamProcessor : queryStreamProcessorList) {
            streamJunctionMap.get(queryStreamProcessor.getStreamId()).addEventFlow(queryStreamProcessor);
        }
    }

    private void updateOutDefinitionsToQueryEventStreams(
            List<QueryEventStream> queryEventStreamList, SiddhiContext siddhiContext) {
        for (QueryEventStream queryEventStream : queryEventStreamList) {
            if (queryEventStream.getTransformer() == null) {
                queryEventStream.setOutStreamDefinition(queryEventStream.getInStreamDefinition());
            } else {
                Transformer transformer = queryEventStream.getTransformer();
                TransformProcessor transformProcessor = (TransformProcessor) SiddhiClassLoader.loadProcessor(transformer.getName(), transformer.getExtension(), TransformProcessor.class,
                                                                                                             TransformExtensionHolder.getInstance(siddhiContext));
                queryEventStream.setOutStreamDefinition(transformProcessor.getOutStreamDefinition());
            }
        }
    }


    public String getQueryId() {
        return queryId;
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryManager)) {
            return false;
        }

        QueryManager that = (QueryManager) o;

        if (queryId != null ? !queryId.equals(that.queryId) : that.queryId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return queryId != null ? queryId.hashCode() : 0;
    }

    public void setQueryProjector(QueryProjector queryProjector) {
        this.queryProjector = queryProjector;
    }

    public void setQueryStreamProcessorList(List<HandlerProcessor> queryStreamProcessorList) {
        this.queryStreamProcessorList = queryStreamProcessorList;
    }

    private void populateProjection(List<QueryEventStream> queryEventStreamList, Query query) {

        //populate projection for * case
        List<OutputAttribute> attributeList = this.query.getProjector().getProjectionList();
        if (attributeList.size() == 0) {
            if (query.getInputStream() instanceof SingleStream && queryEventStreamList.size() == 1) {
                QueryEventStream queryEventStream = queryEventStreamList.get(0);
                for (Attribute attribute : queryEventStreamList.get(0).getOutStreamDefinition().getAttributeList()) {
                    attributeList.add(new SimpleAttribute(attribute.getName(), Expression.variable(queryEventStream.getReferenceStreamId(), attribute.getName())));
                }
            } else {
                for (QueryEventStream queryEventStream : queryEventStreamList) {
                    for (Attribute attribute : queryEventStream.getOutStreamDefinition().getAttributeList()) {
                        attributeList.add(new SimpleAttribute(queryEventStream.getReferenceStreamId() + "_" + attribute.getName(), Expression.variable(queryEventStream.getReferenceStreamId(), attribute.getName())));
                    }
                }
            }
        }

    }

    public QueryProjector getQueryProjector() {
        return queryProjector;
    }

    public StreamDefinition getOutputStreamDefinition() {
        return queryProjector.getOutputStreamDefinition();
    }

    public List<HandlerProcessor> getQueryStreamProcessorList() {
        return queryStreamProcessorList;
    }

    public void removeQuery(ConcurrentMap<String, StreamJunction> streamJunctionMap,
                            ConcurrentMap<String, StreamDefinition> streamDefinitionMap) {
        for (HandlerProcessor queryStreamProcessor : queryStreamProcessorList) {
            StreamJunction junction = streamJunctionMap.get(queryStreamProcessor.getStreamId());
            junction.removeEventFlow(queryStreamProcessor);
        }
        streamDefinitionMap.remove(query.getOutputStream().getStreamId());
    }

    private void validateOutput(List<QueryEventStream> queryEventStreamList) {

        List<OutputAttribute> outputAttributeList = query.getProjector().getProjectionList();
        for (OutputAttribute outputAttribute : outputAttributeList) {
            if (outputAttribute instanceof SimpleAttribute) {
                ExpressionValidator.validate(((SimpleAttribute) outputAttribute).getExpression(), queryEventStreamList, null, false);
            } else if (outputAttribute instanceof ComplexAttribute) {
                for (Expression expression : ((ComplexAttribute) outputAttribute).getExpressions()) {
                    ExpressionValidator.validate(expression, queryEventStreamList, null, false);
                }
            } else {//Extension
                for (Expression expression : ((OutputAttributeExtension) outputAttribute).getExpressions()) {
                    ExpressionValidator.validate(expression, queryEventStreamList, null, false);
                }
            }
        }


    }


}
