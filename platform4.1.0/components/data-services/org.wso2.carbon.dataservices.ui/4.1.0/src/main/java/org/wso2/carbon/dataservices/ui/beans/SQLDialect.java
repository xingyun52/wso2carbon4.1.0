/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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
package org.wso2.carbon.dataservices.ui.beans;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;

/**
 * Object to support multiple queries 
 * ie <sql dialect="h2">SELECT employeeNumber,lastName,firstName,extension FROM Employees</sql>                                          
 *    <sql dialect="x2,mysql,x1">SELECT employeeNumber,reportsTo,jobTitle FROM Employees</sql>
 */

public class SQLDialect {
	
	private String dialect;
	
	private String sql;

	public String getDialect() {
		return dialect;
	}

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public SQLDialect(String dialect, String sql) {
		super();
		this.dialect = dialect;
		this.sql = sql;
	}
	
	public SQLDialect() {
		super();
	}

	public OMElement buildXML() {
    	OMFactory fac = OMAbstractFactory.getOMFactory();
    	OMElement sqlEl = fac.createOMElement("sql", null);
    	if ((this.getDialect() != null && this.getDialect().trim().length() > 0) && ((this.getSql() != null && this.getSql().trim().length() > 0))) {
    		sqlEl.addAttribute("dialect", this.getDialect().trim(), null);
    		sqlEl.setText(this.getSql().trim());
    	}
    	return sqlEl;
	}

}
