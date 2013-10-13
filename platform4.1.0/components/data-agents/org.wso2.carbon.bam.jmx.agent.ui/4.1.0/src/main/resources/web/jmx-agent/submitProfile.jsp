<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.bam.jmx.agent.stub.profiles.xsd.ArrayOfArrayOfString" %>
<%@ page import="org.wso2.carbon.bam.jmx.agent.stub.profiles.xsd.ArrayOfStringE" %>
<%@ page import="org.wso2.carbon.bam.jmx.agent.stub.profiles.xsd.Profile" %>
<%@ page import="org.wso2.carbon.bam.jmx.agent.ui.JmxConnector" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>
<%@ page
        import="org.wso2.carbon.bam.jmx.agent.stub.JmxAgentProfileDoesNotExistExceptionException" %>
<%@ page
        import="org.wso2.carbon.bam.jmx.agent.stub.JmxAgentProfileAlreadyExistsExceptionException" %>

<%

    //initialize
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);

    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().
                    getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    JmxConnector connector = new JmxConnector(configContext, serverURL, cookie);

    //create the profile
    Profile profile = new Profile();


    //if this is a profile update request
    if (request.getParameter("newProfile").equalsIgnoreCase("false")) {


        try {
            profile = connector.getProfile(request.getParameter("profileName"));
        } catch (JmxAgentProfileDoesNotExistExceptionException e) {
            e.printStackTrace();
            return;
        }
        //set Data publisher data
        profile.setDpReceiverAddress(request.getParameter("pubAddress"));
        profile.setDpUserName(request.getParameter("pubUserName"));
        profile.setDpPassword(request.getParameter("pubUserPass"));
        profile.setDpReceiverConnectionType(request.getParameter("pubConnType"));
        profile.setDpSecureUrlConnectionType(request.getParameter("pubSecureConnType"));
        profile.setDpSecureAddress(request.getParameter("pubSecureAddress"));

        //get the cron expression
        if (request.getParameter("presetCronExpr").equalsIgnoreCase("custom")) {
            profile.setCronExpression(request.getParameter("cronExprTxtInput"));
        } else {
            profile.setCronExpression(request.getParameter("presetCronExpr"));
        }

        //increment the profile version
        if(request.getParameter("incrementVersion").equalsIgnoreCase("true")){
            profile.setVersion(profile.getVersion() + 1);
        }



        //set JMX data
        profile.setUserName(request.getParameter("jmxUserName"));
        profile.setPass(request.getParameter("jmxUserPass"));
        profile.setUrl(request.getParameter("jmxServerUrl"));

        //get attribute data
        String mBeanAttrData = request.getParameter("mBeanAttrData");
        //remove new line characters
        //mBeanAttrData = mBeanAttrData.replaceAll("\\s", "");

        //we receive a string like this
        //MBeanName__-__AttrName__-__keyname__-__Alias;MBeanName__-__AttrName__-__Alias;
        //(    attribute with composite data   )(      normal attribute      )
        String[] mBeanAttrPairs = mBeanAttrData.split("__-__");

        Map<String, LinkedList<String[]>> map = new HashMap<String, LinkedList<String[]>>();

        for (String pair : mBeanAttrPairs) {
            String[] data = pair.split(";");
            //iterate over the data and add it to the map


            //if the mBean exists
            if (map.containsKey(data[0])) {
                //get the attributes
                LinkedList<String[]> list = map.get(data[0]);
                //add the attributes to the list - don't add the MBean
                list.add(Arrays.copyOfRange(data, 1, data.length));
                //update the MBean entry
                map.put(data[0], list);
            }
            //if the mBean does not exit
            else {
                LinkedList<String[]> list = new LinkedList<String[]>();
                //add the attributes to the list
                list.add(Arrays.copyOfRange(data, 1, data.length));
                //add the MBean entry
                map.put(data[0], list);

            }

        }

        profile.setAttributes(mapToStringArr(map));

        //update the profile
        try {
            connector.updateProfile(profile);
        } catch (JmxAgentProfileDoesNotExistExceptionException e) {
            e.printStackTrace();
            return;
        }

    }
    //if this is a profile creation request
    if (request.getParameter("newProfile").equalsIgnoreCase("true")) {

        //get profile name
        profile.setName(request.getParameter("profileName"));

        //set JMX data
        profile.setUserName(request.getParameter("jmxUserName"));
        profile.setPass(request.getParameter("jmxUserPass"));
        profile.setUrl(request.getParameter("jmxServerUrl"));

        //set Data publisher data
        profile.setDpReceiverAddress(request.getParameter("pubAddress"));
        profile.setDpUserName(request.getParameter("pubUserName"));
        profile.setDpPassword(request.getParameter("pubUserPass"));
        profile.setDpReceiverConnectionType(request.getParameter("pubConnType"));
        profile.setDpSecureUrlConnectionType(request.getParameter("pubSecureConnType"));
        profile.setDpSecureAddress(request.getParameter("pubSecureAddress"));

        //get the cron expression
        if (request.getParameter("presetCronExpr").equalsIgnoreCase("custom")) {
            profile.setCronExpression(request.getParameter("cronExprTxtInput"));
        } else {
            profile.setCronExpression(request.getParameter("presetCronExpr"));
        }

        //set the profile version
        profile.setVersion(1);


        //get attribute data
        String mBeanAttrData = request.getParameter("mBeanAttrData");

        //we receive a string like this
        //MBeanName - AttrName - keyname - Alias;MBeanName - AttrName - Alias;
        //(    attribute with composite data   )(      normal attribute      )
        String[] mBeanAttrPairs = mBeanAttrData.split(" - ");

        Map<String, LinkedList<String[]>> map = new HashMap<String, LinkedList<String[]>>();

        for (String pair : mBeanAttrPairs) {
            String[] data = pair.split(";");
            //iterate over the data and add it to the map

            //if the mBean exists
            if (map.containsKey(data[0])) {
                //get the attributes
                LinkedList<String[]> list = map.get(data[0]);
                //add the attributes to the list - don't add the MBean
                list.add(Arrays.copyOfRange(data, 1, data.length));
                //update the MBean entry
                map.put(data[0], list);
            }
            //if the mBean does not exit
            else {
                LinkedList<String[]> list = new LinkedList<String[]>();
                //add the attributes to the list - don't add the MBean
                list.add(Arrays.copyOfRange(data, 1, data.length));
                //add the MBean entry
                map.put(data[0], list);

            }

        }

        profile.setAttributes(mapToStringArr(map));

        //create new profile
        profile.setActive(true); //since profile is automatically activated initially
        try {
            connector.addProfile(profile);
        } catch (JmxAgentProfileAlreadyExistsExceptionException e) {
            e.printStackTrace();
            return;
        }

    }


%>
<script type="text/javascript">
    //redirect to the index page
    location.href = "../jmx-agent/index.jsp";
</script>

%>

<%--functions--%>
<%!

    private ArrayOfArrayOfString[] mapToStringArr(Map<String, LinkedList<String[]>> map) {

        String[][][] contents;

        int rows = map.size();

        contents = new String[rows][][];

        //iterate through all the keys of the map

        Set<String> keys = map.keySet();

        int count = 0;
        for (String key : keys) {
            //create a new String array
            Object[] objArray = map.get(key).toArray();

            //to add the key to the front of the array
            List<Object> list = Arrays.asList(objArray);
            LinkedList<Object> llist = new LinkedList<Object>(list);
            String[] keyArr = {key, ""};
            llist.add(0, keyArr);

            objArray = llist.toArray();


            String[][] mBeanArr = Arrays.asList(objArray).toArray(new String[objArray.length][2]);

            contents[count] = mBeanArr;
            count++;
        }
        return ArrToArrayOfArrayOfString(contents);

    }

    private ArrayOfArrayOfString[] ArrToArrayOfArrayOfString(String[][][] arr) {
        ArrayOfArrayOfString[] output = new ArrayOfArrayOfString[arr.length];


        //iterate over the array
        int count = 0;
        for (String[][] strArr : arr) {

            int count1 = 0;
            ArrayOfStringE[] instance = new ArrayOfStringE[strArr.length];
            for (String[] arr2 : strArr) {


                instance[count1] = new ArrayOfStringE();
                instance[count1].setArray(arr2);
                count1++;
            }

            output[count] = new ArrayOfArrayOfString();
            output[count].setArray(instance);
            count++;
        }


        return output;
    }
%>

