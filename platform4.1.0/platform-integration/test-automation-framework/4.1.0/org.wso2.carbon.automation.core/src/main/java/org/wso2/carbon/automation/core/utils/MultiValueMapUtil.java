/*
* Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.wso2.carbon.automation.core.utils;

import java.util.Collection;


import java.util.*;

public class MultiValueMapUtil<K, V> {

    private Map<K, Collection<V>> map;

    private Class<? extends Collection<V>> clazz;

    public MultiValueMapUtil(Collection<V> coll) {
        map = new HashMap<K, Collection<V>>();
        this.clazz = (Class<? extends Collection<V>>) coll.getClass();
    }

    public void addValue(K key, V value) {
        Collection<V> collection = map.get(key);
        if (collection == null) {
            collection = createCollection();
            if (collection == null) {
                return;
            }
            map.put(key, collection);
        }
        collection.add(value);
    }

    public Collection<V> getValues(K key) {
        Collection<V> collection = map.get(key);
        if (collection == null) {
            return Collections.emptySet();
        }
        return collection;
    }

    public Set getKey() {
        Set keys = map.keySet();
        return keys;
    }

    private Collection<V> createCollection() {
        Collection<V> collection = null;
        try {
            collection = clazz.newInstance();
        } catch (InstantiationException ex) {
            // handling here
        } catch (IllegalAccessException ex) {
            // handling here
        }
        return collection;
    }

    public static void main(String[] args) {
        final String KEY = "KEY1";
        MultiValueMapUtil<String, String> mvm =
                new MultiValueMapUtil<String, String>(new ArrayList<String>());
        mvm.addValue(KEY, "bbb");
        mvm.addValue(KEY, "ddd");
        mvm.addValue(KEY, "ccc");
        mvm.addValue(KEY, "aaa");
        mvm.addValue(KEY, "eee");
        mvm.addValue("KEY2", "eee");
        mvm.addValue("KEY2", "eee");
        mvm.addValue("KEY2", "eee");

        System.out.println("For ArrayList: ");
        for (String value : mvm.getValues(KEY)) {
//            System.out.println("Value: "+value);
            System.out.println(mvm.map);
        }
        mvm = new MultiValueMapUtil<String, String>(new TreeSet<String>());
        mvm.addValue(KEY, "bbb");
        mvm.addValue(KEY, "ddd");
        mvm.addValue(KEY, "ccc");
        mvm.addValue(KEY, "aaa");
        mvm.addValue(KEY, "eee");
        System.out.println("For TreeSet: ");
        for (String value : mvm.getValues(KEY)) {
//            System.out.println("Value: "+value);
            System.out.println(mvm.map);
        }
    }
}