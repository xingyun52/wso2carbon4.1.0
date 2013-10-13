tree grammar SiddhiQLGrammarWalker;

options {
    backtrack=true;
    tokenVocab=SiddhiQLGrammar;
    ASTLabelType=CommonTree;
}
 
@header {
	package org.wso2.siddhi.query.compiler;

    import org.wso2.siddhi.query.api.ExecutionPlan;
    import org.wso2.siddhi.query.api.QueryFactory;
    import org.wso2.siddhi.query.api.condition.Condition;
    import org.wso2.siddhi.query.api.condition.ConditionExtension;
    import org.wso2.siddhi.query.api.definition.Attribute;
    import org.wso2.siddhi.query.api.definition.StreamDefinition;
    import org.wso2.siddhi.query.api.definition.TableDefinition;
    import org.wso2.siddhi.query.api.expression.Expression;
    import org.wso2.siddhi.query.api.expression.ExpressionExtension;
    import org.wso2.siddhi.query.api.expression.Time;
    import org.wso2.siddhi.query.api.expression.Variable;
    import org.wso2.siddhi.query.api.expression.constant.Constant;
    import org.wso2.siddhi.query.api.extension.Extension;
    import org.wso2.siddhi.query.api.query.Query;
    import org.wso2.siddhi.query.api.query.input.BasicStream;
    import org.wso2.siddhi.query.api.query.input.JoinStream;
    import org.wso2.siddhi.query.api.query.input.SingleStream;
    import org.wso2.siddhi.query.api.query.input.Stream;
    import org.wso2.siddhi.query.api.query.input.handler.Filter;
    import org.wso2.siddhi.query.api.query.input.handler.TransformExtension;
    import org.wso2.siddhi.query.api.query.input.handler.Transformer;
    import org.wso2.siddhi.query.api.query.input.handler.Window;
    import org.wso2.siddhi.query.api.query.input.handler.WindowExtension;
    import org.wso2.siddhi.query.api.query.input.pattern.Pattern;
    import org.wso2.siddhi.query.api.query.input.pattern.PatternStream;
    import org.wso2.siddhi.query.api.query.input.pattern.element.LogicalElement;
    import org.wso2.siddhi.query.api.query.input.pattern.element.PatternElement;
    import org.wso2.siddhi.query.api.query.input.sequence.Sequence;
    import org.wso2.siddhi.query.api.query.input.sequence.SequenceStream;
    import org.wso2.siddhi.query.api.query.input.sequence.element.SequenceElement;
    import org.wso2.siddhi.query.api.query.output.OutStream;
    import org.wso2.siddhi.query.api.query.projection.Projector;
    import org.wso2.siddhi.query.api.query.projection.attribute.ComplexAttribute;
    import org.wso2.siddhi.query.api.query.projection.attribute.OutputAttribute;
    import org.wso2.siddhi.query.api.query.projection.attribute.OutputAttributeExtension;
    import org.wso2.siddhi.query.api.query.projection.attribute.SimpleAttribute;

} 

executionPlan returns [List<ExecutionPlan> executionPlanList]
    @init{
        $executionPlanList=new ArrayList<ExecutionPlan>();
    }
	: (^(STREAM_DEFINITION definitionStream {$executionPlanList.add($definitionStream.streamDefinition);}))*
	  (^(TABLE_DEFINITION definitionTable {$executionPlanList.add($definitionTable.tableDefinition);}))*
	  (query)*
	; 

definitionStream returns [StreamDefinition streamDefinition]
	@init{
        $streamDefinition = QueryFactory.createStreamDefinition();
    }
	:  ^(streamId {$streamDefinition.name($streamId.value);} (^(IN_ATTRIBUTE attributeName type {$streamDefinition.attribute($attributeName.value, $type.type);}))+)
	;

definitionTable returns [TableDefinition tableDefinition]
    @init{
        $tableDefinition = QueryFactory.createTableDefinition();
    }
	:  ^(id {$tableDefinition.name($id.value);}  (^(IN_ATTRIBUTE attributeName type {$tableDefinition.attribute($attributeName.value, $type.type);}))+  (^(TABLE  tableType databaseName tableName {$tableDefinition.from($tableType.value, $databaseName.value, $tableName.value);}))? )
	;

query returns [Query query]
	: ^(QUERY outputStream inputStream outputProjection ) {$query = QueryFactory.createQuery().outStream($outputStream.value).
																						from($inputStream.inStream).
																						project($outputProjection.projector);}
	| ^(QUERY inputStream outputProjection ) {$query = QueryFactory.createQuery().from($inputStream.inStream).
																						project($outputProjection.projector);}
	;

outputStream returns [OutStream value]
	:  ^(OUT_STREAM streamId {$value=new OutStream($streamId.value);} (outputType {$value=new OutStream($streamId.value,$outputType.outputType);})?)
	;

outputType returns [OutStream.OutputEvents outputType]
	: 'expired-events' {$outputType=OutStream.OutputEvents.EXPIRED_EVENTS;}
	| 'current-events' {$outputType=OutStream.OutputEvents.CURRENT_EVENTS;}
	| 'all-events'     {$outputType=OutStream.OutputEvents.ALL_EVENTS;}
	;

inputStream returns [Stream inStream]
	: ^(SEQUENCE_FULL sequenceFullStream) {$inStream=$sequenceFullStream.stream;}
	| ^(PATTERN_FULL  patternFullStream ) {$inStream=$patternFullStream.stream;}
	| ^(JOIN joinStream) 	{$inStream=$joinStream.stream;}
	| windowStream          {$inStream=$windowStream.singleStream;}
	| basicStream  	        {$inStream=$basicStream.basicStream;}
	;  
 
basicStream returns [BasicStream basicStream]
	: ^(STREAM rawStream {$basicStream=$rawStream.rawStream;} (transformHandler {$basicStream.setTransformer($transformHandler.transformer);})? (id {$basicStream.setStreamReferenceId($id.value);})?)
	;

windowStream returns [SingleStream singleStream]
    @init{
		BasicStream rawStream=null;
	}
	: ^(STREAM  ^(ANONYMOUS returnQuery {rawStream=$returnQuery.stream;} (filterHandler {rawStream.setFilter($filterHandler.filter);})? (transformHandler {rawStream.setTransformer($transformHandler.transformer);})?  windowHandler {$singleStream=rawStream.window($windowHandler.window);} ) (id {$singleStream.setStreamReferenceId($id.value);})?)
	| ^(STREAM  ^(streamId {rawStream=QueryFactory.inputStream($streamId.value);} (filterHandler {rawStream.setFilter($filterHandler.filter);})? (transformHandler {rawStream.setTransformer($transformHandler.transformer);})?  windowHandler {$singleStream=rawStream.window($windowHandler.window);} ) (id {$singleStream.setStreamReferenceId($id.value);})?)
	;

rawStream returns [BasicStream rawStream]
	: ^( ANONYMOUS returnQuery {$rawStream=$returnQuery.stream;} (filterHandler {$rawStream.setFilter($filterHandler.filter);})?  )
	| ^(streamId {$rawStream=QueryFactory.inputStream($streamId.value);} (filterHandler {$rawStream.setFilter($filterHandler.filter);})? )
	;

joinStream  returns [Stream stream]
	scope{
		Condition onCondition;
		Expression within;
	}
	: leftStream  join rightStream 'unidirectional' (condition {$joinStream::onCondition=$condition.condition;})? (time {$joinStream::within=$time.expression;})? { stream=QueryFactory.joinStream($leftStream.singleStream,$join.type,$rightStream.singleStream,$joinStream::onCondition,(Constant)$joinStream::within,JoinStream.EventTrigger.RIGHT);}
	| leftStream join rightStream (condition {$joinStream::onCondition=$condition.condition;})? (time {$joinStream::within=$time.expression;})? 				  { stream=QueryFactory.joinStream($leftStream.singleStream,$join.type,$rightStream.singleStream,$joinStream::onCondition,(Constant)$joinStream::within);}
	| leftStream 'unidirectional'  join rightStream (condition {$joinStream::onCondition=$condition.condition;})? (time {$joinStream::within=$time.expression;})? { stream=QueryFactory.joinStream($leftStream.singleStream,$join.type,$rightStream.singleStream,$joinStream::onCondition,(Constant)$joinStream::within,JoinStream.EventTrigger.LEFT);}
	;

leftStream returns [SingleStream singleStream]
    :  windowStream {$singleStream=$windowStream.singleStream;}
    |  basicStream  {$singleStream=$basicStream.basicStream;}
    ;

rightStream returns [SingleStream singleStream]
    :  windowStream {$singleStream=$windowStream.singleStream;}
    |  basicStream  {$singleStream=$basicStream.basicStream;}
    ;
 
returnQuery returns [BasicStream stream]
	@init{
		System.err.println("Return Query not yet supported!");
	}
	: ^(RETURN_QUERY  inputStream outputProjection) {stream=QueryFactory.createQuery().from($inputStream.inStream).project($outputProjection.projector).returnStream();}
	;

patternFullStream returns [PatternStream stream]
	: ^(PATTERN  patternStream {stream= QueryFactory.patternStream($patternStream.element);} (time {stream= QueryFactory.patternStream($patternStream.element,(Constant)$time.expression);})?  )
	;

patternStream returns [PatternElement element]
	: ^('every'  patternItem) {$element=Pattern.every($patternItem.element);} (p=patternStream {$element=Pattern.followedBy($element,$p.element);})?
	| ^('every' nonEveryPatternStream ) {$element=Pattern.every($nonEveryPatternStream.element);}   (p=patternStream {$element=Pattern.followedBy($element,$p.element);})?
	| patternItem {$element=$patternItem.element;} (p=patternStream {$element=Pattern.followedBy($element,$p.element);})?
	;
    
nonEveryPatternStream returns [PatternElement element]
	: patternItem {$element=$patternItem.element;} (p=nonEveryPatternStream {$element=Pattern.followedBy($element,$p.element);})?
	;

sequenceFullStream returns [SequenceStream stream]
	: ^(SEQUENCE  sequenceStream {stream= QueryFactory.sequenceStream($sequenceStream.element);} (time {stream= QueryFactory.sequenceStream($sequenceStream.element,(Constant)$time.expression);})? )
	;
	
sequenceStream returns [SequenceElement element]
	@init{
		List<SequenceElement> elementList=new ArrayList<SequenceElement>();
	}
	@after{
		$element=elementList.get(elementList.size()-1);
		for(int i=elementList.size()-2;i>=0;i--){
			$element=Sequence.next(elementList.get(i),$element);
		}
	}
	:  (sequenceItem {elementList.add($sequenceItem.element);} )+
	;
	
patternItem returns [PatternElement element]
	: ^('and' i1=itemStream i2=itemStream) 	{element=Pattern.logical($i1.basicStream,LogicalElement.Type.AND,$i2.basicStream);}
	| ^('or'  i1=itemStream i2=itemStream)	{element=Pattern.logical($i1.basicStream,LogicalElement.Type.OR,$i2.basicStream);}
	| ^(COLLECT itemStream collect) 	{element=Pattern.count($itemStream.basicStream,$collect.startVal,$collect.endVal);}
	| itemStream						{element=$itemStream.basicStream;}
	;

sequenceItem returns [SequenceElement element]
	: ^('or' i1=itemStream i2=itemStream) 					{element=Sequence.or($i1.basicStream,$i2.basicStream);}
	| ^(REGEX itemStream regex[$itemStream.basicStream])  	{element=$regex.element;}
	| itemStream											{element=$itemStream.basicStream;}
	;   

itemStream returns [BasicStream basicStream]
	: ^(STREAM rawStream {$basicStream=$rawStream.rawStream;}(attributeName {$basicStream.setStreamReferenceId($attributeName.value);})?)
	;
		

regex [BasicStream basicStream] returns [SequenceElement element]
	: ('*' {$element=Sequence.zeroOrMany($basicStream);} |'+' {$element=Sequence.oneOrMany($basicStream);}|'?' {$element=Sequence.zeroOrOne($basicStream);}) ('?' {System.err.println(" ? variation in regex not yet supported!");})?
	;

outputProjection returns [Projector projector]
	: {$projector = QueryFactory.outputProjector();} 
		externalCall? 
		^(OUT_ATTRIBUTES outputAttributeList {$projector.addProjectionList($outputAttributeList.attributeList);} ) 
		(groupBy {$projector.addGroupByList($groupBy.variables);})?
		(having {$projector.having($having.value);})? 
	;
	
outputAttributeList returns [List<OutputAttribute> attributeList]
	@init{
		$attributeList=new ArrayList<OutputAttribute>();
	}
	: '*' 
	| ( ^(OUT_ATTRIBUTE outputItem {$attributeList.add($outputItem.value);}))+
	;
	
outputItem returns [OutputAttribute value]
	: id extensionOutFunction[$id.value] {$value=$extensionOutFunction.value;}
    | outFunction id			{$value=new ComplexAttribute($id.value, $outFunction.name, $outFunction.expressions);}
	| ex=expression id 	    {$value=new SimpleAttribute($id.value, $ex.expression);}
	| attributeVariable		{$value=new SimpleAttribute($attributeVariable.variable.getAttributeName(), $attributeVariable.variable);}
	;

extensionOutFunction[String id] returns [OutputAttributeExtension value]
    : ^( EXTENSION_FUNCTION extensionId functionId {$value =new OutputAttributeExtension($id, $extensionId.value, $functionId.value,null);}
                            (parameters  {$value = new OutputAttributeExtension($id, $extensionId.value, $functionId.value, $parameters.expressions);})?)
    ;

	
outFunction returns [String name, Expression[\] expressions]
	:  ^( FUNCTION ID {$name=$ID.text;} (parameters  {$expressions=$parameters.expressions;})?)
	;

groupBy returns [List<Variable> variables]
	@init{
		$variables=new ArrayList<Variable>();
	}	
	: ^('group' (attributeVariable {$variables.add($attributeVariable.variable);})+)
	;

having returns [Condition value]
	: ^('having' condition) {$value=$condition.condition;}
	;

externalCall
	: ^( 'call' ^(ID parameters?))
	;

filterHandler returns [Filter filter]
	: ^( FILTER condition {$filter=new Filter($condition.condition);} )
	;

transformHandler returns [Transformer transformer]
	: ^( TRANSFORM extensibleId["transform"] {$transformer=new Transformer($extensibleId.name,(TransformExtension) $extensibleId.extension, null);} (parameters {$transformer=new Transformer($extensibleId.name, (TransformExtension)$extensibleId.extension, $parameters.expressions);})?)
	;
 
windowHandler returns [Window window]
	: ^(WINDOW extensibleId["window"] {$window=new Window($extensibleId.name,(WindowExtension) $extensibleId.extension, null);} (parameters {$window=new Window($extensibleId.name, (WindowExtension)$extensibleId.extension, $parameters.expressions);})? )
	;

extensibleId[String type] returns [String name, Extension extension]
    : id {$name=$id.value;}
    | ^( EXTENSION   extensionId functionId {
        if("window".equals(type)){
            $extension=new WindowExtension($extensionId.value, $functionId.value);
        } else if("transform".equals(type)){
            $extension=new TransformExtension($extensionId.value, $functionId.value);
        } else {
            System.err.println("Extension for "+type+" is not supported!");
        }
    })
    ;

parameters returns [Expression[\] expressions]
	scope{
		List<Expression> parameterlist;
	}
	@init{
		$parameters::parameterlist=new ArrayList<Expression>();
	}
	@after{
		$expressions=$parameters::parameterlist.toArray(new Expression[$parameters::parameterlist.size()]);
	}	
	:  ^(PARAMETERS (parameter {$parameters::parameterlist.add($parameter.expression);})+)
	;

time returns [Expression expression]
	: constant {$expression=$constant.expression;}
	;
	
parameter returns [Expression expression]
	: e1=expression	{$expression=$e1.expression;}
	;

collect returns [int startVal , int endVal]
	@init{
		$startVal=0;
		$endVal=-2;//UNLIMITED
	}
	: countStart ':' countEnd 	{$startVal=$countStart.value; $endVal=$countEnd.value;}
	| countStart ':'			{$startVal=$countStart.value;}
	| ':' countEnd 				{$endVal=$countEnd.value;}
	| countStartAndEnd 		    {$startVal=$countStartAndEnd.value; $endVal=$countStartAndEnd.value;}
	
	;	

countStart returns [int value]
	:POSITIVE_INT_VAL {$value=Integer.parseInt($POSITIVE_INT_VAL.text);}
	;
	
countEnd returns [int value]
	:POSITIVE_INT_VAL {$value=Integer.parseInt($POSITIVE_INT_VAL.text);}
	;
		
countStartAndEnd returns [int value]
	:POSITIVE_INT_VAL {$value=Integer.parseInt($POSITIVE_INT_VAL.text);}
	;			

//conditions start

condition returns [Condition condition]
	: ^(CONDITION conditionExpression {$condition=$conditionExpression.condition;})
	;

conditionExpression returns [Condition condition]
    : ^('or' c1=conditionExpression c2=conditionExpression) 	{$condition=Condition.or($c1.condition,$c2.condition);}
    | ^('and' c1=conditionExpression c2=conditionExpression) 	{$condition=Condition.and($c1.condition,$c2.condition);}
    | ^( compareOperation m1=expression m2=expression)	        {$condition=Condition.compare($m1.expression,$compareOperation.operator,$m2.expression);}
    | ^('not' c=conditionExpression)							{$condition=Condition.not($c.condition);}
    | ^( CONDITION_FUNCTION extensionId functionId {$condition=new ConditionExtension($extensionId.value,$functionId.value,null);} (parameters 	{$condition=new ConditionExtension($extensionId.value,$functionId.value,$parameters.expressions);})?)
    |boolVal  													{System.err.println("Boolean Value as condition not yet supported!");}
	;
	
expression returns [Expression expression]
	: ^('%' e1=expression e2=expression)	{$expression=Expression.mod($e1.expression,$e2.expression);}
    | ^('/' e1=expression e2=expression)	{$expression=Expression.divide($e1.expression,$e2.expression);}
    | ^('*' e1=expression e2=expression)	{$expression=Expression.multiply($e1.expression,$e2.expression);}
    | ^('-' e1=expression e2=expression)	{$expression=Expression.minus($e1.expression,$e2.expression);}
    | ^('+' e1=expression e2=expression)	{$expression=Expression.add($e1.expression,$e2.expression);}
    |valueExpression 							{$expression=$valueExpression.expression;}
    ;

valueExpression returns [Expression expression]
   	:constant 			{$expression=$constant.expression;}
   	|type 			    {$expression=Expression.type($type.type);}
   	|attributeVariable	{$expression=$attributeVariable.variable;}
   	|^( EXTENSION_FUNCTION extensionId functionId {$expression=new ExpressionExtension($extensionId.value,$functionId.value, null);} (parameters {$expression=new ExpressionExtension($extensionId.value,$functionId.value,$parameters.expressions);})?)
   	;


//conditions end

constant returns [Expression expression]
	: ^( CONSTANT intVal) 		{$expression=Expression.value($intVal.value);}
	| ^( CONSTANT longVal)		{$expression=Expression.value($longVal.value);}
	| ^( CONSTANT floatVal)		{$expression=Expression.value($floatVal.value);}
	| ^( CONSTANT doubleVal)	{$expression=Expression.value($doubleVal.value);}
	| ^( CONSTANT boolVal)		{$expression=Expression.value($boolVal.value);}
	| ^( CONSTANT stringVal)	{$expression=Expression.value($stringVal.value);}
	| ^( CONSTANT timeExpr)      {$expression=Expression.value($timeExpr.value);}
	;

 timeExpr returns [long value]
	:^(TIME_EXP (yearValue   {$value =  $yearValue.value;})? (monthValue   {$value =  $monthValue.value;})? (weekValue   {$value =  $weekValue.value;})? (dayValue   {$value =  $dayValue.value;})?  (hourValue  {$value +=  $hourValue.value;} )? (minuteValue  {$value +=  $minuteValue.value;} )?
	 (secondValue  {$value +=  $secondValue.value;} )? (milliSecondValue  {$value +=  $milliSecondValue.value;} )? )
	;

yearValue returns [long value]
	: a= POSITIVE_INT_VAL ( 'years' | 'year')  {$value =  Time.year(Integer.parseInt($a.text)).value();}
	;

monthValue returns [long value]
	: a = POSITIVE_INT_VAL ( 'months' | 'month') {$value =  Time.month(Integer.parseInt($a.text)).value();}
	;

weekValue returns [long value]
	: a = POSITIVE_INT_VAL ( 'weeks' | 'week')  {$value =  Time.week(Integer.parseInt($a.text)).value();}
	;

dayValue  returns [long value]
	: a = POSITIVE_INT_VAL ('days' | 'day')  {$value =  Time.day(Integer.parseInt($a.text)).value();}
	;

hourValue  returns [long value]
	: a = POSITIVE_INT_VAL ('hours' | 'hour') {$value =  Time.hour(Integer.parseInt($a.text)).value();}
	;

minuteValue  returns [long value]
	: a = POSITIVE_INT_VAL ('minutes' | 'minute' | 'min') {$value = Time.minute(Integer.parseInt($a.text)).value();}
	;

secondValue returns [long value]
	: a= POSITIVE_INT_VAL ('seconds' | 'second' | 'sec') {$value =  Time.sec(Integer.parseInt($a.text)).value();}
	;

milliSecondValue  returns [long value]
	: a = POSITIVE_INT_VAL ('milliseconds' | 'millisecond')  {$value =  Time.milliSec(Integer.parseInt($a.text)).value();}
	;


streamId returns [String value]
	: id {$value=$id.value;}
	;

attributeVariable  returns [Variable variable]
	:streamPositionAttributeName	{$variable=Expression.variable($streamPositionAttributeName.stream,$streamPositionAttributeName.position,$streamPositionAttributeName.attribute);}
   	|streamAttributeName 			{$variable=Expression.variable($streamAttributeName.stream,$streamAttributeName.attribute);}
   	|attributeName					{$variable=Expression.variable($attributeName.value);}
	;

streamPositionAttributeName returns [String stream, int position, String attribute]
	: ^( ATTRIBUTE ^(streamId {$stream=$streamId.value;} POSITIVE_INT_VAL {$position=Integer.parseInt($POSITIVE_INT_VAL.text);}) id {$attribute=$id.value;})
	; 	

streamAttributeName returns [String stream, String attribute]
	:^( ATTRIBUTE (streamId {$stream=$streamId.value;}) id {$attribute=$id.value;})
	;
	
attributeName returns [String value]
	:^( ATTRIBUTE id {$value=$id.value;})
	;	
 
join returns [JoinStream.Type type]
	:  ^('join' ^('outer' 'left'))  {$type=JoinStream.Type.LEFT_OUTER_JOIN;}	{System.err.println("Left outer join not yet supported!");}
	|  ^('join' ^('outer' 'right'))	{$type=JoinStream.Type.RIGHT_OUTER_JOIN;} 	{System.err.println("Right outer join not yet supported!");}
	|  ^('join' ^('outer' 'full'))	{$type=JoinStream.Type.FULL_OUTER_JOIN;}	{System.err.println("Full outer join not yet supported!");}
	| ^('join' 'inner')				{$type=JoinStream.Type.INNER_JOIN;}
	;

compareOperation returns [Condition.Operator operator]
	:'=='   	{$operator=Condition.Operator.EQUAL;}
	|'!=' 		{$operator=Condition.Operator.NOT_EQUAL;}
	|'<='		{$operator=Condition.Operator.LESS_THAN_EQUAL;}
	|'>=' 		{$operator=Condition.Operator.GREATER_THAN_EQUAL;}
	|'<' 		{$operator=Condition.Operator.LESS_THAN;}
	|'>'  		{$operator=Condition.Operator.GREATER_THAN;}
	|'contains'	{$operator=Condition.Operator.CONTAINS;}
	|'instanceof'	{$operator=Condition.Operator.INSTANCE_OF;}
	;
	
id returns [String value]
    : ID {$value=$ID.text;}
    | ID_QUOTES {$value=$ID_QUOTES.text;}
    ;

intVal returns [int value]
    : ^(SIGNED_VAL  POSITIVE_INT_VAL {$value=Integer.parseInt($POSITIVE_INT_VAL.text);} ('-' {$value=$value*-1;})?)
    ;

longVal returns [long value]
    : ^(SIGNED_VAL  POSITIVE_LONG_VAL {$value=Long.parseLong($POSITIVE_LONG_VAL.text.substring(0,$POSITIVE_LONG_VAL.text.length()-1));} ('-' {$value=$value*-1;})?)
    ;

floatVal returns [float value]
    : ^(SIGNED_VAL  POSITIVE_FLOAT_VAL {$value=Float.parseFloat($POSITIVE_FLOAT_VAL.text);} ('-' {$value=$value*-1;})?)
    ;

doubleVal returns [double value]
    : ^(SIGNED_VAL  POSITIVE_DOUBLE_VAL {$value=Double.parseDouble($POSITIVE_DOUBLE_VAL.text);} ('-' {$value=$value*-1;})?)
    ;

boolVal returns [boolean value]
    : BOOL_VAL {$value=Boolean.parseBoolean($BOOL_VAL.text);}
    ;

extensionId returns [String value]
    : id {$value=$id.value;}
    ;

functionId returns [String value]
    : id {$value=$id.value;}
    ;

tableType returns [String value]
    : id {$value=$id.value;}
    ;

databaseName returns [String value]
    : id {$value=$id.value;}
    ;

tableName returns [String value]
    : id {$value=$id.value;}
    ;

stringVal returns [String value]
    : STRING_VAL {$value=$STRING_VAL.text;}
    ;
    
type returns [Attribute.Type type]
	:'string' 	{$type=Attribute.Type.STRING;} 
	|'int'  	{$type=Attribute.Type.INT;} 
	|'long' 	{$type=Attribute.Type.LONG;} 
	|'float' 	{$type=Attribute.Type.FLOAT;} 
	|'double' 	{$type=Attribute.Type.DOUBLE;} 
	|'bool' 	{$type=Attribute.Type.BOOL;} 
	; 

	
	
	
	

