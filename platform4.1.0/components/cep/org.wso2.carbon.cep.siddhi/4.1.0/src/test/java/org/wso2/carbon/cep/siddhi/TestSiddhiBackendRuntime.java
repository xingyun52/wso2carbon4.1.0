/*
 * Copyright 2004,2012 The Apache Software Foundation.
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

package org.wso2.carbon.cep.siddhi;

import junit.framework.TestCase;
import org.wso2.carbon.broker.core.exception.BrokerConfigException;
import org.wso2.carbon.cep.core.Expression;
import org.wso2.carbon.cep.core.backend.CEPBackEndRuntime;
import org.wso2.carbon.cep.core.exception.CEPConfigurationException;
import org.wso2.carbon.cep.core.exception.CEPEventProcessingException;
import org.wso2.carbon.cep.core.mapping.input.mapping.InputMapping;
import org.wso2.carbon.cep.core.mapping.input.mapping.TupleInputMapping;
import org.wso2.carbon.cep.core.mapping.input.property.TupleInputProperty;
import org.wso2.carbon.cep.siddhi.backend.SiddhiBackEndRuntimeFactory;
import org.wso2.carbon.databridge.commons.Event;

import java.util.ArrayList;
import java.util.List;

public class TestSiddhiBackendRuntime extends TestCase {

    public void testBAMEvent()
            throws CEPConfigurationException, CEPEventProcessingException, InterruptedException, BrokerConfigException {

        SiddhiBackEndRuntimeFactory factory = new SiddhiBackEndRuntimeFactory();

        //inputMapping
        TupleInputMapping mapping = new TupleInputMapping();
        mapping.setStream("Event");
        TupleInputProperty property = null;

        property = new TupleInputProperty();
        property.setName("hostName");
        property.setInputName("hostName");
        property.setType("java.lang.String");
        property.setInputName("metaData");
        mapping.addProperty(property);

        property = new TupleInputProperty();
        property.setName("requestCount");
        property.setInputName("requestCount");
        property.setType("java.lang.Integer");
        property.setInputDataType("payloadData");
        mapping.addProperty(property);

        List<InputMapping> inputMappings = new ArrayList<InputMapping>();
        inputMappings.add(mapping);

        //add inputMappings to Siddhi backend
        CEPBackEndRuntime siddhiBackendRuntime = factory.createCEPBackEndRuntime("Processor1", null, inputMappings, 0);

        //Query
        Expression expression = new Expression();
        expression.setText("from Event[requestCount >=requestCount]#window.length(5) " +
                           " insert into OutputStream  requestCount,hostName, sum(requestCount) as totalRequestCount" +
                           " having totalRequestCount>1");

        siddhiBackendRuntime.addQuery("bamquery", expression, new DummyCEPListener(null, 0, null, 0));

        //Event
        Event event = new Event();
        event.setStreamId("Event");
        event.setPayloadData(new Object[]{"http://localhost:8443/carbon", new Integer(21)});

        siddhiBackendRuntime.insertEvent(event, mapping);
        Thread.sleep(1000);


    }

    public void testQuery()
            throws CEPConfigurationException, CEPEventProcessingException, InterruptedException, BrokerConfigException {


        SiddhiBackEndRuntimeFactory factory = new SiddhiBackEndRuntimeFactory();

        TupleInputMapping mapping2 = new TupleInputMapping();
        mapping2.setStream("testEvent");
        TupleInputProperty property2 = null;

        property2 = new TupleInputProperty();
        property2.setName("minimumResponseTime");
        property2.setType("java.lang.Double");
        mapping2.addProperty(property2);

        property2 = new TupleInputProperty();
        property2.setName("maximumResponseTime");
        property2.setType("java.lang.Double");
        mapping2.addProperty(property2);

        List<InputMapping> inputMapping2s = new ArrayList<InputMapping>();
        inputMapping2s.add(mapping2);


        CEPBackEndRuntime esperBackendRuntime = factory.createCEPBackEndRuntime("Processor2", null, inputMapping2s, 0);

        Expression expression2 = new Expression();
        expression2.setText("from testEvent[maximumResponseTime > -1]#window.length(5)" +
                            " insert into ResponseStream  minimumResponseTime, maximumResponseTime, avg(maximumResponseTime) as avgMaximumResponseTime" +
                            " having  avgMaximumResponseTime > 100");
        esperBackendRuntime.addQuery("bamquery", expression2, new DummyCEPListener(null, 0, null, 1));

        Event event = new Event();
        event.setStreamId("testEvent");
        event.setPayloadData(new Object[]{new Double(121), new Double(0)});
        esperBackendRuntime.insertEvent(event, mapping2);
        Thread.sleep(1000);


    }

    public void testStockTweet() throws InterruptedException,
            CEPEventProcessingException, CEPConfigurationException, BrokerConfigException {

        SiddhiBackEndRuntimeFactory esperBackendRuntimeFactory = new SiddhiBackEndRuntimeFactory();

        TupleInputMapping allStockQuotesMapping = new TupleInputMapping();
        allStockQuotesMapping.setStream("allStockQuotes");

        TupleInputProperty symbol = new TupleInputProperty();
        symbol.setName("symbol");
        symbol.setInputName("symbol");
        symbol.setType("java.lang.String");
        allStockQuotesMapping.addProperty(symbol);

        TupleInputProperty price = new TupleInputProperty();
        price.setName("price");
        price.setInputName("price");
        price.setType("java.lang.Double");
        allStockQuotesMapping.addProperty(price);

        TupleInputMapping twitterFeed = new TupleInputMapping();
        twitterFeed.setStream("twitterFeed");

        TupleInputProperty company = new TupleInputProperty();
        company.setName("company");
        company.setInputName("company");
        company.setType("java.lang.String");
        twitterFeed.addProperty(company);

        TupleInputProperty wordCount = new TupleInputProperty();
        wordCount.setName("wordCount");
        wordCount.setInputName("wordCount");
        wordCount.setType("java.lang.Integer");
        twitterFeed.addProperty(wordCount);

        ArrayList<InputMapping> mappings = new ArrayList<InputMapping>();
        mappings.add(allStockQuotesMapping);
        mappings.add(twitterFeed);

        CEPBackEndRuntime cepBackEndRuntime =
                esperBackendRuntimeFactory.createCEPBackEndRuntime("Provider1", null, mappings, 0);

//        Query query = new Query();
//        query.setName("StocksPredictor");

        String fastStocksStream = "from  allStockQuotes#window.time(600000)" +
                                  " insert into fastMovingStockQuotes symbol, price, avg(price) as averagePrice" +
                                  " group by symbol " +
                                  " having ( price > averagePrice*1.02) or ( averagePrice*0.98 > price ) ";
//        String fastStocksStream = "fastMovingStockQuotes:= select symbol, price, averagePrice=avg(price) from  allStockQuotes [win.time=600000] where symbol contains '' group by symbol having ((price > (averagePrice*1.02)) or (averagePrice*0.98)> price ))";
        String workCountStream = "from  twitterFeed#window.time(600000)" +
                                 " insert into wordCounts company, sum(wordCount) as words" +
                                 " group by company " +
                                 " having (words > 10)";
//        String workCountStream = "wordCounts:= select company, words=sum(wordCount) from  twitterFeed[win.time=600000] where company contains '' group by company having (words > 10)";
        String selectFastMovingHighWordCount = "from fastMovingStockQuotes#window.time(600000) join wordCounts#window.time(600000)  on fastMovingStockQuotes.symbol== wordCounts.company" +
                                               " insert into fastMovingHighWordCount fastMovingStockQuotes.symbol as company, fastMovingStockQuotes.averagePrice as averagePrice, wordCounts.words as words ";
//        String selectFastMovingHighWordCount = "fastMovingHighWordCount:=select company=fastMovingStockQuotes.symbol,averagePrice=fastMovingStockQuotes.averagePrice, words=wordCounts.words from fastMovingStockQuotes[win.time=600000] , wordCounts[win.time=600000]  where fastMovingStockQuotes.symbol==wordCounts.company";


        Expression fastStockStreamExpression = new Expression();
        fastStockStreamExpression.setText(fastStocksStream);

        cepBackEndRuntime.addQuery("fastStocksStream", fastStockStreamExpression, null);

        Expression wordCountStreamExpression = new Expression();
        wordCountStreamExpression.setText(workCountStream);

        cepBackEndRuntime.addQuery("workCountStream", wordCountStreamExpression, null);

        Expression selectFastMovingHighWordCountExpression = new Expression();
        selectFastMovingHighWordCountExpression.setText(selectFastMovingHighWordCount);

        DummyCEPListener testCEPListner = new DummyCEPListener(null, 0, null, 2);

        cepBackEndRuntime.addQuery("selectFastMovingHighWordCount", selectFastMovingHighWordCountExpression, testCEPListner);

        Event event = new Event();
        event.setStreamId("testEvent");
        event.setPayloadData(new Object[]{"IBM", 25.23});

        cepBackEndRuntime.insertEvent(event, allStockQuotesMapping);

        event = new Event();
        event.setStreamId("testEvent");
        event.setPayloadData(new Object[]{"IBM", 30.23});

        cepBackEndRuntime.insertEvent(event, allStockQuotesMapping);

        event = new Event();
        event.setStreamId("testEvent");
        event.setPayloadData(new Object[]{"IBM", 40.23});

        cepBackEndRuntime.insertEvent(event, allStockQuotesMapping);

        event = new Event();
        event.setStreamId("testEvent");
        event.setPayloadData(new Object[]{"IBM", 8});

        cepBackEndRuntime.insertEvent(event, twitterFeed);

        event = new Event();
        event.setStreamId("testEvent");
        event.setPayloadData(new Object[]{"IBM", 8});

        cepBackEndRuntime.insertEvent(event, twitterFeed);
        Thread.sleep(3000);

    }
}
