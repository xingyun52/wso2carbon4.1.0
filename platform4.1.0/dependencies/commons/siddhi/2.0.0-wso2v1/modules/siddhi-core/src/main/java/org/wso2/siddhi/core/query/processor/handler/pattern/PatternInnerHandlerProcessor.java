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
package org.wso2.siddhi.core.query.processor.handler.pattern;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.event.AtomicEvent;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.StateEvent;
import org.wso2.siddhi.core.event.StreamEvent;
import org.wso2.siddhi.core.event.in.InStateEvent;
import org.wso2.siddhi.core.event.management.PersistenceManagementEvent;
import org.wso2.siddhi.core.persistence.PersistenceObject;
import org.wso2.siddhi.core.persistence.PersistenceStore;
import org.wso2.siddhi.core.persistence.Persister;
import org.wso2.siddhi.core.query.processor.filter.FilterProcessor;
import org.wso2.siddhi.core.query.processor.handler.InnerHandlerProcessor;
import org.wso2.siddhi.core.query.projector.QueryProjector;
import org.wso2.siddhi.core.statemachine.pattern.LogicPatternState;
import org.wso2.siddhi.core.statemachine.pattern.PatternState;
import org.wso2.siddhi.core.util.LogHelper;
import org.wso2.siddhi.core.util.statelist.StateList;
import org.wso2.siddhi.core.util.statelist.StateListGrid;

import java.util.Collection;

public class PatternInnerHandlerProcessor
        implements InnerHandlerProcessor, Persister {

    static final Logger log = Logger.getLogger(PatternInnerHandlerProcessor.class);
    protected int complexEventSize;
    protected PatternState state;
    protected PatternState nextState;
    protected PatternState nextEveryState;
    protected FilterProcessor filterProcessor;
    protected StateList<StateEvent> currentEvents;
    protected StateList<StateEvent> nextEvents;
    protected final int currentState;
    protected String elementId;
    protected PersistenceStore persistenceStore;
    private long within = -1;
    protected boolean distributedProcessing;
    protected SiddhiContext siddhiContext;
    protected QueryProjector queryProjector;


    public PatternInnerHandlerProcessor(PatternState state,
                                        FilterProcessor filterProcessor,
                                        int complexEventSize, SiddhiContext siddhiContext, String elementId) {
        this.state = state;
        this.elementId = elementId;
        this.nextState = state.getNextState();
        this.nextEveryState = state.getNextEveryState();
        this.currentState = state.getStateNumber();
        this.complexEventSize = complexEventSize;
        this.distributedProcessing = siddhiContext.isDistributedProcessing();
        this.siddhiContext = siddhiContext;
        this.filterProcessor = filterProcessor;
        this.elementId = siddhiContext.getElementIdGenerator().createNewId();
        if (distributedProcessing) {
            currentEvents = new StateListGrid(this.elementId + "-eventBank", siddhiContext);
            nextEvents = new StateListGrid(this.elementId + "-nextEvents", siddhiContext);
        } else {
            currentEvents = new StateList<StateEvent>();
            nextEvents = new StateList<StateEvent>();
        }
    }

    public void init() {
        if (state.isFirst()) {
            //first event
            if (distributedProcessing) {
                if (!nextEvents.isInited()) {
                    addToNextEvents(new InStateEvent(new StreamEvent[complexEventSize], siddhiContext.getGlobalIndexGenerator().getNewIndex()));
                }
            } else {
                addToNextEvents(new InStateEvent(new StreamEvent[complexEventSize]));
            }
        }
    }

    @Override
    public void process(StreamEvent event) {
        try {
            LogHelper.logMethod(log, event);

            Collection<StateEvent> collection = getCollection();
            if( log.isDebugEnabled()){
                LogHelper.debugLogMessage(log, event,  "current events:" + collection.size());
            }
            for (StateEvent currentEvent : collection) {
                if( log.isDebugEnabled()){
                    LogHelper.debugLogMessage(log, event, "processing with event:" + currentEvent);
                }
                if (isEventsWithin(event, currentEvent)) {
                    currentEvent.setStreamEvent(currentState, event);
                    StateEvent newEvent = (StateEvent) filterProcessor.process(currentEvent);
                    if (newEvent != null) {
                        processSuccessEvent(newEvent);
                    } else {
                        currentEvent.setStreamEvent(currentState, null);
                        addToNextEvents(currentEvent);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void processSuccessEvent(StateEvent stateEvent) {
        LogHelper.logMethod(log, (AtomicEvent) stateEvent);
        setEventState(stateEvent);
        if (state.isLast()) {
            sendEvent(stateEvent);
        }
        passToNextStates(stateEvent);
    }

    protected Collection<StateEvent> getCollection() {
        Collection<StateEvent> collection;
        if (distributedProcessing) {
            if (within > -1) {
                collection = ((StateListGrid) currentEvents).getCollection("( timeStamp < " + (System.currentTimeMillis() + within) + ")");
            } else {
                collection = currentEvents.getCollection();
            }
        } else {
            collection = currentEvents.getCollection();
        }
        return collection;
    }

    protected boolean isEventsWithin(StreamEvent incomingEvent, StateEvent currentEvent) {
        if (log.isDebugEnabled()) {
            LogHelper.debugLogMessage(log, incomingEvent, "time difference for Pattern events " + (incomingEvent.getTimeStamp() - currentEvent.getFirstEventTimeStamp()));
        }
        if (within == -1 || currentEvent.getFirstEventTimeStamp() == 0) {
            return true;
        } else if ((incomingEvent.getTimeStamp() - currentEvent.getFirstEventTimeStamp()) <= within) {
            return true;
        } else {
            return false;
        }
    }

    public String getStreamId() {
        return state.getTransformedStream().getStreamId();
    }

    @Override
    public void addToNextEvents(StateEvent stateEvent) {
        LogHelper.logMethod(log, (AtomicEvent)stateEvent);
        try {
            nextEvents.put(stateEvent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void moveNextEventsToCurrentEvents() {
        //todo need to check which is faster
        // 1
        currentEvents.clear();
        currentEvents.addAll(nextEvents.getAll());
        nextEvents.clear();

//        // 2
//        eventBank = nextEvents;
//        nextEvents = new LinkedBlockingQueue<StateEvent>();
    }

    public String getElementId() {
        return elementId;
    }

    @Override
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    @Override
    public void setPersistenceStore(PersistenceStore persistenceStore) {
        this.persistenceStore = persistenceStore;
    }

    @Override
    public void save(PersistenceManagementEvent persistenceManagementEvent) {
        persistenceStore.save(persistenceManagementEvent, elementId, new PersistenceObject(currentEvents.currentState(), nextEvents.currentState()));
    }

    @Override
    public void load(PersistenceManagementEvent persistenceManagementEvent) {
        PersistenceObject persistenceObject = persistenceStore.load(persistenceManagementEvent, elementId);
        currentEvents.restoreState((Object[]) persistenceObject.getData()[0]);
        nextEvents.restoreState((Object[]) persistenceObject.getData()[1]);
    }

    public void setWithin(long within) {
        this.within = within;
    }

    public void updateToCurrentEvents(StateEvent updateContainingStateEvent, int updatingState) {
        ((StateListGrid) currentEvents).update(updateContainingStateEvent, updatingState);
    }

    public void updateToNextEvents(StateEvent updateContainingStateEvent, int updatingState) {
        ((StateListGrid) nextEvents).update(updateContainingStateEvent, updatingState);
    }

    public void removeFromCurrentEvents(StateEvent removingStateEvent) {
        ((StateListGrid) currentEvents).remove(removingStateEvent);
    }

    public void removeFromNextEvents(StateEvent removingStateEvent) {
        ((StateListGrid) nextEvents).remove(removingStateEvent);
    }

    protected void setEventState(StateEvent eventBundle) {
       if( log.isDebugEnabled()){
        LogHelper.debugLogMessage(log, (ComplexEvent)eventBundle, "moves to state -> " + state.getStateNumber());
       }
        eventBundle.setEventState(state.getStateNumber());
    }

    protected void sendEvent(AtomicEvent atomicEvent) {
        queryProjector.process(atomicEvent);
    }


    public void setQueryProjector(QueryProjector queryProjector) {
        this.queryProjector = queryProjector;
    }

    protected void passToNextStates(StateEvent eventBundle) {
        if (nextState != null) {
            if (log.isDebugEnabled()) {
                log.debug("->" + nextState.getStateNumber());
            }
            if (nextState instanceof LogicPatternState) {
                if (log.isDebugEnabled()) {
                    log.debug("->" + ((LogicPatternState) nextState).getPartnerState().getStateNumber());
                }
                ((LogicPatternState) nextState).getPartnerState().getInnerHandlerProcessor().addToNextEvents(eventBundle);
            }
            nextState.getInnerHandlerProcessor().addToNextEvents(eventBundle);
        }
        if (nextEveryState != null) {
            if (log.isDebugEnabled()) {
                log.debug("->" + nextEveryState.getStateNumber());
            }
            StateEvent newStateEvent;
            if (distributedProcessing) {
                newStateEvent = eventBundle.cloneEvent(nextEveryState.getStateNumber(), siddhiContext.getGlobalIndexGenerator().getNewIndex());
            } else {
                newStateEvent = eventBundle.cloneEvent(nextEveryState.getStateNumber(), null);
            }
            newStateEvent.setEventState(nextEveryState.getStateNumber() - 1);
            if (nextEveryState instanceof LogicPatternState) {
                if (log.isDebugEnabled()) {
                    log.debug("->" + ((LogicPatternState) nextEveryState).getPartnerState().getStateNumber());
                }
                ((LogicPatternState) nextEveryState).getPartnerState().getInnerHandlerProcessor().addToNextEvents(newStateEvent);
            }
            nextEveryState.getInnerHandlerProcessor().addToNextEvents(newStateEvent);
        }

    }
}
