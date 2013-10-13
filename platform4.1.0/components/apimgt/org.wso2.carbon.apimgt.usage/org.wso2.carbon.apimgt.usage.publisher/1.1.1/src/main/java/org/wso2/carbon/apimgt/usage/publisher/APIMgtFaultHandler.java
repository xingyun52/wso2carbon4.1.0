package org.wso2.carbon.apimgt.usage.publisher;

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.carbon.apimgt.usage.publisher.dto.FaultPublisherDTO;
import org.wso2.carbon.apimgt.usage.publisher.internal.UsageComponent;

public class APIMgtFaultHandler extends AbstractMediator{

    private boolean enabled = UsageComponent.getApiMgtConfigReaderService().isEnabled();

    private volatile APIMgtUsageDataPublisher publisher;

    private String publisherClass = UsageComponent.getApiMgtConfigReaderService().getPublisherClass();

    public APIMgtFaultHandler(){

        if (!enabled) {
            return;
        }

        if (publisher == null) {
            synchronized (this){
                if (publisher == null) {
                    try {
                        log.debug("Instantiating Data Publisher");
                        publisher = (APIMgtUsageDataPublisher)Class.forName(publisherClass).newInstance();
                        publisher.init();
                    } catch (ClassNotFoundException e) {
                        log.error("Class not found " + publisherClass);
                    } catch (InstantiationException e) {
                        log.error("Error instantiating " + publisherClass);
                    } catch (IllegalAccessException e) {
                        log.error("Illegal access to " + publisherClass);
                    }
                }
            }
        }
    }

    public boolean mediate(MessageContext messageContext) {

        try{
            if (!enabled) {
                return true;
            }

            long requestTime = ((Long) messageContext.getProperty(APIMgtUsagePublisherConstants.REQUEST_TIME)).longValue();

            FaultPublisherDTO faultPublisherDTO = new FaultPublisherDTO();
            faultPublisherDTO.setConsumerKey((String)messageContext.getProperty(APIMgtUsagePublisherConstants.CONSUMER_KEY));
            faultPublisherDTO.setUsername((String)messageContext.getProperty(APIMgtUsagePublisherConstants.USER_ID));
            faultPublisherDTO.setContext((String) messageContext.getProperty(APIMgtUsagePublisherConstants.CONTEXT));
            faultPublisherDTO.setApi_version((String) messageContext.getProperty(APIMgtUsagePublisherConstants.API_VERSION));
            faultPublisherDTO.setApi((String) messageContext.getProperty(APIMgtUsagePublisherConstants.API));
            faultPublisherDTO.setVersion((String) messageContext.getProperty(APIMgtUsagePublisherConstants.VERSION));
            faultPublisherDTO.setResource((String) messageContext.getProperty(APIMgtUsagePublisherConstants.RESOURCE));
            faultPublisherDTO.setMethod((String)messageContext.getProperty(APIMgtUsagePublisherConstants.HTTP_METHOD));
            faultPublisherDTO.setErrorCode(String.valueOf(messageContext.getProperty(SynapseConstants.ERROR_CODE)));
            faultPublisherDTO.setErrorMessage((String)messageContext.getProperty(SynapseConstants.ERROR_MESSAGE));
            faultPublisherDTO.setRequestTime(requestTime);
            faultPublisherDTO.setHostName((String)messageContext.getProperty(APIMgtUsagePublisherConstants.HOST_NAME));
            faultPublisherDTO.setApiPublisher((String)messageContext.getProperty(APIMgtUsagePublisherConstants.API_PUBLISHER));

            publisher.publishEvent(faultPublisherDTO);

        }catch (Throwable e){
            log.error("Cannot publish event. " + e.getMessage(), e);
        }
        return true; // Should never stop the message flow
    }
}
