package org.wso2.carbon.appfactory.tenant.roles.S2Integration;

public class CartridgeInfo {
    private int minInstances;
    private int maxInstances;
    private boolean shouldActivate;
    private String alias;
    private String cartridgeType;
    private String repoURL;
    private String dataCartridgeType;
    private String dataCartridgeAlias;

    private String endpoint;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getMinInstances() {
        return minInstances;
    }

    public void setMinInstances(int minInstances) {
        this.minInstances = minInstances;
    }

    public int getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    public boolean isShouldActivate() {
        return shouldActivate;
    }

    public void setShouldActivate(boolean shouldActivate) {
        this.shouldActivate = shouldActivate;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCartridgeType() {
        return cartridgeType;
    }

    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }

    public String getRepoURL() {
        return repoURL;
    }

    public void setRepoURL(String repoURL) {
        this.repoURL = repoURL;
    }

    public String getDataCartridgeType() {
        return dataCartridgeType;
    }

    public void setDataCartridgeType(String dataCartridgeType) {
        this.dataCartridgeType = dataCartridgeType;
    }

    public String getDataCartridgeAlias() {
        return dataCartridgeAlias;
    }

    public void setDataCartridgeAlias(String dataCartridgeAlias) {
        this.dataCartridgeAlias = dataCartridgeAlias;
    }
}