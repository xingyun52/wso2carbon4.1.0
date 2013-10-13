/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.internal;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.topology.mgt.service.TopologyManagementService;
import org.wso2.carbon.utils.CarbonUtils;

public class MySQLPasswordConfigurer implements Runnable {

	private static final Log log = LogFactory.getLog(MySQLPasswordConfigurer.class);

	String cartridgeType;
	String clusterDomain;
	String clusterSubdomain;
	String mysqlPassword;

	public MySQLPasswordConfigurer(String cartridgeType, String clusterDomain,
	                               String clusterSubdomain, String mysqlPassword) {
		this.cartridgeType = cartridgeType;
		this.clusterDomain = clusterDomain;
		this.clusterSubdomain = clusterSubdomain;
		this.mysqlPassword = mysqlPassword;
	}

	@Override
	public void run() {

		TopologyManagementService topologyMgtService = DataHolder.getTopologyMgtService();

		while (true) {
			String[] instanceIp =
			                      topologyMgtService.getActiveIPs(cartridgeType, clusterDomain,
			                                                      clusterSubdomain);

			if (instanceIp == null || instanceIp.length <= 0) {
				try {
					Thread.sleep(3000);
					continue;
				} catch (InterruptedException e) {
					log.error(" Exception is occurred.. : " + e.getMessage());
				}
			} else {
				try {
					while (true) {
						if (!isIPAndPortAvailable(instanceIp[0], 22)) {
							Thread.sleep(3000);
							continue;
						} else {
							while (true) {
								if (!isIPAndPortAvailable(instanceIp[0], 3306)) {
									Thread.sleep(3000);
									continue;
								} else {
									log.info("MySQL service is ready. ");
									setMySqlPassword(instanceIp[0], mysqlPassword);
									break;
								}
							}
							break;
						}
					}
				} catch (Exception e) {
					log.error(" Exception is occurred.. : " + e.getMessage());
				}
				break;
			}
		}
	}

	private boolean isIPAndPortAvailable(String ip, int port) {

		boolean isIpPortAvailable = false;
		InputStream is = null;
		DataInputStream dis = null;
		Socket s1 = null;
		try {
			s1 = new Socket(ip, port);
			is = s1.getInputStream();
			dis = new DataInputStream(is);
			if (dis != null) {
				log.info("Connected with ip " + ip + " and port " + port);
				isIpPortAvailable = true;
			}

		} catch (Exception e) {
			log.error("Exception occurred .. retrying " + e.getMessage());
			isIpPortAvailable = false;
		} finally {
			try {
				if (dis != null) {
					dis.close();
				}
			} catch (Exception e) {
				log.error("Error in closing datainstream. " + e.getMessage());
			}
			try {
				if (s1 != null) {
					s1.close();
				}
			} catch (IOException e) {
				log.error("Error in closing socket. " + e.getMessage());
			}
		}

		return isIpPortAvailable;
	}

	private void setMySqlPassword(String ip, String password) throws Exception {

		// set-mysql-password <instance ip> <cartridge private key> <password>

		// TODO validate ip / password / mysql with tenant..

		try {
			Process proc = null;
			String cartridgePrivateKey = "/home/wso2/.ssh/id_rsa";
			String command =
			                 CarbonUtils.getCarbonHome() + File.separator + "bin" + File.separator +
			                         "set-mysql-password.sh " + ip + " " + cartridgePrivateKey +
			                         " " + password + " /";
			log.info("executing set-mysql-password .. command :" + command);
			proc = Runtime.getRuntime().exec(command);
			proc.waitFor();
			log.info("executed..");
		} catch (Exception e) {
			log.error("Exception is occurred ... " + e.getMessage());
			throw e;
		}

	}

}
