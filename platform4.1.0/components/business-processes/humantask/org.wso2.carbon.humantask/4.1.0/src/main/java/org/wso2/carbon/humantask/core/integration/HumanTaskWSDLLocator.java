/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.humantask.core.integration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

import javax.wsdl.xml.WSDLLocator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * WSDL locator for the human task deployment unit
 */
public class HumanTaskWSDLLocator implements WSDLLocator {
    private static final Log log = LogFactory.getLog(HumanTaskWSDLLocator.class);
    private URI baseUri;
    private String latest;

    public HumanTaskWSDLLocator(URI baseUri) {
        this.baseUri = baseUri;
    }

    public HumanTaskWSDLLocator() {
        this.baseUri = null;
    }

    public InputSource getBaseInputSource() {
        try {
            InputSource is = new InputSource();
            is.setByteStream(openResource(baseUri));
            is.setSystemId(baseUri.toString());
            return is;
        } catch (IOException e) {
            log.error("Unable to create InputSource for " + baseUri, e);
            return null;
        }
    }

    public InputStream openResource(URI uri) throws IOException {
        if (uri.isAbsolute() && uri.getScheme().equals("file")) {
            try {
                return uri.toURL().openStream();
            } catch (Exception except) {
                log.error("openResource: unable to open file URL " + uri + "; " + except.toString());
                return null;
            }
        }

        // Note that if we get an absolute URI, the relativize operation will simply
        // return the absolute URI.
        URI relative = baseUri.relativize(uri);

        if (relative.isAbsolute() && relative.getScheme().equals("http")) {
            try {
                return relative.toURL().openStream();
            } catch (Exception except) {
                log.error("openResource: unable to open http URL " + uri + "; " + except.toString());
                return null;
            }
        }

        if (relative.isAbsolute() && !relative.getScheme().equals("urn")) {
            log.error("openResource: invalid scheme (should be urn:)  " + uri);
            return null;
        }

        File f = new File(baseUri.getPath(), relative.getPath());
        if (!f.exists()) {
            log.error("openResource: file not found " + f);
            return null;
        }
        return new FileInputStream(f);
    }

    //With registry handler
//    public InputSource getImportInputSource(String parent, String imprt) {
//        int begin = imprt.indexOf(HIConstants.HUMAN_INTERACTION_WSDL_LOCATION);
//        String wsdlLocation = imprt;
//        if (begin < 0) {
//            begin = imprt.indexOf(HIConstants.HUMAN_INTERACTION_SCHEMA_LOCATION);
//        }
//
//        if (begin < 0) {
//            String errMsg = "Invalid schema location: " + imprt;
//            log.error(errMsg);
//            return null;
//        }
//
//        if (begin > 0) {
//            wsdlLocation = imprt.substring(begin);
//        }
//        InputStream is;
//        try {
//            Registry governanceRegistry = HumanTaskServiceComponent.getRegistryService().getGovernanceSystemRegistry();
//            if (!governanceRegistry.resourceExists(wsdlLocation)) {
//                String errMsg = "Schema does not exist in: " + wsdlLocation;
//                log.error(errMsg);
//                return null;
//            }
//            is = governanceRegistry.get(wsdlLocation).getContentStream();
//            if (is == null) {
//                log.error("Exception resolving entity: WSDL location: " + wsdlLocation + " baseUri: " + baseUri);
//                return null;
//            }
//            InputSource source = new InputSource(is);
//            source.setSystemId(wsdlLocation);
//            source.setPublicId(wsdlLocation);
//            //return new InputSource(is);
//            latest = parent;
//            return source;
//        } catch (Exception e) {
//            log.error("Exception resolving entity: WSDL location: " + wsdlLocation + " baseUri: " + baseUri, e);
//            return null;
//        }
//    }

    //Without registry handler
    public InputSource getImportInputSource(String parent, String imprt) {
        URI uri;
        try {
            uri = parent == null ? baseUri.resolve(imprt) : new URI(parent).resolve(imprt);
        } catch (URISyntaxException e1) {
            log.error("URI syntax error: parent=" + parent, e1);
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("Get import:  import=" + imprt + " parent=" + parent);
        }

        InputSource is = new InputSource();
        try {
            is.setByteStream(openResource(uri));
        } catch (Exception e) {
            log.error("Unable to open import resource: " + uri, e);
            return null;
        }
        is.setSystemId(uri.toString());
        latest = uri.toString();
        return is;
    }

    public String getBaseURI() {
        if (baseUri == null) {
            return null;
        }
        return baseUri.toString();
    }

    public String getLatestImportURI() {
        return latest;
    }

    public void close() {
    }
}
