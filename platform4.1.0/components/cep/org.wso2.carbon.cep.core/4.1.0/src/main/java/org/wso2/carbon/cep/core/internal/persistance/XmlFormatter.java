/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.cep.core.internal.persistance;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Pretty-prints xml, supplied as a string.
 * <p/>
 * eg.
 * <code>
 * String formattedXml = new XmlFormatter().format("<tag><nested>hello</nested></tag>");
 * </code>
 */
public class XmlFormatter {

    public XmlFormatter() {
    }

    public String format(String unformattedXml) {
        try {
            final Document document = parseXmlFile(unformattedXml);

            OutputFormat format = new OutputFormat(document);
            format.setLineWidth(65);
            format.setIndenting(true);
            format.setIndent(2);
            Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);

            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    public static void main(String[] args) {
//        String unformattedXml =
//                "<cep:bucket xmlns:cep=\"http://wso2.org/carbon/cep\" name=\"XMLStockQuoteAnalyzer\"><cep:description>\n" +
//                "This bucket analyzes stock quotes and trigger an event if the last\n" +
//                "traded amount vary by 2 percent with regards to the average traded\n" +
//                "price within past 2 minutes.\n" +
//                "    </cep:description><cep:engineProviderConfiguration engineProvider=\"SiddhiCEPRuntime\"><cep:property name=\"siddhi.persistence.snapshot.time.interval.minutes\">0</cep:property><cep:property name=\"siddhi.enable.distributed.processing\">false</cep:property></cep:engineProviderConfiguration><cep:input topic=\"AllStockQuotes\" brokerName=\"localBroker\"><cep:xmlMapping queryEventType=\"Tuple\" stream=\"allStockQuotesStream\"><cep:xpathDefinition prefix=\"quotedata\" namespace=\"http://ws.cdyne.com/\"/><cep:property name=\"price\" xpath=\"//quotedata:StockQuoteEvent/quotedata:LastTradeAmount\" type=\"java.lang.Double\"/><cep:property name=\"symbol\" xpath=\"//quotedata:StockQuoteEvent/quotedata:StockSymbol\" type=\"java.lang.String\"/></cep:xmlMapping></cep:input><cep:query name=\"StockDetector\"><cep:expression><![CDATA[from allStockQuotesStream#window.time(120000)\n" +
//                "insert into fastMovingStockQuotesStream\n" +
//                "symbol,avg(price) as avgPrice, price\n" +
//                "group by symbol;]]></cep:expression><cep:output topic=\"FastMovingStockQuotes\" brokerName=\"localBroker\"><cep:xmlMapping><quotedata:StockQuoteDataEvent xmlns:quotedata=\"http://ws.cdyne.com/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
//                "                    <quotedata:StockSymbol>{symbol}</quotedata:StockSymbol>\n" +
//                "                    <quotedata:AvgLastTradeAmount>{avgPrice}</quotedata:AvgLastTradeAmount>\n" +
//                "                    <quotedata:LastTradeAmount>{price}</quotedata:LastTradeAmount>\n" +
//                "                </quotedata:StockQuoteDataEvent></cep:xmlMapping></cep:output></cep:query></cep:bucket>";
////        String unformattedXml =
////                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><QueryMessage\n" +
////                "        xmlns=\"http://www.SDMX.org/resources/SDMXML/schemas/v2_0/message\"\n" +
////                "        xmlns:query=\"http://www.SDMX.org/resources/SDMXML/schemas/v2_0/query\">\n" +
////                "    <Query>\n" +
////                "        <query:CategorySchemeWhere>\n" +
////                "   \t\t\t\t\t         <query:AgencyID>ECB\n\n\n\n</query:AgencyID>\n" +
////                "        </query:CategorySchemeWhere>\n" +
////                "    </Query>\n\n\n\n\n" +
////                "</QueryMessage>";
//
//        System.out.println(new XmlFormatter().format(unformattedXml));
//    }

}
