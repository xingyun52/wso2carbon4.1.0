package org.wso2.carbon.hadoop.hive.jdbc.storage.utils;


import org.apache.hadoop.hive.serde2.io.ByteWritable;
import org.apache.hadoop.hive.serde2.io.ShortWritable;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.hadoop.hive.jdbc.storage.db.DatabaseProperties;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Commons {

    private static final Logger log = LoggerFactory.getLogger(Commons.class);

    public static final String BLOCK_OFFSET_INSIDE_FILE="BLOCK__OFFSET__INSIDE__FILE";
    public static final String INPUT_FILE_NAME ="INPUT__FILE__NAME";

    public static Object getObjectFromWritable(Writable w) {
        if (w instanceof IntWritable) {
            // int
            return ((IntWritable) w).get();
        } else if (w instanceof ShortWritable) {
            // short
            return ((ShortWritable) w).get();
        } else if (w instanceof ByteWritable) {
            // byte
            return ((ByteWritable) w).get();
        } else if (w instanceof BooleanWritable) {
            // boolean
            return ((BooleanWritable) w).get();
        } else if (w instanceof LongWritable) {
            // long
            return ((LongWritable) w).get();
        } else if (w instanceof FloatWritable ) {
            // float
            return ((FloatWritable) w).get();
        } else if (w instanceof DoubleWritable ) {
            // double
            return ((DoubleWritable) w).get();
        } else if (w instanceof org.apache.hadoop.hive.serde2.io.DoubleWritable){
            return ((org.apache.hadoop.hive.serde2.io.DoubleWritable) w).get();
            //double
        }else if (w instanceof NullWritable) {
            //null
            return null;
        } else {
            // treat as string
            return w.toString();
        }

    }


    /**
     * Set values for the prepared statement
     *
     * @param value     value
     * @param position
     * @param statement prepared statement
     * @return
     */
    public static PreparedStatement assignCorrectObjectType(Object value, int position,
                                                            PreparedStatement statement) {
        try {
            if (value instanceof Integer) {
                if (value != null) {
                    statement.setInt(position, (Integer) value);
                } else {
                    statement.setNull(position, Types.INTEGER);
                }

            } else if (value instanceof Short) {
                if (value != null) {
                    statement.setShort(position, (Short) value);
                } else {
                    statement.setNull(position, Types.SMALLINT);
                }
            } else if (value instanceof Byte) {
                if (value != null) {
                    statement.setByte(position, (Byte) value);
                } else {
                    statement.setNull(position, Types.TINYINT);
                }

            } else if (value instanceof Boolean) {
                if (value != null) {
                    statement.setBoolean(position, (Boolean) value);
                } else {
                    statement.setNull(position, Types.BIT);
                }

            } else if (value instanceof Long) {
                if (value != null) {
                    statement.setLong(position, (Long) value);
                } else {
                    statement.setNull(position, Types.BIGINT);
                }
            } else if (value instanceof Float) {
                if (value != null) {
                    statement.setFloat(position, (Float) value);
                } else {
                    statement.setNull(position, Types.REAL);
                }
            } else if (value instanceof Double) {
                if (value != null) {
                    statement.setDouble(position, (Double) value);
                } else {
                    statement.setNull(position, Types.DOUBLE);
                }
            } else {
                if (value != null) {
                    statement.setString(position, value.toString());
                } else {
                    statement.setNull(position,Types.VARCHAR);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to assign value to statement for position: " + position, e);
        }

        return statement;

    }

    //If user has given the query for creating table, we don't need to ask the table name again. Get the table name from the query
    public final static String extractingTableNameFromQuery(String createTableQuery) {
        String tableName = null;
        if (createTableQuery != null) {
            List<String> queryList = Arrays.asList(createTableQuery.split(" "));
            Iterator<String> iterator = queryList.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().equalsIgnoreCase("create")) {
                    while (iterator.hasNext()) {
                        if (iterator.next().equalsIgnoreCase("table")) {
                            while (iterator.hasNext()) {
                                String nextString = iterator.next();
                                if (!nextString.equalsIgnoreCase("")) {
                                    //create table foo(bar varchar(100)); split("\\(") needed for fixing
                                    // the issue If we get a create query like this.
                                    tableName = nextString.split("\\(")[0];
                                    return tableName;
                                }
                            }
                        }
                    }
                }

            }
        } else {
            throw new IllegalArgumentException("You should provide at least " +
                                               DBConfiguration.OUTPUT_TABLE_NAME_PROPERTY + " or " +
                                               ConfigurationUtils.HIVE_JDBC_TABLE_CREATE_QUERY + " property.");
        }
        return tableName;
    }

    public static final String extractFieldNames(String selectQuery) {
        String fields = "";
        List<String> queryList = Arrays.asList(selectQuery.split(" "));
        Iterator<String> iterator = queryList.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equalsIgnoreCase("select")) {
                while (iterator.hasNext()) {
                    String nextString = iterator.next();
                    if (nextString.equalsIgnoreCase("from")) {
                        return fields;
                    }
                    fields += nextString;
                }
            }
        }
        return fields;
    }

    // get database properties obj.
    public static DatabaseProperties getDbPropertiesObj(JobConf conf) {
        DatabaseProperties dbProperties = new DatabaseProperties();
        String connectionUrl = ConfigurationUtils.getConnectionUrl(conf);
        connectionUrl = connectionUrl.replaceAll(" ", "");
        dbProperties.setConnectionUrl(connectionUrl);
        dbProperties.setDriverClass(ConfigurationUtils.getDriverClass(conf));
        dbProperties.setUserName(ConfigurationUtils.getDatabaseUserName(conf));
        dbProperties.setPassword(ConfigurationUtils.getDatabasePassword(conf));
        return dbProperties;
    }

}
