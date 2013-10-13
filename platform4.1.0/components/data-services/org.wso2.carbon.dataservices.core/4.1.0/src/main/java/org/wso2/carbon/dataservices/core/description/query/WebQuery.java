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
package org.wso2.carbon.dataservices.core.description.query;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.log4j.Logger;
import org.webharvest.runtime.variables.Variable;
import org.wso2.carbon.dataservices.core.DataServiceFault;
import org.wso2.carbon.dataservices.core.description.config.WebConfig;
import org.wso2.carbon.dataservices.core.description.event.EventTrigger;
import org.wso2.carbon.dataservices.core.engine.DataEntry;
import org.wso2.carbon.dataservices.core.engine.DataService;
import org.wso2.carbon.dataservices.core.engine.InternalParamCollection;
import org.wso2.carbon.dataservices.core.engine.ParamValue;
import org.wso2.carbon.dataservices.core.engine.QueryParam;
import org.wso2.carbon.dataservices.core.engine.Result;

import javax.xml.stream.XMLStreamWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class represents a Web query in a data service.
 */
public class WebQuery extends Query {

    private WebConfig config;

    private String scraperVariable;

    private static Logger log = Logger.getLogger(WebQuery.class);

    public WebQuery(DataService dataService, String queryId,
                    List<QueryParam> queryParams, String configId, Result result,
                    EventTrigger inputEventTrigger, EventTrigger outputEventTrigger,
                    Map<String, String> advancedProperties, String scraperVariable,
                    String inputNamespace)
            throws DataServiceFault {
		super(dataService, queryId, queryParams, result, configId,
				inputEventTrigger, outputEventTrigger, advancedProperties, inputNamespace);
        try {
            this.config = (WebConfig) this.getDataService().getConfig(this.getConfigId());
        } catch (ClassCastException e) {
			throw new DataServiceFault(e, "Configuration is not an Web config:"
					+ this.getConfigId());
        }
        this.scraperVariable = scraperVariable;
    }

    public WebConfig getConfig() {
        return config;
    }

    public String getScraperVariable() {
        return scraperVariable;
    }

    @SuppressWarnings("unchecked")
    public void runQuery(XMLStreamWriter xmlWriter, InternalParamCollection params,
                                     int queryLevel)
            throws DataServiceFault {
        Variable scrapedOutput = this.getConfig().getScrapedResult(getScraperVariable());
        try {
            OMElement resultEl = AXIOMUtil.stringToOM(scrapedOutput.toString());
            OMElement entryEl, fieldEl;
            DataEntry dataEntry;
            boolean useColumnNumbers = this.isUsingColumnNumbers();
            int i;
            for (Iterator<OMElement> recordItr = resultEl.getChildElements(); recordItr.hasNext();) {
                entryEl = recordItr.next();
                dataEntry = new DataEntry();
                i = 1;
                for (Iterator<OMElement> fieldItr = entryEl.getChildElements(); fieldItr.hasNext();) {
                    fieldEl = fieldItr.next();
                    dataEntry.addValue(useColumnNumbers ? Integer.toString(i) : 
                    	fieldEl.getLocalName(), new ParamValue(fieldEl.getText()));
                    i++;
                }
                this.writeResultEntry(xmlWriter, dataEntry, params, queryLevel);
            }
        } catch (Exception e) {
            log.error("Error in executing web scraping query", e);
            throw new DataServiceFault(e);
        }
    }

}
