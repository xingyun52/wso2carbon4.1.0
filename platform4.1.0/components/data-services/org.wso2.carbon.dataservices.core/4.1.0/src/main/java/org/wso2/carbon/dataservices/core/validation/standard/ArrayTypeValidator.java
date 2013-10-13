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
package org.wso2.carbon.dataservices.core.validation.standard;

import org.wso2.carbon.dataservices.core.engine.ParamValue;
import org.wso2.carbon.dataservices.core.validation.ValidationContext;
import org.wso2.carbon.dataservices.core.validation.ValidationException;
import org.wso2.carbon.dataservices.core.validation.Validator;

/**
 * Represents a validator to check if the given value is an array.
 */
public class ArrayTypeValidator implements Validator {

	private static ArrayTypeValidator instance;
	
	private ArrayTypeValidator() { }
	
	public static ArrayTypeValidator getInstance() {
		if (instance == null) {
			instance = new ArrayTypeValidator();
		}
		return instance;		
	}
	
	public void validate(ValidationContext context, String name, ParamValue value) throws ValidationException {
		if (value.getValueType() != ParamValue.PARAM_VALUE_ARRAY) {
			throw new ValidationException("Array type expected", name, value);
		}
	}

}
