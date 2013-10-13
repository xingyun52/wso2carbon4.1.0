package org.wso2.carbon.appfactory.tenant.roles.util;

import org.wso2.carbon.appfactory.common.AppFactoryConstants;

import java.net.MalformedURLException;
import java.net.URL;

public class CommonUtil {
    public static String getAdminUsername() {
        return Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME);
    }

    public static String getAdminUsername(String applicationId) {
        return Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME) +
                "@" + applicationId;
    }

    public static String getServerAdminPassword() {
        return Util.getConfiguration().getFirstProperty(AppFactoryConstants.SERVER_ADMIN_PASSWORD);
    }

    public static String getRemoteHost(String serverUrl) throws MalformedURLException {
        URL url = new URL(serverUrl);
        return url.getHost();
    }
}
