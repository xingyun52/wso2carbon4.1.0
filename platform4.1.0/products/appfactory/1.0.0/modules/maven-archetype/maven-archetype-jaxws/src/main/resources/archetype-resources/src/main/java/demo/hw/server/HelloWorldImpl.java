/*
 * Copyright 2011-2012 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// START SNIPPET: service
package demo.hw.server;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jws.WebService;


/**
 * 
 * HelloWorld WebService implementation
 *
 */
@WebService(endpointInterface = "demo.hw.server.HelloWorld",
            serviceName = "HelloWorld")
public class HelloWorldImpl implements HelloWorld {
    Map<Integer, User> users = new LinkedHashMap<Integer, User>();

    /**
     * @param name This is user name
     * @return this returns a statement, like "Hello WSO2"
     */
    public String sayHi(String name) {
        return "Hello " + name;
    }

    /**
     * 
     * @param user User is the information of the user
     * @return this returns a statement, like "Hello WSO2"
     */
    public String sayHiToUser(User user) {
        users.put(users.size() + 1, user);
        return "Hello "  + user.getName();
    }

    /**
     * 
     * @return this returns LinkedHashMap that contain User objects
     */
    public Map<Integer, User> getUsers() {
        return users;
    }

}
