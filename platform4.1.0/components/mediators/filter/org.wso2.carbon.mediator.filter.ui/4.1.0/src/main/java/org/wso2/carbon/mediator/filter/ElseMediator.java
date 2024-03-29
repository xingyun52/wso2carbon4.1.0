/**
 *  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 **/

package org.wso2.carbon.mediator.filter;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.mediator.service.ui.AbstractListMediator;

public class ElseMediator extends AbstractListMediator {

    public String getTagLocalName() {
        return "else";
    }

    public OMElement serialize(OMElement parent) {
        OMElement in = fac.createOMElement("else", synNS);
        saveTracingState(in, this);

        serializeChildren(in, getList());

        if (parent != null) {
            parent.addChild(in);
        }

        return in;
    }

    public void build(OMElement elem) {
        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        processAuditStatus(this, elem);
        addChildren(elem, this);
    }
}
