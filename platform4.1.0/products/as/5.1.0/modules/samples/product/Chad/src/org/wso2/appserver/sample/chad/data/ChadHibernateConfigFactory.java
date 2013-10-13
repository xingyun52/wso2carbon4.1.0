/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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

package org.wso2.appserver.sample.chad.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Caches the HibernateConfig for a particular database
 */
public final class ChadHibernateConfigFactory {
    private static Map hibernateConfigMap = new HashMap();

    public static ChadHibernateConfig getDefaultConfig(String hbConfigKey) {
        ChadHibernateConfig hbConfig;
        Object obj = ChadHibernateConfigFactory.hibernateConfigMap.get(hbConfigKey);
        if (obj == null) {
            hbConfig = new ChadHibernateConfig();
            ChadHibernateConfigFactory.hibernateConfigMap.put(hbConfigKey, hbConfig);
        } else {
            hbConfig = (ChadHibernateConfig) obj;
        }
        return hbConfig;
    }
}
