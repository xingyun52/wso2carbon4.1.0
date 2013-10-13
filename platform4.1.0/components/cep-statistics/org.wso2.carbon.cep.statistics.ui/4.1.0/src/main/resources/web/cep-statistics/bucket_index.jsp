<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.cep.stub.admin.CEPAdminServiceStub" %>
<%@ page import="org.wso2.carbon.cep.stub.admin.internal.xsd.BucketBasicInfoDTO" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<fmt:bundle basename="org.wso2.carbon.cep.statistics.ui.i18n.Resources">
    <script type="text/javascript">
        //    function editBucket(link) {
        //        var rowToEdit = link.parentNode.parentNode;
        //        var bucketToEdit = rowToEdit.cells[0].innerHTML.trim();
        //        loadBucketFromBackend(bucketToEdit.getValue());
        //
        //    }
        //    function loadBucketFromBackend(bucketName) {
        //
        //        $.ajax({
        //                   type:"POST",
        //                   url:"cep_load_bucket_from_bEnd.jsp",
        //                   data:{'bucketName':bucketName},
        //                   async:false,
        //                   success:function (msg) {
        ////                    alert("Data Saved: " + msg);
        //                   }
        //               });
        //        location.href = 'cep_buckets.jsp?edit=true';
        //    }
        //    function loadBucketFromBackEndForView(bucketName) {
        //        $.ajax({
        //                   type:"POST",
        //                   url:"cep_load_bucket_from_bEnd.jsp",
        //                   data:{'bucketName':bucketName},
        //                   async:false,
        //                   success:function (msg) {
        ////                    alert("Data Saved: " + msg);
        //                   }
        //               });
        //        location.href = 'cep_view_bucket.jsp?edit=true';
        //    }


    </script>

    <%
        ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
                .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        //Server URL which is defined in the server.xml
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(),
                                                     session) + "CEPAdminService.CEPAdminServiceHttpsSoap12Endpoint";
        CEPAdminServiceStub stub = new CEPAdminServiceStub(configContext, serverURL);

        String cookie = (String) session.getAttribute(org.wso2.carbon.utils.ServerConstants.ADMIN_SERVICE_COOKIE);

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        int pageNumberInt = 0;
        String pageNumberAsStr = request.getParameter("pageNumber");
        if (pageNumberAsStr != null) {
            pageNumberInt = Integer.parseInt(pageNumberAsStr);
        }

        session.removeAttribute("editingBucket");
        session.removeAttribute("bucket");
        BucketBasicInfoDTO[] availableBuckets = stub.getAllBucketNames(pageNumberInt * 10, 10,"");
        session.setAttribute("availableBuckets", availableBuckets);

        int bucketCount = stub.getAllBucketCount();
        int pageCount = (int) Math.ceil(((float) bucketCount) / 10);
        if (pageCount <= 0) {
            //this is to make sure it works with defualt values
            pageCount = 1;
        }
        String parameters = "serviceTypeFilter=" + "&serviceGroupSearchString=";

    %>
    <div id="middle">
        <h2><img src="images/cep-buckets.gif" alt=""/> <fmt:message key="buckets"/></h2>

        <div id="workArea">
            <carbon:paginator pageNumber="<%=pageNumberInt%>" numberOfPages="<%=pageCount%>"
                              page="bucket_index.jsp" pageNumberParameterName="pageNumber"
                              resourceBundle="org.wso2.carbon.cep.ui.i18n.Resources"
                              prevKey="prev" nextKey="next"
                              parameters="<%=parameters%>"/>

            <form name="bucketsForm" action="cep_delete_buckets_ajaxprocessor.jsp" method="post">
                <input type="hidden" name="pageNumber" value="<%= pageNumberInt%>"/>
                <table class="styledLeft" style="width:100%">
                    <thead>
                    <tr>
                        <th><fmt:message key="bucket.name"/></th>
                        <th><fmt:message key="bucket.description"/></th>
                        <th><fmt:message key="monitor"/></th>
                    </tr>
                    </thead>
                    <%
                        if (availableBuckets != null && availableBuckets.length > 0) {
                    %>
                    <tbody>
                    <%
                        int position = 0;
                        for (BucketBasicInfoDTO bucket : availableBuckets) {
                            String bgColor = ((position % 2) == 1) ? "#EEEFFB" : "white";
                            position++;
                    %>
                    <tr bgcolor="<%= bgColor%>">
                        <td>
                            <%=bucket.getName()%>
                        </td>
                        <td>
                            <%=bucket.getDescription()%>
                        </td>
                        <td>
                            <a style="background-image: url(images/chart_bar.gif);"
                               class="icon-link"
                               href="sub_stats_monitor.jsp?bucketName=<%=bucket.getName()%>"><fmt:message
                                    key="monitor"/>
                            </a>
                        </td>
                    </tr>
                    <%
                        }
                    %>
                    </tbody>
                    <%
                        }
                    %>
                </table>
            </form>
            <carbon:paginator pageNumber="<%=pageNumberInt%>" numberOfPages="<%=pageCount%>"
                              page="bucket_index.jsp" pageNumberParameterName="pageNumber"
                              resourceBundle="org.wso2.carbon.cep.ui.i18n.Resources"
                              prevKey="prev" nextKey="next"
                              parameters="<%=parameters%>"/>
        </div>
    </div>
</fmt:bundle>
