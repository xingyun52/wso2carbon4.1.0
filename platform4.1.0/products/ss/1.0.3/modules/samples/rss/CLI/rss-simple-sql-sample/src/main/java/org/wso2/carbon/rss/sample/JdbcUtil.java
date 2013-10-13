/*
 * Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.rss.sample;

import java.io.InputStream;
import java.sql.*;
import java.util.Scanner;

public class JdbcUtil {

    public static Connection getConnection() {

        Connection conn = null;
        //  jdbc:mysql://localhost/rss01
        String jdbcUrl = System.getProperty("jdbcurl");
        //  com.mysql.jdbc.Driver
        String driverClass = System.getProperty("driver");
        String userName = System.getProperty("username");
        String userPassword = System.getProperty("password");

        try {
            Class.forName(driverClass).newInstance();
            conn = DriverManager.getConnection(jdbcUrl, userName, userPassword);
        } catch (ClassNotFoundException ex) {
            System.err.println(ex.getMessage());
        } catch (IllegalAccessException ex) {
            System.err.println(ex.getMessage());
        } catch (InstantiationException ex) {
            System.err.println(ex.getMessage());
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
        return conn;
    }

    public static void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    public static void createDatabase(Connection connection) throws SQLException {
        String sql = "CREATE DATABASE rssdb";
        Statement statement = null;
        statement = connection.createStatement();
        statement.executeUpdate(sql);
        System.out.println("ORDER Database created successfully.");
    }

    public static void executeSqlScript(Connection conn, InputStream inputFile) {

        // Delimiter
        String delimiter = ";";

        // Create scanner
        Scanner scanner;
        scanner = new Scanner(inputFile).useDelimiter(delimiter);

        // Loop through the SQL file statements
        Statement currentStatement = null;
        while (scanner.hasNext()) {

            // Get statement
            String rawStatement = scanner.next() + delimiter;
            try {
                // Execute statement
                currentStatement = conn.createStatement();
                currentStatement.execute(rawStatement);
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                // Release resources
                if (currentStatement != null) {
                    try {
                        currentStatement.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                currentStatement = null;
            }
        }
    }

    public static void executeQuery(Connection connection) throws SQLException {

        String sql01 = "INSERT INTO Persons (P_Id,LastName,FirstName,Address,City) VALUES (1,'Hansen','Ola','Timoteivn,10','Sandnes')";
        String sql02 = "INSERT INTO Persons (P_Id,LastName,FirstName,Address,City) VALUES (2,'Svendson','Tove','Borgvn,23','Sandnes')";
        String sql03 = "INSERT INTO Persons (P_Id,LastName,FirstName,Address,City) VALUES (3,'Pettersen','Kari','Storgt,20','Stavanger')";

        String sql04 = "INSERT INTO Orders (O_Id,OrderNo,P_Id) VALUES (1,77895,3)";
        String sql05 = "INSERT INTO Orders (O_Id,OrderNo,P_Id) VALUES (2,44678,3)";
        String sql06 = "INSERT INTO Orders (O_Id,OrderNo,P_Id) VALUES (3,22456,2)";
        String sql07 = "INSERT INTO Orders (O_Id,OrderNo,P_Id) VALUES (4,24562,1)";

        Statement statement = null;
        statement = connection.createStatement();
        statement.execute(sql01);
        statement.execute(sql02);
        statement.execute(sql03);
        statement.execute(sql04);
        statement.execute(sql05);
        statement.execute(sql06);
        statement.execute(sql07);

        System.out.println("SQL query executed successfully.");
    }

    public static void getQueryResults(Connection connection) throws SQLException {

        String sql01 = "SELECT * FROM Persons";
        String sql02 = "SELECT * FROM Orders";

        Statement statement = null;
        Statement statement2 = null;
        statement = connection.createStatement();
        ResultSet resultSet01 =  statement.executeQuery(sql01);
        statement2 = connection.createStatement();
        ResultSet resultSet02 =  statement2.executeQuery(sql02);

        while(resultSet01.next()){
            System.out.println(resultSet01.getString(2));
        }

        while(resultSet02.next()){
            System.out.println(resultSet02.getString(2));
        }

    }
}
