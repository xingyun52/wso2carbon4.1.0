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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.dataservices.common.DBConstants.BoxcarringOps;
import org.wso2.carbon.dataservices.common.DBConstants.DBSFields;
import org.wso2.carbon.dataservices.core.DataServiceFault;
import org.wso2.carbon.dataservices.core.description.query.QueryFactory;
import org.wso2.carbon.dataservices.core.engine.CallQuery;
import org.wso2.carbon.dataservices.core.engine.CallQueryGroup;
import org.wso2.carbon.dataservices.core.engine.DataService;

/**
 * This class creates the Operation objects by passing the 
 * relevant operation sections in the dbs file.
 */
public class OperationFactory {

	private OperationFactory() { }
	
	@SuppressWarnings("unchecked")
	public static Operation createOperation(DataService dataService,
			OMElement opEl) throws DataServiceFault {
		String name = opEl.getAttributeValue(new QName(DBSFields.NAME));
		
		/* get the description */
		OMElement descEl = opEl.getFirstChildWithName(new QName(DBSFields.DESCRIPTION));
		String description = null;
		if (descEl != null) {
			description = descEl.getText();
		}
		
		CallQueryGroup callQueryGroup = null;

		List<CallQueryGroup> cqGroups = QueryFactory.createCallQueryGroups(dataService, 
				opEl.getChildrenWithName(new QName(DBSFields.CALL_QUERY)), 
				opEl.getChildrenWithName(new QName(DBSFields.CALL_QUERY_GROUP)));
		if (cqGroups.size() > 0) {
			callQueryGroup = cqGroups.get(0);
		}
    
		String disableStreamingRequestStr = opEl.getAttributeValue(
				new QName(DBSFields.DISABLE_STREAMING));
		boolean disableStreamingRequest = false;
		if (disableStreamingRequestStr != null) {
			disableStreamingRequest = Boolean.parseBoolean(disableStreamingRequestStr);
		}
		boolean disableStreamingEffective = disableStreamingRequest | dataService.isDisableStreaming();
		
	    /* the last param is 'null' because, this is not a batch operation and 
	     * there is no parent operation */
	    Operation operation = new Operation(dataService, name, description,
	 		callQueryGroup, false, null, disableStreamingRequest, disableStreamingEffective);
	    
	    String returnReqStatusStr = opEl.getAttributeValue(
				new QName(DBSFields.RETURN_REQUEST_STATUS));
		boolean returnReqStatus = false;
		if (returnReqStatusStr != null) {
			returnReqStatus = Boolean.parseBoolean(returnReqStatusStr);
		}
		operation.setReturnRequestStatus(returnReqStatus);
		
	    return operation;
	}
	
	public static Operation createBeginBoxcarOperation(DataService dataService) {
		List<CallQuery> callQueries = new ArrayList<CallQuery>();
		callQueries.add(QueryFactory.createEmptyCallQuery(dataService));
		CallQueryGroup callQueryGroup = new CallQueryGroup(callQueries);
		Operation operation = new Operation(dataService, BoxcarringOps.BEGIN_BOXCAR, 
				"Control operation for beginning a boxcarring session",
				callQueryGroup, false, null, false, false);
		return operation;
	}
	
	public static Operation createEndBoxcarOperation(DataService dataService) {
		List<CallQuery> callQueries = new ArrayList<CallQuery>();
		callQueries.add(QueryFactory.createEmptyEndBoxcarCallQuery(dataService));
		CallQueryGroup callQueryGroup = new CallQueryGroup(callQueries);
		Operation operation = new Operation(dataService, BoxcarringOps.END_BOXCAR, 
				"Control operation for ending a boxcarring session",
				callQueryGroup, false, null, false, false);
		return operation;
	}
	
	public static Operation createAbortBoxcarOperation(DataService dataService) {
		List<CallQuery> callQueries = new ArrayList<CallQuery>();
		callQueries.add(QueryFactory.createEmptyCallQuery(dataService));
		CallQueryGroup callQueryGroup = new CallQueryGroup(callQueries);
		Operation operation = new Operation(dataService, BoxcarringOps.ABORT_BOXCAR, 
				"Control operation for aborting a boxcarring session",
				callQueryGroup, false, null, false, false);
		return operation;
	}
	
}
