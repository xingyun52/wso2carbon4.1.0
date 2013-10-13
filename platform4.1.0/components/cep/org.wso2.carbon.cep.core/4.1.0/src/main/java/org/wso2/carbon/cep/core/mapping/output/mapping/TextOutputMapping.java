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

package org.wso2.carbon.cep.core.mapping.output.mapping;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cep.core.exception.CEPEventProcessingException;
import org.wso2.carbon.cep.core.internal.util.CEPRegistryUtils;

import java.util.ArrayList;
import java.util.List;

public class TextOutputMapping extends OutputMapping {

    private static final Log log = LogFactory.getLog(TextOutputMapping.class);

    private String mappingText;
    private List<String> mappingTextList = new ArrayList<String>();

    public String getMappingText() {
        return mappingText;
    }

    public void setMappingText(String mappingText) {
        this.mappingText = mappingText;
        String text = mappingText;
        if (CEPRegistryUtils.isRegistryPath(mappingText)) {
            text = CEPRegistryUtils.getResource(mappingText);
        }
        mappingTextList.clear();
        while (text.contains("{") && text.indexOf("}") > 0) {
            mappingTextList.add(text.substring(0, text.indexOf("{")));
            mappingTextList.add(text.substring(text.indexOf("{") + 1, text.indexOf("}")));
            text = text.substring(text.indexOf("}") + 1);
        }
        mappingTextList.add(text);

    }

    public Object convert(Object event) throws CEPEventProcessingException {

        String eventText = mappingTextList.get(0);
        for (int i = 1; i < mappingTextList.size(); i++) {
            if (i % 2 == 0) {
                eventText += mappingTextList.get(i);
            } else {
                eventText += getPropertyValue(event, mappingTextList.get(i)).toString();
            }
        }
        return eventText;
    }

}
