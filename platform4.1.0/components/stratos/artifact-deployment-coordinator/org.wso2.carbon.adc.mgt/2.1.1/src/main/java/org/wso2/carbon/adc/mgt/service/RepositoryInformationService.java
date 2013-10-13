/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.mgt.utils.PersistenceManager;

/**
 * 
 * Exposes information related to internally created repositories  
 * 
 */
public class RepositoryInformationService {

	private static final Log log = LogFactory.getLog(RepositoryInformationService.class);

	public String getRepositoryUrl(int tenantId, String cartridgeType) throws Exception {

		String repoUrl = null;
		try {
			repoUrl = PersistenceManager.getRepoURL(tenantId, cartridgeType);
		} catch (Exception e) {
			String msg = "System Exception is occurred when retriving repo URL";
			log.error(msg + ". Reason :" + e.getMessage());
			throw new Exception(msg);
		}
		if (repoUrl == null) {
			log.error("Repository URL is not successfully retrieved " + "for tenant [" + tenantId +
			          "] and cartridge [" + cartridgeType + "] ");
		}
		return repoUrl;
	}

    public String getRepositoryCredentials(int tenantId, String cartridgeType) throws Exception {

        String credentialsString = null;
        try {
            credentialsString = PersistenceManager.getRepoCredentials(tenantId,cartridgeType);
        } catch (Exception e) {
            String msg = "System Exception is occurred when retrieving user credentials";
            log.error(msg + ". Reason :" + e.getMessage());
            throw new Exception(msg);
        }
        if (credentialsString == null) {
            log.error("Repository credentials are not successfully retrieved " + "for tenant [" + tenantId +
                    "] and cartridge [" + cartridgeType + "] ");
        }
        return credentialsString;
    }
}
