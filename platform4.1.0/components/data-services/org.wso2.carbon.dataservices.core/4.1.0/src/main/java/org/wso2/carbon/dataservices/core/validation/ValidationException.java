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
package org.wso2.carbon.dataservices.core.validation;

import org.wso2.carbon.dataservices.core.engine.ParamValue;

/**
 * This class represents a validation failure exception.
 */
public class ValidationException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String fieldName;
	
	private ParamValue fieldValue;
	
	private String message;
		
	public ValidationException(String message, String fieldName, ParamValue fieldValue) {
		this.fieldName = fieldName;
		this.fieldValue = fieldValue;
		this.message = message;
	}
	
	public ParamValue getFieldValue() {
		return fieldValue;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
	public String getValidationErrorMessage() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.message);
		if (this.getFieldName() != null) {
			builder.append("\nField Name: " + this.getFieldName());
		}
		if (this.getFieldValue() != null) {
			builder.append("\nField Value: " + this.getFieldValue().getValueAsString());
		}
		return builder.toString();
	}
		
	@Override
	public String toString() {
		return this.getValidationErrorMessage();
	}
	
}

