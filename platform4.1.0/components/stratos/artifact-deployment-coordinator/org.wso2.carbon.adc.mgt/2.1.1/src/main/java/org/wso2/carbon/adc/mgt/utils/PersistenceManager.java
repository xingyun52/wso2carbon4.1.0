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

package org.wso2.carbon.adc.mgt.utils;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.mgt.dao.CartridgeSubscription;
import org.wso2.carbon.adc.mgt.dao.DataCartridge;
import org.wso2.carbon.adc.mgt.dao.PortMapping;
import org.wso2.carbon.adc.mgt.dao.Repository;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;

/**
 * This class is responsible for handling persistence
 * 
 */
public class PersistenceManager {

	private static final Log log = LogFactory.getLog(PersistenceManager.class);

	public static void persistCartridgeInstanceInfo(String instanceIp, 
	                                                String clusterDomain,
	                                                String clusterSubDomain,
	                                                String cartridgeType,
	                                                String state)  throws Exception {
		
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;
		
		boolean isUpdate = false;
		int instanceId = 0;
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();
			
			// First check whether Ip exists..
			String sql =
			             "SELECT ID FROM CARTRIDGE_INSTANCE where INSTANCE_IP='" + instanceIp
			             		 + "' AND CARTRIDGE_TYPE='" + cartridgeType
			             		 + "' AND CLUSTER_DOMAIN='" + clusterDomain
			             		 + "' AND CLUSTER_SUBDOMAIN='" + clusterSubDomain
			             		 + "' ";
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {				
				isUpdate = true;
				instanceId = resultSet.getInt("ID");
				log.info("Update state.... id: " + instanceId);
			}

			String persistQuery = null;
			if(isUpdate) {
				persistQuery = 
					"UPDATE CARTRIDGE_INSTANCE SET STATE='" + state + 
					"' WHERE ID='" + instanceId + "' ";
				
			}else {
				persistQuery = 
		             "INSERT INTO CARTRIDGE_INSTANCE (INSTANCE_IP, CARTRIDGE_TYPE, STATE, CLUSTER_DOMAIN, CLUSTER_SUBDOMAIN)" +
		                     " VALUES ('" + instanceIp +"'" +
		                     ", '" + cartridgeType + "'" +
		                     ", '" + state + "'" +
		                     ", '" + clusterDomain + "'" +
		                     ", '" + clusterSubDomain + "' " +
		                     " )";
			}
			statement.executeUpdate(persistQuery);
			if (!con.getAutoCommit()) {
				con.commit();
			}
		} catch (Exception e) {
			if (!con.getAutoCommit()) {
				con.rollback();
			}
			log.error(e.getMessage());
			throw e;
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception e) {
			}
		}
	}

	public static List<String> retrieveApplications(int tenantId, String cartridgeType)
	                                                                                   throws Exception {
		List<String> appList = new ArrayList<String>();
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;

		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();
			String sql =
			             "SELECT * FROM REPO WHERE TENANT_ID='" + tenantId + "' AND " +
			                     "CARTRIDGE='" + cartridgeType + "'"; // TODO --
			// active
			// state
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				appList.add(resultSet.getString("APP_TYPE"));
			}

		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			cleanupResources(resultSet, statement, con);
		}

		return appList;
	}

	public static List<CartridgeSubscription> retrieveSubscribedCartridges(int tenantId) throws Exception {

		List<CartridgeSubscription> subscribedCartridgeList =
		                                                      new ArrayList<CartridgeSubscription>();
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;

		try {
            Class.forName(driver);
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
            statement = con.createStatement();
            String sql =
                    "SELECT C.CARTRIDGE, C.ALIAS, C.CLUSTER_DOMAIN, C.CLUSTER_SUBDOMAIN, C.MIN_INSTANCES, C.MAX_INSTANCES," +
                            " C.STATE, C.PROVIDER, C.HOSTNAME, R.REPO_NAME FROM CARTRIDGE_SUBSCRIPTION C " +
                            "LEFT JOIN REPOSITORY R ON C.REPO_ID=R.REPO_ID WHERE TENANT_ID='" + tenantId +
                            "' AND C.STATE != 'UNSUBSCRIBED' ";

			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				CartridgeSubscription cartridge = new CartridgeSubscription();
				cartridge.setName(resultSet.getString("ALIAS"));
				cartridge.setCartridge(resultSet.getString("CARTRIDGE"));
				cartridge.setState(resultSet.getString("STATE"));
				cartridge.setClusterDomain(resultSet.getString("CLUSTER_DOMAIN"));
				cartridge.setClusterSubdomain(resultSet.getString("CLUSTER_SUBDOMAIN"));
				cartridge.setProvider(resultSet.getString("PROVIDER"));
                cartridge.setMaxInstances(resultSet.getInt("MAX_INSTANCES"));
                cartridge.setMinInstances(resultSet.getInt("MIN_INSTANCES"));
				Repository repo = new Repository();
				repo.setRepoName(resultSet.getString("REPO_NAME"));
				cartridge.setRepository(repo);
				cartridge.setHostName(resultSet.getString("HOSTNAME"));
				subscribedCartridgeList.add(cartridge);
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			cleanupResources(resultSet, statement, con);
		}
		return subscribedCartridgeList;
	}

	public static String getRepoURL(int tenantId, String cartridge) throws Exception {
		
		String repoUrl = null;
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();
			String sql = "SELECT REPO_NAME FROM REPOSITORY R, CARTRIDGE_SUBSCRIPTION C " +
					"WHERE C.REPO_ID=R.REPO_ID AND C.TENANT_ID='"+tenantId+"' AND C.CARTRIDGE='"+cartridge+"' " +
							"AND C.STATE != 'UNSUBSCRIBED' ";
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				repoUrl = resultSet.getString("REPO_NAME");
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			cleanupResources(resultSet, statement, con);
		}
		return repoUrl;
	}

    public static String getRepoCredentials(int tenantId, String cartridge) throws Exception {

        String repoUrl = null;
        String repoUserName = null;
        String repoUserPassword = null;
        Connection con = null;
        String url = System.getProperty(CartridgeConstants.DB_URL);
        String db = System.getProperty(CartridgeConstants.DB_NAME);
        String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
        String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
        String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
            Class.forName(driver);
            statement = con.createStatement();
            String sql = "SELECT REPO_NAME,REPO_USER_NAME,REPO_USER_PASSWORD FROM REPOSITORY R, CARTRIDGE_SUBSCRIPTION C " +
                    "WHERE C.REPO_ID=R.REPO_ID AND C.TENANT_ID='"+tenantId+"' AND C.CARTRIDGE='"+cartridge+"' "+
                    "AND C.STATE != 'UNSUBSCRIBED' ";
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                repoUrl = resultSet.getString("REPO_NAME");
                repoUserName = resultSet.getString("REPO_USER_NAME");
                repoUserPassword = decryptPassword(resultSet.getString("REPO_USER_PASSWORD"));
            }
        } catch (Exception s) {
            String msg = "Error while sql connection :" + s.getMessage();
            log.error(msg);
            throw s;
        } finally {
            cleanupResources(resultSet, statement, con);
        }
        String JSONCredentialString="{credentials:{url:"+repoUrl+",username:"+repoUserName+",password:"+repoUserPassword+"}}";
        return JSONCredentialString;
    }

	public static boolean isAliasAlreadyTaken(String alias, String cartridgeType) throws Exception {
		boolean aliasAlreadyTaken = false;
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;

		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();
			String sql =
			             "SELECT SUBSCRIPTION_ID FROM CARTRIDGE_SUBSCRIPTION where ALIAS='" + alias +
			                     "' AND CARTRIDGE='" + cartridgeType + "' AND STATE != 'UNSUBSCRIBED'";
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				log.info("Already taken..");
				aliasAlreadyTaken = true;
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			cleanupResources(resultSet, statement, con);
		}
		return aliasAlreadyTaken;
	}

	private static void cleanupResources(ResultSet resultSet, Statement statement, Connection con) {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
		} catch (Exception e) {
		}
		try {
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
		}
		try {
			if (con != null) {
				con.close();
			}
		} catch (Exception e) {
		}
	}

	public static int persistSubscription(CartridgeSubscription cartridgeSubscription) throws AxisFault {

		int cartridgeSubscriptionId = 0;
		int repoId = 0;
		int dataCartridgeId = 0;
		ResultSet res = null;
		Statement insertSubscriptionStmt = null;
		Statement insertPortsStmt = null;
		Statement insertRepoStmt = null;
		Statement insertDataCartStmt = null;
		
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;

		// persist cartridge_subscription
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			con.setAutoCommit(false); // Transaction will be commit manually
									  // after all inserts
			Class.forName(driver);
			insertSubscriptionStmt = con.createStatement();
			insertPortsStmt = con.createStatement();
			insertRepoStmt = con.createStatement();
			insertDataCartStmt = con.createStatement();
			
			// persist repo
			if (cartridgeSubscription.getRepository() != null) {
                String encryptedRepoUserPassword = encryptPassword(cartridgeSubscription.getRepository().getRepoUserPassword());
				String insertRepo =
			                        "INSERT INTO REPOSITORY (REPO_NAME,STATE,REPO_USER_NAME,REPO_USER_PASSWORD)"
                                         +" VALUES (" + "'" +cartridgeSubscription.getRepository().getRepoName() 
                                         +"' , 'ACTIVE' ,"+"'" +cartridgeSubscription.getRepository().getRepoUserName()
                                         +"',"+"'" +encryptedRepoUserPassword+"'"+")";
                
				insertRepoStmt.executeUpdate(insertRepo,Statement.RETURN_GENERATED_KEYS);
				res = insertRepoStmt.getGeneratedKeys();
				while (res.next())
					repoId = res.getInt(1);
			}
			
			// persist data cartridge
			if(cartridgeSubscription.getDataCartridge() != null) {
				String insertDataCartridge =
                    "INSERT INTO DATA_CARTRIDGE (TYPE,USER_NAME,PASSWORD,STATE)" +
                            " VALUES (" + "'" + cartridgeSubscription.getDataCartridge().getDataCartridgeType() +
                            			  "', '" + cartridgeSubscription.getDataCartridge().getUserName() +
                            			  "', '" + cartridgeSubscription.getDataCartridge().getPassword() + 
                            			  "', 'ACTIVE' " + ")";
					insertDataCartStmt.executeUpdate(insertDataCartridge,Statement.RETURN_GENERATED_KEYS);
					res = insertDataCartStmt.getGeneratedKeys();
					while (res.next())
						dataCartridgeId = res.getInt(1);
					
			}
			
			String insertSubscription =
			                            "INSERT INTO CARTRIDGE_SUBSCRIPTION (TENANT_ID, CARTRIDGE, PROVIDER,"
			                                    + "HOSTNAME, MIN_INSTANCES, MAX_INSTANCES, CLUSTER_DOMAIN, " 
			                                    + "CLUSTER_SUBDOMAIN, STATE, ALIAS, TENANT_DOMAIN, BASE_DIR, REPO_ID, DATA_CARTRIDGE_ID)"
			                                    + " VALUES (" + "'" +
			                                    cartridgeSubscription.getTenantId() +
			                                    "', " +
			                                    "'" +
			                                    cartridgeSubscription.getCartridge() +
			                                    "', " +
			                                    "'" +
			                                    cartridgeSubscription.getProvider() +
			                                    "', " +
			                                    "'" +
			                                    cartridgeSubscription.getHostName() +
			                                    "', " +
			                                    "'" +
			                                    cartridgeSubscription.getMinInstances() +
			                                    "', " +
			                                    "'" +
			                                    cartridgeSubscription.getMaxInstances() +
			                                    "', " +
			                                    "'" +
			                                    cartridgeSubscription.getClusterDomain() +
			                                    "', " +
			                                    "'" +
			                                    cartridgeSubscription.getClusterSubdomain() +
			                                    "', " +
			                                    "'" +
			                                    cartridgeSubscription.getState() +
			                                    "', " +
			                                    "'" +
			                                    cartridgeSubscription.getName() +
			                                    "', " +
                                                "'" +
                                                cartridgeSubscription.getTenantDomain() +
                                                "', " +
                                                "'" +
                                                cartridgeSubscription.getBaseDirectory() +
                                                "', " +
			                                    "'" +
			                                    repoId +
			                                    "', " +
			                                    "'" +
			                                    dataCartridgeId +
			                                    "' " + "" + ")";
			insertSubscriptionStmt.executeUpdate(insertSubscription,
			                                     Statement.RETURN_GENERATED_KEYS);
			res = insertSubscriptionStmt.getGeneratedKeys();
			while (res.next())
				cartridgeSubscriptionId = res.getInt(1);

			
			
			
			List<PortMapping> portMapping = cartridgeSubscription.getPortMappings();
			// persist port map
			if (portMapping != null && !portMapping.isEmpty()) {
				for (PortMapping portMap : portMapping) {
					insertPortsStmt.executeUpdate("INSERT INTO PORT_MAPPING (SUBSCRIPTION_ID, TYPE, PRIMARY_PORT, PROXY_PORT, STATE)" +
					                              " VALUES (" +
					                              "'" +
					                              cartridgeSubscriptionId +
					                              "', " +
					                              "'" +
					                              portMap.getType() +
					                              "', " +
					                              "'" +
					                              portMap.getPrimaryPort() +
					                              "', " +
					                              "'" +
					                              portMap.getProxyPort() +
					                              "' , 'ACTIVE'" +
					                              "" +
					                              ")");
				}
			}
			con.commit(); // Commit manually
		} catch (Exception e) {
            try {
                con.rollback();
            } catch (SQLException ignore) {
                log.error(e.getMessage());
                String msg = "Exception : " + e.getMessage();
                log.error(msg, e);
                throw new AxisFault("Sorry, subscribe failed!", e);
            }
            log.error(e.getMessage());
            String msg = "Exception : " + e.getMessage();
            log.error(msg, e);
            throw new AxisFault("Sorry, subscribe failed!", e);
		} finally {
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception e) {
			}
		}
        return cartridgeSubscriptionId;
	}

    public static String getHostNameForCartridgeName(int tenantId, String name) throws Exception {

		String hostName = null;
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();
			String sql =
			             "SELECT HOSTNAME FROM CARTRIDGE_SUBSCRIPTION where TENANT_ID='" +
			                     String.valueOf(tenantId) + "' AND ALIAS='" + name + "' AND STATE != 'UNSUBSCRIBED'";
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				hostName = resultSet.getString("HOSTNAME");
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			cleanupResources(resultSet, statement, con);
		}
		return hostName;
	}

	
	public static CartridgeSubscription getSubscription(String tenantDomain, String cartridgeName)
	                                                                                              throws Exception {
		
		CartridgeSubscription cartridgeSubscription = new CartridgeSubscription();
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();

			String sql = "SELECT * FROM CARTRIDGE_SUBSCRIPTION C left join REPOSITORY R on " +
						 "C.REPO_ID=R.REPO_ID left join DATA_CARTRIDGE D on " +
						 "D.DATA_CART_ID=C.DATA_CARTRIDGE_ID WHERE ALIAS='"+cartridgeName+"' AND TENANT_DOMAIN='"+tenantDomain+"' AND C.STATE != 'UNSUBSCRIBED'";
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				populateSubscription(cartridgeSubscription, resultSet);
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			cleanupResources(resultSet, statement, con);			
		}

		return cartridgeSubscription;
	}
	
	

	private static void populateSubscription(CartridgeSubscription cartridgeSubscription, ResultSet resultSet)
	                                                                                     throws Exception {
		String repoName = resultSet.getString("REPO_NAME");
		if(repoName != null) {
			Repository repo = new Repository();
			repo.setRepoName(repoName);
			cartridgeSubscription.setRepository(repo);
		}
		
		int dataCartridgeId = resultSet.getInt("DATA_CARTRIDGE_ID");
		if(dataCartridgeId != 0) {
			DataCartridge dataCartridge = new DataCartridge();
			dataCartridge.setDataCartridgeType(resultSet.getString("TYPE"));
			dataCartridge.setPassword(resultSet.getString("PASSWORD"));
			dataCartridge.setUserName(resultSet.getString("USER_NAME"));
			cartridgeSubscription.setDataCartridge(dataCartridge);
		}
		cartridgeSubscription.setPortMappings(getPortMappings(resultSet.getInt("SUBSCRIPTION_ID")));
		cartridgeSubscription.setTenantId(resultSet.getInt("TENANT_ID"));
		cartridgeSubscription.setState(resultSet.getString("STATE"));
		cartridgeSubscription.setMaxInstances(resultSet.getInt("MAX_INSTANCES"));
		cartridgeSubscription.setMinInstances(resultSet.getInt("MIN_INSTANCES"));		
		cartridgeSubscription.setCartridge(resultSet.getString("CARTRIDGE"));
		cartridgeSubscription.setName(resultSet.getString("ALIAS"));
		cartridgeSubscription.setClusterDomain(resultSet.getString("CLUSTER_DOMAIN"));
		cartridgeSubscription.setClusterSubdomain(resultSet.getString("CLUSTER_SUBDOMAIN"));
		cartridgeSubscription.setProvider(resultSet.getString("PROVIDER"));
		cartridgeSubscription.setHostName(resultSet.getString("HOSTNAME"));
		cartridgeSubscription.setTenantDomain(resultSet.getString("TENANT_DOMAIN"));
		cartridgeSubscription.setBaseDirectory(resultSet.getString("BASE_DIR"));
		cartridgeSubscription.setSubscriptionId(resultSet.getInt("SUBSCRIPTION_ID"));
		
	}

	private static List<PortMapping> getPortMappings(int subscriptionId) throws Exception {
		
		List<PortMapping> portMappingList = new ArrayList<PortMapping>();
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();
			String sql = "SELECT * FROM PORT_MAPPING WHERE SUBSCRIPTION_ID = '"+subscriptionId+"' ";
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				PortMapping portMapping = new PortMapping();
				portMapping.setPrimaryPort(resultSet.getString("PRIMARY_PORT"));
				portMapping.setProxyPort(resultSet.getString("PROXY_PORT"));
				portMapping.setType(resultSet.getString("TYPE"));
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
			} catch (Exception e) {
			}
			try {
				if (statement != null) {
					statement.close();
				}
			} catch (SQLException e) {
			}
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception e) {
			}
		}
	    return portMappingList;
    }

	public static void updateMinMax(int tenantId, String name, int minInstances, int maxInstance)
	                                                                                             throws Exception {
		
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;

		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();
			String sql =
			             "UPDATE CARTRIDGE_SUBSCRIPTION SET MIN_INSTANCES='" + minInstances +
			                     "', " + "MAX_INSTANCES='" + maxInstance + "' WHERE TENANT_ID='" +
			                     tenantId + "' AND ALIAS='" + name + "'";
			statement.executeUpdate(sql);
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			cleanupResources(resultSet, statement, con);
		}
	}

	public static List<CartridgeSubscription> getSubscription(String repositoryURL) throws Exception {
		
		List<CartridgeSubscription> subscriptionList = new ArrayList<CartridgeSubscription>();
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();

			String sql =
			             "SELECT * from CARTRIDGE_SUBSCRIPTION C, REPOSITORY R " +
			             "where R.REPO_NAME='"+repositoryURL+"' AND C.REPO_ID = R.REPO_ID AND C.STATE != 'UNSUBSCRIBED'";
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				CartridgeSubscription cartridgeSubscription = new CartridgeSubscription();
				populateSubscription(cartridgeSubscription, resultSet);
				subscriptionList.add(cartridgeSubscription);
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			cleanupResources(resultSet, statement, con);
		}
		return subscriptionList;
	}

	public static void updateSubscriptionState(int subscriptionId, String state) throws Exception {

		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();
			String sql =
			             "UPDATE CARTRIDGE_SUBSCRIPTION SET STATE='" + state +
			                     "' WHERE SUBSCRIPTION_ID='" +
			                     subscriptionId +"'";
			statement.executeUpdate(sql);
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;			
		} finally {
			cleanupResources(resultSet, statement, con);
		}
    }
	
	public static Map<String, String> getCartridgeInstanceInfo(String[] ips, String clusterDomain, String clusterSubdomain)
            throws AxisFault {
		Map<String, String> instanceIpToStateMap = new HashMap<String, String>();
		
		StringBuilder ipListBuilder = new StringBuilder();
		
		for (String string : ips) {
			ipListBuilder.append("'").append(string).append("'").append(",");
        }
		String ipListBuilderString = ipListBuilder.toString(); 
		String ipListString = ipListBuilderString.substring(0, ipListBuilderString.length()-1);
		
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();

			String sql = "SELECT INSTANCE_IP, STATE FROM CARTRIDGE_INSTANCE WHERE INSTANCE_IP IN ("+ipListString+") AND" +
					" CLUSTER_DOMAIN='"+clusterDomain+"' AND CLUSTER_SUBDOMAIN='"+clusterSubdomain+"'";
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				instanceIpToStateMap.put(resultSet.getString("INSTANCE_IP"), resultSet.getString("STATE"));
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg, s);
			throw new AxisFault("Ann error occurred while listing cartridge information.");
		} finally {
			cleanupResources(resultSet, statement, con);
		}
		return instanceIpToStateMap;
	}
	
	
	public static String getRepoURLForAlias(int tenantId, String alias) throws Exception {
		String repoUrl = null;
		Connection con = null;
		String url = System.getProperty(CartridgeConstants.DB_URL);
		String db = System.getProperty(CartridgeConstants.DB_NAME);
		String driver = System.getProperty(CartridgeConstants.DB_DRIVER);
		String dbUsername = System.getProperty(CartridgeConstants.DB_USERNAME);
		String dbPassword = System.getProperty(CartridgeConstants.DB_PASSWORD);
		Statement statement = null;
		ResultSet resultSet = null;
		
		try {
			con = DriverManager.getConnection(url + db, dbUsername, dbPassword);
			Class.forName(driver);
			statement = con.createStatement();
			String sql = "SELECT REPO_NAME FROM REPOSITORY R, CARTRIDGE_SUBSCRIPTION C " +
					"WHERE C.REPO_ID=R.REPO_ID AND C.TENANT_ID='"+tenantId+"' AND C.ALIAS='"+alias+"' " +
							"AND C.STATE != 'UNSUBSCRIBED' ";
			resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				repoUrl = resultSet.getString("REPO_NAME");
			}
		} catch (Exception s) {
			String msg = "Error while sql connection :" + s.getMessage();
			log.error(msg);
			throw s;
		} finally {
			cleanupResources(resultSet, statement, con);
		}
		return repoUrl;
	}

    public static String getSecurityKey() {
        String securityKey = CartridgeConstants.DEFAULT_SECURITY_KEY;
        OMElement documentElement = null;
        File xmlFile = new File(CarbonUtils.getCarbonHome() + File.separator + "repository" + File.separator + "conf"
                + File.separator + CartridgeConstants.SECURITY_KEY_FILE);

        if (xmlFile.exists()) {
            try {
                documentElement = new StAXOMBuilder(xmlFile.getPath()).getDocumentElement();
            } catch (Exception ex) {
                String msg = "Error occurred when parsing the " + xmlFile.getPath() + ".";
                log.error(msg, ex);
                ex.printStackTrace();
            }
            if (documentElement != null) {
                Iterator<?> it = documentElement.getChildrenWithName(new QName(CartridgeConstants.SECURITY_KEY));
                if (it.hasNext()) {
                    OMElement securityKeyElement = (OMElement) it.next();
                    SecretResolver secretResolver = SecretResolverFactory.create(documentElement, false);
                    String alias = securityKeyElement.getAttributeValue(new QName(CartridgeConstants.ALIAS_NAMESPACE,
                            CartridgeConstants.ALIAS_LOCALPART,CartridgeConstants.ALIAS_PREFIX));

                    if (secretResolver != null && secretResolver.isInitialized() &&
                            secretResolver.isTokenProtected(alias)) {
                        securityKey = "";
                        securityKey = secretResolver.resolve(alias);
                        //TODO : a proper testing on the secure vault protected user defined encryption key
                    }
                }
            }
        }else{
            System.out.println("No such file exists");
        }
        return securityKey;
    }

    private static String encryptPassword(String repoUserPassword) {
        String encryptPassword="";
        String secret = getSecurityKey(); // secret key length must be 16
        SecretKey key;
        Cipher cipher;
        Base64 coder;
        key = new SecretKeySpec(secret.getBytes(), "AES");
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
            coder = new Base64();
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cipherText = cipher.doFinal(repoUserPassword.getBytes());
            encryptPassword = new String(coder.encode(cipherText));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptPassword;
    }

    private static String decryptPassword(String repoUserPassword) {
        String decryptPassword="";
        String secret = getSecurityKey(); // secret key length must be 16
        SecretKey key;
        Cipher cipher;
        Base64 coder;
        key = new SecretKeySpec(secret.getBytes(), "AES");
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
            coder = new Base64();
            byte[] encrypted = coder.decode(repoUserPassword.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(encrypted);
            decryptPassword = new String(decrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptPassword;
    }

}
