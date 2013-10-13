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
package org.wso2.carbon.business.messaging.hl7.message;

import org.apache.axis2.builder.Builder;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.AxisFault;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.business.messaging.hl7.common.HL7ProcessingContext;
import org.wso2.carbon.business.messaging.hl7.common.HL7Utils;

import java.io.InputStream;

import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import ca.uhn.hl7v2.validation.impl.NoValidation;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;

public class HL7MessageBuilder implements Builder {
	
    private static final Log log = LogFactory.getLog(HL7MessageBuilder.class);

    /** {@inheritdoc }**/
    public OMElement processDocument(InputStream inputStream, String contentType, 
    		MessageContext msgCtx) throws AxisFault {
        try {
        	HL7ProcessingContext processingCtx = new HL7ProcessingContext(msgCtx.getAxisService());
        	String hl7String = HL7Utils.streamToString(inputStream);
            if (log.isDebugEnabled()) {
                log.debug("HL7 String: " + hl7String);
            }
            Message message = processingCtx.parseMessage(hl7String);
            processingCtx.initMessageContext(message, msgCtx);
            processingCtx.checkConformanceProfile(message);
            String hl7Xml = serializeHL7toXML(message);
            return HL7Utils.generateHL7MessageElement(hl7Xml);
        } catch (AxisFault e) {
			throw e;
		} catch (Exception e) {
			throw new AxisFault(e.getMessage(), e);
		}
    }

    private String serializeHL7toXML(Message message) throws AxisFault {
        Parser xmlParser = new DefaultXMLParser();
        xmlParser.setValidationContext(new NoValidation());
        try {
            return xmlParser.encode(message);
        } catch (HL7Exception e) {
            throw new AxisFault("Error on converting to HL7 XML: " + e.getMessage(), e);
        }
    }

}
