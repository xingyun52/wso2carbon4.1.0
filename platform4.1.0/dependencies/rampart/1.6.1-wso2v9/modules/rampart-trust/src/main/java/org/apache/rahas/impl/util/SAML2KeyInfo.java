package org.apache.rahas.impl.util;

import org.opensaml.saml2.core.Assertion;

import java.security.cert.X509Certificate;/*
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

/**
 * TODO : This class should be moved to WSS4J once a new version of it is avaliable
 * This class holds the secrets contained in a SAML2 token.
 */
public class SAML2KeyInfo {
    /**
     * Certificates
     */
    private X509Certificate[] certs;

    /**
     * Key bytes (e.g.: held in an encrypted key)
     */
    private byte[] secret;

    /**
     * SAMLAssertion
     */
    Assertion assertion;

    public SAML2KeyInfo(Assertion assertions, X509Certificate[] certs) {
        this.certs = certs;
        this.assertion = assertions;
    }

    public SAML2KeyInfo(Assertion assertions, byte[] secret) {
        this.secret = secret;
        this.assertion = assertions;
    }

    public X509Certificate[] getCerts() {
        return certs;
    }

    public byte[] getSecret() {
        return secret;
    }

    public Assertion getAssertion() {
        return assertion;
    }
}
