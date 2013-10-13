package org.wso2.carbon.bam.service.data.publisher.publish;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.bam.data.publisher.util.BAMDataPublisherConstants;
import org.wso2.carbon.bam.service.data.publisher.conf.EventingConfigData;
import org.wso2.carbon.bam.service.data.publisher.conf.Property;
import org.wso2.carbon.bam.service.data.publisher.util.ServiceStatisticsPublisherConstants;
import org.wso2.carbon.bam.service.data.publisher.util.StatisticsType;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;

import java.util.ArrayList;
import java.util.List;

public class StreamDefinitionCreatorUtil {

    private static Log log = LogFactory.getLog(StreamDefinitionCreatorUtil.class);

    public static StreamDefinition getStreamDefinition(EventingConfigData configData,
                                                       StatisticsType statisticsType) {
        StreamDefinition streamDefForActivity;
        StreamDefinition streamDefForServiceStats;
        StreamDefinition streamDefForActivityServiceStats;

        StreamDefinition streamDef = null;
            switch (statisticsType) {

                case ACTIVITY_STATS:
                    streamDefForActivity = streamDefinitionForActivity(configData);
                    streamDef = streamDefForActivity;
                    break;
                case SERVICE_STATS:
                    streamDefForServiceStats = streamDefinitionForServiceStats(configData);
                    streamDef = streamDefForServiceStats;
                    break;
                case ACTIVITY_SERVICE_STATS:
                        streamDefForActivityServiceStats = streamDefinitionForActivityServiceStats(configData);
                    streamDef = streamDefForActivityServiceStats;
                    break;
            }
        return streamDef;
    }


    private static StreamDefinition streamDefinitionForActivity(EventingConfigData configData) {
        StreamDefinition streamDef = null;
        try {
            streamDef = new StreamDefinition(
                    configData.getActivityStreamName(), configData.getActivityStreamVersion());
            streamDef.setNickName(configData.getActivityStreamNickName());
            streamDef.setDescription(configData.getActivityStreamDescription());

            List<Attribute> metaDataAttributeList = new ArrayList<Attribute>();
            setUserAgentMetadata(metaDataAttributeList);
            setPropertiesAsMetaData(metaDataAttributeList, configData);

            streamDef.setMetaData(metaDataAttributeList);

            List<Attribute> payLoadData = new ArrayList<Attribute>();
            payLoadData = addCommonPayLoadData(payLoadData);
            payLoadData = addMessagePayLoadData(payLoadData);
            //payLoadData = addOutOnlyPayLoadData(payLoadData);
            streamDef.setPayloadData(payLoadData);

            streamDef.setCorrelationData(setActivityCorrelationData());
        } catch (MalformedStreamDefinitionException e) {
            log.error("Malformed Stream Definition", e);
        }
        return streamDef;
    }

    private static StreamDefinition streamDefinitionForServiceStats(EventingConfigData configData) {
        StreamDefinition streamDef = null;
        try {
            streamDef = new StreamDefinition(
                    configData.getStreamName(), configData.getVersion());
            streamDef.setNickName(configData.getNickName());
            streamDef.setDescription(configData.getDescription());

            List<Attribute> metaDataAttributeList = new ArrayList<Attribute>();
            setUserAgentMetadata(metaDataAttributeList);
            setPropertiesAsMetaData(metaDataAttributeList, configData);

            streamDef.setMetaData(metaDataAttributeList);

            List<Attribute> payLoadData = new ArrayList<Attribute>();
            payLoadData = addCommonPayLoadData(payLoadData);
            payLoadData = addServiceStatsPayLoadData(payLoadData);
            streamDef.setPayloadData(payLoadData);

        } catch (MalformedStreamDefinitionException e) {
            log.error("Malformed Stream Definition", e);
        }
        return streamDef;
    }

    private static StreamDefinition streamDefinitionForActivityServiceStats(EventingConfigData configData) {
        StreamDefinition streamDef = null;
        try {
            streamDef = new StreamDefinition(
                    configData.getStreamName(), configData.getVersion());
            streamDef.setNickName(configData.getNickName());
            streamDef.setDescription(configData.getDescription());

            List<Attribute> metaDataAttributeList = new ArrayList<Attribute>();
            setUserAgentMetadata(metaDataAttributeList);
            setPropertiesAsMetaData(metaDataAttributeList, configData);

            streamDef.setMetaData(metaDataAttributeList);

            List<Attribute> payLoadData = new ArrayList<Attribute>();
            payLoadData = addCommonPayLoadData(payLoadData);
            payLoadData = addMessagePayLoadData(payLoadData);
            //payLoadData = addOutOnlyPayLoadData(payLoadData);
            payLoadData = addServiceStatsPayLoadData(payLoadData);
            streamDef.setPayloadData(payLoadData);

            streamDef.setCorrelationData(setActivityCorrelationData());
        } catch (MalformedStreamDefinitionException e) {
            log.error("Malformed Stream Definition", e);
        }
        return streamDef;
    }

    private static void setPropertiesAsMetaData(List<Attribute> metaDataAttributeList,
                                         EventingConfigData configData) {
        Property[] properties = configData.getProperties();
        if (properties != null) {
            for (int i = 0; i < properties.length; i++) {
                Property property = properties[i];
                if (property.getKey() != null && !property.getKey().isEmpty()) {
                    metaDataAttributeList.add(new Attribute(property.getKey(), AttributeType.STRING));
                }
            }
        }
    }


    private static List<Attribute> setActivityCorrelationData() {
        List<Attribute> attributeList = new ArrayList<Attribute>();
        attributeList.add(new Attribute(BAMDataPublisherConstants.MSG_ACTIVITY_ID, AttributeType.STRING));
        return attributeList;
    }

    private static List<Attribute> addMessagePayLoadData(List<Attribute> payLoadData) {
        payLoadData.add(new Attribute(BAMDataPublisherConstants.MSG_ID,
                                      AttributeType.STRING));
        payLoadData.add(new Attribute(BAMDataPublisherConstants.SOAP_HEADER,
                                      AttributeType.STRING));
        payLoadData.add(new Attribute(BAMDataPublisherConstants.SOAP_BODY,
                                      AttributeType.STRING));
        payLoadData.add(new Attribute(BAMDataPublisherConstants.MSG_DIRECTION,
                                      AttributeType.STRING));
        return payLoadData;
    }

/*    private static List<Attribute> addOutOnlyPayLoadData(List<Attribute> payLoadData) {
        payLoadData.add(new Attribute(BAMDataPublisherConstants.OUT_MSG_ID,
                                      AttributeType.STRING));
        payLoadData.add(new Attribute(BAMDataPublisherConstants.OUT_MSG_BODY,
                                      AttributeType.STRING));
        return payLoadData;
    }*/

    private static List<Attribute> addCommonPayLoadData(List<Attribute> payLoadData) {
        payLoadData.add(new Attribute(BAMDataPublisherConstants.SERVICE_NAME,
                                      AttributeType.STRING));
        payLoadData.add(new Attribute(BAMDataPublisherConstants.OPERATION_NAME,
                                      AttributeType.STRING));
        payLoadData.add(new Attribute(BAMDataPublisherConstants.TIMESTAMP,
                                      AttributeType.LONG));
        return payLoadData;
    }

    private static List<Attribute> addServiceStatsPayLoadData(List<Attribute> payLoadData) {
        payLoadData.add(new Attribute(ServiceStatisticsPublisherConstants.RESPONSE_TIME,
                                      AttributeType.LONG));
        payLoadData.add(new Attribute(ServiceStatisticsPublisherConstants.REQUEST_COUNT,
                                      AttributeType.INT));
        payLoadData.add(new Attribute(ServiceStatisticsPublisherConstants.RESPONSE_COUNT,
                                      AttributeType.INT));
        payLoadData.add(new Attribute(ServiceStatisticsPublisherConstants.FAULT_COUNT,
                                      AttributeType.INT));
        return payLoadData;
    }

    private static void setUserAgentMetadata(List<Attribute> attributeList) {
        attributeList.add(new Attribute(BAMDataPublisherConstants.REQUEST_URL,
                                        AttributeType.STRING));
        attributeList.add(new Attribute(BAMDataPublisherConstants.REMOTE_ADDRESS,
                                        AttributeType.STRING));
        attributeList.add(new Attribute(BAMDataPublisherConstants.CONTENT_TYPE,
                                        AttributeType.STRING));
        attributeList.add(new Attribute(BAMDataPublisherConstants.USER_AGENT,
                                        AttributeType.STRING));
        attributeList.add(new Attribute(BAMDataPublisherConstants.HOST,
                                        AttributeType.STRING));
        attributeList.add(new Attribute(BAMDataPublisherConstants.REFERER,
                                        AttributeType.STRING));
    }
}
