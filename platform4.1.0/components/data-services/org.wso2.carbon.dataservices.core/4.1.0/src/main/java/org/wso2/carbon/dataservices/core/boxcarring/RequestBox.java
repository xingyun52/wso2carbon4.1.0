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
package org.wso2.carbon.dataservices.core.boxcarring;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.commons.io.output.NullOutputStream;
import org.wso2.carbon.dataservices.core.DBUtils;
import org.wso2.carbon.dataservices.core.DataServiceFault;
import org.wso2.carbon.dataservices.core.dispatch.DataServiceRequest;

/**
 * Represents a boxcarring session service request group.
 */
public class RequestBox {
	
	private List<DataServiceRequest> requests;
		
	public RequestBox() {
		requests = new ArrayList<DataServiceRequest>();
	}
	
	public List<DataServiceRequest> getRequests() {
		return requests;
	}
	
	public void addRequest(DataServiceRequest request) {
		this.getRequests().add(request);
	}
	
	public void clear() {
		this.getRequests().clear();
	}
	
	/**
	 * This is called when a boxcarring session is over, 
	 * and the stored requests will be executed,
	 * the result of the last operation is returned.
	 */
	public synchronized OMElement execute() throws DataServiceFault {
		OMElement result;
		List<DataServiceRequest> reqList = this.getRequests();
		int n = reqList.size();
		OMElement resultElement = null;
		for (int i = 0; i < n; i++) {
			result = reqList.get(i).dispatch();
			if (result != null) {
				try {
					/* if it's the last request, return the result,
					 * getXMLStreamReader() method will execute the actual request */
					if (i == (n - 1)) {
						resultElement = DBUtils.cloneAndReturnBuiltElement(result);
						return DBUtils.wrapBoxCarringResponse(resultElement);
					} else {
					    /* process the result of the request, no need to cache the data */
					    result.serializeAndConsume(new NullOutputStream());
					}
				} catch (XMLStreamException e) {
					throw new DataServiceFault(e, "Error in request box result serializing");
				}
			} else {
				if (i == (n - 1)) {
					return DBUtils.wrapBoxCarringResponse(resultElement);
				}
			}
		}
		return null;
	}
	
}
