package org.wso2.carbon.deployment.synchronizer.git.internal;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.repository.information.RepositoryInformationServiceException;
import org.wso2.carbon.adc.repository.information.RepositoryInformationServiceStub;

import java.rmi.RemoteException;

/**
 * Client for ReposioryInformationService. Used to get the git repo URL
 * for a given tenant Id and the cartridge type (short name)
 */
public class RepositoryInformationServiceClient {

    private static final Log log = LogFactory.getLog(RepositoryInformationServiceClient.class);
    private RepositoryInformationServiceStub repositoryInformationServiceStub;

    /**
     * Constructor
     *
     * @param epr end point reference for the RepositoryInformationServiceClient
     *
     * @throws AxisFault
     */
    public RepositoryInformationServiceClient (String epr) throws AxisFault {

        try {
            repositoryInformationServiceStub = new RepositoryInformationServiceStub(epr);

        } catch (AxisFault axisFault) {
            String errorMsg = "Repository Information Service client initialization failed " + axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new AxisFault(errorMsg, axisFault);
        }
    }

    /**
     * Retrieves the Git Repository URL for the tenant and cartridge type
     *
     * @param tenantId id of the tenant
     * @param cartridgeType cartridge type tenant is subscribed to
     *
     * @return valid repository url if exists
     *
     * @throws Exception
     */
    public String getGitRepositoryUrl (int tenantId, String cartridgeType) throws Exception {

        String repoUrl = null;
        try {
            repoUrl = repositoryInformationServiceStub.getRepositoryUrl(tenantId, cartridgeType).trim();

        } catch (RemoteException e) {
            log.error("Repository Information Service invocation failed in querying repo url");
            e.printStackTrace();

        } catch (RepositoryInformationServiceException e) {
            log.error("Git repository url querying failed for tenant " + tenantId);
            e.printStackTrace();
        }
        return repoUrl;
    }

    /**
     * Retrieves the repository url, username and password in json format
     *
     * @param tenantId id of the tenant
     * @param cartridgeType cartridge type tenant is subscribed to
     *
     * @return json format String with repository url, username and password
     *
     * @throws Exception
     */
    public String getJsonRepositoryInformation (int tenantId, String cartridgeType) throws Exception {

        String jsonRepoInfomation = "";
        try {
            jsonRepoInfomation = repositoryInformationServiceStub.getRepositoryCredentials(tenantId, cartridgeType);

        } catch (RemoteException e) {
            log.error("Repository Information Service invocation failed in querying repo information");
            e.printStackTrace();

        } catch (RepositoryInformationServiceException e) {
            log.error("Git repository information querying failed for tenant " + tenantId);
            e.printStackTrace();
        }
        return jsonRepoInfomation;
    }
}
