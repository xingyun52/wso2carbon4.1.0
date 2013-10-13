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

package org.apache.synapse.transport.passthru.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.nio.NHttpServiceHandler;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;

import java.io.IOException;

public class LoggingSourceHandler implements NHttpServiceHandler {

    private final Log log;

    private final NHttpServiceHandler handler;

    public LoggingSourceHandler (final NHttpServiceHandler handler) {
        super();
        if (handler == null) {
            throw new IllegalArgumentException("HTTP service handler may not be null");
        }
        this.handler = handler;
        this.log = LogFactory.getLog(handler.getClass());
    }

    public void connected(final NHttpServerConnection conn) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Connected");
        }
        this.handler.connected(conn);
    }

    public void closed(final NHttpServerConnection conn) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Closed");
        }
        this.handler.closed(conn);
    }

    public void exception(final NHttpServerConnection conn, final IOException ex) {
        if (ex instanceof ConnectionClosedException ||
                ex.getMessage().contains("Connection reset by peer") ||
                ex.getMessage().contains("forcibly closed")) {
            if (this.log.isDebugEnabled()) {
                this.log.debug("HTTP connection " + conn + ": " + ex.getMessage() +
                    " (Probably the keepalive connection was closed)");
            }
        } else {
            this.log.error("IO Error occured on HTTP connection " + conn + ": " + ex.getMessage(), ex);
        }
        this.handler.exception(conn, ex);
    }

    public void exception(final NHttpServerConnection conn, final HttpException ex) {
        this.log.error("HTTP Error occured on connection " + conn + ": " + ex.getMessage(), ex);
        this.handler.exception(conn, ex);
    }

    public void requestReceived(final NHttpServerConnection conn) {
        HttpRequest request = conn.getHttpRequest();
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP InRequest Received on connection " + conn + ": "
                    + request.getRequestLine());
        }
        this.handler.requestReceived(conn);
    }

    public void outputReady(final NHttpServerConnection conn, final ContentEncoder encoder) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Output ready");
        }
        this.handler.outputReady(conn, encoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content encoder " + encoder);
        }
    }

    public void responseReady(final NHttpServerConnection conn) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Response ready");
        }
        this.handler.responseReady(conn);
    }

    public void inputReady(final NHttpServerConnection conn, final ContentDecoder decoder) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Input ready");
        }
        this.handler.inputReady(conn, decoder);
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Content decoder " + decoder);
        }
    }

    public void timeout(final NHttpServerConnection conn) {
        if (this.log.isDebugEnabled()) {
            this.log.debug("HTTP connection " + conn + ": Timeout");
        }
        this.handler.timeout(conn);
    }
}
