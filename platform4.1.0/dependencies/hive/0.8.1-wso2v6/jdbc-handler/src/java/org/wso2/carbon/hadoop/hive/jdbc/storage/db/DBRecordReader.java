package org.wso2.carbon.hadoop.hive.jdbc.storage.db;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.hadoop.hive.jdbc.storage.exception.UnsupportedDatabaseException;
import org.wso2.carbon.hadoop.hive.jdbc.storage.input.JDBCSplit;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.RecordReader;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class DBRecordReader implements RecordReader<LongWritable, MapWritable> {

    private static final Logger log = LoggerFactory.getLogger(DBRecordReader.class);

    private Connection connection;
    private Statement statement;
    private ResultSet results;
    private JDBCSplit split;
    private long pos = 0;
    private DatabaseProperties databaseProperties;


    public DBRecordReader(JDBCSplit split, DatabaseProperties dbProperties,
                          DBManager dbManager) {
        DatabaseType databaseType = null;
        String sqlQuery = null;
        try {
            this.split = split;
            databaseProperties = dbProperties;
            connection = dbManager.getConnection();
            connection.setAutoCommit(false);
            statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if (dbProperties.getConnectionUrl() != null) {
                String databaseName = dbProperties.getConnectionUrl().split(":")[1];
                databaseType = dbManager.getDatabaseType(databaseName);
            } else {
                databaseType = dbManager.getDatabaseName(connection);
            }
            sqlQuery = new QueryConstructor().constructSelectQueryForReading(dbProperties, split,
                                                                             databaseType);
            results = statement.executeQuery(sqlQuery);

        } catch (ClassNotFoundException e) {
            log.error("Failed to get connection", e);
        } catch (SQLException e) {
            log.error("Failed to fetch data from database using query: " + sqlQuery, e);
        } catch (UnsupportedDatabaseException e) {
            log.error("This database doesn't support by hive-jdbc-handler", e);
        }
    }

    public boolean next(LongWritable key, MapWritable value) throws IOException {
        try {
            if (!results.next()) {
                return false;
            }

            // Set the key field value as the output key value
            key.set(pos + split.getStart());

            ResultSetMetaData resultsMetaData = results.getMetaData();
            int columnCount = resultsMetaData.getColumnCount();

            List<String> names = new ArrayList<String>();
            List<Integer> types = new ArrayList<Integer>();
            // The column count starts from 1
            for (int i = 1; i <= columnCount; i++) {
                //This is the column name in db table
                String name = resultsMetaData.getColumnName(i).toLowerCase();
                //Get the relevant metaTable name
                name = databaseProperties.getInputColumnMappingFields().get(name);
                int type = resultsMetaData.getColumnType(i);
                //Hive keeps column names in lowercase

                names.add(name.toLowerCase());
                types.add(type);
            }

            for (int j = 0; j < types.size(); j++) {
                value.put(new Text(names.get(j)), getActualObjectTypeForValue(results, types, j));
            }

            pos++;
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
        return true;
    }


    private Writable getActualObjectTypeForValue(ResultSet results, List<Integer> types, int i) {
        int sqlType = types.get(i);
        int columnNo = i + 1;
        try {
            //Only primitive data types will be supported

            switch (sqlType) {
                case Types.CHAR:
                case Types.VARCHAR:
                case Types.NVARCHAR:
                case Types.NCHAR:
                case Types.LONGNVARCHAR:
                    String result = results.getString(columnNo);
                    if(result != null) {
                        return new Text(results.getString(columnNo));
                    }else {
                        return new Text("");
                    }
                case Types.INTEGER:
                case Types.SMALLINT:
                    return new IntWritable(results.getInt(columnNo));

                case Types.BIT:
                    return new BooleanWritable(results.getBoolean(columnNo));

                case Types.NUMERIC:
                    return new DoubleWritable(results.getBigDecimal(columnNo).doubleValue());

                case Types.BIGINT:
                    return new LongWritable(results.getLong(columnNo));

                case Types.REAL:
                    return new FloatWritable(results.getFloat(columnNo));

                case Types.DOUBLE:
                    return new DoubleWritable(results.getDouble(columnNo));

                case Types.TINYINT:
                    return new ByteWritable(results.getByte(columnNo));

                case Types.NULL:
                    return null;

            }
            //Others treated as string data type
            return new Text(results.getString(columnNo));
        } catch (SQLException e) {
            log.error("Failed to get value for column no: " + columnNo, e);
        }
        return null;
    }

    public LongWritable createKey() {
        return new LongWritable();
    }

    public MapWritable createValue() {
        return new MapWritable();
    }

    public long getPos() throws IOException {
        return pos;
    }

    public void close() throws IOException {
        try {
            connection.commit();
            results.close();
            statement.close();
        } catch (SQLException e) {
            log.error("Failed to close", e);
        }
    }

    public float getProgress() throws IOException {
        return split.getLength() > 0 ? pos / (float) split.getLength() : 1.0f;
    }
}
