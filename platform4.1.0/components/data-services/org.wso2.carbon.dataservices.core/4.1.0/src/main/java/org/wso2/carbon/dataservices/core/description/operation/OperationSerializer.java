/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.dataservices.core.description.operation;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.wso2.carbon.dataservices.common.DBConstants.DBSFields;
import org.wso2.carbon.dataservices.core.DBUtils;
import org.wso2.carbon.dataservices.core.description.query.QuerySerializer;

/**
 * This class represents the serializing functionality of an Operation.
 * @see Operation
 */
public class OperationSerializer {

	public static OMElement serializeOperation(Operation operation) {
		OMFactory fac = DBUtils.getOMFactory();
		OMElement opEl = fac.createOMElement(new QName(DBSFields.OPERATION));
		opEl.addAttribute(DBSFields.NAME, operation.getName(), null);
		String description = operation.getDescription();
		if (!DBUtils.isEmptyString(description)) {
			OMElement desEl = fac.createOMElement(new QName(DBSFields.DESCRIPTION));
			desEl.setText(description);
			opEl.addChild(desEl);
		}
		QuerySerializer.serializeCallQueryGroup(operation.getCallQueryGroup(), opEl, fac);
		return opEl;
	}
	
}
