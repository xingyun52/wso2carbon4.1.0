package org.wso2.carbon.appfactory.core;

import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.dto.API;

public interface APIIntegration {

    /**
     * Method to create an application in the API Manager
     *
     * @param applicationId the id of the application to be created.
     * @return whether the application creation was successful or not
     */
    public boolean createApplication(String applicationId) throws AppFactoryException;

    /**
     * Method to remove an application from the API Manager
     *
     * @param applicationID the id of the application to be removed
     * @return whether the application removal was successful or not
     */
    public boolean removeApplication(String applicationID) throws AppFactoryException;

    /**
     * Method to add an API to an application
     *
     * @param applicationId the id of the application
     * @param apiName       the name of the API
     * @param apiVersion    the version of the API
     * @param apiProvider   the name of the API provider
     * @return whether the operation was successful or not
     */
    public boolean addAPIsToApplication(String applicationId, String apiName, String apiVersion, String apiProvider) throws AppFactoryException;

    /**
     * Method to get all the APIs of an application
     *
     * @param applicationId the id of the application
     * @return array of API info data objects which contain the name,version and the provider of the API
     */
    public API[] getAPIsOfApplication(String applicationId) throws AppFactoryException;

    /**
     * Method to remove an API from an application
     *
     * @param applicationId the id of the application
     * @param apiName       the name of the API
     * @param apiVersion    the version of the API
     * @param apiProvider   the name of the API provider
     * @return an array of API data objects
     */
    public boolean removeAPIFromApplication(String applicationId, String apiName, String apiVersion, String apiProvider) throws AppFactoryException;

    /**
     * Method to get the details of a single API
     *
     * @param name     the name of the API
     * @param version  the version of the API
     * @param provider the provider of the API
     * @return an API data object
     */
    public API getAPIInformation(String name, String version, String provider) throws AppFactoryException;
}
