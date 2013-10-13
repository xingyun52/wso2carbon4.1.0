package org.wso2.carbon.hadoop.hive.jdbc.storage.db;


import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.hadoop.hive.jdbc.storage.datasource.CarbonDataSourceFetcher;
import org.wso2.carbon.hadoop.hive.jdbc.storage.utils.Commons;
import org.wso2.carbon.hadoop.hive.jdbc.storage.utils.ConfigurationUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBOperation {

    private static final Logger log = LoggerFactory.getLogger(DBOperation.class);

    public static int rowCountWritten = 0;

    DatabaseProperties dbProperties;
    List<String> fieldNames;
    List<Object> values;
    Map<String, Object> fieldNamesAndValuesMap = new HashMap<String, Object>();
    Connection connection = null;

    public DBOperation(DatabaseProperties databaseProperties, Connection con) {
        dbProperties = databaseProperties;
        connection = con;
    }

    public DBOperation() {
    }

    public void writeToDB(MapWritable map) throws SQLException {

        rowCountWritten++;

        System.out.println("^^^^^^^ Row count written : " + rowCountWritten + " ^^^^^^^");

        fillFieldNamesAndValueObj(map);
        
        PreparedStatement statement = null;
        try {
            if (!dbProperties.isUpdateOnDuplicate()) { //Insert every record
                if (log.isDebugEnabled()) {
                    log.debug("Inserting all data");
                }
                statement = insertData(statement);
            } else { //upsert
                if (dbProperties.getDbSpecificUpsertQuery() == null) {   //User haven't given the db specific upsert query
                    if (log.isDebugEnabled()) {
                        log.debug("Do the insert and update in DB independent manner.");
                    }

                    boolean isRowExisting = isRowExisting(statement);
                    if (isRowExisting) {     // If result is zero, then update
                        statement = updateData(statement);
                    } else {
                        statement = insertData(statement);
                    }

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Do the insert and update using DB specific query.");
                    }
                    String upsertQuery = dbProperties.getDbSpecificUpsertQuery();
                    statement = connection.prepareStatement(upsertQuery);
                    statement = setValuesForUpsertStatement(statement);
                    if (log.isDebugEnabled()) {
                        log.debug("Executing query: " + upsertQuery);
                    }
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            log.error("Failed to write data to database", e);
        } finally {
            statement.close();
        }
    }

    private void fillFieldNamesAndValueObj(MapWritable map) {

        fieldNames = new ArrayList<String>();
        values = new ArrayList<Object>();

        for (final Map.Entry<Writable, Writable> entry : map.entrySet()) {
            //If there is no mapping database table and metadata table are same
            if (dbProperties.getColumnMappingFields() == null) {
                fieldNames.add(entry.getKey().toString());
            }
            values.add(Commons.getObjectFromWritable(entry.getValue()));
        }
        if (dbProperties.getColumnMappingFields() != null) {
            fieldNames.addAll(Arrays.asList(dbProperties.getColumnMappingFields()));
        }
        for (int i = 0; i < fieldNames.size(); i++) {
            fieldNamesAndValuesMap.put(fieldNames.get(i), values.get(i));
        }

    }


    private PreparedStatement updateData(PreparedStatement statement) throws SQLException {
        QueryConstructor queryConstructor = new QueryConstructor();
        String sqlQuery = queryConstructor.constructUpdateQuery(dbProperties.getTableName(),
                                                                fieldNames, dbProperties.getPrimaryFields());
        statement = connection.prepareStatement(sqlQuery);
        statement = setValuesForUpdateStatement(statement);
        if(log.isDebugEnabled()){
            log.debug("Executing query: " + sqlQuery);
        }
        statement.executeUpdate();
        return statement;
    }

    private ResultSet selectData(PreparedStatement statement) throws SQLException {
        QueryConstructor queryConstructor = new QueryConstructor();
        String sqlQuery =queryConstructor.constructSelectQuery(dbProperties.getTableName(),
                                                               fieldNames, dbProperties.getPrimaryFields());
        statement = connection.prepareStatement(sqlQuery);
        statement = setValuesForWhereClause(statement);
        if(log.isDebugEnabled()){
            log.debug("Executing query: " + sqlQuery);
        }
        ResultSet resultSet = statement.executeQuery();
        return resultSet;
    }

    private boolean isRowExisting(PreparedStatement statement) throws SQLException {
        QueryConstructor queryConstructor = new QueryConstructor();
        String sqlQuery =queryConstructor.constructSelectQuery(dbProperties.getTableName(),
                                                               fieldNames, dbProperties.getPrimaryFields());
        statement = connection.prepareStatement(sqlQuery);
        statement = setValuesForWhereClause(statement);
        if(log.isDebugEnabled()){
            log.debug("Executing query: " + sqlQuery);
        }
        ResultSet resultSet = statement.executeQuery();

        boolean isRowExisting = false;
        if (resultSet.next()) {
            isRowExisting = true;
        }

        try {
            resultSet.close();
        } finally {
            statement.close();
        }

        return isRowExisting;

    }

    private PreparedStatement insertData(PreparedStatement statement) throws SQLException {
        QueryConstructor queryConstructor = new QueryConstructor();
        String sqlQuery = queryConstructor.constructInsertQuery(dbProperties.getTableName(),
                                                                fieldNames.toArray(new String[fieldNames.size()]));
        statement = connection.prepareStatement(sqlQuery);
        statement = setValues(statement);
        if(log.isDebugEnabled()){
            log.debug("Executing query: " + sqlQuery);
        }
        statement.executeUpdate();
        return statement;
    }


    private PreparedStatement setValuesForUpsertStatement(PreparedStatement statement) {
        String[] valuesOrder = dbProperties.getUpsertQueryValuesOrder();
        if (valuesOrder == null) {
            throw new IllegalArgumentException("You must supply both " +
                                               ConfigurationUtils.HIVE_JDBC_UPSERT_QUERY_VALUES_ORDER +
                                               " and " + ConfigurationUtils.HIVE_JDBC_OUTPUT_UPSERT_QUERY);
        }

        for (int valuesOrderCount = 0; valuesOrderCount < valuesOrder.length; valuesOrderCount++) {
            Object value = fieldNamesAndValuesMap.get(valuesOrder[valuesOrderCount].toLowerCase()); //Hive use lower case
            statement = Commons.assignCorrectObjectType(value, valuesOrderCount + 1, statement);
        }
        return statement;
    }

    private PreparedStatement setValuesForWhereClause(PreparedStatement statement) {

        String[] primaryFields = dbProperties.getPrimaryFields();
        if (primaryFields == null || primaryFields.length == 0) {
            primaryFields = new String[1];
            primaryFields[0] = fieldNames.get(0);
        }
        for (int fieldsCount = 0; fieldsCount < fieldNames.size(); fieldsCount++) {
            for (int primaryFieldsCount = 0; primaryFieldsCount < primaryFields.length; primaryFieldsCount++) {
                if (fieldNames.get(fieldsCount).equals(primaryFields[primaryFieldsCount])) {
                    statement = Commons.assignCorrectObjectType(values.get(fieldsCount), primaryFieldsCount + 1, statement);
                }
            }
        }
        return statement;
    }

    private PreparedStatement setValuesForUpdateStatement(PreparedStatement statement) {
        String[] primaryKeyFields = dbProperties.getPrimaryFields();
        if (primaryKeyFields == null) {
            primaryKeyFields = new String[1];
            primaryKeyFields[0] = fieldNames.get(0);
        }
        int counter = 0;
        for (int fieldCount = 0; fieldCount < fieldNames.size(); fieldCount++) {
            boolean isPrimaryField = false;
            for (int primaryFieldCount = 0; primaryFieldCount < primaryKeyFields.length; primaryFieldCount++) {
                if (fieldNames.get(fieldCount).equals(primaryKeyFields[primaryFieldCount])) {
                    statement = Commons.assignCorrectObjectType(values.get(fieldCount), fieldNames.size() - (primaryKeyFields.length - primaryFieldCount - 1),
                                                                statement);  //Primary key fields add in the where clause.
                    isPrimaryField = true;
                    break;
                }
            }
            if (!isPrimaryField) {
                counter++;
                statement = Commons.assignCorrectObjectType(values.get(fieldCount), counter, statement);
            }
        }
        return statement;
    }

    private PreparedStatement setValues(PreparedStatement statement) {
        for (int i = 0; i < values.size(); i++) {
            statement = Commons.assignCorrectObjectType(values.get(i), i + 1, statement);
        }
        return statement;
    }

    public boolean isTableExist(String tableName, Connection connection) throws SQLException {
        //This return all tables, we use this because it is not db specific, Passing table name doesn't
        //work with every database
        ResultSet tables = connection.getMetaData().getTables(null, null, "%", null);
        while (tables.next()) {
            if (tables.getString(3).equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        tables.close();
        return false;
    }


    public void createTableIfNotExist(Map<String, String> tableParameters) {
        String inputTable = tableParameters.get(DBConfiguration.INPUT_TABLE_NAME_PROPERTY);
        String outputTable = tableParameters.get(DBConfiguration.OUTPUT_TABLE_NAME_PROPERTY);
        String createTableQuery = tableParameters.get(ConfigurationUtils.HIVE_JDBC_TABLE_CREATE_QUERY);
        /*If inputTable=null, then most probably it should be a output table.
         In input table table must already exist.
          */
        if (inputTable == null && (outputTable != null || createTableQuery != null)) {
            DatabaseProperties dbProperties = new DatabaseProperties();
            dbProperties.setTableName(outputTable);
            dbProperties.setUserName(tableParameters.get(DBConfiguration.USERNAME_PROPERTY));
            dbProperties.setPassword(tableParameters.get(DBConfiguration.PASSWORD_PROPERTY));
            dbProperties.setConnectionUrl(tableParameters.get(DBConfiguration.URL_PROPERTY));
            dbProperties.setDriverClass(tableParameters.get(DBConfiguration.DRIVER_CLASS_PROPERTY));
            dbProperties.setDataSourceName(tableParameters.get(ConfigurationUtils.HIVE_PROP_CARBON_DS_NAME));

            if (dbProperties.getTableName() == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Extracting Table name from sql query");
                }
                dbProperties.setTableName(Commons.extractingTableNameFromQuery(createTableQuery));
            }

            if (dbProperties.getConnectionUrl() == null && dbProperties.getDataSourceName() != null) {
                CarbonDataSourceFetcher carbonDataSourceFetcher = new CarbonDataSourceFetcher();
                Map<String, String> dataSource = carbonDataSourceFetcher.getCarbonDataSource(
                        dbProperties.getDataSourceName());
                dbProperties.setConnectionUrl(dataSource.get(DBConfiguration.URL_PROPERTY));
                dbProperties.setDriverClass(dataSource.get(DBConfiguration.DRIVER_CLASS_PROPERTY));
                dbProperties.setUserName(dataSource.get(DBConfiguration.USERNAME_PROPERTY));
                dbProperties.setPassword(dataSource.get(DBConfiguration.PASSWORD_PROPERTY));
                // We are not getting connection pool parameters,
                // because this is just for creating a table.
            }

            DBManager dbManager = new DBManager();
            dbManager.createConnection(dbProperties);
            Connection connection = null;
            Statement statement = null;
            try {
                connection = dbManager.getConnection();
                if (!isTableExist(dbProperties.getTableName(), connection)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Creating table " + dbProperties.getTableName());
                    }
                    statement = connection.createStatement();
                    statement.executeUpdate(createTableQuery);
                }
            } catch (ClassNotFoundException e) {
                log.error("Failed to get connection", e);
            } catch (SQLException e) {
                log.error("Failed to create the table " + dbProperties.getTableName(), e);
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        log.error("Failed to close to statement", e);
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        log.error("Failed to close the connection", e);
                    }
                }
            }
        }
    }

    public int getTotalCount(String sql, Connection connection) throws SQLException {
        ResultSet resultSet = null;
        PreparedStatement statement = null;
        int noOfRows = 0;
        try {
            statement = connection.prepareStatement(sql);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                noOfRows = resultSet.getInt(1);
            } else {
                throw new SQLException("Can't get total rows count using sql " + sql);
            }
        } catch (SQLException e) {
            log.error("Failed to get total row count", e);
        } finally {
            resultSet.close();
            statement.close();
            connection.close();
        }
        return noOfRows;
    }

    public void runSQLQueryBeforeDataInsert(Map<String, String> tableParameters) {
        String sqlQueryBeforeDataInsert = tableParameters.get(ConfigurationUtils.HIVE_JDBC_OUTPUT_SQL_QUERY_BEFORE_DATA_INSERT);
        if(sqlQueryBeforeDataInsert !=null){
            DatabaseProperties dbProperties = new DatabaseProperties();
            dbProperties.setUserName(tableParameters.get(DBConfiguration.USERNAME_PROPERTY));
            dbProperties.setPassword(tableParameters.get(DBConfiguration.PASSWORD_PROPERTY));
            dbProperties.setConnectionUrl(tableParameters.get(DBConfiguration.URL_PROPERTY));
            dbProperties.setDriverClass(tableParameters.get(DBConfiguration.DRIVER_CLASS_PROPERTY));
            dbProperties.setDataSourceName(tableParameters.get(ConfigurationUtils.HIVE_PROP_CARBON_DS_NAME));

            if (dbProperties.getConnectionUrl() == null && dbProperties.getDataSourceName() != null) {
                CarbonDataSourceFetcher carbonDataSourceFetcher = new CarbonDataSourceFetcher();
                Map<String, String> dataSource = carbonDataSourceFetcher.getCarbonDataSource(
                        dbProperties.getDataSourceName());
                dbProperties.setConnectionUrl(dataSource.get(DBConfiguration.URL_PROPERTY));
                dbProperties.setDriverClass(dataSource.get(DBConfiguration.DRIVER_CLASS_PROPERTY));
                dbProperties.setUserName(dataSource.get(DBConfiguration.USERNAME_PROPERTY));
                dbProperties.setPassword(dataSource.get(DBConfiguration.PASSWORD_PROPERTY));
            }

            DBManager dbManager = new DBManager();
            dbManager.createConnection(dbProperties);
            Connection connection = null;
            Statement statement = null;

            try {
                connection = dbManager.getConnection();
                statement = connection.createStatement();
                statement.executeUpdate(sqlQueryBeforeDataInsert);
            } catch (ClassNotFoundException e) {
                log.error("Failed to get connection", e);
            } catch (SQLException e) {
                log.error("Failed to execute the statement " + sqlQueryBeforeDataInsert, e);
            } finally {
                try {
                    statement.close();
                    connection.close();
                } catch (SQLException e) {
                    log.error("Failed to close the statement/connection ",e);
                }
            }
        }
    }
}
