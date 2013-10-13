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
package org.wso2.siddhi.core.query.processor.window;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.event.AtomicEvent;
import org.wso2.siddhi.core.event.BundleEvent;
import org.wso2.siddhi.core.event.StreamEvent;
import org.wso2.siddhi.core.event.in.InEvent;
import org.wso2.siddhi.core.event.in.InListEvent;
import org.wso2.siddhi.core.event.management.PersistenceManagementEvent;
import org.wso2.siddhi.core.persistence.PersistenceObject;
import org.wso2.siddhi.core.persistence.PersistenceStore;
import org.wso2.siddhi.core.persistence.Persister;
import org.wso2.siddhi.core.query.MarkedElement;
import org.wso2.siddhi.core.query.QueryPostProcessingElement;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.api.expression.Expression;

import java.util.Iterator;
import java.util.concurrent.locks.Lock;

/**
 * The common interface for all windows, windows always holds RemoveEvents within them.
 * This only processes InStreamEvents
 */
public abstract class WindowProcessor implements Persister, MarkedElement, QueryPostProcessingElement {
    protected static final Logger log = Logger.getLogger(WindowProcessor.class);

    protected String elementId;
    protected PersistenceStore persistenceStore;
    protected SiddhiContext siddhiContext;
    private Lock lock;
    protected QueryPostProcessingElement nextProcessor;
    protected boolean async;
    protected StreamDefinition streamDefinition;

    public abstract void setParameters(Expression[] parameters);

    @Override
    public void process(AtomicEvent atomicEvent) {
        if (atomicEvent instanceof InEvent) {
            processEvent((InEvent) atomicEvent);
        } else {
            log.error("Un expected message type " + atomicEvent.getClass().getCanonicalName() + " for event " + atomicEvent);
        }
    }

    @Override
    public void process(BundleEvent bundleEvent) {
        if (bundleEvent instanceof InListEvent) {
            processEvent((InListEvent) bundleEvent);
        } else {
            log.error("Un expected message type " + bundleEvent.getClass().getCanonicalName() + " for event " + bundleEvent);
        }
    }

    protected abstract void processEvent(InEvent event);

    protected abstract void processEvent(InListEvent listEvent);

    public abstract Iterator<StreamEvent> iterator();

    public abstract Iterator<StreamEvent> iterator(String predicate);

    @Override
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    @Override
    public void setPersistenceStore(PersistenceStore persistenceStore) {
        this.persistenceStore = persistenceStore;
    }

    @Override
    public final void save(PersistenceManagementEvent persistenceManagementEvent) {
        persistenceStore.save(persistenceManagementEvent, elementId, new PersistenceObject(currentState()));
    }

    protected abstract Object[] currentState();

    @Override
    public final void load(PersistenceManagementEvent persistenceManagementEvent) {
        PersistenceObject persistenceObject = persistenceStore.load(persistenceManagementEvent, elementId);
        restoreState(persistenceObject.getData());
    }

    protected abstract void restoreState(Object[] data);

    public void setSiddhiContext(SiddhiContext siddhiContext) {
        this.siddhiContext = siddhiContext;
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }

    protected void acquireLock() {
        if (lock != null) {
            if (log.isDebugEnabled()) {
                log.debug(lock + " trying to acquire window Lock");
            }
            lock.lock();
            if (log.isDebugEnabled()) {
                log.debug(lock + " window locked");
            }
        }
    }

    protected void releaseLock() {
        if (lock != null) {
            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.warn(e.getMessage());
            }
            if (log.isDebugEnabled()) {
                log.debug(lock + " window unlocked");
            }
        }
    }

    public void setNext(QueryPostProcessingElement nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public String getElementId() {
        return elementId;
    }

    public final void init(boolean async) {
        this.async = async;
        initWindow();
    }

    protected abstract void initWindow();

    public void setStreamDefinition(StreamDefinition streamDefinition) {
        this.streamDefinition = streamDefinition;
    }
}
