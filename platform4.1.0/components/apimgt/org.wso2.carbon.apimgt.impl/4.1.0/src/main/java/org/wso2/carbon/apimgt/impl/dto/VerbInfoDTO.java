package org.wso2.carbon.apimgt.impl.dto;

import org.wso2.carbon.apimgt.impl.APIConstants;

public class VerbInfoDTO {

    private String httpVerb;

    private String authType;

    public String getHttpVerb() {
        return httpVerb;
    }

    public void setHttpVerb(String httpVerb) {
        this.httpVerb = httpVerb;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public boolean requiresAuthentication(){
        return !APIConstants.AUTH_TYPE_NONE.equalsIgnoreCase(authType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VerbInfoDTO that = (VerbInfoDTO) o;

        if (httpVerb != null ? !httpVerb.equals(that.getHttpVerb()) : that.getHttpVerb() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return httpVerb != null ? httpVerb.hashCode() : 0;
    }
}
