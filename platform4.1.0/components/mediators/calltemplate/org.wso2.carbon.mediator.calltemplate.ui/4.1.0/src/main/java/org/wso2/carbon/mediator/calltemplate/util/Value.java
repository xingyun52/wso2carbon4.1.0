/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.mediator.calltemplate.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a Value
 * Handling both static and dynamic(Xpath) keys.
 * User can give Xpath expression as a key and derive
 * real key based on message context
 */
public class Value {
    private static final Log log = LogFactory.getLog(org.apache.synapse.mediators.Value.class);

    /**
     * Name of the value attribute
     */
    private String name = null;

    /**
     * The static key value 
     */
    private String keyValue = null;
    /**
     * the dynamic key
     */
    private SynapseXPath expression = null;

    private List<OMNamespace> namespaceList = new ArrayList<OMNamespace>();
    /**
     * Create a key instance using a static key
     *
     * @param staticKey static key
     */
    public Value(String staticKey) {
        this.keyValue = staticKey;
    }

    /**
     * Create a key instance using a dynamic key (Xpath Expression)
     *
     * @param expression SynapseXpath for dynamic key
     */
    public Value(SynapseXPath expression) {
        this.expression = expression;
    }

    /**
     * Retrieving static key
     *
     * @return static key
     */
    public String getKeyValue() {
        return keyValue;
    }

    /**
     * Retrieving dynamic key
     *
     * @return SynapseXpath
     */
    public SynapseXPath getExpression() {
        if(expression == null && keyValue != null && hasExprTypeKey()){
            try {
                expression = new SynapseXPath(keyValue.substring(1, keyValue.length()-1));
                for (OMNamespace aNamespaceList : namespaceList) {
                    expression.addNamespace(aNamespaceList);
                }
            } catch (JaxenException e) {
                expression = null;
                handleException("Can not evaluate escaped expression..");

            }
        }
        return expression;
    }


    /**
     * Handle exceptions
     *
     * @param msg error message
     */
    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }


    /**
     * checks whether key returned by #getKeyValue() is a string of an expression type.
     * @return if true if this is an expression
     */
    public boolean hasExprTypeKey() {
        return keyValue != null && keyValue.startsWith("{") && keyValue.endsWith("}");
    }

    public void setNamespaces(OMElement elem){
        Iterator namespaces = elem.getAllDeclaredNamespaces();
        while (namespaces.hasNext()){
            OMNamespace ns = (OMNamespace) namespaces.next();
            namespaceList.add(ns);
        }
    }

    public void addNamespace(OMNamespace ns){
        if (ns != null){
            namespaceList.add(ns);
        }
    }

    @Override
    public String toString() {
        return "Value {" +
                "name ='" + name + '\'' +
                (keyValue != null ? ", keyValue ='" + keyValue + '\'' : "") +
                (expression != null ? ", expression =" + expression : "") +
                '}';
    }

}


