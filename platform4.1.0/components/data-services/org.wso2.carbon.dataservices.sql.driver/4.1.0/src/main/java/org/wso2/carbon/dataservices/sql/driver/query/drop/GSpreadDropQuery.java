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
package org.wso2.carbon.dataservices.sql.driver.query.drop;

import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;
import org.wso2.carbon.dataservices.sql.driver.TDriverUtil;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GSpreadDropQuery extends DropQuery {

    public GSpreadDropQuery(Statement stmt) throws SQLException {
        super(stmt);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        this.executeSQL();
        return null;
    }

    @Override
    public int executeUpdate() throws SQLException {
        this.executeSQL();
        return 0;
    }

    @Override
    public boolean execute() throws SQLException {
        this.executeSQL();
        return false;
    }

    private synchronized void executeSQL() throws SQLException {
        WorksheetEntry currentWorkSheet =
                TDriverUtil.getCurrentWorkSheetEntry(this.getConnection(), this.getTableName());

        if (currentWorkSheet == null) {
            throw new SQLException("WorkSheet named '" + this.getTableName() + "' does not exist");
        }

        try {
            currentWorkSheet.delete();
        } catch (IOException e) {
            throw new SQLException("Error occurred while deleting the work sheet entry '" +
                    this.getTableName() + "'", e);
        } catch (ServiceException e) {
            throw new SQLException("Error occurred while deleting the work sheet entry '" +
                    this.getTableName() + "'", e);
        }
    }

}
