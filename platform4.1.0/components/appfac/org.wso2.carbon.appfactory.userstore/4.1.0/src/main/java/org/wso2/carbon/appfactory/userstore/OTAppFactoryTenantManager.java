package org.wso2.carbon.appfactory.userstore;

import java.util.Map;

import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;

public class OTAppFactoryTenantManager extends AppFactoryUserStore {

    public OTAppFactoryTenantManager(RealmConfiguration realmConfig,
                                     Map<String, Object> properties, ClaimManager claimManager,
                                     ProfileConfigurationManager profileManager, UserRealm realm,
                                     Integer tenantId) throws UserStoreException {
        super(realmConfig, properties, claimManager, profileManager, realm, tenantId);
    }

    public OTAppFactoryTenantManager(RealmConfiguration realmConfig, ClaimManager claimManager,
                                     ProfileConfigurationManager profileManager)
                                                                                throws UserStoreException {
        super(realmConfig, claimManager, profileManager);
    }

    @Override
    public boolean authenticate(String userName, Object credential) throws UserStoreException {
        return super.authenticate(doConvert(userName), credential);
    }

    @Override
    public void doUpdateUserListOfRole(String userName, String[] deletedRoles, String[] newRoles)
                                                                                                 throws UserStoreException {
        super.doUpdateUserListOfRole(doConvert(userName), deletedRoles, newRoles);
    }

    @Override
    public boolean doAuthenticate(String userName, Object password) throws UserStoreException {
        return super.doAuthenticate(doConvert(userName), password);
    }

    @Override
    public String[] getRoleListOfUser(String userName) throws UserStoreException {
        return super.getRoleListOfUser(doConvert(userName));
    }

    @Override
    public int getTenantId() throws UserStoreException {
        return super.getTenantId();
    }

    @Override
    public int getTenantId(String tenantId) throws UserStoreException {
        return super.getTenantId(tenantId);
    }

    @Override
    public int getUserId(String userName) throws UserStoreException {
        return super.getUserId(doConvert(userName));
    }

    @Override
    public boolean isExistingUser(String userName) throws UserStoreException {
        return super.isExistingUser(doConvert(userName));
    }

    @Override
    public boolean isUserHasTheRole(String userName, String roleName) throws UserStoreException {
        return super.isUserHasTheRole(doConvert(userName), roleName);
    }

    private String doConvert(String name) {
        return name.replace("@", ".");
    }

}
