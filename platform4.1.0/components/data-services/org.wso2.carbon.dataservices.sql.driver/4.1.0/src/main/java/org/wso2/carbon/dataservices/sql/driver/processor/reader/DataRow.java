/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.dataservices.sql.driver.processor.reader;

import java.util.HashMap;
import java.util.Map;

public class DataRow {

    private int rowId;

    private Map<Integer, DataCell> cells;

    public DataRow(int rowId) {
        this.rowId = rowId;
        this.cells = new HashMap<Integer, DataCell>();
    }

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public Map<Integer, DataCell> getCells() {
        return cells;
    }

    public void setCells(Map<Integer, DataCell> cells) {
        this.cells = cells;
    }

    public void addCell(int cellId, DataCell cell) {
        this.getCells().put(cellId, cell);
    }

    public DataCell getCell(int id) {
        return getCells().get(id);
    }


}
