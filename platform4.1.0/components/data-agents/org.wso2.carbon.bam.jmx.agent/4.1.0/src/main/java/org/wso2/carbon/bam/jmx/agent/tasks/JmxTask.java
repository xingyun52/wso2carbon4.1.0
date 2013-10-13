/*
*  Copyright (c) WSO2 Inc. (http://wso2.com) All Rights Reserved.

  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*
*/

package org.wso2.carbon.bam.jmx.agent.tasks;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.bam.jmx.agent.JmxAgent;
import org.wso2.carbon.bam.jmx.agent.exceptions.ProfileDoesNotExistException;
import org.wso2.carbon.bam.jmx.agent.profiles.Profile;
import org.wso2.carbon.bam.jmx.agent.profiles.ProfileManager;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.databridge.agent.thrift.DataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.exception.AuthenticationException;
import org.wso2.carbon.databridge.commons.exception.DifferentStreamDefinitionAlreadyDefinedException;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.databridge.commons.exception.NoStreamDefinitionExistException;
import org.wso2.carbon.databridge.commons.exception.StreamDefinitionException;
import org.wso2.carbon.databridge.commons.exception.TransportException;
import org.wso2.carbon.ntask.core.AbstractTask;

import javax.management.openmbean.CompositeData;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JmxTask extends AbstractTask {

    private static final Log log = LogFactory.getLog(JmxTask.class);

    //stores the data publishers of the tenants
    private static HashMap<String, DataPublisher> dataPublisherHashMap;

    public JmxTask() {


        if (dataPublisherHashMap == null) {
            if (log.isDebugEnabled()) {
                log.info("Data Publisher hash map created.");
            }
            dataPublisherHashMap = new HashMap<String, DataPublisher>();
        }
    }

    @Override
    public void execute() {

        try {
            if (log.isDebugEnabled()) {

                log.info("Running the profile : " + this.getProperties().
                        get(JmxTaskConstants.JMX_PROFILE_NAME));
            }


            Map<String, String> dataMap = this.getProperties();

            //get profile name
            String profileName = dataMap.get(JmxTaskConstants.JMX_PROFILE_NAME);

            //get the profile
            Profile profile;
            try {
                profile = new ProfileManager().getProfile(profileName);
            } catch (ProfileDoesNotExistException e) {

                log.error(e);
                e.printStackTrace();
                return;
            }
            String[][][] attributes = profile.getAttributes();


            //Create a Stream name
            String streamName = "org.wso2.bam.jmx.agent." + profile.getName();
            //Append ".0.0 for the sake of string matching! "
            String version = Integer.toString(profile.getVersion()) + ".0.0";

            //create a Jmx JmxAgent to fetch Jmx data
            org.wso2.carbon.bam.jmx.agent.JmxAgent jmxAgent =
                    new org.wso2.carbon.bam.jmx.agent.JmxAgent(profile);

            //create data publishers
            String streamId;

            DataPublisher dataPublisher = createDataPublisher(profile);


            //try to publish the data
            try {
                streamId = dataPublisher.findStream(streamName, version);
                publishData(streamId, attributes, dataPublisher, jmxAgent, profileName);

            } catch (NoStreamDefinitionExistException e) {
                if (log.isDebugEnabled()) {

                    log.info("Stream definition does not exist for profile " + profile.getName());
                }

                //create the stream definition
                try {
                    createStreamDefinition(streamName, version, attributes, jmxAgent,
                                           dataPublisher);
                } catch (StreamDefinitionException e1) {
                    log.error("Failed to create a new stream definition : " + e);
                    //no point of trying if the stream definition cannot be created
                    return;
                } catch (DifferentStreamDefinitionAlreadyDefinedException e1) {
                    log.error("Failed to create a new stream definition : " + e);
                    //no point of trying if the stream definition cannot be created
                    return;
                } catch (MalformedURLException e1) {
                    log.error("Failed to create a new stream definition : " + e);
                    //no point of trying if the stream definition cannot be created
                    return;
                } catch (AgentException e1) {
                    log.error("Failed to create a new stream definition : " + e);
                    //no point of trying if the stream definition cannot be created
                    return;
                }

                //republish the data
                try {
                    streamId = dataPublisher.findStream(streamName, version);
                    publishData(streamId, attributes, dataPublisher, jmxAgent, profileName);
                } catch (StreamDefinitionException e1) {
                    log.error("Failed to publish data after creating the " +
                              "new stream definition : " + e);
                    return;
                } catch (NoStreamDefinitionExistException e1) {
                    log.error("Failed to publish data after creating the " +
                              "new stream definition : " + e);
                    return;
                } catch (AgentException e1) {
                    log.error("Failed to publish data after creating the " +
                              "new stream definition : " + e);
                    return;
                }

            } catch (StreamDefinitionException e) {
                log.error("Stream definition seems to be invalid : " + e);
            } catch (AgentException e) {
                log.error(e);
            }


        } catch (MalformedURLException e) {
            log.error(e);
        } catch (AgentException e) {
            log.error(e);
        } catch (AuthenticationException e) {
            log.error(e);

            //remove all the data publishers
            for (String key : dataPublisherHashMap.keySet()) {
                dataPublisherHashMap.remove(key);
            }

            if (log.isDebugEnabled()) {
                log.info("Data Publisher hash table cleared");
            }

        } catch (TransportException e) {
            log.error(e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DataPublisher createDataPublisher(Profile profile)
            throws AgentException, MalformedURLException, AuthenticationException,
                   TransportException {

        String dataPublisherReceiverUrl = profile.getDpReceiverAddress();
        String dataPublisherUname = profile.getDpUserName();
        String dataPublisherPass = profile.getDpPassword();
        String dataPublisherReceiverConnectionType = profile.getDpReceiverConnectionType();
        String dataPublisherSecureConnectionType = profile.getDpSecureUrlConnectionType();
        String dataPublisherSecureUrl = profile.getDpSecureAddress();

        //get the tenant ID
        int tenantID = CarbonContext.getCurrentContext().getTenantId();

        //create the key for data publisher storage. Using this key, the correct data
        //publisher with the correct configuration will be returned
        String key = dataPublisherSecureConnectionType + dataPublisherSecureUrl +
                     dataPublisherReceiverConnectionType + dataPublisherReceiverUrl +
                     dataPublisherUname + dataPublisherPass + Integer.toString(tenantID);

        DataPublisher dataPublisher;

        //check for the availability of the data publisher
        if (dataPublisherHashMap.containsKey(key)) {

            if (log.isDebugEnabled()) {

                log.info("DataPublisher exists for tenant " + tenantID);
            }

            dataPublisher = dataPublisherHashMap.get(key);

        } else {

            if (log.isDebugEnabled()) {

                log.info("DataPublisher does not exist for tenant " + tenantID);
            }

            dataPublisher = new DataPublisher(dataPublisherSecureConnectionType +
                                              dataPublisherSecureUrl,
                                              dataPublisherReceiverConnectionType +
                                              dataPublisherReceiverUrl,
                                              dataPublisherUname, dataPublisherPass);

            dataPublisherHashMap.put(key, dataPublisher);
        }


        return dataPublisher;
    }

    private void publishData(String streamId, String[][][] attributes, DataPublisher dataPublisher,
                             JmxAgent jmxAgent, String profileName) throws AgentException {

        ArrayList<Object> arrayList = new ArrayList<Object>();

        /*

       The format of the mbeans/attribtutes array (a 3D array) will be a 2D array
       consisting of an array of the following format.

       -------------------------------------------------
       |Mbean name     |               |               |
       -------------------------------------------------
       |Attribute name |Alias          |               |
       -------------------------------------------------
       |Attribute name |Key            | Alias         |  (If composite)
       -------------------------------------------------

        */

        for (String[][] attribute : attributes) {
            for (int k = 1; k < attribute.length; k++) {
                Object attrValue;

                //if this is a composite data type
                if (attribute[k].length == 3) {
                    if (log.isDebugEnabled()) {
                        System.out.print(attribute[0][0] + "__" +
                                         attribute[k][0] + "__" + attribute[k][1] + "  == ");

                    }
                    //Get the composite object
                    CompositeData cd = (CompositeData)
                            jmxAgent.getAttribute(attribute[0][0], attribute[k][0]);
                    attrValue = cd.get(attribute[k][1]);
                } else {
                    if (log.isDebugEnabled()) {

                        System.out.print(attribute[0][0] + "__" + attribute[k][0] + "  == ");
                    }
                    //Get the attribute
                    attrValue = jmxAgent.getAttribute(attribute[0][0], attribute[k][0]);
                }

                if (log.isDebugEnabled()) {

                    System.out.println(attrValue);
                }

                //TODO-just a temporary fix. Fix this properly!
                if (attrValue == null) {
                    arrayList.add("null");
                } else {
                    if (attrValue instanceof String ||
                        attrValue instanceof Integer ||
                        attrValue instanceof Double ||
                        attrValue instanceof Long ||
                        attrValue instanceof Boolean || attrValue instanceof Float) {
                        arrayList.add(attrValue);
                    }


                }


            }
        }
        Event jmxEvent = new Event(streamId, System.currentTimeMillis(),
                                   new Object[]{"externalEvent"}, null,
                                   arrayList.toArray());
        dataPublisher.publish(jmxEvent);
        if (log.isDebugEnabled()) {

            log.info("jmx Event published for " + profileName);
        }


    }

    private void createStreamDefinition(String streamName, String version, String[][][] attributes,
                                        JmxAgent jmxAgent, DataPublisher dataPublisher)
            throws MalformedURLException, StreamDefinitionException,
                   DifferentStreamDefinitionAlreadyDefinedException, AgentException {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.
                append("{" + "  'name':'").append(streamName).append("',").
                append("  'version':'").append(version).append("',").
                append("  'nickName': 'JMX Dump',").
                append("  'description': 'JMX monitoring data',").
                append("  'metaData':[").
                append("          {'name':'clientType','type':'STRING'}").
                append("  ],").
                append("  'payloadData':[");

        //add the attributes
        for (String[][] attribute : attributes) {
            for (int k = 1; k < attribute.length; k++) {

                //TODO-Change the way of object type determination

                //Get the attribute
                if (log.isDebugEnabled()) {

                    if (attribute[k].length == 3) {
                        log.info("trying to create def for " +
                                 attribute[0][0] + "__" + attribute[k][0] + "__" + attribute[k][1]);
                    } else {

                        log.info("trying to create def for " +
                                 attribute[0][0] + "__" + attribute[k][0]);
                    }

                }

                Object attrValue;
                //if this is a composite data type
                if (attribute[k].length == 3) {
                    //Get the composite object
                    CompositeData cd = (CompositeData) jmxAgent.
                            getAttribute(attribute[0][0], attribute[k][0]);
                    attrValue = cd.get(attribute[k][1]);
//                            isComposite=true;
                } else {
                    //Get the attribute
                    attrValue = jmxAgent.getAttribute(attribute[0][0], attribute[k][0]);
                }

                //if the value is a string
                if (attrValue instanceof String) {
                    stringBuilder.append("{'name':'").
                            append(attribute[k][attribute[k].length - 1]).
                            append("','type':'STRING'},");
                }

                //if the value is an integer
                else if (attrValue instanceof Integer) {
                    stringBuilder.append("{'name':'").
                            append(attribute[k][attribute[k].length - 1]).
                            append("','type':'INT'},");
                }

                //if the value is a double
                else if (attrValue instanceof Double) {
                    stringBuilder.append("{'name':'").
                            append(attribute[k][attribute[k].length - 1]).
                            append("','type':'DOUBLE'},");
                }

                //if the value is a long
                else if (attrValue instanceof Long) {
                    stringBuilder.append("{'name':'").
                            append(attribute[k][attribute[k].length - 1]).
                            append("','type':'LONG'},");
                }

                //if the value is a boolean
                else if (attrValue instanceof Boolean) {
                    stringBuilder.append("{'name':'").
                            append(attribute[k][attribute[k].length - 1]).
                            append("','type':'BOOL'},");
                }

                //if the value is a float
                else if (attrValue instanceof Float) {
                    stringBuilder.append("{'name':'").
                            append(attribute[k][attribute[k].length - 1]).
                            append("','type':'FLOAT'},");
                } else {
                    log.error("Missed attribute in stream def: " +
                              attribute[k][attribute[k].length - 1]);
                }


            }
        }

        //to delete the last comma
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);


        stringBuilder.append("  ] }");

        try {
            dataPublisher.defineStream(stringBuilder.toString());

        } catch (MalformedStreamDefinitionException e) {
            log.error(e);
            throw new MalformedURLException(e.getErrorMessage());
        } catch (StreamDefinitionException e) {
            log.error(e);
            throw new StreamDefinitionException(e.getErrorMessage());
        } catch (DifferentStreamDefinitionAlreadyDefinedException e) {
            log.error(e);
            throw new DifferentStreamDefinitionAlreadyDefinedException(e.getErrorMessage());
        } catch (AgentException e) {
            log.error(e);
            throw new AgentException(e.getErrorMessage());
        }


    }


}
