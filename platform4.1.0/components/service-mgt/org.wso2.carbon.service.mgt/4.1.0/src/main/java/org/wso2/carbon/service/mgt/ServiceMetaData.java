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
package org.wso2.carbon.service.mgt;

import org.apache.axis2.description.AxisEndpoint;
import org.wso2.carbon.service.mgt.util.Utils;
import org.wso2.carbon.utils.CarbonUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/*
* 
*/
public class ServiceMetaData {
    private String serviceId;
    private String serviceVersion;
    private String serviceGroupName;
    private String scope;
    private String name;
    private boolean active;
    private String description;
    private String[] operations;
    private String[] eprs;
    private String[] wsdlURLs;
    private boolean foundWebResources;
    private String serviceType;
    private String mtomStatus;
    private boolean disableTryit;
    private String tryitURL;
    private boolean disableDeletion;
    private long deployedTime;
    private String[] endPoints;

    public void setEndPoints(Map endPnts){
        endPoints = new String[endPnts.keySet().size()];
        int i =0 ;
        for (Object p : endPnts.keySet())     {
            this.endPoints[i] = p.toString();
            i++;
        }
    }

    public String[] getEndPoints(){
        return endPoints;
    }

    public String getSecurityScenarioId() {
        return securityScenarioId;
    }

    public void setSecurityScenarioId(String securityScenarioId) {
        this.securityScenarioId = securityScenarioId;
    }

    private String securityScenarioId;

    public String[] getEprs() {
        return CarbonUtils.arrayCopyOf(eprs);
    }

    public void setEprs(String[] eprs) {
        this.eprs = CarbonUtils.arrayCopyOf(eprs);
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String[] getOperations() {
        return CarbonUtils.arrayCopyOf(operations);
    }

    public void setOperations(String[] operations) {
        this.operations = CarbonUtils.arrayCopyOf(operations);
    }

    public boolean isFoundWebResources() {
        return foundWebResources;
    }

    public void setFoundWebResources(boolean foundWebResources) {
        this.foundWebResources = foundWebResources;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }


    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getServiceGroupName() {
        return serviceGroupName;
    }

    public void setServiceGroupName(String serviceGroupName) {
        this.serviceGroupName = serviceGroupName;
    }

    public String getMtomStatus() {
        return mtomStatus;
    }

    public void setMtomStatus(String mtomStatus) {
        this.mtomStatus = mtomStatus;
    }

    public String[] getWsdlURLs() {
        return CarbonUtils.arrayCopyOf(wsdlURLs);
    }

    public void setWsdlURLs(String[] wsdlURLs) {
        this.wsdlURLs = CarbonUtils.arrayCopyOf(wsdlURLs);
    }

    public boolean isDisableTryit() {
        return disableTryit;
    }

    public void setDisableTryit(boolean disableTryit) {
        this.disableTryit = disableTryit;
    }

    public String getTryitURL() {
        return tryitURL;
    }

    public void setTryitURL(String tryitURL) {
        this.tryitURL = tryitURL;
    }

    public boolean isDisableDeletion() {
        return disableDeletion;
    }

    public void setDisableDeletion(boolean disableDeletion) {
        this.disableDeletion = disableDeletion;
    }

    public void setServiceDeployedTime(long deployedTime){
        this.deployedTime = deployedTime;
    }

    public String getServiceUpTime(){
        return Utils.getFormattedDuration(deployedTime);
    }

    public String getServiceDeployedTime(){
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
        return dateFormatter.format(new Date(deployedTime));
    }
}
