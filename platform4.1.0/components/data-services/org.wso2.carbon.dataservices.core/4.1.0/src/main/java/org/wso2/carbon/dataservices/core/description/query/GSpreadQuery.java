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
package org.wso2.carbon.dataservices.core.description.query;

import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import org.wso2.carbon.dataservices.core.DBUtils;
import org.wso2.carbon.dataservices.core.DataServiceFault;
import org.wso2.carbon.dataservices.core.description.config.GSpreadConfig;
import org.wso2.carbon.dataservices.core.description.event.EventTrigger;
import org.wso2.carbon.dataservices.core.engine.*;

import javax.xml.stream.XMLStreamWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a GSpread data services query.
 */
public class GSpreadQuery extends Query {

	private GSpreadConfig config;
	
	private int worksheetNumber;
	
	private int startingRow;
	
	private int maxRowCount;
	
	private boolean hasHeader;
		
	public GSpreadQuery(DataService dataService, String queryId,
			List<QueryParam> queryParams, String configId, int worksheetNumber,
			boolean hasHeader, int startingRow, int maxRowCount, Result result,
			EventTrigger inputEventTrigger, EventTrigger outputEventTrigger,
			Map<String, String> advancedProperties, String inputNamespace)
			throws DataServiceFault {
		super(dataService, queryId, queryParams, result, configId,
				inputEventTrigger, outputEventTrigger, advancedProperties, inputNamespace);
		try {
		    this.config = (GSpreadConfig) this.getDataService().getConfig(this.getConfigId());
		} catch (ClassCastException e) {
			throw new DataServiceFault(e, "Configuration is not a GSpread config:" + 
					this.getConfigId());
		}
		this.worksheetNumber = worksheetNumber;
		this.hasHeader = hasHeader;
		this.startingRow = startingRow;
		this.maxRowCount = maxRowCount;
	}
	
	public boolean isHasHeader() {
		return hasHeader;
	}
	
	public GSpreadConfig getConfig() {
		return config;
	}

	public int getMaxRowCount() {
		return maxRowCount;
	}

	public int getStartingRow() {
		return startingRow;
	}

	public int getWorksheetNumber() {
		return worksheetNumber;
	}
	
	public GSpreadResultSet retrieveData() throws Exception {
		URL worksheetUrl = new URL(this.getConfig().generateWorksheetFeedURL());
		WorksheetFeed feedw = this.getConfig().getFeed(worksheetUrl, WorksheetFeed.class);
		WorksheetEntry worksheetEntry = feedw.getEntries().get(this.getWorksheetNumber() - 1);			
		CellFeed feedc = this.getConfig().getFeed(worksheetEntry.getCellFeedUrl(), CellFeed.class);			
		List<CellEntry> entries = feedc.getEntries();			
		GSpreadResultSet grs = new GSpreadResultSet();
		
		/* store the data */
		for (CellEntry entry : entries) {
			grs.addCell(this.getPosStringFromId(entry.getId()), 
					entry.getTextContent().getContent().getPlainText());				
		}
		
		return grs;
	}
	
	private String[] getHeader(GSpreadResultSet grs) {
		if (!this.isHasHeader()) {
			return null;
		}
		String[] header = new String[grs.getColumnCount()];
		for (int i = 0; i < header.length; i++) {
			header[i] = grs.getValueAt(1, i + 1);
		}
		return header;
	}
	
	public void runQuery(XMLStreamWriter xmlWriter, InternalParamCollection params,	int queryLevel)
			throws DataServiceFault {
		try {
			/* get the data */
			GSpreadResultSet grs = this.retrieveData();
			/* if no data, return. */
			if (grs.getRowCount() == 0) {
				return;
			}			
			/* get the column mappings */
			Map<Integer, String> columnsMap = DBUtils.createColumnMappings(this.getHeader(grs));
			
			/* process the data */
			int rowCount, columnCount = grs.getColumnCount();
			if (this.getMaxRowCount() == -1) {
				rowCount = grs.getRowCount();
			} else {
			    rowCount = this.getMaxRowCount() + this.getStartingRow() - 1;
			    if (rowCount > grs.getRowCount()) {
					rowCount = grs.getRowCount();
				}
			}
			
			DataEntry dataEntry = new DataEntry();
			String columnValue;
			boolean useColumnNumbers = this.isUsingColumnNumbers();			
			for (int i = this.getStartingRow() - 1; i < rowCount; i++) {
				for (int j = 1; j <= columnCount; j++) {
					columnValue = grs.getValueAt(i + 1, j);
					dataEntry.addValue(useColumnNumbers ? Integer.toString(j) : 
						columnsMap.get(j), new ParamValue(columnValue));
				}				
				/* write data */
				this.writeResultEntry(xmlWriter, dataEntry, params, queryLevel);
			}
		} catch (Exception e) {
			throw new DataServiceFault(e, "Error in query GSpreadQuery.execute");
		}
	}
	
	private String getPosStringFromId(String id) {
		return id.substring(id.lastIndexOf("/") + 1);
	}
	
    private static class GSpreadResultSet {
		
		private HashMap<String, String> dataMap = null;
		
		private int rowCount = 0;
		
		private int columnCount = 0;
		
		public GSpreadResultSet() {
			this.dataMap = new HashMap<String, String>();
		}
		
		public void addCell(String pos, String val) {
			this.dataMap.put(pos, val);
			int[] intPos = this.getIntPos(pos);
			if (intPos[0] > this.rowCount) {
				this.rowCount = intPos[0];
			}
			if (intPos[1] > this.columnCount) {
				this.columnCount = intPos[1];
			}
		}
		
		public String getValueAt(int row, int col) {
			String value = this.dataMap.get("R" + row + "C" + col);
			if (value == null) {
				value = "";
			}
			return value;
		}
		
		private int[] getIntPos(String pos) {
			int[] intPos = new int[2];
			int i1 = pos.indexOf("C");
			intPos[0] = Integer.parseInt(pos.substring(1, i1));
			intPos[1] = Integer.parseInt(pos.substring(i1 + 1));
			return intPos;
		}
		
		public int getRowCount() {
			return this.rowCount;
		}
		
		public int getColumnCount() {
			return this.columnCount;
		}
		
	}
	
}
