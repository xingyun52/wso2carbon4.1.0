package org.wso2.carbon.bam.service.data.publisher.conf;


import org.wso2.carbon.databridge.commons.StreamDefinition;

public class EventConfigNStreamDef {
    private boolean isServiceStatsEnable;
    private boolean isMsgDumpingEnable;
    private String url;
    private String userName;
    private String password;

    private String streamName = "bam_service_data_publisher";
    private String version = "1.0.0";
    private String nickName = "ServiceDataAgent";
    private String description = "Publish service statistics events";

    private String activityStreamName = "bam_activity_data_publisher";
    private String activityStreamVersion = "1.0.0";
    private String activityStreamNickName = "ActivityDataAgent";
    private String activityStreamDescription = "Activity events";

    private StreamDefinition streamDefinition;
    private StreamDefinition activityStreamDefinition;

    private boolean isLoadBalancingConfig = false;

    private Property[] properties;

    public StreamDefinition getStreamDefinition() {
        return streamDefinition;
    }

    public void setStreamDefinition(StreamDefinition streamDefinition) {
        this.streamDefinition = streamDefinition;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(Property[] properties) {
        this.properties = properties;
    }

    public boolean isServiceStatsEnable() {
        return isServiceStatsEnable;
    }

    public void setServiceStatsEnable(boolean serviceStatsEnable) {
        isServiceStatsEnable = serviceStatsEnable;
    }

    public boolean isMsgDumpingEnable() {
        return isMsgDumpingEnable;
    }

    public void setMsgDumpingEnable(boolean msgDumpingEnable) {
        isMsgDumpingEnable = msgDumpingEnable;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        setLoadBalancingConfig();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private void setLoadBalancingConfig() {
        this.isLoadBalancingConfig = this.url.split(",").length > 1;
    }

    public boolean isLoadBalancingConfig() {
        return isLoadBalancingConfig;
    }

    public String getActivityStreamName() {
        return activityStreamName;
    }

    public void setActivityStreamName(String activityStreamName) {
        this.activityStreamName = activityStreamName;
    }

    public String getActivityStreamVersion() {
        return activityStreamVersion;
    }

    public void setActivityStreamVersion(String activityStreamVersion) {
        this.activityStreamVersion = activityStreamVersion;
    }

    public String getActivityStreamNickName() {
        return activityStreamNickName;
    }

    public void setActivityStreamNickName(String activityStreamNickName) {
        this.activityStreamNickName = activityStreamNickName;
    }

    public String getActivityStreamDescription() {
        return activityStreamDescription;
    }

    public void setActivityStreamDescription(String activityStreamDescription) {
        this.activityStreamDescription = activityStreamDescription;
    }

    public void setActivityStreamDefinition(StreamDefinition activityStreamDefinition) {
        this.activityStreamDefinition = activityStreamDefinition;
    }

    public StreamDefinition getActivityStreamDefinition() {
        return activityStreamDefinition;
    }

}
