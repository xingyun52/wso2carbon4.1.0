/*
 *  Copyright WSO2 Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.apimgt.impl.template;

import org.apache.axiom.om.OMElement;

public interface APITemplateBuilder {

    public static final String KEY_FOR_API_NAME = "key_for_api_name";
    public static final String KEY_FOR_API_CONTEXT = "key_for_api_context";
    public static final String KEY_FOR_API_VERSION = "key_for_api_version";

    public static final String KEY_FOR_RESOURCE_URI_TEMPLATE = "key_for_resource_uri_template";
    public static final String KEY_FOR_RESOURCE_METHODS = "key_for_resource_methods";
    public static final String KEY_FOR_RESOURCE_URI = "key_for_resource_uri";
    public static final String KEY_FOR_RESOURCE_SANDBOX_URI = "key_for_resource_sandbox_uri";

    public static final String KEY_FOR_HANDLER = "key_for_handler_class";
    public static final String KEY_FOR_HANDLER_POLICY_KEY = "key_for_handler_policy";
    public static final String KEY_FOR_ENDPOINT_SECURED = "key_for_endpoint_secured";
    public static final String KEY_FOR_ENDPOINT_USERNAME = "key_for_endpoint_username";
    public static final String KEY_FOR_ENDPOINT_PASSWORD = "key_for_endpoint_password";
    
    public String getConfigStringForTemplate() throws APITemplateException;

    public OMElement getConfigXMLForTemplate() throws APITemplateException;

}
