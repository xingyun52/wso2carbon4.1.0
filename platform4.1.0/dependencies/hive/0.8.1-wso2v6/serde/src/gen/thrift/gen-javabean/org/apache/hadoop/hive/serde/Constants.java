/**
 * Autogenerated by Thrift Compiler (0.7.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 */
package org.apache.hadoop.hive.serde;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

  public static final String SERIALIZATION_LIB = "serialization.lib";

  public static final String SERIALIZATION_CLASS = "serialization.class";

  public static final String SERIALIZATION_FORMAT = "serialization.format";

  public static final String SERIALIZATION_DDL = "serialization.ddl";

  public static final String SERIALIZATION_NULL_FORMAT = "serialization.null.format";

  public static final String SERIALIZATION_LAST_COLUMN_TAKES_REST = "serialization.last.column.takes.rest";

  public static final String SERIALIZATION_SORT_ORDER = "serialization.sort.order";

  public static final String SERIALIZATION_USE_JSON_OBJECTS = "serialization.use.json.object";

  public static final String FIELD_DELIM = "field.delim";

  public static final String COLLECTION_DELIM = "colelction.delim";

  public static final String LINE_DELIM = "line.delim";

  public static final String MAPKEY_DELIM = "mapkey.delim";

  public static final String QUOTE_CHAR = "quote.delim";

  public static final String ESCAPE_CHAR = "escape.delim";

  public static final String VOID_TYPE_NAME = "void";

  public static final String BOOLEAN_TYPE_NAME = "boolean";

  public static final String TINYINT_TYPE_NAME = "tinyint";

  public static final String SMALLINT_TYPE_NAME = "smallint";

  public static final String INT_TYPE_NAME = "int";

  public static final String BIGINT_TYPE_NAME = "bigint";

  public static final String FLOAT_TYPE_NAME = "float";

  public static final String DOUBLE_TYPE_NAME = "double";

  public static final String STRING_TYPE_NAME = "string";

  public static final String DATE_TYPE_NAME = "date";

  public static final String DATETIME_TYPE_NAME = "datetime";

  public static final String TIMESTAMP_TYPE_NAME = "timestamp";

  public static final String BINARY_TYPE_NAME = "binary";

  public static final String LIST_TYPE_NAME = "array";

  public static final String MAP_TYPE_NAME = "map";

  public static final String STRUCT_TYPE_NAME = "struct";

  public static final String UNION_TYPE_NAME = "uniontype";

  public static final String LIST_COLUMNS = "columns";

  public static final String LIST_COLUMN_TYPES = "columns.types";

  public static final Set<String> PrimitiveTypes = new HashSet<String>();
  static {
    PrimitiveTypes.add("void");
    PrimitiveTypes.add("boolean");
    PrimitiveTypes.add("tinyint");
    PrimitiveTypes.add("smallint");
    PrimitiveTypes.add("int");
    PrimitiveTypes.add("bigint");
    PrimitiveTypes.add("float");
    PrimitiveTypes.add("double");
    PrimitiveTypes.add("string");
    PrimitiveTypes.add("date");
    PrimitiveTypes.add("datetime");
    PrimitiveTypes.add("timestamp");
    PrimitiveTypes.add("binary");
  }

  public static final Set<String> CollectionTypes = new HashSet<String>();
  static {
    CollectionTypes.add("array");
    CollectionTypes.add("map");
  }

}
