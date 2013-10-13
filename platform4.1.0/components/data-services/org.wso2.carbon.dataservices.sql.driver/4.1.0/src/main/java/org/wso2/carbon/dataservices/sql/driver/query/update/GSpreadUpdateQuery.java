/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.dataservices.sql.driver.query.update;

import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;
import org.wso2.carbon.dataservices.sql.driver.TDriverUtil;
import org.wso2.carbon.dataservices.sql.driver.processor.reader.DataRow;
import org.wso2.carbon.dataservices.sql.driver.query.ColumnInfo;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class GSpreadUpdateQuery extends UpdateQuery {
    
    public GSpreadUpdateQuery(Statement stmt) throws SQLException {
        super(stmt);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        this.executeSQL();
        return null;
    }

    @Override
    public int executeUpdate() throws SQLException {
        return this.executeSQL();
    }

    @Override
    public boolean execute() throws SQLException {
        return (this.executeSQL() > 0);
    }

    private int executeSQL() throws SQLException {
        int count = 0;
        Map<Integer, DataRow> result;
        if (getCondition().getLhs() == null && getCondition().getRhs() == null) {
            result = getTargetTable().getRows();
        } else {
            result = getCondition().process(getTargetTable());
        }
        WorksheetEntry currentWorkSheet =
                TDriverUtil.getCurrentWorkSheetEntry(getConnection(), getTargetTableName());
        if (currentWorkSheet == null) {
            throw new SQLException("WorkSheet '" + getTargetTableName() + "' does not exist");
        }

        ListFeed listFeed = TDriverUtil.getListFeed(getConnection(), currentWorkSheet);
        for (Map.Entry<Integer, DataRow> row : result.entrySet()) {
            ListEntry listEntry = listFeed.getEntries().get(row.getKey() - 1);
            for (ColumnInfo column : getTargetColumns()) {
                listEntry.getCustomElements().setValueLocal(column.getName(),
                        column.getValue().toString());
            }
            try {
                listEntry.update();
                count++;
            } catch (IOException e) {
                throw new SQLException("Error occurred while updating the record ", e);
            } catch (ServiceException e) {
                throw new SQLException("Error occurred while updating the record", e);
            }
        }
        return count;
    }

    
}
