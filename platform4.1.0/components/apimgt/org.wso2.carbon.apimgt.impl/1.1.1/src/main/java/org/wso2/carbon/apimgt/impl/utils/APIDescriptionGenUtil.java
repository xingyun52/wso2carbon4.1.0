/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.apimgt.impl.utils;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.APIConstants;

public class APIDescriptionGenUtil {
    /**
     * Class Logger
     */
    private static Log log = LogFactory.getLog(APIDescriptionGenUtil.class);

    private static final String DESCRIPTION = "Allows [1] request(s) per minute.";

    public static String generateDescriptionFromPolicy(OMElement policy) throws APIManagementException {
        //Here as the method is about extracting some info from the policy. And it's not concern on compliance to
        // specification. So it just extract the required element.
        OMElement maxCount = null;
        OMElement timeUnit = null;
        int requestPerMinute;
        try {
            maxCount = policy.getFirstChildWithName(APIConstants.POLICY_ELEMENT).getFirstChildWithName
                    (APIConstants
                            .THROTTLE_CONTROL_ELEMENT).getFirstChildWithName(APIConstants.POLICY_ELEMENT).
                    getFirstChildWithName(APIConstants.THROTTLE_MAXIMUM_COUNT_ELEMENT);
            timeUnit = policy.getFirstChildWithName(APIConstants.POLICY_ELEMENT).getFirstChildWithName
                    (APIConstants
                            .THROTTLE_CONTROL_ELEMENT).getFirstChildWithName(APIConstants.POLICY_ELEMENT).
                    getFirstChildWithName(APIConstants.THROTTLE_UNIT_TIME_ELEMENT);
            //Here we will assume time unit provided as milli second and do calculation to get requests per minute.
            if (maxCount.getText().isEmpty() || timeUnit.getText().isEmpty()) {
                String msg = APIConstants.THROTTLE_MAXIMUM_COUNT_ELEMENT.toString() + "or"
                        + APIConstants.THROTTLE_UNIT_TIME_ELEMENT.toString() + " element data found empty in " +
                        "the policy.";
                log.warn(msg);
                throw new APIManagementException(msg);
            }
            requestPerMinute = (Integer.parseInt(maxCount.getText().trim()) * 60000) / (Integer.parseInt(timeUnit.getText().trim()));
            if (requestPerMinute >= 1) {
                String description = DESCRIPTION.replaceAll("\\[1\\]", Integer.toString(requestPerMinute));
                return description;
            }
            return DESCRIPTION;
        } catch (NullPointerException npe) {
            String msg = "Policy could not be parsed correctly based on http://schemas.xmlsoap.org/ws/2004/09/policy " +
                    "specification";
            log.warn(msg);
            throw new APIManagementException(msg);
        }
    }
}