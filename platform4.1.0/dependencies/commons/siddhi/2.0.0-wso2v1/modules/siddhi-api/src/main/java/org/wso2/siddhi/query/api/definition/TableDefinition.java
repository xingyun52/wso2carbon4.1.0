/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.siddhi.query.api.definition;

public class TableDefinition extends AbstractDefinition {

    private ExternalTable externalTable = null;

    public TableDefinition name(String streamId) {
        id = streamId;
        return this;
    }

    public TableDefinition attribute(String attributeName, Attribute.Type type) {
        checkAttribute(attributeName);
        this.attributeList.add(new Attribute(attributeName, type));
        return this;
    }

    public String getTableId() {
        return id;
    }

    @Override
    public String toString() {
        return "TableDefinition{" +
               "tableId='" + id + '\'' +
               ", attributeList=" + attributeList + '\'' +
               ", externalTable=" + externalTable + '\'' +
               "} ";
    }

    public TableDefinition from(String tableType, String databaseName, String tableName) {
        this.externalTable = new ExternalTable(tableType, databaseName, tableName);
        return this;
    }


    public class ExternalTable {
        private String tableType;
        private String databaseName;
        private String tableName;

        public ExternalTable(String tableType, String databaseName, String tableName) {
            this.tableType = tableType;
            this.databaseName = databaseName;
            this.tableName = tableName;
        }

        public String getTableType() {
            return tableType;
        }

        public String getDatabaseName() {
            return databaseName;
        }

        public String getTableName() {
            return tableName;
        }

        @Override
        public String toString() {
            return "ExternalTable{" +
                   "tableType='" + tableType + '\'' +
                   ", databaseName='" + databaseName + '\'' +
                   ", tableName='" + tableName + '\'' +
                   '}';
        }
    }
}
