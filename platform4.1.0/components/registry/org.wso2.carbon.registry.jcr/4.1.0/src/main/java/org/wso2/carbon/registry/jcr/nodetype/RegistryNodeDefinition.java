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

package org.wso2.carbon.registry.jcr.nodetype;

import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;

/**
 * This is just used for inheritance purpose and sub interface implementations are implemented
 */
public class RegistryNodeDefinition implements NodeDefinition {

    public NodeType[] getRequiredPrimaryTypes() {
        return new NodeType[0];
    }

    public String[] getRequiredPrimaryTypeNames() {
        return new String[0];
    }

    public NodeType getDefaultPrimaryType() {
        return null;
    }

    public String getDefaultPrimaryTypeName() {
        return null;
    }

    public boolean allowsSameNameSiblings() {
        return false;
    }

    public NodeType getDeclaringNodeType() {
        return null;
    }

    public String getName() {
        return null;
    }

    public boolean isAutoCreated() {
        return false;
    }

    public boolean isMandatory() {
        return false;
    }

    public int getOnParentVersion() {
        return 0;
    }

    public boolean isProtected() {
        return false;
    }
}
