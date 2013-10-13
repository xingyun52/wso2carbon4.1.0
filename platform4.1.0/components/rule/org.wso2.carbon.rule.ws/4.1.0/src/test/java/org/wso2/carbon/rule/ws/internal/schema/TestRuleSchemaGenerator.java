/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.wso2.carbon.rule.ws.internal.schema;

import junit.framework.TestCase;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.axis2.description.AxisService;

import java.util.Map;
import java.util.HashMap;

public class TestRuleSchemaGenerator extends TestCase {

    public void testGenerateSchema() {
        try {

            AxisService axisService = new AxisService();
            RuleSchemaGenerator ruleSchemaGenerator =
                    new RuleSchemaGenerator(this.getClass().getClassLoader(), axisService);

            Map<String, String> packageToNamespaceMap = new HashMap<String, String>();
            packageToNamespaceMap.put(
                    "org.wso2.carbon.rule.ws.internal.schema","http://test.com/rule");

            ruleSchemaGenerator.setPkg2nsmap(packageToNamespaceMap);
            QName result = ruleSchemaGenerator.generateSchema(PlaceOrder.class);
            XmlSchema xmlSchema = ruleSchemaGenerator.getXmlSchema("http://test.com/rule");
            xmlSchema.write(System.out);
            System.out.println("Xmls schema " + xmlSchema.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
