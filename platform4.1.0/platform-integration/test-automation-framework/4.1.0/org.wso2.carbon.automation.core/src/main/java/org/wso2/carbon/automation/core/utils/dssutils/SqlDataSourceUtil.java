/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.carbon.automation.core.utils.dssutils;

import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.wso2.carbon.automation.api.clients.rssmanager.RSSManagerAdminServiceClient;
import org.wso2.carbon.automation.core.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.core.utils.UserInfo;
import org.wso2.carbon.automation.core.utils.UserListCsvReader;
import org.wso2.carbon.automation.core.utils.dbutils.DatabaseFactory;
import org.wso2.carbon.automation.core.utils.dbutils.DatabaseManager;
import org.wso2.carbon.automation.core.utils.dbutils.H2DataBaseManager;
import org.wso2.carbon.automation.core.utils.environmentutils.EnvironmentBuilder;
import org.wso2.carbon.automation.core.utils.fileutils.FileManager;
import org.wso2.carbon.automation.core.utils.frameworkutils.FrameworkProperties;
import org.wso2.carbon.rssmanager.ui.stub.types.Database;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabaseMetaData;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabasePrivilegeTemplate;
import org.wso2.carbon.rssmanager.ui.stub.types.DatabaseUserMetaData;
import org.wso2.carbon.rssmanager.ui.stub.types.RSSInstanceMetaData;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SqlDataSourceUtil {
    private static final Log log = LogFactory.getLog(SqlDataSourceUtil.class);
    private String dssBackEndUrl;
    private String sessionCookie;
    private FrameworkProperties frameworkProperties;
    private UserInfo userInfo;
    private RSSManagerAdminServiceClient rssAdminClient;

    private String rssInstanceName;
    private DatabasePrivilegeTemplate userPrivilegeGroup;
    private String jdbcUrl = null;
    private String jdbcDriver = null;
    private int databaseUserId = -1;
    private String databaseName;
    private String databaseUser;
    private String databasePassword;
    private final String userPrivilegeGroupName = "automation";


    public SqlDataSourceUtil(String sessionCookie, String backEndUrl,
                             FrameworkProperties frameworkProperties, int userId) {
        this.sessionCookie = sessionCookie;
        this.dssBackEndUrl = backEndUrl;
        this.frameworkProperties = frameworkProperties;
        this.userInfo = UserListCsvReader.getUserInfo(userId);

    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    public String getDatabaseUser() {
        return this.databaseUser;
    }

    public String getDatabasePassword() {
        return this.databasePassword;
    }

    public int getDatabaseUserId() {
        return this.databaseUserId;
    }

    public String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public String getDriver() {
        return this.jdbcDriver;
    }

    /**
     * @param dbsFilePath
     * @return
     * @throws XMLStreamException
     * @throws IOException
     */
    public DataHandler createArtifact(String dbsFilePath) throws XMLStreamException, IOException {
        Assert.assertNotNull(jdbcUrl, "Initialize jdbcUrl");
        try {
            OMElement dbsFile = AXIOMUtil.stringToOM(FileManager.readFile(dbsFilePath));
            OMElement dbsConfig = dbsFile.getFirstChildWithName(new QName("config"));
            Iterator configElement1 = dbsConfig.getChildElements();
            while (configElement1.hasNext()) {
                OMElement property = (OMElement) configElement1.next();
                String value = property.getAttributeValue(new QName("name"));
                if ("org.wso2.ws.dataservice.protocol".equals(value)) {
                    property.setText(jdbcUrl);

                } else if ("org.wso2.ws.dataservice.driver".equals(value)) {
                    property.setText(jdbcDriver);

                } else if ("org.wso2.ws.dataservice.user".equals(value)) {
                    property.setText(databaseUser);

                } else if ("org.wso2.ws.dataservice.password".equals(value)) {
                    property.setText(databasePassword);
                }
            }
            log.debug(dbsFile);
            ByteArrayDataSource dbs = new ByteArrayDataSource(dbsFile.toString().getBytes());
            return new DataHandler(dbs);

        } catch (XMLStreamException e) {
            log.error("XMLStreamException when Reading Service File", e);
            throw new XMLStreamException("XMLStreamException when Reading Service File", e);
        } catch (IOException e) {
            log.error("IOException when Reading Service File", e);
            throw new IOException("IOException  when Reading Service File", e);
        }

    }

    /**
     * @param sqlFileList
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public void createDataSource(List<File> sqlFileList) throws IOException, ClassNotFoundException,
                                                                SQLException {
        databaseName = frameworkProperties.getDataSource().getDbName();
        databaseUser = frameworkProperties.getDataSource().getDbUser();
        databasePassword = frameworkProperties.getDataSource().getDbPassword();
        jdbcUrl = frameworkProperties.getDataSource().getDbUrl();
        jdbcDriver = frameworkProperties.getDataSource().get_dbDriverName();
        databaseUser = frameworkProperties.getDataSource().getDbUser();
        databasePassword = frameworkProperties.getDataSource().getDbPassword();
        EnvironmentBuilder environmentBuilder = new EnvironmentBuilder();
        String executionMode = environmentBuilder.getFrameworkSettings().getEnvironmentSettings()
                .executionMode().toString();
        String environment = environmentBuilder.getFrameworkSettings().getEnvironmentSettings()
                .executionEnvironment();
        if (environment.equals(ExecutionEnvironment.stratos.name())) {
            rssAdminClient = new RSSManagerAdminServiceClient(dssBackEndUrl, sessionCookie);
            //rssAdminClient.
//            databaseUser = frameworkProperties.getDataSource().getRssDbUser();
//            databasePassword = frameworkProperties.getDataSource().getRssDbPassword();

            DatabaseMetaData rssInstance = rssAdminClient.getDatabaseInstance(databaseName + "_" + userInfo.getDomain().replace(".", "_"));
            if (rssInstance != null) {
                setPriConditions();
                createDataBase();
                createPrivilegeGroup();
                createUser();
            } else {
                createDataBase(jdbcDriver, jdbcUrl, databaseUser, databasePassword);
            }
        } else {
            jdbcUrl = frameworkProperties.getDataSource().getDbUrl();
            jdbcDriver = frameworkProperties.getDataSource().get_dbDriverName();
            if (jdbcUrl.contains("h2") && jdbcDriver.contains("h2")) {
                /*Random number appends to a database name to create new database for H2*/
                databaseName = databaseName + new Random().nextInt();
                jdbcUrl = jdbcUrl + databaseName;
                //create database on in-memory
                H2DataBaseManager h2 = null;
                try {
                    h2 = new H2DataBaseManager(jdbcUrl, databaseUser, databasePassword);
                    h2.executeUpdate("DROP ALL OBJECTS");
                } finally {
                    if (h2 != null) {
                        h2.disconnect();
                    }
                }

            } else {
                createDataBase(jdbcDriver, jdbcUrl, databaseUser, databasePassword);
            }
        }
        executeUpdate(sqlFileList);
    }

    /**
     * @param dbName
     * @param dbUser
     * @param dbPassword
     * @param sqlFileList
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */

    public void createDataSource(String dbName, String dbUser, String dbPassword,
                                 List<File> sqlFileList)
            throws IOException, ClassNotFoundException, SQLException {
        databaseName = dbName;

        if (frameworkProperties.getEnvironmentSettings().is_runningOnStratos()) {
            rssAdminClient = new RSSManagerAdminServiceClient(dssBackEndUrl, sessionCookie);
            databaseUser = dbUser;
            databasePassword = dbPassword;
            setPriConditions();
            createDataBase();
            createPrivilegeGroup();
            createUser();
        } else {
            jdbcUrl = frameworkProperties.getDataSource().getDbUrl();
            jdbcDriver = frameworkProperties.getDataSource().get_dbDriverName();
            databaseUser = frameworkProperties.getDataSource().getDbUser();
            databasePassword = frameworkProperties.getDataSource().getDbPassword();

            if (jdbcUrl.contains("h2") && jdbcDriver.contains("h2")) {
                /*Random number appends to a database name to create new database for H2*/
                databaseName = databaseName + new Random().nextInt();
                jdbcUrl = jdbcUrl + databaseName;
                //create database on in-memory
                H2DataBaseManager h2 = null;
                try {
                    h2 = new H2DataBaseManager(jdbcUrl, databaseUser, databasePassword);
                    h2.executeUpdate("DROP ALL OBJECTS");
                } finally {
                    if (h2 != null) {
                        h2.disconnect();
                    }
                }

            } else {
                createDataBase(jdbcDriver, jdbcUrl, databaseUser, databasePassword);
            }

        }
        executeUpdate(sqlFileList);
    }

    private void createDataBase() throws RemoteException {
        RSSInstanceMetaData rssInstance = null;

        rssInstanceName = "WSO2_RSS";
        log.info("RSS Instance Name :" + rssInstanceName);

        Database database = new Database();

        //creating database
        rssAdminClient.createDatabase(database);
        log.info("Database created");
        //set database full name
        databaseName = databaseName + "_" + userInfo.getDomain().replace(".", "_");
        log.info("Database name : " + databaseName);

        DatabaseMetaData db = rssAdminClient.getDatabase(rssInstanceName, databaseName);
        log.info("JDBC URL : " + db.getUrl());
    }

    private void createDataBase(String driver, String jdbc, String user, String password)
            throws ClassNotFoundException, SQLException {
        try {
            DatabaseManager dbm = DatabaseFactory.getDatabaseConnector(driver, jdbc, user, password);
            dbm.executeUpdate("DROP DATABASE IF EXISTS " + databaseName);
            dbm.executeUpdate("CREATE DATABASE " + databaseName);
            jdbcUrl = jdbc + "/" + databaseName;

            dbm.disconnect();
        } catch (ClassNotFoundException e) {
            log.error("Class Not Found. Check MySql-jdbc Driver in classpath: ", e);
            throw new ClassNotFoundException("Class Not Found. Check MySql-jdbc Driver in classpath: ", e);
        } catch (SQLException e) {
            log.error("SQLException When executing SQL: ", e);
            throw new SQLException("SQLException When executing SQL: ", e);
        }

    }

    private void createPrivilegeGroup() throws RemoteException {
        rssAdminClient.createPrivilegeGroup(userPrivilegeGroupName);
        userPrivilegeGroup = rssAdminClient.getPrivilegeGroup(userPrivilegeGroupName);
        log.info("privilege Group Created");
        log.debug("Privilege Group Name :" + userPrivilegeGroupName);
        Assert.assertNotSame(-1, userPrivilegeGroupName, "Privilege Group Not Found");
    }

    private void createUser() throws RemoteException {
        DatabaseUserMetaData dbUser;
        rssAdminClient.createDatabaseUser(databaseUser, databasePassword, rssInstanceName);
        log.info("Database User Created");

        dbUser = rssAdminClient.getDatabaseUser(rssInstanceName, databaseUser);
        log.debug("Database Username :" + databaseUser);

        databaseUser = rssAdminClient.getFullyQualifiedUsername(databaseUser, userInfo.getDomain());
        log.info("Database User Name :" + databaseUser);
        Assert.assertEquals(dbUser.getUsername(), databaseUser, "Database UserName mismatched");

    }

    private void setPriConditions() throws RemoteException {
        DatabaseMetaData dbInstance;
        DatabaseUserMetaData userEntry;
        DatabasePrivilegeTemplate privGroup;

        log.info("Setting pre conditions");

        dbInstance = rssAdminClient.getDatabaseInstance(databaseName + "_" + userInfo.getDomain().replace(".", "_"));
        if (dbInstance != null) {
            log.info("Database name already in server");
            userEntry =
                    rssAdminClient.getDatabaseUser(rssInstanceName,
                                                   rssAdminClient.getFullyQualifiedUsername(databaseUser, userInfo.getDomain()));
            if (userEntry != null) {

                log.info("User already in Database. deleting user");
                rssAdminClient.dropDatabaseUser(rssInstanceName, userInfo.getUserName());
                log.info("User Deleted");
            }
            log.info("Dropping database");
            rssAdminClient.dropDatabase(rssInstanceName, databaseName);
            log.info("database Dropped");
        }

        privGroup = rssAdminClient.getPrivilegeGroup(userPrivilegeGroupName);
        if (privGroup != null) {
            log.info("Privilege Group name already in server");
            rssAdminClient.dropPrivilegeGroup(privGroup.getName());
            log.info("Privilege Group Deleted");
        }
        log.info("pre conditions created");

    }

    private void executeUpdate(List<File> sqlFileList)
            throws IOException, ClassNotFoundException, SQLException {
        DatabaseManager dbm = null;
        try {
            dbm = DatabaseFactory.getDatabaseConnector(jdbcDriver, jdbcUrl, databaseUser, databasePassword);
            for (File sql : sqlFileList) {
                dbm.executeUpdate(sql);
            }

        } catch (IOException e) {
            log.error("IOException When reading SQL files: ", e);
            throw new IOException("IOException When reading SQL files: ", e);
        } catch (ClassNotFoundException e) {
            log.error("Class Not Found. Check MySql-jdbc Driver in classpath: " + e);
            throw new ClassNotFoundException("Class Not Found. Check MySql-jdbc Driver in classpath: ", e);
        } catch (SQLException e) {
            log.error("SQLException When executing SQL: " + e);
            throw new SQLException("SQLException When executing SQL: ", e);
        } finally {
            if (dbm != null) {
                dbm.disconnect();
            }
        }
    }
}
