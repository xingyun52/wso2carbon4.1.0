package org.wso2.carbon.appfactory.core.build;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.common.util.AppFactoryUtil;
import org.wso2.carbon.appfactory.common.util.NotificationSender;
import org.wso2.carbon.appfactory.core.ArtifactStorage;
import org.wso2.carbon.appfactory.core.BuildDriverListener;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.rmi.RemoteException;


public class DefaultBuildDriverListener implements BuildDriverListener {

    private static final Log log = LogFactory.getLog(DefaultBuildDriverListener.class);
    private static final String EVENT = "build";
    private static final String SUCCESS = "successful";
    private static final String FAILED = "failed";

    @Override
    public void onBuildSuccessful(String applicationId, String version, String revision, File file) {
        ArtifactStorage storage = ServiceHolder.getArtifactStorage();
        try {
            String fileName = file.getAbsolutePath();

             FileDataSource dataSource = new FileDataSource(fileName);
        DataHandler fileDataHandler = new DataHandler(dataSource);



			storage.storeArtifact(applicationId, version, revision, fileDataHandler, file.getName());
		} catch (AppFactoryException e) {
	        sendEventNotification(applicationId, EVENT, FAILED);
		}
        sendMessageToCreateArtifactCallback(applicationId, version, revision);
        sendEventNotification(applicationId, EVENT, SUCCESS);

    }

    @Override
    public void onBuildFailure(String applicationId, String version, String revision, String errorMessage)
            throws AppFactoryException {
        sendEventNotification(applicationId, EVENT, FAILED);
    }

    private void sendMessageToCreateArtifactCallback(final String applicationId, final String version, final String revision) {
        try {
            AppFactoryConfiguration configuration = ServiceHolder.getAppFactoryConfiguration();
            final String ARTIFACT_CREATE_EPR = configuration.getFirstProperty(AppFactoryConstants.BPS_SERVER_URL) + "ArtifactCreateCallbackService";
            AppFactoryUtil.sendNotification(applicationId, version, revision, ARTIFACT_CREATE_EPR, getPayload(applicationId, version, revision));
        } catch (XMLStreamException e) {
            log.error(e);
        }
    }

    private static OMElement getPayload(String applicationId, String version, String revision) throws XMLStreamException, javax.xml.stream.XMLStreamException {
        String payload = "<p:callbackMessgae xmlns:p=\"http://localhost:9763/services/ArtifactCreateCallbackService\"><p:applicationId>" + applicationId +
                "</p:applicationId><p:revision>" + revision + "</p:revision><p:version>" + version + "</p:version></p:callbackMessgae>";

        return new StAXOMBuilder(new ByteArrayInputStream(payload.getBytes())).getDocumentElement();
    }

    public void sendEventNotification(final String applicationId, final String event, final String result) {
        try {
            AppFactoryConfiguration configuration = ServiceHolder.getAppFactoryConfiguration();
            String serverUrl = configuration.getFirstProperty(AppFactoryConstants.APPFACTORY_SERVER_URL);
            NotificationSender notificationSender = new NotificationSender(serverUrl);
                notificationSender.publishEvents(applicationId, event, result);;
        } catch (RemoteException e) {
            log.error("Notification sending failed "+e.getMessage());
        }
    }
}
