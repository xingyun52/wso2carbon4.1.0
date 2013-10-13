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
 */

package org.wso2.carbon.transport.passthru;

import org.apache.http.impl.nio.reactor.SSLSetupHandler;
import org.apache.http.impl.nio.ssl.SSLServerIOEventDispatch;
import org.apache.http.nio.NHttpServerIOTarget;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.params.HttpParams;
import org.wso2.carbon.transport.passthru.logging.LoggingUtils;

import javax.net.ssl.SSLContext;

public class SSLSourceIOEventDispatch extends SSLServerIOEventDispatch {

    private HttpParams params = null;

    public SSLSourceIOEventDispatch(NHttpServiceHandler handler,
                                    SSLContext sslcontext,
                                    SSLSetupHandler sslHandler,
                                    HttpParams params) {
        super(handler, sslcontext, sslHandler, params);
        this.params = params;
    }

    @Override
    protected NHttpServerIOTarget createConnection(IOSession session) {
        session = LoggingUtils.decorate(session, "sslserver");
        return LoggingUtils.createServerConnection(
                session,
                createHttpRequestFactory(),
                createByteBufferAllocator(),
                this.params);
    }
}
