/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.serde2.lazy.objectinspector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hive.serde2.lazy.LazyMap;
import org.apache.hadoop.hive.serde2.objectinspector.MapObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;

import java.util.Map;

public class LazyCassandraMapObjectInspector extends LazyMapObjectInspector {

  public static final Log LOG = LogFactory.getLog(LazyMapObjectInspector.class.getName());

  ObjectInspector mapKeyObjectInspector;
  ObjectInspector mapValueObjectInspector;

  protected LazyCassandraMapObjectInspector(ObjectInspector mapKeyObjectInspector,
                                            ObjectInspector mapValueObjectInspector) {

    super(mapKeyObjectInspector, mapValueObjectInspector, (byte)0, (byte)0, null, false, (byte)0);
    this.mapKeyObjectInspector = mapKeyObjectInspector;
    this.mapValueObjectInspector = mapValueObjectInspector;
  }

  @Override
  public ObjectInspector getMapKeyObjectInspector() {
    return mapKeyObjectInspector;
  }

  @Override
  public ObjectInspector getMapValueObjectInspector() {
    return mapValueObjectInspector;
  }

  @Override
  public Object getMapValueElement(Object data, Object key) {
    if (data == null) {
      return null;
    }
    return ((LazyMap) data).getMapValueElement(key);
  }

  @Override
  public Map<?, ?> getMap(Object data) {
    if (data == null) {
      return null;
    }
    return ((LazyMap) data).getMap();
  }

  @Override
  public int getMapSize(Object data) {
    if (data == null) {
      return -1;
    }
    return ((LazyMap) data).getMapSize();
  }

  @Override
  public String getTypeName() {
    return org.apache.hadoop.hive.serde.Constants.MAP_TYPE_NAME + "<"
        + mapKeyObjectInspector.getTypeName() + ","
        + mapValueObjectInspector.getTypeName() + ">";
  }

}
