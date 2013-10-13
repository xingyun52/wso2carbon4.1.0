package org.wso2.carbon.appfactory.core.dto;

public class API {
    String apiName;
    String apiVersion;
    String apiProvider;
    APIMetadata[] keys;
    APIMetadata[] endpointUrls;
    String owner;
    String context;
    String wadlUrl;
    String wsdlUrl;
    String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getWadlUrl() {
        return wadlUrl;
    }

    public void setWadlUrl(String wadlUrl) {
        this.wadlUrl = wadlUrl;
    }

    public String getWsdlUrl() {
        return wsdlUrl;
    }

    public void setWsdlUrl(String wsdlUrl) {
        this.wsdlUrl = wsdlUrl;
    }

    public APIMetadata[] getEndpointUrls() {
        return endpointUrls;
    }

    public void setEndpointUrls(APIMetadata[] endpointUrls) {
        this.endpointUrls = endpointUrls;
    }

    public APIMetadata[] getKeys() {
        return keys;
    }

    public void setKeys(APIMetadata[] keys) {
        this.keys = keys;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getApiProvider() {
        return apiProvider;
    }

    public void setApiProvider(String apiProvider) {
        this.apiProvider = apiProvider;
    }

}
