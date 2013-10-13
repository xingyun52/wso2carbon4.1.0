/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.governance.registry.extensions.executors.apistore;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.services.ServiceManager;
import org.wso2.carbon.governance.api.services.dataobjects.Service;
import org.wso2.carbon.governance.registry.extensions.interfaces.Execution;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.internal.RegistryCoreServiceComponent;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.registry.extensions.utils.CommonUtil;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.Map;

/**
 * This executor used to publish a service to API store as a API.
 */
public class ApiStoreExecutor implements Execution {

    Log log = LogFactory.getLog(ApiStoreExecutor.class);

    private final String API_ARTIFACT_KEY = "api";
    private final String DEFAULT_TIER = "Gold";
    private final String DEFAULT_STATUS = "CREATED";

    //URI Template default settings
    private final String DEFAULT_URI_PATTERN = "/*";
    private final String DEFAULT_HTTP_VERB = "GET";
    private final String DEFAULT_AUTH_TYPE = "Application";
    
    // Those constance are used in API artifact.
    public static final String API_OVERVIEW_NAME = "overview_name";
    public static final String API_OVERVIEW_VERSION = "overview_version";
    public static final String API_OVERVIEW_CONTEXT = "overview_context";
    public static final String API_OVERVIEW_ENDPOINT_URL = "overview_endpointURL";
    public static final String API_OVERVIEW_WSDL = "overview_WSDL";
    public static final String API_OVERVIEW_PROVIDER = "overview_provider";
    public static final String API_OVERVIEW_TIER = "overview_tier";
    public static final String API_OVERVIEW_STATUS = "overview_status";
    public static final String API_URI_PATTERN ="URITemplate_urlPattern";
    public static final String API_URI_HTTP_METHOD ="URITemplate_httpVerb";
    public static final String API_URI_AUTH_TYPE ="URITemplate_authType";

    /**
     * This method is called when the execution class is initialized.
     * All the execution classes are initialized only once.
     *
     * @param parameterMap Static parameter map given by the user.
     *                     These are the parameters that have been given in the
     *                     lifecycle configuration as the parameters of the executor.
     */
    @Override
    public void init(Map parameterMap) {

    }

    /**
     * @param context      The request context that was generated from the registry core.
     *                     The request context contains the resource, resource path and other
     *                     variables generated during the initial call.
     * @param currentState The current lifecycle state.
     * @param targetState  The target lifecycle state.
     * @return Returns whether the execution was successful or not.
     */
    @Override
    public boolean execute(RequestContext context, String currentState, String targetState) {

        Resource resource = context.getResource();
        try {
            String artifactString = RegistryUtils.decodeBytes((byte[]) resource.getContent());
            String user = CarbonContext.getCurrentContext().getUsername();
            OMElement xmlContent = AXIOMUtil.stringToOM(artifactString);
            String serviceName = CommonUtil.getServiceName(xmlContent);

            GenericArtifactManager artifactManager =
                    new GenericArtifactManager(RegistryCoreServiceComponent.
                            getRegistryService().getGovernanceUserRegistry(user,CarbonContext.getCurrentContext().getTenantId()), API_ARTIFACT_KEY);
            GenericArtifact genericArtifact =
                    artifactManager.newGovernanceArtifact(new QName(serviceName));


            ServiceManager serviceManager = new ServiceManager(RegistryCoreServiceComponent.
                    getRegistryService().getGovernanceUserRegistry(user,CarbonContext.getCurrentContext().getTenantId()));
            Service service = serviceManager.getService(context.getResource().getUUID());

            genericArtifact.setAttribute(API_OVERVIEW_NAME, serviceName);
            genericArtifact.setAttribute(API_OVERVIEW_CONTEXT, serviceName);
            genericArtifact.setAttribute(API_OVERVIEW_VERSION,
                    service.getAttribute("overview_version"));

//            if (service.getAttachedWsdls().length > 0) {
//                String url = service.getAttachedWsdls()[0].getUrl();
//                genericArtifact.setAttribute(API_OVERVIEW_WSDL, url);
//            }
            if (service.getAttachedEndpoints().length > 0) {
                genericArtifact.setAttribute(API_OVERVIEW_ENDPOINT_URL,
                        service.getAttachedEndpoints()[0].getUrl());
            }
            genericArtifact.setAttribute(API_OVERVIEW_PROVIDER,
                    CarbonContext.getCurrentContext().getUsername());
            genericArtifact.setAttribute(API_OVERVIEW_TIER, DEFAULT_TIER);
            genericArtifact.setAttribute(API_OVERVIEW_STATUS, DEFAULT_STATUS);
            
            genericArtifact.setAttribute(API_URI_PATTERN, DEFAULT_URI_PATTERN);
			genericArtifact.setAttribute(API_URI_HTTP_METHOD, DEFAULT_HTTP_VERB);
			genericArtifact.setAttribute(API_URI_AUTH_TYPE, DEFAULT_AUTH_TYPE);
			
            artifactManager.addGenericArtifact(genericArtifact);


        } catch (RegistryException e) {
            log.error("Failed to publish service to API store ", e);
            return false;
        } catch (XMLStreamException e) {
            log.error("Failed to convert service to xml content");
            return false;
        }
        return true;
    }
}
