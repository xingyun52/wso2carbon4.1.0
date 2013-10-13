package org.wso2.carbon.appfactory.core.util;

import org.wso2.carbon.appfactory.common.AppFactoryConstants;
import org.wso2.carbon.appfactory.core.internal.ServiceHolder;

public class CommonUtil {
    public static String getAdminUsername() {
        return ServiceHolder.getAppFactoryConfiguration()
                .getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME);
    }

    public static String getAdminUsername(String applicationId) {
        return ServiceHolder.getAppFactoryConfiguration()
                .getFirstProperty(AppFactoryConstants.SERVER_ADMIN_NAME) +
                "@" + applicationId;
    }

    public static String getServerAdminPassword() {
        return ServiceHolder.getAppFactoryConfiguration()
                .getFirstProperty(AppFactoryConstants.SERVER_ADMIN_PASSWORD);
    }
}
