/*
 * Copyright 2004,2005 The Apache Software Foundation.
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
package org.wso2.carbon.bam.service.data.publisher.modules;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.bam.data.publisher.util.BAMDataPublisherConstants;
import org.wso2.carbon.bam.data.publisher.util.PublisherUtil;
import org.wso2.carbon.bam.service.data.publisher.conf.EventConfigNStreamDef;
import org.wso2.carbon.bam.service.data.publisher.data.BAMServerInfo;
import org.wso2.carbon.bam.service.data.publisher.data.Event;
import org.wso2.carbon.bam.service.data.publisher.data.EventData;
import org.wso2.carbon.bam.service.data.publisher.data.PublishData;
import org.wso2.carbon.bam.service.data.publisher.publish.EventPublisher;
import org.wso2.carbon.bam.service.data.publisher.publish.ServiceAgentUtil;
import org.wso2.carbon.bam.service.data.publisher.util.ActivityPublisherConstants;
import org.wso2.carbon.bam.service.data.publisher.util.TenantEventConfigData;
import org.wso2.carbon.core.util.SystemFilter;
import org.wso2.carbon.statistics.StatisticsConstants;

import javax.xml.namespace.QName;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;


public class ActivityInHandler extends AbstractHandler {

    private static Log log = LogFactory.getLog(ActivityInHandler.class);


    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {

        int tenantID = PublisherUtil.getTenantId(messageContext);

        Map<Integer, EventConfigNStreamDef> tenantSpecificEventConfig = TenantEventConfigData.getTenantSpecificEventingConfigData();
        EventConfigNStreamDef eventingConfigData = tenantSpecificEventConfig.get(tenantID);

        if (eventingConfigData != null && eventingConfigData.isMsgDumpingEnable()) {
            Timestamp timestamp;
            AxisService service = messageContext.getAxisService();

            if (service == null || SystemFilter.isFilteredOutService(service.getAxisServiceGroup()) || service.isClientSide()) {
                return InvocationResponse.CONTINUE;
            } else {
                SOAPFactory soapFactory = null;
                SOAPHeaderBlock soapHeaderBlock = null;
                SOAPEnvelope soapEnvelope = messageContext.getEnvelope();
                String soapNamespaceURI = soapEnvelope.getNamespace().getNamespaceURI();
                String activityUUID = getUniqueId();

                if (messageContext.getMessageID() == null) {
                    messageContext.setMessageID(getUniqueId());
                }

                if (soapNamespaceURI.equals(SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
                    soapFactory = OMAbstractFactory.getSOAP11Factory();
                } else if (soapNamespaceURI.equals(SOAP12Constants.SOAP_ENVELOPE_NAMESPACE_URI)) {
                    soapFactory = OMAbstractFactory.getSOAP12Factory();
                } else {
                    log.error("Not a standard soap message");
                }

                if (soapEnvelope.getHeader() != null) {
                    Iterator itr = soapEnvelope.getHeader().getChildrenWithName(new QName(
                            ActivityPublisherConstants.BAM_ACTIVITY_ID_HEADER_NAMESPACE_URI,
                            ActivityPublisherConstants.ACTIVITY_ID_HEADER_BLOCK_NAME));
                    //Go through the header and see whether the AID is present or not. If not add.
                    if (!itr.hasNext()) {
                        OMFactory fac = OMAbstractFactory.getOMFactory();
                        OMNamespace omNs = fac.createOMNamespace(
                                ActivityPublisherConstants.BAM_ACTIVITY_ID_HEADER_NAMESPACE_URI, "ns");
                        soapHeaderBlock = soapEnvelope.getHeader().addHeaderBlock(
                                ActivityPublisherConstants.ACTIVITY_ID_HEADER_BLOCK_NAME, omNs);
                        soapHeaderBlock.addAttribute(ActivityPublisherConstants.ACTIVITY_ID, activityUUID, null);
                    } else {
                        OMElement element = (OMElement) itr.next();
                        String aid = element.getAttributeValue(new QName(ActivityPublisherConstants.ACTIVITY_ID));
                        if (aid != null) {
                            if (aid.equals("")) {
                                element.addAttribute(ActivityPublisherConstants.ACTIVITY_ID, activityUUID, null);
                            }
                        } else {
                            element.addAttribute(ActivityPublisherConstants.ACTIVITY_ID, activityUUID, null);
                        }
                    }
                } else {
                    if (soapFactory != null) {
                        soapFactory.createSOAPHeader(soapEnvelope);
                        OMFactory fac = OMAbstractFactory.getOMFactory();
                        OMNamespace omNs = fac.createOMNamespace(
                                ActivityPublisherConstants.BAM_ACTIVITY_ID_HEADER_NAMESPACE_URI, "ns");
                        soapHeaderBlock = soapEnvelope.getHeader().addHeaderBlock(
                                ActivityPublisherConstants.ACTIVITY_ID_HEADER_BLOCK_NAME, omNs);
                        soapHeaderBlock.addAttribute(ActivityPublisherConstants.ACTIVITY_ID,
                                                     activityUUID, null);
                    }
                }

                MessageContext inMessageContext = messageContext.getOperationContext().getMessageContext(
                        WSDL2Constants.MESSAGE_LABEL_IN);
                EventData eventData = new EventData();
                if (inMessageContext != null) {
                    //Get timestamp value set from system-statistics module
                    timestamp = new Timestamp(Long.parseLong(inMessageContext.getProperty(
                            StatisticsConstants.REQUEST_RECEIVED_TIME).toString()));
                    Object requestProperty = inMessageContext.getProperty(
                            HTTPConstants.MC_HTTP_SERVLETREQUEST);
                    ServiceAgentUtil.extractInfoFromHttpHeaders(eventData, requestProperty);
                } else {
                    Date currentDate = new Date();
                    timestamp = new Timestamp(currentDate.getTime());
                }

                addDetailsOfTheMessage(eventData, timestamp, activityUUID, messageContext);
                BAMServerInfo bamServerInfo = ServiceAgentUtil.addBAMServerInfo(eventingConfigData);

                PublishData publishData = new PublishData();
                publishData.setEventData(eventData);
                publishData.setBamServerInfo(bamServerInfo);

                if (!isInOnlyMEP(messageContext)) {
                    inMessageContext.setProperty(BAMDataPublisherConstants.ACTIVITY_PUBLISH_DATA, publishData);
                }

                Event event = ServiceAgentUtil.makeEventList(publishData, eventingConfigData);
                EventPublisher publisher = new EventPublisher();
                publisher.publish(event, eventingConfigData);

            }
        }
        return InvocationResponse.CONTINUE;
    }


    private EventData addDetailsOfTheMessage(EventData eventData, Timestamp timestamp,
                                             String randomUUID,
                                             MessageContext messageContext) {
        eventData.setActivityId(randomUUID);
        eventData.setTimestamp(timestamp);
        SOAPEnvelope envelope = messageContext.getEnvelope();

        String soapBody = null;
        String soapHeader = null;
        try {
            soapHeader = envelope.getHeader().toString();
            soapBody = envelope.getBody().toString();
        } catch (OMException e) {
            log.warn("Exception occurred while getting soap envelop", e);
        }

        eventData.setMessageDirection(BAMDataPublisherConstants.IN_DIRECTION);
        eventData.setSOAPHeader(soapHeader);
        eventData.setSOAPBody(soapBody);
        //eventData.setMessageDirection(ActivityPublisherConstants.ACTIVITY_DATA_MESSAGE_DIRECTION_IN);
        eventData.setServiceName(messageContext.getAxisService().getName());
        eventData.setOperationName(messageContext.getAxisOperation().getName().getLocalPart());
        eventData.setMessageId(messageContext.getMessageID());

        return eventData;

    }

    private boolean isInOnlyMEP(MessageContext messageContext) {
        String mep = messageContext.getOperationContext().getAxisOperation().getMessageExchangePattern();

        if (mep.equals(WSDL2Constants.MEP_URI_IN_ONLY) ||
            mep.equals(WSDL2Constants.MEP_URI_IN_OPTIONAL_OUT) ||
            mep.equals(WSDL2Constants.MEP_URI_ROBUST_IN_ONLY)) {

            return true;
        }

        return false;

    }

    public String getUniqueId() {
        return System.nanoTime() + "_" + Thread.currentThread().getId();
    }
}
