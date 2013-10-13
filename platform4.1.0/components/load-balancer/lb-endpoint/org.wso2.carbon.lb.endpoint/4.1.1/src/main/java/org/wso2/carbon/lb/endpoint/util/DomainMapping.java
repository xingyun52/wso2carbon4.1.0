package org.wso2.carbon.lb.endpoint.util;

/**
 *
 */
public class DomainMapping {
    private String mapping;
    private String actualHost;
    private String app;
    private String appType;
    private String tenantContext;

    public DomainMapping(String mapping) {
        this.mapping = mapping;
    }

    public String getActualHost() {
        return actualHost;
    }

    public void setActualHost(String actualHost) {
        this.actualHost = actualHost;
    }

    public String getApp() {
        if(app != null){
            return app;
        } else return "";
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getAppType() {
        if(appType != null) {
            return appType;
        } else return "";
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getTenantContext() {
        if(tenantContext != null) {
                return tenantContext;
        } else return "";
    }

    public void setTenantContext(String tenantContext) {
        this.tenantContext = tenantContext;
    }

    public String getMapping() {
        return mapping;
    }
}
