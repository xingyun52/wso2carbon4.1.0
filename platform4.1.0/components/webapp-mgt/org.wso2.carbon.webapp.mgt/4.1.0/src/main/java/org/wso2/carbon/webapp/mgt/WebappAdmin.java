/*
 *  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.webapp.mgt;

import org.apache.axis2.AxisFault;
import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.deployment.Deployer;
import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.catalina.Container;
import org.apache.catalina.core.StandardWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.utils.ArchiveManipulator;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.DataPaginator;
import org.wso2.carbon.utils.NetworkUtils;
import org.wso2.carbon.webapp.mgt.sync.ApplicationSynchronizeRequest;
import org.wso2.carbon.webapp.mgt.WebappsConstants.ApplicationOpType;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Admin service for managing webapps
 *
 */
@SuppressWarnings("unused")
public class WebappAdmin extends AbstractAdmin {

    private static final Log log = LogFactory.getLog(WebappAdmin.class);

    public WebappAdmin() {
    }

    public WebappAdmin(AxisConfiguration axisConfig) throws Exception {
        super(axisConfig);
    }

    /**
     * Get a page of started webapps
     *
     * @param webappSearchString Search string
     * @param webappState        State of the webapp.
     *                           Can be either WebappsConstants.WebappState.STARTED or
     *                           WebappsConstants.WebappState.STOPPED
     * @param webappType application type
     * @param pageNumber         The pageNumber of the page to be fetched
     * @return WebappsWrapper
     * @throws AxisFault
     */
    public WebappsWrapper getPagedWebappsSummary(String webappSearchString,
                                                 String webappState, String webappType,
                                                 int pageNumber) throws AxisFault {
        if (webappState.equalsIgnoreCase(WebappsConstants.WebappState.STARTED)) {
            return getPagedWebapps(pageNumber,
                                   getStartedWebapps(webappType, webappSearchString), webappType);
        } else if (webappState.equalsIgnoreCase(WebappsConstants.WebappState.STOPPED)) {
            return getPagedWebapps(pageNumber,
                                   getStoppedWebapps(webappType, webappSearchString), webappType);
        } else if(webappState.equalsIgnoreCase(WebappsConstants.WebappState.ALL)){
            List<WebappMetadata> webapps = getStartedWebapps(webappType, webappSearchString);
            webapps.addAll(getStoppedWebapps(webappType, webappSearchString));
            return getPagedWebapps(pageNumber, webapps, webappType);
        } else {
            throw new AxisFault("Invalid webapp state: ", webappState);
        }
    }

    /**
     * Get the details of a deplyed webapp
     *
     * @param webappFileName
     * @return
     */
    public WebappMetadata getStartedWebapp(String webappFileName) {
        WebApplicationsHolder holder = getWebappsHolder();
        WebApplication webApplication = holder.getStartedWebapps().get(webappFileName);
        WebappMetadata webappMetadata = null;
        if (webApplication != null) {
            webappMetadata = getWebapp(webApplication);
            webappMetadata.setStarted(true);
        }
        return webappMetadata;
    }

    /**
     * Get the details of an stopped webapp
     *
     * @param webappFileName
     * @return
     */
    public WebappMetadata getStoppedWebapp(String webappFileName) {
        WebApplicationsHolder holder = getWebappsHolder();
        WebApplication webApplication = holder.getStoppedWebapps().get(webappFileName);
        WebappMetadata webappMetadata = null;
        if (webApplication != null) {
            webappMetadata = getWebapp(webApplication);
            webappMetadata.setStarted(false);
        }
        return webappMetadata;
    }

    private WebappMetadata getWebapp(WebApplication webApplication) {
        WebappMetadata webappMetadata;
        webappMetadata = new WebappMetadata();

        String appContext = "/";
        for (Container container : webApplication.getContext().findChildren()) {
            if(((StandardWrapper) container).getServletName().equals("JAXServlet")) {
                appContext = (((StandardWrapper) container).findMappings())[0];
            }
        }
        if(appContext.endsWith("/*")) {
            appContext = appContext.substring(0, appContext.indexOf("/*"));
        }
        webappMetadata.setDisplayName(webApplication.getDisplayName());
        webappMetadata.setContext(webApplication.getContextName());
        webappMetadata.setServletContext(appContext);
        webappMetadata.setLastModifiedTime(webApplication.getLastModifiedTime());
        webappMetadata.setWebappFile(webApplication.getWebappFile().getName());
        webappMetadata.setState(webApplication.getState());

        WebApplication.Statistics statistics = webApplication.getStatistics();
        WebappStatistics stats = new WebappStatistics();
        stats.setActiveSessions(statistics.getActiveSessions());
        stats.setAvgSessionLifetime(statistics.getAvgSessionLifetime());
        stats.setExpiredSessions(statistics.getExpiredSessions());
        stats.setMaxActiveSessions(statistics.getMaxActiveSessions());
        stats.setMaxSessionInactivityInterval(statistics.getMaxSessionInactivityInterval());
        stats.setMaxSessionLifetime(statistics.getMaxSessionLifetime());
        stats.setRejectedSessions(statistics.getRejectedSessions());

        webappMetadata.setStatistics(stats);
        webappMetadata.setWebappType((String) webApplication.getProperty(WebappsConstants.WEBAPP_FILTER));

        return webappMetadata;
    }

    /**
     * @param webappSearchString
     * @param webappType application type
     * @param pageNumber
     * @return
     * @throws AxisFault
     */
    public WebappsWrapper getPagedFaultyWebappsSummary(String webappSearchString,
                                                       String webappType,
                                                       int pageNumber) throws AxisFault {
        return getPagedWebapps(pageNumber, getFaultyWebapps(webappSearchString), webappType);
    }

    private WebappsWrapper getPagedWebapps(int pageNumber, List<WebappMetadata> webapps, String webappType) {
        WebApplicationsHolder webappsHolder = getWebappsHolder();
        WebappsWrapper webappsWrapper = getWebappsWrapper(webappsHolder, webapps, webappType);
        try {
            webappsWrapper.setHostName(NetworkUtils.getLocalHostname());
        } catch (SocketException e) {
            log.error("Error occurred while getting local hostname", e);
        }

        if(getConfigContext().getAxisConfiguration().getTransportIn("http") != null) {
            int httpProxyPort = CarbonUtils.getTransportProxyPort(getConfigContext(), "http");
            if (httpProxyPort != -1) {
                webappsWrapper.setHttpPort(httpProxyPort);
            } else {
                int httpPort = CarbonUtils.getTransportPort(getConfigContext(), "http");
                webappsWrapper.setHttpPort(httpPort);
            }
        }

        if(getConfigContext().getAxisConfiguration().getTransportIn("https") != null) {
            int httpsProxyPort = CarbonUtils.getTransportProxyPort(getConfigContext(), "https");
            if (httpsProxyPort != -1) {
                webappsWrapper.setHttpsPort(httpsProxyPort);
            } else {
                int httpsPort = CarbonUtils.getTransportPort(getConfigContext(), "https");
                webappsWrapper.setHttpsPort(httpsPort);
            }
        }


        sortWebapps(webapps);
        DataPaginator.doPaging(pageNumber, webapps, webappsWrapper);
        return webappsWrapper;
    }

    private void sortWebapps(List<WebappMetadata> webapps) {
        if (webapps.size() > 0) {
            Collections.sort(webapps, new Comparator<WebappMetadata>() {
                public int compare(WebappMetadata arg0, WebappMetadata arg1) {
                    return arg0.getContext().compareToIgnoreCase(arg1.getContext());
                }
            });
        }
    }

    private List<WebappMetadata> getStartedWebapps(String webappType, String webappSearchString) {
        return getWebapps(getWebappsHolder().getStartedWebapps().values(), webappType, webappSearchString);
    }

    private List<WebappMetadata> getStoppedWebapps(String webappType, String webappSearchString) {
        return getWebapps(getWebappsHolder().getStoppedWebapps().values(), webappType, webappSearchString);
    }

    private List<WebappMetadata> getWebapps(Collection<WebApplication> allWebapps,
                                            String webappType, String webappsSearchString) {
        List<WebappMetadata> webapps = new ArrayList<WebappMetadata>();
        for (WebApplication webapp : allWebapps) {
            if (!doesWebappSatisfySearchString(webapp, webappsSearchString)) {
                continue;
            }
            // Check whether this is a generic webapp, if not ignore..
            if (!isWebappRelevant(webapp, webappType)) {
                continue;
            }
            String appContext = "/";
            for (Container container : webapp.getContext().findChildren()) {
/*
                try {
                    Class servletClass = Class.forName(((StandardWrapper) container).getServletClass(), false,
                            container.getLoader().getClassLoader());
                    Class cXFServletClass = Class.forName("org.apache.cxf.transport.servlet.CXFServlet", false,
                            container.getLoader().getClassLoader());
*/
                if(((StandardWrapper) container).getServletClass().equals("org.apache.cxf.transport.servlet.CXFServlet")) {
                    appContext = (((StandardWrapper) container).findMappings())[0];
                }
                else if (((StandardWrapper) container).getServletName().toLowerCase().contains("cxf")) {
                    appContext = (((StandardWrapper) container).findMappings())[0];
                }
/*
                } catch (ClassNotFoundException e) {
                    log.warn(e);
                } catch (InstantiationException e) {
                    log.warn(e);
                } catch (IllegalAccessException e) {
                    log.warn(e);
                }
*/
            }
            if(appContext.endsWith("/*")) {
                appContext = appContext.substring(0, appContext.indexOf("/*"));
            }
            WebappMetadata webappMetadata = new WebappMetadata();
            webappMetadata.setDisplayName(webapp.getDisplayName());
            webappMetadata.setContext(webapp.getContextName());
            webappMetadata.setServletContext(appContext);
            webappMetadata.setLastModifiedTime(webapp.getLastModifiedTime());
            webappMetadata.setWebappFile(webapp.getWebappFile().getName());
            webappMetadata.setState(webapp.getState());
            WebappStatistics statistics = new WebappStatistics();
            statistics.setActiveSessions(webapp.getStatistics().getActiveSessions());
            webappMetadata.setStatistics(statistics);
            webappMetadata.setWebappType((String) webapp.getProperty(WebappsConstants.WEBAPP_FILTER));
            webapps.add(webappMetadata);
        }
        return webapps;
    }

    private List<WebappMetadata> getFaultyWebapps(String webappsSearchString) {
        WebApplicationsHolder webappsHolder = getWebappsHolder();
        if (webappsHolder == null) {
            return null;
        }
        List<WebappMetadata> webapps = new ArrayList<WebappMetadata>();
        for (WebApplication webapp : webappsHolder.getFaultyWebapps().values()) {
            if (!doesWebappSatisfySearchString(webapp, webappsSearchString)) {
                continue;
            }
            WebappMetadata webappMetadata = new WebappMetadata();
            webappMetadata.setContext(webapp.getContextName());
            webappMetadata.setLastModifiedTime(webapp.getLastModifiedTime());
            webappMetadata.setWebappFile(webapp.getWebappFile().getName());
            webappMetadata.setStarted(false); //TODO
            webappMetadata.setRunning(false); //TODO
            webappMetadata.setFaulty(true);

            // Set the fault reason
            StringWriter sw = new StringWriter();
            webapp.getFaultReason().printStackTrace(new PrintWriter(sw));
            String faultException = sw.toString();
            webappMetadata.setFaultException(faultException);

            webapps.add(webappMetadata);
        }
        return webapps;
    }

    protected boolean doesWebappSatisfySearchString(WebApplication webapp,
                                                  String searchString) {

        if (searchString != null) {
            String regex = searchString.toLowerCase().
                    replace("..?", ".?").replace("..*", ".*").
                    replaceAll("\\?", ".?").replaceAll("\\*", ".*?");

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(webapp.getContextName().toLowerCase());

            return regex.trim().length() == 0 || matcher.find();
        }

        return false;
    }

    private WebApplicationsHolder getWebappsHolder() {
        return (WebApplicationsHolder) getConfigContext().
                getProperty(CarbonConstants.WEB_APPLICATIONS_HOLDER);
    }

    private WebappsWrapper getWebappsWrapper(WebApplicationsHolder webappsHolder,
                                             List<WebappMetadata> webapps, String webappType) {
        WebappsWrapper webappsWrapper = new WebappsWrapper();
        webappsWrapper.setWebapps(webapps.toArray(new WebappMetadata[webapps.size()]));
        webappsWrapper.setNumberOfCorrectWebapps(
                getNumberOfWebapps(webappsHolder.getStartedWebapps(), webappType));
        webappsWrapper.setNumberOfFaultyWebapps(
                getNumberOfWebapps(webappsHolder.getFaultyWebapps(), webappType));
        return webappsWrapper;
    }

    private int getNumberOfWebapps(Map <String, WebApplication> webappMap, String webappType) {
        int number = 0;
        for (Map.Entry<String, WebApplication> webappEntry : webappMap.entrySet()) {
            // Check whether this is a generic webapp, if so count..
            if (isWebappRelevant(webappEntry.getValue(), webappType)) {
                number++;
            }
        }
        return number;
    }

    /**
     * This method can be used to check whether the given web app is relevant for this Webapp
     * type. Only generic webapps are relevant for this Admin service.
     *
     *
     * @param webapp - WebApplication instance
     * @param webappType  application type
     * @return - true if relevant
     */
    protected boolean isWebappRelevant(WebApplication webapp, String webappType) {
        // skip the Stratos landing page webapp 
        if (webapp.getContextName().contains("STRATOS_ROOT")) {
            return false;
        }
        String filterProp = (String) webapp.getProperty(WebappsConstants.WEBAPP_FILTER);
        // If non of the filters are set, this is a generic webapp, so return true

        if(WebappsConstants.ALL_WEBAPP_FILTER_PROP.equalsIgnoreCase(webappType)) {
            return true;
        } else if(WebappsConstants.JAX_WEBAPP_FILTER_PROP.equalsIgnoreCase(webappType)) {
            return filterProp != null &&
                   WebappsConstants.JAX_WEBAPP_FILTER_PROP.equalsIgnoreCase
                    (filterProp);
        } else if(WebappsConstants.JAGGERY_WEBAPP_FILTER_PROP.equalsIgnoreCase(webappType)) {
            return filterProp != null &&
                   WebappsConstants.JAGGERY_WEBAPP_FILTER_PROP.equalsIgnoreCase(filterProp);
        } else {
            return filterProp == null ||
                   WebappsConstants.WEBAPP_FILTER_PROP.equalsIgnoreCase(filterProp);
        }
    }

    /**
     * Delete a set of started webapps
     *
     * @param webappFileNames The names of the webapp files to be deleted
     * @throws AxisFault If an error occurs while deleting a webapp
     */
    public void deleteStartedWebapps(String[] webappFileNames) throws AxisFault {
        deleteWebapps(webappFileNames, getWebappsHolder().getStartedWebapps());
    }

    /**
     * Delete a set of stopped webapps
     *
     * @param webappFileNames The names of the webapp files to be deleted
     * @throws AxisFault If an error occurs while deleting a webapp
     */
    public void deleteStoppedWebapps(String[] webappFileNames) throws AxisFault {
        deleteWebapps(webappFileNames, getWebappsHolder().getStoppedWebapps());
    }

    /**
     * Delete a set of faulty webapps
     *
     * @param webappFileNames The names of the webapp files to be deleted
     * @throws AxisFault If an error occurs while deleting a webapp
     */
    public void deleteFaultyWebapps(String[] webappFileNames) throws AxisFault {
        deleteWebapps(webappFileNames, getWebappsHolder().getFaultyWebapps());
    }

    /**
     *Delete set of all types of webapps. (started, stopped, faulty)
     *
     * @param webappFileNames  The names of the webapp files to be deleted
     * @throws AxisFault   AxisFault If an error occurs while deleting a webapp
     */
    public void deleteAllWebApps(String[] webappFileNames) throws AxisFault {
        for (String webappFileName : webappFileNames) {
            deleteWebapp(webappFileName);
        }
    }

    /**
     * Delete a single webapp which can be in any state; started, stopped or faulty. This method
     * will search the webapp in all lists and delete it if found.
     *
     * @param webappFileName - name of the file to be deleted
     * @throws AxisFault - If an error occurs while deleting the webapp
     */
    public void deleteWebapp(String webappFileName) throws AxisFault {
        WebApplicationsHolder holder = getWebappsHolder();
        if (holder.getStartedWebapps().get(webappFileName) != null) {
            deleteStartedWebapps(new String[]{webappFileName});
        } else if (holder.getStoppedWebapps().get(webappFileName) != null) {
            deleteStoppedWebapps(new String[]{webappFileName});
        } else if (holder.getFaultyWebapps().get(webappFileName) != null) {
            deleteFaultyWebapps(new String[]{webappFileName});
        }
    }

    private void deleteWebapps(String[] webappFileNames,
                               Map<String, WebApplication> webapps) throws AxisFault {
        for (String webappFileName : webappFileNames) {
            WebApplication webapp = webapps.get(webappFileName);
            try {
                webapps.remove(webappFileName);
                webapp.delete();
            } catch (CarbonException e) {
                handleException("Could not delete webapp " + webapp, e);
            }
        }
    }

    private void undeployWebapps(String[] webappFileNames,
                                 Map<String, WebApplication> webapps) throws AxisFault {
        for (String webappFileName : webappFileNames) {
            WebApplication webapp = webapps.get(webappFileName);
            try {
                webapp.undeploy();
                webapps.remove(webappFileName);
            } catch (CarbonException e) {
                handleException("Could not delete webapp " + webapp, e);
            }
        }
    }

    /**
     * Delete all started webapps
     *
     * @throws AxisFault If an error occurs while deleting a webapp
     */
    public void deleteAllStartedWebapps() throws AxisFault {
        deleteAllWebapps(getWebappsHolder().getStartedWebapps());
    }

    /**
     * Delete all stopped webapps
     *
     * @throws AxisFault If an error occurs while deleting a webapp
     */
    public void deleteAllStoppedWebapps() throws AxisFault {
        deleteAllWebapps(getWebappsHolder().getStoppedWebapps());
    }

    /**
     * Delete all faulty webapps
     *
     * @throws AxisFault If an error occurs while deleting a webapp
     */
    public void deleteAllFaultyWebapps() throws AxisFault {
        deleteAllWebapps(getWebappsHolder().getFaultyWebapps());
    }

    private void deleteAllWebapps(Map<String, WebApplication> webapps) throws AxisFault {
        for (WebApplication webapp : webapps.values()) {
            try {
                webapp.delete();
            } catch (CarbonException e) {
                handleException("Could not delete started webapp " + webapp, e);
            }
        }
        webapps.clear();
    }

    /**
     * Reload all webapps
     */
    public void reloadAllWebapps() {
        Map<String, WebApplication> startedWebapps = getWebappsHolder().getStartedWebapps();
        String[] webappFileNames = Arrays.copyOf(startedWebapps.keySet().toArray(),
                startedWebapps.size(), String[].class);
        for (WebApplication webapp : startedWebapps.values()) {
            webapp.reload();
        }
        sendClusterSyncMessage(ApplicationOpType.RELOAD, webappFileNames);
    }

    /**
     * Reload a set of webapps
     *
     * @param webappFileNames The file names of the webapps to be reloaded
     */
    public void reloadWebapps(String[] webappFileNames) {
        for (String webappFileName : webappFileNames) {
            getWebappsHolder().getStartedWebapps().get(webappFileName).reload();
        }
        sendClusterSyncMessage(ApplicationOpType.RELOAD, webappFileNames);
    }

    /**
     * Undeploy all webapps
     *
     * @throws AxisFault If an error occurs while undeploying
     */
    public void stopAllWebapps() throws AxisFault {
        Map<String, WebApplication> startedWebapps = getWebappsHolder().getStartedWebapps();
        String[] webappFileNames = Arrays.copyOf(startedWebapps.keySet().toArray(),
                startedWebapps.size(), String[].class);
        for (WebApplication webapp : startedWebapps.values()) {
            try {
                webapp.stop();
            } catch (CarbonException e) {
                handleException("Error occurred while undeploying all webapps", e);
            }
        }
        startedWebapps.clear();
        sendClusterSyncMessage(ApplicationOpType.STOP, webappFileNames);
    }

    /**
     * Undeploy a set of webapps
     *
     * @param webappFileNames The file names of the webapps to be stopped
     * @throws AxisFault If an error occurs while undeploying
     */
    public void stopWebapps(String[] webappFileNames) throws AxisFault {
        WebApplicationsHolder webappsHolder = getWebappsHolder();
        Map<String, WebApplication> startedWebapps = webappsHolder.getStartedWebapps();
        for (String webappFileName : webappFileNames) {
            try {
                WebApplication webApplication = startedWebapps.get(webappFileName);
                if(webApplication != null) {
                    webappsHolder.stopWebapp(webApplication);
                }
            } catch (CarbonException e) {
                handleException("Error occurred while undeploying webapps", e);
            }
        }
        sendClusterSyncMessage(ApplicationOpType.STOP, webappFileNames);
    }

    /**
     * Redeploy all webapps
     *
     * @throws org.apache.axis2.AxisFault If an error occurs while restarting webapps
     */
    public void startAllWebapps() throws AxisFault {
        Map<String, WebApplication> stoppedWebapps = getWebappsHolder().getStoppedWebapps();
        String[] webappFileNames = Arrays.copyOf(stoppedWebapps.keySet().toArray(),
                stoppedWebapps.size(), String[].class);
        Deployer webappDeployer =
                ((DeploymentEngine) getAxisConfig().getConfigurator()).getDeployer(WebappsConstants
                        .WEBAPP_DEPLOYMENT_FOLDER, WebappsConstants.WEBAPP_EXTENSION);
        for (WebApplication webapp : stoppedWebapps.values()) {
            startWebapp(stoppedWebapps, webapp);
        }
        stoppedWebapps.clear();
        sendClusterSyncMessage(ApplicationOpType.START, webappFileNames);
    }

    /**
     * Redeploy a set of webapps
     *
     * @param webappFileNames The file names of the webapps to be restarted
     * @throws org.apache.axis2.AxisFault If a deployment error occurs
     */
    public void startWebapps(String[] webappFileNames) throws AxisFault {
        WebApplicationsHolder webappsHolder = getWebappsHolder();
        Map<String, WebApplication> stoppedWebapps = webappsHolder.getStoppedWebapps();
        Deployer webappDeployer =
                ((DeploymentEngine) getAxisConfig().getConfigurator()).getDeployer(WebappsConstants
                        .WEBAPP_DEPLOYMENT_FOLDER, WebappsConstants.WEBAPP_EXTENSION);
        for (String webappFileName : webappFileNames) {
            WebApplication webapp = stoppedWebapps.get(webappFileName);
            if(webapp!= null){
                startWebapp(stoppedWebapps, webapp);
            }
        }
        sendClusterSyncMessage(ApplicationOpType.START, webappFileNames);
    }

    private void startWebapp(Map<String, WebApplication> stoppedWebapps,
                             WebApplication webapp) throws AxisFault {
        try {
            boolean started = webapp.start();
            if (started) {
                String webappFileName = webapp.getWebappFile().getName();
                stoppedWebapps.remove(webappFileName);
                WebApplicationsHolder webappsHolder = getWebappsHolder();
                Map<String, WebApplication> startedWebapps = webappsHolder.getStartedWebapps();
                startedWebapps.put(webappFileName, webapp);
            }
        } catch (CarbonException e) {
            String msg = "Cannot start webapp " + webapp;
            log.error(msg, e);
            throw new AxisFault(msg, e);
        }
    }

    /**
     * Get all active sessions of a webapp
     *
     * @param webappFileName The names of the webapp file
     * @param pageNumber     The number of the page to fetch, starting with 0
     * @return The session array
     */
    public SessionsWrapper getActiveSessions(String webappFileName, int pageNumber) {
        WebApplication webapp = getWebappsHolder().getStartedWebapps().get(webappFileName);
        List<SessionMetadata> sessionMetadataList = new ArrayList<SessionMetadata>();
        int numOfActiveSessions = 0;
        if (webapp != null) {
            List<WebApplication.HttpSession> sessions = webapp.getSessions();
            numOfActiveSessions = sessions.size();
            for (WebApplication.HttpSession session : sessions) {
                sessionMetadataList.add(new SessionMetadata(session));
            }
        }
        sortSessions(sessionMetadataList);
        SessionsWrapper sessionsWrapper = new SessionsWrapper(sessionMetadataList);
        DataPaginator.doPaging(pageNumber, sessionMetadataList, sessionsWrapper);
        sessionsWrapper.setWebappFileName(webappFileName);
        sessionsWrapper.setNumberOfActiveSessions(numOfActiveSessions);
        return sessionsWrapper;
    }

    private void sortSessions(List<SessionMetadata> sessions) {
        if (sessions.size() > 0) {
            Collections.sort(sessions, new Comparator<SessionMetadata>() {
                public int compare(SessionMetadata arg0, SessionMetadata arg1) {
                    return (int) (arg0.getLastAccessedTime() - arg1.getLastAccessedTime());
                }
            });
        }
    }

    /**
     * Expire all sessions in all webapps
     */
    public void expireSessionsInAllWebapps() {
        Map<String, WebApplication> webapps = getWebappsHolder().getStartedWebapps();
        for (WebApplication webapp : webapps.values()) {
            webapp.expireAllSessions();
        }
    }

    /**
     * Expire all sessions in specified webapps
     *
     * @param webappFileNames The file names of the webapps whose sessions should be expired
     */
    public void expireSessionsInWebapps(String[] webappFileNames) {
        Map<String, WebApplication> webapps = getWebappsHolder().getStartedWebapps();
        for (String webappFileName : webappFileNames) {
            WebApplication webapp = webapps.get(webappFileName);
            webapp.expireAllSessions();
        }
    }

    /**
     * Expire all sessions in the specified webapp which has a
     * lifetime >= <code>maxSessionLifetimeMillis</code>
     *
     * @param webappFileName           The file name of the webapp whose sessions should be expired
     * @param maxSessionLifetimeMillis The max allowed lifetime for the sessions
     */
    public void expireSessionsInWebapp(String webappFileName, long maxSessionLifetimeMillis) {
        Map<String, WebApplication> webapps = getWebappsHolder().getStartedWebapps();
        WebApplication webapp = webapps.get(webappFileName);
        webapp.expireSessions(maxSessionLifetimeMillis);
    }

    /**
     * Expire a given session in a webapp
     *
     * @param webappFileName The file name of the webapp whose sessions should be expired
     * @param sessionIDs     Array of session IDs
     * @throws org.apache.axis2.AxisFault If an error occurs while retrieving sessions
     */
    public void expireSessions(String webappFileName, String[] sessionIDs) throws AxisFault {
        Map<String, WebApplication> webapps = getWebappsHolder().getStartedWebapps();
        WebApplication webapp = webapps.get(webappFileName);
        try {
            webapp.expireSessions(sessionIDs);
        } catch (CarbonException e) {
            handleException("Cannot expire specified sessions in webapp " + webappFileName, e);
        }
    }

    /**
     * Expire a given session in a webapp
     *
     * @param webappFileName The file name of the webapp whose sessions should be expired
     */
    public void expireAllSessions(String webappFileName) {
        Map<String, WebApplication> webapps = getWebappsHolder().getStartedWebapps();
        WebApplication webapp = webapps.get(webappFileName);
        webapp.expireAllSessions();
    }

    /**
     * Upload a webapp
     *
     * @param webappUploadDataList Array of data representing the webapps that are to be uploaded
     * @return true - if upload was successful
     * @throws AxisFault If an error occurrs while uploading
     */
    public boolean uploadWebapp(WebappUploadData[] webappUploadDataList) throws AxisFault {
        AxisConfiguration axisConfig = getAxisConfig();
        File webappsDir = new File(getWebappDeploymentDirPath(WebappsConstants.WEBAPP_FILTER_PROP));
        if (!webappsDir.exists() && !webappsDir.mkdirs()) {
            log.warn("Could not create directory " + webappsDir.getAbsolutePath());
        }

        for (WebappUploadData uploadData : webappUploadDataList) {
            String fileName = uploadData.getFileName();
            fileName = fileName.substring(fileName.lastIndexOf(System.getProperty("file.separator"))+1);
            File destFile = new File(webappsDir, fileName);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(destFile);
                uploadData.getDataHandler().writeTo(fos);
            } catch (IOException e) {
                handleException("Error occured while uploading the webapp " + fileName, e);
            } finally {
                try {
                    if (fos != null) {
                        fos.flush();
                        fos.close();
                    }
                } catch (IOException e) {
                    log.warn("Could not close file " + destFile.getAbsolutePath());
                }
            }
        }
        return true;
    }

    protected String getWebappDeploymentDirPath(String webappType) {
        String webappDeploymentDir;
        if(WebappsConstants.JAX_WEBAPP_FILTER_PROP.equalsIgnoreCase(webappType)) {
            webappDeploymentDir = WebappsConstants.JAX_WEBAPP_REPO;
        } else if(WebappsConstants.JAGGERY_WEBAPP_FILTER_PROP.equalsIgnoreCase(webappType)) {
            webappDeploymentDir = WebappsConstants.JAGGERY_WEBAPP_REPO;
        } else {
            webappDeploymentDir = WebappsConstants.WEBAPP_DEPLOYMENT_FOLDER;
        }
        return getAxisConfig().getRepository().getPath() + File.separator + webappDeploymentDir;
    }

    private void handleException(String msg, Exception e) throws AxisFault {
        log.error(msg, e);
        throw new AxisFault(msg, e);
    }

    /**
     * Downloads the webapp archive (.war) file
     * @param fileName name of the .war that needs to be downloaded
     * @param webappType application type
     * @return the corresponding data handler of the .war that needs to be downloaded
     */
    public DataHandler downloadWarFileHandler(String fileName, String webappType) {
        String repoPath = getAxisConfig().getRepository().getPath();
        String appsPath = getWebappDeploymentDirPath(webappType) + File.separator + fileName;

        File webAppFile = new File(appsPath);
        DataHandler handler = null;

        if (webAppFile.isDirectory()) {
            String zipTo = "tmp" + File.separator + fileName + ".zip";
            File fDownload = new File(zipTo);
            ArchiveManipulator archiveManipulator = new ArchiveManipulator();
            synchronized (this) {
                try {
                    archiveManipulator.archiveDir(zipTo, webAppFile.getAbsolutePath());
                    FileDataSource datasource = new FileDataSource(fDownload);
                    handler = new DataHandler(datasource);
                } catch (IOException e) {
                    log.error("Error downloading WAR file.", e);
                }
            }
        } else {
            FileDataSource datasource = new FileDataSource(new File(appsPath));
            handler = new DataHandler(datasource);
        }
        return handler;
    }

    /**
     * check if unpack wars enabled
     *
     * @return true if enabled.
     */
    public boolean isUnpackWARs(){
        return TomcatUtil.checkUnpackWars();
    }

    private void sendClusterSyncMessage(ApplicationOpType applicationOpType, String[] webappFileNames) {
        // For sending clustering messages we need to use the super-tenant's AxisConfig (Main Server
        // AxisConfiguration) because we are using the clustering facility offered by the ST in the
        // tenants
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        ClusteringAgent clusteringAgent =
                DataHolder.getServerConfigContext().getAxisConfiguration().getClusteringAgent();
        if (clusteringAgent != null) {
            int numberOfRetries = 0;
            UUID messageId = UUID.randomUUID();
            ApplicationSynchronizeRequest request =
                    new ApplicationSynchronizeRequest(tenantId, tenantDomain, messageId,
                            applicationOpType, webappFileNames);
            while (numberOfRetries < 60) {
                try {
                    clusteringAgent.sendMessage(request, true);
                    log.info("Sent [" + request + "]");
                    break;
                } catch (ClusteringFault e) {
                    numberOfRetries++;
                    if (numberOfRetries < 60) {
                        log.warn("Could not send SynchronizeRepositoryRequest for tenant " +
                                tenantId + ". Retry will be attempted in 2s. Request: " + request, e);
                    } else {
                        log.error("Could not send SynchronizeRepositoryRequest for tenant " +
                                tenantId + ". Several retries failed. Request:" + request, e);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}