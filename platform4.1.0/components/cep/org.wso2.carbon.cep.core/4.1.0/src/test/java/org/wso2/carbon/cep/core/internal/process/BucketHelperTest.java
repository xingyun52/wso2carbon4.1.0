package org.wso2.carbon.cep.core.internal.process;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.wso2.carbon.cep.core.Bucket;
import org.wso2.carbon.cep.core.Expression;
import org.wso2.carbon.cep.core.Query;
import org.wso2.carbon.cep.core.XpathDefinition;
import org.wso2.carbon.cep.core.internal.config.BucketHelper;
import org.wso2.carbon.cep.core.mapping.input.Input;
import org.wso2.carbon.cep.core.mapping.input.mapping.XMLInputMapping;
import org.wso2.carbon.cep.core.mapping.input.property.XMLInputProperty;
import org.wso2.carbon.cep.core.mapping.output.Output;
import org.wso2.carbon.cep.core.mapping.output.mapping.XMLOutputMapping;

import java.util.ArrayList;
import java.util.List;

public class BucketHelperTest extends TestCase {

	private Bucket bucket;
	private Output outputOne;
	private Expression expressionOne;
	private Expression expressiontwo;
	private XMLOutputMapping xmlOutPutMapping;
	private XpathDefinition xpathDefinition;
	private XMLInputMapping xmlInputMapping;
	private Query queryOne;
	private Query queryTwo;
	private List<Input> inputList;
	private List<XMLInputProperty> inputPropertyList;
	private List<XpathDefinition> inputXpathDefinitionList;
	private List<Query> queryList;
	private OMElement bucketOM;

	@Override
	protected void setUp() throws Exception {
		bucket = new Bucket();
		queryOne = new Query();
		queryTwo = new Query();
		outputOne = new Output();
		expressionOne = new Expression();
		expressiontwo = new Expression();
		xmlOutPutMapping = new XMLOutputMapping();
		xpathDefinition = new XpathDefinition();
		xmlInputMapping = new XMLInputMapping();
		bucket.setName("TestBucket");
		bucket.setDescription("This is tesing Bucket");
		bucket.setEngineProvider("DroolsFusionEngineProvider");
		inputList = new ArrayList<Input>();
		inputPropertyList = new ArrayList<XMLInputProperty>();
		inputXpathDefinitionList = new ArrayList<XpathDefinition>();
		queryList = new ArrayList<Query>();
		xmlInputMapping.setStream("TestingStream");
		xmlInputMapping.setProperties(inputPropertyList);
		xpathDefinition.setPrefix("prefix");
		xpathDefinition.setNamespace("nameSpace");
		inputXpathDefinitionList.add(xpathDefinition);
		xmlInputMapping.setXpathDefinitionList(inputXpathDefinitionList);
		bucket.setInputs(inputList);
		queryOne.setName("query");
		outputOne.setTopic("output");
		outputOne.setBrokerName("wsEventBroker");
		xmlOutPutMapping.setMappingXMLText("<cep>Testing</cep>");
		outputOne.setOutputMapping(xmlOutPutMapping);

		queryTwo.setName("queryTwo");
		expressionOne.setText("Testing");
		expressiontwo.setText("TestingTwo");
		queryTwo.setExpression(expressionOne);
		queryOne.setExpression(expressiontwo);
		queryList.add(queryOne);
		queryList.add(queryTwo);
		bucket.setQueries(queryList);
		super.setUp();

	}

	public void testOMBucket() throws Exception {
		bucketOM = BucketHelper.bucketToOM(bucket);
		assertTrue(bucketOM != null);
		bucket = BucketHelper.fromOM(bucketOM);
		assertTrue(bucket != null);
		assertTrue(bucket.getName().equals("TestBucket"));
		assertTrue(bucket.getDescription().equals("This is tesing Bucket"));
		assertTrue(bucket.getEngineProvider().equals(
				"DroolsFusionEngineProvider"));
		inputList = bucket.getInputs();
		queryList = bucket.getQueries();
		for (Input input : inputList) {
			assertTrue(input.getTopic().equals("TestingInput"));
			assertTrue(input.getBrokerName().equals("wsEventBroker"));
			xmlInputMapping = (XMLInputMapping) input.getInputMapping();
			assertTrue(xmlInputMapping.getStream().equals("TestingStream"));
			inputPropertyList = xmlInputMapping.getProperties();
			inputXpathDefinitionList = xmlInputMapping
					.getXpathNamespacePrefixes();
			for (XMLInputProperty property : inputPropertyList) {
				assertTrue(property.getName().equals("inputproperty"));
				assertTrue(property.getType().equals("java.lang.Integer"));
				assertTrue(property.getXpath().equals("xpath"));
			}
			for (XpathDefinition definition : inputXpathDefinitionList) {
				assertTrue(definition.getPrefix().equals("prefix"));
				assertTrue(definition.getNamespace().equals("nameSpace"));
			}
		}
		for (Query query : queryList) {
			if (query.getName().equals("query")) {
				outputOne = query.getOutput();
				expressionOne = query.getExpression();
				assertTrue(outputOne.getBrokerName().equals("wsEventBroker"));
				assertTrue(outputOne.getTopic().equals("output"));
				assertTrue(expressionOne.getText().equals("TestingTwo"));
				xmlOutPutMapping = (XMLOutputMapping) outputOne
						.getOutputMapping();
				assertTrue(xmlOutPutMapping.getMappingXMLText().equals(
						"<cep>Testing</cep>"));
			} else if (query.getName().equals("queryTwo")) {
				expressiontwo = query.getExpression();
				assertTrue(expressiontwo !=null);
			}

		}
	}
}
