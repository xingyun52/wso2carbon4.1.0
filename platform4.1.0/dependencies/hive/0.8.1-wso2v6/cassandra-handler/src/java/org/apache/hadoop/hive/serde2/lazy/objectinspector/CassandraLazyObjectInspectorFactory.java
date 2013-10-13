package org.apache.hadoop.hive.serde2.lazy.objectinspector;

import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.hadoop.hive.serde2.lazy.objectinspector.primitive.LazyPrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.MapTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.io.Text;

import java.util.ArrayList;
import java.util.HashMap;

public class CassandraLazyObjectInspectorFactory {
  public static ObjectInspector getLazyObjectInspector(
      AbstractType validator) {
    return new CassandraValidatorObjectInspector(validator);
  }

/*  public static ObjectInspector getLazyCassandraMapObjectInspector(TypeInfo typeInfo) {
    ObjectInspector.Category c = typeInfo.getCategory();

    if (c.equals(ObjectInspector.Category.MAP)) {

    }
  } */

  static HashMap<ArrayList<Object>, LazyCassandraMapObjectInspector> cachedLazyCassandraMapObjectInspector =
          new HashMap<ArrayList<Object>, LazyCassandraMapObjectInspector>();

  public static ObjectInspector getLazyCassandraMapObjectInspector(TypeInfo typeInfo,
                                                                   boolean escaped, byte escapeChar) {
    ObjectInspector.Category c = typeInfo.getCategory();
    if (c.equals(ObjectInspector.Category.MAP)) {
      TypeInfo mapKeyTypeInfo = ((MapTypeInfo)typeInfo).getMapKeyTypeInfo();
      TypeInfo mapValueTypeInfo = ((MapTypeInfo)typeInfo).getMapValueTypeInfo();

      if (!mapKeyTypeInfo.getCategory().equals(ObjectInspector.Category.PRIMITIVE) ||
          !mapValueTypeInfo.getCategory().equals(ObjectInspector.Category.PRIMITIVE)) {
          throw new RuntimeException("Only primitive types allowed in Cassandra based Maps");
      }

      ObjectInspector mapKeyObjectInspector =
              LazyPrimitiveObjectInspectorFactory.getLazyObjectInspector(
                      ((PrimitiveTypeInfo) mapKeyTypeInfo).getPrimitiveCategory(), escaped,
                      escapeChar);
      ObjectInspector mapValueObjectInspector =
              LazyPrimitiveObjectInspectorFactory.getLazyObjectInspector(
                      ((PrimitiveTypeInfo) mapValueTypeInfo).getPrimitiveCategory(), escaped,
                      escapeChar);
      ArrayList<Object> signature = new ArrayList<Object>();
      signature.add(mapKeyObjectInspector);
      signature.add(mapValueObjectInspector);
      LazyCassandraMapObjectInspector result = cachedLazyCassandraMapObjectInspector
            .get(signature);
      if (result == null) {
        result = new LazyCassandraMapObjectInspector(mapKeyObjectInspector,
            mapValueObjectInspector);
        cachedLazyCassandraMapObjectInspector.put(signature, result);
      }
      return result;
    }

    throw new RuntimeException("Unexpected Cassandra type. Expected Map data type.");
  }

  private CassandraLazyObjectInspectorFactory() {
    // prevent instantiation
  }
}
