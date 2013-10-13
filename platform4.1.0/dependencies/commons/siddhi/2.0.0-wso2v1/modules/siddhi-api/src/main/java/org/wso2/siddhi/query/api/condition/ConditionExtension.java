/*
*  Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.siddhi.query.api.condition;

import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.expression.ExpressionValidator;
import org.wso2.siddhi.query.api.extension.Extension;
import org.wso2.siddhi.query.api.query.QueryEventStream;

import java.util.List;

public class ConditionExtension extends Condition implements Extension {

    private String extensionName;
    private String functionName;
    private Expression[] parameters;

    public ConditionExtension(String extensionName, String functionName, Expression[] parameters) {
        this.extensionName = extensionName;
        this.functionName = functionName;
        this.parameters = parameters;
    }

    public String getNamespace() {
        return extensionName;
    }

    public String getFunction() {
        return functionName;
    }

    public Expression[] getParameters() {
        return parameters;
    }

    @Override
    protected void validate(List<QueryEventStream> queryEventStreamList, String streamReferenceId,
                            boolean processInStreamDefinition) {
        for (Expression expression : parameters) {
            ExpressionValidator.validate(expression, queryEventStreamList, streamReferenceId, processInStreamDefinition);
        }
    }

    @Override
    public String toString() {
        return "ConditionExtension{" +
               "extensionName='" + extensionName + '\'' +
               ", functionName='" + functionName + '\'' +
               '}';
    }
}