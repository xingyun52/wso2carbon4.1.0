/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.registry.jcr.query.qom;

import javax.jcr.query.qom.EquiJoinCondition;

public class RegistryEquiJoinCondition implements EquiJoinCondition {

    private String selector1Name = "";
    private String property1Name = "";
    private String selector2Name = "";
    private String property2Name = "";

    public RegistryEquiJoinCondition(String selector1Name, String property1Name,
                                     String selector2Name, String property2Name) {

        this.selector1Name = selector1Name;
        this.selector2Name = selector2Name;
        this.property1Name = property1Name;
        this.property2Name = property2Name;

    }

    public String getSelector1Name() {

        return selector1Name;
    }

    public String getProperty1Name() {

        return property1Name;
    }

    public String getSelector2Name() {

        return selector2Name;
    }

    public String getProperty2Name() {

        return property2Name;
    }
}
