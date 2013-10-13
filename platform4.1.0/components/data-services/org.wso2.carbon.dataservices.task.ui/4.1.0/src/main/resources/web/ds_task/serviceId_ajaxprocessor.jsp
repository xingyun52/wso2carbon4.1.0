<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.apache.commons.logging.Log" %>
<%@ page import="org.apache.commons.logging.LogFactory" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.dataservices.task.ui.DSTaskClient" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<fmt:bundle basename="org.wso2.carbon.dataservices.task.ui.i18n.Resources">
    <%
        DSTaskClient client;
        Log log = LogFactory.getLog(this.getClass());

        String backendServerUrl = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.getServletContext().
                getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        try {
            client = new DSTaskClient(cookie, backendServerUrl, configContext);

            String serviceId = request.getParameter("serviceId");
            String[] triggerList = client.getNoParamDSOperations(serviceId);
            StringBuffer triggerListAsString = new StringBuffer();

            if (triggerList != null) {
                for (String trigger : triggerList) {
                    triggerListAsString.append(trigger).append(",");
                }
            }
            
            response.setContentType("application/xml");
            // Set standard HTTP/1.1 no-cache headers.
            response.setHeader("Cache-Control", "no-store, max-age=0, no-cache, must-revalidate");
            // Set IE extended HTTP/1.1 no-cache headers.
            response.addHeader("Cache-Control", "post-check=0, pre-check=0");
            // Set standard HTTP/1.0 no-cache header.
            response.setHeader("Pragma", "no-cache");

            response.getWriter().write(triggerListAsString.toString());
        } catch (Exception e) {
            log.error("Error While Retrieving Trigger List", e);
        }
    %>

</fmt:bundle>