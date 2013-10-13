package org.wso2.carbon.remotetasks.ui;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.remotetasks.stub.admin.common.xsd.StaticTaskInformation;
import org.wso2.carbon.remotetasks.stub.admin.common.xsd.TriggerInformation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class RemoteTaskManagementHelper {
	
	private static final Log log = LogFactory.getLog(RemoteTaskManagementHelper.class);
	
	public static StaticTaskInformation createStaticTaskInformation 
		(HttpServletRequest request) throws ServletException, AxisFault {
		String taskName = request.getParameter("taskName");
		String targetUrl = request.getParameter("targetUrl");
		String allowConcurrentEx = request.getParameter("allowConcurrentEx");
		boolean allowConcurrentExecution = (allowConcurrentEx != null) ? true :false;
		
		TriggerInformation triggerInfo = new TriggerInformation();
		String count = request.getParameter("triggerCount");
		if (count != null && !"".equals(count)) {
			try {
				triggerInfo.setTaskCount(Integer.parseInt(count.trim()));
			} catch (NumberFormatException e) {
				handleException("Invalid value for Count (Expected type is int) : "
						+ count);
			}
		}
		String interval = request.getParameter("triggerInterval");
		if (interval != null && !"".equals(interval)) {
			try {
				triggerInfo.setTaskInterval(Integer.parseInt(interval.trim()));
			} catch (NumberFormatException e) {
				handleException("Invalid value for interval (Expected type is integer) : "
						+ interval);
			}
		}
		String cronExp = request.getParameter("cronExp");
		if (cronExp != null && !"".equals(cronExp)) {
			triggerInfo.setCronExpression(cronExp);
		}
		
		String startTimeAsString = request.getParameter("startTime");
        if (startTimeAsString != null && !"".equals(startTimeAsString.trim())) {
        	triggerInfo.setStartTime(getProcessedStartTime(startTimeAsString));
        }
        
        String endTimeAsString = request.getParameter("endTime");
        if (endTimeAsString != null && !"".equals(endTimeAsString.trim())) {
        	triggerInfo.setEndTime(getProcessedStartTime(endTimeAsString));
        }
		
		StaticTaskInformation staticInfo = new StaticTaskInformation();
		staticInfo.setName(taskName);
		staticInfo.setTargetURI(targetUrl);
		staticInfo.setAllowConcurrentExecutions(allowConcurrentExecution);
		staticInfo.setTriggerInformation(triggerInfo);
		return staticInfo;
	}
	
	private static Calendar getProcessedStartTime(String startTimeAsString) throws AxisFault {
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date startTime;
        try {
            startTime = df.parse(startTimeAsString);
            Calendar cal = Calendar.getInstance();
            cal.setTime(startTime);
            return cal;
        } catch (ParseException e) {
            throw new AxisFault("Invalid DateTime format", e);
        }
    }

    public static String formatStartTime(Calendar startTime) throws AxisFault {
        if (startTime == null) {
            return "";
        }
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return df.format(startTime.getTime());
    }
	
	private static void handleException(String msg) throws ServletException {
        log.error(msg);
        throw new ServletException(msg);
    }
	
	public static String formatGeneralString(String value) {
		if (value == null) {
			return "";
		} else {
			return value;
		}
	}

}
