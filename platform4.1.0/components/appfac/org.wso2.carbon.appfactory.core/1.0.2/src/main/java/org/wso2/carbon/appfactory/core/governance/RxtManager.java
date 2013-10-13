/*
 * Copyright 2005-2011 WSO2, Inc. (http://wso2.com)
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package org.wso2.carbon.appfactory.core.governance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.deploy.Artifact;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifactImpl;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

public class RxtManager {
	/*
	 * todo: this class was moved from appfactory.governance to appfactory.core
	 * to remove the
	 * dependancy from core to governance.
	 * appfactory.core should be refactored to store only necessary classes
	 */

	private static Log log = LogFactory.getLog(RxtManager.class);

	/**
	 * This method will add the given newValue as the value of the key,
	 * replacing the existing value
	 * 
	 * @param applicationId
	 *            the Id of the current application
	 * @param stage
	 *            stage of the current application
	 * @param version
	 *            version of the current application
	 * @param key
	 *            the attribute key that is been updated
	 * @param newValue
	 *            the new value of the attribute key
	 * @throws AppFactoryException
	 */
	public void updateAppVersionRxt(String applicationId, String stage, String version, String key,
	                                String newValue) throws AppFactoryException {
		GenericArtifactImpl artifact = getAppVersionArtifact(applicationId, stage, version);
		log.info("=============== updating rxt =============== key:" + key + " value:" + newValue);
		if (artifact == null) {
			String errorMsg =
			                  String.format("Unable to find appversion information for id : %s",
			                                applicationId);
			log.error(errorMsg);
			throw new AppFactoryException(errorMsg);
		}
		try {
			String currentVal = artifact.getAttribute(key);
			if (currentVal == null) {
				artifact.addAttribute(key, newValue);
			} else {
				artifact.setAttribute(key, newValue);
			}
			RegistryService registryService = ServiceHolder.getRegistryService();
			// RegistryService registryService =
			// ServiceReferenceHolder.getInstance().getRegistryService();
			UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
			GovernanceUtils.loadGovernanceArtifacts(userRegistry);
			GenericArtifactManager artifactManager =
			                                         new GenericArtifactManager(userRegistry,
			                                                                    "appversion");
			artifactManager.updateGenericArtifact(artifact);
		} catch (RegistryException e) {
			String errorMsg = "Error while updating the artifact " + applicationId;
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}
	}

	/**
	 * This method will append the given newValues as values for the key given
	 * 
	 * @param applicationId
	 *            the ID of the current application
	 * @param stage
	 *            the stage of the current application
	 * @param version
	 *            the version of the current application
	 * @param key
	 *            the attribute key that is been updated
	 * @param newValues
	 *            array of new values for the attribute key
	 * @throws AppFactoryException
	 */
	public void updateAppVersionRxt(String applicationId, String stage, String version, String key,
	                                String[] newValues) throws AppFactoryException {
		GenericArtifactImpl artifact = getAppVersionArtifact(applicationId, stage, version);
		log.info("=============== updating rxt =============== key:" + key + " with " +
		         newValues.length + " values");
		if (artifact == null) {
			String errorMsg =
			                  String.format("Unable to find appversion information for id : %s",
			                                applicationId);
			log.error(errorMsg);
			throw new AppFactoryException(errorMsg);
		}
		try {
			for (String value : newValues) {
				artifact.addAttribute(key, value);
			}

			RegistryService registryService = ServiceHolder.getRegistryService();
			UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
			GovernanceUtils.loadGovernanceArtifacts(userRegistry);
			GenericArtifactManager artifactManager =
			                                         new GenericArtifactManager(userRegistry,
			                                                                    "appversion");
			artifactManager.updateGenericArtifact(artifact);
		} catch (RegistryException e) {
			String errorMsg = "Error while updating the artifact " + applicationId;
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}
	}

	/**
	 * This method returns the stage of a given application version
	 * 
	 * @param applicationId
	 *            the ID of the current application
	 * @param appVersion
	 *            the version of the current application
	 * 
	 * @return the stage of the given application version
	 * @throws AppFactoryException
	 */
	public String getStage(String applicationId, String appVersion) throws AppFactoryException {
		String[] versionPaths = getVersionPaths(applicationId);
		// path to a version is in the structure .../<appid>/<lifecycle>/1.0.1 )
		if (versionPaths != null) {
			for (String path : versionPaths) {
				String[] s = path.trim().split(RegistryConstants.PATH_SEPARATOR);
				if (appVersion.equals(s[s.length - 1])) {
					// get the <lifecycle>
					return s[s.length - 2];
				}
			}
		}
		return null;
	}

	private String[] getVersionPaths(String applicationId) throws AppFactoryException {
		List<String> versionPaths = new ArrayList<String>();
		try {
			RegistryService registryService = ServiceHolder.getRegistryService();
			UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
			// child nodes of this will contains folders for all life cycles (
			// e.g. QA, Dev, Prod)
			Resource application =
			                       userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
			                                        RegistryConstants.PATH_SEPARATOR +
			                                        applicationId);

			if (application != null && application instanceof Collection) {

				// Contains paths to life cycles (.e.g .../<appid>/dev,
				// .../<appid>/qa , .../<appid>/prod )
				String[] definedLifeCyclePaths = ((Collection) application).getChildren();

				for (String lcPath : definedLifeCyclePaths) {

					Resource versionsInLCResource = userRegistry.get(lcPath);
					if (versionsInLCResource != null && versionsInLCResource instanceof Collection) {

						// contains paths to a versions (e.g.
						// .../<appid>/<lifecycle>/trunk,
						// .../<appid>/<lifecycle>/1.0.1 )
						Collections.addAll(versionPaths,
						                   ((Collection) versionsInLCResource).getChildren());
					}

				}

			}

		} catch (RegistryException e) {
			String errorMsg =
			                  String.format("Unable to load the application information for applicaiton id: %s",
			                                applicationId);
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}
		return versionPaths.toArray(new String[versionPaths.size()]);
	}

	/**
	 * @param applicationId
	 *            the ID of the current application
	 * @param stage
	 *            the stage of the current application
	 * @param version
	 *            the version of the current application
	 * @return generic artifact implementation of the artifact that matches the
	 *         given applicationId, stage and version
	 * @throws AppFactoryException
	 */
	private GenericArtifactImpl getAppVersionArtifact(String applicationId, String stage,
	                                                  String version) throws AppFactoryException {
		GenericArtifactImpl artifact;
		try {

			RegistryService registryService = ServiceHolder.getRegistryService();
			UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
			Resource resource =
			                    userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
			                                     RegistryConstants.PATH_SEPARATOR + applicationId +
			                                     RegistryConstants.PATH_SEPARATOR + stage +
			                                     RegistryConstants.PATH_SEPARATOR + version +
			                                     RegistryConstants.PATH_SEPARATOR + "appversion");
			GovernanceUtils.loadGovernanceArtifacts(userRegistry);
			GenericArtifactManager artifactManager =
			                                         new GenericArtifactManager(userRegistry,
			                                                                    "appversion");
			artifact = (GenericArtifactImpl) artifactManager.getGenericArtifact(resource.getUUID());

		} catch (RegistryException e) {
			String errorMsg =
			                  String.format("Unable to load the application information for applicaiton id: %s",
			                                applicationId);
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}

		return artifact;
	}

	/**
	 * @param applicationId
	 *            the ID of the current application
	 * @param stage
	 *            the stage of the current application
	 * @param version
	 *            the version of the current application
	 * @param key
	 *            the key is one of element what we need to get the value
	 * @return keyValue the keyValue is the returned value for the given key
	 * @throws AppFactoryException
	 */
	public String getAppVersionRxtValue(String applicationId, String stage, String version,
	                                    String key) throws AppFactoryException {
		GenericArtifactImpl artifact;
		String keyValue = null;
		try {
			artifact = getAppVersionArtifact(applicationId, stage, version);
			keyValue = artifact.getAttribute(key);

		} catch (RegistryException e) {
			String errorMsg =
			                  String.format("Unable to load the application information for applicaiton id: %s",
			                                applicationId);
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}

		return keyValue;
	}

	/**
	 * Retrieves artifact infomration related to all the versions of the given
	 * {@code applicationId}
	 * 
	 * @param applicationId
	 * @return list of {@link Artifact}
	 * @throws AppFactoryException
	 * @throws RegistryException
	 */
	public List<Artifact> getAppVersionRxtForApplication(final String applicationId)
	                                                                                throws AppFactoryException,
	                                                                                RegistryException {

		RegistryService registryService = ServiceHolder.getRegistryService();
		UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
		GovernanceUtils.loadGovernanceArtifacts(userRegistry);
		GenericArtifactManager artifactManager =
		                                         new GenericArtifactManager(userRegistry,
		                                                                    "appversion");
		final List<Artifact> artifactList = new ArrayList<Artifact>();
		List<String> versionUUID = new ArrayList<String>();

		Resource resource =
		                    userRegistry.get(AppFactoryConstants.REGISTRY_APPLICATION_PATH +
		                                     RegistryConstants.PATH_SEPARATOR + applicationId);
		if (resource instanceof Collection) {
			String[] appStageChildren = ((Collection) resource).getChildren();

			if (appStageChildren != null) {
				for (String appStageChild : appStageChildren) {
					if (!userRegistry.resourceExists(appStageChild)) {
						continue;
					}
					Resource stageChild = userRegistry.get(appStageChild);
					if (stageChild instanceof Collection) {
						// Here it contains all the app versions
						String[] versionChildren = ((Collection) stageChild).getChildren();
						if (versionChildren != null) {
							for (String versionChild : versionChildren) {
								if (!userRegistry.resourceExists(versionChild)) {
									continue;
								}
								Resource child = userRegistry.get(versionChild);
								if (child instanceof Collection) {
									String[] appVersionChildren =
									                              ((Collection) child).getChildren();
									if (appVersionChildren != null) {
										for (String appVersionChildPath : appVersionChildren) {
											if (!userRegistry.resourceExists(appVersionChildPath)) {
												continue;
											}
											// We use the resource UUID to fetch
											// info
											Resource appVersionChild =
											                           userRegistry.get(appVersionChildPath);
											if (!isCollection(appVersionChild)) {
												versionUUID.add(appVersionChild.getUUID());
											}
										}
									}
								}

							}
						}
					}
				}
			}
		}

		for (String uuid : versionUUID) {
			GenericArtifact paramGenericArtifact = artifactManager.getGenericArtifact(uuid);
			artifactList.add(getArtifactByGenericArtifact(paramGenericArtifact));
		}

		/*
		 * GenericArtifact[] artifacts =
		 * artifactManager.findGenericArtifacts(new GenericArtifactFilter() {
		 * 
		 * @Override
		 * public boolean matches(GenericArtifact paramGenericArtifact)
		 * throws GovernanceException {
		 * String attributeVal =
		 * paramGenericArtifact.getAttribute("appversion_key");
		 * if (attributeVal != null &&
		 * attributeVal.equals(applicationId)) {
		 * artifactList.add(getArtifactByGenericArtifact(paramGenericArtifact));
		 * return true;
		 * }
		 * return false;
		 * }
		 * 
		 * });
		 */

		return artifactList;
	}

	private boolean isCollection(Resource res) {
		return (res instanceof Collection);
	}

	private Artifact getArtifactByGenericArtifact(GenericArtifact paramGenericArtifact)
	                                                                                   throws GovernanceException {

		String applicationKey = paramGenericArtifact.getAttribute("appversion_key");
		String lastBuildStatus = paramGenericArtifact.getAttribute("appversion_LastBuildStatus");
		String version = paramGenericArtifact.getAttribute("appversion_version");

		String autoBuildStr = paramGenericArtifact.getAttribute("appversion_isAutoBuild");
		String autoDeployStr = paramGenericArtifact.getAttribute("appversion_isAutoDeploy");
		String lastDeployedId = paramGenericArtifact.getAttribute("appversion_lastdeployedid");

		boolean isAutoDeploy = (autoDeployStr == null) ? false : Boolean.valueOf(autoDeployStr);
		boolean isAutoBuild = autoBuildStr == null ? false : Boolean.valueOf(autoBuildStr);

		return new Artifact(applicationKey, lastBuildStatus, version, isAutoBuild, isAutoDeploy,
		                    lastDeployedId);
	}

	/**
	 * Generic method to add new registry artifact.
	 * @param rxt
	 * @param qname
	 * @param newValueMap - values to be saved
	 * @throws AppFactoryException
	 */
	public void addNewArtifact(String rxt, String qname, Map<String,String> newValueMap) throws AppFactoryException {
		RegistryService registryService = ServiceHolder.getRegistryService();
		try {
			UserRegistry registry = registryService.getGovernanceSystemRegistry();

			GovernanceUtils.loadGovernanceArtifacts(registry);
			GenericArtifactManager manager = new GenericArtifactManager(registry, rxt);
			GenericArtifact artifact =
			                           manager.newGovernanceArtifact(new QName(qname));
			
			Set<Entry<String, String>> newValueEntrySet = newValueMap.entrySet();

			for (Entry<String, String> newValues : newValueEntrySet) {
				artifact.addAttribute(newValues.getKey(), newValues.getValue());
			}
			manager.addGenericArtifact(artifact);

		} catch (RegistryException e) {
			String errorMsg = String.format("Unable to add new artifact to the rxt %s", rxt);
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}
	}
	
	/**
	 * Generic method to retrieve artifact.
	 * @param resourcePath
	 * @param rxt
	 * @return {@link GenericArtifact}
	 * @throws AppFactoryException
	 */
	public GenericArtifact getArtifact(String resourcePath, String rxt) throws AppFactoryException {
		
		RegistryService registryService = ServiceHolder.getRegistryService();
		try {
			UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();

			if (userRegistry.resourceExists(resourcePath)) {
				Resource resource = userRegistry.get(resourcePath);
				GovernanceUtils.loadGovernanceArtifacts(userRegistry);

				GenericArtifactManager artifactManager =
				                                         new GenericArtifactManager(userRegistry,
				                                                                    rxt);

				return artifactManager.getGenericArtifact(resource.getUUID());
			}
			return null;

		} catch (RegistryException e) {
			String errorMsg =
			                  String.format("Unable to load the artidact information for recource path: %s for rxt: %s",
			                                resourcePath, rxt);
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}
	}
	
	/**
	 * Returns saved values of the given {@code artifact} as a Map.
	 * @param artifact
	 * @return saved values as a {@link Map} 
	 * @throws AppFactoryException
	 */
	public Map<String, String> readArtifact(GenericArtifact artifact) throws AppFactoryException {
		try {
			String[] attributeKeys = artifact.getAttributeKeys();
			Map<String, String> readValues = new HashMap<String, String>();
			for (String key : attributeKeys) {
				readValues.put(key, artifact.getAttribute(key));
			}
			return readValues;
		} catch (RegistryException e) {
			String errorMsg = "Erro reading Artifact infromation";
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}
	}
	
	/**
	 * Generic method to update the given {@code updatableArtiafact} with new values provided by {@code newValueMap}
	 * @param updatableArtiafact
	 * @param rxt
	 * @param qname
	 * @param newValueMap
	 * @throws AppFactoryException
	 */
	public void updateExistingArtifact(GenericArtifact updatableArtiafact, String rxt, String qname,
	                                   Map<String, String> newValueMap) throws AppFactoryException {
		try {
			RegistryService registryService = ServiceHolder.getRegistryService();
			UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
			GovernanceUtils.loadGovernanceArtifacts(userRegistry);
			GenericArtifactManager artifactManager = new GenericArtifactManager(userRegistry, rxt);
			
			//FIXME: here update is done by first removing exising one and adding new one.
			// make it a real update.
			artifactManager.removeGenericArtifact(updatableArtiafact.getId());
			addNewArtifact(rxt, qname, newValueMap);
		} catch (RegistryException e) {
			String errorMsg =
			                  String.format("Error While updating existing resgistry artifact: %s",
			                                rxt);
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}		
		
	}
	
	/**
	 * Returns all the saved artifacts related to ETA.
	 * @param applicationKey
	 * @param stage
	 * @param version
	 * @return
	 * @throws AppFactoryException
	 */
	public List<GenericArtifact> getETAArtifacts(String applicationKey, String stage, String version)
	                                                                                                 throws AppFactoryException {
		RegistryService registryService = ServiceHolder.getRegistryService();
		try {
			UserRegistry userRegistry = registryService.getGovernanceSystemRegistry();
			GovernanceUtils.loadGovernanceArtifacts(userRegistry);
			
			String resourcePath = AppFactoryConstants.REGISTRY_APPLICATION_PATH +
            RegistryConstants.PATH_SEPARATOR + applicationKey +
            RegistryConstants.PATH_SEPARATOR + "eta" +
            RegistryConstants.PATH_SEPARATOR + stage +
            RegistryConstants.PATH_SEPARATOR + version;
			
			List<GenericArtifact> etaArtifacts = new ArrayList<GenericArtifact>();
			if(!userRegistry.resourceExists(resourcePath)){
				log.debug("No ETA information to load for applikation key -" + applicationKey + " stage -" + stage + " version -" + version ); 
				return etaArtifacts;
			}

			Resource resource = userRegistry.get(resourcePath);

			GenericArtifactManager artifactManager =
			                                         new GenericArtifactManager(userRegistry, "eta");

			if (resource instanceof Collection) {
				String[] userChildren = ((Collection) resource).getChildren();

				for (String userChild : userChildren) {
					if (!userRegistry.resourceExists(userChild)) {
						continue;
					}

					Resource user = userRegistry.get(userChild);

					if (user instanceof Collection) {
						String etaChild = ((Collection) user).getChildren()[0];

						Resource eta = userRegistry.get(etaChild);
						if (!isCollection(eta)) {
							etaArtifacts.add(artifactManager.getGenericArtifact(eta.getUUID()));
						}
					}

				}
			}
			return etaArtifacts;
		} catch (RegistryException e) {
			String errorMsg =
			                  String.format("Error While retrieving eta information for : %s version : %s",
			                                applicationKey, version);
			log.error(errorMsg, e);
			throw new AppFactoryException(errorMsg, e);
		}
	}
	
}
