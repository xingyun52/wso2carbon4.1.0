/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
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
 */
package org.jclouds.vcloud.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;

import org.jclouds.crypto.Crypto;
import org.jclouds.crypto.CryptoStreams;
import org.jclouds.http.HttpException;
import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpRequestFilter;
import org.jclouds.http.utils.ModifyRequest;
import org.jclouds.rest.annotations.Credential;
import org.jclouds.rest.annotations.Identity;

/**
 * Uses Basic Authentication to sign the request.
 * 
 * @see <a href= "http://en.wikipedia.org/wiki/Basic_access_authentication" />
 * @author Adrian Cole
 * 
 */
@Singleton
public class VCloudBasicAuthentication implements HttpRequestFilter {

   private final String header;
   private final String acceptHeader;

   @Inject
   public VCloudBasicAuthentication(@Identity String user, @Credential String password, Crypto crypto) {
      this.header = "Basic "
               + CryptoStreams.base64(String.format("%s:%s", checkNotNull(user, "user"),
                        checkNotNull(password, "password")).getBytes());
      this.acceptHeader = "application/*+xml;version=1.5";
   }

   @Override
	public HttpRequest filter(HttpRequest request) throws HttpException {
		HttpRequest req = ModifyRequest.replaceHeader(request,
				HttpHeaders.AUTHORIZATION, header);
		req = ModifyRequest
				.replaceHeader(req, HttpHeaders.ACCEPT, acceptHeader);
		System.out.println("******** Request : " + req.toString());
		return req;
	}
}