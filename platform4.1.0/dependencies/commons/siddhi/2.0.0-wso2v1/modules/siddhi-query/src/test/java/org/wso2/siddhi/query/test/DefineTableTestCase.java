package org.wso2.siddhi.query.test;

import junit.framework.Assert;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.TableDefinition;
import org.wso2.siddhi.query.compiler.SiddhiCompiler;
import org.wso2.siddhi.query.compiler.exception.SiddhiPraserException;

public class DefineTableTestCase {


    @Test
    public void Test1() throws RecognitionException, SiddhiPraserException {
        TableDefinition streamDefinition = SiddhiCompiler.parseTableDefinition("define table cseStream ( symbol string, price int, volume float )");
        Assert.assertEquals(new TableDefinition().
                name("cseStream").
                attribute("symbol", Attribute.Type.STRING).
                attribute("price", Attribute.Type.INT).
                attribute("volume", Attribute.Type.FLOAT).toString(),
                            streamDefinition.toString());
    }

    @Test
    public void Test2() throws RecognitionException, SiddhiPraserException {
        TableDefinition streamDefinition = SiddhiCompiler.parseTableDefinition("define table `define` ( `string` string, price int, volume float );");
        Assert.assertEquals(new TableDefinition().
                name("define").
                attribute("string", Attribute.Type.STRING).
                attribute("price", Attribute.Type.INT).
                attribute("volume", Attribute.Type.FLOAT).toString(),
                            streamDefinition.toString());
    }

    @Test
    public void Test3() throws RecognitionException, SiddhiPraserException {
        TableDefinition streamDefinition = SiddhiCompiler.parseTableDefinition("define table cseStream ( symbol string, price int, volume float ) from SQL:myDB.tableName");
        Assert.assertEquals(new TableDefinition().
                name("cseStream").
                attribute("symbol", Attribute.Type.STRING).
                attribute("price", Attribute.Type.INT).
                attribute("volume", Attribute.Type.FLOAT).
                from("SQL", "myDB", "tableName").toString(),
                            streamDefinition.toString());
    }


}
