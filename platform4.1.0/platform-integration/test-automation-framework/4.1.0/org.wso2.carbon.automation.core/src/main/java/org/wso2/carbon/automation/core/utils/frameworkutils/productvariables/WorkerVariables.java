package org.wso2.carbon.automation.core.utils.frameworkutils.productvariables;

public class WorkerVariables {

    private String _workerHostName;
    private String _workerHttpPort;
    private String _workerHttpsPort;
    private String _workerWebContextRoot;
    private String _workerNhttpPort;
    private String _workerNhttpsPort;
    private String _workerQpidPort;
    private String _workerBackendUrl;

    public String getHostName() {
        return _workerHostName;
    }

    public String getHttpPort() {
        return _workerHttpPort;
    }

    public String getHttpsPort() {
        return _workerHttpsPort;
    }

    public String getWebContextRoot() {
        return _workerWebContextRoot;
    }

    public String getNhttpPort() {
        return _workerNhttpPort;
    }

    public String getNhttpsPort() {
        return _workerNhttpsPort;
    }

    public String getQpidPort() {
        return _workerQpidPort;
    }

    public String getBackendUrl() {
        return _workerBackendUrl;
    }


    public void setWorkerVariables(String hostName, String httpPort, String httpsPort,
                                    String webContextRoot, String nhttpPort, String nhttpsPort,
                                    String qpidPort, String backendUrl) {
        _workerHostName = hostName;
        _workerHttpPort = httpPort;
        _workerHttpsPort = httpsPort;
        _workerWebContextRoot = webContextRoot;
        _workerQpidPort = qpidPort;
        _workerNhttpPort = nhttpPort;
        _workerNhttpsPort = nhttpsPort;
        _workerBackendUrl = backendUrl;
    }

    public void setWorkerVariables(String hostName, String httpPort, String httpsPort,
                                    String webContextRoot, String nhttpPort, String nhttpsPort,
                                    String backendUrl) {
        _workerHostName = hostName;
        _workerHttpPort = httpPort;
        _workerHttpsPort = httpsPort;
        _workerWebContextRoot = webContextRoot;
        _workerNhttpPort = nhttpPort;
        _workerNhttpsPort = nhttpsPort;
        _workerBackendUrl = backendUrl;
    }

    public void setWorkerVariables(String hostName, String httpPort, String httpsPort,
                                    String webContextRoot, String qpidPort, String backendUrl) {
        _workerHostName = hostName;
        _workerHttpPort = httpPort;
        _workerHttpsPort = httpsPort;
        _workerWebContextRoot = webContextRoot;
        _workerQpidPort = qpidPort;
        _workerBackendUrl = backendUrl;
    }

    public void setWorkerVariables(String hostName, String httpPort, String httpsPort,
                                    String webContextRoot, String backendUrl) {
        _workerHostName = hostName;
        _workerHttpPort = httpPort;
        _workerHttpsPort = httpsPort;
        _workerWebContextRoot = webContextRoot;
        _workerBackendUrl = backendUrl;
    }
}