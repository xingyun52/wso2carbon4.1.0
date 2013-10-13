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

import java.util.HashMap;
import java.util.Map;

import org.wso2.carbon.dataservices.core.engine.ParamValue;

/**
 * This class represents a thread local storage for parameters,
 * results from queries etc..
 */
public class TLParamStore {

	private static ThreadLocal<Map<String, ParamValue>> tlParams = new ThreadLocal<Map<String, ParamValue>>() {
		@Override
		protected synchronized Map<String, ParamValue> initialValue() {
			return new HashMap<String, ParamValue>();
		}
	};
	
	public static void addParam(String name, ParamValue value) {
		tlParams.get().put(name, value);
	}
	
	public static ParamValue getParam(String name) {
		return tlParams.get().get(name);
	}
	
	public static Map<String, ParamValue> getParameterMap() {
		return tlParams.get();
	}
	
	public static void clear() {
		tlParams.remove();
	}
	
}
