package org.wso2.carbon.appfactory.core;

import javax.activation.DataHandler;

import org.wso2.carbon.appfactory.common.AppFactoryException;

/**
 * Listens to the events of the BuildDriver
 */
public interface BuildDriverListener {

    /**
     *
     * @param applicationId
     * @param version
     * @param revision
     * @param data
     * @param fileName
     * @throws AppFactoryException
     */
    public void onBuildSuccessful(String applicationId, String version, String revision, String buildId, DataHandler data, String fileName)
            throws AppFactoryException;

    /**
     * Called upon build failure
     * 
     * @param applicationId
     * @param version
     * @param revision
     * @param revision
     * @param errorMessage
     * @throws AppFactoryException
     */
    public void onBuildFailure(String applicationId, String version, String revision, String buildId,
                               String errorMessage) throws AppFactoryException;

}
