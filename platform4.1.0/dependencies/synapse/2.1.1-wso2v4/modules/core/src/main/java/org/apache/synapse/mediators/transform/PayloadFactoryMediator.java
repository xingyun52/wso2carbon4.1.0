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

package org.apache.synapse.mediators.transform;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.AXIOMUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PayloadFactoryMediator extends AbstractMediator {

    private String format;
    private Value formatKey = null;
    private boolean isFormatDynamic = false;
    private List<Argument> argumentList = new ArrayList<Argument>();

    private Pattern pattern = Pattern.compile("\\$(\\d)+");

    public boolean mediate(MessageContext synCtx) {

        SOAPBody soapBody = synCtx.getEnvelope().getBody();

        StringBuffer result = new StringBuffer();
        regexTransform(result, synCtx);

        OMElement resultElement;
        try {
            resultElement = AXIOMUtil.stringToOM(result.toString());
        } catch (XMLStreamException e) {      /*Use the XMLStreamException and log the proper stack trace*/
            handleException("Unable to create a valid XML payload", synCtx);
            return false;
        }

        for (Iterator itr = soapBody.getChildElements(); itr.hasNext(); ) {
            OMElement child = (OMElement) itr.next();
            child.detach();
        }

        QName resultQName = resultElement.getFirstElement().getQName();


        if (resultQName.getLocalPart().equals("Envelope") && (
                resultQName.getNamespaceURI().equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI) ||
                        resultQName.getNamespaceURI().
                                equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI))) {
            SOAPEnvelope soapEnvelope = AXIOMUtils.getSOAPEnvFromOM(resultElement.getFirstElement());
            if (soapEnvelope != null) {
                try {
                    synCtx.setEnvelope(soapEnvelope);
                } catch (AxisFault axisFault) {
                    handleException("Unable to attach SOAPEnvelope", axisFault, synCtx);
                }
            }
        } else {
            for (Iterator itr = resultElement.getChildElements(); itr.hasNext(); ) {
                OMElement child = (OMElement) itr.next();
                soapBody.addChild(child);
            }
        }

        return true;
    }

    /* ToDO : Return string buffer*/
    private void regexTransform(StringBuffer result, MessageContext synCtx) {
        if (isFormatDynamic()) {
            String key = formatKey.evaluateValue(synCtx);

            OMElement element = (OMElement) synCtx.getEntry(key);
            removeIndentations(element);
            String format2 = element.toString();
            replace(format2, result, synCtx);
        } else {
            replace(format, result, synCtx);

        }
    }

    /**
     * repplace the message with format
     *
     * @param format
     * @param result
     * @param synCtx
     */
    private void replace(String format, StringBuffer result, MessageContext synCtx) {
        Object[] argValues = getArgValues(synCtx);
        Matcher matcher = pattern.matcher("<dummy>" + format + "</dummy>");
        while (matcher.find()) {
            String matchSeq = matcher.group();
            int argIndex = Integer.parseInt(matchSeq.substring(1));
            matcher.appendReplacement(result, argValues[argIndex - 1].toString());
        }
        matcher.appendTail(result);
    }

    private void removeIndentations(OMElement element) {
        List<OMText> removables = new ArrayList<OMText>();
        removeIndentations(element, removables);
        for (OMText node : removables) {
            node.detach();
        }
    }

    private void removeIndentations(OMElement element, List<OMText> removables) {
        Iterator children = element.getChildren();
        while (children.hasNext()) {
            Object next = children.next();
            if (next instanceof OMText) {
                OMText text = (OMText) next;
                if (text.getText().trim().equals("")) {
                    removables.add(text);
                }
            } else if (next instanceof OMElement) {
                removeIndentations((OMElement) next, removables);
            }
        }
    }

    private Object[] getArgValues(MessageContext synCtx) {

        Object[] argValues = new Object[argumentList.size()];
        for (int i = 0; i < argumentList.size(); ++i) {       /*ToDo use foreach*/
            Argument arg = argumentList.get(i);
            if (arg.getValue() != null) {
                String value = arg.getValue();
                if (!isXML(value)) {
                    value = StringEscapeUtils.escapeXml(value);
                }
                value = Matcher.quoteReplacement(value);
                argValues[i] = value;
            } else if (arg.getExpression() != null) {
                String value = arg.getExpression().stringValueOf(synCtx);   /*ToDo We can change this to string array*/
                if (value != null) {
                    //escaping string unless there might be exceptions when tries to insert values
                    // such as string with & (XML special char) and $ (regex special char)
                    if (!isXML(value)) {
                        value = StringEscapeUtils.escapeXml(value);
                    }
                    value = Matcher.quoteReplacement(value);
                    argValues[i] = value;
                } else {
                    argValues[i] = "";
                }
            } else {
                handleException("Unexpected arg type detected", synCtx);
            }
        }
        return argValues;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void addArgument(Argument arg) {
        argumentList.add(arg);
    }

    public List<Argument> getArgumentList() {
        return argumentList;
    }

    private boolean isXML(String value) {
        try {
            AXIOMUtil.stringToOM(value);
        } catch (XMLStreamException ignore) {
            // means not a xml
            return false;
        } catch (OMException ignore) {
            // means not a xml
            return false;
        }
        return true;
    }

    /**
     * To get the key which is used to pick the format definition from the local registry
     *
     * @return return the key which is used to pick the format definition from the local registry
     */
    public Value getFormatKey() {
        return formatKey;
    }

    /**
     * To set the local registry key in order to pick the format definition
     *
     * @param key the local registry key
     */
    public void setFormatKey(Value key) {
        this.formatKey = key;
    }

    public void setFormatDynamic(boolean formatDynamic) {
        this.isFormatDynamic = formatDynamic;
    }

    public boolean isFormatDynamic() {
        return isFormatDynamic;
    }


}
