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
package org.wso2.carbon.dataservices.core.description.query;

import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.wso2.carbon.dataservices.common.DBConstants.DBSFields;
import org.wso2.carbon.dataservices.common.DBConstants.FaultCodes;
import org.wso2.carbon.dataservices.common.DBConstants.QueryParamTypes;
import org.wso2.carbon.dataservices.common.DBConstants.QueryTypes;
import org.wso2.carbon.dataservices.core.DBUtils;
import org.wso2.carbon.dataservices.core.DataServiceFault;
import org.wso2.carbon.dataservices.core.boxcarring.TLParamStore;
import org.wso2.carbon.dataservices.core.description.event.EventTrigger;
import org.wso2.carbon.dataservices.core.engine.*;
import org.wso2.carbon.dataservices.core.validation.ValidationContext;
import org.wso2.carbon.dataservices.core.validation.ValidationException;
import org.wso2.carbon.dataservices.core.validation.Validator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a query in a data service.
 */
public abstract class Query extends XMLWriterHelper {

	private String queryId;
	
	private List<QueryParam> queryParams;
	
	private DataService dataService;
	
	private Result result;
	
	private boolean writeRow;
	
	private String configId;
	
	private EventTrigger inputEventTrigger;
	
	private EventTrigger outputEventTrigger;
	
	private Map<String, String> advancedProperties;
	
	private String inputNamespace;
	
	/* this is set to true if the result has to be built before sending, 
	 * requires for xslt transformation / eventing */
	private boolean preBuildResult;
	
	private boolean useColumnNumbers;
	
	public Query(DataService dataService, String queryId,
			List<QueryParam> queryParams, Result result, String configId,
			EventTrigger inputEventTrigger, EventTrigger outputEventTrigger, 
			Map<String, String> advancedProperties, String inputNamespace) {
		super(result != null ? result.getNamespace() : dataService.getDefaultNamespace());
		this.dataService = dataService;
		this.queryId = queryId;
		this.queryParams = queryParams;
		this.result = result;
		this.writeRow = this.getResult() != null && this.getResult().getRowName() != null && 
				this.getResult().getRowName().trim().length() > 0;
		this.configId = configId;
		this.inputEventTrigger = inputEventTrigger;
		this.outputEventTrigger = outputEventTrigger;
		this.advancedProperties = advancedProperties;
		this.inputNamespace = inputNamespace;
		this.preBuildResult = this.checkPreBuildResult();
		if (result != null) {
			useColumnNumbers = result.isUseColumnNumbers();
		}
	}
	
	private boolean checkPreBuildResult() {
		return this.getOutputEventTrigger() != null	|| 
		           (this.getResult() != null && 
				    this.getResult().getXsltTransformer() != null);
	}
	
	public String getInputNamespace() {
		return inputNamespace;
	}
	
	public boolean isPreBuildResult() {
		return preBuildResult;
	}
	
	public EventTrigger getInputEventTrigger() {
		return inputEventTrigger;
	}

	public EventTrigger getOutputEventTrigger() {
		return outputEventTrigger;
	}
	
	public Map<String, String> getAdvancedProperties() {
		return advancedProperties;
	}

	public String getConfigId() {
		return configId;
	}
			
	public DataService getDataService() {
		return dataService;
	}

	public String getQueryId() {
		return queryId;
	}
	
	public List<QueryParam> getQueryParams() {
		return queryParams;
	}
	
	public Result getResult() {
		return result;
	}
	
	public boolean hasResult() {
		return this.getResult() != null;
	}
	
	public boolean isWriteRow() {
		return writeRow;
	}
	
	public boolean isUsingColumnNumbers() {
		return useColumnNumbers;
	}

	/**
	 * Converts the parameter map passed into the query, to InternalParam objects,
	 * where they are created by taking in information also that is mentioned in
	 * QueryParams - "param" elements in the "query" element.
	 */
	private InternalParamCollection extractParams(Map<String, ParamValue> params)
			throws DataServiceFault {
		InternalParamCollection ipc = new InternalParamCollection();
		/* exported values from earlier queries */
		Map<String, ParamValue> exportedParams = TLParamStore.getParameterMap();
		ParamValue tmpParamValue;
		for (QueryParam queryParam : this.getQueryParams()) {
			tmpParamValue = params.get(queryParam.getName());			
			if (tmpParamValue == null) {
				if (!(QueryTypes.OUT.equals(queryParam.getType()) || 
						QueryTypes.INOUT.equals(queryParam.getType()))) {
					/* check the exported values */
					tmpParamValue = exportedParams.get(queryParam.getName());
					if (tmpParamValue == null && !queryParam.hasDefaultValue()) {
						/* still can't find, throw an exception */
						throw new DataServiceFault(FaultCodes.INCOMPATIBLE_PARAMETERS_ERROR,
								"Error in 'Query.extractParams', " +
								"cannot find query param with name:" + queryParam.getName());
					}
				}
			}
			for (int ordinal : queryParam.getOrdinals()) {
			    ipc.addParam(new InternalParam(queryParam.getName(), tmpParamValue,
                        queryParam.getSqlType(), queryParam.getType(), queryParam.getStructType(),
                        ordinal));
			}
		}
		return ipc;
	}
	
	/**
	 * Check the params to see if a scalar param needs to be converted 
	 * to an array type which has one element.
	 */
	private void preprocessParams(Map<String, ParamValue> params) {
		ParamValue value = null;
		for (QueryParam queryParam : this.getQueryParams()) {
            if (!queryParam.getType().equals(QueryTypes.OUT)) {
            	/* convert from scalar to array, if required */
                if (queryParam.getParamType().equals(QueryParamTypes.ARRAY)) { 
    				value = params.get(queryParam.getName());
    				if (value != null) {
    					if (value.getValueType() == ParamValue.PARAM_VALUE_SCALAR) {
    						/* replace the existing scalar with the array value */
    						params.put(queryParam.getName(), 
    								ParamValue.convertFromScalarToArray(value));
    					}
    				}
    			}
            }            
        }
	}

	private ValidationContext createValidationContext(Map<String, ParamValue> params) {
		ValidationContext context = new ValidationContext(params);
		return context;
	}
	
	private void validateParams(Map<String, ParamValue> params) throws DataServiceFault {
		try {
			ParamValue value = null;
			ValidationContext context = this.createValidationContext(params);
			for (QueryParam queryParam : this.getQueryParams()) {
				value = params.get(queryParam.getName());
				if (value != null) {
					for (Validator validator : queryParam.getValidators()) {
						validator.validate(context, queryParam.getName(), value);
					}
				}
			}
		} catch (ValidationException e) {
			throw new DataServiceFault(e, FaultCodes.VALIDATION_ERROR, null);
		}
	}

	public void execute(XMLStreamWriter xmlWriter, Map<String, ParamValue> params, 
			int queryLevel) throws DataServiceFault {
		if (this.hasResult()) {
			/* set required roles in result */
			if (DataService.getCurrentUser() != null) {
				this.getResult().applyUserRoles(DataService.getCurrentUser().getUserRoles());
			} else {
				this.getResult().applyUserRoles(null);
			}
		}
		/* pre-process parameters as needed */
		this.preprocessParams(params);
		/* validate params */
		this.validateParams(params);
		/* extract parameters, to be used internally in queries */
		InternalParamCollection internalParams = this.extractParams(params);
		/* process input events */
		this.processInputEvents(internalParams);
		/* write the content */
		this.runQuery(xmlWriter, internalParams, queryLevel);
	}
	
	private OMElement createOMElementFromInputParams(InternalParamCollection params) {
		OMFactory fac = DBUtils.getOMFactory();
		OMDocument doc = fac.createOMDocument();
		OMElement retEl = fac.createOMElement(new QName(this.getQueryId()));
		OMElement scalarEl;
		List<OMElement> arrayEl;
		ParamValue paramValue;
		for (InternalParam param : params.getParams()) {
			paramValue = param.getValue();
			if (paramValue.getValueType() == ParamValue.PARAM_VALUE_SCALAR ||
					paramValue.getValueType() == ParamValue.PARAM_VALUE_UDT) {
				scalarEl = fac.createOMElement(new QName(param.getName()));
				scalarEl.setText(paramValue.getScalarValue());
				retEl.addChild(scalarEl);
			} else if (paramValue.getValueType() == ParamValue.PARAM_VALUE_ARRAY) {
				arrayEl = this.createOMElementsFromArrayValue(param.getName(), paramValue, fac);
				for (OMElement el : arrayEl) {
					retEl.addChild(el);
				}
			}
		}
		doc.addChild(retEl);
		return doc.getOMDocumentElement();
	}
	
	private List<OMElement> createOMElementsFromArrayValue(String name, ParamValue value, 
			OMFactory fac) {
		List<OMElement> arrayEl = new ArrayList<OMElement>();
		List<ParamValue> strVals = value.getArrayValue();
		OMElement el;
		for (ParamValue val : strVals) {
			el = fac.createOMElement(new QName(name));
			el.setText(val.toString());
			arrayEl.add(el);
		}
		return arrayEl;
	}
	
	private void processInputEvents(InternalParamCollection params) throws DataServiceFault {
		EventTrigger inputEventTrigger = this.getInputEventTrigger();
		if (inputEventTrigger != null) {
			OMElement input = this.createOMElementFromInputParams(params);
			inputEventTrigger.execute(input, this.getQueryId());
		}
	}
	
	/**
	 * This method must be implemented by concrete implementations of this class,
	 * to provide the logic to execute the query.
	 */
	public abstract void runQuery(XMLStreamWriter xmlWriter, InternalParamCollection params, 
			int queryLevel) throws DataServiceFault;
	
	/**
	 * writes an result entry to the output.
	 */
	public void writeResultEntry(XMLStreamWriter xmlWriter, DataEntry dataEntry, 
			InternalParamCollection ipc, int queryLevel) throws DataServiceFault {
		/* increment query level */
		queryLevel++;
		
		/* populate params, here an ExternalParamCollection is created from the
		 * passed data and the internal parameters. This is done because, again,
		 * output elements are simply provided with ExternalParam object for their values 
		 * to be outputted. Output elements include, static elements and other call-query
		 * object itself, where call-queries are used for nested queries. */
		ExternalParamCollection params = this.createExternalParamCollection(dataEntry, ipc);
		
		/* write result wrapper */
		if (this.isWriteRow()) {
			try {
		        this.startRowElement(xmlWriter, 
		        		this.getResult().getRowName(), 
		        		this.getResult().getResultType(), this.getResult(), params);
			} catch (XMLStreamException e) {
				throw new DataServiceFault(e, 
						"Error in start write row at Query.writeResultEntry");
			}
		}
		/* write the result */
		this.getResult().getDefaultElementGroup().execute(xmlWriter, params, queryLevel);
		/* end result wrapper */
		if (this.isWriteRow()) {
			try {
		        this.endElement(xmlWriter);
			} catch (XMLStreamException e) {
				throw new DataServiceFault(e, "Error in end write row at Query.writeResultEntry");
			}
		}
	}
	
	private ExternalParamCollection createExternalParamCollection(DataEntry dataEntry, 
			InternalParamCollection queryParams) {
		ExternalParamCollection pc = new ExternalParamCollection();
		/* 'toLowerCase' - workaround for different character case issues in column names */
		for (String name : dataEntry.getNames()) {
			pc.addParam(new org.wso2.carbon.dataservices.core.engine.ExternalParam(
					name.toLowerCase(), dataEntry.getValue(name), DBSFields.COLUMN));
		}
		for (InternalParam iParam : queryParams.getParams()) {
			pc.addParam(new org.wso2.carbon.dataservices.core.engine.ExternalParam(
					iParam.getName().toLowerCase(), iParam.getValue(), DBSFields.QUERY_PARAM));
		}
		return pc;
	}
	
}
