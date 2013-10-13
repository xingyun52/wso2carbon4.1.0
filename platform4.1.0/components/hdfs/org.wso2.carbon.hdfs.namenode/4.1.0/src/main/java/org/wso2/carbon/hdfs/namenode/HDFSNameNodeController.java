package org.wso2.carbon.hdfs.namenode;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="org.wso2.carbon.hdfs.namenode.component" immediate="true"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"  unbind="unsetRealmService"
 */

public class HDFSNameNodeController {

    private static Log log = LogFactory.getLog(HDFSNameNodeController.class);
    private static String DISABLE_HDFS_NAMENODE_STARTUP = "disable.hdfs.namenode.startup";
    private static String DISABLE_HDFS_STARTUP = "disable.hdfs.startup";

    private RealmService realmService;
    // private AuthenticationService authenticationService;


    protected void activate(ComponentContext componentContext) {
        if (log.isDebugEnabled()) {
            log.debug("HDFS Name Node bunddle is activated.");
        }
        String disableNameNodeStartup = System.getProperty(DISABLE_HDFS_NAMENODE_STARTUP);
        String disableHdfsStartup = System.getProperty(DISABLE_HDFS_STARTUP);
        if (("true".equals(disableNameNodeStartup)) || ("true".equals(disableHdfsStartup))) {
            log.debug("HDFS name node is disabled and not started in the service activator");
            return;
        }
        HDFSNameNodeComponentManager.getInstance().init(realmService);
        HDFSNameNode HDFSNameNode = new HDFSNameNode();
        //HDFSNameNode.start();
    }

    protected void deactivate(ComponentContext componentContext) {
        if (log.isDebugEnabled()) {
            log.debug("HDFS Name Node bunddle is deactivated.");
        }
        HDFSNameNodeComponentManager.getInstance().destroy();
    }

    protected void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        this.realmService = null;
    }

//    protected void setAuthenticationService(AuthenticationService authenticationService) {
//        this.authenticationService = authenticationService;
//    }
//
//    protected void unsetAuthenticationService(AuthenticationService authenticationService) {
//        this.authenticationService = null;
//    }

}

