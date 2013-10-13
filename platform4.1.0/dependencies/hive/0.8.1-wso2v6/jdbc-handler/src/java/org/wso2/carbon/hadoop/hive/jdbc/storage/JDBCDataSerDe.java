package org.wso2.carbon.hadoop.hive.jdbc.storage;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.serde.Constants;
import org.apache.hadoop.hive.serde2.SerDe;
import org.apache.hadoop.hive.serde2.SerDeException;
import org.apache.hadoop.hive.serde2.SerDeStats;
import org.apache.hadoop.hive.serde2.objectinspector.*;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.AbstractPrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


public class JDBCDataSerDe implements SerDe {


    static final String HIVE_TYPE_DOUBLE = "double";
    static final String HIVE_TYPE_FLOAT = "float";
    static final String HIVE_TYPE_BOOLEAN = "boolean";
    static final String HIVE_TYPE_BIGINT = "bigint";
    static final String HIVE_TYPE_TINYINT = "tinyint";
    static final String HIVE_TYPE_SMALLINT = "smallint";
    static final String HIVE_TYPE_INT = "int";

    private final MapWritable cachedWritable = new MapWritable();

    private StructObjectInspector objectInspector;
    private int fieldCount;
    private List<String> columnNames;
    String[] columnTypesArray;

    private List<Object> row;

    public void initialize(Configuration entries, Properties properties) throws SerDeException {

        String tableColumnNamesString = properties.getProperty(Constants.LIST_COLUMNS);

        if (tableColumnNamesString != null) {

            String[] columnNamesArray = tableColumnNamesString.split(",");

/*            if (log.isDebugEnabled()) {
                log.debug("table column string: " + tblColumnNamesStr);
            }*/
            fieldCount = columnNamesArray.length;
            columnNames = new ArrayList<String>(columnNamesArray.length);
            columnNames.addAll(Arrays.asList(columnNamesArray));

            String columnTypesString = properties.getProperty(Constants.LIST_COLUMN_TYPES);

            columnTypesArray = columnTypesString.split(":");

            final List<ObjectInspector> fieldObjectInspectors = new ArrayList<ObjectInspector>(columnNamesArray.length);

            for (int i = 0; i < columnNamesArray.length; i++) {

                if (HIVE_TYPE_INT.equalsIgnoreCase(columnTypesArray[i])) {
                    fieldObjectInspectors.add(PrimitiveObjectInspectorFactory.javaIntObjectInspector);
                } else if (HIVE_TYPE_SMALLINT.equalsIgnoreCase(columnTypesArray[i])) {
                    fieldObjectInspectors.add(PrimitiveObjectInspectorFactory.javaShortObjectInspector);
                } else if (HIVE_TYPE_TINYINT.equalsIgnoreCase(columnTypesArray[i])) {
                    fieldObjectInspectors.add(PrimitiveObjectInspectorFactory.javaByteObjectInspector);
                } else if (HIVE_TYPE_BIGINT.equalsIgnoreCase(columnTypesArray[i])) {
                    fieldObjectInspectors.add(PrimitiveObjectInspectorFactory.javaLongObjectInspector);
                } else if (HIVE_TYPE_BOOLEAN.equalsIgnoreCase(columnTypesArray[i])) {
                    fieldObjectInspectors.add(PrimitiveObjectInspectorFactory.javaBooleanObjectInspector);
                } else if (HIVE_TYPE_FLOAT.equalsIgnoreCase(columnTypesArray[i])) {
                    fieldObjectInspectors.add(PrimitiveObjectInspectorFactory.javaFloatObjectInspector);
                } else if (HIVE_TYPE_DOUBLE.equalsIgnoreCase(columnTypesArray[i])) {
                    fieldObjectInspectors.add(PrimitiveObjectInspectorFactory.javaDoubleObjectInspector);
                } else {
                    // treat as string
                    fieldObjectInspectors.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);
                }
            }

            objectInspector = ObjectInspectorFactory
                    .getStandardStructObjectInspector(columnNames, fieldObjectInspectors);

        } else {
            throw new SerDeException("Can't find table column definitions");
        }
        row = new ArrayList<Object>(columnNames.size());
    }

    public Class<? extends Writable> getSerializedClass() {
        return MapWritable.class;
    }

    public Writable serialize(Object obj, ObjectInspector objectInspector) throws SerDeException {
        if (objectInspector.getCategory() != ObjectInspector.Category.STRUCT) {
            throw new SerDeException(getClass().toString()
                                     + " can only serialize struct types, but we got: "
                                     + objectInspector.getTypeName());
        }

        // Prepare the field ObjectInspectors
        StructObjectInspector structObjectInspector = (StructObjectInspector) objectInspector;

        final List<? extends StructField> fields = structObjectInspector.getAllStructFieldRefs();
        if (fields.size() != columnNames.size()) {
            throw new SerDeException(String.format("Required %d columns, received %d.", columnNames.size(), fields.size()));
        }

        cachedWritable.clear();

        for (int c = 0; c < fieldCount; c++) {
            StructField structField = fields.get(c);
            if (structField != null) {
                final Object field = structObjectInspector.getStructFieldData(obj,
                                                                              fields.get(c));
                //TODO:currently only support hive primitive type
                final AbstractPrimitiveObjectInspector fieldOI = (AbstractPrimitiveObjectInspector) fields.get(c)
                        .getFieldObjectInspector();

                Writable value = (Writable) fieldOI.getPrimitiveWritableObject(field);

                if (value == null) {
                    if (PrimitiveObjectInspector.PrimitiveCategory.STRING.equals(fieldOI.getPrimitiveCategory())) {
                        value = NullWritable.get();
                        //value = new Text("");
                    } else {
                        //TODO: now all treat as number
                        value = new IntWritable(0);
                    }
                }
                cachedWritable.put(new Text(columnNames.get(c)), value);
            }
        }
        return cachedWritable;
    }

    public Object deserialize(Writable writable) throws SerDeException {
        if (!(writable instanceof MapWritable)) {
            throw new SerDeException("Expected MapWritable, received "
                                     + writable.getClass().getName());
        }
        final MapWritable input = (MapWritable) writable;
        final Text t = new Text();
        row.clear();

        for (int i = 0; i < fieldCount; i++) {
            t.set(columnNames.get(i));
            final Writable value = input.get(t);
            if (value != null && !NullWritable.get().equals(value)) {
                //parse as double to avoid NumberFormatException...
                if (HIVE_TYPE_INT.equalsIgnoreCase(columnTypesArray[i])) {
                    row.add(Double.valueOf(value.toString()).intValue());
                } else if (HIVE_TYPE_SMALLINT.equalsIgnoreCase(columnTypesArray[i])) {
                    row.add(Double.valueOf(value.toString()).shortValue());
                } else if (HIVE_TYPE_TINYINT.equalsIgnoreCase(columnTypesArray[i])) {
                    row.add(Byte.valueOf(value.toString()));
                } else if (HIVE_TYPE_BIGINT.equalsIgnoreCase(columnTypesArray[i])) {
                    Long longValue;
                    if(value instanceof DoubleWritable){
                         longValue = Double.valueOf(value.toString()).longValue();
                    }else if(value instanceof IntWritable){
                        longValue = Integer.valueOf(value.toString()).longValue();
                    } else {
                        longValue = Long.valueOf(value.toString());
                    }
                    row.add(longValue);
                } else if (HIVE_TYPE_BOOLEAN.equalsIgnoreCase(columnTypesArray[i])) {
                    row.add(Boolean.valueOf(value.toString()));
                } else if (HIVE_TYPE_FLOAT.equalsIgnoreCase(columnTypesArray[i])) {
                    row.add(Double.valueOf(value.toString()).floatValue());
                } else if (HIVE_TYPE_DOUBLE.equalsIgnoreCase(columnTypesArray[i])) {
                    row.add(Double.valueOf(value.toString()));
                } else {
                    row.add(value.toString());
                }
            } else {
                row.add(null);
            }
        }

        return row;
    }

    public ObjectInspector getObjectInspector() throws SerDeException {
        return objectInspector;
    }

    public SerDeStats getSerDeStats() {
        return null;
    }

}
