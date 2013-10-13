/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.bam.service.data.publisher.data;

import org.wso2.carbon.statistics.services.util.SystemStatistics;

import java.sql.Timestamp;

public class EventData {

    private String serviceName;
    private String operationName;
    private Timestamp timestamp;
    private String userAgent;
    private String remoteAddress;
    private String host;
    private String contentType;
    private String referer;
    private String requestURL;

    private SystemStatistics systemStatistics;

    private String activityId;
    private String direction;
    private String messageId;
    private String soapHeader;
    private String soapBody;
/*    private String inMessageId;
    private String inMsgBody;
    private String outMessageId;
    private String outMsgBody;*/
    
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public SystemStatistics getSystemStatistics() {
        return systemStatistics;
    }

    public void setSystemStatistics(SystemStatistics systemStatistics) {
        this.systemStatistics = systemStatistics;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSOAPBody() {
        return soapBody;
    }

    public void setSOAPBody(String soapBody) {
        this.soapBody = soapBody;
    }

    public void setMessageDirection(String direction) {
        this.direction = direction;
    }

    public String getMessageDirection() {
        return direction;
    }

    public void setSOAPHeader(String soapHeader) {
        this.soapHeader = soapHeader;
    }

    public String getSOAPHeader() {
        return soapHeader;
    }
    
/*    public String getOutMessageId() {
        return outMessageId;
    }

    public void setOutMessageId(String outMessageId) {
        this.outMessageId = outMessageId;
    }

    public String getOutMessageBody() {
        return outMsgBody;
    }

    public void setOutMessageBody(String outMsgBody) {
        this.outMsgBody = outMsgBody;
    }*/
}
