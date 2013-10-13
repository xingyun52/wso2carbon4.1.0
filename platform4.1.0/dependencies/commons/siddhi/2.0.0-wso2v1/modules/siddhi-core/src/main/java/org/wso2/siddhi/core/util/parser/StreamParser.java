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
package org.wso2.siddhi.core.util.parser;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.executor.conditon.ConditionExecutor;
import org.wso2.siddhi.core.executor.expression.ExpressionExecutor;
import org.wso2.siddhi.core.query.processor.filter.FilterProcessor;
import org.wso2.siddhi.core.query.processor.filter.PassthruFilterProcessor;
import org.wso2.siddhi.core.query.processor.handler.HandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.SimpleHandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.pattern.AndPatternInnerHandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.pattern.CountPatternInnerHandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.pattern.OrPatternInnerHandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.pattern.PatternHandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.pattern.PatternInnerHandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.sequence.CountSequenceInnerHandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.sequence.OrSequenceInnerHandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.sequence.SequenceHandlerProcessor;
import org.wso2.siddhi.core.query.processor.handler.sequence.SequenceInnerHandlerProcessor;
import org.wso2.siddhi.core.query.processor.join.JoinProcessor;
import org.wso2.siddhi.core.query.processor.join.LeftInStreamJoinProcessor;
import org.wso2.siddhi.core.query.processor.join.LeftRemoveStreamJoinProcessor;
import org.wso2.siddhi.core.query.processor.join.RightInStreamJoinProcessor;
import org.wso2.siddhi.core.query.processor.join.RightRemoveStreamJoinProcessor;
import org.wso2.siddhi.core.query.processor.transform.TransformProcessor;
import org.wso2.siddhi.core.query.processor.window.RunnableWindowProcessor;
import org.wso2.siddhi.core.query.processor.window.WindowProcessor;
import org.wso2.siddhi.core.query.projector.QueryProjector;
import org.wso2.siddhi.core.statemachine.pattern.AndPatternState;
import org.wso2.siddhi.core.statemachine.pattern.CountPatternState;
import org.wso2.siddhi.core.statemachine.pattern.OrPatternState;
import org.wso2.siddhi.core.statemachine.pattern.PatternState;
import org.wso2.siddhi.core.statemachine.sequence.CountSequenceState;
import org.wso2.siddhi.core.statemachine.sequence.OrSequenceState;
import org.wso2.siddhi.core.statemachine.sequence.SequenceState;
import org.wso2.siddhi.core.util.SiddhiClassLoader;
import org.wso2.siddhi.core.util.extensionholder.TransformExtensionHolder;
import org.wso2.siddhi.core.util.extensionholder.WindowExtensionHolder;
import org.wso2.siddhi.query.api.condition.Condition;
import org.wso2.siddhi.query.api.condition.ConditionValidator;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.expression.constant.Constant;
import org.wso2.siddhi.query.api.query.QueryEventStream;
import org.wso2.siddhi.query.api.query.input.JoinStream;
import org.wso2.siddhi.query.api.query.input.SingleStream;
import org.wso2.siddhi.query.api.query.input.Stream;
import org.wso2.siddhi.query.api.query.input.WindowStream;
import org.wso2.siddhi.query.api.query.input.handler.Filter;
import org.wso2.siddhi.query.api.query.input.handler.Window;
import org.wso2.siddhi.query.api.query.input.pattern.PatternStream;
import org.wso2.siddhi.query.api.query.input.sequence.SequenceStream;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StreamParser {

    static final Logger log = Logger.getLogger(StreamParser.class);


    public static List<HandlerProcessor> parseStream(Stream queryStream,
                                                     List<QueryEventStream> queryEventStreamList,
                                                     QueryProjector queryProjector,
                                                     SiddhiContext siddhiContext) {
        List<HandlerProcessor> handlerProcessorList = new ArrayList<HandlerProcessor>();

        if (queryStream instanceof SingleStream) {

            QueryEventStream queryEventStream = ((SingleStream) queryStream).getQueryEventStream();

            SimpleHandlerProcessor simpleHandlerProcessor = new SimpleHandlerProcessor(queryEventStream,
                                                                                       generateFilerProcessor(queryEventStream, queryEventStreamList, siddhiContext),
                                                                                       generateTransformProcessor(queryEventStream, queryEventStreamList, siddhiContext),
                                                                                       siddhiContext);

            if (queryStream instanceof WindowStream) {
                WindowProcessor windowProcessor = generateWindowProcessor(queryEventStream, siddhiContext, null, false);
                simpleHandlerProcessor.setNext(windowProcessor);
                windowProcessor.setNext(queryProjector);
            } else {
                simpleHandlerProcessor.setNext(queryProjector);
            }

            handlerProcessorList.add(simpleHandlerProcessor);
            return handlerProcessorList;

        } else if (queryStream instanceof JoinStream) {
            ConditionExecutor onConditionExecutor;
            if (((JoinStream) queryStream).getOnCompare() != null) {
                onConditionExecutor = ExecutorParser.parseCondition(((JoinStream) queryStream).getOnCompare(), queryEventStreamList, null, false, siddhiContext);
            } else {
                onConditionExecutor = ExecutorParser.parseCondition(Condition.bool(Expression.value(true)), queryEventStreamList, null, false, siddhiContext);
            }
            JoinProcessor leftInStreamJoinProcessor;
            JoinProcessor rightInStreamJoinProcessor;
            JoinProcessor leftRemoveStreamJoinProcessor;
            JoinProcessor rightRemoveStreamJoinProcessor;
            Lock lock;
            if (siddhiContext.isDistributedProcessing()) {
                lock = siddhiContext.getHazelcastInstance().getLock(siddhiContext.getElementIdGenerator().createNewId() + "-join-lock");
            } else {
                lock = new ReentrantLock();
            }
            switch (((JoinStream) queryStream).getTrigger()) {
                case LEFT:
                    leftInStreamJoinProcessor = new LeftInStreamJoinProcessor(onConditionExecutor, true, siddhiContext.isDistributedProcessing(), lock);
                    rightInStreamJoinProcessor = new RightInStreamJoinProcessor(onConditionExecutor, false, siddhiContext.isDistributedProcessing(), lock);
                    leftRemoveStreamJoinProcessor = new LeftRemoveStreamJoinProcessor(onConditionExecutor, true, siddhiContext.isDistributedProcessing(), lock);
                    rightRemoveStreamJoinProcessor = new RightRemoveStreamJoinProcessor(onConditionExecutor, false, siddhiContext.isDistributedProcessing(), lock);
                    break;
                case RIGHT:
                    leftInStreamJoinProcessor = new LeftInStreamJoinProcessor(onConditionExecutor, false, siddhiContext.isDistributedProcessing(), lock);
                    rightInStreamJoinProcessor = new RightInStreamJoinProcessor(onConditionExecutor, true, siddhiContext.isDistributedProcessing(), lock);
                    leftRemoveStreamJoinProcessor = new LeftRemoveStreamJoinProcessor(onConditionExecutor, false, siddhiContext.isDistributedProcessing(), lock);
                    rightRemoveStreamJoinProcessor = new RightRemoveStreamJoinProcessor(onConditionExecutor, true, siddhiContext.isDistributedProcessing(), lock);
                    break;
                default:
                    leftInStreamJoinProcessor = new LeftInStreamJoinProcessor(onConditionExecutor, true, siddhiContext.isDistributedProcessing(), lock);
                    rightInStreamJoinProcessor = new RightInStreamJoinProcessor(onConditionExecutor, true, siddhiContext.isDistributedProcessing(), lock);
                    leftRemoveStreamJoinProcessor = new LeftRemoveStreamJoinProcessor(onConditionExecutor, true, siddhiContext.isDistributedProcessing(), lock);
                    rightRemoveStreamJoinProcessor = new RightRemoveStreamJoinProcessor(onConditionExecutor, true, siddhiContext.isDistributedProcessing(), lock);
                    break;
            }
            Constant within = ((JoinStream) queryStream).getWithin();
            if (within != null) {
                long longValue = ExecutorParser.getLong(within);
                leftInStreamJoinProcessor.setWithin(longValue);
                rightInStreamJoinProcessor.setWithin(longValue);
                leftRemoveStreamJoinProcessor.setWithin(longValue);
                rightRemoveStreamJoinProcessor.setWithin(longValue);
            }

            QueryEventStream leftQueryEventStream = ((SingleStream) ((JoinStream) queryStream).getLeftStream()).getQueryEventStream();
            QueryEventStream rightQueryEventStream = ((SingleStream) ((JoinStream) queryStream).getRightStream()).getQueryEventStream();

            WindowProcessor leftWindowProcessor = generateWindowProcessor(leftQueryEventStream, siddhiContext, lock, false);
            WindowProcessor rightWindowProcessor = generateWindowProcessor(rightQueryEventStream, siddhiContext, lock, false);

            SimpleHandlerProcessor leftSimpleHandlerProcessor = new SimpleHandlerProcessor(leftQueryEventStream,
                                                                                           generateFilerProcessor(leftQueryEventStream, queryEventStreamList, siddhiContext),
                                                                                           generateTransformProcessor(leftQueryEventStream, queryEventStreamList, siddhiContext),
                                                                                           siddhiContext);
            SimpleHandlerProcessor rightSimpleHandlerProcessor = new SimpleHandlerProcessor(rightQueryEventStream,
                                                                                            generateFilerProcessor(rightQueryEventStream, queryEventStreamList, siddhiContext),
                                                                                            generateTransformProcessor(rightQueryEventStream, queryEventStreamList, siddhiContext),
                                                                                            siddhiContext);

            leftSimpleHandlerProcessor.setNext(leftInStreamJoinProcessor);
            rightSimpleHandlerProcessor.setNext(rightInStreamJoinProcessor);

            //joinStreamPacker next
            leftInStreamJoinProcessor.setNext(queryProjector);
            rightInStreamJoinProcessor.setNext(queryProjector);
            leftRemoveStreamJoinProcessor.setNext(queryProjector);
            rightRemoveStreamJoinProcessor.setNext(queryProjector);

            //Window joinStreamPacker relation settings
            leftInStreamJoinProcessor.setWindowProcessor(leftWindowProcessor);
            leftWindowProcessor.setNext(leftRemoveStreamJoinProcessor);

            rightInStreamJoinProcessor.setWindowProcessor(rightWindowProcessor);
            rightWindowProcessor.setNext(rightRemoveStreamJoinProcessor);


            //joinStreamPacker prev
            rightInStreamJoinProcessor.setOppositeWindowProcessor(leftInStreamJoinProcessor.getWindowProcessor());
            leftInStreamJoinProcessor.setOppositeWindowProcessor(rightInStreamJoinProcessor.getWindowProcessor());

            rightRemoveStreamJoinProcessor.setOppositeWindowProcessor(leftInStreamJoinProcessor.getWindowProcessor());
            leftRemoveStreamJoinProcessor.setOppositeWindowProcessor(rightInStreamJoinProcessor.getWindowProcessor());

            handlerProcessorList.add(leftSimpleHandlerProcessor);
            handlerProcessorList.add(rightSimpleHandlerProcessor);
            return handlerProcessorList;

        } else if (queryStream instanceof PatternStream) {

            List<PatternState> patternStateList = StateParser.convertToPatternStateList(StateParser.identifyStates(((PatternStream) queryStream).getPatternElement()));
            //    queryEventStreamList ;
            // PatternQueryStreamPacker patternStreamPacker = new PatternQueryStreamPacker(stateList);
            // PatternQuerySingleStreamProcessor[] patternSingleStreamAnalyserArray = new PatternQuerySingleStreamProcessor[stateList.size()];
            for (String streamId : queryStream.getStreamIds()) {

                //    List<BasicStream> streamList = new ArrayList<BasicStream>();
                List<PatternInnerHandlerProcessor> patternInnerHandlerProcessorList = new ArrayList<PatternInnerHandlerProcessor>();
                for (PatternState state : patternStateList) {
                    if (state.getTransformedStream().getStreamId().equals(streamId)) {
                        //           streamList.add(state.getTransformedStream());

                        FilterProcessor filterProcessor = generateFilerProcessor(state.getTransformedStream().getQueryEventStream(), queryEventStreamList, siddhiContext);
                        PatternInnerHandlerProcessor patternInnerHandlerProcessor;

                        if (state instanceof OrPatternState) {
                            patternInnerHandlerProcessor = new OrPatternInnerHandlerProcessor(((OrPatternState) state), filterProcessor, patternStateList.size(), siddhiContext,
                                                                                              siddhiContext.getElementIdGenerator().createNewId());
                        } else if (state instanceof AndPatternState) {
                            patternInnerHandlerProcessor = new AndPatternInnerHandlerProcessor(((AndPatternState) state), filterProcessor, patternStateList.size(), siddhiContext,
                                                                                               siddhiContext.getElementIdGenerator().createNewId());
                        } else if (state instanceof CountPatternState) {
                            patternInnerHandlerProcessor = new CountPatternInnerHandlerProcessor(((CountPatternState) state), filterProcessor, patternStateList.size(), siddhiContext,
                                                                                                 siddhiContext.getElementIdGenerator().createNewId());
                        } else {
                            patternInnerHandlerProcessor = new PatternInnerHandlerProcessor(state, filterProcessor, patternStateList.size(), siddhiContext,
                                                                                            siddhiContext.getElementIdGenerator().createNewId());
                        }

                        state.setPatternInnerHandlerProcessor(patternInnerHandlerProcessor);
                        patternInnerHandlerProcessorList.add(patternInnerHandlerProcessor);
                        //  patternSingleStreamAnalyserArray[state.getStateNumber()] = patternInnerHandlerProcessor;

                        patternInnerHandlerProcessor.setQueryProjector(queryProjector);

                        //patternInnerHandlerProcessor.setPrevious(singleStreamPacker); since not needed not set
                    }
                }

                PatternHandlerProcessor patternHandlerProcessor = new PatternHandlerProcessor(streamId, patternInnerHandlerProcessorList, siddhiContext);
                patternHandlerProcessor.setElementId(siddhiContext.getElementIdGenerator().createNewId());

                handlerProcessorList.add(patternHandlerProcessor);

                //for persistence, elementId marking and window
                for (PatternInnerHandlerProcessor patternInnerHandlerProcessor : patternInnerHandlerProcessorList) {
                    if (((PatternStream) queryStream).getWithin() != null) {
                        patternInnerHandlerProcessor.setWithin(ExecutorParser.getLong(((PatternStream) queryStream).getWithin()));
                    }
                    siddhiContext.getPersistenceService().addPersister(patternInnerHandlerProcessor);
                }

            }


            //   patternStreamPacker.setPatternSingleStreamAnalyserArray(patternSingleStreamAnalyserArray);
            //patternStreamPacker next
            //  patternStreamPacker.setNext(next, 0);

            for (PatternState state : patternStateList) {
                state.getInnerHandlerProcessor().init();
            }
            return handlerProcessorList;

        } else if (queryStream instanceof SequenceStream) {


            List<SequenceState> sequenceStateList = StateParser.convertToSequenceStateList(StateParser.identifyStates(((SequenceStream) queryStream).getSequenceElement()));
            //    queryEventStreamList ;
            // PatternQueryStreamPacker patternStreamPacker = new PatternQueryStreamPacker(stateList);
            // PatternQuerySingleStreamProcessor[] patternSingleStreamAnalyserArray = new PatternQuerySingleStreamProcessor[stateList.size()];
            for (String streamId : queryStream.getStreamIds()) {

                //    List<BasicStream> streamList = new ArrayList<BasicStream>();
                List<SequenceInnerHandlerProcessor> sequenceInnerHandlerProcessorList = new ArrayList<SequenceInnerHandlerProcessor>();
                for (SequenceState state : sequenceStateList) {
                    if (state.getTransformedStream().getStreamId().equals(streamId)) {
                        //           streamList.add(state.getTransformedStream());


                        FilterProcessor filterProcessor = generateFilerProcessor(state.getTransformedStream().getQueryEventStream(), queryEventStreamList, siddhiContext);
                        SequenceInnerHandlerProcessor sequenceInnerHandlerProcessor;


                        if (state instanceof OrSequenceState) {
                            sequenceInnerHandlerProcessor = new OrSequenceInnerHandlerProcessor(((OrSequenceState) state), filterProcessor, sequenceStateList.size(),
                                                                                                siddhiContext, siddhiContext.getElementIdGenerator().createNewId());
                        } else if (state instanceof CountSequenceState) {
                            sequenceInnerHandlerProcessor = new CountSequenceInnerHandlerProcessor(((CountSequenceState) state), filterProcessor, sequenceStateList.size(),
                                                                                                   siddhiContext, siddhiContext.getElementIdGenerator().createNewId());
                        } else {
                            sequenceInnerHandlerProcessor = new SequenceInnerHandlerProcessor(state, filterProcessor, sequenceStateList.size(),
                                                                                              siddhiContext, siddhiContext.getElementIdGenerator().createNewId());
                        }

                        state.setSequenceInnerHandlerProcessor(sequenceInnerHandlerProcessor);
                        sequenceInnerHandlerProcessorList.add(sequenceInnerHandlerProcessor);
                        //  patternSingleStreamAnalyserArray[state.getStateNumber()] = patternSingleStreamAnalyser;

                        sequenceInnerHandlerProcessor.setNext(queryProjector);

                    }
                }

                SequenceHandlerProcessor sequenceHandlerProcessor = new SequenceHandlerProcessor(streamId, sequenceInnerHandlerProcessorList, siddhiContext);
                sequenceHandlerProcessor.setElementId(siddhiContext.getElementIdGenerator().createNewId());

                handlerProcessorList.add(sequenceHandlerProcessor);

                //for persistence, elementId marking and window
                for (SequenceInnerHandlerProcessor sequenceInnerHandlerProcessor : sequenceInnerHandlerProcessorList) {
                    if (((SequenceStream) queryStream).getWithin() != null) {
                        sequenceInnerHandlerProcessor.setWithin(ExecutorParser.getLong(((SequenceStream) queryStream).getWithin()));
                    }
                    siddhiContext.getPersistenceService().addPersister(sequenceInnerHandlerProcessor);
                }
            }

            //   patternStreamPacker.setPatternSingleStreamAnalyserArray(patternSingleStreamAnalyserArray);
            //patternStreamPacker next
            //  patternStreamPacker.setNext(next, 0);

            for (SequenceState state : sequenceStateList) {
                state.getSequenceInnerHandlerProcessor().init();
            }

            for (HandlerProcessor queryStreamProcessor : handlerProcessorList) {
                List<SequenceInnerHandlerProcessor> otherStreamAnalyserList = new ArrayList<SequenceInnerHandlerProcessor>();
                for (HandlerProcessor otherQueryStreamProcessor : handlerProcessorList) {
                    if (otherQueryStreamProcessor != queryStreamProcessor) {
                        otherStreamAnalyserList.addAll(((SequenceHandlerProcessor) otherQueryStreamProcessor).
                                getSequenceInnerHandlerProcessorList());
                    }
                }
                ((SequenceHandlerProcessor) queryStreamProcessor).setOtherSequenceInnerHandlerProcessorList(otherStreamAnalyserList);
            }
            return handlerProcessorList;
        }
        return handlerProcessorList;

    }


    private static FilterProcessor generateFilerProcessor(QueryEventStream queryEventStream,
                                                          List<QueryEventStream> queryEventStreamList,
                                                          SiddhiContext siddhiContext) {
        Filter filter = queryEventStream.getFilter();
        if (filter == null) {
            return new PassthruFilterProcessor();
        } else {
            Condition condition = filter.getFilterCondition();
            ConditionValidator.validate(condition, queryEventStreamList, queryEventStream.getReferenceStreamId(), true);
            return new FilterProcessor(ExecutorParser.parseCondition(condition, queryEventStreamList, queryEventStream.getReferenceStreamId(), true, siddhiContext));
        }


    }

    private static TransformProcessor generateTransformProcessor(QueryEventStream queryEventStream,
                                                                 List<QueryEventStream> queryEventStreamList,
                                                                 SiddhiContext siddhiContext) {
        if (queryEventStream.getTransformer() == null) {
            return null;
        }
        TransformProcessor transformProcessor = (TransformProcessor) SiddhiClassLoader.loadProcessor(queryEventStream.getTransformer().getName(), queryEventStream.getTransformer().getExtension(),
                                                                                                     TransformProcessor.class, TransformExtensionHolder.getInstance(siddhiContext));

        transformProcessor.setSiddhiContext(siddhiContext);
        transformProcessor.setInStreamDefinition(queryEventStream.getInStreamDefinition());
        List<ExpressionExecutor> expressionExecutors = new LinkedList<ExpressionExecutor>();
        for (Expression expression : queryEventStream.getTransformer().getParameters()) {
            expressionExecutors.add(ExecutorParser.parseExpression(expression, queryEventStreamList, queryEventStream.getReferenceStreamId(), true, siddhiContext));
        }
        transformProcessor.setExpressionExecutors(expressionExecutors);
        transformProcessor.setParameters(queryEventStream.getTransformer().getParameters());

        //for adding elementId
        transformProcessor.setElementId(siddhiContext.getElementIdGenerator().createNewId());

        //for persistence
        siddhiContext.getPersistenceService().addPersister(transformProcessor);
        return transformProcessor;
    }

//    private static void connectToStreamFlow(List<QueryStreamProcessor> queryStreamProcessorList,
//                                            QueryStreamProcessor queryStreamProcessor) {
//        if (queryStreamProcessorList.size() > 0) {
//            QueryStreamHandler prevStreamHandler = (QueryStreamHandler) queryStreamProcessorList.get(queryStreamProcessorList.size() - 1);
//            prevStreamHandler.setNext(queryStreamProcessor);
//        }
//        queryStreamProcessorList.add(queryStreamProcessor);
//    }

    private static WindowProcessor generateWindowProcessor(QueryEventStream queryEventStream,
                                                           SiddhiContext siddhiContext, Lock lock,
                                                           boolean async) {
        Window window = queryEventStream.getWindow();
        if (window == null) {
            window = new Window("length", new Expression[]{Expression.value(Integer.MAX_VALUE)});
        }
        WindowProcessor windowProcessor = (WindowProcessor) SiddhiClassLoader.loadProcessor(window.getName(), window.getExtension(),
                                                                                            WindowProcessor.class, WindowExtensionHolder.getInstance(siddhiContext));

//                    Window window = new TimeWindowProcessor();
        windowProcessor.setSiddhiContext(siddhiContext);
        windowProcessor.setStreamDefinition(queryEventStream.getOutStreamDefinition());
        if (windowProcessor instanceof RunnableWindowProcessor) {
            ((RunnableWindowProcessor) windowProcessor).setScheduledExecutorService(siddhiContext.getScheduledExecutorService());
            ((RunnableWindowProcessor) windowProcessor).setThreadBarrier(siddhiContext.getThreadBarrier());
        }
        windowProcessor.setParameters(window.getParameters());

        //for adding elementId
        windowProcessor.setElementId(siddhiContext.getElementIdGenerator().createNewId());

        if (lock == null) {
            if (siddhiContext.isDistributedProcessing()) {
                windowProcessor.setLock(siddhiContext.getHazelcastInstance().getLock(windowProcessor.getElementId() + "-lock"));
            } else {
                windowProcessor.setLock(new ReentrantLock());
            }
        } else {
            windowProcessor.setLock(lock);
        }
        //for persistence
        siddhiContext.getPersistenceService().addPersister(windowProcessor);
        windowProcessor.init(async);
        return windowProcessor;
    }

}
