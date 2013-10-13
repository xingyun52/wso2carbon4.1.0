package org.wso2.carbon.appfactory.utilities.security;

import org.apache.ws.security.WSPasswordCallback;
import org.wso2.carbon.appfactory.common.util.AppFactoryUtil;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

public class PWCBHandler implements CallbackHandler {

    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {

        for (int i = 0; i < callbacks.length; i++) {
            WSPasswordCallback pwcb = (WSPasswordCallback) callbacks[i];
            String id = pwcb.getIdentifer();
            int usage = pwcb.getUsage();


            if (usage == WSPasswordCallback.USERNAME_TOKEN) {
                // Logic to get the password to build the username token
                if (AppFactoryUtil.getAdminUsername().equals(id)) {
                    pwcb.setPassword(AppFactoryUtil.getAdminPassword());
                } else {
                    //not authenticated
                }
            }
        }
    }

}