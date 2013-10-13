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
package org.wso2.carbon.mediator.entitlement;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.wso2.carbon.mediator.service.MediatorException;
import org.wso2.carbon.mediator.service.ui.AbstractListMediator;
import org.wso2.carbon.mediator.service.ui.Mediator;

import javax.xml.namespace.QName;

public class EntitlementMediator extends AbstractListMediator {
    private String remoteServiceUserName;
    private String remoteServicePassword;
    private String remoteServiceUrl;
    private static final QName PROP_NAME_SERVICE_EPR = new QName("remoteServiceUrl");
    private static final QName PROP_NAME_USER = new QName("remoteServiceUserName");
    private static final QName PROP_NAME_PASSWORD = new QName("remoteServicePassword");
    private static final String ADVICE = "advice";
    private static final String OBLIGATIONS = "obligations";
    private String onRejectSeqKey = null;
    private String onAcceptSeqKey = null;
    private String adviceSeqKey = null;
    private String obligationsSeqKey = null;

    public EntitlementMediator() {
        addChild(new OnAcceptMediator());
        addChild(new OnRejectMediator());
        addChild(new ObligationsMediator());
        addChild(new AdviceMediator());
    }

    /**
     * {@inheritDoc}
     */
    public OMElement serialize(OMElement parent) {
        OMElement entitlementService = fac.createOMElement("entitlementService", synNS);

        if (remoteServiceUrl != null) {
            entitlementService.addAttribute(fac.createOMAttribute("remoteServiceUrl", nullNS,
                    remoteServiceUrl));
        } else {
            throw new MediatorException(
                    "Invalid Entitlement mediator.Entitlement service epr required");
        }

        if (remoteServiceUserName != null) {
            entitlementService.addAttribute(fac.createOMAttribute("remoteServiceUserName", nullNS,
                    remoteServiceUserName));
        } else {
            throw new MediatorException(
                    "Invalid Entitlement mediator. Remote service user name required");
        }

        if (remoteServicePassword != null) {
            entitlementService.addAttribute(fac.createOMAttribute("remoteServicePassword", nullNS,
                    remoteServicePassword));
        } else {
            throw new MediatorException(
                    "Invalid Entitlement mediator. Remote service password required");
        }

        if (onRejectSeqKey != null) {
            entitlementService.addAttribute(fac.createOMAttribute(XMLConfigConstants.ONREJECT, nullNS,
                    onRejectSeqKey));
        } else {
            for (Mediator m : getList()) {
                if (m instanceof OnRejectMediator) {
                    m.serialize(entitlementService);
                }
            }
        }

        if (onAcceptSeqKey != null) {
            entitlementService.addAttribute(fac.createOMAttribute(XMLConfigConstants.ONACCEPT, nullNS,
                    onAcceptSeqKey));
        } else {
            for (Mediator m : getList())  {
                if (m instanceof OnAcceptMediator) {
                    m.serialize(entitlementService);
                }
            }
        }

        if (adviceSeqKey != null) {
            entitlementService.addAttribute(fac.createOMAttribute(ADVICE, nullNS,
                    adviceSeqKey));
        } else {
            for (Mediator m : getList())  {
                if (m instanceof AdviceMediator) {
                    m.serialize(entitlementService);
                }
            }
        }

        if (obligationsSeqKey != null) {
            entitlementService.addAttribute(fac.createOMAttribute(OBLIGATIONS, nullNS,
                    obligationsSeqKey));
        } else {
            for (Mediator m : getList())  {
                if (m instanceof ObligationsMediator) {
                    m.serialize(entitlementService);
                }
            }
        }

        saveTracingState(entitlementService, this);

        if (parent != null) {
            parent.addChild(entitlementService);
        }
        return entitlementService;
    }

    /**
     * {@inheritDoc}
     */
    public void build(OMElement elem) {
        getList().clear();
        OMAttribute attRemoteServiceUri = elem.getAttribute(PROP_NAME_SERVICE_EPR);
        OMAttribute attRemoteServiceUserName = elem.getAttribute(PROP_NAME_USER);
        OMAttribute attRemoteServicePassword = elem.getAttribute(PROP_NAME_PASSWORD);
        this.onAcceptSeqKey = null;
        this.onRejectSeqKey = null;
        this.adviceSeqKey = null;
        this.obligationsSeqKey = null;

        if (attRemoteServiceUri != null) {
            remoteServiceUrl = attRemoteServiceUri.getAttributeValue();
        } else {
            throw new MediatorException(
                    "The 'remoteServiceUrl' attribute is required for the Entitlement mediator");
        }

        if (attRemoteServiceUserName != null) {
            remoteServiceUserName = attRemoteServiceUserName.getAttributeValue();
        } else {
            throw new MediatorException(
                    "The 'remoteServiceUserName' attribute is required for the Entitlement mediator");
        }

        if (attRemoteServicePassword != null) {
            remoteServicePassword = attRemoteServicePassword.getAttributeValue();
        } else {
            throw new MediatorException(
                    "The 'remoteServicePassword' attribute is required for the Entitlement mediator");
        }

        OMAttribute onReject = elem.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.ONREJECT));
        if (onReject != null) {
            String onRejectValue = onReject.getAttributeValue();
            if (onRejectValue != null) {
                onRejectSeqKey = onRejectValue.trim();
            }
        } else {
            OMElement onRejectMediatorElement = elem.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, XMLConfigConstants.ONREJECT));
            if (onRejectMediatorElement != null) {
                OnRejectMediator onRejectMediator = new OnRejectMediator();
                onRejectMediator.build(onRejectMediatorElement);
                addChild(onRejectMediator);
            }
        }
        OMAttribute onAccept = elem.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.ONACCEPT));
        if (onAccept != null) {
            String onAcceptValue = onAccept.getAttributeValue();
            if (onAcceptValue != null) {
                onAcceptSeqKey = onAcceptValue;
            }
        } else {
            OMElement onAcceptMediatorElement = elem.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, XMLConfigConstants.ONACCEPT));
            if (onAcceptMediatorElement != null) {
                OnAcceptMediator onAcceptMediator = new OnAcceptMediator();
                onAcceptMediator.build(onAcceptMediatorElement);
                addChild(onAcceptMediator);
            }
        }
        OMAttribute advice = elem.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, ADVICE));
        if (advice != null) {
            String adviceValue = advice.getAttributeValue();
            if (adviceValue != null) {
                adviceSeqKey = adviceValue;
            }
        } else {
            OMElement adviceMediatorElement = elem.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, ADVICE));
            if (adviceMediatorElement != null) {
                AdviceMediator adviceMediator = new AdviceMediator();
                adviceMediator.build(adviceMediatorElement);
                addChild(adviceMediator);
            }
        }
        OMAttribute obligations = elem.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, OBLIGATIONS));
        if (obligations != null) {
            String obligationsValue = obligations.getAttributeValue();
            if (obligationsValue != null) {
                onAcceptSeqKey = obligationsValue;
            }
        } else {
            OMElement obligationsMediatorElement = elem.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, OBLIGATIONS));
            if (obligationsMediatorElement != null) {
                ObligationsMediator obligationsMediator = new ObligationsMediator();
                obligationsMediator.build(obligationsMediatorElement);
                addChild(obligationsMediator);
            }
        }
    }

    public String getRemoteServiceUserName() {
        return remoteServiceUserName;
    }

    public void setRemoteServiceUserName(String remoteServiceUserName) {
        this.remoteServiceUserName = remoteServiceUserName;
    }

    public String getRemoteServicePassword() {
        return remoteServicePassword;
    }

    public void setRemoteServicePassword(String remoteServicePassword) {
        this.remoteServicePassword = remoteServicePassword;
    }

    public String getRemoteServiceUrl() {
        return remoteServiceUrl;
    }

    public void setRemoteServiceUrl(String remoteServiceUrl) {
        this.remoteServiceUrl = remoteServiceUrl;
    }

    public String getOnRejectSeqKey() {
        return onRejectSeqKey;
    }

    public void setOnRejectSeqKey(String onRejectSeqKey) {
        this.onRejectSeqKey = onRejectSeqKey;
    }

    public String getOnAcceptSeqKey() {
        return onAcceptSeqKey;
    }

    public void setOnAcceptSeqKey(String onAcceptSeqKey) {
        this.onAcceptSeqKey = onAcceptSeqKey;
    }

    public String getAdviceSeqKey() {
        return adviceSeqKey;
    }

    public void setAdviceSeqKey(String adviceSeqKey) {
        this.adviceSeqKey = adviceSeqKey;
    }

    public String getObligationsSeqKey() {
        return obligationsSeqKey;
    }

    public void setObligationsSeqKey(String obligationsSeqKey) {
        this.obligationsSeqKey = obligationsSeqKey;
    }

    /**
     * {@inheritDoc}
     */
    public String getTagLocalName() {
        return "entitlementService";
    }
}
