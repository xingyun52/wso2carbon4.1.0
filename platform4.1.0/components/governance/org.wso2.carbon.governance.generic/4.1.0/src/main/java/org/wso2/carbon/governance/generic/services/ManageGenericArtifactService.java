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
package org.wso2.carbon.governance.generic.services;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.governance.api.common.dataobjects.GovernanceArtifact;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactFilter;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifactImpl;
import org.wso2.carbon.governance.api.util.GovernanceArtifactConfiguration;
import org.wso2.carbon.governance.api.util.GovernanceConstants;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.governance.generic.beans.ArtifactBean;
import org.wso2.carbon.governance.generic.beans.ArtifactsBean;
import org.wso2.carbon.governance.generic.beans.ContentArtifactsBean;
import org.wso2.carbon.governance.generic.beans.StoragePathBean;
import org.wso2.carbon.governance.generic.util.GenericArtifactUtil;
import org.wso2.carbon.governance.generic.util.Util;
import org.wso2.carbon.governance.list.util.StringComparatorUtil;
import org.wso2.carbon.governance.registry.extensions.utils.CommonUtil;
import org.wso2.carbon.registry.admin.api.governance.IManageGenericArtifactService;
import org.wso2.carbon.registry.common.CommonConstants;
import org.wso2.carbon.registry.common.services.RegistryAbstractAdmin;
import org.wso2.carbon.registry.core.*;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.user.core.UserStoreException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.*;

@SuppressWarnings({"unused", "NonJaxWsWebServices", "ValidExternallyBoundObject"})
public class ManageGenericArtifactService extends RegistryAbstractAdmin implements IManageGenericArtifactService {
    private static final Log log = LogFactory.getLog(ManageGenericArtifactService.class);
    private static final String GOVERNANCE_ARTIFACT_CONFIGURATION_PATH =
            RegistryConstants.GOVERNANCE_COMPONENT_PATH + "/configuration/";

    public String addArtifact(String key, String info, String lifecycleAttribute) throws
            RegistryException {
        RegistryUtils.recordStatistics(key, info, lifecycleAttribute);
        Registry registry = getGovernanceUserRegistry();
        if (RegistryUtils.isRegistryReadOnly(registry.getRegistryContext())) {
            return null;
        }
        try {
            XMLStreamReader reader =
                    XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(info));

            GovernanceArtifactConfiguration configuration =
                    GovernanceUtils.findGovernanceArtifactConfiguration(key, getRootRegistry());

            GenericArtifactManager manager = new GenericArtifactManager(registry,
                    configuration.getMediaType(), configuration.getArtifactNameAttribute(),
                    configuration.getArtifactNamespaceAttribute(),
                    configuration.getArtifactElementRoot(),
                    configuration.getArtifactElementNamespace(),
                    configuration.getPathExpression(),
                    configuration.getRelationshipDefinitions());
            GenericArtifact artifact = manager.newGovernanceArtifact(
                    new StAXOMBuilder(reader).getDocumentElement());

            List<Map> validAttr = configuration.getValidationAttributes();
            manager.validateArtifact(artifact, validAttr);
            manager.addGenericArtifact(artifact);
            if (lifecycleAttribute != null) {
                String lifecycle = artifact.getAttribute(lifecycleAttribute);
                if (lifecycle != null) {
                    artifact.attachLifecycle(lifecycle);
                }
            }
            return RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + artifact.getPath();
        } catch (Exception e) {
            String msg = "Unable to add artifact. ";
            if (e instanceof RegistryException) {
                throw (RegistryException) e;
            } else if (e instanceof OMException) {
                msg += "Unexpected character found in input-field name.";
                log.error(msg, e);
                throw new RegistryException(msg, e);
            }
            throw new RegistryException(
                    msg + (e.getCause() instanceof SQLException ? "" : e.getCause().getMessage()),
                    e);
        }
    }

    public StoragePathBean getStoragePath(String key) {
        UserRegistry governanceRegistry = (UserRegistry) getGovernanceUserRegistry();
        StoragePathBean bean = new StoragePathBean();
        try {
            GovernanceArtifactConfiguration configuration =
                    GovernanceUtils.findGovernanceArtifactConfiguration(key, getRootRegistry());
            bean.setStoragePath(configuration.getPathExpression());
            OMElement contentDefinition = configuration.getContentDefinition();
            Iterator fields = contentDefinition.getChildrenWithName(new QName("field"));
            List<String> names = new LinkedList<String>();
            List<String> labels = new LinkedList<String>();
            while (fields.hasNext()) {
                OMElement fieldElement = (OMElement) fields.next();
                OMElement nameElement = fieldElement.getFirstChildWithName(new QName("name"));
                String name = nameElement.getText();
                names.add(name);
                String label = nameElement.getAttributeValue(new QName("label"));
                labels.add(label != null ? label : name);
                bean.increment();
            }
            if (bean.getSize() > 0) {
                bean.setNames(names.toArray(new String[names.size()]));
                bean.setLabels(labels.toArray(new String[labels.size()]));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while obtaining the storage path details.", e);
        }
        return bean;
    }

    private boolean nameMatches(GovernanceArtifact artifact, String criteria)
            throws GovernanceException {
        String name = getName(artifact);
        return name != null && name.contains(criteria);
    }


    private boolean lcMatches(GovernanceArtifact artifact, String LCName, String LCState, String LCInOut, String LCStateInOut)
            throws GovernanceException {
        String name = artifact.getLifecycleName();
        String state = artifact.getLifecycleState();
        if(LCName.equalsIgnoreCase("")){
            return true;
        }
        if(!LCState.equalsIgnoreCase("")){
            if(LCInOut.equalsIgnoreCase("in") && LCStateInOut.equalsIgnoreCase("in")){
                if(name != null
                   && state != null && LCState.equalsIgnoreCase(state)
                   && LCName.equalsIgnoreCase(name)){
                    return true;
                }else{
                    return false;
                }
            } else if(LCInOut.equalsIgnoreCase("in") && !LCStateInOut.equalsIgnoreCase("in")){
                if(name != null
                   && state != null && !LCState.equalsIgnoreCase(state)
                   && LCName.equalsIgnoreCase(name)){
                    return true;
                }else{
                    return false;
                }
            } else if(!LCInOut.equalsIgnoreCase("in") && LCStateInOut.equalsIgnoreCase("in")){
                if(name != null
                   && state != null && LCState.equalsIgnoreCase(state)
                   && !LCName.equalsIgnoreCase(name)){
                    return true;
                }else{
                    return false;
                }
            } else {
                if(name != null
                   && state != null && !LCState.equalsIgnoreCase(state)
                   && !LCName.equalsIgnoreCase(name)){
                    return true;
                }else{
                    return false;
                }
            }

        }else{
            if(LCInOut.equalsIgnoreCase("in")){
                if(name != null
                   && LCName.equalsIgnoreCase(name)){
                    return true;
                }else{
                    return false;
                }
            }else{
                if(!LCName.equalsIgnoreCase(name)){
                    return true;
                }else{
                    return false;
                }
            }
        }
    }

    private String getName(GovernanceArtifact artifact) {
        String local = artifact.getQName().getLocalPart();
        if (local != null && !"".equals(local)) {
            if (local.contains("\\.")) {
                return local.substring(0, local.lastIndexOf("\\."));
            } else {
                return local;
            }
        }
        return local;
    }

    public ContentArtifactsBean listContentArtifacts(String mediaType)throws RegistryException{
        return listContentArtifactsByName(mediaType, null);
    }

    public ContentArtifactsBean listContentArtifactsByLC(String mediaType, String LCName, String LCState, String LCInOut, String LCStateInOut)throws RegistryException{
        RegistryUtils.recordStatistics();
        ContentArtifactsBean bean = new ContentArtifactsBean();
        UserRegistry registry = (UserRegistry)getGovernanceUserRegistry();
        String[] paths;
        try {
            paths = GovernanceUtils.findGovernanceArtifacts(mediaType, registry);
        } catch (RegistryException e) {
            log.error("An error occurred while obtaining the list of artifacts.", e);
            paths = new String[0];
        }
        String[] names = new String[paths.length];
        String[] namespaces = new String[paths.length];
        boolean[] canDelete = new boolean[paths.length];
        String[] lifecycleName = new String[paths.length];
        String[] lifecycleState = new String[paths.length];
        for(int i = 0; i < paths.length; i++){
            GovernanceArtifact artifact =
                    GovernanceUtils.retrieveGovernanceArtifactByPath(registry, paths[i]);
            if (!lcMatches(artifact, LCName, LCState, LCInOut, LCStateInOut)) {
                continue;
            }
            bean.increment();
            names[i] = artifact.getQName().getLocalPart();
            namespaces[i] = artifact.getQName().getNamespaceURI();
            lifecycleName[i] = artifact.getLifecycleName();
            lifecycleState[i] = artifact.getLifecycleState();
            if (registry.getUserRealm() != null && registry.getUserName() != null) {
                try {
                    canDelete[i] =
                            registry.getUserRealm().getAuthorizationManager().isUserAuthorized(
                                    registry.getUserName(),
                                    RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + paths[i],
                                    ActionConstants.DELETE);
                } catch (UserStoreException ignored) {
                }
            }
        }
        bean.setName(names);
        bean.setNamespace(namespaces);
        bean.setPath(paths);
        bean.setCanDelete(canDelete);
        bean.setLCName(lifecycleName);
        bean.setLCState(lifecycleState);
        return bean.getSize() > 1 ? sortArtifactsByName(bean) : bean;
    }

    public ContentArtifactsBean listContentArtifactsByName(String mediaType, String criteria)
            throws RegistryException{
        RegistryUtils.recordStatistics();
        ContentArtifactsBean bean = new ContentArtifactsBean();
        UserRegistry registry = (UserRegistry)getGovernanceUserRegistry();
        String[] paths;
        try {
            paths = GovernanceUtils.findGovernanceArtifacts(mediaType, registry);
        } catch (RegistryException e) {
            log.error("An error occurred while obtaining the list of artifacts.", e);
            paths = new String[0];
        }
        String[] names = new String[paths.length];
        String[] namespaces = new String[paths.length];
        boolean[] canDelete = new boolean[paths.length];
        String[] lifecycleName = new String[paths.length];
        String[] lifecycleState = new String[paths.length];
        for(int i = 0; i < paths.length; i++){
            GovernanceArtifact artifact =
                    GovernanceUtils.retrieveGovernanceArtifactByPath(registry, paths[i]);
            if (criteria != null && !criteria.equals("") && !nameMatches(artifact, criteria)) {
                continue;
            }
            bean.increment();
            names[i] = artifact.getQName().getLocalPart();
            namespaces[i] = artifact.getQName().getNamespaceURI();
            lifecycleName[i] = artifact.getLifecycleName();
            lifecycleState[i] = artifact.getLifecycleState();
            if (registry.getUserRealm() != null && registry.getUserName() != null) {
                try {
                    canDelete[i] =
                            registry.getUserRealm().getAuthorizationManager().isUserAuthorized(
                                    registry.getUserName(),
                                    RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + paths[i],
                                    ActionConstants.DELETE);
                } catch (UserStoreException ignored) {
                }
            }
        }
        bean.setName(names);
        bean.setNamespace(namespaces);
        bean.setPath(paths);
        bean.setCanDelete(canDelete);
        bean.setLCName(lifecycleName);
        bean.setLCState(lifecycleState);
        return bean.getSize() > 1 ? sortArtifactsByName(bean) : bean;
    }

    private static ContentArtifactsBean sortArtifactsByName(ContentArtifactsBean bean) {
        List<ArtifactBean> temp = new LinkedList<ArtifactBean>();
        for (int i = 0; i < bean.getSize(); i++) {
            ArtifactBean artifact = new ArtifactBean();
            artifact.setValuesA(new String[]{bean.getName()[i], bean.getNamespace()[i]});
            artifact.setPath(bean.getPath()[i]);
            artifact.setCanDelete(bean.getCanDelete()[i]);
            artifact.setLCName(bean.getLCName()[i]);
            artifact.setLCState(bean.getLCState()[i]);
            temp.add(artifact);
        }
        ArtifactBean[] artifacts = sortArtifactsByName(temp.toArray(new ArtifactBean[temp.size()]));
        for (int i = 0; i < bean.getSize(); i++) {
            bean.getName()[i] = artifacts[i].getValuesA()[0];
            bean.getNamespace()[i] = artifacts[i].getValuesA()[1];
            bean.getPath()[i] = artifacts[i].getPath();
            bean.getCanDelete()[i] = artifacts[i].getCanDelete();
            bean.getLCName()[i] = artifacts[i].getLCName();
            bean.getLCState()[i] = artifacts[i].getLCState();
        }
        return bean;
    }

    public ArtifactsBean listArtifacts(String key, String criteria) {
        RegistryUtils.recordStatistics(key, criteria);
        UserRegistry governanceRegistry = (UserRegistry) getGovernanceUserRegistry();
        ArtifactsBean bean = new ArtifactsBean();
        try {
            final GovernanceArtifactConfiguration configuration =
                    GovernanceUtils.findGovernanceArtifactConfiguration(key, getRootRegistry());

            GenericArtifactManager manager = new GenericArtifactManager(governanceRegistry,
                    configuration.getMediaType(), configuration.getArtifactNameAttribute(),
                    configuration.getArtifactNamespaceAttribute(),
                    configuration.getArtifactElementRoot(),
                    configuration.getArtifactElementNamespace(),
                    configuration.getPathExpression(),
                    configuration.getRelationshipDefinitions());
            final GenericArtifact referenceArtifact;
            if (criteria != null) {
                XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(
                        new StringReader(criteria));
                referenceArtifact = manager.newGovernanceArtifact(
                        new StAXOMBuilder(reader).getDocumentElement());
            } else {
                referenceArtifact = null;
            }
            GenericArtifactFilter artifactFilter = new GenericArtifactFilter() {

                public boolean matches(GenericArtifact artifact) throws GovernanceException {
                    if (referenceArtifact == null) {
                        return true;
                    }
                    String[] keys = referenceArtifact.getAttributeKeys();
                    boolean defaultNameMatched = false;
                    boolean defaultNamespaceMatched = false;

                    for (String key : keys) {
                        if ("operation".equals(key)) {
                            // this is a special case
                            continue;
                        }
                        if (key.toLowerCase().contains("count")) {
                            // we ignore the count.
                            continue;
                        }
                        String[] referenceValues = referenceArtifact.getAttributes(key);
                        if (referenceValues == null) {
                            continue;
                        }
                        else {
                            if(!defaultNameMatched &&
                                    key.equals(configuration.getArtifactNameAttribute()) &&
                                    GovernanceConstants.DEFAULT_SERVICE_NAME.
                                    equalsIgnoreCase(referenceArtifact.getAttribute(
                                            configuration.getArtifactNameAttribute()))) {
                                defaultNameMatched = true;
                                continue;
                            }

                            if(!defaultNamespaceMatched &&
                                    key.equals(configuration.getArtifactNamespaceAttribute()) &&
                                    GovernanceConstants.DEFAULT_NAMESPACE.
                                    equals(referenceArtifact.getAttribute(
                                            configuration.getArtifactNamespaceAttribute()))){
                                defaultNamespaceMatched = true;
                                continue;
                            }
                        }
                        // all the valid keys should be satisfied..
                        String[] realValues = artifact.getAttributes(key);
                        if (realValues != null) {
                            boolean satisfied = false; // either one of value should be satisfied.
                            for (String referenceValue : referenceValues) {
                                satisfied = false;
                                for (String realValue : realValues) {
                                    if (satisfied) {
                                        continue;
                                    }
                                    try {
                                        if (realValue.toLowerCase().contains(
                                                referenceValue.toLowerCase()) ||
                                                realValue.matches(referenceValue)) {
                                            satisfied = true;
                                        }
                                    } catch (Exception e) {
                                        String msg = "Error in performing the regular expression " +
                                                "matches for: " + referenceValue + ".";
                                        throw new GovernanceException(msg, e);
                                    }
                                }
                                if (!satisfied) {
                                    if (log.isDebugEnabled()) {
                                        String msg = "key: " + key +
                                                " is not satisfied by the service: " +
                                                artifact.getQName() + ".";
                                        log.debug(msg);
                                    }
                                    return false;
                                }
                            }
                        } else {
                            return false;
                        }

                    }
                    return true;
                }
            };
            bean.setNames(configuration.getNamesOnListUI());
            bean.setTypes(configuration.getTypesOnListUI());
            bean.setKeys(configuration.getKeysOnListUI());
            String[] expressions = configuration.getExpressionsOnListUI();
            String[] keys = configuration.getKeysOnListUI();
            GovernanceUtils.loadGovernanceArtifacts((UserRegistry) getGovernanceUserRegistry());
            GenericArtifact[] artifacts = manager.findGenericArtifacts(artifactFilter);
            if (artifacts != null) {
                List<ArtifactBean> artifactBeans = new LinkedList<ArtifactBean>();
                for (GenericArtifact artifact : artifacts) {
                    int kk=0;
                    ArtifactBean artifactBean = new ArtifactBean();
                    List<String> paths = new ArrayList<String>();
                    List<String> values = new ArrayList<String>();
                    String path =
                            RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + ((GenericArtifactImpl) artifact).getArtifactPath();
                    artifactBean.setPath(path);
                    for(int i=0;i<expressions.length;i++){
                        if (expressions[i] != null) {
                            if (expressions[i].contains("@{storagePath}") && ((GenericArtifactImpl) artifact).getArtifactPath() != null) {
                                paths.add(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                        GovernanceUtils
                                                .getPathFromPathExpression(expressions[i], artifact,
                                                        ((GenericArtifactImpl) artifact).getArtifactPath()));
                            } else {
                                if("link".equals(bean.getTypes()[i])){
                                    paths.add(GovernanceUtils
                                            .getPathFromPathExpression(expressions[i], artifact,
                                                    configuration.getPathExpression()));
                                } else {
                                    paths.add(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                            GovernanceUtils
                                                    .getPathFromPathExpression(expressions[i], artifact,
                                                            configuration.getPathExpression()));
                                }
                            }
                        } else {
                            paths.add("");
                        }
                    }
                    artifactBean.setValuesB(paths.toArray(new String[paths.size()]));
                    for (String keyForValue : keys) {
                        if (keyForValue != null) {
                            values.add(artifact.getAttribute(keyForValue));
                        } else {
                            values.add("");
                        }
                    }
                    artifactBean.setValuesA(values.toArray(new String[values.size()]));
                    artifactBean.setCanDelete(
                            governanceRegistry.getUserRealm().getAuthorizationManager()
                                    .isUserAuthorized(governanceRegistry.getUserName(),
                                            path, ActionConstants.DELETE));
                    artifactBean.setLCName(((GenericArtifactImpl) artifact).getLcName());
                    artifactBean.setLCState(((GenericArtifactImpl) artifact).getLcState());
                    artifactBeans.add(artifactBean);
                }
                bean.setArtifacts(sortArtifactsByName(
                        artifactBeans.toArray(new ArtifactBean[artifactBeans.size()])));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while obtaining the list of artifacts.", e);
        }
        return bean;
    }


    public ArtifactsBean listArtifactsByLC(String key, final String LCName, final String LCState, final String LCInOut, final String LCStateInOut) {
        RegistryUtils.recordStatistics(key);
        UserRegistry governanceRegistry = (UserRegistry) getGovernanceUserRegistry();
        ArtifactsBean bean = new ArtifactsBean();
        try {
            GovernanceArtifactConfiguration configuration =
                    GovernanceUtils.findGovernanceArtifactConfiguration(key, getRootRegistry());

            GenericArtifactManager manager = new GenericArtifactManager(governanceRegistry,
                                                                        configuration.getMediaType(), configuration.getArtifactNameAttribute(),
                                                                        configuration.getArtifactNamespaceAttribute(),
                                                                        configuration.getArtifactElementRoot(),
                                                                        configuration.getArtifactElementNamespace(),
                                                                        configuration.getPathExpression(),
                                                                        configuration.getRelationshipDefinitions());

            GenericArtifact[] artifacts =manager.findGenericArtifacts(new GenericArtifactFilter() {
                public boolean matches(GenericArtifact genericArtifact) throws GovernanceException {

                    String name = genericArtifact.getLifecycleName();
                    String state = genericArtifact.getLifecycleState();

                    if(!LCState.equalsIgnoreCase("")){
                        if(LCInOut.equalsIgnoreCase("in") && LCStateInOut.equalsIgnoreCase("in")){
                            if(name != null
                               && state != null && LCState.equalsIgnoreCase(state)
                               && LCName.equalsIgnoreCase(name)){
                                return true;
                            }else{
                                return false;
                            }
                        } else if(LCInOut.equalsIgnoreCase("in") && !LCStateInOut.equalsIgnoreCase("in")){
                            if(name != null
                               && state != null && !LCState.equalsIgnoreCase(state)
                               && LCName.equalsIgnoreCase(name)){
                                return true;
                            }else{
                                return false;
                            }
                        } else if(!LCInOut.equalsIgnoreCase("in") && LCStateInOut.equalsIgnoreCase("in")){
                            if(name != null
                               && state != null && LCState.equalsIgnoreCase(state)
                               && !LCName.equalsIgnoreCase(name)){
                                return true;
                            }else{
                                return false;
                            }
                        } else {
                            if(name != null
                               && state != null && !LCState.equalsIgnoreCase(state)
                               && !LCName.equalsIgnoreCase(name)){
                                return true;
                            }else{
                                return false;
                            }
                        }

                    }else{
                        if(LCInOut.equalsIgnoreCase("in")){
                            if(name != null
                               && LCName.equalsIgnoreCase(name)){
                                return true;
                            }else{
                                return false;
                            }
                        }else{
                            if(!LCName.equalsIgnoreCase(name)){
                                return true;
                            }else{
                                return false;
                            }
                        }
                    }
                }
            });

            bean.setNames(configuration.getNamesOnListUI());
            bean.setTypes(configuration.getTypesOnListUI());
            bean.setKeys(configuration.getKeysOnListUI());
            String[] expressions = configuration.getExpressionsOnListUI();
            String[] keys = configuration.getKeysOnListUI();

            if (artifacts != null) {
                List<ArtifactBean> artifactBeans = new LinkedList<ArtifactBean>();
                for (GenericArtifact artifact : artifacts) {
                    ArtifactBean artifactBean = new ArtifactBean();
                    List<String> paths = new ArrayList<String>();
                    List<String> values = new ArrayList<String>();
                    String path =
                            RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + artifact.getPath();
                    artifactBean.setPath(path);
                    for (String expression : expressions) {
                        if (expression != null) {
                            if (expression.contains("@{storagePath}") && artifact.getPath() != null) {
                                paths.add(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                          GovernanceUtils
                                                  .getPathFromPathExpression(expression, artifact,
                                                                             artifact.getPath()));
                            } else {
                                paths.add(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                          GovernanceUtils
                                                  .getPathFromPathExpression(expression, artifact,
                                                                             configuration.getPathExpression()));
                            }
                        } else {
                            paths.add("");
                        }
                    }
                    artifactBean.setValuesB(paths.toArray(new String[paths.size()]));
                    for (String keyForValue : keys) {
                        if (keyForValue != null) {
                            values.add(artifact.getAttribute(keyForValue));
                        } else {
                            values.add("");
                        }
                    }
                    artifactBean.setValuesA(values.toArray(new String[values.size()]));
                    artifactBean.setCanDelete(
                            governanceRegistry.getUserRealm().getAuthorizationManager()
                                    .isUserAuthorized(governanceRegistry.getUserName(),
                                                      path, ActionConstants.DELETE));
                    artifactBean.setLCName(artifact.getLifecycleName());
                    artifactBean.setLCState(artifact.getLifecycleState());
                    artifactBeans.add(artifactBean);
                }
                bean.setArtifacts(artifactBeans.toArray(new ArtifactBean[artifactBeans.size()]));

            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while obtaining the list of artifacts.", e);
        }
        return bean;
    }


    private static ArtifactBean[] sortArtifactsByName(ArtifactBean[] artifacts) {
        Arrays.sort(artifacts, new Comparator<ArtifactBean>()  {
            public int compare(ArtifactBean se1, ArtifactBean se2) {
                int res = 0;
                for (int i = 0; i < se1.getValuesA().length; i++) {
                    String val1 = se1.getValuesA()[i];
                    String val2 = se2.getValuesA()[i];
                    if (val1 != null && !val1.equals("")) {
                        if (val1.matches(
                                org.wso2.carbon.registry.extensions.utils.CommonConstants.SERVICE_VERSION_REGEX)) {
                            res = StringComparatorUtil.compare(val1, val2);
                        } else {
                            res = val1.compareToIgnoreCase(val2);
                        }
                        if (res != 0) {
                            return res;
                        }
                    }
                }
                return res;
            }
        });
        return artifacts;
    }

    public String editArtifact(String path, String key, String info, String lifecycleAttribute)
            throws RegistryException {
        RegistryUtils.recordStatistics(path, key, info, lifecycleAttribute);
        Registry registry = getGovernanceUserRegistry();
        if (RegistryUtils.isRegistryReadOnly(registry.getRegistryContext())) {
            return null;
        }
        try {
            XMLStreamReader reader =
                    XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(info));

            GovernanceArtifactConfiguration configuration =
                    GovernanceUtils.findGovernanceArtifactConfiguration(key, getRootRegistry());

            GenericArtifactManager manager = new GenericArtifactManager(registry,
                    configuration.getMediaType(), configuration.getArtifactNameAttribute(),
                    configuration.getArtifactNamespaceAttribute(),
                    configuration.getArtifactElementRoot(),
                    configuration.getArtifactElementNamespace(),
                    configuration.getPathExpression(),
                    configuration.getRelationshipDefinitions());
            GenericArtifact artifact = manager.newGovernanceArtifact(
                    new StAXOMBuilder(reader).getDocumentElement());
            String currentPath;
            if (path != null && path.length() > 0) {
                currentPath = path.substring(
                        RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH.length());
            } else {
                currentPath = GovernanceUtils.getPathFromPathExpression(
                        configuration.getPathExpression(), artifact);
            }
            if (registry.resourceExists(currentPath)) {
                GovernanceArtifact oldArtifact = GovernanceUtils
                        .retrieveGovernanceArtifactByPath(registry, currentPath);
                if (!(oldArtifact instanceof GenericArtifact)) {
                    String msg = "The updated path is occupied by a non-generic artifact. path: " +
                            currentPath + ".";
                    log.error(msg);
                    throw new Exception(msg);
                }
                for (String attributeKey : artifact.getAttributeKeys()) {
                    oldArtifact.setAttributes(attributeKey, artifact.getAttributes(attributeKey));
                }

                //validates deleted attributes in update
                for (String attributeKey : oldArtifact.getAttributeKeys()) {
                    if(oldArtifact.getAttributes(attributeKey) != null
                            && artifact.getAttributes(attributeKey) == null) {
                    oldArtifact.removeAttribute(attributeKey);
                    }
                }

                artifact = (GenericArtifact) oldArtifact;
                List<Map> validAttr = configuration.getValidationAttributes();
                manager.validateArtifact(artifact, validAttr);
                manager.updateGenericArtifact(artifact);
            } else {
                List<Map> validAttr = configuration.getValidationAttributes();
                manager.validateArtifact(artifact, validAttr);
                manager.addGenericArtifact(artifact);
            }
            if (lifecycleAttribute != null && !lifecycleAttribute.equals("null")) {
                String lifecycle = artifact.getAttribute(lifecycleAttribute);
                artifact.attachLifecycle(lifecycle);
            }
            return RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + artifact.getPath();
        } catch (Exception e) {
            String msg = "Unable to edit artifact. ";
            if (e instanceof RegistryException) {
                throw (RegistryException) e;
            } else if (e instanceof OMException) {
                msg += "Unexpected character found in input-field name.";
                log.error(msg, e);
                throw new RegistryException(msg, e);
            }
            throw new RegistryException(msg + (e.getCause() instanceof SQLException ? "" :
                    e.getCause().getMessage()), e);
        }
    }

    public String getArtifactContent(String path) throws RegistryException {
        Registry registry = getGovernanceUserRegistry();
        // resource path is created to make sure the version page doesn't give null values
        if (!registry.resourceExists(new ResourcePath(path).getPath())) {
            return null;
        }
        return RegistryUtils.decodeBytes((byte[]) registry.get(path).getContent());
    }

    public String getArtifactUIConfiguration(String key) throws RegistryException {
        try {
            Registry registry = getConfigSystemRegistry();
            return RegistryUtils.decodeBytes((byte[]) registry.get(GOVERNANCE_ARTIFACT_CONFIGURATION_PATH + key)
                    .getContent());
        } catch (Exception e) {
            log.error("An error occurred while obtaining configuration", e);
            return null;
        }
    }

    public boolean setArtifactUIConfiguration(String key, String update) throws RegistryException {
        Registry registry = getConfigSystemRegistry();
        if (RegistryUtils.isRegistryReadOnly(registry.getRegistryContext())) {
            return false;
        }
        try {
            Util.validateOMContent(Util.buildOMElement(update));

            String path = GOVERNANCE_ARTIFACT_CONFIGURATION_PATH + key;
            if(registry.resourceExists(path)) {
            Resource resource = registry.get(path);
            resource.setContent(update);
            registry.put(path, resource);
            }
            return true;
        } catch (Exception e) {
            log.error("An error occurred while saving configuration", e);
            return false;
        }
    }

    public boolean canChange(String path) throws Exception {
        UserRegistry registry = (UserRegistry) getRootRegistry();
        if (registry.getUserName() != null && registry.getUserRealm() != null) {
            if (registry.getUserRealm().getAuthorizationManager().isUserAuthorized(
                    registry.getUserName(), path, ActionConstants.PUT)) {
                Resource resource = registry.get(path);
                String property = resource.getProperty(
                        CommonConstants.RETENTION_WRITE_LOCKED_PROP_NAME);
                return property == null || !Boolean.parseBoolean(property) ||
                        registry.getUserName().equals(
                                resource.getProperty(CommonConstants.RETENTION_USERNAME_PROP_NAME));

            }
        }
        return false;
    }

    /* get available aspects */
    public String[] getAvailableAspects() throws Exception {
        return GovernanceUtils.getAvailableAspects();
    }


    public ArtifactsBean listArtifactsByName(String key, final String name) {
        RegistryUtils.recordStatistics(key);
        UserRegistry governanceRegistry = (UserRegistry) getGovernanceUserRegistry();
        ArtifactsBean bean = new ArtifactsBean();
        try {
            GovernanceArtifactConfiguration configuration =
                    GovernanceUtils.findGovernanceArtifactConfiguration(key, getRootRegistry());

            GenericArtifactManager manager = new GenericArtifactManager(governanceRegistry,
                    configuration.getMediaType(), configuration.getArtifactNameAttribute(),
                    configuration.getArtifactNamespaceAttribute(),
                    configuration.getArtifactElementRoot(),
                    configuration.getArtifactElementNamespace(),
                    configuration.getPathExpression(),
                    configuration.getRelationshipDefinitions());

            GenericArtifact[] artifacts =manager.findGenericArtifacts(new GenericArtifactFilter() {
                public boolean matches(GenericArtifact genericArtifact) throws GovernanceException {
                    String local = genericArtifact.getQName().getLocalPart();
                    if (local != null
                            && name != null
                            && !"".equals(name)
                            && local.contains(name)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });

            bean.setNames(configuration.getNamesOnListUI());
            bean.setTypes(configuration.getTypesOnListUI());
            String[] expressions = configuration.getExpressionsOnListUI();
            String[] keys = configuration.getKeysOnListUI();

            if (artifacts != null) {
                List<ArtifactBean> artifactBeans = new LinkedList<ArtifactBean>();
                for (GenericArtifact artifact : artifacts) {
                    ArtifactBean artifactBean = new ArtifactBean();
                    List<String> paths = new ArrayList<String>();
                    List<String> values = new ArrayList<String>();
                    String path =
                            RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH + artifact.getPath();
                    artifactBean.setPath(path);
                    for (String expression : expressions) {
                        if (expression != null) {
                            if (expression.contains("@{storagePath}") && artifact.getPath() != null) {
                                paths.add(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                        GovernanceUtils
                                                .getPathFromPathExpression(expression, artifact,
                                                        artifact.getPath()));
                            } else {
                                paths.add(RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
                                        GovernanceUtils
                                                .getPathFromPathExpression(expression, artifact,
                                                        configuration.getPathExpression()));
                            }
                        } else {
                            paths.add("");
                        }
                    }
                    artifactBean.setValuesB(paths.toArray(new String[paths.size()]));
                    for (String keyForValue : keys) {
                        if (keyForValue != null) {
                            values.add(artifact.getAttribute(keyForValue));
                        } else {
                            values.add("");
                        }
                    }
                    artifactBean.setValuesA(values.toArray(new String[values.size()]));
                    artifactBean.setCanDelete(
                            governanceRegistry.getUserRealm().getAuthorizationManager()
                                    .isUserAuthorized(governanceRegistry.getUserName(),
                                            path, ActionConstants.DELETE));
                    artifactBean.setLCName(artifact.getLifecycleName());
                    artifactBean.setLCState(artifact.getLifecycleState());
                    artifactBeans.add(artifactBean);
                }
                bean.setArtifacts(sortArtifactsByName(
                        artifactBeans.toArray(new ArtifactBean[artifactBeans.size()])));

            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while obtaining the list of artifacts.", e);
        }
        return bean;
    }

    public boolean addRXTResource(String rxtConfig,String path) throws RegistryException {
        //TODO record stats
         boolean result  = GenericArtifactUtil.addRXTResource(path,rxtConfig, getGovernanceUserRegistry());
            setArtifactUIConfiguration(GenericArtifactUtil.getRXTKeyFromContent(rxtConfig),
                    GenericArtifactUtil.getArtifactUIContentFromConfig(rxtConfig));

        return result;
    }

    public String getRxtAbsPathFromRxtName(String rxtName) {
        return new StringBuilder(GovernanceConstants.RXT_CONFIGS_PATH).
                append("/").
                append(rxtName).
                append(".rxt").toString();
    }

    public String getArtifactViewRequestParams(String key) throws Exception {
        return GenericArtifactUtil.getArtifactViewRequestParams(
                getArtifactContent(new StringBuilder(GenericArtifactUtil.REL_RXT_BASE_PATH).
                append("/").
                append(key).
                append(".rxt").toString()));
    }


    /**
     * return all states of a given LC
     * @param LCName
     * @return
     */
    public String[] getAllLifeCycleState(String LCName){
        String[] LifeCycleStates = null;
        try {
            LifeCycleStates = CommonUtil.getAllLifeCycleStates(getRootRegistry(), LCName);
        } catch (org.wso2.carbon.registry.api.RegistryException e) {
            log.error("An error occurred while obtaining the list of sates in "+LCName, e);
        }
        return LifeCycleStates;
    }


}
