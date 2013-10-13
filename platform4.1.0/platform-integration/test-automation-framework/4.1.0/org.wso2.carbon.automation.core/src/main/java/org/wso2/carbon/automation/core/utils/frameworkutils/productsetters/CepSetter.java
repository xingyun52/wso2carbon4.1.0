package org.wso2.carbon.automation.core.utils.frameworkutils.productsetters;

import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.environmentutils.ProductUrlGeneratorUtil;
import org.wso2.carbon.automation.core.utils.frameworkutils.EnvironmentSetter;
import org.wso2.carbon.automation.core.utils.frameworkutils.productvariables.ProductVariables;
import org.wso2.carbon.automation.core.utils.frameworkutils.productvariables.WorkerVariables;

import java.util.Properties;

public class CepSetter extends EnvironmentSetter {
    ProductVariables productVariables = new ProductVariables();
    WorkerVariables workerVariables = new WorkerVariables();
    EnvironmentBuilder environmentBuilder = new EnvironmentBuilder();
    public String managerHostName;
    public String managerHttpPort;
    public String managerHttpsPort;
    public String managerQpidPort;
    public String managerWebContextRoot;
    public String workerHostName = null;
    public String workerHttpPort = null;
    public String workerHttpsPort = null;
    public String workerQpidPort = null;
    public String workerWebContextRoot = null;
    public Properties properties;

    public CepSetter() {
        this.properties = new ProductUrlGeneratorUtil().getStream();

        String hostNames;
        if (Boolean.parseBoolean(prop.getProperty("stratos.test"))) {
            hostNames = (properties.getProperty("cep.service.host.name", "process.stratoslive.wso2.com"));
        } else {
            hostNames = (properties.getProperty("cep.host.name", "localhost"));
        }
        String httpPorts = (prop.getProperty("cep.http.port", "9763"));
        String httpsPorts = (prop.getProperty("cep.https.port", "9443"));
        String webContextRoots = (prop.getProperty("cep.webContext.root", null));
        String qpidPorts = prop.getProperty("cep.qpid.port", "5672");

        if (hostNames.contains(",")) {
            managerHostName = hostNames.split(",")[0];
            workerHostName = hostNames.split(",")[1];
        } else {
            managerHostName = hostNames;
        }
        if (httpPorts.contains(",")) {
            managerHttpPort = httpPorts.split(",")[0];
            workerHttpPort = httpPorts.split(",")[1];
        } else {
            managerHttpPort = httpPorts;
        }
        if (httpsPorts.contains(",")) {
            managerHttpsPort = httpsPorts.split(",")[0];
            workerHttpsPort = httpsPorts.split(",")[1];
        } else {
            managerHttpsPort = httpsPorts;
        }
        if (webContextRoots != null) {
            if (webContextRoots.contains(",")) {
                managerWebContextRoot = webContextRoots.split(",")[0];
                workerWebContextRoot = webContextRoots.split(",")[1];
            } else {
                managerWebContextRoot = webContextRoots;
            }
        } else {
            managerWebContextRoot = webContextRoots;
        }
        if (qpidPorts.contains(",")) {
            managerQpidPort = qpidPorts.split(",")[0];
            workerQpidPort = qpidPorts.split(",")[1];
        } else {
            managerQpidPort = qpidPorts;
        }
    }


    public ProductVariables getProductVariables() {
        ProductUrlGeneratorUtil productUrlGeneratorUtil = new ProductUrlGeneratorUtil();
        this.properties = new ProductUrlGeneratorUtil().getStream();

        productVariables.setProductVariables
                (this.managerHostName, this.managerHttpPort, this.managerHttpsPort, this.managerWebContextRoot, this.managerQpidPort,
                 productUrlGeneratorUtil.getBackendUrl(managerHttpsPort, managerHostName,
                                                       managerWebContextRoot));
        return productVariables;
    }

    public WorkerVariables getWorkerVariables() {
        ProductUrlGeneratorUtil productUrlGeneratorUtil = new ProductUrlGeneratorUtil();
        this.properties = new ProductUrlGeneratorUtil().getStream();

        if (environmentBuilder.getFrameworkSettings().getEnvironmentSettings().isClusterEnable()) {

            workerVariables.setWorkerVariables(this.managerHostName, this.managerHttpPort, this.managerHttpsPort, this.managerWebContextRoot, this.managerQpidPort,
                                               productUrlGeneratorUtil.getBackendUrl(managerHttpsPort, managerHostName,
                                                                                     managerWebContextRoot));
        }
        return workerVariables;
    }
}
