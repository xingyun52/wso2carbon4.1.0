package org.wso2.carbon.appfactory.apiManager.integration.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.wso2.carbon.appfactory.apiManager.integration.internal.ServiceHolder;
import org.wso2.carbon.appfactory.common.AppFactoryConfiguration;
import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.common.AppFactoryException;
import org.wso2.carbon.appfactory.core.dto.API;
import org.wso2.carbon.appfactory.core.dto.APIMetadata;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static org.wso2.carbon.appfactory.apiManager.integration.utils.Constants.*;

public class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);
    private static String apiManagerRESTEndpointURL;

    private static String apiManagerDefaultTier;

    public static HttpResponse executeHttpMethod(HttpClient httpClient, HttpPost postMethod)
            throws AppFactoryException {

        try {
            HttpResponse response = httpClient.execute(postMethod);
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
                throw new Exception("Authorization error. "
                        + "Please check if you provided a valid Login and Password.");
            }

            if (responseCode != HttpStatus.SC_OK) {
                throw new Exception("Error in invoking path "
                        + ". Return status is " + responseCode);
            }
            return response;
        } catch (Exception e) {
            String msg = "Unable to execute http method";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }
    }

    public static JsonObject getJsonObject(String response) {
        JsonParser parser = new JsonParser();
        return (JsonObject) parser.parse(response);
    }

    public static HttpPost createHttpPostRequest(URL url, List<NameValuePair> params, String path)
            throws AppFactoryException {

        URI uri;
        try {
            uri = URIUtils.createURI(url.getProtocol(), url.getHost(), url.getPort(), path,
                    URLEncodedUtils.format(params, "UTF-8"), null);
        } catch (URISyntaxException e) {
            String msg = "Invalid URL syntax";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }

        return new HttpPost(uri);
    }

    public static String getApiManagerRESTEndpointURL() {
        if (apiManagerRESTEndpointURL == null) {
            apiManagerRESTEndpointURL = ServiceHolder.getInstance().getAppFactoryConfiguration().getProperties(
                    AppFactoryConstants.API_MANAGER_SERVICE_ENDPOINT)[0];
        }
        return apiManagerRESTEndpointURL;
    }

    public static URL getApiManagerURL() throws AppFactoryException {
        URL endpoint;
        try {
            endpoint = new URL(getApiManagerRESTEndpointURL());
        } catch (MalformedURLException e) {
            String msg = "API Manager url is malformed";
            log.error(msg, e);
            throw new AppFactoryException(msg, e);
        }
        return endpoint;
    }

    public static API populateAPIInfo(JsonObject applicationSubscription) {
        List<APIMetadata> keyList = new ArrayList<APIMetadata>();
        List<APIMetadata> endpointList = new ArrayList<APIMetadata>();

        API apiInfo = new API();

        apiInfo.setApiName(applicationSubscription.get(NAME).getAsString());
        apiInfo.setApiVersion(applicationSubscription.get(VERSION).getAsString());
        apiInfo.setApiProvider(applicationSubscription.get(PROVIDER).getAsString());

        if (applicationSubscription.get(CONTEXT) != null &&
                !applicationSubscription.get(CONTEXT).isJsonNull()) {
            apiInfo.setContext(applicationSubscription.get(CONTEXT).getAsString());
        }
        if (applicationSubscription.get(WADL) != null &&
                !applicationSubscription.get(WADL).isJsonNull()) {
            apiInfo.setContext(applicationSubscription.get(WADL).getAsString());
        }
        if (applicationSubscription.get(WSDL) != null &&
                !applicationSubscription.get(WSDL).isJsonNull()) {
            apiInfo.setContext(applicationSubscription.get(WSDL).getAsString());
        }
        if (applicationSubscription.get(DESCRIPTION) != null &&
                !applicationSubscription.get(DESCRIPTION).isJsonNull()) {
            apiInfo.setContext(applicationSubscription.get(DESCRIPTION).getAsString());
        }

//        Adding the keys to the map
//        Adding the production keys
        if (applicationSubscription.get(PROD_KEY) != null &&
                !applicationSubscription.get(PROD_KEY).isJsonNull()) {
            keyList.add(new APIMetadata(PROD_KEY,
                    applicationSubscription.get(PROD_KEY).getAsString(),null));
        }
        if (applicationSubscription.get(PROD_CONSUMER_KEY) != null &&
                !applicationSubscription.get(PROD_CONSUMER_KEY).isJsonNull()) {
            keyList.add(new APIMetadata(PROD_CONSUMER_KEY,
                    applicationSubscription.get(PROD_CONSUMER_KEY).getAsString(),null));
        }
        if (applicationSubscription.get(PROD_CONSUMER_SECRET) != null &&
                !applicationSubscription.get(PROD_CONSUMER_SECRET).isJsonNull()) {
            keyList.add(new APIMetadata(PROD_CONSUMER_SECRET,
                    applicationSubscription.get(PROD_CONSUMER_SECRET).getAsString(),null));
        }

//        Adding the sandbox keys
        if (applicationSubscription.get(SANDBOX_KEY) != null &&
                !applicationSubscription.get(SANDBOX_KEY).isJsonNull()) {
            keyList.add(new APIMetadata(SANDBOX_KEY,
                    applicationSubscription.get(SANDBOX_KEY).getAsString(),null));
        }
        if (applicationSubscription.get(SANDBOX_CONSUMER_KEY) != null &&
                !applicationSubscription.get(SANDBOX_CONSUMER_KEY).isJsonNull()) {
            keyList.add(new APIMetadata(SANDBOX_CONSUMER_KEY,
                    applicationSubscription.get(SANDBOX_CONSUMER_KEY).getAsString(),null));
        }
        if (applicationSubscription.get(SANDBOX_CONSUMER_SECRET) != null &&
                !applicationSubscription.get(SANDBOX_CONSUMER_SECRET).isJsonNull()) {
            keyList.add(new APIMetadata(SANDBOX_CONSUMER_SECRET,
                    applicationSubscription.get(SANDBOX_CONSUMER_SECRET).getAsString(),null));
        }
        apiInfo.setKeys(keyList.toArray(new APIMetadata[keyList.size()]));


//        Adding the endpoints to the map
        if (applicationSubscription.get(ENDPOINT) != null &&
                !applicationSubscription.get(ENDPOINT).isJsonNull()) {
            endpointList.add(new APIMetadata(ENDPOINT,
                    applicationSubscription.get(ENDPOINT).getAsString(),null));
        }
        if (applicationSubscription.get(SANDBOX) != null &&
                !applicationSubscription.get(SANDBOX).isJsonNull()) {
            endpointList.add(new APIMetadata(SANDBOX,
                    applicationSubscription.get(SANDBOX).getAsString(),null));
        }
        apiInfo.setEndpointUrls(endpointList.toArray(new APIMetadata[endpointList.size()]));


        return apiInfo;
    }

    public static void generateKey(String appId, URL apiManagerUrl, String keyType, HttpClient httpClient)
            throws AppFactoryException {
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new BasicNameValuePair(ACTION, "generateApplicationKey"));
        parameters.add(new BasicNameValuePair("application", appId));
        parameters.add(new BasicNameValuePair("keytype", keyType));

        HttpPost postMethod = createHttpPostRequest(apiManagerUrl, parameters, ADD_SUBSCRIPTIONS_ENDPOINT);

        HttpResponse httpResponse = executeHttpMethod(httpClient, postMethod);
        if (httpResponse != null) {
            try {

                HttpEntity responseEntity = httpResponse.getEntity();
                EntityUtils.toString(responseEntity);
            } catch (IOException e) {
                String msg = "Error reading the json response";
                log.error(msg, e);
                throw new AppFactoryException(msg, e);
            }
        }
    }

    public static void registerSandboxKeys(Registry tenantRegistry ,String name,String value)
            throws AppFactoryException{

        writeToRegistry(tenantRegistry,DEVELOPMENT,name,value);

        writeToRegistry(tenantRegistry,TESTING,name,value);

    }
    public static void registerProdKeys(Registry tenantRegistry,String name,String value)
            throws AppFactoryException{

        writeToRegistry(tenantRegistry,PRODUCTION,name,value);
    }

    public static void writeToRegistry(Registry tenantRegistry,String state, String name,
                                       String value) throws AppFactoryException{
        try {

            String resourcePath = getResourcePathString(state,name);

            if (resourcePath != null) {
                Resource resource;
                if(tenantRegistry.resourceExists(resourcePath)){
                    resource = tenantRegistry.get(resourcePath);
                }else{
                    resource = tenantRegistry.newResource();
                }

                resource.setContent(value);
                tenantRegistry.put(resourcePath,resource);
            }
        } catch (RegistryException e) {
            String msg = "Unable to write values to registry";
            log.error(msg);
            throw new AppFactoryException(msg,e);
        }
    }

    public static String getResourcePathString(String state,String name) throws AppFactoryException{
        AppFactoryConfiguration configuration = ServiceHolder.getInstance().getAppFactoryConfiguration();

        String devMount = configuration.getFirstProperty(DEV_MOUNT);
        String testMount = configuration.getFirstProperty(TEST_MOUNT);
        String prodMount = configuration.getFirstProperty(PROD_MOUNT);


        if(state.equals(DEVELOPMENT)){
            if(devMount == null){
                return null;
            }
            return RegistryConstants.PATH_SEPARATOR + devMount + RegistryConstants.PATH_SEPARATOR + name;
        }else if(state.equals(TESTING)){
            if(testMount == null){
                return null;
            }
            return RegistryConstants.PATH_SEPARATOR + testMount +  RegistryConstants.PATH_SEPARATOR + name;
        }else if(state.equals(PRODUCTION)){
            if(prodMount == null){
                return null;
            }
            return RegistryConstants.PATH_SEPARATOR + prodMount + RegistryConstants.PATH_SEPARATOR + name;
        }else{
            String msg = "Could not recognise lifecycle state";
            log.error(msg);
            throw new AppFactoryException(msg);
        }
    }

    public static String decode(String encodedStr) throws AppFactoryException {
        try {
            org.apache.commons.codec.binary.Base64 base64Decoder = new org.apache.commons.codec.binary.Base64();
            byte[] xmlBytes = encodedStr.getBytes("UTF-8");
            byte[] base64DecodedByteArray = base64Decoder.decode(xmlBytes);

            try {
                Inflater inflater = new Inflater(true);
                inflater.setInput(base64DecodedByteArray);
                byte[] xmlMessageBytes = new byte[5000];
                int resultLength = inflater.inflate(xmlMessageBytes);

                if (!inflater.finished()) {
                    throw new RuntimeException("didn't allocate enough space to hold "
                            + "decompressed data");
                }

                inflater.end();
                return new String(xmlMessageBytes, 0, resultLength, "UTF-8");

            } catch (DataFormatException e) {
                ByteArrayInputStream bais = new ByteArrayInputStream(
                        base64DecodedByteArray);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InflaterInputStream iis = new InflaterInputStream(bais);
                byte[] buf = new byte[1024];
                int count = iis.read(buf);
                while (count != -1) {
                    baos.write(buf, 0, count);
                    count = iis.read(buf);
                }
                iis.close();

                return new String(baos.toByteArray());
            }
        } catch (IOException e) {
            throw new AppFactoryException("Error when decoding the SAML Request.", e);
        }

    }

    public static String getDefaultTier(){
        if (apiManagerDefaultTier != null) {
            AppFactoryConfiguration configuration = ServiceHolder.getInstance().getAppFactoryConfiguration();
            apiManagerDefaultTier = configuration.getFirstProperty(API_MANAGER_DEFAULT_TIER);
        }
        return apiManagerDefaultTier;
    }


}
