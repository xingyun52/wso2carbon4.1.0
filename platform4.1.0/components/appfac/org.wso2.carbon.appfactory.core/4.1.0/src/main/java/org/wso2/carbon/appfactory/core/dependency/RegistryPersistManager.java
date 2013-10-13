package org.wso2.carbon.appfactory.core.dependency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.util.Constants;
import org.wso2.carbon.appfactory.core.util.DependencyUtil;
import org.wso2.carbon.appfactory.core.dto.Dependency;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.utils.RegistryUtils;

public class RegistryPersistManager {
    private static final Log log = LogFactory.getLog(RegistryPersistManager.class);

    public static void addToRegistry(String stage, String name, Object value, String description, int tenantId)
            throws AppFactoryException {

        try {
            Registry governanceRegistry = DependencyUtil.getGovernanceRegistry(tenantId);

//            We are not caring whether there is a resource of the given path.
//            if there was a resource, then we are overriding it.
            String dependenciesPath = DependencyUtil.getDependenciesPath(name);
            String finalPath = DependencyUtil.getMountedDependenciesPath(stage, dependenciesPath);

            Resource resource = governanceRegistry.newResource();
            resource.setContent(value);
            resource.setDescription(description);

            governanceRegistry.put(finalPath, resource);

//            We are also persisting the value to the non mount location.
//            That is to keep track of all the dependencies created.
//            The resource value does not count here. We only need to keep track of the resource name and description.
            if(!governanceRegistry.resourceExists(dependenciesPath)){
                governanceRegistry.put(dependenciesPath,resource);
            }
        } catch (RegistryException e) {
            String msg = "Unable to add the resource to registry";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }

    }


    public static void removeFromRegistry(String stage, String name, int tenantId)
            throws AppFactoryException {
        Registry governanceRegistry;
        try {

            governanceRegistry = DependencyUtil.getGovernanceRegistry(tenantId);

            String dependenciesPath = DependencyUtil.getDependenciesPath(name);
            String finalPath = DependencyUtil.getMountedDependenciesPath(stage, dependenciesPath);

            if (governanceRegistry.resourceExists(finalPath)) {
                governanceRegistry.delete(finalPath);
            }

//            Removing the non mounted one
            if(governanceRegistry.resourceExists(dependenciesPath)){
                governanceRegistry.delete(dependenciesPath);
            }
        } catch (RegistryException e) {
            String msg = "Unable to remove the resource from the registry";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }

    }

    /**
     * @return an array of the name and the description.
     * @tenantId the tenant Id of that application
     */
    public static Dependency[] getAllDependencyPaths(int tenantId) throws AppFactoryException {
        Registry governanceRegistry;

        Dependency[] dependencies = new Dependency[0];

        try {
            governanceRegistry = DependencyUtil.getGovernanceRegistry(tenantId);
            String dependencyCollectionPath = RegistryConstants.PATH_SEPARATOR + Constants.DEPENDENCIES_HOME;

            if(governanceRegistry.resourceExists(dependencyCollectionPath)){
                Resource dependencyParent = governanceRegistry.get(dependencyCollectionPath);

                if(dependencyParent instanceof Collection){
                    Collection collection = (Collection) dependencyParent;
                    String[] children =  collection.getChildren();

                    if(children == null){
                        log.warn("No resources were found as dependencies");
                        return dependencies;
                    }

                    dependencies = new Dependency[children.length];

                    for (int i = 0; i < children.length; i++) {
                        String childPath = children[i];
                        Resource child = governanceRegistry.get(childPath);

                        Dependency element = new Dependency();
                        element.setName(RegistryUtils.getResourceName(child.getPath()));
                        element.setDescription(child.getDescription());
                        element.setValue(DependencyUtil.getResourceContent(child));

                        dependencies[i] = element;
                    }
                }else{
                    log.warn("No resources were found as dependencies");
                }
            }

        } catch (RegistryException e) {
            String msg = "Unable to get the dependencies from registry";
            log.error(msg,e);
            throw new AppFactoryException(msg,e);
        }
        return dependencies;
    }

}
