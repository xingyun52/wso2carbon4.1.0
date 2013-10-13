package org.apache.hadoop.hive.cassandra.input;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hive.serde2.lazy.ByteArrayRef;
import org.apache.hadoop.hive.serde2.lazy.LazyFactory;
import org.apache.hadoop.hive.serde2.lazy.LazyMap;
import org.apache.hadoop.hive.serde2.lazy.LazyPrimitive;
import org.apache.hadoop.hive.serde2.lazy.objectinspector.LazyMapObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.MapObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Writable;

public class LazyCassandraCellMap extends LazyMap{

  private List<BytesWritable> cassandraColumnsBB;
  private MapWritable columnMap;
  private List<Writable> mapColumnKeys = new ArrayList<Writable>();
  private ObjectInspector oi;

  protected LazyCassandraCellMap(LazyMapObjectInspector oi) {
    super(oi);
    this.oi = oi;
  }

  public void init(MapWritable columnMap, List<BytesWritable> cassandraColumnsBB) {
    this.columnMap = columnMap;
    this.cassandraColumnsBB = cassandraColumnsBB;
    setParsed(false);
  }

  private void parse() {
    if (cachedMap == null) {
      cachedMap = new LinkedHashMap<Object, Object>();
    } else {
      cachedMap.clear();
    }


    for (Map.Entry<Writable, Writable> entry : columnMap.entrySet()) {

        boolean columnFound = false;
        for (Writable column : cassandraColumnsBB) {
            if (column.equals(entry.getKey())) {
                columnFound = true;
                break;
            }
        }

        if (!columnFound) {
            mapColumnKeys.add(entry.getKey());
        }
    }
    setParsed(true);
  }

  /**
   * Get the value in the map for the given key.
   *
   * @param key
   * @return
   */

  @Override
  public Object getMapValueElement(Object key) {
    if (!getParsed()) {
      parse();
    }

    ByteArrayRef ref = new ByteArrayRef();
    for (Writable columnKeyBB : mapColumnKeys) {
       LazyPrimitive<?,?> keyObject = LazyFactory.createLazyPrimitiveClass(
                (PrimitiveObjectInspector) ((MapObjectInspector) oi).getMapKeyObjectInspector());

       ref.setData(((BytesWritable)columnKeyBB).getBytes());
       keyObject.init(ref, 0, ref.getData().length);

       if (keyObject.getWritableObject().equals(key)) {
          LazyPrimitive<?,?> valueObject = LazyFactory.createLazyPrimitiveClass(
                (PrimitiveObjectInspector) ((MapObjectInspector) oi).getMapValueObjectInspector());
          Writable valueBB = columnMap.get(columnKeyBB);

          if (valueBB != null) {
              ref.setData(((BytesWritable)valueBB).getBytes());

              valueObject.init(ref, 0, ref.getData().length);
              return valueObject.getObject();
          } else {
              return null;
          }
       }
    }

    return null;
  }
    
  @Override
  public Map<Object, Object> getMap() {
    if (!getParsed()) {
      parse();
    }

    for (Writable columnKey : mapColumnKeys) {
        LazyPrimitive<?,?> keyObject = LazyFactory.createLazyPrimitiveClass(
                (PrimitiveObjectInspector) ((MapObjectInspector) oi).getMapValueObjectInspector());
        LazyPrimitive<?,?> valueObject = LazyFactory.createLazyPrimitiveClass(
                (PrimitiveObjectInspector) ((MapObjectInspector) oi).getMapValueObjectInspector());
        Writable valueBB = columnMap.get(columnKey);

        ByteArrayRef ref = new ByteArrayRef();
        if (valueBB != null) {
            ref.setData(((BytesWritable)valueBB).getBytes());

            valueObject.init(ref, 0, ref.getData().length);
        } else {
            valueObject = null;
        }

        ref.setData(((BytesWritable)columnKey).getBytes());
        keyObject.init(ref, 0, ref.getData().length);

        if (valueObject != null) {
            cachedMap.put(keyObject.getObject(), valueObject.getObject());
        } else {
            cachedMap.put(keyObject.getObject(), null);
        }
    }
    return cachedMap;
  }

  @Override
  public int getMapSize() {
    if (!getParsed()) {
      parse();
    }
    return mapColumnKeys.size();
  }

}
