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
package org.wso2.carbon.automation.core.utils.dbutils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.core.utils.fileutils.FileManager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Managing derby database executions
 * Sample JDBC URL = "jdbc:derby://localhost:1527/myDB;create=true"
 */
public class DerbyDatabaseManager implements DatabaseManager {
    private static final Log log = LogFactory.getLog(DerbyDatabaseManager.class);
    private Statement stmt = null;
    private Connection dbConnection = null;

    /**
     * method will start derby server and create a database connection
     *
     * @param jdbcURL JDBC url sample provided in class comment
     * @throws Exception sql exceptions
     */
    public DerbyDatabaseManager(String jdbcURL) throws ClassNotFoundException,
                                                       SQLException, IllegalAccessException,
                                                       InstantiationException {
        String driverClass = "org.apache.derby.jdbc.ClientDriver";
        Class.forName(driverClass).newInstance();
        dbConnection = DriverManager.getConnection(jdbcURL);
    }

    /**
     * @param jdbcURL
     * @param userName
     * @param passWord
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public DerbyDatabaseManager(String jdbcURL, String userName, String passWord)
            throws ClassNotFoundException,
                   SQLException, IllegalAccessException,
                   InstantiationException {
        String driverClass = "org.apache.derby.jdbc.ClientDriver";
        Class.forName(driverClass).newInstance();
        dbConnection = DriverManager.getConnection(jdbcURL, userName, passWord);
    }

    public void setAutoCommit() throws SQLException {
        dbConnection.setAutoCommit(false);
        dbConnection.commit();
    }

    /**
     * update existing database information
     *
     * @param sql update sql string
     * @throws Exception sql exception
     */
    public void executeUpdate(String sql) throws SQLException {
        stmt = dbConnection.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
    }

    /**
     * update database information bu giving sql file
     *
     * @param sqlFile sql file
     * @throws Exception sql exception
     */
    public void executeUpdate(File sqlFile) throws SQLException, IOException {

        Statement st = null;
        String sql = FileManager.readFile(sqlFile).trim();
        log.debug("Query List:" + sql);
        String[] sqlQuery = sql.split(";");
        try {
            st = dbConnection.createStatement();
            for (String query : sqlQuery) {
                log.debug(query);
                st.executeUpdate(query.trim());
            }
        } finally {
            if (st != null) {
                st.close();
            }
        }
        log.debug("Sql execution Success");
    }

    /**
     * Executing sql query
     *
     * @param sqlQuery query statement
     * @return output result-set
     * @throws Exception exception
     */
    public ResultSet executeQuery(String sqlQuery) throws SQLException {
        stmt = dbConnection.createStatement();
        ResultSet resultSet = stmt.executeQuery(sqlQuery);
        stmt.close();
        return resultSet;
    }

    /**
     * execute sql statement
     *
     * @param sql sql statement
     * @throws Exception exception
     */
    public void execute(String sql) throws SQLException {
        stmt = dbConnection.createStatement();
        stmt.execute(sql);
        stmt.close();
    }

    /**
     *
     * @param sql
     * @return
     * @throws SQLException
     */

    public Statement getStatement(String sql) throws SQLException {
        return dbConnection.createStatement();

    }

    /**
     * disconnect from database server
     * @throws SQLException
     */

    public void disconnect() throws SQLException {
        dbConnection.close();
        log.debug("Disconnected from database");
    }

    protected void finalize() throws Throwable {
        try {
            if (!dbConnection.isClosed()) {
                disconnect();
            }

        } catch (SQLException e) {
            log.error("Error while disconnecting from database");
            throw new SQLException("Error while disconnecting from database");
        }
        super.finalize();
    }

}
