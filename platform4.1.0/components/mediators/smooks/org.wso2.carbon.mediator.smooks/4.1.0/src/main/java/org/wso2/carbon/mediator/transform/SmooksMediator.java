/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.mediator.transform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.mediators.AbstractMediator;
import org.milyn.Smooks;
import org.milyn.SmooksException;
import org.milyn.container.ExecutionContext;
import org.milyn.payload.JavaResult;
import org.milyn.persistence.util.PersistenceUtil;
import org.milyn.scribe.adapter.jpa.EntityManagerRegister;
import org.xml.sax.SAXException;

import org.wso2.carbon.mediator.service.MediatorException;

/**
 * Transforms the current message payload using the given Smooks configuration.
 * The current message context is replaced with the result as XML.
 */
public class SmooksMediator extends AbstractMediator {
	public enum TYPES {
		TEXT, XML, JAVA
	}

	/** Smooks engine */
	private Smooks smooks = null;
	/** Smooks configuration file */
	private String configKey = null;
	/** This lock is used to create the smooks configuration synchronously */
	private volatile Lock lock = new ReentrantLock();

	private Input input = null;

	private Output output = null;
	/** JPA Persistence Unit Name */
	private String persistenceUnitName = null;

	private EntityTransaction transaction = null;

	private boolean transactionStarted = false;

	private EntityManagerFactory emf;

	public boolean mediate(MessageContext synCtx) {
		SynapseLog synLog = getLog(synCtx);

		if (synLog.isTraceOrDebugEnabled()) {
			synLog.traceOrDebug("Start : Smooks mediator");

			if (synLog.isTraceTraceEnabled()) {
				synLog.traceTrace("Message : " + synCtx.getEnvelope());
			}
		}

		// check weather we need to create the smooks configuration
		lock.lock();
		try {
			if (isCreationOrRecreationRequired(synCtx.getConfiguration())) {
				smooks = createSmooksConfig(synCtx);
			}
		} finally {
			lock.unlock();
		}

		// get the input as an stream
		StreamSource streamSource = input.process(synCtx, synLog);

		// create the execution context for smooks. This is required for every
		// message
		ExecutionContext executionContext = smooks.createExecutionContext();

		try {
			// Start transaction if persistenceUnit name is provided
			if (persistenceUnitName != null) {
				startTransaction(synCtx, executionContext);
			}

			if (output.getType().equals(SmooksMediator.TYPES.JAVA)) {
				// create a JavaResult object to store java result
				JavaResult result = new JavaResult();
				// filter the message through smooks
				smooks.filterSource(executionContext, streamSource, result);
				// add result
				output.process(null, synCtx, synLog, result);
			} else {
				// create an output stream for store the result
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				StreamResult streamResult = new StreamResult(outputStream);
				// filter the message through smooks
				smooks.filterSource(executionContext, streamSource, streamResult);
				// add result
				output.process(outputStream, synCtx, synLog, null);
			}
			if (transactionStarted) {
				commitTransaction();
				transactionStarted = false;
			}
		} catch (AxisFault e) {
			handleException("Error occured while processing smooks output", e);
		} catch (SmooksException e) {
			if (transactionStarted) {
				this.transaction.rollback();
			}
			handleException(e.getMessage(), e);
		}

		if (synLog.isTraceOrDebugEnabled()) {
			synLog.traceOrDebug("End : Smooks mediator");

			if (synLog.isTraceTraceEnabled()) {
				synLog.traceTrace("Message : " + synCtx.getEnvelope());
			}
		}

		return true;
	}

	/**
	 * create the entity manager if not created before and begin transaction
	 * 
	 * @param synCtx
	 * @param executionContext
	 */
	private void startTransaction(MessageContext synCtx, ExecutionContext executionContext) {
		if (emf == null) {
			emf = Persistence.createEntityManagerFactory(persistenceUnitName);
		}
		EntityManager em = emf.createEntityManager();
		PersistenceUtil.setDAORegister(executionContext, new EntityManagerRegister(em));
		this.transaction = em.getTransaction();
		this.transaction.begin();
		this.transactionStarted = true;
	}

	/**
	 * commit transaction
	 */
	private void commitTransaction() {
		this.transaction.commit();
	}

	/**
	 * Create the smoooks configuration from the configuration key. Smooks
	 * configuration can be stored as a local entry or can be stored in the
	 * registry.
	 * 
	 * @param synCtx
	 *            synapse context
	 * @return Smooks configuration
	 */
	private Smooks createSmooksConfig(MessageContext synCtx) {
		SynapseLog log = getLog(synCtx);
		Object o = synCtx.getEntry(configKey);
		if (o == null) {
			handleException("Cannot find the object for smooks config key: " + configKey);
		}

		InputStream in = SynapseConfigUtils.getInputStream(o);
		if (in == null) {
			handleException("Cannot get the input stream from the config key: " + configKey);
		}

		try {
			Smooks smooks = new Smooks(in);
			if (log.isTraceOrDebugEnabled()) {
				log.traceOrDebug("Smooks configuration is created from the config key: "
						+ configKey);
			}
			return smooks;
		} catch (IOException e) {
			handleException("I/O error occurred while creating the Smooks "
					+ "configuration from the config key: " + configKey, e);
		} catch (SAXException e) {
			handleException("XML error occurred while creating the Smooks "
					+ "configuration from the config key: " + configKey, e);
		}

		return null;
	}

	private boolean isCreationOrRecreationRequired(SynapseConfiguration synCfg) {
		// if there are no cachedTemplates we need to create a one
		if (smooks == null) {
			// this is a creation case
			return true;
		} else {
			// build transformer - if necessary
			Entry dp = synCfg.getEntryDefinition(configKey);
			// if the smooks config key refers to a dynamic resource, and if it
			// has been expired
			// it is a recreation case
			boolean shouldRecreate = dp != null && dp.isDynamic()
					&& (!dp.isCached() || dp.isExpired());
			if (shouldRecreate) {
				// we should clear all the existing resources
				smooks.close();
			}
			return shouldRecreate;
		}
	}
	
	private void handleException(String msg) {
        log.error(msg);
        throw new MediatorException(msg);
    }

    private void handleException(String msg, Exception ex) {
        log.error(msg, ex);
        throw new MediatorException(msg + " Caused by " + ex.getMessage());
    }

	public String getConfigKey() {
		return configKey;
	}

	public void setConfigKey(String configKey) {
		this.configKey = configKey;
	}

	public Input getInput() {
		return input;
	}

	public Output getOutput() {
		return output;
	}

	public void setInput(Input input) {
		this.input = input;
	}

	public void setOutput(Output output) {
		this.output = output;
	}

	public void setPersistenceUnitAttr(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	public String getPersistenceUnitName() {
		return persistenceUnitName;
	}
}