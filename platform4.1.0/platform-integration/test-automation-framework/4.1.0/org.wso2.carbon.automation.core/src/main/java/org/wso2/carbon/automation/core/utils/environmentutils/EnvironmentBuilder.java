package org.wso2.carbon.automation.core.utils.environmentutils;

import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.api.clients.authenticators.AuthenticatorClient;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.frameworkutils.EnvironmentSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkFactory;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkProperties;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkSettings;
import org.wso2.carbon.automation.core.utils.frameworkutils.productvariables.ProductVariables;
import org.wso2.carbon.automation.core.utils.frameworkutils.productvariables.WorkerVariables;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class EnvironmentBuilder {
    protected EnvironmentVariables as;
    protected EnvironmentVariables esb;
    protected EnvironmentVariables is;
    protected EnvironmentVariables bps;
    protected EnvironmentVariables dss;
    protected EnvironmentVariables greg;
    protected EnvironmentVariables bam;
    protected EnvironmentVariables brs;
    protected EnvironmentVariables cep;
    protected EnvironmentVariables gs;
    protected EnvironmentVariables mb;
    protected EnvironmentVariables ms;
    protected EnvironmentVariables ss;
    protected EnvironmentVariables axis2;
    protected EnvironmentVariables manager;
    protected EnvironmentVariables clusterNode;
    protected List<EnvironmentVariables> clusterList = new LinkedList<EnvironmentVariables>();
    protected Map<String, EnvironmentVariables> clusterMap = new HashMap();
    private String managerSessionCookie;
    private String workerSessionCookie = null;
    private String managerBackEndUrl;
    private String workerBackEndUrl;
    private String serviceUrl;
    private String secureServiceUrl;
    private String managerHostName;
    private String workerHostName;
    private String httpPort;
    private String webAppURL;
    private EnvironmentVariables envVariables;

    public EnvironmentBuilder() {
    }

    public EnvironmentBuilder as(int userId)
            throws RemoteException, LoginAuthenticationExceptionException {
        envVariables = new EnvironmentVariables();
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.APP_SERVER_NAME);
        ProductVariables asSetter = frameworkProperties.getProductVariables();
        WorkerVariables asWorker = frameworkProperties.getWorkerVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        managerBackEndUrl = asSetter.getBackendUrl();
        workerBackEndUrl = asWorker.getBackendUrl();
        managerHostName = asSetter.getHostName();
        workerHostName = asWorker.getHostName();
        httpPort = asSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetupAs(userId, managerServiceAuthentication, workerServiceAuthentication, frameworkProperties, asSetter, asWorker);
        this.as = envVariables;
        return this;
    }

    public EnvironmentBuilder esb(int userId)
            throws LoginAuthenticationExceptionException, RemoteException {
        envVariables = new EnvironmentVariables();
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.ESB_SERVER_NAME);
        ProductVariables esbSetter = frameworkProperties.getProductVariables();
        WorkerVariables esbWorker = frameworkProperties.getWorkerVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;

        managerBackEndUrl = esbSetter.getBackendUrl();
        workerBackEndUrl = esbWorker.getBackendUrl();
        managerHostName = esbSetter.getHostName();
        workerHostName = esbWorker.getHostName();
        httpPort = esbSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        /*if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }*/
        envVariables = loginSetup(userId, managerServiceAuthentication, workerServiceAuthentication
                , frameworkProperties, esbSetter, esbWorker);
        this.esb = envVariables;
        return this;
    }

    public EnvironmentBuilder ss(int userId)
            throws LoginAuthenticationExceptionException, RemoteException {
        envVariables = new EnvironmentVariables();
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.SS_SERVER_NAME);
        ProductVariables ssSetter = frameworkProperties.getProductVariables();
        WorkerVariables ssWorker = frameworkProperties.getWorkerVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        managerBackEndUrl = ssSetter.getBackendUrl();
        workerBackEndUrl = ssWorker.getBackendUrl();
        managerHostName = ssSetter.getHostName();
        workerHostName = ssWorker.getHostName();
        httpPort = ssSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(userId, managerServiceAuthentication, workerServiceAuthentication
                , frameworkProperties, ssSetter, ssWorker);
        this.ss = envVariables;
        return this;
    }

    public EnvironmentBuilder axis2(int userId)
            throws RemoteException, LoginAuthenticationExceptionException {
        envVariables = new EnvironmentVariables();
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.AXIS2_SERVER_NAME);
        ProductVariables axis2Setter = frameworkProperties.getProductVariables();
        // AuthenticatorClient adminServiceAuthentication;
        managerBackEndUrl = axis2Setter.getBackendUrl();
        managerHostName = axis2Setter.getHostName();
        httpPort = axis2Setter.getHttpPort();
        envVariables = loginSetupAxis2(userId, frameworkProperties, axis2Setter);
        this.axis2 = envVariables;
        return this;
    }

    public EnvironmentBuilder is(int userId)
            throws LoginAuthenticationExceptionException, RemoteException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.IS_SERVER_NAME);
        ProductVariables isSetter = frameworkProperties.getProductVariables();
        WorkerVariables isWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = isSetter.getBackendUrl();
        workerBackEndUrl = isWorker.getBackendUrl();
        managerHostName = isSetter.getHostName();
        workerHostName = isWorker.getHostName();
        httpPort = isSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(userId, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, isSetter, isWorker);
        this.is = envVariables;
        return this;
    }

    public EnvironmentBuilder bps(int userId)
            throws LoginAuthenticationExceptionException, RemoteException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.BPS_SERVER_NAME);
        ProductVariables bpsSetter = frameworkProperties.getProductVariables();
        WorkerVariables bpsWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = bpsSetter.getBackendUrl();
        workerBackEndUrl = bpsWorker.getBackendUrl();
        managerHostName = bpsSetter.getHostName();
        workerHostName = bpsWorker.getHostName();
        httpPort = bpsSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(userId, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties,
                bpsSetter, bpsWorker);
        this.bps = envVariables;
        return this;
    }

    public EnvironmentBuilder dss(int userId)
            throws LoginAuthenticationExceptionException, RemoteException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.DSS_SERVER_NAME);
        ProductVariables dssSetter = frameworkProperties.getProductVariables();
        WorkerVariables dssWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = dssSetter.getBackendUrl();
        workerBackEndUrl = dssWorker.getBackendUrl();
        managerHostName = dssSetter.getHostName();
        workerHostName = dssWorker.getHostName();
        httpPort = dssSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(userId, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, dssSetter, dssWorker);
        this.dss = envVariables;
        return this;
    }

    public EnvironmentBuilder greg(int tenent)
            throws RemoteException, LoginAuthenticationExceptionException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.GREG_SERVER_NAME);
        ProductVariables gregSetter = frameworkProperties.getProductVariables();
        WorkerVariables gregWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = gregSetter.getBackendUrl();
        workerBackEndUrl = gregWorker.getBackendUrl();
        managerHostName = gregSetter.getHostName();
        workerHostName = gregWorker.getHostName();
        httpPort = gregSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }

        envVariables = loginSetup(tenent, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, gregSetter, gregWorker);
        this.greg = envVariables;
        return this;
    }

    public EnvironmentBuilder bam(int tenent)
            throws LoginAuthenticationExceptionException, RemoteException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.BAM_SERVER_NAME);
        ProductVariables bamSetter = frameworkProperties.getProductVariables();
        WorkerVariables bamWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = bamSetter.getBackendUrl();
        workerBackEndUrl = bamWorker.getBackendUrl();
        managerHostName = bamSetter.getHostName();
        workerHostName = bamWorker.getHostName();
        httpPort = bamSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(tenent, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, bamSetter, bamWorker);
        this.bam = envVariables;
        return this;
    }


    public EnvironmentBuilder brs(int tenent)
            throws RemoteException, LoginAuthenticationExceptionException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.BRS_SERVER_NAME);
        ProductVariables brsSetter = frameworkProperties.getProductVariables();
        WorkerVariables brsWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = brsSetter.getBackendUrl();
        workerBackEndUrl = brsWorker.getBackendUrl();
        managerHostName = brsSetter.getHostName();
        workerHostName = brsWorker.getHostName();
        httpPort = brsSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(tenent, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, brsSetter, brsWorker);
        this.brs = envVariables;
        return this;
    }

    public EnvironmentBuilder cep(int tenent)
            throws RemoteException, LoginAuthenticationExceptionException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.CEP_SERVER_NAME);
        ProductVariables cepSetter = frameworkProperties.getProductVariables();
        WorkerVariables cepWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = cepSetter.getBackendUrl();
        workerBackEndUrl = cepWorker.getBackendUrl();
        managerHostName = cepSetter.getHostName();
        workerHostName = cepWorker.getHostName();
        httpPort = cepSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(tenent, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, cepSetter, cepWorker);
        this.cep = envVariables;
        return this;
    }

    public EnvironmentBuilder gs(int tenent)
            throws RemoteException, LoginAuthenticationExceptionException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.GS_SERVER_NAME);
        ProductVariables gsSetter = frameworkProperties.getProductVariables();
        WorkerVariables gsWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = gsSetter.getBackendUrl();
        workerBackEndUrl = gsWorker.getBackendUrl();
        managerHostName = gsSetter.getHostName();
        workerHostName = gsWorker.getHostName();
        httpPort = gsSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(tenent, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, gsSetter, gsWorker);
        this.gs = envVariables;
        return this;
    }

    public EnvironmentBuilder ms(int tenent)
            throws RemoteException, LoginAuthenticationExceptionException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.MS_SERVER_NAME);
        ProductVariables msSetter = frameworkProperties.getProductVariables();
        WorkerVariables msWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = msSetter.getBackendUrl();
        workerBackEndUrl = msWorker.getBackendUrl();
        managerHostName = msSetter.getHostName();
        workerHostName = msWorker.getHostName();
        httpPort = msSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(tenent, managerServiceAuthentication, workerServiceAuthentication
                , frameworkProperties, msSetter, msWorker);
        this.ms = envVariables;
        return this;
    }

    public EnvironmentBuilder mb(int tenent)
            throws RemoteException, LoginAuthenticationExceptionException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.MB_SERVER_NAME);
        ProductVariables mbSetter = frameworkProperties.getProductVariables();
        WorkerVariables mbWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = mbSetter.getBackendUrl();
        workerBackEndUrl = mbWorker.getBackendUrl();
        managerHostName = mbSetter.getHostName();
        workerHostName = mbWorker.getHostName();
        httpPort = mbSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(tenent, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, mbSetter, mbWorker);
        this.mb = envVariables;
        return this;
    }


    public EnvironmentBuilder manager(int tenent)
            throws RemoteException, LoginAuthenticationExceptionException {
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getFrameworkProperties(ProductConstant.MANAGER_SERVER_NAME);
        ProductVariables managerSetter = frameworkProperties.getProductVariables();
        WorkerVariables managerWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = managerSetter.getBackendUrl();
        workerBackEndUrl = managerWorker.getBackendUrl();
        managerHostName = managerSetter.getHostName();
        workerHostName = managerWorker.getHostName();
        httpPort = managerSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(tenent, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, managerSetter, managerWorker);
        this.manager = envVariables;
        return this;
    }

    public EnvironmentBuilder clusterNode(String node, int tenent)
            throws RemoteException, LoginAuthenticationExceptionException {
        ClusterReader reader = new ClusterReader();

        reader.getProductName(node);
        envVariables = new EnvironmentVariables();
        AuthenticatorClient managerServiceAuthentication;
        AuthenticatorClient workerServiceAuthentication = null;
        FrameworkProperties frameworkProperties =
                FrameworkFactory.getClusterProperties(node);
        ProductVariables clusterSetter = frameworkProperties.getProductVariables();
        WorkerVariables clusterWorker = frameworkProperties.getWorkerVariables();
        managerBackEndUrl = clusterSetter.getBackendUrl();
        workerBackEndUrl = clusterWorker.getBackendUrl();
        managerHostName = clusterSetter.getHostName();
        workerHostName = clusterWorker.getHostName();
        httpPort = clusterSetter.getHttpPort();
        managerServiceAuthentication = new AuthenticatorClient(managerBackEndUrl);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerServiceAuthentication = new AuthenticatorClient(workerBackEndUrl);
        }
        envVariables = loginSetup(tenent, managerServiceAuthentication, workerServiceAuthentication,
                frameworkProperties, clusterSetter, clusterWorker);
        this.clusterNode = envVariables;
        this.clusterList.add(this.clusterNode);
        this.clusterMap.put(node, this.clusterNode);
        return this;
    }


    private EnvironmentVariables loginSetup(int userID,
                                            AuthenticatorClient managerServiceAuthentication,
                                            AuthenticatorClient workerServiceAuthentication,
                                            FrameworkProperties frameworkProperties,
                                            ProductVariables productSetter, WorkerVariables workerSetter)
            throws LoginAuthenticationExceptionException, RemoteException {
        UserInfo userInfo = UserListCsvReader.getUserInfo(userID);
        managerSessionCookie = managerServiceAuthentication.login(userInfo.getUserName(),
                userInfo.getPassword(), managerHostName);
        /*if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerSessionCookie = workerServiceAuthentication.login(userInfo.getUserName(),
                    userInfo.getPassword(), workerHostName);
        }*/
        serviceUrl = getServiceURL(frameworkProperties, productSetter, workerSetter, userInfo);
        secureServiceUrl=getSecureServiceURL(frameworkProperties,productSetter,workerSetter,userInfo);
        envVariables.setEnvironment(managerSessionCookie, workerSessionCookie, managerBackEndUrl, serviceUrl,secureServiceUrl, userInfo,
                managerServiceAuthentication, productSetter, workerSetter);
        return envVariables;
    }

    private EnvironmentVariables loginSetupAxis2(int userID,
                                                 FrameworkProperties frameworkProperties,
                                                 ProductVariables productSetter)
            throws LoginAuthenticationExceptionException, RemoteException {
        UserInfo userInfo = UserListCsvReader.getUserInfo(userID);
        serviceUrl = getServiceURL(frameworkProperties, productSetter, userInfo);
        secureServiceUrl=getSecureServiceURL(frameworkProperties,productSetter,userInfo);
        envVariables.setEnvironment(managerBackEndUrl, serviceUrl,secureServiceUrl, userInfo, productSetter);
        return envVariables;
    }

    private EnvironmentVariables loginSetupAs(int user,
                                              AuthenticatorClient managerServiceAuthentication,
                                              AuthenticatorClient workerServiceAuthentication,
                                              FrameworkProperties frameworkProperties,
                                              ProductVariables productSetter, WorkerVariables workerSetter)
            throws LoginAuthenticationExceptionException, RemoteException {
        UserInfo userInfo = UserListCsvReader.getUserInfo(user);
        managerSessionCookie = managerServiceAuthentication.login(userInfo.getUserName(),
                userInfo.getPassword(), managerHostName);
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            workerSessionCookie = workerServiceAuthentication.login(userInfo.getUserName(),
                    userInfo.getPassword(), workerHostName);
        }
        serviceUrl = getServiceURL(frameworkProperties, productSetter, workerSetter, userInfo);
        secureServiceUrl= getSecureServiceURL(frameworkProperties, productSetter, workerSetter, userInfo);
        webAppURL = new ProductUrlGeneratorUtil().
                getWebappURL(httpPort, managerHostName, frameworkProperties, userInfo);
        envVariables.setEnvironment(managerSessionCookie, workerSessionCookie, managerBackEndUrl, serviceUrl,secureServiceUrl, webAppURL,
                userInfo, managerServiceAuthentication, productSetter, workerSetter);
        return envVariables;
    }

    private String getServiceURL(FrameworkProperties frameworkProperties,
                                 ProductVariables productSetter, WorkerVariables workerVariables, UserInfo userInfo) {
        String generatedServiceURL;
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            if (workerVariables.getNhttpPort() != null) { //if port is nhttp port
                generatedServiceURL = new ProductUrlGeneratorUtil().
                        getHttpServiceURL(workerVariables.getHttpPort(), workerVariables.getNhttpPort(),
                                workerVariables.getHostName(), frameworkProperties, userInfo);
            } else {
                generatedServiceURL = new ProductUrlGeneratorUtil().
                        getHttpServiceURL(workerVariables.getHttpPort(), workerVariables.getNhttpPort(),
                                workerVariables.getHostName(), frameworkProperties, userInfo);
            }
        } else {
            if (productSetter.getNhttpPort() != null) { //if port is nhttp port
                generatedServiceURL = new ProductUrlGeneratorUtil().
                        getHttpServiceURL(productSetter.getHttpPort(), productSetter.getNhttpPort(), managerHostName, frameworkProperties, userInfo);
            } else {
                generatedServiceURL = new ProductUrlGeneratorUtil().
                        getHttpServiceURL(httpPort, productSetter.getNhttpPort(), managerHostName, frameworkProperties, userInfo);
            }
        }
        return generatedServiceURL;
    }

    private String getServiceURL(FrameworkProperties frameworkProperties,
                                 ProductVariables productSetter, UserInfo userInfo) {
        String generatedSecureServiceURL;
        if (productSetter.getNhttpPort() != null) { //if port is nhttp port
            generatedSecureServiceURL = new ProductUrlGeneratorUtil().
                    getHttpServiceURL(productSetter.getHttpPort(), productSetter.getNhttpPort(), managerHostName, frameworkProperties, userInfo);
        } else {
            generatedSecureServiceURL = new ProductUrlGeneratorUtil().
                    getHttpServiceURL(httpPort, productSetter.getNhttpPort(), managerHostName, frameworkProperties, userInfo);
        }

        return generatedSecureServiceURL;
    }


    private String getSecureServiceURL(FrameworkProperties frameworkProperties,
                                 ProductVariables productSetter, WorkerVariables workerVariables, UserInfo userInfo) {
        String generatedSecureServiceURL;
        if (frameworkProperties.getEnvironmentSettings().isClusterEnable()) {
            if (workerVariables.getNhttpsPort() != null) { //if port is nhttp port
                generatedSecureServiceURL = new ProductUrlGeneratorUtil().
                        getHttpsServiceURL(workerVariables.getHttpsPort(), workerVariables.getNhttpsPort(),
                                          workerVariables.getHostName(), frameworkProperties, userInfo);
            } else {
                generatedSecureServiceURL = new ProductUrlGeneratorUtil().
                        getHttpsServiceURL(workerVariables.getHttpsPort(), workerVariables.getNhttpsPort(),
                                          workerVariables.getHostName(), frameworkProperties, userInfo);
            }
        } else {
            if (productSetter.getNhttpPort() != null) { //if port is nhttp port
                generatedSecureServiceURL = new ProductUrlGeneratorUtil().
                        getHttpsServiceURL(productSetter.getHttpsPort(), productSetter.getNhttpsPort(), managerHostName, frameworkProperties, userInfo);
            } else {
                generatedSecureServiceURL = new ProductUrlGeneratorUtil().
                        getHttpsServiceURL(productSetter.getHttpsPort(), productSetter.getNhttpsPort(), managerHostName, frameworkProperties, userInfo);
            }
        }
        return generatedSecureServiceURL;
    }

    private String getSecureServiceURL(FrameworkProperties frameworkProperties,
                                 ProductVariables productSetter, UserInfo userInfo) {
        String generatedSecureServiceURL;
        if (productSetter.getNhttpsPort() != null) { //if port is nhttps port
            generatedSecureServiceURL = new ProductUrlGeneratorUtil().
                    getHttpsServiceURL(productSetter.getHttpPort(), productSetter.getNhttpsPort(), managerHostName, frameworkProperties, userInfo);
        } else {
            generatedSecureServiceURL = new ProductUrlGeneratorUtil().
                    getHttpsServiceURL(productSetter.getHttpsPort(), productSetter.getNhttpsPort(), managerHostName, frameworkProperties, userInfo);
        }

        return generatedSecureServiceURL;
    }


    public FrameworkSettings getFrameworkSettings() {
        FrameworkSettings frameworkSettings = new FrameworkSettings();
        EnvironmentSetter setter = new EnvironmentSetter();
        frameworkSettings.setFrameworkSettings(setter.getDataSource(), setter.getEnvironmentSettings(),
                setter.getEnvironmentVariables(), setter.getSelenium(),
                setter.getRavana(), setter.getDashboardVariables(), setter.getCoverageSettings());
        return frameworkSettings;
    }

    public ManageEnvironment build() {
        return new ManageEnvironment(this);
    }
}
