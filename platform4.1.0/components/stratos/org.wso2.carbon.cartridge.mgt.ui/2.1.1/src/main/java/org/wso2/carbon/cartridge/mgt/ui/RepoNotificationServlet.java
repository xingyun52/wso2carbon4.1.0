package org.wso2.carbon.cartridge.mgt.ui;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.servlet.internal.ServletConfigImpl;

public class RepoNotificationServlet extends HttpServlet {

	private static final long serialVersionUID = 4315990619456849911L;
	private static final Log log = LogFactory.getLog(RepoNotificationServlet.class);

	public RepoNotificationServlet() {
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		StringBuffer xmlStr = new StringBuffer();
		int d;
		InputStream in;
		String payload = "";
		try {
			in = request.getInputStream();
			while ((d = in.read()) != -1) {
				xmlStr.append((char) d);
			}
			payload = xmlStr.toString();
			log.info(" repository payload received : " + payload);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String repositoryURL;
		if (payload.split("\"url\":").length > 2) {
			repositoryURL = payload.split("\"url\":")[1].split(",")[0].replace("\"", "");
			log.info(" repository url : " + repositoryURL);
			try {
				String backendServerURL = CarbonUIUtil.getServerURL(getServletContext(),
						request.getSession());
				ConfigurationContext configContext = (ConfigurationContext) getServletContext()
						.getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
				String cookie = (String) request.getSession().getAttribute(
						ServerConstants.ADMIN_SERVICE_COOKIE);
				new RepoNotificationClient(cookie, backendServerURL, configContext,
						request.getLocale()).synchronize(repositoryURL);
			} catch (Exception e) {
				log.error("Exception is occurred in synchronize, Reason : " + e.getMessage());
			}

		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) {
		log.info("Inside RepoNotificationServlet doGet");
		this.doPost(req, res);
	}

}