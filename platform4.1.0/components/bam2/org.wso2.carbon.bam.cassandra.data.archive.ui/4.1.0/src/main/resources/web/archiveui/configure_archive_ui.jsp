<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.bam.cassandra.data.archive.ui.CassandraDataArchiveAdminClient" %>
<%@ page import="org.wso2.carbon.bam.cassandra.data.archive.stub.util.ArchiveConfiguration" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>


<%@ page contentType="text/html;charset=UTF-8" language="java" %>



<fmt:bundle basename="org.wso2.carbon.bam.cassandra.data.archive.ui.i18n.Resources">
<carbon:breadcrumb
            label="cassandra.data.archive"
            resourceBundle="org.wso2.carbon.bam.cassandra.data.archive.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

<%
        String streamName = "";
        String version = "";
        String from = "";
        String to ="";

        String setConfig = request.getParameter("setConfig"); // hidden parameter to check if the form is being submitted
        if(request.getParameter("stream_name")!=null){
            streamName = request.getParameter("stream_name");
        }
        if(request.getParameter("version")!=null){
            version = request.getParameter("version");
        }
        if(request.getParameter("from")!=null){
            from = request.getParameter("from");
        }
        if(request.getParameter("to")!=null){
            to = request.getParameter("to");
        }


        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        CassandraDataArchiveAdminClient client = new CassandraDataArchiveAdminClient(
                cookie, backendServerURL, configContext, request.getLocale());

        ArchiveConfiguration archiveConfiguration = null;

        if (setConfig != null) {    // form submitted request to set eventing config
            archiveConfiguration = new ArchiveConfiguration();
            if (streamName != null && !streamName.equals("")) {
                archiveConfiguration.setStreamName(streamName);
            }
            if (version != null && !version.equals("")) {
                archiveConfiguration.setVersion(version);
            }
            if (from != null && !from.equals("")) {
                Date fromDate = new SimpleDateFormat("dd/mm/yyy").parse(from);
                archiveConfiguration.setStartDate(fromDate);
            }
            if(to!=null && !to.equals("")){
                Date toDate =  new SimpleDateFormat("dd/mm/yyy").parse(to);
                archiveConfiguration.setEndDate(toDate);
            }
        }

            try {
                if (archiveConfiguration != null && archiveConfiguration.getStreamName()!=null) {
                    client.archiveCassandraData(archiveConfiguration);
                }

%>
    <script type="text/javascript">
        /*jQuery(document).init(function () {*/
        function handleOK() {

        }

        CARBON.showInfoDialog("Configuration submitted successfully!", handleOK);
        /*});*/
    </script>

    <%
    } catch (Exception e) {
            CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
            session.setAttribute(CarbonUIMessage.ID, uiMsg);
    %>
    <jsp:include page="../admin/error.jsp"/>
    <%
    }
    %>

<div id="middle">
<h2>
    <fmt:message key="bam.cassandra.data.archive.config"/>
</h2>

<div id="workArea">
<div id="result"></div>
<p>&nbsp;</p>

<form action="configure_archive_ui.jsp" method="post">
<input type="hidden" name="setConfig" value="on"/>
<table width="100%" class="styledLeft" style="margin-left: 0px;">
<thead>
<tr>
    <th colspan="4">
        <fmt:message key="archive.configuration"/>
    </th>
</tr>
</thead>

<tr>
    <td><fmt:message key="stream.name"/></td>
    <td><input id="stream_name" class="serviceConfigurationInput" type="text" name="stream_name" value="<%=streamName%>"/></td>
    <td><fmt:message key="version"/></td>
    <td><input id="version" type="text" class="serviceConfigurationInput" name="version" value="<%=version%>"/></td>
</tr>
<tr>
    <td><fmt:message key="from"/></td>
    <td><input id="from" type="text" class="serviceConfigurationInput" name="from" value="<%=from%>"/></td>
    <td><fmt:message key="to"/></td>
    <td><input id="to" type="text" class="serviceConfigurationInput" name="to" value="<%=to%>"/></td>
</tr>
 <tr>
     <td colspan="4" class="buttonRow">
            <input type="submit" class="button" value="<fmt:message key="submit"/>"
                   id="updateStats"/>
      </td>
 </tr>
</table>
</form>
</div>

</fmt:bundle>