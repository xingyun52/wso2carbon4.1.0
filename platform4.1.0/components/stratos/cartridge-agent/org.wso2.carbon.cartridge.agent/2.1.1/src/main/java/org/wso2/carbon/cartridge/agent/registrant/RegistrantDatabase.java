/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.cartridge.agent.registrant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cartridge.agent.exception.CartridgeAgentException;
import org.wso2.carbon.cartridge.agent.InstanceStateNotificationClientThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class represents a database for {@link Registrant}s. Registrants added to this database will be
 * persisted, so that when the Cartridge Agent is restarted, the Registrants can be restored.
 *
 * @see Registrant
 */
public class RegistrantDatabase {
    private static final Log log = LogFactory.getLog(RegistrantDatabase.class);

    private List<Registrant> registrants = new CopyOnWriteArrayList<Registrant>();

    public void add(Registrant registrant) throws CartridgeAgentException {
        if (registrants.contains(registrant) && registrant.running()) {
            throw new CartridgeAgentException("Active registrant with key " +
                                              registrant.getKey() + " already exists");
        }
        persist(registrant);
        registrants.add(registrant);
        log.info("Added registrant " + registrant);
    }

    private void persist(Registrant registrant) throws CartridgeAgentException {
        try {
            ObjectOutput out = null;
            try {
                // Serialize to a file
                if (!new File("registrants").exists() && !new File("registrants").mkdirs()) {
                    throw new IOException("Cannot create registrants directory");
                }
                out = new ObjectOutputStream(new FileOutputStream("registrants" + File.separator +
                                                                  registrant.getKey() + ".ser"));
                out.writeObject(registrant);
                out.close();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            log.error("Could not serialize registrant " + registrant, e);
        }
    }

    public void stopAll() {
        for (Registrant registrant : registrants) {
        	new Thread(new InstanceStateNotificationClientThread(registrant, "INACTIVE")).start();
            registrant.stop();
        }
    }

    public boolean containsActive(Registrant registrant) {
        return registrants.contains(registrant) &&
               registrants.get(registrants.indexOf(registrant)).running();
    }

    public List<Registrant> getRegistrants() {
        return Collections.unmodifiableList(registrants);
    }
}
