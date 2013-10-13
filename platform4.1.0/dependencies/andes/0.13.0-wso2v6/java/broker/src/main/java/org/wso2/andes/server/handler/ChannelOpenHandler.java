/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.andes.server.handler;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.wso2.andes.AMQException;
import org.wso2.andes.framing.ChannelOpenBody;
import org.wso2.andes.framing.ChannelOpenOkBody;
import org.wso2.andes.framing.MethodRegistry;
import org.wso2.andes.framing.ProtocolVersion;
import org.wso2.andes.framing.amqp_0_9.MethodRegistry_0_9;
import org.wso2.andes.framing.amqp_0_91.MethodRegistry_0_91;
import org.wso2.andes.framing.amqp_8_0.MethodRegistry_8_0;
import org.wso2.andes.protocol.AMQConstant;
import org.wso2.andes.server.AMQChannel;
import org.wso2.andes.server.protocol.AMQProtocolSession;
import org.wso2.andes.server.state.AMQStateManager;
import org.wso2.andes.server.state.StateAwareMethodListener;
import org.wso2.andes.server.virtualhost.VirtualHost;

public class ChannelOpenHandler implements StateAwareMethodListener<ChannelOpenBody>
{
    private static final Logger _logger = Logger.getLogger(ChannelOpenHandler.class);
    
    private static ChannelOpenHandler _instance = new ChannelOpenHandler();

    public static ChannelOpenHandler getInstance()
    {
        return _instance;
    }

    private ChannelOpenHandler()
    {
    }

    public void methodReceived(AMQStateManager stateManager, ChannelOpenBody body, int channelId) throws AMQException
    {
        AMQProtocolSession session = stateManager.getProtocolSession();
        VirtualHost virtualHost = session.getVirtualHost();
        
        // Protect the broker against out of order frame request.
        if (virtualHost == null)
        {
            throw new AMQException(AMQConstant.COMMAND_INVALID, "Virtualhost has not yet been set. ConnectionOpen has not been called.", null);
        }
        _logger.info("Connecting to: " + virtualHost.getName());

        final AMQChannel channel = new AMQChannel(session,channelId, virtualHost.getMessageStore());
        
        session.addChannel(channel);

        ChannelOpenOkBody response;

        ProtocolVersion pv = session.getProtocolVersion();

        if(pv.equals(ProtocolVersion.v8_0))
        {
            MethodRegistry_8_0 methodRegistry = (MethodRegistry_8_0) MethodRegistry.getMethodRegistry(ProtocolVersion.v8_0);
            response = methodRegistry.createChannelOpenOkBody();

        }
        else if(pv.equals(ProtocolVersion.v0_9))
        {
            MethodRegistry_0_9 methodRegistry = (MethodRegistry_0_9) MethodRegistry.getMethodRegistry(ProtocolVersion.v0_9);
            UUID uuid = UUID.randomUUID();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(output);
            try
            {
                dataOut.writeLong(uuid.getMostSignificantBits());
                dataOut.writeLong(uuid.getLeastSignificantBits());
                dataOut.flush();
                dataOut.close();
            }
            catch (IOException e)
            {
                // This *really* shouldn't happen as we're not doing any I/O
                throw new RuntimeException("I/O exception when writing to byte array", e);
            }

            // should really associate this channelId to the session
            byte[] channelName = output.toByteArray();
            
            response = methodRegistry.createChannelOpenOkBody(channelName);
        }
        else if(pv.equals(ProtocolVersion.v0_91))
        {
            MethodRegistry_0_91 methodRegistry = (MethodRegistry_0_91) MethodRegistry.getMethodRegistry(ProtocolVersion.v0_91);
            UUID uuid = UUID.randomUUID();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(output);
            try
            {
                dataOut.writeLong(uuid.getMostSignificantBits());
                dataOut.writeLong(uuid.getLeastSignificantBits());
                dataOut.flush();
                dataOut.close();
            }
            catch (IOException e)
            {
                // This *really* shouldn't happen as we're not doing any I/O
                throw new RuntimeException("I/O exception when writing to byte array", e);
            }

            // should really associate this channelId to the session
            byte[] channelName = output.toByteArray();

            response = methodRegistry.createChannelOpenOkBody(channelName);
        }
        else
        {
            throw new AMQException(AMQConstant.INTERNAL_ERROR, "Got channel open for protocol version not catered for: " + pv, null);
        }


        session.writeFrame(response.generateFrame(channelId));
    }
}
