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
package org.wso2.carbon.humantask.core.dao.jpa.openjpa.model.provider;

import org.wso2.carbon.humantask.TFrom;
import org.wso2.carbon.humantask.core.HumanTaskConstants;
import org.wso2.carbon.humantask.core.dao.OrganizationalEntityDAO;
import org.wso2.carbon.humantask.core.engine.PeopleQueryEvaluator;
import org.wso2.carbon.humantask.core.engine.runtime.api.EvaluationContext;
import org.wso2.carbon.humantask.core.engine.runtime.api.ExpressionLanguageRuntime;
import org.wso2.carbon.humantask.core.internal.HumanTaskServiceComponent;

import java.util.ArrayList;
import java.util.List;

public class ExpressionBasedOrgEntityProvider implements OrganizationalEntityProvider {

    public List<OrganizationalEntityDAO> getOrganizationalEntities(
            PeopleQueryEvaluator peopleQueryEvaluator, TFrom tFrom,
            EvaluationContext evaluationContext) {
        String expression = tFrom.newCursor().getTextValue().trim();

        String expLang = (tFrom.getExpressionLanguage() == null) ?
                HumanTaskConstants.WSHT_EXP_LANG_XPATH20 : tFrom.getExpressionLanguage();
        ExpressionLanguageRuntime expLangRuntime = HumanTaskServiceComponent.getHumanTaskServer().
                    getTaskEngine().getExpressionLanguageRuntime(expLang);
        List list = expLangRuntime.evaluate(expression, evaluationContext);

        List<OrganizationalEntityDAO> orgEntities = new ArrayList<OrganizationalEntityDAO>();
        orgEntities.add(peopleQueryEvaluator.createUserOrgEntityForName("prabath"));
        orgEntities.add(peopleQueryEvaluator.createUserOrgEntityForName("admin"));

        return orgEntities;
    }

}
