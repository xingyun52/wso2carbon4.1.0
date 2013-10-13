/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.wso2.siddhi.query.api.exception.MalformedAttributeException;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.expression.ExpressionValidator;
import org.wso2.siddhi.query.api.expression.constant.StringConstant;
import org.wso2.siddhi.query.api.query.QueryEventStream;

import java.util.List;

public class Compare extends Condition {
    private Expression rightExpression;
    private Operator operator;
    private Expression leftExpression;

    public Compare(Expression leftExpression, Operator operator,
                   Expression  rightExpression) {
        this.rightExpression = rightExpression;
        this.operator = operator;
        this.leftExpression = leftExpression;
    }

    public Expression getRightExpression() {
        return rightExpression;
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression getLeftExpression() {
        return leftExpression;
    }

    @Override
   protected void validate(List<QueryEventStream> queryEventStreamList, String streamReferenceId,
                           boolean processInStreamDefinition) {
        if (operator.equals(Operator.INSTANCE_OF)) {

            if (rightExpression instanceof StringConstant) {
                String dataType = ((StringConstant) rightExpression).getValue();
                if (!(dataType.equalsIgnoreCase(String.class.getSimpleName()) || dataType.equalsIgnoreCase(Integer.class.getSimpleName())
                      || dataType.equalsIgnoreCase(Double.class.getSimpleName()) || dataType.equalsIgnoreCase(Boolean.class.getSimpleName())
                      || dataType.equalsIgnoreCase(Long.class.getSimpleName()) || dataType.equalsIgnoreCase(Float.class.getSimpleName()))) {
                    throw new MalformedAttributeException("\""+dataType + "\" is not a valid attribute data type");
                }
            }

        }
        ExpressionValidator.validate(rightExpression, queryEventStreamList, streamReferenceId, processInStreamDefinition);
        ExpressionValidator.validate(leftExpression, queryEventStreamList, streamReferenceId, processInStreamDefinition);
    }
}
