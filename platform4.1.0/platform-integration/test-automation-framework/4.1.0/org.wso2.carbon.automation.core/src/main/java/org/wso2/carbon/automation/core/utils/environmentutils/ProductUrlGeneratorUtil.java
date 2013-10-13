package org.wso2.carbon.automation.core.utils.environmentutils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkFactory;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ProductUrlGeneratorUtil {
    public static final Properties prop = new Properties();
    private static final Log log = LogFactory.getLog(ProductUrlGeneratorUtil.class);

    static {
        setStream();
    }

    public static Properties setStream() {
        String automationPropertiesFile = null;
        ProductConstant.init();
        try {
            automationPropertiesFile = ProductConstant.SYSTEM_TEST_SETTINGS_LOCATION + File.separator + "automation.properties";
            File automationPropertyFile = new File(automationPropertiesFile);
            InputStream inputStream = null;
            if (automationPropertyFile.exists()) {
                inputStream = new FileInputStream(automationPropertyFile);
            }

            if (inputStream != null) {
                prop.load(inputStream);
                inputStream.close();
                return prop;
            }

        } catch (IOException ignored) {
            log.error("automation.properties file not found, please check your configuration");
        }

        return null;
    }

    public Properties getStream() {
        return prop;
    }

    public String getHttpServiceURL(String httpPort, String nhttpPort, String hostName,
                                    FrameworkProperties frameworkProperties, UserInfo userInfo) {
        if (frameworkProperties.getEnvironmentSettings().is_runningOnStratos()) {
            return getHttpServiceURLOfStratos(httpPort, nhttpPort, hostName, frameworkProperties, userInfo);
        } else {
            return getHttpServiceURLOfProduct(httpPort, nhttpPort, hostName, frameworkProperties);
        }
    }

    public String getHttpsServiceURL(String httpsPort, String nhttpsPort, String hostName,
                                     FrameworkProperties frameworkProperties, UserInfo userInfo) {
        if (frameworkProperties.getEnvironmentSettings().is_runningOnStratos()) {
            return getHttpsServiceURLOfStratos(httpsPort, nhttpsPort, hostName, frameworkProperties, userInfo);
        } else {
            return getHttpsServiceURLOfProduct(httpsPort, nhttpsPort, hostName, frameworkProperties);
        }
    }

    public String getHttpServiceURLOfProduct(String httpPort, String nhttpPort, String hostName,
                                             FrameworkProperties frameworkProperties) {
        String serviceURL;
        boolean webContextEnabled = frameworkProperties.getEnvironmentSettings().isEnableCarbonWebContext();
        boolean portEnabled = frameworkProperties.getEnvironmentSettings().isEnablePort();
        String webContextRoot = frameworkProperties.getProductVariables().getWebContextRoot();
        if (nhttpPort != null) {
            httpPort = nhttpPort;
        }

        if (portEnabled && webContextEnabled) {
            if (webContextRoot != null && httpPort != null) {
                serviceURL = "http://" + hostName + ":" + httpPort + "/" + webContextRoot + "/" + "services";
            } else if (webContextRoot == null && httpPort != null) {
                serviceURL = "http://" + hostName + ":" + httpPort + "/" + "services";
            } else if (webContextRoot == null) {
                serviceURL = "http://" + hostName + "/" + "services/";
            } else {
                serviceURL = "http://" + hostName + "/" + webContextRoot + "/" + "services";
            }
        } else if (!portEnabled && webContextEnabled) {
            serviceURL = "http://" + hostName + "/" + webContextRoot + "/" + "services";
        } else if (portEnabled && !webContextEnabled) {
            serviceURL = "http://" + hostName + ":" + httpPort + "/" + "services";
        } else {
            serviceURL = "http://" + hostName + "/" + "services";
        }
        return serviceURL;

    }


    public String getHttpServiceURLOfStratos(String httpPort, String nhttpPort, String hostName,
                                             FrameworkProperties frameworkProperties,
                                             UserInfo info) {
        String serviceURL;
        boolean webContextEnabled = frameworkProperties.getEnvironmentSettings().isEnableCarbonWebContext();
        boolean portEnabled = frameworkProperties.getEnvironmentSettings().isEnablePort();
        String webContextRoot = frameworkProperties.getProductVariables().getWebContextRoot();
        String superTenantID = "0";
        String tenantDomain;

        if (info.getUserId().equals(superTenantID)) { /*skip the domain if user is super admin */
            tenantDomain = null;
        } else {
            tenantDomain = info.getUserName().split("@")[1];
        }
        if (nhttpPort == null) {
            if (portEnabled && webContextEnabled) {
                if (webContextRoot != null && httpPort != null) {
                    serviceURL = "http://" + hostName + ":" + httpPort + "/" + webContextRoot + "/" + "services/t/" + tenantDomain;
                } else if (webContextRoot == null && httpPort != null) {
                    serviceURL = "http://" + hostName + ":" + httpPort + "/" + "services/t/" + tenantDomain;
                } else if (webContextRoot == null) {
                    serviceURL = "http://" + hostName + "/" + "services/t/" + tenantDomain;
                } else {
                    serviceURL = "http://" + hostName + "/" + webContextRoot + "/" + "services/t/" + tenantDomain;
                }
            } else if (!portEnabled && webContextEnabled) {
                serviceURL = "http://" + hostName + "/" + webContextRoot + "/" + "services/t/" + tenantDomain;
            } else if (portEnabled && !webContextEnabled) {
                serviceURL = "http://" + hostName + ":" + httpPort + "/" + "services/t/" + tenantDomain;
            } else {
                serviceURL = "http://" + hostName + "/" + "services/t/" + tenantDomain;
            }
        } else {
            if (webContextEnabled) {
                if (webContextRoot == null) {
                    serviceURL = "http://" + hostName + ":" + nhttpPort + "/" + "services/t/" + tenantDomain;
                } else {
                    serviceURL = "http://" + hostName + ":" + nhttpPort + "/" + webContextRoot + "/" + "services/t/" + tenantDomain;

                }

            } else {
                serviceURL = "http://" + hostName + ":" + nhttpPort + "/" + "services/t/" + tenantDomain;
            }
        }
        return serviceURL;
    }

    public String getHttpsServiceURLOfProduct(String httpsPort, String nhttpsPort, String hostName,
                                              FrameworkProperties frameworkProperties) {
        String serviceURL;
        boolean webContextEnabled = frameworkProperties.getEnvironmentSettings().isEnableCarbonWebContext();
        boolean portEnabled = frameworkProperties.getEnvironmentSettings().isEnablePort();
        String webContextRoot = frameworkProperties.getProductVariables().getWebContextRoot();
        if (nhttpsPort != null) {
            httpsPort = nhttpsPort;
        }

        if (portEnabled && webContextEnabled) {
            if (webContextRoot != null && httpsPort != null) {
                serviceURL = "https://" + hostName + ":" + httpsPort + "/" + webContextRoot + "/" + "services";
            } else if (webContextRoot == null && httpsPort != null) {
                serviceURL = "https://" + hostName + ":" + httpsPort + "/" + "services";
            } else if (webContextRoot == null) {
                serviceURL = "https://" + hostName + "/" + "services/";
            } else {
                serviceURL = "https://" + hostName + "/" + webContextRoot + "/" + "services";
            }
        } else if (!portEnabled && webContextEnabled) {
            serviceURL = "https://" + hostName + "/" + webContextRoot + "/" + "services";
        } else if (portEnabled && !webContextEnabled) {
            serviceURL = "https://" + hostName + ":" + httpsPort + "/" + "services";
        } else {
            serviceURL = "https://" + hostName + "/" + "services";
        }
        return serviceURL;

    }


    public String getHttpsServiceURLOfStratos(String httpsPort, String nhttpsPort, String hostName,
                                              FrameworkProperties frameworkProperties,
                                              UserInfo info) {
        String serviceURL;
        boolean webContextEnabled = frameworkProperties.getEnvironmentSettings().isEnableCarbonWebContext();
        boolean portEnabled = frameworkProperties.getEnvironmentSettings().isEnablePort();
        String webContextRoot = frameworkProperties.getProductVariables().getWebContextRoot();
        String superTenantID = "0";
        String tenantDomain;

        if (info.getUserId().equals(superTenantID)) { /*skip the domain if user is super admin */
            tenantDomain = null;
        } else {
            tenantDomain = info.getUserName().split("@")[1];
        }
        if (nhttpsPort == null) {
            if (portEnabled && webContextEnabled) {
                if (webContextRoot != null && httpsPort != null) {
                    serviceURL = "https://" + hostName + ":" + httpsPort + "/" + webContextRoot + "/" + "services/t/" + tenantDomain;
                } else if (webContextRoot == null && httpsPort != null) {
                    serviceURL = "https://" + hostName + ":" + httpsPort + "/" + "services/t/" + tenantDomain;
                } else if (webContextRoot == null) {
                    serviceURL = "https://" + hostName + "/" + "services/t/" + tenantDomain;
                } else {
                    serviceURL = "https://" + hostName + "/" + webContextRoot + "/" + "services/t/" + tenantDomain;
                }
            } else if (!portEnabled && webContextEnabled) {
                serviceURL = "https://" + hostName + "/" + webContextRoot + "/" + "services/t/" + tenantDomain;
            } else if (portEnabled && !webContextEnabled) {
                serviceURL = "https://" + hostName + ":" + httpsPort + "/" + "services/t/" + tenantDomain;
            } else {
                serviceURL = "https://" + hostName + "/" + "services/t/" + tenantDomain;
            }
        } else {
            if (webContextEnabled) {
                if (webContextRoot == null) {
                    serviceURL = "https://" + hostName + ":" + nhttpsPort + "/" + "services/t/" + tenantDomain;
                } else {
                    serviceURL = "https://" + hostName + ":" + nhttpsPort + "/" + webContextRoot + "/" + "services/t/" + tenantDomain;

                }

            } else {
                serviceURL = "https://" + hostName + ":" + nhttpsPort + "/" + "services/t/" + tenantDomain;
            }
        }
        return serviceURL;
    }



    public String getBackendUrl(String httpsPort, String hostName, String webContextRoot) {
        String backendUrl;
        boolean webContextEnabled = Boolean.parseBoolean(prop.getProperty("carbon.web.context.enable"));
        boolean portEnabled = Boolean.parseBoolean(prop.getProperty("port.enable"));

        if (portEnabled && webContextEnabled) {
            if (webContextRoot != null && httpsPort != null) {
                backendUrl = "https://" + hostName + ":" + httpsPort + "/" + webContextRoot + "/" + "services/";
            } else if (webContextRoot == null && httpsPort != null) {
                backendUrl = "https://" + hostName + ":" + httpsPort + "/" + "services/";
            } else if (webContextRoot == null) {
                backendUrl = "https://" + hostName + "/" + "services/";
            } else {
                backendUrl = "https://" + hostName + "/" + webContextRoot + "/" + "services/";
            }
        } else if (!portEnabled && webContextEnabled) {
            backendUrl = "https://" + hostName + "/" + webContextRoot + "/" + "services/";
        } else if (portEnabled && !webContextEnabled) {
            backendUrl = "https://" + hostName + ":" + httpsPort + "/" + "services/";
        } else {
            backendUrl = "https://" + hostName + "/" + "services/";
        }
        return backendUrl;
    }

    public String getWebappURL(String httpPort, String hostName,
                               FrameworkProperties frameworkProperties, UserInfo user) {
        String webAppURL;
        boolean portEnabled = frameworkProperties.getEnvironmentSettings().isEnablePort();

        if (frameworkProperties.getEnvironmentSettings().is_runningOnStratos()) {
            if (portEnabled && httpPort != null) {
                webAppURL = "http://" + hostName + ":" + httpPort + "/t/" + user.getUserName().split("@")[1] + "/webapps";
            } else {
                webAppURL = "http://" + hostName + "/t/" + user.getUserName().split("@")[1] + "/webapps";
            }
        } else {
            if (portEnabled && httpPort != null) {
                webAppURL = "http://" + hostName + ":" + httpPort;
            } else {
                webAppURL = "http://" + hostName;
            }
        }
        return webAppURL;
    }

    public static String getServiceHomeURL(String productName) {
        String indexURL;
        FrameworkProperties properties = FrameworkFactory.getFrameworkProperties(productName);
        boolean webContextEnabled = Boolean.parseBoolean(prop.getProperty("carbon.web.context.enable"));
        boolean portEnabled = Boolean.parseBoolean(prop.getProperty("port.enable"));
        String webContextRoot = properties.getProductVariables().getWebContextRoot();
        String httpsPort = properties.getProductVariables().getHttpsPort();
        String hostName = properties.getProductVariables().getHostName();

        if (portEnabled && webContextEnabled) {
            if (webContextRoot != null && httpsPort != null) {
                indexURL = "https://" + hostName + ":" + httpsPort + "/" + webContextRoot + "/" +
                           "home" + "/" + "index.html";
            } else if (webContextRoot == null && httpsPort != null) {
                indexURL = "https://" + hostName + ":" + httpsPort + "/" + "home" + "/" + "index.html";
            } else if (webContextRoot == null) {
                indexURL = "https://" + hostName + "/" + "home" + "/" + "index.html";
            } else {
                indexURL = "https://" + hostName + "/" + webContextRoot + "/" + "home" + "/" + "index.html";
            }
        } else if (!portEnabled && webContextEnabled) {
            indexURL = "https://" + hostName + "/" + webContextRoot + "/" + "home" + "/" + "index.html";
        } else if (portEnabled && !webContextEnabled) {
            indexURL = "https://" + hostName + ":" + httpsPort + "/" + "home" + "/" + "index.html";
        } else {
            indexURL = "https://" + hostName + "/" + "home" + "/" + "index.html";
        }
        return indexURL;
    }

    public static String getProductHomeURL(String productName) {
        String indexURL;
        FrameworkProperties properties = FrameworkFactory.getFrameworkProperties(productName);
        boolean webContextEnabled = Boolean.parseBoolean(prop.getProperty("carbon.web.context.enable"));
        boolean portEnabled = Boolean.parseBoolean(prop.getProperty("port.enable"));
        String webContextRoot = properties.getProductVariables().getWebContextRoot();
        String httpsPort = properties.getProductVariables().getHttpsPort();
        String hostName = properties.getProductVariables().getHostName();

        if (portEnabled && webContextEnabled) {
            if (webContextRoot != null && httpsPort != null) {
                indexURL = "https://" + hostName + ":" + httpsPort + "/" + webContextRoot + "/" +
                           "carbon";
            } else if (webContextRoot == null && httpsPort != null) {
                indexURL = "https://" + hostName + ":" + httpsPort + "/" + "carbon";
            } else if (webContextRoot == null) {
                indexURL = "https://" + hostName + "/" + "carbon";
            } else {
                indexURL = "https://" + hostName + "/" + webContextRoot + "/" + "carbon";
            }
        } else if (!portEnabled && webContextEnabled) {
            indexURL = "https://" + hostName + "/" + webContextRoot + "/" + "carbon";
        } else if (portEnabled && !webContextEnabled) {
            indexURL = "https://" + hostName + ":" + httpsPort + "/" + "carbon";
        } else {
            indexURL = "https://" + hostName + "/" + "carbon";
        }
        return indexURL;
    }

    public static String getRemoteRegistryURLOfProducts(String httpsPort, String hostName,
                                                        String webContextRoot) {
        String remoteRegistryURL;
        boolean webContextEnabled = Boolean.parseBoolean(prop.getProperty("carbon.web.context.enable"));
        boolean portEnabled = Boolean.parseBoolean(prop.getProperty("port.enable"));

        if (portEnabled && webContextEnabled) {
            if (webContextRoot != null && httpsPort != null) {
                remoteRegistryURL = "https://" + hostName + ":" + httpsPort + "/" + webContextRoot + "/" + "registry/";
            } else if (webContextRoot == null && httpsPort != null) {
                remoteRegistryURL = "https://" + hostName + ":" + httpsPort + "/" + "registry/";
            } else if (webContextRoot == null) {
                remoteRegistryURL = "https://" + hostName + "/" + "services/";
            } else {
                remoteRegistryURL = "https://" + hostName + "/" + webContextRoot + "/" + "registry/";
            }
        } else if (!portEnabled && webContextEnabled) {
            remoteRegistryURL = "https://" + hostName + "/" + webContextRoot + "/" + "registry/";
        } else if (portEnabled && !webContextEnabled) {
            remoteRegistryURL = "https://" + hostName + ":" + httpsPort + "/" + "registry/";
        } else {
            remoteRegistryURL = "https://" + hostName + "/" + "registry/";
        }
        return remoteRegistryURL;
    }

    public static String getRemoteRegistryURLOfStratos(String httpsPort, String hostName,
                                                       FrameworkProperties frameworkProperties,
                                                       UserInfo info) {
        String remoteRegistryURL;
        boolean webContextEnabled = frameworkProperties.getEnvironmentSettings().isEnableCarbonWebContext();
        boolean portEnabled = frameworkProperties.getEnvironmentSettings().isEnablePort();
        String webContextRoot = frameworkProperties.getProductVariables().getWebContextRoot();
        String superTenantID = "0";
        String tenantDomain;

        if (info.getUserId().equals(superTenantID)) { /*skip the domain if user is super admin */
            tenantDomain = null;
        } else {
            tenantDomain = info.getUserName().split("@")[1];
        }

        if (portEnabled && webContextEnabled) {
            if (webContextRoot != null && httpsPort != null) {
                remoteRegistryURL = "https://" + hostName + ":" + httpsPort + "/" + webContextRoot + "/t/" + tenantDomain + "/registry/";
            } else if (webContextRoot == null && httpsPort != null) {
                remoteRegistryURL = "https://" + hostName + ":" + httpsPort + "/" + "t/" + tenantDomain + "/registry/";
            } else if (webContextRoot == null) {
                remoteRegistryURL = "https://" + hostName + "/" + "t/" + tenantDomain + "/registry";
            } else {
                remoteRegistryURL = "https://" + hostName + "/" + webContextRoot + "/" + "t/" + tenantDomain + "/registry/";
            }
        } else if (!portEnabled && webContextEnabled) {
            remoteRegistryURL = "https://" + hostName + "/" + webContextRoot + "/" + "t/" + tenantDomain + "/registry/";
        } else if (portEnabled && !webContextEnabled) {
            remoteRegistryURL = "https://" + hostName + ":" + httpsPort + "/" + "t/" + tenantDomain + "/registry/";
        } else {
            remoteRegistryURL = "https://" + hostName + "/" + "t/" + tenantDomain + "/registry/";
        }
        return remoteRegistryURL;
    }
}