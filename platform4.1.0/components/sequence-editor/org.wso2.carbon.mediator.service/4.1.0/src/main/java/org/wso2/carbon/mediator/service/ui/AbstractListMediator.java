/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.mediator.service.ui;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.mediator.service.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class AbstractListMediator extends AbstractMediator implements ListMediator {

    protected List<Mediator> mediators = new ArrayList<Mediator>();

    public List<Mediator> getList() {
        return mediators;
    }

    public Mediator getChild(int pos) {
        return mediators.get(pos);
    }

    public Mediator removeChild(int pos) {
        return mediators.remove(pos);
    }

    public boolean removeChild(Mediator mediator) {
        return mediators.remove(mediator);
    }

    public void addChild(Mediator mediator) {
        mediators.add(mediator);
    }

    protected void serializeChildren(OMElement parent, List<Mediator> list) {
        for (Mediator child : list) {
            child.serialize(parent);
        }
    }

    protected void addChildren(OMElement el, ListMediator m) {
        Iterator it = el.getChildElements();
        while (it.hasNext()) {
            OMElement child = (OMElement) it.next();
            MediatorService mediatorService = MediatorStore.getInstance().getMediatorService(child);
            if (mediatorService != null) {
                Mediator med = mediatorService.getMediator();
                if (med != null) {
                    med.build(child);
                    m.addChild(med);
                } else {
                    String msg = "Unknown mediator : " + child.getLocalName();
                    throw new MediatorException(msg);
                }
            }
        }
    }
}
