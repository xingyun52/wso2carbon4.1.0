package org.wso2.carbon.rest.api.service;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.locks.Lock;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.SequenceMediatorSerializer;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.xml.rest.APIFactory;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.rest.API;
import org.apache.synapse.rest.Resource;
import org.apache.synapse.rest.dispatch.DispatcherHelper;
import org.apache.synapse.rest.dispatch.URITemplateHelper;
import org.apache.synapse.rest.dispatch.URLMappingHelper;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.mediation.initializer.AbstractServiceBusAdmin;
import org.wso2.carbon.mediation.initializer.ServiceBusConstants;
import org.wso2.carbon.mediation.initializer.ServiceBusUtils;
import org.wso2.carbon.mediation.initializer.persistence.MediationPersistenceManager;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import org.wso2.carbon.rest.api.*;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.NetworkUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class RestApiAdmin extends AbstractServiceBusAdmin{
	
    private static Log log = LogFactory.getLog(RestApiAdmin.class);
    private static final String TENANT_DELIMITER = "/t/";
	
	public boolean addApi(APIData apiData) throws APIException{
		final Lock lock = getLock();
        try {
            lock.lock();
            String tenantDomain = PrivilegedCarbonContext.getCurrentContext().getTenantDomain(true);
            if (tenantDomain != null && !tenantDomain.equals("")
                    && !tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                String tenantApiContext = apiData.getContext();
                apiData.setContext(TENANT_DELIMITER + tenantDomain + tenantApiContext);
            }
            addApi(RestApiAdminUtils.retrieveAPIOMElement(apiData), null, false);
            return true;
        } finally {
            lock.unlock();
        }
	}
	
	public boolean addApiFromString(String apiData) throws APIException{
		final Lock lock = getLock();
        try {
            lock.lock();
            OMElement apiElement = AXIOMUtil.stringToOM(apiData);
            addApi(apiElement, null, false);
            return true;
        } catch (XMLStreamException e) {
			handleException(log, "Could not parse String to OMElement", e);
			return false;
		} finally {
            lock.unlock();
        }
	}
	
	public boolean updateApi(String apiName, APIData apiData) throws APIException{
		
		final Lock lock = getLock();
        try {
            lock.lock();
            assertNameNotEmpty(apiName);
            
            API oldAPI = null;
            API api = APIFactory.createAPI(RestApiAdminUtils.retrieveAPIOMElement(apiData));
            SynapseConfiguration synapseConfiguration = getSynapseConfiguration();
            
            oldAPI = synapseConfiguration.getAPI(apiName);
            if (oldAPI != null) {
                oldAPI.destroy();
            	api.setFileName(oldAPI.getFileName());
            }
    		
            synapseConfiguration.updateAPI(apiName, api);
            api.init(getSynapseEnvironment());
    		
    		MediationPersistenceManager pm = getMediationPersistenceManager();
            String fileName = api.getFileName();
            pm.deleteItem(apiName, fileName, ServiceBusConstants.ITEM_TYPE_REST_API);
            pm.saveItem(apiName, ServiceBusConstants.ITEM_TYPE_REST_API);
    		
    		return true;
        } finally {
            lock.unlock();
        }
	}
	
	public boolean updateApiFromString(String apiName, String apiData) throws APIException{
		
		final Lock lock = getLock();
        try {
            lock.lock();
            assertNameNotEmpty(apiName);
            
            OMElement apiElement = AXIOMUtil.stringToOM(apiData);
            
            //Set API name to old value since we do not allow editing the API name.
            OMAttribute nameAttribute = apiElement.getAttribute(new QName("name"));
            if(nameAttribute == null || "".equals(nameAttribute.getAttributeValue().trim())){
            	apiElement.addAttribute("name", apiName, null);
            }
            
            API oldAPI = null;
            API api = APIFactory.createAPI(apiElement);
            
            SynapseConfiguration synapseConfiguration = getSynapseConfiguration();
            
            oldAPI = synapseConfiguration.getAPI(apiName);
            if (oldAPI != null){
                oldAPI.destroy();
            	api.setFileName(oldAPI.getFileName());
            }
                        
    		synapseConfiguration.updateAPI(apiName, api);
            api.init(getSynapseEnvironment());
    		
    		MediationPersistenceManager pm = getMediationPersistenceManager();
            String fileName = api.getFileName();
            pm.deleteItem(apiName, fileName, ServiceBusConstants.ITEM_TYPE_REST_API);
            pm.saveItem(apiName, ServiceBusConstants.ITEM_TYPE_REST_API);
    		
    		return true;
        } catch (XMLStreamException e) {
			handleException(log, "Could not parse String to OMElement", e);
			return false;
		} finally {
            lock.unlock();
        }
	}
	
	public boolean deleteApi(String apiName) throws APIException{
		final Lock lock = getLock();
        try {
            lock.lock();
            assertNameNotEmpty(apiName);
            apiName = apiName.trim();
            if (log.isDebugEnabled()) {
                log.debug("Deleting API : " + apiName + " from the configuration");
            }

            SynapseConfiguration synapseConfiguration = getSynapseConfiguration();
            API api = synapseConfiguration.getAPI(apiName);
            api.destroy();
            synapseConfiguration.removeAPI(apiName);

            MediationPersistenceManager pm = getMediationPersistenceManager();
            String fileName = api.getFileName();
            pm.deleteItem(apiName, fileName, ServiceBusConstants.ITEM_TYPE_REST_API);

            if (log.isDebugEnabled()) {
                log.debug("Api : " + apiName + " removed from the configuration");
            }
        } finally {
            lock.unlock();
        }
        return true;
	}

    public APIData[] getAPIsForListing(int pageNumber, int itemsPerPage){
        final Lock lock = getLock();
        try {
            lock.lock();
            SynapseConfiguration synapseConfiguration = getSynapseConfiguration();
            Collection<API> apis = synapseConfiguration.getAPIs();

            List<APIData> apiDataList = null;
            if(apis != null){
                apiDataList = new ArrayList<APIData>(apis.size());

                for(API api : apis){
                    //Populate the fields we need to show
                    APIData apiData = new APIData();
                    apiData.setName(api.getName());
                    apiData.setContext(api.getContext());

                    apiDataList.add(apiData);
                }
                //Sort APIs by name.
                Collections.sort(apiDataList, new APIDataSorter());

                int size = apiDataList.size();
                int startIndex = pageNumber * itemsPerPage;
                int endIndex = ((pageNumber + 1) * itemsPerPage);

                List<APIData> returnList = null;

                //We do not have enough APIs
                if(size <= startIndex){
                    return null;
                }
                else if(size <= endIndex){
                    returnList = apiDataList.subList(startIndex, size);
                }
                else{
                    returnList = apiDataList.subList(startIndex, endIndex);
                }

                return  returnList.toArray(new APIData[returnList.size()]);
            }

            return null;
        } finally {
            lock.unlock();
        }
    }

    public int getAPICount(){

        final Lock lock = getLock();
        try {
            lock.lock();
            SynapseConfiguration synapseConfiguration = getSynapseConfiguration();
            Collection<API> apis = synapseConfiguration.getAPIs();
            if(apis != null){
                return apis.size();
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }
    
    public String getServerContext() throws APIException {
        AxisConfiguration configuration = null;

        try {
            configuration = ConfigHolder.getInstance().getAxisConfiguration();
        } catch (APIException e) {
            handleException(log, "Could not retrieve server context", e);
        }

        String httpPort = (String)configuration.getTransportIn("http").getParameter("port").getValue();
        String host = null;

        Parameter hostParam =  configuration.getParameter("hostname");

        if(hostParam != null){
            host = (String)hostParam.getValue();
        }
        else{
            try {
                host = NetworkUtils.getLocalHostname();
            } catch (SocketException e) {
                log.warn("SocketException occured when trying to obtain IP address of local machine");
                host = "localhost";
            }
        }

        String serverContext = "http://" + host + ":" + httpPort;

        return serverContext;
    }


	public String[] getApiNames(){
		final Lock lock = getLock();
        try {
            lock.lock();
            SynapseConfiguration synapseConfiguration = getSynapseConfiguration();
            Collection<API> apis = synapseConfiguration.getAPIs();
            return listToNames(apis.toArray(
                    new API[apis.size()]));
        } finally {
            lock.unlock();
        }
	}

	public APIData	getApiByName(String apiName){
		final Lock lock = getLock();
		try{
			lock.lock();
			SynapseConfiguration synapseConfiguration = getSynapseConfiguration();
			API api = synapseConfiguration.getAPI(apiName);
			return convertApiToAPIData(api);
		}finally{
			lock.unlock();
		}
	}

	public String[] getSequences(){
		final Lock lock = getLock();
		String[] sequenceNames = new String[0];
		try{
			lock.lock();
			SynapseConfiguration synapseConfiguration = getSynapseConfiguration();
			Map<String, SequenceMediator> sequences = synapseConfiguration.getDefinedSequences();

			if(sequences != null && !sequences.isEmpty()){
				sequenceNames = new String[sequences.size()];
				return sequences.keySet().toArray(sequenceNames);
			}
			else{
				return sequenceNames;
			}
		}finally{
			lock.unlock();
		}
	}

	public String getApiSource(APIData apiData){
		return RestApiAdminUtils.retrieveAPIOMElement(apiData).toString();
	}

    public String getResourceSource(ResourceData resourceData) {
        return RestApiAdminUtils.retrieveResourceOMElement(resourceData).toString();
    }

	private APIData convertApiToAPIData(API api){
		if(api == null){
			return null;
		}

		APIData apiData = new APIData();
		apiData.setName(api.getName());
		apiData.setContext(api.getContext());
		apiData.setHost(api.getHost());
		apiData.setPort(api.getPort());
		apiData.setFileName(api.getFileName());

		Resource[] resources = api.getResources();
		ResourceData[] resourceDatas = new ResourceData[resources.length];

		for(int i=0; i<resources.length; i++){

			Resource resource = resources[i];
			ResourceData data = new ResourceData();

			String[] methods = resource.getMethods();
			data.setMethods(methods);
			data.setContentType(resource.getContentType());
			data.setProtocol(resource.getProtocol());
			DispatcherHelper dispatcherHelper = resource.getDispatcherHelper();
			if(dispatcherHelper instanceof URITemplateHelper){
				data.setUriTemplate(dispatcherHelper.getString());
			}
			else if(dispatcherHelper instanceof URLMappingHelper){
				data.setUrlMapping(dispatcherHelper.getString());
			}

            if (resource.getInSequenceKey() != null) {
                data.setInSequenceKey(resource.getInSequenceKey());
            } else if (resource.getInSequence() != null) {
                data.setInSeqXml(createAnonymousSequenceElement(
                        resource.getInSequence(),
                        "inSequence"
                ).toString());
            }

            if (resource.getOutSequenceKey() != null) {
                data.setOutSequenceKey(resource.getOutSequenceKey());
            } else if (resource.getOutSequence() != null) {
                data.setOutSeqXml(createAnonymousSequenceElement(
                        resource.getOutSequence(),
                        "outSequence"
                ).toString());
            }

            if (resource.getFaultSequenceKey() != null) {
                data.setFaultSequenceKey(resource.getFaultSequenceKey());
            } else if (resource.getFaultSequence() != null) {
                data.setFaultSeqXml(createAnonymousSequenceElement(
                        resource.getFaultSequence(),
                        "faultSequence"
                ).toString());
            }
            data.setUserAgent(resource.getUserAgent());

			resourceDatas[i] = data;
		}
		apiData.setResources(resourceDatas);
		return apiData;
	}

    private OMElement createAnonymousSequenceElement(SequenceMediator sequenceMediator, String seqElemName) {
        SequenceMediatorSerializer serializer = new SequenceMediatorSerializer();
        OMElement sequenceElem = serializer.serializeAnonymousSequence(null, sequenceMediator);
        if (!"inSequence".equals(seqElemName)
                && !"outSequence".equals(seqElemName)
                && !"faultSequence".equals(seqElemName)) {
            return null;
        }
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace syn = SynapseConstants.SYNAPSE_OMNAMESPACE;
        OMNamespace nullNS = fac.createOMNamespace("","");

        OMElement seq = fac.createOMElement(seqElemName, syn);
        Iterator<OMAttribute> attributes = sequenceElem.getAllAttributes();
        for (; attributes.hasNext(); ) {
            OMAttribute attrb = attributes.next();
            seq.addAttribute(attrb.getLocalName(), attrb.getAttributeValue(), nullNS);
        }
        Iterator<OMElement> children = sequenceElem.getChildElements();
        for (; children.hasNext(); ) {
            OMElement child = children.next();
            seq.addChild(child);
        }
        return seq;
    }

	private String[] listToNames(API[] apis) {
        if (apis == null) {
            return null;
        } else {
            String[] datas = new String[apis.length];
            for (int i = 0; i < apis.length; i++) {
                API api = apis[i];
                datas[i] = api.getName();
            }
            return datas;
        }
    }

	/**
     * Add an api described by the given OMElement
     *
     * @param apiElement configuration of the api which needs to be added
     * @param fileName Name of the file in which this configuration should be saved or null
     * @throws APIException if the element is not an api or if an api with the
     *                   same name exists
     */
	private void addApi(OMElement apiElement,
                                 String fileName, boolean updateMode) throws APIException{

		try {
			if (apiElement.getQName().getLocalPart()
					.equals(XMLConfigConstants.API_ELT.getLocalPart())) {

				String apiName = apiElement.getAttributeValue(new QName("name"));

				if (getSynapseConfiguration().getAxisConfiguration().getService(
						apiName) != null) {
					handleException(log, "A service named " + apiName + " already exists", null);
				} else {
					API api = APIFactory.createAPI(apiElement);

					try {
						getSynapseConfiguration().addAPI(api.getName(), api);

						//addParameterObserver(api.getName());

						if(log.isDebugEnabled()) {
							log.debug("Added API : " + apiName);
						}

						if (updateMode) {
							api.setFileName(fileName);
						} else {
							if (fileName != null) {
								api.setFileName(fileName);
							} else {
								api.setFileName(ServiceBusUtils.generateFileName(api.getName()));
							}
						}
                        api.init(getSynapseEnvironment());
						persistApi(api);

					} catch (Exception e) {
                        api.destroy();
						getSynapseConfiguration().removeAPI(api.getName());
						try{
							if (getAxisConfig().getService(api.getName()) != null) {
								getAxisConfig().removeService(api.getName());
							}
						} catch (Exception ignore) {}
						handleException(log, "Error trying to add the API to the ESB " +
								"configuration : " + api.getName(), e);
					}
				}
			} else {
				handleException(log, "Invalid API definition", null);
			}
		} catch (AxisFault af) {
			handleException(log, "Invalid API definition", af);
		}
	}

	private void handleException(Log log, String message, Exception e) throws APIException {
        if (e == null) {
        	APIException apiException = new APIException(message);
            log.error(message, apiException);
            throw apiException;
        } else {
            message = message + " :: " + e.getMessage();
            log.error(message, e);
            throw new APIException(message, e);
        }
    }
	
	private void persistApi(API api) throws APIException {
        MediationPersistenceManager pm = getMediationPersistenceManager();
        if (pm != null) {
            pm.saveItem(api.getName(), ServiceBusConstants.ITEM_TYPE_REST_API);
        }
    }
	
	private void assertNameNotEmpty(String apiName) throws APIException {
        if (apiName == null || "".equals(apiName.trim())) {
            handleFault("Invalid name : Name is empty.", null);
        }
    }
	
	private void handleFault(String message, Exception e) throws APIException {
        if (e != null) {
            log.error(message, e);
            throw new APIException(e.getMessage(), e);
        } else {
            log.error(message);
            throw new APIException(message);
        }
    }

}
