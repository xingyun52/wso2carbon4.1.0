/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.governance.list.util.filter;

import org.wso2.carbon.registry.core.Registry;

public class FilterFactory {

    public enum FilterTypes {
        SERVICE, WSDL, SCHEMA, POLICY
    }

    public static FilterStrategy createFilter(String type, String criteria,
                                              Registry governanceRegistry) throws Exception {
        FilterTypes types = FilterTypes.valueOf(type.toUpperCase());
        switch (types) {
            case SERVICE:
                return new FilterService(criteria, governanceRegistry);
            case SCHEMA:
                return new FilterSchema(criteria, governanceRegistry);
            case WSDL:
                return new FilterWSDL(criteria, governanceRegistry);
            case POLICY:
                return new FilterPolicy(criteria, governanceRegistry);
            default:
                throw new Exception("Unsupported filter strategy");
        }
    }

}
