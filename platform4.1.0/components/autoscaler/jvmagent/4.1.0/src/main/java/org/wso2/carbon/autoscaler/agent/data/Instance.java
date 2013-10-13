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
package org.wso2.carbon.autoscaler.agent.data;

/**
 * This object holds the details regarding a server Instance.
 */
public class Instance {

    /**
     * Unique id to represent this instance
     */
    private String instanceId;

    /**
     * Physical path to the carbon_home of this instance
     */
    private String pathToInstance;

    /**
     * portOffset of this instance
     */
    private int portOffset;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getPathToInstance() {
        return pathToInstance;
    }

    public void setPathToInstance(String pathToInstance) {
        this.pathToInstance = pathToInstance;
    }

    public int getPortOffset() {
        return portOffset;
    }

    public void setPortOffset(int offset) {
        this.portOffset = offset;
    }

    /**
     * If the instanceId is not null, match based on it.
     * If it is null, match based on the equality of path to instance.
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Instance instance = (Instance) o;

        // if the instanceId is not null, match based on it.
        if (instanceId != null) {

            return instanceId.equals(instance.getInstanceId());
        }
        // if the instanceId is null, match based on the path
        else {
            return (pathToInstance.equals(instance.getPathToInstance()));
        }

    }

    public int hashCode() {
        return instanceId.hashCode();
    }

    public String toString() {
        return "Instance{" + "instanceId='" + instanceId + '\'' + "instancePath='" +
               pathToInstance + '\'' + "portOffset='" + portOffset + '\'' + '}';
    }
}
